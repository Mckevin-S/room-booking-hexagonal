package com.mckevin.roombooking.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByRecipientIdOrderByCreatedAtDescIdDesc(long recipientId, Pageable page);

    long countByRecipientIdAndReadFalse(long recipientId);

    @Modifying
    @Query("update NotificationEntity n set n.read = true where n.recipientId = :recipient and n.read = false")
    int markAllRead(@Param("recipient") long recipient);
}
