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
    Player [] player = new Player[6];
    TextField [] tf = new TextField[7];
    int currentColor;
    Label [] colorLabel = new Label[6];
    Button [] colorButton = new Button[6];


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
        Button b1 = new Button("OK");
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
            tf[0].setText(""); 
            return;
        }

        // Make sure each player has a unique, non-empty name
        String [] playerName = new String[numPlayers];
        for (int i = 0; i < numPlayers; i++)
        {
            playerName[i] = tf[i + 1].getText();
            if (playerName[i].length() == 0)
            {
                error = true;
            }
            for (int j = 0; j < i; j++)
            {
                if (playerName[i].compareTo(playerName[j]) == 0)
                {
                    error = true;
                    tf[i + 1].setText("");
                }
            }
        }
        
        if (error)
        {
            return;
        }

        // Fill the player objects
        for (int i = 0; i < numPlayers; i++)
        {
            player[i] = new Player(playerName[i]);
        }

        // Since the inputs are validated, it's time to roll for towers.
        assignTowers();
    }


    void assignTowers()
    {
        int [] playerTower = new int[6];

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
            player[i].setTower(playerTower[i]);
        }

        chooseColors();
    }


    void chooseColors()
    {
        removeAll();
        setTitle("Choose Colors");
        setLayout(new GridLayout(0, 3));
        // XXX Why doesn't this stick?
        setSize(300, 250);

        add(new Label("Tower"));
        add(new Label("Name"));
        add(new Label("Color"));


        // Sort in increasing tower order
        for (int i = 1; i <= 6; i++)
        {
            for (int j = 0; j < numPlayers; j++)
            {
                if (player[j].startingTower == i)
                {
                    add(new Label(String.valueOf(100 * i)));
                    add(new Label(player[j].name));
                    colorLabel[j] = new Label("");
                    add(colorLabel[j]);
                }
            }
        }

        colorButton[0] = new Button("Black");
        colorButton[1] = new Button("Blue");
        colorButton[2] = new Button("Brown");
        colorButton[3] = new Button("Gold");
        colorButton[4] = new Button("Green");
        colorButton[5] = new Button("Red");
        for (int i = 0; i < 6; i++)
        {
            add(colorButton[i]);
            colorButton[i].addActionListener(this);
        }

        Button b1 = new Button("Done");
        add(b1);
        b1.addActionListener(this);
        Button b2 = new Button("Restart");
        add(b2);
        b2.addActionListener(this);
        Button b3 = new Button("Quit");
        add(b3);
        b3.addActionListener(this);

        pack();

        for (int i = 1; i <= 6; i++)
        {
            for (int j = 0; j < numPlayers; j++)
            {
                if (player[j].startingTower == i)
                {
                    currentColor = i;
                    colorLabel[j].setText("?");
                    return;
                }
            }
        }
    }


    void processColorChoice(String color)
    {
        // Turn off the button that was just used.
        int i = 0;
        while (colorButton[i].getLabel() != color)
        {
            i++;
        }
        colorButton[i].setLabel("");
        colorButton[i].removeActionListener(this);


        for (int j = 0; j < numPlayers; j++)
        {
            if (player[j].startingTower == currentColor)
            {
                player[j].setColor(color);
                colorLabel[j].setText(color);
            }
        }
    
        for (i = currentColor + 1; i <= 6; i++)
        {
            for (int j = 0; j < numPlayers; j++)
            {
                if (player[j].startingTower == i)
                {
                    currentColor = i;
                    colorLabel[j].setText("?");
                    return;
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
        else if (e.getActionCommand() == "OK")
        {
            validateInputs();
        }
        else if (e.getActionCommand() == "Restart")
        {
            chooseColors();
        }
        else if (e.getActionCommand() == "Done")
        {
            // Go on to do initial splits
            System.out.println("To Do: initial splits");
            System.exit(0);
        }
        else  // Color button
        {
            processColorChoice(e.getActionCommand());
        }
    }


    public static void main(String args[])
    {
        Game game = new Game();
    }
}
