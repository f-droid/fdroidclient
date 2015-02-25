package org.fdroid.fdroid.views.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.TextUtils;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.PreferencesActivity;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.installer.CheckRootAsyncTask;
import org.fdroid.fdroid.installer.Installer;

public class PreferenceFragment
        extends android.support.v4.preference.PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static String[] summariesToUpdate = {
        Preferences.PREF_UPD_INTERVAL,
        Preferences.PREF_UPD_WIFI_ONLY,
        Preferences.PREF_UPD_NOTIFY,
        Preferences.PREF_UPD_HISTORY,
        Preferences.PREF_ROOTED,
        Preferences.PREF_INCOMP_VER,
        Preferences.PREF_THEME,
        Preferences.PREF_PERMISSIONS,
        Preferences.PREF_COMPACT_LAYOUT,
        Preferences.PREF_IGN_TOUCH,
        Preferences.PREF_LOCAL_REPO_BONJOUR,
        Preferences.PREF_LOCAL_REPO_NAME,
        Preferences.PREF_LOCAL_REPO_HTTPS,
        Preferences.PREF_CACHE_APK,
        Preferences.PREF_EXPERT,
        Preferences.PREF_ROOT_INSTALLER,
        Preferences.PREF_SYSTEM_INSTALLER,
        Preferences.PREF_ENABLE_PROXY,
        Preferences.PREF_PROXY_HOST,
        Preferences.PREF_PROXY_PORT,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    protected void checkSummary(String key, int resId) {
        CheckBoxPreference pref = (CheckBoxPreference)findPreference(key);
        pref.setSummary(resId);
    }

    protected void entrySummary(String key) {
        ListPreference pref = (ListPreference)findPreference(key);
        pref.setSummary(pref.getEntry());
    }

    protected void textSummary(String key, int resId) {
        EditTextPreference pref = (EditTextPreference)findPreference(key);
        pref.setSummary(getString(resId, pref.getText()));
    }

    protected void updateSummary(String key, boolean changing) {

        int result = 0;

        if (key.equals(Preferences.PREF_UPD_INTERVAL)) {
            ListPreference pref = (ListPreference)findPreference(
                    Preferences.PREF_UPD_INTERVAL);
            int interval = Integer.parseInt(pref.getValue());
            Preference onlyOnWifi = findPreference(
                    Preferences.PREF_UPD_WIFI_ONLY);
            onlyOnWifi.setEnabled(interval > 0);
            if (interval == 0) {
                pref.setSummary(R.string.update_interval_zero);
            } else {
                pref.setSummary(pref.getEntry());
            }

        } else if (key.equals(Preferences.PREF_UPD_WIFI_ONLY)) {
            checkSummary(key, R.string.automatic_scan_wifi_on);

        } else if (key.equals(Preferences.PREF_UPD_NOTIFY)) {
            checkSummary(key, R.string.notify_on);

        } else if (key.equals(Preferences.PREF_UPD_HISTORY)) {
            textSummary(key, R.string.update_history_summ);

        } else if (key.equals(Preferences.PREF_PERMISSIONS)) {
            checkSummary(key, R.string.showPermissions_on);

        } else if (key.equals(Preferences.PREF_COMPACT_LAYOUT)) {
            checkSummary(key, R.string.compactlayout_on);

        } else if (key.equals(Preferences.PREF_THEME)) {
            entrySummary(key);
            if (changing) {
                result |= PreferencesActivity.RESULT_RESTART;
                getActivity().setResult(result);
            }

        } else if (key.equals(Preferences.PREF_INCOMP_VER)) {
            checkSummary(key, R.string.show_incompat_versions_on);

        } else if (key.equals(Preferences.PREF_ROOTED)) {
            checkSummary(key, R.string.rooted_on);

        } else if (key.equals(Preferences.PREF_IGN_TOUCH)) {
            checkSummary(key, R.string.ignoreTouch_on);

        } else if (key.equals(Preferences.PREF_LOCAL_REPO_BONJOUR)) {
            checkSummary(key, R.string.local_repo_bonjour_on);

        } else if (key.equals(Preferences.PREF_LOCAL_REPO_NAME)) {
            textSummary(key, R.string.local_repo_name_summary);

        } else if (key.equals(Preferences.PREF_LOCAL_REPO_HTTPS)) {
            checkSummary(key, R.string.local_repo_https_on);

        } else if (key.equals(Preferences.PREF_CACHE_APK)) {
            checkSummary(key, R.string.cache_downloaded_on);

        } else if (key.equals(Preferences.PREF_EXPERT)) {
            checkSummary(key, R.string.expert_on);

        } else if (key.equals(Preferences.PREF_ROOT_INSTALLER)) {
            checkSummary(key, R.string.root_installer_on);

        } else if (key.equals(Preferences.PREF_SYSTEM_INSTALLER)) {
            checkSummary(key, R.string.system_installer_on);

        } else if (key.equals(Preferences.PREF_ENABLE_PROXY)) {
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setSummary(R.string.enable_proxy_summary);

        } else if (key.equals(Preferences.PREF_PROXY_HOST)) {
            EditTextPreference textPref = (EditTextPreference) findPreference(key);
            String text = Preferences.get().getProxyHost();
            if (TextUtils.isEmpty(text) || text.equals(Preferences.DEFAULT_PROXY_HOST))
                textPref.setSummary(R.string.proxy_host_summary);
            else
                textPref.setSummary(text);

        } else if (key.equals(Preferences.PREF_PROXY_PORT)) {
            EditTextPreference textPref = (EditTextPreference) findPreference(key);
            int port = Preferences.get().getProxyPort();
            if (port == Preferences.DEFAULT_PROXY_PORT)
                textPref.setSummary(R.string.proxy_port_summary);
            else
                textPref.setSummary(String.valueOf(port));

        }
    }

    /**
     * Initializes RootInstaller preference. This method ensures that the preference can only be checked and persisted
     * when the user grants root access for F-Droid.
     */
    protected void initRootInstallerPreference() {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(Preferences.PREF_ROOT_INSTALLER);

        // we are handling persistence ourself!
        pref.setPersistent(false);

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                final CheckBoxPreference pref = (CheckBoxPreference) preference;

                if (pref.isChecked()) {
                    CheckRootAsyncTask checkTask = new CheckRootAsyncTask(getActivity(), new CheckRootAsyncTask.CheckRootCallback() {

                        @Override
                        public void onRootCheck(boolean rootGranted) {
                            if (rootGranted) {
                                // root access granted
                                SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                                editor.putBoolean(Preferences.PREF_ROOT_INSTALLER, true);
                                editor.commit();
                                pref.setChecked(true);
                            } else {
                                // root access denied
                                SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                                editor.putBoolean(Preferences.PREF_ROOT_INSTALLER, false);
                                editor.commit();
                                pref.setChecked(false);

                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                                alertBuilder.setTitle(R.string.root_access_denied_title);
                                alertBuilder.setMessage(getActivity().getString(R.string.root_access_denied_body));
                                alertBuilder.setNeutralButton(android.R.string.ok, null);
                                alertBuilder.create().show();
                            }
                        }
                    });
                    checkTask.execute();
                } else {
                    SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                    editor.putBoolean(Preferences.PREF_ROOT_INSTALLER, false);
                    editor.commit();
                    pref.setChecked(false);
                }

                return true;
            }
        });
    }

    /**
     * Initializes SystemInstaller preference, which can only be enabled when F-Droid is installed as a system-app
     */
    protected void initSystemInstallerPreference() {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(Preferences.PREF_SYSTEM_INSTALLER);

        // we are handling persistence ourself!
        pref.setPersistent(false);

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                final CheckBoxPreference pref = (CheckBoxPreference) preference;

                if (pref.isChecked()) {
                    if (Installer.hasSystemPermissions(getActivity(), getActivity().getPackageManager())) {
                        // system-permission are granted, i.e. F-Droid is a system-app
                        SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                        editor.putBoolean(Preferences.PREF_SYSTEM_INSTALLER, true);
                        editor.commit();
                        pref.setChecked(true);
                    } else {
                        // system-permission not available
                        SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                        editor.putBoolean(Preferences.PREF_SYSTEM_INSTALLER, false);
                        editor.commit();
                        pref.setChecked(false);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                        alertBuilder.setTitle(R.string.system_permission_denied_title);
                        alertBuilder.setMessage(getActivity().getString(R.string.system_permission_denied_body));
                        alertBuilder.setNeutralButton(android.R.string.ok, null);
                        alertBuilder.create().show();
                    }
                } else {
                    SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                    editor.putBoolean(Preferences.PREF_SYSTEM_INSTALLER, false);
                    editor.commit();
                    pref.setChecked(false);
                }

                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        for (String key : summariesToUpdate) {
            updateSummary(key, false);
        }

        initRootInstallerPreference();
        initSystemInstallerPreference();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        updateSummary(key, true);
    }


}
