package org.fdroid.fdroid.data;

import android.provider.BaseColumns;

/**
 * The authoritative reference to each table/column which should exist in the database.
 * Constants from this interface should be used in preference to string literals when referring to
 * the tables/columns in the database.
 */
public interface Schema {

    /**
     * A package is essentially the app that a developer builds and wants you to install on your
     * device. It differs from entries in:
     *  * {@link ApkTable} because they are specific builds of a particular package. Many different
     *    builds of the same package can exist.
     *  * {@link AppMetadataTable} because this is metdata about a package which is specified by a
     *    given repo. Different repos can provide the same package with different descriptions,
     *    categories, etc.
     */
    interface PackageTable {

        String NAME = "fdroid_package";

        interface Cols {
            String ROW_ID = "rowid";
            String PACKAGE_NAME = "packageName";

            /**
             * Metadata about a package (e.g. description, icon, etc) can come from multiple
             * different repos. This is a foreign key to the row in {@link AppMetadataTable} for
             * this package that comes from the repo with the best priority. Although it can be
             * calculated at runtime using an SQL query, it is more efficient to figure out the
             * preferred metadata once, after a repo update, rather than every time we need to know
             * about a package.
             */
            String PREFERRED_METADATA = "preferredMetadata";

            String[] ALL = {
                    ROW_ID, PACKAGE_NAME, PREFERRED_METADATA,
            };
        }
    }

    interface AppPrefsTable {

        String NAME = "fdroid_appPrefs";

        interface Cols extends BaseColumns {
            // Join onto app table via packageName, not appId. The corresponding app row could
            // be deleted and then re-added in the future with the same metadata but a different
            // rowid. This should not cause us to forget the preferences specified by a user.
            String PACKAGE_NAME = "packageName";

            String IGNORE_ALL_UPDATES = "ignoreAllUpdates";
            String IGNORE_THIS_UPDATE = "ignoreThisUpdate";

            String[] ALL = {PACKAGE_NAME, IGNORE_ALL_UPDATES, IGNORE_THIS_UPDATE};
        }
    }

    interface CategoryTable {

        String NAME = "fdroid_category";

        interface Cols {
            String ROW_ID = "rowid";
            String NAME = "name";

            String[] ALL = {
                    ROW_ID, NAME,
            };
        }
    }

    /**
     * An entry in this table signifies that an app is in a particular category. Each repo can
     * classify its apps in separate categories, and so the same record in {@link PackageTable}
     * can be in the same category multiple times, if multiple repos think that is the case.
     * @see CategoryTable
     * @see AppMetadataTable
     */
    interface CatJoinTable {

        String NAME = "fdroid_categoryAppMetadataJoin";

        interface Cols {
            /**
             * Foreign key to {@link AppMetadataTable}.
             * @see AppMetadataTable
             */
            String APP_METADATA_ID = "appMetadataId";

            /**
             * Foreign key to {@link CategoryTable}.
             * @see CategoryTable
             */
            String CATEGORY_ID = "categoryId";
        }
    }

    interface AppMetadataTable {

        String NAME = "fdroid_app";

        interface Cols {
            /**
             * Same as the primary key {@link Cols#ROW_ID}, except aliased as "_id" instead
             * of "rowid". Required for {@link android.content.CursorLoader}s.
             */
            String _ID = "rowid as _id";
            String ROW_ID = "rowid";
            String _COUNT = "_count";
            String IS_COMPATIBLE = "compatible";
            String PACKAGE_ID = "packageId";
            String REPO_ID = "repoId";
            String NAME = "name";
            String SUMMARY = "summary";
            String ICON = "icon";
            String DESCRIPTION = "description";
            String LICENSE = "license";
            String AUTHOR = "author";
            String EMAIL = "email";
            String WEB_URL = "webURL";
            String TRACKER_URL = "trackerURL";
            String SOURCE_URL = "sourceURL";
            String CHANGELOG_URL = "changelogURL";
            String DONATE_URL = "donateURL";
            String BITCOIN_ADDR = "bitcoinAddr";
            String LITECOIN_ADDR = "litecoinAddr";
            String FLATTR_ID = "flattrID";
            String SUGGESTED_VERSION_CODE = "suggestedVercode";
            String UPSTREAM_VERSION_NAME = "upstreamVersion";
            String UPSTREAM_VERSION_CODE = "upstreamVercode";
            String ADDED = "added";
            String LAST_UPDATED = "lastUpdated";
            String CATEGORIES = "categories";
            String ANTI_FEATURES = "antiFeatures";
            String REQUIREMENTS = "requirements";
            String ICON_URL = "iconUrl";
            String ICON_URL_LARGE = "iconUrlLarge";

