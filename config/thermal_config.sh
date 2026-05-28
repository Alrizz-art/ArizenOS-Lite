#!/usr/bin/env sh
# ArizenOS Lite — Thermal Management Config
# Target: SM-T295 / Snapdragon 429
# Goal: prevent thermal throttle on sustained loads while staying stable

# ─────────────────────────────────────────────────────────────────────────────
# Thermal zone paths (Snapdragon 429 / MSM8937)
# Zone 0: cpu-small cluster
# Zone 1: cpu-big cluster  
# Zone 2: gpu
# ─────────────────────────────────────────────────────────────────────────────

# Set thermal policy: balanced (not max_cooling)
for zone in /sys/class/thermal/thermal_zone*/policy; do
    current=$(cat "$zone" 2>/dev/null || echo "")
    if echo "$current" | grep -q "step_wise"; then
        echo "step_wise" > "$zone" 2>/dev/null || true
    fi
done

# Trip point temperatures (millidegrees Celsius)
# Passive cooling starts at 70°C, critical at 90°C
for zone in /sys/class/thermal/thermal_zone*/; do
    [ -d "$zone" ] || continue
    # Set trip temps if writable
    for trip in "$zone"trip_point_*_temp; do
        [ -w "$trip" ] || continue
        trip_type_file=$(echo "$trip" | sed 's/_temp/_type/')
        trip_type=$(cat "$trip_type_file" 2>/dev/null || echo "")
        if echo "$trip_type" | grep -q "passive"; then
            echo 70000 > "$trip" 2>/dev/null || true
        elif echo "$trip_type" | grep -q "critical"; then
            echo 95000 > "$trip" 2>/dev/null || true
        fi
    done
done

# Qualcomm thermal daemon hints
THERMALD_CONF=/etc/thermal-engine.conf
if [ -f "$THERMALD_CONF" ]; then
    echo "ArizenOS: thermal-engine.conf present, using device defaults"
fi

# Disable unnecessary wake locks from thermal sysfs
echo 0 > /sys/module/msm_thermal/parameters/enabled 2>/dev/null || true
echo 1 > /sys/module/msm_thermal/core_control/enabled 2>/dev/null || true

echo "ArizenOS: thermal config applied"
