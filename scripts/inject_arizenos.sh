#!/usr/bin/env bash
# ArizenOS Lite — inject_arizenos.sh (v6)
# Full brand takeover: strip Samsung, inject ArizenOS Lite
set -euo pipefail

WORK_DIR="$(pwd)/work"
SYSTEM_MNT="${SYSTEM_MNT:-$(cat $WORK_DIR/.mount_path 2>/dev/null || echo $WORK_DIR/system_mount)}"
CONFIG_DIR="$(pwd)/config"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "${CYAN}[ArizenOS]${NC} $*"; }
ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
warn() { echo -e "${YELLOW}[!]${NC} $*"; }
fail() { echo -e "${RED}[✗]${NC} $*"; exit 1; }
section() { echo -e "\n${BLUE}══════════════════════════════════════${NC}"; echo -e "${BLUE} $*${NC}"; echo -e "${BLUE}══════════════════════════════════════${NC}"; }

[[ -d "$SYSTEM_MNT/app" || -d "$SYSTEM_MNT/priv-app" ]] || \
    fail "System not mounted at $SYSTEM_MNT. Run unpack_ap.sh first."

log "ArizenOS Lite — System Injector v6"
log "System: $SYSTEM_MNT"

# ─────────────────────────────────────────────────────────────────────────────
section "[1/10] Samsung App Debloat"
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
section "[2/10] Remove Samsung Launchers"
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
section "[3/10] Strip Samsung Boot Media"
# ─────────────────────────────────────────────────────────────────────────────
# Remove Samsung boot animation
STOCK_BOOTANIM="$SYSTEM_MNT/media/bootanimation.zip"
[[ -f "$STOCK_BOOTANIM" ]] && { rm -f "$STOCK_BOOTANIM"; log "  ✓ Removed Samsung bootanimation.zip"; }

# Remove Samsung boot sound
for sound_path in \
    "$SYSTEM_MNT/media/audio/ui/boot_seq.ogg" \
    "$SYSTEM_MNT/media/audio/ui/PowerOn.ogg" \
    "$SYSTEM_MNT/media/audio/ui/shutdown.ogg" \
    "$SYSTEM_MNT/media/audio/ui/PowerOff.ogg"; do
    [[ -f "$sound_path" ]] && { rm -f "$sound_path"; log "  ✓ Removed: $(basename $sound_path)"; }
done

# Remove Samsung ringtones (keep generic Android ones)
for ringtone_dir in \
    "$SYSTEM_MNT/media/audio/ringtones/Over_the_Horizon.ogg" \
    "$SYSTEM_MNT/media/audio/ringtones/Spaceline.ogg" \
    "$SYSTEM_MNT/media/audio/ringtones/My_Galaxy.ogg"; do
    [[ -f "$ringtone_dir" ]] && { rm -f "$ringtone_dir"; log "  ✓ Removed Samsung ringtone: $(basename $ringtone_dir)"; }
done

ok "Samsung boot media stripped"

