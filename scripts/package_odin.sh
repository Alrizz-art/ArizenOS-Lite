#!/usr/bin/env bash
# ArizenOS Lite — package_odin.sh (FIXED v2)
set -euo pipefail

WORK_DIR="$(pwd)/work"
AP_EXTRACTED="$WORK_DIR/ap_extracted"
OUTPUT_DIR="$(pwd)/output"
SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

VERSION="${ARIZEN_VERSION:-1.0}"
DEVICE="SM-T295"
OUTPUT_NAME="ArizenOSLite_v${VERSION}_${DEVICE}_AP.tar.md5"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ ! -f "$AP_EXTRACTED/system.img" ]] && fail "system.img not found. Run repack_ap.sh first."
mkdir -p "$OUTPUT_DIR"

log "Packaging: $OUTPUT_NAME"

# Build file list — include all partition images and lz4 files
IMGS=$(find "$AP_EXTRACTED" -maxdepth 1 \( \
  -name "*.img" -o -name "*.img.lz4" -o \
  -name "*.bin" -o -name "*.mbn" \) \
  ! -name "*.md5" | sort | xargs -I{} basename {})

log "Partition images:"
for f in $IMGS; do
  SIZE=$(du -sh "$AP_EXTRACTED/$f" | cut -f1)
  log "  $f ($SIZE)"
done

# Create tar (must be GNU tar format for Odin compatibility)
TEMP_TAR="$OUTPUT_DIR/AP_temp.tar"
log "Creating tar archive..."
tar --format=gnu -C "$AP_EXTRACTED" -cf "$TEMP_TAR" $IMGS
ok "Tar created ($(du -sh "$TEMP_TAR" | cut -f1))"

# Append MD5 checksum (Samsung Odin format)
log "Generating MD5..."
MD5=$(md5sum "$TEMP_TAR" | awk '{print $1}')
log "MD5: $MD5"

cat "$TEMP_TAR" > "$OUTPUT_DIR/$OUTPUT_NAME"
printf "%s" "$MD5" >> "$OUTPUT_DIR/$OUTPUT_NAME"
rm -f "$TEMP_TAR"

# Verify
FINAL_SIZE=$(du -sh "$OUTPUT_DIR/$OUTPUT_NAME" | cut -f1)
ok ""
ok "╔══════════════════════════════════════════╗"
ok "║  ArizenOS Lite — Odin Package Ready!    ║"
ok "╚══════════════════════════════════════════╝"
ok "File: output/$OUTPUT_NAME"
ok "Size: $FINAL_SIZE"
ok "MD5:  $MD5"
echo ""
log "Flash via Odin:"
log "  1. Boot SM-T295 → Download Mode"
log "     (Power + Vol Down + Bixby → Vol Up)"
log "  2. Open Odin → AP → select output/$OUTPUT_NAME"
log "  3. Options: Auto Reboot ✓ | F.Reset Time ✓ | Re-Partition ✗"
log "  4. Click Start — wait for PASS!"
