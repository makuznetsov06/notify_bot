package home.makuznetsov.notifybot.fw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import home.makuznetsov.notifybot.config.RabbitMQConfig;
import home.makuznetsov.notifybot.entity.Reminder;
import home.makuznetsov.notifybot.entity.ReminderMessage;
import home.makuznetsov.notifybot.entity.ReminderStatus;
import home.makuznetsov.notifybot.service.ReminderService;
import home.makuznetsov.notifybot.service.TelegramBotService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderQueueConsumer {

    private final ReminderService reminderService;
    private final TelegramBotService telegramBotService;

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @RabbitListener(queues = RabbitMQConfig.REMINDER_QUEUE)
    public void processReminder(Message message) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ReminderMessage reminder = objectMapper.readValue(body, ReminderMessage.class);
            log.info("Processing reminder: {}", reminder.getReminderId());

            // Проверяем, актуально ли еще напоминание
            Reminder actualReminder = reminderService.findByIdWithUser(reminder.getReminderId())
                    .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));

            if (actualReminder.getStatus() != ReminderStatus.SCHEDULED) {
                log.info("Reminder {} is not in SCHEDULED status, skipping", actualReminder.getId());
                return;
            }

            // Отправляем уведомление
            telegramBotService.sendMarkdownMessage(
                    actualReminder.getUser().getTelegramUserId(),
                    formatReminderMessage(actualReminder)
            );

            // Помечаем как отправленное
            reminderService.markAsSent(actualReminder.getId());

            log.info("Successfully processed reminder: {}", actualReminder.getId());

        } catch (Exception e) {
            log.error("Failed to process reminder: {}", message.toString(), e);
            // Бросаем исключение для retry механизма RabbitMQ
            throw new AmqpRejectAndDontRequeueException("Failed to process reminder", e);
        }
    }

    private String formatReminderMessage(Reminder reminder) {
        String timeFormatted = reminder.getScheduledTime()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        return String.format("""
            ⏰ Напоминание!
            
            %s
            
            Запланировано на: %s
            """,
                reminder.getTitle(),
                timeFormatted
        );
    }

    // Обработчик для dead letter queue
    @RabbitListener(queues = RabbitMQConfig.REMINDER_DLQ)
    public void processFailedReminder(ReminderMessage message) {
        log.error("Reminder {} failed to process multiple times: {}",
                message.getReminderId(), message);

    }
}
