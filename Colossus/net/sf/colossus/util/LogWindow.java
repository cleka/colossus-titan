package net.sf.colossus.util;


import javax.swing.*;
import java.awt.*;


/** 
 *  Simple log window
 *  @version $Id$
 *  @author David Ripton 
 */
public final class LogWindow extends JTextArea
{
    private JFrame logFrame;
    private JScrollPane scrollPane;

    public LogWindow()
    {
        setBackground(Color.white);
        logFrame = new JFrame("Log Window");
        scrollPane = new JScrollPane(this);
        logFrame.getContentPane().add(scrollPane);
        logFrame.pack();
        logFrame.setSize(getMinimumSize());
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int y = d.height - getMinimumSize().height;
        logFrame.setLocation(new Point(0, y));
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
        return new Dimension(640, 100);
    }


    public void dispose()
    {
        logFrame.dispose();
    }
}
