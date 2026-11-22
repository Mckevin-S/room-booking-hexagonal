package com.mckevin.roombooking.application.port;

import com.mckevin.roombooking.application.notification.Notification;
import com.mckevin.roombooking.domain.employee.EmployeeId;
import java.util.List;

public interface NotificationRepository {

    void save(Notification notification);

    List<Notification> recentOf(EmployeeId recipient, int limit);

    long unreadCount(EmployeeId recipient);

    void markAllRead(EmployeeId recipient);
}
