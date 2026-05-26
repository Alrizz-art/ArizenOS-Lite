# Arizen Settings

The ArizenOS Settings app provides configuration UI for:

## Sections

### AI
- Provider selection (Groq, Together AI, OpenAI, Cloudflare, Ollama)
- API Key input (encrypted storage)
- Model selection
- System prompt customization
- Ambient mode toggle

### Appearance
- AMOLED dark mode (always on)
- Accent color selection
- Font size
- Icon pack

### Dashboard
- Widget configuration
- Smart widget layout
- AI shortcut cards

### Performance
- ZRAM status
- Animation speed
- Background app limit
- RAM usage monitor

### Privacy
- AI permissions per tool
- Conversation history (disabled by default)
- Data sharing settings

### Automation
- Scheduled AI tasks
- Trigger-based automations
- Routine configuration

### Labs
- Experimental features
- Beta AI models
- Debug mode

## Building

Build as part of the main launcher project in Android Studio.
The settings APK is installed to `/system/priv-app/ArizenSettings/`.
