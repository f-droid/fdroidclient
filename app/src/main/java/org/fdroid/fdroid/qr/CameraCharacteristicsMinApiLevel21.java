package org.fdroid.fdroid.qr;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class CameraCharacteristicsMinApiLevel21 extends CameraCharacteristicsChecker {

    private static final String TAG = "CameraCharMinApiLevel21";
    private final CameraManager cameraManager;

    CameraCharacteristicsMinApiLevel21(final Context context) {
        this.cameraManager = ContextCompat.getSystemService(context, CameraManager.class);
    }

    @Override
    public boolean hasAutofocus() {
        boolean hasAutofocus = false;
        try {
            hasAutofocus = hasDeviceAutofocus();
        } catch (FDroidDeviceException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return hasAutofocus;
    }

    private boolean hasDeviceAutofocus() throws FDroidDeviceException {
        try {
            boolean deviceHasAutofocus = false;
            final String[] cameraIdList = getCameraIdList();

            for (final String cameraId : cameraIdList) {
                if (isLensFacingBack(cameraId)) {
                    deviceHasAutofocus = testAutofocusModeForCamera(cameraId);
                    break;
                }
            }
            return deviceHasAutofocus;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            throw new FDroidDeviceException("Exception accessing the camera list", e);
        }

    }

    @NonNull
    private String[] getCameraIdList() throws FDroidDeviceException {
        try {
            return cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new FDroidDeviceException("Exception accessing the camera list", e);
        }
    }

    private boolean isLensFacingBack(final String cameraId) throws FDroidDeviceException {
        final Integer lensFacing = getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING);

        return lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK;
    }

    @NonNull
    private CameraCharacteristics getCameraCharacteristics(final String cameraId) throws FDroidDeviceException {
        try {
            return cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new FDroidDeviceException("Exception accessing the camera id = " + cameraId, e);
        }

    }

    private boolean testAutofocusModeForCamera(final String cameraId) throws FDroidDeviceException {
        try {
            boolean hasAutofocusMode = false;
            final int[] autoFocusModes = getAvailableAFModes(cameraId);
            if (autoFocusModes != null) {
                hasAutofocusMode = testAvailableMode(autoFocusModes);
            }

            return hasAutofocusMode;
        } catch (FDroidDeviceException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new FDroidDeviceException("Exception accessing the camera id = " + cameraId, e);
        }
    }

    private int[] getAvailableAFModes(final String cameraId) throws FDroidDeviceException {
        return getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
    }

    private boolean testAvailableMode(final int[] autoFocusModes) {
        boolean hasAutofocusMode = false;
        for (final int mode : autoFocusModes) {
            boolean afMode = isAutofocus(mode);
            hasAutofocusMode |= afMode;
        }
        return hasAutofocusMode;
    }

    private boolean isAutofocus(final int mode) {
        return mode != android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF;
    }

}
