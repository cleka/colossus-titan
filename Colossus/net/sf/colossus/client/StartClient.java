package net.sf.colossus.client;


import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.werken.opt.Options;
import com.werken.opt.Option;
import com.werken.opt.CommandLine;

import net.sf.colossus.server.Constants;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.KDialog;


/**
 *  Startup code for network Client
 *  @version $Id$
 *  @author David Ripton
 */


public class StartClient extends KDialog implements WindowListener,
    ActionListener
{
    String playerName;
    String hostname;
    int port;

    JComboBox nameBox; 
    JComboBox hostBox; 
    JComboBox portBox; 


    public StartClient(String playerName, String hostname, int port)
    {
        super(new JFrame(), "Client startup options", false);
        getContentPane().setLayout(new GridLayout(0, 2));

        this.playerName = playerName;
        this.hostname = hostname;
        this.port = port;

        getContentPane().add(new JLabel("Player name"));
        Set nameChoices = new TreeSet();
        nameChoices.add(playerName);
        nameChoices.add(Constants.username);
        nameBox = new JComboBox(new Vector(nameChoices));
        nameBox.setEditable(true);
        nameBox.addActionListener(this);
        getContentPane().add(nameBox);

        getContentPane().add(new JLabel("Server hostname"));
        Set hostChoices = new TreeSet();
        hostChoices.add(hostname);
        hostChoices.add(Constants.localhost);
        hostBox = new JComboBox(new Vector(hostChoices));
        hostBox.setEditable(true);
        hostBox.addActionListener(this);
        getContentPane().add(hostBox);

        getContentPane().add(new JLabel("Server port"));
        Set portChoices = new TreeSet();
        portChoices.add("" + port);
        portChoices.add("" + Constants.defaultPort);
        portBox = new JComboBox(new Vector(portChoices));
        portBox.setEditable(true);
        portBox.addActionListener(this);
        getContentPane().add(portBox);

        JButton goButton = new JButton("Go");
        goButton.addActionListener(this);
        getContentPane().add(goButton);

        JButton quitButton = new JButton(Constants.quit);
        quitButton.addActionListener(this);
        getContentPane().add(quitButton);

        addWindowListener(this);
        pack();
        centerOnScreen();
        setVisible(true);
    }

    public Dimension getMinimumSize()
    {
        return new Dimension(300, 200);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(Constants.quit))
        {
            dispose();
            System.exit(0);
        }
        else if (e.getActionCommand().equals("Go"))
        {
            dispose();
            connect(playerName, hostname, port);
        }
        else  // A combo box was changed.
        {
            Object source = e.getSource();
            if (source == nameBox)
            {
                playerName = (String)nameBox.getSelectedItem();
            }
            else if (source == hostBox)
            {
                hostname = (String)hostBox.getSelectedItem();
            }
            else if (source == portBox)
            {
                port = Integer.parseInt((String)portBox.getSelectedItem());
            }
        }
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
        System.exit(0);
    }


    public static void connect(String playerName, String hostname, int port)
    {
        new Client(hostname, port, playerName, true);
    }


    private static void usage(Options opts)
    {
        Log.event("Usage: java net.sf.colossus.client.StartClient [options]");
        Iterator it = opts.getOptions().iterator();
        while (it.hasNext())
        {
            Option opt = (Option)it.next();
            Log.event(opt.toString());
        }
    }
}
