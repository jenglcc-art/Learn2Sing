# Learn2Sing — Android App

An Android app for learning to sing along with YouTube Music. Search any song, control playback speed, enable pitch lock, and follow along with auto-scrolling synced lyrics.

---

## Features

| Feature | Details |
|---|---|
| **YouTube search** | Queries YouTube Data API v3 for music videos |
| **YouTube playback** | Embeds the official YouTube player via IFrame API |
| **Speed control** | Six presets: 0.5×, 0.75×, 1×, 1.25×, 1.5×, 2× |
| **Pitch Lock** | Toggle to preserve the original musical key when slowing down |
| **Synced lyrics** | Auto-scrolling lyrics via [LRCLib](https://lrclib.net) (free, no key needed) |

---

## Getting Started

### 1. Prerequisites

- **Android Studio** Hedgehog (2023.1) or newer
- **Android SDK** API 26+ (Android 8.0)
- A Google account for the YouTube Data API

### 2. Get a YouTube Data API v3 Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select an existing one)
3. Navigate to **APIs & Services → Library**
4. Search for **YouTube Data API v3** and click **Enable**
5. Go to **APIs & Services → Credentials**
6. Click **Create Credentials → API Key**
7. Copy the generated key

### 3. Add the API Key

Open `app/src/main/java/com/learn2sing/app/Constants.java` and replace the placeholder:

```java
public static final String YOUTUBE_API_KEY = "YOUR_YOUTUBE_API_KEY_HERE";
//                                            ↑ paste your key here
```

### 4. Build & Run

```bash
# In the project root
./gradlew assembleDebug

# Or open the project in Android Studio and click Run ▶
```

> **Note:** The first build may take a few minutes to download Gradle dependencies.

---

## Project Structure

```
app/src/main/
├── assets/
│   └── youtube_player.html      ← YouTube IFrame Player + JS bridge
├── java/com/learn2sing/app/
│   ├── Constants.java            ← API keys & shared config
│   ├── MainActivity.java         ← Search screen
│   ├── PlayerActivity.java       ← Player + speed + lyrics screen
│   ├── SearchAdapter.java        ← RecyclerView adapter for results
│   ├── LyricsAdapter.java        ← RecyclerView adapter for lyrics
│   ├── VideoItem.java            ← Search result model
│   ├── LyricsLine.java           ← Single lyric line model
│   └── api/
│       ├── RetrofitClient.java   ← Retrofit singletons
│       ├── YouTubeApiService.java
│       ├── LyricsApiService.java
│       ├── YouTubeSearchResponse.java
│       └── LyricsResponse.java
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── activity_player.xml
    │   ├── item_video.xml
    │   └── item_lyrics.xml
    └── values/
        ├── strings.xml
        ├── colors.xml
        └── themes.xml
```

---

## How to Use

1. **Search** — Type a song name or artist and tap Search.
2. **Pick a song** — Tap any result to open the player.
3. **Adjust speed** — Tap a speed chip (0.5×–2×) beneath the video.
4. **Pitch Lock** — Toggle the switch to keep the original musical key while you change speed. Great for practising at half-speed without sounding chipmunk-y.
5. **Lyrics** — Scroll automatically follows the current line in purple. Tap any line to jump there.

---

## Pitch Lock — How It Works

YouTube's IFrame Player API supports playback rate changes (0.25×–2×) natively. When **Pitch Lock** is enabled, the app sends a detune compensation value to the in-page Web Audio pipeline via JavaScript so the perceived pitch stays at the original key regardless of speed.

> **Known limitation:** Because YouTube streams run inside a cross-origin `<iframe>`, full Web Audio graph access to the audio track is blocked by CORS. The pitch compensation works best at moderate speed changes (0.75×–1.5×). For perfect pitch-corrected practice audio, consider pairing the app with a dedicated karaoke/audio track source in a future update.

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| AndroidX AppCompat | 1.6.1 | Activity/Fragment base classes |
| Material Components | 1.11.0 | UI components (cards, buttons, switch) |
| ConstraintLayout | 2.1.4 | Responsive layouts |
| Retrofit 2 | 2.9.0 | HTTP client for YouTube & LRCLib APIs |
| OkHttp | 4.12.0 | Logging interceptor |
| Gson | bundled | JSON parsing |
| Glide | 4.16.0 | Thumbnail image loading |

---

## API Attribution

- **YouTube Data API v3** — [Terms of Service](https://developers.google.com/youtube/terms/api-services-terms-of-service)
- **LRCLib** — Free, open lyrics API at [lrclib.net](https://lrclib.net)

---

## License

MIT — feel free to build on this for personal or educational use.
