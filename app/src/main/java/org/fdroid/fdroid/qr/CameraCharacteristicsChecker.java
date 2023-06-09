package org.fdroid.fdroid.qr;

import android.content.Context;

public abstract class CameraCharacteristicsChecker {
    public static CameraCharacteristicsChecker getInstance(final Context context) {
        return new CameraCharacteristicsMinApiLevel21(context);
    }

    public abstract boolean hasAutofocus();

    static class FDroidDeviceException extends Exception {
        FDroidDeviceException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
