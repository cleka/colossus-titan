package net.sf.colossus.server;


import net.sf.colossus.common.Constants;


/**
 * Encapsulates one message that was sent to client, together with the
 * last commitNumber and the message number (relative to the commit).
 */
class MessageForClient
{
    private static int MAX_PRINT_LEN = 100;

    private final int messageNumber;
    private final int commitNumber;
    private final String message;
    private final String method;

    public MessageForClient(int messageNr, int commitNr, String message)
    {
        this.messageNumber = messageNr;
        this.commitNumber = commitNr;
        this.message = message;
        int i = message.indexOf(Constants.protocolTermSeparator);
        if (i == -1)
        {
            this.method = message;
        }
        else
        {
            this.method = message.substring(0, i);
        }
    }

    /** Clone from another message in queue, but rewrite the message
     *  number because in re-sending they are different.
     * @param original The original MessageForClient to clone from
     * @param newMsgNumber the MessageNumber to used instead
     */
    public MessageForClient(MessageForClient original, int newMsgNumber)
    {
        this.messageNumber = newMsgNumber;
        this.commitNumber = original.commitNumber;
        this.message = original.message;
        this.method = original.method;
    }

    public int getMessageNr()
    {
        return messageNumber;
    }

    public int getCommitNumber()
    {
        return commitNumber;
    }

    public String getMessage()
    {
        return message;
    }

    public String getShortenedMessage()
    {
        String shortMessage;
        if (message.length() < MAX_PRINT_LEN)
        {
            shortMessage = message;
        }
        else
        {
            shortMessage = message.substring(0, MAX_PRINT_LEN);
        }
        return shortMessage;
    }

    public String getMethod()
    {
        return method;
    }
}
