#!/usr/bin/env bash
# ArizenOS Lite — download_lineage.sh
# Download LineageOS zip for SM-T295 (Galaxy Tab A 8.0" 2019 LTE)
# ─────────────────────────────────────────────────────────────────────────────
# SM-T295 LineageOS device codename: gtaslte  (unofficial builds)
# Official builds may not exist — check these sources:
#   https://forum.xda-developers.com/c/samsung-galaxy-tab-a-8-0-2019.9148/
#   https://xdaforums.com search: "SM-T295 LineageOS"
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BLUE='\033[0;34m'; NC='\033[0m'
log()     { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()      { echo -e "${GREEN}[✓]${NC} $*"; }
warn()    { echo -e "${YELLOW}[!]${NC} $*"; }
fail()    { echo -e "${RED}[✗]${NC} $*"; exit 1; }
section() { echo -e "\n${BLUE}══════════════════════════════════════${NC}"; \
            echo -e "${BLUE} $*${NC}"; \
            echo -e "${BLUE}══════════════════════════════════════${NC}"; }

FIRMWARE_DIR="$(pwd)/firmware"
mkdir -p "$FIRMWARE_DIR"

DEVICE="SM-T295"
CODENAME="gtaslte"

section "ArizenOS Lite — LineageOS Base Downloader"
log "Device  : $DEVICE"
log "Codename: $CODENAME"
log "Target  : $FIRMWARE_DIR/"

# ─────────────────────────────────────────────────────────────────────────────
# Check if already downloaded
# ─────────────────────────────────────────────────────────────────────────────
EXISTING=$(find "$FIRMWARE_DIR" -name "lineage-*-$CODENAME*.zip" 2>/dev/null | head -1)
if [[ -n "$EXISTING" ]]; then
    ok "LineageOS zip already present: $(basename $EXISTING)"
    echo "$(realpath $EXISTING)" > "$FIRMWARE_DIR/.lineage_zip_path"
    exit 0
fi

# ─────────────────────────────────────────────────────────────────────────────
# Try official LineageOS download API
# ─────────────────────────────────────────────────────────────────────────────
section "Checking LineageOS Official Builds…"
API_URL="https://download.lineageos.org/api/v1/${CODENAME}/nightly/last/"
log "API: $API_URL"

RESPONSE=$(curl -sL --max-time 10 "$API_URL" 2>/dev/null || echo "")
if echo "$RESPONSE" | grep -q '"filename"'; then
    FILENAME=$(echo "$RESPONSE" | node -e \
        "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{
          try{ const r=JSON.parse(d); console.log(r.response[0].filename); }
          catch(e){ console.log(''); }
        })" 2>/dev/null || echo "")
    DOWNLOAD_URL=$(echo "$RESPONSE" | node -e \
        "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{
          try{ const r=JSON.parse(d); console.log(r.response[0].url); }
          catch(e){ console.log(''); }
        })" 2>/dev/null || echo "")

    if [[ -n "$DOWNLOAD_URL" && -n "$FILENAME" ]]; then
        log "Found: $FILENAME"
        log "URL  : $DOWNLOAD_URL"
        log "Downloading…"
        wget -O "$FIRMWARE_DIR/$FILENAME" \
             --progress=bar:force \
             --show-progress \
             "$DOWNLOAD_URL" \
        && ok "Downloaded: $FILENAME" \
        && echo "$FIRMWARE_DIR/$FILENAME" > "$FIRMWARE_DIR/.lineage_zip_path" \
        && exit 0
    fi
fi

# ─────────────────────────────────────────────────────────────────────────────
# Official not found → manual instructions
# ─────────────────────────────────────────────────────────────────────────────
section "Manual Download Required"
warn "No official LineageOS build found for $CODENAME ($DEVICE)"
warn ""
warn "SM-T295 may only have UNOFFICIAL LineageOS builds."
warn "Recommended sources:"
warn ""
warn "  1. XDA Forums (best source):"
warn "     https://xdaforums.com/c/samsung-galaxy-tab-a-8-0-2019.9148/"
warn "     Search: 'LineageOS' or 'AOSP' or 'ArizenOS'"
warn ""
warn "  2. SourceForge:"
warn "     https://sourceforge.net/search/?q=SM-T295+lineage"
warn ""
warn "  3. LineageOS unofficial:"
warn "     https://lineageos.zulipchat.com (ask community)"
warn ""
warn "AFTER downloading the LineageOS zip:"
warn ""
warn "  mkdir -p firmware"
warn "  cp /path/to/lineage-*-$CODENAME*.zip firmware/"
warn "  bash scripts/unpack_lineage.sh"
warn ""

# ─────────────────────────────────────────────────────────────────────────────
# Fallback: check if user manually placed a zip
# ─────────────────────────────────────────────────────────────────────────────
MANUAL=$(find "$FIRMWARE_DIR" \( -name "lineage-*.zip" -o -name "*T295*.zip" -o -name "*gtaslte*.zip" \) 2>/dev/null | head -1)
if [[ -n "$MANUAL" ]]; then
    warn "Found a zip in firmware/ — using it: $(basename $MANUAL)"
    echo "$(realpath $MANUAL)" > "$FIRMWARE_DIR/.lineage_zip_path"
    ok "Set as LineageOS source. Run: bash scripts/unpack_lineage.sh"
    exit 0
fi

echo ""
log "Place your LineageOS zip in: $FIRMWARE_DIR/"
log "Then run: bash scripts/unpack_lineage.sh"
exit 1
