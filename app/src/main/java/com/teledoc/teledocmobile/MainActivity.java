package com.teledoc.teledocmobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import java.util.List;
import java.util.UUID;

import eu.hgross.blaubot.android.BlaubotAndroidFactory;
import eu.hgross.blaubot.core.Blaubot;
import eu.hgross.blaubot.core.BlaubotFactory;
import eu.hgross.blaubot.core.IBlaubotDevice;
import eu.hgross.blaubot.core.ILifecycleListener;
import eu.hgross.blaubot.messaging.BlaubotMessage;
import eu.hgross.blaubot.messaging.IBlaubotChannel;
import eu.hgross.blaubot.messaging.IBlaubotMessageListener;

public class MainActivity extends AppCompatActivity {
    public static final UUID TELEDOC_UUID = UUID.fromString("3ef00e98-a42e-4d71-acc7-c9d5bde24c90");

    public Blaubot mBlaubot;
    public Button btnMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnMessage = (Button)findViewById(R.id.btnMessage);

        mBlaubot = BlaubotAndroidFactory.createEthernetBlaubot(TELEDOC_UUID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startNetworking();
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
        mBlaubot.stopBlaubot();
        super.onStop();
    }

    public void startNetworking() {
        mBlaubot.startBlaubot();

        // create the channel
        final IBlaubotChannel channel = mBlaubot.createChannel((short)1);

        btnMessage.setOnClickListener((evt) -> {
            channel.publish(("Android msg " + System.currentTimeMillis()).getBytes());
        });

        channel.publish("Hello world!".getBytes());

        System.out.println("subscribing");
        channel.subscribe(new IBlaubotMessageListener() {
            @Override
            public void onMessage(BlaubotMessage message) {
                String msg = new String(message.getPayload());
                System.out.println("msg received: " + msg);
            }
        });

        Thread t = new Thread(() -> {
            while (true) {
                List<IBlaubotDevice> devices = mBlaubot.getConnectionManager().getConnectedDevices();
                devices.add(0, mBlaubot.getOwnDevice());
                int i = 0;
                System.out.println("Devices:");
                for (IBlaubotDevice d : devices) {
                    System.out.println("dev " + i + ": " + d);
                    i++;
                }
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return;
                }
            }
        });
        t.setDaemon(true);
        t.start();

        mBlaubot.addLifecycleListener(new ILifecycleListener() {
            @Override
            public void onDisconnected() {
                System.out.println("onDisconnected");
            }

            @Override
            public void onDeviceLeft(IBlaubotDevice blaubotDevice) {
                System.out.println("onDeviceLeft " + blaubotDevice);
            }

            @Override
            public void onDeviceJoined(IBlaubotDevice blaubotDevice) {
                System.out.println("onDeviceJoined " + blaubotDevice);
            }

            @Override
            public void onConnected() {
                System.out.println("onConnected");
            }

            @Override
            public void onPrinceDeviceChanged(IBlaubotDevice oldPrince, IBlaubotDevice newPrince) {
                System.out.println("onPrinceDeviceChanged " + oldPrince + " -> " + newPrince);
            }

            @Override
            public void onKingDeviceChanged(IBlaubotDevice oldKing, IBlaubotDevice newKing) {
                System.out.println("onKingDeviceChanged " + oldKing + " -> " + newKing);
            }
        });
    }
}
