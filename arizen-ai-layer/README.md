# Arizen AI Layer

This directory contains the Arizen Core AI system components.

## Architecture

```
Arizen Core
├── ArizenCore.apk          ← Main AI service APK
├── ArizenAssistant         ← Floating UI overlay
├── ArizenToolManager       ← Tool routing engine
└── ArizenSettings.apk      ← AI configuration UI
```

## Building ArizenCore.apk

1. Open `arizen-launcher/` in Android Studio
2. Build → Generate Signed APK
3. Copy to `arizen-assets/ArizenCore.apk`

## AI Provider Integration

Arizen Core connects to cloud AI providers via standard OpenAI-compatible REST API:

```
POST {base_url}/chat/completions
Authorization: Bearer {api_key}
Content-Type: application/json

{
  "model": "llama-3.1-8b-instant",
  "messages": [...],
  "stream": false,
  "max_tokens": 512
}
```

## Tool System

Tools are declared in `ArizenToolManager.java`. To add a new tool:
1. Create `YourTool.java` implementing `ArizenTool`
2. Register in `ArizenToolManager` constructor
3. Add description to system prompt

## Privacy

- API keys encrypted with AES on-device
- No conversation logs stored
- No telemetry sent by Arizen
- All data stays between device and chosen AI provider
