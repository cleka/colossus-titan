package net.sf.colossus.server;


import java.util.logging.Handler;
import java.util.logging.LogRecord;


/**
 * A java.util.logging Handler that appends to a <code>Server</code> via
 * <code>allLog</code>.
 *
 * Clemens: this is totally odd. Above it says it appends to a server, but
 * in practice it is used in a way that any thing logged on server is
 * transmitted to all remote clients, and even in wrong way; see below...
 */
public class RemoteLogHandler extends Handler
{
    // Right now taken out of use, because it does not work anyway
    // 1) if there is 3 remote clients, each remote logger sends to all three
    //    remote clients
    // 2) serialization does not work (any more?) - remote client shows merely
    //    LogRecord@123456 instead of something meaningful
    // 3) When client connection gets lost, hassle with the isGone and logging
    //    of related information (still tried to send to client).

    private final static boolean PUBLISH_TO_REMOTE_LOGGERS = false;

    private Server server = null;

    public RemoteLogHandler(Server server)
    {
        super();
        this.server = server;
        net.sf.colossus.util.InstanceTracker.register(this, "TheServerRLH");
    }

    public boolean requiresLayout()
    {
        return true;
    }

    @Override
    public void close()
    {
        server = null;
    }

    @Override
    public void publish(LogRecord record)
    {
        if (PUBLISH_TO_REMOTE_LOGGERS)
        {
            if (server != null)
            {
                server.allLog(record.toString());
            }
        }
    }

    @Override
    public void flush()
    {
        // nothing to do
    }
}
