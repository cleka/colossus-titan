package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;

import net.sf.colossus.util.Split;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.SaveGameFilter;
import net.sf.colossus.client.VariantSupport;

/**
 * Class GetPlayers is a dialog used to enter players' names, types, variant, etc. 
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */


public final class GetPlayers extends KDialog implements WindowListener,
    ActionListener, ItemListener
{
    public static final String newGame = "New Game";
    public static final String loadGame = "Load Game";
    public static final String loadVariant = "Load External Variant";
    public static final String quit = "Quit";
    public static final String none = "None";
    public static final String byColor = "<By color>";
    public static final String username = System.getProperty("user.name",
        byColor);

    private JFrame parentFrame;

    private Vector typeChoices = new Vector();
    private JComboBox [] playerTypes = new JComboBox[6];
    private JComboBox [] playerNames = new JComboBox[6];
    private JEditorPane readme = new JEditorPane();
    private JScrollPane scrollPane = null;

    private JComboBox variantBox;

    /** This is Game's options, which we will modify directly. */
    private Options options;

    private Container checkboxPane;


    /** Clear options to abort */
    public GetPlayers(JFrame parentFrame, Options options)
    {
        super(parentFrame, "Player Setup", true);

        this.options = options;

        setupTypeChoices();

        this.parentFrame = parentFrame;
        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();
        BoxLayout baseLayout = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
        contentPane.setLayout(baseLayout);

        for (int i = 0; i < 6; i++)
        {
            doOnePlayer(i);
        }

        Container gamePane = new Container();
        gamePane.setLayout(new GridLayout(0, 3));
        contentPane.add(gamePane);
        
        JButton button1 = new JButton(newGame);
        button1.setMnemonic(KeyEvent.VK_N);
        gamePane.add(button1);
        button1.addActionListener(this);
        JButton button2 = new JButton(loadGame);
        button2.setMnemonic(KeyEvent.VK_L);
        gamePane.add(button2);
        button2.addActionListener(this);
        JButton button3 = new JButton(quit);
        button3.setMnemonic(KeyEvent.VK_Q);
        gamePane.add(button3);
        button3.addActionListener(this);

        checkboxPane = new JPanel();
        checkboxPane.setLayout(new GridLayout(0, 3));
        contentPane.add(checkboxPane);
        
        addCheckbox(Options.autosave);
        addCheckbox(Options.logDebug);
        addCheckbox(Options.balancedTowers);
        addCheckbox(Options.allStacksVisible);
        addCheckbox(Options.autoStop);
        addCheckbox(Options.autoQuit);


        Container delayPane = new JPanel();
        BoxLayout delayLayout = new BoxLayout(delayPane, BoxLayout.X_AXIS);
        delayPane.setLayout(delayLayout);
        JButton delayButton = new JButton(options.aiDelay);
        delayButton.addActionListener(this);
        delayPane.add(delayButton);
        contentPane.add(delayPane);
        

        Container variantPane = new Container();
        variantPane.setLayout(new GridLayout(0, 2));
        contentPane.add(variantPane);

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
        scrollPane = new JScrollPane(readme);
        readmePane.add(scrollPane);
        contentPane.add(readmePane);

        Document doc = VariantSupport.loadVariant(variantName);
        readme.setContentType((String)doc.getProperty(
            ResourceLoader.keyContentType));
        readme.setDocument(doc);
        options.setOption(Options.variant,variantName);

        pack();
        
        centerOnScreen();

        addWindowListener(this);
        setVisible(true);
    }

    private void setupTypeChoices()
    {
        typeChoices.clear();
        typeChoices.add("Human");
        typeChoices.add("None");
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
    }

    private void doOnePlayer(final int i)
    {
        Container playerPane = new Container();
        playerPane.setLayout(new GridLayout(0, 3));
        getContentPane().add(playerPane);
        
        String s = "Player " + (i + 1);
        playerPane.add(new JLabel(s));
        
        JComboBox playerType = new JComboBox(typeChoices);
        
        String type = options.getStringOption(Options.playerType + i);
        if (type == null || type.length() == 0)
        {
            type = none;
        }
        playerType.setSelectedItem(type);

        playerPane.add(playerType);
        playerType.addActionListener(this);
        playerTypes[i] = playerType;

        String name = options.getStringOption(Options.playerName + i);
        if (name == null || name.length() == 0)
        {
            name = none;
        }
        if (name.startsWith(byColor))
        {
            name = byColor;
        }

        Vector nameChoices = new Vector();
        nameChoices.add(name);
        if (!nameChoices.contains(byColor))
        {
            nameChoices.add(byColor);
        }
        if (!nameChoices.contains(username))
        {
            nameChoices.add(username);
        }
        if (!nameChoices.contains(none))
        {
            nameChoices.add(none);
        }

        JComboBox playerName = new JComboBox(nameChoices);
        playerName.setEditable(true);
        playerPane.add(playerName);
        playerName.addActionListener(this);
        playerNames[i] = playerName;
    }


    private void addCheckbox(String optname)
    {
        JCheckBox cb = new JCheckBox(optname);
        cb.setSelected(options.getOption(optname));
        cb.addItemListener(this);
        checkboxPane.add(cb);
    }


    /** Start new game if values are legal. */
    private void validateInputs()
    {
        options.clearPlayerInfo();

        int numPlayers = 0;
        java.util.List names = new ArrayList();
        java.util.List types = new ArrayList();

        for (int i = 0; i < 6; i++)
        {
            String name = (String)(playerNames[i].getSelectedItem());
            String type = (String)(playerTypes[i].getSelectedItem());
            if (name.length() > 0 && !name.equals(none) && !type.equals(none))
            {
                // Make byColor names unique by appending row number.
                if (name.equals(byColor))
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
            options.setOption(loadGame, chooser.getSelectedFile().getName());
            dispose();
        }
    }


    public void windowClosing(WindowEvent e)
    {
        dispose();
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
        javax.swing.JFileChooser varChooser = new JFileChooser(".");
        varChooser.setFileFilter(new varFileFilter());
        varChooser.setDialogTitle(
            "Choose your variant (or cancel for default game)");
        int returnVal = varChooser.showOpenDialog(varChooser);
        String varName = Constants.defaultVARFile;
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
        {
            File varFile = varChooser.getSelectedFile();
            Document doc = VariantSupport.loadVariant(varFile);
            if (doc != null)
            {
                String name = varFile.getName();
                name = name.substring(0,name.lastIndexOf(".var"));
                options.setOption(Options.variant,
                                  name);
                readme.setContentType((String)doc.getProperty(
                           ResourceLoader.keyContentType));
                readme.setDocument(doc);
            }
        }
    }

    public synchronized void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(quit))
        {
            options.clear();
            dispose();
        }
        else if (e.getActionCommand().equals(newGame))
        {
            validateInputs();
        }
        else if (e.getActionCommand().equals(loadGame))
        {
            doLoadGame();
        }
        else if (e.getActionCommand().equals(Options.aiDelay))
        {
            int oldDelay = options.getIntOption(Options.aiDelay);
            if (oldDelay < Constants.MIN_DELAY || 
                oldDelay > Constants.MAX_DELAY)
            {
                oldDelay = Constants.DEFAULT_DELAY;
            }

            final int newDelay = PickIntValue.pickIntValue(parentFrame,
                oldDelay, "Pick AI Delay", Constants.MIN_DELAY, 
                Constants.MAX_DELAY);
            if (newDelay != oldDelay)
            {
                options.setOption(Options.aiDelay, newDelay);
            }
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
                    options.setOption(Options.variant,value);
                    String prop = (String)doc.getProperty(
                           ResourceLoader.keyContentType);
                    readme.setContentType(prop);
                    readme.setDocument(doc);
                }
            }
            else
            {
                for (int i = 0; i < 6; i++)
                {
                    JComboBox box = playerTypes[i];
                    if (box == source)
                    {
                        // If player type was changed to none, also change 
                        // player name to none.
                        String value = (String)box.getSelectedItem();
                        if (value.equals(none))
                        {
                            playerNames[i].setSelectedItem(none);
                        }
                        // If player type was changed away from none, also 
                        // change player name to something else.
                        else if (playerNames[i].getSelectedItem().equals(none))
                        {
                            playerNames[i].setSelectedItem(byColor);
                        }
                    }
    
                    box = playerNames[i];
                    if (box == source)
                    {
                        // If player name was changed to none, also change 
                        // player type to none.
                        String value = (String)box.getSelectedItem();
                        if (value.equals(none))
                        {
                            playerTypes[i].setSelectedItem(none);
                        }
                        // If player type was changed away from none, also 
                        // change player name to something else.
                        else if (playerTypes[i].getSelectedItem().equals(none))
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
        return getMinimumSize();
    }


    public void itemStateChanged(ItemEvent e)
    {
        JCheckBox source = (JCheckBox)e.getSource();
        String text = source.getText();
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
        options.setOption(text, selected);
    }
}
