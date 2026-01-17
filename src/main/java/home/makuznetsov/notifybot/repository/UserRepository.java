package home.makuznetsov.notifybot.repository;

import home.makuznetsov.notifybot.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Поиск всех пользователей с напоминаниями
    @EntityGraph(value = "User.withReminders", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT u FROM User u")
    List<User> findAllWithReminders();

    // Поиск по ID с напоминаниями
    @EntityGraph(value = "User.withReminders", type = EntityGraph.EntityGraphType.FETCH)
    Optional<User> findById(Long id);

    // Поиск по Telegram ID с напоминаниями
    @EntityGraph(value = "User.withReminders", type = EntityGraph.EntityGraphType.FETCH)
    Optional<User> findByTelegramUserId(Long telegramUserId);

    // Метод для отчета (пользователи + количество напоминаний)
    @Query("""
        SELECT u.id, u.username, u.telegramUserId, COUNT(r.id) as reminderCount 
        FROM User u 
        LEFT JOIN u.reminders r 
        GROUP BY u.id, u.username, u.telegramUserId
        ORDER BY reminderCount DESC
    """)
    List<Object[]> getUsersWithReminderCount();

    void deleteById(Long id);
}
