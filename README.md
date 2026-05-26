<p align="center">
  <img src="docs/assets/arizenos-logo.png" alt="ArizenOS Lite" width="200"/>
</p>

<h1 align="center">ArizenOS Lite</h1>
<p align="center"><b>Samsung Stock Firmware AI Mod ROM</b></p>
<p align="center">
  <img src="https://img.shields.io/badge/Device-SM--T295-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/Android-9%20Pie-green?style=flat-square"/>
  <img src="https://img.shields.io/badge/Base-Samsung%20Stock-orange?style=flat-square"/>
  <img src="https://img.shields.io/badge/Flash-Odin-red?style=flat-square"/>
  <img src="https://img.shields.io/badge/RAM-2GB%20Optimized-purple?style=flat-square"/>
</p>

---

> **ArizenOS Lite** transforms stock Samsung firmware into a premium AI-native operating system experience — maximizing stability and Odin flash success rate on the Samsung Galaxy Tab A 8.0 (2019) SM-T295.

---

## ✨ What is ArizenOS Lite?

ArizenOS Lite is **not** a full custom ROM rewrite. It is a careful, intelligent modification of official Samsung stock firmware — preserving full hardware compatibility, Samsung kernel, vendor blobs, and partition layout — while layering a completely redesigned AI-native user experience on top.

**Architecture:**
```
Samsung Stock Firmware
  + Samsung Debloat
  + Arizen Launcher
  + Arizen AI Layer (Arizen Core)
  + UI Redesign (AMOLED dark theme)
  + Performance Optimization (2GB RAM tuned)
  + Odin Repackaging
= ArizenOS Lite
```

---

## 📱 Device

| Field | Value |
|-------|-------|
| **Device** | Samsung Galaxy Tab A 8.0 (2019) |
| **Model** | SM-T295 |
| **Android** | 9 Pie (One UI) |
| **RAM** | 2 GB |
| **Storage** | 32 GB |
| **Flash Method** | Odin |

---

## 🚀 Features

### Arizen Launcher
- Dashboard-style AI-centered home screen
- Ultra lightweight — optimized for 2GB RAM
- Ambient AI assistant card
- Smart widgets & quick actions
- Modern search & contextual suggestions
- Clean app grid with elegant dock

### Arizen Core (AI System)
- Cloud-based AI assistant (no local LLMs)
- Modular tool system:
  - `AppLauncherTool` — launch apps via voice/AI
  - `NotificationTool` — manage notifications
  - `WifiTool` — toggle/connect WiFi
  - `BluetoothTool` — manage Bluetooth
  - `ClipboardTool` — clipboard AI actions
  - `BrowserTool` — AI-powered web search
  - `FileSearchTool` — smart file search
  - `SettingsTool` — change system settings
  - `AutomationTool` — task automation
- Supports: Groq, Together AI, OpenAI-compatible, Cloudflare AI, Ollama

### UI Design Language
- AMOLED dark theme
- Matte surfaces with subtle transparency
- Thin typography & elegant spacing
- Smooth cinematic transitions
- Inspired by: visionOS, Nothing OS, Tesla UI, Jarvis

### Performance
- ZRAM tuning for 2GB RAM
- Animation speed optimization
- Background process limiting
- Battery & thermal optimization
- Debloated Samsung system

---

## 📦 Build & Flash

### Prerequisites
- Linux/macOS build environment
- `simg2img`, `img2simg`, `e2fsck`, `resize2fs`
- `tar`, `openssl` (for md5 generation)
- Official Samsung firmware for SM-T295
- Odin (Windows) or Heimdall (Linux)

### Quick Start

```bash
# 1. Clone this repo
git clone https://github.com/Alrizz-art/ArizenOS-Lite.git
cd ArizenOS-Lite

# 2. Place your Samsung firmware zip in firmware/
mkdir firmware
# Copy your SM-T295 firmware zip here

# 3. Extract firmware
chmod +x scripts/*.sh
./scripts/extract_firmware.sh firmware/YOUR_FIRMWARE.zip

# 4. Unpack AP partition
./scripts/unpack_ap.sh

# 5. Inject ArizenOS layer
./scripts/inject_arizenos.sh

# 6. Repack AP
./scripts/repack_ap.sh

# 7. Package for Odin
./scripts/package_odin.sh

# Output: output/ArizenOSLite_v1_SM-T295_AP.tar.md5
```

### Flash via Odin
1. Boot device into Download Mode (`Power + Vol Down + Bixby`)
2. Open Odin on PC
3. Select `AP` → `output/ArizenOSLite_v1_SM-T295_AP.tar.md5`
4. Click **Start**
5. Device reboots into ArizenOS Lite

---

## 📚 Documentation

- [Flashing Guide](docs/flashing-guide.md)
- [AI Setup Guide](docs/ai-setup-guide.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Optimization Guide](docs/optimization.md)
- [Debloat List](config/debloat_list.txt)

---

## ⚠️ Disclaimer

ArizenOS Lite modifies your device firmware. While designed to be as safe as possible:
- Always backup your data before flashing
- Keep stock firmware for recovery
- Flash at your own risk
- This project is not affiliated with Samsung

---

## 📜 License

MIT License — see [LICENSE](LICENSE)

---

<p align="center">Made with ❤️ by <b>Alrizz-art</b> | ArizenOS Project</p>
