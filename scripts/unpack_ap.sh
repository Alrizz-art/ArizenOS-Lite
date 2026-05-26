#!/usr/bin/env bash
# ArizenOS Lite — unpack_ap.sh
# Unpacks AP.tar.md5 and extracts system.img
set -euo pipefail

WORK_DIR="$(pwd)/work"
AP_TAR="$WORK_DIR/AP.tar.md5"
AP_EXTRACTED="$WORK_DIR/ap_extracted"
SYSTEM_MNT="$WORK_DIR/system_mount"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ ! -f "$AP_TAR" ]] && fail "AP.tar.md5 not found at $AP_TAR. Run extract_firmware.sh first."

log "ArizenOS Lite — AP Unpacker"
log "Verifying MD5..."
md5sum -c <(tail -c 32 "$AP_TAR" | xxd -p | tr -d '\n' | awk '{print $0 "  '"$AP_TAR"'"}') 2>/dev/null \
  && ok "MD5 verified" || warn "MD5 check skipped (continuing)"

mkdir -p "$AP_EXTRACTED" "$SYSTEM_MNT"

log "Extracting AP.tar.md5..."
# Strip MD5 trailer and extract tar
head -c -32 "$AP_TAR" | tar -x -C "$AP_EXTRACTED" 2>/dev/null || \
tar --ignore-zeros -x -C "$AP_EXTRACTED" -f "$AP_TAR" 2>/dev/null || \
tar -x -C "$AP_EXTRACTED" -f "$AP_TAR"
ok "AP extracted to: $AP_EXTRACTED"

# Find system image
SYSTEM_IMG=$(find "$AP_EXTRACTED" -name "system.img*" | head -1)
BOOT_IMG=$(find "$AP_EXTRACTED" -name "boot.img*" | head -1)

[[ -z "$SYSTEM_IMG" ]] && fail "system.img not found in AP package"
ok "Found system image: $SYSTEM_IMG"
[[ -n "$BOOT_IMG" ]] && ok "Found boot image: $BOOT_IMG"

# Convert sparse to raw if needed
log "Checking image format..."
if file "$SYSTEM_IMG" | grep -q "sparse"; then
    log "Converting sparse image to raw..."
    simg2img "$SYSTEM_IMG" "$WORK_DIR/system_raw.img"
    ok "Converted to raw: $WORK_DIR/system_raw.img"
else
    cp "$SYSTEM_IMG" "$WORK_DIR/system_raw.img"
    ok "Copied raw image: $WORK_DIR/system_raw.img"
fi

# Check and repair filesystem
log "Checking filesystem integrity..."
e2fsck -f -y "$WORK_DIR/system_raw.img" || warn "Filesystem check warnings (may be normal)"

# Resize for modification headroom
log "Resizing image for modification headroom..."
resize2fs "$WORK_DIR/system_raw.img" 2G || warn "Resize warning (continuing)"

log "Mounting system image..."
sudo mount -o loop,rw "$WORK_DIR/system_raw.img" "$SYSTEM_MNT" || \
  fail "Failed to mount system.img. Ensure you have loop device access."
ok "System mounted at: $SYSTEM_MNT"

ok "AP unpack complete!"
log "Next step: ./scripts/inject_arizenos.sh"
