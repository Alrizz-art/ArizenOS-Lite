#!/usr/bin/env sh
# ArizenOS Lite — Low Memory Killer (LMK) Tuning
# Target: SM-T295 — 2GB RAM variant
# Place in /system/etc/arizen/lmk_config.sh
# Run at boot via init.arizen.rc

# ─────────────────────────────────────────────────────────────────────────────
# LMK minfree thresholds (pages, 1 page = 4KB)
# Format: foreground,visible,secondary_server,hidden,content,empty
#
# 2GB RAM tuning — keep foreground tasks alive, kill empties early
# Thresholds (MB converted to pages):
#   18MB  = 4608  pages  (foreground — only kill if critically low)
#   24MB  = 6144  pages  (visible)
#   32MB  = 8192  pages  (secondary server)
#   40MB  = 10240 pages  (hidden)
#   64MB  = 16384 pages  (content provider)
#   96MB  = 24576 pages  (empty / cached processes — aggressive kill)
# ─────────────────────────────────────────────────────────────────────────────

LMK_MINFREE="4608,6144,8192,10240,16384,24576"
LMK_ADJ="0,1,2,3,4,9"

echo "$LMK_MINFREE" > /sys/module/lowmemorykiller/parameters/minfree 2>/dev/null || true
echo "$LMK_ADJ"     > /sys/module/lowmemorykiller/parameters/adj     2>/dev/null || true

# Extra free kbytes — keep a buffer for burst allocation
# 24MB buffer for 2GB RAM
echo 24576 > /proc/sys/vm/extra_free_kbytes 2>/dev/null || true

# Min free kbytes — minimum kernel page reserve (16MB)
echo 16384 > /proc/sys/vm/min_free_kbytes 2>/dev/null || true

# Page cluster — reduce swapping chunk size for low-RAM devices
echo 0 > /proc/sys/vm/page-cluster 2>/dev/null || true

# Compact memory proactively — reduces fragmentation
echo 1 > /proc/sys/vm/compact_memory 2>/dev/null || true

echo "ArizenOS: LMK tuning applied (2GB profile)"
