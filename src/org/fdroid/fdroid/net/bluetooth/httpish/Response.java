package org.fdroid.fdroid.net.bluetooth.httpish;

import org.fdroid.fdroid.net.bluetooth.FileDetails;
import org.fdroid.fdroid.net.bluetooth.httpish.headers.Header;

import java.io.InputStream;
import java.util.Map;

public class Response {

    private int statusCode;
    private Map<String, String> headers;
    private final InputStream contentStream;

    public Response(int statusCode, Map<String, String> headers) {
        this(statusCode, headers, null);
    }

    /**
     * This class expects 'contentStream' to be open, and ready for use.
     * It will not close it either. However it will block wile doing things
     * so you can call a method, wait for it to finish, and then close
     * it afterwards if you like.
     */
    public Response(int statusCode, Map<String, String> headers, InputStream contentStream) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.contentStream = contentStream;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Extracts meaningful headers from the response into a more useful and safe
     * {@link org.fdroid.fdroid.net.bluetooth.FileDetails} object.
     */
    public FileDetails toFileDetails() {
        FileDetails details = new FileDetails();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            Header.process(details, entry.getKey(), entry.getValue());
        }
        return details;
    }

    /**
     * After parsing a response,
     */
    public InputStream toContentStream() throws UnsupportedOperationException {
        if (contentStream == null) {
            throw new UnsupportedOperationException("This kind of response doesn't have a content stream. Did you perform a HEAD request instead of a GET request?");
        }
        return contentStream;
    }
}
