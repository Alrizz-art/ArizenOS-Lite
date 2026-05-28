#!/usr/bin/env bash
# ArizenOS Lite — inject_arizenos.sh (FIXED v4)
# Injects ArizenOS components + sets Arizen Launcher as default HOME
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="${SYSTEM_MNT:-$(cat $WORK_DIR/.mount_path 2>/dev/null || echo $WORK_DIR/system_mount)}"
CONFIG_DIR="$(pwd)/config"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

[[ -d "$SYSTEM_MNT/app" || -d "$SYSTEM_MNT/priv-app" ]] || \
    fail "System not mounted at $SYSTEM_MNT. Run unpack_ap.sh first."

log "ArizenOS Lite — System Injector v4"
log "System: $SYSTEM_MNT"
echo ""

# ── 1. DEBLOAT ────────────────────────────────────────────────────────────────
log "[1/8] Samsung debloat..."
REMOVED=0
if [[ -f "$CONFIG_DIR/debloat_list.txt" ]]; then
    while IFS= read -r pkg; do
        [[ -z "$pkg" || "$pkg" == \#* ]] && continue
        for dir in app priv-app; do
            if [[ -d "$SYSTEM_MNT/$dir/$pkg" ]]; then
                rm -rf "$SYSTEM_MNT/$dir/$pkg"
                log "  ✓ Removed: $pkg"
                ((REMOVED++))
                break
            fi
        done
    done < "$CONFIG_DIR/debloat_list.txt"
    ok "Debloat: removed $REMOVED packages"
else
    warn "debloat_list.txt not found, skipping debloat"
fi

# ── 2. REMOVE SAMSUNG LAUNCHER (make ArizenLauncher the only HOME app) ───────
log "[2/8] Replacing Samsung launcher..."
SAMSUNG_LAUNCHERS=(
    "NexusLauncher"
    "SecLauncher2"
    "SecLauncher3"
    "TouchWizHome"
    "Launcher3"
)
LAUNCHER_REMOVED=0
for launcher in "${SAMSUNG_LAUNCHERS[@]}"; do
    for dir in priv-app app; do
        if [[ -d "$SYSTEM_MNT/$dir/$launcher" ]]; then
            rm -rf "$SYSTEM_MNT/$dir/$launcher"
            log "  ✓ Removed Samsung launcher: $launcher"
            ((LAUNCHER_REMOVED++))
            break
        fi
    done
done
# Also check by APK filename pattern
find "$SYSTEM_MNT/priv-app" "$SYSTEM_MNT/app" -maxdepth 2 \
    -name "SecLauncher*.apk" -o -name "TouchWizHome.apk" -o -name "NexusLauncher.apk" \
    2>/dev/null | while read f; do
    rm -rf "$(dirname "$f")"
    log "  ✓ Removed: $(basename $f)"
done
ok "Samsung launchers removed: $LAUNCHER_REMOVED"

# ── 3. ARIZEN LAUNCHER (install as priv-app = default HOME) ──────────────────
log "[3/8] Arizen Launcher..."
LAUNCHER_APK=$(find "$REPO_ROOT" -name "ArizenLauncher.apk" 2>/dev/null | head -1)
if [[ -n "$LAUNCHER_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenLauncher"
    cp "$LAUNCHER_APK" "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"
    ok "Arizen Launcher installed to priv-app"

    # Set as preferred HOME via preferred-apps XML
    mkdir -p "$SYSTEM_MNT/etc/preferred-apps"
    cat > "$SYSTEM_MNT/etc/preferred-apps/arizen_launcher.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<preferred-activities>
    <item name="com.arizen.launcher/.MainActivity" match="0x108000" always="true">
        <set name="com.arizen.launcher/.MainActivity"/>
        <filter>
            <action name="android.intent.action.MAIN"/>
            <cat name="android.intent.category.HOME"/>
            <cat name="android.intent.category.DEFAULT"/>
        </filter>
    </item>
</preferred-activities>
EOF
    chmod 644 "$SYSTEM_MNT/etc/preferred-apps/arizen_launcher.xml"
    ok "Arizen Launcher set as default HOME via preferred-apps"
else
    warn "ArizenLauncher.apk not found — APK must be built first"
    warn "In GitHub Actions, this is built automatically before this step"
    warn "To build manually: cd arizen-launcher && ./gradlew assembleRelease"
fi

# ── 4. ARIZEN CORE AI ────────────────────────────────────────────────────────
log "[4/8] Arizen Core (AI layer)..."
CORE_APK=$(find "$REPO_ROOT" -name "ArizenCore.apk" 2>/dev/null | head -1)
if [[ -n "$CORE_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenCore"
    cp "$CORE_APK" "$SYSTEM_MNT/priv-app/ArizenCore/ArizenCore.apk"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenCore/ArizenCore.apk"
    ok "Arizen Core installed"
else
    warn "ArizenCore.apk not found — skipping AI layer"
fi

# ── 5. ARIZEN SETTINGS ───────────────────────────────────────────────────────
log "[5/8] Arizen Settings..."
SETTINGS_APK=$(find "$REPO_ROOT" -name "ArizenSettings.apk" 2>/dev/null | head -1)
if [[ -n "$SETTINGS_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenSettings"
    cp "$SETTINGS_APK" "$SYSTEM_MNT/priv-app/ArizenSettings/ArizenSettings.apk"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenSettings/ArizenSettings.apk"
    ok "Arizen Settings installed"
else
    warn "ArizenSettings.apk not found — skipping custom settings"
fi

# ── 6. BUILD.PROP PATCH ───────────────────────────────────────────────────────
log "[6/8] Patching build.prop..."
BUILD_PROP="$SYSTEM_MNT/build.prop"
[[ ! -f "$BUILD_PROP" ]] && fail "build.prop not found at $BUILD_PROP"

patch_prop() {
    local key="$1" val="$2"
    if grep -q "^$key=" "$BUILD_PROP" 2>/dev/null; then
        sed -i "s|^$key=.*|$key=$val|" "$BUILD_PROP"
    else
        echo "$key=$val" >> "$BUILD_PROP"
    fi
}

# ArizenOS branding
patch_prop "ro.build.display.id"        "ArizenOS Lite v1.0"
patch_prop "ro.product.brand"           "ArizenOS"
patch_prop "ro.product.name"            "arizen_t295"
patch_prop "ro.product.device"          "SM-T295"
patch_prop "ro.build.description"       "ArizenOS-Lite v1.0 ArizenOS SM-T295 release-keys"
patch_prop "ro.build.user"              "arizen"
patch_prop "ro.build.host"              "ArizenOS-Builder"

# Performance — 2GB RAM optimized
patch_prop "ro.config.max_starting_bg"           "4"
patch_prop "ro.sys.fw.bg_apps_limit"             "12"
patch_prop "ro.sys.fw.use_trim_settings"         "true"
patch_prop "persist.sys.purgeable_assets"        "1"
patch_prop "ro.config.low_ram"                   "false"
patch_prop "dalvik.vm.heapstartsize"             "8m"
patch_prop "dalvik.vm.heapgrowthlimit"           "192m"
patch_prop "dalvik.vm.heapsize"                  "512m"
patch_prop "dalvik.vm.heaptargetutilization"     "0.75"
patch_prop "dalvik.vm.heapminfree"               "512k"
patch_prop "dalvik.vm.heapmaxfree"               "8m"

# ZRAM via build.prop
patch_prop "ro.config.zram_options"              "true"
patch_prop "ro.lmk.kill_heaviest_task"           "true"
patch_prop "ro.lmk.kill_timeout_ms"              "100"
patch_prop "ro.lmk.swap_free_low_percentage"     "20"

# Animation & rendering
patch_prop "ro.surface_flinger.max_frame_buffer_acquired_buffers" "3"
patch_prop "debug.sf.reuse_framebuffers"         "1"
patch_prop "ro.config.hw_quickpoweron"           "true"

# ArizenOS identification
patch_prop "ro.arizen.version"                   "1.0"
patch_prop "ro.arizen.device"                    "SM-T295"
patch_prop "ro.arizen.variant"                   "Lite"
patch_prop "ro.arizen.build.date"                "$(date +%Y%m%d)"

ok "build.prop patched"

# ── 7. ZRAM via Android init.rc ──────────────────────────────────────────────
log "[7/8] ZRAM config (init.rc)..."
INIT_RC_DIR="$SYSTEM_MNT/etc/init"
mkdir -p "$INIT_RC_DIR"
cat > "$INIT_RC_DIR/arizen_zram.rc" << 'EOF'
# ArizenOS Lite — ZRAM for SM-T295 (2GB RAM)
on post-fs-data
    write /sys/block/zram0/comp_algorithm lz4
    write /sys/block/zram0/disksize 536870912
    exec /system/bin/sh -c "mkswap /dev/block/zram0 && swapon /dev/block/zram0 -p 5"
EOF
chmod 644 "$INIT_RC_DIR/arizen_zram.rc"
ok "ZRAM init.rc installed"

# ── 8. BOOT ANIMATION ────────────────────────────────────────────────────────
log "[8/8] Boot animation..."
BOOTANIM=$(find "$REPO_ROOT" -name "bootanimation.zip" 2>/dev/null | head -1)
if [[ -n "$BOOTANIM" ]]; then
    cp "$BOOTANIM" "$SYSTEM_MNT/media/bootanimation.zip"
    chmod 644 "$SYSTEM_MNT/media/bootanimation.zip"
    ok "Boot animation installed"
else
    warn "bootanimation.zip not found — using stock"
fi

echo ""
ok "All ArizenOS Lite components injected!"
log "Summary:"
log "  Debloated:      $REMOVED packages removed"
log "  Samsung launchers removed: $LAUNCHER_REMOVED"
[[ -n "${LAUNCHER_APK:-}" ]] && log "  Launcher:       ArizenLauncher installed + set as default HOME"
log "  build.prop:     ArizenOS branding + performance tweaks"
log "  ZRAM:           /system/etc/init/arizen_zram.rc"
log "  Preferred apps: /system/etc/preferred-apps/arizen_launcher.xml"
echo ""
log "Next: ./scripts/repack_ap.sh"
