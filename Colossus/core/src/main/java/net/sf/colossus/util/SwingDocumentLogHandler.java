package net.sf.colossus.util;


import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(SwingDocumentLogHandler.class.getName());

    private Document document = new PlainDocument();

    public SwingDocumentLogHandler()
    {
        super();
    }

    public Document getDocument()
    {
        return document;
    }

    public void publish(LogRecord record) {
        try
        {
            document.insertString(document.getLength(),
                record.getMessage() + "\n", null);
        }
        catch (BadLocationException e)
        {
            LOGGER.log(Level.SEVERE, "append() call failed on document.", e);
        }
    }

    public void flush() {
        // nothing to do, just making the method concrete
    }

    public void close()
        throws SecurityException {
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
