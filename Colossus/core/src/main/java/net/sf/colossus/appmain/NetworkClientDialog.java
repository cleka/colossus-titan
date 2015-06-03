package net.sf.colossus.appmain;


import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.guiutil.SaveWindow;


/**
 * Startup code for network Client
 *
 * @author David Ripton
 */
@SuppressWarnings("serial")
public class NetworkClientDialog extends KFrame
{
    private static final Logger LOGGER = Logger
        .getLogger(NetworkClientDialog.class.getName());

    private final Object mutex;
    private final Options netclientOptions;
    private final Options stOptions;
    private final WhatNextManager whatNextManager;

    private String playerName;
    private String hostname;
    private int port;
    private final SaveWindow saveWindow;

    private final JComboBox<String> nameBox;
    private final JComboBox<String> hostBox;
    private final JComboBox<String> portBox;

    public NetworkClientDialog(Object mutex, final WhatNextManager whatNextMgr)
    {
        super("Client startup options");
        getContentPane().setLayout(new GridLayout(0, 2));

        net.sf.colossus.util.InstanceTracker.register(this, "only one");

        this.mutex = mutex;
        this.whatNextManager = whatNextMgr;
        this.stOptions = whatNextMgr.getStartOptions();

        // player, preferred host (or null) and port from main() / cmdline
        this.playerName = stOptions.getStringOption(Options.runClientPlayer);
        this.hostname = stOptions.getStringOption(Options.runClientHost);
        this.port = stOptions.getIntOption(Options.runClientPort);

        // LRU list of hosts, and window geometry from NetClient cf file
        netclientOptions = new Options(Constants.OPTIONS_NET_CLIENT_NAME);
        netclientOptions.loadOptions();

        Container panel = getContentPane();

        panel.add(new JLabel(
            stOptions.getOption(Options.runSpectatorClient) ? "Spectator name"
                : "Player name"));
        Set<String> nameChoices = new TreeSet<String>();
        nameChoices.add(playerName);
        nameChoices.add(Constants.username);
        nameBox = new JComboBox<String>(new Vector<String>(nameChoices));
        nameBox.setEditable(true);
        nameBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                playerName = (String)nameBox.getSelectedItem();
            }
        });
        nameBox.setSelectedItem(playerName);
        panel.add(nameBox);

        panel.add(new JLabel("Server hostname"));
        Set<String> hostChoices = new TreeSet<String>();
        String preferred = initServerNames(hostname, hostChoices,
            netclientOptions);
        this.hostname = preferred;
        hostBox = new JComboBox<String>(new Vector<String>(hostChoices));
        hostBox.setEditable(true);
        hostBox.setSelectedItem(preferred);
        hostBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                hostname = (String)hostBox.getSelectedItem();
            }
        });
        panel.add(hostBox);

        panel.add(new JLabel("Server port"));
        Set<String> portChoices = new TreeSet<String>();
        portChoices.add("" + port);
        portChoices.add("" + Constants.defaultPort);
        portBox = new JComboBox<String>(
            portChoices.toArray(new String[portChoices.size()]));
        portBox.setEditable(true);
        portBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                port = Integer.parseInt((String)portBox.getSelectedItem());
            }
        });
        panel.add(portBox);

        JButton goButton = new JButton("Go");
        goButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doRunNetClient();
            }
        });
        panel.add(goButton);

        JButton quitButton = new JButton(Constants.quitGame);
        quitButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                whatNextMgr.setWhatToDoNext(WhatToDoNext.QUIT_ALL, true);
                dispose();
            }
        });
        panel.add(quitButton);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                whatNextMgr.setWhatToDoNext(WhatToDoNext.GET_PLAYERS_DIALOG,
                    false);
                dispose();
            }
        });
        pack();
        saveWindow = new SaveWindow(netclientOptions, "NetworkClientDialog");
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
    public static String initServerNames(String wantedHost,
        Set<String> hostChoices, Options netclientOptions)
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

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(350, 200);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

    @Override
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

        whatNextManager.setWhatToDoNext(WhatToDoNext.START_NET_CLIENT, false);
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

        List<String> names = new ArrayList<String>();
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
            netclientOptions.setOption(Options.serverName + i, names.get(i));
        }
    }
}
