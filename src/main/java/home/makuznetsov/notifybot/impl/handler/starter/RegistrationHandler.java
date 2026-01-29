package home.makuznetsov.notifybot.impl.handler.starter;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.service.botUtil.LocalizationService;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserService;
import home.makuznetsov.notifybot.service.UserSessionService;
import home.makuznetsov.notifybot.service.botUtil.KeyboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final UserService userService;
    private final TelegramBotService botService;
    private final KeyboardService keyboardService;
    private final LocalizationService localizationService;

    private final String WRONG_NAME_FORMAT = "Неправильно введено имя, попробуйте еще раз";

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}",user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return currentAction.isPresent() &&
                    currentAction.get().equals(SessionType.REGISTRATION);
        }
        return false;
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            if(isValidNameInput(message)) {
                try {
                    userService.updateNameByTelegramId(message.getTelegramId(),
                            message.getMessage());
                    sessionService.updateAction(message.getTelegramId(),
                            user.get().getId(),
                            SessionType.MAIN_MENU);
                    botService.sendMessageWithInlineKeyboard(message.getTelegramId(),
                            localizationService.getMainMenuText(),
                            keyboardService.getMainMenuKeyboard());
                } catch (Exception e) {
                    log.error(e.getMessage());                }
            } else {
                botService.sendMarkdownMessage(message.getTelegramId(), WRONG_NAME_FORMAT);
            }
        }
    }

    private boolean isValidNameInput(IncomingMessageDto message) {
        return message.getMessage().trim().length() >= 2;
    }
}
