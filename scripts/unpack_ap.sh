#!/usr/bin/env bash
# ArizenOS Lite — unpack_ap.sh (FIXED v2)
# Works in: GitHub Actions, Docker, macOS+Docker, local Linux
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

log "Extracting AP.tar.md5..."
# Samsung AP.tar.md5: tar + 32-byte MD5 appended at end
# Strip trailing MD5 and extract
AP_SIZE=$(stat -c%s "$AP_TAR" 2>/dev/null || stat -f%z "$AP_TAR")
CONTENT_SIZE=$((AP_SIZE - 32))
dd if="$AP_TAR" bs=1 count="$CONTENT_SIZE" 2>/dev/null | tar -x -C "$AP_EXTRACTED" || \
  tar --ignore-zeros -x -C "$AP_EXTRACTED" -f "$AP_TAR" 2>/dev/null || \
  tar -x -C "$AP_EXTRACTED" -f "$AP_TAR"
ok "AP extracted"

# Locate images
SYSTEM_IMG=$(find "$AP_EXTRACTED" -maxdepth 1 \( -name "system.img" -o -name "system.img.ext4" -o -name "system.img.lz4" \) | head -1)
BOOT_IMG=$(find "$AP_EXTRACTED" -maxdepth 1 \( -name "boot.img" -o -name "boot.img.lz4" \) | head -1)
[[ -z "$SYSTEM_IMG" ]] && fail "system.img not found in AP"
ok "system image: $SYSTEM_IMG"
[[ -n "$BOOT_IMG" ]] && ok "boot image: $BOOT_IMG (preserved — stock kernel)"

# Decompress lz4 if needed
if echo "$SYSTEM_IMG" | grep -q "\.lz4$"; then
  log "Decompressing lz4..."
  command -v lz4 >/dev/null || apt-get install -y -qq lz4 2>/dev/null || true
  lz4 -d "$SYSTEM_IMG" "${SYSTEM_IMG%.lz4}"
  SYSTEM_IMG="${SYSTEM_IMG%.lz4}"
  ok "lz4 decompressed"
fi

# Install simg2img if missing
if ! command -v simg2img >/dev/null 2>&1; then
  log "Installing simg2img..."
  apt-get install -y -qq android-tools-fsutils 2>/dev/null || \
  apt-get install -y -qq simg2img 2>/dev/null || \
  (apt-get install -y -qq build-essential wget 2>/dev/null && \
   wget -qO /usr/local/bin/simg2img \
     "https://github.com/xpirt/simg2img/releases/download/1.1/simg2img-linux" && \
   chmod +x /usr/local/bin/simg2img) || true
fi

# Convert sparse → raw
IMG_TYPE=$(file "$SYSTEM_IMG" | grep -oP "(Android sparse image|Linux rev \d+\.\d+ ext\d+)" | head -1)
log "Image type: $IMG_TYPE"

if file "$SYSTEM_IMG" | grep -q "sparse"; then
  log "Converting sparse → raw..."
  simg2img "$SYSTEM_IMG" "$SYSTEM_RAW"
  ok "Converted to raw: $SYSTEM_RAW"
else
  cp "$SYSTEM_IMG" "$SYSTEM_RAW"
  ok "Copied raw image"
fi

# Filesystem repair
log "Checking filesystem..."
e2fsck -f -y "$SYSTEM_RAW" 2>/dev/null || warn "fsck warnings (normal for stock firmware)"

# Expand for modification headroom (add 256MB)
CURRENT_SIZE=$(du -b "$SYSTEM_RAW" | cut -f1)
NEW_SIZE=$(( (CURRENT_SIZE / 1073741824 + 1) * 1073741824 + 268435456 ))
log "Resizing image to $(( NEW_SIZE / 1073741824 ))GB..."
resize2fs "$SYSTEM_RAW" "${NEW_SIZE}b" 2>/dev/null || true

# Mount
log "Mounting system image..."
if mount --help 2>&1 | grep -q "\-o loop" || [[ "$(uname)" == "Linux" ]]; then
  mkdir -p "$SYSTEM_MNT"
  sudo mount -t ext4 -o loop,rw "$SYSTEM_RAW" "$SYSTEM_MNT" || \
  sudo mount -o loop,rw "$SYSTEM_RAW" "$SYSTEM_MNT" || \
  fail "Cannot mount system.img — ensure you have root/sudo access"
  ok "Mounted at $SYSTEM_MNT"
else
  fail "Unsupported OS for mounting. Use Linux or Docker."
fi

echo "$SYSTEM_MNT" > "$WORK_DIR/.mount_path"
ok "Unpack complete! Next: ./scripts/inject_arizenos.sh"
