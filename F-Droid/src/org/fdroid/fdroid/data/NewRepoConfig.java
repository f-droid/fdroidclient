package org.fdroid.fdroid.data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.fdroid.fdroid.R;

import java.util.Arrays;
import java.util.Locale;

public class NewRepoConfig {

    private static final String TAG = "fdroid.NewRepoConfig";

    private String errorMessage;
    private boolean isValidRepo = false;

    private String uriString;
    private Uri uri;
    private String host;
    private int port = -1;
    private String scheme;
    private String fingerprint;
    private String bssid;
    private String ssid;
    private boolean fromSwap;

    public NewRepoConfig(Context context, String uri) {
        init(context, uri != null ? Uri.parse(uri) : null);
    }

    public NewRepoConfig(Context context, Intent intent) {
        init(context, intent.getData());
    }

    private void init(Context context, Uri incomingUri) {
        /* an URL from a click, NFC, QRCode scan, etc */
        uri = incomingUri;
        if (uri == null) {
            isValidRepo = false;
            return;
        }

        Log.d(TAG, "Parsing incoming intent looking for repo: " + incomingUri);

        // scheme and host should only ever be pure ASCII aka Locale.ENGLISH
        scheme = uri.getScheme();
        host = uri.getHost();
        port = uri.getPort();
        if (TextUtils.isEmpty(scheme) || TextUtils.isEmpty(host)) {
            errorMessage = String.format(context.getString(R.string.malformed_repo_uri), uri);
            isValidRepo = false;
            return;
        }

        if (Arrays.asList("FDROIDREPO", "FDROIDREPOS").contains(scheme)) {
            /*
             * QRCodes are more efficient in all upper case, so QR URIs are
             * encoded in all upper case, then forced to lower case. Checking if
             * the special F-Droid scheme being all is upper case means it
             * should be downcased.
             */
            uri = Uri.parse(uri.toString().toLowerCase(Locale.ENGLISH));
        } else if (uri.getPath().endsWith("/FDROID/REPO")) {
            /*
             * some QR scanners chop off the fdroidrepo:// and just try http://,
             * then the incoming URI does not get downcased properly, and the
             * query string is stripped off. So just downcase the path, and
             * carry on to get something working.
             */
            uri = Uri.parse(uri.toString().toLowerCase(Locale.ENGLISH));
        }

        // make scheme and host lowercase so they're readable in dialogs
        scheme = scheme.toLowerCase(Locale.ENGLISH);
        host = host.toLowerCase(Locale.ENGLISH);
        fingerprint = uri.getQueryParameter("fingerprint");
        bssid = uri.getQueryParameter("bssid");
        ssid = uri.getQueryParameter("ssid");
        fromSwap = uri.getQueryParameter("swap") != null;

        if (!Arrays.asList("fdroidrepos", "fdroidrepo", "https", "http").contains(scheme)) {
            isValidRepo = false;
            return;
        }

        uriString = sanitizeRepoUri(uri);
        isValidRepo = true;

    }

    public String getBssid() {
        return bssid;
    }

    public String getSsid() {
        return ssid;
    }

    public int getPort() {
        return port;
    }

    public String getUriString() {
        return uriString;
    }

    public Uri getUri() {
        return uri;
    }

    public String getHost() {
        return host;
    }

    public String getScheme() {
        return scheme;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public boolean isValidRepo() {
        return isValidRepo;
    }

    public boolean isFromSwap() {
        return fromSwap;
    }

    /*
     * The port starts out as 8888, but if there is a conflict, it will be
     * incremented until there is a free port found.
     */
    public boolean looksLikeLocalRepo() {
        return (port >= 8888 && host.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+"));
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Sanitize and format an incoming repo URI for function and readability */
    public static String sanitizeRepoUri(Uri uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        return uri.toString()
                .replaceAll("\\?.*$", "") // remove the whole query
                .replaceAll("/*$", "") // remove all trailing slashes
                .replace(host, host.toLowerCase(Locale.ENGLISH))
                .replace(scheme, scheme.toLowerCase(Locale.ENGLISH))
                .replace("fdroidrepo", "http") // proper repo address
                .replace("/FDROID/REPO", "/fdroid/repo"); // for QR FDroid path
    }
}
