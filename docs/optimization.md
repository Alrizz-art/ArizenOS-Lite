# ArizenOS Lite — Optimization Guide

> SM-T295 (Snapdragon 429, 2GB/3GB RAM) — v1.1 Zenith

---

## What ArizenOS Does Automatically (On Flash)

### ZRAM — Virtual RAM Expansion
- **Size:** 512MB (2GB) / 768MB (3GB) — auto-detected at boot
- **Algorithm:** lz4hc → lz4 → lzo (best available)
- **Effect:** Effectively gives you ~2.5–3GB usable memory
- **Location:** `/system/etc/init/arizen_zram.rc`

### Debloat — Background Process Reduction
- ~84 Samsung system apps removed (Bixby, Facebook, Knox, Galaxy Store, etc.)
- Frees 300–600MB RAM
- Reduces background services from ~40 to ~12
- Does NOT remove Camera, WiFi, Bluetooth, or core Samsung drivers

### build.prop — System-level Tuning
- Dalvik heap optimized for 2GB RAM (`heapgrowthlimit=128m`)
- Background apps limited to 8 (`ro.sys.fw.bg_apps_limit=8`)
- `ro.config.animation_scale=0.5` — 2x faster UI transitions
- LMK tuned to kill empty processes earlier, keep foreground alive longer
- TRIM enabled (`ro.sys.fw.use_trim_settings=true`)

### LMK — Low Memory Killer Tuning
| ADJ Level | Threshold | What gets killed |
|-----------|-----------|-----------------|
| Foreground | 18MB | Almost never killed |
| Visible | 24MB | Only if needed |
| Secondary server | 32MB | Background services |
| Hidden | 40MB | Background apps |
| Content provider | 64MB | Less-used providers |
| Empty | 96MB | Cached processes — first to go |

### CPU Governor — Balanced by Default
- Default: `schedutil` (kernel-driven, best battery/performance balance)
- Changeable in System Monitor (tap CPU card to cycle profile)

### sysctl Tuning
- TCP fastopen, buffer sizing for WiFi throughput
- Scheduler latency reduced for snappier UI on 4-core device
- Kernel panic auto-reboot in 5s

---

## Performance Profiles

Switch in **System Monitor → tap CPU card** or via Command Palette (`>monitor`):

| Profile | Governor | Max Freq | Swappiness | Best For |
|---------|----------|----------|------------|----------|
| **Balanced** (default) | schedutil | 1.4GHz | 60 | Daily use |
| **Performance** | interactive | 1.4GHz | 100 | Coding, AI, heavy tasks |
| **Battery Saver** | conservative | 960MHz | 30 | Long sessions, reading |

---

## Manual Optimizations (Optional)

### Developer Options
1. Settings → About → tap Build Number 7x
2. Set animation scales to **0.5x** (all three)
3. Enable **Force 4x MSAA** — improves GPU render quality
4. Set background process limit to **3 processes**

### Recommended App Stack for ArizenOS
| Category | App | Why |
|----------|-----|-----|
| Browser | **Via Browser** | 10MB, fast on low RAM |
| Terminal | **Termux** | Linux environment on Android |
| Code editor | **Spck Code Editor** | Lightweight IDE |
| Notes | **Markor** | Markdown, offline |
| SSH | **ConnectBot** | Lightweight SSH client |
| File manager | **MiXplorer** | Fast, feature-rich |
| AI dashboard | **Arizen AI** | Built-in — native |

### What to Avoid on 2GB RAM
- Chrome with >5 tabs (use Via or Firefox Focus instead)
- Multiple streaming apps simultaneously
- Heavy games while AI is running
- Large file sync during active use

---

## Termux Integration (Optional)

For a Linux-like coding environment:

```bash
# Install Termux from F-Droid (NOT Play Store)
# In Termux:
pkg update && pkg upgrade
pkg install python nodejs git openssh

# Remote workstation via SSH
ssh user@your-server

# Local code server (lightweight)
pip install httpserver
python -m http.server 8080
```

---

## Battery Optimization

ArizenOS reduces standby drain by:
- Removing Samsung telemetry wake locks
- Stripping Bixby always-listening service
- Removing Samsung Push Service

**Estimated standby improvement:** 15–30% better than stock

**For longer battery life:**
1. Switch to Battery Saver profile (System Monitor)
2. Disable WiFi when not in use via `>wifi off` in Command Palette
3. Reduce screen brightness to 40–60%

---

## After Flash Checklist

- [ ] Verify launcher opens (Arizen home screen visible)
- [ ] Test RAM Boost in Labs or System Monitor
- [ ] Configure AI key: Labs → Arizen → Settings → API Key (Groq is free)
- [ ] Try Command Palette: `> CMD` button on home screen
- [ ] Check System Monitor for baseline RAM usage
- [ ] Set preferred Performance Profile

---

*ArizenOS Lite v1.1 Zenith — Built for SM-T295*
