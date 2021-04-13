package org.fdroid.fdroid.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.fdroid.fdroid.BuildConfig;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public abstract class FDroidProvider extends ContentProvider {

    public static final String TAG = "FDroidProvider";

    static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".data";

    static final int CODE_LIST = 1;
    static final int CODE_SINGLE = 2;

    private boolean applyingBatch;

    protected abstract String getTableName();

    protected abstract String getProviderName();

    /**
     * Should always be the same as the provider:name in the AndroidManifest
     */
    public final String getName() {
        return AUTHORITY + "." + getProviderName();
    }

    /**
     * Tells us if we are in the middle of a batch of operations. Allows us to
     * decide not to notify the content resolver of changes,
     * every single time we do something during many operations.
     * Based on http://stackoverflow.com/a/15886915.
     */
    protected final boolean isApplyingBatch() {
        return this.applyingBatch;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        ContentProviderResult[] result = null;
        applyingBatch = true;
        final SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            result = super.applyBatch(operations);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            applyingBatch = false;
        }
        return result;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    protected final synchronized SQLiteDatabase db() {
        return DBHelper.getInstance(getContext()).getWritableDatabase();
    }

    @Override
    public String getType(@NonNull Uri uri) {
        String type;
        switch (getMatcher().match(uri)) {
            case CODE_LIST:
                type = "dir";
                break;
            case CODE_SINGLE:
            default:
                type = "item";
                break;
        }
        return "vnd.android.cursor." + type + "/vnd." + AUTHORITY + "." + getProviderName();
    }

    protected abstract UriMatcher getMatcher();

    protected static String generateQuestionMarksForInClause(int num) {
        StringBuilder sb = new StringBuilder(num * 2);
        for (int i = 0; i < num; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        return sb.toString();
    }

    protected void validateFields(String[] validFields, ContentValues values)
            throws IllegalArgumentException {
        for (final String key : values.keySet()) {
            boolean isValid = false;
            for (final String validKey : validFields) {
                if (validKey.equals(key)) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) {
                throw new IllegalArgumentException(
                        "Cannot save field '" + key + "' to provider " + getProviderName());
            }
        }
    }

    /**
     * Helper function to be used when you need to know the primary key from the package table
     * when all you have is the package name.
     */
    protected static String getPackageIdFromPackageNameQuery() {
        return "SELECT " + Schema.PackageTable.Cols.ROW_ID + " FROM " + Schema.PackageTable.NAME
                + " WHERE " + Schema.PackageTable.Cols.PACKAGE_NAME + " = ?";
    }
}
