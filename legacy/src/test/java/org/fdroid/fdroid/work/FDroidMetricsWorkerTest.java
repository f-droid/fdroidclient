package org.fdroid.fdroid.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ContextWrapper;
import android.text.format.DateUtils;

import androidx.test.core.app.ApplicationProvider;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.work.FDroidMetricsWorker.MatomoEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

@Config(application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class FDroidMetricsWorkerTest {
    protected ContextWrapper context;

    @Before
    public final void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Preferences.setupForTests(context);
    }

    @Test
    public void testNormalizeTimestampToWeek() {
        long startTime = 1610038865743L;
        long endTime = 1610037631519L;

        long normalizedStart = FDroidMetricsWorker.toCleanInsightsTimestamp(startTime);
        long normalizedEnd = FDroidMetricsWorker.toCleanInsightsTimestamp(endTime);
        assertEquals(normalizedStart, normalizedEnd);

        long normalizedRelativeEnd = FDroidMetricsWorker.toCleanInsightsTimestamp(startTime, endTime);
        assertEquals(1609976365L, normalizedRelativeEnd);
    }

    @Test
    public void testGenerateReport() throws IOException {
        String json = FDroidMetricsWorker.generateReport(context);
        System.out.println(json);
        File downloads = new File(System.getenv("HOME"), "Downloads");
        if (downloads.exists()) {
            File output = new File(downloads, getClass().getName() + ".testGenerateReport.json");
            FileUtils.writeStringToFile(output, json);
        }
        // TODO validate against the schema
    }

    @Test
    public void testParseInstallHistory() throws IOException {
        FileUtils.copyFile(TestUtils.copyResourceToTempFile("install_history_all"),
                InstallHistoryService.getInstallHistoryFile(context));
        long weekStart = FDroidMetricsWorker.getReportingWeekStart(1611268892206L + DateUtils.WEEK_IN_MILLIS);
        Collection<? extends MatomoEvent> events = FDroidMetricsWorker.parseInstallHistoryCsv(context,
                weekStart);
        assertEquals(3, events.size());
        for (MatomoEvent event : events) {
            assertEquals(event.name, "com.termux");
        }

        Collection<? extends MatomoEvent> oneWeekAgo = FDroidMetricsWorker.parseInstallHistoryCsv(context,
                weekStart - DateUtils.WEEK_IN_MILLIS);
        assertEquals(11, oneWeekAgo.size());

        Collection<? extends MatomoEvent> twoWeeksAgo = FDroidMetricsWorker.parseInstallHistoryCsv(context,
                weekStart - (2 * DateUtils.WEEK_IN_MILLIS));
        assertEquals(0, twoWeeksAgo.size());

        Collection<? extends MatomoEvent> threeWeeksAgo = FDroidMetricsWorker.parseInstallHistoryCsv(context,
                weekStart - (3 * DateUtils.WEEK_IN_MILLIS));
        assertEquals(9, threeWeeksAgo.size());
        assertNotEquals(oneWeekAgo, threeWeeksAgo);
    }

    @Test
    public void testGetReportingWeekStart() throws ParseException {
        long now = System.currentTimeMillis();
        long start = FDroidMetricsWorker.getReportingWeekStart(now);
        assertTrue((now - DateUtils.WEEK_IN_MILLIS) > start);
        assertTrue((now - DateUtils.WEEK_IN_MILLIS) < (start + DateUtils.WEEK_IN_MILLIS));
    }
}
