package home.makuznetsov.notifybot.impl;

import home.makuznetsov.notifybot.chat.outbound.ChatOutbound;
import home.makuznetsov.notifybot.config.TelegramBotConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageSender implements ChatOutbound {

    private DefaultAbsSender telegramSender;
    private final TelegramBotConfig botConfig;

    @PostConstruct
    public void init() {
        this.telegramSender = new DefaultAbsSender(
                new DefaultBotOptions(),
                botConfig.getToken()
        ) {};
    }

    @Override
    public void sendMarkdownMessage(Long chatId, String markdownText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(markdownText);
        message.enableMarkdown(true);
        message.setParseMode("Markdown");

        executeMessage(message);
    }

    @Override
    public void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            telegramSender.execute(message);
            log.debug("Message sent to chat {}: {}",
                    message.getChatId(),
                    getShortText(message.getText()));
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}",
                    message.getChatId(), e.getMessage());
            // Можно добавить логику повторной попытки
            retrySend(message);
        }
    }

    /**
     * Повторная попытка отправки (опционально)
     */
    private void retrySend(SendMessage message) {
        int maxRetries = 3;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                Thread.sleep(1000 * i); // Увеличивающаяся задержка
                telegramSender.execute(message);
                log.info("Message sent on retry {} to chat {}", i, message.getChatId());
                return;
            } catch (Exception e) {
                log.warn("Retry {} failed for chat {}: {}", i, message.getChatId(), e.getMessage());
            }
        }
        log.error("All {} retries failed for chat {}", maxRetries, message.getChatId());
    }

    /**
     * Укорачивает текст для логов
     */
    private String getShortText(String text) {
        if (text == null) return "null";
        return text.length() > 50 ? text.substring(0, 47) + "..." : text;
    }
}
