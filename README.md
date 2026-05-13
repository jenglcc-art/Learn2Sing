# Learn2Sing — Android App

An Android app for learning to sing along with your personal music library. Connect to your **Plex Media Server**, search any song, control playback speed and pitch independently, and follow along with auto-scrolling synced lyrics.

---

## Features

| Feature | Details |
|---|---|
| **Plex integration** | Streams directly from your Plex Media Server — no third-party service required |
| **Music search** | Searches your Plex music library by song title or artist |
| **Speed control** | Continuous slider from 0.25× to 2.0× |
| **Pitch control** | Independent pitch slider — slow down without changing key, or transpose on the fly |
| **Synced lyrics** | Auto-scrolling lyrics via [LRCLib](https://lrclib.net) (free, no key needed) |
| **Album art** | Cover art loaded from your Plex library |

---

## Requirements

- **Android Studio** Hedgehog (2023.1) or newer
- **Android SDK** API 26+ (Android 8.0)
- A running **Plex Media Server** with a music library
- A **Plex account** (free) to authenticate

---

## Getting Started

### 1. Build & Run

```bash
# In the project root
./gradlew assembleDebug

# Or open the project in Android Studio and click Run ▶
```

> **Note:** The first build may take a few minutes to download Gradle dependencies.

### 2. Sign In to Plex

On first launch the app opens a PIN sign-in screen. Tap **Open Browser**, sign in to your Plex account, then return to the app — it will detect the login automatically and connect to your server.

Your session is saved; you won't need to sign in again unless you clear app data.

### 3. Search & Play

1. **Search** — Type a song name or artist and tap Search.
2. **Pick a song** — Tap any result to open the player.
3. **Adjust speed** — Drag the speed slider (0.25×–2.0×).
4. **Adjust pitch** — Drag the pitch slider independently to change key without affecting tempo.
5. **Lyrics** — The current line is highlighted and the list auto-scrolls. Tap any line to jump there.

---

## How Authentication Works

Learn2Sing uses Plex's PIN-based OAuth flow:

1. The app requests a PIN from `plex.tv`
2. You sign in via the browser
3. The app polls `plex.tv` until it detects your approval
4. Your auth token and server details are saved locally — no credentials ever leave your device

---

## Project Structure

```
app/src/main/
└── java/com/learn2sing/app/
    ├── Constants.java              ← Shared config & preference keys
    ├── PlexAuthActivity.java       ← PIN OAuth flow & server discovery
    ├── PlexSession.java            ← Singleton holding token + server URI
    ├── MainActivity.java           ← Search screen
    ├── PlayerActivity.java         ← Player + speed/pitch + lyrics
    ├── SearchAdapter.java          ← RecyclerView adapter for search results
    ├── LyricsAdapter.java          ← RecyclerView adapter for lyrics
    ├── VideoItem.java              ← Track model (title, artist, stream URL…)
    ├── LyricsLine.java             ← Single lyric line model
    ├── PlexGlideModule.java        ← Glide custom HTTP module (trust-all SSL)
    └── api/
        ├── PlexRetrofitClient.java      ← Retrofit + trust-all OkHttp client
        ├── PlexTvApiService.java        ← plex.tv PIN auth endpoints
        ├── PlexServerApiService.java    ← Plex server search + sections
        ├── PlexResourcesResponse.java   ← Server list model
        ├── PlexSectionsResponse.java    ← Library sections model
        ├── PlexSearchResponse.java      ← Track search results model
        ├── LyricsApiService.java        ← LRCLib lyrics API
        └── LyricsResponse.java
```

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| AndroidX AppCompat | 1.6.1 | Activity base classes |
| Material Components | 1.11.0 | UI components |
| ConstraintLayout | 2.1.4 | Responsive layouts |
| Retrofit 2 | 2.9.0 | HTTP client for Plex & LRCLib APIs |
| OkHttp | 4.12.0 | HTTP + logging interceptor |
| Gson | bundled | JSON parsing |
| Glide | 4.16.0 | Album art image loading |
| Glide OkHttp integration | 4.16.0 | Trust-all SSL for Glide |
| ExoPlayer (media3) | 1.3.1 | Audio playback with speed/pitch control |
| ExoPlayer OkHttp datasource | 1.3.1 | Trust-all SSL for ExoPlayer |

---

## API Attribution

- **LRCLib** — Free, open synced-lyrics API at [lrclib.net](https://lrclib.net)
- **Plex API** — Streams from your own Plex Media Server; requires a Plex account at [plex.tv](https://www.plex.tv)

---

## License

MIT — feel free to build on this for personal or educational use.
