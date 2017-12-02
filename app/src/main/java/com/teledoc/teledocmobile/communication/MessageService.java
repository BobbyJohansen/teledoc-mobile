package com.teledoc.teledocmobile.communication;

import com.google.gson.Gson;
import com.teledoc.common.communication.TeleDocMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.hgross.blaubot.android.BlaubotAndroidFactory;
import eu.hgross.blaubot.core.Blaubot;
import eu.hgross.blaubot.core.IBlaubotDevice;
import eu.hgross.blaubot.core.ILifecycleListener;
import eu.hgross.blaubot.messaging.BlaubotMessage;
import eu.hgross.blaubot.messaging.IBlaubotChannel;
import eu.hgross.blaubot.messaging.IBlaubotMessageListener;

public class MessageService {

    private Map<Integer, IBlaubotChannel> channelMap;

    //create singleton
    private static MessageService service;

    //create p2p network
    public Blaubot mBlaubot;

    public static MessageService getInstance(UUID messengerId){
        if (service == null){
            service = new MessageService(messengerId);
        }
        return service;
    }

    protected MessageService(UUID messengerId) {
        channelMap = new HashMap<>();
        initializeNetwork(messengerId);
    }

    public void initializeNetwork(UUID messengerId) {
        mBlaubot = BlaubotAndroidFactory.createEthernetBlaubot(messengerId);
        Thread t = new Thread(() -> {
            while (true) {
                Blaubot bb = mBlaubot;
                try {
                    if (bb != null) {
                        List<IBlaubotDevice> devices = bb.getConnectionManager().getConnectedDevices();
                        devices.add(0, bb.getOwnDevice());
                        int i = 0;
                        System.out.println("Devices:");
                        for (IBlaubotDevice d : devices) {
                            System.out.println("dev " + i + ": " + d);
                            i++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
    }

    public void stopNetworking(){
        mBlaubot.stopBlaubot();
    }

    public void startNetworking(int channelId, IBlaubotMessageListener listener){
        mBlaubot.startBlaubot();

        // create the channel
        final IBlaubotChannel channel = mBlaubot.createChannel((short)channelId);
        channelMap.put(channelId, channel);

        channel.publish("Hello world!".getBytes());

        //Start listening to the channel we are talking over
        System.out.println("subscribing");
        channel.subscribe(listener);

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

    public void send(int channelId, TeleDocMessage message) {
        IBlaubotChannel channel = null;
        try {
            channel = channelMap.get(channelId);
            channel.publish(("Android msg " + System.currentTimeMillis()).getBytes());
            channel.publish(serialize(message).getBytes());
        } catch (Exception e) {
            System.out.println("Could not find channel to send message to on channel: " + channelId);
        }
    }

    private String serialize(Object message) {
        Gson gson = new Gson();
        return gson.toJson(message);
    }


}
