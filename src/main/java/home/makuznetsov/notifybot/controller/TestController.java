package home.makuznetsov.notifybot.controller;

import home.makuznetsov.notifybot.config.TelegramBotConfig;
import home.makuznetsov.notifybot.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final TelegramBotConfig botConfig;
    private final TelegramBotController botController;
    private final UserSessionService userSessionService;


    @GetMapping("/sessions")
    public void getUsersSessions() {
        log.info(userSessionService.getAllUsersSessions());
    }

    @PostMapping("/simulate-telegram")
    public void simulateUpdate(@RequestBody Update update) {
        // Вручную вызываем обработчик вебхука
        botController.onWebhookUpdateReceived(update);
    }

    @GetMapping("/check-bot")
    public String checkBot() {
        if (botController != null) {
            return "Бот найден в контексте Spring! Имя: " + botController.getBotUsername();
        } else {
            return "Бот НЕ найден в контексте Spring. Проверьте конфигурацию.";
        }
    }

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return Map.of(
                "botPath", botConfig.getPath(),
                "botUsername", botConfig.getUsername(),
                "botToken", botConfig.getToken() != null ? "***" + botConfig.getToken().substring(botConfig.getToken().length() - 4) : "null",
                "actualBotPath", botController.getBotPath()
        );
    }
}