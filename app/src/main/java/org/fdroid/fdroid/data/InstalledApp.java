package org.fdroid.fdroid.data;

import android.database.Cursor;

public class InstalledApp extends ValueObject {

    private long id;
    private String packageName;
    private int versionCode;
    private String versionName;
    private String applicationLabel;
    private String signature;
    private long lastUpdateTime;
    private String hashType;
    private String hash;

    public InstalledApp(Cursor cursor) {

        checkCursorPosition(cursor);

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String n = cursor.getColumnName(i);
            switch (n) {
                case Schema.InstalledAppTable.Cols._ID:
                    id = cursor.getLong(i);
                    break;
                case Schema.InstalledAppTable.Cols.Package.NAME:
                    packageName = cursor.getString(i);
                    break;
                case Schema.InstalledAppTable.Cols.VERSION_CODE:
                    versionCode = cursor.getInt(i);
                    break;
                case Schema.InstalledAppTable.Cols.VERSION_NAME:
                    versionName = cursor.getString(i);
                    break;
                case Schema.InstalledAppTable.Cols.APPLICATION_LABEL:
                    applicationLabel = cursor.getString(i);
                    break;
                case Schema.InstalledAppTable.Cols.SIGNATURE:
                    signature = cursor.getString(i);
                    break;
                case Schema.InstalledAppTable.Cols.LAST_UPDATE_TIME:
                    lastUpdateTime = cursor.getLong(i);
                    break;
                case Schema.InstalledAppTable.Cols.HASH_TYPE:
                    hashType = cursor.getString(i);
                    break;
                case Schema.InstalledAppTable.Cols.HASH:
                    hash = cursor.getString(i);
                    break;
            }
        }
    }

    public long getId() {
        return id;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getApplicationLabel() {
        return applicationLabel;
    }

    public String getSignature() {
        return signature;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public String getHashType() {
        return hashType;
    }

    public String getHash() {
        return hash;
    }
}
