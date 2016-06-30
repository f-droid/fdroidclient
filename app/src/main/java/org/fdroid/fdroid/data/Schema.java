package org.fdroid.fdroid.data;

/**
 * The authoritative reference to each table/column which should exist in the database.
 * Constants from this interface should be used in preference to string literals when referring to
 * the tables/columns in the database.
 */
interface Schema {

    interface AppTable {
        String NAME = DBHelper.TABLE_APP;
        interface Cols extends AppProvider.DataColumns {}
    }

    interface ApkTable {
        String NAME = DBHelper.TABLE_APK;
        interface Cols extends ApkProvider.DataColumns {}
    }

    interface RepoTable {
        String NAME = DBHelper.TABLE_REPO;
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

            String[] ALL = {
                    _ID, ADDRESS, NAME, DESCRIPTION, IN_USE, PRIORITY, SIGNING_CERT,
                    FINGERPRINT, MAX_AGE, LAST_UPDATED, LAST_ETAG, VERSION, IS_SWAP,
                    USERNAME, PASSWORD, TIMESTAMP,
            };
        }
    }

}
