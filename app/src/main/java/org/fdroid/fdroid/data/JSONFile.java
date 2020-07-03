package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.fdroid.fdroid.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static org.fdroid.fdroid.data.CollectionProvider.getJSONUri;


public class JSONFile {

    private static final String TAG = "JSONFile";

    public static String[] JSON_read(Context context, String filePath) {

        String error = "0";
        String errorMessage = "";

        try {
            StringBuilder jsonStringB = new StringBuilder();

            FileReader fileReader = new FileReader(filePath);


            int tmp;
            while ((tmp = fileReader.read()) != -1) {
                jsonStringB.append((char) tmp);
            }
            fileReader.close();

            String jsonString = jsonStringB.toString();

            String name;
            String package_name;
            String last_modified;
            String hidden;
            String version_code;
            String version_name;
            String ignoring_versions;

            try {
                JSONArray Apps = new JSONArray(jsonString);

                for (int i1 = 0; i1 < Apps.length(); i1++) {

                    boolean is_correct = false;
                    ContentValues values_import = new ContentValues();

                    JSONObject JObject = Apps.getJSONObject(i1);

                    package_name = JObject.isNull("package_name") ? null : JObject.getString("package_name");
                    name = JObject.isNull("name") ? null : JObject.getString("name");
                    last_modified = JObject.isNull("last_modified") ? null : JObject.getString("last_modified");
                    hidden = JObject.isNull("hidden") ? "0" : JObject.getString("hidden");
                    version_code = JObject.isNull("version_code") ? null : JObject.getString("version_code");
                    version_name = JObject.isNull("version_name") ? null : JObject.getString("version_name");
                    ignoring_versions = JObject.isNull("ignoring_version") ? null : JObject.getString("ignoring_version");


                    if (name != null) {
                        values_import.put(Schema.CollectionTable.Cols.NAME, name);
                    }
                    if (package_name != null) {
                        values_import.put(Schema.CollectionTable.Cols.PACKAGE_NAME, package_name);
                        int count = 0;
                        for (int i = 0; i < package_name.length(); i++) {
                            if (package_name.charAt(i) == '.') {
                                count++;
                            }

                            if (count >= 2) {
                                is_correct = true;
                                break;
                            }
                        }
                    }
                    if (last_modified != null) {
                        values_import.put(Schema.CollectionTable.Cols.LAST_MODIFIED, last_modified);
                    }
                    if (hidden != null) {
                        values_import.put(Schema.CollectionTable.Cols.HIDDEN, hidden);
                    }
                    if (version_code != null) {
                        values_import.put(Schema.CollectionTable.Cols.VERSION_CODE, version_code);
                    }
                    if (version_name != null) {
                        values_import.put(Schema.CollectionTable.Cols.VERSION_NAME, version_name);
                    }
                    if (ignoring_versions != null) {
                        values_import.put(Schema.CollectionTable.Cols.IGNORING_VERSION_CODE, ignoring_versions);
                    }


                    if (is_correct) {
                        int ret = context.getContentResolver().update(getJSONUri(), values_import, Schema.CollectionTable.Cols.PACKAGE_NAME + "=?", new String[]{package_name});
                        if (ret == 0) {
                            context.getContentResolver().insert(getJSONUri(), values_import);
                        }
                    } else {
                        error = "-1";
                        errorMessage = "package_name failure";
                    }
                }
            } catch (JSONException e) {
                error = "-1";
                errorMessage = e.getMessage();
                Log.e(TAG, "Error while using JSON:\n" + errorMessage);
            }

        } catch (IOException e) {
            error = "-2";
            errorMessage = e.getMessage();
            Log.e(TAG, "Error while reading file:\n" + errorMessage);
        }

        return new String[]{error, errorMessage};
    }


    public static String[] JSON_write(Context context, String filePath) {

        String error = "0";
        String errorMessage = "";

        String jsonString = "";

        try {

            Cursor cursor = context.getContentResolver().query(getJSONUri(), null, null, null, null);
            assert cursor != null;
            cursor.moveToPosition(-1);


            JSONArray Apps = new JSONArray();
            while (cursor.moveToNext()) {
                App app = new App(cursor);

                String tmpHidden = (app.collectionHidden.equals("0")) ? null : app.collectionHidden;
                String tmpIgVersion = (app.collectionIgnoringVersionCode > 0) ? String.valueOf(app.collectionIgnoringVersionCode) : null;

                JSONObject JObject = new JSONObject();
                JObject.put("name", app.name);
                JObject.put("package_name", app.packageName);
                JObject.put("last_modified", Utils.formatTime(app.collectionLastModified, null));
                JObject.put("hidden", tmpHidden);
                JObject.put("version_code", app.suggestedVersionCode);
                JObject.put("version_name", app.suggestedVersionName);
                JObject.put("ignoring_version", tmpIgVersion);

                Apps.put(JObject);
            }
            cursor.close();

            jsonString = Apps.toString(2);
        } catch (JSONException e) {
            error = "-1";
            errorMessage = e.getMessage();
            Log.e(TAG, "Error while using JSON:\n" + errorMessage);
        }


        Log.i(TAG, jsonString);

        try {
            FileWriter myWriter = new FileWriter(filePath);
            myWriter.write(jsonString);
            myWriter.flush();
            myWriter.close();
            Log.i(TAG, "Successfully wrote to the file.");
        } catch (IOException e) {
            error = "-2";
            errorMessage = e.getMessage();
            Log.e(TAG, "Error while writing file:\n" + errorMessage);
        }

        return new String[]{error, errorMessage};
    }
}
