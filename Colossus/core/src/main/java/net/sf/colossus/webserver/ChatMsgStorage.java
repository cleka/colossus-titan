package net.sf.colossus.webserver;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.ChatMessage;


public class ChatMsgStorage
{
    private static final Logger LOGGER = Logger.getLogger(ChatMsgStorage.class
        .getName());

    private final WebServerOptions options;
    private final ChatChannel channel;
    private final List<ChatMessage> lastNChatMessages;

    /** Just by coincidence, we use the same separator as for the network
     *  transmissions, so then there is no risk of "can't be unambiguely
     *  encoded".
     */
    private final static String SEP = " ~ ";

    public ChatMsgStorage(ChatChannel theChannel, WebServerOptions options)
    {
        this.options = options;
        this.channel = theChannel;
        LOGGER.fine("Chat Message Storage instantiated for channel "
            + getChannel().getChannelId());
        this.lastNChatMessages = new ArrayList<ChatMessage>();
        restoreMessages();
    }

    public void dispose()
    {
        storeMessages();
        LOGGER.fine("disposing - stored " + lastNChatMessages.size()
            + " messages");
    }

    public ChatChannel getChannel()
    {
        return channel;
    }

    public List<ChatMessage> getLastNChatMessages()
    {
        return Collections.unmodifiableList(lastNChatMessages);
    }

    void storeMessage(ChatMessage msg)
    {
        List<ChatMessage> list = lastNChatMessages;
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

    /** Store all messages to a permanent storage (Disk file or DB) from where
     *  they can be read back when server is restarting
     */
    private void storeMessages()
    {
        String usersFileDirectory = options
            .getStringOption(WebServerConstants.optDataDirectory);
        if (usersFileDirectory == null)
        {
            LOGGER
                .severe("Data Directory (for chat messages file) is null! Define it in cf file!");
            System.exit(1);
        }
        String filename = "Chatmessages-" + getChannel().getChannelId()
            + ".txt";

        PrintWriter out = null;
        try
        {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                new File(usersFileDirectory, filename)),
                WebServerConstants.charset));

            for (ChatMessage msg : getLastNChatMessages())
            {
                String line = makeLine(msg);
                out.println(line);
            }
            out.close();
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Writing char messages file " + filename
                + "failed: FileNotFoundException: ", e);
        }

    }

    private void restoreMessages()
    {
        String usersFileDirectory = options
            .getStringOption(WebServerConstants.optDataDirectory);
        if (usersFileDirectory == null)
        {
            LOGGER
                .severe("Data Directory (for chat messages file) is null! Define it in cf file!");
            System.exit(1);
        }
        String filename = "Chatmessages-" + getChannel().getChannelId()
            + ".txt";

        try
        {
            BufferedReader msgs = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(usersFileDirectory, filename)),
                WebServerConstants.charset));

            String line = null;
            while ((line = msgs.readLine()) != null)
            {
                if (line.matches("\\s*"))
                {
                    // ignore empty line
                }
                else
                {
                    parseMsgLine(line);
                }
            }
            msgs.close();
            LOGGER.info("Restored " + lastNChatMessages.size()
                + " messages from file.");
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Chat messages file " + filename
                + " not found!", e);
            System.exit(1);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE,
                "IOException while reading chat messages file " + filename
                    + "!", e);
            System.exit(1);
        }
    }

    public String makeLine(ChatMessage msg)
    {
        return msg.getChatId() + SEP + msg.getWhen() + SEP + msg.getSender()
            + SEP + msg.getMessage();
    }

    private void parseMsgLine(String line)
    {
        String[] tokens = line.split(SEP, 4);
        if (tokens.length != 4)
        {
            LOGGER.log(Level.WARNING, "invalid line '" + line
                + "' in chat messages file!");
            return;
        }
        String chatId = tokens[0].trim();
        long when = Long.parseLong(tokens[1].trim());
        String sender = tokens[2].trim();
        String text = tokens[3].trim();

        ChatMessage msg = new ChatMessage(chatId, when, sender, text);
        storeMessage(msg);
    }
}
