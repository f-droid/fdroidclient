package org.fdroid.fdroid.net.bluetooth.httpish;

import org.fdroid.fdroid.net.bluetooth.BluetoothConnection;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Request {


    public static interface Methods {
        public static final String HEAD = "HEAD";
        public static final String GET  = "GET";
    }

    private String method;
    private String path;
    private Map<String, String> headers;

    private BluetoothConnection connection;
    private BufferedWriter output;
    private BufferedReader input;

    private Request(String method, String path, BluetoothConnection connection) {
        this.method = method;
        this.path = path;
        this.connection = connection;
    }

    public static Request createHEAD(String path, BluetoothConnection connection)
    {
        return new Request(Methods.HEAD, path, connection);
    }

    public static Request createGET(String path, BluetoothConnection connection) {
        return new Request(Methods.GET, path, connection);
    }

    public Response send() throws IOException {

        output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        input  = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        output.write(method);
        output.write(' ');
        output.write(path);

        output.write("\n\n");

        int responseCode = readResponseCode();
        Map<String, String> headers = readHeaders();

        if (method.equals(Methods.HEAD)) {
            return new Response(responseCode, headers);
        } else {
            return new Response(responseCode, headers, connection.getInputStream());
        }

    }

    /**
     * Helper function used by listenForRequest().
     * The reason it is here is because the listenForRequest() is a static function, which would
     * need to instantiate it's own InputReaders from the bluetooth connection. However, we already
     * have that happening in a Request, so it is in some ways simpler to delegate to a member
     * method like this.
     */
    private boolean listen() throws IOException {

        String requestLine = input.readLine();

        if (requestLine == null || requestLine.trim().length() == 0)
            return false;

        String[] parts = requestLine.split("\\s+");

        // First part is the method (GET/HEAD), second is the path (/fdroid/repo/index.jar)
        if (parts.length < 2)
            return false;

        method  = parts[0].toUpperCase(Locale.ENGLISH);
        path    = parts[1];
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
     * First line of a HTTP response is the status line:
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1
     * The first part is the HTTP version, followed by a space, then the status code, then
     * a space, and then the status label (which may contain spaces).
     */
    private int readResponseCode() throws IOException {
        String line = input.readLine();
        if (line == null) {
            // TODO: What to do?
            return -1;
        }

        // TODO: Error handling
        int firstSpace = line.indexOf(' ');
        int secondSpace = line.indexOf(' ', firstSpace);

        String status = line.substring(firstSpace, secondSpace);
        return Integer.parseInt(status);
    }

    /**
     * Subsequent lines (after the status line) represent the headers, which are case
     * insensitive and may be multi-line. We don't deal with multi-line headers in
     * our HTTP-ish implementation.
     */
    private Map<String, String> readHeaders() throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        String responseLine = input.readLine();
        while (responseLine != null && responseLine.length() > 0) {

            // TODO: Error handling
            String[] parts = responseLine.split(":");
            String header = parts[0].trim();
            String value  = parts[1].trim();
            headers.put(header, value);
            responseLine = input.readLine();
        }
        return headers;
    }



}
