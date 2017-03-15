package org.fdroid.fdroid.net;

import android.content.Context;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class used by the Universal Image Loader library (UIL) to fetch images for displaying in F-Droid.
 * See {@link org.fdroid.fdroid.FDroidApp} for where this gets configured.
 */
public class ImageLoaderForUIL implements com.nostra13.universalimageloader.core.download.ImageDownloader {

    private final Context context;

    public ImageLoaderForUIL(Context context) {
        this.context = context;
    }

    @Override
    public InputStream getStream(String imageUri, Object extra) throws IOException {
        switch (Scheme.ofUri(imageUri)) {
            case ASSETS:
                return context.getAssets().open(Scheme.ASSETS.crop(imageUri));

            case DRAWABLE:
                return new BaseImageDownloader(context).getStream(imageUri, extra);

            default:
                return DownloaderFactory.create(context, imageUri).getInputStream();
        }
    }

}
