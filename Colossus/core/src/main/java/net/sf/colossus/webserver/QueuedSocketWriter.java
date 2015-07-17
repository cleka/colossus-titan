package net.sf.colossus.webserver;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


class QueuedSocketWriter extends Thread
{
    static final Logger LOGGER = Logger.getLogger(QueuedSocketWriter.class
        .getName());

    private final static String MSG_EXIT_LOOP = "_EXIT_LOOP";
    private final static String MSG_FLUSH_MSGS = "_FLUSH_MESSAGES";

    /**
     * The actual queue holding all messages that need to be sent.
     * This is a concurrent-safe queue.
     */
    private final LinkedBlockingQueue<String> queue;

    /**
     * The actual writer object which will send printed data over the socket.
     */
    PrintWriter out;

    /**
     * Sending thread that requests the flushing, waits on this mutex until
     * notified that the flush was completed (boolean 'flushed' below set to true).
     */
    private final Object flushMutex = new Object();

    /**
     * Set to true when flushing is completed.
     */
    private boolean flushed;

    private boolean done = false;

    private static int instanceIdCounter = 0;

    private final int instanceId;

    public QueuedSocketWriter(Socket socket) throws IOException
    {
        this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
            socket.getOutputStream())), true);
        this.queue = new LinkedBlockingQueue<String>();
        this.instanceId = ++instanceIdCounter;
        LOGGER.info("QueuedSocketWriter #" + instanceId + " created.");
    }

    @Override
    public void run()
    {
        while (!done)
        {
            String message = readNextFromQueue();
            if (message == null)
            {
                LOGGER.warning("Got null message from queue?");
                continue;
            }
            if (message.equals(MSG_EXIT_LOOP))
            {
                LOGGER.info("QueuedSocketWriter #" + instanceId
                    + ": Got MSG_EXIT_LOOP.");
                break;
            }
            if (message.equals(MSG_FLUSH_MSGS))
            {
                LOGGER.info("QueuedSocketWriter #" + instanceId
                    + ": reached the FLUSH marker.");
                synchronized (flushMutex)
                {
                    flushed = true;
                    LOGGER.info("QueuedSocketWriter #" + instanceId
                        + ": notifying flusher.");
                    flushMutex.notify();
                }
            }
            else
            {
                long sentTime = new Date().getTime();
                out.println(message);
                long spentTime = new Date().getTime() - sentTime;
                String logMsg = "QueuedSocketWriter #" + instanceId
                    + ": actual writing took " + spentTime
                    + " ms for message: " + message;
                if (spentTime > 500)
                {
                    LOGGER.info("NOTE: " + logMsg);
                }
                else
                {
                    // LOGGER.fine(logMsg);
                }
            }
        }
        LOGGER.info("QueuedSocketWriter #" + instanceId + " after main loop.");
    }

    /**
     * Enqueues a flush marker and waits on the mutex until the flushing of all
     * messages enqueued prior to the marker have been sent.
     * (this does at the moment not imply that the client has received them
     * (not to mention even has processed them).
     */
    public void flushMessages()
    {
        synchronized (flushMutex)
        {
            sendMessage(MSG_FLUSH_MSGS);
            try
            {
                flushed = false;
                while (!flushed)
                {
                    flushMutex.wait();
                }
            }
            catch (InterruptedException e)
            {
                LOGGER.warning("flushMessages waiting interrupted?");
            }
        }
    }

    public void stopWriter()
    {
        done = true;
        sendMessage(MSG_EXIT_LOOP);
    }

    public void sendMessage(String message)
    {
        queue.add(message);
    }

    /**
     * We use no timeout while waiting for next message in the queue.
     * To get it out of the loop, we enqueue a special marker (MSG_EXIT_LOOP).
     * @return String containing the next message to write.
     */
    private String readNextFromQueue()
    {
        try
        {
            String message = queue.take();
            return message;
        }
        catch (InterruptedException e)
        {
            LOGGER.warning("queue.poll interrupted!");
        }

        return null;
    }

}
