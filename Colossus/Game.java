import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;


/**
 * Class Game gets and holds high-level data about a Titan game.
 * @version $Id$
 * @author David Ripton
 */


public class Game
{
    private ArrayList players = new ArrayList(6);
    private MasterBoard board;
    private int activePlayerNum;
    private int turnNumber = 1;  // Advance when every player has a turn
    private StatusScreen statusScreen;
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
    private JFrame masterFrame;

    // For debugging, or if the game crashes after movement
    // has been rolled, we can force the next movement roll
    // from the command line.
    private int forcedMovementRoll;

    // Constants for savegames
    public static final String saveDirname = "saves";
    public static final String saveExtension = ".sav";
    public static final String xmlSaveExtension = ".xml";

    // Per-player client options
    private static boolean autoPickRecruiter;
    private static boolean showDice = true;
    private static boolean showGameStatus = true;

    // Server options
    private static boolean autosaveEveryTurn = true;
    private static boolean allVisible;


    public Game(boolean isApplet, GameApplet applet)
    {
        this.isApplet = isApplet;
        Chit.setApplet(isApplet);
        if (applet != null)
        {
            this.applet = applet;
        }

        JFrame frame = new JFrame();

        new GetPlayers(frame, this);
        // GetPlayers will fill in the player objects

        // Since the inputs are validated, it's time to roll for towers.
        assignTowers();

        // Renumber players in descending tower order.
        Collections.sort(players);

        ListIterator lit = players.listIterator(players.size());
        while (lit.hasPrevious())
        {
            Player player = (Player)lit.previous();
            new PickColor(frame, this, player);
            player.initMarkersAvailable();
        }

        if (!disposed)
        {
            statusScreen = new StatusScreen(this);
            board = new MasterBoard(this);
            masterFrame = board.getFrame();
            Iterator it = players.iterator();
            while (it.hasNext())
            {
                Player player = (Player)it.next();
                pickInitialMarker(player);
                placeInitialLegion(player);
                updateStatusScreen();
            }
            board.loadInitialMarkerImages();
            board.setupPhase();

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
        masterFrame = board.getFrame();
        loadGame(filename);
        board.loadInitialMarkerImages();
        statusScreen = new StatusScreen(this);
        // XXX Assumes that we only load at the beginning of a phase.
        board.setupPhase();
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
        int numPlayers = getNumPlayers();
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
                if (playerTower[i] == UNASSIGNED)
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
            Player player = getPlayer(i);
            logEvent(player.getName() + " gets tower " + playerTower[i]);
            player.setTower(playerTower[i]);
        }
    }


    public boolean getAllVisible()
    {
        return allVisible;
    }

    public int getNumPlayers()
    {
        return players.size();
    }


    public void addPlayer(String name)
    {
        players.add(new Player(name, this));
    }


    public void addPlayer(Player player)
    {
        players.add(player);
    }


