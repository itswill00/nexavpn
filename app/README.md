# :app — Android client

Kotlin + Jetpack Compose VPN client using the official
[`wireguard-android`](https://github.com/WireGuard/wireguard-android) library
(`com.wireguard.android:tunnel`) for the data plane.

## Module layout

```
app/src/main/java/com/nexavpn/app/
├── MainActivity.kt        # Single-activity Compose dashboard
├── NexaApp.kt             # Application subclass
├── data/
│   ├── ApiClient.kt       # Backend HTTP integration (OkHttp + kotlinx.serialization)
│   └── Models.kt          # VpnServer, ServerListResponse, PeerRegistration
├── ui/theme/Theme.kt      # Material 3 light/dark color schemes
└── vpn/
    ├── TunnelService.kt   # VpnService + GoBackend tunnel lifecycle
    └── VpnController.kt   # Keypair generation, start/stop bridge
```

## Building

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17, Android SDK 35.

## Configuration

| What | Where |
|---|---|
| Backend URL | `data/ApiClient.kt` → `DEFAULT_BASE_URL` |
| VPN DNS | `TunnelService` (currently from wg-quick config) |
| Min/target SDK | `app/build.gradle.kts` (26 / 35) |

## Runtime flow

1. `MainActivity` loads servers via `ApiClient.fetchServers()`; falls back to
   a bundled placeholder list when the backend is unreachable.
2. On connect, `VpnController.ensurePeerKeys()` generates a fresh Curve25519
   keypair **on-device**.
3. The client posts the **public key** + chosen server id to `POST /peers`
   and receives a wg-quick config.
4. `TunnelService` parses the config and brings the tunnel up through
   `GoBackend`.

## Current limitations

See the root `README.md` and `SECURITY.md`. Notably: connect flow is
scaffolded (no `VpnService.prepare` consent prompt yet), keys are not
persisted across sessions, and peer provisioning auth is not implemented.
