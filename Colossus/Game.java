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
    Player [] player;
    MasterBoard masterboard;
    int turn = 0;

    TextField [] tf = new TextField[6];
    int currentColor;  // state holder during color choice
    Label [] colorLabel = new Label[6];
    Button [] colorButton = new Button[6];
    Label [] activeLabel;
    Label [] elimLabel;
    Label [] legionsLabel;
    Label [] markersLabel;
    Label [] titanLabel;
    Label [] scoreLabel;

    Game()
    {
        super("Player Setup");
        pack();
        setSize(300, 250);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));
        
        setLayout(new GridLayout(0, 2));

        for (int i = 0; i < 6; i++)
        {
            String s = "Player " + (i + 1) + " Name";
            add(new Label(s));
            tf[i] = new TextField(20);
            add(tf[i]);
        }
        
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
        String [] s = new String[6];
        String [] playerName = new String[6];

        for (int i = 0; i < 6; i++)
        {
            s[i] = tf[i].getText();
        }

        // Sort in reverse order so that empties go last.
        sortStrings(s);

        // Make sure each player has a unique name.
        numPlayers = 0;
        for (int i = 0; i < 6; i++)
        {
            if (s[i].length() > 0)
            {
                if (i > 0 && s[i].compareTo(s[i - 1]) == 0)
                {
                    error = true;
                }
                else
                {
                    playerName[numPlayers] = s[i];
                    numPlayers++;
                }
            }
        }
        
        if (error || numPlayers == 0)
        {
            for (int i = 0; i < 6; i++)
            {
                tf[i].setText("");
            }
            return;
        }

        // Fill the player objects
        player = new Player[numPlayers];
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


    // XXX This should eventually be one dialog per player.
    void chooseColors()
    {
        removeAll();
        setTitle("Choose Colors");
        setLayout(new GridLayout(0, 3));

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
        colorButton[0].setBackground(Color.black);
        colorButton[0].setForeground(Color.white);
        colorButton[1] = new Button("Blue");
        colorButton[1].setBackground(Color.blue);
        colorButton[1].setForeground(Color.white);
        colorButton[2] = new Button("Brown");
        colorButton[2].setBackground(new Color(180, 90, 0));
        colorButton[2].setForeground(Color.white);
        colorButton[3] = new Button("Gold");
        colorButton[3].setBackground(Color.yellow);
        colorButton[4] = new Button("Green");
        colorButton[4].setBackground(Color.green);
        colorButton[5] = new Button("Red");
        colorButton[5].setBackground(Color.red);
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
        setSize(230, 50 * numPlayers + 100);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));

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
        colorButton[i].setBackground(Color.lightGray);
        colorButton[i].setForeground(Color.black);
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

    
    void initStatusScreen()
    {
        setVisible(false);
        removeAll();
        setTitle("Game Status");
        setLayout(new GridLayout(0, 9));


        // Need to sort player in descending tower order
        sortPlayers();

        // active, player name, tower, color, colors eliminated, legions,
        //     markers, titan power, score

        add(new Label(""));
        add(new Label("Player"));
        add(new Label("Tower"));
        add(new Label("Color"));
        add(new Label("Colors Elim"));
        add(new Label("Legions"));
        add(new Label("Markers"));
        add(new Label("Titan Power"));
        add(new Label("Score"));

        activeLabel = new Label[numPlayers];
        elimLabel = new Label[numPlayers];
        legionsLabel = new Label[numPlayers];
        markersLabel = new Label[numPlayers];
        titanLabel = new Label[numPlayers];
        scoreLabel = new Label[numPlayers];

        for (int i = 0; i < numPlayers; i++)
        {
            activeLabel[i] = new Label(" ");
            add(activeLabel[i]);
            add(new Label(player[i].name));
            add(new Label(String.valueOf(100 * player[i].startingTower)));
            add(new Label(player[i].color));
            elimLabel[i] = new Label("");
            add(elimLabel[i]); 
            legionsLabel[i] = new Label("");
            add(legionsLabel[i]);
            markersLabel[i] = new Label("12");
            add(markersLabel[i]);
            titanLabel[i] = new Label("6");
            add(titanLabel[i]);
            scoreLabel[i] = new Label("0");
            add(scoreLabel[i]);
        }

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, 0));

        setVisible(true);

        masterboard = new MasterBoard(this);
    }


    void updateStatusScreen()
    {
        for (int i = 0; i < numPlayers; i++)
        {
            if (player[i].myTurn)
            {
                activeLabel[i].setText("*");
            }
            else
            {
                activeLabel[i].setText(" ");
            }
            elimLabel[i].setText(player[i].playersEliminated);
            legionsLabel[i].setText(String.valueOf(player[i].numLegions));
            markersLabel[i].setText(String.valueOf(
                player[i].numMarkersAvailable));
            titanLabel[i].setText(String.valueOf(player[i].titanPower()));
            scoreLabel[i].setText(String.valueOf(player[i].score));
        }

        repaint();
    }

    
    // Sort player array into turn order, by descending tower number.
    void sortPlayers()
    {
        for (int i = 0; i < numPlayers - 1; i++)
        {
            for (int j = i + 1; j < numPlayers; j++)
            {
                if (player[i].startingTower < player[j].startingTower)
                {
                    Player tempPlayer = player[i];
                    player[i] = player[j];
                    player[j] = tempPlayer;
                }
            }
        }
    }
    
    
    // Sort string array in reverse order
    void sortStrings(String [] s)
    {
        for (int i = 0; i < s.length - 1; i++)
        {
            for (int j = i + 1; j < s.length; j++)
            {
                if (s[i].compareTo(s[j]) > 0)
                {
                    String temp = s[i];
                    s[i] = s[j];
                    s[j] = temp;
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
            // Make sure all colors are assigned before continuing.
            for (int i = 0; i < numPlayers; i++)
            {
                if (player[i].color == null) 
                {
                    return;
                }
            }

            // Change this window into a status screen, and then 
            //     move on to the first player's first turn.
            initStatusScreen();
        }
        else
        {
            // Color button
            processColorChoice(e.getActionCommand());
        }
    }


    public static void main(String args[])
    {
        Game game = new Game();
    }
}
