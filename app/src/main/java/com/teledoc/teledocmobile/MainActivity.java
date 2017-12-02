package com.teledoc.teledocmobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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
    public static final UUID TELEDOC_UUID = UUID.fromString("3ef00e98-a42e-4d71-acc7-c9d5bde24c90");


    public Button btnMessage;
    private MessageService messageService = MessageService.getInstance(TELEDOC_UUID);

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

}
