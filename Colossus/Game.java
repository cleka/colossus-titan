import java.awt.*;
import java.awt.event.*;

/**
 * Class Game gets and holds high-level data about a Titan game.
 * @version $Id$
 * author David Ripton
 */

class Game extends Frame implements WindowListener, ActionListener
{
    int numPlayers;
    String [] playerName; 
    int [] playerTower;
    String [] playerColor;
    Player [] player;
    TextField [] tf = new TextField[7];


    Game()
    {
        super("Player Setup");
        pack();
        setSize(300, 250);
        
        setLayout(new GridLayout(0, 2));

        add(new Label("Number of Players (2-6)"));
        tf[0] = new TextField(1);
        add(tf[0]);
        add(new Label("Player 1 Name"));
        tf[1] = new TextField(20);
        add(tf[1]);
        add(new Label("Player 2 Name"));
        tf[2] = new TextField(20);
        add(tf[2]);
        add(new Label("Player 3 Name"));
        tf[3] = new TextField(20);
        add(tf[3]);
        add(new Label("Player 4 Name"));
        tf[4] = new TextField(20);
        add(tf[4]);
        add(new Label("Player 5 Name"));
        tf[5] = new TextField(20);
        add(tf[5]);
        add(new Label("Player 6 Name"));
        tf[6] = new TextField(20);
        add(tf[6]);
        Button b1 = new Button("Done");
        add(b1);
        b1.addActionListener(this);
        Button b2 = new Button("Quit");
        add(b2);
        b2.addActionListener(this);

        setVisible(true);
        addWindowListener(this);
    }


    void validateInputs()
    {
        boolean error = false;
        String [] s = new String[7];
        for (int i = 0; i <= 6; i++)
        {
           s[i] = tf[i].getText(); 
           System.out.println(i + ": " + s[i]);
        }

        // s[0] needs to be a number in the range 1-6
        try
        {
            numPlayers = Integer.valueOf(s[0]).intValue();
            if (numPlayers < 1 || numPlayers > 6)
            {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException e)
        {
            System.out.println("Bogus number of players: " + s[0]);
            tf[0].setText(""); 
            return;
        }

        // Make sure each player has a unique, non-empty name
        playerName = new String[numPlayers];
        for (int i = 0; i < numPlayers; i++)
        {
            playerName[i] = tf[i + 1].getText();
            if (playerName[i].length() == 0)
            {
                error = true;
                System.out.println("Player " + (i + 1) + " has an empty name");
            }
            for (int j = 0; j < i; j++)
            {
                if (playerName[i].compareTo(playerName[j]) == 0)
                {
                    error = true;
                    System.out.println("Players " + (j + 1) + " and " + (i + 1) 
                        + " have the same name.");
                    tf[i + 1].setText("");
                }
            }
        }
        
        if (error)
        {
            return;
        }

        // Since the inputs are validated, it's time to roll for towers.
        assignTowers();
    }


    void assignTowers()
    {
        playerTower = new int[6];

        // A random card-shuffling algorithm is cleaner than repeated 
        //    die-rolling and checking for duplicates

        for (int i = 0; i < 6 ; i++) 
        {
            playerTower[i] = i + 1;
        }

        // 1000 shuffles should be more than enough.
        for (int i = 0; i < 1000; i++)
        {
            int m = (int) Math.floor(6 * Math.random());
            int n = (int) Math.floor(6 * Math.random());
            int t = playerTower[m];
            playerTower[m] = playerTower[n];
            playerTower[n] = t;
        }

        for (int i = 0; i < numPlayers; i++)
        {
            System.out.println(i + ": tower " + playerTower[i] + "00  name: " 
                + playerName[i]);
        }

        chooseColors();
    }


    void chooseColors()
    {
        removeAll();
        setTitle("Choose Colors");
        setLayout(new GridLayout(0, 3));
        // XXX Why doesn't this work?
        setSize(300, 250);

        add(new Label("Tower"));
        add(new Label("Name"));
        add(new Label("Color"));

        Label [] colorLabel = new Label[numPlayers];

        // Sort in increasing tower order
        for (int i = 1; i <= 6; i++)
        {
            for (int j = 0; j < numPlayers; j++)
            {
                if (playerTower[j] == i)
                {
                    add(new Label(String.valueOf(100 * i)));
                    add(new Label(playerName[j]));
                    colorLabel[j] = new Label("");
                    add(colorLabel[j]);
                }
            }
        }

        Button [] colorButton = new Button[6];
        colorButton[0] = new Button("Black");
        colorButton[1] = new Button("Blue");
        colorButton[2] = new Button("Brown");
        colorButton[3] = new Button("Gold");
        colorButton[4] = new Button("Green");
        colorButton[5] = new Button("Red");
        for (int i = 0; i < 6; i++)
        {
            add(colorButton[i]);
        }

        pack();

        for (int i = 1; i <= 6; i++)
        {
            for (int j = 0; j < numPlayers; j++)
            {
                if (playerTower[j] == i)
                {
                    colorLabel[j].setText("?");
                }
            }
        }

    }


    public void windowActivated(WindowEvent event)
    {
    }

    public void windowClosed(WindowEvent event)
    {
    }

    public void windowClosing(WindowEvent event)
    {
        System.exit(0);
    }

    public void windowDeactivated(WindowEvent event)
    {
    }
                                                            
    public void windowDeiconified(WindowEvent event)
    {
    }

    public void windowIconified(WindowEvent event)
    {
    }

    public void windowOpened(WindowEvent event)
    {
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "Quit")
        {
            System.exit(0);
        }
        else if (e.getActionCommand() == "Done")
        {
            validateInputs();
        }
    }


    public static void main(String args[])
    {
        Game game = new Game();
    }
}
