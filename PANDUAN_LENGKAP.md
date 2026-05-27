# 📱 Panduan Lengkap ArizenOS Lite — MacBook ke SM-T295

> Dari nol sampai firmware terflash — step by step

---

## ⚠️ Baca Dulu

- **Backup semua data di tablet kamu** — proses flash akan menghapus semua data
- Pastikan baterai tablet minimal **50%** sebelum flash
- Simpan firmware Samsung original kamu sebagai backup
- Ikuti langkah secara **urut** — jangan skip

---

## 🗺️ Gambaran Proses

```
MacBook
  │
  ├─ 1. Install Docker Desktop
  ├─ 2. Clone repo ArizenOS
  ├─ 3. Jalankan auto_build_mac.sh
  │       │
  │       ├─ Download firmware Samsung (~1.5GB)
  │       ├─ Unpack AP partition (system.img)
  │       ├─ Debloat Samsung bloatware
  │       ├─ Inject Arizen Launcher + AI Layer
  │       ├─ Tweak build.prop + ZRAM
  │       └─ Repack → ArizenOSLite_v1_SM-T295_AP.tar.md5
  │
  ├─ 4. Install Heimdall (flash tool untuk Mac)
  └─ 5. Flash ke SM-T295 via USB
```

**Estimasi waktu total:** 45-90 menit (tergantung kecepatan internet & Mac)

---

## LANGKAH 1 — Install Docker Desktop

Docker adalah aplikasi yang menjalankan Linux di dalam Mac kamu. Dibutuhkan untuk proses build.

### 1.1 Download Docker Desktop
Buka browser, pergi ke:
```
https://www.docker.com/products/docker-desktop/
```
Download versi **Mac (Apple Silicon)** kalau MacBook kamu pakai chip M1/M2/M3, atau **Mac (Intel)** kalau pakai chip Intel.

> Cek chip kamu: klik ikon Apple (pojok kiri atas) → About This Mac → chip yang tercantum

### 1.2 Install Docker
1. Buka file `.dmg` yang sudah didownload
2. Drag **Docker** ke folder **Applications**
3. Buka Docker dari Applications
4. Klik **Accept** pada Terms of Service
5. Tunggu sampai icon Docker di menu bar berwarna **putih/solid** (bukan animasi)

### 1.3 Verifikasi Docker berjalan
Buka **Terminal** (Cmd + Space → ketik "Terminal" → Enter) lalu ketik:
```bash
docker --version
```
Harus muncul sesuatu seperti: `Docker version 24.x.x`

```bash
docker info
```
Harus muncul banyak info tanpa error.

✅ **Docker siap!**

---

## LANGKAH 2 — Install Git & Clone Repo

### 2.1 Install Git (kalau belum ada)
Di Terminal:
```bash
git --version
```
Kalau belum terinstall, macOS akan otomatis menawarkan install Xcode Command Line Tools. Klik **Install** dan tunggu.

### 2.2 Clone repo ArizenOS
Di Terminal, ketik:
```bash
cd ~/Desktop
git clone https://github.com/Alrizz-art/ArizenOS-Lite.git
cd ArizenOS-Lite
```

Sekarang kamu berada di folder `ArizenOS-Lite` di Desktop.

---

## LANGKAH 3 — Jalankan Auto Build Script

Ini adalah langkah utama. Satu script akan mengurus semua proses otomatis.

### 3.1 Beri izin eksekusi ke script
```bash
chmod +x auto_build_mac.sh
chmod +x scripts/*.sh
```

### 3.2 Jalankan!
```bash
./auto_build_mac.sh
```

### 3.3 Ikuti prompt yang muncul

**Prompt 1 — Pilih Region:**
```
Select Your Region:
  1) XSP — Global / International
  2) INS — India
  3) EUX — Europe
  ...
Select region [1-6, default=1]:
```
Tekan **Enter** saja (pilih default XSP - Global) kecuali kamu tahu region spesifik tablet kamu.

> Untuk cek region tablet: Settings → About tablet → Model number
> Atau lihat stiker di belakang/dalam slot SIM

**Kemudian script akan berjalan otomatis:**

```
[ArizenOS] Downloading firmware from Samsung servers...
(ini makan waktu 10-30 menit — firmware ~1.5GB)

[ArizenOS] Step 1/5: Extracting firmware...
[ArizenOS] Step 2/5: Unpacking AP partition...
[ArizenOS] Step 3/5: Injecting ArizenOS components...
[ArizenOS] Step 4/5: Repacking AP...
[ArizenOS] Step 5/5: Packaging for Odin...

╔════════════════════════════════════════════════╗
║         ArizenOS Lite Build Complete!          ║
╚════════════════════════════════════════════════╝
[OK] Output: ArizenOSLite_v1_SM-T295_AP.tar.md5 (1.2G)
```

### 3.4 Cek hasil build
```bash
ls -lh output/
```
Harus ada file: `ArizenOSLite_v1_SM-T295_AP.tar.md5`

✅ **Firmware ArizenOS Lite siap!**

---

## LANGKAH 4 — Install Heimdall (Flash Tool untuk Mac)

Heimdall adalah versi Mac dari Odin — untuk flash firmware Samsung dari macOS.

