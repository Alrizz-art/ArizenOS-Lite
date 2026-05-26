#!/usr/bin/env bash
# ArizenOS Lite — Build Configuration

export ARIZEN_VERSION="1.0"
export ARIZEN_CODENAME="Lite"
export ARIZEN_DEVICE="SM-T295"
export ARIZEN_ANDROID="9"
export ARIZEN_BUILD_DATE="$(date +%Y%m%d)"

export WORK_DIR="$(pwd)/work"
export OUTPUT_DIR="$(pwd)/output"
export ARIZEN_ASSETS="$(pwd)/arizen-assets"

export OUTPUT_FILENAME="ArizenOSLite_v${ARIZEN_VERSION}_${ARIZEN_DEVICE}_AP.tar.md5"

# Build flags
export ARIZEN_DEBLOAT=true
export ARIZEN_ZRAM=true
export ARIZEN_BOOTANIM=true
export ARIZEN_PERF_TWEAKS=true
