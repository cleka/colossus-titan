import java.awt.*;
import java.awt.event.*;


/**
 * Class PickColor lets a player choose a color of legion markers.
 * @version $Id$ 
 * @author David Ripton
 */


public class PickColor extends Dialog implements WindowListener, ActionListener
{
    private Label [] colorLabel = new Label[6];
    private Game game;
    private int playerNum;
    private String [] colorNames = new String[6];


    public PickColor(Frame parentFrame, Game game, int playerNum)
    {
        super(parentFrame, game.getPlayer(playerNum).getName() + 
            ", Pick a Color", true);
        this.game = game;
        this.playerNum = playerNum;
    
        colorNames[0] = "Black";
        colorNames[1] = "Blue";
        colorNames[2] = "Brown";
        colorNames[3] = "Gold";
        colorNames[4] = "Green";
        colorNames[5] = "Red";

        setBackground(Color.lightGray);
        pack();

        setLayout(new GridLayout(0, 3));

        add(new Label("Tower"));
        add(new Label("Name"));
        add(new Label("Color"));
        
        boolean [] colorTaken = new boolean[6];
        for (int i = 0; i < 6; i++)
        {
            colorTaken[i] = false;
        }

        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            int tower = game.getPlayer(i).getTower();
            add(new Label(String.valueOf(100 * tower)));
            add(new Label(game.getPlayer(i).getName()));
            String color = game.getPlayer(i).getColor(); 

            if (color == null)
            {
                if (i == playerNum)
                {
                    colorLabel[i] = new Label("?");
                }
                else
                {
                    colorLabel[i] = new Label("");
                }
            }
            else
            {
                colorLabel[i] = new Label(color);
                if (colorNumber(color) != -1)
                {
                    colorTaken[colorNumber(color)] = true;
                }
            }

            add(colorLabel[i]);
        }

        Color [] background = new Color[6];
        background[0] = Color.black;
        background[1] = Color.blue;
        background[2] = new Color(180, 90, 0);
        background[3] = Color.yellow;
        background[4] = Color.green;
        background[5] = Color.red;
        
        Color [] foreground = new Color[6];
        foreground[0] = Color.white;
        foreground[1] = Color.white;
        foreground[2] = Color.white;
        foreground[3] = Color.black;
        foreground[4] = Color.black;
        foreground[5] = Color.black;

        for (int i = 0; i < 6; i++)
        {
            Button button = new Button();
            if (colorTaken[i])
            {
                button.setBackground(Color.lightGray);
                button.setForeground(Color.black);
            }
            else
            {
                button.setLabel(colorNames[i]);
                button.setBackground(background[i]);
                button.setForeground(foreground[i]);
                button.addActionListener(this);
            }
            add(button);
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
        // Send data back to game, and exit.
        game.getPlayer(playerNum).setColor(e.getActionCommand());
        dispose();
    }
}
