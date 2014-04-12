package org.fdroid.fdroid.net.bluetooth.httpish;

import org.fdroid.fdroid.net.bluetooth.BluetoothClient;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Request {

    public static interface Methods {
        public static final String HEAD = "HEAD";
        public static final String GET  = "GET";
    }

    private final BluetoothClient client;
    private final String method;

    private BluetoothClient.Connection connection;
    private BufferedWriter output;
    private BufferedReader input;

    public Request(String method, BluetoothClient client) {
        this.method = method;
        this.client = client;
    }

    public Response send() throws IOException {

        connection = client.openConnection();
        output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        input  = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        output.write(method);

        int responseCode = readResponseCode();
        Map<String, String> headers = readHeaders();

        if (method.equals(Methods.HEAD)) {
            return new Response(responseCode, headers);
        } else {
            return new Response(responseCode, headers, connection.getInputStream());
        }

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
