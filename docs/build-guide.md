# ArizenOS Lite — Build Guide

> LineageOS → ArizenOS Lite → Odin AP.tar.md5 for SM-T295

---

## Overview

```
LineageOS zip
     ↓  scripts/unpack_lineage.sh
system.img (ext4, mounted)
     ↓  scripts/inject_arizenos.sh
system.img (ArizenOS — Launcher + branding + tuning)
     ↓  scripts/repack_odin.sh
ArizenOS-Lite_v1.1_SM-T295_YYYYMMDD_AP.tar.md5
     ↓  Odin / Heimdall
SM-T295 running ArizenOS Lite 🚀
```

---

## Step 0 — Get LineageOS for SM-T295

SM-T295 may not have an **official** LineageOS build. Check these sources:

### Official LineageOS
- https://lineageos.org/devices/ → search `gtaslte` or `SM-T295`

### Unofficial (XDA — recommended)
- https://xdaforums.com/c/samsung-galaxy-tab-a-8-0-2019.9148/
- Search threads for: **LineageOS**, **AOSP**, **crDroid**, **EvolutionX**

### What to look for
- LineageOS 18.1 (Android 11) — **recommended** (uses ext4, widely tested)
- LineageOS 19.1 (Android 12) — may use EROFS (incompatible)
- Any AOSP-based Android 10/11 ROM in zip format

### Download automatically
```bash
bash scripts/download_lineage.sh
```

### Or manually
```bash
mkdir -p firmware
# Copy your LineageOS zip here:
cp /path/to/lineage-18.1-*-gtaslte*.zip firmware/
```

---

## Step 1 — Prerequisites

### Ubuntu / Debian
```bash
sudo apt-get update
sudo apt-get install -y \
    android-tools-fsutils \
    e2fsprogs \
    brotli \
    unzip \
    lz4 \
    python3 \
    wget \
    curl \
    file \
    git
```

### macOS — use Docker
```bash
# Install Docker Desktop first
docker pull ubuntu:22.04
docker run --privileged -it \
    -v $(pwd):/workspace \
    ubuntu:22.04 bash

# Inside container:
cd /workspace
apt-get update && apt-get install -y android-tools-fsutils e2fsprogs brotli unzip lz4 python3 wget curl file
```

### Windows
Use WSL2 (Ubuntu 22.04) — same commands as Ubuntu.

---

## Step 2 — Build Arizen Launcher APK

The launcher must be compiled before injecting.

### Requirements
- Java 17+ (`sudo apt install openjdk-17-jdk`)
- Android SDK (or use the included Gradle wrapper — downloads SDK automatically)

```bash
cd arizen-launcher
chmod +x gradlew
./gradlew assembleRelease

# Copy APK to repo root
cp app/build/outputs/apk/release/app-release*.apk ../ArizenLauncher.apk
cd ..
ls -lh ArizenLauncher.apk
```

If you don't have Android SDK, the CI (GitHub Actions) builds it automatically.

---

## Step 3 — Unpack LineageOS

```bash
sudo bash scripts/unpack_lineage.sh
```

This script:
1. Locates the LineageOS zip in `firmware/`
2. Auto-detects format: **payload.bin** / **system.new.dat.br** / **system.img**
3. Converts to raw ext4 if needed (brotli decompression, sdat2img)
4. Rejects EROFS (read-only, non-modifiable) — use Android 11 build if this happens
5. Expands image by 250MB for ArizenOS files
6. Mounts at `work/system_mount/`

### Supported input formats
| Format | Description | LineageOS version |
|--------|-------------|-------------------|
| `system.img` | Raw ext4 in zip | Some 17.x/18.x |
| `system.new.dat.br` | Brotli block-based | Most 17.x/18.x |
| `payload.bin` | A/B OTA format | 20+ (Android 13) |
| EROFS | ❌ Not supported | 20+ (Android 13) |

**Recommendation: Use LineageOS 18.1 (Android 11)**

---

## Step 4 — Inject ArizenOS

```bash
sudo bash scripts/inject_arizenos.sh
```

