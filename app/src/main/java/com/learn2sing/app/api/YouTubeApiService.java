package com.learn2sing.app.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface for YouTube Data API v3.
 * Base URL: https://www.googleapis.com/youtube/v3/
 *
 * Authentication is handled via the "Authorization: Bearer {token}" header
 * injected by {@link RetrofitClient} — no API key query param needed.
 */
public interface YouTubeApiService {

    /**
     * Search YouTube for music videos.
     *
     * @param query           Search terms (song + artist)
     * @param type            Fixed to "video"
     * @param part            "snippet" for titles, thumbnails, channel names
     * @param videoCategoryId "10" = Music category filter
     * @param videoEmbeddable "true" = only return videos that allow embedding
     * @param maxResults      Number of results to return
     */
    @GET("search")
    Call<YouTubeSearchResponse> searchVideos(
            @Query("q")               String query,
            @Query("type")            String type,
            @Query("part")            String part,
            @Query("videoCategoryId") String videoCategoryId,
            @Query("videoEmbeddable") String videoEmbeddable,
            @Query("maxResults")      int    maxResults
    );

    /**
     * Rate (like/dislike/remove rating) a video.
     * Requires the youtube scope — uses the OAuth token from RetrofitClient.
     *
     * @param videoId YouTube video ID
     * @param rating  "like" | "dislike" | "none"
     */
    @POST("videos/rate")
    Call<Void> rateVideo(
            @Query("id")     String videoId,
            @Query("rating") String rating
    );

    /**
     * Retrieve the current user's rating for a video.
     *
     * @param videoId YouTube video ID
     */
    @GET("videos/getRating")
    Call<VideoRatingResponse> getVideoRating(
            @Query("id") String videoId
    );
}
