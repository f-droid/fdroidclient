package org.fdroid.fdroid;

import android.os.Environment;
import android.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Michael Pöhn (michael.poehn@fsfe.org)
 */
public class Provisioner {

    public static final String TAG = "Provisioner";

    private static final String DEFAULT_PROVISION_DIR = Environment.getExternalStorageDirectory().getPath();

    protected Provisioner() {
    }

    /**
     * search for provision files and process them
     */
    public void scanAndProcess() {

        List<File> files = findProvisionFiles();
        List<ProvisionPlaintext> plaintexts = extractProvisionsPlaintext(files);
        files.clear();

        List<Provision> provisions = parseProvisions(plaintexts);
        plaintexts.clear();

        // TODO: do something useful with provisions, like prompting users
        for (Provision provision : provisions) {
            if (provision.getRepositoryProvision() != null) {
                RepositoryProvision repo = provision.getRepositoryProvision();
                Utils.debugLog(TAG, "repository:"
                        + " " + repo.getName()
                        + " " + repo.getUrl()
                        + " " + repo.getUsername());
            }
        }
    }

    /**
     * @return List of
     */
    public List<File> findProvisionFiles() {
        return findProvisionFilesInDir(new File(DEFAULT_PROVISION_DIR));
    }

    protected List<File> findProvisionFilesInDir(File file) {
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
    }

    String rot13(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'a' && c <= 'm') || (c >= 'A' && c <= 'M')) {
                sb.append(c + 13);
            } else if ((c >= 'n' && c <= 'z') || (c >= 'N' && c <= 'Z')) {
                sb.append(c - 13);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    protected String deobfuscate(String obfuscated) {
        try {
            return new String(Base64.decode(rot13(obfuscated), Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // encoding is defined to be utf8, continue gracefully if this magically fails.
            return "";
        }
    }

    protected List<ProvisionPlaintext> extractProvisionsPlaintext(List<File> files) {
        List<ProvisionPlaintext> result = new ArrayList<>();
        for (File file : files) {
            ProvisionPlaintext plain = new ProvisionPlaintext();
            plain.setProvisionPath(file.getAbsolutePath());
            ZipInputStream in = null;
            try {
                in = new ZipInputStream(new FileInputStream(file));
                ZipEntry zipEntry = null;
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
        return result;
    }

    public List<Provision> parseProvisions(List<ProvisionPlaintext> provisionPlaintexts) {

        List<Provision> provisions = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        for (ProvisionPlaintext provisionPlaintext : provisionPlaintexts) {
            Provision provision = new Provision();
            provision.setProvisonPath(provisionPlaintext.getProvisionPath());
            try {
                provision.setRepositoryProvision(
                        mapper.readValue(provisionPlaintext.getRepositoryProvision(), RepositoryProvision.class));
            } catch (IOException e) {
                Utils.debugLog(TAG, "could not parse repository provision", e);
            }
            provisions.add(provision);
        }

        return provisions;
    }

    public static class ProvisionPlaintext {
        private String provisionPath;
        private String repositoryProvision;

        public String getProvisionPath() {
            return provisionPath;
        }

        public void setProvisionPath(String provisionPath) {
            this.provisionPath = provisionPath;
        }

        public String getRepositoryProvision() {
            return repositoryProvision;
        }

        public void setRepositoryProvision(String repositoryProvision) {
            this.repositoryProvision = repositoryProvision;
        }
    }

    public static class Provision {
        private String provisonPath;
        private RepositoryProvision repositoryProvision;

        public String getProvisonPath() {
            return provisonPath;
        }

        public void setProvisonPath(String provisonPath) {
            this.provisonPath = provisonPath;
        }

        public RepositoryProvision getRepositoryProvision() {
            return repositoryProvision;
        }

        public void setRepositoryProvision(RepositoryProvision repositoryProvision) {
            this.repositoryProvision = repositoryProvision;
        }
    }

    public static class RepositoryProvision {

        private String name;
        private String url;
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
