package org.fdroid.fdroid.localrepo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.RepoUpdater;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.cert.CertificateEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * The {@link SwapService} deals with managing the entire workflow from selecting apps to
 * swap, to invoking this class to prepare the webroot, to enabling various communication protocols.
 * This class deals specifically with the webroot side of things, ensuring we have a valid index.jar
 * and the relevant .apk and icon files available.
 */
@SuppressWarnings("LineLength")
public final class LocalRepoManager {
    private static final String TAG = "LocalRepoManager";

    private final Context context;
    private final PackageManager pm;
    private final AssetManager assetManager;
    private final String fdroidPackageName;

    private static final String[] WEB_ROOT_ASSET_FILES = {
            "swap-icon.png",
            "swap-tick-done.png",
            "swap-tick-not-done.png",
    };

    private final Map<String, App> apps = new HashMap<>();

    private final SanitizedFile xmlIndexJar;
    private final SanitizedFile xmlIndexJarUnsigned;
    private final SanitizedFile webRoot;
    private final SanitizedFile fdroidDir;
    private final SanitizedFile fdroidDirCaps;
    private final SanitizedFile repoDir;
    private final SanitizedFile repoDirCaps;
    private final SanitizedFile iconsDir;

    @Nullable
    private static LocalRepoManager localRepoManager;

    @NonNull
    public static LocalRepoManager get(Context context) {
        if (localRepoManager == null) {
            localRepoManager = new LocalRepoManager(context);
        }
        return localRepoManager;
    }

    private LocalRepoManager(Context c) {
        context = c.getApplicationContext();
        pm = c.getPackageManager();
        assetManager = c.getAssets();
        fdroidPackageName = c.getPackageName();

        webRoot = SanitizedFile.knownSanitized(c.getFilesDir());
        /* /fdroid/repo is the standard path for user repos */
        fdroidDir = new SanitizedFile(webRoot, "fdroid");
        fdroidDirCaps = new SanitizedFile(webRoot, "FDROID");
        repoDir = new SanitizedFile(fdroidDir, "repo");
        repoDirCaps = new SanitizedFile(fdroidDirCaps, "REPO");
        iconsDir = new SanitizedFile(repoDir, "icons");
        xmlIndexJar = new SanitizedFile(repoDir, RepoUpdater.SIGNED_FILE_NAME);
        xmlIndexJarUnsigned = new SanitizedFile(repoDir, "index.unsigned.jar");

        if (!fdroidDir.exists() && !fdroidDir.mkdir()) {
            Log.e(TAG, "Unable to create empty base: " + fdroidDir);
        }

        if (!repoDir.exists() && !repoDir.mkdir()) {
            Log.e(TAG, "Unable to create empty repo: " + repoDir);
        }

        if (!iconsDir.exists() && !iconsDir.mkdir()) {
            Log.e(TAG, "Unable to create icons folder: " + iconsDir);
        }
    }

