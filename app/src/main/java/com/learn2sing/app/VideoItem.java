package com.learn2sing.app;

/**
 * Represents a single search result — either a YouTube video or a Plex track.
 *
 * For YouTube results:
 *   videoId      = YouTube video ID
 *   streamUrl    = null  (PlayerActivity will show a "not available" notice)
 *
 * For Plex results:
 *   videoId      = Plex track rating key (used for lyrics lookup)
 *   streamUrl    = full Plex stream URL with token (ready for ExoPlayer)
 *   channelName  = artist name
 */
public class VideoItem {

    private final String videoId;       // YouTube ID  OR  Plex track key
    private final String title;
    private final String channelName;   // YouTube channel  OR  Plex artist
    private final String thumbnailUrl;
    private final String duration;      // display string, e.g. "3:42"
    private final String streamUrl;     // null for YouTube; full URL for Plex

    /** YouTube constructor (no stream URL). */
    public VideoItem(String videoId, String title, String channelName,
                     String thumbnailUrl, String duration) {
        this(videoId, title, channelName, thumbnailUrl, duration, null);
    }

    /** Full constructor — use for Plex tracks. */
    public VideoItem(String videoId, String title, String channelName,
                     String thumbnailUrl, String duration, String streamUrl) {
        this.videoId      = videoId;
        this.title        = title;
        this.channelName  = channelName;
        this.thumbnailUrl = thumbnailUrl;
        this.duration     = duration;
        this.streamUrl    = streamUrl;
    }

    public String getVideoId()      { return videoId; }
    public String getTitle()        { return title; }
    public String getChannelName()  { return channelName; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getDuration()     { return duration; }
    public String getStreamUrl()    { return streamUrl; }

    /** True when this item has a ready-to-stream Plex URL. */
    public boolean hasStreamUrl()   { return streamUrl != null && !streamUrl.isEmpty(); }
}
