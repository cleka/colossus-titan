import java.io.*;
import java.util.*;
import javax.swing.*;


/**
 * Class Game gets and holds high-level data about a Titan game.
 * @version $Id$
 * @author David Ripton
 * @author Bruce Sherrod <bruce@thematrix.com>
 */


public final class Game
{
    private ArrayList players = new ArrayList(6);
    public MasterBoard board;
    private int activePlayerNum;
    private int turnNumber = 1;  // Advance when every player has a turn
    private StatusScreen statusScreen;
    private GameApplet applet;
    private Battle battle;
    public static Random random = new Random();
    private MovementDie movementDie;
    private SummonAngel summonAngel;
    private AI ai = new SimpleAI();  // TODO move to player
    private Caretaker caretaker = new Caretaker();

    public static final int SPLIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int MUSTER = 4;
    private int phase = SPLIT;

    private boolean isApplet;
    public JFrame masterFrame;

    private boolean engagementInProgress;


    // Constants for savegames
    public static final String saveDirname = "saves";
    public static final String saveExtension = ".sav";
    public static final String saveGameVersion =
        "Colossus savegame version 5";

    public static final String configVersion =
        "Colossus config file version 2";

    /** Preference file */
    public static final String optionsPath = "Colossus";
    public static final String optionsSep = "-";
    public static final String optionsExtension = ".cfg";
    private Properties options = new Properties();


    // Option names

    // Server options
    public static final String autosave = "Autosave";
    public static final String allStacksVisible = "All stacks visible";
    
    // Debug options (server)
    public static final String chooseMovement = "Choose movement roll";
    public static final String chooseHits= "Choose number of hits";
    public static final String chooseTowers = "Choose towers";
    public static final String chooseCreatures = "Choose creatures";

    // Graphics options (client)
    public static final String showStatusScreen = "Show game status";
    public static final String showDice = "Show dice";
    public static final String antialias = "Antialias";

    // AI options (player)
    public static final String autoRecruit = "Auto recruit";
    public static final String autoPickRecruiter = "Auto pick recruiter";
    public static final String autoPickMarker = "Auto pick marker";
    public static final String autoPickEntrySide = "Auto pick entry side";
    public static final String autoForcedStrike = "Auto forced strike";
    public static final String autoSplit = "Auto split";
    public static final String autoMasterMove = "Auto masterboard move";
    public static final String autoPlay = "Auto play";

    private static HashSet perPlayerOptions = new HashSet();


    static
    {
        perPlayerOptions.add(autoRecruit);
        perPlayerOptions.add(autoPickRecruiter);
        perPlayerOptions.add(autoPickMarker);
        perPlayerOptions.add(autoPickEntrySide);
        perPlayerOptions.add(autoForcedStrike);
        perPlayerOptions.add(autoSplit);
        perPlayerOptions.add(autoMasterMove);
        perPlayerOptions.add(autoPlay);
    }


    /** Start a new game. */
    public Game(GameApplet applet)
    {
        this.applet = applet;
        isApplet = (applet != null);
        Chit.setApplet(isApplet);

        newGame();
    }


    /** Load a saved game. */
    public Game(GameApplet applet, String filename)
    {
        this.applet = applet;
        isApplet = (applet != null);
        Chit.setApplet(isApplet);

        board = new MasterBoard(this);
        masterFrame = board.getFrame();
        loadGame(filename);

        loadOptions();
        updateStatusScreen();
    }


    /** Default constructor, for testing only. */
    public Game()
    {
    }


    public void newGame()
    {
        logEvent("\nStarting new game");

        turnNumber = 1;

        caretaker.resetAllCounts();
        if (board != null)
        {
            board.clearLegions();
        }
        players.clear();
        statusScreen = null;

        JFrame frame = new JFrame();

        TreeSet playerNames = GetPlayers.getPlayers(frame);
        if (playerNames.isEmpty())
        {
            dispose();
            return;
        }
        Iterator it = playerNames.iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            addPlayer(name);
            logEvent("Add player " + name);
        }

        if (board == null)
        {
            board = new MasterBoard(this);
        }
        else
        {
            board.setGame(this);
        }
        masterFrame = board.getFrame();

        assignTowers();

        // Renumber players in descending tower order.
        Collections.sort(players);
        activePlayerNum = 0;

        ListIterator lit = players.listIterator(players.size());
        while (lit.hasPrevious())
        {
            Player player = (Player)lit.previous();
            String color;
            do
            {
                color = PickColor.pickColor(frame, this, player);
            }
            while (color == null);
            logEvent(player.getName() + " chooses color " + color);
            player.setColor(color);
            player.initMarkersAvailable();
        }