# ─────────────────────────────────────────────────────────────────────────────
section "[4/10] Remove Samsung UI Overlays"
# ─────────────────────────────────────────────────────────────────────────────
SAMSUNG_OVERLAYS=(
    "SamsungTheme"
    "SecThemeService"
    "ThemeService"
    "SamsungOneUITheme"
    "SamsungIconPackQXXX"
    "SecProductFeature"
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

# Remove Samsung font if present (will fall back to Roboto)
for font_dir in \
    "$SYSTEM_MNT/fonts/SamsungSans*" \
    "$SYSTEM_MNT/fonts/SamsungIF*"; do
    for f in $font_dir; do
        [[ -f "$f" ]] && { rm -f "$f"; log "  ✓ Removed Samsung font: $(basename $f)"; }
    done
done

ok "Samsung overlays removed: $OVL_REMOVED"

# ─────────────────────────────────────────────────────────────────────────────
section "[5/10] Install Arizen Launcher (default HOME)"
# ─────────────────────────────────────────────────────────────────────────────
LAUNCHER_APK=$(find "$REPO_ROOT" -name "ArizenLauncher.apk" 2>/dev/null | head -1)
if [[ -n "$LAUNCHER_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenLauncher"
    cp "$LAUNCHER_APK" "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"

    # Set as preferred HOME via preferred-activities XML
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
section "[6/10] Install ArizenOS Boot Animation"
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
section "[7/10] Install ArizenOS Wallpaper"
# ─────────────────────────────────────────────────────────────────────────────
WALLPAPER=$(find "$REPO_ROOT" -name "arizen_wallpaper.png" -o -name "arizen_wallpaper.jpg" 2>/dev/null | head -1)
if [[ -n "$WALLPAPER" ]]; then
    mkdir -p "$SYSTEM_MNT/etc/arizen"
    cp "$WALLPAPER" "$SYSTEM_MNT/etc/arizen/wallpaper$(echo "${WALLPAPER##*.}" | sed 's/^/./') "
    ok "ArizenOS wallpaper installed"
fi

# ─────────────────────────────────────────────────────────────────────────────
section "[8/10] build.prop — Full ArizenOS Branding + Performance"
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

# ── Branding — full ArizenOS identity ──────────────────────────────────────
patch_prop "ro.build.display.id"           "ArizenOS-Lite/1.0/SM-T295"
patch_prop "ro.build.version.release_or_codename" "ArizenOS Lite 1.0"
patch_prop "ro.product.brand"              "ArizenOS"
patch_prop "ro.product.manufacturer"       "ArizenLabs"
patch_prop "ro.product.name"               "arizen_t295"
patch_prop "ro.product.device"             "SM-T295"
patch_prop "ro.product.model"              "ArizenOS Lite Edition"
patch_prop "ro.build.description"          "arizen_t295-user 9 PPR1.180610.011 ArizenOS release-keys"
patch_prop "ro.build.flavor"               "arizen_t295-user"
patch_prop "ro.build.user"                 "arizen-builder"
patch_prop "ro.build.host"                 "ArizenOS-Builder"
patch_prop "ro.build.tags"                 "release-keys"
patch_prop "ro.build.type"                 "user"
patch_prop "ro.arizen.version"             "1.0"
patch_prop "ro.arizen.variant"             "Lite"
patch_prop "ro.arizen.build.date"          "$(date +%Y%m%d)"
patch_prop "ro.arizen.build.number"        "$(date +%Y%m%d)-001"
patch_prop "ro.arizen.labs.enabled"        "true"
patch_prop "ro.arizen.ai.enabled"          "true"
patch_prop "ro.arizen.codename"            "Zenith"

# ── Remove Samsung branding from build.prop ─────────────────────────────────
for key in \
    "ro.product.samsung.model" \
    "ro.product.samsung.brand" \
    "ro.buildtype.samsung" \
    "ro.config.samsung" \
    "ro.oem.key1" \
    "ro.oem.key2"; do
    sed -i "/^$key=/d" "$BUILD_PROP" 2>/dev/null || true
done
log "  ✓ Samsung build.prop identifiers removed"

# ── Font — use Roboto (remove Samsung Sans reference) ─────────────────────
patch_prop "persist.sys.default_font"      "Roboto"
sed -i '/SamsungSans\|SamsungIF/d' "$BUILD_PROP" 2>/dev/null || true

# ── Dalvik/ART — tuned 2GB RAM ────────────────────────────────────────────
patch_prop "dalvik.vm.heapstartsize"           "8m"
patch_prop "dalvik.vm.heapgrowthlimit"         "128m"
patch_prop "dalvik.vm.heapsize"                "256m"
patch_prop "dalvik.vm.heaptargetutilization"   "0.75"
patch_prop "dalvik.vm.heapminfree"             "2m"
patch_prop "dalvik.vm.heapmaxfree"             "8m"

# ── Background Process Limits ─────────────────────────────────────────────
patch_prop "ro.config.max_starting_bg"         "2"
patch_prop "ro.sys.fw.bg_apps_limit"           "8"
patch_prop "ro.sys.fw.use_trim_settings"       "true"
patch_prop "persist.sys.fw.trim_enable_memory" "1024"
patch_prop "persist.sys.purgeable_assets"      "1"

# ── LMK ─────────────────────────────────────────────────────────────────────
patch_prop "ro.lmk.kill_heaviest_task"         "true"
patch_prop "ro.lmk.kill_timeout_ms"            "100"
patch_prop "ro.lmk.use_minfree_levels"         "true"
patch_prop "ro.lmk.low"                        "1001"
patch_prop "ro.lmk.medium"                     "800"
patch_prop "ro.lmk.critical"                   "0"

# ── Performance ───────────────────────────────────────────────────────────
patch_prop "ro.config.animation_scale"         "0.5"
patch_prop "ro.config.low_ram"                 "false"
patch_prop "persist.sys.dalvik.multithread"    "true"
patch_prop "ro.surface_flinger.max_frame_buffer_acquired_buffers" "3"
patch_prop "debug.sf.reuse_framebuffers"       "1"
patch_prop "ro.config.hw_quickpoweron"         "true"

# ── ADB / Dev convenience (user build) ───────────────────────────────────
patch_prop "ro.adb.secure"                     "0"
patch_prop "ro.debuggable"                     "0"

ARIZEN_COUNT=$(grep -c "^ro.arizen\|^ro.lmk\|^dalvik.vm\|^ro.product.brand=ArizenOS" "$BUILD_PROP" 2>/dev/null || echo "0")
ok "build.prop — $ARIZEN_COUNT ArizenOS properties set"

# ─────────────────────────────────────────────────────────────────────────────
section "[9/10] ZRAM + VM sysctl init.rc"
# ─────────────────────────────────────────────────────────────────────────────
mkdir -p "$SYSTEM_MNT/etc/init"
cat > "$SYSTEM_MNT/etc/init/arizen_zram.rc" << 'EOF'
# ArizenOS Lite — ZRAM + VM tuning for SM-T295 (2GB RAM)
on post-fs-data
    write /sys/block/zram0/comp_algorithm lz4
    write /sys/block/zram0/disksize 536870912
    exec /system/bin/sh -c "mkswap /dev/block/zram0 2>/dev/null; swapon /dev/block/zram0 -p 5 2>/dev/null || true"
    write /proc/sys/vm/swappiness 60
    write /proc/sys/vm/dirty_ratio 10
    write /proc/sys/vm/dirty_background_ratio 5
    write /proc/sys/vm/vfs_cache_pressure 150
    write /proc/sys/vm/extra_free_kbytes 24300
    write /proc/sys/vm/min_free_kbytes 6144
    write /proc/sys/vm/oom_kill_allocating_task 0
EOF
chmod 644 "$SYSTEM_MNT/etc/init/arizen_zram.rc"

# Arizen ambient service init
cat > "$SYSTEM_MNT/etc/init/arizen_services.rc" << 'EOF'
# ArizenOS Lite — Services
on boot
    setprop ro.arizen.status ready
    setprop ro.arizen.initialized true
EOF
chmod 644 "$SYSTEM_MNT/etc/init/arizen_services.rc"
ok "ZRAM + VM sysctl + Arizen services init.rc installed"

# ─────────────────────────────────────────────────────────────────────────────
section "[10/10] Summary"
# ─────────────────────────────────────────────────────────────────────────────
echo ""
ok "══════════════════════════════════════"
ok "  ArizenOS Lite injection complete!"
ok "══════════════════════════════════════"
log "  Samsung apps debloated : $REMOVED packages"
log "  Samsung launchers gone  : $LAUNCHER_REMOVED replaced"
log "  Samsung overlays gone   : $OVL_REMOVED removed"
log "  Boot animation          : ArizenOS Lite (cinematic)"
log "  Boot sound              : stripped (silent)"
log "  Branding                : 100% ArizenOS"
log "  RAM tuning              : 2GB-optimized"
log "  ZRAM                    : 512MB lz4"
log "  Arizen Launcher         : default HOME"
echo ""
log "Next: ./scripts/repack_ap.sh"
