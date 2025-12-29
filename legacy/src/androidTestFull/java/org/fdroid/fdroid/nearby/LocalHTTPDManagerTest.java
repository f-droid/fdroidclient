package org.fdroid.fdroid.nearby;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Netstat;
import org.fdroid.fdroid.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test the nearby webserver in the emulator.
 */
@Ignore // TODO this test has worked in the past, but needs work.
@RunWith(AndroidJUnit4.class)
public class LocalHTTPDManagerTest {
    private static final String TAG = "LocalHTTPDManagerTest";

    private Context context;
    private LocalBroadcastManager lbm;

    private static final String LOCALHOST = "localhost";
    private static final int PORT = 8888;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        lbm = LocalBroadcastManager.getInstance(context);

        FDroidApp.ipAddressString = LOCALHOST;
        FDroidApp.port = PORT;

        for (Netstat.Connection connection : Netstat.getConnections()) { // NOPMD
            Log.i("LocalHTTPDManagerTest", "connection: " + connection.toString());
        }
        assertFalse(Utils.isServerSocketInUse(PORT));
        LocalHTTPDManager.stop(context);

        for (Netstat.Connection connection : Netstat.getConnections()) { // NOPMD
            Log.i("LocalHTTPDManagerTest", "connection: " + connection.toString());
        }
    }

    @After
    public void tearDown() {
        lbm.unregisterReceiver(startedReceiver);
        lbm.unregisterReceiver(stoppedReceiver);
        lbm.unregisterReceiver(errorReceiver);
    }

    @Ignore
    @Test
    public void testStartStop() throws InterruptedException {
        Log.i(TAG, "testStartStop");

        final CountDownLatch startLatch = new CountDownLatch(1);
        BroadcastReceiver latchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startLatch.countDown();
            }
        };
        lbm.registerReceiver(latchReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STARTED));
        lbm.registerReceiver(stoppedReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STOPPED));
        lbm.registerReceiver(errorReceiver, new IntentFilter(LocalHTTPDManager.ACTION_ERROR));
        LocalHTTPDManager.start(context, false);
        assertTrue(startLatch.await(30, TimeUnit.SECONDS));
        assertTrue(Utils.isServerSocketInUse(PORT));
        assertTrue(Utils.canConnectToSocket(LOCALHOST, PORT));
        lbm.unregisterReceiver(latchReceiver);
        lbm.unregisterReceiver(stoppedReceiver);
        lbm.unregisterReceiver(errorReceiver);

        final CountDownLatch stopLatch = new CountDownLatch(1);
        latchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopLatch.countDown();
            }
        };
        lbm.registerReceiver(startedReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STARTED));
        lbm.registerReceiver(latchReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STOPPED));
        lbm.registerReceiver(errorReceiver, new IntentFilter(LocalHTTPDManager.ACTION_ERROR));
        LocalHTTPDManager.stop(context);
        assertTrue(stopLatch.await(30, TimeUnit.SECONDS));
        assertFalse(Utils.isServerSocketInUse(PORT));
        assertFalse(Utils.canConnectToSocket(LOCALHOST, PORT)); // if this is flaky, just remove it
        lbm.unregisterReceiver(latchReceiver);
    }

    @Test
    public void testError() throws InterruptedException, IOException {
        Log.i("LocalHTTPDManagerTest", "testError");
        ServerSocket blockerSocket = new ServerSocket(PORT);

        final CountDownLatch latch = new CountDownLatch(1);
        BroadcastReceiver latchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                latch.countDown();
            }
        };
        lbm.registerReceiver(startedReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STARTED));
        lbm.registerReceiver(stoppedReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STOPPED));
        lbm.registerReceiver(latchReceiver, new IntentFilter(LocalHTTPDManager.ACTION_ERROR));
        LocalHTTPDManager.start(context, false);
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertTrue(Utils.isServerSocketInUse(PORT));
        assertNotEquals(PORT, FDroidApp.port);
        assertFalse(Utils.isServerSocketInUse(FDroidApp.port));
        lbm.unregisterReceiver(latchReceiver);
        blockerSocket.close();
    }

    @Test
    public void testRestart() throws InterruptedException, IOException {
        Log.i("LocalHTTPDManagerTest", "testRestart");
        assertFalse(Utils.isServerSocketInUse(PORT));
        final CountDownLatch startLatch = new CountDownLatch(1);
        BroadcastReceiver latchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startLatch.countDown();
            }
        };
        lbm.registerReceiver(latchReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STARTED));
        lbm.registerReceiver(stoppedReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STOPPED));
        lbm.registerReceiver(errorReceiver, new IntentFilter(LocalHTTPDManager.ACTION_ERROR));
        LocalHTTPDManager.start(context, false);
        assertTrue(startLatch.await(30, TimeUnit.SECONDS));
        assertTrue(Utils.isServerSocketInUse(PORT));
        lbm.unregisterReceiver(latchReceiver);
        lbm.unregisterReceiver(stoppedReceiver);

        final CountDownLatch restartLatch = new CountDownLatch(1);
        latchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                restartLatch.countDown();
            }
        };
        lbm.registerReceiver(latchReceiver, new IntentFilter(LocalHTTPDManager.ACTION_STARTED));
        LocalHTTPDManager.restart(context, false);
        assertTrue(restartLatch.await(30, TimeUnit.SECONDS));
        lbm.unregisterReceiver(latchReceiver);
    }

    private final BroadcastReceiver startedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.i(TAG, "startedReceiver: " + message);
            fail();
        }
    };

    private final BroadcastReceiver stoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.i(TAG, "stoppedReceiver: " + message);
            fail();
        }
    };

    private final BroadcastReceiver errorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.i(TAG, "errorReceiver: " + message);
            fail();
        }
    };
}
