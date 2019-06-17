package org.fdroid.fdroid.nearby.httpish;

public class ETagHeader extends Header {

    @Override
    public String getName() {
        return "etag";
    }

    public void handle(FileDetails details, String value) {
        details.setCacheTag(value);
    }

}
