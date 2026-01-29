package home.makuznetsov.notifybot.impl.user;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.service.UserService;
import home.makuznetsov.notifybot.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewUserHandler {

    private final UserSessionService sessionService;
    private final UserService userService;

    public void createNewUserFromMessage(IncomingMessageDto message) {
        User newUser = User.builder()
                .telegramUserId(message.getTelegramId())
                .username(message.getUsername())
                .firstName("new_user")
                .registeredAt(ZonedDateTime.now())
                .isActive(true)
                .languageCode("RU")
                .build();

        try {
            userService.save(newUser);
            sessionService.createSession(newUser.getTelegramUserId(),
                    newUser.getId(),
                    SessionType.REGISTRATION);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
