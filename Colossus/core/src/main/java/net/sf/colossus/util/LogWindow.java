package net.sf.colossus.util;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.SaveWindow;


/** 
 *  Simple log window
 *  @version $Id$
 *  @author David Ripton 
 */
public final class LogWindow extends JTextArea
{
    private JFrame logFrame;
    private JScrollPane scrollPane;
    private Client client;
    private Point location;
    private Dimension size;
    private SaveWindow saveWindow;
    private final Logger logger;
    private SwingDocumentLogHandler handler;

    public LogWindow(Client client, Logger logger)
    {
        this.client = client;
        this.logger = logger;
        setEditable(false);
        setBackground(Color.white);

        logFrame = new JFrame("Log Window");
        logFrame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                LogWindow.this.client.setOption(Options.showLogWindow, false);
            }
        });

        scrollPane = new JScrollPane(this);
        logFrame.getContentPane().add(scrollPane);
        logFrame.pack();

        saveWindow = new SaveWindow(client, "LogWindow");

        size = saveWindow.loadSize();
        if (size == null)
        {
            size = getMinimumSize();
        }
        logFrame.setSize(size);

        location = saveWindow.loadLocation();
        if (location == null)
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            int y = d.height - size.height;
            location = new Point(0, y);
        }
        logFrame.setLocation(location);

        logFrame.setVisible(true);

        handler = new SwingDocumentLogHandler();
        logger.addHandler(handler);
        setDocument(handler.getDocument());
    }

    public void append(String s)
    {
        super.append(s);

        // XXX Removed because of graphical corruption
        // scrollToEnd();   
    }

    void scrollToEnd()
    {
        JScrollBar vert = scrollPane.getVerticalScrollBar();
        vert.setValue(vert.getMaximum());
        repaint();
    }

    public Dimension getMinimumSize()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Dimension(Math.min(d.width, 800), 100);
    }

    public void dispose()
    {
        saveWindow.save(logFrame);
        logFrame.dispose();
        logger.removeHandler(handler);
        client = null;
    }
}
