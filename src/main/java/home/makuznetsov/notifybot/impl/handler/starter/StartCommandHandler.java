package home.makuznetsov.notifybot.impl.handler.starter;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.impl.user.NewUserHandler;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserSessionService;
import home.makuznetsov.notifybot.service.botUtil.KeyboardService;
import home.makuznetsov.notifybot.service.botUtil.LocalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartCommandHandler implements ChatInbound {

    private final TelegramBotService botService;
    private final UserSessionService sessionService;
    private final KeyboardService keyboardService;
    private final NewUserHandler newUserHandler;
    private final LocalizationService localizationService;

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        return localizationService.getBotStartCommand().equals(message.getMessage());
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("User with Id: {} has been logged", user.get().getId());
            sessionService.createSession(user.get().getTelegramUserId(),
                    user.get().getId(),
                    SessionType.MAIN_MENU);

            botService.sendMessageWithInlineKeyboard(user.get().getTelegramUserId(),
                    localizationService.getWelcomeBackMessage(),
                    keyboardService.getMainMenuKeyboard());
            return ;
        }
        createAndGreetUser(message, localizationService.getWelcomeMessage());
    }

    private void createAndGreetUser(IncomingMessageDto message, String greetMessageText) {
        newUserHandler.createNewUserFromMessage(message);

        botService.sendMarkdownMessage(message.getTelegramId(), greetMessageText);
    }
}
