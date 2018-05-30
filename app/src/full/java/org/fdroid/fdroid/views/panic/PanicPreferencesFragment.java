package org.fdroid.fdroid.views.panic;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.TypedValue;
import info.guardianproject.panic.Panic;
import info.guardianproject.panic.PanicResponder;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.hiding.HidingManager;

import java.util.ArrayList;

public class PanicPreferencesFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PREF_EXIT = Preferences.PREF_PANIC_EXIT;
    private static final String PREF_APP = "pref_panic_app";
    private static final String PREF_HIDE = Preferences.PREF_PANIC_HIDE;

    private PackageManager pm;
    private ListPreference prefApp;
    private CheckBoxPreference prefExit;
    private CheckBoxPreference prefHide;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences_panic);

        pm = getActivity().getPackageManager();
        prefExit = (CheckBoxPreference) findPreference(PREF_EXIT);
        prefApp = (ListPreference) findPreference(PREF_APP);
        prefHide = (CheckBoxPreference) findPreference(PREF_HIDE);
        prefHide.setTitle(getString(R.string.panic_hide_title, getString(R.string.app_name)));

        if (PanicResponder.checkForDisconnectIntent(getActivity())) {
            // the necessary action should have been performed by the check already
            getActivity().finish();
            return;
        }
        String connectIntentSender = PanicResponder.getConnectIntentSender(getActivity());
        // if there's a connecting app and it is not the old one
        if (!TextUtils.isEmpty(connectIntentSender) && !TextUtils.equals(connectIntentSender, PanicResponder
                .getTriggerPackageName(getActivity()))) {
            // Show dialog allowing the user to opt-in
            showOptInDialog();
        }

        prefApp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String packageName = (String) newValue;
                PanicResponder.setTriggerPackageName(getActivity(), packageName);
                if (packageName.equals(Panic.PACKAGE_NAME_NONE)) {
                    prefHide.setChecked(false);
                    prefHide.setEnabled(false);
                    getActivity().setResult(Activity.RESULT_CANCELED);
                } else {
                    prefHide.setEnabled(true);
                }
                showPanicApp(packageName);
                return true;
            }
        });
        showPanicApp(PanicResponder.getTriggerPackageName(getActivity()));
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREF_HIDE) && sharedPreferences.getBoolean(PREF_HIDE, false)) {
            showHideConfirmationDialog();
        }
        // disable "hiding" if "exit" gets disabled
        if (key.equals(PREF_EXIT) && !sharedPreferences.getBoolean(PREF_EXIT, true)) {
            prefHide.setChecked(false);
        }
    }

    private void showPanicApp(String packageName) {
        // Fill list of available panic apps
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> entryValues = new ArrayList<>();
        entries.add(0, getString(R.string.panic_app_setting_none));
        entryValues.add(0, Panic.PACKAGE_NAME_NONE);

        for (ResolveInfo resolveInfo : PanicResponder.resolveTriggerApps(pm)) {
            if (resolveInfo.activityInfo == null) continue;
            entries.add(resolveInfo.activityInfo.loadLabel(pm));
            entryValues.add(resolveInfo.activityInfo.packageName);
        }

        prefApp.setEntries(entries.toArray(new CharSequence[entries.size()]));
        prefApp.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
        prefApp.setDefaultValue(Panic.PACKAGE_NAME_NONE);

        if (entries.size() <= 1) {
            // bring the user to Ripple if no other panic apps are available
            prefApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=info.guardianproject.ripple"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                    return true;
                }
            });
        }

        if (TextUtils.isEmpty(packageName) || packageName.equals(Panic.PACKAGE_NAME_NONE)) {
            // no panic app set
            prefApp.setValue(Panic.PACKAGE_NAME_NONE);
            prefApp.setSummary(getString(R.string.panic_app_setting_summary));

            prefApp.setIcon(null); // otherwise re-setting view resource doesn't work
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_cancel);
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getActivity().getTheme();
            theme.resolveAttribute(R.attr.appListItem, typedValue, true);
            @ColorInt int color = typedValue.data;
            icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            prefApp.setIcon(icon);

            // disable destructive panic actions
            prefHide.setEnabled(false);
        } else {
            // try to display connected panic app
            try {
                prefApp.setValue(packageName);
                prefApp.setSummary(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));
                prefApp.setIcon(pm.getApplicationIcon(packageName));
                prefHide.setEnabled(true);
            } catch (PackageManager.NameNotFoundException e) {
                // revert back to no app, just to be safe
                PanicResponder.setTriggerPackageName(getActivity(), Panic.PACKAGE_NAME_NONE);
                showPanicApp(Panic.PACKAGE_NAME_NONE);
            }
        }
    }

    private void showOptInDialog() {
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                PanicResponder.setTriggerPackageName(getActivity());
                showPanicApp(PanicResponder.getTriggerPackageName(getActivity()));
                getActivity().setResult(Activity.RESULT_OK);
            }
        };
        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.panic_app_dialog_title));

        CharSequence app = getString(R.string.panic_app_unknown_app);
        String packageName = getCallingPackageName();
        if (packageName != null) {
            try {
                app = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        String text = String.format(getString(R.string.panic_app_dialog_message), app);
        builder.setMessage(text);
        builder.setNegativeButton(R.string.allow, okListener);
        builder.setPositiveButton(R.string.cancel, cancelListener);
        builder.show();
    }

    @Nullable
    private String getCallingPackageName() {
        ComponentName componentName = getActivity().getCallingActivity();
        String packageName = null;
        if (componentName != null) {
            packageName = componentName.getPackageName();
        }
        return packageName;
    }

    private void showHideConfirmationDialog() {
        String appName = getString(R.string.app_name);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.panic_hide_warning_title);
        builder.setMessage(getString(R.string.panic_hide_warning_message, appName,
                HidingManager.getUnhidePin(getActivity()), getString(R.string.hiding_calculator)));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // enable "exit" if "hiding" gets enabled
                prefExit.setChecked(true);
                // dismiss, but not cancel dialog
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                prefHide.setChecked(false);
            }
        });
        builder.setView(R.layout.dialog_app_hiding);
        builder.create().show();
    }

}
