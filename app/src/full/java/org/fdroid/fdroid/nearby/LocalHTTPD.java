package org.fdroid.fdroid.nearby;

/*
 * #%L
 * NanoHttpd-Webserver
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import android.content.Context;
import android.net.Uri;

import org.fdroid.fdroid.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;

/**
 * A HTTP server for serving the files that are being swapped via WiFi, etc.
 * The only changes were to remove unneeded extras like {@code main()}, the
 * plugin interface, and custom CORS header manipulation.
 * <p>
 * This is mostly just synced from {@code SimpleWebServer.java} from NanoHTTPD.
 *
 * @see
 * <a href="https://github.com/NanoHttpd/nanohttpd/blob/nanohttpd-project-2.3.1/webserver/src/main/java/fi/iki/elonen/SimpleWebServer.java">webserver/src/main/java/fi/iki/elonen/SimpleWebServer.java</a>
 */
public class LocalHTTPD extends NanoHTTPD {
    private static final String TAG = "LocalHTTPD";

    /**
     * Default Index file names.
     */
    public static final String[] INDEX_FILE_NAMES = {"index.html"};

    private final WeakReference<Context> context;

    protected List<File> rootDirs;

    // Date format specified by RFC 7231 section 7.1.1.1.
    private static final DateFormat RFC_1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);

    static {
        RFC_1123.setLenient(false);
        RFC_1123.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Configure and start the webserver.  This also sets the MIME Types only
     * for files that should be downloadable when a browser is used to display
     * the swap repo, rather than the F-Droid client.  The other file types
     * should not be added because it could expose exploits to the browser.
     */
    public LocalHTTPD(Context context, String hostname, int port, File webRoot, boolean useHttps) {
        super(hostname, port);
        rootDirs = Collections.singletonList(webRoot);
        this.context = new WeakReference<>(context.getApplicationContext());
        if (useHttps) {
            enableHTTPS();
        }
        MIME_TYPES = new HashMap<>(); // ignore nanohttpd's list
        MIME_TYPES.put("apk", "application/vnd.android.package-archive");
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("xml", "application/xml");
    }

    private boolean canServeUri(String uri, File homeDir) {
        boolean canServeUri;
        File f = new File(homeDir, uri);
        canServeUri = f.exists();
        return canServeUri;
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
     * instead of '+'.
     */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if ("/".equals(tok)) {
                newUri += "/";
            } else if (" ".equals(tok)) {
                newUri += "%20";
            } else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri;
    }

    private String findIndexFileInDirectory(File directory) {
        for (String fileName : LocalHTTPD.INDEX_FILE_NAMES) {
            File indexFile = new File(directory, fileName);
            if (indexFile.isFile()) {
                return fileName;
            }
        }
        return null;
    }

    protected Response getForbiddenResponse(String s) {
        return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                "INTERNAL ERROR: " + s);
    }

    protected Response getNotFoundResponse() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not " +
                "found.");
    }

    protected String listDirectory(String uri, File f) {
        String heading = "Directory " + uri;
        StringBuilder msg =
                new StringBuilder("<html><head><title>" + heading + "</title><style><!--\n" + "span.dirname { " +
                        "font-weight: bold; }\n" + "span.filesize { font-size: 75%; }\n"
                        + "// -->\n" + "</style>" + "</head><body><h1>" + heading + "</h1>");

        String up = null;
        if (uri.length() > 1) {
            String u = uri.substring(0, uri.length() - 1);
            int slash = u.lastIndexOf('/');
            if (slash >= 0 && slash < u.length()) {
                up = uri.substring(0, slash + 1);
            }
        }

        List<String> files =
                Arrays.asList(Objects.requireNonNull(f.list((dir, name) -> new File(dir, name).isFile())));
        Collections.sort(files);
        List<String> directories =
                Arrays.asList(Objects.requireNonNull(f.list((dir, name) -> new File(dir, name).isDirectory())));
        Collections.sort(directories);
        if (up != null || directories.size() + files.size() > 0) {
            msg.append("<ul>");
            if (up != null || directories.size() > 0) {
                msg.append("<section class=\"directories\">");
                if (up != null) {
                    msg.append("<li><a rel=\"directory\" href=\"").append(up).append("\"><span class=\"dirname\">." +
                            ".</span></a></li>");
                }
                for (String directory : directories) {
                    String dir = directory + "/";
                    msg.append("<li><a rel=\"directory\" href=\"").append(encodeUri(uri + dir)).append("\"><span " +
                            "class=\"dirname\">").append(dir).append("</span></a></li>");
                }
                msg.append("</section>");
            }
            if (files.size() > 0) {
                msg.append("<section class=\"files\">");
                for (String file : files) {
                    msg.append("<li><a href=\"").append(encodeUri(uri + file)).append("\"><span class=\"filename" +
                            "\">").append(file).append("</span></a>");
                    File curFile = new File(f, file);
                    long len = curFile.length();
                    msg.append("&nbsp;<span class=\"filesize\">(");
                    if (len < 1024) {
                        msg.append(len).append(" bytes");
                    } else if (len < 1024 * 1024) {
                        msg.append(len / 1024).append(".").append(len % 1024 / 10 % 100).append(" KB");
                    } else {
                        msg.append(len / (1024 * 1024)).append(".").append(len % (1024 * 1024) / 10000 % 100).append(" MB");
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

    /**
     * {@link Response#setKeepAlive(boolean)} alone does not seem to stop
     * setting the {@code Connection} header to {@code keep-alive}, so also
     * just directly set that header.
     */
    public static Response addResponseHeaders(Response response) {
        response.setKeepAlive(false);
        response.setGzipEncoding(false);
        response.addHeader("Connection", "close");
        response.addHeader("Content-Security-Policy",
                "default-src 'none'; img-src 'self'; style-src 'self' 'unsafe-inline';");
        return response;
    }

    public static Response newFixedLengthResponse(String msg) {
        return addResponseHeaders(NanoHTTPD.newFixedLengthResponse(msg));
    }

    public static Response newFixedLengthResponse(Response.IStatus status, String mimeType,
                                                  InputStream data, long totalBytes) {
        return addResponseHeaders(NanoHTTPD.newFixedLengthResponse(status, mimeType, data, totalBytes));
    }

    public static Response newFixedLengthResponse(IStatus status, String mimeType, String message) {
        Response response = NanoHTTPD.newFixedLengthResponse(status, mimeType, message);
        addResponseHeaders(response);
        response.addHeader("Accept-Ranges", "bytes");
        return response;
    }

    private Response respond(Map<String, String> headers, IHTTPSession session, String uri) {
        return defaultRespond(headers, session, uri);
    }

    private Response defaultRespond(Map<String, String> headers, IHTTPSession session, String uri) {
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.contains("../")) {
            return getForbiddenResponse("Won't serve ../ for security reasons.");
        }

        boolean canServeUri = false;
        File homeDir = null;
        for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
            homeDir = this.rootDirs.get(i);
            canServeUri = canServeUri(uri, homeDir);
        }
        if (!canServeUri) {
            return getNotFoundResponse();
        }

        // Browsers get confused without '/' after the directory, send a
        // redirect.
        File f = new File(homeDir, uri);
        if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res =
                    newFixedLengthResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: " +
                            "<a href=\"" + uri + "\">" + uri + "</a></body></html>");
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
                    return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, listDirectory(uri, f));
                } else {
                    return getForbiddenResponse("No directory listing.");
                }
            } else {
                return respond(headers, session, uri + indexFile);
            }
        }
        String mimeTypeForFile = getMimeTypeForFile(uri);
        Response response = serveFile(uri, headers, f, mimeTypeForFile);
        return response != null ? response : getNotFoundResponse();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();

        if (BuildConfig.DEBUG) {
            System.out.println(session.getMethod() + " '" + uri + "' ");

            Iterator<String> e = header.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                System.out.println("  HDR: '" + value + "' = '" + header.get(value) + "'");
            }
            e = parms.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                System.out.println("  PRM: '" + value + "' = '" + parms.get(value) + "'");
            }
        }

        if (session.getMethod() == Method.POST) {
            try {
                session.parseBody(new HashMap<>());
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "Internal server error, check logcat on server for details.");
            } catch (ResponseException re) {
                return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }

            return handlePost(session);
        }

        for (File homeDir : this.rootDirs) {
            // Make sure we won't die of an exception later
            if (!homeDir.isDirectory()) {
                return getInternalErrorResponse("given path is not a directory (" + homeDir + ").");
            }
        }
        return respond(Collections.unmodifiableMap(header), session, uri);
    }

    private Response handlePost(IHTTPSession session) {
        Uri uri = Uri.parse(session.getUri());
        switch (uri.getPath()) {
            case "/request-swap":
                if (!session.getParms().containsKey("repo")) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                            "Requires 'repo' parameter to be posted.");
                }
                SwapWorkflowActivity.requestSwap(context.get(), session.getParms().get("repo"));
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Swap request received.");
        }
        return newFixedLengthResponse("");
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        Response res;
        try {
            // Calculate etag
            String etag =
                    Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
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
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            String ifRange = header.get("if-range");
            boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

            String ifNoneMatch = header.get("if-none-match");
            boolean headerIfNoneMatchPresentAndMatching =
                    ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    FileInputStream fis = new FileInputStream(file);
                    fis.skip(startFrom);

                    res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, fis, newLen);
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                    res.addHeader("Last-Modified", RFC_1123.format(new Date(file.lastModified())));
                }
            } else {

                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes */" + fileLen);
                    res.addHeader("ETag", etag);
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified

                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    // supply the file
                    res = newFixedFileResponse(file, mime);
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                    res.addHeader("Last-Modified", RFC_1123.format(new Date(file.lastModified())));
                }
            }
        } catch (IOException ioe) {
            res = getForbiddenResponse("Reading file failed.");
        }

        return addResponseHeaders(res);
    }

    private Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
        Response res;
        res = newFixedLengthResponse(Response.Status.OK, mime, new FileInputStream(file), (int) file.length());
        addResponseHeaders(res);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private void enableHTTPS() {
        try {
            LocalRepoKeyStore localRepoKeyStore = LocalRepoKeyStore.get(context.get());
            SSLServerSocketFactory factory = NanoHTTPD.makeSSLSocketFactory(
                    localRepoKeyStore.getKeyStore(),
                    localRepoKeyStore.getKeyManagers());
            makeSecure(factory, null);
        } catch (LocalRepoKeyStore.InitException | IOException e) {
            e.printStackTrace();
        }
    }
}
