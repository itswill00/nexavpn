#!/usr/bin/env bash
# NexaVPN — WireGuard server bootstrap for Debian/Ubuntu.
# Run as root on a fresh VPS. Idempotent.
set -euo pipefail

WG_IFACE=${WG_IFACE:-wg0}
WG_PORT=${WG_PORT:-51820}
WG_SUBNET=${WG_SUBNET:-10.66.66.0/24}
SERVER_DIR=/etc/wireguard
SERVER_PRIVKEY=$SERVER_DIR/server.key

[[ $EUID -eq 0 ]] || { echo "run as root"; exit 1; }

apt-get update -qq
apt-get install -y -qq wireguard qrencode >/dev/null

# Enable IP forwarding
sysctl -w net.ipv4.ip_forward=1 >/dev/null
grep -q '^net.ipv4.ip_forward=1' /etc/sysctl.conf || echo 'net.ipv4.ip_forward=1' >> /etc/sysctl.conf

# NAT (detect default iface)
DEFAULT_IFACE=$(ip route show default | awk '/default/ {print $5; exit}')
iptables -t nat -C POSTROUTING -s "$WG_SUBNET" -o "$DEFAULT_IFACE" -j MASQUERADE 2>/dev/null || \
  iptables -t nat -A POSTROUTING -s "$WG_SUBNET" -o "$DEFAULT_IFACE" -j MASQUERADE

mkdir -p "$SERVER_DIR"
if [[ ! -f $SERVER_PRIVKEY ]]; then
  umask 077
  wg genkey > "$SERVER_PRIVKEY"
fi
SERVER_PUBKEY=$(wg pubkey < "$SERVER_PRIVKEY")

cat > "$SERVER_DIR/$WG_IFACE.conf" <<EOF
[Interface]
Address = $WG_SUBNET
ListenPort = $WG_PORT
PrivateKey = $(cat "$SERVER_PRIVKEY")
PostUp = iptables -t nat -A POSTROUTING -s $WG_SUBNET -o $DEFAULT_IFACE -j MASQUERADE
PostDown = iptables -t nat -D POSTROUTING -s $WG_SUBNET -o $DEFAULT_IFACE -j MASQUERADE
EOF

systemctl enable "wg-quick@$WG_IFACE" >/dev/null
systemctl restart "wg-quick@$WG_IFACE"

echo
echo "=== NexaVPN server ready ==="
echo "Public key : $SERVER_PUBKEY"
echo "Port       : $WG_PORT/udp"
echo "Subnet     : $WG_SUBNET"
echo
echo "Register this server in the backend 'servers' table with the"
echo "public key above, then clients can provision peers via POST /peers."
