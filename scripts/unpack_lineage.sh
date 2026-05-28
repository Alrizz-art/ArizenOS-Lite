#!/usr/bin/env bash
# ArizenOS Lite — unpack_lineage.sh
# Unpack LineageOS zip → extract system.img → mount for modification
# Input : firmware/lineage-*.zip  (or path in firmware/.lineage_zip_path)
# Output: work/system_mount/  (mounted ext4, writable)
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

WORK_DIR="$(pwd)/work"
FIRMWARE_DIR="$(pwd)/firmware"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BLUE='\033[0;34m'; NC='\033[0m'
log()     { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()      { echo -e "${GREEN}[✓]${NC} $*"; }
warn()    { echo -e "${YELLOW}[!]${NC} $*"; }
fail()    { echo -e "${RED}[✗]${NC} $*"; exit 1; }
section() { echo -e "\n${BLUE}══════════════════════════════════════${NC}"; \
            echo -e "${BLUE} $*${NC}"; \
            echo -e "${BLUE}══════════════════════════════════════${NC}"; }

# ─────────────────────────────────────────────────────────────────────────────
section "[0] Locate LineageOS zip"
# ─────────────────────────────────────────────────────────────────────────────
LINEAGE_ZIP="${1:-}"
if [[ -z "$LINEAGE_ZIP" ]]; then
    if [[ -f "$FIRMWARE_DIR/.lineage_zip_path" ]]; then
        LINEAGE_ZIP=$(cat "$FIRMWARE_DIR/.lineage_zip_path")
    else
        LINEAGE_ZIP=$(find "$FIRMWARE_DIR" \
            \( -name "lineage-*.zip" -o -name "*T295*.zip" -o -name "*gtaslte*.zip" \) \
            2>/dev/null | sort -V | tail -1 || true)
    fi
fi

[[ -z "$LINEAGE_ZIP" || ! -f "$LINEAGE_ZIP" ]] && \
    fail "LineageOS zip not found.\nRun: bash scripts/download_lineage.sh\n  OR: bash scripts/unpack_lineage.sh /path/to/lineage.zip"

ok "Using: $(basename $LINEAGE_ZIP) ($(du -sh $LINEAGE_ZIP | cut -f1))"

mkdir -p "$WORK_DIR/lineage_extracted" "$WORK_DIR/system_mount"
EXTRACT_DIR="$WORK_DIR/lineage_extracted"
SYSTEM_MNT="$WORK_DIR/system_mount"
SYSTEM_RAW="$WORK_DIR/system_raw.img"

# ─────────────────────────────────────────────────────────────────────────────
section "[1] Verify zip integrity"
# ─────────────────────────────────────────────────────────────────────────────
log "Checking zip…"
if unzip -t "$LINEAGE_ZIP" >/dev/null 2>&1; then
    ok "Zip integrity OK"
else
    warn "Zip test failed — attempting extraction anyway"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[2] List zip contents"
# ─────────────────────────────────────────────────────────────────────────────
log "Zip contents:"
unzip -l "$LINEAGE_ZIP" | grep -E "\.(img|zip|br|new\.dat|patch\.dat|xz)$" | head -20

# Detect format: payload.bin (A/B), system.new.dat.br (block-based), system.img (raw)
HAS_PAYLOAD=$(unzip -l "$LINEAGE_ZIP" 2>/dev/null | grep -c "payload.bin" || true)
HAS_NEWDAT_BR=$(unzip -l "$LINEAGE_ZIP" 2>/dev/null | grep -c "system.new.dat.br" || true)
HAS_NEWDAT=$(unzip -l "$LINEAGE_ZIP" 2>/dev/null | grep -c "system.new.dat$" || true)
HAS_IMG=$(unzip -l "$LINEAGE_ZIP" 2>/dev/null | grep -c "system.img" || true)

log "Format detection:"
log "  payload.bin   : $HAS_PAYLOAD"
log "  system.new.dat.br : $HAS_NEWDAT_BR"
log "  system.new.dat    : $HAS_NEWDAT"
log "  system.img        : $HAS_IMG"

# ─────────────────────────────────────────────────────────────────────────────
section "[3] Extract system image"
# ─────────────────────────────────────────────────────────────────────────────

if [[ "$HAS_IMG" -gt 0 ]]; then
    # ── Format A: Raw system.img directly in zip ─────────────────────────────
    log "Format: raw system.img in zip"
    unzip -o "$LINEAGE_ZIP" "system.img" -d "$EXTRACT_DIR"
    RAW_IMG="$EXTRACT_DIR/system.img"

elif [[ "$HAS_PAYLOAD" -gt 0 ]]; then
    # ── Format B: A/B OTA — payload.bin (Pixel-style) ───────────────────────
    log "Format: payload.bin (A/B OTA)"
    command -v payload-dumper-go >/dev/null 2>&1 || {
        log "Installing payload-dumper-go…"
        # Try pre-built binary
        ARCH=$(uname -m)
        PD_URL="https://github.com/ssut/payload-dumper-go/releases/latest/download/payload-dumper-go_linux_${ARCH}.tar.gz"
        mkdir -p /tmp/pd
        curl -sL "$PD_URL" | tar -xz -C /tmp/pd 2>/dev/null || true
        [[ -f /tmp/pd/payload-dumper-go ]] && {
            cp /tmp/pd/payload-dumper-go /usr/local/bin/
            chmod +x /usr/local/bin/payload-dumper-go
        } || fail "payload-dumper-go not found. Install it from:\nhttps://github.com/ssut/payload-dumper-go"
    }
    mkdir -p "$EXTRACT_DIR/payload_out"
    unzip -o "$LINEAGE_ZIP" "payload.bin" -d "$EXTRACT_DIR"
    payload-dumper-go -p system -o "$EXTRACT_DIR/payload_out" "$EXTRACT_DIR/payload.bin"
    RAW_IMG=$(find "$EXTRACT_DIR/payload_out" -name "system.img" | head -1)
    [[ -z "$RAW_IMG" ]] && fail "system.img not found in payload output"

elif [[ "$HAS_NEWDAT_BR" -gt 0 ]]; then
    # ── Format C: Brotli-compressed block-based (most LineageOS 17/18/19) ───
    log "Format: system.new.dat.br (brotli block-based)"
    unzip -o "$LINEAGE_ZIP" \
        "system.new.dat.br" "system.patch.dat" "system.transfer.list" \
        -d "$EXTRACT_DIR"

    # Decompress brotli
    command -v brotli >/dev/null 2>&1 || \
        apt-get install -y -qq brotli 2>/dev/null || true
    command -v brotli >/dev/null 2>&1 || fail "brotli not installed. Run: apt install brotli"
    log "Decompressing brotli…"
    brotli -d "$EXTRACT_DIR/system.new.dat.br" -o "$EXTRACT_DIR/system.new.dat"

    # Convert .new.dat → raw img using sdat2img
    log "Converting system.new.dat → system.img…"
    if ! command -v sdat2img >/dev/null 2>&1; then
        log "Fetching sdat2img…"
        curl -sL "https://raw.githubusercontent.com/xpirt/sdat2img/master/sdat2img.py" \
            -o /tmp/sdat2img.py 2>/dev/null || true
        [[ -f /tmp/sdat2img.py ]] && \
            chmod +x /tmp/sdat2img.py && \
            ln -sf /tmp/sdat2img.py /usr/local/bin/sdat2img || true
    fi

    if command -v python3 >/dev/null 2>&1 && [[ -f /tmp/sdat2img.py ]]; then
        python3 /tmp/sdat2img.py \
            "$EXTRACT_DIR/system.transfer.list" \
            "$EXTRACT_DIR/system.new.dat" \
            "$EXTRACT_DIR/system.img"
    elif command -v sdat2img >/dev/null 2>&1; then
        sdat2img \
            "$EXTRACT_DIR/system.transfer.list" \
            "$EXTRACT_DIR/system.new.dat" \
            "$EXTRACT_DIR/system.img"
    else
        fail "sdat2img not found. Install python3 or sdat2img."
    fi
    RAW_IMG="$EXTRACT_DIR/system.img"

elif [[ "$HAS_NEWDAT" -gt 0 ]]; then
    # ── Format D: Uncompressed block-based ──────────────────────────────────
    log "Format: system.new.dat (uncompressed block-based)"
    unzip -o "$LINEAGE_ZIP" \
        "system.new.dat" "system.patch.dat" "system.transfer.list" \
        -d "$EXTRACT_DIR"
    command -v sdat2img >/dev/null 2>&1 || \
        fail "sdat2img not found. Run: pip install sdat2img OR download from GitHub"
    sdat2img \
        "$EXTRACT_DIR/system.transfer.list" \
        "$EXTRACT_DIR/system.new.dat" \
        "$EXTRACT_DIR/system.img"
    RAW_IMG="$EXTRACT_DIR/system.img"
else
    fail "Cannot detect LineageOS format. Contents:\n$(unzip -l $LINEAGE_ZIP | head -30)"
fi

ok "System image extracted: $(du -sh $RAW_IMG | cut -f1)"

# ─────────────────────────────────────────────────────────────────────────────
section "[4] Detect filesystem type"
# ─────────────────────────────────────────────────────────────────────────────
IMG_TYPE=$(file "$RAW_IMG")
log "Image type: $IMG_TYPE"

if echo "$IMG_TYPE" | grep -qi "erofs"; then
    # EROFS — read-only compressed filesystem (LineageOS 20+ / Android 12+)
    warn "EROFS filesystem detected!"
    warn "EROFS is read-only and cannot be directly modified."
    warn ""
    warn "Options:"
    warn "  A) Use LineageOS 18.1 (Android 11) or older — usually ext4"
    warn "  B) Use a build with system as root (SAR) ext4"
    warn ""
    warn "Checking if an older build is available…"
    fail "EROFS not supported. Use LineageOS 18.1 (Android 11) or a build with ext4 system."

elif echo "$IMG_TYPE" | grep -qi "sparse"; then
    # Android sparse image → convert to raw ext4
    log "Sparse image — converting to raw ext4…"
    command -v simg2img >/dev/null 2>&1 || \
        apt-get install -y -qq android-tools-fsutils 2>/dev/null || true
    command -v simg2img >/dev/null 2>&1 || \
        fail "simg2img not found. Run: apt install android-tools-fsutils"
    simg2img "$RAW_IMG" "$SYSTEM_RAW"
    ok "Sparse → raw: $(du -sh $SYSTEM_RAW | cut -f1)"

else
    # Raw ext4
    cp "$RAW_IMG" "$SYSTEM_RAW"
    ok "Raw ext4: $(du -sh $SYSTEM_RAW | cut -f1)"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[5] Filesystem check"
# ─────────────────────────────────────────────────────────────────────────────
log "Running e2fsck…"
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || \
    warn "e2fsck warnings (normal for stock LineageOS images)"

# ─────────────────────────────────────────────────────────────────────────────
section "[6] Expand image +250MB for ArizenOS components"
# ─────────────────────────────────────────────────────────────────────────────
BLOCK_SIZE=$(tune2fs -l "$SYSTEM_RAW" 2>/dev/null | awk '/^Block size:/{print $3}')
BLOCK_COUNT=$(tune2fs -l "$SYSTEM_RAW" 2>/dev/null | awk '/^Block count:/{print $3}')

if [[ -n "$BLOCK_SIZE" && -n "$BLOCK_COUNT" ]]; then
    EXTRA=$(( (250 * 1024 * 1024) / BLOCK_SIZE ))
    NEW=$(( BLOCK_COUNT + EXTRA ))
    log "Expanding: $(( BLOCK_SIZE * BLOCK_COUNT / 1048576 ))MB → $(( BLOCK_SIZE * NEW / 1048576 ))MB"
    resize2fs "$SYSTEM_RAW" "${NEW}" 2>/dev/null || \
        warn "resize2fs failed — continuing at current size"
else
    CURRENT=$(stat -c%s "$SYSTEM_RAW" 2>/dev/null || stat -f%z "$SYSTEM_RAW")
    truncate -s "$(( CURRENT + 250 * 1024 * 1024 ))" "$SYSTEM_RAW"
    resize2fs "$SYSTEM_RAW" 2>/dev/null || true
fi
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || true

# ─────────────────────────────────────────────────────────────────────────────
section "[7] Mount"
# ─────────────────────────────────────────────────────────────────────────────
mount | grep -qF "$SYSTEM_MNT" && sudo umount -l "$SYSTEM_MNT" 2>/dev/null || true
log "Mounting at $SYSTEM_MNT…"
sudo mount -t ext4 -o loop,rw "$SYSTEM_RAW" "$SYSTEM_MNT" || \
    fail "Mount failed — run with sudo or in a privileged container (--privileged)"

ok "Mounted — $(ls $SYSTEM_MNT | wc -l) top-level entries"
echo "$SYSTEM_MNT"  > "$WORK_DIR/.mount_path"
echo "lineageos"    > "$WORK_DIR/.base_type"
echo "$(basename $LINEAGE_ZIP)" > "$WORK_DIR/.lineage_source"

echo ""
ok "═══════════════════════════════════════"
ok "  Unpack complete!"
ok "═══════════════════════════════════════"
log "  System mounted at: $SYSTEM_MNT"
log "  Next: sudo bash scripts/inject_arizenos.sh"
