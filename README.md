# NexaVPN

Clean-room VPN client + backend inspired by the architecture observed in a
commercial VPN app (WireGuard core, server-list API, one-tap dashboard).
No code, assets, keys or branding were copied from the original app.

## Layout

```
app/       Android client (Kotlin + Jetpack Compose, wireguard-android library)
backend/   FastAPI service: server list + peer provisioning (SQLite)
server/    WireGuard VPS bootstrap script
```

## Quick start

### 1. Provision a VPS

```bash
ssh root@your-vps 'bash -s' < server/setup-wg-server.sh
# note the printed public key
```

### 2. Register the server in the backend

```sql
INSERT INTO servers(id, country, country_code, city, hostname,
                    public_key, endpoint_port, premium)
VALUES ('vps-1','Germany','DE','Frankfurt',
        'your-vps.example.com','<PUBKEY>',51820,0);
```

### 3. Run the backend

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 4. Build the app

Open the project in Android Studio (or `./gradlew :app:assembleDebug`) and
point `ApiClient.DEFAULT_BASE_URL` at your backend.

## Security notes

- Private keys are generated **on-device** (`KeyPair()` from wireguard-android's
  crypto) and never leave the device; only the public key is sent to the
  backend for provisioning.
- The sample `PeerResponse.config` contains a placeholder — the client must
  substitute its own private key locally. Harden this before any real use.
- Add authentication (device attestation / tokens) to /peers before exposing
  it publicly, otherwise anyone can allocate peer slots.

## Status

Scaffold / MVP. Not production-ready: see TODOs in
`TunnelService.kt` and `MainActivity.kt`.
