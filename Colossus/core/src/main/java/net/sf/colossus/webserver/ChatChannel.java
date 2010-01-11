package net.sf.colossus.webserver;


import java.util.Collection;
import java.util.Date;

import net.sf.colossus.webcommon.ChatMessage;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.User;


public class ChatChannel
{
    // private static final Logger LOGGER = Logger.getLogger(ChatChannel.class
    //    .getName());

    private final String chatId;
    private final ChatMsgStorage storage;

    public ChatChannel(String id, WebServerOptions options)
    {
        this.chatId = id;
        this.storage = new ChatMsgStorage(this, options);
    }

    public String getChannelId()
    {
        return chatId;
    }

    public void dispose()
    {
        storage.dispose();
    }

    public void createWelcomeMessage()
    {
        long now = new Date().getTime();
        ChatMessage startMsg = new ChatMessage(this.chatId, now, "SYSTEM",
            "WebServer started. Welcome!!");
        storage.storeMessage(startMsg);
    }

    public void createStoreAndDeliverMessage(String sender, String message)
    {
        long now = new Date().getTime();
        ChatMessage msg = new ChatMessage(this.chatId,
            now, sender, message);
        storage.storeMessage(msg);
        deliverMessage(msg);
    }

    private void deliverMessage(ChatMessage msg)
    {
        Collection<User> users = User.getLoggedInUsers();
        for (User u : users)
        {
            IWebClient client = (IWebClient)u.getThread();
            deliverMessageToClient(msg, client, false);
        }
    }

    private void deliverMessageToClient(ChatMessage msg, IWebClient client,
        boolean isResent)
    {
        client.chatDeliver(msg.getChatId(), msg.getWhen(), msg.getSender(),
            msg.getMessage(), isResent);
    }

    public synchronized void tellLastMessagesToOne(IWebClient client)
    {
        for (ChatMessage msg : storage.getLastNChatMessages())
        {
            deliverMessageToClient(msg, client, true);
        }
        long now = new Date().getTime();
        client.chatDeliver(chatId, now, null, null, true);
    }

}
