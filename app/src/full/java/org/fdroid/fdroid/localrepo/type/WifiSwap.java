package org.fdroid.fdroid.localrepo.type;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.LocalHTTPDManager;
import org.fdroid.fdroid.localrepo.SwapService;
import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

@SuppressWarnings("LineLength")
public class WifiSwap extends SwapType {

    private static final String TAG = "WifiSwap";

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
        sendBroadcast(SwapService.EXTRA_STARTING);
        wifiManager.setWifiEnabled(true);

        LocalHTTPDManager.start(context);

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
            public void call(SingleSubscriber<? super Boolean> singleSubscriber) {
                singleSubscriber.onSuccess(true);
            }
        };
    }

    @Override
    public void stop() {
        sendBroadcast(SwapService.EXTRA_STOPPING); // This needs to be per-SwapType
        Utils.debugLog(TAG, "Sending message to swap webserver to stop it.");
        LocalHTTPDManager.stop(context);

        // Stop the Bonjour stuff after asking the webserver to stop. This is not required in this
        // order, but it helps. In practice, the Bonjour stuff takes a second or two to stop. This
        // should give enough time for the message we posted above to reach the web server thread
        // and for the webserver to thus be stopped.
        bonjourBroadcast.stop();
        setConnected(false);
    }

}
