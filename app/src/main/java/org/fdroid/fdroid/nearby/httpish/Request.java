package org.fdroid.fdroid.nearby.httpish;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.nearby.BluetoothConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Request {

    private static final String TAG = "bluetooth.Request";

    public interface Methods {
        String HEAD = "HEAD";
        String GET = "GET";
    }

    private String method;
    private String path;
    private Map<String, String> headers;

    private final BluetoothConnection connection;
    private final Writer output;
    private final InputStream input;

    private Request(String method, String path, BluetoothConnection connection) {
        this.method = method;
        this.path = path;
        this.connection = connection;

        output = new OutputStreamWriter(connection.getOutputStream());
        input = connection.getInputStream();
    }

    public static Request createHEAD(String path, BluetoothConnection connection) {
        return new Request(Methods.HEAD, path, connection);
    }

    public static Request createGET(String path, BluetoothConnection connection) {
        return new Request(Methods.GET, path, connection);
    }

    public String getHeaderValue(String header) {
        return headers.containsKey(header) ? headers.get(header) : null;
    }

    public Response send() throws IOException {

        Utils.debugLog(TAG, "Sending request to server (" + path + ")");

        output.write(method);
        output.write(' ');
        output.write(path);

        output.write("\n\n");

        output.flush();

        Utils.debugLog(TAG, "Finished sending request, now attempting to read response status code...");

        int responseCode = readResponseCode();

        Utils.debugLog(TAG, "Read response code " + responseCode + " from server, now reading headers...");

        Map<String, String> headers = readHeaders();

        Utils.debugLog(TAG, "Read " + headers.size() + " headers");

        if (method.equals(Methods.HEAD)) {
            Utils.debugLog(TAG, "Request was a " + Methods.HEAD
                    + " request, not including anything other than headers and status...");
            return new Response(responseCode, headers);
        }
        Utils.debugLog(TAG, "Request was a " + Methods.GET
                + " request, so including content stream in response...");
        return new Response(responseCode, headers, connection.getInputStream());
    }

    /**
     * Helper function used by listenForRequest().
     * The reason it is here is because the listenForRequest() is a static function, which would
     * need to instantiate it's own InputReaders from the bluetooth connection. However, we already
     * have that happening in a Request, so it is in some ways simpler to delegate to a member
     * method like this.
     */
    private boolean listen() throws IOException {

        String requestLine = readLine();

        if (requestLine == null || requestLine.trim().length() == 0) {
            return false;
        }

        String[] parts = requestLine.split("\\s+");

        // First part is the method (GET/HEAD), second is the path (/fdroid/repo/index.jar)
        if (parts.length < 2) {
            return false;
        }

        method = parts[0].toUpperCase(Locale.ENGLISH);
        path = parts[1];
        headers = readHeaders();
        return true;
    }

    /**
     * This is a blocking method, which will wait until a full Request is received.
     */
    public static Request listenForRequest(BluetoothConnection connection) throws IOException {
        Request request = new Request("", "", connection);
        return request.listen() ? request : null;
    }

    /**
     * First line of a HTTP 1.1 response is the status line:
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1
     * The first part is the HTTP version, followed by a space, then the status code, then
     * a space, and then the status label (which may contain spaces).
     */
    private int readResponseCode() throws IOException {

        String line = readLine();

        // TODO: Error handling
        int firstSpace = line.indexOf(' ');
        int secondSpace = line.indexOf(' ', firstSpace + 1);

        String status = line.substring(firstSpace + 1, secondSpace);
        return Integer.parseInt(status);
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String line = null;

        while (line == null) {

            while (input.available() > 0) {

                int b = input.read();

                if (((char) b) == '\n') {
                    if (baos.size() > 0) {
                        line = baos.toString();
                    }

                    return line;
                }

                baos.write(b);
            }

            try {
                Thread.sleep(100);
            } catch (Exception e) {
                // ignore
            }
        }

        return line;
    }

    /**
     * Subsequent lines (after the status line) represent the headers, which are case
     * insensitive and may be multi-line. We don't deal with multi-line headers in
     * our HTTP-ish implementation.
     */
    private Map<String, String> readHeaders() throws IOException {
        Map<String, String> headers = new HashMap<>();
        String responseLine = readLine();
        while (responseLine != null) {

            // TODO: Error handling
            String[] parts = responseLine.split(":");
            if (parts.length > 1) {
                String header = parts[0].trim();
                String value = parts[1].trim();
                headers.put(header, value);
            }

            if (input.available() > 0) {
                responseLine = readLine();
            } else {
                break;
            }

        }
        return headers;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

}
