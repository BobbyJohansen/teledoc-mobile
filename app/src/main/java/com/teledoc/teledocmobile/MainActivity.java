package com.teledoc.teledocmobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.teledoc.common.communication.DataType;
import com.teledoc.common.communication.TeleDocMessage;
import com.teledoc.teledocmobile.communication.MessageService;

import org.apache.commons.collections4.ListUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.hgross.blaubot.messaging.BlaubotMessage;
import eu.hgross.blaubot.messaging.IBlaubotMessageListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final Gson gson = new Gson();

    public static final UUID TELEDOC_UUID = UUID.fromString("3ef00e98-a42e-4d71-acc7-c9d5bde24c90");
    public static final UUID INSTANCE_UUID = UUID.randomUUID();

    public Button btnMessage;
    private MessageService messageService = MessageService.getInstance(TELEDOC_UUID); //TODO Save locally?


    //Our message Listener
    private class MessageListener implements IBlaubotMessageListener{
        @Override
        public void onMessage(BlaubotMessage message) {
            String msg = new String(message.getPayload());
            System.out.println("msg received: " + msg);
            TeleDocMessage teleDocMessage = deserializeMessage(msg);
        }

        private TeleDocMessage deserializeMessage(String message) {
            Gson gson = new Gson();
             TeleDocMessage teleDocMessage = gson.fromJson(message, TeleDocMessage.class);
             return teleDocMessage;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnMessage = (Button)findViewById(R.id.btnMessage);

        btnMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    System.out.println("Sending Message via Message Service");
                    TeleDocMessage message = new TeleDocMessage();
                    Double[] bogusData = {1.0, 2.0, 3.0, 4.0};
                    message.setData(Arrays.asList(bogusData));
                    message.setDataType(DataType.HEART_RATE);
                    messageService.send(1, message);
                    System.out.println("Finished passing message over the wire");
                } catch (Exception e) {
                    System.out.println("Failed to send message over the wire womp womp");
                }
            }
        });

        listenToWatch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        messageService.startNetworking(1, new MessageListener());
//        mBlaubot.registerReceivers(this);
//        mBlaubot.setContext(this);
//        mBlaubot.onResume(this);
    }

    @Override
    protected void onPause() {
        //mBlaubot.unregisterReceivers(this);
        //mBlaubot.onPause(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        messageService.stopNetworking();
        super.onStop();
    }

    public void listenToWatch() {
        //TODO I don't yet start/stop the listeners with the activity lifecycle, which would be bad-ish in production.
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
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

        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                String msgStr = new String(messageEvent.getData());
                Log.d(TAG, "Received msg: " + msgStr);
                if ("/sensor/data".equals(messageEvent.getPath())) {
                    TeleDocMessage msg = gson.fromJson(msgStr, TeleDocMessage.class);
                    msg.setPerson(INSTANCE_UUID);
                    messageService.send(1, msg);
                }
            }
        }).setResultCallback((r) -> {
            if (!r.isSuccess()) {
                Log.e(TAG, "Unable to register watch listener: " + r.getStatusMessage());
            }
        });
    }
}
