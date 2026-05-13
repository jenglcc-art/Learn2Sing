# Skill: ExoPlayer Speed & Pitch Control

## Overview
ExoPlayer supports independent speed and pitch control via `PlaybackParameters`. This lets users slow down audio for practice without changing pitch (or change pitch without changing speed).

## Basic Usage

```java
// Speed: 1.0f = normal, 0.5f = half speed, 2.0f = double speed
// Pitch: 1.0f = normal, <1.0 = lower, >1.0 = higher
PlaybackParameters params = new PlaybackParameters(speed, pitch);
exoPlayer.setPlaybackParameters(params);
```

## Speed-Only Control (Preserve Pitch)
To slow down without lowering pitch (most useful for singing practice):

```java
float speed = 0.75f;
float pitch = 1.0f;   // keep pitch unchanged
exoPlayer.setPlaybackParameters(new PlaybackParameters(speed, pitch));
```

## Pitch-Only Control (Preserve Speed)
To change key without changing tempo:

```java
float speed = 1.0f;   // keep speed unchanged
float pitch = 0.9f;   // lower pitch by ~1 semitone
exoPlayer.setPlaybackParameters(new PlaybackParameters(speed, pitch));
```

## UI: SeekBar Wiring Pattern

```java
// speed ranges from 0.25 to 2.0
speedSeekBar.setMax(100);  // 0 → 0.25×, 100 → 2.0×
speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        float speed = 0.25f + (progress / 100f) * 1.75f;
        updatePlaybackParams();
    }
    @Override public void onStartTrackingTouch(SeekBar bar) {}
    @Override public void onStopTrackingTouch(SeekBar bar) {}
});

// pitch ranges from 0.5 to 2.0
pitchSeekBar.setMax(100);
pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        float pitch = 0.5f + (progress / 100f) * 1.5f;
        updatePlaybackParams();
    }
    @Override public void onStartTrackingTouch(SeekBar bar) {}
    @Override public void onStopTrackingTouch(SeekBar bar) {}
});
```

```java
private void updatePlaybackParams() {
    if (exoPlayer == null) return;
    float speed = 0.25f + (speedSeekBar.getProgress() / 100f) * 1.75f;
    float pitch = 0.5f  + (pitchSeekBar.getProgress() / 100f) * 1.5f;
    exoPlayer.setPlaybackParameters(new PlaybackParameters(speed, pitch));
    speedLabel.setText(String.format("%.2f×", speed));
    pitchLabel.setText(String.format("%.2f×", pitch));
}
```

## Displaying Current Playback Position (Lyrics Sync)

Use a `Handler` + `Runnable` to poll `exoPlayer.getCurrentPosition()`:

```java
private final Handler syncHandler = new Handler(Looper.getMainLooper());
private final Runnable syncRunnable = new Runnable() {
    @Override
    public void run() {
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            long positionMs = exoPlayer.getCurrentPosition();
            syncLyrics(positionMs);
        }
        syncHandler.postDelayed(this, 100);  // poll every 100ms
    }
};

// Start in onResume / after play begins
syncHandler.post(syncRunnable);

// Stop in onPause / onDestroy
syncHandler.removeCallbacks(syncRunnable);
```

## Syncing Timed Lyrics to Playback
LRCLib returns a list of lines with timestamps in seconds:

```java
// LrcLine has: timeSeconds (float), text (String)
private void syncLyrics(long positionMs) {
    float posSec = positionMs / 1000f;
    int activeLine = 0;
    for (int i = 0; i < lrcLines.size(); i++) {
        if (lrcLines.get(i).timeSeconds <= posSec) {
            activeLine = i;
        }
    }
    lyricsAdapter.setActiveLine(activeLine);
    lyricsRecyclerView.smoothScrollToPosition(activeLine);
}
```

## Lifecycle Notes
- **Always release ExoPlayer** in `onDestroy()`:
  ```java
  if (exoPlayer != null) {
      exoPlayer.release();
      exoPlayer = null;
  }
  ```
- **Stop the sync handler** in `onPause()` or `onDestroy()` to avoid leaks.
- Set playback parameters **after** `exoPlayer.prepare()` and before or after `play()` — order doesn't matter.

## Required Dependency

```groovy
// build.gradle (app)
implementation 'androidx.media3:media3-exoplayer:1.3.1'
implementation 'androidx.media3:media3-ui:1.3.1'
// For custom HTTP (e.g., trust-all SSL for local servers):
implementation 'androidx.media3:media3-datasource-okhttp:1.3.1'
```

## Custom HTTP (Trust-All SSL) + ExoPlayer

ExoPlayer's default `DefaultHttpDataSource` does full SSL certificate validation.
For local servers with raw-IP or self-signed certs, replace it:

```java
OkHttpDataSource.Factory dataSourceFactory =
        new OkHttpDataSource.Factory(buildTrustAllClient());

DefaultMediaSourceFactory mediaSourceFactory =
        new DefaultMediaSourceFactory(this)
                .setDataSourceFactory(dataSourceFactory);

exoPlayer = new ExoPlayer.Builder(this)
        .setMediaSourceFactory(mediaSourceFactory)
        .build();
```

See `android-https-local-server.md` for the full `buildTrustAllClient()` implementation.
