#!/usr/bin/env bash
# ArizenOS Lite — Auto Builder for macOS (Docker)
# Satu script untuk: download firmware + build + package Odin
# Jalankan: chmod +x auto_build_mac.sh && ./auto_build_mac.sh

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }
header() { echo -e "\n${BOLD}${CYAN}━━━ $* ━━━${NC}\n"; }

clear
echo -e "${BOLD}${CYAN}"
cat << 'LOGO'
    _          _                ___  ____
   / \   _ __ (_)_______  ____/ _ \/ ___|
  / _ \ | '__|| |_  / _ \|  _ \ | | \___ \
 / ___ \| |   | |/ /  __/| | | |_| |___) |
/_/   \_\_|   |_/___\___||_|  \___/|____/
 Lite — Auto Builder for SM-T295
LOGO
echo -e "${NC}"

# ─── CHECK PREREQUISITES ──────────────────────────────────────────────────────
header "Checking Prerequisites"

command -v docker &>/dev/null || fail "Docker not found. Install from: https://www.docker.com/products/docker-desktop/"
docker info &>/dev/null || fail "Docker is not running. Open Docker Desktop first."
ok "Docker is running"

# ─── CONFIG ───────────────────────────────────────────────────────────────────
DEVICE_MODEL="SM-T295"
DEVICE_REGION="XSP"      # Global (change if needed: INS=India, EUX=Europe, etc.)
WORK_DIR="$(pwd)"
FIRMWARE_DIR="$WORK_DIR/firmware"
OUTPUT_DIR="$WORK_DIR/output"

log "Device: $DEVICE_MODEL ($DEVICE_REGION)"
mkdir -p "$FIRMWARE_DIR" "$OUTPUT_DIR"

# ─── REGION SELECTION ─────────────────────────────────────────────────────────
header "Select Your Region"
echo "Common regions for SM-T295:"
echo "  1) XSP — Global / International"
echo "  2) INS — India"
echo "  3) EUX — Europe"
echo "  4) DBT — Germany"
echo "  5) BTU — UK"
echo "  6) Other (enter manually)"
echo ""
read -p "Select region [1-6, default=1]: " REGION_CHOICE

case "$REGION_CHOICE" in
    2) DEVICE_REGION="INS" ;;
    3) DEVICE_REGION="EUX" ;;
    4) DEVICE_REGION="DBT" ;;
    5) DEVICE_REGION="BTU" ;;
    6) read -p "Enter region code: " DEVICE_REGION ;;
    *) DEVICE_REGION="XSP" ;;
esac
ok "Region set to: $DEVICE_REGION"

# ─── DOWNLOAD FIRMWARE ────────────────────────────────────────────────────────
header "Downloading Samsung Firmware"
log "Model: $DEVICE_MODEL | Region: $DEVICE_REGION"
log "Downloading from Samsung servers via samloader..."
log "(This may take 10-30 minutes depending on your connection — firmware is ~1.5GB)"
echo ""

docker run --rm -it \
    -v "$FIRMWARE_DIR":/firmware \
    python:3.11-slim bash -c "
        pip install samloader --quiet &&
        samloader \
            --model $DEVICE_MODEL \
            --region $DEVICE_REGION \
            download \
            --output /firmware \
            --auto-download
    " || {
        # Fallback: try without auto-download flag (older samloader)
        docker run --rm -it \
            -v "$FIRMWARE_DIR":/firmware \
            python:3.11-slim bash -c "
                pip install samloader --quiet &&
                samloader \
                    -m $DEVICE_MODEL \
                    -r $DEVICE_REGION \
                    download \
                    -O /firmware
            "
    }

# Verify firmware downloaded
FIRMWARE_ZIP=$(find "$FIRMWARE_DIR" -name "*.zip" 2>/dev/null | head -1)
[[ -z "$FIRMWARE_ZIP" ]] && fail "Firmware download failed. Check your internet connection."
ok "Firmware downloaded: $(basename $FIRMWARE_ZIP)"

# ─── BUILD ARIZENOS ──────────────────────────────────────────────────────────
header "Building ArizenOS Lite"
log "Starting build in Docker container..."
echo ""

docker run --rm --privileged \
    -v "$WORK_DIR":/workspace \
    ubuntu:22.04 bash << 'DOCKERSCRIPT'

set -e
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }

log "Installing build tools..."
apt-get update -qq
apt-get install -y -qq android-tools-fsutils e2fsprogs tar openssl unzip file 2>/dev/null || \
apt-get install -y -qq e2fsprogs tar openssl unzip file wget

# Install simg2img/img2simg if not available
command -v simg2img &>/dev/null || {
    log "Installing simg2img..."
    apt-get install -y -qq android-tools-fsutils 2>/dev/null || \
    (wget -qO /usr/local/bin/simg2img "https://github.com/anestisb/android-simg2img/releases/download/1.1.4/simg2img-linux-x64" && \
     chmod +x /usr/local/bin/simg2img) || true
}

cd /workspace
chmod +x scripts/*.sh

FIRMWARE_ZIP=$(find firmware/ -name "*.zip" | head -1)
log "Using firmware: $FIRMWARE_ZIP"

log "Step 1/5: Extracting firmware..."
./scripts/extract_firmware.sh "$FIRMWARE_ZIP"

log "Step 2/5: Unpacking AP partition..."
./scripts/unpack_ap.sh

log "Step 3/5: Injecting ArizenOS components..."
./scripts/inject_arizenos.sh

log "Step 4/5: Repacking AP..."
./scripts/repack_ap.sh

log "Step 5/5: Packaging for Odin..."
./scripts/package_odin.sh

DOCKERSCRIPT

# ─── DONE ─────────────────────────────────────────────────────────────────────
OUTPUT_FILE=$(find "$OUTPUT_DIR" -name "*.tar.md5" | head -1)

echo ""
echo -e "${GREEN}${BOLD}"
echo "╔════════════════════════════════════════════════╗"
echo "║         ArizenOS Lite Build Complete!          ║"
echo "╚════════════════════════════════════════════════╝"
echo -e "${NC}"

if [[ -n "$OUTPUT_FILE" ]]; then
    FILE_SIZE=$(du -sh "$OUTPUT_FILE" | cut -f1)
    ok "Output: $(basename $OUTPUT_FILE) ($FILE_SIZE)"
    echo ""
    log "Flash via Odin (Windows/Mac with Heimdall):"
    log "  1. Boot SM-T295 into Download Mode"
    log "     (Power + Vol Down + Bixby, then Vol Up)"
    log "  2. Open Odin → click AP"
    log "  3. Select: output/$(basename $OUTPUT_FILE)"
    log "  4. Click Start"
    echo ""
    ok "File location: $OUTPUT_FILE"
else
    warn "Output file not found — check logs above for errors"
fi
