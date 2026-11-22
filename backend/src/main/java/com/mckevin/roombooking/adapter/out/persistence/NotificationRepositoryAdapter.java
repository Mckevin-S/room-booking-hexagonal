package com.mckevin.roombooking.adapter.out.persistence;

import com.mckevin.roombooking.application.notification.Notification;
import com.mckevin.roombooking.application.port.NotificationRepository;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository notifications;

    NotificationRepositoryAdapter(NotificationJpaRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public void save(Notification notification) {
        notifications.save(new NotificationEntity(notification.recipient().value(), notification.message(),
                notification.createdAt()));
    }

    @Override
    public List<Notification> recentOf(EmployeeId recipient, int limit) {
        return notifications.findByRecipientIdOrderByCreatedAtDescIdDesc(recipient.value(), PageRequest.of(0, limit)).stream()
                .map(n -> new Notification(n.id, EmployeeId.of(n.recipientId), n.message, n.createdAt, n.read))
                .toList();
    }

    @Override
    public long unreadCount(EmployeeId recipient) {
        return notifications.countByRecipientIdAndReadFalse(recipient.value());
    }

    @Override
    public void markAllRead(EmployeeId recipient) {
        notifications.markAllRead(recipient.value());
    }
}
