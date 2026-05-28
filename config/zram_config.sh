#!/usr/bin/env sh
# ArizenOS Lite — ZRAM Configuration
# Target: SM-T295 — 2GB or 3GB RAM
# Place in /system/etc/init/arizen_zram.rc  (as init.d script)

# Auto-detect RAM size
TOTAL_RAM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
TOTAL_RAM_MB=$((TOTAL_RAM_KB / 1024))

if [ "$TOTAL_RAM_MB" -ge 2900 ]; then
    # 3GB variant — 768MB ZRAM
    ZRAM_SIZE=805306368
    SWAPPINESS=50
    RAM_VARIANT="3GB"
else
    # 2GB variant — 512MB ZRAM (balanced for low RAM)
    ZRAM_SIZE=536870912
    SWAPPINESS=60
    RAM_VARIANT="2GB"
fi

echo "ArizenOS: ZRAM init — RAM=${RAM_VARIANT} ZRAM_SIZE=$((ZRAM_SIZE/1024/1024))MB"

# ─── ZRAM device setup ────────────────────────────────────────────────────────

# Reset any existing ZRAM first
swapoff /dev/block/zram0 2>/dev/null || true
echo 1 > /sys/block/zram0/reset 2>/dev/null || true

# Choose best available compression algorithm
# Priority: lz4hc > lz4 > lzo (speed vs ratio)
AVAIL_ALGOS=$(cat /sys/block/zram0/comp_algorithm 2>/dev/null || echo "lzo")
if echo "$AVAIL_ALGOS" | grep -q "lz4hc"; then
    ALGO="lz4hc"
elif echo "$AVAIL_ALGOS" | grep -q "lz4"; then
    ALGO="lz4"
else
    ALGO="lzo"
fi

echo "$ALGO" > /sys/block/zram0/comp_algorithm 2>/dev/null || true
echo "ArizenOS: ZRAM algorithm=$ALGO"

# Set ZRAM disk size
echo "$ZRAM_SIZE" > /sys/block/zram0/disksize 2>/dev/null || true

# Initialize and enable swap
mkswap /dev/block/zram0 2>/dev/null || true
swapon /dev/block/zram0 -p 100 2>/dev/null || true

# ─── VM tuning ────────────────────────────────────────────────────────────────

# Swappiness — controls aggressiveness of swapping to ZRAM
echo "$SWAPPINESS" > /proc/sys/vm/swappiness 2>/dev/null || true

# Dirty page ratios — keep write pressure reasonable
echo 10 > /proc/sys/vm/dirty_ratio            2>/dev/null || true
echo 5  > /proc/sys/vm/dirty_background_ratio 2>/dev/null || true

# VFS cache pressure — free dentries/inodes moderately
echo 100 > /proc/sys/vm/vfs_cache_pressure    2>/dev/null || true

# Prevent OOM killing the allocating task (kill cached instead)
echo 0 > /proc/sys/vm/oom_kill_allocating_task 2>/dev/null || true

# Extra free kbytes — memory buffer before LMK kicks in
echo 24576 > /proc/sys/vm/extra_free_kbytes   2>/dev/null || true

echo "ArizenOS: ZRAM setup complete — swappiness=$SWAPPINESS"
