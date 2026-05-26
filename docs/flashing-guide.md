# ArizenOS Lite — Flashing Guide

## Device: Samsung Galaxy Tab A 8.0 (2019) SM-T295

---

## ⚠️ Before You Start

- **Backup all your data** — flashing will wipe the device
- Keep the stock firmware zip somewhere safe (for recovery)
- Use a stable USB cable and PC connection
- Charge your device to at least 50%
- This guide assumes Windows PC with Odin

---

## Step 1: Enable Download Mode

1. Power off your SM-T295
2. Hold **Power + Volume Down + Bixby** simultaneously
3. Press **Volume Up** when prompted to enter Download Mode
4. You should see a blue/green screen with a robot icon

---

## Step 2: Open Odin

1. Download and open **Odin3** (version 3.13 or newer recommended)
2. Connect your device via USB
3. Odin should show your device in the **ID:COM** box (e.g. `0:[COM5]`)
4. If not detected: install Samsung USB drivers and retry

---

## Step 3: Load ArizenOS Lite

1. In Odin, click **AP**
2. Select `ArizenOSLite_v1_SM-T295_AP.tar.md5` from the `output/` folder
3. Leave BL, CP, CSC empty (we only flash AP)

**Odin Settings:**
- ✅ Auto Reboot — ON
- ✅ F. Reset Time — ON  
- ❌ Re-Partition — **OFF** (important!)

---

## Step 4: Flash

1. Click **Start** in Odin
2. Wait for the progress bar to complete (~5-10 minutes)
3. Odin shows **PASS!** in green when done
4. Device reboots automatically into ArizenOS Lite

---

## Step 5: First Boot

- First boot takes 2-5 minutes (normal)
- Complete the Android setup wizard
- Arizen Launcher will be set as the default home

---

## Recovery: Returning to Stock Samsung

If anything goes wrong, flash the original Samsung firmware:
1. Download stock firmware from [SamFW](https://samfw.com) for SM-T295
2. Open Odin and load all partitions (AP, BL, CP, CSC)
3. Flash normally

---

## Common Issues

| Problem | Solution |
|---------|----------|
| FAIL in Odin | Re-download the AP file, retry |
| Stuck on boot | Odin flash stock firmware to recover |
| Device not detected | Install Samsung USB drivers |
| Odin shows wrong port | Try different USB port/cable |
