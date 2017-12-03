package com.teledoc.teledocmobile;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.teledoc.common.communication.DataType;
import com.teledoc.common.communication.TeleDocMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class MainActivity extends WearableActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";

    public static final String MESSAGE_PATH = "/sensor/data";

    private static final Gson gson = new Gson();

    private List<Node> mConnectedNodes;
    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private Sensor mSensorHeartRate;
    private Sensor mSensorPpg;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();

        if (checkPermission(Manifest.permission.BODY_SENSORS)) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//            List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            mSensorHeartRate = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            mSensorPpg = mSensorManager.getSensorList(Sensor.TYPE_ALL).stream().filter(s -> "HR PPG SENSOR".equalsIgnoreCase(s.getName())).findAny().get();
        } else {
            this.finish();
        }

        setupComms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorPpg, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    private volatile long timeStart = -1;
    private volatile int count = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (timeStart < 0) {
            timeStart = System.currentTimeMillis();
        }
        count++;
        long time = System.currentTimeMillis();
        if (count % 100 == 0) {
            Log.d(TAG, "count " + count + " in " + (time - timeStart));
        }
//        if (1==1) {
//            return;
//        }
//
//        Log.d(TAG, "read sensor");
        TeleDocMessage tdm = new TeleDocMessage();
        tdm.setDataType(DataType.PPG);
        ArrayList<Double> data = new ArrayList<>(event.values.length);
        for (float f : event.values) {
            data.add((double)f);
        }
        tdm.setData(data);
        //TODO Make more asynchronous?
        sendData(gson.toJson(tdm).getBytes());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setupComms() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
    }

    public void sendData(byte[] data) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback((r) -> {
            mConnectedNodes = r.getNodes();
        });
        //TODO Note that when the connection status changes, an extra message or two may be lost
        List<Node> nodes = mConnectedNodes;
        if (nodes != null) {
            for (Node n : nodes) {
                Wearable.MessageApi.sendMessage(mGoogleApiClient, n.getId(), MESSAGE_PATH, data).setResultCallback(
                        new ResultCallback() {
                            @Override
                            public void onResult(Result sendMessageResult) {
                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    // Failed to send message
                                    Log.e(TAG, "Failed to send message - " + sendMessageResult.getStatus().getStatusMessage());
                                } else {
                                    //Log.d(TAG, "Successfully sent message");
                                }
                            }
                        }
                );
            }
        }
    }

    public boolean checkPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
            return false;
        } else {
            return true;
        }
    }
}
