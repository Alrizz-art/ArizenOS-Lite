#!/usr/bin/env bash
# ArizenOS Lite — inject_arizenos.sh (FIXED v2)
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="${SYSTEM_MNT:-$WORK_DIR/system_mount}"
CONFIG_DIR="$(pwd)/config"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

# Validate mount
[[ ! -d "$SYSTEM_MNT/app" ]] && \
  [[ ! -d "$SYSTEM_MNT/priv-app" ]] && \
  fail "System not mounted at $SYSTEM_MNT. Run unpack_ap.sh first."

log "ArizenOS Lite — System Injector v2"
log "System: $SYSTEM_MNT"
echo ""

# ── 1. DEBLOAT ────────────────────────────────────────────────────────────────
log "[1/7] Samsung Debloat..."
REMOVED=0
if [[ -f "$CONFIG_DIR/debloat_list.txt" ]]; then
  while IFS= read -r pkg; do
    [[ -z "$pkg" || "$pkg" == \#* ]] && continue
    for dir in app priv-app; do
      [[ -d "$SYSTEM_MNT/$dir/$pkg" ]] && {
        rm -rf "$SYSTEM_MNT/$dir/$pkg"
        log "  ✓ Removed: $pkg"
        ((REMOVED++))
      }
    done
  done < "$CONFIG_DIR/debloat_list.txt"
  ok "Debloat: $REMOVED packages removed"
else
  warn "debloat_list.txt not found, skipping"
fi

# ── 2. ARIZEN LAUNCHER ────────────────────────────────────────────────────────
log "[2/7] Arizen Launcher..."
LAUNCHER_APK=$(find . -name "ArizenLauncher.apk" 2>/dev/null | head -1)
if [[ -n "$LAUNCHER_APK" ]]; then
  mkdir -p "$SYSTEM_MNT/priv-app/ArizenLauncher"
  cp "$LAUNCHER_APK" "$SYSTEM_MNT/priv-app/ArizenLauncher/"
  chmod 644 "$SYSTEM_MNT/priv-app/ArizenLauncher/"*.apk
  ok "Arizen Launcher installed"
else
  warn "ArizenLauncher.apk not built yet — build with Android Studio first"
fi

# ── 3. ARIZEN CORE AI ─────────────────────────────────────────────────────────
log "[3/7] Arizen Core (AI)..."
CORE_APK=$(find . -name "ArizenCore.apk" 2>/dev/null | head -1)
if [[ -n "$CORE_APK" ]]; then
  mkdir -p "$SYSTEM_MNT/priv-app/ArizenCore"
  cp "$CORE_APK" "$SYSTEM_MNT/priv-app/ArizenCore/"
  chmod 644 "$SYSTEM_MNT/priv-app/ArizenCore/"*.apk
  ok "Arizen Core installed"
else
  warn "ArizenCore.apk not built yet"
fi

# ── 4. ARIZEN SETTINGS ────────────────────────────────────────────────────────
log "[4/7] Arizen Settings..."
SETTINGS_APK=$(find . -name "ArizenSettings.apk" 2>/dev/null | head -1)
if [[ -n "$SETTINGS_APK" ]]; then
  mkdir -p "$SYSTEM_MNT/priv-app/ArizenSettings"
  cp "$SETTINGS_APK" "$SYSTEM_MNT/priv-app/ArizenSettings/"
  chmod 644 "$SYSTEM_MNT/priv-app/ArizenSettings/"*.apk
  ok "Arizen Settings installed"
else
  warn "ArizenSettings.apk not built yet"
fi

# ── 5. BUILD.PROP PATCH ────────────────────────────────────────────────────────
log "[5/7] Patching build.prop..."
BUILD_PROP="$SYSTEM_MNT/build.prop"
[[ ! -f "$BUILD_PROP" ]] && fail "build.prop not found"

patch_prop() {
  local key="$1" val="$2"
  if grep -q "^$key=" "$BUILD_PROP"; then
    sed -i "s|^$key=.*|$key=$val|" "$BUILD_PROP"
  else
    echo "$key=$val" >> "$BUILD_PROP"
  fi
}

# ArizenOS branding
patch_prop "ro.build.display.id"     "ArizenOS Lite v1.0"
patch_prop "ro.product.brand"        "ArizenOS"
patch_prop "ro.product.name"         "arizen_t295"
patch_prop "ro.product.device"       "SM-T295"
patch_prop "ro.build.description"    "ArizenOS Lite v1.0 — AI-native Android for SM-T295"

# Performance — 2GB RAM optimized
patch_prop "ro.config.max_starting_bg"         "4"
patch_prop "ro.sys.fw.bg_apps_limit"           "12"
patch_prop "ro.sys.fw.use_trim_settings"       "true"
patch_prop "persist.sys.purgeable_assets"      "1"
patch_prop "ro.config.low_ram"                 "false"
patch_prop "dalvik.vm.heapstartsize"           "8m"
patch_prop "dalvik.vm.heapgrowthlimit"         "192m"
patch_prop "dalvik.vm.heapsize"               "512m"
patch_prop "dalvik.vm.heaptargetutilization"   "0.75"
patch_prop "dalvik.vm.heapminfree"             "512k"
patch_prop "dalvik.vm.heapmaxfree"             "8m"

# Animation smoothness
patch_prop "ro.surface_flinger.max_frame_buffer_acquired_buffers" "3"
patch_prop "debug.sf.reuse_framebuffers"       "1"

# ArizenOS marker
patch_prop "ro.arizen.version"                 "1.0"
patch_prop "ro.arizen.device"                  "SM-T295"
patch_prop "ro.arizen.variant"                 "Lite"

ok "build.prop patched ($(grep -c "^ro.arizen" "$BUILD_PROP") Arizen props)"

# ── 6. ZRAM INIT SCRIPT ───────────────────────────────────────────────────────
log "[6/7] Installing ZRAM init script..."
INIT_D="$SYSTEM_MNT/etc/init.d"
mkdir -p "$INIT_D"
if [[ -f "$CONFIG_DIR/zram_config.sh" ]]; then
  cp "$CONFIG_DIR/zram_config.sh" "$INIT_D/99arizen_zram"
  chmod 755 "$INIT_D/99arizen_zram"
  ok "ZRAM script installed"
fi

# ── 7. BOOT ANIMATION ─────────────────────────────────────────────────────────
log "[7/7] Boot animation..."
BOOTANIM=$(find . -name "bootanimation.zip" 2>/dev/null | head -1)
if [[ -n "$BOOTANIM" ]]; then
  cp "$BOOTANIM" "$SYSTEM_MNT/media/bootanimation.zip"
  chmod 644 "$SYSTEM_MNT/media/bootanimation.zip"
  ok "Boot animation installed"
else
  warn "bootanimation.zip not found — using stock"
fi

echo ""
ok "All ArizenOS components injected!"
log "Next: ./scripts/repack_ap.sh"
