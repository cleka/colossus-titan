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
    private int activePlayerNum;
    private int turnNumber = 1;  // Advance when every player has a turn
    private StatusScreen statusScreen;
    private Turn turn;
    private GameApplet applet;
    private boolean summoningAngel;
    private SummonAngel summonAngel;
    private Battle battle;
    private BattleMap map;
    private static Random random = new Random();

    // Keep multiple quick clicks from popping up multiples
    // of the same dialog.
    private boolean dialogLock;
    private boolean dialogLock2;

    public static final int SPLIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int MUSTER = 4;
    private int phase = SPLIT;

    private boolean isApplet;
    private boolean disposed;

    // For debugging, or if the game crashes after movement
    // has been rolled, we can force the next movement roll
    // from the command line.
    private int forcedMovementRoll;

    // XXX These should be added to the options menu.
    private static boolean autosaveEveryTurn = true;
    private static boolean allVisible;
    private static boolean pickRecruiter = true;


    public Game(boolean isApplet, GameApplet applet)
    {
        this.isApplet = isApplet;
        Chit.setApplet(isApplet);
        if (applet != null)
        {
            this.applet = applet;
        }

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

        if (!disposed)
        {
            statusScreen = new StatusScreen(this);
            board = new MasterBoard(this);
            for (int i = 0; i < getNumPlayers(); i++)
            {
                pickInitialMarker(getPlayer(i));
                placeInitialLegion(getPlayer(i));
                updateStatusScreen();
            }
            board.loadInitialMarkerImages();

            turn = new Turn(this, board);
            board.setVisible(true);
            board.repaint();
        }
    }


    // Load a saved game.
    public Game(boolean isApplet, GameApplet applet, String filename)
    {
        this.isApplet = isApplet;
        Chit.setApplet(isApplet);
        if (applet != null)
        {
            this.applet = applet;
        }

        board = new MasterBoard(this);
        board.loadInitialMarkerImages();
        loadGame(filename);
        statusScreen = new StatusScreen(this);
    }


    // Load a saved game, and force the first movement roll.
    public Game(boolean isApplet, GameApplet applet, String filename,
        int forcedMovementRoll)
    {
        // Call the normal saved game constructor.
        this(isApplet, applet, filename);

        if (forcedMovementRoll >= 1 && forcedMovementRoll <= 6)
        {
            this.forcedMovementRoll = forcedMovementRoll;
        }
    }


    // For testing only.
    public Game()
    {
        players = new Player[6];
    }


    private static String getPhaseName(int phase)
    {
        switch (phase)
        {
            case SPLIT:
                return "Split";
            case MOVE:
                return "Move";
            case FIGHT:
                return "Fight";
            case MUSTER:
                return "Muster";
            default:
                return "?????";
        }
    }


    // Randomize towers by rolling dice and rerolling ties.
    private void assignTowers()
    {
        int [] playerTower = new int[numPlayers];
        int [] rolls = new int[numPlayers];

        final int UNASSIGNED = 0;
        for (int i = 0; i < numPlayers; i++)
        {
            playerTower[i] = UNASSIGNED;
        }

        int playersLeft = numPlayers;
        while (playersLeft > 0)
        {
            for (int i = 0; i < numPlayers; i++)
            {
                if (playerTower[i] == UNASSIGNED)
                {
                    rolls[i] = rollDie();
                }
            }

            for (int i = 0; i < numPlayers; i++)
            {
                if (playerTower[i] == 0)
                {
                    boolean unique = true;
                    for (int j = 0; j < numPlayers; j++)
                    {
                        if (i != j && rolls[i] == rolls[j])
                        {
                            unique = false;
                            break;
                        }
                    }
                    if (unique)
                    {
                        playerTower[i] = rolls[i];
                        playersLeft--;
                    }
                }
            }
        }

        for (int i = 0; i < numPlayers; i++)
        {
            logEvent(players[i].getName() + " gets tower " + playerTower[i]);
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


    public boolean getAllVisible()
    {
        return allVisible;
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


    private int getNumLivingPlayers()
    {
        int count = 0;
        for (int i = 0; i < numPlayers; i++)
        {
            if (!players[i].isDead())
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


    public void setPlayer(int i, Player player)
    {
        if (i >= 0 && i < numPlayers)
        {
            players[i] = player;
        }
    }


    public void checkForVictory()
    {
        int remaining = 0;
        int winner = -1;

        for (int i = 0; i < numPlayers; i++)
        {
            if (!players[i].isDead())
            {
                remaining++;
                winner = i;
            }
        }

        switch (remaining)
        {
            case 0:
                logEvent("Draw");
                new MessageBox(board, "Draw");
                dispose();
                break;

            case 1:
                logEvent(players[winner].getName() + " wins");
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
        phase++;

        if (phase > MUSTER ||
            (getActivePlayer().isDead() && getNumLivingPlayers() > 0))
        {
            advanceTurn();
        }
        else
        {
            board.unselectAllHexes();
            logEvent("Phase advances to " + getPhaseName(phase));
        }
    }


    private void advanceTurn()
    {
        board.unselectAllHexes();
        activePlayerNum++;
        if (activePlayerNum == numPlayers)
        {
            activePlayerNum = 0;
            turnNumber++;
        }
        phase = SPLIT;
        if (getActivePlayer().isDead() && getNumLivingPlayers() > 0)
        {
            advanceTurn();
        }
        else
        {
            logEvent("\n" + getActivePlayer().getName() +
                "'s turn, number " + turnNumber);

            updateStatusScreen();

            if (!isApplet && autosaveEveryTurn)
            {
                saveGame();
            }
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
            // Check if the player is alive.
            out.println(!player.isDead());
            out.println(player.getMulligansLeft());
            out.println(player.getPlayersElim());
            out.println(player.getNumMarkersAvailable());

            Collection markerIds = player.getMarkersAvailable();
            Iterator it = markerIds.iterator();
            while (it.hasNext())
            {
                String markerId = (String)it.next();
                out.println(markerId);
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


    private class SaveGameFilter implements FilenameFilter
    {
        public boolean accept(File dir, String name)
        {
            if (name.endsWith(".sav"))
            {
                return true;
            }
            else
            {
                return false;
            }
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

                // Output whether the player is alive.
                players[i].setDead(!Boolean.valueOf(buf).booleanValue());

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
                        markerId, null, board,
                        MasterBoard.getHexFromLabel(hexLabel), creatures[0],
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

            // Move all legions into their hexes.
            for (int i = 0; i < getNumPlayers(); i++)
            {
                Player player = getPlayer(i);
                for (int j = 0; j < player.getNumLegions(); j++)
                {
                    Legion legion = player.getLegion(j);
                    MasterHex hex = legion.getCurrentHex();
                    hex.addLegion(legion);
                }
            }

            turn = new Turn(this, board);
            board.setVisible(true);
            board.repaint();
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
    private long numberValue(String filename)
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
    private void sortSaveFilenames(String [] filenames)
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


    public static void doRecruit(Creature recruit, Legion legion,
        Frame parentFrame)
    {
        // Pick the recruiter(s) if necessary.
        Critter [] recruiters = new Critter[4];
        Critter recruiter;

        int numEligibleRecruiters = findEligibleRecruiters(legion, recruit,
            recruiters);

        if (numEligibleRecruiters == 1)
        {
            recruiter = recruiters[0];
        }
        else if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiter = null;
        }
        else if (allRecruitersVisible(legion, recruiters))
        {
            // If all possible recruiters are already visible, don't
            // bother picking which ones to reveal.
            recruiter = recruiters[0];
        }
        else
        {
            // Only use the PickRecruiter dialog if the pickRecruiter
            // option is true.  If it's false, just use the first one.
            if (pickRecruiter)
            {
                new PickRecruiter(parentFrame, legion, numEligibleRecruiters,
                    recruiters);
            }
            recruiter = recruiters[0];
        }

        if (recruit != null && (recruiter != null ||
            numEligibleRecruiters == 0))
        {
            legion.addCreature(recruit, true);

            // Mark the recruiter(s) as visible.
            int numRecruiters = numberOfRecruiterNeeded(recruiter,
                recruit, legion.getCurrentHex().getTerrain());
            if (numRecruiters >= 1)
            {
                legion.revealCreatures(recruiter, numRecruiters);
            }

            logEvent("Legion " + legion.getMarkerId() + " in " +
                legion.getCurrentHex().getDescription() +
                " recruits " + recruit.getName() + " with " +
                (numRecruiters == 0 ? "nothing" :
                numRecruiters + " " + (numRecruiters > 1 ?
                recruiter.getPluralName() : recruiter.getName())));

            // Recruits are one to a customer.
            legion.markRecruited();

            legion.getPlayer().markLastLegionRecruited(legion);
        }

    }


    // Returns the number of types of angels that can be acquired.
    public static int findEligibleAngels(Legion legion, Creature [] recruits,
        boolean archangel)
    {
        if (legion.getHeight() >= 7)
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
        disposed = true;

        if (isApplet)
        {
            if (board != null)
            {
                board.dispose();
            }
            if (map != null)
            {
                map.dispose();
            }
            if (statusScreen != null)
            {
                statusScreen.dispose();
            }
            if (applet != null)
            {
                applet.destroy();
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


    public static void logEvent(String s)
    {
        System.out.println(s);
    }


    // Put all die rolling in one place, in case we decide to change random
    // number algorithms, use an external dice server, etc.
    public static int rollDie()
    {
        return random.nextInt(6) + 1;
    }


    public void pickInitialMarker(Player player)
    {
        do
        {
            new PickMarker(board, player);
        }
        while (player.getSelectedMarker() == null);

        logEvent(player.getName() + " selected initial marker");
    }


    public void placeInitialLegion(Player player)
    {
        // Lookup coords for chit starting from player[i].getTower()
        MasterHex hex = MasterBoard.getHexFromLabel(100 * player.getTower());

        Creature.titan.takeOne();
        Creature.angel.takeOne();
        Creature.ogre.takeOne();
        Creature.ogre.takeOne();
        Creature.centaur.takeOne();
        Creature.centaur.takeOne();
        Creature.gargoyle.takeOne();
        Creature.gargoyle.takeOne();

        Legion legion = new Legion(3 * MasterBoard.getScale(),
            player.getSelectedMarker(), null, board, hex, Creature.titan,
            Creature.angel, Creature.ogre, Creature.ogre, Creature.centaur,
            Creature.centaur, Creature.gargoyle, Creature.gargoyle, player);

        player.addLegion(legion);
        hex.addLegion(legion);
    }


    public void highlightUnmovedLegions()
    {
        board.unselectAllHexes();

        Player player = getActivePlayer();
        player.unselectLegion();

        TreeSet set = new TreeSet();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            if (!legion.hasMoved())
            {
                MasterHex hex = legion.getCurrentHex();
                set.add(hex.getLabel());
            }
        }

        board.selectHexesByLabels(set);

        board.repaint();
    }


    public int getForcedMovementRoll()
    {
        return forcedMovementRoll;
    }


    public void clearForcedMovementRoll()
    {
        forcedMovementRoll = 0;
    }


    private static final int ARCHES_AND_ARROWS = -1;
    private static final int ARROWS_ONLY = -2;

    private static final int NOWHERE = -1;


    /** Recursively find conventional moves from this hex.  Select
     *  all legal final destinations.  If block >= 0, go only
     *  that way.  If block == -1, use arches and arrows.  If
     *  block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.
     */
    private Set findMoves(MasterHex hex, Player player, Legion legion,
        int roll, int block, int cameFrom)
    {
        TreeSet set = new TreeSet();

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (hex.getNumEnemyLegions(player) > 0)
        {
            if (hex.getNumFriendlyLegions(player) == 0)
            {
                set.add(hex.getLabel());
                // XXX
                // Set the entry side relative to the hex label.
                hex.setEntrySide((6 + cameFrom - hex.getLabelSide()) % 6);
            }
            return set;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            for (int i = 0; i < player.getNumLegions(); i++)
            {
                // Account for spin cycles.
                if (player.getLegion(i).getCurrentHex() == hex &&
                    player.getLegion(i) != legion)
                {
                    return set;
                }
            }

            set.add(hex.getLabel());

            // XXX
            // Need to set entry sides even if no possible engagement,
            // for MasterHex.chooseWhetherToTeleport()
            hex.setEntrySide((6 + cameFrom - hex.getLabelSide()) % 6);

            return set;
        }


        if (block >= 0)
        {
            set.addAll(findMoves(hex.getNeighbor(block), player, legion,
                roll - 1, ARROWS_ONLY, (block + 3) % 6));
        }
        else if (block == ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARCH && i != cameFrom)
                {
                    set.addAll(findMoves(hex.getNeighbor(i), player, legion,
                        roll - 1, ARROWS_ONLY, (i + 3) % 6));
                }
            }
        }
        else if (block == ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARROW && i != cameFrom)
                {
                    set.addAll(findMoves(hex.getNeighbor(i), player, legion,
                        roll - 1, ARROWS_ONLY, (i + 3) % 6));
                }
            }
        }

        return set;
    }


    // Recursively find tower teleport moves from this hex.  That's
    // all unoccupied hexes within 6 hexes.  Teleports to towers
    // are handled separately.  Do not double back.
    private Set findTowerTeleportMoves(MasterHex hex, Player player,
        Legion legion, int roll, int cameFrom)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.

        TreeSet set = new TreeSet();

        if (!hex.isOccupied())
        {
            set.add(hex.getLabel());

            // XXX
            // Mover can choose side of entry.
            hex.setTeleported(true);
        }

        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom && (hex.getExitType(i) != MasterHex.NONE ||
                   hex.getEntranceType(i) != MasterHex.NONE))
                {
                    set.addAll(findTowerTeleportMoves(hex.getNeighbor(i),
                        player, legion, roll - 1, (i + 3) % 6));
                }
            }
        }

        return set;
    }


    /** Return number of legal non-teleport moves. */
    public int countConventionalMoves(Legion legion)
    {
        return showMoves(legion, false).size();
    }


    /** Select hexes where this legion can move. Return total number of
     *  legal moves. */
    public int highlightMoves(Legion legion)
    {
        Set set = showMoves(legion, true);
        board.unselectAllHexes();
        board.selectHexesByLabels(set);
        return set.size();
    }


    /** Return set of hex labels where this legion can move.
     *  Include teleport moves only if teleport is true. */
    private Set showMoves(Legion legion, boolean teleport)
    {
        Set set = new TreeSet();

        if (legion.hasMoved())
        {
            return set;
        }

        Player player = legion.getPlayer();

        // XXX entry sides
        board.clearAllNonFriendlyOccupiedEntrySides(player);

        MasterHex hex = legion.getCurrentHex();

        // Conventional moves

        // First, look for a block.
        int block = ARCHES_AND_ARROWS;
        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == MasterHex.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }

        set.addAll(findMoves(hex, player, legion, player.getMovementRoll(),
            block, NOWHERE));

        if (teleport && player.getMovementRoll() == 6)
        {
            // Tower teleport
            if (hex.getTerrain() == 'T' && legion.numLords() > 0 &&
                player.canTeleport())
            {
                // Mark every unoccupied hex within 6 hexes.
                set.addAll(findTowerTeleportMoves(hex, player, legion, 6,
                    NOWHERE));

                // Mark every unoccupied tower.
                for (int tower = 100; tower <= 600; tower += 100)
                {
                    hex = MasterBoard.getHexFromLabel(tower);
                    if (!hex.isOccupied())
                    {
                        set.add(hex.getLabel());

                        // XXX
                        // Mover can choose side of entry.
                        hex.setTeleported(true);
                    }
                }
            }

            // Titan teleport
            if (player.canTitanTeleport() &&
                legion.numCreature(Creature.titan) > 0)
            {
                // Mark every hex containing an enemy stack that does not
                // already contain a friendly stack.
                for (int i = 0; i < getNumPlayers(); i++)
                {
                    if (getPlayer(i) != player)
                    {
                        for (int j = 0; j < getPlayer(i).getNumLegions();
                            j++)
                        {
                            hex = getPlayer(i).getLegion(j).getCurrentHex();
                            if (!hex.isEngagement())
                            {
                                set.add(hex.getLabel());
                                // XXX
                                // Mover can choose side of entry.
                                hex.setTeleported(true);
                            }
                        }
                    }
                }
            }
        }

        return set;
    }


    /** Present a dialog allowing the player to enter via land or teleport. */
    private void chooseWhetherToTeleport(MasterHex hex)
    {
        new OptionDialog(board, "Teleport?", "Teleport?", "Teleport",
            "Move Normally");

        // If Teleport, then leave teleported set.
        if (OptionDialog.getLastAnswer() == OptionDialog.NO_OPTION)
        {
            hex.setTeleported(false);
        }
    }


    /** Return number of engagements found. */
    public int highlightEngagements()
    {
        int count = 0;
        Player player = getActivePlayer();

        board.unselectAllHexes();

        TreeSet set = new TreeSet();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            MasterHex hex = legion.getCurrentHex();
            if (hex.getNumEnemyLegions(player) > 0)
            {
                count++;
                set.add(hex.getLabel());
            }
        }

        board.selectHexesByLabels(set);

        return count;
    }


    public void setSummonAngel(SummonAngel summonAngel)
    {
        this.summonAngel = summonAngel;
    }


    public SummonAngel getSummonAngel()
    {
        return summonAngel;
    }


    public void finishBattle()
    {
        board.show();

        if (summoningAngel && summonAngel != null)
        {
            highlightSummonableAngels(summonAngel.getLegion());
            summonAngel.repaint();
        }
        else
        {
            highlightEngagements();
        }
        battle = null;
        map = null;

        turn.setVisible(true);
        turn.setEnabled(true);

        // Insert a blank line in the log file after each battle.
        logEvent("\n");
    }


    // Returns number of legions with summonable angels.
    public int highlightSummonableAngels(Legion legion)
    {
        board.unselectAllHexes();

        Player player = legion.getPlayer();
        player.unselectLegion();

        int count = 0;

        TreeSet set = new TreeSet();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion candidate = player.getLegion(i);
            if (candidate != legion)
            {
                MasterHex hex = candidate.getCurrentHex();
                if ((candidate.numCreature(Creature.angel) > 0 ||
                    candidate.numCreature(Creature.archangel) > 0) &&
                    !hex.isEngagement())
                {

                    count++;
                    set.add(hex.getLabel());
                }
            }
        }

        if (count > 0)
        {
            summoningAngel = true;
            board.selectHexesByLabels(set);
        }

        return count;
    }


    public void finishSummoningAngel()
    {
        summoningAngel = false;
        highlightEngagements();
        summonAngel = null;
        if (battle != null)
        {
            battle.finishSummoningAngel();
        }
    }


    // Returns number of legions that can recruit.
    public int highlightPossibleRecruits()
    {
        int count = 0;
        Player player = getActivePlayer();

        TreeSet set = new TreeSet();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            if (legion.hasMoved() && legion.canRecruit())
            {
                Creature [] recruits = new Creature[5];
                if (findEligibleRecruits(legion, recruits) >= 1)
                {
                    MasterHex hex = legion.getCurrentHex();
                    set.add(hex.getLabel());
                    count++;
                }
            }
        }

        if (count > 0)
        {
            board.selectHexesByLabels(set);
        }

        return count;
    }


    public void actOnLegion(Legion legion)
    {
        Player player = legion.getPlayer();

        switch (getPhase())
        {
            case Game.SPLIT:
                // Need a legion marker to split.
                if (player.getNumMarkersAvailable() == 0)
                {
                    new MessageBox(board, "No markers are available.");
                    return;
                }
                // Don't allow extra splits in turn 1.
                if (getTurnNumber() == 1 && player.getNumLegions() > 1)
                {
                    new MessageBox(board, "Cannot split twice on Turn 1.");
                    return;
                }

                if (!dialogLock)
                {
                    dialogLock = true;
                    new SplitLegion(board, legion, player);
                    dialogLock = false;
                }

                // Update status window.
                updateStatusScreen();
                // If we split, unselect this hex.
                if (legion.getHeight() < 7)
                {
                    MasterHex hex = legion.getCurrentHex();
                    board.unselectHexByLabel(hex.getLabel());
                }
                return;

            case Game.MOVE:
                // Mark this legion as active.
                player.selectLegion(legion);

                // Highlight all legal destinations
                // for this legion.
                highlightMoves(legion);
                return;

            case Game.FIGHT:
                doFight(legion.getCurrentHex(), player);
                break;

            case Game.MUSTER:
                if (legion.hasMoved() && legion.canRecruit())
                {
                    if (!dialogLock)
                    {
                        dialogLock = true;
                        new PickRecruit(board, legion);
                        if (!legion.canRecruit())
                        {
                            board.unselectHexByLabel(
                                legion.getCurrentHex().getLabel());

                            updateStatusScreen();
                        }
                        dialogLock = false;
                    }
                }

                return;
        }
    }


    public void actOnHex(MasterHex hex)
    {
        Player player = getActivePlayer();

        switch (getPhase())
        {
            // If we're moving, and have selected a legion which
            // has not yet moved, and this hex is a legal
            // destination, move the legion here.
            case Game.MOVE:
                Legion legion = player.getSelectedLegion();
                if (legion != null && hex.isSelected())
                {
                    // Pick teleport or normal move if necessary.
                    if (hex.getTeleported() && hex.canEnterViaLand())
                    {
                        chooseWhetherToTeleport(hex);
                    }

                    // If this is a tower hex, set the entry side
                    // to '3', regardless.
                    if (hex.getTerrain() == 'T')
                    {
                        hex.clearAllEntrySides();
                        hex.setEntrySide(3);
                    }
                    // If this is a teleport to a non-tower hex,
                    // then allow entry from all three sides.
                    else if (hex.getTeleported())
                    {
                        hex.setEntrySide(1);
                        hex.setEntrySide(3);
                        hex.setEntrySide(5);
                    }

                    // Pick entry side if hex is enemy-occupied
                    // and there is more than one possibility.
                    if (hex.isOccupied() && hex.getNumEntrySides() > 1)
                    {
                        // Only allow one PickEntrySide dialog.
                        if (!dialogLock)
                        {
                            dialogLock = true;
                            new PickEntrySide(board, hex);
                            dialogLock = false;
                        }
                    }

                    // Unless a PickEntrySide was cancelled or
                    // disallowed, execute the move.
                    if (!hex.isOccupied() || hex.getNumEntrySides() == 1)
                    {
                        // If the legion teleported, reveal a lord.
                        if (hex.getTeleported())
                        {

                            // If it was a Titan teleport, that
                            // lord must be the titan.
                            if (hex.isOccupied())
                            {
                                legion.revealCreatures(Creature.titan, 1);
                            }
                            else
                            {
                                legion.revealTeleportingLord(board);
                            }
                        }

                        legion.moveToHex(hex);
                        legion.getStartingHex().repaint();
                        hex.repaint();
                    }

                    highlightUnmovedLegions();
                }
                else
                {
                    highlightUnmovedLegions();
                }
                break;

            // If we're fighting and there is an engagement here,
            // resolve it.  If an angel is being summoned, mark
            // the donor legion instead.
            case Game.FIGHT:
                doFight(hex, player);
                break;

            default:
                break;
        }
    }


    public void doFight(MasterHex hex, Player player)
    {
        if (summoningAngel)
        {
            Legion donor = hex.getFriendlyLegion(player);
            player.selectLegion(donor);
            if (summonAngel == null)
            {
                summonAngel = battle.getSummonAngel();
            }
            summonAngel.repaint();
            donor.getMarker().repaint();
        }

        // Do not allow clicking on engagements if one is
        // already being resolved.
        else if (hex.isEngagement() && !dialogLock)
        {
            dialogLock = true;
            Legion attacker = hex.getFriendlyLegion(player);
            Legion defender = hex.getEnemyLegion(player);

            if (defender.canFlee())
            {
                // Fleeing gives half points and denies the
                // attacker the chance to summon an angel.
                new Concede(board, defender, attacker, true);
            }

            if (hex.isEngagement())
            {
                // The attacker may concede now without
                // allowing the defender a reinforcement.
                new Concede(board, attacker, defender, false);

                // The players may agree to a negotiated
                // settlement.
                if (hex.isEngagement())
                {
                    new Negotiate(board, attacker, defender);
                }


                if (!hex.isEngagement())
                {
                    if (hex.getLegion(0) == defender && defender.canRecruit())
                    {
                        // If the defender won the battle by agreement,
                        // he may recruit.
                        if (!dialogLock2)
                        {
                            dialogLock2 = true;
                            new PickRecruit(board, defender);
                            dialogLock2 = false;
                        }
                    }
                    else if (hex.getLegion(0) == attacker &&
                        attacker.getHeight() < 7 &&
                        player.canSummonAngel())
                    {
                        // If the attacker won the battle by agreement,
                        // he may summon an angel.
                        summonAngel = new SummonAngel(board, attacker);
                    }
                }

                // Battle
                if (hex.isEngagement())
                {
                    // Hide turn to keep it out of the way.
                    turn.setVisible(false);
                    turn.setEnabled(false);

                    // Reveal both legions to all players.
                    attacker.revealAllCreatures();
                    defender.revealAllCreatures();
                    battle = new Battle(board, attacker, defender,
                        hex);
                    map = battle.getBattleMap();
                }
            }

            highlightEngagements();
            dialogLock = false;
        }
    }


    public void actOnMisclick()
    {
        switch (getPhase())
        {
            case Game.MOVE:
                highlightUnmovedLegions();
                break;

            case Game.FIGHT:
                if (summoningAngel && summonAngel != null)
                {
                    highlightSummonableAngels(summonAngel.getLegion());
                    summonAngel.repaint();
                }
                else
                {
                    highlightEngagements();
                }
                break;

            default:
                break;
        }
    }


    public static void main(String [] args)
    {
        if (args.length == 0)
        {
            // Start a new game.
            new Game(false, null);
        }
        else if (args.length == 1)
        {
            // Load a game.
            new Game(false, null, args[0]);
        }
        else
        {
            // Load a game, and specify the next movement roll.
            new Game(false, null, args[0], Integer.parseInt(args[1]));
        }
    }
}
