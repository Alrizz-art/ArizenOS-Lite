#!/usr/bin/env bash
# ArizenOS Lite — repack_ap.sh (FIXED v3)
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="${SYSTEM_MNT:-$(cat $WORK_DIR/.mount_path 2>/dev/null || echo $WORK_DIR/system_mount)}"
SYSTEM_RAW="$WORK_DIR/system_raw.img"
AP_EXTRACTED="$WORK_DIR/ap_extracted"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ ! -f "$SYSTEM_RAW" ]] && fail "system_raw.img not found. Run unpack_ap.sh first."

log "ArizenOS Lite — AP Repacker v3"

# ── 1. Unmount system image ───────────────────────────────────────────────────
if mount | grep -qF "$SYSTEM_MNT"; then
    log "Syncing filesystem..."
    sync
    log "Unmounting $SYSTEM_MNT..."
    sudo umount "$SYSTEM_MNT" 2>/dev/null || \
    sudo umount -l "$SYSTEM_MNT" 2>/dev/null || \
    fail "Cannot unmount $SYSTEM_MNT — check if files are in use"
    ok "Unmounted"
else
    warn "System was not mounted (already unmounted or inject ran differently)"
fi

# ── 2. Filesystem check + shrink ─────────────────────────────────────────────
log "Running filesystem check..."
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || warn "fsck warnings — attempting repair"
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || true

log "Shrinking image to minimum size..."
resize2fs -M "$SYSTEM_RAW" 2>/dev/null || warn "resize2fs -M failed — using current size"
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || true

FINAL_SIZE=$(du -sh "$SYSTEM_RAW" | cut -f1)
log "Final system.img size: $FINAL_SIZE"

# ── 3. Convert raw → sparse ───────────────────────────────────────────────────
ORIG_NAME=$(cat "$WORK_DIR/.system_img_name" 2>/dev/null || echo "system.img")
DEST_IMG="$AP_EXTRACTED/${ORIG_NAME%.lz4}"  # always output non-lz4 (Odin accepts raw ext4)

if command -v img2simg >/dev/null 2>&1; then
    log "Converting raw → Android sparse image..."
    img2simg "$SYSTEM_RAW" "$DEST_IMG"
    ok "Sparse image: $(basename $DEST_IMG) ($(du -sh $DEST_IMG | cut -f1))"
else
    log "img2simg not found — installing..."
    apt-get install -y -qq android-tools-fsutils 2>/dev/null || true
    if command -v img2simg >/dev/null 2>&1; then
        img2simg "$SYSTEM_RAW" "$DEST_IMG"
        ok "Sparse image: $(basename $DEST_IMG) ($(du -sh $DEST_IMG | cut -f1))"
    else
        warn "img2simg unavailable — using raw ext4 (larger but valid)"
        cp "$SYSTEM_RAW" "$DEST_IMG"
        ok "Raw image: $(basename $DEST_IMG) ($(du -sh $DEST_IMG | cut -f1))"
    fi
fi

# Remove old lz4 if it existed (replaced by our non-lz4 version)
[[ "$ORIG_NAME" == *.lz4 ]] && rm -f "$AP_EXTRACTED/$ORIG_NAME" 2>/dev/null || true

ok "✓ Repack complete! Next: ./scripts/package_odin.sh"
