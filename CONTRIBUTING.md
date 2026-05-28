# Contributing to ArizenOS Lite

Thank you for your interest in contributing!

---

## Ways to Contribute

### Bug Report
Open an [Issue](../../issues/new) with:
- Device model + Android version
- Steps to reproduce
- Expected vs actual behavior
- Logcat or error output if available

### Feature Request
Open an Issue with the `enhancement` label. Describe the use case clearly.

### Submit a Fix

1. Fork the repo on GitHub
2. Create a branch from main for your fix
3. Make your changes and test locally (see README Local Build section)
4. Open a Pull Request — describe what changed and why

---

## Adding Packages to Debloat List

Edit [`config/debloat_list.txt`](config/debloat_list.txt):

```
# Format: exact folder name in /system/app or /system/priv-app
PackageFolderName
```

Rules:
- Only add packages safe to remove (no system-critical services)
- Never add: `SystemUI`, `TelephonyUI`, `SecSettings`, `DownloadProvider`, `MediaProvider`, `SamsungIME`

---

## Adding Device Support

Currently only SM-T295 is supported. To add another Samsung device:

1. Verify device uses **ext4** system partition (not erofs)
2. Create `config/device_SM-XXXX.sh` with device-specific variables
3. Update `config/build_config.sh` to load the device config
4. Test the full pipeline end-to-end
5. Open a PR with build logs attached

---

## Script Guidelines

All shell scripts must:
- Start with `#!/usr/bin/env bash`
- Use `set -euo pipefail`
- Use colored logging via `log()` / `ok()` / `warn()` / `fail()` helpers
- Be idempotent where possible
- Handle both sparse and raw ext4 images

---

## Java Code Style (Arizen Launcher)

- Target API 28 (Android 9) minimum
- No external libraries beyond `androidx.appcompat`
- Prefer `Activity` over `AppCompatActivity` for minimal footprint
- All AI calls go through `ArizenAIBridge`

---

## Questions?

Open a [Discussion](../../discussions) or an Issue tagged `question`.
