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

        for (int i = numPlayers - 1; i >= 0; i--)
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

        for (int i = playerTower.length - 1; i >= 0; i--)
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
    
    
    // Returns the number of the given recruiter needed to muster the given
    // recruit in the given terrain.  Returns -1 on error.
    public static int numberOfRecruiterNeeded(Critter recruiter, Creature
        recruit, char terrain)
    {
        switch (terrain)
        {
            case 'B':
                if (recruit.getName().equals("Gargoyle"))
                {
                    if (recruiter.getName().equals("Gargoyle") || 
                        recruiter.getName().equals("Cyclops") ||
                        recruiter.getName().equals("Gorgon"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Cyclops"))
                {
                    if (recruiter.getName().equals("Gargoyle"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Cyclops") ||
                             recruiter.getName().equals("Gorgon"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Gorgon"))
                {
                    if (recruiter.getName().equals("Cyclops"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Gorgon"))
                    {
                        return 1;
                    }
                }
                break;
            
            case 'D':
                if (recruit.getName().equals("Lion"))
                {
                    if (recruiter.getName().equals("Lion") || 
                        recruiter.getName().equals("Griffon") ||
                        recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Griffon"))
                {
                    if (recruiter.getName().equals("Lion"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Griffon") ||
                             recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Hydra"))
                {
                    if (recruiter.getName().equals("Griffon"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                break;

            case 'H':
                if (recruit.getName().equals("Ogre"))
                {
                    if (recruiter.getName().equals("Ogre") || 
                        recruiter.getName().equals("Minotaur") ||
                        recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Minotaur"))
                {
                    if (recruiter.getName().equals("Ogre"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Minotaur") ||
                             recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Unicorn"))
                {
                    if (recruiter.getName().equals("Minotaur"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                break;

            case 'J':
                if (recruit.getName().equals("Gargoyle"))
                {
                    if (recruiter.getName().equals("Gargoyle") || 
                        recruiter.getName().equals("Cyclops") ||
                        recruiter.getName().equals("Behemoth") ||
                        recruiter.getName().equals("Serpent"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Cyclops"))
                {
                    if (recruiter.getName().equals("Gargoyle"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Cyclops") ||
                             recruiter.getName().equals("Behemoth") ||
                             recruiter.getName().equals("Serpent"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Behemoth"))
                {
                    if (recruiter.getName().equals("Cyclops"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Behemoth") ||
                             recruiter.getName().equals("Serpent"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Serpent"))
                {
                    if (recruiter.getName().equals("Behemoth"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Serpent"))
                    {
                        return 1;
                    }
                }
                break;

            case 'm':
                if (recruit.getName().equals("Lion"))
                {
                    if (recruiter.getName().equals("Lion") || 
                        recruiter.getName().equals("Minotaur") ||
                        recruiter.getName().equals("Dragon") ||
                        recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Minotaur"))
                {
                    if (recruiter.getName().equals("Lion"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Minotaur") ||
                             recruiter.getName().equals("Dragon") ||
                             recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Dragon"))
                {
                    if (recruiter.getName().equals("Minotaur"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Dragon") ||
                             recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Colossus"))
                {
                    if (recruiter.getName().equals("Dragon"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                break;

            case 'M':
                if (recruit.getName().equals("Ogre"))
                {
                    if (recruiter.getName().equals("Ogre") || 
                        recruiter.getName().equals("Troll") ||
                        recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Troll"))
                {
                    if (recruiter.getName().equals("Ogre"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Troll") ||
                             recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Ranger"))
                {
                    if (recruiter.getName().equals("Troll"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                break;

            case 'P':
                if (recruit.getName().equals("Centaur"))
                {
                    if (recruiter.getName().equals("Centaur") || 
                        recruiter.getName().equals("Lion") ||
                        recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Lion"))
                {
                    if (recruiter.getName().equals("Centaur"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Lion") ||
                             recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Ranger"))
                {
                    if (recruiter.getName().equals("Lion"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                break;

            case 'S':
                if (recruit.getName().equals("Troll"))
                {
                    if (recruiter.getName().equals("Troll") || 
                        recruiter.getName().equals("Wyvern") ||
                        recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Wyvern"))
                {
                    if (recruiter.getName().equals("Troll"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Wyvern") ||
                             recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Hydra"))
                {
                    if (recruiter.getName().equals("Wyvern"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                break;

            case 't':
                if (recruit.getName().equals("Troll"))
                {
                    if (recruiter.getName().equals("Troll") || 
                        recruiter.getName().equals("Warbear") ||
                        recruiter.getName().equals("Giant") ||
                        recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Warbear"))
                {
                    if (recruiter.getName().equals("Troll"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Warbear") ||
                             recruiter.getName().equals("Giant") ||
                             recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Giant"))
                {
                    if (recruiter.getName().equals("Warbear"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Giant") ||
                             recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Colossus"))
                {
                    if (recruiter.getName().equals("Giant"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                break;

            case 'T':
                if (recruit.getName().equals("Centaur") ||
                    recruit.getName().equals("Gargoyle") ||
                    recruit.getName().equals("Ogre"))
                {
                    return 0;
                }
                else if (recruit.getName().equals("Warlock"))
                {
                    if (recruiter.getName().equals("Titan") ||
                        recruiter.getName().equals("Warlock"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Guardian"))
                {
                    if (recruiter.getName().equals("Behemoth") ||
                        recruiter.getName().equals("Centaur") ||
                        recruiter.getName().equals("Colossus") ||
                        recruiter.getName().equals("Cyclops") ||
                        recruiter.getName().equals("Dragon") ||
                        recruiter.getName().equals("Gargoyle") ||
                        recruiter.getName().equals("Giant") ||
                        recruiter.getName().equals("Gorgon") ||
                        recruiter.getName().equals("Griffon") ||
                        recruiter.getName().equals("Hydra") ||
                        recruiter.getName().equals("Lion") ||
                        recruiter.getName().equals("Minotaur") ||
                        recruiter.getName().equals("Ogre") ||
                        recruiter.getName().equals("Ranger") ||
                        recruiter.getName().equals("Serpent") ||
                        recruiter.getName().equals("Troll") ||
                        recruiter.getName().equals("Unicorn") ||
                        recruiter.getName().equals("Warbear") ||
                        recruiter.getName().equals("Wyvern"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Guardian"))
                    {
                        return 1;
                    }
                }
                break;

            case 'W':
                if (recruit.getName().equals("Centaur"))
                {
                    if (recruiter.getName().equals("Centaur") || 
                        recruiter.getName().equals("Warbear") ||
                        recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Warbear"))
                {
                    if (recruiter.getName().equals("Centaur"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Warbear") ||
                             recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Unicorn"))
                {
                    if (recruiter.getName().equals("Warbear"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                break;

            default:
                return -1;
        }
        return -1;
    }


    // Returns the number of eligible recruits.  The passed-in recruits array
    // should be of length 5, and will be filled with the eligible recruits.
    public static int findEligibleRecruits(Legion legion, Creature [] recruits)
    {
        // Paranoia
        if (recruits.length != 5)
        {
            System.out.println("Bad arg passed to findEligibleRecruits()");
            return 0;
        }
        for (int i = 0; i < recruits.length; i++)
        {
            recruits[i] = null;
        }
        
        MasterHex hex = legion.getCurrentHex();

        // Towers are a special case.
        if (hex.getTerrain() == 'T')
        {
            recruits[0] = Creature.centaur;
            recruits[1] = Creature.gargoyle;
            recruits[2] = Creature.ogre;
            if (legion.numCreature(Creature.behemoth) >= 3 ||
                legion.numCreature(Creature.centaur) >= 3 ||
                legion.numCreature(Creature.colossus) >= 3 ||
                legion.numCreature(Creature.cyclops) >= 3 ||
                legion.numCreature(Creature.dragon) >= 3 ||
                legion.numCreature(Creature.gargoyle) >= 3 ||
                legion.numCreature(Creature.giant) >= 3 ||
                legion.numCreature(Creature.gorgon) >= 3 ||
                legion.numCreature(Creature.griffon) >= 3 ||
                legion.numCreature(Creature.guardian) >= 1 ||
                legion.numCreature(Creature.hydra) >= 3 ||
                legion.numCreature(Creature.lion) >= 3 ||
                legion.numCreature(Creature.minotaur) >= 3 ||
                legion.numCreature(Creature.ogre) >= 3 ||
                legion.numCreature(Creature.ranger) >= 3 ||
                legion.numCreature(Creature.serpent) >= 3 ||
                legion.numCreature(Creature.troll) >= 3 ||
                legion.numCreature(Creature.unicorn) >= 3 ||
                legion.numCreature(Creature.warbear) >= 3 ||
                legion.numCreature(Creature.wyvern) >= 3)
            {
                recruits[3] = Creature.guardian;
            }
            if (legion.numCreature(Creature.titan) >= 1 ||
                legion.numCreature(Creature.warlock) >= 1)
            {
                recruits[4] = Creature.warlock;
            }
        }
        else
        {
            int numRecruitTypes = hex.getNumRecruitTypes();
            Creature [] recruitTypes = new Creature[numRecruitTypes];
            for (int i = 0; i < numRecruitTypes; i++)
            {
                recruitTypes[i] = hex.getRecruit(i);
            }
          
            for (int i = numRecruitTypes - 1; i >= 0; i--)
            {
                int numCreature = legion.numCreature(recruitTypes[i]);
                if (numCreature >= 1)
                {
                    int numToRecruit = hex.getNumToRecruit(i + 1);
                    if (numToRecruit > 0 && numCreature >= numToRecruit)
                    {
                        // We can recruit the next highest creature.
                        recruits[i + 1] = recruitTypes[i + 1];
                    }
                    for (int j = i; j >= 0; j--)
                    {
                        // We can recruit this creature and all below it.
                        recruits[j] = recruitTypes[j];
                    }
                    break;
                }
            }
        }


        // Check for availability of chits.
        int count = 0;

        for (int i = 0; i < recruits.length; i++)
        {
            if (recruits[i] != null && recruits[i].getCount() < 1)
            {
                recruits[i] = null;
            }
            if (recruits[i] != null)
            {
                count++;
            }
        }

        // Pack the recruits array for display.
        for (int i = 0; i < count; i++)
        {
            while (recruits[i] == null)
            {
                for (int j = i; j < recruits.length - 1; j++)
                {
                    recruits[j] = recruits[j + 1];
                }
                recruits[recruits.length - 1] = null;
            }
        }

        return count;
    }


    // Returns the number of eligible recruiters.  The passed-in recruiters
    // array should be of length 4 and will be filled in with recruiters.
    // We use a Critter array instead of a Creature array so that Titan
    // power is shown properly.
    public static int findEligibleRecruiters(Legion legion, Creature recruit, 
        Critter [] recruiters)
    {
        // Paranoia
        if (recruiters.length != 4)
        {
            System.out.println("Bad arg passed to findEligibleRecruiters()");
            return 0;
        }
        for (int i = 0; i < recruiters.length; i++)
        {
            recruiters[i] = null;
        }

        MasterHex hex = legion.getCurrentHex();

        int count = 0;

        if (hex.getTerrain() == 'T')
        {
            // Towers are a special case.  The recruiter of tower creatures 
            // remains anonymous, so we only deal with guardians and warlocks.
            if (recruit.getName().equals("Guardian"))
            {
                for (int i = 0; i < Creature.creatures.length; i++)
                {
                    Creature creature = Creature.creatures[i];
                    if (creature.getName().equals("Guardian") && 
                        legion.numCreature(creature) >= 1)
                    {
                        recruiters[count++] = legion.getCritter(creature);
                    }
                    else if (!creature.isImmortal() &&
                        legion.numCreature(creature) >= 3)
                    {
                        recruiters[count++] = legion.getCritter(creature);
                    }
                }
            }
            else if (recruit.getName().equals("Warlock"))
            {
                if (legion.numCreature(Creature.titan) >= 1)
                {
                    recruiters[count++] = legion.getCritter(Creature.titan);
                }
                if (legion.numCreature(Creature.warlock) >= 1)
                {
                    recruiters[count++] = legion.getCritter(Creature.warlock);
                }
            }
        }
        else
        {
            int numRecruitTypes = hex.getNumRecruitTypes();

            for (int i = 0; i < numRecruitTypes; i++)
            {
                if (recruit.getName().equals(hex.getRecruit(i).getName()))
                {
                    int numToRecruit = hex.getNumToRecruit(i);
                    if (numToRecruit > 0 && 
                        legion.numCreature(hex.getRecruit(i - 1)) >= 
                        numToRecruit) 
                    {
                        // Can recruit up.
                        recruiters[count++] = legion.getCritter(
                            hex.getRecruit(i - 1));
                    }
                    for (int j = i; j < numRecruitTypes; j++)
                    {
                        if (legion.numCreature(hex.getRecruit(j)) >= 1)
                        {
                            // Can recruit down or level.
                            recruiters[count++] = legion.getCritter(
                                hex.getRecruit(j));
                        }
                    }
                    break;
                }
            }
        }

        return count;
    }


    // Return true if all members of legion who are in recruiters are
    // already visible.  The passed-in recruiters array must be of
    // length 4.
    public static boolean allRecruitersVisible(Legion legion, 
        Creature [] recruiters)
    {
        // Paranoia
        if (recruiters.length != 4)
        {
            System.out.println("Bad arg passed to allRecruitersVisible()");
            return false;
        }

        int height = legion.getHeight();

        for (int i = 0; i < height; i++)
        {
            Critter critter = legion.getCritter(i);
            if (!critter.isVisible())
            {
                for (int j = 0; j < recruiters.length; j++)
                {
                    Creature recruiter = recruiters[j];
                    if (recruiter != null && recruiter.getName().equals(
                        critter.getName()))
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }


    // Returns the number of types of angels that can be acquired.
    static int findEligibleAngels(Legion legion, Creature [] recruits,
        boolean archangel)
    {
        if (legion.getHeight() > 6)
        {
            return 0;
        }

        recruits[0] = Creature.angel;
        if (archangel)
        {
            recruits[1] = Creature.archangel;
        }

        // Check for availability of chits.
        for (int i = 0; i < recruits.length; i++)
        {
            if (recruits[i] != null && recruits[i].getCount() < 1)
            {
                recruits[i] = null;
            }
        }

        // Pack the recruits array for display.
        for (int i = 0; i < recruits.length - 1; i++)
        {
            if (recruits[i] == null)
            {
                for (int j = i; j < recruits.length - 1; j++)
                {
                    recruits[j] = recruits[j + 1];
                }
                recruits[recruits.length - 1] = null;
            }
        }

        int count = 0;
        for (int i = 0; i < recruits.length; i++)
        {
            if (recruits[i] != null)
            {
                count++;
            }
        }
        return count;
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