            interface SuggestedApk {
                String VERSION_NAME = "suggestedApkVersion";
            }

            interface InstalledApp {
                String VERSION_CODE = "installedVersionCode";
                String VERSION_NAME = "installedVersionName";
                String SIGNATURE = "installedSig";
            }

            interface Package {
                String PACKAGE_NAME = "package_packageName";
            }

            /**
             * Each of the physical columns in the sqlite table. Differs from {@link Cols#ALL} in
             * that it doesn't include fields which are aliases of other fields (e.g. {@link Cols#_ID}
             * or which are from other related tables (e.g. {@link Cols.SuggestedApk#VERSION_NAME}).
             */
            String[] ALL_COLS = {
                    ROW_ID, PACKAGE_ID, REPO_ID, IS_COMPATIBLE, NAME, SUMMARY, ICON, DESCRIPTION,
                    LICENSE, AUTHOR, EMAIL, WEB_URL, TRACKER_URL, SOURCE_URL,
                    CHANGELOG_URL, DONATE_URL, BITCOIN_ADDR, LITECOIN_ADDR, FLATTR_ID,
                    UPSTREAM_VERSION_NAME, UPSTREAM_VERSION_CODE, ADDED, LAST_UPDATED,
                    CATEGORIES, ANTI_FEATURES, REQUIREMENTS, ICON_URL, ICON_URL_LARGE,
                    SUGGESTED_VERSION_CODE,
            };

            /**
             * Superset of {@link Cols#ALL_COLS} including fields from other tables and also an alias
             * to satisfy the Android requirement for an "_ID" field.
             * @see Cols#ALL_COLS
             */
            String[] ALL = {
                    _ID, ROW_ID, REPO_ID, IS_COMPATIBLE, NAME, SUMMARY, ICON, DESCRIPTION,
                    LICENSE, AUTHOR, EMAIL, WEB_URL, TRACKER_URL, SOURCE_URL,
                    CHANGELOG_URL, DONATE_URL, BITCOIN_ADDR, LITECOIN_ADDR, FLATTR_ID,
                    UPSTREAM_VERSION_NAME, UPSTREAM_VERSION_CODE, ADDED, LAST_UPDATED,
                    CATEGORIES, ANTI_FEATURES, REQUIREMENTS, ICON_URL, ICON_URL_LARGE,
                    SUGGESTED_VERSION_CODE, SuggestedApk.VERSION_NAME,
                    InstalledApp.VERSION_CODE, InstalledApp.VERSION_NAME,
                    InstalledApp.SIGNATURE, Package.PACKAGE_NAME,
            };
        }
    }

    /**
     * This table stores details of all the application versions we
     * know about. Each relates directly back to an entry in TABLE_APP.
     * This information is retrieved from the repositories.
     */
    interface ApkTable {

        String NAME = "fdroid_apk";

        interface Cols extends BaseColumns {
            String _COUNT_DISTINCT = "countDistinct";

            /**
             * Foreign key to the {@link AppMetadataTable}.
             */
            String APP_ID          = "appId";
            String ROW_ID          = "rowid";
            String VERSION_NAME    = "version";
            String REPO_ID         = "repo";
            String HASH            = "hash";
            String VERSION_CODE    = "vercode";
            String NAME            = "apkName";
            String SIZE            = "size";
            String SIGNATURE       = "sig";
            String SOURCE_NAME     = "srcname";
            String MIN_SDK_VERSION = "minSdkVersion";
            String TARGET_SDK_VERSION = "targetSdkVersion";
            String MAX_SDK_VERSION = "maxSdkVersion";
            String OBB_MAIN_FILE   = "obbMainFile";
            String OBB_MAIN_FILE_SHA256 = "obbMainFileSha256";
            String OBB_PATCH_FILE  = "obbPatchFile";
            String OBB_PATCH_FILE_SHA256 = "obbPatchFileSha256";
            String REQUESTED_PERMISSIONS = "permissions";
            String FEATURES        = "features";
            String NATIVE_CODE     = "nativecode";
            String HASH_TYPE       = "hashType";
            String ADDED_DATE      = "added";
            String IS_COMPATIBLE   = "compatible";
            String INCOMPATIBLE_REASONS = "incompatibleReasons";

