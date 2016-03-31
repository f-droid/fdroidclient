
package org.fdroid.fdroid.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.test.ServiceTestCase;
import android.util.Log;

@SuppressWarnings("PMD")  // TODO port this to JUnit 4 semantics
public class DownloaderServiceTest extends ServiceTestCase<DownloaderService> {
    public static final String TAG = "DownloaderServiceTest";

    String[] urls = {
            "https://en.wikipedia.org/wiki/Index.html",
            "https://mirrors.kernel.org/debian/dists/stable/Release",
            "https://f-droid.org/archive/de.we.acaldav_5.apk",
            // sites that use SNI for HTTPS
            "https://guardianproject.info/fdroid/repo/index.jar",
    };

    public DownloaderServiceTest() {
        super(DownloaderService.class);
    }

    public void testQueueingDownload() throws InterruptedException {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceive " + intent);
            }
        }, new IntentFilter(Downloader.ACTION_PROGRESS));
        for (String url : urls) {
            DownloaderService.queue(getContext(), url);
        }
        Thread.sleep(30000);
    }
}
