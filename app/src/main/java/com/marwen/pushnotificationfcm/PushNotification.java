package com.marwen.pushnotificationfcm;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class PushNotification extends FirebaseMessagingService {
    private static final String TAG = "0";
    private LocalBroadcastManager broadcaster;
    Handler handler;


    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    public PushNotification() {
        handler = new Handler();
    }

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Intent intent = new Intent("MyData");
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message data payload: " + remoteMessage.getNotification());
        Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        intent.putExtra("msg", remoteMessage.getNotification().getBody());
        broadcaster.sendBroadcast(intent);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
    }


}
