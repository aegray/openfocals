package com.openfocals.services.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;


import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.messages.Notification;
import com.openfocals.focals.messages.NotificationAction;
import com.openfocals.focals.messages.NotificationResponse;
import com.openfocals.focals.messages.RemoteInput;
import com.openfocals.focals.messages.RemoteInputResult;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class NotificationSender {
    private static final String TAG = "FOCALS_NOTIF_SENDER";
    private static final SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.US);

    Context context_;
    Device device_;



    private class NotificationInfo {
        String id;
        String key;
        List<android.app.Notification.Action> actions;
    }

    Map<String, NotificationInfo> notifications_ = new HashMap<>();




    public NotificationSender(Context c, Device d) {
        device_ = d;
        context_ = c;
        ISO8601_FORMAT.setTimeZone(TimeZone.getDefault());

        EventBus.getDefault().register(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNotificationPosted(NotificationPostedEvent e) {
        if (device_.isConnected()) {
            StatusBarNotification sb = e.notification;

            if (sb.isClearable() && !sb.isOngoing()) {
                // sb.isGroup()
                // sb.isOngoing()

                // sb.getTag() -> string
                // sb.getGroupKey() -> string
                // sb.getId() -> int (original id)




                String pack = sb.getPackageName();
                CharSequence ticker_raw = sb.getNotification().tickerText;
                Bundle extras = sb.getNotification().extras;
                String title = extras.getString(android.app.Notification.EXTRA_TITLE);
                String text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString();

                NotificationInfo ni = new NotificationInfo();

                ni.actions = null;
                if (sb.getNotification().actions != null) {
                    ni.actions = Arrays.asList(sb.getNotification().actions);
                }
                ni.key = sb.getKey();

                // create unique id

                String use_id = pack + ":" + e.notification.getKey();
                ni.id = use_id;

                android.app.Notification.WearableExtender wear_ext = new android.app.Notification.WearableExtender((sb.getNotification()));
                List<android.app.Notification.Action> wear_actions = wear_ext.getActions();
                if (!wear_actions.isEmpty()) {
                    ni.actions = wear_actions;
                }

                List<NotificationAction> focals_actions = null;
                if (ni.actions != null) {
                    focals_actions = new ArrayList<>();

                    for (int i = 0; i < ni.actions.size(); ++i) {
                        NotificationAction.Builder na = NotificationAction.newBuilder();
                        na.setId(Integer.toString(i));
                        //na.setActionIconFileId("");
                        String atitle = ni.actions.get(i).title.toString();
                        if (atitle == null) title = "";
                        na.setTitle(atitle);
                        android.app.RemoteInput[] ris = ni.actions.get(i).getRemoteInputs();
                        if ((ris != null) && (ris.length > 0)) {
                            List<RemoteInput> remotes = new ArrayList<>();
                            for (int j = 0; j < ris.length; ++j) {
                                RemoteInput.Builder rib = RemoteInput.newBuilder();
                                String reskey = ris[j].getResultKey();
                                String reslabel = ris[j].getLabel().toString();
                                rib.setKey(reskey == null ? "" : reskey);
                                rib.setLabel(reslabel == null ? "" : reslabel);
                                rib.setAllowsFreeForm(ris[j].getAllowFreeFormInput());

                                CharSequence[] choices = ris[j].getChoices();
                                if ((choices != null) && (choices.length > 0)) {
                                    List<String> schoices = new ArrayList<>();
                                    for (int m = 0; m < choices.length; ++m) {
                                        String schoice = choices[m].toString();
                                        if (schoice != null)
                                            schoices.add(choices[m].toString());
                                    }
                                    rib.addAllChoices(schoices);
                                }
                                remotes.add(rib.build());
                            }
                            if (!remotes.isEmpty())
                                na.addAllInputs(remotes);
                        }
                        focals_actions.add(na.build());
                    }
                }

                notifications_.put(use_id, ni);


                Log.i(TAG, "Got notification: key=" + e.notification.getKey() + " title=" + title + " text=" + text + " pack=" + pack);
                Notification.Builder nb = Notification.newBuilder();
                nb = nb.setId(use_id)
                        .setTitle(title)
                        .setText(text)
                        //.setIconId("icon")
                        .setTime(ISO8601_FORMAT.format(new Date(sb.getPostTime())));

                if (focals_actions != null) {
                    nb = nb.addAllActions(focals_actions);
                }

                Notification n = nb.build();

                device_.sendNotification(n);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNotificationRemoved(NotificationRemovedEvent e) {
        Log.i(TAG, "Got notification removed: " + e.notification);

        String use_id = e.notification.getPackageName() + ":" + e.notification.getKey();
        if (notifications_.containsKey(use_id)) {
            notifications_.remove(use_id);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsMessage(FocalsBluetoothMessageEvent e) {
        if (e.message.hasNotificationDismiss()) {
            NotificationInfo ni = notifications_.get(e.message.getNotificationDismiss().getId());
            if (ni != null) {
                NotificationService.getInstance().cancelNotification(ni.key);
                notifications_.remove(e.message.getNotificationDismiss().getId());
            }
        } else if (e.message.hasNotificationResponse()) {
            NotificationResponse r = e.message.getNotificationResponse();
            if (r.hasPerformAction()) {
                NotificationInfo ni = notifications_.get(r.getPerformAction().getNotificationId());
                if (ni != null) {
                    int action_id = Integer.valueOf(r.getPerformAction().getActionId());

                    if ((action_id >= 0) && (action_id < ni.actions.size())) {
                        android.app.Notification.Action action = ni.actions.get(action_id);
                        PendingIntent pi = action.actionIntent;
                        if (pi != null) {
                            Intent intent = new Intent();
                            if (action.getRemoteInputs() != null) {
                                Bundle b = new Bundle();
                                for (RemoteInputResult rir : r.getPerformAction().getInputsList()) {
                                    Log.i(TAG, "ADDING remote result: key=" + rir.getKey()+ " : " + rir.getValue());
                                    b.putString(rir.getKey(), rir.getValue());
                                }
                                android.app.RemoteInput.addResultsToIntent(action.getRemoteInputs(), intent, b);
                            }
                            try {
                                Log.i(TAG, "ADDING sENDING INTENT");
                                pi.send(this.context_, 0, intent);
                            } catch (PendingIntent.CanceledException ex) {
                                Log.e(TAG, "ADDING action was cancelled");
                            }
                        }
                    }
                }
            }
        }
    }

    private String getAppName(String pkg) {
        try {
            PackageManager pm = context_.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
