package net.sf.colossus.client;


import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import javax.swing.JLabel;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Start;
import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.Options;


/**
 *  Startup code for network Client
 *  @version $Id$
 *  @author David Ripton
 */

public class StartClient extends KFrame implements WindowListener,
    ActionListener
{
    private static final Logger LOGGER = Logger.getLogger(StartClient.class
        .getName());

    private Object mutex;
    private Options netclientOptions;
    private Options stOptions;
    private Start startObject;

    private String playerName;
    private String hostname;
    private int port;
    private SaveWindow saveWindow;

    private JComboBox nameBox;
    private JComboBox hostBox;
    private JComboBox portBox;

    public StartClient(Object mutex, Start startObject)
    {
        super("Client startup options");
        getContentPane().setLayout(new GridLayout(0, 2));

        net.sf.colossus.webcommon.FinalizeManager.register(this, "only one");

        this.mutex = mutex;
        this.startObject = startObject;
        this.stOptions = startObject.getStartOptions();

        // player, preferred host (or null) and port from main() / cmdline 
        this.playerName = stOptions.getStringOption(Options.runClientPlayer);
        this.hostname = stOptions.getStringOption(Options.runClientHost);
        this.port = stOptions.getIntOption(Options.runClientPort);

        // LRU list of hsots, and window geometry from NetClient cf file
        netclientOptions = new Options(Constants.optionsNetClientName);
        netclientOptions.loadOptions();

        Container panel = getContentPane();

        panel.add(new JLabel("Player name"));
        Set nameChoices = new TreeSet();
        nameChoices.add(playerName);
        nameChoices.add(Constants.username);
        nameBox = new JComboBox(new Vector(nameChoices));
        nameBox.setEditable(true);
        nameBox.addActionListener(this);
        nameBox.setSelectedItem(playerName);
        panel.add(nameBox);

        panel.add(new JLabel("Server hostname"));
        Set hostChoices = new TreeSet();
        String preferred = initServerNames(hostname, hostChoices,
            netclientOptions);
        this.hostname = preferred;
        hostBox = new JComboBox(new Vector(hostChoices));
        hostBox.setEditable(true);
        hostBox.setSelectedItem(preferred);
        hostBox.addActionListener(this);
        panel.add(hostBox);

        panel.add(new JLabel("Server port"));
        Set portChoices = new TreeSet();
        portChoices.add("" + port);
        portChoices.add("" + Constants.defaultPort);
        portBox = new JComboBox(new Vector(portChoices));
        portBox.setEditable(true);
        portBox.addActionListener(this);
        panel.add(portBox);

        JButton goButton = new JButton("Go");
        goButton.addActionListener(this);
        panel.add(goButton);

        JButton quitButton = new JButton(Constants.quitGame);
        quitButton.addActionListener(this);
        panel.add(quitButton);

        addWindowListener(this);
        pack();
        saveWindow = new SaveWindow(netclientOptions, "StartClient");
        saveWindow.restoreOrCenter(this);

        setVisible(true);
    }

    /* Public and static for Start.java.
     * Initializes the hostChoices set for the ComboBox with
     * - current running host name and IP
     * - wantedHost hostname got as parameter (got from cmdline), 
     *   can be null
     * - LRU list from cf file
     * Returns the "preferred" servername, i.e. the one that shall be
     * set as preselected item in the hostbox. The preferred one is 
     * server name the one given as parameter, or if none, then the 
     * one last used from LRU list (or if even that is empty, defaults
     * to current hostname).
     */
    public static String initServerNames(String wantedHost, Set hostChoices,
        Options netclientOptions)
    {
        String preferred = null;
        try
        {
            InetAddress ia = InetAddress.getLocalHost();
            String hostAddr = ia.getHostAddress();
            if (hostAddr != null)
            {
                hostChoices.add(hostAddr);
                preferred = ia.getHostAddress();
            }
            String hostName = ia.getHostName();
            if (hostName != null)
            {
                hostChoices.add(hostName);
                preferred = ia.getHostName();
            }
        }
        catch (UnknownHostException ex)
        {
            LOGGER.log(Level.SEVERE, "Can not resolve host", ex);
        }

        // LRU, i.e. serverName0 is the one last time used,
        // that's why we go backwards.
        // Combobox will display them alphabetically anyway,
        // so make at least the last-used-one be preselected.
        for (int i = Constants.numSavedServerNames - 1; i >= 0; i--)
        {
            String serverName = netclientOptions
                .getStringOption(Options.serverName + i);
            if (serverName != null)
            {
                hostChoices.add(serverName);
                preferred = serverName;
            }
        }

        if (wantedHost != null && !wantedHost.equals(""))
        {
            // given one overrides all others: add it and make it preferred:
            hostChoices.add(wantedHost);
            preferred = wantedHost;
        }
        // Just as paranoid fail-safe, should never happen:
        else if (preferred == null)
        {
            preferred = "localhost";
        }
        else
        {
            // no name given - default to what we otherwise decided
            // to be the "preferred" one.
            // No asignment here - caller will assing his "hostname" 
            // be become what we give back
        }

        return preferred;
    }

    public Dimension getMinimumSize()
    {
        return new Dimension(350, 200);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(Constants.quitGame))
        {
            startObject.setWhatToDoNext(Start.QuitAll);
            dispose();
        }
        else if (e.getActionCommand().equals("Go"))
        {
            doRunNetClient();
        }
        else
        // A combo box was changed.
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
        startObject.setWhatToDoNext(Start.GetPlayersDialog);
        dispose();
    }

    public void dispose()
    {
        // Dispose dialog and notify main() so that game starts:
        super.dispose();
        synchronized (mutex)
        {
            mutex.notify();
        }
    }

    private void doRunNetClient()
    {
        stOptions.setOption(Options.runClientPlayer, playerName);
        stOptions.setOption(Options.runClientHost, hostname);
        stOptions.setOption(Options.runClientPort, port);

        // prepend used hostname to LRU list
        saveHostname(netclientOptions);
        saveWindow.save(this);
        netclientOptions.saveOptions();

        startObject.setWhatToDoNext(Start.StartNetClient);
        dispose();
    }

    /** 
     *  Put the chosen hostname as first to the LRU sorted list
     *  in NetClient cf file. 
     */
    private void saveHostname(Options netclientOptions)
    {
        if (netclientOptions == null)
        {
            return;
        }

        List names = new ArrayList();
        // Last used one to front of LRU list:
        names.add(hostname);
        for (int i = 0; i < Constants.numSavedServerNames; i++)
        {
            String serverName = netclientOptions
                .getStringOption(Options.serverName + i);
            if (serverName != null)
            {
                // Don't add it twice:
                if (!serverName.equals(hostname))
                {
                    names.add(serverName);
                }
            }
        }
        for (int i = 0; i < names.size() && i < Constants.numSavedServerNames; i++)
        {
            netclientOptions.setOption(Options.serverName + i, (String)names
                .get(i));
        }
    }
}
