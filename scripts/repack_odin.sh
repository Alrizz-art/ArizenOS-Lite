#!/usr/bin/env bash
# ArizenOS Lite — repack_odin.sh
# Repack modified system → Odin-flashable AP.tar.md5
# Works for both LineageOS-based and Samsung stock-based builds
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="${SYSTEM_MNT:-$(cat $WORK_DIR/.mount_path 2>/dev/null || echo $WORK_DIR/system_mount)}"
SYSTEM_RAW="$WORK_DIR/system_raw.img"
OUTPUT_DIR="$(pwd)/output"
BASE_TYPE=$(cat "$WORK_DIR/.base_type" 2>/dev/null || echo "lineageos")

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BLUE='\033[0;34m'; NC='\033[0m'
log()     { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()      { echo -e "${GREEN}[✓]${NC} $*"; }
warn()    { echo -e "${YELLOW}[!]${NC} $*"; }
fail()    { echo -e "${RED}[✗]${NC} $*"; exit 1; }
section() { echo -e "\n${BLUE}══════════════════════════════════════${NC}"; \
            echo -e "${BLUE} $*${NC}"; \
            echo -e "${BLUE}══════════════════════════════════════${NC}"; }

[[ ! -f "$SYSTEM_RAW" ]] && fail "system_raw.img not found. Run unpack_lineage.sh first."
mkdir -p "$OUTPUT_DIR"

log "ArizenOS Lite — Odin Repacker"
log "Base: $BASE_TYPE"

# ─────────────────────────────────────────────────────────────────────────────
section "[1] Unmount system image"
# ─────────────────────────────────────────────────────────────────────────────
if mount | grep -qF "$SYSTEM_MNT"; then
    log "Syncing filesystem…"
    sync
    log "Unmounting $SYSTEM_MNT…"
    sudo umount "$SYSTEM_MNT" 2>/dev/null || \
    sudo umount -l "$SYSTEM_MNT" 2>/dev/null || \
    fail "Cannot unmount $SYSTEM_MNT — files may be in use"
    ok "Unmounted"
else
    warn "System not mounted (or already unmounted)"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[2] Filesystem repair + shrink"
# ─────────────────────────────────────────────────────────────────────────────
log "Running filesystem check…"
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || \
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || \
    warn "e2fsck warnings — proceeding anyway"

log "Shrinking image to minimum size…"
resize2fs -M "$SYSTEM_RAW" 2>/dev/null || warn "resize2fs -M failed — using current size"
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || true

SIZE=$(du -sh "$SYSTEM_RAW" | cut -f1)
log "System image size after shrink: $SIZE"

# ─────────────────────────────────────────────────────────────────────────────
section "[3] Convert raw → Android sparse image"
# ─────────────────────────────────────────────────────────────────────────────
SPARSE_IMG="$WORK_DIR/system_sparse.img"

if command -v img2simg >/dev/null 2>&1; then
    log "Converting raw → sparse…"
    img2simg "$SYSTEM_RAW" "$SPARSE_IMG"
    ok "Sparse image: $(du -sh $SPARSE_IMG | cut -f1)"
else
    log "img2simg not found — attempting install…"
    apt-get install -y -qq android-tools-fsutils 2>/dev/null || true
    if command -v img2simg >/dev/null 2>&1; then
        img2simg "$SYSTEM_RAW" "$SPARSE_IMG"
        ok "Sparse image: $(du -sh $SPARSE_IMG | cut -f1)"
    else
        warn "img2simg unavailable — using raw ext4 (Odin accepts both)"
        cp "$SYSTEM_RAW" "$SPARSE_IMG"
    fi
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[4] Gather Odin partition files"
# ─────────────────────────────────────────────────────────────────────────────
LINEAGE_ZIP=$(cat "$WORK_DIR/.lineage_source" 2>/dev/null || echo "")
FIRMWARE_DIR="$(pwd)/firmware"
STAGING="$WORK_DIR/odin_staging"
mkdir -p "$STAGING"

# Always use our modified system
cp "$SPARSE_IMG" "$STAGING/system.img"
log "  ✓ system.img (modified — ArizenOS)"

# Extract boot.img from LineageOS zip if available
LINEAGE_ZIP_PATH=$(find "$FIRMWARE_DIR" \
    \( -name "lineage-*.zip" -o -name "*T295*.zip" -o -name "*gtaslte*.zip" \) \
    2>/dev/null | sort -V | tail -1 || true)

if [[ -n "$LINEAGE_ZIP_PATH" ]]; then
    log "Extracting boot.img from LineageOS zip…"
    BOOT_NAME=""
    # Try common boot image names
    for name in boot.img boot.img.lz4; do
        if unzip -l "$LINEAGE_ZIP_PATH" 2>/dev/null | grep -q "$name"; then
            unzip -o "$LINEAGE_ZIP_PATH" "$name" -d "$STAGING" 2>/dev/null || true
            BOOT_NAME="$name"
            break
        fi
    done

    if [[ -n "$BOOT_NAME" && -f "$STAGING/$BOOT_NAME" ]]; then
        ok "  ✓ $BOOT_NAME (LineageOS kernel — preserves rooting/TWRP compatibility)"
    else
        warn "boot.img not found in zip — Odin package will be system-only"
    fi
fi

# Check for vendor.img (some LineageOS builds)
for name in vendor.img vendor.img.lz4; do
    if [[ -n "$LINEAGE_ZIP_PATH" ]] && unzip -l "$LINEAGE_ZIP_PATH" 2>/dev/null | grep -q "$name"; then
        log "Extracting $name…"
        unzip -o "$LINEAGE_ZIP_PATH" "$name" -d "$STAGING" 2>/dev/null || true
        [[ -f "$STAGING/$name" ]] && ok "  ✓ $name (vendor partition — keeps hardware compatibility)"
        break
    fi
done

ls -lh "$STAGING/"

# ─────────────────────────────────────────────────────────────────────────────
section "[5] Create Odin AP.tar.md5"
# ─────────────────────────────────────────────────────────────────────────────
VERSION=$(cat "$WORK_DIR/../arizen-assets/build.prop.patch" 2>/dev/null | \
    grep "^ro.arizen.version=" | cut -d= -f2 || echo "1.1")
DATE=$(date +%Y%m%d)
DEVICE="SM-T295"
OUTPUT_FILE="$OUTPUT_DIR/ArizenOS-Lite_v${VERSION}_${DEVICE}_${DATE}_AP.tar.md5"

log "Creating AP tar: $(basename $OUTPUT_FILE)…"

# Create tar without directory prefix — Odin expects flat archive
cd "$STAGING"
TAR_FILES=$(ls *.img *.lz4 2>/dev/null | tr '\n' ' ')
log "  Packing: $TAR_FILES"

# GNU tar is required — macOS tar doesn't support --format=gnu
if tar --version 2>&1 | grep -q "GNU"; then
    tar -H ustar -c $TAR_FILES | md5sum - | {
        read hash _
        tar -H ustar -c $TAR_FILES > "$OUTPUT_FILE.tmp"
        printf "%s  %s\n" "$hash" "$(basename $OUTPUT_FILE)" >> "$OUTPUT_FILE.tmp"
        mv "$OUTPUT_FILE.tmp" "$OUTPUT_FILE"
    }
else
    # BSD tar fallback (macOS)
    warn "BSD tar detected — using gtar if available"
    GTAR=$(command -v gtar || command -v tar)
    $GTAR -c -f - $TAR_FILES | {
        tee "$OUTPUT_FILE.tmp" | md5sum | {
            read hash _
            cat "$OUTPUT_FILE.tmp" > "$OUTPUT_FILE"
            printf "%s  %s\n" "$hash" "$(basename $OUTPUT_FILE)" >> "$OUTPUT_FILE"
            rm -f "$OUTPUT_FILE.tmp"
        }
    }
fi
cd - > /dev/null

# ─────────────────────────────────────────────────────────────────────────────
section "[6] Verify Odin package"
# ─────────────────────────────────────────────────────────────────────────────
log "Validating $OUTPUT_FILE…"

# 1. File size check (must be > 100MB)
FILE_BYTES=$(stat -c%s "$OUTPUT_FILE" 2>/dev/null || stat -f%z "$OUTPUT_FILE")
FILE_MB=$(( FILE_BYTES / 1048576 ))
[[ $FILE_MB -lt 100 ]] && warn "Output file suspiciously small: ${FILE_MB}MB"
ok "  Size: ${FILE_MB}MB"

# 2. GNU tar magic bytes — must start with ustar
TAR_MAGIC=$(head -c 265 "$OUTPUT_FILE" | tail -c 6 | od -c 2>/dev/null | head -1 | awk '{print $2$3$4$5$6}' || true)
log "  Tar magic block check: $(head -c 265 "$OUTPUT_FILE" | strings | head -1)"

# 3. MD5 check — last line of file is md5 hash
TAIL=$(tail -c 100 "$OUTPUT_FILE")
if echo "$TAIL" | grep -qE "[0-9a-f]{32}"; then
    ok "  MD5 trailer: present"
else
    warn "  MD5 trailer not found — may fail Odin validation"
fi

# 4. List contents
log "  Archive contents:"
# Strip MD5 trailer to list contents (MD5 is last 36+ bytes)
FILE_SIZE=$(stat -c%s "$OUTPUT_FILE" 2>/dev/null || stat -f%z "$OUTPUT_FILE")
head -c $(( FILE_SIZE - 40 )) "$OUTPUT_FILE" | tar -t 2>/dev/null | \
    while IFS= read -r line; do log "    → $line"; done || \
    warn "  Could not list tar contents (MD5 trailer may affect)"

# ─────────────────────────────────────────────────────────────────────────────
section "[7] Done"
# ─────────────────────────────────────────────────────────────────────────────
echo ""
ok "══════════════════════════════════════"
ok "  ArizenOS Lite package ready!"
ok "══════════════════════════════════════"
ok "  Output: $(basename $OUTPUT_FILE)"
ok "  Size  : ${FILE_MB}MB"
ok "  Path  : $OUTPUT_FILE"
echo ""
log "Flash via Odin (Windows):"
log "  1. Boot SM-T295 into Download Mode"
log "     (Power off → Vol Down + Power → Vol Up to confirm)"
log "  2. Open Odin 3.14+"
log "  3. Click AP → select the .tar.md5 file"
log "  4. Options: ✅ Auto Reboot · ✅ F.Reset · ❌ Re-Partition"
log "  5. Start → wait for PASS!"
echo ""
log "Flash via Heimdall (Linux/macOS):"
log "  heimdall flash --SYSTEM work/system_raw.img"
