package com.openfocals.services.notifications;

import android.service.notification.StatusBarNotification;

public class NotificationPostedEvent {
    public StatusBarNotification notification;

    public NotificationPostedEvent(StatusBarNotification n) {
        notification = n;
    }
}
