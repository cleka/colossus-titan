import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


/**
 * Class PickColor lets a player choose a color of legion markers.
 * @version $Id$ 
 * @author David Ripton
 */


public class PickColor extends JDialog implements WindowListener, ActionListener
{
    private JLabel [] colorLabel = new JLabel[6];
    private Game game;
    private Player player;
    private static final String [] colorNames = 
        {"Black", "Blue", "Brown", "Gold", "Green", "Red"};
    private static final int [] colorMnemonics = 
        {KeyEvent.VK_B, KeyEvent.VK_L, KeyEvent.VK_O, KeyEvent.VK_G, 
            KeyEvent.VK_E, KeyEvent.VK_R};
        

    public PickColor(JFrame parentFrame, Game game, Player player)
    {
        super(parentFrame, player.getName() + ", Pick a Color", true);
        this.game = game;
        this.player = player;
    
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
        
        Color [] background = { Color.black, Color.blue, new Color(180, 90, 0),
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
        String color = e.getActionCommand();
        // Send data back to game, and exit.
        Game.logEvent(player.getName() + " chooses color " + color);
        player.setColor(color);
        setVisible(false);
        dispose();
    }


    public Dimension getMinimumSize()
    {
        return new Dimension(250, 250);
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

        new PickColor(frame, game, p0);
        new PickColor(frame, game, p1);

        System.exit(0);
    }
}
