package org.fdroid.fdroid.localrepo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

public class LocalRepoManager {
    private static final String TAG = "LocalRepoManager";

    // For ref, official F-droid repo presently uses a maxage of 14 days
    private static final String DEFAULT_REPO_MAX_AGE_DAYS = "14";

    private final Context context;
    private final PackageManager pm;
    private final AssetManager assetManager;
    private final String fdroidPackageName;

    private static final String[] WEB_ROOT_ASSET_FILES = {
        "swap-icon.png",
        "swap-tick-done.png",
        "swap-tick-not-done.png"
    };

    private final Map<String, App> apps = new HashMap<>();

    public final SanitizedFile xmlIndex;
    private SanitizedFile xmlIndexJar = null;
    private SanitizedFile xmlIndexJarUnsigned = null;
    public final SanitizedFile webRoot;
    public final SanitizedFile fdroidDir;
    public final SanitizedFile fdroidDirCaps;
    public final SanitizedFile repoDir;
    public final SanitizedFile repoDirCaps;
    public final SanitizedFile iconsDir;

    @Nullable
    private static LocalRepoManager localRepoManager;

    @NonNull
    public static LocalRepoManager get(Context context) {
        if (localRepoManager == null)
            localRepoManager = new LocalRepoManager(context);
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
        xmlIndex = new SanitizedFile(repoDir, "index.xml");
        xmlIndexJar = new SanitizedFile(repoDir, "index.jar");
        xmlIndexJarUnsigned = new SanitizedFile(repoDir, "index.unsigned.jar");

        if (!fdroidDir.exists())
            if (!fdroidDir.mkdir())
                Log.e(TAG, "Unable to create empty base: " + fdroidDir);

        if (!repoDir.exists())
            if (!repoDir.mkdir())
                Log.e(TAG, "Unable to create empty repo: " + repoDir);

        if (!iconsDir.exists())
            if (!iconsDir.mkdir())
                Log.e(TAG, "Unable to create icons folder: " + iconsDir);
    }

