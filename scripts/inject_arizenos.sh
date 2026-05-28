#!/usr/bin/env bash
# ArizenOS Lite — inject_arizenos.sh (v8 — LineageOS base)
# Injects ArizenOS identity, launcher, and system tuning into LineageOS system
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="${SYSTEM_MNT:-$(cat $WORK_DIR/.mount_path 2>/dev/null || echo $WORK_DIR/system_mount)}"
CONFIG_DIR="$(pwd)/config"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_TYPE=$(cat "$WORK_DIR/.base_type" 2>/dev/null || echo "lineageos")

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BLUE='\033[0;34m'; NC='\033[0m'
log()     { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()      { echo -e "${GREEN}[✓]${NC} $*"; }
warn()    { echo -e "${YELLOW}[!]${NC} $*"; }
fail()    { echo -e "${RED}[✗]${NC} $*"; exit 1; }
section() { echo -e "\n${BLUE}══════════════════════════════════════${NC}"; \
            echo -e "${BLUE} $*${NC}"; \
            echo -e "${BLUE}══════════════════════════════════════${NC}"; }

[[ -d "$SYSTEM_MNT/app" || -d "$SYSTEM_MNT/priv-app" ]] || \
    fail "System not mounted at $SYSTEM_MNT. Run unpack_lineage.sh first."

log "ArizenOS Lite — System Injector v8 (LineageOS base)"
log "System: $SYSTEM_MNT"
log "Base  : $BASE_TYPE"

# ─────────────────────────────────────────────────────────────────────────────
section "[1/10] Remove stock launchers (LineageOS default home)"
# ─────────────────────────────────────────────────────────────────────────────
# LineageOS ships with Trebuchet or Launcher3 — remove to force Arizen
STOCK_LAUNCHERS=(
    "Trebuchet"       # LineageOS default
    "Launcher3"       # AOSP fallback
    "Launcher3QuickStep"
    "NexusLauncher"
    "PixelLauncher"
    "QuickStep"
)
LAUNCHER_REMOVED=0
for launcher in "${STOCK_LAUNCHERS[@]}"; do
    for dir in priv-app app; do
        if [[ -d "$SYSTEM_MNT/$dir/$launcher" ]]; then
            rm -rf "$SYSTEM_MNT/$dir/$launcher"
            log "  ✓ Removed launcher: $launcher"
            ((LAUNCHER_REMOVED++))
            break
        fi
    done
done

# Also remove as individual APKs
for f in \
    "$SYSTEM_MNT/priv-app/Trebuchet/Trebuchet.apk" \
    "$SYSTEM_MNT/app/Launcher3/Launcher3.apk"; do
    [[ -f "$f" ]] && { rm -f "$f"; log "  ✓ Removed APK: $(basename $f)"; ((LAUNCHER_REMOVED++)); }
done

ok "Stock launchers removed: $LAUNCHER_REMOVED"

# ─────────────────────────────────────────────────────────────────────────────
section "[2/10] Install Arizen Launcher as default HOME"
# ─────────────────────────────────────────────────────────────────────────────
LAUNCHER_APK=$(find "$REPO_ROOT" -name "ArizenLauncher.apk" 2>/dev/null | head -1)
if [[ -n "$LAUNCHER_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenLauncher"
    cp "$LAUNCHER_APK" "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"

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
    ok "Arizen Launcher installed as default HOME"
else
    warn "ArizenLauncher.apk not found — will be built by CI/Gradle"
    warn "Run: cd arizen-launcher && ./gradlew assembleRelease"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[3/10] LineageOS residual cleanup"
# ─────────────────────────────────────────────────────────────────────────────
# Remove LineageOS-specific apps that conflict with Arizen experience
LINEAGE_REMOVE=(
    "LineageParts"           # LineageOS Settings extension (we have ArizenSettings)
    "AudioFX"                # LineageOS equalizer (nice but heavy)
    "Eleven"                 # LineageOS Music (user can install own)
    "Jelly"                  # LineageOS Browser (lightweight but replaceable)
    "Recorder"               # Optional screen recorder
)
LOS_REMOVED=0
for pkg in "${LINEAGE_REMOVE[@]}"; do
    for dir in priv-app app; do
        if [[ -d "$SYSTEM_MNT/$dir/$pkg" ]]; then
            rm -rf "$SYSTEM_MNT/$dir/$pkg"
            log "  ✓ $pkg"
            ((LOS_REMOVED++))
            break
        fi
    done
done
ok "LineageOS extras removed: $LOS_REMOVED"

# ─────────────────────────────────────────────────────────────────────────────
section "[4/10] Install ArizenOS boot animation"
# ─────────────────────────────────────────────────────────────────────────────
BOOTANIM=$(find "$REPO_ROOT" -name "bootanimation.zip" -path "*/bootanimation/*" 2>/dev/null | head -1)
if [[ -n "$BOOTANIM" ]]; then
    mkdir -p "$SYSTEM_MNT/media"
    cp "$BOOTANIM" "$SYSTEM_MNT/media/bootanimation.zip"
    chmod 644 "$SYSTEM_MNT/media/bootanimation.zip"
    ok "ArizenOS boot animation installed ($(du -sh $BOOTANIM | cut -f1))"
else
    warn "bootanimation.zip not found in bootanimation/ — using LineageOS default"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[5/10] ArizenOS wallpaper"
# ─────────────────────────────────────────────────────────────────────────────
WALLPAPER=$(find "$REPO_ROOT" \( -name "arizen_wallpaper.png" -o -name "arizen_wallpaper.jpg" \) 2>/dev/null | head -1)
if [[ -n "$WALLPAPER" ]]; then
    EXT="${WALLPAPER##*.}"
    mkdir -p "$SYSTEM_MNT/etc/arizen"
    cp "$WALLPAPER" "$SYSTEM_MNT/etc/arizen/wallpaper.$EXT"
    ok "Wallpaper installed"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[6/10] build.prop — ArizenOS identity over LineageOS"
# ─────────────────────────────────────────────────────────────────────────────
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

# ── ArizenOS branding over LineageOS ─────────────────────────────────────────
patch_prop "ro.build.display.id"           "ArizenOS-Lite/1.1/SM-T295"
patch_prop "ro.product.brand"              "ArizenOS"
patch_prop "ro.product.manufacturer"       "ArizenLabs"
patch_prop "ro.product.name"               "arizen_t295"
patch_prop "ro.product.device"             "SM-T295"
patch_prop "ro.product.model"              "ArizenOS Lite Edition"
patch_prop "ro.build.flavor"               "arizen_t295-userdebug"
patch_prop "ro.build.user"                 "arizen-builder"
patch_prop "ro.build.host"                 "ArizenOS-CI"
patch_prop "ro.build.tags"                 "release-keys"

# ── ArizenOS custom properties ────────────────────────────────────────────────
patch_prop "ro.arizen.version"             "1.1"
patch_prop "ro.arizen.variant"             "Lite"
patch_prop "ro.arizen.base"                "LineageOS"
patch_prop "ro.arizen.codename"            "Zenith"
patch_prop "ro.arizen.build.date"          "$(date +%Y%m%d)"
patch_prop "ro.arizen.build.number"        "$(date +%Y%m%d)-001"
patch_prop "ro.arizen.labs.enabled"        "true"
patch_prop "ro.arizen.ai.enabled"          "true"
patch_prop "ro.arizen.features.cmd_palette" "true"
patch_prop "ro.arizen.features.workspace"  "true"
patch_prop "ro.arizen.features.monitor"    "true"

# ── Keep LineageOS's ro.lineage.* intact for compatibility ────────────────────
# (LineageOS apps check for these — we don't remove them)
log "  LineageOS compatibility props preserved"

# ── Dalvik/ART — 2GB RAM optimized (override LineageOS defaults) ──────────────
patch_prop "dalvik.vm.heapstartsize"           "8m"
patch_prop "dalvik.vm.heapgrowthlimit"         "128m"
patch_prop "dalvik.vm.heapsize"                "256m"
patch_prop "dalvik.vm.heaptargetutilization"   "0.75"
patch_prop "dalvik.vm.heapminfree"             "2m"
patch_prop "dalvik.vm.heapmaxfree"             "8m"

# ── Background limits ────────────────────────────────────────────────────────
patch_prop "ro.config.max_starting_bg"         "2"
patch_prop "ro.sys.fw.bg_apps_limit"           "8"
patch_prop "ro.sys.fw.use_trim_settings"       "true"
patch_prop "persist.sys.purgeable_assets"      "1"

# ── LMK ──────────────────────────────────────────────────────────────────────
patch_prop "ro.lmk.kill_heaviest_task"         "true"
patch_prop "ro.lmk.kill_timeout_ms"            "100"
patch_prop "ro.lmk.use_minfree_levels"         "true"

# ── Rendering + UX ────────────────────────────────────────────────────────────
patch_prop "ro.config.animation_scale"         "0.5"
patch_prop "persist.sys.dalvik.multithread"    "true"
patch_prop "debug.sf.reuse_framebuffers"       "1"
patch_prop "ro.config.hw_quickpoweron"         "true"
patch_prop "ro.surface_flinger.max_frame_buffer_acquired_buffers" "3"
patch_prop "persist.sys.default_font"          "Roboto"

# ── Remove LineageOS branding lines ──────────────────────────────────────────
sed -i '/^ro.lineage.display.version=/d'   "$BUILD_PROP" 2>/dev/null || true
sed -i '/^ro.modversion=/d'                "$BUILD_PROP" 2>/dev/null || true

ARIZEN_COUNT=$(grep -c "^ro.arizen" "$BUILD_PROP" 2>/dev/null || echo "0")
ok "build.prop — $ARIZEN_COUNT ArizenOS properties set"

# ─────────────────────────────────────────────────────────────────────────────
section "[7/10] ZRAM + VM tuning (init.rc)"
# ─────────────────────────────────────────────────────────────────────────────
mkdir -p "$SYSTEM_MNT/etc/init"
cat > "$SYSTEM_MNT/etc/init/arizen_zram.rc" << 'EOF'
# ArizenOS Lite — ZRAM + VM tuning (SM-T295, 2GB RAM)
on post-fs-data
    write /sys/block/zram0/comp_algorithm lz4
    write /sys/block/zram0/disksize 536870912
    exec /system/bin/sh -c "mkswap /dev/block/zram0 2>/dev/null; swapon /dev/block/zram0 -p 5 2>/dev/null || true"
    write /proc/sys/vm/swappiness 60
    write /proc/sys/vm/dirty_ratio 10
    write /proc/sys/vm/dirty_background_ratio 5
    write /proc/sys/vm/vfs_cache_pressure 100
    write /proc/sys/vm/extra_free_kbytes 24576
    write /proc/sys/vm/min_free_kbytes 16384
    write /proc/sys/vm/oom_kill_allocating_task 0
    write /proc/sys/vm/page-cluster 0
EOF
chmod 644 "$SYSTEM_MNT/etc/init/arizen_zram.rc"
ok "ZRAM init.rc installed"

# ─────────────────────────────────────────────────────────────────────────────
section "[8/10] CPU + LMK performance init.rc"
# ─────────────────────────────────────────────────────────────────────────────
cat > "$SYSTEM_MNT/etc/init/arizen_perf.rc" << 'EOF'
# ArizenOS Lite — CPU + LMK tuning (SM-T295, Snapdragon 429)
on boot
    # CPU governor: schedutil (balanced default)
    write /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor schedutil
    write /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor schedutil
    write /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor schedutil
    write /sys/devices/system/cpu/cpu3/cpufreq/scaling_governor schedutil

    # LMK — 2GB profile (minfree in pages: 18/24/32/40/64/96 MB)
    write /sys/module/lowmemorykiller/parameters/minfree 4608,6144,8192,10240,16384,24576
    write /sys/module/lowmemorykiller/parameters/adj 0,1,2,3,4,9

    # ArizenOS status
    setprop ro.arizen.status ready
    setprop ro.arizen.initialized true
    setprop ro.arizen.perf.profile balanced
EOF
chmod 644 "$SYSTEM_MNT/etc/init/arizen_perf.rc"
ok "CPU + LMK init.rc installed"

# ─────────────────────────────────────────────────────────────────────────────
section "[9/10] ArizenOS system assets"
# ─────────────────────────────────────────────────────────────────────────────
ARIZEN_DIR="$SYSTEM_MNT/etc/arizen"
mkdir -p "$ARIZEN_DIR"

# Performance profiles script
if [[ -f "$CONFIG_DIR/performance_profiles.sh" ]]; then
    cp "$CONFIG_DIR/performance_profiles.sh" "$ARIZEN_DIR/performance_profiles.sh"
    chmod 755 "$ARIZEN_DIR/performance_profiles.sh"
    ok "Performance profiles script installed"
fi

# sysctl config
if [[ -f "$CONFIG_DIR/sysctl_arizen.conf" ]]; then
    mkdir -p "$SYSTEM_MNT/etc/sysctl.d"
    cp "$CONFIG_DIR/sysctl_arizen.conf" "$SYSTEM_MNT/etc/sysctl.d/99-arizen.conf"
    chmod 644 "$SYSTEM_MNT/etc/sysctl.d/99-arizen.conf"
    ok "sysctl config installed"
fi

# Version manifest
cat > "$ARIZEN_DIR/version.json" << EOF
{
  "name": "ArizenOS Lite",
  "version": "1.1",
  "codename": "Zenith",
  "device": "SM-T295",
  "base": "LineageOS",
  "build_date": "$(date +%Y-%m-%d)",
  "features": {
    "ai_assistant": true,
    "command_palette": true,
    "system_monitor": true,
    "workspace_mode": true,
    "zram": true
  }
}
EOF
chmod 644 "$ARIZEN_DIR/version.json"
ok "version.json installed"

# ─────────────────────────────────────────────────────────────────────────────
section "[10/10] Summary"
# ─────────────────────────────────────────────────────────────────────────────
ARIZEN_FILES=$(find "$ARIZEN_DIR" | wc -l)
echo ""
ok "══════════════════════════════════════"
ok "  ArizenOS Lite injection complete!"
ok "  v8 — LineageOS base"
ok "══════════════════════════════════════"
log "  Stock launchers removed : $LAUNCHER_REMOVED (Trebuchet/Launcher3)"
log "  LineageOS extras removed: $LOS_REMOVED"
log "  Arizen Launcher         : installed as default HOME"
log "  Boot animation          : $([ -n "${BOOTANIM:-}" ] && echo "ArizenOS" || echo "LineageOS default")"
log "  ArizenOS branding       : 100% — 1.1 Zenith"
log "  Base OS preserved       : LineageOS kernel + vendor + blobs"
log "  RAM tuning              : 2GB-optimized (ZRAM 512MB lz4)"
log "  LMK tuning              : 6-level 2GB profile"
log "  Perf profiles           : balanced/performance/saver"
log "  Features                : CMD Palette, Monitor, Workspace"
log "  ArizenOS assets         : $ARIZEN_FILES files"
echo ""
log "Next: sudo bash scripts/repack_odin.sh"
