package net.sf.colossus.server;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;
import javax.swing.text.Document;

import net.sf.colossus.client.PickIntValue;
import net.sf.colossus.client.SaveWindow;
import net.sf.colossus.client.ShowReadme;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;

/**
 * Class GetPlayers is a dialog used to enter players' 
 *   names, types, variant, etc.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */


public final class GetPlayers extends JFrame implements WindowListener,
            ActionListener, ItemListener
{
	private static final Logger LOGGER = Logger.getLogger(GetPlayers.class.getName());

    public static final String loadVariant = "Load External Variant";

    private Object mutex;

    private Vector typeChoices = new Vector();
    private JComboBox[] playerTypes = new JComboBox[Constants.MAX_MAX_PLAYERS];
    private JComboBox[] playerNames = new JComboBox[Constants.MAX_MAX_PLAYERS];
    private JEditorPane readme = new JEditorPane();

    private JComboBox variantBox;
    private JComboBox viewModeBox;
    private JComboBox eventExpiringBox;

    /** This is Game's options, which we will modify directly. */
    private Options options;

    private int oldDelay;
    private JLabel delayLabel;
    private int oldLimit;
    private JLabel timeLimitLabel;
    private SaveWindow saveWindow;

    /** Clear options to abort */
    public GetPlayers(Options options, Object mutex)
    {
        super("Game Setup");

        this.options = options;
        this.mutex = mutex;
        
        setupTypeChoices();

        setBackground(Color.lightGray);
        pack();

        Container mainPane = new Box(BoxLayout.Y_AXIS);

        JScrollPane mainScrollPane = new JScrollPane(mainPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainScrollPane, BorderLayout.CENTER);

        try
        {
            InetAddress ia = InetAddress.getLocalHost();
            JLabel iaLabel = new JLabel("Running on " + ia.toString());
            Container iaPane = new Container();
            iaPane.setLayout(new GridLayout(0, 1));
            mainPane.add(iaPane);
            iaPane.add(iaLabel);
        }
        catch (UnknownHostException ex)
        {
            LOGGER.log(Level.SEVERE, ex.toString(), (Throwable)null);
        }

        JTabbedPane tabbedPane = new JTabbedPane();

        Box allPlayersPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Players", allPlayersPane);
        mainPane.add(tabbedPane);
        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            doOnePlayer(i, allPlayersPane);
        }

        Box optionPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Options", optionPane);

        JPanel checkboxPane = new JPanel(new GridLayout(0, 3));
        checkboxPane.setBorder(new TitledBorder("General"));
        optionPane.add(checkboxPane);

        addCheckbox(Options.autosave, checkboxPane);
        addCheckbox(Options.logDebug, checkboxPane);
        addCheckbox(Options.balancedTowers, checkboxPane);
        addCheckbox(Options.autoStop, checkboxPane);
        addCheckbox(Options.autoQuit, checkboxPane);

        
        String viewmodeName = options.getStringOption(Options.viewMode);
        if ( viewmodeName == null )
        {
            viewmodeName = Options.viewableAll; 
        }
        
        JPanel viewModePane = new JPanel(new GridLayout(0, 2));
        viewModePane.setBorder(new TitledBorder("Viewability of legion and events"));
        optionPane.add(viewModePane);

        viewModeBox = new JComboBox(Options.viewModeArray);
        viewModeBox.addActionListener(this);
        viewModeBox.setSelectedItem(viewmodeName);
        viewModePane.add(new JLabel("Viewable legion content:"));
        viewModePane.add(viewModeBox);

        String eventExpiringVal = options.getStringOption(Options.eventExpiring);
        if ( eventExpiringVal == null )
        {
            eventExpiringVal = "5"; 
        }

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
        if (oldDelay < Constants.MIN_AI_DELAY ||
                oldDelay > Constants.MAX_AI_DELAY)
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
        if (oldLimit < Constants.MIN_AI_TIME_LIMIT ||
                oldLimit > Constants.MAX_AI_TIME_LIMIT)
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

        String variantName = options.getStringOption(Options.variant);
        // XXX Make sure chosen variant is in the list.
        if (variantName == null || variantName.length() == 0)
        {
            // Default variant
            variantName = Constants.variantArray[0];
        }

        variantBox = new JComboBox(Constants.variantArray);
        variantBox.addActionListener(this);
        variantBox.setSelectedItem(variantName);
        variantPane.add(variantBox);
        JButton buttonVariant = new JButton(loadVariant);
        variantPane.add(buttonVariant);
        buttonVariant.addActionListener(this);

        options.setOption(Options.variant, variantName);

        // if we don't pass the JEditorPane ("readme"), 
        // it won't be updated when Variant changes.
        JScrollPane readmeScrollPane = ShowReadme.readmeContentScrollPane(
                        readme, variantName);
        tabbedPane.addTab("Variant README", readmeScrollPane);

        JPanel gamePane = new JPanel();
        gamePane.setBorder(new TitledBorder("Game Startup"));
        gamePane.setLayout(new GridLayout(0, 4));
        mainPane.add(gamePane);

        JButton button1 = new JButton(Constants.newGame);
        button1.setMnemonic(KeyEvent.VK_N);
        gamePane.add(button1);
        button1.addActionListener(this);

        JButton button2 = new JButton(Constants.loadGame);
        button2.setMnemonic(KeyEvent.VK_L);
        gamePane.add(button2);
        button2.addActionListener(this);

        JButton button3 = new JButton(Constants.runClient);
        button3.setMnemonic(KeyEvent.VK_C);
        gamePane.add(button3);
        button3.addActionListener(this);

        JButton button4 = new JButton(Constants.quit);
        button4.setMnemonic(KeyEvent.VK_Q);
        gamePane.add(button4);
        button4.addActionListener(this);

        enablePlayers();

        pack();

        saveWindow = new SaveWindow(options, "GetPlayers");
        Point loadLocation = saveWindow.loadLocation();
        if (loadLocation == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(loadLocation);
        }


        addWindowListener(this);
        setVisible(true);
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
        
        String type = options.getStringOption(Options.playerType + i);
        if (type == null || type.length() == 0)
        {
            type = Constants.none;
        }
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
        Vector nameChoices = new Vector();
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

    /** Start new game if values are legal. */
    private void validateInputs()
    {
        options.clearPlayerInfo();

        int numPlayers = 0;
        List names = new ArrayList();
        List types = new ArrayList();

        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            String name = (String)(playerNames[i].getSelectedItem());
            String type = (String)(playerTypes[i].getSelectedItem());
            if (name.length() > 0 && !name.equals(Constants.none) &&
                    !type.equals(Constants.none))
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
                if (name.equals(Constants.byColor) ||
                        name.equals(Constants.byType) ||
                        name.equals(Constants.byClient)
                    )
                {
                    name = name + i;
                }
                // Duplicate names are not allowed.
                if (names.contains(name))
                {
                    JOptionPane.showMessageDialog(this, "Duplicate player names!");
                    options.clearPlayerInfo();
                    return;
                }
                numPlayers++;
                names.add(name);
                types.add(type);
            }
        }

        // Exit if there aren't enough unique player names.
        if (numPlayers < 1 || names.size() != numPlayers)
        {
            JOptionPane.showMessageDialog(this, "Not enough different unique player names!");
            options.clearPlayerInfo();
            return;
        }

        // Okay.  Copy names and types to options.
        for (int i = 0; i < numPlayers; i++)
        {
            String name = (String)names.get(i);
            options.setOption(Options.playerName + i, name);

            String type = (String)types.get(i);
            options.setOption(Options.playerType + i, type);
        }

        // Eliminate modal dialog, allowing game to start.
        dispose();
    }

    private void doLoadGame()
    {
        JFileChooser chooser = new JFileChooser(Constants.saveDirname);
        chooser.setFileFilter(new XMLSnapshotFilter());
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            // Set key to "load game" and value to savegame filename.
            options.setOption(Constants.loadGame,
                    chooser.getSelectedFile().getPath());
            dispose();
        }
    }

    private void doRunClient()
    {
        options.setOption(Constants.runClient, true);
        dispose();
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
        System.exit(0);
    }

    static class varFileFilter extends javax.swing.filechooser.FileFilter
    {
        public boolean accept(File f)
        {
            if (f.isDirectory())
            {
                return(true);
            }
            if (f.getName().endsWith(Constants.varEnd))
            {
                return(true);
            }
            return(false);
        }

        public String getDescription()
        {
            return("Colossus VARiant file");
        }
    }

    private void doLoadVariant()
    {
        int maxPlayers = VariantSupport.getMaxPlayers();
        javax.swing.JFileChooser varChooser =
                new JFileChooser(Constants.gameDataPath);
        varChooser.setFileFilter(new varFileFilter());
        varChooser.setDialogTitle(
                "Choose your variant (or cancel for default game)");
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
                readme.setContentType((String)doc.getProperty(
                        ResourceLoader.keyContentType));
                readme.setDocument(doc);
                if (maxPlayers != VariantSupport.getMaxPlayers())
                {
                    enablePlayers();
                }
            }
        }
    }

    public synchronized void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(Constants.quit))
        {
            options.clear();
            dispose();
            System.exit(0);
        }
        else if (e.getActionCommand().equals(Constants.newGame))
        {
            validateInputs();
        }
        else if (e.getActionCommand().equals(Constants.loadGame))
        {
            doLoadGame();
        }
        else if (e.getActionCommand().equals(Constants.runClient))
        {
            doRunClient();
        }
        else if (e.getActionCommand().equals(Options.aiDelay))
        {
            final int newDelay = PickIntValue.pickIntValue(this,
                    oldDelay, "Pick AI Delay (in ms)", Constants.MIN_AI_DELAY,
                    Constants.MAX_AI_DELAY, 100, options);
            if (newDelay != oldDelay)
            {
                options.setOption(Options.aiDelay, newDelay);
            }
            setDelayLabel(newDelay);
        }
        else if (e.getActionCommand().equals(Options.aiTimeLimit))
        {
            final int newLimit = PickIntValue.pickIntValue(this,
                oldLimit, "Pick AI Time Limit (in s)",
                Constants.MIN_AI_TIME_LIMIT, Constants.MAX_AI_TIME_LIMIT, 
                1, options);
            if (newLimit != oldLimit)
            {
                options.setOption(Options.aiTimeLimit, newLimit);
            }
            setTimeLimitLabel(newLimit);
        }
        else if (e.getActionCommand().startsWith(loadVariant))
        {
            doLoadVariant();
            String varName = VariantSupport.getVarName();
            if (!(Constants.getVariantList().contains(varName)))
            {
                String buttonName = varName.substring(0,
                        varName.lastIndexOf(Constants.varEnd));
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
                if (VariantSupport.getVarName().equals(value +
                        Constants.varEnd))
                { // re-selecting the same ; do nothing
                }
                else
                { // selecting different ; remove all non-included
                    if (variantBox.getItemCount() > Constants.numVariants)
                    {
                        variantBox.removeItemAt(Constants.numVariants);
                    }
                    Document doc = VariantSupport.loadVariant(value, true);
                    options.setOption(Options.variant, value);
                    String prop = (String)doc.getProperty(
                            ResourceLoader.keyContentType);
                    readme.setContentType(prop);
                    readme.setDocument(doc);
                    if (maxPlayers != VariantSupport.getMaxPlayers())
                    {
                        enablePlayers();
                    }
                }
            }
            else if ( source == viewModeBox )
            {
                String value = (String) viewModeBox.getSelectedItem();
                options.setOption(Options.viewMode, value);
            }
            else if ( source == eventExpiringBox )
            {
                String value = (String) eventExpiringBox.getSelectedItem();
                options.setOption(Options.eventExpiring, value);
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
                            playerNames[i].setSelectedItem(Constants.username);
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

    public Dimension getMinimumSize()
    {
        return new Dimension(640, 480);
    }

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

    public void dispose()
    {
        if (!options.isEmpty())
        {
            saveWindow.saveLocation(getLocation());
        }
        super.dispose();
        synchronized(mutex)
        {
            mutex.notify();
        }
    }
    
    /** Center this dialog on the screen.  Must be called after the dialog
     *  size has been set. */
    public void centerOnScreen()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));
    }
    
    public void windowActivated(WindowEvent e)
    {
        // nothing to do
    }

    public void windowClosed(WindowEvent e)
    {
        // nothing to do
    }

    public void windowDeactivated(WindowEvent e)
    {
        // nothing to do
    }

    public void windowDeiconified(WindowEvent e)
    {
        // nothing to do
    }

    public void windowIconified(WindowEvent e)
    {
        // nothing to do
    }

    public void windowOpened(WindowEvent e)
    {
        // nothing to do
    }
}
