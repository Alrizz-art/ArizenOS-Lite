# ArizenOS — Brand Guidelines

> Version 1.0 | Designed for ArizenOS Lite

---

## Brand Identity

**Name:** ArizenOS  
**Tagline:** *Intelligence, Elevated.*  
**Sub-brand:** ArizenOS Lite (device-specific)  
**Target:** Samsung Galaxy Tab A 8.0 (SM-T295)

### Brand Personality
- Calm, not cold
- Intelligent, not complex
- Premium, not pretentious
- Modern, not trendy
- Minimal, not empty

### Brand Voice
Write like a brilliant friend who happens to be an AI — clear, warm, precise. Never robotic. Never corporate. Short sentences. Direct. Helpful.

---

## Logo

### Primary Logo
```
 ▀▄  ARIZEN OS
 ▄▀  ──────────
     L I T E
```
The Arizen mark is a soft-corner A formed by two overlapping chevrons — representing intelligence converging. The wordmark uses thin-weight sans-serif.

### Logo Variants

| Variant | Usage |
|---------|-------|
| `arizen-logo-white.svg` | Dark backgrounds (primary) |
| `arizen-logo-dark.svg` | Light backgrounds |
| `arizen-logo-mark.svg` | Icon only (app icon, favicon) |
| `arizen-logo-splash.svg` | Boot animation, splash screen |

### Clear Space
Minimum clear space = height of the letter A in the wordmark. Never crowd the logo.

### Don'ts
- Do not change logo colors
- Do not add glow/shadow effects
- Do not stretch or distort
- Do not use on busy backgrounds without a backing surface

---

## Color System

### Primary Palette
```
─────────────────────────────────────────────────────
  Void Black      #000000   AMOLED true black
  Surface Dark    #0F0F0F   Card / panel backgrounds
  Graphite Deep   #1C1C1E   Secondary surfaces
  Graphite Mid    #2C2C2E   Borders, dividers
  Graphite Light  #3A3A3C   Inactive elements
─────────────────────────────────────────────────────

─────────────────────────────────────────────────────
  Arizen Blue     #4A9EFF   Primary accent, CTAs, AI elements
  Arizen Blue 20% #334A9EFF Glow / ambient fill
  Arizen Blue 10% #1A4A9EFF Subtle highlight
─────────────────────────────────────────────────────

─────────────────────────────────────────────────────
  White Primary   #FFFFFF   Main text
  White 60%       #99FFFFFF Secondary text
  White 30%       #4DFFFFFF Placeholder / hint text
  White 15%       #26FFFFFF Subtle borders
─────────────────────────────────────────────────────
```

### Semantic Colors
```
Success   #34C759   (iOS green — calm success)
Warning   #FF9F0A   (amber — non-alarming warning)
Error     #FF453A   (red — clear but not aggressive)
Info      #4A9EFF   (same as accent)
```

### Color Rules
1. **Always start with black** — #000000 on AMOLED saves battery and looks premium
2. **One accent** — Arizen Blue only. Never use multiple accent colors
3. **Transparency > solid** — prefer 10-30% white overlays over hard colors for surfaces
4. **No neon** — keep the blue cool and muted, never saturate above #4A9EFF

---

## Typography

### Type Scale
```
Display    — 48sp, weight 100 (thin)    — Hero headlines, time display
Headline   — 28sp, weight 300 (light)   — Section headers
Title      — 20sp, weight 400 (regular) — Card titles
Body       — 15sp, weight 300 (light)   — Main content
Label      — 13sp, weight 400 (regular) — Metadata, badges
Caption    — 11sp, weight 400 (regular) — Legal, hints
Mono       — 13sp, monospace            — Code, API keys
```

### Typeface
- **Primary:** `sans-serif-thin` / `sans-serif-light` (system fonts, no custom download)
- **Mono:** `monospace` (for API keys, technical data)
- **Never use:** bold weights for body copy, decorative fonts, or mixed typefaces

### Typography Rules
1. Thin weights (100-300) for large text
2. Regular weight (400) for small text (readability)
3. Track letters in ALL CAPS labels: `letterSpacing="0.15"`
4. 1.5× line height for body text

---

## Iconography

### Style
- Line icons — 1.5dp stroke width
- 24×24dp grid, 2dp padding
- Rounded corners and line caps
- Single color: white or Arizen Blue

