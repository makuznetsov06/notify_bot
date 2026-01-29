package home.makuznetsov.notifybot.service;

import home.makuznetsov.notifybot.entity.*;
import home.makuznetsov.notifybot.fw.ReminderQueueProducer;
import home.makuznetsov.notifybot.repository.ReminderRepository;
import home.makuznetsov.notifybot.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ReminderQueueProducer reminderQueueProducer;
    private final UserRepository userRepository;

    @Transactional
    public Optional<Reminder> findById(Long reminderId) {
        return reminderRepository.findById(reminderId);
    }

    @Transactional
    public Optional<Reminder> findByIdWithUser(Long reminderId) { return reminderRepository.findByIdWithUser(reminderId); }

    @Transactional
    public Reminder createReminder(Long userId, CreateReminderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        ZonedDateTime scheduledTime = request.getScheduledTime()
                .withZoneSameInstant(ZoneId.systemDefault());

        Reminder reminder = Reminder.builder()
                .user(user)
                .title(request.getTitle())
                .scheduledTime(scheduledTime)
                .recurrencePattern(request.getRecurrencePattern())
                .status(ReminderStatus.SCHEDULED)
                .metadata(request.getMetadata())
                .build();

        Reminder savedReminder = reminderRepository.save(reminder);

        // Отправляем в очередь только если напоминание в будущем
        if (scheduledTime.isAfter(ZonedDateTime.now())) {
            reminderQueueProducer.scheduleReminder(savedReminder);
        } else {
            log.warn("Reminder scheduled for past time: {}", scheduledTime);
        }

        return savedReminder;
    }

    @Transactional
    public Reminder updateReminder(Long reminderId, UpdateReminderRequest request) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));

        // Если меняем время - нужно перепланировать
        boolean needsReschedule = request.getScheduledTime() != null &&
                !request.getScheduledTime().equals(reminder.getScheduledTime());

        if (request.getTitle() != null) {
            reminder.setTitle(request.getTitle());
        }
        if (request.getScheduledTime() != null) {
            reminder.setScheduledTime(request.getScheduledTime());
        }
        if (request.getRecurrencePattern() != null) {
            reminder.setRecurrencePattern(request.getRecurrencePattern());
        }
        if (request.getMetadata() != null) {
            reminder.setMetadata(request.getMetadata());
        }

        if (needsReschedule && reminder.getStatus() == ReminderStatus.SCHEDULED) {
            reminderQueueProducer.cancelAndRescheduleReminder(reminder);
        }

        return reminderRepository.save(reminder);
    }

    @Transactional
    public void cancelReminder(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));

        reminder.setStatus(ReminderStatus.CANCELLED);
        reminderRepository.save(reminder);

        // В идеале нужно также удалить из очереди, но это сложнее
        // Можно добавить флаг в сообщение для игнорирования при обработке
    }

    @Transactional
    public void markAsSent(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));

        reminder.setStatus(ReminderStatus.SENT);
        reminder.setLastTriggeredAt(ZonedDateTime.now());

        // Если это повторяющееся напоминание, создаем следующее
        if (reminder.getRecurrencePattern() != null && !RecurrencePattern.NONE.name().equals(reminder.getRecurrencePattern())) {
            createNextRecurrence(reminder);
        }

        reminderRepository.save(reminder);
    }

    private void createNextRecurrence(Reminder reminder) {
        ZonedDateTime nextTime = calculateNextOccurrence(
                reminder.getScheduledTime(),
                reminder.getRecurrencePattern()
        );

        Reminder nextReminder = Reminder.builder()
                .user(reminder.getUser())
                .title(reminder.getTitle())
                .scheduledTime(nextTime)
                .recurrencePattern(reminder.getRecurrencePattern())
                .status(ReminderStatus.SCHEDULED)
                .metadata(reminder.getMetadata())
                .build();

        Reminder saved = reminderRepository.save(nextReminder);
        reminderQueueProducer.scheduleReminder(saved);
    }

    private ZonedDateTime calculateNextOccurrence(ZonedDateTime currentTime, String recurrencePattern) {
        if (RecurrencePattern.DAILY.name().equals(recurrencePattern)) {
            return currentTime.plusDays(1);
        } else if (RecurrencePattern.WEEKLY.name().equals(recurrencePattern)) {
            return currentTime.plusWeeks(1);
        } else if (RecurrencePattern.MONTHLY.name().equals(recurrencePattern)) {
            return currentTime.plusMonths(1);
        } else if (RecurrencePattern.YEARLY.name().equals(recurrencePattern)) {
            return currentTime.plusYears(1);
        }

        try {
            CronExpression cronExpression = CronExpression.parse(recurrencePattern);
            LocalDateTime nextLocal = cronExpression.next(currentTime.toLocalDateTime());
            if (nextLocal != null) {
                return nextLocal.atZone(currentTime.getZone());
            } else {
                // Если следующего времени нет (например, неправильное выражение)
                throw new IllegalArgumentException("No next occurrence for cron expression: " + recurrencePattern);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid recurrence pattern: " + recurrencePattern);
        }
    }

    public List<Reminder> getUserReminders(Long userId) {
        return reminderRepository.findByUserIdAndStatusOrderByScheduledTimeAsc(
                userId,
                ReminderStatus.SCHEDULED
        );
    }

    // Для инициализации при старте приложения - планируем все незавершенные напоминания
    @PostConstruct
    @Transactional
    public void schedulePendingReminders() {
        List<Reminder> pendingReminders = reminderRepository
                .findByStatusAndScheduledTimeAfter(
                        ReminderStatus.SCHEDULED,
                        ZonedDateTime.now()
                );

        for (Reminder reminder : pendingReminders) {
            reminderQueueProducer.scheduleReminder(reminder);
        }

        log.info("Scheduled {} pending reminders", pendingReminders.size());
    }
}
