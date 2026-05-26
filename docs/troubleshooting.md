# ArizenOS Lite — Troubleshooting Guide

---

## Flashing Issues

### Odin shows FAIL
- Ensure "Re-Partition" is OFF in Odin options
- Try a different USB cable/port
- Redownload the AP.tar.md5 file (may be corrupted)
- Try Odin 3.13 if using a newer version

### Device not detected in Odin
1. Install [Samsung USB Drivers](https://developer.samsung.com/mobile/android-usb-driver.html)
2. Try USB 2.0 port instead of USB 3.0
3. Try different cable

### Stuck in boot loop
- Power off: hold Power for 10+ seconds
- Boot into Recovery: Power + Vol Up + Bixby
- Factory reset from recovery
- If still failing, flash stock firmware via Odin

---

## Performance Issues

### Lag or slowness
- Open Arizen Settings → Performance → Apply Optimization
- Disable unused AI features temporarily
- Clear app cache: Settings → Apps → (app) → Clear Cache

### High RAM usage
- ZRAM should handle this automatically
- Reduce number of background apps
- Open Arizen Settings → Performance → Aggressive RAM mode

### Battery draining fast
- Check if AI assistant is running in background unnecessarily
- Open Arizen Settings → AI → Ambient Mode → Conservative
- Disable AI background suggestions

---

## AI Issues

### AI not responding
- Check your internet connection
- Verify API key in Arizen Settings → AI → API Key
- Try a different AI provider
- Check provider status page

### AI responses are slow
- Switch to Groq provider (fastest)
- Use a smaller model (e.g., llama-3.1-8b-instant)
- Disable streaming if on weak connection

### AI tool not working
- Check permissions: Arizen Settings → AI Permissions
- Restart Arizen Core: Settings → Apps → Arizen Core → Force Stop

---

## Restore to Stock Samsung

If you need to completely restore:

1. Download original firmware from [SamFW.com](https://samfw.com)
   - Search: SM-T295
   - Download your region's firmware
2. Open Odin
3. Load ALL partitions (AP, BL, CP, CSC)
4. Flash normally
5. Device returns to 100% stock Samsung
