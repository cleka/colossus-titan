package net.sf.colossus.server;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.util.*;
import java.net.*;

import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.Log;
import net.sf.colossus.client.PickIntValue;

/**
 * Class GetPlayers is a dialog used to enter players' names, types, variant, etc. 
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */


public final class GetPlayers extends KDialog implements WindowListener,
    ActionListener, ItemListener
{
    public static final String loadVariant = "Load External Variant";

    private JFrame parentFrame;

    private Vector typeChoices = new Vector();
    private JComboBox[] playerTypes = new JComboBox[Constants.MAX_MAX_PLAYERS];
    private JComboBox[] playerNames = new JComboBox[Constants.MAX_MAX_PLAYERS];
    private JEditorPane readme = new JEditorPane();

    private JComboBox variantBox;

    /** This is Game's options, which we will modify directly. */
    private Options options;

    private int oldDelay;
    private JLabel delayLabel;
    private int oldLimit;
    private JLabel timeLimitLabel;



    /** Clear options to abort */
    public GetPlayers(JFrame parentFrame, Options options)
    {
        super(parentFrame, "Game Setup", true);

        this.options = options;

        setupTypeChoices();

        this.parentFrame = parentFrame;
        setBackground(Color.lightGray);
        pack();

        Container mainPane = new JPanel();
        BoxLayout baseLayout = new BoxLayout(mainPane, BoxLayout.Y_AXIS);
        mainPane.setLayout(baseLayout);

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
            Log.error(ex.toString());
        }

        JPanel allPlayersPane = new JPanel();
        allPlayersPane.setBorder(new TitledBorder("Players"));
        BoxLayout allPlayersLayout = new BoxLayout(allPlayersPane, 
            BoxLayout.Y_AXIS);
        allPlayersPane.setLayout(allPlayersLayout);
        mainPane.add(allPlayersPane);
        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            doOnePlayer(i, allPlayersPane);
        }

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
        button2.setMnemonic(KeyEvent.VK_C);
        gamePane.add(button3);
        button3.addActionListener(this);

        JButton button4 = new JButton(Constants.quit);
        button4.setMnemonic(KeyEvent.VK_Q);
        gamePane.add(button4);
        button4.addActionListener(this);

        JPanel checkboxPane = new JPanel(new GridLayout(0, 3));
        checkboxPane.setBorder(new TitledBorder("General Options"));
        mainPane.add(checkboxPane);
        
        addCheckbox(Options.autosave, checkboxPane);
        addCheckbox(Options.logDebug, checkboxPane);
        addCheckbox(Options.balancedTowers, checkboxPane);
        addCheckbox(Options.allStacksVisible, checkboxPane);
        addCheckbox(Options.autoStop, checkboxPane);
        addCheckbox(Options.autoQuit, checkboxPane);

        JPanel teleportPane = new JPanel(new GridLayout(0, 2));
        teleportPane.setBorder(new TitledBorder("Teleport Options"));
        mainPane.add(teleportPane);

        addCheckbox(Options.noFirstTurnT2TTeleport, teleportPane);
        addCheckbox(Options.noFirstTurnTeleport, teleportPane);
        addCheckbox(Options.noTitanTeleport, teleportPane);
        addCheckbox(Options.towerToTowerTeleportOnly, teleportPane);
        addCheckbox(Options.noTowerTeleport, teleportPane);

        JPanel rulesOptionsPane = new JPanel(new GridLayout(0, 2));
        rulesOptionsPane.setBorder(new TitledBorder("Rules Options"));
        mainPane.add(rulesOptionsPane);

        addCheckbox(Options.cumulativeSlow, rulesOptionsPane);
        addCheckbox(Options.oneHexAllowed, rulesOptionsPane);

        JPanel aiTimePane = new JPanel(new FlowLayout());
        aiTimePane.setBorder(new TitledBorder("AI Timing"));
        mainPane.add(aiTimePane);

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

        JPanel readmePane = new JPanel();
        readmePane.setLayout(new GridLayout(0, 1));
        readme.setEditable(false);
        // Must be tall enough for biggest variant readme file.
        Dimension readmeSize = new Dimension(600, 2000);
        readmePane.setMaximumSize(readmeSize);
        readmePane.setPreferredSize(readmeSize);
        readmePane.add(readme);
        mainPane.add(readmePane);

        Document doc = VariantSupport.loadVariant(variantName);
        readme.setContentType((String)doc.getProperty(
            ResourceLoader.keyContentType));
        readme.setDocument(doc);
        options.setOption(Options.variant,variantName);

        enablePlayers();

        pack();
        
        centerOnScreen();

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
        for (int i = 0 ; i < Constants.numAITypes; i++) 
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

        String name = options.getStringOption(Options.playerName + i);
        if (name == null || name.length() == 0)
        {
            name = Constants.none;
        }
        if (name.startsWith(Constants.byColor))
        {
            name = Constants.byColor;
        }

        Vector nameChoices = new Vector();
        nameChoices.add(name);
        if (!nameChoices.contains(Constants.byColor))
        {
            nameChoices.add(Constants.byColor);
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
        java.util.List names = new ArrayList();
        java.util.List types = new ArrayList();

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
                    name.equals(Constants.byClient))
                {
                    name = name + i;
                }
                // Duplicate names are not allowed.
                if (names.contains(name))
                {
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
        chooser.setFileFilter(new SaveGameFilter());
        int returnVal = chooser.showOpenDialog(parentFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            options.clearPlayerInfo();
            // Set key to "load game" and value to savegame filename.
            options.setOption(Constants.loadGame, 
                chooser.getSelectedFile().getName());
            dispose();
        }
    }


    private void doRunClient()
    {
Log.debug("GetPlayers.doRunClient()");
        setVisible(false);
        options.setOption(Constants.runClient, true);
Log.debug("after setting runClient true: " + options.toString());
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
            if (f.getName().endsWith(".var")) 
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
        int returnVal = varChooser.showOpenDialog(varChooser);
        String varName = Constants.defaultVARFile;
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
        {
            File varFile = varChooser.getSelectedFile().getAbsoluteFile();
            Document doc = VariantSupport.loadVariant(varFile);
            if (doc != null)
            {
                String name = varFile.getName();
                name = name.substring(0, name.lastIndexOf(".var"));
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
            final int newDelay = PickIntValue.pickIntValue(parentFrame,
                oldDelay, "Pick AI Delay (in ms)", Constants.MIN_AI_DELAY, 
                Constants.MAX_AI_DELAY);
            if (newDelay != oldDelay)
            {
                options.setOption(Options.aiDelay, newDelay);
            }
            setDelayLabel(newDelay);
        }
        else if (e.getActionCommand().equals(Options.aiTimeLimit))
        {
            final int newLimit = PickIntValue.pickIntValue(parentFrame,
                oldLimit, "Pick AI Time Limit (in s)", 
                Constants.MIN_AI_TIME_LIMIT, Constants.MAX_AI_TIME_LIMIT);
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
                    varName.lastIndexOf(".var"));
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
                if (VariantSupport.getVarName().equals(value + ".var"))
                { // re-selecting the same ; do nothing
                }
                else
                { // selecting different ; remove all non-included
                    if (variantBox.getItemCount() > Constants.numVariants)
                    {
                        variantBox.removeItemAt(Constants.numVariants);
                    }
                    Document doc = VariantSupport.loadVariant(value);
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
}
