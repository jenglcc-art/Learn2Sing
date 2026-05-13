package com.learn2sing.app;

/**
 * One line of time-synced lyrics in LRC format.
 */
public class LyricsLine {

    private final double timeSeconds;  // start time in seconds
    private final String text;

    public LyricsLine(double timeSeconds, String text) {
        this.timeSeconds = timeSeconds;
        this.text        = text;
    }

    public double getTimeSeconds() { return timeSeconds; }
    public String getText()        { return text; }
}
