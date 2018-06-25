package org.fdroid.fdroid.views.swap.device.camera;

import android.hardware.Camera;
import android.util.Log;

import java.util.List;

public class CameraCharacteristicsMaxApiLevel20 extends CameraCharacteristicsChecker {

    private static final String TAG = "CameraCharMaxApiLevel20";

    protected CameraCharacteristicsMaxApiLevel20() {
    }

    @Override
    public boolean hasAutofocus() {
        boolean hasAutofocus = false;
        try {
            hasAutofocus = hasDeviceAutofocusCapability();
        } catch (FDroidDeviceException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return hasAutofocus;
    }

    private boolean hasDeviceAutofocusCapability() throws FDroidDeviceException {
        try {
            final int numberOfCameras = Camera.getNumberOfCameras();
            if (numberOfCameras == 0) {
                Log.i(TAG, "No camera on device");
                return false;
            }

            boolean hasAutofocus = false;
            for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
                Camera camera = Camera.open(cameraId);
                Camera.Parameters parameters = camera.getParameters();
                List<String> availableAFModes = parameters.getSupportedFocusModes();
                hasAutofocus = availableAFModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            return hasAutofocus;
        } catch (Exception e) {
            String msg = "Exception accessing device camera";
            Log.e(TAG, msg, e);
            throw new FDroidDeviceException(msg, e);
        }
    }

}