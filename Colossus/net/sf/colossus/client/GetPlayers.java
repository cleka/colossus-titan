package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.Split;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.SaveGameFilter;
import net.sf.colossus.server.Game;
import net.sf.colossus.client.VariantSupport;

/**
 * Class GetPlayers is a dialog used to enter players' names.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */


public final class GetPlayers extends JDialog implements WindowListener,
    ActionListener
{
    public static final int maxAIsupported = 16;

    public static final String newGame = "New Game";
    public static final String loadGame = "Load Game";
    public static final String loadVariant = "Load External Variant";
    public static final String quit = "Quit";
    public static final String none = "None";
    public static final String byColor = "<By color>";
    public static final String username = System.getProperty("user.name",
        byColor);
    private static String [] typeChoices = null;
    private static String [] nameChoices = { byColor, username, none };

    private JFrame parentFrame;

    private JComboBox [] playerTypes = new JComboBox[6];
    private JComboBox [] playerNames = new JComboBox[6];

    /** List of Map.Entry objects that map player names to player types */
    private static java.util.List playerInfo = new ArrayList();

    private static String anyAI = "A Random AI";
    private static String defaultAI = "SimpleAI";
    /* aiList should match the class name of available AI */
    // XXX MinimaxAI is currently very broken.
    //private static String[] aiList = { "SimpleAI", "MinimaxAI" };
    private static String[] aiList = { "SimpleAI" };

    /** list of available Variant */
    private static String[] variantArray =
    {
        "Default",
        "TitanPlus",
        "ExtTitan", 
        "Badlands",
        "Outlands"
    };
    private JComboBox variantBox;
    private java.util.List variantList;

    private GetPlayers(JFrame parentFrame)
    {
        super(parentFrame, "Player Setup", true);

        /* get the list of the available AI */
        /* not reliable yet */
        // aiList = getAIList();

        int ainum = 0, j = 0;
        for (int i = 0 ; i < aiList.length ; i++) 
        {
            if (!(aiList[i].equals("")))
            {
                ainum++;
            }
        }
        typeChoices = new String[3 + ainum];
        typeChoices[0] = "Human";
        typeChoices[1] = "None";
        j = 2;
        for (int i = 0 ; i < aiList.length ; i++) 
        {
            if (!(aiList[i].equals("")))
            {
                typeChoices[j] = aiList[i];
            }
            j++;
        }
        typeChoices[2 + ainum] = anyAI;

        this.parentFrame = parentFrame;
        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();
        GridLayout baseLayout = new GridLayout(0, 1);
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
                playerType.setSelectedItem(aiList[0]);
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

        pack();

        // Center dialog on screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));

        addWindowListener(this);
        setVisible(true);
    }


    /** Return a List of Strings containing tilde-separated name/type. */
    public static java.util.List getPlayers(JFrame parentFrame)
    {
        new GetPlayers(parentFrame);
        return playerInfo;
    }


    private void validateInputs()
    {
        playerInfo.clear();
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
                playerInfo.add(entry);
                namesTaken.add(name);
            }
        }

        // Exit if there aren't enough unique player names.
        if (numPlayers < 1 || playerInfo.size() != numPlayers)
        {
            return;
        }

        dispose();
    }


    private void doLoadGame()
    {
        JFileChooser chooser = new JFileChooser(Constants.saveDirname);
        chooser.setFileFilter(new SaveGameFilter());
        int returnVal = chooser.showOpenDialog(parentFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            playerInfo.clear();
            // Set key to "load game" and value to savegame filename.
            playerInfo.add(loadGame + "~" +
                chooser.getSelectedFile().getName());
            dispose();
        }
    }


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
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

    private static void doLoadVariant()
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
            VariantSupport.loadVariant(varFile);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(quit))
        {
            playerInfo.clear();
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
                    VariantSupport.loadVariant(value + ".var", value);
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
        return new Dimension(500, 400);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }
}
