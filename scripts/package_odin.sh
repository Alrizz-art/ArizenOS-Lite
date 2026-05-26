#!/usr/bin/env bash
# ArizenOS Lite — package_odin.sh
# Packages the modified AP into Odin-flashable AP.tar.md5
set -euo pipefail

WORK_DIR="$(pwd)/work"
AP_EXTRACTED="$WORK_DIR/ap_extracted"
OUTPUT_DIR="$(pwd)/output"
VERSION="v1"
DEVICE="SM-T295"
OUTPUT_NAME="ArizenOSLite_${VERSION}_${DEVICE}_AP.tar.md5"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

log "ArizenOS Lite — Odin Packager"
log "Output: $OUTPUT_NAME"

mkdir -p "$OUTPUT_DIR"

# Create tar without MD5 first
log "Creating AP tar archive..."
cd "$AP_EXTRACTED"
tar -cf "$OUTPUT_DIR/AP_temp.tar" --format=gnu \
    $(ls *.img *.lz4 2>/dev/null | tr '\n' ' ') 2>/dev/null || \
tar -cf "$OUTPUT_DIR/AP_temp.tar" --format=gnu .
ok "Tar created"

# Generate and append MD5
log "Generating MD5 checksum..."
./scripts/generate_md5.sh "$OUTPUT_DIR/AP_temp.tar" "$OUTPUT_DIR/$OUTPUT_NAME"

ok ""
ok "Odin package ready: output/$OUTPUT_NAME"
log "Flash via Odin:"
log "  1. Boot into Download Mode (Power + Vol Down + Bixby)"
log "  2. Open Odin on PC"
log "  3. Select AP → output/$OUTPUT_NAME"
log "  4. Click Start"
