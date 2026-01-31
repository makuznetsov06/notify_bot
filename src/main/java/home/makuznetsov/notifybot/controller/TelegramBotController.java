package home.makuznetsov.notifybot.controller;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.config.TelegramBotConfig;
import home.makuznetsov.notifybot.entity.TelegramUpdate;
import home.makuznetsov.notifybot.impl.MessageHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TelegramBotController extends TelegramWebhookBot {

    private final TelegramBotConfig botConfig;
    private final MessageHandler messageHandler;

    @PostConstruct
    public void init() {
        log.info("Bot config loaded: path={}, username={}",
                botConfig.getPath(),
                botConfig.getUsername());
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }
    
    @PostMapping("${telegram.bot.path:/webhook}")
    public ResponseEntity<String> onWebhookUpdateReceived(@RequestBody TelegramUpdate update) {
        log.info("Update received: {}", update);
        try {
            IncomingMessageDto message = parseIncomingUpdate(update);
            messageHandler.handleMessage(message);

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.ok("OK");
        }
    }

    private IncomingMessageDto parseIncomingUpdate(TelegramUpdate update) {

        if (update.callback_query() != null) {
            return IncomingMessageDto.builder()
                    .telegramId(update.callback_query().from().id())
                    .username(update.callback_query().from().username())
                    .message(update.callback_query().message().text())
                    .callbackData(update.callback_query().data())
                    .build();
        }
//        if (update.containsKey("callback_query")) {
//            message = processCallbackQueryFromMap((Map<String, Object>) update.get("callback_query"));
//        } else if (update.containsKey("message")) {
//            message = processMessageFromMap((Map<String, Object>) update.get("message"));
//        }
        return IncomingMessageDto.builder()
                .telegramId(update.message().from().id())
                .username(update.message().from().username())
                .message(update.message().text())
                .callbackData("")
                .build();
    }

    private IncomingMessageDto processCallbackQueryFromMap(Map<String, Object> callbackQuery) {
        String callbackData = (String) callbackQuery.get("data");

        Map<String, Object> messageMap = (Map<String, Object>) callbackQuery.get("message");
        Map<String, Object> fromMap = (Map<String, Object>) callbackQuery.get("from");

        Long chatId = ((Number) fromMap.get("id")).longValue();
        String username = fromMap.get("username").toString();
        String messageText = messageMap.get("text").toString();

        IncomingMessageDto message = IncomingMessageDto.builder()
                .telegramId(chatId)
                .username(username)
                .message(messageText)
                .callbackData(callbackData)
                .build();
        return message;
    }

    private IncomingMessageDto processMessageFromMap(Map<String, Object> message) {
        Map<String, Object> fromMap = (Map<String, Object>) message.get("from");

        Long chatId = ((Number) fromMap.get("id")).longValue();
        String messageText = message.get("text").toString();
        String username = fromMap.get("username").toString();

        IncomingMessageDto updateMessage = IncomingMessageDto.builder()
                .telegramId(chatId)
                .username(username)
                .message(messageText)
                .build();

        return updateMessage;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return null;
    }

    @Override
    public String getBotPath() {
        return botConfig.getPath();
    }
}