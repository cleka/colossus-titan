package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import net.sf.colossus.common.Options;
import net.sf.colossus.guiutil.SaveWindow;


/**
 * Log window for connection issues
 * Based on simple log window
 *
 * @author David Ripton
 * @author Clemens Katzer
 */
public class ConnectionLogWindow extends JTextArea
{
    private final JFrame logFrame;
    private final JScrollPane scrollPane;
    private final Options options;
    private Point location;
    private Dimension size;
    private final SaveWindow saveWindow;
    private final Document document;

    public ConnectionLogWindow(Options options)
    {
        this.options = options;
        setEditable(false);
        setBackground(Color.white);

        logFrame = new JFrame("Connection Log Window");
        logFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                ConnectionLogWindow.this.options.setOption(
                    Options.showLogWindow, false);
            }
        });

        scrollPane = new JScrollPane(this);
        logFrame.getContentPane().add(scrollPane);
        logFrame.pack();

        saveWindow = new SaveWindow(options, "ConnectionLogWindow");

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

        document = new PlainDocument();
        setDocument(document);
    }

    public static String getNow()
    {
        return new Date().toString();
    }

    @Override
    public void append(String s)
    {
        super.append(getNow() + ": " + s + "\n");
        logFrame.toFront();

        // XXX Removed because of graphical corruption
        // scrollToEnd();
    }

    void scrollToEnd()
    {
        JScrollBar vert = scrollPane.getVerticalScrollBar();
        vert.setValue(vert.getMaximum());
        repaint();
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
    }
}
