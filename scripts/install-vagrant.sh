#!/bin/bash

# Install Vagrant locally without sudo
# Downloads Vagrant binary to ~/.local/bin

set -e

DOWNLOAD_URL="https://releases.hashicorp.com/vagrant/2.4.9/vagrant_2.4.9_linux_amd64.zip"
INSTALL_DIR="$HOME/.local/bin"
TEMP_DIR=$(mktemp -d)

echo "Installing Vagrant 2.4.9..."

# Create install directory
mkdir -p "$INSTALL_DIR"

# Download and extract
cd "$TEMP_DIR"
curl -L "$DOWNLOAD_URL" -o vagrant.zip
unzip vagrant.zip

# Move binary to install directory
mv vagrant "$INSTALL_DIR/"

# Clean up
cd - > /dev/null
rm -rf "$TEMP_DIR"

# Add to PATH if not already there
if ! grep -q "$INSTALL_DIR" ~/.zshrc; then
    echo "export PATH=\"$INSTALL_DIR:\$PATH\"" >> ~/.zshrc
fi

echo "✅ Vagrant installed successfully!"
echo "Restart your terminal or run: source ~/.zshrc"
echo "Verify installation: vagrant --version"