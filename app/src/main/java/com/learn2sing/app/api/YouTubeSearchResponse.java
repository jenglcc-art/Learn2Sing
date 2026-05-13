package com.learn2sing.app.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * POJO for the YouTube Data API v3 search/list response.
 */
public class YouTubeSearchResponse {

    @SerializedName("items")
    public List<Item> items;

    public static class Item {
        @SerializedName("id")
        public Id id;

        @SerializedName("snippet")
        public Snippet snippet;
    }

    public static class Id {
        @SerializedName("videoId")
        public String videoId;
    }

    public static class Snippet {
        @SerializedName("title")
        public String title;

        @SerializedName("channelTitle")
        public String channelTitle;

        @SerializedName("thumbnails")
        public Thumbnails thumbnails;
    }

    public static class Thumbnails {
        @SerializedName("medium")
        public Thumbnail medium;

        @SerializedName("high")
        public Thumbnail high;

        @SerializedName("default")
        public Thumbnail defaultThumb;
    }

    public static class Thumbnail {
        @SerializedName("url")
        public String url;
    }
}
