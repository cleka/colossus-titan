package net.sf.colossus.util;


import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;


/**
 * A java.util.logging Handler that appends to a <code>javax.swing.text.Document</code>.
 *
 * @author Barrie Treloar
 * @author Peter Becker
 */
public class SwingDocumentLogHandler extends Handler
{
    private static final Logger LOGGER = Logger
        .getLogger(SwingDocumentLogHandler.class.getName());

    private Document document = new PlainDocument();
    private final JTextArea textArea;

    public SwingDocumentLogHandler(JTextArea area)
    {
        super();
        this.textArea = area;
    }

    public Document getDocument()
    {
        return document;
    }

    @Override
    public void publish(final LogRecord record)
    {
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                try
                {
                    final String message = TimeFormats.getCurrentTime24h()
                        + ": " + record.getMessage() + "\n";

                    document.insertString(document.getLength(), message, null);
                    textArea.setCaretPosition(document.getLength() - 1);
                }
                catch (BadLocationException e)
                {
                    LOGGER.log(Level.SEVERE,
                        "append() call failed on document.", e);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    @Override
    public void flush()
    {
        // nothing to do, just making the method concrete
    }

    @Override
    public void close() throws SecurityException
    {
        try
        {
            document.remove(0, document.getLength());
            document = null;
        }
        catch (BadLocationException e)
        {
            LOGGER.log(Level.SEVERE, "close() call failed on document.", e);
        }
    }
}
