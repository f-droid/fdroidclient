package org.fdroid.fdroid.net.bluetooth.httpish;

import android.util.Log;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.bluetooth.BluetoothConnection;
import org.fdroid.fdroid.net.bluetooth.FileDetails;
import org.fdroid.fdroid.net.bluetooth.httpish.headers.Header;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Response {

    private static final String TAG = "bluetooth.Response";

    private final int statusCode;
    private final Map<String, String> headers;
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

    public Response(int statusCode, String mimeType, String content) {
        this.statusCode = statusCode;
        this.headers = new HashMap<>();
        this.headers.put("Content-Type", mimeType);
        try {
            this.contentStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // Not quite sure what to do in the case of a phone not supporting UTF-8, so lets
            // throw a runtime exception and hope that we get good bug reports if this ever happens.
            Log.e(TAG, "Device does not support UTF-8", e);
            throw new IllegalStateException("Device does not support UTF-8.", e);
        }
    }

    public Response(int statusCode, String mimeType, InputStream contentStream) {
        this.statusCode = statusCode;
        this.headers = new HashMap<>();
        this.headers.put("Content-Type", mimeType);
        this.contentStream = contentStream;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getFileSize() {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("content-length".equals(entry.getKey().toLowerCase(Locale.ENGLISH))) {
                    try {
                        return Integer.parseInt(entry.getValue());
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                }
            }
        }
        return -1;
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

    public InputStream toContentStream() throws UnsupportedOperationException {
        if (contentStream == null) {
            throw new UnsupportedOperationException("This kind of response doesn't have a content stream."
                    + " Did you perform a HEAD request instead of a GET request?");
        }
        return contentStream;
    }

    public void send(BluetoothConnection connection) throws IOException {

        Utils.debugLog(TAG, "Sending Bluetooth HTTP-ish response...");

        Writer output = new OutputStreamWriter(connection.getOutputStream());
        output.write("HTTP(ish)/0.1 200 OK\n");

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            output.write(entry.getKey());
            output.write(": ");
            output.write(entry.getValue());
            output.write("\n");
        }

        output.write("\n");
        output.flush();

        if (contentStream != null) {
            Utils.copy(contentStream, connection.getOutputStream());
        }

        output.flush();

    }

    public static class Builder {

        private InputStream contentStream;
        private int statusCode = 200;
        private int fileSize = -1;
        private String etag;

        public Builder() {
        }

        public Builder(InputStream contentStream) {
            this.contentStream = contentStream;
        }

        public Builder setStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder setFileSize(int fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder setETag(String etag) {
            this.etag = etag;
            return this;
        }

        public Response build() {

            Map<String, String> headers = new HashMap<>(3);

            if (fileSize > 0) {
                headers.put("Content-Length", Integer.toString(fileSize));
            }

            if (etag != null) {
                headers.put("ETag", etag);
            }

            return new Response(statusCode, headers, contentStream);
        }

    }
}
