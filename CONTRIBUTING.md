# Contribution Guide

Thanks for your interest in contributing to NexaVPN! This document describes
how to set up a development environment, the project conventions, and the
process for submitting changes.

## Code of Conduct

By participating in this project you agree to keep discussions respectful and
on-topic. Report unacceptable behavior by opening a private contact with the
maintainer.

## Project overview

NexaVPN is a **clean-room, full-stack reference implementation** of a modern
VPN product:

| Component | Path | Stack |
|---|---|---|
| Android client | [`app/`](app/) | Kotlin, Jetpack Compose, wireguard-android |
| REST API backend | [`backend/`](backend/) | Python, FastAPI, SQLite |
| VPS provisioning | [`server/`](server/) | Bash, WireGuard, iptables |

The architecture was inspired by (not copied from) a commercial VPN app. All
code in this repository is original.

## Getting started

### Prerequisites

- **Client:** Android Studio Ladybug+ (or Android SDK 35, JDK 17)
- **Backend:** Python 3.11+
- **Server script:** a Debian/Ubuntu VPS with root SSH access

### Client

```bash
# CLI build
./gradlew :app:assembleDebug

# Output APK
app/build/outputs/apk/debug/app-debug.apk
```

Open the project root in Android Studio for the full IDE experience.

### Backend

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload
```

Interactive API docs are served at `http://127.0.0.1:8000/docs`.

### Server

```bash
ssh root@your-vps 'bash -s' < server/setup-wg-server.sh
```

## Repository conventions

### Branching

- `main` — stable, always buildable
- `feat/<short-name>` — feature branches
- `fix/<short-name>` — bug fixes
- `docs/<topic>` — documentation-only changes

### Commit messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(app): add split tunneling UI
fix(backend): reject duplicate peer registration
docs: expand server provisioning guide
chore: bump wireguard-android to 1.1.1
```

### Code style

- **Kotlin:** official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html);
  ktlint-compatible formatting, 4-space indent
- **Python:** PEP 8, formatted with `ruff format`
- **Bash:** `shellcheck` clean, `set -euo pipefail` in every script
- **Commits:** imperative mood, subject ≤ 72 chars, body wrapped at 100

## Submitting changes

1. Fork (or branch directly if you have write access)
2. Create a topic branch from `main`
3. Make your change, including tests where applicable
4. Ensure the project builds cleanly (`./gradlew assembleDebug`,
   `ruff check backend`)
5. Open a Pull Request describing **what** and **why**, linking any related
   issue

### Pull request checklist

- [ ] Builds without warnings introduced by this change
- [ ] No secrets, API keys, or personal endpoints committed
- [ ] Documentation updated (`README.md`, module-level docs)
- [ ] Commit messages follow Conventional Commits

## Reporting issues

Open a GitHub issue with:

- Component (`app` / `backend` / `server`)
- Steps to reproduce
- Expected vs actual behavior
- Log excerpts (redact IPs, keys, and hostnames)

## Security

**Do not open public issues for security vulnerabilities.** See
[`SECURITY.md`](SECURITY.md) for private disclosure instructions.
