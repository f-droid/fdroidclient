package org.fdroid.fdroid.net.bluetooth.httpish.headers;

import org.fdroid.fdroid.net.bluetooth.FileDetails;

public class ContentLengthHeader extends Header {

    @Override
    public String getName() {
        return "content-length";
    }

    public void handle(FileDetails details, String value) {
        details.setFileSize(Integer.parseInt(value));
    }

}