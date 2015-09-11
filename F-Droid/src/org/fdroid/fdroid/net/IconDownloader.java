package org.fdroid.fdroid.net;

import android.content.Context;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

        Scheme scheme = Scheme.ofUri(imageUri);

        switch (scheme) {
            case HTTP:
            case HTTPS:
                Downloader downloader = DownloaderFactory.create(context, imageUri);
                return downloader.getInputStream();
        }

        //bluetooth isn't a scheme in the Scheme. library, so we can add a check here
        if (imageUri.toLowerCase().startsWith("bluetooth")) {
            Downloader downloader = DownloaderFactory.create(context, imageUri);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = downloader.getInputStream();

            int b;
            while ((b = is.read())!=-1)
                baos.write(b);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            downloader.close();

            return bais;

        }


        return super.getStream(imageUri, extra);

    }


}
