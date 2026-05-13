package com.learn2sing.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.learn2sing.app.api.PlexResourcesResponse;
import com.learn2sing.app.api.PlexRetrofitClient;
import com.learn2sing.app.api.PlexSectionsResponse;
import com.learn2sing.app.api.PlexTvApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Plex PIN-based OAuth flow.
 *
 * Steps:
 *  1. POST plex.tv/api/v2/pins  → get { id, code }
 *  2. Open browser to app.plex.tv/auth with the code
 *  3. Poll plex.tv/api/v2/pins/{id} every 2 s until authToken != null
 *  4. Discover the user's Plex server URI
 *  5. Find the music library section
 *  6. Launch MainActivity
 */
public class PlexAuthActivity extends AppCompatActivity {

    private static final int POLL_INTERVAL_MS = 2000;
    private static final int POLL_TIMEOUT_MS  = 300_000; // 5 minutes

    private TextView    tvStatus, tvCode;
    private ProgressBar progressBar;
    private Button      btnOpenBrowser, btnRetry;

    private PlexSession session;
    private PlexTvApiService plexTvApi;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long   pinId;
    private String pinCode;
    private int    pollCount;
    private static final int MAX_POLLS = POLL_TIMEOUT_MS / POLL_INTERVAL_MS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plex_auth);

        tvStatus      = findViewById(R.id.tv_plex_status);
        tvCode        = findViewById(R.id.tv_plex_code);
        progressBar   = findViewById(R.id.progress_plex);
        btnOpenBrowser = findViewById(R.id.btn_open_browser);
        btnRetry      = findViewById(R.id.btn_plex_retry);

        session = PlexSession.getInstance();
        session.restore(this);

        // Already signed in — go straight to the main screen
        if (session.isAuthenticated() && session.hasServer()) {
            launchMain();
            return;
        }

        plexTvApi = PlexRetrofitClient.getPlexTvService(session.getClientId());

        btnRetry.setOnClickListener(v -> startPinFlow());
        btnOpenBrowser.setOnClickListener(v -> openAuthBrowser());

        startPinFlow();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ── Step 1: Request a PIN ─────────────────────────────────────────────────

    private void startPinFlow() {
        setUiState(UiState.LOADING, "Connecting to Plex…");
        btnOpenBrowser.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        tvCode.setVisibility(View.GONE);
        pollCount = 0;

        plexTvApi.createPin("true").enqueue(new Callback<PlexTvApiService.PinResponse>() {
            @Override
            public void onResponse(Call<PlexTvApiService.PinResponse> call,
                                   Response<PlexTvApiService.PinResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setUiState(UiState.ERROR, "Could not reach Plex. Check your internet connection.");
                    return;
                }
                pinId   = response.body().id;
                pinCode = response.body().code;
                onPinReceived();
            }
            @Override
            public void onFailure(Call<PlexTvApiService.PinResponse> call, Throwable t) {
                setUiState(UiState.ERROR, "Network error: " + t.getMessage());
            }
        });
    }

    // ── Step 2: Show PIN code and open browser ────────────────────────────────

    private void onPinReceived() {
        setUiState(UiState.WAITING, "Sign in to Plex in the browser,\nthen return to this app.");
        tvCode.setText(pinCode);
        tvCode.setVisibility(View.VISIBLE);
        btnOpenBrowser.setVisibility(View.VISIBLE);

        // Auto-open the browser
        openAuthBrowser();

        // Start polling
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void openAuthBrowser() {
        String url = "https://app.plex.tv/auth#?"
                + "clientID=" + session.getClientId()
                + "&code=" + pinCode
                + "&context[device][product]=" + Constants.PLEX_PRODUCT;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    // ── Step 3: Poll for auth token ───────────────────────────────────────────

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (pollCount++ > MAX_POLLS) {
                setUiState(UiState.ERROR, "Timed out waiting for Plex sign-in.");
                return;
            }
            plexTvApi.checkPin(pinId).enqueue(new Callback<PlexTvApiService.PinResponse>() {
                @Override
                public void onResponse(Call<PlexTvApiService.PinResponse> call,
                                       Response<PlexTvApiService.PinResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                        return;
                    }
                    String token = response.body().authToken;
                    if (token != null && !token.isEmpty()) {
                        session.saveToken(PlexAuthActivity.this, token);
                        discoverServer();
                    } else {
                        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                    }
                }
                @Override
                public void onFailure(Call<PlexTvApiService.PinResponse> call, Throwable t) {
                    handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                }
            });
        }
    };

    // ── Step 4: Discover Plex server ──────────────────────────────────────────

    private void discoverServer() {
        setUiState(UiState.LOADING, "Finding your Plex server…");
        tvCode.setVisibility(View.GONE);
        btnOpenBrowser.setVisibility(View.GONE);

        // Use a freshly-built client with the token baked in — avoids any
        // interaction between @Header annotations and OkHttp interceptors.
        PlexRetrofitClient.getAuthenticatedPlexTvService(
                        session.getClientId(), session.getAuthToken())
                .getResources()
                .enqueue(new Callback<List<PlexResourcesResponse>>() {
                    @Override
                    public void onResponse(Call<List<PlexResourcesResponse>> call,
                                           Response<List<PlexResourcesResponse>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            setUiState(UiState.ERROR, "Could not list Plex servers.");
                            return;
                        }
                        String bestUri = chooseBestServerUri(response.body());
                        if (bestUri == null) {
                            setUiState(UiState.ERROR,
                                    "No Plex Media Server found on your account.\n"
                                    + "Make sure your server is running and signed in to Plex.");
                            return;
                        }
                        session.saveServer(PlexAuthActivity.this, bestUri);

                        // Also save a remote URI so the app works away from home WiFi.
                        String remoteUri = chooseRemoteServerUri(response.body());
                        if (remoteUri != null && !remoteUri.equals(bestUri)) {
                            session.saveRemoteServer(PlexAuthActivity.this, remoteUri);
                        }

                        discoverMusicSection();
                    }
                    @Override
                    public void onFailure(Call<List<PlexResourcesResponse>> call, Throwable t) {
                        setUiState(UiState.ERROR, "Network error: " + t.getMessage());
                    }
                });
    }

    private String chooseBestServerUri(List<PlexResourcesResponse> resources) {
        for (PlexResourcesResponse resource : resources) {
            if (resource.provides == null || !resource.provides.contains("server")) continue;
            if (resource.connections == null) continue;

            // We trust all SSL certs (OkHttp is configured that way), so HTTPS always
            // works regardless of whether the host is a raw IP or a plex.direct hostname.
            // Always prefer HTTPS because Plex may require it ("Require HTTPS" setting).

            // 1. Local HTTPS — plex.direct hostname (best option)
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (conn.local && !conn.relay && conn.uri != null
                        && conn.uri.startsWith("https")
                        && !isRawIpUri(conn.uri)) {
                    return conn.uri;
                }
            }
            // 2. Local HTTPS — raw IP (works because we trust all certs)
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (conn.local && !conn.relay && conn.uri != null
                        && conn.uri.startsWith("https")) {
                    return conn.uri;
                }
            }
            // 3. Remote HTTPS — plex.direct (off-LAN access)
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (!conn.local && !conn.relay && conn.uri != null
                        && conn.uri.startsWith("https")) {
                    return conn.uri;
                }
            }
            // 4. Relay
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (conn.relay && conn.uri != null) return conn.uri;
            }
            // 5. Any HTTP as last resort — upgrade to HTTPS
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (conn.uri != null) return toHttps(conn.uri);
            }
        }
        return null;
    }

    /**
     * Returns the best non-local connection URI — used as a fallback when the
     * local URI is unreachable (e.g. user is away from home WiFi).
     *
     * Priority: remote HTTPS plex.direct → relay → any remote HTTP→HTTPS
     */
    private String chooseRemoteServerUri(List<PlexResourcesResponse> resources) {
        for (PlexResourcesResponse resource : resources) {
            if (resource.provides == null || !resource.provides.contains("server")) continue;
            if (resource.connections == null) continue;

            // 1. Remote HTTPS (non-relay)
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (!conn.local && !conn.relay && conn.uri != null
                        && conn.uri.startsWith("https")) {
                    return conn.uri;
                }
            }
            // 2. Relay HTTPS
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (conn.relay && conn.uri != null
                        && conn.uri.startsWith("https")) {
                    return conn.uri;
                }
            }
            // 3. Any relay
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (conn.relay && conn.uri != null) {
                    return toHttps(conn.uri);
                }
            }
            // 4. Any remote
            for (PlexResourcesResponse.Connection conn : resource.connections) {
                if (!conn.local && conn.uri != null) {
                    return toHttps(conn.uri);
                }
            }
        }
        return null;
    }

    private static String toHttps(String uri) {
        if (uri != null && uri.startsWith("http://")) return "https://" + uri.substring(7);
        return uri;
    }

    /** True if the URI host is a raw IPv4 address (digits and dots only). */
    private boolean isRawIpUri(String uri) {
        try {
            String host = java.net.URI.create(uri).getHost();
            return host != null && host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
        } catch (Exception e) {
            return false;
        }
    }

    // ── Step 5: Find music library section ────────────────────────────────────

    private void discoverMusicSection() {
        setUiState(UiState.LOADING, "Finding your music library…");

        PlexRetrofitClient.getServerService(session.getServerUri(), session.getClientId(), session.getAuthToken())
                .getSections()
                .enqueue(new Callback<PlexSectionsResponse>() {
                    @Override
                    public void onResponse(Call<PlexSectionsResponse> call,
                                           Response<PlexSectionsResponse> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().MediaContainer == null) {
                            // No music library found — still proceed, user can search anyway
                            launchMain();
                            return;
                        }
                        String musicSectionId = null;
                        if (response.body().MediaContainer.Directory != null) {
                            for (PlexSectionsResponse.Directory dir
                                    : response.body().MediaContainer.Directory) {
                                if ("artist".equals(dir.type)) {
                                    musicSectionId = dir.key;
                                    break;
                                }
                            }
                        }
                        if (musicSectionId != null) {
                            session.saveSection(PlexAuthActivity.this, musicSectionId);
                        }
                        launchMain();
                    }
                    @Override
                    public void onFailure(Call<PlexSectionsResponse> call, Throwable t) {
                        // Proceed anyway — user can still search
                        launchMain();
                    }
                });
    }

    // ── Step 6: Done ──────────────────────────────────────────────────────────

    private void launchMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private enum UiState { LOADING, WAITING, ERROR }

    private void setUiState(UiState state, String message) {
        tvStatus.setText(message);
        progressBar.setVisibility(state == UiState.LOADING ? View.VISIBLE : View.GONE);
        if (state == UiState.ERROR) btnRetry.setVisibility(View.VISIBLE);
    }
}
