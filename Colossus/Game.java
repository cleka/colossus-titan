import java.awt.*;
import java.io.*;
import java.util.*;


/**
 * Class Game gets and holds high-level data about a Titan game.
 * @version $Id$
 * @author David Ripton
 */


public class Game
{
    private int numPlayers;
    private String [] playerNames = new String[6];
    private Player [] players;
    private MasterBoard board;
    private int activePlayerNum = 0;
    private int turnNumber = 1;  // Advance when every player has a turn
    private StatusScreen statusScreen;

    public static final int SPLIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int MUSTER = 4;
    private int phase = SPLIT;

    private boolean isApplet; 


    public Game(boolean isApplet)
    {
        this.isApplet = isApplet;
        Chit.setApplet(isApplet);

        Frame frame = new Frame();

        new GetPlayers(frame, this);

        // Fill in the player objects
        players = new Player[numPlayers];
        for (int i = 0; i < numPlayers; i++)
        {
            players[i] = new Player(playerNames[i], this);
        }

        // Since the inputs are validated, it's time to roll for towers.
        assignTowers();

        // Renumber players in descending tower order.
        sortPlayers();

        for (int i = 0; i < numPlayers; i++)
        {
            new PickColor(frame, this, i);
            players[i].initMarkersAvailable();
        }

        statusScreen = new StatusScreen(this);
        board = new MasterBoard(this, true);
    }
    
    
    // Load a saved game.
    public Game(boolean isApplet, String filename)
    {
        this.isApplet = isApplet;
        Chit.setApplet(isApplet);

        board = new MasterBoard(this, false);
        loadGame(filename);
        statusScreen = new StatusScreen(this);
    }


    // Use a Fisher-Yates shuffle to randomize towers.
    private void assignTowers()
    {
        int [] playerTower = new int[6];

        for (int i = 0; i < 6; i++)
        {
            playerTower[i] = i + 1;
        }

        for (int i = numPlayers - 1; i >= 0; i--)
        {
            int j = (int) Math.floor((i + 1) * Math.random());
            if (i != j)
            {
                int t = playerTower[i];
                playerTower[i] = playerTower[j];
                playerTower[j] = t;
            }
        }

        for (int i = 0; i < numPlayers; i++)
        {
            players[i].setTower(playerTower[i]);
        }
    }


    // Sort player array by descending tower number, into the order
    // in which they'll move.
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


    public int getNumPlayers()
    {
        return numPlayers;
    }
    
    
    public void setNumPlayers(int numPlayers)
    {
        this.numPlayers = numPlayers;
    }


    public void setPlayerName(int playerNum, String name)
    {
        if (playerNum < 0 || playerNum > getNumPlayers() - 1)
        {
            return;
        }
        playerNames[playerNum] = name;
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
                new MessageBox(board, "draw");
                dispose();
                break;

            case 1:
                new MessageBox(board, players[winner].getName() + " wins");
                dispose();
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

        // XXX This should be removed eventually.
        if (!isApplet)
        {
            saveGame();
        }
    }


    public void updateStatusScreen()
    {
        if (statusScreen != null)
        {
            statusScreen.updateStatusScreen();
        }
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
        File savesDir = new File("saves");
        if (!savesDir.exists() || !savesDir.isDirectory())
        {
             if (!savesDir.mkdir())
             {
                 System.out.println("Could not create saves directory");
                 return;
             }
        }

        String filename = "saves/" + date.getTime() + ".sav";
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


    // Try to load a game from ./filename first, then from saves/filename.
    // If the filename is "--latest" then load the latest savegame found in
    // saves/ 
    private void loadGame(String filename)
    {
        // XXX Need a dialog to pick the savegame's filename, and 
        //     confirmation if there's already a game in progress.
        File file;

        if (filename.equals("--latest"))
        {
            File dir = new File("saves");
            if (!dir.exists() || !dir.isDirectory())
            {
                System.out.println("No saves directory");
                dispose();
            }
            String [] filenames = dir.list(new SaveGameFilter());
            if (filenames.length < 1)
            {
                System.out.println("No savegames found in saves directory");
                dispose();
            }
            sortSaveFilenames(filenames);
            file = new File("saves/" + filenames[0]); 
        }
        else
        {
            file = new File(filename);
            if (!file.exists())
            {
                file = new File("saves/" + filename);
            }
        }
         
        try
        {
            FileReader fileReader = new FileReader(file);
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

                    Legion legion = new Legion(3 * board.getScale(), 
                        markerId, null, board, height, 
                        board.getHexFromLabel(hexLabel), creatures[0], 
                        creatures[1], creatures[2], creatures[3], creatures[4],
                        creatures[5], creatures[6], creatures[7], players[i]);

                    for (int k = 0; k < height; k++)
                    {
                        if (visibles[k])
                        {
                            legion.revealCreature(k);
                        }
                    }

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
            dispose();
        }
    }


    // Extract and return the numeric part of a filename.
    long numberValue(String filename)
    {
        StringBuffer numberPart = new StringBuffer();
        for (int i = 0; i < filename.length(); i++)
        {
            char ch = filename.charAt(i);
            if (Character.isDigit(ch))
            {
                numberPart.append(ch);
            }
        }
        try
        {
            return Long.valueOf(new String(numberPart)).longValue();
        }
        catch (NumberFormatException e)
        {
            return -1L;
        }
    }


    // Sort filenames in descending numeric order.  (1000000000.sav
    // comes before 999999999.sav)
    void sortSaveFilenames(String [] filenames)
    {
        for (int i = 0; i < filenames.length - 1; i++)
        {
            for (int j = i + 1; j < filenames.length; j++)
            {
                if (numberValue(filenames[i]) < numberValue(filenames[j]))
                {
                    String temp = filenames[i];
                    filenames[i] = filenames[j];
                    filenames[j] = temp;
                }
            }
        }
    }


    public void dispose()
    {
        if (isApplet)
        {
            if (board != null)
            {
                board.dispose();
            }
            if (statusScreen != null)
            {
                statusScreen.dispose();
            }
        }
        else
        {
            System.exit(0);
        }
    }


    public boolean isApplet()
    {
        return isApplet;
    }


    public static void main(String [] args)
    {
        if (args.length == 0)
        {
            new Game(false);
        }
        else
        {
            new Game(false, args[0]);
        }
    }
}
