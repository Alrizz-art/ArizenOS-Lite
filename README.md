<p align="center">
  <img src="docs/assets/arizenos-logo.svg" alt="ArizenOS Lite" width="380"/>
</p>

<p align="center">
  <a href="https://github.com/Alrizz-art/ArizenOS-Lite/actions/workflows/build.yml">
    <img src="https://github.com/Alrizz-art/ArizenOS-Lite/actions/workflows/build.yml/badge.svg" alt="Build Status"/>
  </a>
  <img src="https://img.shields.io/badge/Device-SM--T295-0D6EFD?style=flat-square&logo=samsung&logoColor=white"/>
  <img src="https://img.shields.io/badge/Android-9%20%2F%2010-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Base-Samsung%20Stock-1428A0?style=flat-square"/>
  <img src="https://img.shields.io/badge/Flash-Odin%20%2F%20Heimdall-E53935?style=flat-square"/>
  <img src="https://img.shields.io/badge/RAM-2GB%20Optimized-7B2FFF?style=flat-square"/>
  <img src="https://img.shields.io/badge/License-MIT-22C55E?style=flat-square"/>
</p>

<h3 align="center">Stock Samsung firmware — debloated, optimized, Arizen Launcher as default home</h3>

---

## What is ArizenOS Lite?

ArizenOS Lite is **not** a full custom ROM rewrite. It is an intelligent modification of official Samsung stock firmware — preserving full hardware compatibility, Samsung kernel, vendor blobs, and partition layout — while replacing the launcher and removing bloatware.

```
Samsung Stock Firmware (SM-T295)
  ├── Debloat — 84 Samsung/Facebook/Knox packages removed
  ├── Arizen Launcher — replaces TouchWizHome, set as default HOME
  ├── ZRAM 512MB — lz4 compression for 2GB RAM
  ├── build.prop tuning — heap, LMK, VM sysctl
  └── Odin repackaging — valid .tar.md5 with MD5 self-check
= ArizenOS Lite  →  flash via Odin/Heimdall
```

---

## Device

| Field | Value |
|-------|-------|
| **Model** | Samsung Galaxy Tab A 8.0 (2019) |
| **Model Number** | SM-T295 |
| **Android** | 9 Pie / 10 |
| **RAM** | 2 GB |
| **Storage** | 32 GB |
| **Flash Method** | Odin (Windows) · Heimdall (macOS/Linux) |

---

## Features

### Arizen Launcher
- Dashboard-style AI-centered home screen
- Ultra lightweight — no background services by default
- Built-in AI assistant shortcut (Arizen Core)
- Clean app grid with minimal dock
- Replaces Samsung TouchWizHome / SecLauncher

### Performance Tuning (2GB RAM optimized)
| Setting | Value | Effect |
|---------|-------|--------|
| `dalvik.vm.heapgrowthlimit` | 128 MB | GC fires earlier → more free RAM |
| `dalvik.vm.heapsize` | 256 MB | App RAM cap |
| `ro.sys.fw.bg_apps_limit` | 8 | Background process cap |
| ZRAM | 512 MB lz4 | Compressed swap, negligible CPU cost |
| `vm.vfs_cache_pressure` | 150 | Reclaim page cache aggressively |
| `vm.swappiness` | 60 | Swap to ZRAM before OOM |
| LMK | Aggressive | Kill BG apps before system slows |

### Debloat
**84 packages removed** including: Bixby suite, SamsungPay, Facebook, OneDrive, LinkedIn, Knox/SecureFolder, Samsung Health, AR Zone, Samsung Members, GalaxyStore, Kids Mode, and more.

See full list: [`config/debloat_list.txt`](config/debloat_list.txt)

---

## Quick Build (GitHub Actions — Recommended)

No local setup needed. Uses the official Samsung firmware auto-downloader.

1. Fork this repo
2. Go to **Actions** tab → **Build ArizenOS Lite** → **Run workflow**
3. Choose your region (`XSP` = Global)
4. Wait ~60-90 min → download from **Releases** tab

The CI pipeline runs **7 Odin validation checks** before creating a release — including GNU tar magic bytes, MD5 integrity, and system.img size verification.

---

## Local Build

### Prerequisites

```bash
# Ubuntu / Debian
sudo apt-get install -y android-tools-fsutils e2fsprogs tar lz4 unzip file

# macOS (via Docker — see PANDUAN_LENGKAP.md)
./auto_build_mac.sh
```

