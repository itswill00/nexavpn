# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
