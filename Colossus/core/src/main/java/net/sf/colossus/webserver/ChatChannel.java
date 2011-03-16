package net.sf.colossus.webserver;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.ChatMessage;
import net.sf.colossus.webcommon.FormatWhen;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.User;
import net.sf.colossus.webcommon.UserDB;


public class ChatChannel
{
    private static final Logger LOGGER = Logger.getLogger(ChatChannel.class
        .getName());

    private final UserDB userDB;
    private final String chatId;
    private final ChatMsgStorage storage;
    private final PrintWriter chatLog;
    private final FormatWhen whenFormatter;

    private final static String doubledashes = "=========================";

    public ChatChannel(String id, WebServerOptions options, UserDB userDB)
    {
        this.userDB = userDB;
        this.chatId = id;
        this.storage = new ChatMsgStorage(this, options);
        this.chatLog = openLogForAppend(options);
        this.whenFormatter = new FormatWhen();
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
        synchronized(storage)
        {
            storage.storeMessage(startMsg);
        }
    }


    /** Send message of the day lines to one client. */
    public void deliverMessageOfTheDayToClient(String chatId,
        IWebClient client, ArrayList<String> lines)
    {
        long when = new Date().getTime();
        String sender = "SYSTEM";
        boolean isResent = false;

        for (String line : lines)
        {
            client.chatDeliver(chatId, when, sender, line, isResent);
        }
    }

    // TODO is this perhaps obsolete nowadays?
    /** Send message of the day lines to one client. */
    public void deliverOldVersionWarning(String chatId, String userName,
        IWebClient client)
    {
        long when = new Date().getTime();
        String sender = "SYSTEM";
        boolean isResent = false;

        ArrayList<String> lines = new ArrayList<String>();
        lines.add("");
        lines
            .add("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        lines.add("");
        lines.add(" Hello " + userName + ", please note:");
        lines
            .add(" You are using a Colossus version older than 0.10.3. See message above!");
        lines.add("");
        lines
            .add("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        lines.add("");

        for (String line : lines)
        {
            client.chatDeliver(chatId, when, sender, line, isResent);
        }
    }


    public void createStoreAndDeliverMessage(String sender, String message)
    {
        long now = new Date().getTime();
        ChatMessage msg = new ChatMessage(this.chatId,
            now, sender, message);
        synchronized (storage)
        {
            storage.storeMessage(msg);
        }
        appendToChatlog(msg);
        deliverMessage(msg, userDB);
    }

    private void deliverMessage(ChatMessage msg, UserDB userDB)
    {
        Collection<User> users = userDB.getLoggedInUsers();
        for (User u : users)
        {
            IWebClient client = u.getWebserverClient();
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
        synchronized (storage)
        {
            for (ChatMessage msg : storage.getLastNChatMessages())
            {
                deliverMessageToClient(msg, client, true);
            }
        }
        long now = new Date().getTime();
        client.chatDeliver(chatId, now, null, null, true);
    }

    private PrintWriter openLogForAppend(WebServerOptions options)
    {
        String usersFileDirectory = options
            .getStringOption(WebServerConstants.optDataDirectory);
        if (usersFileDirectory == null)
        {
            LOGGER
                .severe("Data Directory (for chat messages log file) is null! Define it in cf file!");
            System.exit(1);
        }
        String filename = "ChatLog-" + getChannelId() + ".txt";

        PrintWriter chatLog = null;
        try
        {
            boolean append = true;
            chatLog = new PrintWriter(new FileOutputStream(new File(
                usersFileDirectory, filename), append));
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Writing char messages file " + filename
                + "failed: FileNotFoundException: ", e);
        }
        return chatLog;
    }

    private void appendToChatlog(ChatMessage msg)
    {
        String sender = msg.getSender();
        String message = msg.getMessage();
        long when = msg.getWhen();


        String whenTime = whenFormatter.timeAsString(when);
        String dateChange = whenFormatter.hasDateChanged();
        if (dateChange != null)
        {
            chatLog.println(doubledashes + " " + dateChange + " "
                + doubledashes);
        }
        chatLog.println(whenTime + " " + sender + ": " + message);
        chatLog.flush();
    }
}
