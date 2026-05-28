# ArizenOS Lite — Build Guide

> GSI-based custom ROM → Odin AP.tar.md5 untuk SM-T295

---

## Apa itu GSI?

**GSI = Generic System Image** — satu file `system.img` yang bisa jalan di semua device Android yang support Project Treble.

SM-T295 support Treble sejak Android 9 (stock Samsung). Artinya bisa pakai GSI apapun — tidak perlu cari build device-specific yang mungkin tidak ada.

```
GSI system.img  (Superior OS / AOSP / LineageOS GSI)
       ↓  scripts/unpack_gsi.sh
system.img di-mount (ext4, writable)
       ↓  scripts/inject_arizenos.sh
system.img + ArizenOS (Launcher + branding + tuning)
       ↓  scripts/repack_odin.sh
ArizenOS-Lite_v1.1_SM-T295_YYYYMMDD_AP.tar.md5
       ↓  Odin / Heimdall
SM-T295 running ArizenOS Lite
```

---

## Step 0 — Dapatkan GSI untuk SM-T295

### Rekomendasi: Superior OS GSI

Thread XDA khusus SM-T295:
> **https://xdaforums.com/t/rom-gsi-sm-t295-gto-superior-os-12l-13-gsi-for-galaxy-tab-a-8-0-2019.4650847/**

Buka thread tersebut, cari link download (biasanya SourceForge / Google Drive / Telegram channel dev-nya).

**Yang perlu didownload:**
- Format file: `.img` (bukan `.zip`)
- Variant: `arm64_bvS` atau `arm64_bgS`
  - `arm64` = 64-bit (Snapdragon 429 adalah arm64)
  - `bvS` = tanpa GApps (lebih ringan, direkomendasikan untuk ArizenOS)
  - `bgS` = dengan GApps

### Alternatif GSI lain (semua arm64)

| ROM | Link | Android |
|-----|------|---------|
| **Superior OS GSI** | XDA thread di atas | 12L / 13 |
| AOSP GSI (Google) | https://developer.android.com/topic/generic-system-image | 12 / 13 |
| LineageOS GSI | https://github.com/phhusson/treble_experimentations/releases | 19/20 |
| crDroid GSI | https://sourceforge.net/projects/crdroid-gsi/ | 13 |
| Pixel Experience GSI | https://github.com/ponces/treble_build_pe/releases | 13 |

### ⚠️ Masalah EROFS

GSI Android 12L/13 sering pakai **EROFS** (filesystem read-only terkompresi) — tidak bisa dimodifikasi langsung.

Jika dapat error EROFS:
- Cari variant yang ada tulisan **ext4** di nama filenya
- Atau cari build Android 11 (biasanya ext4)
- Atau tanya dev di thread XDA apakah ada ext4 variant

```bash
# Cek apakah .img kamu EROFS atau ext4:
file your_system.img
# ext4 filesystem data → ✅ bisa dipakai
# EROFS filesystem     → ❌ cari yang lain
```

### Cara taruh file

```bash
mkdir -p firmware
# Salin .img ke sini:
cp /path/to/system_arm64_bvS.img firmware/
# Atau download langsung jika ada URL:
bash scripts/download_rom.sh "https://direct-url.com/system.img"
```

---

## Step 1 — Persiapan (Linux/macOS/WSL2)

### Ubuntu / Debian / WSL2
```bash
sudo apt-get update && sudo apt-get install -y \
    android-tools-fsutils \
    e2fsprogs \
    brotli \
    xz-utils \
    unzip \
    lz4 \
    file \
    python3 \
    wget curl git
```

### macOS — pakai Docker
```bash
# Pastikan Docker Desktop sudah terinstall
docker run --privileged -it \
    -v "$(pwd):/workspace" \
    ubuntu:22.04 bash

# Di dalam container:
cd /workspace
apt-get update && apt-get install -y \
    android-tools-fsutils e2fsprogs brotli xz-utils unzip lz4 file python3 wget curl git
```

### Windows — pakai WSL2
```powershell
# PowerShell (sebagai Admin)
wsl --install -d Ubuntu-22.04
# Lalu buka Ubuntu dari Start Menu, jalankan perintah Ubuntu di atas
```

---

## Step 2 — Build Arizen Launcher APK

```bash
# Butuh Java 17
sudo apt install openjdk-17-jdk  # Ubuntu

cd arizen-launcher
chmod +x gradlew
./gradlew assembleRelease

cp app/build/outputs/apk/release/app-release*.apk ../ArizenLauncher.apk
cd ..
ls -lh ArizenLauncher.apk   # harus ada dan > 1MB
```

