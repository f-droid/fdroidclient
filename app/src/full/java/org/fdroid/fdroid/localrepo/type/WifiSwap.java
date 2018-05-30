package org.fdroid.fdroid.localrepo.type;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.LocalHTTPD;
import org.fdroid.fdroid.net.WifiStateChangeService;
import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.net.BindException;
import java.util.Random;

@SuppressWarnings("LineLength")
public class WifiSwap extends SwapType {

    private static final String TAG = "WifiSwap";

    private Handler webServerThreadHandler;
    private LocalHTTPD localHttpd;
    private final BonjourBroadcast bonjourBroadcast;
    private final WifiManager wifiManager;

    public WifiSwap(Context context, WifiManager wifiManager) {
        super(context);
        bonjourBroadcast = new BonjourBroadcast(context);
        this.wifiManager = wifiManager;
    }

    protected String getBroadcastAction() {
        return SwapService.WIFI_STATE_CHANGE;
    }

    public BonjourBroadcast getBonjour() {
        return bonjourBroadcast;
    }

    @Override
    public void start() {
        wifiManager.setWifiEnabled(true);

        Utils.debugLog(TAG, "Preparing swap webserver.");
        sendBroadcast(SwapService.EXTRA_STARTING);

        if (FDroidApp.ipAddressString == null) {
            Log.e(TAG, "Not starting swap webserver, because we don't seem to be connected to a network.");
            setConnected(false);
        }

        Single.zip(
                Single.create(getWebServerTask()),
                Single.create(getBonjourTask()),
                new Func2<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean webServerTask, Boolean bonjourServiceTask) {
                        return bonjourServiceTask && webServerTask;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean success) {
                            setConnected(success);
                        }
                    },
                    new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            setConnected(false);
                        }
                    });
    }

    /**
     * A task which starts the {@link WifiSwap#bonjourBroadcast} and then emits a `true` value at
     * the end.
     */
    private Single.OnSubscribe<Boolean> getBonjourTask() {
        return new Single.OnSubscribe<Boolean>() {
            @Override
            public void call(SingleSubscriber<? super Boolean> singleSubscriber) {
                bonjourBroadcast.start();

                // TODO: Be more intelligent about failures here so that we can invoke
                // singleSubscriber.onError() in the appropriate circumstances.
                singleSubscriber.onSuccess(true);
            }
        };
    }

    /**
     * Constructs a new {@link Thread} for the webserver to run on. If successful, it will also
     * populate the webServerThreadHandler property and bind it to that particular thread. This
     * allows messages to be sent to the webserver thread by posting messages to that handler.
     */
    private Single.OnSubscribe<Boolean> getWebServerTask() {
        return new Single.OnSubscribe<Boolean>() {
            @Override
            public void call(final SingleSubscriber<? super Boolean> singleSubscriber) {
                new Thread(new Runnable() {
                    // Tell Eclipse this is not a leak because of Looper use.
                    @SuppressLint("HandlerLeak")
                    @Override
                    public void run() {
                        localHttpd = new LocalHTTPD(
                            context,
                            FDroidApp.ipAddressString,
                            FDroidApp.port,
                            context.getFilesDir(),
                            Preferences.get().isLocalRepoHttpsEnabled());

                        Looper.prepare(); // must be run before creating a Handler
                        webServerThreadHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                Log.i(TAG, "we've been asked to stop the webserver: " + msg.obj);
                                localHttpd.stop();
                                Looper looper = Looper.myLooper();
                                if (looper == null) {
                                    Log.e(TAG, "Looper.myLooper() was null for sum reason while shutting down the swap webserver.");
                                } else {
                                    looper.quit();
                                }
                            }
                        };
                        try {
                            Utils.debugLog(TAG, "Starting swap webserver...");
                            localHttpd.start();
                            Utils.debugLog(TAG, "Swap webserver started.");
                            singleSubscriber.onSuccess(true);
                        } catch (BindException e) {
                            int prev = FDroidApp.port;
                            FDroidApp.port = FDroidApp.port + new Random().nextInt(1111);
                            WifiStateChangeService.start(context, null);
                            singleSubscriber.onError(new Exception("port " + prev + " occupied, trying on " + FDroidApp.port + "!"));
                        } catch (IOException e) {
                            Log.e(TAG, "Could not start local repo HTTP server", e);
                            singleSubscriber.onError(e);
                        }
                        Looper.loop(); // start the message receiving loop
                    }
                }).start();
            }
        };
    }

    @Override
    public void stop() {
        sendBroadcast(SwapService.EXTRA_STOPPING);
        if (webServerThreadHandler == null) {
            Log.i(TAG, "null handler in stopWebServer");
        } else {
            Utils.debugLog(TAG, "Sending message to swap webserver to stop it.");
            Message msg = webServerThreadHandler.obtainMessage();
            msg.obj = webServerThreadHandler.getLooper().getThread().getName() + " says stop";
            webServerThreadHandler.sendMessage(msg);
        }

        // Stop the Bonjour stuff after asking the webserver to stop. This is not required in this
        // order, but it helps. In practice, the Bonjour stuff takes a second or two to stop. This
        // should give enough time for the message we posted above to reach the web server thread
        // and for the webserver to thus be stopped.
        bonjourBroadcast.stop();
        setConnected(false);
    }

}
