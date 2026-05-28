#!/usr/bin/env bash
# ArizenOS Lite — ADB Debloat Script
# Removes or disables Samsung bloatware via ADB for SM-T295
# Usage: bash debloat_adb.sh [--disable | --uninstall | --restore]
# Requires: ADB running, device connected with USB debugging ON

set -euo pipefail

MODE="${1:---disable}"
DEVICE_SERIAL="${ANDROID_SERIAL:-}"
ADB="adb ${DEVICE_SERIAL:+-s $DEVICE_SERIAL}"

# ─── Verify connection ────────────────────────────────────────────────────────
echo "ArizenOS Lite — ADB Debloat"
echo "Mode: $MODE"
echo "Checking ADB connection…"
$ADB wait-for-device
MODEL=$($ADB shell getprop ro.product.model 2>/dev/null | tr -d '\r')
echo "Connected: $MODEL"
if [[ "$MODEL" != *"T295"* ]]; then
    echo "WARNING: Expected SM-T295, got '$MODEL'. Proceed anyway? (y/N)"
    read -r yn
    [[ "$yn" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 1; }
fi

# ─── Package list ────────────────────────────────────────────────────────────
PACKAGES=(
    # Bixby
    com.samsung.android.bixby.agent
    com.samsung.android.bixby.agent.dummy
    com.samsung.android.bixby.wakeup
    com.samsung.android.bixby.service
    com.samsung.android.bixbyvision.framework
    com.samsung.android.app.spage
    com.samsung.android.bixby.voiceinput

    # Samsung Pay
    com.samsung.android.spay
    com.samsung.android.spayfw
    com.samsung.android.samsungpass
    com.samsung.android.samsungpassautofill

    # Facebook
    com.facebook.appmanager
    com.facebook.services
    com.facebook.system
    com.facebook.katana
    com.facebook.orca

    # Samsung Analytics / Telemetry
    com.samsung.android.mapsagent
    com.samsung.android.scloud
    com.sec.android.diagmonagent
    com.samsung.android.sm.devicesecurity
    com.samsung.android.mdm
    com.sec.enterprise.mdm.services.simpin

    # Samsung Health & Wearable
    com.sec.android.app.shealth
    com.samsung.android.app.watchmanagerstub
    com.samsung.android.beaconmanager
    com.samsung.android.easysetup

    # Samsung Members / Tips
    com.samsung.android.app.spage
    com.samsung.android.voc
    com.sec.android.wifiguider

    # AR / Vision
    com.samsung.android.arzone
    com.samsung.android.aremoji
    com.samsung.android.visionintelligence

    # Samsung Browser (replace with Firefox/Brave)
    com.sec.android.app.sbrowser

    # Game tools
    com.samsung.android.game.gamehome
    com.samsung.android.game.gametools
    com.samsung.android.game.gos

    # Samsung Kids
    com.samsung.android.kidsinstaller
    com.samsung.android.kidsmode

    # Smart things / suggestions
    com.samsung.android.smartsuggestions
    com.samsung.android.smartcapture

    # Misc
    com.sec.android.autodoodle
    com.samsung.android.stickercenter
    com.samsung.android.personalitytest
    com.sec.android.app.kidshome
    com.samsung.android.unifiedsearchwidget
    com.sec.android.desktopmode.uiservice
    com.samsung.android.privateshare

    # Microsoft / LinkedIn
    com.microsoft.skydrive
    com.linkedin.android

    # Netflix stub
    com.netflix.partner.activation
)

# ─── Do not remove ────────────────────────────────────────────────────────────
KEEP=(
    com.samsung.android.camera          # Camera HAL
    com.samsung.android.provider.filterprovider
    com.samsung.android.wifi            # WiFi
    com.android.bluetooth              # Bluetooth
    com.sec.android.inputmethod.keyboard  # Samsung keyboard
    com.samsung.android.drm.core       # DRM
    com.samsung.android.media.faces    # Media faces provider
)

PASS=0; FAIL=0; SKIP=0

disable_pkg() {
    local pkg="$1"
    if $ADB shell pm list packages -d 2>/dev/null | grep -q "package:$pkg"; then
        echo "  SKIP (already disabled): $pkg"
        ((SKIP++)); return
    fi
    if ! $ADB shell pm list packages 2>/dev/null | grep -q "package:$pkg"; then
        echo "  SKIP (not installed): $pkg"
        ((SKIP++)); return
    fi
    if $ADB shell pm disable-user --user 0 "$pkg" 2>&1 | grep -q "new state: disabled"; then
        echo "  ✅ DISABLED: $pkg"
        ((PASS++))
    else
        echo "  ❌ FAILED: $pkg"
        ((FAIL++))
    fi
}

uninstall_pkg() {
    local pkg="$1"
    if ! $ADB shell pm list packages 2>/dev/null | grep -q "package:$pkg"; then
        echo "  SKIP (not installed): $pkg"
        ((SKIP++)); return
    fi
    if $ADB shell pm uninstall --user 0 "$pkg" 2>&1 | grep -q "Success"; then
        echo "  ✅ REMOVED: $pkg"
        ((PASS++))
    else
        echo "  ❌ FAILED: $pkg"
        ((FAIL++))
    fi
}

restore_pkg() {
    local pkg="$1"
    if $ADB shell pm install-existing --user 0 "$pkg" 2>&1 | grep -q -E "Success|installed"; then
        echo "  ✅ RESTORED: $pkg"
        ((PASS++))
    else
        echo "  SKIP (not on device): $pkg"
        ((SKIP++))
    fi
}

# ─── Execute ──────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════"
echo "  Processing ${#PACKAGES[@]} packages…"
echo "════════════════════════════════════"

for pkg in "${PACKAGES[@]}"; do
    case "$MODE" in
        --disable)   disable_pkg   "$pkg" ;;
        --uninstall) uninstall_pkg "$pkg" ;;
        --restore)   restore_pkg   "$pkg" ;;
        *) echo "Unknown mode: $MODE. Use --disable | --uninstall | --restore"; exit 1 ;;
    esac
done

echo ""
echo "════════════════════════════════════"
echo "  Done: $PASS ok, $FAIL failed, $SKIP skipped"
echo "════════════════════════════════════"
[[ $FAIL -eq 0 ]] && echo "✅ Debloat complete" || echo "⚠️  Some packages failed — check above"
