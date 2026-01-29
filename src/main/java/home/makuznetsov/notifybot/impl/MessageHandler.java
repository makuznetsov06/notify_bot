package home.makuznetsov.notifybot.impl;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.impl.handler.DefaultMessageHandler;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageHandler implements InitializingBean {

    private final List<ChatInbound> messageHandlers;
    private final UserService userService;
    private final TelegramBotService botService;
    private final DefaultMessageHandler defaultMessageHandler;

    public void handleMessage(IncomingMessageDto message) {
        try {
            var user = userService.getUserByTelegramId(message.getTelegramId());
            var handler = messageHandlers.stream()
                    .filter(command -> command.canHandle(message, user))
                    .findFirst()
                    .orElse(defaultMessageHandler);
            handler.handle(message, user);
        } catch (Exception e) {
            botService.sendErrorMarkdownMessage(message.getTelegramId());
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        OrderComparator.sort(messageHandlers);
    }
}
