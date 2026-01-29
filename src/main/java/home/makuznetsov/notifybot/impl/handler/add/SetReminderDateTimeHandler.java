package home.makuznetsov.notifybot.impl.handler.add;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.entity.UserSession;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserSessionService;
import home.makuznetsov.notifybot.service.botUtil.KeyboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetReminderDateTimeHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final TelegramBotService botService;
    private final KeyboardService keyboardService;
    private final String SET_RECURRENCE_PATTERN = "Выберите частоту повторения:";
    private final String WRONG_DATETIME_FORMAT = """
                    Введеная дата не соответствует формату
                    день.месяц.год час:минута
                    Пожалуйста, попробуйте снова
                    """;


    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}",user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            Optional<UserSession> currentUserSession = sessionService.getSession(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return isCurrentActionSetReminderDateTime(currentUserSession);
        }
        return false;
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        try{
            if(isValidInputDateTime(message, user)) {
                sessionService.getSession(message.getTelegramId(), user.get().getId())
                        .orElseThrow(Exception::new).setLastEnteredDateTime(message.getMessage());
                botService.sendMessageWithInlineKeyboard(message.getTelegramId(),
                        SET_RECURRENCE_PATTERN,
                        keyboardService.getRecurrencePatternKeyboard());
            } else {
                botService.sendMarkdownMessage(message.getTelegramId(), WRONG_DATETIME_FORMAT);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private boolean isValidInputDateTime(IncomingMessageDto message, Optional<User> user) {
        String inputDateTime = message.getMessage().trim();
        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        ZoneId userTimeZone = ZoneId.of(user.get().getTimezone());
        if (inputDateTime != null && !inputDateTime.isBlank()) {
            try {
                ZonedDateTime userDateTime = LocalDateTime.parse(inputDateTime, formatter)
                        .atZone(userTimeZone);
                return !userDateTime.isBefore(ZonedDateTime.now(userTimeZone));
            } catch (DateTimeParseException e) {
                return false;
            }
        }
        return false;
    }

    private boolean isCurrentActionSetReminderDateTime(Optional<UserSession> currentUserSession) {
        return currentUserSession.isPresent() &&
                currentUserSession.get().getCurrentAction().equals(SessionType.REMINDER_CREATION) &&
                currentUserSession.get().getLastEnteredTitle() != null &&
                currentUserSession.get().getLastEnteredDateTime() == null &&
                currentUserSession.get().getLastEnteredRecurrencePattern() == null;
    }
}
