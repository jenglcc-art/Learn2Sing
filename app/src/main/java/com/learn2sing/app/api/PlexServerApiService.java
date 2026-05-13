package com.learn2sing.app.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for calls to the user's own Plex Media Server.
 *
 * Auth token is injected as X-Plex-Token header by OkHttp interceptor
 * in PlexRetrofitClient.
 *
 * Plex type IDs:
 *   1 = Movie, 2 = Show, 4 = Season, 10 = Track (audio)
 */
public interface PlexServerApiService {

    /**
     * Lightweight identity check — returns server version/name with no auth needed.
     * Use this to verify connectivity before making library calls.
     */
    @GET("identity")
    Call<ResponseBody> identity();

    /** List all library sections (to find the music section key). */
    @GET("library/sections")
    Call<PlexSectionsResponse> getSections();

    /**
     * Search tracks within a specific music section.
     * Uses /all?type=10&title= which is the stable Plex search path.
     */
    @GET("library/sections/{sectionId}/all")
    Call<PlexSearchResponse> searchTracks(
            @Path("sectionId") String sectionId,
            @Query("title") String title,
            @Query("type") int type          // 10 = Track
    );

    /**
     * Fallback: search across the entire library when sectionId is unknown.
     */
    @GET("library/all")
    Call<PlexSearchResponse> searchAll(
            @Query("title") String title,
            @Query("type") int type
    );
}
