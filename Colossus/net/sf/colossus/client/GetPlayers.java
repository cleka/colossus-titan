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
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.SaveGameFilter;
import net.sf.colossus.server.Game;
import net.sf.colossus.client.VariantSupport;

/**
 * Class GetPlayers is a dialog used to enter players' names, types, variant, etc. 
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */


public final class GetPlayers extends KDialog implements WindowListener,
    ActionListener
{
    public static final String newGame = "New Game";
    public static final String loadGame = "Load Game";
    public static final String loadVariant = "Load External Variant";
    public static final String quit = "Quit";
    public static final String none = "None";
    public static final String byColor = "<By color>";
    public static final String username = System.getProperty("user.name",
        byColor);
    private static String [] nameChoices = { byColor, username, none };

    private JFrame parentFrame;

    private JComboBox [] playerTypes = new JComboBox[6];
    private JComboBox [] playerNames = new JComboBox[6];
    private JEditorPane readme = null;
    private JScrollPane scrollPane = null;

    /** List of Map.Entry objects that map player names to player types */
    private static java.util.List playerStuff = new ArrayList();

    private static String anyAI = "A Random AI";
    /* aiList should match the class name of available AI */
    private static String[] aiList = { "SimpleAI", "CowardSimpleAI" };
    /* default AI should be one of aiList or anyAI */
    public static String defaultAI = anyAI;

    /** list of available Variant */
    private static String[] variantArray =
    {
        "Default",
        "TitanPlus",
        "ExtTitan", 
        "Badlands",
        "Outlands",
        "Undead"
    };
    private JComboBox variantBox;
    private java.util.List variantList;

    private GetPlayers(JFrame parentFrame)
    {
        super(parentFrame, "Player Setup", true);

        int ainum = 0;
        for (int i = 0 ; i < aiList.length ; i++) 
        {
            if (!(aiList[i].equals("")))
            {
                ainum++;
            }
        }
        // Use a Vector because JComboBox does not know about Lists.
        Vector typeChoices = new Vector();
        typeChoices.add("Human");
        typeChoices.add("None");
        for (int i = 0 ; i < aiList.length ; i++) 
        {
            if (!(aiList[i].equals("")))
            {
                typeChoices.add(aiList[i]);
            }
        }
        // Only show random AI choice if more than one AI.
        if (ainum >= 2)
        {
            typeChoices.add(anyAI);
        }

        this.parentFrame = parentFrame;
        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();
        BoxLayout baseLayout = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
        contentPane.setLayout(baseLayout);

        for (int i = 0; i < 6; i++)
        {
            Container playerPane = new Container();
            playerPane.setLayout(new GridLayout(0, 3));
            contentPane.add(playerPane);
            
            String s = "Player " + (i + 1);
            playerPane.add(new JLabel(s));
            
            JComboBox playerType = new JComboBox(typeChoices);
            if (i == 0)
            {
                playerType.setSelectedItem("Human");
            }
            else
            {
                playerType.setSelectedItem(defaultAI);
            }
            playerPane.add(playerType);
            playerType.addActionListener(this);
            playerTypes[i] = playerType;

            JComboBox playerName = new JComboBox(nameChoices);
            playerName.setEditable(true);
            if (i == 0)
            {
                playerName.setSelectedItem(username);
            }
            playerPane.add(playerName);
            playerName.addActionListener(this);
            playerNames[i] = playerName;
        }

        Container gamePane = new Container();
        gamePane.setLayout(new GridLayout(0, 3));
        Container variantPane = new Container();
        variantPane.setLayout(new GridLayout(0, 2));

        contentPane.add(gamePane);
        contentPane.add(variantPane);
        
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
        
        variantBox = new JComboBox(variantArray);
        variantBox.addActionListener(this);
        variantBox.setSelectedItem(variantArray[0]);
        variantList = new ArrayList();
        for(int i = 0; i < variantArray.length; i++)
        {
            variantList.add(variantArray[i]);
        }
        variantPane.add(variantBox);
        JButton buttonVariant =
            new JButton(loadVariant);
        variantPane.add(buttonVariant);
        buttonVariant.addActionListener(this);

        JPanel readmePane = new JPanel();
        readmePane.setLayout(new GridLayout(0, 1));
        readme = new JEditorPane();
        readme.setEditable(false);
        scrollPane = new JScrollPane(readme);
        readmePane.add(scrollPane);
        contentPane.add(readmePane);

        Document doc =
            VariantSupport.loadVariant(variantArray[0] + ".var",
                                       variantArray[0]);
        readme.setContentType((String)doc.getProperty(
            ResourceLoader.keyContentType));
        readme.setDocument(doc);

        pack();
        
        centerOnScreen();

        addWindowListener(this);
        setVisible(true);
    }


    /** Return a List of Strings containing tilde-separated name/type. */
    public static java.util.List getPlayers(JFrame parentFrame)
    {
        new GetPlayers(parentFrame);
        return playerStuff;
    }


    /** Start new game if values are legal. */
    private void validateInputs()
    {
        playerStuff.clear();
        Set namesTaken = new HashSet();
        int numPlayers = 0;
        Random aiRand = new Random();

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
                if (namesTaken.contains(name))
                {
                    return;
                }
                if (type.equals(anyAI))
                {
                    type = aiList[aiRand.nextInt(aiList.length)];
                }
                numPlayers++;
                String entry = name + "~" + type;
                playerStuff.add(entry);
                namesTaken.add(name);
            }
        }

        // Exit if there aren't enough unique player names.
        if (numPlayers < 1 || playerStuff.size() != numPlayers)
        {
            return;
        }

        // Will eliminate modal dialog, allowing game to start.
        dispose();
    }


    private void doLoadGame()
    {
        JFileChooser chooser = new JFileChooser(Constants.saveDirname);
        chooser.setFileFilter(new SaveGameFilter());
        int returnVal = chooser.showOpenDialog(parentFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            playerStuff.clear();
            // Set key to "load game" and value to savegame filename.
            playerStuff.add(loadGame + "~" +
                chooser.getSelectedFile().getName());
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
            readme.setContentType((String)doc.getProperty(
                ResourceLoader.keyContentType));
            readme.setDocument(doc);
        }
    }

    public synchronized void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(quit))
        {
            playerStuff.clear();
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
        else if (e.getActionCommand().startsWith(loadVariant))
        {
            doLoadVariant();
            String varName = VariantSupport.getVarName();
            if (!(variantList.contains(varName)))
            {
                String buttonName =
                    varName.substring(0,
                                      varName.lastIndexOf(".var"));
                if (variantBox.getItemCount() > variantArray.length)
                    variantBox.removeItemAt(variantArray.length);
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
                    if (variantBox.getItemCount() > variantArray.length)
                        variantBox.removeItemAt(variantArray.length);
                    Document doc =
                        VariantSupport.loadVariant(value + ".var",
                                                   value);
                    readme.setContentType((String)doc.getProperty(
                        ResourceLoader.keyContentType));
                    readme.setDocument(doc);
                }
            }
            else
            for (int i = 0; i < 6; i++)
            {
                JComboBox box = playerTypes[i];
                if (box == source)
                {
                    // If player type was changed to none, also change player
                    // name to none.
                    String value = (String)box.getSelectedItem();
                    if (value.equals(none))
                    {
                        playerNames[i].setSelectedItem(none);
                    }
                }

                box = playerNames[i];
                if (box == source)
                {
                    // If player name was changed to none, also change player
                    // type to none.
                    String value = (String)box.getSelectedItem();
                    if (value.equals(none))
                    {
                        playerTypes[i].setSelectedItem(none);
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
}
