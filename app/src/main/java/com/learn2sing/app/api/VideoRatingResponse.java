package com.learn2sing.app.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response from YouTube Data API v3 videos/getRating endpoint.
 */
public class VideoRatingResponse {

    @SerializedName("items")
    public List<RatingItem> items;

    public static class RatingItem {
        @SerializedName("videoId")
        public String videoId;

        /** "like" | "dislike" | "none" */
        @SerializedName("rating")
        public String rating;
    }

    /** Convenience helper — returns the rating for the first item, or "none". */
    public String getFirstRating() {
        if (items != null && !items.isEmpty() && items.get(0).rating != null) {
            return items.get(0).rating;
        }
        return "none";
    }
}
