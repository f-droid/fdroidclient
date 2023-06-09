package org.fdroid.fdroid.net;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

import org.fdroid.download.DownloadRequest;
import org.fdroid.download.glide.DownloadRequestLoader;
import org.fdroid.fdroid.Preferences;

import java.io.InputStream;

/**
 * The one time initialization of Glide.
 */
@GlideModule
public class FDroidGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, GlideBuilder builder) {
        builder.setDefaultTransitionOptions(Drawable.class,
                        DrawableTransitionOptions.withCrossFade()).setDefaultTransitionOptions(Bitmap.class,
                        BitmapTransitionOptions.withCrossFade())
                .setDefaultRequestOptions(new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .onlyRetrieveFromCache(!Preferences.get().isBackgroundDownloadAllowed()));
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, Registry registry) {
        DownloadRequestLoader.Factory requestLoaderFactory =
                new DownloadRequestLoader.Factory(DownloaderFactory.HTTP_MANAGER);
        registry.append(DownloadRequest.class, InputStream.class, requestLoaderFactory);
    }
}
