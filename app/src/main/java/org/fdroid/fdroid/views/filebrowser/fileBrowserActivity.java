package org.fdroid.fdroid.views.filebrowser;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.JSONFile;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;


public class fileBrowserActivity extends AppCompatActivity {

    private static final String TAG = "fileBrowserActivity";

    public static final int MODE_SAVE = 1;
    public static final int MODE_LOAD = 2;
    private static final int TYP_SAVE_FILE = 1;
    private static final int TYP_OVERWRITE_YES_NO = TYP_SAVE_FILE + 1;
    private static final int TYP_LOAD_YES_NO = TYP_OVERWRITE_YES_NO + 1;
    private static final int TYP_NEW_FOLDER = TYP_LOAD_YES_NO + 1;

    private int mode;
    private File currentDir;
    private fileBrowserAdapter adapter;
    private TextView URI;
    private final String STORE = Environment.getExternalStorageDirectory().getPath();
    private final int rootLength = STORE.length();

    public static String defaultPath = Preferences.get().BackupPath();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        this.mode = getIntent().getIntExtra("mode", 0);

        if (!permissionCheck()) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("modeCache", mode);
            setResult(Activity.RESULT_OK, returnIntent); //if u go back then say: "no permission!"
            finish(); // if no permission to read/write directory's -> close this activity
            return;
        }

        setContentView(R.layout.activity_filebrowser_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (mode == MODE_SAVE) {
            toolbar.setTitle(R.string.menu_export);
        } else if (mode == MODE_LOAD) {
            toolbar.setTitle(R.string.menu_import);
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentDir = new File(defaultPath);
        if (!currentDir.exists()) {
            currentDir = new File(STORE); //fallback
        }


        URI = findViewById(R.id.URITextView);

        //find RecyclerView_Layout;
        RecyclerView rv = findViewById(R.id.rv);
        rv.setHasFixedSize(true);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);


        adapter = new fileBrowserAdapter() {
            @Override
            void result(File file) {
                dialog_result(file);
            }
        };
        rv.setAdapter(adapter);

        change_dir(currentDir);
    }


    private void openDialog(int typ, File file) {
        if (file == null) {
            file = new File(STORE);
        }

        fileBrowserDialog dialog = new fileBrowserDialog(this) {

            public void result(int requestCode, int buttonValue, String text, Object passOn) {
                File r_file = (File) passOn;
                if (r_file == null) {
                    return;
                }

                switch (requestCode) {
                    case TYP_SAVE_FILE:
                        if (buttonValue == AlertDialog.BUTTON_POSITIVE) {

                            File file = new File(r_file.getParentFile().getPath() + "/" + text);
                            if (file.exists()) {
                                open(
                                        TYP_OVERWRITE_YES_NO,
                                        new int[]{AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE},
                                        new String[]{getString(R.string.filebrowser_dialog_cancel), getString(R.string.filebrowser_dialog_overwrite)},
                                        getString(R.string.filebrowser_dialog_save_file),
                                        getString(R.string.filebrowser_dialog_overwrite_exist_file, file.getName()),
                                        null,
                                        file
                                );
                            } else save_file(file.getPath());
                        }
                        break;
                    case TYP_OVERWRITE_YES_NO:
                        if (buttonValue == AlertDialog.BUTTON_POSITIVE) {
                            save_file(r_file.getPath());
                        }
                        break;
                    case TYP_LOAD_YES_NO:
                        if (buttonValue == AlertDialog.BUTTON_POSITIVE) {
                            load_file(r_file.getPath());
                        }
                        break;
                    case TYP_NEW_FOLDER:
                        if (buttonValue == AlertDialog.BUTTON_POSITIVE) {
                            File file = new File(r_file.getPath() + "/" + text);
                            if (file.exists()) {
                                open(
                                        TYP_NEW_FOLDER,
                                        new int[]{AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE},
                                        new String[]{getString(R.string.filebrowser_dialog_cancel), getString(R.string.filebrowser_dialog_creat)},
                                        getString(R.string.filebrowser_dialog_new_folder),
                                        getString(R.string.filebrowser_dialog_already_exist),
                                        text,
                                        r_file
                                );
                            } else {
                                if (file.mkdir()) {
                                    change_dir(r_file);
                                } else {
                                    Log.e("NewDir", "Fehler");
                                }
                            }
                        }
                        break;
                }


            }
        };


        switch (typ) {
            case TYP_SAVE_FILE:
                if (!file.isFile()) {
                    file = new File(file.getPath() + "/fdroid_apps_" + date(String.valueOf(System.currentTimeMillis())) + ".json");
                }

                dialog.open(
                        TYP_SAVE_FILE,
                        new int[]{AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE},
                        new String[]{getString(R.string.filebrowser_dialog_cancel), getString(R.string.filebrowser_dialog_save)},
                        getString(R.string.filebrowser_dialog_save_file),
                        null,
                        file.getName(),
                        file
                );
                break;
            case TYP_LOAD_YES_NO:
                dialog.open(
                        TYP_LOAD_YES_NO,
                        new int[]{AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE},
                        new String[]{getString(R.string.filebrowser_dialog_no), getString(R.string.filebrowser_dialog_yes)},
                        getString(R.string.filebrowser_dialog_load_file),
                        getString(R.string.filebrowser_dialog_would_load, file.getName()),
                        null,
                        file
                );
                break;
            case TYP_NEW_FOLDER:
                dialog.open(
                        TYP_NEW_FOLDER,
                        new int[]{android.app.AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE},
                        new String[]{getString(R.string.filebrowser_dialog_cancel), getString(R.string.filebrowser_dialog_creat)},
                        getString(R.string.filebrowser_dialog_new_folder),
                        null,
                        "",
                        file
                );
                break;
        }
    }


    private String date(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        long timestamp = Long.parseLong(string);
        Date date = new Date(timestamp);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH);

        return df.format(date);
    }

    void toast_colour(String message, String textColour) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        View view = toast.getView();
        TextView text = (TextView) view.findViewById(android.R.id.message);
        text.setTextColor(Color.parseColor(textColour));
        toast.show();
    }


    void save_file(String filePath) {
        String[] resume = JSONFile.JSON_write(this, filePath);

        switch (Integer.parseInt(resume[0])) {
            case 0:
                toast_colour(getString(R.string.installed_toast_export), "black");
                break;
            case -1:
                toast_colour("JSON: " + resume[1], "red");
                break;
            case -2:
                toast_colour("File: " + resume[1], "red");
                break;
        }

        finish();
    }

    void load_file(String filePath) {
        String[] resume = JSONFile.JSON_read(this, filePath);

        switch (Integer.parseInt(resume[0])) {
            case 0:
                toast_colour(getString(R.string.installed_toast_import), "black");
                Intent returnIntent = new Intent();
                returnIntent.putExtra("loadSuccessfully", 1);
                setResult(Activity.RESULT_CANCELED, returnIntent);
                break;
            case -1:
                toast_colour("JSON: " + resume[1], "red");
                break;
            case -2:
                toast_colour("File: " + resume[1], "red");
                break;
        }
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_filebrowser, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mode == MODE_LOAD) {
            menu.findItem(R.id.action_save).setVisible(false);
            menu.findItem(R.id.action_newdir).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_save:
                openDialog(TYP_SAVE_FILE, currentDir);
                break;
            case R.id.action_newdir:
                openDialog(TYP_NEW_FOLDER, currentDir);
                break;
            case R.id.action_cancel:
                finish();
                break;
            case R.id.menu_change_backupdir:
                saveDefaultPath();
                break;
            default:
                Log.e(TAG, "onOptionsItemSelected" + " Unknown column name " + item.getTitle());
        }

        return super.onOptionsItemSelected(item);
    }


    void saveDefaultPath() {
        defaultPath = currentDir.getPath();
        Preferences.get().setBackupPath(defaultPath);
        adapter.refresh();
    }

    void change_dir(File file) {
        URI_Path(file.getPath());

        currentDir = file;

        File[] files = file.listFiles();
        Arrays.sort(files,
                new Comparator<File>() {
                    @Override
                    public int compare(File f2, File f1) {
                        if (f1.isFile() == f2.isFile()) {
                            return f2.getName().toLowerCase(Locale.getDefault()).compareTo(f1.getName().toLowerCase(Locale.getDefault()));
                        } else if (f1.isFile()) {
                            return 1; //put files on top
                        } else {
                            return -1; //put dirs on bottom
                        }
                    }
                }
        );

        adapter.setFiles(files);
    }

    void URI_Path(String Path) {
        Path = Path.substring(rootLength);

        if (Path.isEmpty()) {
            URI.setText("/");
        } else {
            URI.setText(Path);
        }
    }

    public void dialog_result(File file) {
        if (file.isDirectory()) {
            change_dir(file);
        } else if (file.isFile()) {
            if (mode == MODE_SAVE) {
                openDialog(TYP_SAVE_FILE, file);
            }
            if (mode == MODE_LOAD) {
                openDialog(TYP_LOAD_YES_NO, file);
            }
        }

    }


    @Override
    protected void onRestart() {
        super.onRestart();
        change_dir(currentDir);
    }

    @Override
    public void onBackPressed() {
        File file = currentDir.getParentFile();

        if (file.getPath().length() >= rootLength) {
            change_dir(file);
        } else {
            finish(); //if we are in root-directory than close this activity
        }
    }

    boolean permissionCheck() {

        String[] Permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, Permissions, 1);
            return false;
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}