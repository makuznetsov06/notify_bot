package home.makuznetsov.notifybot.fw;

import home.makuznetsov.notifybot.config.RabbitMQConfig;
import home.makuznetsov.notifybot.entity.Reminder;
import home.makuznetsov.notifybot.entity.ReminderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderQueueProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void scheduleReminder(Reminder reminder) {
        ReminderMessage message = ReminderMessage.fromReminder(reminder);
        String jsonMessage = objectMapper.writeValueAsString(message);

        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-delay", calculateDelay(reminder.getScheduledTime()));
        properties.setContentType("application/json");

        Message rabbitMessage = new Message(jsonMessage.getBytes(), properties);

        rabbitTemplate.send(
                RabbitMQConfig.REMINDER_DELAYED_EXCHANGE,
                RabbitMQConfig.REMINDER_ROUTING_KEY,
                rabbitMessage
        );

        log.info("Scheduled reminder {} for user {} at {}",
                reminder.getId(),
                reminder.getUser().getId(),
                reminder.getScheduledTime()
        );
    }

    public void cancelAndRescheduleReminder(Reminder reminder) {

        scheduleReminder(reminder);
    }

    private long calculateDelay(ZonedDateTime scheduledTime) {
        Instant now = Instant.now();
        Instant scheduledInstant = scheduledTime.toInstant();

        long delay = Duration.between(now, scheduledInstant).toMillis();

        return Math.max(delay, 0);
    }
}