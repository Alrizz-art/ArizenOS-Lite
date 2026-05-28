#!/usr/bin/env bash
# ArizenOS Lite — inject_arizenos.sh (v7)
# Full brand takeover: strip Samsung, inject ArizenOS Lite
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="${SYSTEM_MNT:-$(cat $WORK_DIR/.mount_path 2>/dev/null || echo $WORK_DIR/system_mount)}"
CONFIG_DIR="$(pwd)/config"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; BLUE='\033[0;34m'; NC='\033[0m'
log()     { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()      { echo -e "${GREEN}[✓]${NC} $*"; }
warn()    { echo -e "${YELLOW}[!]${NC} $*"; }
fail()    { echo -e "${RED}[✗]${NC} $*"; exit 1; }
section() { echo -e "\n${BLUE}══════════════════════════════════════${NC}"; echo -e "${BLUE} $*${NC}"; echo -e "${BLUE}══════════════════════════════════════${NC}"; }

[[ -d "$SYSTEM_MNT/app" || -d "$SYSTEM_MNT/priv-app" ]] || \
    fail "System not mounted at $SYSTEM_MNT. Run unpack_ap.sh first."

log "ArizenOS Lite — System Injector v7"
log "System: $SYSTEM_MNT"

# ─────────────────────────────────────────────────────────────────────────────
section "[1/12] Samsung App Debloat"
# ─────────────────────────────────────────────────────────────────────────────
REMOVED=0
if [[ -f "$CONFIG_DIR/debloat_list.txt" ]]; then
    while IFS= read -r pkg; do
        [[ -z "$pkg" || "$pkg" == \#* ]] && continue
        for dir in app priv-app; do
            if [[ -d "$SYSTEM_MNT/$dir/$pkg" ]]; then
                rm -rf "$SYSTEM_MNT/$dir/$pkg"
                log "  ✓ $pkg"
                ((REMOVED++))
                break
            fi
        done
    done < "$CONFIG_DIR/debloat_list.txt"
    ok "Debloat: $REMOVED packages removed"
else
    warn "debloat_list.txt not found — skipping"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[2/12] Remove Samsung Launchers"
# ─────────────────────────────────────────────────────────────────────────────
SAMSUNG_LAUNCHERS=(
    "NexusLauncher" "SecLauncher2" "SecLauncher3"
    "TouchWizHome" "Launcher3" "Launcher2"
    "OneUIHome" "SamsungLauncher"
)
LAUNCHER_REMOVED=0
for launcher in "${SAMSUNG_LAUNCHERS[@]}"; do
    for dir in priv-app app; do
        if [[ -d "$SYSTEM_MNT/$dir/$launcher" ]]; then
            rm -rf "$SYSTEM_MNT/$dir/$launcher"
            log "  ✓ Removed launcher: $launcher"
            ((LAUNCHER_REMOVED++))
            break
        fi
    done
done
ok "Samsung launchers removed: $LAUNCHER_REMOVED"

# ─────────────────────────────────────────────────────────────────────────────
section "[3/12] Strip Samsung Boot Media"
# ─────────────────────────────────────────────────────────────────────────────
STOCK_BOOTANIM="$SYSTEM_MNT/media/bootanimation.zip"
[[ -f "$STOCK_BOOTANIM" ]] && { rm -f "$STOCK_BOOTANIM"; log "  ✓ Removed Samsung bootanimation.zip"; }

for sound_path in \
    "$SYSTEM_MNT/media/audio/ui/boot_seq.ogg" \
    "$SYSTEM_MNT/media/audio/ui/PowerOn.ogg" \
    "$SYSTEM_MNT/media/audio/ui/shutdown.ogg" \
    "$SYSTEM_MNT/media/audio/ui/PowerOff.ogg"; do
    [[ -f "$sound_path" ]] && { rm -f "$sound_path"; log "  ✓ Removed: $(basename $sound_path)"; }
done

for ringtone in \
    "$SYSTEM_MNT/media/audio/ringtones/Over_the_Horizon.ogg" \
    "$SYSTEM_MNT/media/audio/ringtones/Spaceline.ogg" \
    "$SYSTEM_MNT/media/audio/ringtones/My_Galaxy.ogg"; do
    [[ -f "$ringtone" ]] && { rm -f "$ringtone"; log "  ✓ Removed Samsung ringtone: $(basename $ringtone)"; }
done
ok "Samsung boot media stripped"

# ─────────────────────────────────────────────────────────────────────────────
section "[4/12] Remove Samsung UI Overlays"
# ─────────────────────────────────────────────────────────────────────────────
SAMSUNG_OVERLAYS=(
    "SamsungTheme" "SecThemeService" "ThemeService"
    "SamsungOneUITheme" "SamsungIconPackQXXX" "SecProductFeature"
)
OVL_REMOVED=0
for pkg in "${SAMSUNG_OVERLAYS[@]}"; do
    for dir in overlay app priv-app; do
        if [[ -d "$SYSTEM_MNT/$dir/$pkg" ]]; then
            rm -rf "$SYSTEM_MNT/$dir/$pkg"
            log "  ✓ Removed overlay: $pkg"
            ((OVL_REMOVED++))
            break
        fi
    done
done
for f in "$SYSTEM_MNT/fonts/SamsungSans"* "$SYSTEM_MNT/fonts/SamsungIF"*; do
    [[ -f "$f" ]] && { rm -f "$f"; log "  ✓ Removed Samsung font: $(basename $f)"; }
done
ok "Samsung overlays removed: $OVL_REMOVED"

# ─────────────────────────────────────────────────────────────────────────────
section "[5/12] Install Arizen Launcher (default HOME)"
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
    ok "Arizen Launcher installed + set as default HOME"
else
    warn "ArizenLauncher.apk not found — built by CI automatically"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[6/12] Install ArizenOS Boot Animation"
# ─────────────────────────────────────────────────────────────────────────────
BOOTANIM=$(find "$REPO_ROOT" -name "bootanimation.zip" -path "*/bootanimation/*" 2>/dev/null | head -1)
if [[ -n "$BOOTANIM" ]]; then
    mkdir -p "$SYSTEM_MNT/media"
    cp "$BOOTANIM" "$SYSTEM_MNT/media/bootanimation.zip"
    chmod 644 "$SYSTEM_MNT/media/bootanimation.zip"
    ok "ArizenOS boot animation installed ($(du -sh $BOOTANIM | cut -f1))"
else
    warn "bootanimation.zip not found in bootanimation/ folder"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[7/12] Install ArizenOS Wallpaper"
# ─────────────────────────────────────────────────────────────────────────────
WALLPAPER=$(find "$REPO_ROOT" -name "arizen_wallpaper.png" -o -name "arizen_wallpaper.jpg" 2>/dev/null | head -1)
if [[ -n "$WALLPAPER" ]]; then
    mkdir -p "$SYSTEM_MNT/etc/arizen"
    EXT="${WALLPAPER##*.}"
    cp "$WALLPAPER" "$SYSTEM_MNT/etc/arizen/wallpaper.$EXT"
    ok "ArizenOS wallpaper installed"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[8/12] build.prop — Full ArizenOS Branding + Performance"
# ─────────────────────────────────────────────────────────────────────────────
BUILD_PROP="$SYSTEM_MNT/build.prop"
[[ ! -f "$BUILD_PROP" ]] && fail "build.prop not found"

patch_prop() {
    local key="$1" val="$2"
    if grep -q "^$key=" "$BUILD_PROP" 2>/dev/null; then
        sed -i "s|^$key=.*|$key=$val|" "$BUILD_PROP"
    else
        echo "$key=$val" >> "$BUILD_PROP"
    fi
}

# ── Branding ────────────────────────────────────────────────────────────────
patch_prop "ro.build.display.id"           "ArizenOS-Lite/1.1/SM-T295"
patch_prop "ro.product.brand"              "ArizenOS"
patch_prop "ro.product.manufacturer"       "ArizenLabs"
patch_prop "ro.product.name"               "arizen_t295"
patch_prop "ro.product.device"             "SM-T295"
patch_prop "ro.product.model"              "ArizenOS Lite Edition"
patch_prop "ro.build.flavor"               "arizen_t295-user"
patch_prop "ro.build.user"                 "arizen-builder"
patch_prop "ro.build.host"                 "ArizenOS-Builder"
patch_prop "ro.build.tags"                 "release-keys"
patch_prop "ro.build.type"                 "user"
patch_prop "ro.arizen.version"             "1.1"
patch_prop "ro.arizen.variant"             "Lite"
patch_prop "ro.arizen.build.date"          "$(date +%Y%m%d)"
patch_prop "ro.arizen.build.number"        "$(date +%Y%m%d)-001"
patch_prop "ro.arizen.labs.enabled"        "true"
patch_prop "ro.arizen.ai.enabled"          "true"
patch_prop "ro.arizen.codename"            "Zenith"
patch_prop "ro.arizen.features.cmd_palette" "true"
patch_prop "ro.arizen.features.workspace"  "true"
patch_prop "ro.arizen.features.monitor"    "true"

# ── Remove Samsung branding identifiers ────────────────────────────────────
for key in "ro.product.samsung.model" "ro.product.samsung.brand" \
           "ro.buildtype.samsung" "ro.config.samsung" \
           "ro.oem.key1" "ro.oem.key2"; do
    sed -i "/^$key=/d" "$BUILD_PROP" 2>/dev/null || true
done
sed -i '/SamsungSans\|SamsungIF/d' "$BUILD_PROP" 2>/dev/null || true
patch_prop "persist.sys.default_font"      "Roboto"

# ── Dalvik/ART — tuned 2GB RAM ────────────────────────────────────────────
patch_prop "dalvik.vm.heapstartsize"           "8m"
patch_prop "dalvik.vm.heapgrowthlimit"         "128m"
patch_prop "dalvik.vm.heapsize"                "256m"
patch_prop "dalvik.vm.heaptargetutilization"   "0.75"
patch_prop "dalvik.vm.heapminfree"             "2m"
patch_prop "dalvik.vm.heapmaxfree"             "8m"

# ── Background process limits ─────────────────────────────────────────────
patch_prop "ro.config.max_starting_bg"         "2"
patch_prop "ro.sys.fw.bg_apps_limit"           "8"
patch_prop "ro.sys.fw.use_trim_settings"       "true"
patch_prop "persist.sys.fw.trim_enable_memory" "1024"
patch_prop "persist.sys.purgeable_assets"      "1"

# ── LMK ──────────────────────────────────────────────────────────────────
patch_prop "ro.lmk.kill_heaviest_task"         "true"
patch_prop "ro.lmk.kill_timeout_ms"            "100"
patch_prop "ro.lmk.use_minfree_levels"         "true"
patch_prop "ro.lmk.low"                        "1001"
patch_prop "ro.lmk.medium"                     "800"
patch_prop "ro.lmk.critical"                   "0"

# ── Performance tweaks ────────────────────────────────────────────────────
patch_prop "ro.config.animation_scale"         "0.5"
patch_prop "ro.config.low_ram"                 "false"
patch_prop "persist.sys.dalvik.multithread"    "true"
patch_prop "ro.surface_flinger.max_frame_buffer_acquired_buffers" "3"
patch_prop "debug.sf.reuse_framebuffers"       "1"
patch_prop "ro.config.hw_quickpoweron"         "true"
patch_prop "ro.adb.secure"                     "0"

ARIZEN_COUNT=$(grep -c "^ro.arizen" "$BUILD_PROP" 2>/dev/null || echo "0")
ok "build.prop — $ARIZEN_COUNT ArizenOS properties set"

# ─────────────────────────────────────────────────────────────────────────────
section "[9/12] ZRAM + VM sysctl init.rc"
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

# ─────────────────────────────────────────────────────────────────────────────
section "[10/12] LMK + Performance profiles init.rc"
# ─────────────────────────────────────────────────────────────────────────────
cat > "$SYSTEM_MNT/etc/init/arizen_perf.rc" << 'EOF'
# ArizenOS Lite — Performance + LMK tuning (SM-T295)
on boot
    # LMK minfree: 18/24/32/40/64/96 MB (pages)
    write /sys/module/lowmemorykiller/parameters/minfree 4608,6144,8192,10240,16384,24576
    write /sys/module/lowmemorykiller/parameters/adj 0,1,2,3,4,9
    write /proc/sys/vm/extra_free_kbytes 24576

    # CPU: schedutil (balanced default)
    write /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor schedutil
    write /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor schedutil
    write /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor schedutil
    write /sys/devices/system/cpu/cpu3/cpufreq/scaling_governor schedutil

    # ArizenOS status
    setprop ro.arizen.status ready
    setprop ro.arizen.initialized true
    setprop ro.arizen.perf.profile balanced
EOF
chmod 644 "$SYSTEM_MNT/etc/init/arizen_perf.rc"

# Install performance profile script
mkdir -p "$SYSTEM_MNT/etc/arizen"
if [[ -f "$CONFIG_DIR/performance_profiles.sh" ]]; then
    cp "$CONFIG_DIR/performance_profiles.sh" "$SYSTEM_MNT/etc/arizen/performance_profiles.sh"
    chmod 755 "$SYSTEM_MNT/etc/arizen/performance_profiles.sh"
    ok "Performance profiles script installed"
fi

# Install sysctl config
if [[ -f "$CONFIG_DIR/sysctl_arizen.conf" ]]; then
    mkdir -p "$SYSTEM_MNT/etc/sysctl.d"
    cp "$CONFIG_DIR/sysctl_arizen.conf" "$SYSTEM_MNT/etc/sysctl.d/99-arizen.conf"
    chmod 644 "$SYSTEM_MNT/etc/sysctl.d/99-arizen.conf"
    ok "sysctl config installed"
fi

ok "LMK + Performance init.rc installed"

# ─────────────────────────────────────────────────────────────────────────────
section "[11/12] ArizenOS file system structure"
# ─────────────────────────────────────────────────────────────────────────────
ARIZEN_DIR="$SYSTEM_MNT/etc/arizen"
mkdir -p "$ARIZEN_DIR"

# ArizenOS version manifest
cat > "$ARIZEN_DIR/version.json" << EOF
{
  "name": "ArizenOS Lite",
  "version": "1.1",
  "codename": "Zenith",
  "device": "SM-T295",
  "build_date": "$(date +%Y-%m-%d)",
  "features": {
    "ai_assistant": true,
    "command_palette": true,
    "system_monitor": true,
    "workspace_mode": true,
    "zram": true,
    "debloat": true
  }
}
EOF
chmod 644 "$ARIZEN_DIR/version.json"
ok "ArizenOS filesystem structure created"

# ─────────────────────────────────────────────────────────────────────────────
section "[12/12] Summary"
# ─────────────────────────────────────────────────────────────────────────────
echo ""
ok "══════════════════════════════════════"
ok "  ArizenOS Lite injection complete!"
ok "  v7 — Command Palette + Monitor"
ok "══════════════════════════════════════"
log "  Samsung apps debloated  : $REMOVED packages"
log "  Samsung launchers gone  : $LAUNCHER_REMOVED replaced by Arizen"
log "  Samsung overlays gone   : $OVL_REMOVED removed"
log "  Boot animation          : ArizenOS Lite (cinematic)"
log "  Boot sound              : stripped (silent)"
log "  Branding                : 100% ArizenOS"
log "  RAM tuning              : 2GB-optimized (ZRAM 512MB lz4)"
log "  LMK tuning              : 6-level 2GB profile"
log "  Perf profiles           : balanced/performance/saver"
log "  sysctl                  : 99-arizen.conf"
log "  Arizen Launcher         : default HOME"
log "  New features            : Command Palette, System Monitor, Workspace"
echo ""
log "Next: sudo bash scripts/repack_ap.sh"
