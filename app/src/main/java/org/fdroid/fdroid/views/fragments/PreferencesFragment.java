/*
 * Copyright (C) 2014-2018 Hans-Christoph Steiner
 * Copyright (C) 2014-2017 Peter Serwylo
 * Copyright (C) 2015-2016 Daniel Martí
 * Copyright (C) 2015 Dominik Schürmann
 * Copyright (C) 2018 Torsten Grote
 * Copyright (C) 2018 dkanada
 * Copyright (C) 2018 Senecto Limited
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.views.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.SeekBarPreference;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.WindowManager;
import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import org.fdroid.fdroid.AppDetails2;
import org.fdroid.fdroid.CleanCacheService;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Languages;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.installer.PrivilegedInstaller;
import org.fdroid.fdroid.views.LiveSeekBarPreference;

public class PreferencesFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "PreferencesFragment";

    private static final String[] SUMMARIES_TO_UPDATE = {
            Preferences.PREF_OVER_WIFI,
            Preferences.PREF_OVER_DATA,
            Preferences.PREF_UPDATE_INTERVAL,
            Preferences.PREF_UPDATE_NOTIFICATION_ENABLED,
            Preferences.PREF_SHOW_ROOT_APPS,
            Preferences.PREF_SHOW_ANTI_FEATURE_APPS,
            Preferences.PREF_SHOW_INCOMPAT_VERSIONS,
            Preferences.PREF_THEME,
            Preferences.PREF_FORCE_TOUCH_APPS,
            Preferences.PREF_LOCAL_REPO_NAME,
            Preferences.PREF_LANGUAGE,
            Preferences.PREF_KEEP_CACHE_TIME,
            Preferences.PREF_EXPERT,
            Preferences.PREF_PRIVILEGED_INSTALLER,
            Preferences.PREF_ENABLE_PROXY,
            Preferences.PREF_PROXY_HOST,
            Preferences.PREF_PROXY_PORT,
    };

    private static final int[] UPDATE_INTERVAL_NAMES = {
            R.string.interval_never,
            R.string.interval_2w,
            R.string.interval_1w,
            R.string.interval_1d,
            R.string.interval_12h,
            R.string.interval_4h,
            R.string.interval_1h,
    };

    private static final int REQUEST_INSTALL_ORBOT = 0x1234;

    private PreferenceGroup otherPrefGroup;
    private LiveSeekBarPreference overWifiSeekBar;
    private LiveSeekBarPreference overDataSeekBar;
    private LiveSeekBarPreference updateIntervalSeekBar;
    private SwitchPreference enableProxyCheckPref;
    private SwitchPreference useTorCheckPref;
    private Preference updateAutoDownloadPref;
    private Preference updatePrivilegedExtensionPref;
    private CheckBoxPreference keepInstallHistoryPref;
    private Preference installHistoryPref;
    private long currentKeepCacheTime;
    private int overWifiPrevious;
    private int overDataPrevious;
    private int updateIntervalPrevious;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        Preferences.get().migrateOldPreferences();

        addPreferencesFromResource(R.xml.preferences);
        otherPrefGroup = (PreferenceGroup) findPreference("pref_category_other");

        keepInstallHistoryPref = (CheckBoxPreference) findPreference(Preferences.PREF_KEEP_INSTALL_HISTORY);
        installHistoryPref = findPreference("installHistory");
        installHistoryPref.setVisible(keepInstallHistoryPref.isChecked());

        useTorCheckPref = (SwitchPreference) findPreference(Preferences.PREF_USE_TOR);
        enableProxyCheckPref = (SwitchPreference) findPreference(Preferences.PREF_ENABLE_PROXY);
        updateAutoDownloadPref = findPreference(Preferences.PREF_AUTO_DOWNLOAD_INSTALL_UPDATES);
        updatePrivilegedExtensionPref = findPreference(Preferences.PREF_UNINSTALL_PRIVILEGED_APP);

        overWifiSeekBar = (LiveSeekBarPreference) findPreference(Preferences.PREF_OVER_WIFI);
        overWifiPrevious = overWifiSeekBar.getValue();
        overWifiSeekBar.setSeekBarLiveUpdater(new LiveSeekBarPreference.SeekBarLiveUpdater() {
            @Override
            public String seekBarUpdated(int position) {
                return getNetworkSeekBarSummary(position);
            }
        });
        overDataSeekBar = (LiveSeekBarPreference) findPreference(Preferences.PREF_OVER_DATA);
        overDataPrevious = overDataSeekBar.getValue();
        overDataSeekBar.setSeekBarLiveUpdater(new LiveSeekBarPreference.SeekBarLiveUpdater() {
            @Override
            public String seekBarUpdated(int position) {
                return getNetworkSeekBarSummary(position);
            }
        });
        updateIntervalSeekBar = (LiveSeekBarPreference) findPreference(Preferences.PREF_UPDATE_INTERVAL);
        updateIntervalPrevious = updateIntervalSeekBar.getValue();
        updateIntervalSeekBar.setSeekBarLiveUpdater(new LiveSeekBarPreference.SeekBarLiveUpdater() {
            @Override
            public String seekBarUpdated(int position) {
                return getString(UPDATE_INTERVAL_NAMES[position]);
            }
        });

        ListPreference languagePref = (ListPreference) findPreference(Preferences.PREF_LANGUAGE);
        if (Build.VERSION.SDK_INT >= 24) {
            PreferenceCategory category = (PreferenceCategory) findPreference("pref_category_display");
            category.removePreference(languagePref);
        } else {
            Languages languages = Languages.get(getActivity());
            languagePref.setDefaultValue(Languages.USE_SYSTEM_DEFAULT);
            languagePref.setEntries(languages.getAllNames());
            languagePref.setEntryValues(languages.getSupportedLocales());
        }
    }

    private void checkSummary(String key, int resId) {
        Preference pref = findPreference(key);
        pref.setSummary(resId);
    }

    private void entrySummary(String key) {
        ListPreference pref = (ListPreference) findPreference(key);
        if (pref != null) {
            pref.setSummary(pref.getEntry());
        }
    }

    private void textSummary(String key, int resId) {
        EditTextPreference pref = (EditTextPreference) findPreference(key);
        if (pref == null) {
            Utils.debugLog(TAG, "null preference found for " + key);
        } else {
            pref.setSummary(getString(resId, pref.getText()));
        }
    }

    private String getNetworkSeekBarSummary(int position) {
        if (position == 0) {
            return getString(R.string.over_network_never_summary);
        } else if (position == 1) {
            return getString(R.string.over_network_on_demand_summary);
        } else if (position == 2) {
            return getString(R.string.over_network_always_summary);
        } else {
            throw new IllegalArgumentException("Unknown seekbar position");
        }
    }

    private void setNetworkSeekBarSummary(SeekBarPreference seekBarPreference) {
        int position = seekBarPreference.getValue();
        seekBarPreference.setSummary(getNetworkSeekBarSummary(position));
    }

    private void enableUpdateInverval() {
        if (overWifiSeekBar.getValue() == Preferences.OVER_NETWORK_NEVER
                && overDataSeekBar.getValue() == Preferences.OVER_NETWORK_NEVER) {
            updateIntervalSeekBar.setEnabled(false);
            updateIntervalSeekBar.setSummary(UPDATE_INTERVAL_NAMES[0]);
        } else {
            updateIntervalSeekBar.setEnabled(true);
            updateIntervalSeekBar.setSummary(UPDATE_INTERVAL_NAMES[updateIntervalSeekBar.getValue()]);
        }
    }

    private void updateSummary(String key, boolean changing) {

        switch (key) {

            case Preferences.PREF_UPDATE_INTERVAL:
                updateIntervalSeekBar.setMax(Preferences.UPDATE_INTERVAL_VALUES.length - 1);
                int seekBarPosition = updateIntervalSeekBar.getValue();
                updateIntervalSeekBar.setSummary(UPDATE_INTERVAL_NAMES[seekBarPosition]);
                break;

            case Preferences.PREF_OVER_WIFI:
                overWifiSeekBar.setMax(Preferences.OVER_NETWORK_ALWAYS);
                setNetworkSeekBarSummary(overWifiSeekBar);
                enableUpdateInverval();
                break;

            case Preferences.PREF_OVER_DATA:
                overDataSeekBar.setMax(Preferences.OVER_NETWORK_ALWAYS);
                setNetworkSeekBarSummary(overDataSeekBar);
                enableUpdateInverval();
                break;

            case Preferences.PREF_UPDATE_NOTIFICATION_ENABLED:
                checkSummary(key, R.string.notify_on);
                break;

            case Preferences.PREF_THEME:
                entrySummary(key);
                if (changing) {
                    Activity activity = getActivity();
                    FDroidApp fdroidApp = (FDroidApp) activity.getApplication();
                    fdroidApp.reloadTheme();
                    fdroidApp.applyTheme(activity);
                    FDroidApp.forceChangeTheme(activity);
                }
                break;

            case Preferences.PREF_SHOW_INCOMPAT_VERSIONS:
                checkSummary(key, R.string.show_incompat_versions_on);
                break;

            case Preferences.PREF_SHOW_ROOT_APPS:
                checkSummary(key, R.string.show_root_apps_on);
                break;

            case Preferences.PREF_SHOW_ANTI_FEATURE_APPS:
                checkSummary(key, R.string.show_anti_feature_apps_on);
                break;

            case Preferences.PREF_FORCE_TOUCH_APPS:
                checkSummary(key, R.string.force_touch_apps_on);
                break;

            case Preferences.PREF_LOCAL_REPO_NAME:
                textSummary(key, R.string.local_repo_name_summary);
                break;

            case Preferences.PREF_LOCAL_REPO_HTTPS:
                checkSummary(key, R.string.local_repo_https_on);
                break;

            case Preferences.PREF_LANGUAGE:
                entrySummary(key);
                if (changing) {
                    Activity activity = getActivity();
                    Languages.setLanguage(activity);

                    RepoProvider.Helper.clearEtags(getActivity());
                    UpdateService.updateNow(getActivity());

                    Languages.forceChangeLanguage(activity);
                }
                break;

            case Preferences.PREF_KEEP_CACHE_TIME:
                entrySummary(key);
                if (changing
                        && currentKeepCacheTime != Preferences.get().getKeepCacheTime()) {
                    CleanCacheService.schedule(getActivity());
                }
                break;

            case Preferences.PREF_EXPERT:
                checkSummary(key, R.string.expert_on);
                boolean isExpertMode = Preferences.get().expertMode();
                for (int i = 0; i < otherPrefGroup.getPreferenceCount(); i++) {
                    Preference pref = otherPrefGroup.getPreference(i);
                    if (TextUtils.equals(Preferences.PREF_EXPERT, pref.getDependency())) {
                        pref.setVisible(isExpertMode);
                    }
                }
                if (changing) {
                    RecyclerView recyclerView = getListView();
                    recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
                }
                break;

            case Preferences.PREF_PRIVILEGED_INSTALLER:
                // We may have removed this preference if it is not suitable to show the user.
                // So lets check it is here first.
                final CheckBoxPreference pref = (CheckBoxPreference) findPreference(
                        Preferences.PREF_PRIVILEGED_INSTALLER);
                if (pref != null) {
                    checkSummary(key, R.string.system_installer_on);
                }
                break;

            case Preferences.PREF_ENABLE_PROXY:
                SwitchPreference checkPref = (SwitchPreference) findPreference(key);
                checkPref.setSummary(R.string.enable_proxy_summary);
                break;

            case Preferences.PREF_PROXY_HOST:
                EditTextPreference textPref = (EditTextPreference) findPreference(key);
                String text = Preferences.get().getProxyHost();
                if (TextUtils.isEmpty(text) || text.equals(Preferences.DEFAULT_PROXY_HOST)) {
                    textPref.setSummary(R.string.proxy_host_summary);
                } else {
                    textPref.setSummary(text);
                }
                break;

            case Preferences.PREF_PROXY_PORT:
                EditTextPreference textPref2 = (EditTextPreference) findPreference(key);
                int port = Preferences.get().getProxyPort();
                if (port == Preferences.DEFAULT_PROXY_PORT) {
                    textPref2.setSummary(R.string.proxy_port_summary);
                } else {
                    textPref2.setSummary(String.valueOf(port));
                }
                break;

            case Preferences.PREF_KEEP_INSTALL_HISTORY:
                if (keepInstallHistoryPref.isChecked()) {
                    InstallHistoryService.register(getActivity());
                    installHistoryPref.setVisible(true);
                } else {
                    InstallHistoryService.unregister(getActivity());
                    installHistoryPref.setVisible(false);
                }
                break;
        }
    }

    /**
     * Initializes SystemInstaller preference, which can only be enabled when F-Droid is installed as a system-app
     */
    private void initPrivilegedInstallerPreference() {
        final CheckBoxPreference pref = (CheckBoxPreference) findPreference(Preferences.PREF_PRIVILEGED_INSTALLER);

        // This code will be run each time the activity is resumed, and so we may have already removed
        // this preference.
        if (pref == null) {
            return;
        }

        Preferences p = Preferences.get();
        boolean enabled = p.isPrivilegedInstallerEnabled();
        boolean installed = PrivilegedInstaller.isExtensionInstalledCorrectly(getActivity())
                == PrivilegedInstaller.IS_EXTENSION_INSTALLED_YES;

        // On later versions of Android the privileged installer needs to be installed
        // via flashing an update.zip or building into a rom. As such, if it isn't installed
        // by the time the user boots, opens F-Droid, and views this settings page, then there
        // is no benefit showing it to them (it will only be disabled and we can't offer any
        // way to easily install from here.
        if (Build.VERSION.SDK_INT > 19 && !installed) {
            if (pref != null) {
                otherPrefGroup.removePreference(pref);
            }
        } else {
            pref.setEnabled(installed);
            pref.setDefaultValue(installed);
            pref.setChecked(enabled && installed);

            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                    if (pref.isChecked()) {
                        editor.remove(Preferences.PREF_PRIVILEGED_INSTALLER);
                    } else {
                        editor.putBoolean(Preferences.PREF_PRIVILEGED_INSTALLER, false);
                    }
                    editor.apply();
                    return true;
                }
            });
        }
    }

    private void initUpdatePrivilegedExtensionPreference() {
        if (Build.VERSION.SDK_INT > 19) {
            // this will never work on newer Android versions, so hide it
            otherPrefGroup.removePreference(updatePrivilegedExtensionPref);
            return;
        }
        updatePrivilegedExtensionPref.setPersistent(false);
        updatePrivilegedExtensionPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Open details of F-Droid Privileged
                Intent intent = new Intent(getActivity(), AppDetails2.class);
                intent.putExtra(AppDetails2.EXTRA_APPID,
                        PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME);
                startActivity(intent);

                return true;
            }
        });
    }

    /**
     * If a user specifies they want to fetch updates automatically, then start the download of relevant
     * updates as soon as they enable the feature.
     * Also, if the user has the priv extention installed then change the label to indicate that it
     * will actually _install_ apps, not just fetch their .apk file automatically.
     */
    private void initAutoFetchUpdatesPreference() {
        updateAutoDownloadPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean && (boolean) newValue) {
                    UpdateService.autoDownloadUpdates(getActivity());
                }
                return true;
            }
        });

        if (PrivilegedInstaller.isDefault(getActivity())) {
            updateAutoDownloadPref.setTitle(R.string.update_auto_install);
            updateAutoDownloadPref.setSummary(R.string.update_auto_install_summary);
        }
    }

    /**
     * The default for "Use Tor" is dynamically set based on whether Orbot is installed.
     */
    private void initUseTorPreference() {
        boolean useTor = Preferences.get().isTorEnabled();
        useTorCheckPref.setDefaultValue(useTor);
        useTorCheckPref.setChecked(useTor);
        useTorCheckPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object enabled) {
                if ((Boolean) enabled) {
                    final Activity activity = getActivity();
                    enableProxyCheckPref.setEnabled(false);
                    if (OrbotHelper.isOrbotInstalled(activity)) {
                        NetCipher.useTor();
                    } else {
                        Intent intent = OrbotHelper.getOrbotInstallIntent(activity);
                        activity.startActivityForResult(intent, REQUEST_INSTALL_ORBOT);
                    }
                } else {
                    enableProxyCheckPref.setEnabled(true);
                    NetCipher.clearProxy();
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        for (final String key : SUMMARIES_TO_UPDATE) {
            updateSummary(key, false);
        }

        currentKeepCacheTime = Preferences.get().getKeepCacheTime();

        initAutoFetchUpdatesPreference();
        initPrivilegedInstallerPreference();
        initUpdatePrivilegedExtensionPreference();
        initUseTorPreference();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        Preferences.get().configureProxy();

        if (updateIntervalPrevious != updateIntervalSeekBar.getValue()) {
            UpdateService.schedule(getActivity());
        } else if (Build.VERSION.SDK_INT >= 21 &&
                (overWifiPrevious != overWifiSeekBar.getValue() || overDataPrevious != overDataSeekBar.getValue())) {
            UpdateService.schedule(getActivity());
        }
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        updateSummary(key, true);
        if (key.equals(Preferences.PREF_PREVENT_SCREENSHOTS)) {
            if (Preferences.get().preventScreenshots()) {
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        }
    }
}
