package home.makuznetsov.notifybot.chat;

import home.makuznetsov.notifybot.entity.User;

import java.util.Optional;

public interface CommandHandler {

    void handle(IncomingMessage message, Optional<User> user);
}
