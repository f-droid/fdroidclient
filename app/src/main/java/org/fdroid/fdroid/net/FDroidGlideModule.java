package org.fdroid.fdroid.net;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;

import androidx.annotation.NonNull;

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
                        .onlyRetrieveFromCache(!Preferences.get().isBackgroundDownloadAllowed())
                        .timeout(FDroidApp.getTimeout()));
    }
}
