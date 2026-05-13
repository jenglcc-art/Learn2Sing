package com.learn2sing.app.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for the plex.tv cloud API.
 *
 * All calls include Plex headers injected by OkHttp (see PlexRetrofitClient).
 *
 * Endpoints used:
 *   POST /api/v2/pins           — create a new login PIN
 *   GET  /api/v2/pins/{id}      — poll until authToken is populated
 *   GET  /api/v2/resources      — list all Plex servers on this account
 */
public interface PlexTvApiService {

    /** Step 1: request a new PIN code. strong=true gives a longer code. */
    @POST("api/v2/pins")
    Call<PinResponse> createPin(@Query("strong") String strong);

    /** Step 3: poll until authToken != null. */
    @GET("api/v2/pins/{id}")
    Call<PinResponse> checkPin(@Path("id") long pinId);

    /**
     * Step 4: list all Media Servers for the authenticated user.
     * Use with getAuthenticatedPlexTvService() — the token is sent via the
     * OkHttp interceptor (both header and query param), not @Header annotation.
     */
    @GET("api/v2/resources")
    Call<List<PlexResourcesResponse>> getResources();

    // ── Inner response model ───────────────────────────────────────────────

    class PinResponse {
        public long   id;
        public String code;
        public String authToken;   // Plex API v2 returns camelCase "authToken"
    }
}
