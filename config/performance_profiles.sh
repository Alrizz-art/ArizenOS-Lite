#!/usr/bin/env sh
# ArizenOS Lite — Performance Profile Manager
# Target: SM-T295 (Snapdragon 429 / MSM8937)
# Place in /system/etc/arizen/performance_profiles.sh
# Called by init.arizen.rc on boot

PROFILE="${1:-balanced}"

echo "ArizenOS: applying profile=$PROFILE"

# ─────────────────────────────────────────────────────────────────────────────
# CPU Governor
# Snapdragon 429: 4x Cortex-A53 @ 1.4 GHz, cluster 0 = cpu0–3
# ─────────────────────────────────────────────────────────────────────────────
set_governor() {
    for cpu in /sys/devices/system/cpu/cpu[0-3]/cpufreq; do
        [ -d "$cpu" ] || continue
        echo "$1" > "$cpu/scaling_governor" 2>/dev/null || true
    done
}

set_freq_limits() {
    MIN=$1; MAX=$2
    for cpu in /sys/devices/system/cpu/cpu[0-3]/cpufreq; do
        [ -d "$cpu" ] || continue
        echo "$MIN" > "$cpu/scaling_min_freq" 2>/dev/null || true
        echo "$MAX" > "$cpu/scaling_max_freq" 2>/dev/null || true
    done
}

# ─────────────────────────────────────────────────────────────────────────────
case "$PROFILE" in

  performance)
    # Max frequency, interactive governor — favour responsiveness
    set_governor "interactive"
    set_freq_limits 960000 1401600
    # Interactive tuning
    IDIR=/sys/devices/system/cpu/cpu0/cpufreq/interactive
    if [ -d "$IDIR" ]; then
        echo 60000   > "$IDIR/above_hispeed_delay"    2>/dev/null || true
        echo 1401600 > "$IDIR/hispeed_freq"           2>/dev/null || true
        echo 90      > "$IDIR/target_loads"           2>/dev/null || true
        echo 30000   > "$IDIR/min_sample_time"        2>/dev/null || true
    fi
    # GPU: max performance
    echo 0 > /sys/class/kgsl/kgsl-3d0/bus_split             2>/dev/null || true
    echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on          2>/dev/null || true
    echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on          2>/dev/null || true
    # VM
    echo 100 > /proc/sys/vm/swappiness                       2>/dev/null || true
    echo 150  > /proc/sys/vm/vfs_cache_pressure              2>/dev/null || true
    ;;

  saver)
    # Low frequency, conservative governor — max battery
    set_governor "conservative"
    set_freq_limits 307200 960000
    CDIR=/sys/devices/system/cpu/cpu0/cpufreq/conservative
    if [ -d "$CDIR" ]; then
        echo 95 > "$CDIR/up_threshold"   2>/dev/null || true
        echo 60 > "$CDIR/down_threshold" 2>/dev/null || true
        echo 4  > "$CDIR/freq_step"      2>/dev/null || true
    fi
    # GPU: power save
    echo 1 > /sys/class/kgsl/kgsl-3d0/bus_split             2>/dev/null || true
    echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on          2>/dev/null || true
    echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on          2>/dev/null || true
    # VM
    echo 30  > /proc/sys/vm/swappiness                       2>/dev/null || true
    echo 50  > /proc/sys/vm/vfs_cache_pressure               2>/dev/null || true
    ;;

  balanced|*)
    # Default: schedutil — kernel-driven, efficient
    set_governor "schedutil"
    set_freq_limits 307200 1401600
    SDIR=/sys/devices/system/cpu/cpufreq/schedutil
    if [ -d "$SDIR" ]; then
        echo 2000 > "$SDIR/rate_limit_us"           2>/dev/null || true
        echo 1    > "$SDIR/iowait_boost_enable"     2>/dev/null || true
    fi
    # GPU: auto
    echo 1 > /sys/class/kgsl/kgsl-3d0/bus_split             2>/dev/null || true
    echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on          2>/dev/null || true
    echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on          2>/dev/null || true
    # VM
    echo 60  > /proc/sys/vm/swappiness                       2>/dev/null || true
    echo 100 > /proc/sys/vm/vfs_cache_pressure               2>/dev/null || true
    ;;
esac

echo "ArizenOS: profile=$PROFILE applied"
