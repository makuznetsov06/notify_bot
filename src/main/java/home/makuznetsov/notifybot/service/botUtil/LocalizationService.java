package home.makuznetsov.notifybot.service.botUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalizationService {


    public final String getMainMenuText() {
        return """
            Вы в главном меню
          
            Используйте кнопки ниже,
            """;
    }

    public final String getWelcomeMessage() {
        return """
            🎉 Добро пожаловать!
            
            Для регистрации введите ваше имя (минимум 2 символа).           
            """;
    }

    public final String getWelcomeBackMessage() {
        return """
            👋 С возвращением!
            
            Используйте кнопки ниже для работы с напоминаниями.
            """;
    }

    public final String getDateTimeFormat() {
        return "dd.MM.yyyy HH:mm";
    }

    public final String getBotStartCommand() {
        return "/start";
    }
}
