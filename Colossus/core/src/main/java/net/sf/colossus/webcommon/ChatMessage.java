/**
 *
 */
package net.sf.colossus.webcommon;


public class ChatMessage
{
    String chatId;
    long when;
    String sender;
    String message;

    public ChatMessage(String chatId, long when, String sender, String message)
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
