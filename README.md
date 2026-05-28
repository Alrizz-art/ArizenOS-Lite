<p align="center">
  <img src="docs/assets/arizenos-logo.svg" alt="ArizenOS Lite" width="380"/>
</p>

<p align="center">
  <a href="https://github.com/Alrizz-art/ArizenOS-Lite/actions/workflows/build.yml">
    <img src="https://github.com/Alrizz-art/ArizenOS-Lite/actions/workflows/build.yml/badge.svg" alt="Build Status"/>
  </a>
  <img src="https://img.shields.io/badge/Device-SM--T295-0D6EFD?style=flat-square&logo=samsung&logoColor=white"/>
  <img src="https://img.shields.io/badge/Android-12L%20%2F%2013-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Base-GSI%20%28Treble%29-8BC34A?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Flash-Odin%20%2F%20Heimdall-E53935?style=flat-square"/>
  <img src="https://img.shields.io/badge/RAM-2GB%20Optimized-7B2FFF?style=flat-square"/>
  <img src="https://img.shields.io/badge/License-MIT-22C55E?style=flat-square"/>
</p>

<h3 align="center">GSI-based custom ROM — Odin-flashable AP.tar.md5 untuk SM-T295</h3>

---

## Apa itu ArizenOS Lite?

ArizenOS Lite adalah custom Android experience untuk **Samsung Galaxy Tab A 8.0" 2019 (SM-T295)**, dibangun di atas **GSI (Generic System Image)** — memanfaatkan dukungan Project Treble yang ada di SM-T295 sejak Android 9.

GSI seperti Superior OS dipakai sebagai fondasi bersih. ArizenOS menambahkan identitas custom, tools produktivitas, dan hardware tuning khusus untuk device ini.

```
GSI system.img  (Superior OS / AOSP / LineageOS GSI — arm64)
  ├── Launcher GSI dihapus (Trebuchet / Launcher3)
  ├── Arizen Launcher — AI-centered home screen, default HOME
  ├── ArizenOS branding — identitas 100% custom (v1.1 Zenith)
  ├── Command Palette — >CMD launcher ala Linux (16 perintah)
  ├── System Monitor — dashboard RAM/CPU/battery/thermal live
  ├── Workspace Mode — focus mode dengan session timer
  ├── ZRAM 512MB — kompresi lz4 untuk 2GB RAM
  ├── Performance Profiles — balanced / performance / battery saver
  ├── LMK + sysctl tuning — memory management 2GB-optimized
  └── Odin repackaging — valid AP.tar.md5 flashable via Odin/Heimdall
= ArizenOS Lite v1.1 "Zenith"
```

---

## Device

| Field | Value |
|-------|-------|
| **Model** | Samsung Galaxy Tab A 8.0" (2019) |
| **Model Number** | SM-T295 |
| **Chipset** | Snapdragon 429 (MSM8937), 4x Cortex-A53 @ 1.4GHz |
| **RAM** | 2 GB |
| **Storage** | 32 GB |
| **Treble Support** | ✅ Ya (sejak Android 9 stock) |
| **Base ROM** | GSI arm64 — Superior OS / AOSP / LineageOS GSI |
| **Flash Method** | Odin (Windows) · Heimdall (macOS/Linux) |

---

## Kenapa GSI?

SM-T295 **tidak punya device-specific LineageOS build** yang aktif dirawat. Tapi SM-T295 support Project Treble — artinya bisa pakai GSI manapun.

| | Samsung Stock | ArizenOS Lite (GSI) |
|--|--------------|---------------------|
| Bloatware | 60–80 Samsung apps | Bersih dari awal |
| Bixby | Selalu jalan | Tidak ada |
| Android versi | Max Android 11 (official) | 12L / 13 |
| Modifiable | Sulit (EROFS baru) | ✅ Ya (ext4 GSI) |
| Privasi | Samsung telemetry | Bersih |

---

## Download Base ROM

### Rekomendasi: Superior OS GSI untuk SM-T295

