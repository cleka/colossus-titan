package net.sf.colossus.server;


/**
 * Encapsulates one message that was sent to client, together with the
 * last commitNumber and the message number (relative to the commit).
 */
class MessageForClient
{
    private final int messageNumber;
    private final int commitNumber;
    private final String message;

    public MessageForClient(int messageNr, int commitNr, String message)
    {
        this.messageNumber = messageNr;
        this.commitNumber = commitNr;
        this.message = message;
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
}