### 4.1 Install Homebrew (kalau belum ada)
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```
Ikuti instruksi di Terminal, masukkan password Mac kamu kalau diminta.

Setelah selesai, kalau pakai chip M1/M2/M3 tambahkan ini:
```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
source ~/.zprofile
```

### 4.2 Install Heimdall
```bash
brew install heimdall
```

### 4.3 Verifikasi
```bash
heimdall version
```
Harus muncul: `Heimdall v1.x.x`

✅ **Heimdall siap!**

---

## LANGKAH 5 — Masuk Download Mode di Tablet

Ini adalah mode khusus Samsung untuk menerima firmware baru.

### 5.1 Matikan tablet
Tekan **Power** hingga muncul menu → **Power off** → **Power off** lagi.

### 5.2 Masuk Download Mode
Tahan **3 tombol bersamaan**:
```
Power + Volume Down + Bixby
```
(Bixby = tombol di sisi kiri, di bawah volume)

Tahan sampai layar bergetar/menyala.

### 5.3 Konfirmasi
Ketika muncul layar biru/hijau dengan gambar Android dan peringatan, tekan:
```
Volume Up
```

Layar sekarang menampilkan "Downloading... Do not turn off target"

✅ **Tablet dalam Download Mode!**

---

## LANGKAH 6 — Sambungkan Tablet ke MacBook

### 6.1 Gunakan kabel USB yang baik
Gunakan kabel USB original Samsung atau kabel data berkualitas baik.
Sambungkan tablet ke MacBook.

### 6.2 Verifikasi terdeteksi
```bash
heimdall detect
```
Harus muncul:
```
Device detected
```

Kalau muncul "No device found":
- Coba port USB yang berbeda di MacBook
- Coba kabel yang berbeda
- Pastikan tablet sudah dalam Download Mode (layar harus menampilkan pesan downloading)
- Coba `sudo heimdall detect` dengan password Mac kamu

---

## LANGKAH 7 — Flash ArizenOS Lite! 🚀

### 7.1 Siapkan command flash
```bash
cd ~/Desktop/ArizenOS-Lite
```

### 7.2 Lihat partisi yang tersedia
```bash
heimdall print-pit
```
Ini akan menampilkan daftar partisi tablet. Cari yang namanya **SYSTEM** atau **system**.

### 7.3 Flash AP (System Partition)
```bash
heimdall flash --SYSTEM output/ArizenOSLite_v1_SM-T295_AP.tar.md5 --no-reboot
```

Kalau command di atas gagal, coba format Odin langsung:
```bash
heimdall flash --verbose \
  --AP output/ArizenOSLite_v1_SM-T295_AP.tar.md5
```

**Proses flash berjalan:**
```
Uploading SYSTEM...
████████████████████████████████ 100%
SYSTEM upload successful
```

Flash memakan waktu **5-15 menit**. Jangan cabut kabel selama proses!

### 7.4 Reboot tablet
Setelah flash selesai:
```bash
heimdall --reboot
```
Atau: tahan **Power + Volume Up** bersamaan selama 10 detik.

---

## LANGKAH 8 — First Boot ArizenOS Lite

### 8.1 Tunggu first boot
First boot memakan waktu **2-5 menit** — ini normal karena sistem sedang setup ulang.
Jangan matikan tablet selama proses ini.

### 8.2 Setup awal
Ikuti wizard setup Android yang muncul.
Arizen Launcher akan aktif sebagai home screen default.

### 8.3 Setup AI (opsional)
1. Buka **Arizen Settings** dari home screen
2. Masuk ke **AI → API Key**
3. Pilih provider (Groq direkomendasikan — gratis & cepat)
4. Daftar di [console.groq.com](https://console.groq.com) → dapat API key gratis
5. Paste API key → Save

✅ **ArizenOS Lite berhasil terinstall!**

---

## 🆘 Troubleshooting

### "Docker not running"
→ Buka Docker Desktop dari Applications, tunggu icon di menu bar solid putih

### "No device found" di Heimdall
→ Coba kabel/port USB berbeda
→ Pastikan Download Mode aktif (layar harus kuning/biru dengan teks downloading)
→ Coba: `sudo heimdall detect`

### Firmware download gagal
→ Cek koneksi internet
→ Coba region berbeda (contoh: INS atau EUX)
→ Download manual dari [samfw.com](https://samfw.com) → search SM-T295 → taruh zip di folder `firmware/`

### Flash gagal di tengah jalan
→ **Jangan panik!** Tablet masih aman karena masih dalam Download Mode
→ Cabut kabel, ulangi dari Langkah 5
→ Coba pakai flag `--verbose` untuk lihat error detail

### Stuck di boot loop
→ Boot ke Recovery: tahan Power + Vol Up + Bixby
→ Pilih Wipe data/factory reset
→ Kalau masih gagal, flash firmware Samsung original untuk recovery

### Kembali ke stock Samsung
```bash
# Download firmware original di samfw.com untuk SM-T295
# Lalu:
heimdall flash --SYSTEM firmware_original/system.img.ext4
```

---

## 📋 Rangkuman Cepat (Cheat Sheet)

```bash
# SETUP (sekali saja)
brew install --cask docker        # Install Docker
brew install heimdall             # Install Heimdall
git clone https://github.com/Alrizz-art/ArizenOS-Lite.git
cd ArizenOS-Lite

# BUILD
chmod +x auto_build_mac.sh scripts/*.sh
./auto_build_mac.sh               # Build otomatis (45-90 menit)

# FLASH
# 1. Boot tablet ke Download Mode (Power + Vol Down + Bixby → Vol Up)
# 2. Sambungkan USB ke Mac
heimdall detect                   # Verifikasi terdeteksi
heimdall flash --SYSTEM output/ArizenOSLite_v1_SM-T295_AP.tar.md5
```

---

*ArizenOS Lite — Transform your Galaxy Tab A into a premium AI-native experience*
*Made by Alrizz-art | github.com/Alrizz-art/ArizenOS-Lite*
