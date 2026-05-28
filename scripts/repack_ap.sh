#!/usr/bin/env bash
# ArizenOS Lite — repack_ap.sh (FIXED v2)
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="${SYSTEM_MNT:-$WORK_DIR/system_mount}"
SYSTEM_RAW="$WORK_DIR/system_raw.img"
AP_EXTRACTED="$WORK_DIR/ap_extracted"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }

log "ArizenOS Lite — AP Repacker v2"

# Unmount
if mountpoint -q "$SYSTEM_MNT" 2>/dev/null; then
  log "Syncing and unmounting..."
  sync
  sudo umount "$SYSTEM_MNT"
  ok "Unmounted"
fi

# Filesystem check & shrink
log "Checking filesystem..."
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || true
log "Shrinking to minimum..."
resize2fs -M "$SYSTEM_RAW" 2>/dev/null || true
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || true

# Install img2simg if missing
if ! command -v img2simg >/dev/null 2>&1; then
  log "Installing img2simg..."
  apt-get install -y -qq android-tools-fsutils 2>/dev/null || true
fi

# Convert raw → sparse
if command -v img2simg >/dev/null 2>&1; then
  log "Converting raw → sparse..."
  img2simg "$SYSTEM_RAW" "$AP_EXTRACTED/system.img"
  ok "Sparse image created"
else
  warn "img2simg not found — copying raw (larger file size)"
  cp "$SYSTEM_RAW" "$AP_EXTRACTED/system.img"
fi

ok "Repack complete! Next: ./scripts/package_odin.sh"