    private String writeFdroidApkToWebroot() {
        ApplicationInfo appInfo;
        String fdroidClientURL = "https://f-droid.org/FDroid.apk";

        try {
            appInfo = pm.getApplicationInfo(fdroidPackageName, PackageManager.GET_META_DATA);
            SanitizedFile apkFile = SanitizedFile.knownSanitized(appInfo.publicSourceDir);
            SanitizedFile fdroidApkLink = new SanitizedFile(fdroidDir, "F-Droid.apk");
            attemptToDelete(fdroidApkLink);
            if (Utils.symlinkOrCopyFileQuietly(apkFile, fdroidApkLink)) {
                fdroidClientURL = "/" + fdroidDir.getName() + "/" + fdroidApkLink.getName();
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not set up F-Droid apk in the webroot", e);
        }
        return fdroidClientURL;
    }

    public void writeIndexPage(String repoAddress) {
        final String fdroidClientURL = writeFdroidApkToWebroot();
        try {
            File indexHtml = new File(webRoot, "index.html");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(assetManager.open("index.template.html"), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(indexHtml)));

            StringBuilder builder = new StringBuilder();
            for (App app : apps.values()) {
                builder.append("<li><a href=\"/fdroid/repo/")
                        .append(app.installedApk.apkName)
                        .append("\"><img width=\"32\" height=\"32\" src=\"/fdroid/repo/icons/")
                        .append(app.packageName)
                        .append("_")
                        .append(app.installedApk.versionCode)
                        .append(".png\">")
                        .append(app.name)
                        .append("</a></li>\n");
            }

            String line;
            while ((line = in.readLine()) != null) {
                line = line.replaceAll("\\{\\{REPO_URL\\}\\}", repoAddress);
                line = line.replaceAll("\\{\\{CLIENT_URL\\}\\}", fdroidClientURL);
                line = line.replaceAll("\\{\\{APP_LIST\\}\\}", builder.toString());
                out.write(line);
            }
            in.close();
            out.close();

            for (final String file : WEB_ROOT_ASSET_FILES) {
                InputStream assetIn = assetManager.open(file);
                OutputStream assetOut = new FileOutputStream(new File(webRoot, file));
                Utils.copy(assetIn, assetOut);
                assetIn.close();
                assetOut.close();
            }

            // make symlinks/copies in each subdir of the repo to make sure that
            // the user will always find the bootstrap page.
            symlinkEntireWebRootElsewhere("../", fdroidDir);
            symlinkEntireWebRootElsewhere("../../", repoDir);

            // add in /FDROID/REPO to support bad QR Scanner apps
            attemptToMkdir(fdroidDirCaps);
            attemptToMkdir(repoDirCaps);

            symlinkEntireWebRootElsewhere("../", fdroidDirCaps);
            symlinkEntireWebRootElsewhere("../../", repoDirCaps);

        } catch (IOException e) {
            Log.e(TAG, "Error writing local repo index", e);
        }
    }

    private static void attemptToMkdir(@NonNull File dir) throws IOException {
        if (dir.exists()) {
            if (dir.isDirectory()) {
                return;
            }
            throw new IOException("Can't make directory " + dir + " - it is already a file.");
        }

        if (!dir.mkdir()) {
            throw new IOException("An error occurred trying to create directory " + dir);
        }
    }

    private static void attemptToDelete(@NonNull File file) {
        if (!file.delete()) {
            Log.e(TAG, "Could not delete \"" + file.getAbsolutePath() + "\".");
        }
    }

    private void symlinkEntireWebRootElsewhere(String symlinkPrefix, File directory) {
        symlinkFileElsewhere("index.html", symlinkPrefix, directory);
        for (final String fileName : WEB_ROOT_ASSET_FILES) {
            symlinkFileElsewhere(fileName, symlinkPrefix, directory);
        }
    }

    private void symlinkFileElsewhere(String fileName, String symlinkPrefix, File directory) {
        SanitizedFile index = new SanitizedFile(directory, fileName);
        attemptToDelete(index);
        Utils.symlinkOrCopyFileQuietly(new SanitizedFile(new File(directory, symlinkPrefix), fileName), index);
    }

