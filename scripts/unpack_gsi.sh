#!/usr/bin/env bash
# ArizenOS Lite — unpack_gsi.sh
# Unpack and mount a GSI system.img for modification
# Supports: raw ext4 .img, sparse .img, lz4-compressed, xz-compressed, zip-wrapped
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

mkdir -p "$WORK_DIR/system_mount"
SYSTEM_MNT="$WORK_DIR/system_mount"
SYSTEM_RAW="$WORK_DIR/system_raw.img"

# ─────────────────────────────────────────────────────────────────────────────
section "[1] Locate ROM file"
# ─────────────────────────────────────────────────────────────────────────────
ROM_FILE="${1:-}"
if [[ -z "$ROM_FILE" ]]; then
    # Try cached path first
    if [[ -f "$FIRMWARE_DIR/.rom_path" ]]; then
        ROM_FILE=$(cat "$FIRMWARE_DIR/.rom_path")
    else
        # Auto-detect: prefer .img, then .zip
        ROM_FILE=$(find "$FIRMWARE_DIR" -name "*.img" -size +200M 2>/dev/null | head -1 || true)
        [[ -z "$ROM_FILE" ]] && \
        ROM_FILE=$(find "$FIRMWARE_DIR" \( -name "*.zip" -o -name "*.xz" \) \
            -size +100M 2>/dev/null | sort -V | tail -1 || true)
    fi
fi

[[ -z "$ROM_FILE" || ! -f "$ROM_FILE" ]] && \
    fail "No ROM found in firmware/.\nRun: bash scripts/download_rom.sh\nOr: bash scripts/unpack_gsi.sh /path/to/system.img"

EXT="${ROM_FILE##*.}"
ok "ROM: $(basename $ROM_FILE) ($(du -sh $ROM_FILE | cut -f1)) — type: .$EXT"

# ─────────────────────────────────────────────────────────────────────────────
section "[2] Extract if needed"
# ─────────────────────────────────────────────────────────────────────────────
WORK_IMG="$WORK_DIR/extracted_system.img"

case "$EXT" in
    img)
        # Direct image file — most common for GSI
        WORK_IMG="$ROM_FILE"
        ;;

    xz)
        # XZ compressed image (some GSIs)
        log "Decompressing .xz…"
        command -v xz >/dev/null 2>&1 || apt-get install -y -qq xz-utils 2>/dev/null || true
        command -v xz >/dev/null 2>&1 || fail "xz not found. Run: apt install xz-utils"
        xz -dkf "$ROM_FILE"
        WORK_IMG="${ROM_FILE%.xz}"
        ok "Decompressed → $(basename $WORK_IMG)"
        ;;

    lz4)
        # LZ4 compressed image
        log "Decompressing .lz4…"
        command -v lz4 >/dev/null 2>&1 || apt-get install -y -qq lz4 2>/dev/null || true
        lz4 -d "$ROM_FILE" "$WORK_DIR/system_decompressed.img"
        WORK_IMG="$WORK_DIR/system_decompressed.img"
        ok "Decompressed → $(basename $WORK_IMG)"
        ;;

    zip)
        # ZIP-wrapped — extract system.img from inside
        log "Extracting system.img from zip…"
        # Detect what's inside
        CONTENTS=$(unzip -l "$ROM_FILE" 2>/dev/null)

        if echo "$CONTENTS" | grep -q "system.img"; then
            unzip -o "$ROM_FILE" "system.img" -d "$WORK_DIR"
            WORK_IMG="$WORK_DIR/system.img"
        elif echo "$CONTENTS" | grep -q "system.new.dat.br"; then
            log "Brotli block-based format — using unpack_lineage.sh"
            bash "$(dirname "$0")/unpack_lineage.sh" "$ROM_FILE"
            exit 0
        elif echo "$CONTENTS" | grep -q "payload.bin"; then
            log "A/B payload format — using unpack_lineage.sh"
            bash "$(dirname "$0")/unpack_lineage.sh" "$ROM_FILE"
            exit 0
        else
            fail "Cannot detect format inside zip:\n$CONTENTS"
        fi
        ok "Extracted: $(basename $WORK_IMG)"
        ;;

    br)
        # Naked brotli
        log "Decompressing .br…"
        command -v brotli >/dev/null 2>&1 || apt-get install -y -qq brotli 2>/dev/null || true
        brotli -d "$ROM_FILE" -o "$WORK_DIR/system.img"
        WORK_IMG="$WORK_DIR/system.img"
        ok "Decompressed → system.img"
        ;;

    *)
        warn "Unknown extension .$EXT — treating as raw image"
        WORK_IMG="$ROM_FILE"
        ;;
