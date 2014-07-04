package org.fdroid.fdroid;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Locale;

public class RepoIntentParser {

    private final Intent intent;
    private final Context context;

    private String errorMessage;

    private String repoUriString;
    private Uri repoUri;
    private String host;
    private int port;
    private String scheme;
    private String fingerprint;
    private String bssid = null;
    private String ssid = null;
    private boolean fromSwap;

    public RepoIntentParser(Context context, Intent intent) {
        this.intent = intent;
        this.context = context;
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

    public String getUri() {
        return repoUriString;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * TODO: This has changed a little from before, it used to be: port != 8888 and !ip then not local repo.
     * TODO: Port no longer fixed to 8888, as we can use whatever port we want from the settings of the local
     * repo device. However, perhaps we should choose a higher port, as a lot of regular web servers may opt for 8888
     * in liu of root permissions on the server for using port 80, and if port 8080 is already taken.
     */
    public boolean looksLikeLocalAddress() {
        return port == 8888 && host.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
    }

    public boolean parse() {

        /* an URL from a click, NFC, QRCode scan, etc */
        repoUri = intent.getData();
        if (repoUri == null) {
            return false;
        }

        Log.d("org.fdroid.fdroid.RepoIntentParser", "Parsing intent URI " + repoUri);

        // scheme and host should only ever be pure ASCII aka Locale.ENGLISH
        scheme = intent.getScheme();
        host   = repoUri.getHost();
        port   = repoUri.getPort();
        if (scheme == null || host == null) {
            this.errorMessage = String.format(context.getString(R.string.malformed_repo_uri), repoUri);
            return false;
        }

        if (equalsInList(scheme, new String[]{"FDROIDREPO", "FDROIDREPOS"})) {
            /*
             * QRCodes are more efficient in all upper case, so QR URIs are
             * encoded in all upper case, then forced to lower case.
             * Checking if the special F-Droid scheme being all is upper
             * case means it should be downcased.
             */
            repoUri = Uri.parse(repoUri.toString().toLowerCase(Locale.ENGLISH));
        } else if (repoUri.getPath().startsWith("/FDROID/REPO")) {
            /*
             * some QR scanners chop off the fdroidrepo:// and just try
             * http://, then the incoming URI does not get downcased
             * properly, and the query string is stripped off. So just
             * downcase the path, and carry on to get something working.
             */
            repoUri = Uri.parse(repoUri.toString().toLowerCase(Locale.ENGLISH));
        }

        // make scheme and host lowercase so they're readable in dialogs
        scheme = scheme.toLowerCase(Locale.ENGLISH);
        host = host.toLowerCase(Locale.ENGLISH);
        fingerprint = repoUri.getQueryParameter("fingerprint");
        bssid = repoUri.getQueryParameter("bssid");
        ssid = repoUri.getQueryParameter("ssid");
        fromSwap = repoUri.getQueryParameter("swap") != null;

        Log.i("RepoListFragment", "onCreate " + fingerprint);
        if (equalsInList(scheme, new String[] { "fdroidrepos", "fdroidrepo", "https", "http" })) {

            /* sanitize and format for function and readability */
            repoUriString = repoUri.toString()
                    .replaceAll("\\?.*$", "") // remove the whole query
                    .replaceAll("/*$", "") // remove all trailing slashes
                    .replace(repoUri.getHost(), host) // downcase host name
                    .replace(intent.getScheme(), scheme) // downcase scheme
                    .replace("fdroidrepo", "http"); // proper repo address
            return true;

        }

        return false;
    }

    private static boolean equalsInList(String value, String[] list) {
        for (String item : list) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFromSwap() {
        return fromSwap;
    }
}
