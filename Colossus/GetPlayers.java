import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


/**
 * Class GetPlayers is a dialog used to enter players' names.
 * @version $Id$
 * @author David Ripton
 */


public final class GetPlayers extends JDialog implements WindowListener,
    ActionListener
{
    public static String newGame = "New Game";
    public static String loadGame = "Load Game";
    public static String quit = "Quit";
    public static String human = "Human";
    public static String ai = "AI";
    public static String none = "None";
    public static String byColor = "<By color>";
    public static String username = System.getProperty("user.name", byColor);
    public static String [] typeChoices = { ai, human, none };
    public static String [] nameChoices = { byColor, username, none };

    private JFrame parentFrame;

    private JComboBox [] playerTypes = new JComboBox[6];
    private JComboBox [] playerNames = new JComboBox[6];

    /** List of Map.Entry objects that map player names to player types */
    private static ArrayList playerInfo = new ArrayList();


    private GetPlayers(JFrame parentFrame)
    {
        super(parentFrame, "Player Setup", true);

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
                playerType.setSelectedItem(human);
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
}
