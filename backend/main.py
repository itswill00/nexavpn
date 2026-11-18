"""
NexaVPN backend — minimal FastAPI service.

Endpoints:
  GET  /servers         list available VPN servers
  POST /peers           register a device public key on a server, returns wg config

Persistence: SQLite. WireGuard server control is done via SSH/WG API in
server/provision.py (see server/README.md). This keeps the API stateless
regarding crypto: the client generates its own keypair and only sends the
PUBLIC key.
"""
from __future__ import annotations

import os
import sqlite3
import time
import uuid
import secrets
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

DB_PATH = os.environ.get("NEXA_DB", "/var/lib/nexavpn/backend.db")
BASE_SUBNET = os.environ.get("NEXA_SUBNET", "10.66.66")

app = FastAPI(title="NexaVPN API", version="0.1.0")


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
                created_at INTEGER,
                expires_at INTEGER
            );
            """
        )


@app.on_event("startup")
def _startup() -> None:
    init_db()


class PeerRequest(BaseModel):
    server_id: str
    public_key: str = Field(min_length=44, max_length=44)  # base64 wg pubkey


class PeerResponse(BaseModel):
    config: str
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


@app.post("/peers", response_model=PeerResponse)
def register_peer(req: PeerRequest) -> PeerResponse:
    with db() as conn:
        server = conn.execute(
            "SELECT * FROM servers WHERE id = ?", (req.server_id,)
        ).fetchone()
        if server is None:
            raise HTTPException(404, "unknown server")

        existing = conn.execute(
            "SELECT * FROM peers WHERE public_key = ?", (req.public_key,)
        ).fetchone()
        if existing:
            address = existing["address"]
        else:
            n = server["next_ip"]
            if n > 250:
                raise HTTPException(507, "server full")
            address = f"{BASE_SUBNET}.{n}"
            conn.execute(
                "UPDATE servers SET next_ip = next_ip + 1 WHERE id = ?",
                (req.server_id,),
            )

        peer_id = uuid.uuid4().hex
        now = int(time.time())
        expires = now + 30 * 24 * 3600
        conn.execute(
            "INSERT OR REPLACE INTO peers(id, server_id, public_key, address,"
            " created_at, expires_at) VALUES (?,?,?,?,?,?)",
            (peer_id, req.server_id, req.public_key, address, now, expires),
        )

    client_cfg = f"""[Interface]
PrivateKey = <CLIENT_KEEP_PRIVATE_NEVER_SEND>
Address = {address}/32
DNS = 1.1.1.1

[Peer]
PublicKey = {server['public_key']}
Endpoint = {server['hostname']}:{server['endpoint_port']}
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
"""
    return PeerResponse(config=client_cfg, expires_at=expires)


@app.delete("/peers/{public_key}")
def revoke_peer(public_key: str) -> dict:
    with db() as conn:
        cur = conn.execute("DELETE FROM peers WHERE public_key = ?", (public_key,))
    return {"deleted": cur.rowcount}
