package org.fdroid.fdroid.panic;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.installer.PrivilegedInstaller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import info.guardianproject.panic.Panic;
import info.guardianproject.panic.PanicResponder;

public class PanicPreferencesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PREF_APP = "pref_panic_app";

    private PackageManager pm;
    private ListPreference prefApp;
    private CheckBoxPreference prefExit;
    private CheckBoxPreference prefHide;
    private CheckBoxPreference prefResetRepos;
    private PreferenceCategory categoryAppsToUninstall;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences_panic);

        pm = requireActivity().getPackageManager();
        prefExit = findPreference(Preferences.PREF_PANIC_EXIT);
        prefApp = findPreference(PREF_APP);
        prefHide = findPreference(Preferences.PREF_PANIC_HIDE);
        prefHide.setTitle(getString(R.string.panic_hide_title, getString(R.string.app_name)));
        prefResetRepos = findPreference(Preferences.PREF_PANIC_RESET_REPOS);
        categoryAppsToUninstall = findPreference("pref_panic_apps_to_uninstall");

        if (PanicResponder.checkForDisconnectIntent(requireActivity())) {
            // the necessary action should have been performed by the check already
            requireActivity().finish();
            return;
        }
        String connectIntentSender = PanicResponder.getConnectIntentSender(requireActivity());
        // if there's a connecting app and it is not the old one
        if (!TextUtils.isEmpty(connectIntentSender) && !TextUtils.equals(connectIntentSender, PanicResponder
                .getTriggerPackageName(getActivity()))) {
            // Show dialog allowing the user to opt-in
            showOptInDialog();
        }

        prefApp.setOnPreferenceChangeListener((preference, newValue) -> {
            String packageName = (String) newValue;
            PanicResponder.setTriggerPackageName(requireActivity(), packageName);
            if (packageName.equals(Panic.PACKAGE_NAME_NONE)) {
                prefHide.setChecked(false);
                prefHide.setEnabled(false);
                prefResetRepos.setChecked(false);
                prefResetRepos.setEnabled(false);
                requireActivity().setResult(AppCompatActivity.RESULT_CANCELED);
            } else {
                prefHide.setEnabled(true);
                prefResetRepos.setEnabled(true);
            }
            showPanicApp(packageName);
            return true;
        });
        showPanicApp(PanicResponder.getTriggerPackageName(getActivity()));
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        if (!PrivilegedInstaller.isDefault(getActivity())) {
            getPreferenceScreen().removePreference(categoryAppsToUninstall);
            return;
        }
        showWipeList();
    }

    private void showWipeList() {
        Intent intent = new Intent(getActivity(), SelectInstalledAppsActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        Set<String> wipeSet = Preferences.get().getPanicWipeSet();
        categoryAppsToUninstall.removeAll();
        if (Panic.PACKAGE_NAME_NONE.equals(prefApp.getValue())) {
            categoryAppsToUninstall.setEnabled(false);
            return;
        }
        categoryAppsToUninstall.setEnabled(true);
        if (wipeSet.size() > 0) {
            for (String packageName : wipeSet) {
                Preference preference = new DestructivePreference(getActivity());
                preference.setSingleLineTitle(true);
                preference.setIntent(intent);
                categoryAppsToUninstall.addPreference(preference);
                try {
                    preference.setTitle(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));
                    preference.setIcon(pm.getApplicationIcon(packageName));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    preference.setTitle(packageName);
                }
            }
        } else {
            Preference preference = new Preference(requireActivity());
            preference.setIntent(intent);
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_circle_outline);
            icon.setColorFilter(new LightingColorFilter(0, getResources().getColor(R.color.swap_light_grey_icon)));
            preference.setSingleLineTitle(true);
            preference.setTitle(R.string.panic_add_apps_to_uninstall);
            preference.setIcon(icon);
            categoryAppsToUninstall.addPreference(preference);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.PREF_PANIC_HIDE)
                && sharedPreferences.getBoolean(Preferences.PREF_PANIC_HIDE, false)) {
            showHideConfirmationDialog();
        }
        // disable "hiding" if "exit" gets disabled
        if (key.equals(Preferences.PREF_PANIC_EXIT)
                && !sharedPreferences.getBoolean(Preferences.PREF_PANIC_EXIT, true)) {
            prefHide.setChecked(false);
        }
    }

    private void showPanicApp(String packageName) {
        // Fill list of available panic apps
        List<CharSequence> entries = new ArrayList<>(Collections.singletonList(getString(R.string.panic_app_setting_none)));
        List<CharSequence> entryValues = new ArrayList<>(Collections.singletonList(Panic.PACKAGE_NAME_NONE));

        for (ResolveInfo resolveInfo : PanicResponder.resolveTriggerApps(pm)) {
            if (resolveInfo.activityInfo == null) continue;
            entries.add(resolveInfo.activityInfo.loadLabel(pm));
            entryValues.add(resolveInfo.activityInfo.packageName);
        }

        prefApp.setEntries(entries.toArray(new CharSequence[0]));
        prefApp.setEntryValues(entryValues.toArray(new CharSequence[0]));
        prefApp.setDefaultValue(Panic.PACKAGE_NAME_NONE);

        if (entries.size() <= 1) {
            // bring the user to Ripple if no other panic apps are available
            prefApp.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=info.guardianproject.ripple"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            });
        }

        if (TextUtils.isEmpty(packageName) || packageName.equals(Panic.PACKAGE_NAME_NONE)) {
            // no panic app set
            prefApp.setValue(Panic.PACKAGE_NAME_NONE);
            prefApp.setSummary(getString(R.string.panic_app_setting_summary));

            prefApp.setIcon(null); // otherwise re-setting view resource doesn't work
            Drawable icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_cancel);
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = requireActivity().getTheme();
            theme.resolveAttribute(R.attr.appListItem, typedValue, true);
            @ColorInt int color = typedValue.data;
            icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            prefApp.setIcon(icon);

            // disable destructive panic actions
            prefHide.setEnabled(false);
            showWipeList();
        } else {
            // try to display connected panic app
            try {
                prefApp.setValue(packageName);
                prefApp.setSummary(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));
                prefApp.setIcon(pm.getApplicationIcon(packageName));
                prefHide.setEnabled(true);
                prefResetRepos.setEnabled(true);
                showWipeList();
            } catch (PackageManager.NameNotFoundException e) {
                // revert back to no app, just to be safe
                PanicResponder.setTriggerPackageName(requireActivity(), Panic.PACKAGE_NAME_NONE);
                showPanicApp(Panic.PACKAGE_NAME_NONE);
            }
        }
    }

    private void showOptInDialog() {
        DialogInterface.OnClickListener okListener = (dialogInterface, i) -> {
            PanicResponder.setTriggerPackageName(requireActivity());
            showPanicApp(PanicResponder.getTriggerPackageName(getActivity()));
            requireActivity().setResult(AppCompatActivity.RESULT_OK);
        };
        DialogInterface.OnClickListener cancelListener = (dialogInterface, i) -> {
            requireActivity().setResult(AppCompatActivity.RESULT_CANCELED);
            requireActivity().finish();
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
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
        ComponentName componentName = requireActivity().getCallingActivity();
        String packageName = null;
        if (componentName != null) {
            packageName = componentName.getPackageName();
        }
        return packageName;
    }

    private void showHideConfirmationDialog() {
        String appName = getString(R.string.app_name);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.panic_hide_warning_title);
        builder.setMessage(getString(R.string.panic_hide_warning_message, appName,
                HidingManager.getUnhidePin(requireActivity()), getString(R.string.hiding_calculator)));
        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            // enable "exit" if "hiding" gets enabled
            prefExit.setChecked(true);
            // dismiss, but not cancel dialog
            dialogInterface.dismiss();
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
        builder.setOnCancelListener(dialogInterface -> {
            prefHide.setChecked(false);
            prefResetRepos.setChecked(false);
        });
        builder.setView(R.layout.dialog_app_hiding);
        builder.create().show();
    }

}
