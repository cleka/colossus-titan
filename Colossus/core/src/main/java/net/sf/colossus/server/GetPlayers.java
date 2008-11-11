package net.sf.colossus.server;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.text.Document;

import net.sf.colossus.client.PickIntValue;
import net.sf.colossus.client.SaveWindow;
import net.sf.colossus.client.ShowReadme;
import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;


/**
 * Class GetPlayers is a dialog used to enter players' 
 *   names, types, variant, etc.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public final class GetPlayers extends KFrame implements WindowListener,
    ActionListener, ItemListener
{
    private static final Logger LOGGER = Logger.getLogger(GetPlayers.class
        .getName());

    public static final String loadVariant = "Load External Variant";

    private final Object mutex;
    private final JLabel runningOnLabel;
    private final JComboBox serveAtPortBox;
    private final TreeSet<String> sPortChoices;

    private final Vector<String> typeChoices = new Vector<String>();
    private final JComboBox[] playerTypes = new JComboBox[Constants.MAX_MAX_PLAYERS];
    private final JComboBox[] playerNames = new JComboBox[Constants.MAX_MAX_PLAYERS];
    private JEditorPane readme = new JEditorPane();
    private JScrollPane readmeScrollPane;
    private final JTabbedPane tabbedPane;

    private final JComboBox variantBox;
    private final JComboBox viewModeBox;
    private final JComboBox eventExpiringBox;

    private int serveAtPort = -1; // server serves at that.

    /** This is Game's options, which we will modify directly. */
    private final Options options;
    private final Options stOptions;
    private final Start startObject;

    private int oldDelay;
    private final JLabel delayLabel;
    private int oldLimit;
    private final JLabel timeLimitLabel;
    private final SaveWindow saveWindow;

    /** Clear options to abort */
    public GetPlayers(Options options, Object mutex, Start startObject)
    {
        super("Game Setup");

        net.sf.colossus.webcommon.InstanceTracker.register(this, "only one");

        this.options = options;
        this.mutex = mutex;
        this.startObject = startObject;
        this.stOptions = startObject.getStartOptions();

        setupTypeChoices();

        setBackground(Color.lightGray);
        pack();

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
        serveAtPortBox = new JComboBox(sPortChoices
            .toArray(new String[sPortChoices.size()]));
        serveAtPortBox.setEditable(true);
        serveAtPortBox.setSelectedItem("" + stPort);
        serveAtPortBox.addActionListener(this);
        serveAtPort = stPort;
        portPane.add(serveAtPortBox);
        optionPane.add(portPane);

        setRunningOnLabel(stPort);

        JPanel checkboxPane = new JPanel(new GridLayout(0, 3));
        checkboxPane.setBorder(new TitledBorder("General"));
        optionPane.add(checkboxPane);

        addCheckbox(Options.autosave, checkboxPane);
        addCheckbox(Options.balancedTowers, checkboxPane);
        addCheckbox(Options.autoStop, checkboxPane);
        addCheckbox(Options.autoQuit, checkboxPane);
        addCheckbox(Options.hotSeatMode, checkboxPane);

        String viewmodeName = options.getStringOption(Options.viewMode,
            Options.viewableEver);
        JPanel viewModePane = new JPanel(new GridLayout(0, 2));
        viewModePane.setBorder(new TitledBorder(
            "Viewability of legion and events"));
        optionPane.add(viewModePane);

        viewModeBox = new JComboBox(Options.viewModeArray);
        viewModeBox.addActionListener(this);
        viewModeBox.setSelectedItem(viewmodeName);
        viewModePane.add(new JLabel("Viewable legion content:"));
        viewModePane.add(viewModeBox);

        String eventExpiringVal = options.getStringOption(
            Options.eventExpiring, "5");
        eventExpiringBox = new JComboBox(Options.eventExpiringChoices);
        eventExpiringBox.addActionListener(this);
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

        JPanel rulesOptionsPane = new JPanel(new GridLayout(0, 2));
        rulesOptionsPane.setBorder(new TitledBorder("Rules"));
        optionPane.add(rulesOptionsPane);

        addCheckbox(Options.cumulativeSlow, rulesOptionsPane);
        addCheckbox(Options.oneHexAllowed, rulesOptionsPane);
        addCheckbox(Options.nonRandomBattleDice, rulesOptionsPane);
        addCheckbox(Options.unlimitedMulligans, rulesOptionsPane);

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
        delayButton.addActionListener(this);
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
        timeLimitButton.addActionListener(this);
        aiTimePane.add(timeLimitButton);

        JPanel variantPane = new JPanel();
        variantPane.setBorder(new TitledBorder("Variant"));
        variantPane.setLayout(new GridLayout(0, 2));
        mainPane.add(variantPane);

        // XXX Make sure chosen variant is in the list.
        String variantName = options.getStringOption(Options.variant,
            Constants.variantArray[0]);
        variantBox = new JComboBox(Constants.variantArray);
        variantBox.addActionListener(this);
        variantBox.setSelectedItem(variantName);
        variantPane.add(variantBox);
        JButton buttonVariant = new JButton(loadVariant);
        variantPane.add(buttonVariant);
        buttonVariant.addActionListener(this);

        options.setOption(Options.variant, variantName);

        // ================== Variant README tab =====================
        // if we don't pass the JEditorPane ("readme"), 
        // it won't be updated when Variant changes.
        readmeScrollPane = ShowReadme.readmeContentScrollPane(readme,
            variantName);
        tabbedPane.addTab("Variant README", readmeScrollPane);

        JPanel clientPane = new JPanel();
        clientPane.setBorder(new TitledBorder("Clients"));
        clientPane.setLayout(new GridLayout(0, 2));
        mainPane.add(clientPane);

        JButton button3 = new JButton(Constants.runClient);
        button3.setMnemonic(KeyEvent.VK_C);
        clientPane.add(button3);
        button3.addActionListener(this);

        JButton button5 = new JButton(Constants.runWebClient);
        button5.setMnemonic(KeyEvent.VK_W);
        clientPane.add(button5);
        button5.addActionListener(this);

        JPanel gamePane = new JPanel();
        gamePane.setBorder(new TitledBorder("Game Startup"));
        gamePane.setLayout(new GridLayout(0, 3));
        mainPane.add(gamePane);

        JButton button1 = new JButton(Constants.newGame);
        button1.setMnemonic(KeyEvent.VK_N);
        gamePane.add(button1);
        button1.addActionListener(this);

        JButton button2 = new JButton(Constants.loadGame);
        button2.setMnemonic(KeyEvent.VK_L);
        gamePane.add(button2);
        button2.addActionListener(this);

        JButton button4 = new JButton(Constants.quitGame);
        button4.setMnemonic(KeyEvent.VK_Q);
        gamePane.add(button4);
        button4.addActionListener(this);

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

        addWindowListener(this);
        setVisible(true);
    }

    private void setRunningOnLabel(int port)
    {
        InetAddress ia = null;
        String hostString = "<unknown>";
        try
        {
            ia = InetAddress.getLocalHost();
            hostString = ia.toString();
        }
        catch (UnknownHostException ex)
        {
            // In this case the UHExc. is not that a serious problem, because
            // it's for the displaying in GUI only.
            LOGGER.log(Level.WARNING, ex.toString(), ex);
        }
        String runningOnString = "Running on " + hostString + ", port " + port;
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

        JComboBox playerType = new JComboBox(typeChoices);

        // Avoid scrolling (otherwise "Human" might not be visible directly)
        // the number is chosen to be larger then the current choices but not
        // too large to cause havoc if the number of choices increases
        playerType.setMaximumRowCount(12);

        String type = options.getStringOption(Options.playerType + i,
            Constants.none);
        playerType.setSelectedItem(type);

        onePlayerPane.add(playerType);
        playerType.addActionListener(this);
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

        JComboBox playerName = new JComboBox(nameChoices);
        playerName.setEditable(true);
        onePlayerPane.add(playerName);
        playerName.addActionListener(this);
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
        }
    }

    private void addCheckbox(String optname, Container pane)
    {
        JCheckBox cb = new JCheckBox(optname);
        cb.setSelected(options.getOption(optname));
        cb.addItemListener(this);
        pane.add(cb);
    }

    public void addItemToBox(JComboBox box, String port)
    {
        int cnt = box.getItemCount();
        int found = -1;
        int i;
        for (i = 0; i < cnt; i++)
        {
            String p = (String)box.getItemAt(i);
            if (p.equals(port))
            {
                found = i;
            }
        }
        if (found == -1)
        {
            box.addItem(port);
        }
    }

    /** Check if values are legal; if yes, caller can start the game. */
    private boolean validateInputs()
    {
        options.clearPlayerInfo();

        int numPlayers = 0;
        List<String> names = new ArrayList<String>();
        List<String> types = new ArrayList<String>();
        StringBuffer sb = new StringBuffer("");

        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            String name = (String)(playerNames[i].getSelectedItem());
            String type = (String)(playerTypes[i].getSelectedItem());
            if (name.length() > 0 && !name.equals(Constants.none)
                && !type.equals(Constants.none))
            {
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
                && (name.startsWith(Constants.byColor)
                    || name.startsWith(Constants.byType)))
            {
                sb.append("Invalid name \"" + name
                    + "\" for Player " + (i+1) + "\n");
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
        JFileChooser chooser = new JFileChooser(Constants.saveDirname);
        chooser.setFileFilter(new XMLSnapshotFilter());
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            // Set startObject to "load game" and value to filename.
            String filename = chooser.getSelectedFile().getPath();
            stOptions.setOption(Options.loadGameFileName, filename);
            stOptions.setOption(Options.serveAtPort, serveAtPort);
            options.setOption(Options.serveAtPort, serveAtPort);
            Start.setCurrentWhatToDoNext(Start.LoadGame);
            dispose();
        }
    }

    private void doClientDialog()
    {
        startObject.setWhatToDoNext(Start.NetClientDialog);
        dispose();
    }

    private void doRunWebClient()
    {
        startObject.setWhatToDoNext(Start.StartWebClient);
        dispose();
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        doQuit();
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
        startObject.setWhatToDoNext(Start.QuitAll);
        Start.triggerTimedQuit();
        dispose();
    }

    private void doNewGame()
    {
        boolean ok = validateInputs();
        if (ok)
        {
            startObject.setWhatToDoNext(Start.StartGame);
            stOptions.setOption(Options.serveAtPort, serveAtPort);
            options.setOption(Options.serveAtPort, serveAtPort);
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
            Constants.gameDataPath);
        varChooser.setFileFilter(new varFileFilter());
        varChooser
            .setDialogTitle("Choose your variant (or cancel for default game)");
        int returnVal = varChooser.showOpenDialog(this);
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
        {
            File varFile = varChooser.getSelectedFile().getAbsoluteFile();
            Document doc = VariantSupport.loadVariant(varFile, true);
            if (doc != null)
            {
                String name = varFile.getName();
                name = name.substring(0, name.lastIndexOf(Constants.varEnd));
                options.setOption(Options.variant, name);
                readme.setContentType((String)doc
                    .getProperty(ResourceLoader.keyContentType));
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
        StringBuffer tryName = new StringBuffer(baseName);
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
        
        // perhaps .toString() would be ok, but there is arguing whether
        // then you get simply ref to the value of original StringBuffer 
        // object and heaven knows what then... so, just to be sure. 
        return tryName.substring(0);
    }

    public synchronized void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(Constants.quitGame))
        {
            doQuit();
        }
        else if (e.getActionCommand().equals(Constants.newGame))
        {
            doNewGame();
        }
        else if (e.getActionCommand().equals(Constants.loadGame))
        {
            doLoadGame();
        }
        else if (e.getActionCommand().equals(Constants.runClient))
        {
            doClientDialog();
        }
        else if (e.getActionCommand().equals(Constants.runWebClient))
        {
            doRunWebClient();
        }
        else if (e.getActionCommand().equals(Options.aiDelay))
        {
            final int newDelay = PickIntValue.pickIntValue(this, oldDelay,
                "Pick AI Delay (in ms)", Constants.MIN_AI_DELAY,
                Constants.MAX_AI_DELAY, 100, options);
            if (newDelay != oldDelay)
            {
                options.setOption(Options.aiDelay, newDelay);
                oldDelay = newDelay;
            }
            setDelayLabel(newDelay);
        }
        else if (e.getActionCommand().equals(Options.aiTimeLimit))
        {
            final int newLimit = PickIntValue.pickIntValue(this, oldLimit,
                "Pick AI Time Limit (in s)", Constants.MIN_AI_TIME_LIMIT,
                Constants.MAX_AI_TIME_LIMIT, 1, options);
            if (newLimit != oldLimit)
            {
                options.setOption(Options.aiTimeLimit, newLimit);
                oldLimit = newLimit;
            }
            setTimeLimitLabel(newLimit);
        }
        else if (e.getActionCommand().startsWith(loadVariant))
        {
            doLoadVariant();
            String varName = VariantSupport.getVariantName();
            if (!(Constants.getVariantList().contains(varName)))
            {
                String buttonName = varName.substring(0, varName
                    .lastIndexOf(Constants.varEnd));
                if (variantBox.getItemCount() > Constants.numVariants)
                {
                    variantBox.removeItemAt(Constants.numVariants);
                }
                variantBox.addItem(buttonName);
                variantBox.setSelectedItem(buttonName);
            }
        }
        else
        {
            // A combo box was changed.
            Object source = e.getSource();
            if (source == variantBox)
            {
                int maxPlayers = VariantSupport.getMaxPlayers();
                String value = (String)variantBox.getSelectedItem();
                if (VariantSupport.getVariantName().equals(
                    value + Constants.varEnd))
                {
                    // re-selecting the same ; do nothing
                }
                else
                { // selecting different ; remove all non-included
                    if (variantBox.getItemCount() > Constants.numVariants)
                    {
                        variantBox.removeItemAt(Constants.numVariants);
                    }
                    Document doc = VariantSupport.loadVariant(value, true);
                    options.setOption(Options.variant, value);
                    String prop = (String)doc
                        .getProperty(ResourceLoader.keyContentType);
                    readme.setContentType(prop);
                    readme.setDocument(doc);
                    if (maxPlayers != VariantSupport.getMaxPlayers())
                    {
                        enablePlayers();
                    }
                }
            }
            else if (source == viewModeBox)
            {
                String value = (String)viewModeBox.getSelectedItem();
                options.setOption(Options.viewMode, value);
            }
            else if (source == eventExpiringBox)
            {
                String value = (String)eventExpiringBox.getSelectedItem();
                options.setOption(Options.eventExpiring, value);
            }
            else if (source == serveAtPortBox)
            {
                String portString = (String)serveAtPortBox.getSelectedItem();
                serveAtPort = Integer.parseInt(portString);
                setRunningOnLabel(serveAtPort);
            }
            else
            {
                for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
                {
                    JComboBox box = playerTypes[i];
                    if (box == source)
                    {
                        // If player type was changed to none, also change
                        // player name to none.
                        String value = (String)box.getSelectedItem();
                        if (value.equals(Constants.none))
                        {
                            playerNames[i].setSelectedItem(Constants.none);
                        }
                        else if (value.equals(Constants.network))
                        {
                            playerNames[i].setSelectedItem(Constants.byClient);
                        }
                        else if (value.endsWith(Constants.ai))
                        {
                            playerNames[i].setSelectedItem(Constants.byColor);
                        }
                        else if (value.endsWith(Constants.human))
                        {
                            String uniqueHuman = makeUniqueName(
                                Constants.username, i);
                            playerNames[i].setSelectedItem(uniqueHuman);
                        }
                        // If player type was changed away from none, also
                        // change player name to something else.
                        else if (playerNames[i].getSelectedItem().equals(
                            Constants.none))
                        {
                            playerNames[i].setSelectedItem(Constants.byColor);
                        }
                    }

                    box = playerNames[i];
                    if (box == source)
                    {
                        // If player name was changed to none, also change
                        // player type to none.
                        String value = (String)box.getSelectedItem();
                        if (value.equals(Constants.none))
                        {
                            playerTypes[i].setSelectedItem(Constants.none);
                        }
                        // If player type was changed away from none, also
                        // change player name to something else.
                        else if (playerTypes[i].getSelectedItem().equals(
                            Constants.none))
                        {
                            playerTypes[i].setSelectedItem(Constants.anyAI);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(640, 480);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(640, 768);
    }

    public void itemStateChanged(ItemEvent e)
    {
        JToggleButton source = (JToggleButton)e.getSource();
        String text = source.getText();
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
        options.setOption(text, selected);
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
            mutex.notify();
        }
    }

    @Override
    public void windowActivated(WindowEvent e)
    {
        // nothing to do
    }

    @Override
    public void windowClosed(WindowEvent e)
    {
        // nothing to do
    }

    @Override
    public void windowDeactivated(WindowEvent e)
    {
        // nothing to do
    }

    @Override
    public void windowDeiconified(WindowEvent e)
    {
        // nothing to do
    }

    @Override
    public void windowIconified(WindowEvent e)
    {
        // nothing to do
    }

    @Override
    public void windowOpened(WindowEvent e)
    {
        // nothing to do
    }
}
