package org.fdroid.fdroid.localrepo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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

import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.cert.CertificateEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class LocalRepoManager {
    private static final String TAG = "fdroid.LocalRepoManager";

    // For ref, official F-droid repo presently uses a maxage of 14 days
    private static final String DEFAULT_REPO_MAX_AGE_DAYS = "14";

    private final Context context;
    private final PackageManager pm;
    private final AssetManager assetManager;
    private final SharedPreferences prefs;
    private final String fdroidPackageName;

    private static String[] WEB_ROOT_ASSET_FILES = {
        "swap-icon.png",
        "swap-tick-done.png",
        "swap-tick-not-done.png"
    };

    private Map<String, App> apps = new HashMap<>();

    public final SanitizedFile xmlIndex;
    private SanitizedFile xmlIndexJar = null;
    private SanitizedFile xmlIndexJarUnsigned = null;
    public final SanitizedFile webRoot;
    public final SanitizedFile fdroidDir;
    public final SanitizedFile repoDir;
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
        prefs = PreferenceManager.getDefaultSharedPreferences(c);
        fdroidPackageName = c.getPackageName();

        webRoot = SanitizedFile.knownSanitized(c.getFilesDir());
        /* /fdroid/repo is the standard path for user repos */
        fdroidDir = new SanitizedFile(webRoot, "fdroid");
        repoDir = new SanitizedFile(fdroidDir, "repo");
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
        } catch (NameNotFoundException e) {
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

            for (String file : WEB_ROOT_ASSET_FILES) {
                Utils.copy(assetManager.open(file), new FileOutputStream(new File(webRoot, file)));
            }

            // make symlinks/copies in each subdir of the repo to make sure that
            // the user will always find the bootstrap page.
            symlinkIndexPageElsewhere("../", fdroidDir);
            symlinkIndexPageElsewhere("../../", repoDir);

            // add in /FDROID/REPO to support bad QR Scanner apps
            File fdroidCAPS = new File(fdroidDir.getParentFile(), "FDROID");
            attemptToMkdir(fdroidCAPS);

            File repoCAPS = new File(fdroidCAPS, "REPO");
            attemptToMkdir(repoCAPS);

            symlinkIndexPageElsewhere("../", fdroidCAPS);
            symlinkIndexPageElsewhere("../../", repoCAPS);

        } catch (IOException e) {
            Log.e(TAG, "Error writing local repo index: " + e.getMessage());
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private static void attemptToMkdir(@NonNull File dir) throws IOException {
        if (dir.exists()) {
            if (dir.isDirectory()) {
                return;
            } else {
                throw new IOException("Can't make directory " + dir + " - it is already a file.");
            }
        }

        if (!dir.mkdir()) {
            throw new IOException("An error occured trying to create directory " + dir);
        }
    }

    private static void attemptToDelete(File file) {
            Utils.symlinkOrCopyFile(new SanitizedFile(new File(directory, symlinkPrefix), fileName), file);
            if (!file.delete()) {
                Log.e(TAG, "Could not delete \"" + file.getAbsolutePath() + "\".");
            }
        }
    }

    private void symlinkIndexPageElsewhere(String symlinkPrefix, File directory) {
        SanitizedFile index = new SanitizedFile(directory, "index.html");
        attemptToDelete(index);
        Utils.symlinkOrCopyFile(new SanitizedFile(new File(directory, symlinkPrefix), "index.html"), index);

        for(String fileName : WEB_ROOT_ASSET_FILES) {
            SanitizedFile file = new SanitizedFile(directory, fileName);
            attemptToDelete(file);
            Utils.symlinkOrCopyFile(new SanitizedFile(new File(directory, symlinkPrefix), fileName), file);
        }
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
        for (String packageName : appsToCopy) {
            App app = apps.get(packageName);

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
        } catch (NameNotFoundException | CertificateEncodingException | IOException e) {
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
                } catch (NameNotFoundException e) {
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

    private void writeIndexXML() throws TransformerException, ParserConfigurationException, LocalRepoKeyStore.InitException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.newDocument();
        Element rootElement = doc.createElement("fdroid");
        doc.appendChild(rootElement);

        // max age is an EditTextPreference, which is always a String
        int repoMaxAge = Float.valueOf(prefs.getString("max_repo_age_days",
                DEFAULT_REPO_MAX_AGE_DAYS)).intValue();

        String repoName = Preferences.get().getLocalRepoName();

        Element repo = doc.createElement("repo");
        repo.setAttribute("icon", "blah.png");
        repo.setAttribute("maxage", String.valueOf(repoMaxAge));
        repo.setAttribute("name", repoName + " on " + FDroidApp.ipAddressString);
        repo.setAttribute("pubkey", Hasher.hex(LocalRepoKeyStore.get(context).getCertificate()));
        long timestamp = System.currentTimeMillis() / 1000L;
        repo.setAttribute("timestamp", String.valueOf(timestamp));
        rootElement.appendChild(repo);

        Element repoDesc = doc.createElement("description");
        repoDesc.setTextContent("A local FDroid repo generated from apps installed on " + repoName);
        repo.appendChild(repoDesc);

        SimpleDateFormat dateToStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (Entry<String, App> entry : apps.entrySet()) {
            App app = entry.getValue();
            Element application = doc.createElement("application");
            application.setAttribute("id", app.id);
            rootElement.appendChild(application);

            Element appID = doc.createElement("id");
            appID.setTextContent(app.id);
            application.appendChild(appID);

            Element added = doc.createElement("added");
            added.setTextContent(dateToStr.format(app.added));
            application.appendChild(added);

            Element lastUpdated = doc.createElement("lastupdated");
            lastUpdated.setTextContent(dateToStr.format(app.lastUpdated));
            application.appendChild(lastUpdated);

            Element name = doc.createElement("name");
            name.setTextContent(app.name);
            application.appendChild(name);

            Element summary = doc.createElement("summary");
            summary.setTextContent(app.summary);
            application.appendChild(summary);

            Element desc = doc.createElement("desc");
            desc.setTextContent(app.description);
            application.appendChild(desc);

            Element icon = doc.createElement("icon");
            icon.setTextContent(app.icon);
            application.appendChild(icon);

            Element license = doc.createElement("license");
            license.setTextContent("Unknown");
            application.appendChild(license);

            Element categories = doc.createElement("categories");
            categories.setTextContent("LocalRepo," + repoName);
            application.appendChild(categories);

            Element category = doc.createElement("category");
            category.setTextContent("LocalRepo," + repoName);
            application.appendChild(category);

            Element web = doc.createElement("web");
            application.appendChild(web);

            Element source = doc.createElement("source");
            application.appendChild(source);

            Element tracker = doc.createElement("tracker");
            application.appendChild(tracker);

            Element marketVersion = doc.createElement("marketversion");
            marketVersion.setTextContent(app.installedApk.version);
            application.appendChild(marketVersion);

            Element marketVerCode = doc.createElement("marketvercode");
            marketVerCode.setTextContent(String.valueOf(app.installedApk.vercode));
            application.appendChild(marketVerCode);

            Element packageNode = doc.createElement("package");

            Element version = doc.createElement("version");
            version.setTextContent(app.installedApk.version);
            packageNode.appendChild(version);

            // F-Droid unfortunately calls versionCode versioncode...
            Element versioncode = doc.createElement("versioncode");
            versioncode.setTextContent(String.valueOf(app.installedApk.vercode));
            packageNode.appendChild(versioncode);

            Element apkname = doc.createElement("apkname");
            apkname.setTextContent(app.installedApk.apkName);
            packageNode.appendChild(apkname);

            Element hash = doc.createElement("hash");
            hash.setAttribute("type", app.installedApk.hashType);
            hash.setTextContent(app.installedApk.hash.toLowerCase(Locale.US));
            packageNode.appendChild(hash);

            Element sig = doc.createElement("sig");
            sig.setTextContent(app.installedApk.sig.toLowerCase(Locale.US));
            packageNode.appendChild(sig);

            Element size = doc.createElement("size");
            size.setTextContent(String.valueOf(app.installedApk.installedFile.length()));
            packageNode.appendChild(size);

            Element sdkver = doc.createElement("sdkver");
            sdkver.setTextContent(String.valueOf(app.installedApk.minSdkVersion));
            packageNode.appendChild(sdkver);

            Element apkAdded = doc.createElement("added");
            apkAdded.setTextContent(dateToStr.format(app.installedApk.added));
            packageNode.appendChild(apkAdded);

            Element features = doc.createElement("features");
            if (app.installedApk.features != null)
                features.setTextContent(Utils.CommaSeparatedList.str(app.installedApk.features));
            packageNode.appendChild(features);

            Element permissions = doc.createElement("permissions");
            if (app.installedApk.permissions != null) {
                StringBuilder buff = new StringBuilder();

                for (String permission : app.installedApk.permissions) {
                    buff.append(permission.replace("android.permission.", ""));
                    buff.append(",");
                }
                String out = buff.toString();
                if (!TextUtils.isEmpty(out))
                    permissions.setTextContent(out.substring(0, out.length() - 1));
            }
            packageNode.appendChild(permissions);

            application.appendChild(packageNode);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        DOMSource domSource = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlIndex);

        transformer.transform(domSource, result);
    }

    public void writeIndexJar() throws IOException {
        try {
            writeIndexXML();
        } catch (Exception e) {
            Toast.makeText(context, R.string.failed_to_create_index, Toast.LENGTH_LONG).show();
            Log.e(TAG, Log.getStackTraceString(e));
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
