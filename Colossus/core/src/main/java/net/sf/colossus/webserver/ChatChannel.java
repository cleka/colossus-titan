package net.sf.colossus.webserver;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.util.Glob;
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

    private final static String[] chatHelp = new String[] { "Chat help:", "",
        "/help, /h, /? (show help)", "/ping (notify a certain user)",
        "/contact (how to contact admin)",
        "/userinfo (shows what info server has about you)",
        "/ignore (hide in the chat the lines of a certain user)",
        "/userinfo (shows what info server has about you)",
        // "/ignore (hide in the chat the lines of a certain user)",
        "",
        "Use /help <keyword> for detailed help. E.g. /help ping how to use ping." };

    private final static String[] pingHelp = new String[] {
        "Using /ping:",
        "",
        "To notify another user (it will give some beeps, and display your given message"
            + "in a popup dialog),",
        "you can use the /ping command:",
        "  /ping UserName Here comes the message",
        "If the user's name contains spaces, it must be within double quotes:",
        "  /ping \"Lengthy User Name\" Here comes the message" };

    private final static String[] ignoreHelp = new String[] {
        "Using /ignore and /unignore:",
        "",
        "If you do not wish to read anything a certain user wrote, you can add "
            + "that user to your personal 'ignore' list:",
        "  /ignore SomeUserName",
        "  /ignore Some Name With Spaces",
        "Rest of the line will be taken as one single user name; DO NOT PUT QUOTES around it!",
        "",
        "Ignore command without arguments displays your list of ignored users:",
        "  /ignore",
        "You can remove users from that list again with unignore command:",
        "  /unignore SomeUserName", "  /unignore Some Name With Spaces", "",
        "At least at the moment, those lists are not stored to any file ",
        "  => when the server was restarted, they are empty again.", };

    private final static String[] contactHelp = new String[] {
        "Using /contact:",
        "",
        "To contact the administrator of this server, send a mail to support@play-colossus.net .",
        "We also encourage you to use the \"General\" forum, the bugs tracker or the feature",
        "request tracker on our project page on Sourceforge:",
        "  http://sourceforge.net/projects/colossus/" };

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
        synchronized (storage)
        {
            storage.storeMessage(startMsg);
        }
    }

    /** Send message of the day lines to one client. */
    public void deliverMessageOfTheDayToClient(String chatId,
        IWebClient client, List<String> lines)
    {
        sendLinesToClient(chatId, client, lines, false, "SYSTEM");
    }

    public void handleUnknownCommand(String msgAllLower, String chatId,
        IWebClient client, String originalMessage)
    {
        String[] lines = new String[] {
            "Sorry, '" + msgAllLower + "' is not a recognized command.",
            "Use /help to get a list of valid commands.", "",
            "Your text was: " + originalMessage };
        sendLinesToClient(chatId, client, Arrays.asList(lines), true, "");
        // long now = new Date().getTime();
        // client.systemMessage(now, "Your text was: " + originalMessage);
    }

    public void sendHelpToClient(String msgAllLower, String chatId,
        IWebClient client)
    {
        List<String> words = Arrays.asList(msgAllLower.split(" +"));
        if (words.size() == 1)
        {
            sendLinesToClient(chatId, client, Arrays.asList(chatHelp), true,
                "");
        }
        else
        {
            if (words.get(1).startsWith("/ping")
                || words.get(1).startsWith("ping"))
            {
                sendLinesToClient(chatId, client, Arrays.asList(pingHelp),
                    true, "");
            }
            else if (words.get(1).startsWith("/contact")
                || words.get(1).startsWith("contact"))
            {
                showContactHelp(chatId, client);
            }
            if (words.get(1).startsWith("/ignore")
                || words.get(1).startsWith("ignore")
                || words.get(1).startsWith("/unignore")
                || words.get(1).startsWith("unignore"))
            {
                sendLinesToClient(chatId, client, Arrays.asList(ignoreHelp),
                    true, "");
            }
            else
            {
                String[] noSuchHelp = new String[] { "Sorry, no specific help available about '"
                    + words.get(1) + "'." };
                sendLinesToClient(chatId, client, Arrays.asList(noSuchHelp),
                    true, "");
            }
        }
    }

    /**
     * @param chatId Id of the chat
     * @param client WebClient connection who requested the contact help
     */
    public void showContactHelp(String chatId, IWebClient client)
    {
        sendLinesToClient(chatId, client, Arrays.asList(contactHelp), true, "");
    }

    /** Send an arraylist full of lines to one client. */
    public void sendLinesToClient(String chatId, IWebClient client,
        List<String> lines, boolean spacer, String sender)
    {
        long when = new Date().getTime();
        boolean isResent = false;

        if (spacer)
        {
            client.chatDeliver(chatId, when, sender, "", isResent);
        }
        for (String line : lines)
        {
            client.chatDeliver(chatId, when, sender, line, isResent);
        }
        if (spacer)
        {
            client.chatDeliver(chatId, when, sender, "", isResent);
        }

    }

    // handleShowInfo(sender, message, this);
    public void handleShowInfo(IWebClient client, User user)
    {
        List<String> lines = new ArrayList<String>();
        lines.add("Server has following information about you:");
        lines.add("      Name: " + user.getName());
        lines.add("      Email: " + user.getEmail());
        lines.add("      Registered: " + user.getCreated()
            + " [SERVER TIME, i.e. Central European time]");
        long secs = user.getOnlineTime();
        lines.add("      Online time: " + secs + " seconds ("
            + onlineTimeFromSeconds(secs) + ")");
        sendLinesToClient(chatId, client, lines, true, "SYSTEM");
    }

    public void handleIgnore(String msgAllLower, User ignoringUser)
    {
        List<String> words = Arrays.asList(msgAllLower.split(" +", 2));
        if (words.size() == 1)
        {
            tellListOfIgnoredUsers(ignoringUser, null);
        }
        else
        {
            String ignoredUserName = words.get(1);
            User userToBeIgnored = userDB.findUserByName(ignoredUserName);
            if (userToBeIgnored != null)
            {
                ignoringUser.addToIgnoredUsers(ignoredUserName);
                tellListOfIgnoredUsers(ignoringUser, "Added to list: "
                    + ignoredUserName);
            }
            else
            {
                List<String> lines = new ArrayList<String>();
                lines.add("ERROR: Unknown user '" + ignoredUserName + " '!");
                sendLinesToClient(chatId, ignoringUser.getWebserverClient(), lines,
                    true, "SYSTEM");
            }
        }
    }

    public void handleUnignore(String msgAllLower, User user)
    {
        List<String> words = Arrays.asList(msgAllLower.split(" +", 2));
        if (words.size() == 1)
        {
            List<String> lines = new ArrayList<String>();
            lines.add("The command /unignore needs an user "
                + "name argument (no quotes needed).");
            sendLinesToClient(chatId, user.getWebserverClient(), lines, true,
                "SYSTEM");
        }
        else
        {
            User userToBeUnignored = userDB.findUserByName(words.get(1));
            if (userToBeUnignored != null)
            {
                user.removeFromIgnoredUsers(userToBeUnignored.getName());
                tellListOfIgnoredUsers(user,
 "Removed from list: "
                    + userToBeUnignored.getName());
            }
            else
            {
                List<String> lines = new ArrayList<String>();
                lines.add("ERROR: Unknown user '" + words.get(1) + " '!");
                sendLinesToClient(chatId, user.getWebserverClient(), lines,
                    true, "SYSTEM");
            }
        }
    }

    private void tellListOfIgnoredUsers(User user, String change)
    {
        List<String> lines = new ArrayList<String>();
        if (change != null)
        {
            lines.add(change);
        }
        List<String> ignoredUsers = user.getListOfIgnoredUsers();
        if (ignoredUsers.size() == 0)
        {
            lines.add("You have no users in your 'ignore' list.");
        }
        else
        {
            lines.add("The following users are in your 'ignore' list: "
                + Glob.glob(", ", ignoredUsers));
        }
        sendLinesToClient(chatId, user.getWebserverClient(), lines, true,
            "SYSTEM");
    }

    @SuppressWarnings("boxing")
    private String onlineTimeFromSeconds(long totalsecs)
    {
        long total = totalsecs;
        long secs = total % 60;
        total -= secs;
        total /= 60;
        long mins = total % 60;
        total -= mins;
        total /= 60;
        long hours = total % 24;
        total -= hours;
        total /= 24;
        long days = total;

        String onlineTime = String.format(
            "%d days, %d hours, %d minutes, %d seconds", days, hours, mins,
            secs);
        return onlineTime;
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
        lines.add("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        lines.add("");
        lines.add(" Hello " + userName + ", please note:");
        lines.add("");
        lines.add(" You are using a rather old Colossus version!");
        lines.add(" If you can, please start using the newest version, "
            + "for example from the Colossus homepage. Go to the page:");
        lines.add("");
        lines.add("    http://colossus.sourceforge.net/");
        lines.add("");
        lines.add(" and click on the pink icon in upper left corner!");
        lines.add(" Or download newest zip file from SourceForge: "
            + "https://sourceforge.net/projects/colossus/");
        lines.add("");
        lines.add("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        lines.add("");

        for (String line : lines)
        {
            client.chatDeliver(chatId, when, sender, line, isResent);
        }
    }

    public void createStoreAndDeliverMessage(String sender, String message)
    {
        long now = new Date().getTime();
        ChatMessage msg = new ChatMessage(this.chatId, now, sender, message);
        synchronized (storage)
        {
            storage.storeMessage(msg);
        }
        appendToChatlog(msg);
        deliverMessage(msg, userDB);
    }

    private void deliverMessage(ChatMessage msg, UserDB userDB)
    {
        LOGGER.fine("Delivering message " + msg
            + " to clients; checking ignore list:");
        Collection<User> users = userDB.getLoggedInUsers();
        for (User u : users)
        {
            String sender = msg.getSender();
            if (!u.isUserInIgnoredList(sender))
            {
                IWebClient client = u.getWebserverClient();
                deliverMessageToClient(msg, client, false);
            }
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

    public void writeMessageToAdminToChatlog(long when, String fromUser,
        String fromMail, List<String> lines)
    {
        String whenTime = whenFormatter.timeAsString(when);
        String dateChange = whenFormatter.hasDateChanged();
        if (dateChange != null)
        {
            chatLog.println(doubledashes + " " + dateChange + " "
                + doubledashes);
        }

        chatLog.println("\n==============================");
        chatLog.println("\n" + whenTime + ": Message from " + fromUser + " ("
            + fromMail + ") to admin: \n");
        chatLog.println("------------------------------");
        for (String line : lines)
        {
            chatLog.println("  " + line);
        }
        chatLog.println("------------------------------\n");
        chatLog.flush();
    }

}
