#!/usr/bin/env bash

set -euo pipefail

# ============================================================
# Travel-Plan - Local HTTPS Certificate Setup
#
# Architecture:
#   Browser -> HTTPS :8443 -> Vagrant VM -> Traefik
#   Traefik -> HTTP -> frontend / gateway
#
# Public URL:
#   https://travel-plan.local:8443
#
# Certificate files are stored outside the repository.
# Kubernetes TLS Secret and Ingress are configured separately.
# ============================================================

LOCAL_BIN="$HOME/.local/bin"
MKCERT="$LOCAL_BIN/mkcert"
CAROOT="$HOME/.local/share/mkcert"

CERT_DIR="$HOME/.local/share/travel-plan/certs"
CERT_FILE="$CERT_DIR/travel-plan.pem"
KEY_FILE="$CERT_DIR/travel-plan-key.pem"

NSS_DB="$HOME/.pki/nssdb"
NSS_CA_NAME="travel-plan Local CA"

echo "=============================================="
echo " Travel-Plan HTTPS Certificate Setup"
echo "=============================================="
echo

# ------------------------------------------------------------
# 1. Check dependencies
# ------------------------------------------------------------

echo "[1/6] Checking dependencies..."

command -v curl >/dev/null 2>&1 || {
    echo "ERROR: curl is required."
    exit 1
}

if command -v certutil >/dev/null 2>&1; then
    HAS_CERTUTIL=true
else
    HAS_CERTUTIL=false
    echo "WARNING: certutil not found."
    echo "Chrome/NSS trust configuration may require additional setup."
fi

echo "Dependencies checked."
echo

# ------------------------------------------------------------
# 2. Install mkcert locally if necessary
# ------------------------------------------------------------

echo "[2/6] Checking mkcert..."

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

    TMP_FILE="$(mktemp)"
    trap 'rm -f "${TMP_FILE:-}"' EXIT

    curl -fL "$MKCERT_URL" -o "$TMP_FILE"
    chmod 0755 "$TMP_FILE"
    mv "$TMP_FILE" "$MKCERT"
    trap - EXIT
fi

"$MKCERT" -version
echo

# ------------------------------------------------------------
# 3. Preserve and initialize the local CA
# ------------------------------------------------------------

echo "[3/6] Configuring the local CA..."

export CAROOT
mkdir -p "$CAROOT"

# Check if the CA files are missing
if [ ! -f "$CAROOT/rootCA.pem" ] || [ ! -f "$CAROOT/rootCA-key.pem" ]; then
    echo "No valid CA found. Bootstrapping a new non-root local CA..."
    
    # TRUST_STORES="" forces mkcert to only create files in $CAROOT 
    # and completely skips asking for a sudo password.
    TRUST_STORES="" "$MKCERT" -install
else
    echo "Existing CA found. Keeping it."
fi

echo "CA directory: $CAROOT"
echo



# ------------------------------------------------------------
# 4. Generate the application certificate
# ------------------------------------------------------------

echo "[4/6] Generating the Travel-Plan certificate..."

mkdir -p "$CERT_DIR"
chmod 0700 "$CERT_DIR"

# Always regenerate so the certificate includes the correct SANs.
# This does not recreate or replace the CA.
"$MKCERT" \
    -cert-file "$CERT_FILE" \
    -key-file "$KEY_FILE" \
    travel-plan.local \
    localhost \
    127.0.0.1 \
    ::1

chmod 0644 "$CERT_FILE"
chmod 0600 "$KEY_FILE"

echo
echo "Certificate: $CERT_FILE"
echo "Private key: $KEY_FILE"
echo

# ------------------------------------------------------------
# 5. Configure Chrome/NSS trust when available
# ------------------------------------------------------------

echo "[5/6] Configuring browser trust..."

if [ "$HAS_CERTUTIL" = true ]; then
    mkdir -p "$NSS_DB"

    if [ ! -f "$NSS_DB/cert9.db" ]; then
        certutil -N --empty-password -d "sql:$NSS_DB"
    fi

    if certutil -L -d "sql:$NSS_DB" 2>/dev/null |
       grep -Fq "$NSS_CA_NAME"; then

        echo "CA nickname already exists in NSS."
    else
        certutil -A \
            -n "$NSS_CA_NAME" \
            -t "C,," \
            -i "$CAROOT/rootCA.pem" \
            -d "sql:$NSS_DB"

        echo "CA added to NSS."
    fi
else
    echo "Skipped NSS configuration because certutil is unavailable."
fi

echo

# ------------------------------------------------------------
# 6. Verify the generated certificate
# ------------------------------------------------------------

echo "[6/6] Verifying the certificate..."

if ! openssl x509 -in "$CERT_FILE" -noout -subject -issuer -dates; then
    echo "ERROR: Certificate verification failed."
    exit 1
fi

echo
echo "=============================================="
echo " Certificate setup completed"
echo "=============================================="
echo
echo "Application URL:"
echo "  https://travel-plan.local:8443"
echo
echo "Certificate:"
echo "  $CERT_FILE"
echo
echo "Private key:"
echo "  $KEY_FILE"
echo
echo "CA certificate:"
echo "  $CAROOT/rootCA.pem"
echo
echo "IMPORTANT:"
echo "  Never commit the private key or CA private key."
echo "  Do not copy rootCA-key.pem into Kubernetes."
echo "  Keep the certificate files outside Git."
echo
echo "NEXT STEPS:"
echo "  1. Map travel-plan.local to 127.0.0.1 on the host."
echo "  2. Create Kubernetes TLS Secret travel-plan-local-tls."
echo "  3. Update the Ingress to use websecure and TLS."
echo "  4. Test HTTPS through Traefik."
echo

