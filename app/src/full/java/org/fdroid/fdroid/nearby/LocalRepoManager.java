package org.fdroid.fdroid.nearby;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.index.v1.AppV1;
import org.fdroid.index.v1.IndexV1;
import org.fdroid.index.v1.IndexV1Creator;
import org.fdroid.index.v1.IndexV1UpdaterKt;
import org.fdroid.index.v1.IndexV1VerifierKt;
import org.fdroid.index.v1.PackageV1;
import org.fdroid.index.v1.RepoV1;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * The {@link SwapService} deals with managing the entire workflow from selecting apps to
 * swap, to invoking this class to prepare the webroot, to enabling various communication protocols.
 * This class deals specifically with the webroot side of things, ensuring we have a valid index.jar
 * and the relevant .apk and icon files available.
 */
public final class LocalRepoManager {
    private static final String TAG = "LocalRepoManager";

    private final Context context;
    private final PackageManager pm;
    private final AssetManager assetManager;
    private final String fdroidPackageName;

    public static final String[] WEB_ROOT_ASSET_FILES = {
            "swap-icon.png",
            "swap-tick-done.png",
            "swap-tick-not-done.png",
    };

    private final List<App> apps = new ArrayList<>();

    private final SanitizedFile indexJar;
    private final SanitizedFile indexJarUnsigned;
    private final SanitizedFile webRoot;
    private final SanitizedFile fdroidDir;
    private final SanitizedFile fdroidDirCaps;
    private final SanitizedFile repoDir;
    private final SanitizedFile repoDirCaps;

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
        indexJar = new SanitizedFile(repoDir, IndexV1UpdaterKt.SIGNED_FILE_NAME);
        indexJarUnsigned = new SanitizedFile(repoDir, "index-v1.unsigned.jar");

        if (!fdroidDir.exists() && !fdroidDir.mkdir()) {
            Log.e(TAG, "Unable to create empty base: " + fdroidDir);
        }

        if (!repoDir.exists() && !repoDir.mkdir()) {
            Log.e(TAG, "Unable to create empty repo: " + repoDir);
        }

        SanitizedFile iconsDir = new SanitizedFile(repoDir, "icons");
        if (!iconsDir.exists() && !iconsDir.mkdir()) {
            Log.e(TAG, "Unable to create icons folder: " + iconsDir);
        }
    }

    private String writeFdroidApkToWebroot() {
        ApplicationInfo appInfo;
        String fdroidClientURL = "https://f-droid.org/F-Droid.apk";

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

    void writeIndexPage(String repoAddress) {
        final String fdroidClientURL = writeFdroidApkToWebroot();
        try {
            File indexHtml = new File(webRoot, "index.html");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(assetManager.open("index.template.html"), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(indexHtml)));

            StringBuilder builder = new StringBuilder();
            for (App app : apps) {
                builder.append("<li><a href=\"/fdroid/repo/")
                        .append(app.packageName)
                        .append("_")
                        .append(app.installedApk.versionCode)
                        .append(".apk\">")
                        .append("<img width=\"32\" height=\"32\" src=\"/fdroid/repo/icons/")
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
            Log.i(TAG, "Could not delete \"" + file.getAbsolutePath() + "\".");
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
     * Get the {@code index-v1.jar} file that represents the local swap repo.
     */
    public File getIndexJar() {
        return indexJar;
    }

    public File getWebRoot() {
        return webRoot;
    }

    public void deleteRepo() {
        deleteContents(repoDir);
    }

    void generateIndex(String address, String[] selectedApps) throws IOException {
        String name = Preferences.get().getLocalRepoName() + " on " + FDroidApp.ipAddressString;
        String description = "A local FDroid repo generated from apps installed on " + Preferences.get().getLocalRepoName();
        RepoV1 repo = new RepoV1(System.currentTimeMillis(), 20001, 7, name, "swap-icon.png", address, description, Collections.emptyList());
        Set<String> apps = new HashSet<>(Arrays.asList(selectedApps));
        IndexV1Creator creator = new IndexV1Creator(context.getPackageManager(), repoDir, apps, repo);
        IndexV1 indexV1 = creator.createRepo();
        cacheApps(indexV1);
        writeIndexPage(address);
        SanitizedFile indexJson = new SanitizedFile(repoDir, IndexV1VerifierKt.DATA_FILE_NAME);
        writeIndexJar(indexJson);
    }

    private void cacheApps(IndexV1 indexV1) {
        this.apps.clear();
        for (AppV1 a : indexV1.getApps()) {
            App app = new App();
            app.packageName = a.getPackageName();
            app.name = a.getName();
            app.installedApk = new Apk();
            List<PackageV1> packages = indexV1.getPackages().get(a.getPackageName());
            if (packages != null && packages.size() > 0) {
                Long versionCode = packages.get(0).getVersionCode();
                if (versionCode != null) app.installedApk.versionCode = versionCode;
            }
            this.apps.add(app);
        }
    }

    private void writeIndexJar(SanitizedFile indexJson) throws IOException {
        BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(indexJarUnsigned));
        JarOutputStream jo = new JarOutputStream(bo);
        JarEntry je = new JarEntry(indexJson.getName());
        jo.putNextEntry(je);
        FileUtils.copyFile(indexJson, jo);
        jo.close();
        bo.close();

        try {
            LocalRepoKeyStore.get(context).signZip(indexJarUnsigned, indexJar);
        } catch (LocalRepoKeyStore.InitException e) {
            throw new IOException("Could not sign index - keystore failed to initialize");
        } finally {
            attemptToDelete(indexJarUnsigned);
        }

    }

}
