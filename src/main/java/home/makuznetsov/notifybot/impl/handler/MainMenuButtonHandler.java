package home.makuznetsov.notifybot.impl.handler;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.ActionButton;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserSessionService;
import home.makuznetsov.notifybot.service.botUtil.KeyboardService;
import home.makuznetsov.notifybot.service.botUtil.LocalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainMenuButtonHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final TelegramBotService botService;
    private final LocalizationService localizationService;
    private final KeyboardService keyboardService;

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}",user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return currentAction.isPresent() &&
                    (message.getCallbackData().equals(ActionButton.MAIN_MENU.name()) ||
                            message.getMessage().equalsIgnoreCase("меню")
                    );
        }
        return false;
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        try{
            sessionService.updateAction(message.getTelegramId(), user.get().getId(), SessionType.MAIN_MENU);
            botService.sendMessageWithInlineKeyboard(message.getTelegramId(),
                    localizationService.getMainMenuText(),
                    keyboardService.getMainMenuKeyboard());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
