#!/usr/bin/env bash
# ArizenOS Lite — extract_firmware.sh
# Handles: full Samsung firmware ZIP  OR  direct AP.tar.md5 partition file
set -euo pipefail

FIRMWARE_FILE="${1:-}"
WORK_DIR="$(pwd)/work"
FIRMWARE_DIR="$WORK_DIR/firmware_raw"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ -z "$FIRMWARE_FILE" ]] && fail "Usage: $0 <firmware.zip|AP.tar.md5>"
[[ ! -f "$FIRMWARE_FILE" ]] && fail "Firmware file not found: $FIRMWARE_FILE"

log "ArizenOS Lite — Firmware Extractor"
log "Device  : SM-T295"
log "Input   : $FIRMWARE_FILE ($(du -sh "$FIRMWARE_FILE" | cut -f1))"
log "Detected: $(file "$FIRMWARE_FILE")"

mkdir -p "$FIRMWARE_DIR"

# ── Determine format ──────────────────────────────────────────────────────────
FILE_INFO=$(file "$FIRMWARE_FILE")
IS_ZIP=false
IS_TAR=false
echo "$FILE_INFO" | grep -qi "zip" && IS_ZIP=true || true
echo "$FIRMWARE_FILE" | grep -qiE "\.zip$" && IS_ZIP=true || true
echo "$FILE_INFO" | grep -qi "tar" && IS_TAR=true || true
echo "$FIRMWARE_FILE" | grep -qiE "\.(tar\.md5|tar|md5)$" && IS_TAR=true || true

if [[ "$IS_ZIP" == "true" ]]; then
  # ── Full Samsung firmware ZIP (contains AP_*.tar.md5, BL, CP, CSC) ─────────
  log "Format: Samsung firmware ZIP — unzipping..."
  unzip -o "$FIRMWARE_FILE" -d "$FIRMWARE_DIR" || fail "unzip failed — file may be corrupted"
  ok "Extracted to: $FIRMWARE_DIR"

  AP_FILE=$(find "$FIRMWARE_DIR"  -name "AP_*.tar.md5"  | head -1)
  BL_FILE=$(find "$FIRMWARE_DIR"  -name "BL_*.tar.md5"  | head -1)
  CP_FILE=$(find "$FIRMWARE_DIR"  -name "CP_*.tar.md5"  | head -1)
  CSC_FILE=$(find "$FIRMWARE_DIR" -name "CSC_*.tar.md5" | head -1)

  [[ -n "$AP_FILE"  ]] && { ok "AP:  $(basename $AP_FILE)";  cp "$AP_FILE"  "$WORK_DIR/AP.tar.md5";  }
  [[ -n "$BL_FILE"  ]] && { ok "BL:  $(basename $BL_FILE)";  cp "$BL_FILE"  "$WORK_DIR/BL.tar.md5";  }
  [[ -n "$CP_FILE"  ]] && { ok "CP:  $(basename $CP_FILE)";  cp "$CP_FILE"  "$WORK_DIR/CP.tar.md5";  }
  [[ -n "$CSC_FILE" ]] && { ok "CSC: $(basename $CSC_FILE)"; cp "$CSC_FILE" "$WORK_DIR/CSC.tar.md5"; }

  [[ -z "$AP_FILE" ]] && fail "AP_*.tar.md5 not found inside ZIP — confirm this is a full Samsung SM-T295 firmware package"

elif [[ "$IS_TAR" == "true" ]]; then
  # ── Direct AP.tar.md5 (downloaded separately or from Google Drive) ──────────
  log "Format: Direct Samsung AP partition (.tar.md5) — using as AP directly..."
  cp "$FIRMWARE_FILE" "$WORK_DIR/AP.tar.md5"
  ok "AP.tar.md5 ready: $(du -sh $WORK_DIR/AP.tar.md5 | cut -f1)"

else
  # ── Last resort: probe then decide ─────────────────────────────────────────
  log "Format: unknown — probing..."
  if unzip -t "$FIRMWARE_FILE" >/dev/null 2>&1; then
    log "Probe: valid ZIP — extracting..."
    unzip -o "$FIRMWARE_FILE" -d "$FIRMWARE_DIR" || fail "unzip probe-extract failed"
    AP_FILE=$(find "$FIRMWARE_DIR" -name "AP_*.tar.md5" | head -1)
    [[ -n "$AP_FILE" ]] && { cp "$AP_FILE" "$WORK_DIR/AP.tar.md5"; ok "AP: $(basename $AP_FILE)"; } || \
      fail "ZIP extracted but AP_*.tar.md5 not found inside"
  elif tar -tf "$FIRMWARE_FILE" >/dev/null 2>&1; then
    log "Probe: valid tar — treating as AP partition..."
    cp "$FIRMWARE_FILE" "$WORK_DIR/AP.tar.md5"
    ok "AP.tar.md5 ready (tar probe): $(du -sh $WORK_DIR/AP.tar.md5 | cut -f1)"
  else
    fail "Unrecognized firmware format: $FILE_INFO — expected Samsung firmware ZIP or AP.tar.md5"
  fi
fi

ok "Firmware extraction complete!"
log "Next step: ./scripts/unpack_ap.sh"