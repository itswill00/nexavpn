"""
NexaVPN backend — FastAPI service.

Endpoints:
  GET    /health                  liveness
  GET    /servers                 list available VPN servers
  POST   /peers                   register a device public key -> peer lease
  DELETE /peers/{public_key}      revoke a peer

Security model:
  - The backend NEVER receives or stores private keys. Clients generate
    keypairs locally and submit only the public key.
  - POST /peers requires a bearer token (NEXA_API_TOKEN env) once set;
    leave the env unset only for local development.
"""
from __future__ import annotations

import hmac
import os
import sqlite3
import time
import uuid
from typing import Optional

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

DB_PATH = os.environ.get("NEXA_DB", "/var/lib/nexavpn/backend.db")
BASE_SUBNET = os.environ.get("NEXA_SUBNET", "10.66.66")
API_TOKEN = os.environ.get("NEXA_API_TOKEN")  # unset -> auth disabled (dev mode)
LEASE_DAYS = int(os.environ.get("NEXA_LEASE_DAYS", "30"))
DNS_SERVERS = [s.strip() for s in os.environ.get("NEXA_DNS", "1.1.1.1,1.0.0.1").split(",")]

app = FastAPI(title="NexaVPN API", version="0.2.0")


def db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    with db() as conn:
        conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS servers(
                id TEXT PRIMARY KEY,
                country TEXT, country_code TEXT, city TEXT,
                hostname TEXT, public_key TEXT,
                endpoint_port INTEGER DEFAULT 51820,
                premium INTEGER DEFAULT 0, load REAL DEFAULT 0,
                next_ip INTEGER DEFAULT 3
            );
            CREATE TABLE IF NOT EXISTS peers(
                id TEXT PRIMARY KEY,
                server_id TEXT REFERENCES servers(id),
                public_key TEXT UNIQUE,
                address TEXT,
                device_hash TEXT,
                created_at INTEGER,
                expires_at INTEGER
            );
            """
        )


def require_token(authorization: Optional[str] = Header(default=None)) -> None:
    """Bearer-token gate; disabled when NEXA_API_TOKEN is unset (dev)."""
    if API_TOKEN is None:
        return
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "missing bearer token")
    supplied = authorization.removeprefix("Bearer ").strip()
    if not hmac.compare_digest(supplied, API_TOKEN):
        raise HTTPException(403, "invalid token")


@app.on_event("startup")
def _startup() -> None:
    init_db()


class PeerRequest(BaseModel):
    server_id: str
    public_key: str = Field(min_length=44, max_length=44)  # base64 wg pubkey
    device_hash: Optional[str] = None  # opaque client identifier (optional)


class PeerLease(BaseModel):
    address: str
    server_public_key: str
    endpoint: str
    dns: list[str] = DNS_SERVERS
    expires_at: int


@app.get("/health")
def health() -> dict:
    return {"ok": True, "time": int(time.time())}


@app.get("/servers")
def list_servers() -> dict:
    with db() as conn:
        rows = conn.execute(
            "SELECT id, country, country_code, city, hostname, public_key,"
            " endpoint_port, premium, load FROM servers ORDER BY country"
        ).fetchall()
    return {
        "servers": [dict(r) | {"premium": bool(r["premium"])} for r in rows],
        "updatedAt": int(time.time()),
    }


@app.post("/peers", response_model=PeerLease)
def register_peer(
    req: PeerRequest,
    _token: None = Depends(require_token),
) -> PeerLease:
    with db() as conn:
        server = conn.execute(
            "SELECT * FROM servers WHERE id = ?", (req.server_id,)
        ).fetchone()
        if server is None:
            raise HTTPException(404, "unknown server")

        now = int(time.time())
        existing = conn.execute(
            "SELECT * FROM peers WHERE public_key = ?", (req.public_key,)
        ).fetchone()

        if existing and existing["expires_at"] > now:
            address = existing["address"]
            expires = existing["expires_at"]
        else:
            n = server["next_ip"]
            if n > 250:
                raise HTTPException(507, "server full")
            address = f"{BASE_SUBNET}.{n}"
            expires = now + LEASE_DAYS * 86400
            conn.execute(
                "UPDATE servers SET next_ip = next_ip + 1 WHERE id = ?",
                (req.server_id,),
            )

        peer_id = uuid.uuid4().hex
        conn.execute(
            "INSERT OR REPLACE INTO peers(id, server_id, public_key, address,"
            " device_hash, created_at, expires_at) VALUES (?,?,?,?,?,?,?)",
            (peer_id, req.server_id, req.public_key, address,
             req.device_hash, now, expires),
        )

    return PeerLease(
        address=f"{address}/32",
        server_public_key=server["public_key"],
        endpoint=f"{server['hostname']}:{server['endpoint_port']}",
        dns=DNS_SERVERS,
        expires_at=expires,
    )


@app.delete("/peers/{public_key}")
def revoke_peer(
    public_key: str,
    _token: None = Depends(require_token),
) -> dict:
    with db() as conn:
        cur = conn.execute("DELETE FROM peers WHERE public_key = ?", (public_key,))
    return {"deleted": cur.rowcount}
