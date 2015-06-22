package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.IClient;
import net.sf.colossus.common.Constants;


public class MessageRecorder
{
    private static final Logger LOGGER = Logger
        .getLogger(MessageRecorder.class.getName());

    ArrayList<Message> messages;

    public MessageRecorder()
    {
        LOGGER.log(Level.INFO, "MessageRecorder instantiated");

        messages = new ArrayList<Message>();
    }

    public void recordMessageToClient(IClient client, String messageText)
    {
        String[] tokens = messageText.split(" ~ ");
        String command = tokens[0];
        if (command.equals(Constants.pingRequest)
            || command.equals(Constants.serverConnectionOK))
        {
            return;
        }

        Message message = new Message(client, messageText, command);
        messages.add(message);
    }

    public void printMessagesToConsole(IClient client)
    {
        System.out.println("\n");
        System.out.println("BEGIN messages");
        System.out.println("==============");
        for (Message message : messages)
        {
            if (message.getClient().equals(client))
            {
                String messageText = message.getText();
                String shortText = messageText;
                if (messageText.length() > 500)
                {
                    shortText = messageText.substring(0, 497) + "...";
                }
                System.out.println(shortText);
            }
        }
        System.out.println("END   messages\n\n");
    }

    private class Message
    {
        private final IClient client;

        private final String message;

        private final String command;

        public Message(IClient client, String message, String command)
        {
            this.client = client;
            this.message = message;
            this.command = command;
        }

        public IClient getClient()
        {
            return client;
        }

        public String getText()
        {
            return message;
        }

        @SuppressWarnings("unused")
        public String getCommand()
        {
            return command;
        }

    }

}