esac

# ─────────────────────────────────────────────────────────────────────────────
section "[3] Detect filesystem type"
# ─────────────────────────────────────────────────────────────────────────────
IMG_TYPE=$(file "$WORK_IMG")
log "Image type: $IMG_TYPE"

if echo "$IMG_TYPE" | grep -qi "erofs"; then
    warn "══════════════════════════════════════════"
    warn "  EROFS detected — READ-ONLY filesystem"
    warn "══════════════════════════════════════════"
    warn ""
    warn "EROFS cannot be mounted read-write for modification."
    warn "This is common with Android 12L/13 GSI images."
    warn ""
    warn "Solutions:"
    warn ""
    warn "  A) Use an older GSI variant:"
    warn "     - Look for Android 11 / 12 GSI (not 12L or 13)"
    warn "     - Some GSI builders provide ext4 and erofs variants"
    warn "     - File name hints: 'ext4' or 'go' variants often use ext4"
    warn ""
    warn "  B) Use erofs2ext4 tool to convert (experimental):"
    warn "     https://github.com/sekaiacg/erofs-utils"
    warn "     ero2ext4.sh: https://github.com/ErfanGSI/Erfan-GSI"
    warn ""
    warn "  C) Use TWRP + Magisk module approach instead of system modification"
    warn "     (flash base GSI via TWRP, then Magisk module for ArizenOS overlay)"
    warn ""
    warn "Recommended: go back to XDA thread and look for:"
    warn "  → An 'ext4' variant"
    warn "  → Or an Android 11 / 12 build"
    warn "  → Or check if Superior OS has a GTO ext4 download"
    fail "EROFS detected — cannot modify. See options above."

elif echo "$IMG_TYPE" | grep -qi "sparse"; then
    log "Android sparse image — converting to raw ext4…"
    command -v simg2img >/dev/null 2>&1 || \
        apt-get install -y -qq android-tools-fsutils 2>/dev/null || true
    command -v simg2img >/dev/null 2>&1 || \
        fail "simg2img not found. Run: apt install android-tools-fsutils"
    simg2img "$WORK_IMG" "$SYSTEM_RAW"
    ok "Sparse → raw ext4: $(du -sh $SYSTEM_RAW | cut -f1)"

elif echo "$IMG_TYPE" | grep -qi "ext"; then
    log "Raw ext4 — copying to work dir…"
    [[ "$WORK_IMG" != "$SYSTEM_RAW" ]] && cp "$WORK_IMG" "$SYSTEM_RAW" || true
    ok "Raw ext4: $(du -sh $SYSTEM_RAW | cut -f1)"

else
    warn "Unknown type — attempting as raw ext4: $IMG_TYPE"
    [[ "$WORK_IMG" != "$SYSTEM_RAW" ]] && cp "$WORK_IMG" "$SYSTEM_RAW" || true
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[4] Filesystem check"
# ─────────────────────────────────────────────────────────────────────────────
log "Running e2fsck…"
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null && ok "e2fsck passed" || \
    warn "e2fsck warnings (normal for GSI images)"

