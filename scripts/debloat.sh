#!/usr/bin/env bash
# ArizenOS Lite — debloat.sh
# Standalone debloat script (can run independently on mounted system)
set -euo pipefail

SYSTEM_MNT="${1:-$(pwd)/work/system_mount}"
CONFIG_DIR="$(pwd)/config"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS Debloat]${NC} $*"; }
ok()   { echo -e "${GREEN}[REMOVED]${NC} $*"; }
skip() { echo -e "${YELLOW}[SKIP]${NC} $*"; }

[[ ! -d "$SYSTEM_MNT/app" ]] && { echo "System not mounted at $SYSTEM_MNT"; exit 1; }

DEBLOAT_LIST="$CONFIG_DIR/debloat_list.txt"
[[ ! -f "$DEBLOAT_LIST" ]] && { echo "debloat_list.txt not found"; exit 1; }

log "Starting Samsung debloat for ArizenOS Lite..."
REMOVED=0; SKIPPED=0

while IFS= read -r pkg; do
    [[ -z "$pkg" || "$pkg" == \#* ]] && continue
    if [[ -d "$SYSTEM_MNT/app/$pkg" ]]; then
        rm -rf "$SYSTEM_MNT/app/$pkg"
        ok "$pkg"
        ((REMOVED++))
    elif [[ -d "$SYSTEM_MNT/priv-app/$pkg" ]]; then
        rm -rf "$SYSTEM_MNT/priv-app/$pkg"
        ok "$pkg (priv-app)"
        ((REMOVED++))
    else
        skip "$pkg (not found)"
        ((SKIPPED++))
    fi
done < "$DEBLOAT_LIST"

log "Done — Removed: $REMOVED | Skipped: $SKIPPED"
