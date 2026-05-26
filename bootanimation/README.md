# ArizenOS Lite — Boot Animation

## Creating the Boot Animation

The boot animation is a `bootanimation.zip` file placed at `/system/media/bootanimation.zip`.

## Format

```
bootanimation.zip (uncompressed)
├── desc.txt
├── part0/       ← intro (plays once)
│   ├── 0000.png
│   ├── 0001.png
│   └── ...
└── part1/       ← loop (plays until boot complete)
    ├── 0000.png
    └── ...
```

## desc.txt Format

```
<width> <height> <fps>
p <play_count> <pause_ms> part0
p 0 0 part1
```

## ArizenOS Lite Spec

```
1280 800 30
p 1 0 part0
p 0 0 part1
```

- Resolution: 1280×800 (SM-T295 display)
- FPS: 30 (smooth, not battery-hungry)
- Intro: plays once (ArizenOS Lite wordmark fade-in)
- Loop: minimal pulse animation until boot complete

## Design Direction

- Pure black background (#000000)
- ArizenOS Lite wordmark in thin white typography
- Subtle soft blue pulse/glow around the logo
- No aggressive animations — calm, cinematic feel
- Inspired by: Nothing OS startup, visionOS boot

## Tools to Generate

Use GIMP, Figma export, or Python Pillow to generate PNG frames:

```python
# Example: simple fade-in with Python Pillow
from PIL import Image, ImageDraw, ImageFont
import os

frames = 30
for i in range(frames):
    img = Image.new('RGBA', (1280, 800), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)
    alpha = int(255 * (i / frames))
    draw.text((640, 400), "ArizenOS Lite", fill=(255, 255, 255, alpha), anchor="mm")
    img.save(f"part0/{i:04d}.png")
```

## Packaging

```bash
cd bootanimation_frames/
zip -0 -r bootanimation.zip desc.txt part0/ part1/
# Move to arizen-assets/
mv bootanimation.zip ../arizen-assets/
```
