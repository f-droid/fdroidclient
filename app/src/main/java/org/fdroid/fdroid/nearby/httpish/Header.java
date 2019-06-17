package org.fdroid.fdroid.nearby.httpish;

import java.util.Locale;

public abstract class Header {

    private static final Header[] VALID_HEADERS = {
            new ContentLengthHeader(),
            new ETagHeader(),
    };

    protected abstract String getName();

    protected abstract void handle(FileDetails details, String value);

    public static void process(FileDetails details, String header, String value) {
        header = header.toLowerCase(Locale.ENGLISH);
        for (Header potentialHeader : VALID_HEADERS) {
            if (potentialHeader.getName().equals(header)) {
                potentialHeader.handle(details, value);
                break;
            }
        }
    }

}
