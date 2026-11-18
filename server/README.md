# server/ — VPS provisioning

Scripts to turn a fresh Debian/Ubuntu VPS into a NexaVPN WireGuard node.

## setup-wg-server.sh

Idempotent bootstrap that:

1. Installs `wireguard` and `qrencode`
2. Enables IPv4 forwarding (`net.ipv4.ip_forward=1`)
3. Generates the server keypair (kept at `/etc/wireguard/server.key`, mode 0600)
4. Configures NAT/MASQUERADE on the default interface
5. Writes `/etc/wireguard/wg0.conf` and enables `wg-quick@wg0`
6. Prints the server **public key** to register in the backend

### Usage

```bash
# from your workstation
ssh root@your-vps 'bash -s' < server/setup-wg-server.sh
```

Environment overrides (before the script runs):

```bash
ssh root@your-vps "WG_PORT=51821 bash -s" < server/setup-wg-server.sh
```

| Variable | Default | Purpose |
|---|---|---|
| `WG_IFACE` | `wg0` | WireGuard interface name |
| `WG_PORT` | `51820` | UDP listen port |
| `WG_SUBNET` | `10.66.66.0/24` | Tunnel subnet (must match backend `NEXA_SUBNET`) |

### Registering the node with the backend

```sql
INSERT INTO servers(id, country, country_code, city, hostname,
                    public_key, endpoint_port, premium)
VALUES ('vps-1', 'Germany', 'DE', 'Frankfurt',
        'your-vps.example.com', '<PUBKEY>', 51820, 0);
```

## Security notes

- The private key lives only on the VPS (`/etc/wireguard/server.key`).
- Rotate server keys by deleting the key file and re-running the script, then
  updating the backend row.
- This script does **not** harden SSH, configure fail2ban, or manage firewall
  rules beyond the NAT line — treat standard VPS hardening as an operator
  responsibility.
