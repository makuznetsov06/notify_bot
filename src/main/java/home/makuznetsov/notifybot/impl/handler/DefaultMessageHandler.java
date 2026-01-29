package home.makuznetsov.notifybot.impl.handler;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.entity.UserSession;
import home.makuznetsov.notifybot.impl.user.NewUserHandler;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserSessionService;
import home.makuznetsov.notifybot.service.botUtil.KeyboardService;
import home.makuznetsov.notifybot.service.botUtil.LocalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMessageHandler implements ChatInbound{

    private final TelegramBotService botService;
    private final UserSessionService sessionService;
    private final NewUserHandler newUserHandler;
    private final KeyboardService keyboardService;
    private final LocalizationService localizationService;

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        return false;
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            Optional<UserSession> currentSession = sessionService.getSession(message.getTelegramId(),
                    user.get().getId());
            if (currentSession.isPresent()) {
                botService.sendErrorMarkdownMessage(message.getTelegramId());
            } else {
                botService.sendMarkdownMessage(message.getTelegramId(), "Время сессии вышло! Возврат в главное меню...");
                sessionService.createSession(message.getTelegramId(),
                        user.get().getId(),
                        SessionType.MAIN_MENU);
                botService.sendMessageWithInlineKeyboard(message.getTelegramId(),
                        localizationService.getMainMenuText(),
                        keyboardService.getMainMenuKeyboard());
            }
        } else {
            newUserHandler.createNewUserFromMessage(message);
            botService.sendMarkdownMessage(message.getTelegramId(),
                    localizationService.getWelcomeMessage());
        }
    }


}
