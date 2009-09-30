package net.sf.colossus.webserver;


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.User;

public class ChatChannel
{
    // private static final Logger LOGGER = Logger.getLogger(ChatChannel.class
    //    .getName());

    private final String chatId;

    private final List<ChatMessage> lastNChatMessages = new ArrayList<ChatMessage>();

    public ChatChannel(String id)
    {
        this.chatId = id;
    }

    public String getChannelId()
    {
        return chatId;
    }

    public void createWelcomeMessage()
    {
        long now = new Date().getTime();
        ChatMessage startMsg = new ChatMessage(this.chatId, now, "SYSTEM",
            "WebServer started. Welcome!!");
        storeMessage(startMsg);
    }

    public void createStoreAndDeliverMessage(String sender, String message)
    {
        long now = new Date().getTime();
        ChatChannel.ChatMessage msg = new ChatChannel.ChatMessage(this.chatId,
            now, sender, message);
        storeMessage(msg);
        deliverMessage(msg);
    }

    private void storeMessage(ChatChannel.ChatMessage msg)
    {
        List<ChatChannel.ChatMessage> list = lastNChatMessages;
        synchronized (list)
        {
            list.add(msg);
            if (list.size() > WebServerConstants.keepLastNMessages)
            {
                // if longer than max, remove oldest one
                list.remove(0);
            }
        }
    }

    private void deliverMessage(ChatMessage msg)
    {
        Iterator<User> it = User.getLoggedInUsersIterator();
        while (it.hasNext())
        {
            User u = it.next();
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

    public void tellLastMessagesToOne(IWebClient client)
    {
        synchronized (lastNChatMessages)
        {
            for (ChatMessage msg : lastNChatMessages)
            {
                deliverMessageToClient(msg, client, true);
            }
            long now = new Date().getTime();
            client.chatDeliver(chatId, now, null, null, true);
        }
    }


    static class ChatMessage
    {
        String chatId;
        long when;
        String sender;
        String message;

        public ChatMessage(String chatId, long when, String sender,
            String message)
        {
            this.chatId = chatId;
            this.when = when;
            this.sender = sender;
            this.message = message;
        }

        public String getChatId()
        {
            return this.chatId;
        }

        public long getWhen()
        {
            return this.when;
        }

        public String getSender()
        {
            return this.sender;
        }

        public String getMessage()
        {
            return this.message;
        }
    }

}