    private String writeFdroidApkToWebroot() {
        ApplicationInfo appInfo;
        String fdroidClientURL = "https://f-droid.org/FDroid.apk";

        try {
            appInfo = pm.getApplicationInfo(fdroidPackageName, PackageManager.GET_META_DATA);
            SanitizedFile apkFile = SanitizedFile.knownSanitized(appInfo.publicSourceDir);
            SanitizedFile fdroidApkLink = new SanitizedFile(webRoot, "fdroid.client.apk");
            attemptToDelete(fdroidApkLink);
            if (Utils.symlinkOrCopyFile(apkFile, fdroidApkLink))
                fdroidClientURL = "/" + fdroidApkLink.getName();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
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

            String line;
            while ((line = in.readLine()) != null) {
                line = line.replaceAll("\\{\\{REPO_URL\\}\\}", repoAddress);
                line = line.replaceAll("\\{\\{CLIENT_URL\\}\\}", fdroidClientURL);
                out.write(line);
            }
            in.close();
            out.close();

            for (final String file : WEB_ROOT_ASSET_FILES) {
                Utils.copy(assetManager.open(file), new FileOutputStream(new File(webRoot, file)));
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
            Log.e(TAG, "Error writing local repo index: " + e.getMessage());
            Log.e(TAG, Log.getStackTraceString(e));
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
        Utils.symlinkOrCopyFile(new SanitizedFile(new File(directory, symlinkPrefix), fileName), index);
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

    public void deleteRepo() {
        deleteContents(repoDir);
    }

    public void copyApksToRepo() {
        copyApksToRepo(new ArrayList<>(apps.keySet()));
    }

    public void copyApksToRepo(List<String> appsToCopy) {
        for (final String packageName : appsToCopy) {
            final App app = apps.get(packageName);

            if (app.installedApk != null) {
                SanitizedFile outFile = new SanitizedFile(repoDir, app.installedApk.apkName);
                if (Utils.symlinkOrCopyFile(app.installedApk.installedFile, outFile))
                    continue;
            }
            // if we got here, something went wrong
            throw new IllegalStateException("Unable to copy APK");
        }
    }

    public void addApp(Context context, String packageName) {
        App app;
        try {
            app = new App(context.getApplicationContext(), pm, packageName);
            if (!app.isValid())
                return;
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            app.icon = getIconFile(packageName, packageInfo.versionCode).getName();
        } catch (PackageManager.NameNotFoundException | CertificateEncodingException | IOException e) {
            Log.e(TAG, "Error adding app to local repo: " + e.getMessage());
            Log.e(TAG, Log.getStackTraceString(e));
            return;
        }
        Log.i(TAG, "apps.put: " + packageName);
        apps.put(packageName, app);
    }

    public List<String> getApps() {
        return new ArrayList<>(apps.keySet());
    }

    public void copyIconsToRepo() {
        ApplicationInfo appInfo;
        for (final App app : apps.values()) {
            if (app.installedApk != null) {
                try {
                    appInfo = pm.getApplicationInfo(app.id, PackageManager.GET_META_DATA);
                    copyIconToRepo(appInfo.loadIcon(pm), app.id, app.installedApk.vercode);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Extracts the icon from an APK and writes it to the repo as a PNG
     */
    public void copyIconToRepo(Drawable drawable, String packageName, int versionCode) {
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
            e.printStackTrace();
        }
    }

    private File getIconFile(String packageName, int versionCode) {
        return new File(iconsDir, packageName + "_" + versionCode + ".png");
    }

    /**
     * Helper class to aid in constructing index.xml file.
     * It uses the PullParser API, because the DOM api is only able to be serialized from
     * API 8 upwards, but we support 7 at time of implementation.
     */
    public static class IndexXmlBuilder {

        @NonNull
        private final XmlSerializer serializer;

        @NonNull
        private final Map<String, App> apps;

        @NonNull
        private final Context context;

        @NonNull
        private final DateFormat dateToStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        public IndexXmlBuilder(@NonNull Context context, @NonNull Map<String, App> apps) throws XmlPullParserException, IOException {
            this.context = context;
            this.apps = apps;
            serializer = XmlPullParserFactory.newInstance().newSerializer();
        }

        public void build(Writer output) throws IOException, LocalRepoKeyStore.InitException {
            serializer.setOutput(output);
            serializer.startDocument(null, null);
            tagFdroid();
            serializer.endDocument();
        }

        private void tagFdroid() throws IOException, LocalRepoKeyStore.InitException {
            serializer.startTag("", "fdroid");
            tagRepo();
            for (Map.Entry<String, App> entry : apps.entrySet()) {
                tagApplication(entry.getValue());
            }
            serializer.endTag("", "fdroid");
        }

        private void tagRepo() throws IOException, LocalRepoKeyStore.InitException {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            // max age is an EditTextPreference, which is always a String
            int repoMaxAge = Float.valueOf(prefs.getString("max_repo_age_days", DEFAULT_REPO_MAX_AGE_DAYS)).intValue();

            serializer.startTag("", "repo");

            serializer.attribute("", "icon", "blah.png");
            serializer.attribute("", "maxage", String.valueOf(repoMaxAge));
            serializer.attribute("", "name", Preferences.get().getLocalRepoName() + " on " + FDroidApp.ipAddressString);
            serializer.attribute("", "pubkey", Hasher.hex(LocalRepoKeyStore.get(context).getCertificate()));
            long timestamp = System.currentTimeMillis() / 1000L;
            serializer.attribute("", "timestamp", String.valueOf(timestamp));

            tag("description", "A local FDroid repo generated from apps installed on " + Preferences.get().getLocalRepoName());

            serializer.endTag("", "repo");

        }

        /**
         * Helper function to start a tag called "name", fill it with text "text", and then
         * end the tag in a more concise manner.
         */
        private void tag(String name, String text) throws IOException {
            serializer.startTag("", name).text(text).endTag("", name);
        }

        /**
         * Alias for {@link org.fdroid.fdroid.localrepo.LocalRepoManager.IndexXmlBuilder#tag(String, String)}
         * That accepts a number instead of string.
         * @see IndexXmlBuilder#tag(String, String)
         */
        private void tag(String name, long number) throws IOException {
            tag(name, String.valueOf(number));
        }

        /**
         * Alias for {@link org.fdroid.fdroid.localrepo.LocalRepoManager.IndexXmlBuilder#tag(String, String)}
         * that accepts a date instead of a string.
         * @see IndexXmlBuilder#tag(String, String)
         */
        private void tag(String name, Date date) throws IOException {
            tag(name, dateToStr.format(date));
        }

        private void tagApplication(App app) throws IOException {
            serializer.startTag("", "application");
            serializer.attribute("", "id", app.id);

            tag("id", app.id);
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
            tag("marketversion", app.installedApk.version);
            tag("marketvercode", app.installedApk.vercode);

            tagPackage(app);

            serializer.endTag("", "application");
        }

        private void tagPackage(App app) throws IOException {
            serializer.startTag("", "package");

            tag("version", app.installedApk.version);
            tag("versioncode", app.installedApk.vercode);
            tag("apkname", app.installedApk.apkName);
            tagHash(app);
            tag("sig", app.installedApk.sig.toLowerCase(Locale.US));
            tag("size", app.installedApk.installedFile.length());
            tag("sdkver", app.installedApk.minSdkVersion);
            tag("maxsdkver", app.installedApk.maxSdkVersion);
            tag("added", app.installedApk.added);
            tagFeatures(app);
            tagPermissions(app);

            serializer.endTag("", "package");
        }

        private void tagPermissions(App app) throws IOException {
            serializer.startTag("", "permissions");
            if (app.installedApk.permissions != null) {
                StringBuilder buff = new StringBuilder();

                for (String permission : app.installedApk.permissions) {
                    buff.append(permission.replace("android.permission.", ""));
                    buff.append(',');
                }
                String out = buff.toString();
                if (!TextUtils.isEmpty(out))
                    serializer.text(out.substring(0, out.length() - 1));
            }
            serializer.endTag("", "permissions");
        }

        private void tagFeatures(App app) throws IOException {
            serializer.startTag("", "features");
            if (app.installedApk.features != null)
                serializer.text(Utils.CommaSeparatedList.str(app.installedApk.features));
            serializer.endTag("", "features");
        }

        private void tagHash(App app) throws IOException {
            serializer.startTag("", "hash");
            serializer.attribute("", "type", app.installedApk.hashType);
            serializer.text(app.installedApk.hash.toLowerCase(Locale.US));
            serializer.endTag("", "hash");
        }
    }

    public void writeIndexJar() throws IOException {
        try {
            new IndexXmlBuilder(context, apps).build(new FileWriter(xmlIndex));
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            Toast.makeText(context, R.string.failed_to_create_index, Toast.LENGTH_LONG).show();
            return;
        }

        BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(xmlIndexJarUnsigned));
        JarOutputStream jo = new JarOutputStream(bo);

        BufferedInputStream bi = new BufferedInputStream(new FileInputStream(xmlIndex));

        JarEntry je = new JarEntry("index.xml");
        jo.putNextEntry(je);

        byte[] buf = new byte[1024];
        int bytesRead;

        while ((bytesRead = bi.read(buf)) != -1) {
            jo.write(buf, 0, bytesRead);
        }

        bi.close();
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
