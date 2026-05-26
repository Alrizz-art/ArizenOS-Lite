#!/usr/bin/env bash
# ArizenOS Lite — inject_arizenos.sh
# Injects ArizenOS components into the system partition
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="$WORK_DIR/system_mount"
ARIZEN_ASSETS="$(pwd)/arizen-assets"
CONFIG_DIR="$(pwd)/config"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ ! -d "$SYSTEM_MNT/app" ]] && fail "System not mounted. Run unpack_ap.sh first."

log "ArizenOS Lite — System Injector"
log "Injecting ArizenOS components..."

# --- STEP 1: DEBLOAT ---
log "[1/6] Applying Samsung debloat..."
DEBLOAT_LIST="$CONFIG_DIR/debloat_list.txt"
if [[ -f "$DEBLOAT_LIST" ]]; then
    REMOVED=0
    while IFS= read -r pkg; do
        [[ -z "$pkg" || "$pkg" == \#* ]] && continue
        PKG_PATH="$SYSTEM_MNT/app/$pkg"
        PRIV_PATH="$SYSTEM_MNT/priv-app/$pkg"
        if [[ -d "$PKG_PATH" ]]; then
            rm -rf "$PKG_PATH"
            ((REMOVED++))
            log "  Removed: $pkg"
        elif [[ -d "$PRIV_PATH" ]]; then
            rm -rf "$PRIV_PATH"
            ((REMOVED++))
            log "  Removed: $pkg (priv)"
        fi
    done < "$DEBLOAT_LIST"
    ok "Debloat complete — removed $REMOVED packages"
else
    warn "debloat_list.txt not found, skipping debloat"
fi

# --- STEP 2: ARIZEN LAUNCHER ---
log "[2/6] Installing Arizen Launcher..."
LAUNCHER_APK="$ARIZEN_ASSETS/ArizenLauncher.apk"
if [[ -f "$LAUNCHER_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenLauncher"
    cp "$LAUNCHER_APK" "$SYSTEM_MNT/priv-app/ArizenLauncher/"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"
    ok "Arizen Launcher installed"
else
    warn "ArizenLauncher.apk not found — skipping (build it first)"
fi

# --- STEP 3: ARIZEN CORE (AI) ---
log "[3/6] Installing Arizen Core..."
CORE_APK="$ARIZEN_ASSETS/ArizenCore.apk"
if [[ -f "$CORE_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenCore"
    cp "$CORE_APK" "$SYSTEM_MNT/priv-app/ArizenCore/"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenCore/ArizenCore.apk"
    ok "Arizen Core installed"
else
    warn "ArizenCore.apk not found — skipping (build it first)"
fi

# --- STEP 4: ARIZEN SETTINGS ---
log "[4/6] Installing Arizen Settings..."
SETTINGS_APK="$ARIZEN_ASSETS/ArizenSettings.apk"
if [[ -f "$SETTINGS_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenSettings"
    cp "$SETTINGS_APK" "$SYSTEM_MNT/priv-app/ArizenSettings/"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenSettings/ArizenSettings.apk"
    ok "Arizen Settings installed"
else
    warn "ArizenSettings.apk not found — skipping (build it first)"
fi

# --- STEP 5: PERFORMANCE TUNING ---
log "[5/6] Applying performance optimizations..."
INIT_D="$SYSTEM_MNT/etc/init.d"
mkdir -p "$INIT_D"
cp "$CONFIG_DIR/zram_config.sh" "$INIT_D/99arizen_zram" 2>/dev/null || warn "zram_config.sh not found"
chmod 755 "$INIT_D/99arizen_zram" 2>/dev/null || true

# Apply build.prop tweaks
BUILD_PROP="$SYSTEM_MNT/build.prop"
if [[ -f "$BUILD_PROP" ]]; then
    log "  Patching build.prop..."
    # ArizenOS branding
    sed -i 's/ro.build.display.id=.*/ro.build.display.id=ArizenOS Lite v1.0/' "$BUILD_PROP"
    sed -i 's/ro.product.name=.*/ro.product.name=ArizenOS/' "$BUILD_PROP"
    # Performance tweaks
    grep -q "ro.config.max_starting_bg" "$BUILD_PROP" || \
      echo "ro.config.max_starting_bg=8" >> "$BUILD_PROP"
    grep -q "ro.sys.fw.use_trim_settings" "$BUILD_PROP" || \
      echo "ro.sys.fw.use_trim_settings=true" >> "$BUILD_PROP"
    grep -q "persist.sys.purgeable_assets" "$BUILD_PROP" || \
      echo "persist.sys.purgeable_assets=1" >> "$BUILD_PROP"
    grep -q "ro.vendor.arizen" "$BUILD_PROP" || \
      echo "ro.vendor.arizen=1" >> "$BUILD_PROP"
    ok "build.prop patched"
fi

# --- STEP 6: BOOT ANIMATION ---
log "[6/6] Installing boot animation..."
BOOTANIM="$ARIZEN_ASSETS/bootanimation.zip"
if [[ -f "$BOOTANIM" ]]; then
    cp "$BOOTANIM" "$SYSTEM_MNT/media/bootanimation.zip"
    chmod 644 "$SYSTEM_MNT/media/bootanimation.zip"
    ok "Boot animation installed"
else
    warn "bootanimation.zip not found — using stock"
fi

ok ""
ok "ArizenOS injection complete!"
log "Next step: ./scripts/repack_ap.sh"
