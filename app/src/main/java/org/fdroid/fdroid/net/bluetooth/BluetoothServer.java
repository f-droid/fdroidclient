package org.fdroid.fdroid.net.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.type.BluetoothSwap;
import org.fdroid.fdroid.net.bluetooth.httpish.Request;
import org.fdroid.fdroid.net.bluetooth.httpish.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Act as a layer on top of LocalHTTPD server, by forwarding requests served
 * over bluetooth to that server.
 */
@SuppressWarnings("LineLength")
public class BluetoothServer extends Thread {

    private static final String TAG = "BluetoothServer";

    private BluetoothServerSocket serverSocket;
    private final List<ClientConnection> clients = new ArrayList<>();

    private final File webRoot;
    private final BluetoothSwap swap;
    private boolean isRunning;

    public BluetoothServer(BluetoothSwap swap, File webRoot) {
        this.webRoot = webRoot;
        this.swap = swap;

        start();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void close() {

        for (ClientConnection clientConnection : clients) {
            clientConnection.interrupt();
        }

        interrupt();

        if (serverSocket != null) {
            Utils.closeQuietly(serverSocket);
        }
    }

    @Override
    public void run() {

        isRunning = true;
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        try {
            serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord("FDroid App Swap", BluetoothConstants.fdroidUuid());
        } catch (IOException e) {
            Log.e(TAG, "Error starting Bluetooth server socket, will stop the server now", e);
            swap.stop();
            isRunning = false;
            return;
        }

        while (true) {
            if (isInterrupted()) {
                Utils.debugLog(TAG, "Server stopped so will terminate loop looking for client connections.");
                break;
            }

            if (!adapter.isEnabled()) {
                Utils.debugLog(TAG, "User disabled Bluetooth from outside, stopping.");
                break;
            }

            try {
                BluetoothSocket clientSocket = serverSocket.accept();
                if (clientSocket != null) {
                    if (isInterrupted()) {
                        Utils.debugLog(TAG, "Server stopped after socket accepted from client, but before initiating connection.");
                        break;
                    }
                    ClientConnection client = new ClientConnection(clientSocket, webRoot);
                    client.start();
                    clients.add(client);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error receiving client connection over Bluetooth server socket, will continue listening for other clients", e);
            }
        }
        isRunning = false;
    }

    private static class ClientConnection extends Thread {

        private final BluetoothSocket socket;
        private final File webRoot;

        ClientConnection(BluetoothSocket socket, File webRoot) {
            this.socket = socket;
            this.webRoot = webRoot;
        }

        @Override
        public void run() {

            Utils.debugLog(TAG, "Listening for incoming Bluetooth requests from client");

            BluetoothConnection connection;
            try {
                connection = new BluetoothConnection(socket);
                connection.open();
            } catch (IOException e) {
                Log.e(TAG, "Error listening for incoming connections over bluetooth", e);
                return;
            }

            while (true) {

                try {
                    Utils.debugLog(TAG, "Listening for new Bluetooth request from client.");
                    Request incomingRequest = Request.listenForRequest(connection);
                    handleRequest(incomingRequest).send(connection);
                } catch (IOException e) {
                    Log.e(TAG, "Error receiving incoming connection over bluetooth", e);
                    break;
                }

                if (isInterrupted()) {
                    break;
                }
            }

            connection.closeQuietly();

        }

        private Response handleRequest(Request request) {

            Utils.debugLog(TAG, "Received Bluetooth request from client, will process it now.");

            Response.Builder builder = null;

            try {
                int statusCode = 404;
                int totalSize = -1;

                if (request.getMethod().equals(Request.Methods.HEAD)) {
                    builder = new Response.Builder();
                } else {
                    HashMap<String, String> headers = new HashMap<>();
                    Response resp = respond(headers, "/" + request.getPath());

                    builder = new Response.Builder(resp.toContentStream());
                    statusCode = resp.getStatusCode();
                    totalSize = resp.getFileSize();
                }

                // TODO: At this stage, will need to download the file to get this info.
                // However, should be able to make totalDownloadSize and getCacheTag work without downloading.
                return builder
                        .setStatusCode(statusCode)
                        .setFileSize(totalSize)
                        .build();

            } catch (Exception e) {
                // throw new IOException("Error getting file " + request.getPath() + " from local repo proxy - " + e.getMessage(), e);

                Log.e(TAG, "error processing request; sending 500 response", e);

                if (builder == null) {
                    builder = new Response.Builder();
                }

                return builder
                        .setStatusCode(500)
                        .setFileSize(0)
                        .build();

            }

        }

        private Response respond(Map<String, String> headers, String uri) {
            // Remove URL arguments
            uri = uri.trim().replace(File.separatorChar, '/');
            if (uri.indexOf('?') >= 0) {
                uri = uri.substring(0, uri.indexOf('?'));
            }

            // Prohibit getting out of current directory
            if (uri.contains("../")) {
                return createResponse(NanoHTTPD.Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
                        "FORBIDDEN: Won't serve ../ for security reasons.");
            }

            File f = new File(webRoot, uri);
            if (!f.exists()) {
                return createResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                        "Error 404, file not found.");
            }

            // Browsers get confused without '/' after the directory, send a
            // redirect.
            if (f.isDirectory() && !uri.endsWith("/")) {
                uri += "/";
                Response res = createResponse(NanoHTTPD.Response.Status.REDIRECT, NanoHTTPD.MIME_HTML,
                        "<html><body>Redirected: <a href=\"" +
                                uri + "\">" + uri + "</a></body></html>");
                res.addHeader("Location", uri);
                return res;
            }

            if (f.isDirectory()) {
                // First look for index files (index.html, index.htm, etc) and if
                // none found, list the directory if readable.
                String indexFile = findIndexFileInDirectory(f);
                if (indexFile == null) {
                    if (f.canRead()) {
                        // No index file, list the directory if it is readable
                        return createResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "");
                    }
                    return createResponse(NanoHTTPD.Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
                            "FORBIDDEN: No directory listing.");
                }
                return respond(headers, uri + indexFile);
            }

            Response response = serveFile(uri, headers, f, getMimeTypeForFile(uri));
            return response != null ? response :
                    createResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                            "Error 404, file not found.");
        }