> Jika tidak mau setup Java, biarkan CI/GitHub Actions yang build otomatis.

---

## Step 3 — Unpack GSI

```bash
sudo bash scripts/unpack_gsi.sh
# Script otomatis deteksi file di firmware/
# Atau tentukan path:
sudo bash scripts/unpack_gsi.sh firmware/system_arm64_bvS.img
```

Script ini:
1. Deteksi format: raw `.img`, sparse, `.xz`, `.zip`
2. Convert sparse → raw ext4 jika perlu
3. Cek filesystem (e2fsck)
4. Expand +250MB untuk file ArizenOS
5. Mount di `work/system_mount/`
6. Deteksi system-as-root (SAR) structure otomatis

---

## Step 4 — Inject ArizenOS

```bash
sudo bash scripts/inject_arizenos.sh
```

Yang dilakukan (10 langkah):
1. Hapus launcher bawaan GSI (Trebuchet / Launcher3 / PixelLauncher)
2. Install Arizen Launcher sebagai default HOME
3. Bersihkan app GSI yang tidak perlu
4. Install boot animation ArizenOS (jika ada di `bootanimation/`)
5. Install wallpaper (jika ada `arizen_wallpaper.png`)
6. Patch `build.prop`: identitas ArizenOS 1.1 Zenith
7. Install ZRAM init.rc
8. Install CPU + LMK performance init.rc
9. Install performance profiles + sysctl config
10. Buat `version.json` manifest

---

## Step 5 — Repack jadi Odin AP.tar.md5

```bash
sudo bash scripts/repack_odin.sh
```

Output: `output/ArizenOS-Lite_v1.1_SM-T295_YYYYMMDD_AP.tar.md5`

---

## Step 6 — Flash via Odin

### Persiapan: Flash TWRP dulu (wajib)

SM-T295 perlu TWRP custom recovery sebelum bisa flash GSI-based ROM.

1. Download TWRP untuk SM-T295: https://twrp.me/samsung/samsunggalaxytaba82019lte.html
2. Boot ke Download Mode: `Power + Vol Down` → `Vol Up` untuk konfirmasi
3. Odin → AP → pilih TWRP `.tar` → Start
4. Setelah reboot, tahan `Power + Vol Up` untuk masuk TWRP

### Flash ArizenOS via Odin

1. Download `ArizenOS-Lite_v1.1_SM-T295_YYYYMMDD_AP.tar.md5`
2. Boot ke Download Mode
3. Buka **Odin v3.14+**
4. Klik **AP** → pilih file `.tar.md5`
5. Options: ✅ Auto Reboot · ✅ F.Reset Time · ❌ Re-Partition
6. **Start** → tunggu **PASS!**

### Flash via Heimdall (Linux/macOS)

```bash
heimdall flash --SYSTEM work/system_raw.img --no-reboot
```

### Flash via TWRP (alternatif)

1. Reboot ke TWRP (Power + Vol Up)
2. Wipe → Dalvik/ART Cache + Cache
3. Install → pilih file `.tar.md5`
4. Reboot System

---

## Troubleshooting

### "EROFS filesystem detected"
Cari variant **ext4** dari ROM yang sama, atau coba ROM Android 11.

### "Mount failed"
Jalankan dengan `sudo`. Di Docker, tambahkan flag `--privileged`.

### "simg2img not found"
```bash
sudo apt install android-tools-fsutils
```

### "ArizenLauncher.apk not found"
```bash
cd arizen-launcher && ./gradlew assembleRelease
cp app/build/outputs/apk/release/*.apk ../ArizenLauncher.apk
```

### Device bootloop setelah flash
1. Boot ke TWRP (Power + Vol Up)
2. Wipe → Factory Reset
3. Flash ulang ROM base (GSI) via TWRP
4. Atau flash Samsung stock dari [samfw.com](https://samfw.com)

---

## Estimasi Waktu Build

| Step | Waktu |
|------|-------|
| Download GSI | 10–30 menit (tergantung koneksi) |
| Build Launcher | 3–8 menit |
| Unpack GSI | 5–15 menit |
| Inject ArizenOS | 1–2 menit |
| Repack Odin | 5–15 menit |
| **Total** | **~25–70 menit** |

---

*ArizenOS Lite v1.1 Zenith — SM-T295 (GSI-based)*
