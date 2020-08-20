package com.openfocals.services.notifications;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.openfocals.services.notifications.NotificationPostedEvent;
import com.openfocals.services.notifications.NotificationRemovedEvent;

import org.greenrobot.eventbus.EventBus;


public class NotificationService extends NotificationListenerService {

    private static final String TAG = "FOCALS_NOTIFICATION";

    private static NotificationService instance;

    public static NotificationService getInstance() { return instance; }

    Context context_;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        Log.i(TAG, "Starting notification service");
        context_ = getApplicationContext();

        //System.out.println("SERVICE STARTED");
    }



    @Override
    public void onNotificationPosted(StatusBarNotification sb) {
        //String pack = sb.getPackageName();
        //Notification n = sb.getNotification();
        //System.out.println("Got notification: " + pack + " " + n.extras.getString("android.title") + " / " + n.extras.getString("android.text"));
        EventBus.getDefault().post(new NotificationPostedEvent(sb));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sb) {
        EventBus.getDefault().post(new NotificationRemovedEvent(sb));
        //String pack = sb.getPackageName();
        //String ticker = sb.getNotification().tickerText.toString();
        //Bundle extras = sb.getNotification().extras;
        //String title = extras.getString("android.title");
        //String text = extras.getCharSequence("android.text").toString();

        //Log.i(TAG, "Service got removal of notification: pack=" + pack + " ticker=" + ticker + " title=" + title + " text=" + text);
    }
}
