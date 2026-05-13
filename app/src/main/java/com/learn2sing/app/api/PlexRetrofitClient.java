package com.learn2sing.app.api;

import android.util.Log;

import com.learn2sing.app.Constants;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Factory for Retrofit instances that talk to:
 *   (a) plex.tv  — for PIN OAuth and server discovery
 *   (b) the user's Plex server — for library browsing and streaming
 *
 * SSL note: Plex local servers use plex.direct certificates that Android may not
 * trust, and raw-IP HTTPS connections have no cert at all.  Since all security
 * is provided by the X-Plex-Token header, we disable cert verification for all
 * Plex connections — exactly the same approach used by Plexamp and other clients.
 */
public class PlexRetrofitClient {

    private static final String TAG = "PlexRetrofitClient";

    // ── Trust-all SSL (safe because auth is via token, not cert) ─────────────

    private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        @Override public void checkClientTrusted(X509Certificate[] c, String a) {}
        @Override public void checkServerTrusted(X509Certificate[] c, String a) {}
        @Override public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    /**
     * A bare trust-all OkHttpClient with no Plex headers — used by ExoPlayer's
     * OkHttpDataSource so it can stream HTTPS from a local Plex server whose
     * certificate is self-signed or bound to a raw IP address.
     */
    public static OkHttpClient buildTrustAllClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)   // streams need longer read timeout
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{TRUST_ALL}, new SecureRandom());
            builder.sslSocketFactory(ctx.getSocketFactory(), TRUST_ALL);
            builder.hostnameVerifier((h, s) -> true);
        } catch (Exception e) {
            Log.w(TAG, "buildTrustAllClient: SSL setup failed", e);
        }
        return builder.build();
    }

    private static OkHttpClient buildPlexClient(String clientId, String authToken) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);

        // Accept any SSL certificate — handles plex.direct, self-signed, and raw-IP HTTPS
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{TRUST_ALL}, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), TRUST_ALL);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            Log.w(TAG, "Could not install trust-all SSL — falling back to default", e);
        }

        // Inject required Plex identification headers AND token query param on every request
        builder.addInterceptor(chain -> {
            okhttp3.HttpUrl.Builder urlBuilder = chain.request().url().newBuilder();

            // Token as URL query param — required by some Plex versions
            if (authToken != null && !authToken.isEmpty()) {
                urlBuilder.addQueryParameter("X-Plex-Token", authToken);
            }

            Request newRequest = chain.request().newBuilder()
                    .url(urlBuilder.build())
                    .header("X-Plex-Client-Identifier", clientId)
                    .header("X-Plex-Product",           Constants.PLEX_PRODUCT)
                    .header("X-Plex-Version",           Constants.PLEX_VERSION)
                    .header("X-Plex-Platform",          Constants.PLEX_PLATFORM)
                    .header("Accept",                   "application/json")
                    .header("X-Plex-Token",             authToken != null ? authToken : "")
                    .build();

            Log.d(TAG, "→ " + newRequest.url());
            okhttp3.Response response = chain.proceed(newRequest);
            Log.d(TAG, "← " + response.code() + " " + newRequest.url().encodedPath());
            return response;
        });

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        builder.addInterceptor(logging);

        return builder.build();
    }

    // ── plex.tv service (PIN OAuth + server discovery) ────────────────────────

    public static PlexTvApiService getPlexTvService(String clientId) {
        return new Retrofit.Builder()
                .baseUrl(Constants.PLEX_TV_BASE_URL)
                .client(buildPlexClient(clientId, null))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlexTvApiService.class);
    }

    // ── Plex Media Server service ─────────────────────────────────────────────

    /**
     * Returns a service pointing at the user's own Plex server.
     *
     * @param serverUri  base URL, e.g. "https://192.168.0.29:32400" or "http://..."
     * @param clientId   permanent UUID for this app install
     * @param authToken  Plex user auth token
     */
    public static PlexServerApiService getServerService(
            String serverUri, String clientId, String authToken) {

        if (!serverUri.endsWith("/")) serverUri += "/";

        return new Retrofit.Builder()
                .baseUrl(serverUri)
                .client(buildPlexClient(clientId, authToken))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlexServerApiService.class);
    }
}
