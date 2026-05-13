package com.learn2sing.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Singleton that holds the authenticated Plex state:
 *  - clientId   : permanent UUID identifying this app install
 *  - authToken  : Plex user token (from PIN OAuth)
 *  - serverUri  : base URL of the chosen Plex server (local or relay)
 *  - sectionId  : Plex music library section ID
 *
 * All values are persisted to SharedPreferences so they survive restarts.
 */
public class PlexSession {

    private static PlexSession instance;

    private String clientId;
    private String authToken;
    private String serverUri;         // active URI (local when on home WiFi)
    private String serverUriRemote;   // best remote/relay URI saved at discovery time
    private String sectionId;

    private PlexSession() {}

    public static synchronized PlexSession getInstance() {
        if (instance == null) instance = new PlexSession();
        return instance;
    }

    // ── Initialise from SharedPreferences on app start ────────────────────────

    public void restore(Context ctx) {
        SharedPreferences prefs = prefs(ctx);
        authToken = prefs.getString(Constants.PREF_PLEX_TOKEN,      null);
        sectionId = prefs.getString(Constants.PREF_PLEX_SECTION_ID, null);

        // Always use HTTPS — Plex ignores plain HTTP when "Require HTTPS" is on.
        // Our OkHttp client trusts all certs, so HTTPS works for any IP or hostname.
        String saved = prefs.getString(Constants.PREF_PLEX_SERVER_URI, null);
        serverUri = toHttps(saved);

        String savedRemote = prefs.getString(Constants.PREF_PLEX_SERVER_URI_REMOTE, null);
        serverUriRemote = toHttps(savedRemote);

        // clientId is permanent — create once, keep forever
        clientId = prefs.getString(Constants.PREF_PLEX_CLIENT_ID, null);
        if (clientId == null) {
            clientId = UUID.randomUUID().toString();
            prefs.edit().putString(Constants.PREF_PLEX_CLIENT_ID, clientId).apply();
        }
    }

    // ── Setters (also persist) ────────────────────────────────────────────────

    public void saveToken(Context ctx, String token) {
        authToken = token;
        prefs(ctx).edit().putString(Constants.PREF_PLEX_TOKEN, token).apply();
    }

    public void saveServer(Context ctx, String uri) {
        serverUri = uri;
        prefs(ctx).edit().putString(Constants.PREF_PLEX_SERVER_URI, uri).apply();
    }

    public void saveRemoteServer(Context ctx, String uri) {
        serverUriRemote = uri;
        prefs(ctx).edit().putString(Constants.PREF_PLEX_SERVER_URI_REMOTE, uri).apply();
    }

    /**
     * Called when the local server URI is unreachable and the remote one works.
     * Updates the active serverUri for the rest of this session only — the local
     * URI is kept in prefs so we use it again the next time we're on home WiFi.
     */
    public void switchToRemoteServer(String remoteUri) {
        serverUri = remoteUri;
    }

    public void saveSection(Context ctx, String id) {
        sectionId = id;
        prefs(ctx).edit().putString(Constants.PREF_PLEX_SECTION_ID, id).apply();
    }

    public void signOut(Context ctx) {
        authToken = null;
        serverUri = null;
        serverUriRemote = null;
        sectionId = null;
        prefs(ctx).edit()
                .remove(Constants.PREF_PLEX_TOKEN)
                .remove(Constants.PREF_PLEX_SERVER_URI)
                .remove(Constants.PREF_PLEX_SERVER_URI_REMOTE)
                .remove(Constants.PREF_PLEX_SECTION_ID)
                .apply();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isAuthenticated() { return authToken != null && !authToken.isEmpty(); }
    public boolean hasServer()       { return serverUri != null && !serverUri.isEmpty(); }

    public String getClientId()       { return clientId; }
    public String getAuthToken()      { return authToken; }
    public String getServerUri()      { return serverUri; }
    public String getServerUriRemote(){ return serverUriRemote; }
    public String getSectionId()      { return sectionId; }

    /** Convenience: append token as query param — standard Plex auth method. */
    public String tokenParam() {
        return "?X-Plex-Token=" + authToken;
    }

    /** Full stream URL for a Plex part key. */
    public String streamUrl(String partKey) {
        return serverUri + partKey + tokenParam();
    }

    /** Full thumbnail URL for a Plex thumb path. */
    public String thumbUrl(String thumbPath) {
        if (thumbPath == null || thumbPath.isEmpty()) return null;
        return serverUri + thumbPath + tokenParam();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Upgrade http:// → https:// so Plex's "Require HTTPS" setting is satisfied. */
    private static String toHttps(String uri) {
        if (uri == null) return null;
        if (uri.startsWith("http://")) return "https://" + uri.substring(7);
        return uri;
    }

    private SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }
}
