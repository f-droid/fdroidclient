package org.fdroid.fdroid;

import android.app.Application;

/**
 * Due to there being so much static initialization in the main FDroidApp, it becomes hard to reset
 * that state between Robolectric test runs. Therefore, robolectric tests will default to this
 * {@link Application} instead of {@link FDroidApp}. It intentionally doesn't extends {@link FDroidApp}
 * so that the static initialization in {@link FDroidApp#onCreate()} is not executed.
 */
@SuppressWarnings("unused")
public class TestFDroidApp extends Application {

}
