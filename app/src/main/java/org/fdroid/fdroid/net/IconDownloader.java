package org.fdroid.fdroid.net;

import android.content.Context;

import com.nostra13.universalimageloader.core.download.ImageDownloader;

import java.io.IOException;
import java.io.InputStream;

public class IconDownloader implements ImageDownloader {

    private final Context context;

    public IconDownloader(Context context) {
        this.context = context;
    }

    @Override
    public InputStream getStream(String imageUri, Object extra) throws IOException {
        switch (Scheme.ofUri(imageUri)) {
            case ASSETS:
                return context.getAssets().open(Scheme.ASSETS.crop(imageUri));
            default:
                return DownloaderFactory.create(context, imageUri).getInputStream();
        }
    }
}
