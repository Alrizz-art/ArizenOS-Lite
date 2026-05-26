# ArizenOS Lite — Optimization Guide

## Getting the Best Performance on SM-T295 (2GB RAM)

---

## What ArizenOS Does Automatically

### ZRAM (Active by default)
- 512MB compressed swap space
- lz4 compression for speed
- Effectively gives you ~2.5GB usable memory
- Applied at boot via `/system/etc/init.d/99arizen_zram`

### Debloat
- ~20-30 Samsung system apps removed
- Frees ~200-400MB of storage
- Reduces background processes significantly

### build.prop Tweaks
- Optimized GC settings for low-RAM
- Better background process limits
- Purgeable asset handling

---

## Manual Optimizations

### Developer Options Tweaks
1. Enable Developer Options (Settings → About → tap Build Number 7x)
2. Set animation scales to 0.5x:
   - Window animation scale → 0.5x
   - Transition animation scale → 0.5x
   - Animator duration scale → 0.5x

### Recommended Apps
- Use **Arizen Launcher** instead of stock launcher
- Replace heavy apps with lightweight alternatives:
  - Browser: Bromite or Via
  - Maps: OsmAnd (offline)
  - Email: K-9 Mail

### What to Avoid on 2GB RAM
- Chrome with many tabs
- Heavy games simultaneously
- Multiple video streaming apps
- Large file sync operations while using AI
