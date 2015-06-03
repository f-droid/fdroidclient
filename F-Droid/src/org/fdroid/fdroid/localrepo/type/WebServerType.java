package org.fdroid.fdroid.localrepo.type;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.net.LocalHTTPD;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.io.IOException;
import java.net.BindException;
import java.util.Random;

public class WebServerType implements SwapType {

    private static final String TAG = "WebServerType";

    private Handler webServerThreadHandler = null;
    private LocalHTTPD localHttpd;
    private final Context context;

    public WebServerType(Context context) {
        this.context = context;
    }

    @Override
    public void start() {

        Runnable webServer = new Runnable() {
            // Tell Eclipse this is not a leak because of Looper use.
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                localHttpd = new LocalHTTPD(
                        context,
                        context.getFilesDir(),
                        Preferences.get().isLocalRepoHttpsEnabled());

                Looper.prepare(); // must be run before creating a Handler
                webServerThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Log.i(TAG, "we've been asked to stop the webserver: " + msg.obj);
                        localHttpd.stop();
                    }
                };
                try {
                    localHttpd.start();
                } catch (BindException e) {
                    int prev = FDroidApp.port;
                    FDroidApp.port = FDroidApp.port + new Random().nextInt(1111);
                    Log.w(TAG, "port " + prev + " occupied, trying on " + FDroidApp.port + "!");
                    context.startService(new Intent(context, WifiStateChangeService.class));
                } catch (IOException e) {
                    Log.e(TAG, "Could not start local repo HTTP server: " + e);
                    Log.e(TAG, Log.getStackTraceString(e));
                }
                Looper.loop(); // start the message receiving loop
            }
        };
        new Thread(webServer).start();
    }

    @Override
    public void stop() {
        if (webServerThreadHandler == null) {
            Log.i(TAG, "null handler in stopWebServer");
            return;
        }
        Log.d(TAG, "Sending message to swap webserver to stop it.");
        Message msg = webServerThreadHandler.obtainMessage();
        msg.obj = webServerThreadHandler.getLooper().getThread().getName() + " says stop";
        webServerThreadHandler.sendMessage(msg);
    }

    @Override
    public void restart() {

    }

}
