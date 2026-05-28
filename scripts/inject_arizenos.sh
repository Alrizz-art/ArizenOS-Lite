#!/usr/bin/env bash
# ArizenOS Lite — inject_arizenos.sh (v5)
# Debloat + default launcher + agressive 2GB RAM tuning
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

log "ArizenOS Lite — System Injector v5"
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
                log "  ✓ $pkg"
                ((REMOVED++))
                break
            fi
        done
    done < "$CONFIG_DIR/debloat_list.txt"
    ok "Debloat: $REMOVED packages removed"
else
    warn "debloat_list.txt not found"
fi

# ── 2. REPLACE SAMSUNG LAUNCHER ───────────────────────────────────────────────
log "[2/8] Removing Samsung launcher..."
SAMSUNG_LAUNCHERS=(
    "NexusLauncher" "SecLauncher2" "SecLauncher3"
    "TouchWizHome" "Launcher3" "Launcher2"
)
LAUNCHER_REMOVED=0
for launcher in "${SAMSUNG_LAUNCHERS[@]}"; do
    for dir in priv-app app; do
        if [[ -d "$SYSTEM_MNT/$dir/$launcher" ]]; then
            rm -rf "$SYSTEM_MNT/$dir/$launcher"
            log "  ✓ Removed: $launcher"
            ((LAUNCHER_REMOVED++))
            break
        fi
    done
done
ok "Samsung launchers removed: $LAUNCHER_REMOVED"

