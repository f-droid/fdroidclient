package org.fdroid.fdroid.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.core.util.ObjectsCompat;

import org.fdroid.database.AppPrefs;
import org.fdroid.database.AppPrefsDao;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.database.InitialRepository;
import org.fdroid.database.Repository;
import org.fdroid.database.RepositoryDao;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class ContentProviderMigrator {

    private static final String OLD_DB_NAME = "fdroid";

    boolean needsMigration(Context context) {
        for (String db : context.databaseList()) {
            if (OLD_DB_NAME.equals(db)) return true;
        }
        return false;
    }

    void runMigrations(Context context, FDroidDatabase db) {
        try (ContentProviderDbHelper helper = new ContentProviderDbHelper(context)) {
            SQLiteDatabase oldDb = helper.getReadableDatabase();
            migrateOldRepos(db, oldDb);
            migrateIgnoredUpdates(db, oldDb);
        }
    }

    private void migrateOldRepos(FDroidDatabase db, SQLiteDatabase oldDb) {
        RepositoryDao repoDao = db.getRepositoryDao();
        List<Repository> repos = repoDao.getRepositories();
        int weight = repos.isEmpty() ? 0 : repos.get(repos.size() - 1).getWeight();

        String[] projection = new String[]{"name", "address", "pubkey", "inuse", "userMirrors", "disabledMirrors",
                "username", "password"};
        try (Cursor c = oldDb.query("fdroid_repo", projection, null, null, null, null, null)) {
            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                String address = c.getString(c.getColumnIndexOrThrow("address"));
                String certificateDb = c.getString(c.getColumnIndexOrThrow("pubkey"));
                if (certificateDb == null) continue;
                String certificate = certificateDb.toLowerCase(Locale.ROOT);
                boolean enabled = c.getInt(c.getColumnIndexOrThrow("inuse")) == 1;
                String userMirrorsDb = c.getString(c.getColumnIndexOrThrow("userMirrors"));
                List<String> userMirrors = userMirrorsDb == null ? null : Arrays.asList(userMirrorsDb.split(","));
                String disabledMirrorsDb = c.getString(c.getColumnIndexOrThrow("disabledMirrors"));
                List<String> disabledMirrors = disabledMirrorsDb == null ?
                        null : Arrays.asList(disabledMirrorsDb.split(","));
                String username = c.getString(c.getColumnIndexOrThrow("username"));
                String password = c.getString(c.getColumnIndexOrThrow("password"));
                // find existing repos by address, because F-Droid archive re-uses certificate
                Repository repo = null;
                for (Repository r : repos) {
                    if (r.getAddress().equals(address)) {
                        repo = r;
                        break;
                    }
                }
                // add new repo if not existing
                if (repo == null) { // new repo to be added to new DB
                    InitialRepository newRepo = new InitialRepository(name, address, "", certificate,
                            0, enabled, ++weight);
                    long repoId = repoDao.insert(newRepo);
                    repo = ObjectsCompat.requireNonNull(repoDao.getRepository(repoId));
                } else { // old repo that may need an update for the new DB
                    if (!certificate.equals(repo.getCertificate())) {
                        continue; // don't update if certificate does not match
                    }
                }
                // update settings of new or existing repo
                if (repo.getEnabled() != enabled) {
                    repoDao.setRepositoryEnabled(repo.getRepoId(), enabled);
                }
                if (userMirrors != null && !userMirrors.isEmpty()) {
                    repoDao.updateUserMirrors(repo.getRepoId(), userMirrors);
                }
                if (disabledMirrors != null && !disabledMirrors.isEmpty()) {
                    repoDao.updateDisabledMirrors(repo.getRepoId(), disabledMirrors);
                }
                if (username != null || password != null) {
                    repoDao.updateUsernameAndPassword(repo.getRepoId(), username, password);
                }
            }
        } catch (SQLiteException e) {
            if (e.getMessage() != null && e.getMessage().contains("disabledMirrors")) {
                Log.e("DbHelper", "disabledMirrors column missing. Can't migrate. ", e);
            } else throw e;
        }
    }

    private void migrateIgnoredUpdates(FDroidDatabase db, SQLiteDatabase oldDb) {
        AppPrefsDao appPrefsDao = db.getAppPrefsDao();

        String[] projection = new String[]{"packageName", "ignoreThisUpdate", "ignoreAllUpdates"};
        try (Cursor c = oldDb.query("fdroid_appPrefs", projection, null, null, null, null, null)) {
            while (c.moveToNext()) {
                String packageName = c.getString(c.getColumnIndexOrThrow("packageName"));
                long ignoreVersionCode = c.getLong(c.getColumnIndexOrThrow("ignoreThisUpdate"));
                boolean ignoreAllUpdates = c.getInt(c.getColumnIndexOrThrow("ignoreAllUpdates")) == 1;

                // ignored version code is max code to ignore all updates, or a specific one to ignore
                long v = ignoreAllUpdates ? Long.MAX_VALUE : ignoreVersionCode;
                // this is a new DB, so we can just start to insert new AppPrefs
                appPrefsDao.update(new AppPrefs(packageName, v, null));
            }
        }
    }

    void removeOldDb(Context context) {
        context.deleteDatabase(OLD_DB_NAME);
    }

    private static class ContentProviderDbHelper extends SQLiteOpenHelper {
        ContentProviderDbHelper(Context context) {
            super(context, OLD_DB_NAME, null, 85);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}
