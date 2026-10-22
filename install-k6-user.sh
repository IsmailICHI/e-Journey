#!/usr/bin/env bash
set -euo pipefail

# Install only into directories owned by the current user.
INSTALL_DIR="$HOME/.local/opt/k6"
BIN_DIR="$HOME/.local/bin"
ARCHIVE_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$ARCHIVE_DIR"
}
trap cleanup EXIT

case "$(uname -m)" in
  x86_64)
    K6_ARCH="amd64"
    ;;
  aarch64|arm64)
    K6_ARCH="arm64"
    ;;
  *)
    echo "Unsupported CPU architecture: $(uname -m)" >&2
    exit 1
    ;;
esac

mkdir -p "$INSTALL_DIR" "$BIN_DIR"

echo "Resolving the latest stable k6 release..."
API_URL="https://api.github.com/repos/grafana/k6/releases/latest"

if command -v curl >/dev/null 2>&1; then
  JSON="$(curl -fsSL "$API_URL")"
elif command -v wget >/dev/null 2>&1; then
  JSON="$(wget -qO- "$API_URL")"
else
  echo "Install curl or wget first; no sudo will be used." >&2
  exit 1
fi

VERSION="$(
  printf '%s' "$JSON" |
    grep -m1 '"tag_name":' |
    sed -E 's/.*"tag_name":[[:space:]]*"([^"]+)".*/\1/'
)"

if [[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Could not determine a valid stable k6 version." >&2
  exit 1
fi

ARCHIVE="k6-${VERSION#v}-linux-${K6_ARCH}.tar.gz"
URL="https://github.com/grafana/k6/releases/download/${VERSION}/${ARCHIVE}"

echo "Downloading k6 ${VERSION} for Linux ${K6_ARCH}..."

if command -v curl >/dev/null 2>&1; then
  curl -fL "$URL" -o "$ARCHIVE_DIR/$ARCHIVE"
else
  wget -O "$ARCHIVE_DIR/$ARCHIVE" "$URL"
fi

tar -xzf "$ARCHIVE_DIR/$ARCHIVE" -C "$ARCHIVE_DIR"

BINARY="$ARCHIVE_DIR/k6-${VERSION#v}-linux-${K6_ARCH}/k6"
if [[ ! -f "$BINARY" ]]; then
  echo "The expected k6 binary was not found in the archive." >&2
  exit 1
fi

install -m 0755 "$BINARY" "$INSTALL_DIR/k6"
ln -sfn "$INSTALL_DIR/k6" "$BIN_DIR/k6"

echo
echo "Installed k6 at: $INSTALL_DIR/k6"
echo "Binary link:      $BIN_DIR/k6"
echo
"$INSTALL_DIR/k6" version

if [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
  echo
  echo "Add this to your ~/.bashrc:"
  echo 'export PATH="$HOME/.local/bin:$PATH"'
  echo "Then run: source ~/.bashrc"
fi
