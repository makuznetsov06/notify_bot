package home.makuznetsov.notifybot.impl.handler.add;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.ActionButton;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddNewReminderHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final TelegramBotService botService;
    private final String SET_TITLE_TEXT = "Введите название события:";

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}",user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return currentAction.isPresent() &&
                    currentAction.get().equals(SessionType.MAIN_MENU) &&
                    message.getCallbackData().equals(ActionButton.ADD_NEW_REMINDER.name());
        }
        return false;
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        try{
            sessionService.cleanLastEnteredData(message.getTelegramId(),
                    user.orElseThrow(Exception::new).getId());
            sessionService.updateAction(message.getTelegramId(),
                    user.orElseThrow(Exception::new).getId(),
                    SessionType.REMINDER_CREATION);
            botService.sendMarkdownMessage(message.getTelegramId(), SET_TITLE_TEXT);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
