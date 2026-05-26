# ArizenOS Lite — AI Setup Guide

## Setting Up Arizen Core

---

## Step 1: Open Arizen Settings

1. From the home screen, tap the **Arizen** icon (or swipe up for app drawer)
2. Open **Arizen Settings**
3. Go to **AI** section

---

## Step 2: Choose Your AI Provider

Arizen Core supports multiple cloud AI providers:

| Provider | Best For | Free Tier |
|----------|----------|-----------|
| **Groq** | Fastest responses, llama models | ✅ Yes |
| **Together AI** | Wide model selection | ✅ Yes |
| **OpenAI Compatible** | GPT models | Paid |
| **Cloudflare AI** | Privacy-focused | ✅ Yes |
| **Ollama (Remote)** | Self-hosted | ✅ Yes |

---

## Step 3: Get Your API Key

### Groq (Recommended — Fast & Free)
1. Go to [console.groq.com](https://console.groq.com)
2. Sign up / log in
3. Go to **API Keys** → **Create API Key**
4. Copy the key

### Together AI
1. Go to [api.together.ai](https://api.together.ai)
2. Sign up → Dashboard → API Keys
3. Copy the key

---

## Step 4: Enter API Key in Arizen Settings

1. Open **Arizen Settings → AI → API Key**
2. Paste your API key
3. Select your **Provider** and **Model**
4. Tap **Save & Test**

---

## Step 5: Using Arizen AI

### Ambient Assistant Bubble
- Tap the floating Arizen bubble on the home screen
- Speak or type your request
- Arizen responds with action or answer

### AI Tools
Arizen can control your device:
- "Open YouTube" → AppLauncherTool
- "Turn on WiFi" → WifiTool  
- "Search for files named report" → FileSearchTool
- "Set brightness to 50%" → SettingsTool
- "Remind me to drink water every hour" → AutomationTool

---

## Privacy & Security

- Your API key is stored encrypted on-device
- Conversations are NOT stored by ArizenOS
- All AI processing happens via your chosen cloud provider
- You control which tools Arizen can use (Arizen Settings → AI Permissions)
