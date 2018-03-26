package org.fdroid.fdroid.net;

import android.content.Context;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class used by the Universal Image Loader library (UIL) to fetch images for
 * displaying in F-Droid.  A custom subclass is needed since F-Droid's
 * {@link HttpDownloader} provides support for Tor, proxying, and automatic
 * mirror failover.
 *
 * @see org.fdroid.fdroid.FDroidApp#onCreate()  for where this is setup
 */
public class ImageLoaderForUIL implements com.nostra13.universalimageloader.core.download.ImageDownloader {

    private final Context context;

    public ImageLoaderForUIL(Context context) {
        this.context = context;
    }

    @Override
    public InputStream getStream(String imageUri, Object extra) throws IOException {
        switch (Scheme.ofUri(imageUri)) {
            case HTTP:
            case HTTPS:
                return DownloaderFactory.create(context, imageUri).getInputStream();
        }
        return new BaseImageDownloader(context).getStream(imageUri, extra);
    }
}
