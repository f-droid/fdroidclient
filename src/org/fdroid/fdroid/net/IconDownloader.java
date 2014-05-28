
package org.fdroid.fdroid.net;

import android.content.Context;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.IOException;
import java.io.InputStream;

public class IconDownloader extends BaseImageDownloader {

    public IconDownloader(Context context) {
        super(context);
    }

    public IconDownloader(Context context, int connectTimeout, int readTimeout) {
        super(context, connectTimeout, readTimeout);
    }

    @Override
    public InputStream getStream(String imageUri, Object extra) throws IOException {
        switch (Scheme.ofUri(imageUri)) {
            case HTTP:
            case HTTPS:
                Downloader downloader = DownloaderFactory.create(imageUri, context);
                return downloader.getInputStream();
            default:
                return super.getStream(imageUri, extra);
        }
    }
}
