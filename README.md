# NexaVPN

Clean-room, full-stack VPN reference implementation: an Android client built
on the official WireGuard library, a FastAPI provisioning backend, and a VPS
bootstrap script. All code is original.

> **Status:** active development — see the
> [roadmap](#roadmap) and [CHANGELOG](CHANGELOG.md).

## Architecture

```
┌────────────────┐   GET /servers   ┌──────────────┐   SQL    ┌─────────┐
│ Android client │ ───────────────▶ │ FastAPI      │ ───────▶ │ SQLite  │
│ (Compose UI)   │  POST /peers     │ backend/     │          └─────────┘
│                │ ◀─────────────── │              │
│ GoBackend      │   peer lease     └──────┬───────┘
│ (wireguard-    │                         │ operator / future agent
│  android lib)  │                         ▼
└───────┬────────┘            ┌──────────────────────┐
        │ UDP 51820           │ WireGuard VPS        │
        └────────────────────▶│ server/setup-wg-…    │
                              └──────────────────────┘
```

**Security model:** the client generates its Curve25519 keypair on-device
(persisted via DataStore). Only the **public key** is sent to the backend
during provisioning; the backend returns a peer lease (address, server
public key, endpoint, DNS) from which the client builds its WireGuard config
locally.

## Components

| Component | Path | Docs | Stack |
|---|---|---|---|
| Android client | [`app/`](app/) | [app/README.md](app/README.md) | Kotlin, Jetpack Compose, wireguard-android |
| REST API | [`backend/`](backend/) | [backend/README.md](backend/README.md) | Python, FastAPI, SQLite |
| VPS provisioning | [`server/`](server/) | [server/README.md](server/) | Bash, WireGuard, iptables |

## Quick start

### 1. Provision a VPS

```bash
ssh root@your-vps 'bash -s' < server/setup-wg-server.sh
# note the printed public key
```

### 2. Run the backend

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export NEXA_API_TOKEN="$(openssl rand -hex 32)"   # enables bearer auth
export NEXA_DB=./backend.db
uvicorn main:app --host 0.0.0.0 --port 8000
```

Register your VPS in the `servers` table (see
[backend/README.md](backend/README.md)).

### 3. Build the client

Open the project in Android Studio, or:

```bash
./gradlew :app:assembleDebug
```

Point `ApiClient.DEFAULT_BASE_URL` at your backend, and pass the same token
via `ApiClient(deviceToken = …)` for provisioning.

## Roadmap

- [x] Project scaffold, CI-ready docs, initial commit
- [x] Real connect flow: VPN consent → peer provisioning → tunnel UP
- [x] Persistent device keypair (DataStore)
- [x] Backend bearer-token auth on provisioning endpoints
- [ ] Split tunneling (per-app include/exclude) UI
- [ ] Kill switch (block traffic when tunnel drops)
- [ ] Auto-select fastest server (latency probing)
- [ ] Favorites / recent servers
- [ ] Android CI (GitHub Actions: assemble + lint)
- [ ] Release pipeline: signing, R8, Play Store listing assets
- [ ] Play Store: privacy policy, Data Safety form, internal testing track

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Agent-based contributions should
read [`AGENTS.md`](AGENTS.md) first.

## Security

See [`SECURITY.md`](SECURITY.md) for disclosure policy and the current list
of intentional pre-production gaps.

## License

[MIT](LICENSE)
