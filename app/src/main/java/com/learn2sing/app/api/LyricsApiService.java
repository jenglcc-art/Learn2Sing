package com.learn2sing.app.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit interface for LRCLib (https://lrclib.net).
 * No API key required.
 * Base URL: https://lrclib.net/
 */
public interface LyricsApiService {

    /**
     * Search for synced lyrics by combined query string.
     * Returns a list of matching tracks; the first with syncedLyrics is used.
     */
    @GET("api/search")
    Call<List<LyricsResponse>> searchLyrics(
            @Query("q") String query
    );

    /**
     * Fetch lyrics directly by artist + track name.
     */
    @GET("api/get")
    Call<LyricsResponse> getLyrics(
            @Query("artist_name") String artistName,
            @Query("track_name")  String trackName
    );
}
