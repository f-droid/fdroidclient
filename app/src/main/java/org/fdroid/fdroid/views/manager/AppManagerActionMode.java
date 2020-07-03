package org.fdroid.fdroid.views.manager;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.views.filebrowser.fileBrowserDialog;

import java.util.ArrayList;

import static org.fdroid.fdroid.data.CollectionProvider.getJSONUri;

public class AppManagerActionMode {

    private final int id;
    private final Context context;
    private final Activity activity;
    private ActionMode actionMode;

    private final int REMOVE = 1;
    private final int HIDE = 2;
    private final int SHOW = 3;


    public AppManagerActionMode(Activity activity, Context context, int id) {
        this.id = id;
        this.context = context;
        this.activity = activity;
        actionMode = activity.startActionMode(mActionModeCallback);
    }

    public void onCreateAction() {

    }

    public void onDestroyAction() {

    }


    public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        private static final String TAG = "AppManagerActionMode";

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_actionmode, menu);

            onCreateAction(); // callback

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            if (id == 0) {
                menu.findItem(R.id.action_delete).setVisible(false); // hide trash-icon in installed
            }
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_delete:

                    fileBrowserDialog dialog = new fileBrowserDialog(context) {

                        @Override
                        public void result(int requestCode, int buttonValue, String text, Object passOn) {
                            if (buttonValue == AlertDialog.BUTTON_POSITIVE) {
                                actionModeChoice(REMOVE);
                            }
                        }
                    };
                    dialog.open(
                            1,
                            new int[]{AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE},
                            new String[]{activity.getString(R.string.filebrowser_dialog_no), activity.getString(R.string.filebrowser_dialog_yes)},
                            activity.getString(R.string.installed_dialog_delete_app),
                            activity.getString(R.string.installed_dialog_delete),
                            null,
                            ""
                    );

                    break;
                case R.id.action_hide:
                    actionModeChoice(HIDE);
                    break;
                case R.id.action_show:
                    actionModeChoice(SHOW);
                    break;
                case R.id.install:
                    break;
                default:
                    Log.e(TAG, "onOptionsItemSelected " + "Unknown column name " + menuItem.getItemId());
            }
            return false;
        }


        @Override
        public void onDestroyActionMode(ActionMode actionMode2) {
            actionMode = null;

            onDestroyAction(); // callback
        }


    };


    public void closeActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }


    void actionModeChoice(int event) {
        ArrayList<String> wipeList = new ArrayList<>(Preferences.get().getPanicTmpSelectedSet());

        StringBuilder where = new StringBuilder();
        String[] whereAgs = new String[wipeList.size()];
        for (int i = 0; i < wipeList.size(); i++) {
            where.append(Schema.CollectionTable.Cols.PACKAGE_NAME).append("=? OR ");
            whereAgs[i] = wipeList.get(i);
        }
        where.append("0"); // Closing


        ContentValues values_import = new ContentValues();

        switch (event) {
            case REMOVE:
                context.getContentResolver().delete(
                        getJSONUri(),
                        String.valueOf(where),
                        whereAgs
                );
                break;
            case HIDE:
                values_import.put(Schema.CollectionTable.Cols.HIDDEN, 1);
                context.getContentResolver().update(
                        getJSONUri(),
                        values_import,
                        String.valueOf(where),
                        whereAgs
                );
                break;
            case SHOW:
                values_import.put(Schema.CollectionTable.Cols.HIDDEN, 0);
                context.getContentResolver().update(
                        getJSONUri(),
                        values_import,
                        String.valueOf(where),
                        whereAgs
                );
                break;
            default:
                Log.e("TAG", "Unknown order " + event);
        }

        actionMode.finish();
    }

}
