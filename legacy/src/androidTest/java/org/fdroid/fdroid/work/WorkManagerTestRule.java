package org.fdroid.fdroid.work;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class WorkManagerTestRule extends TestWatcher {
    WorkManager workManager;

    @Override
    protected void starting(Description description) {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context targetContext = instrumentation.getTargetContext();
        Configuration configuration = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(targetContext, configuration);
        workManager = WorkManager.getInstance(targetContext);
    }
}