        /**
         * Serves file from homeDir and its' subdirectories (only). Uses only URI,
         * ignores all headers and HTTP parameters.
         */
        Response serveFile(String uri, Map<String, String> header, File file, String mime) {
            Response res;
            try {
                // Calculate etag
                String etag = Integer
                        .toHexString((file.getAbsolutePath() + file.lastModified() + String.valueOf(file.length()))
                                .hashCode());

                // Support (simple) skipping:
                long startFrom = 0;
                long endAt = -1;
                String range = header.get("range");
                if (range != null && range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                // Change return code and add Content-Range header when skipping is
                // requested
                long fileLen = file.length();
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = createResponse(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE,
                                NanoHTTPD.MIME_PLAINTEXT, "");
                        res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                        res.addHeader("ETag", etag);
                    } else {
                        if (endAt < 0) {
                            endAt = fileLen - 1;
                        }
                        long newLen = endAt - startFrom + 1;
                        if (newLen < 0) {
                            newLen = 0;
                        }

                        final long dataLen = newLen;
                        FileInputStream fis = new FileInputStream(file) {
                            @Override
                            public int available() throws IOException {
                                return (int) dataLen;
                            }
                        };
                        long skipped = fis.skip(startFrom);
                        if (skipped != startFrom) {
                            throw new IOException("unable to skip the required " + startFrom + " bytes.");
                        }

                        res = createResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, mime, fis);
                        res.addHeader("Content-Length", String.valueOf(dataLen));
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/"
                                + fileLen);
                        res.addHeader("ETag", etag);
                    }
                } else {
                    if (etag.equals(header.get("if-none-match"))) {
                        res = createResponse(NanoHTTPD.Response.Status.NOT_MODIFIED, mime, "");
                    } else {
                        res = createResponse(NanoHTTPD.Response.Status.OK, mime, new FileInputStream(file));
                        res.addHeader("Content-Length", String.valueOf(fileLen));
                        res.addHeader("ETag", etag);
                    }
                }
            } catch (IOException ioe) {
                res = createResponse(NanoHTTPD.Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
                        "FORBIDDEN: Reading file failed.");
            }

            return res;
        }

        // Announce that the file server accepts partial content requests
        private Response createResponse(NanoHTTPD.Response.Status status, String mimeType, String content) {
            return new Response(status.getRequestStatus(), mimeType, content);
        }

        // Announce that the file server accepts partial content requests
        private Response createResponse(NanoHTTPD.Response.Status status, String mimeType, InputStream content) {
            return new Response(status.getRequestStatus(), mimeType, content);
        }

        public static String getMimeTypeForFile(String uri) {
            String type = null;
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri);
            if (extension != null) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                type = mime.getMimeTypeFromExtension(extension);
            }
            return type;
        }

        private String findIndexFileInDirectory(File directory) {
            String indexFileName = "index.html";
            File indexFile = new File(directory, indexFileName);
            if (indexFile.exists()) {
                return indexFileName;
            }
            return null;
        }
    }

}