> **Note:** `simg2img` / `img2simg` is required. Install via `android-tools-fsutils` or build from [anestisb/android-simg2img](https://github.com/anestisb/android-simg2img).

### Steps

```bash
# 1. Clone
git clone https://github.com/Alrizz-art/ArizenOS-Lite.git
cd ArizenOS-Lite

# 2. Place Samsung SM-T295 firmware zip in firmware/
mkdir -p firmware
# → copy your T295XXU*.zip here
# → or let the script download it automatically

# 3. Build Arizen Launcher APK
cd arizen-launcher && ./gradlew assembleRelease && cd ..
cp arizen-launcher/app/build/outputs/apk/release/*.apk ArizenLauncher.apk

# 4. Build firmware
chmod +x scripts/*.sh
./scripts/extract_firmware.sh firmware/YOUR_FIRMWARE.zip
sudo ./scripts/unpack_ap.sh
sudo ./scripts/inject_arizenos.sh
sudo ./scripts/repack_ap.sh
./scripts/package_odin.sh

# Output: output/ArizenOSLite_v1.0_SM-T295_YYYYMMDD_AP.tar.md5
```

---

## Flash Instructions

### Windows — Odin

1. Download `ArizenOSLite_v*_SM-T295_*_AP.tar.md5` from [Releases](../../releases)
2. Boot SM-T295 into **Download Mode**:
   > Power off → hold `Power + Vol Down + Bixby` simultaneously → press `Vol Up` to confirm
3. Connect USB to PC
4. Open **Odin v3.14.4+**
5. Click **AP** → select the downloaded `.tar.md5` file
6. **Options tab:** ✅ Auto Reboot · ✅ F.Reset Time · ❌ Re-Partition
7. Click **Start** → wait for green **PASS!**

### macOS / Linux — Heimdall

```bash
# Install
brew install heimdall          # macOS
sudo apt install heimdall-flash  # Ubuntu/Debian

# Boot device into Download Mode first, then:
heimdall detect

# Get partition layout
heimdall print-pit > pit.txt

# Flash system partition
heimdall flash --SYSTEM work/system_raw.img --no-reboot
# Then manually reboot: hold Power + Vol Up
```

> ⚠️ Heimdall flashes raw partition images, not `.tar.md5`. Use `work/system_raw.img` (produced during build) or unpack the tar first.

---

## Recovery

If the device bootloops, flash original Samsung firmware:

1. Download original firmware from [samfw.com](https://samfw.com) → search SM-T295
2. Flash via Odin with original AP/BL/CP/CSC files

---

## Project Structure

```
ArizenOS-Lite/
├── arizen-launcher/          # Launcher source (Android/Java)
│   ├── src/main/
│   │   ├── java/com/arizen/launcher/
│   │   └── res/
│   └── build.gradle
├── scripts/
│   ├── extract_firmware.sh   # Unzip Samsung firmware
│   ├── unpack_ap.sh          # Strip MD5, decompress, mount system
│   ├── inject_arizenos.sh    # Debloat + inject APKs + build.prop
│   ├── repack_ap.sh          # Unmount, fsck, sparse → raw
│   └── package_odin.sh       # tar + MD5 → Odin flashable
├── config/
│   ├── debloat_list.txt      # 84 packages to remove
│   └── build_config.sh       # Version/path config
├── .github/workflows/
│   └── build.yml             # Full CI pipeline with Odin validation
├── docs/                     # Flashing, optimization, troubleshooting guides
├── auto_build_mac.sh         # One-click build for macOS (Docker)
└── PANDUAN_LENGKAP.md        # Full Indonesian language guide
```

---

## Documentation

| Doc | Description |
|-----|-------------|
| [PANDUAN_LENGKAP.md](PANDUAN_LENGKAP.md) | Full guide in Bahasa Indonesia (MacBook → flash) |
| [docs/flashing-guide.md](docs/flashing-guide.md) | Detailed flash instructions |
| [docs/ai-setup-guide.md](docs/ai-setup-guide.md) | AI assistant setup (Groq, Together, etc.) |
| [docs/troubleshooting.md](docs/troubleshooting.md) | Common issues & solutions |
| [docs/optimization.md](docs/optimization.md) | Post-flash optimization tips |
| [config/debloat_list.txt](config/debloat_list.txt) | Full debloat package list |

---

## ⚠️ Disclaimer

ArizenOS Lite modifies device firmware. By using this project:
- **Always backup your data before flashing**
- Keep the original Samsung firmware for recovery
- Flash at your own risk — the maintainer is not responsible for bricked devices
- This project is not affiliated with or endorsed by Samsung Electronics

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for how to contribute, report issues, or add device support.

---

## License

[MIT License](LICENSE) — © 2025 Alrizz-art / ArizenOS Project

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/Alrizz-art"><b>Alrizz-art</b></a><br/>
  <sub>ArizenOS Project — github.com/Alrizz-art/ArizenOS-Lite</sub>
</p>
