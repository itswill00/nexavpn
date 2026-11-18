# backend/ — NexaVPN API

FastAPI service that serves the server list and provisions WireGuard peers.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Liveness probe |
| `GET` | `/servers` | List VPN servers (id, location, public key, port, load) |
| `POST` | `/peers` | Register a device public key on a server; returns wg-quick config |
| `DELETE` | `/peers/{public_key}` | Revoke a peer |

Interactive docs: `http://localhost:8000/docs` when running locally.

## Running

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export NEXA_DB=./backend.db          # optional, default /var/lib/nexavpn/backend.db
export NEXA_SUBNET=10.66.66          # optional
uvicorn main:app --reload
```

## Data model

- `servers` — id, country/city, hostname, server **public** key, port,
  premium flag, load, next_ip counter
- `peers` — id, server_id, client **public** key (unique), allocated address,
  created/expires timestamps

## Design rules

- **The backend never sees private keys.** Clients generate keypairs locally
  and submit only public keys.
- Address allocation is a simple counter per server (`10.66.66.3+`); the
  service rejects allocation past `.250` with HTTP 507.
- Peer records expire after 30 days (field present; enforcement is a TODO for
  the provisioning agent).

## Known gaps

Documented in the root `SECURITY.md`:

- No authentication on `POST /peers` — add device attestation or tokens
  before public exposure
- No rate limiting
- Expired peers are not yet pruned from the WireGuard server itself
