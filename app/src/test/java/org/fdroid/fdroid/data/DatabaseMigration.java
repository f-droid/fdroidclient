package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.Utils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class DatabaseMigration {

    protected ShadowContentResolver contentResolver;
    protected ContextWrapper context;

    @Before
    public final void setupBase() {
        contentResolver = Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver());
        context = TestUtils.createContextWithContentResolver(contentResolver);
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
    }

    @Test
    public void migrationsFromDbVersion42Onward() {
        Preferences.setupForTests(context);
        SQLiteOpenHelper opener = new MigrationRunningOpenHelper(context);
        opener.getReadableDatabase();
    }

    /**
     * The database created by this in {@link MigrationRunningOpenHelper#onCreate(SQLiteDatabase)}
     * should be identical to the one which was created by F-Droid circa git tag "db-version/42".
     * After creating the database, this will then ask the base
     * {@link DBHelper#onUpgrade(SQLiteDatabase, int, int)} method to run up until the current
     * {@link DBHelper#DB_VERSION}.
     */
    class MigrationRunningOpenHelper extends DBHelper {

        public static final String TABLE_REPO = "fdroid_repo";

        MigrationRunningOpenHelper(Context context) {
            super(context);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createAppTable(db);
            createApkTable(db);
            createRepoTable(db);
            insertRepos(db);
            onUpgrade(db, 42, DBHelper.DB_VERSION);
        }

        private void createAppTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE fdroid_app ("
                    + "id text not null, "
                    + "name text not null, "
                    + "summary text not null, "
                    + "icon text, "
                    + "description text not null, "
                    + "license text not null, "
                    + "webURL text, "
                    + "trackerURL text, "
                    + "sourceURL text, "
                    + "suggestedVercode text,"
                    + "upstreamVersion text,"
                    + "upstreamVercode integer,"
                    + "antiFeatures string,"
                    + "donateURL string,"
                    + "bitcoinAddr string,"
                    + "litecoinAddr string,"
                    + "dogecoinAddr string,"
                    + "flattrID string,"
                    + "requirements string,"
                    + "categories string,"
                    + "added string,"
                    + "lastUpdated string,"
                    + "compatible int not null,"
                    + "ignoreAllUpdates int not null,"
                    + "ignoreThisUpdate int not null,"
                    + "iconUrl text, "
                    + "primary key(id));");

            db.execSQL("create index app_id on fdroid_app (id);");
        }

        private void createApkTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE fdroid_apk ( "
                    + "id text not null, "
                    + "version text not null, "
                    + "repo integer not null, "
                    + "hash text not null, "
                    + "vercode int not null,"
                    + "apkName text not null, "
                    + "size int not null, "
                    + "sig string, "
                    + "srcname string, "
                    + "minSdkVersion integer, "
                    + "maxSdkVersion integer, "
                    + "permissions string, "
                    + "features string, "
                    + "nativecode string, "
                    + "hashType string, "
                    + "added string, "
                    + "compatible int not null, "
                    + "incompatibleReasons text, "
                    + "primary key(id, vercode)"
                    + ");");
            db.execSQL("create index apk_vercode on fdroid_apk (vercode);");
            db.execSQL("create index apk_id on fdroid_apk (id);");
        }

        private void createRepoTable(SQLiteDatabase db) {
            db.execSQL("create table " + TABLE_REPO + " ("
                    + "_id integer primary key, "
                    + "address text not null, "
                    + "name text, description text, inuse integer not null, "
                    + "priority integer not null, pubkey text, fingerprint text, "
                    + "maxage integer not null default 0, "
                    + "version integer not null default 0, "
                    + "lastetag text, lastUpdated string);");
        }

        private void insertRepos(SQLiteDatabase db) {
            String pubKey = "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef"; // NOCHECKSTYLE LineLength
            String fingerprint = Utils.calcFingerprint(pubKey);

            ContentValues fdroidValues = new ContentValues();
            fdroidValues.put("address", "https://f-droid.org/repo");
            fdroidValues.put("name", "F-Droid");
            fdroidValues.put("description", "The official FDroid repository. Applications in this repository are mostly built directory from the source code. Some are official binaries built by the original application developers - these will be replaced by source-built versions over time."); // NOCHECKSTYLE LineLength
            fdroidValues.put("pubkey", pubKey);
            fdroidValues.put("fingerprint", fingerprint);
            fdroidValues.put("maxage", 0);
            fdroidValues.put("inuse", 1);
            fdroidValues.put("priority", 10);
            fdroidValues.put("lastetag", (String) null);
            db.insert(TABLE_REPO, null, fdroidValues);

            ContentValues archiveValues = new ContentValues();
            archiveValues.put("address", "https://f-droid.org/archive");
            archiveValues.put("name", "F-Droid Archive");
            archiveValues.put("description", "The archive repository of the F-Droid client. This contains older versions of applications from the main repository."); // NOCHECKSTYLE LineLength
            archiveValues.put("pubkey", pubKey);
            archiveValues.put("fingerprint", fingerprint);
            archiveValues.put("maxage", 0);
            archiveValues.put("inuse", 0);
            archiveValues.put("priority", 20);
            archiveValues.put("lastetag", (String) null);
            db.insert(TABLE_REPO, null, archiveValues);
        }

    }

}
