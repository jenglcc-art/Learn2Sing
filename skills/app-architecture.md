# Skill: Learn2Sing App Architecture

## Overview

Learn2Sing is an Android app that streams music from a personal Plex Media Server, with speed/pitch control and synced lyrics for singing practice.

## Activity Flow

```
PlexAuthActivity (launcher)
    │
    ├── Already authenticated? → skip to MainActivity
    │
    └── PIN OAuth flow
            ↓
        plex.tv (browser sign-in)
            ↓
        Poll for token
            ↓
        Discover server URI + music section ID
            ↓
        MainActivity
                ↓
            Search Plex library
                ↓
            Select a track
                ↓
            PlayerActivity
```

## Key Classes

| Class | Responsibility |
|-------|---------------|
| `PlexAuthActivity` | PIN OAuth flow, server discovery, stores session |
| `PlexSession` | Singleton holding auth token + server URI + section ID |
| `MainActivity` | Search UI, calls Plex API, launches player |
| `PlayerActivity` | ExoPlayer streaming, speed/pitch, LRCLib lyrics |
| `PlexRetrofitClient` | Retrofit factory with trust-all SSL + auth interceptor |
| `PlexGlideModule` | Glide custom HTTP module for trust-all image loading |

## Data Models

| Class | Source |
|-------|--------|
| `VideoItem` | Internal — wraps Plex track info + streamUrl |
| `PlexResourcesResponse` | GET plex.tv/api/v2/resources |
| `PlexSectionsResponse` | GET {server}/library/sections |
| `PlexSearchResponse` | GET {server}/library/sections/{id}/all?type=10 |

## Three Independent HTTP Stacks

Android does not have a single HTTP layer. Each library manages its own:

```
┌─────────────────────────────────┐
│  Retrofit (API calls)           │  ← PlexRetrofitClient (trust-all OkHttp)
├─────────────────────────────────┤
│  ExoPlayer (audio streaming)    │  ← OkHttpDataSource.Factory (trust-all OkHttp)
├─────────────────────────────────┤
│  Glide (image loading)          │  ← PlexGlideModule (trust-all OkHttp)
└─────────────────────────────────┘
```

All three must be configured with the same trust-all OkHttp client when connecting to a local Plex server.

## Session Data (SharedPreferences)

Stored keys (see `Constants.java`):

| Key | Value |
|-----|-------|
| `PREF_PLEX_TOKEN` | Plex auth token |
| `PREF_PLEX_SERVER_URI` | Selected server base URL (always HTTPS) |
| `PREF_PLEX_SECTION_ID` | Music library section key (e.g. "1") |
| `PREF_PLEX_CLIENT_ID` | Permanent UUID for this install |

## Intent Extras

`MainActivity → PlayerActivity`:

| Extra | Type | Description |
|-------|------|-------------|
| `EXTRA_TITLE` | String | Track title |
| `EXTRA_ARTIST` | String | Artist name |
| `EXTRA_THUMB_URL` | String | Album art URL (full, with token) |
| `EXTRA_STREAM_URL` | String | Audio stream URL (full, with token) |
| `EXTRA_DURATION` | long | Duration in milliseconds |

## URL Construction

Always build full URLs with token in constructor or before passing to intents:

```java
// Audio stream URL
String streamUrl = serverUri + partKey + "?X-Plex-Token=" + authToken;

// Album art URL
String thumbUrl = serverUri + thumbPath + "?X-Plex-Token=" + authToken;
```

Never pass relative paths (`/library/parts/123/...`) between activities — build the full URL early.

## Key Design Decisions & Lessons Learned

### 1. Always use HTTPS for Plex
Plex's "Require HTTPS" setting silently drops HTTP connections (returns 0 bytes → EOFException). Always upgrade `http://` to `https://`:
```java
if (uri.startsWith("http://")) return "https://" + uri.substring(7);
```
Apply this in both `PlexSession.restore()` and `PlexAuthActivity.chooseBestServerUri()`.

### 2. Plex API v2 returns camelCase JSON
The auth token field is `authToken` (camelCase), NOT `auth_token`. Do NOT add `@SerializedName("auth_token")` — Gson maps camelCase by default.

### 3. Token as both header and query parameter
Some Plex server versions only check the URL query param, not the header. Send both:
```java
urlBuilder.addQueryParameter("X-Plex-Token", authToken);
req.header("X-Plex-Token", authToken);
```

### 4. Use `/all?title=` not `/search?query=` for music search
The `/all?title=` endpoint is more reliable across Plex versions:
```
GET /library/sections/{sectionId}/all?type=10&title={query}
```

### 5. Skip auth flow if already authenticated
Check `session.isAuthenticated() && session.hasServer()` at `onCreate` to avoid re-running the PIN flow every launch.

### 6. Each HTTP library needs trust-all configured independently
Configuring trust-all for Retrofit does NOT affect ExoPlayer or Glide. Configure each separately using the shared `PlexRetrofitClient.buildTrustAllClient()` method.

## Build Dependencies Summary

```groovy
// Retrofit + Gson
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

// ExoPlayer
implementation 'androidx.media3:media3-exoplayer:1.3.1'
implementation 'androidx.media3:media3-ui:1.3.1'
implementation 'androidx.media3:media3-datasource-okhttp:1.3.1'  // trust-all SSL

// Glide
implementation 'com.github.bumptech.glide:glide:4.16.0'
implementation 'com.github.bumptech.glide:okhttp3-integration:4.16.0'  // trust-all SSL
annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
```

## AndroidManifest Requirements

```xml
<!-- Network access -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Allow plain HTTP fallback (for initial connection attempts) -->
<application android:usesCleartextTraffic="true" ...>

<!-- PlexAuthActivity as launcher -->
<activity android:name=".PlexAuthActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```
