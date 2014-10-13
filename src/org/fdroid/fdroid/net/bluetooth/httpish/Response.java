package org.fdroid.fdroid.net.bluetooth.httpish;

import android.util.Log;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.bluetooth.BluetoothConnection;
import org.fdroid.fdroid.net.bluetooth.FileDetails;
import org.fdroid.fdroid.net.bluetooth.httpish.headers.Header;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class Response {

    private static final String TAG = "org.fdroid.fdroid.net.bluetooth.httpish.Response";

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

    public InputStream toContentStream() throws UnsupportedOperationException {
        if (contentStream == null) {
            throw new UnsupportedOperationException("This kind of response doesn't have a content stream. Did you perform a HEAD request instead of a GET request?");
        }
        return contentStream;
    }

    public void send(BluetoothConnection connection) throws IOException {

        Log.d(TAG, "Sending Bluetooth HTTP-ish response...");

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

    public static class ResponseBuilder {

        private InputStream contentStream;
        private int statusCode = 200;
        private int fileSize = -1;
        private String etag = null;

        public ResponseBuilder() {}

        public ResponseBuilder(InputStream contentStream) {
            this.contentStream = contentStream;
        }

        public ResponseBuilder setStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public ResponseBuilder setFileSize(int fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public ResponseBuilder setETag(String etag) {
            this.etag = etag;
            return this;
        }

        public Response build() {

            Map<String, String> headers = new HashMap<>(3);

            if (fileSize > 0) {
                headers.put("Content-Length", Integer.toString(fileSize));
            }

            if (etag != null) {
                headers.put( "ETag", etag);
            }

            return new Response(statusCode, headers, contentStream);
        }

    }
}