**Thread XDA (khusus SM-T295):**
> https://xdaforums.com/t/rom-gsi-sm-t295-gto-superior-os-12l-13-gsi-for-galaxy-tab-a-8-0-2019.4650847/

Buka thread → cari link download → pilih variant **arm64_bvS** (tanpa GApps, paling ringan).

> ⚠️ Pastikan download **ext4 variant** bukan EROFS. Cek dengan: `file system.img`

---

## Features

### Arizen Launcher (v1.1 Zenith)
- Live stat bar — sisa RAM, profile performa, tombol cepat
- **`> CMD` Command Palette** — launcher ala Linux, 16 system command
- **System Monitor** — dashboard hardware live (RAM/CPU/baterai/suhu)
- **Workspace Mode** — focus timer, session management, bebas distraksi
- AI assistant shortcut (Groq / Together.ai — gratis)
- App grid configurable, dock minimal

### Performa (2GB RAM optimized)

| Tuning | Nilai | Efek |
|--------|-------|------|
| ZRAM | 512MB lz4 | Compressed swap, ~2.5GB efektif |
| Dalvik heapgrowthlimit | 128MB | GC lebih cepat, RAM lebih bebas |
| `ro.sys.fw.bg_apps_limit` | 8 | Batas proses background |
| LMK minfree | 18→96MB | Kill empty proses lebih cepat |
| CPU governor | schedutil | Kernel-driven, efisien di SD429 |
| `vm.swappiness` | 60 | Swap ke ZRAM sebelum OOM |

### Performance Profiles

| Profile | Governor | Kapan dipakai |
|---------|----------|--------------|
| Balanced | schedutil | Default — harian |
| Performance | interactive | AI, coding, task berat |
| Battery Saver | conservative | Sesi panjang, baca |

---

## Build Pipeline

```
firmware/system_arm64_bvS.img   ← GSI dari XDA thread
          │
          ▼
scripts/unpack_gsi.sh           ← detect format, mount ext4
          │
          ▼
scripts/inject_arizenos.sh      ← launcher, branding, tuning, init.rc
          │
          ▼
scripts/repack_odin.sh          ← unmount, shrink, sparse, AP.tar.md5
          │
          ▼
output/ArizenOS-Lite_v1.1_SM-T295_YYYYMMDD_AP.tar.md5
```

---

## Quick Start — Local Build

### 1. Prasyarat
```bash
# Ubuntu / WSL2
sudo apt-get install -y android-tools-fsutils e2fsprogs brotli xz-utils unzip lz4 file python3 wget

# macOS — gunakan Docker (lihat docs/build-guide.md)
```

### 2. Download GSI
```bash
# Lihat panduan download:
bash scripts/download_rom.sh

# Atau langsung taruh .img di firmware/:
mkdir -p firmware
cp /path/to/system_arm64_bvS.img firmware/
```

### 3. Build Launcher
```bash
cd arizen-launcher
./gradlew assembleRelease
cp app/build/outputs/apk/release/*.apk ../ArizenLauncher.apk
cd ..
```

### 4. Build ArizenOS
```bash
chmod +x scripts/*.sh
sudo bash scripts/unpack_gsi.sh          # extract + mount
sudo bash scripts/inject_arizenos.sh     # inject ArizenOS
sudo bash scripts/repack_odin.sh         # package AP.tar.md5

# Output:
ls -lh output/*.tar.md5
```

---

## Flash Instructions

### Persiapan: Flash TWRP dulu

