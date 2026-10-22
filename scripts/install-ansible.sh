#!/usr/bin/env bash

###############################################################################
# Ansible Vault - User Local Setup
#
# No sudo / no root required
#
# Installs:
#   - Ansible
#   - ansible-playbook
#   - ansible-vault
#
# Location:
#   ~/.local/venvs/ansible
#   ~/.local/bin
###############################################################################

set -Eeuo pipefail

###############################################################################
# Configuration
###############################################################################

readonly HOME_DIR="${HOME:?HOME is not set}"

readonly LOCAL_DIR="$HOME_DIR/.local"
readonly BIN_DIR="$LOCAL_DIR/bin"
readonly VENV_DIR="$LOCAL_DIR/venvs/ansible"

readonly ANSIBLE_CONFIG_DIR="$HOME_DIR/.ansible"
readonly VAULT_PASSWORD_FILE="$ANSIBLE_CONFIG_DIR/vault_password"

readonly LOG_FILE="/tmp/ansible-vault-setup-$(date +%Y%m%d-%H%M%S).log"

###############################################################################
# Colors
###############################################################################

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

###############################################################################
# Logging
###############################################################################

log() {
    echo -e "${BLUE}[INFO]${NC} $*" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}[OK]${NC} $*" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}[WARN]${NC} $*" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $*" | tee -a "$LOG_FILE"
}

###############################################################################
# Error handling
###############################################################################

handle_error() {
    local exit_code=$?

    error "Command failed: ${BASH_COMMAND}"
    error "Line: ${BASH_LINENO[0]}"
    error "Exit code: ${exit_code}"
    error "Log file: ${LOG_FILE}"

    exit "$exit_code"
}

trap handle_error ERR

###############################################################################
# Cleanup
###############################################################################

cleanup() {
    if [[ -n "${TMP_DIR:-}" && -d "${TMP_DIR:-}" ]]; then
        rm -rf "$TMP_DIR"
    fi
}

trap cleanup EXIT

###############################################################################
# Check dependencies
###############################################################################

check_dependencies() {

    log "Checking dependencies..."

    if ! command -v python3 >/dev/null 2>&1; then
        error "Python 3 is required but was not found."
        error "Ask your administrator to install Python 3."
        exit 1
    fi

    if ! command -v python3 -m venv >/dev/null 2>&1; then
        warning "Python venv support may not be installed."
    fi

    if ! command -v curl >/dev/null 2>&1; then
        error "curl is required but was not found."
        exit 1
    fi

    success "Required dependencies found."
}

###############################################################################
# Create directories
###############################################################################

create_directories() {

    log "Creating user-local directories..."

    mkdir -p "$BIN_DIR"
    mkdir -p "$LOCAL_DIR/venvs"
    mkdir -p "$ANSIBLE_CONFIG_DIR"

    chmod 700 "$ANSIBLE_CONFIG_DIR"

    success "Directories created."
}

###############################################################################
# Create Python virtual environment
###############################################################################

create_virtualenv() {

    if [[ -d "$VENV_DIR" ]]; then
        log "Ansible virtual environment already exists."
        return
    fi

    log "Creating Ansible virtual environment..."

    python3 -m venv "$VENV_DIR"

    success "Virtual environment created."
}

###############################################################################
# Install Ansible
###############################################################################

install_ansible() {

    local pip="$VENV_DIR/bin/pip"

    log "Upgrading pip..."

    "$pip" install --upgrade pip

    log "Installing Ansible..."

    "$pip" install --upgrade ansible

    success "Ansible installed."
}

###############################################################################
# Create symlinks
###############################################################################

create_links() {

    log "Creating user-local Ansible commands..."

    local commands=(
        ansible
        ansible-playbook
        ansible-vault
        ansible-galaxy
        ansible-inventory
        ansible-inv
        ansible-config
        ansible-doc
        ansible-console
        ansible-pull
    )

    for command in "${commands[@]}"; do

        if [[ -x "$VENV_DIR/bin/$command" ]]; then

            ln -sf \
                "$VENV_DIR/bin/$command" \
                "$BIN_DIR/$command"

        fi

    done

    success "Ansible commands linked to $BIN_DIR."
}

###############################################################################
# Configure PATH
###############################################################################

