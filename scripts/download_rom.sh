#!/usr/bin/env bash
# ArizenOS Lite — download_rom.sh
# Guide + helper for downloading base ROM for SM-T295
# Supported bases: GSI (Generic System Image) or LineageOS zip
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

FIRMWARE_DIR="$(pwd)/firmware"
mkdir -p "$FIRMWARE_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BLUE='\033[0;34m'; NC='\033[0m'
log()     { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()      { echo -e "${GREEN}[✓]${NC} $*"; }
warn()    { echo -e "${YELLOW}[!]${NC} $*"; }
section() { echo -e "\n${BLUE}══════════════════════════════════════${NC}"; \
            echo -e "${BLUE} $*${NC}"; \
            echo -e "${BLUE}══════════════════════════════════════${NC}"; }

# ─────────────────────────────────────────────────────────────────────────────
# Check if already downloaded
# ─────────────────────────────────────────────────────────────────────────────
EXISTING_IMG=$(find "$FIRMWARE_DIR" -name "*.img" -size +200M 2>/dev/null | head -1)
EXISTING_ZIP=$(find "$FIRMWARE_DIR" \( -name "*.zip" -o -name "*.xz" \) -size +200M 2>/dev/null | head -1)

if [[ -n "$EXISTING_IMG" ]]; then
    ok "GSI image already present: $(basename $EXISTING_IMG) ($(du -sh $EXISTING_IMG | cut -f1))"
    echo "$EXISTING_IMG" > "$FIRMWARE_DIR/.rom_path"
    echo "gsi" > "$FIRMWARE_DIR/.rom_type"
    exit 0
fi

if [[ -n "$EXISTING_ZIP" ]]; then
    ok "ROM zip already present: $(basename $EXISTING_ZIP) ($(du -sh $EXISTING_ZIP | cut -f1))"
    echo "$EXISTING_ZIP" > "$FIRMWARE_DIR/.rom_path"
    echo "lineageos" > "$FIRMWARE_DIR/.rom_type"
    exit 0
fi

# ─────────────────────────────────────────────────────────────────────────────
section "ArizenOS Lite — ROM Download Guide for SM-T295"
# ─────────────────────────────────────────────────────────────────────────────
log "Device : Samsung Galaxy Tab A 8.0\" (2019) — SM-T295"
log "Target : $FIRMWARE_DIR/"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
section "OPTION 1 — Superior OS GSI [RECOMMENDED]"
# ─────────────────────────────────────────────────────────────────────────────
warn "GSI = Generic System Image (works on any Treble device, including SM-T295)"
warn ""
warn "Download from XDA thread:"
warn "  https://xdaforums.com/t/rom-gsi-sm-t295-gto-superior-os-12l-13-gsi-for-galaxy-tab-a-8-0-2019.4650847/"
warn ""
warn "Steps in the XDA thread:"
warn "  1. Open the thread above"
warn "  2. Find the download link (usually SourceForge / Google Drive / Telegram)"
warn "  3. Download the .img file (look for 'arm64_bvS' or 'arm64_bgS' variant)"
warn "     → arm64 = 64-bit (SM-T295 CPU)"
warn "     → bvS or bgS = with/without GApps, Slim variant"
warn "  4. Place the .img here: firmware/"
warn ""
warn "  Recommended file: arm64_bvS (no Google Apps — leaner, faster)"
warn "  Example filename: superior_a64_bvS_*.img or system_*.img"
warn ""

# ─────────────────────────────────────────────────────────────────────────────
section "OPTION 2 — Any AOSP/LineageOS GSI (arm64)"
# ─────────────────────────────────────────────────────────────────────────────
log "SM-T295 supports Project Treble — any arm64 GSI will work:"
log ""
log "  LineageOS GSI:"
log "    https://github.com/phhusson/treble_experimentations/releases"
log ""
log "  Generic AOSP GSI (Google):"
log "    https://developer.android.com/topic/generic-system-image/releases"
log "    → Download arm64 variant"
log ""
log "  crDroid GSI:"
log "    https://sourceforge.net/projects/crdroid-gsi/"
log ""
log "  Pixel Experience GSI:"
log "    https://github.com/ponces/treble_build_pe/releases"
log ""

# ─────────────────────────────────────────────────────────────────────────────
section "OPTION 3 — Direct URL download"
# ─────────────────────────────────────────────────────────────────────────────
log "If you have a direct .img download URL, run:"
log ""
log "  wget -P firmware/ 'https://YOUR_DIRECT_URL/system.img'"
log "  # OR:"
log "  curl -L -o firmware/system.img 'https://YOUR_DIRECT_URL/system.img'"
log ""

# ─────────────────────────────────────────────────────────────────────────────
section "After Downloading"
# ─────────────────────────────────────────────────────────────────────────────
log "Once the .img file is in firmware/, run:"
log ""
log "  sudo bash scripts/unpack_gsi.sh"
log ""
log "That's it — the script auto-detects GSI vs LineageOS zip."
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# If URL passed as argument, try to download it
# ─────────────────────────────────────────────────────────────────────────────
if [[ "${1:-}" == http* ]]; then
    section "Downloading from provided URL…"
    FILENAME=$(basename "$1" | sed 's/?.*$//')
    [[ "$FILENAME" != *.img && "$FILENAME" != *.zip ]] && FILENAME="system.img"
    log "Saving to: firmware/$FILENAME"
    wget -O "$FIRMWARE_DIR/$FILENAME" --progress=bar:force "$1" \
        && ok "Downloaded: $FILENAME" \
        && echo "$FIRMWARE_DIR/$FILENAME" > "$FIRMWARE_DIR/.rom_path" \
        && { [[ "$FILENAME" == *.img ]] && echo "gsi" || echo "lineageos"; } > "$FIRMWARE_DIR/.rom_type" \
        || { echo -e "${RED}Download failed${NC}"; exit 1; }
fi
