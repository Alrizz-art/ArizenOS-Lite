#!/usr/bin/env bash
# ArizenOS Lite — extract_firmware.sh
# Extracts Samsung firmware zip into working directories
set -euo pipefail

FIRMWARE_ZIP="${1:-}"
WORK_DIR="$(pwd)/work"
FIRMWARE_DIR="$WORK_DIR/firmware_raw"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ -z "$FIRMWARE_ZIP" ]] && fail "Usage: $0 <firmware.zip>"
[[ ! -f "$FIRMWARE_ZIP" ]] && fail "Firmware zip not found: $FIRMWARE_ZIP"

log "ArizenOS Lite — Firmware Extractor"
log "Device: SM-T295"
log "Firmware: $FIRMWARE_ZIP"

mkdir -p "$FIRMWARE_DIR"
log "Extracting firmware zip..."
unzip -o "$FIRMWARE_ZIP" -d "$FIRMWARE_DIR"
ok "Firmware extracted to: $FIRMWARE_DIR"

# Detect partitions
AP_FILE=$(find "$FIRMWARE_DIR" -name "AP_*.tar.md5" | head -1)
BL_FILE=$(find "$FIRMWARE_DIR" -name "BL_*.tar.md5" | head -1)
CP_FILE=$(find "$FIRMWARE_DIR" -name "CP_*.tar.md5" | head -1)
CSC_FILE=$(find "$FIRMWARE_DIR" -name "CSC_*.tar.md5" | head -1)

[[ -n "$AP_FILE" ]] && { ok "AP: $AP_FILE"; cp "$AP_FILE" "$WORK_DIR/AP.tar.md5"; }
[[ -n "$BL_FILE" ]] && { ok "BL: $BL_FILE"; cp "$BL_FILE" "$WORK_DIR/BL.tar.md5"; }
[[ -n "$CP_FILE" ]] && { ok "CP: $CP_FILE"; cp "$CP_FILE" "$WORK_DIR/CP.tar.md5"; }
[[ -n "$CSC_FILE" ]] && { ok "CSC: $CSC_FILE"; cp "$CSC_FILE" "$WORK_DIR/CSC.tar.md5"; }

ok "Firmware extraction complete!"
log "Next step: ./scripts/unpack_ap.sh"