        loadOptions();

        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            placeInitialLegion(player);
            updateStatusScreen();
        }
        board.loadInitialMarkerImages();
        setupPhase();

        board.setVisible(true);
        board.repaint();

        updateStatusScreen();

        if (getOption(showDice))
        {
            initMovementDie();
        }
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


    /** Randomize towers by rolling dice and rerolling ties. */
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
                    if (getOption(chooseTowers))
                    {
                        Player player = getPlayer(i);
                        rolls[i] = PickRoll.pickRoll(masterFrame,
                            "Choose tower roll for " + player.getName());
                    }
                    else
                    {
                        rolls[i] = rollDie();
                    }
                }
            }

            outer:
            for (int i = 0; i < numPlayers; i++)
            {
                if (playerTower[i] == UNASSIGNED)
                {
                    for (int j = 0; j < numPlayers; j++)
                    {
                        if (i != j && rolls[i] == rolls[j])
                        {
                            continue outer;
                        }
                    }
                    playerTower[i] = rolls[i];
                    playersLeft--;
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

    public Caretaker getCaretaker()
    {
	return caretaker;
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


    public int getNumLivingPlayers()
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


    private void setupDice(boolean enable)
    {
        if (enable)
        {
            initMovementDie();
            if (battle != null)
            {
                battle.initBattleDice();
            }
        }
        else
        {
            disposeMovementDie();
            if (battle != null)
            {
                battle.disposeBattleDice();
            }
            if (board != null)
            {
                board.twiddleOption(showDice, false);
            }
        }
    }


    public void repaintAllWindows()
    {
        if (statusScreen != null)
        {
            statusScreen.repaint();
        }
        if (movementDie != null)
        {
            movementDie.repaint();
        }
        if (board != null)
        {
            masterFrame.repaint();
        }
        if (battle != null)
        {
            BattleDice dice = battle.getBattleDice();
            if (dice != null)
            {
                dice.repaint();
            }
            BattleMap map = battle.getBattleMap();
            if (map != null)
            {
                map.repaint();
            }
        }
    }


    private void initMovementDie()
    {
        movementDie = new MovementDie(this);
    }


    private void disposeMovementDie()
    {
        if (movementDie != null)
        {
            movementDie.dispose();
            movementDie = null;
        }
    }


    public void showMovementRoll(int roll)
    {
        if (movementDie != null)
        {
            movementDie.showRoll(roll);
        }
    }


    public static HashSet getPerPlayerOptions()
    {
        return perPlayerOptions;
    }


    /** Return the value of the boolean option given by name. Default
     *  to false if there is no such option. */
    public boolean getOption(String name)
    {
        if (perPlayerOptions.contains(name))
        {
            try
            {
                return getActivePlayer().getOption(name);
            }
            catch (Exception ex)
            {
            }
        }

        String value = options.getProperty(name);
        if (value != null && value.equals("true"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    /** Set option name to (a string version of) the given boolean value. */
    public void setOption(String name, boolean value)
    {
        if (perPlayerOptions.contains(name))
        {
            getActivePlayer().setOption(name, value);
            return;
        }

        options.setProperty(name, String.valueOf(value));

        // Side effects
        if (name.equals(showStatusScreen))
        {
            updateStatusScreen();
        }
        else if (name.equals(antialias))
        {
            Hex.setAntialias(value);
            repaintAllWindows();
        }
        else if (name.equals(showDice))
        {
            setupDice(value);
        }
    }


    public JFrame getMasterFrame()
    {
        return masterFrame;
    }


    public void checkForVictory()
    {
        int remaining = 0;
        Player winner = null;

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

        setupPhase();
    }


    public void setupPhase()
    {

        switch (getPhase())
        {
            case Game.SPLIT:
                setupSplit();
                break;
            case Game.MOVE:
                setupMove();
                break;
            case Game.FIGHT:
                setupFight();
                break;
            case Game.MUSTER:
                setupMuster();
                break;
            default:
                System.out.println("Bogus phase");
        }
    }


    private void setupSplit()
    {
        Player player = getActivePlayer();
        player.resetTurnState();

        // If there are no markers available, skip forward to movement.
        if (player.getNumMarkersAvailable() == 0)
        {
            advancePhase();
        }
        else
        {
            board.setupSplitMenu();

            // Highlight hexes with legions that are 7 high.
            player.highlightTallLegions();
        }

	if (player.getOption(autoSplit))
	{
	    ai.split(this);
	}
    }


    private void setupMove()
    {
        Player player = getActivePlayer();
        player.clearUndoStack();
        player.rollMovement();
        board.setupMoveMenu();

        // Highlight hexes with legions that can move.
        highlightUnmovedLegions();

        if (player.getOption(autoMasterMove))
        {
            ai.move(this);
        }
    }

    private void setupFight()
    {
        // Highlight hexes with engagements.
        // If there are no engagements, move forward to the muster phase.
        if (highlightEngagements() == 0)
        {
            advancePhase();
        }
        else
        {
            board.setupFightMenu();
        }
    } 

    private void setupMuster()
    {
        Player player = getActivePlayer();
        if (player.isDead())
        {
            advancePhase();
        }
        else
        {
            player.clearUndoStack();
            board.setupMusterMenu();

            // Highlight hexes with legions eligible to muster.
            highlightPossibleRecruits();

            if (player.getOption(autoRecruit))
            {
                ai.muster(this);
            }
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
            Player player = getActivePlayer();
            logEvent("\n" + player.getName() + "'s turn, number " + 
                turnNumber);

            player.syncCheckboxes();
            updateStatusScreen();
            if (!isApplet && getOption(autosave))
            {
                saveGame();
            }
        }
    }


    public void updateStatusScreen()
    {
        if (getOption(showStatusScreen))
        {
            if (statusScreen != null)
            {
                statusScreen.updateStatusScreen();
            }
            else
            {
                statusScreen = new StatusScreen(this);
            }
        }
        else
        {
            board.twiddleOption(showStatusScreen, false);
            if (statusScreen != null)
            {
                statusScreen.dispose();
            }
            this.statusScreen = null;
        }
    }


    public int getTurnNumber()
    {
        return turnNumber;
    }


    /** Create a text file describing this game's state, in
     *  file filename.
     *  Format:
     *     Savegame version string
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
     *         Movement roll
     *         Teleported?
     *         Summoned?
     *         Number of Legions
     *
     *         Legion 1:
     *             Marker id
     *             Current hex label
     *             Starting hex label
     *             Moved?
     *             Entry side
     *             Parent
     *             Recruited?
     *             Battle tally
     *             Height
     *             Creature 1:
     *                 Creature type
     *                 Visible?
     *             ...
     *         ...
     *     ...

     *     Engagement hex
     *     Battle turn number
     *     Whose battle turn
     *     Battle phase
     *     Summon state
     *     Carry damage
     *     Drift damage applied?

     *     Attacking Legion:
     *         Marker id
     *         Current hex label
     *         Starting hex label
     *         Moved?
     *         Entry side
     *         Parent
     *         Recruited?
     *         Battle tally
     *         Height
     *         Creature 1:
     *             Creature type
     *             Hits
     *             Current hex
     *             Starting hex
     *             Struck?
     *             Carry flag
     *         ...
     *     Defending Legion:
     *         ...
     */
    public void saveGame(String filename)
    {
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

        out.println(saveGameVersion);
        out.println(getNumPlayers());
        out.println(getTurnNumber());
        out.println(getActivePlayerNum());
        out.println(getPhase());

        java.util.List creatures = Creature.getCreatures();
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            out.println(caretaker.getCount(creature));
        }

        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            out.println(player.getName());
            out.println(player.getColor());
            out.println(player.getTower());
            out.println(player.getScore());
            out.println(player.isDead());
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

            out.println(player.getMovementRoll());
            out.println(player.hasTeleported());
            out.println(player.hasSummoned());
            out.println(player.getNumLegions());

            Collection legions = player.getLegions();
            it2 = legions.iterator();
            while (it2.hasNext())
            {
                Legion legion = (Legion)it2.next();
                dumpLegion(out, legion, false);
            }
        }

        // Battle stuff
        if (engagementInProgress)
        {
            out.println(battle.getMasterHex().getLabel());
            out.println(battle.getTurnNumber());
            out.println(battle.getActivePlayer().getName());
            out.println(battle.getPhase());
            out.println(battle.getSummonState());
            out.println(battle.getCarryDamage());
            out.println(battle.isDriftDamageApplied());

            dumpLegion(out, battle.getAttacker(), true);
            dumpLegion(out, battle.getDefender(), true);
        }

        if (out.checkError())
        {
            System.out.println("Write error " + filename);
            // XXX Delete the partial file?
            return;
        }
    }


    private void dumpLegion(PrintWriter out, Legion legion, boolean inBattle)
    {
        out.println(legion.getMarkerId());
        out.println(legion.getCurrentHex().getLabel());
        out.println(legion.getStartingHex().getLabel());
        out.println(legion.hasMoved());
        out.println(legion.getEntrySide());
        out.println(legion.getParentId());
        out.println(legion.hasRecruited());
        out.println(legion.getBattleTally());
        out.println(legion.getHeight());
        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            out.println(critter.getName());
            out.println(critter.isVisible());
            if (inBattle)
            {
                out.println(critter.getHits());
                out.println(critter.getCurrentHex().getLabel());
                out.println(critter.getStartingHex().getLabel());
                out.println(critter.hasStruck());
                out.println(critter.getCarryFlag());
            }
        }
    }


    /** Create a text file describing this game's state, in
     *  file saves/<time>.sav */
    public void saveGame()
    {
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

        saveGame(filename);
    }


    /** Try to load a game from ./filename first, then from
     *  saveDirName/filename.  If the filename is "--latest" then load
     *  the latest savegame found in saveDirName.  board must be non-null.
     */
    public void loadGame(String filename)
    {
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
            if (!buf.equals(saveGameVersion))
            {
                System.out.println("Can't load this savegame version.");
                dispose();
            }

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
                caretaker.setCount(creature,count);
            }

            players.clear();
            board.clearLegions();
            if (battle != null)
            {
                battle.disposeBattleDice();
                BattleMap map = battle.getBattleMap();
                if (map != null)
                {
                    map.dispose();
                }
            }

            // Players
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
                player.setDead(Boolean.valueOf(buf).booleanValue());

                buf = in.readLine();
                int mulligansLeft = Integer.parseInt(buf);
                player.setMulligansLeft(mulligansLeft);

                String playersElim = in.readLine();
                if (playersElim.equals("null"))
                {
                    playersElim = "";
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
                player.setMovementRoll(Integer.parseInt(buf));

                buf = in.readLine();
                player.setTeleported(Boolean.valueOf(buf).booleanValue());

                buf = in.readLine();
                player.setSummoned(Boolean.valueOf(buf).booleanValue());

                buf = in.readLine();
                int numLegions = Integer.parseInt(buf);

                // Legions
                for (int j = 0; j < numLegions; j++)
                {
                    Legion legion = readLegion(in, player, false);
                }
            }


            // Setup MasterBoard
            board.loadInitialMarkerImages();
            setupPhase();
            board.setVisible(true);
            board.repaint();


            // Battle stuff
            MasterHex engagementHex = null;
            buf = in.readLine();
            if (buf != null && buf.length() > 0)
            {
                engagementHex = board.getHexFromLabel(buf);

                buf = in.readLine();
                int battleTurnNum = Integer.parseInt(buf);

                buf = in.readLine();
                String battleActivePlayerName = buf;

                buf = in.readLine();
                int battlePhase = Integer.parseInt(buf);

                buf = in.readLine();
                int summonState = Integer.parseInt(buf);

                buf = in.readLine();
                int carryDamage = Integer.parseInt(buf);

                buf = in.readLine();
                boolean driftDamageApplied =
                    Boolean.valueOf(buf).booleanValue();

                Player attackingPlayer = getActivePlayer();
                Legion attacker = readLegion(in, attackingPlayer, true);

                Player defendingPlayer = engagementHex.getEnemyLegion(
                    attackingPlayer).getPlayer();
                Legion defender = readLegion(in, defendingPlayer, true);

                Legion activeLegion = defender;
                if (battleActivePlayerName.equals(attackingPlayer.getName()))
                {
                    activeLegion = attacker;
                }

                battle = new Battle(board, attacker, defender, activeLegion,
                    engagementHex, true, battleTurnNum, battlePhase);

                battle.setSummonState(summonState);
                battle.setCarryDamage(carryDamage);
                battle.setDriftDamageApplied(driftDamageApplied);
            }

            loadOptions();

            if (getOption(showDice))
            {
                initMovementDie();
                Player player = getActivePlayer();
                int roll = player.getMovementRoll();
                if (roll != 0)
                {
                    showMovementRoll(roll);
                }
            }
        }
        // FileNotFoundException, IOException, NumberFormatException
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Tried to load corrupt savegame.");
            dispose();
        }
    }


    public Legion readLegion(BufferedReader in, Player player,
        boolean inBattle) throws IOException
    {
        String markerId = in.readLine();
        String currentHexLabel = in.readLine();
        String startingHexLabel = in.readLine();

        String buf = in.readLine();
        boolean moved = Boolean.valueOf(buf).booleanValue();

        buf = in.readLine();
        int entrySide = Integer.parseInt(buf);

        String parentId = in.readLine();

        buf = in.readLine();
        boolean recruited = Boolean.valueOf(buf).booleanValue();

        buf = in.readLine();
        int battleTally = Integer.parseInt(buf);

        buf = in.readLine();
        int height = Integer.parseInt(buf);

        // Critters
        Critter [] critters = new Critter[8];
        for (int k = 0; k < height; k++)
        {
            buf = in.readLine();
            Critter critter = new Critter(
                Creature.getCreatureFromName(buf), false, null);

            buf = in.readLine();
            boolean visible = Boolean.valueOf(buf).booleanValue();
            critter.setVisible(visible);

            // Battle stuff
            if (inBattle)
            {
                buf = in.readLine();
                int hits = Integer.parseInt(buf);
                critter.setHits(hits);

                buf = in.readLine();
                critter.setCurrentHexLabel(buf);
                buf = in.readLine();
                critter.setStartingHexLabel(buf);

                buf = in.readLine();
                boolean struck = Boolean.valueOf(buf).booleanValue();
                critter.setStruck(struck);

                buf = in.readLine();
                boolean carry = Boolean.valueOf(buf).booleanValue();
                critter.setCarryFlag(carry);
            }

            critters[k] = critter;
        }

        // If this legion already exists, modify it in place.
        Legion legion = player.getLegionByMarkerId(markerId);
        if (legion != null)
        {
            for (int k = 0; k < height; k++)
            {
                legion.setCritter(k, critters[k]);
            }
        }
        else
        {
            legion = new Legion(markerId, parentId,
                MasterBoard.getHexFromLabel(currentHexLabel),
                MasterBoard.getHexFromLabel(startingHexLabel),
                critters[0], critters[1], critters[2], critters[3],
                critters[4], critters[5], critters[6], critters[7], player);
            player.addLegion(legion);
            MasterHex hex = legion.getCurrentHex();
            hex.addLegion(legion, false);
        }

        legion.setMoved(moved);
        legion.setRecruited(recruited);
        legion.setEntrySide(entrySide);
        legion.addToBattleTally(battleTally);

        return legion;
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
            return Long.parseLong(numberPart.toString());
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
        final class StringNumComparator implements Comparator
        {
            public int compare(Object o1, Object o2)
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


    // XXX Use separate files and methods for client and server options.

    /** Save game options to a file.  The current format is standard
     *  java.util.Properties keyword=value. */
    public void saveOptions()
    {
	final String optionsFile = optionsPath + optionsExtension;
        try
        {
            FileOutputStream out = new FileOutputStream(optionsFile);
            options.store(out, configVersion);
            out.close();
        }
        catch (IOException e)
        {
            System.out.println("Couldn't write options to " + optionsFile);
        }

        // Save options for each player
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            player.saveOptions();
        }
    }


    /** Load game options from a file. The current format is standard
     *  java.util.Properties keyword=value */
    public void loadOptions()
    {
	final String optionsFile = optionsPath + optionsExtension;
        try
        {
            FileInputStream in = new FileInputStream(optionsFile);
            options.load(in);
        }
        catch (IOException e)
        {
            System.out.println("Couldn't read game options from " + 
                optionsFile);
            return;
        }
        
        // Ensure that menu checkboxes reflect the correct state.
        Enumeration en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            boolean value = getOption(name);
            board.twiddleOption(name, value);
        }
        
        // Load options for each player
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            player.loadOptions();
        }
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
      * recruit in the given terrain.  Return an impossibly big number
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
    public ArrayList findEligibleRecruits(Legion legion)
    {
        return findEligibleRecruits(legion, legion.getCurrentHex());
    }


    /** Return a list of eligible recruits, as Creatures. */
    public ArrayList findEligibleRecruits(Legion legion, MasterHex hex)
    {
        ArrayList recruits;

        char terrain = hex.getTerrain();

        // Towers are a special case.
        if (hex.getTerrain() == 'T')
        {
            recruits = new ArrayList(5);

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
            if (caretaker.getCount(recruit) < 1)
            {
                it.remove();
            }
        }

        return recruits;
    }


    /** Return a list of eligible recruiters. Use Critters instead
     *  of Creatures so that Titan power is shown properly. */
    public ArrayList findEligibleRecruiters(Legion legion, Creature recruit)
    {
        ArrayList recruiters = new ArrayList(4);

        MasterHex hex = legion.getCurrentHex();
        char terrain = hex.getTerrain();

        if (terrain == 'T')
        {
            // Towers are a special case.  The recruiter of tower creatures
            // remains anonymous, so we only deal with guardians and warlocks.
            if (recruit.getName().equals("Warlock"))
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
            else if (recruit.getName().equals("Guardian"))
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
    public boolean allRecruitersVisible(Legion legion,
        ArrayList recruiters)
    {
        if (getOption(allStacksVisible))
        {
            return true;
        }

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


    public void doRecruit(Creature recruit, Legion legion, JFrame parentFrame)
    {
        // Pick the recruiter(s) if necessary.
        ArrayList recruiters = findEligibleRecruiters(legion, recruit);
        Creature recruiter;

        Player player = legion.getPlayer();

        int numEligibleRecruiters = recruiters.size();
        if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiter = null;
        }
        else if (player.getOption(autoPickRecruiter) || 
            numEligibleRecruiters == 1 ||
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
            recruiter = PickRecruiter.pickRecruiter(parentFrame, legion,
                recruiters);
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

        logEvent("Legion " + legion.getLongMarkerName() + " in " +
            legion.getCurrentHex().getDescription() +
            " recruits " + recruit.getName() + " with " +
            (numRecruiters == 0 ? "nothing" :
            numRecruiters + " " + (numRecruiters > 1 ?
            recruiter.getPluralName() : recruiter.getName())));

        // Recruits are one to a customer.
        legion.setRecruited(true);

        legion.getPlayer().setLastLegionRecruited(legion);
    }

    /** Return a list of names of angel types that can be acquired. */
    public ArrayList findEligibleAngels(Legion legion, boolean archangel)
    {
        if (legion.getHeight() >= 7)
        {
            return null;
        }

        ArrayList recruits = new ArrayList(2);

        if (caretaker.getCount(Creature.angel) >= 1)
        {
            recruits.add(Creature.angel.toString());
        }
        if (archangel && caretaker.getCount(Creature.archangel) >= 1)
        {
            recruits.add(Creature.archangel.toString());
        }

        return recruits;
    }


    public void dispose()
    {
        if (isApplet)
        {
            if (board != null)
            {
                masterFrame.dispose();
            }
            if (battle != null)
            {
                battle.disposeBattleDice();
                BattleMap map = battle.getBattleMap();
                if (map != null)
                {
                    map.dispose();
                }
            }
            if (statusScreen != null)
            {
                statusScreen.dispose();
            }
            if (movementDie != null)
            {
                movementDie.dispose();
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


    private void placeInitialLegion(Player player)
    {
        String name = player.getName();
        String selectedMarkerId;
        if (player.getOption(autoPickMarker))
        {
            selectedMarkerId = player.getFirstAvailableMarker();
        }
        else
        {
            do
            {
                selectedMarkerId = PickMarker.pickMarker(masterFrame,
                    name, player.getMarkersAvailable());
            }
            while (selectedMarkerId == null);
        }

        player.selectMarkerId(selectedMarkerId);
        logEvent(name + " selects initial marker");

        // Lookup coords for chit starting from player[i].getTower()
        MasterHex hex = MasterBoard.getHexFromLabel(String.valueOf(
            100 * player.getTower()));

        caretaker.takeOne(Creature.titan);
        caretaker.takeOne(Creature.angel);
        caretaker.takeOne(Creature.ogre);
        caretaker.takeOne(Creature.ogre);
        caretaker.takeOne(Creature.centaur);
        caretaker.takeOne(Creature.centaur);
        caretaker.takeOne(Creature.gargoyle);
        caretaker.takeOne(Creature.gargoyle);

        Legion legion = new Legion(selectedMarkerId, null, hex, hex,
            Creature.titan, Creature.angel, Creature.ogre, Creature.ogre,
            Creature.centaur, Creature.centaur, Creature.gargoyle,
            Creature.gargoyle, player);

        player.addLegion(legion);
        hex.addLegion(legion, false);
    }


    public void highlightUnmovedLegions()
    {
        board.unselectAllHexes();

        Player player = getActivePlayer();
        player.setDonor(null);
        List legions = player.getLegions();
        HashSet set = new HashSet();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (!legion.hasMoved())
            {
                set.add(legion.getCurrentHex().getLabel());
            }
        }

        board.selectHexesByLabels(set);
        board.repaint();
    }


    public static final int ARCHES_AND_ARROWS = -1;
    public static final int ARROWS_ONLY = -2; 
    public static final int NOWHERE = -1;


    /** Recursively find conventional moves from this hex.  Select
     *  all legal final destinations.  If block >= 0, go only
     *  that way.  If block == -1, use arches and arrows.  If
     *  block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.
     */
    public Set findNormalMoves(MasterHex hex, Player player, Legion legion,
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
            // for chooseWhetherToTeleport()
            hex.setEntrySide((6 + cameFrom - hex.getLabelSide()) % 6);

            return set;
        }


        if (block >= 0)
        {
            set.addAll(findNormalMoves(hex.getNeighbor(block), player, legion,
                roll - 1, ARROWS_ONLY, (block + 3) % 6));
        }
        else if (block == ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARCH && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), player,
                        legion, roll - 1, ARROWS_ONLY,
                        (i + 3) % 6));
                }
            }
        }
        else if (block == ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARROW && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), player,
                    legion, roll - 1, ARROWS_ONLY, (i + 3) % 6));
                }
            }
        }

        return set;
    }


    /** Recursively find tower teleport moves from this hex.  That's
     *  all unoccupied hexes within 6 hexes.  Teleports to towers
     *  are handled separately.  Do not double back. */
    public Set findTowerTeleportMoves(MasterHex hex, Player player,
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
        return listMoves(legion, false).size();
    }


    /** Select hexes where this legion can move. Return total number of
     *  legal moves. */
    public int highlightMoves(Legion legion)
    {
        Set set = listMoves(legion, true);
        board.unselectAllHexes();
        board.selectHexesByLabels(set);
        return set.size();
    }


    /** Return set of hex labels where this legion can move.
     *  Include teleport moves only if teleport is true. */
    public Set listMoves(Legion legion, boolean teleport)
    {
        return listMoves(legion, teleport, legion.getCurrentHex(),
             legion.getPlayer().getMovementRoll());
    }



    /** Return set of hex labels where this legion can move.
     *  Include teleport moves only if teleport is true. */
    public Set listMoves(Legion legion, boolean teleport, MasterHex hex,
        int movementRoll)
    {
        HashSet set = new HashSet();
        if (legion.hasMoved())
        {
            return set;
        }

        Player player = legion.getPlayer();
        board.clearAllNonFriendlyOccupiedEntrySides(player);

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

        set.addAll(findNormalMoves(hex, player, legion, movementRoll, block,
            NOWHERE));

        if (teleport && movementRoll == 6)
        {
            // Tower teleport
            if (hex.getTerrain() == 'T' && legion.numLords() > 0 &&
                !player.hasTeleported())
            {
                // Mark every unoccupied hex within 6 hexes.
                set.addAll(findTowerTeleportMoves(hex, player, legion, 6,
                    NOWHERE));

                // Mark every unoccupied tower.
                for (int tower = 100; tower <= 600; tower += 100)
                {
                    String label = String.valueOf(tower);
                    hex = MasterBoard.getHexFromLabel(label);
                    if (!hex.isOccupied())
                    {
                        set.add(label);

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


    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    public boolean chooseWhetherToTeleport(MasterHex hex)
    {
        String [] options = new String[2];
        options[0] = "Teleport";
        options[1] = "Move Normally";
        int answer = JOptionPane.showOptionDialog(board, "Teleport?",
            "Teleport?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

        // If Teleport, then leave teleported set.
        if (answer == JOptionPane.YES_OPTION)
        {
            return true;
        }
        else
        {
            return false;
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


    public SummonAngel getSummonAngel()
    {
        return summonAngel;
    }


    public void createSummonAngel(Legion attacker)
    {
        // Make sure the MasterBoard is visible.
        if (masterFrame.getState() == JFrame.ICONIFIED)
        {
            masterFrame.setState(JFrame.NORMAL);
        }
        // And bring it to the front.
        masterFrame.show();

        summonAngel = SummonAngel.summonAngel(board, attacker);
    }


    public void finishBattle()
    {
        masterFrame.show();

        if (summonAngel != null)
        {
            highlightSummonableAngels(summonAngel.getLegion());
            summonAngel.repaint();
        }
        else
        {
            highlightEngagements();
        }
        battle = null;
        engagementInProgress = false;

        // Insert a blank line in the log file after each battle.
        logEvent("\n");
    }


    /** Return number of legions with summonable angels. */
    public int highlightSummonableAngels(Legion legion)
    {
        board.unselectAllHexes();

        Player player = legion.getPlayer();
        player.setDonor(null);

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
            board.selectHexesByLabels(set);
        }

        return count;
    }


    public void finishSummoningAngel()
    {
        highlightEngagements();
        if (battle != null)
        {
            battle.finishSummoningAngel(summonAngel.getSummoned());
        }
        summonAngel = null;
    }


    public void highlightPossibleRecruits()
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

                SplitLegion.splitLegion(masterFrame, legion,
                    player.getOption(autoPickMarker));

                updateStatusScreen();
                // If we split, unselect this hex.
                if (legion.getHeight() < 7)
                {
                    MasterHex hex = legion.getCurrentHex();
                    board.unselectHexByLabel(hex.getLabel());
                }
                return;

            case Game.MOVE:
                // Select this legion.
                player.setMover(legion);
                // Just painting the marker doesn't always do the trick.
                legion.getCurrentHex().repaint();

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
                    Creature recruit = PickRecruit.pickRecruit(masterFrame,
                        legion);
                    if (recruit != null)
                    {
                        doRecruit(recruit, legion, masterFrame);
                    }

                    if (!legion.canRecruit())
                    {
                        board.unselectHexByLabel(
                            legion.getCurrentHex().getLabel());

                        updateStatusScreen();
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
                Legion legion = player.getMover();
                // Verify that the move is legal, rather than trusting
                // hex.isSelected().
                if (legion != null && listMoves(legion, true).contains(
                    hex.getLabel()))
                {
                    // Pick teleport or normal move if necessary.
                    if (hex.getTeleported() && hex.canEnterViaLand())
                    {
                        boolean answer;
                        if (player.getOption(autoPickEntrySide))
                        {
                            // Always choose to move normally rather
                            // than teleport if auto-picking entry sides.
                            answer = false;
                        }
                        else
                        {
                            answer = chooseWhetherToTeleport(hex);
                        }
                        hex.setTeleported(answer);
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
                        int side;
                        if (player.getOption(autoPickEntrySide))
                        {
                            side = hex.getEntrySide();
                        }
                        else
                        {
                            side = PickEntrySide.pickEntrySide(masterFrame,
                                hex);
                        }
                        hex.clearAllEntrySides();
                        if (side == 1 || side == 3 || side == 5)
                        {
                            hex.setEntrySide(side);
                        }
                    }

                    // Unless a PickEntrySide was cancelled or
                    // disallowed, execute the move.
                    if (!hex.isOccupied() || hex.getNumEntrySides() == 1)
                    {
                        // If the legion teleported, reveal a lord.
                        if (hex.getTeleported() &&
                            !getOption(allStacksVisible))
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
                    actOnMisclick();
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
        if (summonAngel != null)
        {
            Legion donor = hex.getFriendlyLegion(player);
            if (donor != null)
            {
                player.setDonor(donor);
                summonAngel.updateChits();
                summonAngel.repaint();
                donor.getMarker().repaint();
            }
            return;
        }

        // Do not allow clicking on engagements if one is
        // already being resolved.
        if (hex.isEngagement() && !engagementInProgress)
        {
            engagementInProgress = true;
            Legion attacker = hex.getFriendlyLegion(player);
            Legion defender = hex.getEnemyLegion(player);

            if (defender.canFlee())
            {
                // Fleeing gives half points and denies the
                // attacker the chance to summon an angel.
                boolean flees = Concede.flee(masterFrame, defender,
                    attacker);
                if (flees)
                {
                    handleConcession(defender, attacker, true);
                    return;
                }
            }

            // The attacker may concede now without
            // allowing the defender a reinforcement.
            boolean concedes = Concede.concede(masterFrame, attacker,
                defender);

            if (concedes)
            {
                handleConcession(attacker, defender, false);
                return;
            }

            // The players may agree to a negotiated settlement.
            NegotiationResults results = Negotiate.negotiate(masterFrame,
                attacker, defender);

            if (results.isSettled())
            {
                handleNegotiation(results, attacker, defender);
                return;
            }

            else
            {
                // Battle
                // Reveal both legions to all players.
                attacker.revealAllCreatures();
                defender.revealAllCreatures();
                battle = new Battle(board, attacker, defender, defender,
                    hex, false, 1, Battle.MOVE);
            }
        }
    }


    public void handleConcession(Legion loser, Legion winner, boolean fled)
    {
        // Figure how many points the victor receives.
        int points = loser.getPointValue();
        if (fled)
        {
            points /= 2;
            Game.logEvent("Legion " + loser.getLongMarkerName() +
                " flees from legion " + winner.getLongMarkerName());
        }
        else
        {
            Game.logEvent("Legion " + loser.getLongMarkerName() +
                " concedes to legion " + winner.getLongMarkerName());
        }

        // Remove the dead legion.
        loser.remove();

        // Add points, and angels if necessary.
        winner.addPoints(points);
        // Remove any fractional points.
        winner.getPlayer().truncScore();

        // If this was the titan stack, its owner dies and gives half
        // points to the victor.
        if (loser.numCreature(Creature.titan) == 1)
        {
            loser.getPlayer().die(winner.getPlayer(), true);
        }

        // Unselect and repaint the hex.
        MasterHex hex = winner.getCurrentHex();
        MasterBoard.unselectHexByLabel(hex.getLabel());

        // No recruiting or angel summoning is allowed after the
        // defender flees or the attacker concedes before entering
        // the battle.
        highlightEngagements();
        engagementInProgress = false;
    }


    public void handleNegotiation(NegotiationResults results, Legion
        attacker, Legion defender)
    {
        if (results.isMutual())
        {
             // Remove both legions and give no points.
             attacker.remove();
             defender.remove();

             Game.logEvent(attacker.getLongMarkerName() + " and " +
                 defender.getLongMarkerName() +
                 " agree to mutual elimination");

             // If both Titans died, eliminate both players.
             if (attacker.numCreature(Creature.titan) == 1 &&
                 defender.numCreature(Creature.titan) == 1)
             {
                 // Make defender die first, to simplify turn advancing.
                 defender.getPlayer().die(null, false);
                 attacker.getPlayer().die(null, true);
             }

             // If either was the titan stack, its owner dies and gives
             // half points to the victor.
             else if (attacker.numCreature(Creature.titan) == 1)
             {
                 attacker.getPlayer().die(defender.getPlayer(), true);
             }

             else if (defender.numCreature(Creature.titan) == 1)
             {
                 defender.getPlayer().die(attacker.getPlayer(), true);
             }
        }
        else
        {
            // One legion was eliminated during negotiations.
            Legion winner = results.getWinner();
            Legion loser;
            if (winner == defender)
            {
                loser = attacker;
            }
            else
            {
                loser = defender;
            }

            StringBuffer log = new StringBuffer("Winning legion ");
            log.append(winner.getLongMarkerName());
            log.append(" loses creatures ");

            // Remove all dead creatures from the winning legion.
            ArrayList winnerLosses = results.getWinnerLosses();
            Iterator it = winnerLosses.iterator();
            while (it.hasNext())
            {
                Creature creature = (Creature)it.next();
                log.append(creature.getName());
                if (it.hasNext())
                {
                    log.append(", ");
                }
                winner.removeCreature(creature, true, true);
            }
            Game.logEvent(log.toString());

            int points = loser.getPointValue();

            // Remove the losing legion.
            loser.remove();

            // Add points, and angels if necessary.
            winner.addPoints(points);

            Game.logEvent("Legion " + loser.getLongMarkerName() +
               " is eliminated by legion " + winner.getLongMarkerName() +
               " via negotiation");

            // If this was the titan stack, its owner dies and gives half
            // points to the victor.
            if (loser.numCreature(Creature.titan) == 1)
            {
                loser.getPlayer().die(winner.getPlayer(), true);
            }

            if (winner == defender)
            {
                if (defender.canRecruit())
                {
                    // If the defender won the battle by agreement,
                    // he may recruit.
                    Creature recruit = PickRecruit.pickRecruit(
                        masterFrame, defender);
                    if (recruit != null)
                    {
                        doRecruit(recruit, defender, masterFrame);
                    }
                }
            }
            else
            {
                if (attacker.getHeight() < 7 &&
                    !attacker.getPlayer().hasSummoned())
                {
                    // If the attacker won the battle by agreement,
                    // he may summon an angel.
                    createSummonAngel(attacker);
                }
            }
        }

        highlightEngagements();
        engagementInProgress = false;
    }


    public void actOnMisclick()
    {
        switch (getPhase())
        {
            case Game.MOVE:
                getActivePlayer().setMover(null);
                highlightUnmovedLegions();
                break;

            case Game.FIGHT:
                if (summonAngel != null)
                {
                    highlightSummonableAngels(summonAngel.getLegion());
                    summonAngel.repaint();
                }
                else
                {
                    highlightEngagements();
                }
                break;

            case Game.MUSTER:
                highlightPossibleRecruits();
                break;

            default:
                break;
        }
    }


    public static void main(String [] args)
    {
        // Set look and feel to native, since Metal does not show titles
        // for popup menus.
        try
        {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Could not set look and feel.");
        };

        if (args.length == 0)
        {
            // Start a new game.
            new Game(null);
        }
        else
        {
            // Load a game.
            new Game(null, args[0]);
        }
    }
}
