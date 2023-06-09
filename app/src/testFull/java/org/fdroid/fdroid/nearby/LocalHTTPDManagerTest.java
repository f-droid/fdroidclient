package org.fdroid.fdroid.nearby;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Test that this can start and stop the webserver.
 */
@RunWith(RobolectricTestRunner.class)
public class LocalHTTPDManagerTest {

    @Test
    @Ignore("TODO this test has worked in the past, but needs work.")
    public void testStartStop() throws InterruptedException {
        ShadowLog.stream = System.out;
        Context context = ApplicationProvider.getApplicationContext();

        final String host = "localhost";
        final int port = 8888;
        assertFalse(Utils.isServerSocketInUse(port));
        LocalHTTPDManager.stop(context);

        FDroidApp.ipAddressString = host;
        FDroidApp.port = port;

        LocalHTTPDManager.start(context, false);
        final CountDownLatch startLatch = new CountDownLatch(1);
        new Thread(() -> {
            while (!Utils.isServerSocketInUse(port)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    fail();
                }
            }
            startLatch.countDown();
        }).start();
        assertTrue(startLatch.await(10, TimeUnit.MINUTES));
        assertTrue(Utils.isServerSocketInUse(port));
        assertTrue(Utils.canConnectToSocket(host, port));

        LocalHTTPDManager.stop(context);
        final CountDownLatch stopLatch = new CountDownLatch(1);
        new Thread(() -> {
            while (Utils.isServerSocketInUse(port)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    fail();
                }
            }
            stopLatch.countDown();
        }).start();
        assertTrue(stopLatch.await(10, TimeUnit.MINUTES));
    }
}
