package org.fdroid.fdroid;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoPersister;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Receives the index data about all available apps and packages via the V1
 * JSON data {@link #DATA_FILE_NAME}, embedded in a signed jar
 * {@link #SIGNED_FILE_NAME}.  This uses the Jackson library to parse the JSON,
 * with {@link App} and {@link Apk} being instantiated directly from the JSON
 * by Jackson. This is possible but not wise to do with {@link Repo} since that
 * class has many fields that are related to security components of the
 * implementation internal to this app.
 * <p>
 * All non-{@code public} fields and fields tagged with {@code @JsonIgnore} are
 * ignored. All methods are ignored unless they are tagged with {@code @JsonProperty}.
 * This setup prevents the situation where future developers add variables to the
 * App/Apk classes, resulting in malicious servers being able to populate those
 * variables.
 */
public class IndexV1Updater extends RepoUpdater {
    public static final String TAG = "IndexV1Updater";

    private static final String SIGNED_FILE_NAME = "index-v1.jar";
    public static final String DATA_FILE_NAME = "index-v1.json";

    public IndexV1Updater(@NonNull Context context, @NonNull Repo repo) {
        super(context, repo);
    }

    @Override
    protected String getIndexUrl(@NonNull Repo repo) {
        return Uri.parse(repo.address).buildUpon().appendPath(SIGNED_FILE_NAME).build().toString();
    }

    /**
     * @return whether this successfully found an index of this version
     * @throws RepoUpdater.UpdateException
     */
    @Override
    public boolean update() throws RepoUpdater.UpdateException {

        if (repo.isSwap) {
            // swap repos do not support index-v1
            return false;
        }
        Downloader downloader = null;
        InputStream indexInputStream = null;
        try {
            // read file name from file
            final Uri dataUri = Uri.parse(indexUrl);
            downloader = DownloaderFactory.create(context, dataUri.toString());
            downloader.setCacheTag(repo.lastetag);
            downloader.setListener(downloadListener);
            downloader.download();
            if (downloader.isNotFound()) {
                return false;
            }
            hasChanged = downloader.hasChanged();

            if (!hasChanged) {
                return true;
            }

            JarFile jarFile = new JarFile(downloader.outputFile, true);
            JarEntry indexEntry = (JarEntry) jarFile.getEntry(DATA_FILE_NAME);
            indexInputStream = new ProgressBufferedInputStream(jarFile.getInputStream(indexEntry),
                    processIndexListener, new URL(repo.address), (int) indexEntry.getSize());
            processIndexV1(indexInputStream, indexEntry, downloader.getCacheTag());

        } catch (IOException e) {
            if (downloader != null) {
                FileUtils.deleteQuietly(downloader.outputFile);
            }
            throw new RepoUpdater.UpdateException(repo, "Error getting index file", e);
        } catch (InterruptedException e) {
            // ignored if canceled, the local database just won't be updated
        }

        return true;
    }

    /**
     * Get the standard {@link ObjectMapper} instance used for parsing {@code index-v1.json}.
     * This ignores unknown properties so that old releases won't crash when new things are
     * added to {@code index-v1.json}.  This is required for both forward compatibility,
     * but also because ignoring such properties when coming from a malicious server seems
     * reasonable anyway.
     */
    public static ObjectMapper getObjectMapperInstance(long repoId) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setInjectableValues(new InjectableValues.Std().addValue("repoId", repoId));
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        return mapper;
    }

    /**
     * Parses the index and feeds it to the database via {@link Repo}, {@link App},
     * and {@link Apk} instances.  This uses {@link RepoPersister}  to add the apps
     * and packages to the database in {@link RepoPersister#saveToDb(App, List)}
     * to write the {@link Repo}, and commit the whole thing in
     * {@link RepoPersister#commit(ContentValues, long)}.  One confusing thing about this
     * whole process is that {@link RepoPersister} needs to first create and entry
     * in the database, then fetch the ID from the database to populate
     * {@link Repo#id}.  That has to happen first, then the rest of the {@code Repo}
     * data must be added later.
     *
     * @param indexInputStream {@link InputStream} to {@code index-v1.json}
     * @param etag             the {@code etag} value from HTTP headers
     * @throws IOException
     * @throws UpdateException
     */
    public void processIndexV1(InputStream indexInputStream, JarEntry indexEntry, String etag)
            throws IOException, UpdateException {
        Utils.Profiler profiler = new Utils.Profiler(TAG);
        profiler.log("Starting to process index-v1.json");
        ObjectMapper mapper = getObjectMapperInstance(repo.getId());
        JsonFactory f = mapper.getFactory();
        JsonParser parser = f.createParser(indexInputStream);
        HashMap<String, Object> repoMap = null;
        App[] apps = null;
        Map<String, String[]> requests = null;
        Map<String, List<Apk>> packages = null;

        parser.nextToken(); // go into the main object block
        while (true) {
            String fieldName = parser.nextFieldName();
            if (fieldName == null) {
                break;
            }
            switch (fieldName) {
                case "repo":
                    repoMap = parseRepo(mapper, parser);
                    break;
                case "requests":
                    requests = parseRequests(mapper, parser);
                    break;
                case "apps":
                    apps = parseApps(mapper, parser);
                    break;
                case "packages":
                    packages = parsePackages(mapper, parser);
                    break;
            }
        }
        parser.close(); // ensure resources get cleaned up timely and properly
        profiler.log("Finished processing index-v1.json. Now verifying certificate...");

        if (repoMap == null) {
            return;
        }

        long timestamp = (Long) repoMap.get("timestamp") / 1000;

        if (repo.timestamp > timestamp) {
            throw new RepoUpdater.UpdateException(repo, "index.jar is older that current index! "
                    + timestamp + " < " + repo.timestamp);
        }

        X509Certificate certificate = getSigningCertFromJar(indexEntry);
        verifySigningCertificate(certificate);

        profiler.log("Certificate verified. Now saving to database...");

        // timestamp is absolutely required
        repo.timestamp = timestamp;
        // below are optional, can be null
        repo.lastetag = etag;
        repo.name = getStringRepoValue(repoMap, "name");
        repo.icon = getStringRepoValue(repoMap, "icon");
        repo.description = getStringRepoValue(repoMap, "description");
        repo.mirrors = getStringArrayRepoValue(repoMap, "mirrors");
        // below are optional, can be default value
        repo.maxage = getIntRepoValue(repoMap, "maxage");
        repo.version = getIntRepoValue(repoMap, "version");

        RepoPersister repoPersister = new RepoPersister(context, repo);
        if (apps != null && apps.length > 0) {
            int appCount = 0;
            for (App app : apps) {
                appCount++;
                List<Apk> apks = null;
                if (packages != null) {
                    apks = packages.get(app.packageName);
                }

                if (apks == null) {
                    Log.i(TAG, "processIndexV1 empty packages");
                    apks = new ArrayList<>(0);
                }

                if (apks.size() > 0) {
                    app.preferredSigner = apks.get(0).sig;
                }

                if (appCount % 50 == 0) {
                    notifyProcessingApps(appCount, apps.length);
                }

                repoPersister.saveToDb(app, apks);
            }
        }
        profiler.log("Saved to database, but only a temporary table. Now persisting to database...");
        notifyCommittingToDb();

        ContentValues contentValues = new ContentValues();
        contentValues.put(Schema.RepoTable.Cols.LAST_UPDATED, Utils.formatTime(new Date(), ""));
        contentValues.put(Schema.RepoTable.Cols.TIMESTAMP, repo.timestamp);
        contentValues.put(Schema.RepoTable.Cols.LAST_ETAG, repo.lastetag);
        if (repo.version != Repo.INT_UNSET_VALUE) {
            contentValues.put(Schema.RepoTable.Cols.VERSION, repo.version);
        }
        if (repo.maxage != Repo.INT_UNSET_VALUE) {
            contentValues.put(Schema.RepoTable.Cols.MAX_AGE, repo.maxage);
        }
        if (repo.description != null) {
            contentValues.put(Schema.RepoTable.Cols.DESCRIPTION, repo.description);
        }
        if (repo.name != null) {
            contentValues.put(Schema.RepoTable.Cols.NAME, repo.name);
        }
        if (repo.icon != null) {
            contentValues.put(Schema.RepoTable.Cols.ICON, repo.icon);
        }
        if (repo.mirrors != null && repo.mirrors.length > 0) {
            contentValues.put(Schema.RepoTable.Cols.MIRRORS, Utils.serializeCommaSeparatedString(repo.mirrors));
        }
        repoPersister.commit(contentValues, repo.getId());
        profiler.log("Persited to database.");


        // TODO RepoUpdater.processRepoPushRequests(context, repoPushRequestList);
        Utils.debugLog(TAG, "Repo Push Requests: " + requests);
    }

    private int getIntRepoValue(Map<String, Object> repoMap, String key) {
        Object value = repoMap.get(key);
        if (value != null && value instanceof Integer) {
            return (Integer) value;
        }
        return Repo.INT_UNSET_VALUE;
    }

    private String getStringRepoValue(Map<String, Object> repoMap, String key) {
        Object value = repoMap.get(key);
        if (value != null && value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String[] getStringArrayRepoValue(Map<String, Object> repoMap, String key) {
        Object value = repoMap.get(key);
        if (value != null && value instanceof ArrayList) {
            ArrayList<String> list = (ArrayList<String>) value;
            return list.toArray(new String[list.size()]);
        }
        return null;
    }

    private HashMap<String, Object> parseRepo(ObjectMapper mapper, JsonParser parser) throws IOException {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        parser.nextToken();
        parser.nextToken();
        return mapper.readValue(parser, typeRef);
    }

    private Map<String, String[]> parseRequests(ObjectMapper mapper, JsonParser parser) throws IOException {
        TypeReference<HashMap<String, String[]>> typeRef = new TypeReference<HashMap<String, String[]>>() {
        };
        parser.nextToken(); // START_OBJECT
        return mapper.readValue(parser, typeRef);
    }

    private App[] parseApps(ObjectMapper mapper, JsonParser parser) throws IOException {
        TypeReference<App[]> typeRef = new TypeReference<App[]>() {
        };
        parser.nextToken(); // START_ARRAY
        return mapper.readValue(parser, typeRef);
    }

    private Map<String, List<Apk>> parsePackages(ObjectMapper mapper, JsonParser parser) throws IOException {
        TypeReference<HashMap<String, List<Apk>>> typeRef = new TypeReference<HashMap<String, List<Apk>>>() {
        };
        parser.nextToken(); // START_OBJECT
        return mapper.readValue(parser, typeRef);
    }

    /**
     * Verify that the signing certificate used to sign {@link #SIGNED_FILE_NAME}
     * matches the signing stored in the database for this repo.  {@link #repo} and
     * {@code repo.signingCertificate} must be pre-loaded from the database before
     * running this, if this is an existing repo.  If the repo does not exist,
     * this will run the TOFU process.
     * <p>
     * Index V1 works with two copies of the signing certificate:
     * <li>in the downloaded jar</li>
     * <li>stored in the local database</li>
     * <p>
     * A new repo can be added with or without the fingerprint of the signing
     * certificate.  If no fingerprint is supplied, then do a pure TOFU and just
     * store the certificate as valid.  If there is a fingerprint, then first
     * check that the signing certificate in the jar matches that fingerprint.
     * <p>
     * This is also responsible for adding the {@link Repo} instance to the
     * database for the first time.
     * <p>
     * This is the same as {@link RepoUpdater#verifyCerts(String, X509Certificate)},
     * {@link RepoUpdater#verifyAndStoreTOFUCerts(String, X509Certificate)}, and
     * {@link RepoUpdater#assertSigningCertFromXmlCorrect()} except there is no
     * embedded copy of the signing certificate in the index data.
     *
     * @param rawCertFromJar the {@link X509Certificate} embedded in the downloaded jar
     * @see RepoUpdater#verifyAndStoreTOFUCerts(String, X509Certificate)
     * @see RepoUpdater#verifyCerts(String, X509Certificate)
     * @see RepoUpdater#assertSigningCertFromXmlCorrect()
     */
    private void verifySigningCertificate(X509Certificate rawCertFromJar) throws SigningException {
        String certFromJar = Hasher.hex(rawCertFromJar);

        if (TextUtils.isEmpty(certFromJar)) {
            throw new SigningException(repo,
                    SIGNED_FILE_NAME + " must have an included signing certificate!");
        }

        if (repo.signingCertificate == null) {
            if (repo.fingerprint != null) {
                String fingerprintFromJar = Utils.calcFingerprint(rawCertFromJar);
                if (!repo.fingerprint.equalsIgnoreCase(fingerprintFromJar)) {
                    throw new SigningException(repo,
                            "Supplied certificate fingerprint does not match!");
                }
            }
            Utils.debugLog(TAG, "Saving new signing certificate to database for " + repo.address);
            ContentValues values = new ContentValues(2);
            values.put(Schema.RepoTable.Cols.LAST_UPDATED, Utils.formatDate(new Date(), ""));
            values.put(Schema.RepoTable.Cols.SIGNING_CERT, Hasher.hex(rawCertFromJar));
            RepoProvider.Helper.update(context, repo, values);
            repo.signingCertificate = certFromJar;
        }

        if (TextUtils.isEmpty(repo.signingCertificate)) {
            throw new SigningException(repo, "A empty repo signing certificate is invalid!");
        }

        if (repo.signingCertificate.equals(certFromJar)) {
            return; // we have a match!
        }

        throw new SigningException(repo, "Signing certificate does not match!");
    }

}
