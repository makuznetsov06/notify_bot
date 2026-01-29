package home.makuznetsov.notifybot.repository;

import home.makuznetsov.notifybot.entity.Reminder;
import home.makuznetsov.notifybot.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findAllByUserId(Long userId);

    Optional<Reminder> findById(Long reminderId);

    @Query("SELECT r FROM Reminder r JOIN FETCH r.user WHERE r.id = :id")
    Optional<Reminder> findByIdWithUser(@Param("id") Long id);

    void deleteById(Long reminderId);

    @Query("SELECT r FROM Reminder r " +
            "JOIN FETCH r.user u " +
            "WHERE r.status = :status " +
            "AND r.scheduledTime > :time")
    List<Reminder> findByStatusAndScheduledTimeAfter(
            @Param("status") ReminderStatus reminderStatus,
            @Param("time") ZonedDateTime now);

    List<Reminder> findByUserIdAndStatusOrderByScheduledTimeAsc(Long userId, ReminderStatus reminderStatus);
}
