#!/usr/bin/env bash
# ArizenOS Lite — package_odin.sh (v4)
# MD5 self-check is now fatal — bad packages deleted before download
set -euo pipefail

WORK_DIR="$(pwd)/work"
AP_EXTRACTED="$WORK_DIR/ap_extracted"
OUTPUT_DIR="$(pwd)/output"

VERSION="${ARIZEN_VERSION:-1.0}"
DEVICE="SM-T295"
BUILD_DATE=$(date +%Y%m%d)
OUTPUT_NAME="ArizenOSLite_v${VERSION}_${DEVICE}_${BUILD_DATE}_AP.tar.md5"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ ! -d "$AP_EXTRACTED" ]] && fail "ap_extracted not found. Run previous steps first."

# Accept system.img OR system.img.ext4 (repack may output either name)
SYSTEM_IMG=$(find "$AP_EXTRACTED" -maxdepth 1 \( -name "system.img" -o -name "system.img.ext4" \) 2>/dev/null | head -1)
[[ -z "$SYSTEM_IMG" ]] && fail "system.img not found in ap_extracted. Run repack_ap.sh first."
log "System image: $(basename $SYSTEM_IMG) ($(du -sh $SYSTEM_IMG | cut -f1))"

mkdir -p "$OUTPUT_DIR"
log "Packaging: $OUTPUT_NAME"

# ── 1. Collect all partition files ────────────────────────────────────────────
# Odin AP partition: system, boot, recovery, vbmeta, dtbo, userdata, vendor
log "Partition images found in AP:"
IMGS=()
while IFS= read -r -d '' f; do
    fname=$(basename "$f")
    size=$(du -sh "$f" | cut -f1)
    log "  $fname ($size)"
    IMGS+=("$fname")
done < <(find "$AP_EXTRACTED" -maxdepth 1 \( \
    -name "*.img" -o \
    -name "*.img.ext4" -o \
    -name "*.img.lz4" -o \
    -name "*.bin" -o \
    -name "*.mbn" -o \
    -name "*.pit" \
    \) ! -name "*.md5" -print0 | sort -z)

[[ ${#IMGS[@]} -eq 0 ]] && fail "No partition images found in $AP_EXTRACTED"

# ── 2. Create GNU tar (required by Odin) ─────────────────────────────────────
TEMP_TAR="$OUTPUT_DIR/AP_temp.tar"
log "Creating GNU tar archive (${#IMGS[@]} files)..."
# IMPORTANT: -C so paths inside tar are bare filenames (no directory prefix)
# IMPORTANT: --format=gnu for Odin compatibility
tar --format=gnu -C "$AP_EXTRACTED" -cf "$TEMP_TAR" "${IMGS[@]}"
TAR_SIZE=$(du -sh "$TEMP_TAR" | cut -f1)
ok "Tar created: $TAR_SIZE"

# Verify tar integrity before MD5
log "Verifying tar structure..."
tar -t -f "$TEMP_TAR" >/dev/null || fail "Tar verification failed — corrupted archive"
ok "Tar verified (all files readable)"

# ── 3. Append MD5 (Samsung Odin format) ──────────────────────────────────────
log "Computing MD5..."
MD5=$(md5sum "$TEMP_TAR" | awk '{print $1}')
log "MD5: $MD5"

# Samsung Odin format: raw tar bytes + 32-byte ASCII MD5 (NO newline at end)
cat "$TEMP_TAR" > "$OUTPUT_DIR/$OUTPUT_NAME"
printf "%s" "$MD5" >> "$OUTPUT_DIR/$OUTPUT_NAME"
rm -f "$TEMP_TAR"

# ── 4. Final verification — FATAL on mismatch ────────────────────────────────
FINAL_FILE="$OUTPUT_DIR/$OUTPUT_NAME"
FINAL_SIZE=$(du -sh "$FINAL_FILE" | cut -f1)
FINAL_BYTES=$(stat -c%s "$FINAL_FILE" 2>/dev/null || stat -f%z "$FINAL_FILE")

log "Running MD5 self-check..."
VERIFY_MD5=$(head -c $(( FINAL_BYTES - 32 )) "$FINAL_FILE" | md5sum | awk '{print $1}')
STORED_MD5=$(tail -c 32 "$FINAL_FILE")

if [[ "$VERIFY_MD5" == "$STORED_MD5" ]]; then
    ok "MD5 self-check PASSED ✓  ($STORED_MD5)"
else
    log "  Stored:   $STORED_MD5"
    log "  Computed: $VERIFY_MD5"
    rm -f "$FINAL_FILE"
    fail "MD5 self-check FAILED — package deleted. Odin would reject this file. Check system.img integrity."
fi

echo ""
ok "╔══════════════════════════════════════════════════╗"
ok "║     ArizenOS Lite — Odin Package Ready!         ║"
ok "╚══════════════════════════════════════════════════╝"
ok "File: output/$OUTPUT_NAME"
ok "Size: $FINAL_SIZE"
ok "MD5:  $MD5"
echo ""
log "Flash instructions (Windows — Odin):"
log "  1. Boot SM-T295 into Download Mode:"
log "     Power off → hold [Power + Vol Down + Bixby] → press [Vol Up]"
log "  2. Connect USB to PC"
log "  3. Open Odin v3.14.4+"
log "  4. Click [AP] → select output/$OUTPUT_NAME"
log "  5. Options tab: ✓ Auto Reboot  ✓ F.Reset Time  ✗ Re-Partition"
log "  6. Click [Start] — wait for green PASS!"
echo ""
log "Flash instructions (macOS/Linux — Heimdall):"
log "  heimdall flash --SYSTEM output/$OUTPUT_NAME"
