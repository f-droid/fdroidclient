/*
 * Copyright (C) 2021  Hans-Christoph Steiner <hans@eds.org>
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid.work;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.net.HttpPoster;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * This gathers all the information needed for F-Droid Metrics, aka the
 * "Popularity Contest", and submits it to the Clean Insights Matomo.  This
 * should <b>never</b> include any Personally Identifiable Information (PII)
 * like telephone numbers, IP Addresses, MAC, SSID, IMSI, IMEI, user accounts,
 * etc.
 * <p>
 * This uses static methods so that they can easily be tested in Robolectric
 * rather than painful, slow, flaky emulator tests.
 */
public class FDroidMetricsWorker extends Worker {

    public static final String TAG = "FDroidMetricsWorker";

    static SimpleDateFormat weekFormatter = new SimpleDateFormat("yyyy ww", Locale.ENGLISH);

    private static final ArrayList<MatomoEvent> EVENTS = new ArrayList<>();

    public FDroidMetricsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Schedule or cancel a work request to update the app index, according to the
     * current preferences.  It is meant to run weekly, so it will schedule one week
     * from the last run.  If it has never been run, it will run as soon as possible.
     * <p>
     * Although {@link Constraints.Builder#setRequiresDeviceIdle(boolean)} is available
     * down to {@link Build.VERSION_CODES#M}, it will cause {@code UpdateService} to
     * rarely run, if ever on some devices.  So {@link Constraints.Builder#setRequiresDeviceIdle(boolean)}
     * should only be used in conjunction with
     * {@link Constraints.Builder#setTriggerContentMaxDelay(long, TimeUnit)} to ensure
     * that updates actually happen regularly.
     */
    public static void schedule(final Context context) {
        final WorkManager workManager = WorkManager.getInstance(context);
        long interval = TimeUnit.DAYS.toMillis(7);

        final Constraints.Builder constraintsBuilder = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true);
        // TODO use the Data/WiFi preferences here
        if (Build.VERSION.SDK_INT >= 24) {
            constraintsBuilder.setTriggerContentMaxDelay(interval, TimeUnit.MILLISECONDS);
            constraintsBuilder.setRequiresDeviceIdle(true);
        }
        final PeriodicWorkRequest cleanCache =
                new PeriodicWorkRequest.Builder(FDroidMetricsWorker.class, interval, TimeUnit.MILLISECONDS)
                        .setConstraints(constraintsBuilder.build())
                        .build();
        workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, cleanCache);
        Utils.debugLog(TAG, "Scheduled periodic work");
    }

    public static void cancel(final Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(TAG);
    }

    @NonNull
    @Override
    public Result doWork() {
        // TODO check useTor preference and force-submit over Tor.
        String json = generateReport(getApplicationContext());
        try {
            HttpPoster httpPoster = new HttpPoster("https://metrics.cleaninsights.org/cleaninsights.php");
            httpPoster.post(json);
            return ListenableWorker.Result.success();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ListenableWorker.Result.retry();
    }

    /**
     * Convert a Java timestamp in milliseconds to a CleanInsights/Matomo timestamp
     * normalized to the week and in UNIX epoch seconds format.
     */
    static long toCleanInsightsTimestamp(long timestamp) {
        return toCleanInsightsTimestamp(timestamp, timestamp);
    }

    /**
     * Convert a Java timestamp in milliseconds to a CleanInsights/Matomo timestamp
     * normalized to the week and in UNIX epoch seconds format, plus the time
     * difference between {@code relativeTo} and {@code timestamp}.
     */
    static long toCleanInsightsTimestamp(long relativeTo, long timestamp) {
        long diff = timestamp - relativeTo;
        long weekNumber = timestamp / DateUtils.WEEK_IN_MILLIS;
        return ((weekNumber * DateUtils.WEEK_IN_MILLIS) + diff) / 1000L;
    }

    static boolean isTimestampInReportingWeek(long timestamp) {
        return isTimestampInReportingWeek(getReportingWeekStart(), timestamp);
    }

    static boolean isTimestampInReportingWeek(long weekStart, long timestamp) {
        long weekEnd = weekStart + DateUtils.WEEK_IN_MILLIS;
        return weekStart < timestamp && timestamp < weekEnd;
    }

    static long getVersionCode(PackageInfo packageInfo) {
        if (Build.VERSION.SDK_INT < 28) {
            return packageInfo.versionCode;
        } else {
            return packageInfo.getLongVersionCode();
        }
    }

    /**
     * Gets the most recent week that is over based on the current time.
     *
     * @return start timestamp or 0 on parsing error
     */
    static long getReportingWeekStart() {
        return getReportingWeekStart(System.currentTimeMillis());
    }

    /**
     * Gets the most recent week that is over based on {@code timestamp}. This
     * is the testable version of {@link #getReportingWeekStart()}
     *
     * @return start timestamp or 0 on parsing error
     */
    static long getReportingWeekStart(long timestamp) {
        try {
            Date start = new Date(timestamp - DateUtils.WEEK_IN_MILLIS);
            return weekFormatter.parse(weekFormatter.format(start)).getTime();
        } catch (ParseException e) {
            // ignored
        }
        return 0;
    }

    /**
     * Reads the {@link InstallHistoryService} CSV log, debounces the duplicate events,
     * then converts it to {@link MatomoEvent} instances to be gathered.
     */
    static Collection<? extends MatomoEvent> parseInstallHistoryCsv(Context context, long weekStart) {
        try {
            File csv = InstallHistoryService.getInstallHistoryFile(context);
            List<String> lines = FileUtils.readLines(csv, Charset.defaultCharset());
            List<RawEvent> events = new ArrayList<>(lines.size());
            for (String line : lines) {
                RawEvent event = new RawEvent(line.split(","));
                if (isTimestampInReportingWeek(weekStart, event.timestamp)) {
                    events.add(event);
                }
            }
            Collections.sort(events, new Comparator<RawEvent>() {
                @Override
                public int compare(RawEvent e0, RawEvent e1) {
                    int applicationIdComparison = e0.applicationId.compareTo(e1.applicationId);
                    if (applicationIdComparison != 0) {
                        return applicationIdComparison;
                    }
                    int versionCodeComparison = Long.compare(e0.versionCode, e1.versionCode);
                    if (versionCodeComparison != 0) {
                        return versionCodeComparison;
                    }
                    int timestampComparison = Long.compare(e0.timestamp, e1.timestamp);
                    if (timestampComparison != 0) {
                        return timestampComparison;
                    }
                    return 0;
                }
            });
            List<MatomoEvent> toReport = new ArrayList<>();
            RawEvent previousEvent = new RawEvent(new String[]{"0", "", "0", ""});
            for (RawEvent event : events) {
                if (!previousEvent.equals(event)) {
                    toReport.add(new MatomoEvent(event));
                    previousEvent = event;
                }
            }
            // TODO add time to INSTALL_COMPLETE evnts, eg INSTALL_COMPLETE - INSTALL_STARTED
            return toReport;
        } catch (IOException e) {
            // ignored
        }
        return Collections.emptyList();
    }

    public static String generateReport(Context context) {
        long weekStart = getReportingWeekStart();
        CleanInsightsReport cleanInsightsReport = new CleanInsightsReport();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
        Collections.sort(packageInfoList, new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo p1, PackageInfo p2) {
                return p1.packageName.compareTo(p2.packageName);
            }
        });
        App[] installedApps = InstalledAppProvider.Helper.all(context);
        EVENTS.add(getDeviceEvent(weekStart, "isPrivilegedInstallerEnabled",
                Preferences.get().isPrivilegedInstallerEnabled()));
        EVENTS.add(getDeviceEvent(weekStart, "Build.VERSION.SDK_INT", Build.VERSION.SDK_INT));
        if (Build.VERSION.SDK_INT >= 21) {
            EVENTS.add(getDeviceEvent(weekStart, "Build.SUPPORTED_ABIS", Arrays.toString(Build.SUPPORTED_ABIS)));
        }

        for (PackageInfo packageInfo : packageInfoList) {
            boolean found = false;
            for (App app : installedApps) {
                if (packageInfo.packageName.equals(app.packageName)) {
                    found = true;
                    break;
                }
            }
            if (!found) continue;

            if (isTimestampInReportingWeek(weekStart, packageInfo.firstInstallTime)) {
                addFirstInstallEvent(pm, packageInfo);
            }
            if (isTimestampInReportingWeek(weekStart, packageInfo.lastUpdateTime)) {
                addLastUpdateTimeEvent(pm, packageInfo);
            }
        }
        EVENTS.addAll(parseInstallHistoryCsv(context, weekStart));
        cleanInsightsReport.events = EVENTS.toArray(new MatomoEvent[0]);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return mapper.writeValueAsString(cleanInsightsReport);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Bare minimum report data in CleanInsights/Matomo format.
     *
     * @see MatomoEvent
     * @see <a href="https://gitlab.com/cleaninsights/clean-insights-matomo-proxy#api">CleanInsights CIMP API</a>
     * @see <a href="https://matomo.org/docs/event-tracking/">Matomo Event Tracking</a>
     */
    private static class CleanInsightsReport {
        @JsonProperty
        MatomoEvent[] events = new MatomoEvent[0];
        @JsonProperty
        final long idsite = 3; // NOPMD
        @JsonProperty
        final String lang = Locale.getDefault().getLanguage();
        @JsonProperty
        final String ua = Utils.getUserAgent();
    }

    private static void addFirstInstallEvent(PackageManager pm, PackageInfo packageInfo) {
        addInstallerEvent(pm, packageInfo, "PackageInfo.firstInstall", packageInfo.firstInstallTime);
    }

    private static void addLastUpdateTimeEvent(PackageManager pm, PackageInfo packageInfo) {
        addInstallerEvent(pm, packageInfo, "PackageInfo.lastUpdateTime", packageInfo.lastUpdateTime);
    }

    private static void addInstallerEvent(
            PackageManager pm, PackageInfo packageInfo, String action, long timestamp) {
        MatomoEvent matomoEvent = new MatomoEvent(timestamp);
        matomoEvent.category = "APK";
        matomoEvent.action = action;
        matomoEvent.name = pm.getInstallerPackageName(packageInfo.packageName);
        matomoEvent.times = 1;
        for (MatomoEvent me : EVENTS) {
            if (me.equals(matomoEvent)) {
                me.times++;
                return;
            }
        }
        EVENTS.add(matomoEvent);
    }

    /**
     * Events which describe the device that is doing the reporting.
     */
    private static MatomoEvent getDeviceEvent(long startTime, String action, Object name) {
        MatomoEvent matomoEvent = new MatomoEvent(startTime);
        matomoEvent.category = "device";
        matomoEvent.action = action;
        matomoEvent.name = String.valueOf(name);
        matomoEvent.times = 1;
        return matomoEvent;
    }

    /**
     * An event to send to CleanInsights/Matomo with a period of a full,
     * normalized week.
     *
     * @see <a href="https://gitlab.com/cleaninsights/clean-insights-design/-/blob/d4f96ae3/schemas/cimp.schema.json">CleanInsights JSON Schema</a>
     * @see <a href="https://matomo.org/docs/event-tracking/">Matomo Event Tracking</a>
     */
    @SuppressWarnings("checkstyle:MemberName")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class MatomoEvent {
        @JsonProperty
        String category;
        @JsonProperty
        String action;
        @JsonProperty
        String name;
        @JsonProperty
        final long period_start;
        @JsonProperty
        final long period_end;
        @JsonProperty
        long times = 0;
        @JsonProperty
        String value;

        MatomoEvent(long timestamp) {
            period_end = toCleanInsightsTimestamp(timestamp);
            period_start = period_end - (DateUtils.WEEK_IN_MILLIS / 1000);
        }

        MatomoEvent(RawEvent rawEvent) {
            this(rawEvent.timestamp);
            category = "package";
            action = rawEvent.action;
            name = rawEvent.applicationId;
            times = 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MatomoEvent that = (MatomoEvent) o;
            return period_start == that.period_start &&
                    period_end == that.period_end &&
                    TextUtils.equals(category, that.category) &&
                    TextUtils.equals(action, that.action) &&
                    TextUtils.equals(name, that.name);
        }
    }

    /**
     * A raw event as read from {@link InstallHistoryService}'s CSV log file.
     * This should never leave the device as is, it must have data stripped
     * from it first.
     */
    static class RawEvent {
        final long timestamp;
        final String applicationId;
        final long versionCode;
        final String action;

        RawEvent(String[] o) {
            timestamp = Long.parseLong(o[0]);
            applicationId = o[1];
            versionCode = Long.parseLong(o[2]);
            action = o[3];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RawEvent event = (RawEvent) o;
            return versionCode == event.versionCode &&
                    applicationId.equals(event.applicationId) &&
                    action.equals(event.action);
        }

        @Override
        public int hashCode() {
            if (Build.VERSION.SDK_INT >= 19) {
                return Objects.hash(applicationId, versionCode, action);
            } else {
                return new Random().nextInt(); // quick kludge
            }
        }

        @Override
        public String toString() {
            return "RawEvent{" +
                    "timestamp=" + timestamp +
                    ", applicationId='" + applicationId + '\'' +
                    ", versionCode=" + versionCode +
                    ", action='" + action + '\'' +
                    '}';
        }
    }
}
