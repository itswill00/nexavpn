# AGENTS.md

Guidance for AI coding agents (Claude Code, OpenCode, Codex, Cursor, and
similar) working in this repository.

## What this project is

NexaVPN is a **clean-room full-stack VPN reference implementation**:

- `app/` — Android client: Kotlin, Jetpack Compose, official
  `wireguard-android` library
- `backend/` — FastAPI REST service (server list, peer provisioning, SQLite)
- `server/` — WireGuard VPS bootstrap script (Debian/Ubuntu)

The architecture was informed by behavioral analysis of a commercial VPN
application. **No source code, assets, keys, certificates, or branding from
that application may be copied, re-derived, or committed here.** All code is
original.

## Ground rules

### Prohibited

- **Never** copy or transliterate code, resources, package names
  (`app.ninjavpn.*`), obfuscated class names, or configuration blobs
  (`edge.bin`, `stealth.json`, etc.) from the analyzed application into this
  repository.
- **Never** commit secrets: private keys, API tokens, real server hostnames,
  personal endpoints, or `.env` files. Use `.example` templates.
- **Never** weaken privacy properties: no traffic logging, no analytics SDKs,
  no ads SDKs, no excessive permissions beyond what `AndroidManifest.xml`
  already declares.
- **Never** send a WireGuard **private** key over the network. Keys are
  generated on-device; only public keys ever reach the backend.
- Do not bump major dependency versions or switch architectural patterns
  without flagging it explicitly in the PR description.

### Expected

- Follow [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat(app):`, `fix(backend):`, `docs:`, `chore:`).
- Keep `main` buildable at all times; run
  `./gradlew :app:assembleDebug` and `ruff check backend/` before proposing
  completion of any task.
- Match existing structure and naming: snake_case files in `backend/`,
  PascalCase Kotlin classes in `app/`, kebab-case for docs.
- Update documentation in the same change when behavior changes
  (`README.md`, module docs, this file's architecture map).
- Prefer small, single-purpose commits.

## Architecture map

```
┌────────────────┐   GET /servers   ┌──────────────┐   SQL    ┌─────────┐
│ Android client │ ───────────────▶ │ FastAPI      │ ───────▶ │ SQLite  │
│ (Compose UI)   │  POST /peers     │ backend/     │          └─────────┘
│                │ ◀─────────────── │              │
│ TunnelService  │   wg config      └──────┬───────┘
│ (wireguard-    │                         │ provisions peer on
│  android lib)  │                         ▼
└───────┬────────┘            ┌──────────────────────┐
        │ UDP 51820           │ WireGuard VPS        │
        └────────────────────▶│ server/setup-wg-…    │
                              └──────────────────────┘
```

- The client generates a keypair locally (`VpnController.ensurePeerKeys`),
  sends only the **public key** to `POST /peers`, receives a wg-quick config
  with its allocated address, and brings the tunnel up via
  `TunnelService`/`GoBackend`.
- The backend is stateless regarding crypto; it allocates addresses
  (`10.66.66.3+`) and stores peer public keys in SQLite.
- The VPS runs stock `wg-quick`; peers added by the backend operator or a
  future provisioning agent.

## Key files

| File | Purpose |
|---|---|
| `app/src/main/java/com/nexavpn/app/MainActivity.kt` | Compose dashboard: connect button, server list |
| `app/src/main/java/com/nexavpn/app/vpn/TunnelService.kt` | VpnService wrapping GoBackend |
| `app/src/main/java/com/nexavpn/app/vpn/VpnController.kt` | Key generation, start/stop bridge |
| `app/src/main/java/com/nexavpn/app/data/ApiClient.kt` | HTTP client; **edit `DEFAULT_BASE_URL` for deployment** |
| `backend/main.py` | All API endpoints + schema |
| `server/setup-wg-server.sh` | Idempotent VPS bootstrap |

## Verification commands

```bash
# Android
./gradlew :app:assembleDebug

# Backend
cd backend && python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
ruff check .
uvicorn main:app --reload    # smoke test at http://127.0.0.1:8000/docs

# Shell
shellcheck server/*.sh
```

## Known gaps (good first tasks)

See [`SECURITY.md → Known design limitations`](SECURITY.md). In particular,
peer provisioning authentication is the highest-priority hardening task.
