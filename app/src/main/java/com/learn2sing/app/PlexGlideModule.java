package com.learn2sing.app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.learn2sing.app.api.PlexRetrofitClient;

import java.io.InputStream;

/**
 * Replaces Glide's default HTTP stack with an OkHttp client that trusts
 * all SSL certificates.  This is required so Glide can load album art from
 * Plex servers accessed via raw IP (https://192.168.x.x:32400) whose
 * certificate would otherwise be rejected by Android's default trust store.
 */
@GlideModule
public class PlexGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(@NonNull Context context,
                                   @NonNull Glide glide,
                                   @NonNull Registry registry) {
        OkHttpUrlLoader.Factory factory =
                new OkHttpUrlLoader.Factory(PlexRetrofitClient.buildTrustAllClient());
        registry.replace(GlideUrl.class, InputStream.class, factory);
    }
}
