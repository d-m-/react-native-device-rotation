package org.muxe.devicerotation;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class DeviceRotation implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mRotationVector;
    private boolean isRegistered = false;

    private ReactContext mReactContext;


    public DeviceRotation(ReactApplicationContext reactContext) {
        mSensorManager = (SensorManager)reactContext.getSystemService(reactContext.SENSOR_SERVICE);
        mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mReactContext = reactContext;
    }

    public boolean start() {
        if (mRotationVector!= null && isRegistered == false) {
            mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_UI);
            isRegistered = true;
            return true;
        }
        return false;
    }

    public void stop() {
        if (isRegistered == true) {
            mSensorManager.unregisterListener(this);
            isRegistered = false;
        }
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        try {
            mReactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } catch (RuntimeException e) {
            Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke JS before CatalystInstance has been set!");
        }
    }

    float[] mRotation;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        WritableMap map = Arguments.createMap();

        if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            mRotation = sensorEvent.values;
            float[] rotationMatrix = new float[16];
            float[] rotationMatrixTransformed = new float[16];
            mSensorManager.getRotationMatrixFromVector(rotationMatrix, mRotation);
            SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    rotationMatrixTransformed
                    );
            float[] orientation = new float[3];
            mSensorManager.getOrientation(rotationMatrixTransformed, orientation);

            float heading = (float)((Math.toDegrees(orientation[0])) % 360.0f);
            float pitch = (float)((Math.toDegrees(orientation[1])) % 360.0f);
            float roll = (float)((Math.toDegrees(orientation[2])) % 360.0f);
            
            if (heading < 0) {
                heading = 360 - (0 - heading);
            }

            map.putDouble("azimuth", heading);
            map.putDouble("pitch", pitch);
            map.putDouble("roll", roll);
            map.putDouble("accuracy", Math.toDegrees(sensorEvent.values[4]));

            sendEvent("DeviceRotation", map);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
