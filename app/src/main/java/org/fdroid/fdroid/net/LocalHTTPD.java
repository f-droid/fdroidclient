package org.fdroid.fdroid.net;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import fi.iki.elonen.NanoHTTPD;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.LocalRepoKeyStore;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class LocalHTTPD extends NanoHTTPD {
    private static final String TAG = "LocalHTTPD";

    private final Context context;
    private final File webRoot;

    public LocalHTTPD(Context context, String hostname, int port, File webRoot, boolean useHttps) {
        super(hostname, port);
        this.webRoot = webRoot;
        this.context = context.getApplicationContext();
        if (useHttps) {
            enableHTTPS();
        }
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
     * instead of '+'.
     */
    private String encodeUriBetweenSlashes(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            switch (tok) {
                case "/":
                    newUri += "/";
                    break;
                case " ":
                    newUri += "%20";
                    break;
                default:
                    try {
                        newUri += URLEncoder.encode(tok, "UTF-8");
                    } catch (UnsupportedEncodingException ignored) {
                    }
                    break;
            }
        }
        return newUri;
    }

    private void requestSwap(String repo) {
        Utils.debugLog(TAG, "Received request to swap with " + repo);
        Utils.debugLog(TAG, "Showing confirm screen to check whether that is okay with the user.");

        Uri repoUri = Uri.parse(repo);
        Intent intent = new Intent(context, SwapWorkflowActivity.class);
        intent.setData(repoUri);
        intent.putExtra(SwapWorkflowActivity.EXTRA_CONFIRM, true);
        intent.putExtra(SwapWorkflowActivity.EXTRA_PREVENT_FURTHER_SWAP_REQUESTS, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public Response serve(IHTTPSession session) {

        if (session.getMethod() == Method.POST) {
            try {
                session.parseBody(new HashMap<String, String>());
            } catch (IOException e) {
                Log.e(TAG, "An error occured while parsing the POST body", e);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "Internal server error, check logcat on server for details.");
            } catch (ResponseException re) {
                return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }

            return handlePost(session);
        }
        return handleGet(session);
    }

    private Response handlePost(IHTTPSession session) {
        Uri uri = Uri.parse(session.getUri());
        switch (uri.getPath()) {
            case "/request-swap":
                if (!session.getParms().containsKey("repo")) {
                    Log.e(TAG, "Malformed /request-swap request to local repo HTTP server."
                            + " Should have posted a 'repo' parameter.");
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                            "Requires 'repo' parameter to be posted.");
                }
                requestSwap(session.getParms().get("repo"));
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Swap request received.");
        }
        return newFixedLengthResponse("");
    }

    private Response handleGet(IHTTPSession session) {

        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();

        if (BuildConfig.DEBUG) {
            Utils.debugLog(TAG, session.getMethod() + " '" + uri + "' ");

            Iterator<String> e = header.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                Utils.debugLog(TAG, "  HDR: '" + value + "' = '" + header.get(value) + "'");
            }
            e = parms.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                Utils.debugLog(TAG, "  PRM: '" + value + "' = '" + parms.get(value) + "'");
            }
        }

        if (!webRoot.isDirectory()) {
            return createResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                    "INTERNAL ERRROR: given path is not a directory (" + webRoot + ").");
        }

        return respond(Collections.unmodifiableMap(header), uri);
    }

    private void enableHTTPS() {
        try {
            LocalRepoKeyStore localRepoKeyStore = LocalRepoKeyStore.get(context);
            SSLServerSocketFactory factory = NanoHTTPD.makeSSLSocketFactory(
                    localRepoKeyStore.getKeyStore(),
                    localRepoKeyStore.getKeyManagers());
            makeSecure(factory, null);
        } catch (LocalRepoKeyStore.InitException | IOException e) {
            Log.e(TAG, "Could not enable HTTPS", e);
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
            return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
                    "FORBIDDEN: Won't serve ../ for security reasons.");
        }

        File f = new File(webRoot, uri);
        if (!f.exists()) {
            return createResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                    "Error 404, file not found.");
        }

        // Browsers get confused without '/' after the directory, send a
        // redirect.
        if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res = createResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML,
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
                    return createResponse(Response.Status.OK, NanoHTTPD.MIME_HTML,
                            listDirectory(uri, f));
                } else {
                    return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
                            "FORBIDDEN: No directory listing.");
                }
            } else {
                return respond(headers, uri + indexFile);
            }
        }

        Response response = serveFile(headers, f, getAndroidMimeTypeForFile(uri));
        return response != null ? response :
                createResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                        "Error 404, file not found.");
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    private Response serveFile(Map<String, String> header, File file, String mime) {
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
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE,
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

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis);
                    res.addHeader("Content-Length", String.valueOf(dataLen));
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/"
                            + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match"))) {
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                } else {
                    res = createResponse(Response.Status.OK, mime, new FileInputStream(file));
                    res.addHeader("Content-Length", String.valueOf(fileLen));
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
                    "FORBIDDEN: Reading file failed.");
        }

        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, InputStream message) {
        Response res = newChunkedResponse(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = newFixedLengthResponse(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private static String getAndroidMimeTypeForFile(String uri) {
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

    private String listDirectory(String uri, File f) {
        String heading = "Directory " + uri;
        StringBuilder msg = new StringBuilder("<html><head><title>" + heading
                + "</title><style><!--\n" +
                "span.dirname { font-weight: bold; }\n" +
                "span.filesize { font-size: 75%; }\n" +
                "// -->\n" +
                "</style>" +
                "</head><body><h1>" + heading + "</h1>");

        String up = null;
        if (uri.length() > 1) {
            String u = uri.substring(0, uri.length() - 1);
            int slash = u.lastIndexOf('/');
            if (slash >= 0 && slash < u.length()) {
                up = uri.substring(0, slash + 1);
            }
        }

        List<String> files = Arrays.asList(f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        }));
        Collections.sort(files);
        List<String> directories = Arrays.asList(f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        }));
        Collections.sort(directories);
        if (up != null || directories.size() + files.size() > 0) {
            msg.append("<ul>");
            if (up != null || directories.size() > 0) {
                msg.append("<section class=\"directories\">");
                if (up != null) {
                    msg.append("<li><a rel=\"directory\" href=\"").append(up)
                            .append("\"><span class=\"dirname\">..</span></a></b></li>");
                }
                for (String directory : directories) {
                    String dir = directory + "/";
                    msg.append("<li><a rel=\"directory\" href=\"")
                            .append(encodeUriBetweenSlashes(uri + dir))
                            .append("\"><span class=\"dirname\">").append(dir)
                            .append("</span></a></b></li>");
                }
                msg.append("</section>");
            }
            if (files.size() > 0) {
                msg.append("<section class=\"files\">");
                for (String file : files) {
                    msg.append("<li><a href=\"").append(encodeUriBetweenSlashes(uri + file))
                            .append("\"><span class=\"filename\">").append(file)
                            .append("</span></a>");
                    File curFile = new File(f, file);
                    long len = curFile.length();
                    msg.append("&nbsp;<span class=\"filesize\">(");
                    if (len < 1024) {
                        msg.append(len).append(" bytes");
                    } else if (len < 1024 * 1024) {
                        msg.append(len / 1024).append('.').append(len % 1024 / 10 % 100)
                                .append(" KB");
                    } else {
                        msg.append(len / (1024 * 1024)).append('.')
                                .append(len % (1024 * 1024) / 10 % 100).append(" MB");
                    }
                    msg.append(")</span></li>");
                }
                msg.append("</section>");
            }
            msg.append("</ul>");
        }
        msg.append("</body></html>");
        return msg.toString();
    }
}
