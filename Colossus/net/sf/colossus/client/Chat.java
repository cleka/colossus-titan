package net.sf.colossus.client;


import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.sf.colossus.util.Options;

/** 
 *  Simple chat window
 *  @version $Id$
 *  @author David Ripton 
 */
final class Chat extends JFrame implements ActionListener
{
    private Client client;
    private Point location;
    private Dimension size;
    private SaveWindow saveWindow;

    private JScrollPane receiveScrollPane;
    private JScrollPane sendScrollPane;
    private JTextArea receive;
    private JTextArea send;

    private ButtonGroup buttonGroup;
    /** List of RadioButtons */
    private java.util.List buttons;


    Chat(Client client)
    {
        super("Chat");
        this.client = client;

        setBackground(Color.white);

        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                dispose();
            }
        });

        receive = new JTextArea();
        receive.setEditable(false);
        receive.setLineWrap(true);
        receiveScrollPane = new JScrollPane(receive);
        getContentPane().add(receiveScrollPane);

        send = new JTextArea();
        receive.setEditable(true);
        receive.setLineWrap(true);
        sendScrollPane = new JScrollPane(send);
        getContentPane().add(sendScrollPane);

        // All players except self, plus "all"
        buttons = new ArrayList();
        java.util.List names = client.getPlayerNames();
        names.add(0, "All");
        names.remove(client.getPlayerName());
        Iterator it = names.iterator();
        buttonGroup = new ButtonGroup();
        Container buttonPane = new Container();
        getContentPane().add(buttonPane);
        while (it.hasNext())
        {
            String name = (String)it.next();
            JRadioButton button = new JRadioButton(name);
            buttonPane.add(button);
            buttonGroup.add(button);
            button.addActionListener(this);
        }

        pack();

        saveWindow = new SaveWindow(client, "Chat");

        size = saveWindow.loadSize();
        if (size == null)
        {
            size = getMinimumSize();
        }
        setSize(size);

        location = saveWindow.loadLocation();
        if (location == null)
        {
            // Middle of right screen edge.
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            int y = (d.height - size.height) / 2;
            int x = (d.width - size.width);
            location = new Point(x, y);
        }
        setLocation(location);

        setVisible(true);
    }


    void append(String s)
    {
        receive.append(s);
        JScrollBar vert = receiveScrollPane.getVerticalScrollBar();
        vert.setValue(vert.getMaximum());
    }

    void send()
    {
        String target = buttonGroup.getSelection().getActionCommand();
    }

    public Dimension getMinimumSize()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Dimension(Math.min(d.width, 800), 100);
    }


    public void dispose()
    {
        size = getSize();
        saveWindow.saveSize(size);
        location = getLocation();
        saveWindow.saveLocation(location);
        super.dispose();
        client.setOption(Options.showChat, false);
    }


    public void actionPerformed(ActionEvent e)
    {
    }
}
