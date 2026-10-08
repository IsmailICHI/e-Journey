
# version 1

# mkdir -p ~/.local/bin

# curl -L https://dl.filippo.io/mkcert/latest?for=linux/amd64 \
#   -o ~/.local/bin/mkcert

# chmod +x ~/.local/bin/mkcert

# ~/.local/bin/mkcert --version

# ## install the local CA in the system trust store (requires root privileges)
# export CAROOT="$HOME/.local/share/mkcert"
# mkdir -p "$CAROOT"

# ~/.local/bin/mkcert -install

## version 2
#!/usr/bin/env bash

set -euo pipefail

# ============================================================
# Neo4flix - Local HTTPS Setup
#
# Result:
#   https://localhost:4443
#
# Requirements:
#   - Linux
#   - curl
#   - certutil
#   - Docker
#
# No sudo required.
# ============================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

LOCAL_BIN="$HOME/.local/bin"
MKCERT="$LOCAL_BIN/mkcert"
CAROOT="$HOME/.local/share/mkcert"

CERT_DIR="$PROJECT_DIR/front-end/certs"

CERT_FILE="$CERT_DIR/neo4flix.pem"
KEY_FILE="$CERT_DIR/neo4flix-key.pem"

NSS_DB="$HOME/.pki/nssdb"

echo "=============================================="
echo " Neo4flix Local HTTPS Setup"
echo "=============================================="
echo

# ------------------------------------------------------------
# 1. Check required commands
# ------------------------------------------------------------

echo "[1/7] Checking dependencies..."

if ! command -v curl >/dev/null 2>&1; then
    echo "ERROR: curl is required."
    exit 1
fi

if ! command -v certutil >/dev/null 2>&1; then
    echo "ERROR: certutil is required."
    echo
    echo "Install the NSS tools package for your Linux distribution."
    exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: docker is required."
    exit 1
fi

echo "Dependencies OK."
echo


# ------------------------------------------------------------
# 2. Install mkcert locally if necessary
# ------------------------------------------------------------

echo "[2/7] Checking mkcert..."

mkdir -p "$LOCAL_BIN"

if [ ! -x "$MKCERT" ]; then

    ARCH="$(uname -m)"

    case "$ARCH" in
        x86_64)
            MKCERT_URL="https://dl.filippo.io/mkcert/latest?for=linux/amd64"
            ;;

        aarch64|arm64)
            MKCERT_URL="https://dl.filippo.io/mkcert/latest?for=linux/arm64"
            ;;

        *)
            echo "ERROR: Unsupported architecture: $ARCH"
            exit 1
            ;;
    esac

    echo "mkcert not found."
    echo "Downloading mkcert..."

    curl -fL "$MKCERT_URL" -o "$MKCERT"

    chmod +x "$MKCERT"

    echo "mkcert installed at:"
    echo "  $MKCERT"
else
    echo "mkcert already installed."
fi

echo


# ------------------------------------------------------------
# 3. Configure mkcert CA location
# ------------------------------------------------------------

echo "[3/7] Configuring local Certificate Authority..."

export CAROOT

mkdir -p "$CAROOT"

if [ ! -f "$CAROOT/rootCA.pem" ]; then

    echo "Creating local CA..."

    "$MKCERT" -install

else

    echo "Local CA already exists."

fi

if [ ! -f "$CAROOT/rootCA.pem" ]; then
    echo "ERROR: rootCA.pem was not created."
    exit 1
fi

echo "CA:"
echo "  $CAROOT/rootCA.pem"
echo


# ------------------------------------------------------------
# 4. Create certificate
# ------------------------------------------------------------

echo "[4/7] Creating localhost certificate..."

mkdir -p "$CERT_DIR"

if [ ! -f "$CERT_FILE" ] || [ ! -f "$KEY_FILE" ]; then

    "$MKCERT" \
        -cert-file "$CERT_FILE" \
        -key-file "$KEY_FILE" \
        localhost \
        127.0.0.1 \
        ::1

    echo "Certificate created."

else

    echo "Certificate already exists."

fi

chmod 644 "$CERT_FILE"
chmod 600 "$KEY_FILE"

echo
echo "Certificate:"
echo "  $CERT_FILE"

echo "Private key:"
echo "  $KEY_FILE"

echo


# ------------------------------------------------------------
# 5. Configure Chrome NSS trust
# ------------------------------------------------------------

echo "[5/7] Configuring Chrome trust..."

mkdir -p "$NSS_DB"

# Check whether the CA already exists.
if certutil -L -d "sql:$NSS_DB" 2>/dev/null \
    | grep -q "Neo4flix Local CA"; then

    echo "Neo4flix Local CA is already trusted by Chrome."

else

    echo "Adding Neo4flix Local CA to Chrome..."

    certutil -A \
        -n "Neo4flix Local CA" \
        -t "C,," \
        -i "$CAROOT/rootCA.pem" \
        -d "sql:$NSS_DB"

    echo "Chrome trust configured."

fi

echo


# ------------------------------------------------------------
# 6. Show Docker Compose changes
# ------------------------------------------------------------

echo "[6/7] Docker Compose configuration"
echo

echo "Your frontend service should expose both ports:"
echo

cat <<'EOF'
    ports:
      - "${FRONTEND_PORT:-4200}:80"
      - "${FRONTEND_HTTPS_PORT:-4443}:443"
EOF

echo


# ------------------------------------------------------------
# 7. Final instructions
# ------------------------------------------------------------

echo "[7/7] Setup complete."
echo

echo "=============================================="
echo " HTTPS setup completed"
echo "=============================================="
echo
echo "Frontend URL:"
echo "  https://localhost:4443"
echo
echo "HTTP URL:"
echo "  http://localhost:4200"
echo
echo "Certificate:"
echo "  $CERT_FILE"
echo
echo "CA:"
echo "  $CAROOT/rootCA.pem"
echo
echo "IMPORTANT:"
echo "  Do NOT commit front-end/certs/"
echo "  Do NOT commit rootCA-key.pem"
echo
echo "If Chrome was running during this setup,"
echo "completely close Chrome and start it again."
echo
echo "Next:"
echo "  Update nginx.conf to listen on 443"
echo "  Update Dockerfile to copy the certificates"
echo "  Expose 443 in docker-compose.yml"
echo


## This command to start Chrome with the custom user data directory and host resolver rules is commented out, 
## but you can use it if needed.

# /usr/bin/google-chrome \
#   --user-data-dir="$HOME/.config/google-chrome-neo4flix" \
#   --host-resolver-rules="MAP neo4flix.local 127.0.0.1"

