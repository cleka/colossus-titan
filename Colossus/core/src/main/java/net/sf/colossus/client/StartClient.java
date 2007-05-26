package net.sf.colossus.client;


import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Log;


/**
 *  Startup code for network Client
 *  @version $Id$
 *  @author David Ripton
 */


public class StartClient extends KDialog implements WindowListener,
            ActionListener
{
    static String playerName;
    static String hostname;
    int port;
    static net.sf.colossus.util.Options clientOptions;
    SaveWindow saveWindow;

    JComboBox nameBox;
    JComboBox hostBox;
    JComboBox portBox;

    public StartClient(String playerName, String hostname, int port)
    {
        super(new JFrame(), "Client startup options", false);
        getContentPane().setLayout(new GridLayout(0, 2));

        StartClient.playerName = playerName;
        StartClient.hostname = hostname;
        this.port = port;

        getContentPane().add(new JLabel("Player name"));
        Set nameChoices = new TreeSet();
        nameChoices.add(playerName);
        nameChoices.add(Constants.username);
        nameBox = new JComboBox(new Vector(nameChoices));
        nameBox.setEditable(true);
        nameBox.addActionListener(this);
        nameBox.setSelectedItem(playerName);
        getContentPane().add(nameBox);

        getContentPane().add(new JLabel("Server hostname"));
        Set hostChoices = new TreeSet();
        hostChoices.add(hostname);
        try
        {
            InetAddress ia = InetAddress.getLocalHost();
            hostChoices.add(ia.getHostName());
        }
        catch (UnknownHostException ex)
        {
            Log.error(ex.toString());
        }
        loadClientOptions();
        for (int i = 0; i < Constants.numSavedServerNames; i++)
        {
            String serverName = clientOptions.getStringOption(
                    net.sf.colossus.util.Options.serverName + i);
            if (serverName != null)
            {
                hostChoices.add(serverName);
            }
        }
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
        saveWindow = new SaveWindow(clientOptions, "StartClient");
        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(location);
        }
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
            saveWindow.saveLocation(getLocation());
            connect(playerName, hostname, port);
        }
        else  // A combo box was changed.
        {
            Object source = e.getSource();
            if (source == nameBox)
            {
                playerName = (String)nameBox.getSelectedItem();
                loadClientOptions();
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
        saveHostname();
        if ( clientOptions == null)
        {
            // needed e.g. when started as standalone from cmdline with -c -g
            clientOptions = new net.sf.colossus.util.Options(playerName);
            clientOptions.loadOptions();
        }
        new Client(hostname, port, playerName, true);
    }

    private void loadClientOptions()
    {
        clientOptions = new net.sf.colossus.util.Options(playerName);
        clientOptions.loadOptions();
    }

    /** Save the chosen hostname as an option.  LRU sort saved hostnames. */
    private static void saveHostname()
    {
        int highestNum = -1;
        List names = new ArrayList();
        names.add(hostname);
        for (int i = 0; i < Constants.numSavedServerNames; i++)
        {
            if (clientOptions == null)
            {
                return;
            }
            String serverName = clientOptions.getStringOption(
                    net.sf.colossus.util.Options.serverName + i);
            if (serverName != null)
            {
                if (!serverName.equals(hostname))
                {
                    // Don't add it twice.
                    highestNum = i + 1;
                    names.add(serverName);
                }
            }
        }
        for (int i = 0; i <= highestNum; i++)
        {
            clientOptions.setOption(net.sf.colossus.util.Options.serverName +
                    i, (String)names.get(i));
        }
        clientOptions.saveOptions();
    }
}
