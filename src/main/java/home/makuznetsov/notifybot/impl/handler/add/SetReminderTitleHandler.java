package home.makuznetsov.notifybot.impl.handler.add;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.entity.UserSession;
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
public class SetReminderTitleHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final TelegramBotService botService;
    private final String SET_REMINDER_DATE = "Введите дату в будущем в формате день.месяц.год час:минута";

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}",user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            Optional<UserSession> currentUserSession = sessionService.getSession(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return isCurrentActionSetReminderTitle(currentUserSession) &&
                    isValidTitle(message.getMessage());
        }
        return false;
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        try{
            sessionService.getSession(message.getTelegramId(), user.get().getId())
                            .orElseThrow(Exception::new).setLastEnteredTitle(message.getMessage());
            botService.sendMarkdownMessage(message.getTelegramId(), SET_REMINDER_DATE);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private boolean isValidTitle(String message) {
        return message.length() > 0 && message.length() <= 255;
    }

    private boolean isCurrentActionSetReminderTitle(Optional<UserSession> currentUserSession) {
        return currentUserSession.isPresent() &&
                currentUserSession.get().getCurrentAction().equals(SessionType.REMINDER_CREATION) &&
                currentUserSession.get().getLastEnteredTitle() == null &&
                currentUserSession.get().getLastEnteredDateTime() == null &&
                currentUserSession.get().getLastEnteredRecurrencePattern() == null;
    }
}
