package com.learn2sing.app.api;

import android.content.Context;

import com.learn2sing.app.Constants;
import com.learn2sing.app.UserSession;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit clients.
 *
 * YouTube client automatically attaches "Authorization: Bearer {token}" to
 * every request using the signed-in user's OAuth access token.
 * No hard-coded API key is needed when a user is signed in.
 */
public class RetrofitClient {

    // Application context — set once in Application.onCreate() or MainActivity.onCreate()
    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    // ── YouTube ───────────────────────────────────────────────────────────────

    private static Retrofit youtubeRetrofit;

    public static YouTubeApiService getYouTubeService() {
        if (youtubeRetrofit == null) {
            youtubeRetrofit = new Retrofit.Builder()
                    .baseUrl(Constants.YOUTUBE_BASE_URL)
                    .client(buildYouTubeClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return youtubeRetrofit.create(YouTubeApiService.class);
    }

    /**
     * OkHttp client that injects the user's OAuth token as a Bearer header.
     * Falls back gracefully if no token is available (returns 401 from Google).
     *
     * The interceptor is called on a background thread by OkHttp, so the
     * blocking {@link UserSession#getFreshToken(Context)} call is safe here.
     */
    private static OkHttpClient buildYouTubeClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder();

                    // Attach OAuth token if a user is signed in
                    if (appContext != null && UserSession.getInstance().isSignedIn()) {
                        String token = UserSession.getInstance().getFreshToken(appContext);
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                    }

                    return chain.proceed(builder.build());
                })
                .addInterceptor(logging)
                .build();
    }

    // ── LRCLib ────────────────────────────────────────────────────────────────

    private static Retrofit lrcLibRetrofit;

    public static LyricsApiService getLyricsService() {
        if (lrcLibRetrofit == null) {
            lrcLibRetrofit = new Retrofit.Builder()
                    .baseUrl(Constants.LRCLIB_BASE_URL)
                    .client(buildBasicClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return lrcLibRetrofit.create(LyricsApiService.class);
    }

    private static OkHttpClient buildBasicClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }
}
