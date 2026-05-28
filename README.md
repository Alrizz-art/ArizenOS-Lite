<p align="center">
  <img src="docs/assets/arizenos-logo.svg" alt="ArizenOS Lite" width="380"/>
</p>

<p align="center">
  <a href="https://github.com/Alrizz-art/ArizenOS-Lite/actions/workflows/build.yml">
    <img src="https://github.com/Alrizz-art/ArizenOS-Lite/actions/workflows/build.yml/badge.svg" alt="Build Status"/>
  </a>
  <img src="https://img.shields.io/badge/Device-SM--T295-0D6EFD?style=flat-square&logo=samsung&logoColor=white"/>
  <img src="https://img.shields.io/badge/Android-11%20%2F%2018.1-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Base-LineageOS-8BC34A?style=flat-square&logo=lineageos&logoColor=white"/>
  <img src="https://img.shields.io/badge/Flash-Odin%20%2F%20Heimdall-E53935?style=flat-square"/>
  <img src="https://img.shields.io/badge/RAM-2GB%20Optimized-7B2FFF?style=flat-square"/>
  <img src="https://img.shields.io/badge/License-MIT-22C55E?style=flat-square"/>
</p>

<h3 align="center">LineageOS-based custom ROM — Odin-flashable AP.tar.md5 for SM-T295</h3>

---

## What is ArizenOS Lite?

ArizenOS Lite is a fully custom Android experience for the **Samsung Galaxy Tab A 8.0" 2019 (SM-T295)**, built on top of **LineageOS** — replacing Samsung's bloated stock firmware entirely.

LineageOS provides the clean, open-source Android base. ArizenOS layers on top with a custom identity, productivity tools, and hardware tuning for this specific device.

```
LineageOS (SM-T295 / gtaslte build)
  ├── Stock launchers removed (Trebuchet / Launcher3)
  ├── Arizen Launcher — AI-centered home screen, default HOME
  ├── ArizenOS branding — 100% custom identity (no LineageOS/Samsung)
  ├── Command Palette — Linux-style >CMD launcher (16 system commands)
  ├── System Monitor — live RAM/CPU/battery/thermal dashboard
  ├── Workspace Mode — focused productivity with session timer
  ├── ZRAM 512MB — lz4 compression for 2GB RAM
  ├── Performance Profiles — balanced / performance / battery saver
  ├── LMK + sysctl tuning — 2GB-optimized memory management
  └── Odin repackaging — valid AP.tar.md5 flashable via Odin/Heimdall
= ArizenOS Lite v1.1 "Zenith"
```

---

## Device

| Field | Value |
|-------|-------|
| **Model** | Samsung Galaxy Tab A 8.0" (2019) |
| **Model Number** | SM-T295 |
| **Codename** | gtaslte |
| **Chipset** | Snapdragon 429 (MSM8937) |
| **Android** | LineageOS 18.1 (Android 11) recommended |
| **RAM** | 2 GB |
| **Storage** | 32 GB |
| **Flash Method** | Odin (Windows) · Heimdall (macOS/Linux) |

---

## Why LineageOS instead of Samsung stock?

| | Samsung Stock | ArizenOS Lite |
|--|--------------|--------------|
| Bloatware | 60–80 Samsung apps | None |
| Bixby | Always running | Removed |
| Update path | Samsung-controlled | Community |
| Modifiable | Difficult (EROFS on newer builds) | Yes (ext4) |
| Privacy | Samsung telemetry | Clean |
| Base | Proprietary | Open source (LineageOS) |

---

## Features

### Arizen Launcher (v1.1)
- Live stat bar — RAM free, performance profile, quick buttons
- `> CMD` Command Palette — Linux-style launcher, 16 system commands
- System Monitor — live hardware dashboard (RAM / CPU / battery / thermal)
- Workspace Mode — focus timer, session management, distraction-free
- AI assistant shortcut (Groq / Together.ai — free tier)
- Clean app grid with configurable columns and minimal dock

### Performance (2GB RAM optimized)
| Tuning | Value | Effect |
|--------|-------|--------|
| ZRAM | 512MB lz4 | Compressed swap, ~2.5GB effective RAM |
| Dalvik heapgrowthlimit | 128MB | GC fires earlier, more free RAM |
| `ro.sys.fw.bg_apps_limit` | 8 | Background process cap |
| LMK minfree | 18→96MB | Kills empties first, keeps foreground alive |
| CPU governor | schedutil | Kernel-driven, efficient on SD429 |
| `vm.swappiness` | 60 | Swap to ZRAM before OOM |

### Performance Profiles (switchable in System Monitor)
| Profile | Governor | Description |
|---------|----------|-------------|
| Balanced | schedutil | Default — kernel-driven efficiency |
| Performance | interactive | Max freq — for AI / heavy tasks |
| Battery Saver | conservative | Lower freq — long sessions |

---

## Build Pipeline

```
LineageOS zip (firmware/)
      │
      ▼
scripts/unpack_lineage.sh     ← extract system.img, detect format, mount
      │
      ▼
scripts/inject_arizenos.sh    ← install launcher, branding, tuning, init.rc
      │
      ▼
scripts/repack_odin.sh        ← unmount, shrink, sparse, AP.tar.md5 + MD5
      │
      ▼
output/ArizenOS-Lite_v1.1_SM-T295_YYYYMMDD_AP.tar.md5
```

---

## Quick Build (GitHub Actions — Recommended)

1. Fork this repo
2. Get a LineageOS zip for SM-T295 (see [docs/build-guide.md](docs/build-guide.md))
3. Add the download URL as a GitHub secret: `LINEAGE_ZIP_URL`
4. Go to **Actions → Build ArizenOS Lite → Run workflow**
5. Wait ~30-60 min → download from **Releases**

