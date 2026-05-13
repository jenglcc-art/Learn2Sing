package com.learn2sing.app.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response from GET /library/sections/{sectionId}/search?type=10&query={q}
 * (type=10 = tracks in Plex API)
 *
 * Each Track contains:
 *  - title          : track name
 *  - grandparentTitle : artist name
 *  - parentTitle    : album name
 *  - thumb          : album art path (relative, prefix with serverUri)
 *  - Media[].Part[].key : audio stream path (relative, prefix with serverUri + token)
 *  - duration       : ms
 */
public class PlexSearchResponse {

    @SerializedName("MediaContainer")
    public MediaContainer MediaContainer;

    public static class MediaContainer {

        @SerializedName("Metadata")
        public List<Track> Metadata;
    }

    public static class Track {
        public String title;
        public String grandparentTitle;  // artist
        public String parentTitle;       // album
        public String thumb;             // relative URL for album art
        public long   duration;          // ms

        @SerializedName("Media")
        public List<Media> Media;
    }

    public static class Media {
        @SerializedName("Part")
        public List<Part> Part;
    }

    public static class Part {
        public String key;   // relative audio stream path, e.g. /library/parts/123/...
    }
}
