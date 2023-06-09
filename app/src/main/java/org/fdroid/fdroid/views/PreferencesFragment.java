/*
 * Copyright (C) 2019 Michael Pöhn
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

package org.fdroid.fdroid.views;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.util.ObjectsCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Languages;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.installer.PrivilegedInstaller;
import org.fdroid.fdroid.work.CleanCacheWorker;
import org.fdroid.fdroid.work.FDroidMetricsWorker;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class PreferencesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "PreferencesFragment";

    private static final String[] SUMMARIES_TO_UPDATE = {
            Preferences.PREF_OVER_WIFI,
            Preferences.PREF_OVER_DATA,
            Preferences.PREF_UPDATE_INTERVAL,
            Preferences.PREF_UPDATE_NOTIFICATION_ENABLED,
            Preferences.PREF_SHOW_ANTI_FEATURES,
            Preferences.PREF_SHOW_INCOMPAT_VERSIONS,
            Preferences.PREF_THEME,
            Preferences.PREF_USE_PURE_BLACK_DARK_THEME,
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
    private SwitchPreferenceCompat enableProxyCheckPref;
    private SwitchPreferenceCompat useTorCheckPref;
    private Preference updateAutoDownloadPref;
    private CheckBoxPreference keepInstallHistoryPref;
    private CheckBoxPreference sendToFDroidMetricsPref;
    private Preference installHistoryPref;
    private long currentKeepCacheTime;
    private int overWifiPrevious;
    private int overDataPrevious;
    private int updateIntervalPrevious;

    private LinearSmoothScroller topScroller;

    private RequestManager glideRequestManager;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        Preferences preferences = Preferences.get();
        preferences.migrateOldPreferences();

        addPreferencesFromResource(R.xml.preferences);
        otherPrefGroup = findPreference("pref_category_other");


        Preference aboutPreference = findPreference("pref_about");
        if (aboutPreference != null) {
            aboutPreference.setOnPreferenceClickListener(aboutPrefClickedListener);
        }

        keepInstallHistoryPref = findPreference(Preferences.PREF_KEEP_INSTALL_HISTORY);
        sendToFDroidMetricsPref =
                ObjectsCompat.requireNonNull(findPreference(Preferences.PREF_SEND_TO_FDROID_METRICS));
        sendToFDroidMetricsPref.setEnabled(keepInstallHistoryPref.isChecked());
        installHistoryPref = ObjectsCompat.requireNonNull(findPreference("installHistory"));
        installHistoryPref.setVisible(keepInstallHistoryPref.isChecked());
        if (preferences.isSendingToFDroidMetrics()) {
            installHistoryPref.setTitle(R.string.install_history_and_metrics);
        } else {
            installHistoryPref.setTitle(R.string.install_history);
        }

        useTorCheckPref = ObjectsCompat.requireNonNull(findPreference(Preferences.PREF_USE_TOR));
        useTorCheckPref.setOnPreferenceChangeListener(useTorChangedListener);
        enableProxyCheckPref = ObjectsCompat.requireNonNull(findPreference(Preferences.PREF_ENABLE_PROXY));
        enableProxyCheckPref.setOnPreferenceChangeListener(proxyEnabledChangedListener);
        updateAutoDownloadPref = findPreference(Preferences.PREF_AUTO_DOWNLOAD_INSTALL_UPDATES);

        overWifiSeekBar = ObjectsCompat.requireNonNull(findPreference(Preferences.PREF_OVER_WIFI));
        overWifiPrevious = overWifiSeekBar.getValue();
        overWifiSeekBar.setSeekBarLiveUpdater(this::getNetworkSeekBarSummary);
        overDataSeekBar = ObjectsCompat.requireNonNull(findPreference(Preferences.PREF_OVER_DATA));
        overDataPrevious = overDataSeekBar.getValue();
        overDataSeekBar.setSeekBarLiveUpdater(this::getNetworkSeekBarSummary);
        updateIntervalSeekBar = ObjectsCompat.requireNonNull(findPreference(Preferences.PREF_UPDATE_INTERVAL));
        updateIntervalPrevious = updateIntervalSeekBar.getValue();
        updateIntervalSeekBar.setSeekBarLiveUpdater(position -> getString(UPDATE_INTERVAL_NAMES[position]));

        ListPreference languagePref = ObjectsCompat.requireNonNull(findPreference(Preferences.PREF_LANGUAGE));
        if (Build.VERSION.SDK_INT >= 24) {
            PreferenceCategory category = ObjectsCompat.requireNonNull(findPreference("pref_category_display"));
            category.removePreference(languagePref);
        } else {
            Languages languages = Languages.get((AppCompatActivity) getActivity());
            languagePref.setDefaultValue(Languages.USE_SYSTEM_DEFAULT);
            languagePref.setEntries(languages.getAllNames());
            languagePref.setEntryValues(languages.getSupportedLocales());
        }

        if (requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
            PreferenceCategory category = ObjectsCompat.requireNonNull(findPreference(
                    "pref_category_appcompatibility"));
            category.removePreference(
                    ObjectsCompat.requireNonNull(findPreference(Preferences.PREF_FORCE_TOUCH_APPS)));
        }

        topScroller = new LinearSmoothScroller(requireActivity()) {
            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
        };
    }

    private void checkSummary(String key, int resId) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setSummary(resId);
        }
    }

    private void entrySummary(String key) {
        ListPreference pref = findPreference(key);
        if (pref != null) {
            pref.setSummary(pref.getEntry());
        }
    }

    private void textSummary(String key, int resId) {
        EditTextPreference pref = findPreference(key);
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
                    FDroidApp.applyTheme();
                }
                break;

            case Preferences.PREF_USE_PURE_BLACK_DARK_THEME:
                if (changing) {
                    AppCompatActivity activity = (AppCompatActivity) getActivity();
                    // Theme will be applied upon activity creation
                    if (activity != null) {
                        ActivityCompat.recreate(activity);
                    }
                }
                break;

            case Preferences.PREF_SHOW_INCOMPAT_VERSIONS:
                checkSummary(key, R.string.show_incompat_versions_on);
                break;

            case Preferences.PREF_SHOW_ANTI_FEATURES:
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
                    AppCompatActivity activity = (AppCompatActivity) requireActivity();
                    Languages.setLanguage(activity);
                    FDroidApp.onLanguageChanged(activity.getApplicationContext());
                    Languages.forceChangeLanguage(activity);
                }
                break;

            case Preferences.PREF_KEEP_CACHE_TIME:
                entrySummary(key);
                if (changing
                        && currentKeepCacheTime != Preferences.get().getKeepCacheTime()) {
                    CleanCacheWorker.schedule(requireContext());
                }
                break;

            case Preferences.PREF_EXPERT:
                checkSummary(key, R.string.expert_on);
                int expertPreferencesCount = 0;
                boolean isExpertMode = Preferences.get().expertMode();
                for (int i = 0; i < otherPrefGroup.getPreferenceCount(); i++) {
                    Preference pref = otherPrefGroup.getPreference(i);
                    if (TextUtils.equals(Preferences.PREF_EXPERT, pref.getDependency())) {
                        pref.setVisible(isExpertMode);
                        expertPreferencesCount++;
                    }
                }
                if (changing) {
                    RecyclerView recyclerView = getListView();
                    int preferencesCount = recyclerView.getAdapter().getItemCount();
                    if (!isExpertMode) {
                        expertPreferencesCount = 0;
                    }
                    topScroller.setTargetPosition(preferencesCount - expertPreferencesCount - 1);
                    recyclerView.getLayoutManager().startSmoothScroll(topScroller);
                }
                break;

            case Preferences.PREF_PRIVILEGED_INSTALLER:
                // We may have removed this preference if it is not suitable to show the user.
                // So lets check it is here first.
                final CheckBoxPreference pref = findPreference(
                        Preferences.PREF_PRIVILEGED_INSTALLER);
                if (pref != null) {
                    checkSummary(key, R.string.system_installer_on);
                }
                break;

            case Preferences.PREF_ENABLE_PROXY:
                SwitchPreferenceCompat checkPref = ObjectsCompat.requireNonNull(findPreference(key));
                checkPref.setSummary(R.string.enable_proxy_summary);
                break;

            case Preferences.PREF_PROXY_HOST:
                EditTextPreference textPref = ObjectsCompat.requireNonNull(findPreference(key));
                String text = Preferences.get().getProxyHost();
                if (TextUtils.isEmpty(text) || text.equals(Preferences.DEFAULT_PROXY_HOST)) {
                    textPref.setSummary(R.string.proxy_host_summary);
                } else {
                    textPref.setSummary(text);
                }
                break;

            case Preferences.PREF_PROXY_PORT:
                EditTextPreference textPref2 = ObjectsCompat.requireNonNull(findPreference(key));
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
                    sendToFDroidMetricsPref.setEnabled(true);
                } else {
                    InstallHistoryService.unregister(getActivity());
                    installHistoryPref.setVisible(false);
                    sendToFDroidMetricsPref.setEnabled(false);
                }
                setFDroidMetricsWorker();
                break;

            case Preferences.PREF_SEND_TO_FDROID_METRICS:
                setFDroidMetricsWorker();
                break;
        }
    }

    private void setFDroidMetricsWorker() {
        if (sendToFDroidMetricsPref.isEnabled() && sendToFDroidMetricsPref.isChecked()) {
            FDroidMetricsWorker.schedule(getContext());
        } else {
            FDroidMetricsWorker.cancel(getContext());
        }
    }

    /**
     * About dialog click listener
     * <p>
     * TODO: this might need to be changed when updated to the new preference pattern
     */

    private final Preference.OnPreferenceClickListener aboutPrefClickedListener = preference -> {
        final View view = getLayoutInflater().inflate(R.layout.about, null);
        final Context context = requireContext();

        String versionName = Utils.getVersionName(context);
        if (versionName != null) {
            TextView versionNameView = view.findViewById(R.id.version);
            versionNameView.setText(versionName);
            versionNameView.setOnLongClickListener(v -> {
                throw new RuntimeException("BOOM!");
            });
        }
        new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .show();
        return true;
    };

    /**
     * Initializes SystemInstaller preference, which can only be enabled when F-Droid is installed as a system-app
     */
    private void initPrivilegedInstallerPreference() {
        final CheckBoxPreference pref = findPreference(Preferences.PREF_PRIVILEGED_INSTALLER);

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
        if (!installed) {
            otherPrefGroup.removePreference(pref);
        } else {
            pref.setEnabled(true);
            pref.setDefaultValue(true);
            pref.setChecked(enabled);
            pref.setOnPreferenceClickListener(preference -> {
                SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                if (pref.isChecked()) {
                    editor.remove(Preferences.PREF_PRIVILEGED_INSTALLER);
                } else {
                    editor.putBoolean(Preferences.PREF_PRIVILEGED_INSTALLER, false);
                }
                editor.apply();
                return true;
            });
        }
    }

    /**
     * If a user specifies they want to fetch updates automatically, then start the download of relevant
     * updates as soon as they enable the feature.
     * Also, if the user has the priv extension installed then change the label to indicate that it
     * will actually _install_ apps, not just fetch their .apk file automatically.
     */
    private void initAutoFetchUpdatesPreference() {
        updateAutoDownloadPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof Boolean && (boolean) newValue) {
                UpdateService.autoDownloadUpdates(getActivity());
            }
            return true;
        });

        if (PrivilegedInstaller.isDefault(getActivity())) {
            updateAutoDownloadPref.setTitle(R.string.update_auto_install);
            updateAutoDownloadPref.setSummary(R.string.update_auto_install_summary);
        }
    }

    /**
     * The default for "Use Tor" is dynamically set based on whether Orbot is installed.
     */
    private void initUseTorPreference(Context context) {
        useTorCheckPref.setDefaultValue(OrbotHelper.isOrbotInstalled(context));
        useTorCheckPref.setChecked(Preferences.get().isTorEnabled());
    }

    private final Preference.OnPreferenceChangeListener useTorChangedListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object enabled) {
                    if ((Boolean) enabled) {
                        enableProxyCheckPref.setChecked(false);
                        final AppCompatActivity activity = (AppCompatActivity) requireActivity();
                        if (!OrbotHelper.isOrbotInstalled(activity)) {
                            Intent intent = OrbotHelper.getOrbotInstallIntent(activity);
                            activity.startActivityForResult(intent, REQUEST_INSTALL_ORBOT);
                        }
                        // NetCipher gets configured to use Tor in onPause()
                        // via a call to FDroidApp.configureProxy()
                    }
                    return true;
                }
            };

    private final Preference.OnPreferenceChangeListener proxyEnabledChangedListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object enabled) {
                    if ((Boolean) enabled) {
                        useTorCheckPref.setChecked(false);
                    }
                    return true;
                }
            };

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
        initUseTorPreference(requireContext().getApplicationContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        FDroidApp.configureProxy(Preferences.get());

        if (updateIntervalPrevious != updateIntervalSeekBar.getValue()) {
            UpdateService.schedule(getActivity());
        } else if (overWifiPrevious != overWifiSeekBar.getValue() || overDataPrevious != overDataSeekBar.getValue()) {
            UpdateService.schedule(getActivity());
        }
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        updateSummary(key, true);
        //noinspection IfCanBeSwitch
        if (key.equals(Preferences.PREF_PREVENT_SCREENSHOTS)) {
            if (Preferences.get().preventScreenshots()) {
                requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        } else if (Preferences.PREF_SEND_TO_FDROID_METRICS.equals(key)) {
            if (Preferences.get().isSendingToFDroidMetrics()) {
                String msg = getString(R.string.toast_metrics_in_install_history,
                        requireContext().getString(R.string.app_name));
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                installHistoryPref.setTitle(R.string.install_history_and_metrics);
                Intent intent = new Intent(getActivity(), InstallHistoryActivity.class);
                intent.putExtra(InstallHistoryActivity.EXTRA_SHOW_FDROID_METRICS, true);
                startActivity(intent);
            } else {
                installHistoryPref.setTitle(R.string.install_history);
            }
        } else if (Preferences.PREF_OVER_DATA.equals(key) || Preferences.PREF_OVER_WIFI.equals(key)) {
            if (glideRequestManager == null) {
                glideRequestManager = Glide.with(requireContext());
            }
            glideRequestManager.applyDefaultRequestOptions(new RequestOptions()
                    .onlyRetrieveFromCache(Preferences.get().isBackgroundDownloadAllowed()));
        }
    }
}
