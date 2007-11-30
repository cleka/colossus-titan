package net.sf.colossus.client;


import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;


/**
 *  Startup code for network Client
 *  @version $Id$
 *  @author David Ripton
 */


public class StartClient extends KDialog implements WindowListener,
            ActionListener
{
	private static final Logger LOGGER = Logger.getLogger(StartClient.class.getName());

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
        String preferred = hostname;
        try
        {
            InetAddress ia = InetAddress.getLocalHost();
            hostChoices.add(ia.getHostName());
            preferred = ia.getHostName();
        }
        catch (UnknownHostException ex)
        {
            LOGGER.log(Level.SEVERE, ex.toString(), (Throwable)null);
        }
        
        loadClientOptions();
        // LRU, i.e. serverName0 is the one last time used.
        // Combobox will display them alphabetically anyway,
        // so make at least the last-used-one be preselected.
        for (int i = Constants.numSavedServerNames; i > 0 ; i--)
        {
            String serverName = clientOptions.getStringOption(
                    net.sf.colossus.util.Options.serverName + (i-1) );
            if (serverName != null)
            {
                preferred = serverName;
                hostChoices.add(serverName);
            }
        }
        hostBox = new JComboBox(new Vector(hostChoices));
        hostBox.setEditable(true);
        hostBox.setSelectedItem(preferred);
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
        saveHostnames();
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

    /** Put the choosen hostname as first to the LRU sorted list.
     *  Save the list back to the options. 
     */
    private static void saveHostnames()
    {
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
                    names.add(serverName);
                }
            }
        }
        for (int i = 0; i < names.size() && 
                i < Constants.numSavedServerNames ; i++)
        {
            clientOptions.setOption(net.sf.colossus.util.Options.serverName +
                    i, (String)names.get(i));
        }
        clientOptions.saveOptions();
    }
}
