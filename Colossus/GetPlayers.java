import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
//import java.lang.reflect.*;  Needed?

/**
 * Class GetPlayers is a dialog used to enter players' names.
 * @version $Id$
 * @author David Ripton
 */


public final class GetPlayers extends JDialog implements WindowListener,
    ActionListener
{
    public static final int maxAIsupported = 16;
    public static final String newGame = "New Game";
    public static final String loadGame = "Load Game";
    public static final String loadMap = "Load Map";
    public static final String loadRec = "Load Recruiters";
    public static final String quit = "Quit";
    public static final String none = "None";
    public static final String byColor = "<By color>";
    public static final String username = System.getProperty("user.name",
        byColor);
    private static String [] typeChoices = null;;
    private static String [] nameChoices = { byColor, username, none };

    private JFrame parentFrame;

    private JComboBox [] playerTypes = new JComboBox[6];
    private JComboBox [] playerNames = new JComboBox[6];

    /** List of Map.Entry objects that map player names to player types */
    private static ArrayList playerInfo = new ArrayList();

    private static String mapName = "StrategicMap.map";
    private static String recruitName = "Recruit.ter";
    private static String anyAI = "A Random AI";
    private static String defaultAI = "SimpleAI";
    /* aiList should match the class name of available AI */
    private static String[] aiList = { "SimpleAI", "MinimaxAI" };

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

        contentPane.setLayout(new GridLayout(0, 3));

        for (int i = 0; i < 6; i++)
        {
            String s = "Player " + (i + 1);
            contentPane.add(new JLabel(s));

            JComboBox playerType = new JComboBox(typeChoices);
            if (i == 0)
            {
                playerType.setSelectedItem("Human");
            }
            else
            {
                playerType.setSelectedItem(aiList[0]);
            }
            contentPane.add(playerType);
            playerType.addActionListener(this);
            playerTypes[i] = playerType;

            JComboBox playerName = new JComboBox(nameChoices);
            playerName.setEditable(true);
            if (i == 0)
            {
                playerName.setSelectedItem(username);
            }
            contentPane.add(playerName);
            playerName.addActionListener(this);
            playerNames[i] = playerName;
        }


        JButton button1 = new JButton(newGame);
        button1.setMnemonic(KeyEvent.VK_N);
        contentPane.add(button1);
        button1.addActionListener(this);
        JButton button2 = new JButton(loadGame);
        button2.setMnemonic(KeyEvent.VK_L);
        contentPane.add(button2);
        button2.addActionListener(this);
        JButton button3 = new JButton(quit);
        button3.setMnemonic(KeyEvent.VK_Q);
        contentPane.add(button3);
        button3.addActionListener(this);
        JButton button4 = new JButton(loadMap);
        contentPane.add(button4);
        button4.addActionListener(this);
        JButton button5 = new JButton(loadRec);
        contentPane.add(button5);
        button5.addActionListener(this);

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
        HashSet namesTaken = new HashSet();
        int numPlayers = 0;
        Random aiRand = new Random();

        for (int i = 0; i < 6; i++)
        {
            String name = (String)(playerNames[i].getSelectedItem());
            String type = (String)(playerTypes[i].getSelectedItem());
            if (name.length() > 0 && !name.equals(none) && !type.equals(none))
            {
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
                if (!name.equals(byColor))
                {
                    namesTaken.add(name);
                }
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
        JFileChooser chooser = new JFileChooser(Game.saveDirname);
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

    static class mapFileFilter extends javax.swing.filechooser.FileFilter 
    {
        public boolean accept(File f) 
        {
            if (f.isDirectory()) 
            {
                return(true);
            }
            if (f.getName().endsWith(".map")) 
            {
                return(true);
            }
            return(false);
        }
        public String getDescription() 
        {
            return("Colossus MAP file");
        }
    }

    public static String getMapName()
    {
        return mapName;
    }
    
    static class recFileFilter extends javax.swing.filechooser.FileFilter 
    {
        public boolean accept(File f) 
        {
            if (f.isDirectory()) 
            {
                return(true);
            }
            if (f.getName().endsWith(".ter")) 
            {
                return(true);
            }
            return(false);
        }
        public String getDescription() 
        {
            return("Colossus RECruiters file");
        }
    }

    public static String getRecruitName()
    {
        return recruitName;
    }
    
    private static String chooseMap() 
    {
        javax.swing.JFileChooser mapChooser = new JFileChooser();
        mapChooser.setFileFilter(new mapFileFilter());
        mapChooser.setDialogTitle(
            "Choose your map (or cancel for default map)");
        int returnVal = mapChooser.showOpenDialog(mapChooser);
        String mapName = "StrategicMap.map";
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) 
        {
            mapName = mapChooser.getSelectedFile().getAbsolutePath();
        }
        return (mapName);
    }

    private static String chooseRec() 
    {
        javax.swing.JFileChooser recChooser = new JFileChooser();
        recChooser.setFileFilter(new recFileFilter());
        recChooser.setDialogTitle(
            "Choose your recruiters base (or cancel for default base)");
        int returnVal = recChooser.showOpenDialog(recChooser);
        String recName = "Recruit.ter";
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) 
        {
            recName = recChooser.getSelectedFile().getAbsolutePath();
        }
        return (recName);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(quit))
        {
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
        else if (e.getActionCommand().equals(loadMap))
        {
            mapName = chooseMap();
        }
        else if (e.getActionCommand().equals(loadRec))
        {
            recruitName = chooseRec();
        }
        else
        {
            // A combo box was changed.
            Object source = e.getSource();

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


    public static void main(String [] args)
    {
        Game game = new Game();
        java.util.List info = getPlayers(new JFrame());

        // See if user hit the Load game button, and we should
        // load a game instead.
        if (info.size() == 1)
        {
            String entry = (String)info.get(0);
            java.util.List values = Utils.split('~', entry);
            String key = (String)values.get(0);
            if (key.equals(loadGame))
            {
                String filename = (String)values.get(1);
                System.out.println("Would load game from " + filename);
                System.exit(0);
            }
        }

        Iterator it = info.iterator();
        while (it.hasNext())
        {
            String entry = (String)it.next();
            java.util.List values = Utils.split('~', entry);
            String name = (String)values.get(0);
            String type = (String)values.get(1);
            System.out.println("Add " + type + " player " + name);
        }
        System.exit(0);
    }

    /*
    class AIFilenameFilter implements FilenameFilter 
    {
        public boolean accept(File dir, String name) 
        {
            if (name.endsWith("AI.class"))
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        public String getDescription() 
        {
            return("Filter file that ends in AI.class");
        }
    }

    public String[] getAIList() 
    { // not working properly yet.
        String [] aiList = new String[maxAIsupported];
        File simpleAIFile = new File("SimpleAI.class");
        int ainum = 0;
        for (int i = 0; i < maxAIsupported; i++)
        {
            aiList[i] = "";
        }
        
        Class interfaceAIClass = null;
        try 
        {
            interfaceAIClass = Class.forName("AI");
        } 
        catch (Exception e) 
        {
            System.out.println("Checking AI interface failed : " + e);
            System.exit(1);
        }

        String [] tempAIList = simpleAIFile.getAbsoluteFile().
            getParentFile().list(new AIFilenameFilter());
        for (int i = 0; i < tempAIList.length ; i++) 
        {
            if ((!tempAIList[i].equals("AI.class")) && 
                (ainum < maxAIsupported)) 
            {
                String aiClassName = tempAIList[i].substring(0, 
                    tempAIList[i].lastIndexOf(".class"));
                Class aiClass = null;

                System.out.print("Checking AI by the name of " + aiClassName);
                try 
                {
                    aiClass = Class.forName(aiClassName);
                } 
                catch (Exception e) 
                {
                    System.out.println(" ; Checking failed : " + e);
                    System.exit(1);
                }
                if (interfaceAIClass.isAssignableFrom(aiClass)) 
                {
                    System.out.println(" ; success");
                    aiList[ainum] = aiClassName; ainum++;
                }
                else
                {
                    System.out.println(" ; failure");
                }
            }
        }
        return(aiList);
    }
    */

}
