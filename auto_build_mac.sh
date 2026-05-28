#!/usr/bin/env bash
# ArizenOS Lite — Auto Builder for macOS (Docker)
# One script: build ArizenLauncher APK + download firmware + package Odin .tar.md5
# Usage: chmod +x auto_build_mac.sh && ./auto_build_mac.sh
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

# ─── PREREQUISITES ────────────────────────────────────────────────────────────
header "Checking Prerequisites"
command -v docker &>/dev/null || fail "Docker not found. Install: https://www.docker.com/products/docker-desktop/"
docker info &>/dev/null || fail "Docker is not running. Open Docker Desktop first."
ok "Docker: $(docker --version)"

WORK_DIR="$(pwd)"
FIRMWARE_DIR="$WORK_DIR/firmware"
OUTPUT_DIR="$WORK_DIR/output"
DEVICE_MODEL="SM-T295"

mkdir -p "$FIRMWARE_DIR" "$OUTPUT_DIR"

# ─── REGION SELECTION ─────────────────────────────────────────────────────────
header "Select Region"
echo "Regions for SM-T295:"
echo "  1) XSP — Global / International  (recommended)"
echo "  2) INS — India"
echo "  3) EUX — Europe"
echo "  4) DBT — Germany"
echo "  5) BTU — UK"
echo "  6) Other (enter manually)"
echo ""
read -p "Select [1-6, default=1]: " REGION_CHOICE

case "$REGION_CHOICE" in
    2) DEVICE_REGION="INS" ;;
    3) DEVICE_REGION="EUX" ;;
    4) DEVICE_REGION="DBT" ;;
    5) DEVICE_REGION="BTU" ;;
    6) read -p "Enter region code: " DEVICE_REGION ;;
    *) DEVICE_REGION="XSP" ;;
esac
ok "Region: $DEVICE_REGION"

# ─── BUILD ARIZEN LAUNCHER APK ────────────────────────────────────────────────
header "Building ArizenLauncher APK"
log "Using Docker JDK17 + Android SDK container..."

# Check if pre-built APK exists
if [[ -f "$WORK_DIR/ArizenLauncher.apk" ]]; then
    ok "ArizenLauncher.apk already exists — skipping build"
    ok "  Delete ArizenLauncher.apk to force rebuild"
