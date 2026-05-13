package com.learn2sing.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.learn2sing.app.api.PlexRetrofitClient;
import com.learn2sing.app.api.PlexSearchResponse;
import com.learn2sing.app.api.PlexServerApiService;

import okhttp3.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main screen: search the Plex music library.
 *
 * Flow:
 *   1. PlexSession must be authenticated (PlexAuthActivity handles this).
 *   2. User types a song/artist name → searches Plex library sections.
 *   3. Tapping a result opens PlayerActivity with the Plex stream URL.
 *   4. Optional: tap the YouTube icon to sign in with Google and search
 *      YouTube (for song discovery / karaoke versions).
 */
public class MainActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private EditText      etSearch;
    private ProgressBar   progressBar;
    private TextView      tvEmpty;
    private RecyclerView  rvResults;
    private SearchAdapter adapter;

    // Header views
    private TextView  tvServerName;
    private ImageView btnSignOut;

    private static final String TAG = "MainActivity";

    // ── State ─────────────────────────────────────────────────────────────────
    private PlexSession          plexSession;
    private PlexServerApiService serverApi;

    /**
     * False while testConnection() / tryRemoteFallback() is still in progress.
     * Searches are blocked until a working server URI is confirmed, preventing
     * the race condition where a search fires against the local IP before the
     * remote fallback has had a chance to switch serverApi.
     */
    private boolean connectionReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Auth gate — if Plex session lost, back to auth ────────────────────
        plexSession = PlexSession.getInstance();
        plexSession.restore(this);
        if (!plexSession.isAuthenticated()) {
            startActivity(new Intent(this, PlexAuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        bindViews();
        buildServerApi();
        populateHeader();
        setupSearch();
        testConnection();
    }

    // ── Wiring ────────────────────────────────────────────────────────────────

    private void bindViews() {
        etSearch     = findViewById(R.id.et_search);
        progressBar  = findViewById(R.id.progress_bar);
        tvEmpty      = findViewById(R.id.tv_empty);
        rvResults    = findViewById(R.id.rv_results);
        tvServerName = findViewById(R.id.tv_user_name);
        btnSignOut   = findViewById(R.id.btn_sign_out);

        adapter = new SearchAdapter(this::openPlayer);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);
    }

    private void buildServerApi() {
        serverApi = PlexRetrofitClient.getServerService(
                plexSession.getServerUri(),
                plexSession.getClientId(),
                plexSession.getAuthToken());
    }

    /**
     * Fires a lightweight /identity call to verify connectivity.
     * Blocks search until a working server is confirmed.
     * If the local URI fails, automatically retries with the remote fallback.
     */
    private void testConnection() {
        connectionReady = false;
        setSearchEnabled(false, "Checking connection…");
        Log.d(TAG, "Testing connection to: " + plexSession.getServerUri());

        serverApi.identity().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✓ /identity OK (local)");
                    onConnectionReady("Local");
                } else {
                    Log.w(TAG, "✗ /identity HTTP " + response.code());
                    tryRemoteFallback();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "✗ /identity failed: " + t.getMessage());
                tryRemoteFallback();
            }
        });
    }

    /**
     * Tries the remote server URI saved at auth time.
     * If it works, switches serverApi and unblocks search.
     * The local URI stays in prefs for the next time we're on home WiFi.
     */
    private void tryRemoteFallback() {
        String remoteUri = plexSession.getServerUriRemote();
        if (remoteUri == null || remoteUri.equals(plexSession.getServerUri())) {
            runOnUiThread(() -> showConnectionError());
            return;
        }
        Log.d(TAG, "Trying remote fallback: " + remoteUri);
        runOnUiThread(() -> setSearchEnabled(false, "Trying remote access…"));

        PlexServerApiService fallbackApi = PlexRetrofitClient.getServerService(
                remoteUri, plexSession.getClientId(), plexSession.getAuthToken());

        fallbackApi.identity().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✓ Remote fallback OK: " + remoteUri);
                    plexSession.switchToRemoteServer(remoteUri);
                    serverApi = fallbackApi;
                    runOnUiThread(() -> onConnectionReady("Remote"));
                } else {
                    Log.w(TAG, "✗ Remote fallback HTTP " + response.code());
                    runOnUiThread(() -> showConnectionError());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "✗ Remote fallback failed: " + t.getMessage());
                runOnUiThread(() -> showConnectionError());
            }
        });
    }

    /** Called once a working server URI (local or remote) has been confirmed. */
    private void onConnectionReady(String label) {
        connectionReady = true;
        setSearchEnabled(true, null);
        if ("Remote".equals(label)) {
            Toast.makeText(this, "Connected via remote access", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "Connection ready (" + label + "): " + plexSession.getServerUri());
    }

    /** Enables or disables the search button; optionally shows a status hint. */
    private void setSearchEnabled(boolean enabled, String hint) {
        View btnSearch = findViewById(R.id.btn_search);
        if (btnSearch != null) btnSearch.setEnabled(enabled);
        etSearch.setEnabled(enabled);
        if (hint != null) {
            tvEmpty.setText(hint);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showConnectionError() {
        connectionReady = false;
        setSearchEnabled(false, "Cannot reach Plex server.\nCheck Remote Access in Plex settings.");
    }

    private void populateHeader() {
        String uri = plexSession.getServerUri();
        if (uri != null) {
            try {
                java.net.URI parsed = java.net.URI.create(uri);
                String host = parsed.getHost();
                tvServerName.setText(host != null ? host : "Plex");
            } catch (Exception e) {
                tvServerName.setText("Plex");
            }
        } else {
            tvServerName.setText("Plex");
        }

        btnSignOut.setOnClickListener(v -> confirmSignOut());
    }

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performPlexSearch();
                return true;
            }
            return false;
        });
        findViewById(R.id.btn_search).setOnClickListener(v -> performPlexSearch());
    }

    // ── Plex search ───────────────────────────────────────────────────────────

    private void performPlexSearch() {
        if (!connectionReady) {
            Toast.makeText(this, "Still checking connection, please wait…",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            etSearch.setError("Enter a song or artist name");
            return;
        }
        hideKeyboard();
        setLoading(true);
        tvEmpty.setVisibility(View.GONE);
        adapter.setItems(null);

        String sectionId = plexSession.getSectionId();

        Callback<PlexSearchResponse> callback = new Callback<PlexSearchResponse>() {
            @Override
            public void onResponse(Call<PlexSearchResponse> call,
                                   Response<PlexSearchResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showError("Search failed (" + response.code() + ")");
                    return;
                }
                List<VideoItem> results = parsePlexTracks(response.body());
                if (results.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    adapter.setItems(results);
                }
            }

            @Override
            public void onFailure(Call<PlexSearchResponse> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getMessage());
            }
        };

        if (sectionId != null && !sectionId.isEmpty()) {
            serverApi.searchTracks(sectionId, query, 10).enqueue(callback);
        } else {
            serverApi.searchAll(query, 10).enqueue(callback);
        }
    }

    private List<VideoItem> parsePlexTracks(PlexSearchResponse response) {
        List<VideoItem> list = new ArrayList<>();
        if (response.MediaContainer == null
                || response.MediaContainer.Metadata == null) return list;

        for (PlexSearchResponse.Track track : response.MediaContainer.Metadata) {
            String partKey = null;
            if (track.Media != null && !track.Media.isEmpty()) {
                PlexSearchResponse.Media media = track.Media.get(0);
                if (media.Part != null && !media.Part.isEmpty()) {
                    partKey = media.Part.get(0).key;
                }
            }
            if (partKey == null) continue;

            String streamUrl  = plexSession.streamUrl(partKey);
            String thumbUrl   = plexSession.thumbUrl(track.thumb);
            String artist     = track.grandparentTitle != null ? track.grandparentTitle : "";
            String durationStr = formatDuration(track.duration);

            list.add(new VideoItem(
                    partKey,
                    track.title,
                    artist,
                    thumbUrl,
                    durationStr,
                    streamUrl
            ));
        }
        return list;
    }

    private String formatDuration(long ms) {
        if (ms <= 0) return "";
        long secs  = ms / 1000;
        long mins  = secs / 60;
        long hours = mins / 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d",
                    hours, mins % 60, secs % 60);
        }
        return String.format(Locale.US, "%d:%02d", mins, secs % 60);
    }

    // ── Open player ───────────────────────────────────────────────────────────

    private void openPlayer(VideoItem item) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(Constants.EXTRA_VIDEO_TITLE,  item.getTitle());
        intent.putExtra(Constants.EXTRA_CHANNEL_NAME, item.getChannelName());
        intent.putExtra(Constants.EXTRA_THUMBNAIL,    item.getThumbnailUrl());
        intent.putExtra(Constants.EXTRA_STREAM_URL,   item.getStreamUrl());
        intent.putExtra(Constants.EXTRA_VIDEO_ID,     item.getVideoId());
        startActivity(intent);
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Disconnect Plex")
                .setMessage("Sign out and remove Plex credentials?")
                .setPositiveButton("Sign out", (d, w) -> doSignOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doSignOut() {
        plexSession.signOut(this);
        startActivity(new Intent(this, PlexAuthActivity.class));
        finish();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvResults.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
