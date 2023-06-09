package org.fdroid.fdroid.data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.nearby.SwapWorkflowActivity;
import org.fdroid.fdroid.nearby.peers.WifiPeer;
import org.fdroid.fdroid.views.ManageReposActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class NewRepoConfig {

    private static final String TAG = "NewRepoConfig";

    private String errorMessage;
    private boolean isValidRepo;

    private String uriString;
    private String host;
    private String username;
    private String password;
    private String fingerprint;
    private String bssid;
    private String ssid;
    private boolean fromSwap;
    private boolean preventFurtherSwaps;

    public NewRepoConfig(Context context, String uri) {
        init(context, uri != null ? Uri.parse(uri) : null);
    }

    public NewRepoConfig(Context context, Intent intent) {
        init(context, intent.getData());
        preventFurtherSwaps = intent.getBooleanExtra(SwapWorkflowActivity.EXTRA_PREVENT_FURTHER_SWAP_REQUESTS, false);
    }

    private void init(Context context, Uri incomingUri) {
        /* an URL from a click, NFC, QRCode scan, etc */
        Uri uri = incomingUri;
        if (uri == null) {
            isValidRepo = false;
            return;
        }

        if (ManageReposActivity.hasDisallowInstallUnknownSources(context)) {
            errorMessage = ManageReposActivity.getDisallowInstallUnknownSourcesErrorMessage(context);
            isValidRepo = false;
            return;
        }

        Utils.debugLog(TAG, "Parsing incoming intent looking for repo: " + incomingUri);

        // scheme and host should only ever be pure ASCII aka Locale.ENGLISH
        String scheme = uri.getScheme();
        host = uri.getHost();
        if (TextUtils.isEmpty(scheme) || (TextUtils.isEmpty(host) && !"file".equals(scheme))) {
            errorMessage = String.format(context.getString(R.string.malformed_repo_uri), uri);
            Log.i(TAG, errorMessage);
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

        if (uri.getPath() == null
                || !Arrays.asList("https", "http", "fdroidrepos", "fdroidrepo", "content", "file").contains(scheme)) {
            isValidRepo = false;
            return;
        }

        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] userInfoTokens = userInfo.split(":");
            if (userInfoTokens.length >= 2) {
                username = userInfoTokens[0];
                password = userInfoTokens[1];
                for (int i = 2; i < userInfoTokens.length; i++) {
                    password += ":" + userInfoTokens[i];
                }
            }
        }

        fingerprint = uri.getQueryParameter("fingerprint");
        bssid = uri.getQueryParameter("bssid");
        ssid = uri.getQueryParameter("ssid");
        fromSwap = uri.getQueryParameter("swap") != null;
        uriString = sanitizeRepoUri(uri);
        isValidRepo = true;
    }

    public String getBssid() {
        return bssid;
    }

    public String getSsid() {
        return ssid;
    }

    public String getRepoUriString() {
        return uriString;
    }

    public Uri getRepoUri() {
        if (uriString == null) {
            return null;
        }
        return Uri.parse(uriString);
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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

    public boolean preventFurtherSwaps() {
        return preventFurtherSwaps;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static final List<String> FORCE_HTTPS_DOMAINS = Arrays.asList(
            "amazonaws.com",
            "github.com",
            "githubusercontent.com",
            "github.io",
            "gitlab.com",
            "gitlab.io"
    );

    /**
     * Sanitize and format an incoming repo URI for function and readability.
     * This also forces URLs listed in {@code app/src/main/res/xml/network_security_config.xml}
     * to have "https://" as the scheme.
     *
     * @see <a href="https://developer.android.com/training/articles/security-config">Network Security Config</a>
     */
    public static String sanitizeRepoUri(Uri uri) {
        String scheme = uri.getScheme();
        String newScheme = scheme.toLowerCase(Locale.ENGLISH);
        String host = uri.getHost();
        String newHost = host.toLowerCase(Locale.ENGLISH);
        String userInfo = uri.getUserInfo();
        if ("http".equals(newScheme)) {
            for (String httpsDomain : FORCE_HTTPS_DOMAINS) {
                if (newHost.endsWith(httpsDomain)) {
                    scheme = "https";
                    break;
                }
            }
        }

        return uri.toString()
                .replaceAll("\\?.*$", "") // remove the whole query
                .replaceAll("/*$", "") // remove all trailing slashes
                .replace(userInfo + "@", "") // remove user authentication
                .replaceFirst(host, newHost)
                .replaceFirst(scheme, newScheme)
                .replace("fdroidrepo", "http") // proper repo address
                .replace("/FDROID/REPO", "/fdroid/repo"); // for QR FDroid path
    }

    public WifiPeer toPeer() {
        return new WifiPeer(this);
    }
}
