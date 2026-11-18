# Security Policy

## Supported versions

NexaVPN is in an early scaffold stage. Only the latest `main` branch receives
security fixes.

## Reporting a vulnerability

**Do not report security vulnerabilities through public GitHub issues.**

Instead, please report them privately via
[GitHub Security Advisories](https://github.com/itswill00/nexavpn/security/advisories/new)
("Report a vulnerability").

Include:

- Component and affected file(s)
- Step-by-step reproduction or PoC
- Impact assessment
- Suggested fix (optional)

You will receive an acknowledgment within **72 hours**. We aim to release
fixes within 30 days for high-severity issues, and will credit reporters in
the release notes unless anonymity is requested.

## Known design limitations (read before deploying)

This project is a reference implementation, **not a hardened production VPN**.
The following gaps are intentional scaffolding choices and must be addressed
before any real deployment:

| Area | Status | Required action |
|---|---|---|
| Peer provisioning auth | ⚠️ Missing | `/peers` accepts any public key. Add device attestation or signed tokens before exposing publicly. |
| Key handling | ⚠️ Partial | The backend's sample config contains a placeholder private key field; the client must inject its own key locally and **never** transmit it. |
| Rate limiting | ⚠️ Missing | Add per-IP/per-device limits to prevent peer-slot exhaustion. |
| Transport security | ⚠️ Config-dependent | The client's `ApiClient.DEFAULT_BASE_URL` must point to an HTTPS deployment with valid certificates. |
| WireGuard port hardening | ⚠️ Operator task | Restrict UDP 51820 sources if desired; rotate server keys periodically. |
| Logging | ✅ By design | Client and server keep no traffic logs. Backend stores only public keys and allocated IPs. |

## Scope

In scope: code in `app/`, `backend/`, `server/`, and the default
configuration shipped in this repository.

Out of scope: the WireGuard protocol itself, wireguard-go/kernel
implementations, Android OS, or the FastAPI runtime. Report those upstream.
