import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Class Game gets and holds high-level data about a Titan game.
 * @version $Id$
 * author David Ripton
 */


public class Game extends JFrame implements WindowListener, ActionListener
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

    private JTextField [] tf = new JTextField[6];
    private int currentColor;  // state holder during color choice
    private JLabel [] colorLabel = new JLabel[6];
    private JButton [] colorButton = new JButton[6];
    private JLabel [] activeLabel;
    private JLabel [] elimLabel;
    private JLabel [] legionsLabel;
    private JLabel [] markersLabel;
    private JLabel [] creaturesLabel;
    private JLabel [] titanLabel;
    private JLabel [] scoreLabel;
    private Container contentPane;


    Game()
    {
        super("Player Setup");
        setBackground(java.awt.Color.white);
        pack();
        setSize(300, 250);
        setIconImage(Toolkit.getDefaultToolkit().getImage(
            Creature.colossus.getImageName()));

        // Center dialog on screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));

        contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(0, 2));

        for (int i = 0; i < 6; i++)
        {
            String s = "Player " + (i + 1) + " Name";
            contentPane.add(new JLabel(s));
            tf[i] = new JTextField(20);
            contentPane.add(tf[i]);
        }

        JButton button1 = new JButton("OK");
        contentPane.add(button1);
        button1.addActionListener(this);
        JButton button2 = new JButton("Quit");
        contentPane.add(button2);
        button2.addActionListener(this);

        addWindowListener(this);
        setVisible(true);
    }
    
    
    // Load a saved game.
    Game(String filename)
    {
        super("Player Setup");
        setBackground(java.awt.Color.white);
        pack();
        setIconImage(Toolkit.getDefaultToolkit().getImage(
            Creature.colossus.getImageName()));

        addWindowListener(this);

        board = new MasterBoard(this, false);

        loadGame(filename);
        
        initStatusScreen(false);
    }


    private void validateInputs()
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


    private void assignTowers()
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


    private void chooseColors()
    {
        contentPane = getContentPane();
        contentPane.removeAll();
        setTitle("Choose Colors");
        contentPane.setLayout(new GridLayout(0, 3));

        contentPane.add(new JLabel("Tower"));
        contentPane.add(new JLabel("Name"));
        contentPane.add(new JLabel("Color"));

        // Sort in increasing tower order
        for (int i = 1; i <= 6; i++)
        {
            for (int j = 0; j < numPlayers; j++)
            {
                if (players[j].getTower() == i)
                {
                    contentPane.add(new JLabel(String.valueOf(100 * i)));
                    contentPane.add(new JLabel(players[j].getName()));
                    colorLabel[j] = new JLabel("");
                    contentPane.add(colorLabel[j]);
                }
            }
        }

        colorButton[0] = new JButton("Black");
        colorButton[0].setBackground(Color.black);
        colorButton[0].setForeground(Color.white);
        colorButton[1] = new JButton("Blue");
        colorButton[1].setBackground(Color.blue);
        colorButton[1].setForeground(Color.white);
        colorButton[2] = new JButton("Brown");
        colorButton[2].setBackground(new Color(180, 90, 0));
        colorButton[2].setForeground(Color.white);
        colorButton[3] = new JButton("Gold");
        colorButton[3].setBackground(Color.yellow);
        colorButton[4] = new JButton("Green");
        colorButton[4].setBackground(Color.green);
        colorButton[5] = new JButton("Red");
        colorButton[5].setBackground(Color.red);
        for (int i = 0; i < 6; i++)
        {
            contentPane.add(colorButton[i]);
            colorButton[i].addActionListener(this);
        }

        JButton button1 = new JButton("Done");
        contentPane.add(button1);
        button1.addActionListener(this);
        JButton button2 = new JButton("Restart");
        contentPane.add(button2);
        button2.addActionListener(this);
        JButton button3 = new JButton("Quit");
        contentPane.add(button3);
        button3.addActionListener(this);

        // Center dialog on screen.
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


    private void processColorChoice(String color)
    {
        // Turn off the button that was just used.
        int i = 0;
        while (colorButton[i].getText() != color)
        {
            i++;
        }
        colorButton[i].setText("");
        colorButton[i].setBackground(Color.lightGray);
        colorButton[i].setForeground(Color.black);
        colorButton[i].removeActionListener(this);


        for (int j = 0; j < numPlayers; j++)
        {
            if (players[j].getTower() == currentColor)
            {
                players[j].setColor(color);
                colorLabel[j].setText(color);
                players[j].initMarkersAvailable();
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


    private void initStatusScreen(boolean newgame)
    {
        contentPane = getContentPane();
        setVisible(false);
        contentPane.removeAll();
        setTitle("Game Status");
        contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(0, 10));

        // Need to sort players in descending tower order
        sortPlayers();

        // active, player name, tower, color, colors eliminated, legions,
        //     markers, creatures, titan power, score

        contentPane.add(new JLabel(""));
        contentPane.add(new JLabel("Player "));
        contentPane.add(new JLabel("Tower "));
        contentPane.add(new JLabel("Color "));
        contentPane.add(new JLabel("Elim "));
        contentPane.add(new JLabel("Legions "));
        contentPane.add(new JLabel("Markers "));
        contentPane.add(new JLabel("Creatures "));
        contentPane.add(new JLabel("Titan Power "));
        contentPane.add(new JLabel("Score"));

        activeLabel = new JLabel[numPlayers];
        elimLabel = new JLabel[numPlayers];
        legionsLabel = new JLabel[numPlayers];
        markersLabel = new JLabel[numPlayers];
        creaturesLabel = new JLabel[numPlayers];
        titanLabel = new JLabel[numPlayers];
        scoreLabel = new JLabel[numPlayers];

        for (int i = 0; i < numPlayers; i++)
        {
            activeLabel[i] = new JLabel(" ");
            contentPane.add(activeLabel[i]);
            contentPane.add(new JLabel(players[i].getName()));
            contentPane.add(new JLabel(
                String.valueOf(100 * players[i].getTower())));
            contentPane.add(new JLabel(players[i].getColor()));
            elimLabel[i] = new JLabel(players[i].getPlayersElim());
            contentPane.add(elimLabel[i]);
            legionsLabel[i] = new JLabel(String.valueOf(
                players[i].getNumLegions()));
            contentPane.add(legionsLabel[i]);
            markersLabel[i] = new JLabel(String.valueOf(
                players[i].getNumMarkersAvailable()));
            contentPane.add(markersLabel[i]);
            creaturesLabel[i] = new JLabel(String.valueOf(
                players[i].getNumCreatures()));
            contentPane.add(creaturesLabel[i]);
            titanLabel[i] = new JLabel(String.valueOf(
                players[i].getTitanPower()));
            contentPane.add(titanLabel[i]);
            scoreLabel[i] = new JLabel(String.valueOf(
                players[i].getScore()));
            contentPane.add(scoreLabel[i]);
        }

        pack();

        // Move dialog to bottom right of screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width,
            d.height - getSize().height));

        setVisible(true);

        if (newgame)
        {
            board = new MasterBoard(this, newgame);
        }
    }


    public void updateStatusScreen()
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
            creaturesLabel[i].setText(String.valueOf(
                players[i].getNumCreatures()));
            titanLabel[i].setText(String.valueOf(players[i].getTitanPower()));
            scoreLabel[i].setText(String.valueOf(players[i].getScore()));
        }

        repaint();
    }


    // Sort player array into turn order, by descending tower number.
    private void sortPlayers()
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
    private void sortStrings(String [] s)
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


    public int getNumPlayers()
    {
        return numPlayers;
    }


    public int getNumLivingPlayers()
    {
        int count = 0;
        for (int i = 0; i < numPlayers; i++)
        {
            if (players[i].isAlive())
            {
                count++;
            }
        }
        return count;
    }


    public Player getActivePlayer()
    {
        return players[activePlayerNum];
    }


    public int getActivePlayerNum()
    {
        return activePlayerNum;
    }


    public Player getPlayer(int i)
    {
        return players[i];
    }


    public void checkForVictory()
    {
        int remaining = 0;
        int winner = -1;

        for (int i = 0; i < numPlayers; i++)
        {
            if (players[i].isAlive())
            {
                remaining++;
                winner = i;
            }
        }

        switch (remaining)
        {
            case 0:
                JOptionPane.showMessageDialog(board, "draw");
                System.exit(0);
                break;

            case 1:
                JOptionPane.showMessageDialog(board,
                    players[winner].getName() + " wins");
                System.exit(0);
                break;

            default:
                break;
        }
    }


    public int getPhase()
    {
        return phase;
    }


    public MasterBoard getBoard()
    {
        return board;
    }


    public void advancePhase()
    {
        board.unselectAllHexes();
        phase++;
    }


    public void advanceTurn()
    {
        board.unselectAllHexes();
        activePlayerNum++;
        if (activePlayerNum == numPlayers)
        {
            activePlayerNum = 0;
            turnNumber++;
        }
        phase = SPLIT;
        if (!getActivePlayer().isAlive() && getNumLivingPlayers() > 0)
        {
            advanceTurn();
        }

        updateStatusScreen();

        // XXX temporary test
        saveGame();
    }


    public int getTurnNumber()
    {
        return turnNumber;
    }


    // Create a text file describing this game's state.
    // Format:
    //     Number of players
    //     Turn number
    //     Whose turn
    //     Current phase
    //     Creature counts
    //     Player 1:
    //         Name
    //         Color
    //         Starting tower
    //         Score
    //         Alive?
    //         Mulligans left
    //         Players eliminated
    //         Number of markers left
    //         Remaining marker ids
    //         Number of Legions 
    //         Legion 1: 
    //             Marker id
    //             Height
    //             Creature 1
    //             Creature 1 visible?
    //             ...
    //     ...
    private void saveGame()
    {
        // XXX Need dialog to pick filename.
        Date date = new Date();
        String filename = date.getTime() + ".sav";
        FileWriter fileWriter;
        try
        {
            fileWriter = new FileWriter(filename);
        }
        catch (IOException e)
        {
            System.out.println(e.toString());
            System.out.println("Couldn't open " + filename);
            return;
        }
        PrintWriter out = new PrintWriter(fileWriter);

        out.println(getNumPlayers());
        out.println(getTurnNumber());
        out.println(getActivePlayerNum());
        out.println(getPhase());

        for (int i = 0; i < Creature.creatures.length; i++)
        {
            out.println(Creature.creatures[i].getCount());
        }

        for (int i = 0; i < getNumPlayers(); i++)
        {
            Player player = getPlayer(i);
            out.println(player.getName());
            out.println(player.getColor());
            out.println(player.getTower());
            out.println(player.getScore());
            out.println(player.isAlive());
            out.println(player.getMulligansLeft());
            out.println(player.getPlayersElim());
            out.println(player.getNumMarkersAvailable());
            for (int j = 0; j < player.getNumMarkersAvailable(); j++)
            {
                out.println(player.getMarker(j));
            }
            out.println(player.getNumLegions());

            for (int j = 0; j < player.getNumLegions(); j++)
            {
                Legion legion = player.getLegion(j);
                out.println(legion.getMarkerId());
                out.println(legion.getCurrentHex().getLabel());

                out.println(legion.getHeight());
                for (int k = 0; k < legion.getHeight(); k++)
                {
                    out.println(legion.getCritter(k).getName());
                    out.println(legion.getCritter(k).isVisible());
                }
            }
        }
        
        if (out.checkError()) 
        {
            System.out.println("Write error " + filename);
            // XXX Delete the partial file?
            return;
        }
    }


    private void loadGame(String filename)
    {
        // XXX Need a dialog to pick the savegame's filename, and 
        //     confirmation if there's already a game in progress.
         
        try
        {
            FileReader fileReader = new FileReader(filename);
            BufferedReader in = new BufferedReader(fileReader);
            String buf = new String();

            buf = in.readLine();
            numPlayers = Integer.parseInt(buf);

            buf = in.readLine();
            turnNumber = Integer.parseInt(buf);

            buf = in.readLine();
            activePlayerNum = Integer.parseInt(buf);

            buf = in.readLine();
            phase = Integer.parseInt(buf);

            for (int i = 0; i < Creature.creatures.length; i++)
            {
                buf = in.readLine();
                int count = Integer.parseInt(buf);
                Creature.creatures[i].setCount(count);
            }

            players = new Player[numPlayers];
            for (int i = 0; i < numPlayers; i++)
            {
                String name = in.readLine();
                players[i] = new Player(name, this);
                
                String color = in.readLine();
                players[i].setColor(color);
                
                buf = in.readLine();
                int tower = Integer.parseInt(buf);
                players[i].setTower(tower);
                
                buf = in.readLine();
                int score = Integer.parseInt(buf);
                players[i].setScore(score);
                
                buf = in.readLine();
                players[i].setAlive(Boolean.valueOf(buf).booleanValue());

                buf = in.readLine();
                int mulligansLeft = Integer.parseInt(buf);
                players[i].setMulligansLeft(mulligansLeft);

                String playersElim = in.readLine();
                if (playersElim.equals("null"))
                {
                    playersElim = new String("");
                }
                players[i].setPlayersElim(playersElim);
                
                buf = in.readLine();
                int numMarkersAvailable = Integer.parseInt(buf);

                for (int j = 0; j < numMarkersAvailable; j++)
                {
                    String markerId = in.readLine();
                    players[i].addLegionMarker(markerId);
                }

                buf = in.readLine();
                int numLegions = Integer.parseInt(buf);
                // Do not set numLegions in Player yet; let
                // addLegion() do it.

                for (int j = 0; j < numLegions; j++)
                {
                    String markerId = in.readLine();

                    buf = in.readLine();
                    int hexLabel = Integer.parseInt(buf);
    
                    buf = in.readLine();
                    int height = Integer.parseInt(buf);

                    Creature [] creatures = new Creature[8];
                    boolean [] visibles = new boolean[height];

                    for (int k = 0; k < height; k++)
                    {
                        buf = in.readLine();
                        creatures[k] = Creature.getCreatureFromName(buf);
                        buf = in.readLine();
                        visibles[k] = Boolean.valueOf(buf).booleanValue();
                    }

                    Legion legion = new Legion(0, 0, 3 * board.getScale(), 
                        markerId, null, board, height, 
                        board.getHexFromLabel(hexLabel), creatures[0], 
                        creatures[1], creatures[2], creatures[3], creatures[4],
                        creatures[5], creatures[6], creatures[7], players[i]);
                    players[i].addLegion(legion);
                }
            }

            board.finishInit(false);
        }
        // FileNotFoundException, IOException, NumberFormatException
        catch (Exception e)
        {
            System.out.println(e.toString());
            // XXX Ask for another file?  Start new game?
            System.exit(1);
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
                if (players[i].getColor() == null)
                {
                    return;
                }
            }

            // Change this window into a status screen, and then
            //     move on to the first player's first turn.
            initStatusScreen(true);
        }
        else
        {
            // Color button
            processColorChoice(e.getActionCommand());
        }
    }


    public static void main(String args[])
    {
        // Use the system look and feel rather than the cross-platform one.
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ex_ignored)
        {
            System.out.println("Default l&f left in place");
        }

        if (args.length == 0)
        {
            Game game = new Game();
        }
        else
        {
            Game game = new Game(args[0]);
        }
    }
}
