package net.sf.colossus.util;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.sf.colossus.client.Client;


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
    private static Point location;
    private static Dimension size;


    public LogWindow(Client client)
    {
        this.client = client;
        setEditable(false);
        setBackground(Color.white);

        logFrame = new JFrame("Log Window");
        logFrame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                LogWindow.this.dispose();
            }
        });

        scrollPane = new JScrollPane(this);
        logFrame.getContentPane().add(scrollPane);
        logFrame.pack();

        loadSize();
        if (size == null)
        {
            size = getMinimumSize();
        }
        logFrame.setSize(size);

        loadLocation();
        if (location == null)
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            int y = d.height - size.height;
            location = new Point(0, y);
        }
        logFrame.setLocation(location);

        logFrame.setVisible(true);
    }


    public void append(String s)
    {
        super.append(s);
        JScrollBar vert = scrollPane.getVerticalScrollBar();
        vert.setValue(vert.getMaximum());
    }

    public Dimension getMinimumSize()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Dimension(Math.min(d.width, 800), 100);
    }


    public void dispose()
    {
        saveSize();
        saveLocation();
        logFrame.dispose();
        client.setOption(Options.showLogWindow, false);
    }


    private void loadSize()
    {
        int x = client.getIntOption(Options.logWindowSizeX);
        int y = client.getIntOption(Options.logWindowSizeY);
        size = new Dimension(x, y);
    }

    private void saveSize()
    {
        size = logFrame.getSize();
        client.setOption(Options.logWindowSizeX, (int)size.getWidth());
        client.setOption(Options.logWindowSizeY, (int)size.getHeight());
    }

    private void loadLocation()
    {
        int x = client.getIntOption(Options.logWindowLocX);
        int y = client.getIntOption(Options.logWindowLocY);
        if (x >= 0 && y >= 0)
        {
            location = new Point(x, y);
        }
    }

    private void saveLocation()
    {
        location = logFrame.getLocation();
        client.setOption(Options.logWindowLocX, location.x);
        client.setOption(Options.logWindowLocY, location.y);
    }
}
