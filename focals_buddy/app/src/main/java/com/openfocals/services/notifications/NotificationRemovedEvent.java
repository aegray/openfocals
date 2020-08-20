package com.openfocals.services.notifications;

import android.service.notification.StatusBarNotification;

public class NotificationRemovedEvent {
    public StatusBarNotification notification;

    public NotificationRemovedEvent(StatusBarNotification n) {
        notification = n;
    }

}