            interface Repo {
                String VERSION = "repoVersion";
                String ADDRESS = "repoAddress";
            }

            interface Package {
                String PACKAGE_NAME = "package_packageName";
            }

            /**
             * @see AppMetadataTable.Cols#ALL_COLS
             */
            String[] ALL_COLS = {
                    APP_ID, VERSION_NAME, REPO_ID, HASH, VERSION_CODE, NAME,
                    SIZE, SIGNATURE, SOURCE_NAME, MIN_SDK_VERSION, TARGET_SDK_VERSION, MAX_SDK_VERSION,
                    OBB_MAIN_FILE, OBB_MAIN_FILE_SHA256, OBB_PATCH_FILE, OBB_PATCH_FILE_SHA256,
                    REQUESTED_PERMISSIONS, FEATURES, NATIVE_CODE, HASH_TYPE, ADDED_DATE,
                    IS_COMPATIBLE, INCOMPATIBLE_REASONS,
            };

            /**
             * @see AppMetadataTable.Cols#ALL
             */
            String[] ALL = {
                    _ID, APP_ID, Package.PACKAGE_NAME, VERSION_NAME, REPO_ID, HASH, VERSION_CODE, NAME,
                    SIZE, SIGNATURE, SOURCE_NAME, MIN_SDK_VERSION, TARGET_SDK_VERSION, MAX_SDK_VERSION,
                    OBB_MAIN_FILE, OBB_MAIN_FILE_SHA256, OBB_PATCH_FILE, OBB_PATCH_FILE_SHA256,
                    REQUESTED_PERMISSIONS, FEATURES, NATIVE_CODE, HASH_TYPE, ADDED_DATE,
                    IS_COMPATIBLE, Repo.VERSION, Repo.ADDRESS, INCOMPATIBLE_REASONS,
            };
        }
    }

    interface RepoTable {

        String NAME = "fdroid_repo";

        interface Cols extends BaseColumns {

            String ADDRESS      = "address";
            String NAME         = "name";
            String DESCRIPTION  = "description";
            String IN_USE       = "inuse";
            String PRIORITY     = "priority";
            String SIGNING_CERT = "pubkey";
            String FINGERPRINT  = "fingerprint";
            String MAX_AGE      = "maxage";
            String LAST_ETAG    = "lastetag";
            String LAST_UPDATED = "lastUpdated";
            String VERSION      = "version";
            String IS_SWAP      = "isSwap";
            String USERNAME     = "username";
            String PASSWORD     = "password";
            String TIMESTAMP    = "timestamp";
            String PUSH_REQUESTS = "pushRequests";

            String[] ALL = {
                    _ID, ADDRESS, NAME, DESCRIPTION, IN_USE, PRIORITY, SIGNING_CERT,
                    FINGERPRINT, MAX_AGE, LAST_UPDATED, LAST_ETAG, VERSION, IS_SWAP,
                    USERNAME, PASSWORD, TIMESTAMP, PUSH_REQUESTS,
            };
        }
    }

    interface InstalledAppTable {

        String NAME = "fdroid_installedApp";

        interface Cols {
            String _ID = "rowid as _id"; // Required for CursorLoaders
            String PACKAGE_NAME = "appId";
            String VERSION_CODE = "versionCode";
            String VERSION_NAME = "versionName";
            String APPLICATION_LABEL = "applicationLabel";
            String SIGNATURE = "sig";
            String LAST_UPDATE_TIME = "lastUpdateTime";
            String HASH_TYPE = "hashType";
            String HASH = "hash";

            String[] ALL = {
                    _ID, PACKAGE_NAME, VERSION_CODE, VERSION_NAME, APPLICATION_LABEL,
                    SIGNATURE, LAST_UPDATE_TIME, HASH_TYPE, HASH,
            };
        }
    }

}