# ── 3. ARIZEN LAUNCHER ───────────────────────────────────────────────────────
log "[3/8] Arizen Launcher..."
LAUNCHER_APK=$(find "$REPO_ROOT" -name "ArizenLauncher.apk" 2>/dev/null | head -1)
if [[ -n "$LAUNCHER_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenLauncher"
    cp "$LAUNCHER_APK" "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenLauncher/ArizenLauncher.apk"
    # Set as preferred HOME
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
    warn "ArizenLauncher.apk not found (built by CI automatically)"
fi

# ── 4. ARIZEN CORE AI ────────────────────────────────────────────────────────
log "[4/8] Arizen Core..."
CORE_APK=$(find "$REPO_ROOT" -name "ArizenCore.apk" 2>/dev/null | head -1)
if [[ -n "$CORE_APK" ]]; then
    mkdir -p "$SYSTEM_MNT/priv-app/ArizenCore"
    cp "$CORE_APK" "$SYSTEM_MNT/priv-app/ArizenCore/ArizenCore.apk"
    chmod 644 "$SYSTEM_MNT/priv-app/ArizenCore/ArizenCore.apk"
    ok "Arizen Core installed"
else
    warn "ArizenCore.apk not found — skipping"
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
    warn "ArizenSettings.apk not found — skipping"
fi

# ── 6. BUILD.PROP — FULL RAM TUNING ──────────────────────────────────────────
log "[6/8] build.prop — 2GB RAM tuning..."
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

# ── Branding
patch_prop "ro.build.display.id"     "ArizenOS Lite v1.0"
patch_prop "ro.product.brand"        "ArizenOS"
patch_prop "ro.product.name"         "arizen_t295"
patch_prop "ro.product.device"       "SM-T295"
patch_prop "ro.build.description"    "ArizenOS-Lite v1.0 ArizenOS SM-T295 release-keys"
patch_prop "ro.build.user"           "arizen"
patch_prop "ro.build.host"           "ArizenOS-Builder"
patch_prop "ro.arizen.version"       "1.0"
patch_prop "ro.arizen.variant"       "Lite"
patch_prop "ro.arizen.build.date"    "$(date +%Y%m%d)"

# ── Dalvik/ART Heap — tuned for 2GB RAM
# heapgrowthlimit: max heap per app before GC kicks in
# heapsize: absolute max per app
# Lower growthlimit = GC fires earlier = more free RAM for other apps
patch_prop "dalvik.vm.heapstartsize"           "8m"
patch_prop "dalvik.vm.heapgrowthlimit"         "128m"
patch_prop "dalvik.vm.heapsize"                "256m"
patch_prop "dalvik.vm.heaptargetutilization"   "0.75"
patch_prop "dalvik.vm.heapminfree"             "2m"
patch_prop "dalvik.vm.heapmaxfree"             "8m"

# ── Background Process Limits
# max_starting_bg: max apps starting in bg simultaneously
# bg_apps_limit: hard cap on bg processes (8 is good for 2GB)
patch_prop "ro.config.max_starting_bg"         "2"
patch_prop "ro.sys.fw.bg_apps_limit"           "8"
patch_prop "ro.sys.fw.use_trim_settings"       "true"
patch_prop "persist.sys.fw.trim_enable_memory" "1024"
patch_prop "persist.sys.purgeable_assets"      "1"

# ── LMK (Low Memory Killer) — kill bg apps sooner, keep fg smooth
patch_prop "ro.lmk.kill_heaviest_task"         "true"
patch_prop "ro.lmk.kill_timeout_ms"            "100"
patch_prop "ro.lmk.use_minfree_levels"         "true"
patch_prop "ro.lmk.low"                        "1001"
patch_prop "ro.lmk.medium"                     "800"
patch_prop "ro.lmk.critical"                   "0"
patch_prop "ro.lmk.critical_upgrade"           "false"
patch_prop "ro.lmk.upgrade_pressure"           "100"
patch_prop "ro.lmk.downgrade_pressure"         "100"
patch_prop "ro.lmk.swap_free_low_percentage"   "20"

# ── ZRAM via build.prop
patch_prop "ro.config.zram_options"            "true"

# ── Rendering & SurfaceFlinger
patch_prop "ro.surface_flinger.max_frame_buffer_acquired_buffers" "3"
patch_prop "debug.sf.reuse_framebuffers"       "1"
patch_prop "ro.config.hw_quickpoweron"         "true"
patch_prop "debug.hwui.use_buffer_age"         "false"

# ── Animation speed — snappier UI
patch_prop "ro.config.animation_scale"         "0.5"

# ── Misc performance
patch_prop "ro.config.low_ram"                 "false"
patch_prop "persist.sys.dalvik.multithread"    "true"

ARIZEN_COUNT=$(grep -c "^ro.arizen\|^ro.lmk\|^dalvik.vm" "$BUILD_PROP" 2>/dev/null || echo "?")
ok "build.prop patched — $ARIZEN_COUNT performance properties set"

# ── 7. ZRAM init.rc + sysctl tweaks ──────────────────────────────────────────
log "[7/8] ZRAM + VM sysctl config..."
mkdir -p "$SYSTEM_MNT/etc/init"
cat > "$SYSTEM_MNT/etc/init/arizen_zram.rc" << 'EOF'
# ArizenOS Lite — ZRAM + VM tuning for SM-T295 (2GB RAM)
on post-fs-data
    # ZRAM: 512MB with lz4 (fast compress, low CPU)
    write /sys/block/zram0/comp_algorithm lz4
    write /sys/block/zram0/disksize 536870912
    exec /system/bin/sh -c "mkswap /dev/block/zram0 2>/dev/null; swapon /dev/block/zram0 -p 5 2>/dev/null || true"

    # Swappiness: 60 = balanced (swap before OOM, not before needed)
    write /proc/sys/vm/swappiness 60

    # Dirty pages: flush sooner = less memory held by IO cache
    write /proc/sys/vm/dirty_ratio 10
    write /proc/sys/vm/dirty_background_ratio 5

    # Cache pressure: reclaim page cache more aggressively
    write /proc/sys/vm/vfs_cache_pressure 150

    # Extra reserved memory for critical processes (KB)
    write /proc/sys/vm/extra_free_kbytes 24300

    # Min free for emergency (KB) — ~6MB
    write /proc/sys/vm/min_free_kbytes 6144

    # OOM: don't kill allocating task first
    write /proc/sys/vm/oom_kill_allocating_task 0
EOF
chmod 644 "$SYSTEM_MNT/etc/init/arizen_zram.rc"
ok "ZRAM + VM sysctl init.rc installed"

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
ok "═══════════════════════════════════════"
ok " ArizenOS Lite injection complete! "
ok "═══════════════════════════════════════"
log "  Debloated:     $REMOVED packages removed"
log "  Launchers:     $LAUNCHER_REMOVED Samsung launchers replaced"
[[ -n "${LAUNCHER_APK:-}" ]] && log "  Launcher:      ArizenLauncher = default HOME"
log "  RAM tuning:    2GB-optimized heap + LMK + ZRAM 512MB"
log "  VM sysctl:     swappiness=60, dirty=10/5, cache_pressure=150"
echo ""
log "Next: ./scripts/repack_ap.sh"