What this does (10 steps):
1. Remove stock launchers (Trebuchet, Launcher3, Launcher3QuickStep)
2. Install Arizen Launcher as default HOME activity
3. Remove LineageOS extras (AudioFX, Eleven, Jelly, Recorder)
4. Install ArizenOS boot animation (if available in `bootanimation/`)
5. Install wallpaper (if `arizen_wallpaper.png` exists)
6. Patch `build.prop` — full ArizenOS identity (1.1 Zenith) + performance tweaks
7. Install ZRAM init.rc (`/system/etc/init/arizen_zram.rc`)
8. Install CPU + LMK init.rc (`/system/etc/init/arizen_perf.rc`)
9. Install performance profiles script + sysctl config
10. Create `/system/etc/arizen/version.json`

### What's NOT touched
- LineageOS kernel (boot.img) — untouched, keeps root/TWRP compatibility
- Vendor partition — untouched, keeps hardware blobs
- `ro.lineage.*` properties — kept for app compatibility
- Core LineageOS apps (Phone, Contacts, Settings, etc.)

---

## Step 5 — Repack to Odin AP.tar.md5

```bash
sudo bash scripts/repack_odin.sh
```

This script:
1. Unmounts the system image
2. Runs `e2fsck` to repair filesystem
3. Shrinks image to minimum size (`resize2fs -M`)
4. Converts raw ext4 → Android sparse image (`img2simg`)
5. Extracts `boot.img` and `vendor.img` from LineageOS zip (if present)
6. Creates Odin tar archive with `system.img` + `boot.img` + `vendor.img`
7. Appends MD5 hash (Odin self-check format)
8. Validates: size, tar magic bytes, MD5 trailer

Output: `output/ArizenOS-Lite_v1.1_SM-T295_YYYYMMDD_AP.tar.md5`

---

## Troubleshooting

### "EROFS filesystem detected"
Use an older LineageOS build (18.1 / Android 11). EROFS is a read-only compressed filesystem used in Android 12+ LineageOS builds.

### "Mount failed"
Run with `sudo`. If in a Docker container, add `--privileged` flag.

### "sdat2img not found"
```bash
curl -o /tmp/sdat2img.py https://raw.githubusercontent.com/xpirt/sdat2img/master/sdat2img.py
chmod +x /tmp/sdat2img.py
sudo mv /tmp/sdat2img.py /usr/local/bin/sdat2img
```

### "payload-dumper-go not found"
Only needed for A/B (payload.bin) zips. Install from:
https://github.com/ssut/payload-dumper-go/releases

### ArizenLauncher.apk not found
```bash
cd arizen-launcher
./gradlew assembleRelease
cp app/build/outputs/apk/release/*.apk ../ArizenLauncher.apk
```

### Device bootloops after flash
1. Boot into TWRP recovery (Vol Up + Power at boot)
2. Flash LineageOS zip directly from TWRP (Wipe → flash)
3. Or flash original Samsung firmware from samfw.com via Odin

---

## GitHub Actions CI

The included workflow (`.github/workflows/build.yml`) can automate the entire build:

1. Fork the repo
2. Add `LINEAGE_ZIP_URL` as a repository secret (direct download URL for your LineageOS zip)
3. Go to Actions → Build ArizenOS Lite → Run workflow
4. The workflow will:
   - Download LineageOS zip
   - Build Arizen Launcher
   - Run unpack → inject → repack
   - Upload output as a GitHub Release

---

## Full Build Time Estimates

| Step | Local (Linux) | CI (GitHub Actions) |
|------|---------------|---------------------|
| Download LineageOS | 5–20 min | 5–15 min |
| Build Launcher | 2–5 min | 3–8 min |
| Unpack system.img | 5–10 min | 5–10 min |
| Inject ArizenOS | 1–2 min | 1–2 min |
| Repack Odin | 5–15 min | 5–15 min |
| **Total** | **~20–50 min** | **~20–50 min** |

---

*ArizenOS Lite v1.1 Zenith — SM-T295 (gtaslte)*
