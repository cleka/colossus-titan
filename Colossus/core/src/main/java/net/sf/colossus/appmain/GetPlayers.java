package net.sf.colossus.appmain;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.text.Document;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.game.Dice;
import net.sf.colossus.gui.PickIntValue;
import net.sf.colossus.gui.ShowReadme;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.server.XMLSnapshotFilter;
import net.sf.colossus.util.BuildInfo;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.util.SystemInfo;
import net.sf.colossus.variant.Variant;


/**
 * Class GetPlayers is a dialog used to enter players'
 *   names, types, variant, etc.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */
@SuppressWarnings("serial")
public final class GetPlayers extends KFrame
{
    private static final Logger LOGGER = Logger.getLogger(GetPlayers.class
        .getName());

    private static final String loadVariant = "Load External Variant";

    // private static final long REMINDER_INTERVAL_DAYS = 30;
    private final Object mutex;
    private boolean mutexNotified = false;
    private final JLabel runningOnLabel;
    private final JComboBox<String> serveAtPortBox;
    private final TreeSet<String> sPortChoices;

    private final Vector<String> typeChoices = new Vector<String>();
    @SuppressWarnings({ "rawtypes" })
    private final JComboBox[] playerTypes = new JComboBox[Constants.MAX_MAX_PLAYERS];
    @SuppressWarnings({ "rawtypes" })
    private final JComboBox[] playerNames = new JComboBox[Constants.MAX_MAX_PLAYERS];
    private JEditorPane readme = new JEditorPane();
    private JScrollPane readmeScrollPane;
    private final JTabbedPane tabbedPane;

    private final JComboBox<String> variantBox;
    private final Vector<String> variantVector;
    private final JComboBox<String> viewModeBox;
    private final JComboBox<String> eventExpiringBox;

    private int serveAtPort = -1; // server serves at that.

    /** This is Game's options, which we will modify directly. */
    private final Options options;
    private final Options stOptions;
    private final WhatNextManager whatNextManager;

    private final boolean byWebClient;

    private int oldDelay;
    private final JLabel delayLabel;
    private int oldLimit;
    private final JLabel timeLimitLabel;
    private final SaveWindow saveWindow;

    /** Clear options to abort */
    public GetPlayers(final Options options, Object mutex,
        WhatNextManager whatNextMgr, boolean byWebClient)
    {
        super("Game Setup");

        net.sf.colossus.util.InstanceTracker.register(this, "only one");

        this.options = options;
        this.mutex = mutex;
        this.whatNextManager = whatNextMgr;

        this.stOptions = whatNextMgr.getStartOptions();
        this.byWebClient = byWebClient;

        setupTypeChoices();

        setBackground(Color.lightGray);

        Container mainPane = new Box(BoxLayout.Y_AXIS);

        JScrollPane mainScrollPane = new JScrollPane(mainPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainScrollPane, BorderLayout.CENTER);

        // dummy text, will be set finally when serveAtPort is handled.
        runningOnLabel = new JLabel("Running on...");
        JPanel runningOnPane = new JPanel();
        runningOnPane.setLayout(new GridLayout(0, 1));
        runningOnPane.add(runningOnLabel);
        mainPane.add(runningOnPane);

        tabbedPane = new JTabbedPane();

        // ================== Players tab =====================
        //
        Box allPlayersPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Players", allPlayersPane);
        mainPane.add(tabbedPane);
        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            doOnePlayer(i, allPlayersPane);
        }

        Box optionPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Options", optionPane);

        Box portPane = new Box(BoxLayout.X_AXIS);
        portPane.add(new JLabel("Serve game at port: "));

        sPortChoices = new TreeSet<String>();
        sPortChoices.add("" + Constants.defaultPort);
        int stPort = stOptions.getIntOption(Options.serveAtPort);
        if (stPort != -1 && stPort != Constants.defaultPort)
        {
            sPortChoices.add("" + stPort);
        }
        else
        {
            stPort = Constants.defaultPort;
        }
        serveAtPortBox = new JComboBox<String>(
            sPortChoices.toArray(new String[sPortChoices.size()]));
        /* moved further down; when run here it hangs up for a minute or
         * so in Java7 ?
         */
        // serveAtPortBox.setEditable(true);

