#!/usr/bin/env sh
# ArizenOS Lite — ZRAM Configuration
# Optimized for 2GB RAM — SM-T295
# Place in /system/etc/init.d/99arizen_zram

# Enable ZRAM with lz4 compression (fast + efficient)
echo lz4 > /sys/block/zram0/comp_algorithm 2>/dev/null || true

# Set ZRAM size to 512MB (good balance for 2GB RAM)
echo 536870912 > /sys/block/zram0/disksize 2>/dev/null || true

# Initialize swap
mkswap /dev/block/zram0 2>/dev/null || true
swapon /dev/block/zram0 -p 5 2>/dev/null || true

# Swappiness tuning — moderate swappiness for responsive UX
echo 60 > /proc/sys/vm/swappiness 2>/dev/null || true

# Reduce dirty page write delay
echo 10 > /proc/sys/vm/dirty_ratio 2>/dev/null || true
echo 5 > /proc/sys/vm/dirty_background_ratio 2>/dev/null || true

# Cache pressure — free cache more aggressively
echo 100 > /proc/sys/vm/vfs_cache_pressure 2>/dev/null || true

# Reduce OOM aggressiveness
echo 0 > /proc/sys/vm/oom_kill_allocating_task 2>/dev/null || true