---

## Local Build

### Prerequisites

```bash
# Ubuntu / Debian
sudo apt-get install -y \
    android-tools-fsutils \
    e2fsprogs \
    brotli \
    unzip \
    lz4 \
    file \
    python3

# macOS — use Docker (see docs/build-guide.md)
```

### Steps

```bash
# 1. Clone
git clone https://github.com/Alrizz-art/ArizenOS-Lite.git
cd ArizenOS-Lite

# 2. Get LineageOS zip for SM-T295
bash scripts/download_lineage.sh
# Or manually: place zip in firmware/

# 3. Build Arizen Launcher APK
cd arizen-launcher && ./gradlew assembleRelease && cd ..
cp arizen-launcher/app/build/outputs/apk/release/*.apk ArizenLauncher.apk

# 4. Build ArizenOS
chmod +x scripts/*.sh
sudo bash scripts/unpack_lineage.sh          # extract + mount
sudo bash scripts/inject_arizenos.sh         # inject ArizenOS
sudo bash scripts/repack_odin.sh             # package Odin AP.tar.md5

# Output: output/ArizenOS-Lite_v1.1_SM-T295_YYYYMMDD_AP.tar.md5
```

---

## Flash Instructions

### Prerequisites

Before flashing, you need **TWRP custom recovery** installed on SM-T295:
1. Download TWRP for SM-T295 from [twrp.me](https://twrp.me) or XDA
2. Flash TWRP via Odin first (put in AP slot)
3. Then flash ArizenOS via TWRP, OR flash the AP.tar.md5 directly via Odin

### Windows — Odin

1. Download `ArizenOS-Lite_v*_SM-T295_*_AP.tar.md5` from [Releases](../../releases)
2. Boot SM-T295 into **Download Mode**:
   > Power off → hold `Power + Vol Down` → press `Vol Up` to confirm
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

# Boot device into Download Mode, then:
heimdall detect

# Flash system partition directly
heimdall flash --SYSTEM work/system_raw.img --no-reboot
# Manually reboot: hold Power + Vol Up
```

### Via TWRP (Alternative)

If you have TWRP installed:
1. Reboot to recovery (Power + Vol Up at boot)
2. Wipe → Dalvik/ART Cache + Cache
3. Install → select ArizenOS-Lite*.tar.md5
4. Reboot System

---

## First Boot

First boot takes **3–5 minutes** (LineageOS initial setup + Arizen init).

After boot:
1. Skip Samsung/Google account setup if prompted
2. Arizen Launcher opens automatically as HOME
3. Tap **> CMD** to try the Command Palette
4. Tap **Monitor** in dock to see live system stats
5. Go to **Labs → AI** to configure your AI assistant (Groq is free)

---

## Recovery

If the device bootloops:
1. Download the original **LineageOS** zip for SM-T295
2. Flash via TWRP (wipe first)
3. Or flash original Samsung firmware from [samfw.com](https://samfw.com) via Odin

---

## Project Structure

```
ArizenOS-Lite/
├── arizen-launcher/              # Launcher source (Android/Java)
│   └── src/main/java/.../
│       ├── MainActivity.java
│       ├── ArizenCommandPaletteActivity.java
│       ├── ArizenSystemMonitorActivity.java
│       └── ArizenWorkspaceActivity.java
├── scripts/
│   ├── download_lineage.sh       # Auto-download LineageOS zip
│   ├── unpack_lineage.sh         # Extract + mount system.img
│   ├── inject_arizenos.sh        # Inject launcher, branding, tuning
│   ├── repack_odin.sh            # Package Odin AP.tar.md5
│   ├── debloat.sh                # ADB live debloat (optional)
│   └── generate_md5.sh           # MD5 utility
├── config/
│   ├── performance_profiles.sh   # 3 CPU governor profiles
│   ├── lmk_config.sh             # LMK tuning (2GB/3GB)
│   ├── thermal_config.sh         # Thermal zone management
│   ├── sysctl_arizen.conf        # TCP + VM + scheduler sysctl
│   ├── debloat_adb.sh            # ADB debloat script (84 packages)
│   └── zram_config.sh            # ZRAM setup (auto 2/3GB detect)
├── arizen-assets/
│   ├── init.arizen.rc            # Init.rc reference
│   └── build.prop.patch          # build.prop patch reference
├── docs/
│   ├── build-guide.md            # Full LineageOS build guide
│   ├── flashing-guide.md         # Detailed flash instructions
│   ├── optimization.md           # Performance tuning guide
│   ├── command-palette.md        # Command palette reference
│   └── troubleshooting.md        # Common issues
└── firmware/                     # Place LineageOS zip here
```

---

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/build-guide.md](docs/build-guide.md) | Full LineageOS → ArizenOS build process |
| [docs/command-palette.md](docs/command-palette.md) | Command palette reference |
| [docs/optimization.md](docs/optimization.md) | Post-flash performance tuning |
| [docs/flashing-guide.md](docs/flashing-guide.md) | Detailed flash instructions |
| [docs/troubleshooting.md](docs/troubleshooting.md) | Common issues & solutions |
| [PANDUAN_LENGKAP.md](PANDUAN_LENGKAP.md) | Full guide in Bahasa Indonesia |

---

## ⚠️ Disclaimer

ArizenOS Lite modifies device firmware. By using this project:
- **Always backup your data before flashing**
- Keep a recovery method (TWRP + LineageOS zip)
- Flash at your own risk — maintainer is not responsible for bricked devices
- Not affiliated with Samsung Electronics or the LineageOS project

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
