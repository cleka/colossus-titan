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
    public static String loadGame = "Load Game";
    public static String newGame = "New Game";
    public static String quit = "Quit";
    public static String human = "Human";
    public static String ai = "AI";
    public static String def = "Default";
    public static String byColor = "<By color>";
    public static String none = "None";

    private JFrame parentFrame;
    private ArrayList textFields = new ArrayList();
    private ArrayList radioButtons = new ArrayList();
    /** Maps player name to one of "Human" "AI" or "Default" */
    private static TreeMap playerInfo = new TreeMap();
    private String [] autoPlay = new String[6];


    private GetPlayers(JFrame parentFrame)
    {
        super(parentFrame, "Player Setup", true);

        this.parentFrame = parentFrame;
        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();

        contentPane.setLayout(new GridLayout(0, 5));

        for (int i = 0; i < 6; i++)
        {
            String s = "Player " + (i + 1) + " Name";
            contentPane.add(new JLabel(s));
            JTextField tf = new JTextField(20);
            contentPane.add(tf);
            textFields.add(tf);

            ButtonGroup bg = new ButtonGroup();

            JRadioButton button = new JRadioButton(def, true);
            bg.add(button);
            contentPane.add(button);
            radioButtons.add(button);
            button.addActionListener(this);

            button = new JRadioButton(human, false);
            bg.add(button);
            contentPane.add(button);
            radioButtons.add(button);
            button.addActionListener(this);

            button = new JRadioButton(ai, false);
            bg.add(button);
            contentPane.add(button);
            radioButtons.add(button);
            button.addActionListener(this);
        }

        // Fix tab order.
        for (int i = 0; i < 6; i++)
        {
            JTextField tf = (JTextField)textFields.get(i);
            JTextField next = (JTextField)textFields.get((i + 1) % 6);
            tf.setNextFocusableComponent(next);
        }

        JButton button1 = new JButton("New Game");
        button1.setMnemonic(KeyEvent.VK_N);
        contentPane.add(button1);
        button1.addActionListener(this);
        JButton button2 = new JButton("Load Game");
        button2.setMnemonic(KeyEvent.VK_L);
        contentPane.add(button2);
        button2.addActionListener(this);
        JButton button3 = new JButton("Quit");
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


    public static TreeMap getPlayers(JFrame parentFrame)
    {
        new GetPlayers(parentFrame);
        return playerInfo;
    }


    private void validateInputs()
    {
        playerInfo.clear();
        int numPlayers = 0;
        int i = 0;

        Iterator it = textFields.iterator();
        while (it.hasNext())
        {
            JTextField tf = (JTextField)it.next();
            String text = tf.getText();
            if (text.length() > 0)
            {
                numPlayers++;
                String ap = autoPlay[i];
                if (ap == null)
                {
                    ap = def;
                }
                playerInfo.put(text, ap);
            }
            i++;
        }

        // Make sure that there is at least one player, and
        // that each player has a unique name.
        if (numPlayers < 1 || playerInfo.size() != numPlayers)
        {
            it = textFields.iterator();
            while (it.hasNext())
            {
                JTextField tf = (JTextField)it.next();
                tf.setText("");
            }
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
            playerInfo.put(loadGame, chooser.getSelectedFile().getName());
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
            // Radio button.  Set the corresponding player's autoPlay value.
            int buttonNum = radioButtons.indexOf(e.getSource());
            int playerNum = buttonNum / 3;
            autoPlay[playerNum] = e.getActionCommand();
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
        TreeMap info = getPlayers(new JFrame());

        // See if user hit the Load game button, and we should
        // load a game instead.
        if (playerInfo.size() == 1)
        {
            String key = (String)playerInfo.firstKey();
            if (key.equals(loadGame))
            {
                String filename = (String)playerInfo.get(key);
                System.out.println("Would load game from " + filename);
                System.exit(0);
            }
        }

        Iterator it = info.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();
            String ap = (String)entry.getValue();
            System.out.println("Add " + ap + " player " + name);
        }
        System.exit(0);
    }
}
