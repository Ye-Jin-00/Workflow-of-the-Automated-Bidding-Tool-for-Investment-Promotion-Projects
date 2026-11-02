package com.sincit.bid.persistence;

import com.sincit.bid.domain.Notification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class NotificationRepository {

    private final JsonDataStore store;

    public NotificationRepository(JsonDataStore store) {
        this.store = store;
    }

    public synchronized void append(Notification notification) {
        store.database().getNotifications().add(notification);
        store.save();
    }

    public synchronized void saveAll(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        store.database().getNotifications().addAll(notifications);
        store.save();
    }

    public synchronized List<Notification> findByUserId(String userId) {
        List<Notification> result = new ArrayList<Notification>();
        for (Notification notification : store.database().getNotifications()) {
            if (userId.equals(notification.getUserId())) {
                result.add(notification);
            }
        }
        return result;
    }

    public synchronized boolean existsDedupe(String userId, String dedupeKey) {
        if (dedupeKey == null) {
            return false;
        }
        for (Notification notification : store.database().getNotifications()) {
            if (userId.equals(notification.getUserId()) && dedupeKey.equals(notification.getDedupeKey())) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean markRead(String id, String userId) {
        for (Notification notification : store.database().getNotifications()) {
            if (notification.getId().equals(id) && notification.getUserId().equals(userId)) {
                if (!notification.isRead()) {
                    notification.setRead(true);
                    store.save();
                }
                return true;
            }
        }
        return false;
    }

    public synchronized int markReviewTaskRead(String projectId) {
        int count = 0;
        for (Notification notification : store.database().getNotifications()) {
            if (projectId != null && projectId.equals(notification.getProjectId())
                    && notification.getType() == Notification.NotificationType.REVIEW_TASK
                    && !notification.isRead()) {
                notification.setRead(true);
                count++;
            }
        }
        if (count > 0) {
            store.save();
        }
        return count;
    }

    public synchronized int markAllRead(String userId) {
        int count = 0;
        for (Notification notification : store.database().getNotifications()) {
            if (notification.getUserId().equals(userId) && !notification.isRead()) {
                notification.setRead(true);
                count++;
            }
        }
        if (count > 0) {
            store.save();
        }
        return count;
    }

    public synchronized boolean delete(String id, String userId) {
        List<Notification> notifications = store.database().getNotifications();
        boolean removed = notifications.removeIf(n -> n.getId().equals(id) && n.getUserId().equals(userId));
        if (removed) {
            store.save();
        }
        return removed;
    }

    public synchronized int countUnread(String userId) {
        int count = 0;
        for (Notification notification : store.database().getNotifications()) {
            if (userId.equals(notification.getUserId()) && !notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    public synchronized int count() {
        return store.database().getNotifications().size();
    }
}
