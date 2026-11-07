package vn.cleanyourtv.dashboard;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens to active system notifications so the top-right Notification Bell displays live counts.
 */
public final class TvNotificationListener extends NotificationListenerService {

    private static TvNotificationListener instance;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (instance == this) {
            instance = null;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    public static int getActiveCount() {
        try {
            if (instance != null) {
                StatusBarNotification[] list = instance.getActiveNotifications();
                if (list != null) {
                    int count = 0;
                    for (StatusBarNotification sbn : list) {
                        if (sbn != null && !sbn.isOngoing()) {
                            count++;
                        }
                    }
                    return count > 0 ? count : list.length;
                }
            }
        } catch (Exception ignored) {
        }
        return -1; // listener not bound yet
    }

    public static List<String> getNotificationSummaries() {
        List<String> items = new ArrayList<>();
        try {
            if (instance != null) {
                StatusBarNotification[] list = instance.getActiveNotifications();
                if (list != null) {
                    for (StatusBarNotification sbn : list) {
                        if (sbn == null || sbn.getNotification() == null) continue;
                        Bundle extras = sbn.getNotification().extras;
                        String title = extras != null ? String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE, "")) : "";
                        String text = extras != null ? String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT, "")) : "";
                        if (!title.isEmpty() || !text.isEmpty()) {
                            items.add((title.isEmpty() ? sbn.getPackageName() : title)
                                    + (text.isEmpty() ? "" : " — " + text));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return items;
    }
}