### Icon Set Priorities
1. Home / Launcher
2. AI / Assistant
3. Settings (Arizen-specific sections)
4. Status bar icons
5. App shortcuts

---

## Motion Design

### Principles
- **Purposeful** — every animation communicates state change
- **Swift** — durations: 150ms (micro), 250ms (standard), 350ms (emphasis)
- **Natural** — ease-out for elements entering, ease-in for leaving
- **Restrained** — no looping decorative animations in production

### Standard Easing
```
Enter   : cubic-bezier(0.0, 0.0, 0.2, 1)   — decelerate
Exit    : cubic-bezier(0.4, 0.0, 1.0, 1)   — accelerate
Standard: cubic-bezier(0.4, 0.0, 0.2, 1)   — standard
```

### AI Bubble Animation
The Arizen AI bubble should pulse gently (scale 1.0 → 1.06 → 1.0) at 3-second intervals when idle, using a 1200ms ease-in-out cycle. This signals the AI is alive and ready — without being distracting.

---

## UI Patterns

### Cards
- Background: #0F0F0F
- Border: 1dp, 15% white
- Radius: 16dp
- Padding: 20dp
- No shadow (AMOLED — shadows waste pixels)

### Buttons
- Primary: solid Arizen Blue, white text, 12dp radius
- Secondary: transparent, Arizen Blue border + text
- Destructive: #FF453A fill, white text
- Height: 48dp minimum touch target

### Input Fields
- Background: #1A1A1A
- Border: 1dp, 15% white (2dp Arizen Blue when focused)
- Radius: 12dp
- Placeholder: 30% white

### Spacing System
```
4dp   — micro spacing (icon gaps)
8dp   — small (within components)
12dp  — component padding
16dp  — section padding
20dp  — card padding
24dp  — screen margins
32dp  — section separation
48dp  — major section breaks
```

---

## App Icon

### Adaptive Icon Spec
- Foreground: Arizen mark on transparent
- Background: #000000 (pure black)
- Safe zone: 72×72dp within 108×108dp canvas

### Icon States
```
Launcher icon   — 48dp  — adaptive icon
Notification    — 24dp  — white single-color
Status bar      — 16dp  — white single-color
Shortcut        — 48dp  — colored
```

---

## Boot Animation

### Spec
- Resolution: 1280×800 (SM-T295)
- Frame rate: 30fps
- Intro (part0): 60 frames (2 seconds) — plays once
- Loop (part1): 30 frames (1 second) — loops until boot

### Visual
```
Frame 0-20   : Black → Arizen mark fades in (soft blue glow ring)
Frame 20-45  : Wordmark "ArizenOS" fades in below mark
Frame 45-60  : "Lite" fades in, all elements settle
Loop         : Subtle pulse on AI bubble glyph
```

### Rules
- Pure black background always
- No text animations after logo appears
- No jarring cuts
- Sound: single soft chime at frame 20 (optional, respects silent mode)

---

## Photography & Imagery

### Style
- Dark studio photography
- Minimal props, textured surfaces
- Cool-to-neutral color temperature
- Subject separation with soft rim lighting

### Avoid
- Stock photo aesthetics
- Bright colorful backgrounds
- People's faces (privacy-first brand)
- Screenshots of competitor UIs

---

## Voice & Tone

### AI Responses (Arizen personality)
```
✓  "Opening YouTube for you."
✓  "WiFi enabled."
✓  "I found 3 files named 'report'."

✗  "I'm sorry, I'm just an AI but I'll try my best to help you with that!"
✗  "Great question! Let me assist you with..."
✗  "Certainly! I have turned on your WiFi connection successfully."
```

### UI Copy
```
✓  "Tap to ask Arizen"
✓  "AI key not set"
✓  "Connecting…"

✗  "Please tap here to initiate AI assistant"
✗  "Your API authentication key has not been configured"
✗  "Please wait while we establish connection"
```

---

## What ArizenOS Is NOT

| ArizenOS IS | ArizenOS is NOT |
|-------------|-----------------|
| Calm & intelligent | Cold & robotic |
| Premium | Flashy / showy |
| Minimal | Empty / boring |
| Fast & smooth | Feature-bloated |
| AMOLED dark | Pure white / grey |
| Subtle glow | Neon / RGB |
| Ambient AI | Intrusive assistant |
| Stock Samsung base | Experimental ROM |

---

*ArizenOS Brand Guidelines v1.0 — Alrizz-art / ArizenOS Project*