# ─────────────────────────────────────────────────────────────────────────────
section "[5] Expand image +250MB for ArizenOS"
# ─────────────────────────────────────────────────────────────────────────────
BLOCK_SIZE=$(tune2fs -l "$SYSTEM_RAW" 2>/dev/null | awk '/^Block size:/{print $3}')
BLOCK_COUNT=$(tune2fs -l "$SYSTEM_RAW" 2>/dev/null | awk '/^Block count:/{print $3}')

if [[ -n "$BLOCK_SIZE" && -n "$BLOCK_COUNT" ]]; then
    EXTRA=$(( (250 * 1024 * 1024) / BLOCK_SIZE ))
    NEW=$(( BLOCK_COUNT + EXTRA ))
    BEFORE_MB=$(( BLOCK_SIZE * BLOCK_COUNT / 1048576 ))
    AFTER_MB=$(( BLOCK_SIZE * NEW / 1048576 ))
    log "Expanding: ${BEFORE_MB}MB → ${AFTER_MB}MB"
    resize2fs "$SYSTEM_RAW" "${NEW}" 2>/dev/null || \
        warn "resize2fs failed — proceeding at current size"
else
    CURRENT=$(stat -c%s "$SYSTEM_RAW" 2>/dev/null || stat -f%z "$SYSTEM_RAW")
    truncate -s "$(( CURRENT + 250 * 1024 * 1024 ))" "$SYSTEM_RAW"
    resize2fs "$SYSTEM_RAW" 2>/dev/null || true
fi
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || true

# ─────────────────────────────────────────────────────────────────────────────
section "[6] Mount"
# ─────────────────────────────────────────────────────────────────────────────
mount | grep -qF "$SYSTEM_MNT" && sudo umount -l "$SYSTEM_MNT" 2>/dev/null || true
log "Mounting at $SYSTEM_MNT…"
sudo mount -t ext4 -o loop,rw "$SYSTEM_RAW" "$SYSTEM_MNT" || \
    fail "Mount failed. Run with sudo or in privileged container (--privileged)."

ok "Mounted — $(ls $SYSTEM_MNT | wc -l) top-level entries"
echo "$SYSTEM_MNT"      > "$WORK_DIR/.mount_path"
echo "gsi"              > "$WORK_DIR/.base_type"
echo "$(basename $ROM_FILE)" > "$WORK_DIR/.lineage_source"

# ─────────────────────────────────────────────────────────────────────────────
section "[7] GSI compatibility check for SM-T295"
# ─────────────────────────────────────────────────────────────────────────────
log "Checking GSI structure…"

# Check for Treble-compatible system partition structure
TREBLE_OK=true
[[ -d "$SYSTEM_MNT/system" ]] && log "  ✓ system-as-root structure detected" || true
[[ -d "$SYSTEM_MNT/app" ]]    && log "  ✓ /system/app present" || { warn "  /system/app not found"; TREBLE_OK=false; }
[[ -d "$SYSTEM_MNT/priv-app" ]] && log "  ✓ /system/priv-app present" || { warn "  /system/priv-app not found — checking system/system/"; }

# Some GSIs use system-as-root where /system is mounted at /
# Check for nested structure
if [[ -d "$SYSTEM_MNT/system/app" ]]; then
    log "  → System-as-root (SAR) GSI: apps at /system/system/app"
    log "  → Updating mount path to nested system dir…"
    # For SAR, our inject script needs to look at /system/system/ not /system/
    echo "$SYSTEM_MNT/system" > "$WORK_DIR/.mount_path"
    warn "SAR GSI detected — inject_arizenos.sh will target $SYSTEM_MNT/system/"
fi

echo ""
ok "═══════════════════════════════════════"
ok "  GSI unpacked and mounted!"
ok "═══════════════════════════════════════"
log "  System : $SYSTEM_MNT"
log "  Size   : $(df -h $SYSTEM_MNT | awk 'NR==2{print $3"/"$2" used ("$5")"}')"
log "  Next   : sudo bash scripts/inject_arizenos.sh"
