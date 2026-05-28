#!/usr/bin/env bash
# ArizenOS Lite — unpack_ap.sh (FIXED v3)
# Supports: lz4, sparse/raw ext4, erofs detection, Docker/Linux
set -euo pipefail

WORK_DIR="$(pwd)/work"
AP_TAR="$WORK_DIR/AP.tar.md5"
AP_EXTRACTED="$WORK_DIR/ap_extracted"
SYSTEM_MNT="$WORK_DIR/system_mount"
SYSTEM_RAW="$WORK_DIR/system_raw.img"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ ! -f "$AP_TAR" ]] && fail "AP.tar.md5 not found. Run extract_firmware.sh first."
mkdir -p "$AP_EXTRACTED" "$SYSTEM_MNT"

# ── 1. Extract AP.tar.md5 ─────────────────────────────────────────────────────
# Samsung format: GNU tar + 32-byte ASCII MD5 appended at end
log "Extracting AP.tar.md5..."
AP_SIZE=$(stat -c%s "$AP_TAR" 2>/dev/null || stat -f%z "$AP_TAR")
CONTENT_SIZE=$((AP_SIZE - 32))
log "Total: $AP_SIZE bytes | Tar content: $CONTENT_SIZE bytes"

STRIPPED="$WORK_DIR/AP_stripped.tar"
# head -c is more reliable than dd for partial reads
if head -c "$CONTENT_SIZE" "$AP_TAR" > "$STRIPPED" 2>/dev/null && tar -t -f "$STRIPPED" >/dev/null 2>&1; then
    tar -x -C "$AP_EXTRACTED" -f "$STRIPPED"
    rm -f "$STRIPPED"
elif dd if="$AP_TAR" bs=4096 count=$(( CONTENT_SIZE / 4096 )) of="$STRIPPED" 2>/dev/null && \
     tar -t -f "$STRIPPED" >/dev/null 2>&1; then
    tar -x -C "$AP_EXTRACTED" -f "$STRIPPED"
    rm -f "$STRIPPED"
else
    rm -f "$STRIPPED"
    warn "MD5 strip failed — attempting direct tar extraction with ignore-zeros"
    tar --ignore-zeros -x -C "$AP_EXTRACTED" -f "$AP_TAR" 2>/dev/null || \
    tar -x -C "$AP_EXTRACTED" -f "$AP_TAR" 2>/dev/null || \
    fail "Cannot extract AP.tar.md5. File may be corrupted."
fi
ok "AP extracted:"
ls -lh "$AP_EXTRACTED/"

# ── 2. Find system image ──────────────────────────────────────────────────────
SYSTEM_IMG=$(find "$AP_EXTRACTED" -maxdepth 1 \
    \( -name "system.img" -o -name "system.img.ext4" -o -name "system.img.lz4" \) | head -1)
BOOT_IMG=$(find "$AP_EXTRACTED" -maxdepth 1 \
    \( -name "boot.img" -o -name "boot.img.lz4" \) | head -1)

[[ -z "$SYSTEM_IMG" ]] && fail "system.img not found in AP — check firmware compatibility with SM-T295"
ok "System image: $(basename $SYSTEM_IMG) ($(du -sh $SYSTEM_IMG | cut -f1))"
[[ -n "$BOOT_IMG" ]] && ok "Boot image: $(basename $BOOT_IMG) — will be preserved (stock kernel)"

# Save original name for repack
echo "$(basename $SYSTEM_IMG)" > "$WORK_DIR/.system_img_name"