    private void deleteContents(File path) {
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (file.isDirectory()) {
                    deleteContents(file);
                } else {
                    attemptToDelete(file);
                }
            }
        }
    }

    /**
     * Get the {@code index.jar} file that represents the local swap repo.
     */
    public File getIndexJar() {
        return xmlIndexJar;
    }

    public void deleteRepo() {
        deleteContents(repoDir);
    }

    public void copyApksToRepo() {
        copyApksToRepo(new ArrayList<>(apps.keySet()));
    }

    private void copyApksToRepo(List<String> appsToCopy) {
        for (final String packageName : appsToCopy) {
            final App app = apps.get(packageName);

            if (app.installedApk != null) {
                SanitizedFile outFile = new SanitizedFile(repoDir, app.installedApk.apkName);
                if (Utils.symlinkOrCopyFileQuietly(app.installedApk.installedFile, outFile)) {
                    continue;
                }
            }
            // if we got here, something went wrong
            throw new IllegalStateException("Unable to copy APK");
        }
    }

    public void addApp(Context context, String packageName) {
        App app;
        try {
            app = SwapService.getAppFromCache(packageName);
            if (app == null) {
                app = App.getInstance(context.getApplicationContext(), pm, packageName);
            }
            if (app == null || !app.isValid()) {
                return;
            }
        } catch (PackageManager.NameNotFoundException | CertificateEncodingException | IOException e) {
            Log.e(TAG, "Error adding app to local repo", e);
            return;
        }
        Utils.debugLog(TAG, "apps.put: " + packageName);
        apps.put(packageName, app);
    }

    public void copyIconsToRepo() {
        ApplicationInfo appInfo;
        for (final App app : apps.values()) {
            if (app.installedApk != null) {
                try {
                    appInfo = pm.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA);
                    copyIconToRepo(appInfo.loadIcon(pm), app.packageName, app.installedApk.versionCode);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Error getting app icon", e);
                }
            }
        }
    }

    /**
     * Extracts the icon from an APK and writes it to the repo as a PNG
     */
    private void copyIconToRepo(Drawable drawable, String packageName, int versionCode) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        File png = getIconFile(packageName, versionCode);
        OutputStream out;
        try {
            out = new BufferedOutputStream(new FileOutputStream(png));
            bitmap.compress(CompressFormat.PNG, 100, out);
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "Error copying icon to repo", e);
        }
    }

    private File getIconFile(String packageName, int versionCode) {
        return new File(iconsDir, App.getIconName(packageName, versionCode));
    }

    /**
     * Helper class to aid in constructing index.xml file.
     */
    public static final class IndexXmlBuilder {
        @NonNull
        private final XmlSerializer serializer;

        @NonNull
        private final DateFormat dateToStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        private IndexXmlBuilder() throws XmlPullParserException {
            serializer = XmlPullParserFactory.newInstance().newSerializer();
        }

        public void build(Context context, Map<String, App> apps, OutputStream output) throws IOException, LocalRepoKeyStore.InitException {
            serializer.setOutput(output, "UTF-8");
            serializer.startDocument(null, null);
            serializer.startTag("", "fdroid");

            // <repo> block
            serializer.startTag("", "repo");
            serializer.attribute("", "icon", "blah.png");
            serializer.attribute("", "name", Preferences.get().getLocalRepoName() + " on " + FDroidApp.ipAddressString);
            serializer.attribute("", "pubkey", Hasher.hex(LocalRepoKeyStore.get(context).getCertificate()));
            long timestamp = System.currentTimeMillis() / 1000L;
            serializer.attribute("", "timestamp", String.valueOf(timestamp));
            serializer.attribute("", "version", "10");
            tag("description", "A local FDroid repo generated from apps installed on " + Preferences.get().getLocalRepoName());
            serializer.endTag("", "repo");

            // <application> blocks
            for (Map.Entry<String, App> entry : apps.entrySet()) {
                tagApplication(entry.getValue());
            }

            serializer.endTag("", "fdroid");
            serializer.endDocument();
            output.close();
        }

        /**
         * Helper function to start a tag called "name", fill it with text "text", and then
         * end the tag in a more concise manner.  If "text" is blank, skip the tag entirely.
         */
        private void tag(String name, String text) throws IOException {
            if (TextUtils.isEmpty(text)) {
                return;
            }
            serializer.startTag("", name).text(text).endTag("", name);
        }

        /**
         * Alias for {@link org.fdroid.fdroid.localrepo.LocalRepoManager.IndexXmlBuilder#tag(String, String)}
         * That accepts a number instead of string.
         *
         * @see IndexXmlBuilder#tag(String, String)
         */
        private void tag(String name, long number) throws IOException {
            tag(name, String.valueOf(number));
        }

        /**
         * Alias for {@link org.fdroid.fdroid.localrepo.LocalRepoManager.IndexXmlBuilder#tag(String, String)}
         * that accepts a date instead of a string.
         *
         * @see IndexXmlBuilder#tag(String, String)
         */
        private void tag(String name, Date date) throws IOException {
            tag(name, dateToStr.format(date));
        }

        private void tagApplication(App app) throws IOException {
            serializer.startTag("", "application");
            serializer.attribute("", "id", app.packageName);

            tag("id", app.packageName);
            tag("added", app.added);
            tag("lastupdated", app.lastUpdated);
            tag("name", app.name);
            tag("summary", app.summary);
            tag("icon", app.icon);
            tag("desc", app.description);
            tag("license", "Unknown");
            tag("categories", "LocalRepo," + Preferences.get().getLocalRepoName());
            tag("category", "LocalRepo," + Preferences.get().getLocalRepoName());
            tag("web", "web");
            tag("source", "source");
            tag("tracker", "tracker");
            tag("marketversion", app.installedApk.versionName);
            tag("marketvercode", app.installedApk.versionCode);

            tagPackage(app);

            serializer.endTag("", "application");
        }

        private void tagPackage(App app) throws IOException {
            serializer.startTag("", "package");

            tag("version", app.installedApk.versionName);
            tag("versioncode", app.installedApk.versionCode);
            tag("apkname", app.installedApk.apkName);
            tagHash(app);
            tag("sig", app.installedApk.sig.toLowerCase(Locale.US));
            tag("size", app.installedApk.installedFile.length());
            tag("added", app.installedApk.added);
            if (app.installedApk.minSdkVersion > Apk.SDK_VERSION_MIN_VALUE) {
                tag("sdkver", app.installedApk.minSdkVersion);
            }
            if (app.installedApk.targetSdkVersion > app.installedApk.minSdkVersion) {
                tag("targetSdkVersion", app.installedApk.targetSdkVersion);
            }
            if (app.installedApk.maxSdkVersion < Apk.SDK_VERSION_MAX_VALUE) {
                tag("maxsdkver", app.installedApk.maxSdkVersion);
            }
            tagFeatures(app);
            tagPermissions(app);
            tagNativecode(app);

            serializer.endTag("", "package");
        }

        private void tagPermissions(App app) throws IOException {
            serializer.startTag("", "permissions");
            if (app.installedApk.requestedPermissions != null) {
                StringBuilder buff = new StringBuilder();

                for (String permission : app.installedApk.requestedPermissions) {
                    buff.append(permission.replace("android.permission.", ""));
                    buff.append(',');
                }
                String out = buff.toString();
                if (!TextUtils.isEmpty(out)) {
                    serializer.text(out.substring(0, out.length() - 1));
                }
            }
            serializer.endTag("", "permissions");
        }

        private void tagFeatures(App app) throws IOException {
            serializer.startTag("", "features");
            if (app.installedApk.features != null) {
                serializer.text(TextUtils.join(",", app.installedApk.features));
            }
            serializer.endTag("", "features");
        }

        private void tagNativecode(App app) throws IOException {
            if (app.installedApk.nativecode != null) {
                serializer.startTag("", "nativecode");
                serializer.text(TextUtils.join(",", app.installedApk.nativecode));
                serializer.endTag("", "nativecode");
            }
        }

        private void tagHash(App app) throws IOException {
            serializer.startTag("", "hash");
            serializer.attribute("", "type", app.installedApk.hashType);
            serializer.text(app.installedApk.hash);
            serializer.endTag("", "hash");
        }
    }

    public void writeIndexJar() throws IOException, XmlPullParserException, LocalRepoKeyStore.InitException {
        BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(xmlIndexJarUnsigned));
        JarOutputStream jo = new JarOutputStream(bo);
        JarEntry je = new JarEntry(RepoUpdater.DATA_FILE_NAME);
        jo.putNextEntry(je);
        new IndexXmlBuilder().build(context, apps, jo);
        jo.close();
        bo.close();

        try {
            LocalRepoKeyStore.get(context).signZip(xmlIndexJarUnsigned, xmlIndexJar);
        } catch (LocalRepoKeyStore.InitException e) {
            throw new IOException("Could not sign index - keystore failed to initialize");
        } finally {
            attemptToDelete(xmlIndexJarUnsigned);
        }

    }

}
