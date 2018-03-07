package org.fdroid.fdroid;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.views.ManageReposActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Michael Pöhn (michael.poehn@fsfe.org)
 */
@SuppressWarnings("LineLength")
public class Provisioner {

    public static final String TAG = "Provisioner";

    /**
     * This is the name of the subfolder in the file directory of this app
     * where {@link Provisioner} looks for new provisions.
     * <p>
     * eg. in the Emulator (API level 24): /data/user/0/org.fdroid.fdroid.debug/files/provisions
     */
    private static final String NEW_PROVISIONS_DIR = "provisions";

    protected Provisioner() {
    }

    /**
     * search for provision files and process them
     */
    static void scanAndProcess(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            return;
        }
        File provisionDir = new File(externalFilesDir.getAbsolutePath(), NEW_PROVISIONS_DIR);

        if (!provisionDir.isDirectory()) {
            Utils.debugLog(TAG, "Provisions dir does not exists: '" + provisionDir.getAbsolutePath() + "' moving on ...");
        } else if (provisionDir.list().length == 0) {
            Utils.debugLog(TAG, "Provisions dir is empty: '" + provisionDir.getAbsolutePath() + "' moving on ...");
        } else {

            Provisioner p = new Provisioner();
            List<File> files = p.findProvisionFiles(context);
            List<ProvisionPlaintext> plaintexts = p.extractProvisionsPlaintext(files);
            List<Provision> provisions = p.parseProvisions(plaintexts);

            if (provisions == null || provisions.size() == 0) {
                Utils.debugLog(TAG, "Provision dir does not contain any provisions: '" + provisionDir.getAbsolutePath() + "' moving on ...");
            } else {
                int cleanupCounter = 0;
                for (Provision provision : provisions) {
                    if (provision.getRepositoryProvision() != null) {
                        RepositoryProvision repo = provision.getRepositoryProvision();

                        Repo storedRepo = RepoProvider.Helper.findByAddress(context, repo.getUrl());
                        if (storedRepo != null) {
                            Utils.debugLog(TAG, "Provision contains a repo which is already added: '" + provision.getProvisonPath() + "' ignoring ...");
                        } else {
                            // Note: only the last started activity will visible to users.
                            // All other prompting attempts will be lost.
                            Uri origUrl = Uri.parse(repo.getUrl());
                            Uri.Builder data = new Uri.Builder();
                            data.scheme(origUrl.getScheme());
                            data.encodedAuthority(Uri.encode(repo.getUsername()) + ':'
                                    + Uri.encode(repo.getPassword()) + '@' + Uri.encode(origUrl.getAuthority()));
                            data.path(origUrl.getPath());
                            data.appendQueryParameter("fingerprint", repo.getSigfp());
                            Intent i = new Intent(context, ManageReposActivity.class);
                            i.setData(data.build());
                            context.startActivity(i);
                            Utils.debugLog(TAG, "Provision processed: '"
                                    + provision.getProvisonPath() + "' prompted user ...");
                        }

                    }

                    // remove provision file
                    try {
                        if (new File(provision.getProvisonPath()).delete()) {
                            cleanupCounter++;
                        }
                    } catch (SecurityException e) {
                        // ignore this exception
                        Utils.debugLog(TAG, "Removing provision not possible: " + e.getMessage() + " ()");
                    }
                }
                Utils.debugLog(TAG, "Provisions done, removed " + cleanupCounter + " provision(s).");
            }
        }
    }

    private List<File> findProvisionFiles(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            return Collections.emptyList();
        }
        File provisionDir = new File(externalFilesDir.getAbsolutePath(), NEW_PROVISIONS_DIR);
        return findProvisionFilesInDir(provisionDir);
    }

    List<File> findProvisionFilesInDir(File file) {
        if (file == null || !file.isDirectory()) {
            return Collections.emptyList();
        }
        try {
            File[] files = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name != null && name.endsWith(".fdrp")) {
                        return true;
                    }
                    return false;
                }
            });
            return files != null ? Arrays.asList(files) : null;
        } catch (Exception e) {
            Utils.debugLog(TAG, "can not search for provisions, can not access: " + file.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    String rot13(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'a' && c <= 'm') || (c >= 'A' && c <= 'M')) {
                sb.append((char) (c + 13));
            } else if ((c >= 'n' && c <= 'z') || (c >= 'N' && c <= 'Z')) {
                sb.append((char) (c - 13));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    String deobfuscate(String obfuscated) {
        try {
            return new String(Base64.decode(rot13(obfuscated), Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // encoding is defined to be utf8, continue gracefully if this magically fails.
            return "";
        }
    }

    List<ProvisionPlaintext> extractProvisionsPlaintext(List<File> files) {
        List<ProvisionPlaintext> result = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                ProvisionPlaintext plain = new ProvisionPlaintext();
                plain.setProvisionPath(file.getAbsolutePath());
                ZipInputStream in = null;
                try {
                    in = new ZipInputStream(new FileInputStream(file));
                    ZipEntry zipEntry;
                    while ((zipEntry = in.getNextEntry()) != null) {
                        String name = zipEntry.getName();
                        if ("repo_provision.json".equals(name)) {
                            if (plain.getRepositoryProvision() != null) {
                                throw new IOException("provision malformed: contains more than one repo provision file.");
                            }
                            plain.setRepositoryProvision(IOUtils.toString(in, Charset.forName("UTF-8")));
                        } else if ("repo_provision.ojson".equals(name)) {
                            if (plain.getRepositoryProvision() != null) {
                                throw new IOException("provision malformed: contains more than one repo provision file.");
                            }
                            plain.setRepositoryProvision(deobfuscate(IOUtils.toString(in, Charset.forName("UTF-8"))));
                        }
                    }
                } catch (FileNotFoundException e) {
                    Utils.debugLog(TAG, String.format("finding provision '%s' failed", file.getPath()), e);
                    continue;
                } catch (IOException e) {
                    Utils.debugLog(TAG, String.format("reading provision '%s' failed", file.getPath()), e);
                    continue;
                } finally {
                    IOUtils.closeQuietly(in);
                }

                result.add(plain);
            }
        }
        return result;
    }

    List<Provision> parseProvisions(List<ProvisionPlaintext> provisionPlaintexts) {

        List<Provision> provisions = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        if (provisionPlaintexts != null) {
            for (ProvisionPlaintext provisionPlaintext : provisionPlaintexts) {
                Provision provision = new Provision();
                provision.setProvisonPath(provisionPlaintext.getProvisionPath());
                try {
                    provision.setRepositoryProvision(
                            mapper.readValue(provisionPlaintext.getRepositoryProvision(), RepositoryProvision.class));
                    provisions.add(provision);
                } catch (IOException e) {
                    Utils.debugLog(TAG, "could not parse repository provision", e);
                }
            }
        }

        return provisions;
    }

    static class ProvisionPlaintext {
        private String provisionPath;
        private String repositoryProvision;

        String getProvisionPath() {
            return provisionPath;
        }

        void setProvisionPath(String provisionPath) {
            this.provisionPath = provisionPath;
        }

        String getRepositoryProvision() {
            return repositoryProvision;
        }

        void setRepositoryProvision(String repositoryProvision) {
            this.repositoryProvision = repositoryProvision;
        }
    }

    static class Provision {
        private String provisonPath;
        private RepositoryProvision repositoryProvision;

        String getProvisonPath() {
            return provisonPath;
        }

        void setProvisonPath(String provisonPath) {
            this.provisonPath = provisonPath;
        }

        RepositoryProvision getRepositoryProvision() {
            return repositoryProvision;
        }

        void setRepositoryProvision(RepositoryProvision repositoryProvision) {
            this.repositoryProvision = repositoryProvision;
        }
    }

    public static class RepositoryProvision {

        private String name;
        private String url;
        private String sigfp;
        private String username;
        private String password;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSigfp() {
            return sigfp;
        }

        public void setSigfp(String sigfp) {
            this.sigfp = sigfp;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
