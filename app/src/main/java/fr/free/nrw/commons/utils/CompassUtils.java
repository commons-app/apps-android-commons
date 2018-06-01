package fr.free.nrw.commons.utils;

import android.hardware.SensorManager;
import android.view.Surface;

public class CompassUtils {

    /**
     * function calculates device orientation from values form magnetic field and accelerometer sensors
     * @return azimuth in radians or null if calculation fails
     */
    public static Float getDeviceOrientation(int displayRotation, float[] accelerometerValues, float[] magneticValues) {
        float rotationMatrix[] = new float[9];
        float remappedRotation[] = new float[9];
        float orientation[] = new float[3];
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticValues);

        int axisX = SensorManager.AXIS_X;
        int axisY = SensorManager.AXIS_Y;

        switch (displayRotation) {
            case Surface.ROTATION_90:
                axisX = SensorManager.AXIS_Y;
                axisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                axisX = SensorManager.AXIS_MINUS_X;
                axisY = SensorManager.AXIS_MINUS_Y;
                break;
            case Surface.ROTATION_270:
                axisX = SensorManager.AXIS_MINUS_Y;
                axisY = SensorManager.AXIS_X;
                break;
            default:
                break;
        }

        boolean success = SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedRotation);
        if (success) {
            SensorManager.getOrientation(remappedRotation, orientation);
            return orientation[0];
        } else {
            return null;
        }
    }
}
