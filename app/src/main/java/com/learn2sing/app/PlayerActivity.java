package com.learn2sing.app;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.learn2sing.app.api.LyricsResponse;
import com.learn2sing.app.api.PlexRetrofitClient;
import com.learn2sing.app.api.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Player screen — ExoPlayer-based, streams directly from Plex.
 *
 * Features:
 *  - Auto-streams the Plex track URL passed via EXTRA_STREAM_URL
 *  - Continuous speed control: 0.25× – 2.0×
 *  - True pitch lock (original musical key at any speed)
 *  - Auto-scrolling synced lyrics via LRCLib
 *  - Optional like button (requires Google Sign-In)
 *  - Fallback local file picker if no stream URL
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    private static final long SEEK_UPDATE_MS = 500;

    // ── Views ─────────────────────────────────────────────────────────────────
    private de.hdodenhof.circleimageview.CircleImageView ivAlbumArt;
    private TextView    tvTitle, tvChannel;
    private TextView    tvCurrentTime, tvTotalTime, tvSpeedValue, tvLyricsStatus;
    private SeekBar     seekBarPosition, seekBarSpeed;
    private ImageButton btnPlayPause, btnLike, btnBack;
    private Button      btnPickFile;
    private Switch      switchPitchLock;
    private RecyclerView rvLyrics;
    private View        cardNoFile;

    // ── ExoPlayer ─────────────────────────────────────────────────────────────
    private ExoPlayer exoPlayer;
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private boolean isSeekBarTracking = false;
    private boolean hasMedia = false;

    // ── Playback state ────────────────────────────────────────────────────────
    private float   currentSpeed = 1.0f;
    private boolean pitchLock    = true;

    // ── Song info ─────────────────────────────────────────────────────────────
    private String videoId;       // Plex part key or YouTube ID
    private String videoTitle;
    private String channelName;
    private String thumbnailUrl;
    private String streamUrl;     // Plex full stream URL (may be null for local files)

    // ── Like state ────────────────────────────────────────────────────────────
    private boolean isLiked     = false;
    private boolean likeLoading = false;

    // ── Lyrics ────────────────────────────────────────────────────────────────
    private LyricsAdapter       lyricsAdapter;
    private List<LyricsLine>    lyricLines = new ArrayList<>();
    private LinearLayoutManager lyricsLayoutManager;

    // ── Local file picker (fallback) ──────────────────────────────────────────
    private final ActivityResultLauncher<Intent> audioPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            Uri uri = result.getData().getData();
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            loadLocalFile(uri);
                        }
                    }
            );

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        extractIntentExtras();
        bindViews();
        setupExoPlayer();
        setupSeekBar();
        setupSpeedSlider();
        setupPitchLock();
        setupLyricsRecyclerView();
        populateHeader();

        if (streamUrl != null && !streamUrl.isEmpty()) {
            loadPlexStream();
        } else {
            // No Plex stream — show the "pick file" card
            cardNoFile.setVisibility(View.VISIBLE);
        }

        fetchLyrics();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        seekHandler.removeCallbacksAndMessages(null);
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        super.onDestroy();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void extractIntentExtras() {
        videoId      = getIntent().getStringExtra(Constants.EXTRA_VIDEO_ID);
        videoTitle   = getIntent().getStringExtra(Constants.EXTRA_VIDEO_TITLE);
        channelName  = getIntent().getStringExtra(Constants.EXTRA_CHANNEL_NAME);
        thumbnailUrl = getIntent().getStringExtra(Constants.EXTRA_THUMBNAIL);
        streamUrl    = getIntent().getStringExtra(Constants.EXTRA_STREAM_URL);
    }

    private void bindViews() {
        ivAlbumArt      = findViewById(R.id.iv_album_art);
        tvTitle         = findViewById(R.id.tv_player_title);
        tvChannel       = findViewById(R.id.tv_player_channel);
        tvCurrentTime   = findViewById(R.id.tv_current_time);
        tvTotalTime     = findViewById(R.id.tv_total_time);
        tvSpeedValue    = findViewById(R.id.tv_speed_value);
        tvLyricsStatus  = findViewById(R.id.tv_lyrics_status);
        seekBarPosition = findViewById(R.id.seekbar_position);
        seekBarSpeed    = findViewById(R.id.seekbar_speed);
        btnPlayPause    = findViewById(R.id.btn_play_pause);
        btnLike         = findViewById(R.id.btn_like);
        btnBack         = findViewById(R.id.btn_back);
        btnPickFile     = findViewById(R.id.btn_pick_file);
        switchPitchLock = findViewById(R.id.switch_pitch_lock);
        rvLyrics        = findViewById(R.id.rv_lyrics);
        cardNoFile      = findViewById(R.id.card_no_file);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnPickFile.setOnClickListener(v -> launchFilePicker());
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        // Like is only available when Google is signed in
        UserSession userSession = UserSession.getInstance();
        userSession.restoreFromLastSignIn(this);
        if (userSession.isSignedIn()) {
            btnLike.setVisibility(View.VISIBLE);
            btnLike.setOnClickListener(v -> toggleLike());
            RetrofitClient.init(this);
        } else {
            btnLike.setVisibility(View.GONE);
        }
    }

    private void populateHeader() {
        tvTitle.setText(videoTitle != null ? videoTitle : "Unknown song");
        tvChannel.setText(channelName != null ? channelName : "");
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            Glide.with(this).load(thumbnailUrl).into(ivAlbumArt);
        }
    }

    private void setupExoPlayer() {
        // Use OkHttp data source so ExoPlayer inherits our trust-all SSL config.
        // This is required for Plex HTTPS streams to local servers (raw IP certs).
        OkHttpDataSource.Factory dataSourceFactory = new OkHttpDataSource.Factory(
                PlexRetrofitClient.buildTrustAllClient());
        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(this)
                        .setDataSourceFactory(dataSourceFactory);

        exoPlayer = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();
        exoPlayer.addListener(new Player.Listener() {

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(
                        isPlaying ? android.R.drawable.ic_media_pause
                                  : android.R.drawable.ic_media_play);
                if (isPlaying) startSeekUpdater();
                else           seekHandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long dur = exoPlayer.getDuration();
                    seekBarPosition.setMax((int) (dur / 1000));
                    tvTotalTime.setText(formatTime(dur));
                    cardNoFile.setVisibility(View.GONE);
                    hasMedia = true;
                } else if (state == Player.STATE_BUFFERING) {
                    // Optionally show buffering indicator
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                String cause = error.getCause() != null
                        ? error.getCause().getMessage() : error.getMessage();
                Log.e(TAG, "Playback error code=" + error.errorCode + " cause=" + cause, error);
                Toast.makeText(PlayerActivity.this,
                        "Playback error: " + cause,
                        Toast.LENGTH_LONG).show();
            }
        });
        applyPlaybackParameters();
    }

    // ── Plex streaming ────────────────────────────────────────────────────────

    private void loadPlexStream() {
        // Hide the "no file" card immediately — the stream auto-starts
        cardNoFile.setVisibility(View.GONE);
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)));
        exoPlayer.prepare();
        exoPlayer.play();
        hasMedia = true;
    }

    // ── Local file loading (fallback) ─────────────────────────────────────────

    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        audioPickerLauncher.launch(intent);
    }

    private void loadLocalFile(Uri uri) {
        exoPlayer.stop();
        exoPlayer.setMediaItem(MediaItem.fromUri(uri));
        exoPlayer.prepare();
        exoPlayer.play();
        hasMedia = true;
        readAudioMetadata(uri);
    }

    private void readAudioMetadata(Uri uri) {
        new Thread(() -> {
            try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
                mmr.setDataSource(this, uri);
                String title  = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                byte[] art    = mmr.getEmbeddedPicture();

                runOnUiThread(() -> {
                    if (title != null && !title.isEmpty()) {
                        tvTitle.setText(title);
                        videoTitle  = title;
                        channelName = artist != null ? artist : channelName;
                        tvChannel.setText(channelName != null ? channelName : "");
                        fetchLyrics();
                    }
                    if (art != null) {
                        Glide.with(this).load(art).into(ivAlbumArt);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Could not read audio metadata", e);
            }
        }).start();
    }

    // ── Playback control ──────────────────────────────────────────────────────

    private void togglePlayPause() {
        if (exoPlayer == null) return;
        if (!hasMedia) {
            launchFilePicker();
            return;
        }
        if (exoPlayer.isPlaying()) exoPlayer.pause();
        else                       exoPlayer.play();
    }

    private void applyPlaybackParameters() {
        if (exoPlayer == null) return;
        float pitch = pitchLock ? 1.0f : currentSpeed;
        exoPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, pitch));
    }

    private void updateSpeedLabel() {
        tvSpeedValue.setText(String.format(Locale.US, "%.2f×", currentSpeed));
    }

    // ── SeekBar & speed slider ────────────────────────────────────────────────

    private void setupSeekBar() {
        seekBarPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress * 1000L));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isSeekBarTracking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isSeekBarTracking = false;
                if (exoPlayer != null) exoPlayer.seekTo(sb.getProgress() * 1000L);
            }
        });
    }

    private void setupSpeedSlider() {
        seekBarSpeed.setMax(175);
        seekBarSpeed.setProgress(75); // 1.0× → (1.0 - 0.25) / 0.01 = 75
        updateSpeedLabel();

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                currentSpeed = 0.25f + progress * 0.01f;
                updateSpeedLabel();
                if (fromUser) applyPlaybackParameters();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb)  {}
        });
    }

    private void setupPitchLock() {
        switchPitchLock.setChecked(pitchLock);
        switchPitchLock.setOnCheckedChangeListener((btn, checked) -> {
            pitchLock = checked;
            applyPlaybackParameters();
            Toast.makeText(this,
                    checked ? "Pitch Lock ON — original key preserved"
                            : "Pitch Lock OFF",
                    Toast.LENGTH_SHORT).show();
        });
    }

    // ── Seek bar updater ──────────────────────────────────────────────────────

    private final Runnable seekUpdater = new Runnable() {
        @Override public void run() {
            if (exoPlayer != null && !isSeekBarTracking) {
                long pos = exoPlayer.getCurrentPosition();
                seekBarPosition.setProgress((int) (pos / 1000));
                tvCurrentTime.setText(formatTime(pos));

                if (!lyricLines.isEmpty()) {
                    int idx = lyricsAdapter.updateActiveIndex(pos / 1000.0);
                    if (idx >= 0) scrollToLyric(idx);
                }
            }
            seekHandler.postDelayed(this, SEEK_UPDATE_MS);
        }
    };

    private void startSeekUpdater() {
        seekHandler.removeCallbacksAndMessages(null);
        seekHandler.post(seekUpdater);
    }

    // ── Lyrics ────────────────────────────────────────────────────────────────

    private void setupLyricsRecyclerView() {
        lyricsAdapter = new LyricsAdapter();
        lyricsLayoutManager = new LinearLayoutManager(this);
        rvLyrics.setLayoutManager(lyricsLayoutManager);
        rvLyrics.setAdapter(lyricsAdapter);
    }

    private void fetchLyrics() {
        if (videoTitle == null || videoTitle.isEmpty()) return;
        tvLyricsStatus.setVisibility(View.VISIBLE);
        tvLyricsStatus.setText(R.string.lyrics_loading);

        String cleanTitle = videoTitle
                .replaceAll("(?i)\\(official.*?\\)", "")
                .replaceAll("(?i)\\[official.*?]", "")
                .replaceAll("(?i)\\(lyrics?.*?\\)", "")
                .replaceAll("(?i)\\|.*$", "")
                .trim();
        String query = channelName != null && !channelName.isEmpty()
                ? channelName + " " + cleanTitle : cleanTitle;

        RetrofitClient.getLyricsService().searchLyrics(query)
                .enqueue(new Callback<List<LyricsResponse>>() {
                    @Override public void onResponse(Call<List<LyricsResponse>> c,
                                                     Response<List<LyricsResponse>> r) {
                        if (!r.isSuccessful() || r.body() == null) {
                            runOnUiThread(PlayerActivity.this::showLyricsError);
                            return;
                        }
                        // Prefer synced lyrics
                        for (LyricsResponse lr : r.body()) {
                            if (lr.syncedLyrics != null && !lr.syncedLyrics.isEmpty()) {
                                runOnUiThread(() -> displaySyncedLyrics(lr.syncedLyrics));
                                return;
                            }
                        }
                        // Fall back to plain lyrics
                        for (LyricsResponse lr : r.body()) {
                            if (lr.plainLyrics != null && !lr.plainLyrics.isEmpty()) {
                                runOnUiThread(() -> displayPlainLyrics(lr.plainLyrics));
                                return;
                            }
                        }
                        runOnUiThread(PlayerActivity.this::showLyricsError);
                    }
                    @Override public void onFailure(Call<List<LyricsResponse>> c, Throwable t) {
                        runOnUiThread(PlayerActivity.this::showLyricsError);
                    }
                });
    }

    private void displaySyncedLyrics(String lrc) {
        lyricLines = parseLrc(lrc);
        lyricsAdapter.setLines(lyricLines);
        tvLyricsStatus.setVisibility(View.GONE);
        rvLyrics.setVisibility(View.VISIBLE);
    }

    private void displayPlainLyrics(String plain) {
        List<LyricsLine> lines = new ArrayList<>();
        for (String line : plain.split("\n")) lines.add(new LyricsLine(-1, line));
        lyricsAdapter.setLines(lines);
        tvLyricsStatus.setText("Plain lyrics (no sync)");
        rvLyrics.setVisibility(View.VISIBLE);
    }

    private void showLyricsError() {
        tvLyricsStatus.setVisibility(View.VISIBLE);
        tvLyricsStatus.setText(R.string.lyrics_not_found);
        rvLyrics.setVisibility(View.GONE);
    }

    private List<LyricsLine> parseLrc(String lrc) {
        List<LyricsLine> result = new ArrayList<>();
        Pattern p = Pattern.compile("\\[(\\d{2}):(\\d{2}\\.\\d+)](.*)");
        for (String line : lrc.split("\n")) {
            Matcher m = p.matcher(line.trim());
            if (m.matches()) {
                double total = Integer.parseInt(m.group(1)) * 60.0
                             + Double.parseDouble(m.group(2));
                String text  = m.group(3).trim();
                if (!text.isEmpty()) result.add(new LyricsLine(total, text));
            }
        }
        return result;
    }

    private void scrollToLyric(int index) {
        if (index < 0 || index >= lyricLines.size()) return;
        LinearSmoothScroller s = new LinearSmoothScroller(this) {
            @Override protected int getVerticalSnapPreference() { return SNAP_TO_ANY; }
        };
        s.setTargetPosition(index);
        lyricsLayoutManager.startSmoothScroll(s);
    }

    // ── Like (YouTube, optional) ──────────────────────────────────────────────

    private void toggleLike() {
        if (likeLoading) return;
        likeLoading = true;
        btnLike.setAlpha(0.4f);
        String rating = isLiked ? "none" : "like";
        RetrofitClient.getYouTubeService().rateVideo(videoId, rating)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c, Response<Void> r) {
                        likeLoading = false;
                        if (r.isSuccessful() || r.code() == 204) {
                            isLiked = !isLiked;
                            runOnUiThread(() -> {
                                updateLikeButton();
                                Toast.makeText(PlayerActivity.this,
                                        isLiked ? "Added to Liked songs"
                                                : "Removed from Liked songs",
                                        Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            runOnUiThread(() -> btnLike.setAlpha(1.0f));
                        }
                    }
                    @Override public void onFailure(Call<Void> c, Throwable t) {
                        likeLoading = false;
                        runOnUiThread(() -> btnLike.setAlpha(1.0f));
                    }
                });
    }

    private void updateLikeButton() {
        btnLike.setAlpha(1.0f);
        btnLike.setImageResource(
                isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatTime(long ms) {
        long secs = ms / 1000;
        return String.format(Locale.US, "%d:%02d", secs / 60, secs % 60);
    }
}
