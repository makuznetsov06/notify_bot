package home.makuznetsov.notifybot.impl.handler;

import home.makuznetsov.notifybot.chat.IncomingMessage;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatInboundImpl implements ChatInbound {

    private final UserService userService;

    @Override
    public void handleMessage(IncomingMessage message) {
        User user = userService.getUserByTelegramId(message.getTelegramId());

    }
}
