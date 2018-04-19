package org.fdroid.fdroid.views.swap.device.camera;

import android.content.Context;

public abstract class CameraCharacteristicsChecker {
    public static CameraCharacteristicsChecker getInstance(final Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            return new CameraCharacteristicsMinApiLevel21(context);
        } else {
            return new CameraCharacteristicsMaxApiLevel20();
        }
    }

    public abstract boolean hasAutofocus();

    class FDroidDeviceException extends Exception {
        FDroidDeviceException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
