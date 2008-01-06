package net.sf.colossus.server;


import java.util.logging.Handler;
import java.util.logging.LogRecord;


/**
 * A java.util.logging Handler that appends to a <code>Server</code> via <code>allLog</code>.
 */
public class RemoteLogHandler extends Handler
{
    private Server server = null;

    public void setServer(Server server)
    {
        this.server = server;
    }

    public RemoteLogHandler()
    {
        super();
        net.sf.colossus.webcommon.InstanceTracker.register(this,
            "TheServerRLH");
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
        if (server != null)
        {
            server.allLog(record.toString());
        }
    }

    @Override
    public void flush()
    {
        // nothing to do
    }
}
