package home.makuznetsov.notifybot.impl.handler.add;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.*;
import home.makuznetsov.notifybot.service.botUtil.LocalizationService;
import home.makuznetsov.notifybot.service.ReminderService;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserSessionService;
import home.makuznetsov.notifybot.service.botUtil.KeyboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetRecurrentPatternHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final TelegramBotService botService;
    private final ReminderService reminderService;
    private final LocalizationService localizationService;
    private final KeyboardService keyboardService;
    private final String REMINDER_CREATION_SUCCESS = "Напоминание успешно добавлено";

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}",user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            Optional<UserSession> currentUserSession = sessionService.getSession(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return isCurrentActionSetRecurrencePatter(currentUserSession) &&
                    isValidRecurrencePattern(message);
        }
        return false;
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        try{
            Optional<UserSession> currentUserSession = sessionService.getSession(message.getTelegramId(), user.get().getId());
            currentUserSession.orElseThrow(Exception::new).setLastEnteredRecurrencePattern(message.getCallbackData());

            var createReminderRequest = getNewReminder(currentUserSession.get(), user.get().getTimezone());
            reminderService.createReminder(user.get().getId(), createReminderRequest);

            botService.sendMarkdownMessage(message.getTelegramId(), REMINDER_CREATION_SUCCESS);

            sessionService.updateAction(message.getTelegramId(), user.get().getId(), SessionType.MAIN_MENU);

            botService.sendMessageWithInlineKeyboard(message.getTelegramId(),
                    localizationService.getMainMenuText(),
                    keyboardService.getMainMenuKeyboard());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private boolean isCurrentActionSetRecurrencePatter(Optional<UserSession> currentUserSession) {
        return currentUserSession.isPresent() &&
                currentUserSession.get().getCurrentAction().equals(SessionType.REMINDER_CREATION) &&
                currentUserSession.get().getLastEnteredTitle() != null &&
                currentUserSession.get().getLastEnteredDateTime() != null &&
                currentUserSession.get().getLastEnteredRecurrencePattern() == null;
    }

    private boolean isValidRecurrencePattern(IncomingMessageDto message) {
        return RecurrencePattern.contains(message.getCallbackData());
    }

    private CreateReminderRequest getNewReminder(UserSession userSession, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(localizationService.getDateTimeFormat());
        ZoneId zone = ZoneId.of(timezone);
        return CreateReminderRequest.builder()
                .title(userSession.getLastEnteredTitle())
                .scheduledTime(LocalDateTime.parse(userSession.getLastEnteredDateTime(), formatter)
                        .atZone(zone))
                .recurrencePattern(userSession.getLastEnteredRecurrencePattern())
                .build();
    }
}