SM-T295 perlu TWRP sebelum bisa flash custom ROM:
1. Download TWRP untuk SM-T295: [twrp.me](https://twrp.me/samsung/samsunggalaxytaba82019lte.html)
2. Download Mode: `Power + Vol Down` → `Vol Up`
3. Odin → AP → pilih TWRP `.tar` → Start

### Windows — Odin

1. Download `ArizenOS-Lite_v*_SM-T295_*_AP.tar.md5` dari [Releases](../../releases)
2. Boot ke Download Mode: `Power + Vol Down` → `Vol Up`
3. Buka **Odin v3.14.4+**
4. Klik **AP** → pilih file `.tar.md5`
5. Options: ✅ Auto Reboot · ✅ F.Reset Time · ❌ Re-Partition
6. **Start** → tunggu **PASS!**

### Linux/macOS — Heimdall

```bash
heimdall flash --SYSTEM work/system_raw.img --no-reboot
```

### Via TWRP

1. Reboot ke TWRP (`Power + Vol Up` saat boot)
2. Wipe → Dalvik/ART + Cache
3. Install → pilih `ArizenOS-Lite*.tar.md5`
4. Reboot System

---

## Recovery

Jika device bootloop:
1. Boot ke TWRP (`Power + Vol Up`)
2. Wipe → Factory Reset
3. Flash GSI base langsung via TWRP
4. Atau flash Samsung stock dari [samfw.com](https://samfw.com) via Odin

---

## Project Structure

```
ArizenOS-Lite/
├── arizen-launcher/
│   └── src/main/java/.../
│       ├── MainActivity.java
│       ├── ArizenCommandPaletteActivity.java
│       ├── ArizenSystemMonitorActivity.java
│       └── ArizenWorkspaceActivity.java
├── scripts/
│   ├── download_rom.sh           ← panduan + helper download GSI
│   ├── unpack_gsi.sh             ← extract + mount GSI system.img
│   ├── inject_arizenos.sh        ← inject launcher, branding, tuning
│   ├── repack_odin.sh            ← package Odin AP.tar.md5
│   ├── unpack_lineage.sh         ← alternatif: unpack LineageOS zip
│   └── download_lineage.sh       ← alternatif: download LineageOS
├── config/
│   ├── performance_profiles.sh   ← 3 CPU governor profiles
│   ├── lmk_config.sh             ← LMK tuning (2GB/3GB)
│   ├── thermal_config.sh         ← thermal zone
│   ├── sysctl_arizen.conf        ← TCP + VM + scheduler
│   ├── debloat_adb.sh            ← ADB live debloat (opsional)
│   └── zram_config.sh            ← ZRAM setup
├── arizen-assets/
│   ├── init.arizen.rc            ← referensi init.rc
│   └── build.prop.patch          ← referensi build.prop
├── docs/
│   ├── build-guide.md            ← panduan build lengkap (Indonesia + English)
│   ├── command-palette.md        ← referensi command palette
│   ├── optimization.md           ← tips performa post-flash
│   ├── flashing-guide.md         ← panduan flash detail
│   └── troubleshooting.md        ← masalah umum
└── firmware/                     ← taruh GSI .img di sini
```

---

## Dokumentasi

| Dokumen | Keterangan |
|---------|-----------|
| [docs/build-guide.md](docs/build-guide.md) | Panduan build GSI → ArizenOS lengkap |
| [docs/command-palette.md](docs/command-palette.md) | Referensi command palette |
| [docs/optimization.md](docs/optimization.md) | Optimasi post-flash |
| [docs/flashing-guide.md](docs/flashing-guide.md) | Flash via Odin / TWRP |
| [docs/troubleshooting.md](docs/troubleshooting.md) | Masalah umum |
| [PANDUAN_LENGKAP.md](PANDUAN_LENGKAP.md) | Panduan Bahasa Indonesia lengkap |

---

## ⚠️ Disclaimer

ArizenOS Lite memodifikasi firmware device. Dengan menggunakan project ini:
- **Selalu backup data sebelum flash**
- Simpan cara recovery (TWRP + GSI base)
- Flash dengan risiko sendiri — maintainer tidak bertanggung jawab atas device yang brick
- Project ini tidak berafiliasi dengan Samsung Electronics

---

## License

[MIT License](LICENSE) — © 2025 Alrizz-art / ArizenOS Project

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/Alrizz-art"><b>Alrizz-art</b></a><br/>
  <sub>ArizenOS Project — github.com/Alrizz-art/ArizenOS-Lite</sub>
</p>