configure_path() {

    log "Configuring PATH..."

    local shell_rc=""

    case "${SHELL:-}" in

        */bash)
            shell_rc="$HOME_DIR/.bashrc"
            ;;

        */zsh)
            shell_rc="$HOME_DIR/.zshrc"
            ;;

        *)
            warning "Could not determine shell."
            warning "Add this manually to your shell:"
            echo
            echo 'export PATH="$HOME/.local/bin:$PATH"'
            echo
            return
            ;;
    esac

    touch "$shell_rc"

    if ! grep -Fq 'export PATH="$HOME/.local/bin:$PATH"' "$shell_rc"; then

        cat >> "$shell_rc" <<'EOF'

# User-local applications
export PATH="$HOME/.local/bin:$PATH"
EOF

    fi

    export PATH="$BIN_DIR:$PATH"

    success "PATH configured in $shell_rc."
}

###############################################################################
# Generate Vault password
###############################################################################

setup_vault_password() {

    if [[ -f "$VAULT_PASSWORD_FILE" ]]; then

        warning "Vault password file already exists:"
        warning "$VAULT_PASSWORD_FILE"

        return
    fi

    log "Generating Ansible Vault password..."

    umask 077

    python3 - <<'PY' > "$VAULT_PASSWORD_FILE"
import secrets
import string

alphabet = string.ascii_letters + string.digits + string.punctuation

password = ''.join(
    secrets.choice(alphabet)
    for _ in range(48)
)

print(password)
PY

    chmod 600 "$VAULT_PASSWORD_FILE"

    success "Vault password generated."
    success "Stored securely at:"
    echo "  $VAULT_PASSWORD_FILE"
}

###############################################################################
# Configure ansible.cfg
###############################################################################

configure_ansible() {

    local config_file="$ANSIBLE_CONFIG_DIR/ansible.cfg"

    log "Creating Ansible configuration..."

    cat > "$config_file" <<EOF
[defaults]
vault_password_file = $VAULT_PASSWORD_FILE
host_key_checking = False
retry_files_enabled = False
EOF

    chmod 600 "$config_file"

    success "Ansible configuration created:"
    echo "  $config_file"
}

###############################################################################
# Verify installation
###############################################################################

verify_installation() {

    export PATH="$BIN_DIR:$PATH"

    log "Verifying Ansible..."

    if ! command -v ansible >/dev/null 2>&1; then
        error "ansible command not found."
        exit 1
    fi

    if ! command -v ansible-vault >/dev/null 2>&1; then
        error "ansible-vault command not found."
        exit 1
    fi

    success "Ansible:"
    ansible --version | head -n 1

    success "Ansible Vault:"
    ansible-vault --version | head -n 1

    success "Ansible Vault is ready."
}

###############################################################################
# Test Vault
###############################################################################

test_vault() {

    log "Testing Ansible Vault..."

    local test_file
    test_file="$(mktemp)"

    echo "ansible_vault_test: true" > "$test_file"

    "$BIN_DIR/ansible-vault" encrypt "$test_file"

    if ! grep -q '^\$ANSIBLE_VAULT' "$test_file"; then
        rm -f "$test_file"
        error "Vault encryption test failed."
        exit 1
    fi

    "$BIN_DIR/ansible-vault" decrypt "$test_file"

    if ! grep -q 'ansible_vault_test: true' "$test_file"; then
        rm -f "$test_file"
        error "Vault decryption test failed."
        exit 1
    fi

    rm -f "$test_file"

    success "Vault encryption/decryption test passed."
}

###############################################################################
# Summary
###############################################################################

print_summary() {

    echo
    echo "============================================================"
    echo " Ansible Vault Setup Complete"
    echo "============================================================"
    echo
    echo "Ansible:"
    echo "  $BIN_DIR/ansible"
    echo
    echo "Ansible Vault:"
    echo "  $BIN_DIR/ansible-vault"
    echo
    echo "Vault password:"
    echo "  $VAULT_PASSWORD_FILE"
    echo
    echo "Ansible configuration:"
    echo "  $ANSIBLE_CONFIG_DIR/ansible.cfg"
    echo
    echo "Python environment:"
    echo "  $VENV_DIR"
    echo
    echo "============================================================"
    echo
    echo "Reload your shell:"
    echo
    echo "  source ~/.bashrc"
    echo
    echo "Then verify:"
    echo
    echo "  ansible --version"
    echo "  ansible-vault --version"
    echo
    echo "============================================================"
}

###############################################################################
# Main
###############################################################################

main() {

    log "Starting Ansible Vault setup..."

    check_dependencies
    create_directories
    create_virtualenv
    install_ansible
    create_links
    configure_path
    setup_vault_password
    configure_ansible
    verify_installation
    test_vault
    print_summary

    success "Setup completed successfully."
}

main "$@"