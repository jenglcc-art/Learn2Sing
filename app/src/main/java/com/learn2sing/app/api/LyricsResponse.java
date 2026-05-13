package com.learn2sing.app.api;

import com.google.gson.annotations.SerializedName;

/**
 * POJO for a single result from LRCLib's search or get endpoint.
 */
public class LyricsResponse {

    @SerializedName("id")
    public int id;

    @SerializedName("trackName")
    public String trackName;

    @SerializedName("artistName")
    public String artistName;

    @SerializedName("albumName")
    public String albumName;

    @SerializedName("duration")
    public double duration;

    /** Plain (unsynced) lyrics. */
    @SerializedName("plainLyrics")
    public String plainLyrics;

    /** Time-synced lyrics in LRC format. May be null. */
    @SerializedName("syncedLyrics")
    public String syncedLyrics;
}
