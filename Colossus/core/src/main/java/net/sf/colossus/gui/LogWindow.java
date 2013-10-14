package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.colossus.common.Options;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.util.SwingDocumentLogHandler;


/**
 * Simple log window
 *
 * @author David Ripton
 */
public final class LogWindow extends JTextArea
{
    private final JFrame logFrame;
    private final JScrollPane scrollPane;
    private final Options options;
    private Point location;
    private Dimension size;
    private final SaveWindow saveWindow;
    private final Logger logger;
    private final SwingDocumentLogHandler handler;

    public LogWindow(Options options, Logger logger, boolean showInitially)
    {
        this.options = options;
        this.logger = logger;
        setEditable(false);
        setBackground(Color.white);

        logFrame = new JFrame("Log Window");
        logFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                clearAllText();
                LogWindow.this.options.setOption(Options.showLogWindow, false);
            }
        });

        scrollPane = new JScrollPane(this);
        logFrame.getContentPane().add(scrollPane);
        logFrame.pack();

        saveWindow = new SaveWindow(options, "LogWindow");

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
        handler = new SwingDocumentLogHandler(this);
        logger.addHandler(handler);
        setDocument(handler.getDocument());

        logFrame.setVisible(showInitially);
    }

    @Override
    public void append(String s)
    {
        super.append(s);
        this.setCaretPosition(getDocument().getLength() - 1);
    }

    public void clearAllText()
    {
        this.setText("\nAll text before here was wiped out "
            + "when LogWindow was closed.\n\n");
    }

    @Override
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
    }

    @Override
    public void setVisible(boolean show)
    {
        logFrame.setVisible(show);
    }
}
