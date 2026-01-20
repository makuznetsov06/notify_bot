package home.makuznetsov.notifybot.chat.inbound;

import home.makuznetsov.notifybot.chat.IncomingMessage;

public interface ChatInbound {
    void handleMessage(IncomingMessage message);
}
