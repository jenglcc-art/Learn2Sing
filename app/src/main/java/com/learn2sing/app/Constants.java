package com.learn2sing.app;

/**
 * App-wide constants.
 *
 * ── OAuth setup (required) ──────────────────────────────────────────────────
 *  1. Go to https://console.cloud.google.com/
 *  2. Select your project → APIs & Services → Credentials
 *  3. Create Credentials → OAuth 2.0 Client ID
 *     • Application type: "Web application"  ← this gives the Web Client ID
 *       (needed even for an Android app — it identifies the backend)
 *     • Also create an "Android" type client ID using your app's SHA-1
 *  4. Enable "YouTube Data API v3" in the APIs Library
 *  5. Paste the WEB client ID below (looks like "....apps.googleusercontent.com")
 *
 *  The app then uses the signed-in user's OAuth token for all API calls;
 *  no separate API key is required.
 * ───────────────────────────────────────────────────────────────────────────
 */
public final class Constants {

    // ── Google OAuth 2.0 ─────────────────────────────────────────────────────
    /**
     * Web Client ID from Google Cloud Console.
     * Format: "xxxxxxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com"
     */
    public static final String OAUTH_WEB_CLIENT_ID = "323105123977-nvt3hn6g38iov1advu9t8mo3c34hjmar.apps.googleusercontent.com";

    /** YouTube Data API v3 scope — grants read/write access to the user's YouTube account. */
    public static final String YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube";

    // ── YouTube Data API v3 ───────────────────────────────────────────────────
    public static final String YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3/";
    public static final int YOUTUBE_MAX_RESULTS = 15;

    // ── LRCLib (free, no key required) ───────────────────────────────────────
    public static final String LRCLIB_BASE_URL = "https://lrclib.net/";

    // ── Playback speed presets ────────────────────────────────────────────────
    public static final float[] SPEED_PRESETS = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    public static final float DEFAULT_SPEED = 1.0f;

    // ── Intent extras ────────────────────────────────────────────────────────
    public static final String EXTRA_VIDEO_ID      = "extra_video_id";
    public static final String EXTRA_VIDEO_TITLE   = "extra_video_title";
    public static final String EXTRA_CHANNEL_NAME  = "extra_channel_name";
    public static final String EXTRA_THUMBNAIL     = "extra_thumbnail";
    public static final String EXTRA_VIDEO_ID_LIST = "extra_video_id_list";
    public static final String EXTRA_VIDEO_INDEX   = "extra_video_index";

    // ── Plex ─────────────────────────────────────────────────────────────────
    public static final String PLEX_TV_BASE_URL     = "https://plex.tv/";
    public static final String PLEX_PRODUCT         = "Learn2Sing";
    public static final String PLEX_VERSION         = "1.0";
    public static final String PLEX_PLATFORM        = "Android";

    // ── SharedPreferences keys ────────────────────────────────────────────────
    public static final String PREFS_NAME           = "learn2sing_prefs";
    public static final String PREF_LIKED_VIDEOS    = "liked_video_ids";
    public static final String PREF_PLEX_TOKEN      = "plex_token";
    public static final String PREF_PLEX_CLIENT_ID  = "plex_client_id";
    public static final String PREF_PLEX_SERVER_URI = "plex_server_uri";
    public static final String PREF_PLEX_SECTION_ID = "plex_section_id";

    // ── Intent extras (Plex) ─────────────────────────────────────────────────
    public static final String EXTRA_STREAM_URL    = "extra_stream_url";

    private Constants() {}
}
