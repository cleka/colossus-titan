package net.sf.colossus.util;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
    }


    public void append(String s)
    {
        super.append(s);
        scrollToEnd();
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
        size = logFrame.getSize();
        saveWindow.saveSize(size);
        location = logFrame.getLocation();
        saveWindow.saveLocation(location);
        logFrame.dispose();
        client.setOption(Options.showLogWindow, false);
    }
}
