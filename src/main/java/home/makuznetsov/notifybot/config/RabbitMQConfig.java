package home.makuznetsov.notifybot.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String REMINDER_DELAYED_EXCHANGE = "reminder.delayed.exchange";
    public static final String REMINDER_QUEUE = "reminder.queue";
    public static final String REMINDER_ROUTING_KEY = "reminder.key";
    public static final String REMINDER_DLQ = "reminder.dlq";
    public static final String REMINDER_DLX = "reminder.dlx";

    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");

        return new CustomExchange(
                REMINDER_DELAYED_EXCHANGE,
                "x-delayed-message",
                true,
                false,
                args
        );
    }

    @Bean
    public Queue reminderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", REMINDER_DLX);
        args.put("x-dead-letter-routing-key", REMINDER_DLQ);

        return QueueBuilder.durable(REMINDER_QUEUE)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(REMINDER_DLQ).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(REMINDER_DLX);
    }

    @Bean
    public Binding binding(Queue reminderQueue, CustomExchange delayedExchange) {
        return BindingBuilder
                .bind(reminderQueue)
                .to(delayedExchange)
                .with(REMINDER_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(REMINDER_DLQ);
    }
}
