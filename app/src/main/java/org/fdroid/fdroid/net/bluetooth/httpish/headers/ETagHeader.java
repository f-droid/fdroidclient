package org.fdroid.fdroid.net.bluetooth.httpish.headers;

import org.fdroid.fdroid.net.bluetooth.FileDetails;

public class ETagHeader extends Header {

    @Override
    public String getName() {
        return "etag";
    }

    public void handle(FileDetails details, String value) {
        details.setCacheTag(value);
    }

}
