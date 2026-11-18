# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Split tunneling: per-app exclusion list with searchable picker
- Kill switch: auto-reconnect on unexpected tunnel drop; deep-link to
  system always-on VPN for hard enforcement
- Settings screen: kill switch, auto-connect, split tunneling entry
- Latency probing: concurrent TCP probe per server, color-coded badge,
  auto-select fastest reachable server

### Fixed

- WireGuard library coordinates: `com.wireguard.android:tunnel:1.0.20250531`
  (Maven Central date-versioned; the previously referenced 1.1.1 never existed)
- DataStore `edit` receiver usage, `Key.fromBase64` API usage,
  `LatencyProber` coroutine `associate` pattern, duplicate imports,
  VPN settings intent action

### Build

- First successful `assembleDebug` APK (v2-signed, verified via REA)
- Local toolchain: JDK 17, Android SDK 35, Gradle 8.9 wrapper


### Changed

- **Client**: replaced custom `TunnelService` with the library-managed
  `GoBackend` + dedicated state owner `VpnManager` (StateFlow-driven UI)
- **Client**: added `VpnService.prepare` consent flow via ActivityResult API
- **Client**: device keypair now persisted in DataStore (`KeysStore`)
- **Backend**: `POST /peers` now returns a structured peer lease
  (address, server key, endpoint, DNS) instead of a raw config with a
  placeholder private key
- **Backend**: bearer-token auth (`NEXA_API_TOKEN`) on provisioning
  endpoints; disabled only when unset (local dev)

### Added

- Adaptive launcher icon (vector, monochrome-aware)
- Connect flow wired end-to-end: consent → provisioning → tunnel UP

## [0.1.0] - 2026-11-19

### Added

- **Android client** (`app/`)
  - Jetpack Compose dashboard with one-tap connect/disconnect flow
  - Server list UI with country/city rows and PRO badge placeholder
  - Light/dark theme (`ui/theme/Theme.kt`)
  - `TunnelService` — `VpnService` wrapper around the official
    `wireguard-android` `GoBackend`
  - `VpnController` — on-device WireGuard keypair generation, start/stop bridge
  - `ApiClient` — backend integration (`GET /servers`, `POST /peers`) with
    bundled fallback server list
- **Backend** (`backend/`)
  - FastAPI service with `GET /health`, `GET /servers`, `POST /peers`,
    `DELETE /peers/{public_key}`
  - SQLite persistence (servers, peers with allocated addresses)
  - Sequential address allocation from `10.66.66.0/24`
- **Server provisioning** (`server/`)
  - Idempotent WireGuard bootstrap script for Debian/Ubuntu:
    key generation, IP forwarding, NAT/MASQUERADE, `wg-quick` systemd unit
- **Repository hygiene**
  - `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `AGENTS.md`,
    `CHANGELOG.md`, MIT `LICENSE`, `.gitignore`

### Security

- Client generates keypairs locally; private keys never leave the device
- Documented known gaps in `SECURITY.md` (peer auth, rate limiting) as
  pre-deployment hardening requirements

[Unreleased]: https://github.com/itswill00/nexavpn/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/itswill00/nexavpn/releases/tag/v0.1.0
