package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import net.sf.colossus.common.Options;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.util.TimeFormats;


/**
 * Log window for connection issues
 * Based on simple log window
 *
 * @author David Ripton
 * @author Clemens Katzer
 */
public class ConnectionLogWindow extends KFrame
{
    private final static String CL_WINDOW_TITLE = "Connection Log Window";

    private final JScrollPane scrollPane;
    private final Options options;
    private final Document document;
    private final JTextArea textArea;

    public ConnectionLogWindow(Options options)
    {
        super(CL_WINDOW_TITLE);
        this.options = options;

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                ConnectionLogWindow.this.options.setOption(
                    Options.showConnectionLogWindow, false);
            }
        });

        document = new PlainDocument();
        this.textArea = new JTextArea(document);
        textArea.setEditable(false);
        textArea.setBackground(Color.white);

        scrollPane = new JScrollPane(textArea);
        getContentPane().add(scrollPane);

        setSize(getMinimumSize());
        setPreferredSize(getMinimumSize());
        useSaveWindow(options, CL_WINDOW_TITLE, null);

        pack();
        setVisible(true);
    }

    public static String getNow()
    {
        return new Date().toString();
    }

    public void append(String s)
    {
        String currentTime = TimeFormats.getCurrentTime24h();
        textArea.append(currentTime + ": " + s + "\n");
        toFront();

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
        return new Dimension(Math.min(d.width, 800), 200);
    }

}
