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

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * This actually runs {@link FDroidMetricsWorker} on a device/emulator and
 * submits a report to https://metrics.cleaninsights.org
 * <p>
 * This is marked with {@link LargeTest} to exclude it from running on GitLab CI
 * because it always fails on the emulator tests there.  Also, it actually submits
 * a report.
 */
@LargeTest
public class FDroidMetricsWorkerTest {
    public static final String TAG = "FDroidMetricsWorkerTest";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Rule
    public WorkManagerTestRule workManagerTestRule = new WorkManagerTestRule();

    /**
     * A test for easy manual testing.
     */
    @Ignore
    @Test
    public void testGenerateReport() throws IOException {
        String json = FDroidMetricsWorker.generateReport(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        System.out.println(json);
    }

    @Test
    public void testWorkRequest() throws ExecutionException, InterruptedException {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FDroidMetricsWorker.class).build();
        workManagerTestRule.workManager.enqueue(request).getResult();
        ListenableFuture<WorkInfo> workInfo = workManagerTestRule.workManager.getWorkInfoById(request.getId());
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.get().getState());
    }
}