        serveAtPortBox.setSelectedItem("" + stPort);
        serveAtPortBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String portString = (String)serveAtPortBox.getSelectedItem();
                serveAtPort = Integer.parseInt(portString);
                setRunningOnLabel(serveAtPort);
            }
        });
        serveAtPort = stPort;
        portPane.add(serveAtPortBox);
        optionPane.add(portPane);

        setRunningOnLabel(stPort);

        JPanel checkboxPane = new JPanel(new GridLayout(0, 3));
        checkboxPane.setBorder(new TitledBorder("General"));
        optionPane.add(checkboxPane);

        addCheckbox(Options.autosave, checkboxPane);
        addCheckbox(Options.autoStop, checkboxPane);
        addCheckbox(Options.autoQuit, checkboxPane);
        addCheckbox(Options.keepAccepting, checkboxPane);
        addCheckbox(Options.hotSeatMode, checkboxPane);

        String viewmodeName = options.getStringOption(Options.viewMode,
            Options.viewableEver);
        JPanel viewModePane = new JPanel(new GridLayout(0, 2));
        viewModePane.setBorder(new TitledBorder(
            "Viewability of legion and events"));
        optionPane.add(viewModePane);

        viewModeBox = new JComboBox<String>(Options.viewModeArray);
        viewModeBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String value = (String)viewModeBox.getSelectedItem();
                options.setOption(Options.viewMode, value);
            }
        });
        viewModeBox.setSelectedItem(viewmodeName);
        viewModePane.add(new JLabel("Viewable legion content:"));
        viewModePane.add(viewModeBox);

        String eventExpiringVal = options.getStringOption(
            Options.eventExpiring, "5");
        eventExpiringBox = new JComboBox<String>(Options.eventExpiringChoices);
        eventExpiringBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String value = (String)eventExpiringBox.getSelectedItem();
                options.setOption(Options.eventExpiring, value);
            }
        });
        eventExpiringBox.setSelectedItem(eventExpiringVal);
        viewModePane.add(new JLabel("Events expire after (turns):"));
        viewModePane.add(eventExpiringBox);

        options.setOption(Options.viewMode, viewmodeName);

        JPanel teleportPane = new JPanel(new GridLayout(0, 2));
        teleportPane.setBorder(new TitledBorder("Teleport"));
        optionPane.add(teleportPane);

        addCheckbox(Options.noFirstTurnT2TTeleport, teleportPane);
        addCheckbox(Options.noFirstTurnTeleport, teleportPane);
        addCheckbox(Options.noTitanTeleport, teleportPane);
        addCheckbox(Options.towerToTowerTeleportOnly, teleportPane);
        addCheckbox(Options.noTowerTeleport, teleportPane);
        addCheckbox(Options.noFirstTurnWarlockRecruit, teleportPane);

        JPanel rulesOptionsPane = new JPanel(new GridLayout(0, 2));
        rulesOptionsPane.setBorder(new TitledBorder("Rules"));
        optionPane.add(rulesOptionsPane);

        addCheckbox(Options.cumulativeSlow, rulesOptionsPane);
        addCheckbox(Options.oneHexAllowed, rulesOptionsPane);
        addCheckbox(Options.unlimitedMulligans, rulesOptionsPane);
        addCheckbox(Options.balancedTowers, rulesOptionsPane);
        addCheckbox(Options.sansLordAutoBattle, rulesOptionsPane);
        addCheckbox(Options.inactivityTimeout, rulesOptionsPane);
        addCheckbox(Options.pbBattleHits, rulesOptionsPane);
        addCheckbox(Options.fixedSequenceBattleDice, rulesOptionsPane);

        JPanel aiTimePane = new JPanel(new FlowLayout());
        aiTimePane.setBorder(new TitledBorder("AI Timing"));
        optionPane.add(aiTimePane);

        oldDelay = options.getIntOption(Options.aiDelay);
        if (oldDelay < Constants.MIN_AI_DELAY
            || oldDelay > Constants.MAX_AI_DELAY)
        {
            oldDelay = Constants.DEFAULT_AI_DELAY;
        }
        delayLabel = new JLabel();
        setDelayLabel(oldDelay);
        aiTimePane.add(delayLabel);
        JButton delayButton = new JButton(Options.aiDelay);
        delayButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                final int newDelay = PickIntValue.pickIntValue(
                    GetPlayers.this, oldDelay, "Pick AI Delay (in ms)",
                    Constants.MIN_AI_DELAY, Constants.MAX_AI_DELAY, 100,
                    options);
                if (newDelay != oldDelay)
                {
                    options.setOption(Options.aiDelay, newDelay);
                    oldDelay = newDelay;
                }
                setDelayLabel(newDelay);

            }
        });
        aiTimePane.add(delayButton);

        oldLimit = options.getIntOption(Options.aiTimeLimit);
        if (oldLimit < Constants.MIN_AI_TIME_LIMIT
            || oldLimit > Constants.MAX_AI_TIME_LIMIT)
        {
            oldLimit = Constants.DEFAULT_AI_TIME_LIMIT;
        }
        timeLimitLabel = new JLabel();
        setTimeLimitLabel(oldLimit);
        aiTimePane.add(timeLimitLabel);
        JButton timeLimitButton = new JButton(Options.aiTimeLimit);
        timeLimitButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                final int newLimit = PickIntValue.pickIntValue(
                    GetPlayers.this, oldLimit, "Pick AI Time Limit (in s)",
                    Constants.MIN_AI_TIME_LIMIT, Constants.MAX_AI_TIME_LIMIT,
                    1, options);
                if (newLimit != oldLimit)
                {
                    options.setOption(Options.aiTimeLimit, newLimit);
                    oldLimit = newLimit;
                }
                setTimeLimitLabel(newLimit);
            }
        });
        aiTimePane.add(timeLimitButton);

        JPanel variantPane = new JPanel();
        variantPane.setBorder(new TitledBorder("Variant"));
        variantPane.setLayout(new GridLayout(0, 2));
        mainPane.add(variantPane);

        // XXX Make sure chosen variant is in the list.
        String variantName = options.getStringOption(Options.variant,
            Constants.variantArray[0]);
        String variantFullPath = options.getStringOption(
            Options.variantFileWithFullPath, "null");
        if (variantFullPath == null || variantFullPath.equals(""))
        {
            variantFullPath = "null";
        }

        // validate variant name and full path we got from options:
        boolean isValidExternVariant = false;
        if (!variantFullPath.equals("null"))
        {
            File varFile = new File(variantFullPath);
            if (varFile.exists())
            {
                isValidExternVariant = true;
            }
            else
            {
                LOGGER.warning("Invalid variant file name (full path: '"
                    + variantFullPath
                    + "') from Options! Resetting to Default.");

                variantName = Constants.variantArray[0];
                variantFullPath = "null";
                options.setOption(Options.variant, variantName);
                options.setOption(Options.variantFileWithFullPath, "null");
            }
        }

        // We must ensure that the variantFullPath option is now never null
        // nor empty string any more, otherwise synchronizing it to clients
        // causes NPEs.
        options.setOption(Options.variantFileWithFullPath, variantFullPath);

        variantVector = new Vector<String>(Constants.numVariants, 1);
        boolean isBuiltinVariant = false;
        for (int i = 0; i < Constants.numVariants; i++)
        {
            String name = Constants.variantArray[i];
            variantVector.add(i, name);
            if (name.equals(variantName))
            {
                isBuiltinVariant = true;
            }
        }

        variantBox = new JComboBox<String>(variantVector);
        variantBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int oldMaxPlayers = VariantSupport.getMaxPlayers();
                String newVarName = (String)variantBox.getSelectedItem();

                if (VariantSupport.getVariantName().equals(newVarName))
                {
                    // re-selecting the same ; do nothing
                }
                else
                {
                    actOnVariantChange(oldMaxPlayers, newVarName);
                }
            }
        });
        if (isBuiltinVariant)
        {
            variantBox.setSelectedItem(variantName);
        }
        else if (isValidExternVariant)
        {
            VariantSupport.rememberFullPathFileForVariantName(variantName,
                variantFullPath);
            variantVector.add(variantName);
            variantBox.setSelectedItem(variantName);
            options.setOption(Options.variant, variantName);
            options
                .setOption(Options.variantFileWithFullPath, variantFullPath);

        }
        else
        {
            LOGGER.warning("Invalid variant! Resetting to Default.");

            variantName = Constants.variantArray[0];
            variantFullPath = "null";
            options.setOption(Options.variant, variantName);
            options.setOption(Options.variantFileWithFullPath, "null");

            int oldMaxPlayers = VariantSupport.getMaxPlayers();
            actOnVariantChange(oldMaxPlayers, variantName);
            // This should never happen...
            /*           LOGGER
                           .severe("Unexpected else case? Invalid variant name, perhaps?");

                       variantName = Constants.variantArray[0];
                       variantFullPath = "null";
                       options.setOption(Options.variant, variantName);
                       options.setOption(Options.variantFileWithFullPath, "null");
             */
        }

        variantPane.add(variantBox);
        JButton buttonVariant = new JButton(loadVariant);
        variantPane.add(buttonVariant);
        buttonVariant.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doLoadVariant();
                String varName = VariantSupport.getVariantName();
                if (!(Constants.getVariantList().contains(varName)))
                {
                    addVariantToBoxIfNeeded(varName);
                    variantBox.setSelectedItem(varName);
                }
            }
        });

        // ================== Variant README tab =====================
        // if we don't pass the JEditorPane ("readme"),
        // it won't be updated when Variant changes.

        Document doc = VariantSupport.getCurrentVariant().getReadme();
        readmeScrollPane = ShowReadme.readmeContentScrollPane(readme, doc);
        tabbedPane.addTab("Variant README", readmeScrollPane);

        JPanel clientPane = new JPanel();
        clientPane.setBorder(new TitledBorder("Clients"));
        clientPane.setLayout(new GridLayout(0, 2));
        mainPane.add(clientPane);

        JButton button3 = new JButton(Constants.runClient);
        button3.setMnemonic(KeyEvent.VK_C);
        clientPane.add(button3);
        button3.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doClientDialog();
            }
        });

        JButton button5 = new JButton(Constants.runWebClient);
        button5.setMnemonic(KeyEvent.VK_W);
        clientPane.add(button5);
        button5.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doRunWebClient();
            }
        });

        JPanel gamePane = new JPanel();
        gamePane.setBorder(new TitledBorder("Game Startup"));
        gamePane.setLayout(new GridLayout(0, 3));
        mainPane.add(gamePane);

        JButton button1 = new JButton(Constants.newGame);
        button1.setMnemonic(KeyEvent.VK_N);
        gamePane.add(button1);
        button1.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doNewGame();
            }
        });

        JButton button2 = new JButton(Constants.loadGame);
        button2.setMnemonic(KeyEvent.VK_L);
        gamePane.add(button2);
        button2.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doLoadGame();
            }
        });

        JButton button4 = new JButton(Constants.quitGame);
        button4.setMnemonic(KeyEvent.VK_Q);
        gamePane.add(button4);
        button4.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doQuit();
            }
        });

        enablePlayers();

        pack();

        saveWindow = new SaveWindow(options, "GetPlayers");
        Point loadLocation = saveWindow.loadLocation();
        if (loadLocation == null)
        {
            // if we would save&restore both size + pos,
            // could use KFrame's restoreOrCenter and don't
            // need own centerOnScreen in here...
            centerOnScreen();
        }
        else
        {
            setLocation(loadLocation);
        }

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                doQuit();
            }
        });
        setVisible(true);

        serveAtPortBox.setEditable(true);

        WelcomeDialog.showWelcomeDialogMaybe(options);

        if (SystemInfo.isOracleJava7())
        {
            // warnAboutJava7();
        }
    }

    private void actOnVariantChange(int oldMaxPlayers, String newVarName)
    {
        Variant variant = VariantSupport.loadVariantByName(newVarName, true);
        Document doc = variant.getReadme();

        // If loading failed, change variantBox to what is actually now
        // loaded. Probably Default.
        String loadedName = variant.getName();
        if (!loadedName.equals(variantBox.getSelectedItem()))
        {
            LOGGER.info("Ups, loaded variant '" + loadedName
                + "' does not match selected item in variantBox '"
                + variantBox.getSelectedItem() + "'");

            // TODO somewhat hacked to just re-set it to "Default"
            // We just use the default from variant names array,
            // otherwise we would have to search through all items
            // ComboBox changes only if it is the same *object*)
            variantBox.setSelectedItem(Constants.variantArray[0]);
            newVarName = loadedName;
            doc = VariantSupport.getCurrentVariant().getReadme();
        }
        else
        {
            LOGGER.finest("OK, loaded variant (" + loadedName
                + ") matches selected item in variantBox");
        }

        options.setOption(Options.variant, newVarName);
        String varFileWithFullPath = VariantSupport
            .getFullPathFileForVariantName(newVarName);
        if (varFileWithFullPath == null)
        {
            varFileWithFullPath = "null";
        }
        options
            .setOption(Options.variantFileWithFullPath, varFileWithFullPath);

        String prop = (String)doc
            .getProperty(StaticResourceLoader.KEY_CONTENT_TYPE);
        readme.setContentType(prop);
        readme.setDocument(doc);
        if (oldMaxPlayers != VariantSupport.getMaxPlayers())
        {
            enablePlayers();
        }

    }

    private void setRunningOnLabel(int port)
    {
        /* 11.2.2015: Removed the "on host" part, because with Java 7
         * under certain circumstances the InetAdress.getLocalHostname()
         * might be stuck a minute or so, before it times out.
         */
        String runningOnString = "Running Colossus Version "
            + BuildInfo.getReleaseVersion() + " ("
            + BuildInfo.getRevisionInfoString() + ")" + ", listening at port "
            + port;
        runningOnLabel.setText(runningOnString);
    }

    private void setDelayLabel(int delay)
    {
        delayLabel.setText("  Current AI delay: " + delay + " ms  ");
    }

    private void setTimeLimitLabel(int limit)
    {
        timeLimitLabel.setText("  Current AI time limit: " + limit + " s  ");
    }

    private void setupTypeChoices()
    {
        typeChoices.clear();
        typeChoices.add(Constants.human);
        typeChoices.add(Constants.network);
        for (int i = 0; i < Constants.numAITypes; i++)
        {
            if (!(Constants.aiArray[i].equals("")))
            {
                typeChoices.add(Constants.aiArray[i]);
            }
        }
        // Only show random AI choice if more than one AI.
        if (Constants.numAITypes >= 2)
        {
            typeChoices.add(Constants.anyAI);
        }
        typeChoices.add(Constants.none);
    }

    private void doOnePlayer(final int i, Container allPlayersPane)
    {
        JPanel onePlayerPane = new JPanel();
        onePlayerPane.setLayout(new GridLayout(0, 3));
        allPlayersPane.add(onePlayerPane);

        String s = "Player " + (i + 1);
        onePlayerPane.add(new JLabel(s));

        final JComboBox<String> playerType = new JComboBox<String>(typeChoices);

        // Avoid scrolling (otherwise "Human" might not be visible directly)
        // the number is chosen to be larger then the current choices but not
        // too large to cause havoc if the number of choices increases
        playerType.setMaximumRowCount(12);

        String type = options.getStringOption(Options.playerType + i,
            Constants.none);
        playerType.setSelectedItem(type);

        onePlayerPane.add(playerType);
        playerType.addActionListener(new ActionListener()
        {
            private final int row = i;

            public void actionPerformed(ActionEvent e)
            {
                // If player type was changed to none, also change
                // player name to none.
                String value = (String)playerType.getSelectedItem();
                if (value.equals(Constants.none))
                {
                    playerNames[row].setSelectedItem(Constants.none);
                }
                else if (value.equals(Constants.network))
                {
                    playerNames[row].setSelectedItem(Constants.byClient);
                }
                else if (value.endsWith(Constants.ai))
                {
                    playerNames[row].setSelectedItem(Constants.byColor);
                }
                else if (value.endsWith(Constants.human))
                {
                    String uniqueHuman = makeUniqueName(Constants.username,
                        row);
                    playerNames[row].setSelectedItem(uniqueHuman);
                }
                // If player type was changed away from none, also
                // change player name to something else.
                else if (playerNames[row].getSelectedItem().equals(
                    Constants.none))
                {
                    playerNames[row].setSelectedItem(Constants.byColor);
                }
            }
        });
        playerType.setEnabled(false);
        playerTypes[i] = playerType;

        String cmdlineName = options.getStringOption(Options.playerName);
        String name = options.getStringOption(Options.playerName + i);
        if (cmdlineName != null)
        {
            name = cmdlineName;
        }
        else if (name == null || name.length() == 0)
        {
            name = Constants.none;
        }
        else if (name.startsWith(Constants.byColor))
        {
            name = Constants.byColor;
        }
        else if (name.startsWith(Constants.byType))
        {
            name = Constants.byType;
        }
        else if (name.startsWith(Constants.byClient))
        {
            name = Constants.byClient;
        }
        Vector<String> nameChoices = new Vector<String>();
        nameChoices.add(name);
        if (!nameChoices.contains(Constants.byColor))
        {
            nameChoices.add(Constants.byColor);
        }
        if (!nameChoices.contains(Constants.byType))
        {
            nameChoices.add(Constants.byType);
        }
        if (!nameChoices.contains(Constants.username))
        {
            nameChoices.add(Constants.username);
        }
        if (!nameChoices.contains(Constants.none))
        {
            nameChoices.add(Constants.none);
        }

        final JComboBox<String> playerName = new JComboBox<String>(nameChoices);
        // setEditable moved to enablePlayers; if done here, it
        // was hanging for 1 minutes or so.
        onePlayerPane.add(playerName);
        playerName.addActionListener(new ActionListener()
        {
            final int row = i;

            public void actionPerformed(ActionEvent e)
            {
                // If player name was changed to none, also change
                // player type to none.
                String value = (String)playerName.getSelectedItem();
                if (value.equals(Constants.none))
                {
                    playerTypes[row].setSelectedItem(Constants.none);
                }
                // If player type was changed away from none, also
                // change player name to something else.
                else if (playerTypes[row].getSelectedItem().equals(
                    Constants.none))
                {
                    playerTypes[row].setSelectedItem(Constants.anyAI);
                }
            }
        });
        playerName.setEnabled(false);
        playerNames[i] = playerName;
    }

    private void enablePlayers()
    {
        int maxPlayers = VariantSupport.getMaxPlayers();
        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            playerTypes[i].setEnabled(i < maxPlayers);
            playerNames[i].setEnabled(i < maxPlayers);
            playerNames[i].setEditable(true);
        }
    }

    private void addCheckbox(final String optname, Container pane)
    {
        JCheckBox cb = new JCheckBox(optname);
        cb.setSelected(options.getOption(optname));
        cb.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
                options.setOption(optname, selected);
            }
        });
        pane.add(cb);
    }

    /** Check if values are legal; if yes, caller can start the game. */
    private boolean validateInputs()
    {
        options.clearPlayerInfo();

        int numPlayers = 0;
        int numAIs = 0;
        List<String> names = new ArrayList<String>();
        List<String> types = new ArrayList<String>();
        StringBuilder sb = new StringBuilder("");

        for (int i = 0; i < VariantSupport.getMaxPlayers(); i++)
        {
            String name = (String)(playerNames[i].getSelectedItem());
            String type = (String)(playerTypes[i].getSelectedItem());
            if (name.length() > 0 && !name.equals(Constants.none)
                && !type.equals(Constants.none))
            {

                if (!type.equals(Constants.network)
                    && !type.equals(Constants.human))
                {
                    numAIs++;
                }

                // Force all network players to byClient.
                if (type.equals(Constants.network))
                {
                    name = Constants.byClient;
                }
                // Don't allow local players to be called byClient.
                else if (name.startsWith(Constants.byClient))
                {
                    name = Constants.byColor;
                }

                // Make by* names unique by appending row number.
                if (name.equals(Constants.byColor)
                    || name.equals(Constants.byType)
                    || name.equals(Constants.byClient))
                {
                    name = name + i;
                }
                // Duplicate names are not allowed.
                if (names.contains(name))
                {
                    JOptionPane.showMessageDialog(this,
                        "Duplicate player name '" + name + "'!");
                    options.clearPlayerInfo();
                    return false;
                }
                numPlayers++;
                names.add(name);
                types.add(type);
            }
            if (type.equals(Constants.human)
                && (name.startsWith(Constants.byColor) || name
                    .startsWith(Constants.byType)))
            {
                sb.append("Invalid name \"" + name + "\" for Player "
                    + (i + 1) + "\n");
            }

        }

        if (numAIs >= numPlayers && options.getOption(Options.autoStop))
        {
            String message = "Option '" + Options.autoStop
                + "' should be disabled when all players are AIs!";
            JOptionPane.showMessageDialog(this, message);
            return false;
        }

        // preserve the non-used ones
        // (e.g. 7-12 for 6 player variant)
        for (int i = VariantSupport.getMaxPlayers(); i < Constants.MAX_MAX_PLAYERS; i++)
        {
            String name = (String)(playerNames[i].getSelectedItem());
            String type = (String)(playerTypes[i].getSelectedItem());
            if (name.length() > 0 && !name.equals(Constants.none)
                && !type.equals(Constants.none))
            {
                options.setOption(Options.playerName + i, name);
                options.setOption(Options.playerType + i, type);
            }
        }

        if (sb.length() > 0)
        {
            String message = sb.substring(0) + "\n"
                + "Reason: <by...> names are not allowed for human players!";
            JOptionPane.showMessageDialog(this, message);
            return false;

        }

        // Exit if there aren't enough unique player names.
        if (numPlayers < 1 || names.size() != numPlayers)
        {
            JOptionPane.showMessageDialog(this,
                "Not enough different unique player names!");
            options.clearPlayerInfo();
            return false;
        }

        // Okay.  Copy names and types to options.
        for (int i = 0; i < numPlayers; i++)
        {
            String name = names.get(i);
            options.setOption(Options.playerName + i, name);

            String type = types.get(i);
            options.setOption(Options.playerType + i, type);
        }

        return true;
    }

    private void doLoadGame()
    {
        JFileChooser chooser = new JFileChooser(Constants.SAVE_DIR_NAME);
        chooser.setFileFilter(new XMLSnapshotFilter());
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            // Set startObject to "load game" and value to filename.
            String filename = chooser.getSelectedFile().getPath();
            stOptions.setOption(Options.loadGameFileName, filename);
            stOptions.setOption(Options.serveAtPort, serveAtPort);
            options.setOption(Options.serveAtPort, serveAtPort);
            whatNextManager.setWhatToDoNext(WhatToDoNext.LOAD_GAME, false,
                true);
            dispose();
        }
    }

    private void doClientDialog()
    {
        whatNextManager.setWhatToDoNext(WhatToDoNext.NET_CLIENT_DIALOG, false);
        dispose();
    }

    private void doRunWebClient()
    {
        whatNextManager.setWhatToDoNext(WhatToDoNext.START_WEB_CLIENT, false);
        dispose();
    }

    private boolean quitAlreadyTried = false;

    private void doQuit()
    {
        // If "clean quit" fails (e.g. one modifies the Colossus.jar
        // while game is running;-), provide a way that on 2nd click
        // one can get rid of the application...
        if (quitAlreadyTried)
        {
            LOGGER.log(Level.SEVERE,
                "It seems the clean 'Quit' did fail - doing it now "
                    + "the hard way using System.exit(1)", (Throwable)null);
            System.exit(1);
        }
        quitAlreadyTried = true;
        whatNextManager.setWhatToDoNext(WhatToDoNext.QUIT_ALL, true);
        dispose();
    }

    /** package privat for unit test case */
    void doNewGame()
    {
        boolean ok = validateInputs();
        if (ok)
        {
            if (!byWebClient)
            {
                whatNextManager
                    .setWhatToDoNext(WhatToDoNext.START_GAME, false);
                stOptions.setOption(Options.serveAtPort, serveAtPort);
                stOptions.setOption(Options.FORCE_BOARD, true);
                options.setOption(Options.serveAtPort, serveAtPort);
            }
            dispose();
        }
        else
        {
            // ValidateInputs showed an error message box.
        }
    }

    static class varFileFilter extends javax.swing.filechooser.FileFilter
    {
        @Override
        public boolean accept(File f)
        {
            if (f.isDirectory())
            {
                return (true);
            }
            if (f.getName().endsWith(Constants.varEnd))
            {
                return (true);
            }
            return (false);
        }

        @Override
        public String getDescription()
        {
            return ("Colossus VARiant file");
        }
    }

    private void doLoadVariant()
    {
        int maxPlayers = VariantSupport.getMaxPlayers();
        javax.swing.JFileChooser varChooser = new JFileChooser(
            Constants.DEFAULT_COLOSSUS_HOME);
        varChooser.setFileFilter(new varFileFilter());
        varChooser
            .setDialogTitle("Choose your variant (or cancel for default game)");
        int returnVal = varChooser.showOpenDialog(this);
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
        {
            File varFile = varChooser.getSelectedFile().getAbsoluteFile();
            Variant variant = VariantSupport.loadVariantByFile(varFile, true);
            Document doc = variant.getReadme();
            if (doc != null)
            {
                String name = varFile.getName();
                String fullPath = varFile.getPath();

                name = name.substring(0, name.lastIndexOf(Constants.varEnd));
                options.setOption(Options.variant, name);
                options.setOption(Options.variantFileWithFullPath, fullPath);
                readme.setContentType((String)doc
                    .getProperty(StaticResourceLoader.KEY_CONTENT_TYPE));
                readme.setDocument(doc);
                if (maxPlayers != VariantSupport.getMaxPlayers())
                {
                    enablePlayers();
                }
            }
        }
    }

    private String makeUniqueName(String baseName, int i)
    {
        StringBuilder tryName = new StringBuilder(baseName);
        boolean duplicate = true;
        while (duplicate)
        {
            duplicate = false;
            for (int j = 0; j < Constants.MAX_MAX_PLAYERS; j++)
            {
                if (j != i)
                {
                    String otherBoxName = (String)playerNames[j]
                        .getSelectedItem();
                    if (tryName.toString().equals(otherBoxName))
                    {
                        duplicate = true;
                    }
                }
            }
            if (duplicate)
            {
                int nr;

                // on first attempt, take row number
                if (tryName.toString().equals(Constants.username))
                {
                    nr = i;
                }
                // if that does not help, random until unique
                else
                {
                    nr = Dice.rollDie();
                }
                tryName.append(String.valueOf(nr));
            }
        }

        return tryName.toString();
    }

    private void addVariantToBoxIfNeeded(String varName)
    {
        int cnt = variantBox.getItemCount();
        for (int i = 0; i < cnt; i++)
        {
            String item = variantBox.getItemAt(i);
            if (item.equals(varName))
            {
                return;
            }
        }
        variantBox.addItem(varName);
    }

    /*
     * Eliminate dialog and notify mutex, allowing game to start,
     * or main to run web client or netclient dialog.
     */
    @Override
    public void dispose()
    {
        if (!options.isEmpty())
        {
            saveWindow.saveLocation(getLocation());
        }
        options.saveOptions();
        // some cleanup, to ensure proper GC:
        readme.getParent().remove(readme);
        tabbedPane.remove(readmeScrollPane);
        readmeScrollPane = null;
        readme = null;

        // Dispose dialog and notify main() so that game starts:
        super.dispose();
        synchronized (mutex)
        {
            mutexNotified = true;
            mutex.notify();
        }
    }

    /** Only meant for unit tests */
    void setPlayerType(int i, String type)
    {
        playerTypes[i].setSelectedItem(type);
    }

    public boolean getMutexNotified()
    {
        return mutexNotified;
    }

    /**
     * Displays the warning if it hasn't been shown for REMINDER_INTERVAL_DAYS
     * (or never before at all), and if yes, write back today's date.
     * I abuse String here, because I don't really want to build the whole
     * set of getter, setter, trigger, ... for long just for this one case.
     */

    /*
     * 27.11.2015: Not really relevant any more. The battleboard issue can't
     * actually be caused by it, because there has been at least one game where
     * both players used Java 8 and it still happened.
     * Now retrospectively, perhaps this "fixed in java 8 but not 7" was not
     * actually about this, but about the "do not click anything outside a
     * modal dialog, otherwise things hang (which impacted the inactivity-
     * autoplay warning dialog.
     *
     * TODO: if Options.lastJava7Warning option is found in options file,
     * remove it.
     */
    /*
    private void warnAboutJava7()
    {
        long now = new Date().getTime();
        String lastTimeString = options
            .getStringOption(Options.lastJava7Warning);
        long lastTime = -1;
        if (lastTimeString != null)
        {
            lastTime = Long.parseLong(lastTimeString);
        }

        if (lastTime == -1
            || (now - lastTime >= REMINDER_INTERVAL_DAYS * 24 * 60 * 60))
        {
            options.setOption(Options.lastJava7Warning, Long.toString(now));
            String message = "Please note:\n" + "It looks you are using Oracle Java 7 (\""
                + SystemInfo.getDisplayJavaInfo()
                + "\").\n\n"
                + "There are known issues with Colossus and Oracle Java version 7, which\n"
                + "might cause problems, in worst case crash the game.\n"
                + "For example, the 'no battleboard comes up' problem is caused by this.\n"
                + "\n"
                + "If possible, use a different Java version, for example Oracle Java 5, 6, or 8,\n"
                + "or a Java JRE from a different vendor (like OpenJDK/OpenJRE)."
                + "\n\nYou have been warned :-)";
            JOptionPane.showMessageDialog(this, message, "Oracle Java 7",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    */
}
