import java.awt.*;
import java.awt.event.*;

/**
 * Class Game gets and holds high-level data about a Titan game.
 * @version $Id$
 * author David Ripton
 */


public class Game extends Frame implements WindowListener, ActionListener
{
    private int numPlayers;
    private Player [] players;
    private MasterBoard board;
    private int activePlayerNum = 0;
    private int turnNumber = 1;  // Advance when every player has a turn

    public static final int SPLIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int MUSTER = 4;
    private int phase = SPLIT;

    private TextField [] tf = new TextField[6];
    private int currentColor;  // state holder during color choice
    private Label [] colorLabel = new Label[6];
    private Button [] colorButton = new Button[6];
    private Label [] activeLabel;
    private Label [] elimLabel;
    private Label [] legionsLabel;
    private Label [] markersLabel;
    private Label [] titanLabel;
    private Label [] scoreLabel;


    Game()
    {
        super("Player Setup");
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
        
        Button button1 = new Button("OK");
        add(button1);
        button1.addActionListener(this);
        Button button2 = new Button("Quit");
        add(button2);
        button2.addActionListener(this);

        pack();
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
        players = new Player[numPlayers];
        for (int i = 0; i < numPlayers; i++)
        {
            players[i] = new Player(playerName[i], this);
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
            players[i].setTower(playerTower[i]);
        }

        chooseColors();
    }


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
                if (players[j].getTower() == i)
                {
                    add(new Label(String.valueOf(100 * i)));
                    add(new Label(players[j].getName()));
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

        Button button1 = new Button("Done");
        add(button1);
        button1.addActionListener(this);
        Button button2 = new Button("Restart");
        add(button2);
        button2.addActionListener(this);
        Button button3 = new Button("Quit");
        add(button3);
        button3.addActionListener(this);

        setSize(230, 50 * numPlayers + 100);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));

        pack();

        for (int i = 1; i <= 6; i++)
        {
            for (int j = 0; j < numPlayers; j++)
            {
                if (players[j].getTower() == i)
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
            if (players[j].getTower() == currentColor)
            {
                players[j].setColor(color);
                colorLabel[j].setText(color);
            }
        }
    
        for (i = currentColor + 1; i <= 6; i++)
        {
            for (int j = 0; j < numPlayers; j++)
            {
                if (players[j].getTower() == i)
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
            add(new Label(players[i].getName()));
            add(new Label(String.valueOf(100 * players[i].getTower())));
            add(new Label(players[i].getColor()));
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

        board = new MasterBoard(this);
    }


    void updateStatusScreen()
    {
        for (int i = 0; i < numPlayers; i++)
        {
            if (activePlayerNum == i)
            {
                activeLabel[i].setText("*");
            }
            else
            {
                activeLabel[i].setText(" ");
            }
            elimLabel[i].setText(players[i].getPlayersElim());
            legionsLabel[i].setText(String.valueOf(
                players[i].getNumLegions()));
            markersLabel[i].setText(String.valueOf(
                players[i].getNumMarkersAvailable()));
            titanLabel[i].setText(String.valueOf(players[i].getTitanPower()));
            scoreLabel[i].setText(String.valueOf(players[i].getScore()));
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
                if (players[i].getTower() < players[j].getTower())
                {
                    Player tempPlayer = players[i];
                    players[i] = players[j];
                    players[j] = tempPlayer;
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


    int getNumPlayers()
    {
        return numPlayers;
    }


    Player getActivePlayer()
    {
        return players[activePlayerNum];
    }


    int getActivePlayerNum()
    {
        return activePlayerNum;
    }


    Player getPlayer(int i)
    {
        return players[i];
    }


    void checkForVictory()
    {
        int remaining = 0;
        int winner = -1;

        for (int i = 0; i < numPlayers; i++)
        {
            if (players[i].isAlive())
            {
                remaining++;
                if (remaining >= 2)
                {
                    return;
                }
                winner = i;
            }
        }

        if (remaining == 1)
        {
            new MessageBox(board, players[winner].getName() + " wins");
        }
        else
        {
            new MessageBox(board, "draw");
        }
    }


    int getPhase()
    {
        return phase;
    }


    MasterBoard getBoard()
    {
        return board;
    }


    void advancePhase()
    {
        board.unselectAllHexes();
        phase++;
System.out.println("advancePhase to phase " + phase);
    }


    void advanceTurn()
    {
System.out.println("advanceTurn");
        board.unselectAllHexes();
        activePlayerNum++;
        if (activePlayerNum == numPlayers)
        {
            activePlayerNum = 0;
            turnNumber++;
        }
        phase = SPLIT;
System.out.println("phase = SPLIT  activePlayerNum = " + activePlayerNum);
        if (!getActivePlayer().isAlive())
        {
            advanceTurn();
        }
        else
        {
            // Update the status screen to show whose turn it is.
            updateStatusScreen();
        }
    }


    int getTurnNumber()
    {
        return turnNumber;
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
                if (players[i].getColor() == null)
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