# ── 3. Decompress lz4 ────────────────────────────────────────────────────────
if [[ "$SYSTEM_IMG" == *.lz4 ]]; then
    log "Decompressing lz4 system image..."
    if ! command -v lz4 >/dev/null 2>&1; then
        apt-get install -y -qq lz4 2>/dev/null || \
        apt-get install -y -qq liblz4-tool 2>/dev/null || true
    fi
    command -v lz4 >/dev/null 2>&1 || fail "lz4 tool not found. Install with: apt install lz4"
    DECOMPRESSED="${SYSTEM_IMG%.lz4}"
    lz4 -d "$SYSTEM_IMG" "$DECOMPRESSED" || lz4cat "$SYSTEM_IMG" > "$DECOMPRESSED"
    rm -f "$SYSTEM_IMG"
    SYSTEM_IMG="$DECOMPRESSED"
    ok "lz4 decompressed → $(basename $SYSTEM_IMG)"
fi

# ── 4. Detect & reject erofs ─────────────────────────────────────────────────
IMG_TYPE=$(file "$SYSTEM_IMG")
log "Image type: $IMG_TYPE"
if echo "$IMG_TYPE" | grep -qi "erofs"; then
    fail "EROFS filesystem detected — cannot modify read-only compressed filesystem.
Use an older firmware for SM-T295 (Android 9 or 10). Check: https://samfw.com"
fi

# ── 5. Sparse → raw ──────────────────────────────────────────────────────────
if echo "$IMG_TYPE" | grep -qi "sparse"; then
    log "Android sparse image — converting to raw ext4..."
    if ! command -v simg2img >/dev/null 2>&1; then
        apt-get install -y -qq android-tools-fsutils 2>/dev/null || \
        apt-get install -y -qq simg2img 2>/dev/null || true
    fi
    command -v simg2img >/dev/null 2>&1 || fail "simg2img not found"
    simg2img "$SYSTEM_IMG" "$SYSTEM_RAW"
    ok "Sparse → raw: $SYSTEM_RAW ($(du -sh $SYSTEM_RAW | cut -f1))"
else
    cp "$SYSTEM_IMG" "$SYSTEM_RAW"
    ok "Raw image copied: $(du -sh $SYSTEM_RAW | cut -f1)"
fi

# ── 6. Filesystem check ───────────────────────────────────────────────────────
log "Checking ext4 filesystem..."
file "$SYSTEM_RAW" | grep -qi "ext" || fail "Not an ext4 filesystem — incompatible firmware"
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || warn "fsck warnings (normal for stock firmware)"

# ── 7. Expand image +300MB for ArizenOS components ───────────────────────────
BLOCK_SIZE=$(tune2fs -l "$SYSTEM_RAW" 2>/dev/null | awk '/^Block size:/{print $3}')
BLOCK_COUNT=$(tune2fs -l "$SYSTEM_RAW" 2>/dev/null | awk '/^Block count:/{print $3}')
CURRENT_BYTES=$(( BLOCK_SIZE * BLOCK_COUNT ))
EXTRA_BYTES=$(( 300 * 1024 * 1024 ))
NEW_BYTES=$(( CURRENT_BYTES + EXTRA_BYTES ))
log "Expanding: $(( CURRENT_BYTES / 1048576 ))MB → $(( NEW_BYTES / 1048576 ))MB"
resize2fs "$SYSTEM_RAW" "${NEW_BYTES}b" 2>/dev/null || warn "resize2fs failed — proceeding with original size"
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || true

# ── 8. Mount ──────────────────────────────────────────────────────────────────
log "Mounting system.img at $SYSTEM_MNT..."
if mount | grep -qF "$SYSTEM_MNT"; then
    warn "Already mounted — unmounting first"
    sudo umount -l "$SYSTEM_MNT" 2>/dev/null || true
fi
sudo mount -t ext4 -o loop,rw "$SYSTEM_RAW" "$SYSTEM_MNT" || \
    fail "Mount failed — script must run as root or in privileged Docker (--privileged)"

ok "Mounted successfully"
echo "System partition contents: $(ls $SYSTEM_MNT | wc -l) entries"
echo "$SYSTEM_MNT" > "$WORK_DIR/.mount_path"
ok "✓ Unpack complete! Next: ./scripts/inject_arizenos.sh"
