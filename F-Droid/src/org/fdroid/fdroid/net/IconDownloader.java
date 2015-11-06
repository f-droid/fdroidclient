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
        return DownloaderFactory.create(context, imageUri).getInputStream();
    }


}
