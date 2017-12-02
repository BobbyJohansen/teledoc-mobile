package com.teledoc.teledocmobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.google.gson.Gson;
import com.teledoc.common.communication.TeleDocMessage;
import com.teledoc.teledocmobile.communication.MessageService;

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
            Gson gson = new Gson;
             TeleDocMessage teleDocMessage = gson.fromJson(message, TeleDocMessage.class);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnMessage = (Button)findViewById(R.id.btnMessage);

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
