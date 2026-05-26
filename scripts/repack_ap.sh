#!/usr/bin/env bash
# ArizenOS Lite — repack_ap.sh
# Unmounts system, converts back to sparse, repacks into AP.tar
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="$WORK_DIR/system_mount"
AP_EXTRACTED="$WORK_DIR/ap_extracted"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

log "ArizenOS Lite — AP Repacker"

# Unmount system
log "Unmounting system partition..."
if mountpoint -q "$SYSTEM_MNT"; then
    sudo umount "$SYSTEM_MNT"
    ok "System unmounted"
else
    log "System not mounted, continuing..."
fi

# Check filesystem
log "Final filesystem check..."
e2fsck -f -y "$WORK_DIR/system_raw.img" || true

# Shrink image to minimal size
log "Shrinking image..."
resize2fs -M "$WORK_DIR/system_raw.img" || true

# Convert back to sparse
log "Converting to sparse image..."
img2simg "$WORK_DIR/system_raw.img" "$AP_EXTRACTED/system.img"
ok "Sparse image created"

# Copy boot image (unmodified — we preserve stock kernel)
BOOT_IMG=$(find "$AP_EXTRACTED" -name "boot.img*" | head -1)
[[ -n "$BOOT_IMG" ]] && ok "Boot image preserved (stock kernel): $BOOT_IMG"

ok "Repack complete!"
log "Next step: ./scripts/package_odin.sh"
