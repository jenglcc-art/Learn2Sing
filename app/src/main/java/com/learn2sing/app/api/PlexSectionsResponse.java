package com.learn2sing.app.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response from GET /library/sections on the Plex server.
 *
 * We look for a Directory whose type == "artist" — that's the music library.
 *
 * Example:
 * {
 *   "MediaContainer": {
 *     "Directory": [
 *       { "key": "1", "title": "Music", "type": "artist" },
 *       { "key": "2", "title": "Movies","type": "movie"  }
 *     ]
 *   }
 * }
 */
public class PlexSectionsResponse {

    @SerializedName("MediaContainer")
    public MediaContainer MediaContainer;

    public static class MediaContainer {

        @SerializedName("Directory")
        public List<Directory> Directory;
    }

    public static class Directory {
        public String key;
        public String title;
        public String type;   // "artist" for music libraries
    }
}
