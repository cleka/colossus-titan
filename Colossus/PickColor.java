import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


/**
 * Class PickColor lets a player choose a color of legion markers.
 * @version $Id$
 * @author David Ripton
 */


public final class PickColor extends JDialog implements WindowListener,
    ActionListener
{
    private JLabel [] colorLabel = new JLabel[6];
    private Game game;
    private Player player;
    private static final String [] colorNames =
        {"Black", "Blue", "Brown", "Gold", "Green", "Red"};
    private static final int [] colorMnemonics =
        {KeyEvent.VK_B, KeyEvent.VK_L, KeyEvent.VK_O, KeyEvent.VK_G,
            KeyEvent.VK_E, KeyEvent.VK_R};
    private static String color;


    private PickColor(JFrame parentFrame, Game game, Player player)
    {
        super(parentFrame, player.getName() + ", Pick a Color", true);
        this.game = game;
        this.player = player;

        color = null;

        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();

        contentPane.setLayout(new GridLayout(0, 3));

        contentPane.add(new JLabel("Tower"));
        contentPane.add(new JLabel("Name"));
        contentPane.add(new JLabel("Color"));

        boolean [] colorTaken = new boolean[6];
        for (int i = 0; i < 6; i++)
        {
            colorTaken[i] = false;
        }

        Collection players = game.getPlayers();
        Iterator it = players.iterator();
        int i = 0;
        while (it.hasNext())
        {
            Player currentPlayer = (Player)it.next();
            int tower = currentPlayer.getTower();
            contentPane.add(new JLabel(String.valueOf(100 * tower)));
            contentPane.add(new JLabel(currentPlayer.getName()));
            String color = currentPlayer.getColor();

            if (color == null)
            {
                if (currentPlayer == player)
                {
                    colorLabel[i] = new JLabel("?");
                }
                else
                {
                    colorLabel[i] = new JLabel("");
                }
            }
            else
            {
                colorLabel[i] = new JLabel(color);
                if (colorNumber(color) != -1)
                {
                    colorTaken[colorNumber(color)] = true;
                }
            }

            contentPane.add(colorLabel[i]);
            i++;
        }

        Color [] background = { Color.black, Color.blue, HTMLColor.brown,
            Color.yellow, Color.green, Color.red };
        Color [] foreground = { Color.white, Color.white, Color.white,
            Color.black, Color.black, Color.black };

        for (i = 0; i < 6; i++)
        {
            JButton button = new JButton();
            if (colorTaken[i])
            {
                button.setBackground(Color.lightGray);
                button.setForeground(Color.black);
            }
            else
            {
                button.setText(colorNames[i]);
                button.setMnemonic(colorMnemonics[i]);
                button.setBackground(background[i]);
                button.setForeground(foreground[i]);
                button.addActionListener(this);
            }
            contentPane.add(button);
        }

        pack();

        // Center dialog on screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));

        addWindowListener(this);
        setVisible(true);
    }


    public static String pickColor(JFrame parentFrame, Game game,
        Player player)
    {
        new PickColor(parentFrame, game, player);
        return color;
    }


    private int colorNumber(String colorName)
    {
        for (int i = 0; i < 6; i++)
        {
            if (colorName.equals(colorNames[i]))
            {
                return i;
            }
        }

        return -1;
    }


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
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
        color = e.getActionCommand();
        dispose();
    }


    public Dimension getMinimumSize()
    {
        return new Dimension(350, 350);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }


    public static void main(String [] args)
    {
        Game game = new Game();
        JFrame frame = new JFrame();

        Player p0 = new Player("Bo", game);
        Player p1 = new Player("Luke", game);
        game.addPlayer(p0);
        game.addPlayer(p1);

        String answer = pickColor(frame, game, p0);
        Game.logEvent(p0.getName() + " chooses color " + answer);
        p0.setColor(answer);

        answer = pickColor(frame, game, p1);
        Game.logEvent(p1.getName() + " chooses color " + answer);
        p1.setColor(answer);

        System.exit(0);
    }
}