    private int getNumLivingPlayers()
    {
        int count = 0;
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            if (!player.isDead())
            {
                count++;
            }
        }
        return count;
    }


    public Player getActivePlayer()
    {
        return (Player)players.get(activePlayerNum);
    }


    public int getActivePlayerNum()
    {
        return activePlayerNum;
    }


    public Player getPlayer(int i)
    {
        return (Player)players.get(i);
    }


    public Collection getPlayers()
    {
        return players;
    }


    public void checkForVictory()
    {
        int remaining = 0;
        // Assign something to winner to avoid uninitialized var error.
        Player winner = getPlayer(0);

        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            if (!player.isDead())
            {
                remaining++;
                winner = player;
            }
        }

        switch (remaining)
        {
            case 0:
                logEvent("Draw");
                JOptionPane.showMessageDialog(board, "Draw");
                dispose();
                break;

            case 1:
                logEvent(winner.getName() + " wins");
                JOptionPane.showMessageDialog(board,
                    winner.getName() + " wins");
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
        if (activePlayerNum == getNumPlayers())
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


    /** Create a text file describing this game's state, in
     *  file saves/<time>.sav
     *  Format:
     *     Number of players
     *     Turn number
     *     Whose turn
     *     Current phase
     *     Creature counts
     *     Player 1:
     *         Name
     *         Color
     *         Starting tower
     *         Score
     *         Alive?
     *         Mulligans left
     *         Players eliminated
     *         Number of markers left
     *         Remaining marker ids
     *         Number of Legions
     *         Legion 1:
     *             Marker id
     *             Hex label
     *             Height
     *             Creature 1:
     *                 Creature type
     *                 Visible?
     *             ...
     *         ...
     *     ...
     */
    private void saveGame()
    {
        // XXX Need dialog to pick filename.
        Date date = new Date();
        File savesDir = new File(saveDirname);
        if (!savesDir.exists() || !savesDir.isDirectory())
        {
             if (!savesDir.mkdir())
             {
                 System.out.println("Could not create saves directory");
                 return;
             }
        }

        String filename = saveDirname + File.separator +
            date.getTime() + saveExtension;
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

        java.util.List creatures = Creature.getCreatures();
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            out.println(creature.getCount());
        }

        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
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
            Iterator it2 = markerIds.iterator();
            while (it2.hasNext())
            {
                String markerId = (String)it2.next();
                out.println(markerId);
            }

            out.println(player.getNumLegions());

            Collection legions = player.getLegions();
            it2 = legions.iterator();
            while (it2.hasNext())
            {
                Legion legion = (Legion)it2.next();
                out.println(legion.getMarkerId());
                out.println(legion.getCurrentHex().getLabel());

                out.println(legion.getHeight());

                Collection critters = legion.getCritters();
                Iterator it3 = critters.iterator();
                while (it3.hasNext())
                {
                    Critter critter = (Critter)it3.next();
                    out.println(critter.getName());
                    out.println(critter.isVisible());
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

    /*
     *     Game name
     *     Moderator name
     *     Date/time started
     *     Date/time of last move
     *
     *
     *     Number of players
     *     Turn number
     *     Whose turn
     *     Current phase
     *     Creature counts
     *     Player 1:
     *         Name
     *         Color
     *         Starting tower
     *         Score
     *         Alive?
     *         Mulligans left
     *         Players eliminated
     *         Number of markers left
     *         Remaining marker ids
     *         Number of Legions
     *         Legion 1:
     *             Marker id
     *             Hex label
     *             Height
     *             Creature 1:
     *                 Creature type
     *                 Visible?
     *             ...
     *         ...
     *     ...
     *
     *     Battle
               Attacker
               Defender

     */

    /** Save the game to an xml file, saves/<time>.xml
     *  This is meant to convey a current game state completely.
     *  It might also be nice to show recent history.  (Full game
     *  history would go in a bigger file.)
     *  Format: (work in progress)

        <?xml version="1.0" standalone="yes"?>
        <titan_game_state>
            <gamename>string</gamename>
            <moderator>string</moderator>
            <started>datetime</started>
            <last>datetime</last>

            <turn>number</turn>
            <active>player</active>
            <phase>phase</phase>

            <lastevent>string</lastevent>

            <player>
                <playername>string</playername>
                <color>color</color>
                <tower>number</tower>
                <score>number</score>
                <mulligans>number</mulligans>
                <eliminated>player</eliminated>
                <movementroll>number</movementroll>

                <legion>
                    <marker>marker id</marker>
                    <hex>hex id</hex>
                    <split>boolean</split>
                    <parent>legion</parent>
                    <moved>boolean</moved>
                    <entryside>hexside</entryside>
                    <teleported>boolean</teleported>
                    <oldhex>hex id</old hex>
                    <fought>boolean</fought>
                    <summoned>boolean</summoned>
                    <opponent>legion</opponent>
                    <mustered>boolean</mustered>
                    <recruit>creature</recruit>
                    <recruiter>creature</recruiter>
                    ...
                    <creature>creature</creature>
                        <type>creature type</type>
                        <battlehex>battle hex id</battlehex>
                        <hits>number</hits>
                        <battlemoved>boolean</battlemoved>
                        <struck>boolean</struck>
                        <strikerolls>numbers</strikerolls>
                        <targethex>battle hex id</targethex>
                        <targetnum>number</targetnum>
                        <carries>number</carries>
                        <carrytarget>battle hex id</carrytarget>
                        ...
                    ...
                </legion>
            <player>

            <titan_battle_state>
                <battleturn>number</battleturn>
                <battleactive>player</battleactive>
                <battlephase>phase</battlephase>
                <attacker>legion</attacker>
                <defender>legion</defender>
                <summonstate>angel summoning state</summonstate>
            </titan_battle_state>
        </titan_game_state>
     */
    private void saveGameXML()
    {
        // XXX Need dialog to pick filename.
        Date date = new Date();
        File savesDir = new File(saveDirname);
        if (!savesDir.exists() || !savesDir.isDirectory())
        {
             if (!savesDir.mkdir())
             {
                 System.out.println("Could not create saves directory");
                 return;
             }
        }

        String filename = saveDirname + File.separator +
            date.getTime() + xmlSaveExtension;
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

        java.util.List creatures = Creature.getCreatures();
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            out.println(creature.getCount());
        }

        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
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
            Iterator it2 = markerIds.iterator();
            while (it2.hasNext())
            {
                String markerId = (String)it2.next();
                out.println(markerId);
            }

            out.println(player.getNumLegions());

            Collection legions = player.getLegions();
            it2 = legions.iterator();
            while (it2.hasNext())
            {
                Legion legion = (Legion)it2.next();
                out.println(legion.getMarkerId());
                out.println(legion.getCurrentHex().getLabel());

                out.println(legion.getHeight());

                Collection critters = legion.getCritters();
                Iterator it3 = critters.iterator();
                while (it3.hasNext())
                {
                    Critter critter = (Critter)it3.next();
                    out.println(critter.getName());
                    out.println(critter.isVisible());
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
            if (name.endsWith(saveExtension))
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
            File dir = new File(saveDirname);
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
            file = new File(saveDirname + File.separator +
                latestSaveFilename(filenames));
        }
        else
        {
            file = new File(filename);
            if (!file.exists())
            {
                file = new File(saveDirname + File.separator + filename);
            }
        }

        try
        {
            FileReader fileReader = new FileReader(file);
            BufferedReader in = new BufferedReader(fileReader);
            String buf;

            buf = in.readLine();
            int numPlayers = Integer.parseInt(buf);

            buf = in.readLine();
            turnNumber = Integer.parseInt(buf);

            buf = in.readLine();
            activePlayerNum = Integer.parseInt(buf);

            buf = in.readLine();
            phase = Integer.parseInt(buf);

            java.util.List creatures = Creature.getCreatures();
            Iterator it = creatures.iterator();
            while (it.hasNext())
            {
                Creature creature = (Creature)it.next();
                buf = in.readLine();
                int count = Integer.parseInt(buf);
                creature.setCount(count);
            }

            for (int i = 0; i < numPlayers; i++)
            {
                String name = in.readLine();
                Player player = new Player(name, this);
                players.add(player);

                String color = in.readLine();
                player.setColor(color);

                buf = in.readLine();
                int tower = Integer.parseInt(buf);
                player.setTower(tower);

                buf = in.readLine();
                int score = Integer.parseInt(buf);
                player.setScore(score);

                buf = in.readLine();

                // Output whether the player is alive.
                player.setDead(!Boolean.valueOf(buf).booleanValue());

                buf = in.readLine();
                int mulligansLeft = Integer.parseInt(buf);
                player.setMulligansLeft(mulligansLeft);

                String playersElim = in.readLine();
                if (playersElim.equals("null"))
                {
                    playersElim = new String("");
                }
                player.setPlayersElim(playersElim);

                buf = in.readLine();
                int numMarkersAvailable = Integer.parseInt(buf);

                for (int j = 0; j < numMarkersAvailable; j++)
                {
                    String markerId = in.readLine();
                    player.addLegionMarker(markerId);
                }

                buf = in.readLine();
                int numLegions = Integer.parseInt(buf);

                for (int j = 0; j < numLegions; j++)
                {
                    String markerId = in.readLine();

                    buf = in.readLine();
                    int hexLabel = Integer.parseInt(buf);

                    buf = in.readLine();
                    int height = Integer.parseInt(buf);

                    Creature [] critters = new Creature[8];
                    boolean [] visibles = new boolean[height];

                    for (int k = 0; k < height; k++)
                    {
                        buf = in.readLine();
                        critters[k] = Creature.getCreatureFromName(buf);
                        buf = in.readLine();
                        visibles[k] = Boolean.valueOf(buf).booleanValue();
                    }

                    Legion legion = new Legion(markerId, null,
                        MasterBoard.getHexFromLabel(hexLabel), critters[0],
                        critters[1], critters[2], critters[3], critters[4],
                        critters[5], critters[6], critters[7], player);

                    for (int k = 0; k < height; k++)
                    {
                        if (visibles[k])
                        {
                            legion.revealCreature(k);
                        }
                    }

                    player.addLegion(legion);
                    MasterHex hex = legion.getCurrentHex();
                    hex.addLegion(legion);
                }
            }

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


    /** Extract and return the numeric part of a filename. */
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
            else
            {
                break;
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


    /** Find the save filename with the highest numerical value.
        (1000000000.sav comes after 999999999.sav) */
    private String latestSaveFilename(String [] filenames)
    {
        class StringNumComparator implements Comparator
        {
            public int compare(Object o1, Object o2) throws ClassCastException
            {
                if (!(o1 instanceof String) || !(o2 instanceof String))
                {
                    throw new ClassCastException();
                }

                return (int)(numberValue((String)o1) -
                    numberValue((String)o2));
            }
        }

        return (String)Collections.max(Arrays.asList(filenames), new
            StringNumComparator());
    }


    /** Return a list of creatures that can be recruited in
     *  the given terrain, ordered from lowest to highest. */
    public static ArrayList getPossibleRecruits(char terrain)
    {
        ArrayList recruits = new ArrayList(5);

        switch (terrain)
        {
            case 'B':
                recruits.add(Creature.gargoyle);
                recruits.add(Creature.cyclops);
                recruits.add(Creature.gorgon);
                break;

            case 'D':
                recruits.add(Creature.lion);
                recruits.add(Creature.griffon);
                recruits.add(Creature.hydra);
                break;

            case 'H':
                recruits.add(Creature.ogre);
                recruits.add(Creature.minotaur);
                recruits.add(Creature.unicorn);
                break;

            case 'J':
                recruits.add(Creature.gargoyle);
                recruits.add(Creature.cyclops);
                recruits.add(Creature.behemoth);
                recruits.add(Creature.serpent);
                break;

            case 'm':
                recruits.add(Creature.lion);
                recruits.add(Creature.minotaur);
                recruits.add(Creature.dragon);
                recruits.add(Creature.colossus);
                break;

            case 'M':
                recruits.add(Creature.ogre);
                recruits.add(Creature.troll);
                recruits.add(Creature.ranger);
                break;

            case 'P':
                recruits.add(Creature.centaur);
                recruits.add(Creature.lion);
                recruits.add(Creature.ranger);
                break;

            case 'S':
                recruits.add(Creature.troll);
                recruits.add(Creature.wyvern);
                recruits.add(Creature.hydra);
                break;

            case 'T':
                recruits.add(Creature.centaur);
                recruits.add(Creature.gargoyle);
                recruits.add(Creature.ogre);
                recruits.add(Creature.guardian);
                recruits.add(Creature.warlock);
                break;

            case 't':
                recruits.add(Creature.troll);
                recruits.add(Creature.warbear);
                recruits.add(Creature.giant);
                recruits.add(Creature.colossus);
                break;

            case 'W':
                recruits.add(Creature.centaur);
                recruits.add(Creature.warbear);
                recruits.add(Creature.unicorn);
                break;
        }

        return recruits;
    }



    /** Return the number of the given recruiter needed to muster the given
      * recruit in the given terrain.  Return an impossible big number
      * if the recruiter can't muster that recruit in that terrain. */
    public static int numberOfRecruiterNeeded(Creature recruiter, Creature
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
        }

        return 99;
    }


    /** Return a list of eligible recruits, as Creatures. */
    public static ArrayList findEligibleRecruits(Legion legion)
    {
        ArrayList recruits;

        MasterHex hex = legion.getCurrentHex();
        char terrain = hex.getTerrain();

        // Towers are a special case.
        if (hex.getTerrain() == 'T')
        {
            recruits = new ArrayList();

            recruits.add(Creature.centaur);
            recruits.add(Creature.gargoyle);
            recruits.add(Creature.ogre);
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
                recruits.add(Creature.guardian);
            }
            if (legion.numCreature(Creature.titan) >= 1 ||
                legion.numCreature(Creature.warlock) >= 1)
            {
                recruits.add(Creature.warlock);
            }
        }
        else
        {
            recruits = getPossibleRecruits(hex.getTerrain());

            ListIterator lit = recruits.listIterator(recruits.size());
            while (lit.hasPrevious())
            {
                Creature creature = (Creature)lit.previous();
                int numCreature = legion.numCreature(creature);
                if (numCreature >= 1)
                {
                    // We already have one of this creature, so we
                    // can recruit it and all lesser creatures in
                    // this hex.
                    break;
                }
                else
                {
                    if (lit.hasPrevious())
                    {
                        Creature lesser = (Creature)lit.previous();
                        int numLesser = legion.numCreature(lesser);
                        if (numLesser >= numberOfRecruiterNeeded(lesser,
                            creature, terrain))
                        {
                            // We have enough of the previous creature
                            // to recruit this and all lesser creatures
                            // in this hex.
                            break;
                        }
                        else if (numLesser >= 1)
                        {
                            // We can't recruit this creature, but
                            // we have at least one of the previous
                            // creature, so we can recruit all lesser
                            // creatures in this hex.
                            lit.next();
                            lit.next();
                            lit.remove();
                            break;
                        }
                        else
                        {
                            // We can't recruit this creature.  Continue.
                            lit.next();
                            lit.next();
                            lit.remove();
                        }
                    }
                    else
                    {
                        // This is the lowest creature in this hex,
                        // so we can't recruit it with a lesser creature.
                        lit.remove();
                    }
                }
            }
        }

        // Make sure that the potential recruits are available.
        Iterator it = recruits.iterator();
        while (it.hasNext())
        {
            Creature recruit = (Creature)it.next();
            if (recruit.getCount() < 1)
            {
                it.remove();
            }
        }

        return recruits;
    }


    /** Return a list of eligible recruiters. Use Critters instead
     *  of Creatures so that Titan power is shown properly. */
    public static ArrayList findEligibleRecruiters(Legion legion,
        Creature recruit)
    {
        ArrayList recruiters = new ArrayList();

        MasterHex hex = legion.getCurrentHex();
        char terrain = hex.getTerrain();

        if (terrain == 'T')
        {
            // Towers are a special case.  The recruiter of tower creatures
            // remains anonymous, so we only deal with guardians and warlocks.
            if (recruit.getName().equals("Guardian"))
            {
                java.util.List creatures = Creature.getCreatures();
                Iterator it = creatures.iterator();
                while (it.hasNext())
                {
                    Creature creature = (Creature)it.next();
                    if (creature.getName().equals("Guardian") &&
                        legion.numCreature(creature) >= 1)
                    {
                        recruiters.add(legion.getCritter(creature));
                    }
                    else if (!creature.isImmortal() &&
                        legion.numCreature(creature) >= 3)
                    {
                        recruiters.add(legion.getCritter(creature));
                    }
                }
            }
            else if (recruit.getName().equals("Warlock"))
            {
                if (legion.numCreature(Creature.titan) >= 1)
                {
                    recruiters.add(legion.getCritter(Creature.titan));
                }
                if (legion.numCreature(Creature.warlock) >= 1)
                {
                    recruiters.add(legion.getCritter(Creature.warlock));
                }
            }
        }
        else
        {
            recruiters = getPossibleRecruits(terrain);
            Iterator it = recruiters.iterator();
            while (it.hasNext())
            {
                Creature possibleRecruiter = (Creature)it.next();
                int needed = numberOfRecruiterNeeded(possibleRecruiter,
                    recruit, terrain);
                if (needed < 1 || needed > legion.numCreature(
                    possibleRecruiter))
                {
                    // Zap this possible recruiter.
                    it.remove();
                }
            }
        }

        return recruiters;
    }


    /** Return true if all members of legion who are in recruiters are
     *  already visible. */
    public static boolean allRecruitersVisible(Legion legion,
        ArrayList recruiters)
    {
        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (!critter.isVisible())
            {
                Iterator it2 = recruiters.iterator();
                while (it2.hasNext())
                {
                    Creature recruiter = (Creature)it2.next();
                    if (recruiter.getName().equals(critter.getName()))
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }


    public static void doRecruit(Creature recruit, Legion legion,
        JFrame parentFrame)
    {
        // Pick the recruiter(s) if necessary.
        ArrayList recruiters = findEligibleRecruiters(legion, recruit);
        Creature recruiter;

        int numEligibleRecruiters = recruiters.size();
        if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiter = null;
        }
        else if (autoPickRecruiter || numEligibleRecruiters == 1 ||
            allRecruitersVisible(legion, recruiters))
        {
            // If there's only one possible recruiter, or if all
            // possible recruiters are already visible, or if
            // the user has chosen the autoPickRecruiter option,
            // then just reveal the first possible recruiter.
            recruiter = (Creature)recruiters.get(0);
        }
        else
        {
            new PickRecruiter(parentFrame, legion, recruiters);
            recruiter = (Creature)recruiters.get(0);
        }

        legion.addCreature(recruit, true);

        int numRecruiters = 0;
        if (recruiter != null)
        {
            // Mark the recruiter(s) as visible.
            numRecruiters = numberOfRecruiterNeeded(recruiter,
                recruit, legion.getCurrentHex().getTerrain());
            if (numRecruiters >= 1 && numRecruiters <= 3)
            {
                legion.revealCreatures(recruiter, numRecruiters);
            }
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


    // Return the number of types of angels that can be acquired.
    public static int findEligibleAngels(Legion legion, ArrayList recruits,
        boolean archangel)
    {
        if (legion.getHeight() >= 7)
        {
            return 0;
        }

        recruits.add(Creature.angel);
        if (archangel)
        {
            recruits.add(Creature.archangel);
        }

        // Check for availability of chits.
        Iterator it = recruits.iterator();
        while (it.hasNext())
        {
            Creature recruit = (Creature)it.next();
            if (recruit.getCount() < 1)
            {
                it.remove();
            }
        }

        return recruits.size();
    }


    public void dispose()
    {
        disposed = true;

        if (isApplet)
        {
            if (board != null)
            {
                masterFrame.dispose();
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


    /** Put all die rolling in one place, in case we decide to change random
     *  number algorithms, use an external dice server, etc. */
    public static int rollDie()
    {
        return random.nextInt(6) + 1;
    }


    public void pickInitialMarker(Player player)
    {
        do
        {
            new PickMarker(masterFrame, player);
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

        Legion legion = new Legion(player.getSelectedMarker(), null, hex,
            Creature.titan, Creature.angel, Creature.ogre, Creature.ogre,
            Creature.centaur, Creature.centaur, Creature.gargoyle,
            Creature.gargoyle, player);

        player.addLegion(legion);
        hex.addLegion(legion);
    }


    public void highlightUnmovedLegions()
    {
        board.unselectAllHexes();

        Player player = getActivePlayer();
        player.unselectLegion();

        HashSet set = new HashSet();

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
        HashSet set = new HashSet();

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (hex.getNumEnemyLegions(player) > 0)
        {
            if (hex.getNumFriendlyLegions(player) == 0)
            {
                set.add(hex.getLabel());
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

        HashSet set = new HashSet();

        if (!hex.isOccupied())
        {
            set.add(hex.getLabel());

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
        HashSet set = new HashSet();

        if (legion.hasMoved())
        {
            return set;
        }

        Player player = legion.getPlayer();

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
        String [] options = new String[2];
        options[0] = "Teleport";
        options[1] = "Move Normally";
        int answer = JOptionPane.showOptionDialog(board, "Teleport?",
            "Teleport?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

        // If Teleport, then leave teleported set.
        if (answer == JOptionPane.NO_OPTION)
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

        HashSet set = new HashSet();

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
        masterFrame.show();

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

        // Insert a blank line in the log file after each battle.
        logEvent("\n");
    }


    // Return number of legions with summonable angels.
    public int highlightSummonableAngels(Legion legion)
    {
        board.unselectAllHexes();

        Player player = legion.getPlayer();
        player.unselectLegion();

        int count = 0;

        HashSet set = new HashSet();

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


    // Return number of legions that can recruit.
    public int highlightPossibleRecruits()
    {
        int count = 0;
        Player player = getActivePlayer();

        HashSet set = new HashSet();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            if (legion.hasMoved() && legion.canRecruit())
            {
                ArrayList recruits = findEligibleRecruits(legion);
                if (!recruits.isEmpty())
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
                    JOptionPane.showMessageDialog(board,
                        "No markers are available.");
                    return;
                }
                // A legion must be at least 4 high to split.
                if (legion.getHeight() < 4)
                {
                    JOptionPane.showMessageDialog(board,
                        "Legion is too short to split.");
                    return;
                }
                // Don't allow extra splits in turn 1.
                if (getTurnNumber() == 1 && player.getNumLegions() > 1)
                {
                    JOptionPane.showMessageDialog(board,
                        "Cannot split twice on Turn 1.");
                    return;
                }

                if (!dialogLock)
                {
                    dialogLock = true;
                    new SplitLegion(masterFrame, legion, player);
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
                        new PickRecruit(masterFrame, legion);
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
                            new PickEntrySide(masterFrame, hex);
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
                                legion.revealTeleportingLord(masterFrame);
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
            summonAngel.updateChits();
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
                new Concede(masterFrame, defender, attacker, true);
            }

            if (hex.isEngagement())
            {
                // The attacker may concede now without
                // allowing the defender a reinforcement.
                new Concede(masterFrame, attacker, defender, false);

                // The players may agree to a negotiated
                // settlement.
                if (hex.isEngagement())
                {
                    new Negotiate(masterFrame, attacker, defender);
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
                            new PickRecruit(masterFrame, defender);
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
