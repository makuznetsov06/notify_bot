package home.makuznetsov.notifybot.chat.inbound;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.entity.User;

import java.util.Optional;

public interface ChatInbound {
    boolean canHandle(IncomingMessageDto message, Optional<User> user);

    void handle(IncomingMessageDto message, Optional<User> user);
}
