package home.makuznetsov.notifybot.repository;

import home.makuznetsov.notifybot.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findAllByUserId(Long userId);

    Optional<Reminder> findByIdAndUserId(Long reminderId, Long userId);

    void deleteById(Long reminderId);
}