else
    docker run --rm \
        -v "$WORK_DIR":/workspace \
        -w /workspace/arizen-launcher \
        mingc/android-build-box:latest \
        bash -c "
            chmod +x gradlew 2>/dev/null || true
            ./gradlew assembleRelease --no-daemon -q 2>&1 | tail -20
            APK=\$(find . -name '*.apk' -path '*/release/*' 2>/dev/null | head -1)
            [[ -z \"\$APK\" ]] && APK=\$(find . -name '*.apk' 2>/dev/null | head -1)
            [[ -z \"\$APK\" ]] && { echo 'ERROR: APK build failed'; exit 1; }
            cp \"\$APK\" /workspace/ArizenLauncher.apk
            echo \"Built: \$(basename \$APK)\"
        " || {
        warn "Docker Android build failed. Trying alternative approach..."
        # Fallback: use gradle docker image
        docker run --rm \
            -v "$WORK_DIR":/workspace \
            -e ANDROID_SDK_ROOT=/android-sdk \
            -w /workspace/arizen-launcher \
            thyrlian/android-sdk:latest \
            bash -c "
                chmod +x gradlew 2>/dev/null || true
                ./gradlew assembleRelease --no-daemon 2>&1 | tail -30
                cp app/build/outputs/apk/release/*.apk /workspace/ArizenLauncher.apk 2>/dev/null || \
                find . -name '*.apk' -exec cp {} /workspace/ArizenLauncher.apk \; 2>/dev/null
                echo 'APK copied to workspace'
            " || fail "APK build failed. Check arizen-launcher/ for compilation errors."
    }

    [[ -f "$WORK_DIR/ArizenLauncher.apk" ]] || fail "ArizenLauncher.apk not found after build."
    ok "ArizenLauncher.apk built: $(du -sh $WORK_DIR/ArizenLauncher.apk | cut -f1)"
fi

# ─── DOWNLOAD FIRMWARE ────────────────────────────────────────────────────────
header "Downloading Samsung Firmware"

FIRMWARE_ZIP=$(find "$FIRMWARE_DIR" -name "*.zip" 2>/dev/null | head -1)
if [[ -n "$FIRMWARE_ZIP" ]]; then
    ok "Firmware already in firmware/: $(basename $FIRMWARE_ZIP)"
    log "  Delete firmware/*.zip to force re-download"
else
    log "Downloading $DEVICE_MODEL firmware ($DEVICE_REGION) — ~1.5GB, may take 15-30 min..."
    docker run --rm \
        -v "$FIRMWARE_DIR":/firmware \
        python:3.11-slim bash -c "
            pip install samloader -q
            samloader --model $DEVICE_MODEL --region $DEVICE_REGION download --output /firmware --auto-download 2>/dev/null || \
            samloader -m $DEVICE_MODEL -r $DEVICE_REGION download -O /firmware 2>/dev/null || \
            { echo 'Download failed'; exit 1; }
        " || fail "Firmware download failed. Check internet or try a different region."

    FIRMWARE_ZIP=$(find "$FIRMWARE_DIR" -name "*.zip" 2>/dev/null | head -1)
    [[ -z "$FIRMWARE_ZIP" ]] && fail "No firmware zip found after download."
    ok "Downloaded: $(basename $FIRMWARE_ZIP) ($(du -sh $FIRMWARE_ZIP | cut -f1))"
fi

# ─── BUILD FIRMWARE ──────────────────────────────────────────────────────────
header "Building ArizenOS Firmware"
log "Running full build pipeline in privileged Docker container..."

docker run --rm --privileged \
    -v "$WORK_DIR":/workspace \
    ubuntu:22.04 bash << DOCKERSCRIPT
set -e
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
log()  { echo -e "\${CYAN}[ArizenOS]\${NC} \$*"; }
ok()   { echo -e "\${GREEN}[OK]\${NC} \$*"; }
warn() { echo -e "\${YELLOW}[WARN]\${NC} \$*"; }
fail() { echo -e "\${RED}[FAIL]\${NC} \$*"; exit 1; }

log "Installing build tools..."
apt-get update -qq
apt-get install -y -qq android-tools-fsutils e2fsprogs tar lz4 unzip file 2>/dev/null || true
# Fallback: build simg2img from source if package not available
command -v simg2img &>/dev/null || {
    apt-get install -y -qq build-essential libz-dev git curl wget
    git clone --depth=1 https://github.com/anestisb/android-simg2img /tmp/simg2img
    cd /tmp/simg2img && make && cp simg2img img2simg /usr/local/bin/ && cd -
}

cd /workspace
chmod +x scripts/*.sh

FW_ZIP=\$(find firmware/ -name "*.zip" | head -1)
log "Firmware: \$(basename \$FW_ZIP)"

log "[1/5] Extracting firmware..."
bash scripts/extract_firmware.sh "\$FW_ZIP"

log "[2/5] Unpacking AP partition..."
bash scripts/unpack_ap.sh

log "[3/5] Injecting ArizenOS components..."
bash scripts/inject_arizenos.sh

log "[4/5] Repacking AP..."
bash scripts/repack_ap.sh

log "[5/5] Packaging for Odin..."
bash scripts/package_odin.sh

ok "Build pipeline complete!"
DOCKERSCRIPT

# ─── DONE ─────────────────────────────────────────────────────────────────────
OUTPUT_FILE=$(find "$OUTPUT_DIR" -name "*.tar.md5" 2>/dev/null | head -1)

echo ""
echo -e "${BOLD}${GREEN}"
echo "╔════════════════════════════════════════════════╗"
echo "║         ArizenOS Lite Build Complete!          ║"
echo "╚════════════════════════════════════════════════╝"
echo -e "${NC}"

if [[ -n "$OUTPUT_FILE" ]]; then
    FILE_SIZE=$(du -sh "$OUTPUT_FILE" | cut -f1)
    ok "Output: $(basename $OUTPUT_FILE) ($FILE_SIZE)"
    echo ""
    log "Flash via Odin (Windows):"
    log "  1. Boot SM-T295: hold Power + Vol Down + Bixby → press Vol Up"
    log "  2. Connect USB"
    log "  3. Odin → AP → select: output/$(basename $OUTPUT_FILE)"
    log "  4. Options: Auto Reboot ✓  F.Reset Time ✓  Re-Partition ✗"
    log "  5. Start → wait for PASS!"
    echo ""
    log "Flash via Heimdall (macOS/Linux):"
    log "  heimdall detect"
    log "  heimdall flash --SYSTEM work/system_raw.img --no-reboot"
else
    warn "Output not found — check logs above"
    exit 1
fi
