import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;

import net.sf.colossus.protocol.*;


/**
 * Class Game gets and holds high-level data about a Titan game.
 * @version $Id$
 * @author David Ripton
 * @author Bruce Sherrod <bruce@thematrix.com>
 */

public final class Game extends GameSource
{
    private ArrayList players = new ArrayList(6);
    private int activePlayerNum;
    private int turnNumber;    // Advance when every player has a turn
    private boolean engagementInProgress;
    private boolean battleInProgress;
    private boolean summoningAngel;
    private boolean pendingAdvancePhase;
    private boolean gameOver;
    private Battle battle;
    private static Random random = new Random();
    private Caretaker caretaker = new Caretaker(this);
    private Server server;
    // Negotiation
    private Set [] offers = new HashSet[2];

    // Constants for phases of a turn.
    public static final int SPLIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int MUSTER = 4;
    private int phase;


    // Constants for savegames
    public static final String saveDirname = "saves";
    public static final String saveExtension = ".sav";
    public static final String saveGameVersion =
        "Colossus savegame version 8";

    public static final String configVersion =
        "Colossus config file version 2";

    private static TerrainRecruitLoader trl;

    public Game()
    {
    }


    // XXX temp
    public void initServerAndClients()
    {
        try 
        {
            InputStream terIS = 
                java.lang.ClassLoader.getSystemResourceAsStream(
                GetPlayers.recruitName);
            if (terIS == null)
            {
                terIS = new FileInputStream(GetPlayers.recruitName);
            }
            if (terIS == null) 
            {
                System.out.println(
                    "Recruit-per-terrain loading failed for file " + 
                    GetPlayers.recruitName);
                System.exit(1);
            }
            trl = new TerrainRecruitLoader(terIS);
            while (trl.oneTerrain() >= 0) {}
        } 
        catch (Exception e) 
        {
            System.out.println("Recruit-per-terrain loading failed : " + e);
            System.exit(1);
        }

        if (server == null)
        {
            server = new Server(this);
        }
        else
        {
            server.disposeAllClients();
        }
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            server.addClient(new Client(server, player.getName()));
        }
    }


    /** Temporary to avoid having to rename this everywhere.*/
    public void initBoard()
    {
        initServerAndClients();
        server.allInitBoard();
    }


    /**
     * Make a deep copy for the AI to use.
     * This preserves all game state but throws away a lot of the UI stuff
     */
    public Game AICopy()
    {
        // XXX Make sure to clear player options so that we don't get into
        // an AI infinite loop.
        Game newGame = new Game();
        for (int i = 0; i < players.size(); i++)
        {
            Player player = ((Player)players.get(i)).AICopy(newGame);
            newGame.players.add(i, player);
        }
        newGame.activePlayerNum = activePlayerNum;
        newGame.turnNumber = turnNumber;
        if (battle != null)
        {
            newGame.battle = battle.AICopy(newGame);
        }
        newGame.caretaker = caretaker.AICopy();
        newGame.phase = phase;
        newGame.engagementInProgress = engagementInProgress;
        newGame.battleInProgress = battleInProgress;
        newGame.summoningAngel = summoningAngel;
        newGame.server = server;

        return newGame;
    }


    public void newGame()
    {
        turnNumber = 1;
        phase = SPLIT;
        engagementInProgress = false;
        battleInProgress = false;
        summoningAngel = false;
        pendingAdvancePhase = false;
        gameOver = false;
        caretaker.resetAllCounts();
        players.clear();

        // XXX Temporary hotseat startup code
        JFrame frame = new JFrame();
        List playerInfo = GetPlayers.getPlayers(frame);
        if (playerInfo.isEmpty())
        {
            // User selected Quit.
            dispose();
            return;
        }

        // See if user hit the Load game button, and we should
        // load a game instead of starting a new one.
        if (playerInfo.size() == 1)
        {
            String entry = (String)playerInfo.get(0);
            java.util.List values = Utils.split('~', entry);
            String key = (String)values.get(0);
            if (key.equals(GetPlayers.loadGame))
            {
                String filename = (String)values.get(1);
                loadGame(filename);
                return;
            }
        }

        Log.event("Starting new game");

        Iterator it = playerInfo.iterator();
        while (it.hasNext())
        {
            String entry = (String)it.next();
            java.util.List values = Utils.split('~', entry);
            String name = (String)values.get(0);
            String type = (String)values.get(1);
            addPlayer(name, type);
            Log.event("Add " + type + " player " + name);
        }

        assignTowers();

        // Renumber players in descending tower order.
        Collections.sort(players);
        activePlayerNum = 0;

        // We need to set the autoPlay option before loading the board,
        // so that we can avoid showing boards for AI players.

        // Remove caretaker displays.
        if (server != null)
        {
            server.saveOptions();
            server.setAllClientsOption(Options.showCaretaker, false);
        }

        // XXX temp
        initServerAndClients();

        server.loadOptions();

        // Override autoPlay option with selection.
        int i = 0;
        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            String type = player.getType();
            if (type.endsWith("AI"))
            {
                server.setClientOption(i, Options.autoPlay, true);
            }
            else if (type.equals("Human"))
            {
                server.setClientOption(i, Options.autoPlay, false);
            }
            i++;
        }

        server.allInitBoard();

        HashSet colorsLeft = new HashSet();
        for (i = 0; i < PickColor.colorNames.length; i++)
        {
            colorsLeft.add(PickColor.colorNames[i]);
        }

        // Let human players pick colors first, followed by AI players.
        for (i = getNumPlayers() - 1; i >= 0; i--)
        {
            pickPlayerColor(i, "Human", colorsLeft, frame);
        }
        for (i = getNumPlayers() - 1; i >= 0; i--)
        {
            pickPlayerColor(i, "AllAI", colorsLeft, frame);
        }

        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            placeInitialLegion(player);
            server.setupPlayerLabel(player.getName());
            server.allUpdateStatusScreen();
        }
        server.allLoadInitialMarkerImages();

        if (server.getClientOption(Options.autosave))
        {
            saveGame();
        }

        setupPhase();

        server.allUpdateStatusScreen();
        server.allUpdateCaretakerDisplay();
        // Reset the color of the player label now that it's known.
        server.allSetupPlayerLabel();
    }


    /** Let player i pick a color, only if type matches the player's type. */
    private void pickPlayerColor(int i, String type, HashSet colorsLeft,
        JFrame frame)
    {
        Player player = (Player)players.get(i);
        if (player.getType().equals("none"))
        {
            return;
        }
        if (player.getType().equals("Human") && (!type.equals("Human")))
        {
            return;
        }
        if (!player.getType().equals("Human") && (type.equals("Human")))
        {
            return;
        }
        String color;
        do
        {
            if (server.getClientOption(i, Options.autoPickColor))
            {
                color = player.aiPickColor(colorsLeft);
            }
            else
            {
                color = PickColor.pickColor(frame, this, player);
            }
        }
        while (color == null);
        colorsLeft.remove(color);
        player.setColor(color);
        if (GetPlayers.byColor.equals(player.getName()))
        {
            player.setName(color);
            server.getClient(i).setPlayerName(color);
        }
        Log.event(player.getName() + " chooses color " + color);
        player.initMarkersAvailable();
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
                    rolls[i] = rollDie();
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
            Log.event(player.getName() + " gets tower " + playerTower[i]);
            player.setTower(playerTower[i]);
        }
    }


    public Caretaker getCaretaker()
    {
        return caretaker;
    }


    public Server getServer()
    {
        return server;
    }

    public int getNumPlayers()
    {
        return players.size();
    }

    public void addPlayer(String name)
    {
        addPlayer(name, "Human");
    }

    public void addPlayer(String name, String type)
    {
        Player player = new Player(name, this);
        player.setType(type);
        addPlayer(player);
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
        // Sanity check in case called before all players are loaded.
        if (activePlayerNum < players.size())
        {
            return (Player)players.get(activePlayerNum);
        }
        else
        {
            return null;
        }
    }

    public String getActivePlayerName()
    {
        return getActivePlayer().getName();
    }

    public int getActivePlayerNum()
    {
        return activePlayerNum;
    }

    /** for AI only */
    public void setActivePlayerNum(int i)
    {
        activePlayerNum = i;
    }

    public Player getPlayer(int i)
    {
        return (Player)players.get(i);
    }

    public Collection getPlayers()
    {
        return players;
    }

    public Player getPlayer(String name)
    {
        if (name != null)
        {
            Iterator it = players.iterator();
            while (it.hasNext())
            {
                Player player = (Player)it.next();
                if (name.equals(player.getName()))
                {
                    return player;
                }
            }
        }
        return null;
    }


    public int getNumPlayersRemaining()
    {
        int remaining = 0;
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            if (!player.isDead())
            {
                remaining++;
            }
        }
        return remaining;
    }


    public Player getWinner()
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
                if (remaining > 1)
                {
                    return null;
                }
                else
                {
                    winner = player;
                }
            }
        }
        return winner;
    }


    public void checkForVictory()
    {
        int remaining = getNumPlayersRemaining();
        switch (remaining)
        {
            case 0:
                Log.event("Draw");
                server.allShowMessageDialog("Draw");
                gameOver = true;
                break;

            case 1:
                String winnerName = getWinner().getName();
                Log.event(winnerName + " wins");
                server.allShowMessageDialog(winnerName + " wins");
                gameOver = true;
                break;

            default:
                break;
        }
    }


    public boolean isOver()
    {
        return gameOver;
    }


    public int getPhase()
    {
        return phase;
    }


    /** Advance to the next phase, only if the passed oldPhase is current. */
    public void advancePhase(int oldPhase)
    {
        if (oldPhase != phase || pendingAdvancePhase)
        {
            Log.error("Called advancePhase illegally");
            return;
        }

        pendingAdvancePhase = true;

        ActionListener phaseAdvancer = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                advancePhaseInternal();
            }
        };

        // This needs to be configurable.
        int delay = 100;

        // java.util.Timer is not present in JDK 1.2, so use the Swing
        // timer for compatibility.
        javax.swing.Timer timer = new javax.swing.Timer(delay, phaseAdvancer);
        timer.setRepeats(false);
        // Restart rather than start, to throw away excess advances.
        timer.restart();
    }


    /** Advance to the next phase, with no error checking.  Do not
     *  call this directly -- it should only be called from advancePhase(). */
    private void advancePhaseInternal()
    {
        phase++;

        if (phase > MUSTER ||
            (getActivePlayer().isDead() && getNumLivingPlayers() > 0))
        {
            advanceTurn();
        }
        else
        {
            Log.event("Phase advances to " + getPhaseName(phase));
        }
        pendingAdvancePhase = false;

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
                Log.error("Bogus phase");
        }
    }


    private void setupSplit()
    {
        Player player = getActivePlayer();
        player.resetTurnState();

        // If there are no markers available, or no legions tall enough
        // to split, skip forward to movement.
        if (player.getNumMarkersAvailable() == 0 ||
            player.getMaxLegionHeight() < 4)
        {
            advancePhase(SPLIT);
        }
        else
        {
            server.allSetupSplitMenu();
            player.aiSplit();
        }
    }


    private void setupMove()
    {
        Player player = getActivePlayer();
        Client.clearUndoStack();
        player.rollMovement();
        server.allSetupMoveMenu();

        player.aiMasterMove();
    }

    private void setupFight()
    {
        // If there are no engagements, move forward to the muster phase.
        Client.clearUndoStack();
        if (!summoningAngel && findEngagements().size() == 0)
        {
            advancePhase(FIGHT);
        }
        else
        {
            server.allSetupFightMenu();
            aiSetupEngagements();
        }
    }


    private void setupMuster()
    {
        Player player = getActivePlayer();

        // If this player was eliminated in combat, or can't recruit
        // anything, advance to the next turn.
        if (player.isDead() || !player.canRecruit())
        {
            advancePhase(MUSTER);
        }
        else
        {
            player.disbandEmptyDonor();
            Client.clearUndoStack();
            server.allSetupMusterMenu();

            player.aiRecruit();
        }
    }


    private void advanceTurn()
    {
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
            Log.event(player.getName() + "'s turn, number " + turnNumber);

            server.allUpdateStatusScreen();
            if (server.getClientOption(Options.autosave))
            {
                saveGame();
            }
        }
    }


    public int getTurnNumber()
    {
        return turnNumber;
    }


    /** Create a text file describing this game's state, in
     *  file filename.  We don't use XML yet because we don't 
     *  want to require everyone to install a parser.
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
     *         Dead?
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
     *             Recruit name
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
            Log.error(e.toString());
            Log.error("Couldn't open " + filename);
            return;
        }
        PrintWriter out = new PrintWriter(fileWriter);

        out.println(saveGameVersion);
        out.println(getNumPlayers());
        out.println(getTurnNumber());
        out.println(getActivePlayerNum());
        out.println(getPhase());

        // Caretaker stacks
        java.util.List creatures = Creature.getCreatures();
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            out.println(caretaker.getCount(creature));
        }

        // Players
        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            out.println(player.getName());
            out.println(player.getType());
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
        if (engagementInProgress && battle != null)
        {
            out.println(battle.getMasterHexLabel());
            out.println(battle.getTurnNumber());
            out.println(battle.getActivePlayerName());
            out.println(battle.getPhase());
            out.println(battle.getSummonState());
            out.println(battle.getCarryDamage());
            out.println(battle.isDriftDamageApplied());

            int numCarryTargets = battle.getCarryTargets().size();
            out.println(numCarryTargets);
            it = battle.getCarryTargets().iterator();
            while (it.hasNext())
            {
                String carryTarget = (String)it.next();
                out.println(carryTarget);
            }

            dumpLegion(out, battle.getAttacker(), true);
            dumpLegion(out, battle.getDefender(), true);
        }

        if (out.checkError())
        {
            Log.error("Write error " + filename);
            // XXX Delete the partial file?
            return;
        }
    }


    private void dumpLegion(PrintWriter out, Legion legion, boolean inBattle)
    {
        out.println(legion.getMarkerId());
        out.println(legion.getCurrentHexLabel());
        out.println(legion.getStartingHexLabel());
        out.println(legion.hasMoved());
        out.println(legion.getEntrySide(legion.getCurrentHexLabel()));
        out.println(legion.getParentId());
        out.println(legion.getRecruitName());
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
                out.println(critter.getCurrentHexLabel());
                out.println(critter.getStartingHexLabel());
                out.println(critter.hasStruck());
            }
        }
    }


    /** Create a text file describing this game's state, in
     *  file <saveDirName>/<time>.sav */
    public void saveGame()
    {
        Date date = new Date();
        File savesDir = new File(saveDirname);
        if (!savesDir.exists() || !savesDir.isDirectory())
        {
             if (!savesDir.mkdir())
             {
                 Log.error("Could not create saves directory");
                 return;
             }
        }

        String filename = saveDirname + File.separator +
            date.getTime() + saveExtension;

        saveGame(filename);
    }


    /** Try to load a game from saveDirName/filename.  If the filename is
     *  "--latest" then load the latest savegame found in saveDirName. */
    public void loadGame(String filename)
    {
        File file;

        if (filename.equals("--latest"))
        {
            File dir = new File(saveDirname);
            if (!dir.exists() || !dir.isDirectory())
            {
                Log.error("No saves directory");
                dispose();
            }
            String [] filenames = dir.list(new SaveGameFilter());
            if (filenames.length < 1)
            {
                Log.error("No savegames found in saves directory");
                dispose();
            }
            file = new File(saveDirname + File.separator +
                latestSaveFilename(filenames));
        }
        else
        {
            file = new File(saveDirname + File.separator + filename);
        }

        try
        {
            FileReader fileReader = new FileReader(file);
            BufferedReader in = new BufferedReader(fileReader);
            String buf;

            buf = in.readLine();
            if (!buf.equals(saveGameVersion))
            {
                Log.error("Can't load this savegame version.");
                dispose();
            }


            // Reset flags that are not in the savegame file.
            pendingAdvancePhase = false;
            summoningAngel = false;
            gameOver = false;


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
            if (battle != null)
            {
                server.allDisposeBattleMap();
            }

            // Players
            for (int i = 0; i < numPlayers; i++)
            {
                String name = in.readLine();
                Player player = new Player(name, this);
                players.add(player);

                String type = in.readLine();
                player.setType(type);

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

            // Remove caretaker displays.
            if (server != null)
            {
                server.saveOptions();
                server.setAllClientsOption(Options.showCaretaker, false);
            }

            initServerAndClients();

            // Set up autoPlay options from player type.
            it = players.iterator();
            while (it.hasNext())
            {
                Player player = (Player)it.next();
                String name = player.getName();
                if (player.getType().endsWith("AI"))
                {
                    server.setClientOption(name, Options.autoPlay, true);
                }
                else
                {
                    server.setClientOption(name, Options.autoPlay, false);
                }
            }

            // Battle stuff
            buf = in.readLine();
            if (buf != null && buf.length() > 0)
            {
                String engagementHexLabel = buf.toString();

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

                buf = in.readLine();
                int numCarryTargets = Integer.parseInt(buf);
                HashSet carryTargets = new HashSet();
                for (int i = 0; i < numCarryTargets; i++)
                {
                    buf = in.readLine();
                    carryTargets.add(buf);
                }

                Player attackingPlayer = getActivePlayer();
                Legion attacker = readLegion(in, attackingPlayer, true);

                Player defendingPlayer = getFirstEnemyLegion(
                    engagementHexLabel, attackingPlayer).getPlayer();
                Legion defender = readLegion(in, defendingPlayer, true);

                int activeLegionNum;
                if (battleActivePlayerName.equals(attackingPlayer.getName()))
                {
                    activeLegionNum = Battle.ATTACKER;
                }
                else
                {
                    activeLegionNum = Battle.DEFENDER;
                }

                battle = new Battle(this, attacker.getMarkerId(),
                    defender.getMarkerId(), activeLegionNum,
                    engagementHexLabel, battleTurnNum, battlePhase);
                battle.setSummonState(summonState);
                battle.setCarryDamage(carryDamage);
                battle.setDriftDamageApplied(driftDamageApplied);
                battle.setCarryTargets(carryTargets);
                battle.init();
            }

            server.loadOptions();

            // Setup MasterBoard
            server.allInitBoard();
            server.allLoadInitialMarkerImages();
            setupPhase();

            // Setup status screen
            server.allUpdateStatusScreen();
            server.allUpdateCaretakerDisplay();
        }
        // FileNotFoundException, IOException, NumberFormatException
        catch (Exception e)
        {
            Log.error(e + "Tried to load corrupt savegame.");
            e.printStackTrace();
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

        String recruitName = in.readLine();
        if (recruitName.equals("null"))
        {
            recruitName = null;
        }

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
                Creature.getCreatureByName(buf), false, null, this);

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
            legion = new Legion(markerId, parentId, currentHexLabel,
                startingHexLabel, critters[0], critters[1], critters[2],
                critters[3], critters[4], critters[5], critters[6],
                critters[7], player.getName(), this);
            player.addLegion(legion);
        }

        legion.setMoved(moved);
        legion.setRecruitName(recruitName);
        legion.setEntrySide(currentHexLabel, entrySide);
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
        return (String)Collections.max(Arrays.asList(filenames), new
            Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    if (!(o1 instanceof String) || !(o2 instanceof String))
                    {
                        throw new ClassCastException();
                    }
                    long diff = (numberValue((String)o1) -
                        numberValue((String)o2));
                    if (diff > Integer.MAX_VALUE)
                    {
                        return Integer.MAX_VALUE;
                    }
                    if (diff < Integer.MIN_VALUE)
                    {
                        return Integer.MIN_VALUE;
                    }
                    return (int)diff;
                }
            });
    }


    /** Return a list of creatures that can be recruited in
     *  the given terrain, ordered from lowest to highest. */
    public static ArrayList getPossibleRecruits(char terrain)
    {
        ArrayList recruits = trl.getPossibleRecruits(terrain);
        return recruits;
    }

    /** Return the number of the given recruiter needed to muster the given
      * recruit in the given terrain.  Return an impossibly big number
      * if the recruiter can't muster that recruit in that terrain. */
    public static int numberOfRecruiterNeeded(Creature recruiter, Creature
        recruit, char terrain)
    {
        return(trl.numberOfRecruiterNeeded(recruiter,recruit,terrain));
    }

    /** Return a list of eligible recruits, as Creatures. */
    public ArrayList findEligibleRecruits(Legion legion)
    {
        return findEligibleRecruits(legion, legion.getCurrentHexLabel());
    }

    /** Return a list of eligible recruits, as Creatures. */
    public ArrayList findEligibleRecruits(Legion legion, String hexLabel)
    {
        ArrayList recruits;

        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        char terrain = hex.getTerrain();

        // Towers are a special case.
        if (terrain == 'T')
        {
	    recruits = getPossibleRecruits(terrain);
	    if (legion.numCreature(Creature.titan) < 1 &&
                legion.numCreature((Creature)recruits.get(4)) < 1)
	    { /* no Titan, no itself */ 
		recruits.remove(4);
	    }
	    java.util.List creatures = Creature.getCreatures();
	    Iterator it = creatures.iterator();
	    boolean keepGuardian = false; /* guardian or something else... */
	    while (it.hasNext() && !keepGuardian)
            {
		Creature creature = (Creature)it.next();
		if ((legion.numCreature(creature) >= 3) &&
		    (!creature.isImmortal()))
		    keepGuardian = true;
	    }
	    if (!keepGuardian)
	    { /* no non-lord creature is 3 or more in number */
		recruits.remove(3);
	    }
	    /*
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
	    */
        }
        else
        {
            recruits = getPossibleRecruits(terrain);

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

        String hexLabel = legion.getCurrentHexLabel();
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        char terrain = hex.getTerrain();

        if (terrain == 'T')
        {
            // Towers are a special case.  The recruiter of tower creatures
            // remains anonymous, so we only deal with guardians and warlocks.
	    ArrayList possibleRecruiters = getPossibleRecruits(terrain);
	    Creature warlockOrNot = (Creature)possibleRecruiters.get(4);
	    Creature guardianOrNot = (Creature)possibleRecruiters.get(3);
	    if (recruit.getName().equals(warlockOrNot.getName()))
	    {
		if (legion.numCreature(Creature.titan) >= 1)
                {
                    recruiters.add(legion.getCritter(Creature.titan));
                }
		if (legion.numCreature(warlockOrNot) >= 1)
                {
                    recruiters.add(legion.getCritter(warlockOrNot));
                }
	    }
	    else if (recruit.getName().equals(guardianOrNot.getName()))
	    {
                java.util.List creatures = Creature.getCreatures();
                Iterator it = creatures.iterator();
                while (it.hasNext())
		{
		    Creature creature = (Creature)it.next();
                    if (creature.getName().equals(guardianOrNot.getName()) &&
                        (legion.numCreature(creature) >= 1))
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
	    /*
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
	    */
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
    private boolean allRecruitersVisible(Legion legion, ArrayList recruiters)
    {
        if (server.getClientOption(Options.allStacksVisible))
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


    public void doRecruit(Creature recruit, Legion legion)
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
        else if (server.getClientOption(player.getName(),
            Options.autoPickRecruiter) || numEligibleRecruiters == 1 ||
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
            String recruiterName = server.pickRecruiter(legion, recruiters);
            recruiter = Creature.getCreatureByName(recruiterName);
        }

        legion.addCreature(recruit, true);
        MasterHex hex = legion.getCurrentHex();
        int numRecruiters = 0;
        if (recruiter != null)
        {
            // Mark the recruiter(s) as visible.
            numRecruiters = numberOfRecruiterNeeded(recruiter,
                recruit, hex.getTerrain());
            if (numRecruiters >= 1 && numRecruiters <= 3)
            {
                legion.revealCreatures(recruiter, numRecruiters);
            }
        }

        Log.event("Legion " + legion.getLongMarkerName() + " in " +
            hex.getDescription() + " recruits " + recruit.getName() +
            " with " + (numRecruiters == 0 ? "nothing" :
            numRecruiters + " " + (numRecruiters > 1 ?
            recruiter.getPluralName() : recruiter.getName())));

        // Recruits are one to a customer.
        legion.setRecruitName(recruit.getName());

        Client.pushUndoStack(legion.getMarkerId());

        server.allRepaintHex(legion.getCurrentHexLabel());
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
        System.exit(0);
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
        if (server.getClientOption(player.getName(), Options.autoPickMarker))
        {
            selectedMarkerId = player.getFirstAvailableMarker();
        }
        else
        {
            do
            {
                selectedMarkerId = PickMarker.pickMarker(server.getClient(
                    player.getName()).getBoard().getFrame(),
                    name, player.getMarkersAvailable());
            }
            while (selectedMarkerId == null);
        }

        player.selectMarkerId(selectedMarkerId);
        Log.event(name + " selects initial marker");

        // Lookup coords for chit starting from player[i].getTower()
        String hexLabel = (String.valueOf(100 * player.getTower()));

        caretaker.takeOne(Creature.titan);
        caretaker.takeOne(Creature.angel);
        caretaker.takeOne(Creature.ogre);
        caretaker.takeOne(Creature.ogre);
        caretaker.takeOne(Creature.centaur);
        caretaker.takeOne(Creature.centaur);
        caretaker.takeOne(Creature.gargoyle);
        caretaker.takeOne(Creature.gargoyle);

        Legion legion = Legion.getStartingLegion(selectedMarkerId,
            hexLabel, player.getName(), this);
        player.addLegion(legion);
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
    private Set findNormalMoves(MasterHex hex, Player player, Legion legion,
        int roll, int block, int cameFrom, boolean affectEntrySides,
        boolean ignoreFriends)
    {
        HashSet set = new HashSet();
        String hexLabel = hex.getLabel();

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (getNumEnemyLegions(hexLabel, player) > 0)
        {
            if (getNumFriendlyLegions(hexLabel, player) == 0 || ignoreFriends)
            {
                set.add(hexLabel);
                // Set the entry side relative to the hex label.
                if (affectEntrySides)
                {
                    legion.setEntrySide(hexLabel,
                        (6 + cameFrom - hex.getLabelSide()) % 6);
                }
            }
            return set;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            Iterator it = player.getLegions().iterator();
            while (it.hasNext())
            {
                // Account for spin cycles.
                Legion otherLegion = (Legion)it.next();
                if (!ignoreFriends && otherLegion != legion &&
                    hexLabel.equals(otherLegion.getCurrentHexLabel()))
                {
                    return set;
                }
            }
            set.add(hexLabel);

            // Need to set entry sides even if no possible engagement,
            // for chooseWhetherToTeleport()
            if (affectEntrySides)
            {
                legion.setEntrySide(hexLabel,
                    (6 + cameFrom - hex.getLabelSide()) % 6);
            }
            return set;
        }

        if (block >= 0)
        {
            set.addAll(findNormalMoves(hex.getNeighbor(block), player, legion,
                roll - 1, ARROWS_ONLY, (block + 3) % 6, affectEntrySides,
                ignoreFriends));
        }
        else if (block == ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARCH && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), player,
                        legion, roll - 1, ARROWS_ONLY, (i + 3) % 6,
                        affectEntrySides, ignoreFriends));
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
                        legion, roll - 1, ARROWS_ONLY, (i + 3) % 6,
                        affectEntrySides, ignoreFriends));
                }
            }
        }

        return set;
    }

    /** Recursively find tower teleport moves from this hex.  That's
     *  all unoccupied hexes within 6 hexes.  Teleports to towers
     *  are handled separately.  Do not double back. */
    public Set findTowerTeleportMoves(MasterHex hex, Player player,
        Legion legion, int roll, int cameFrom, boolean ignoreFriends)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.
        String hexLabel = hex.getLabel();
        HashSet set = new HashSet();

        if (!isOccupied(hexLabel))
        {
            set.add(hexLabel);

            // Mover can choose side of entry.
            legion.setTeleported(hexLabel, true);
        }

        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom && (hex.getExitType(i) != MasterHex.NONE ||
                   hex.getEntranceType(i) != MasterHex.NONE))
                {
                    set.addAll(findTowerTeleportMoves(hex.getNeighbor(i),
                        player, legion, roll - 1, (i + 3) % 6, ignoreFriends));
                }
            }
        }

        return set;
    }


    /** Return number of legal non-teleport moves. */
    public int countConventionalMoves(Legion legion)
    {
        return listMoves(legion, false, false, false).size();
    }


    /** Return set of hex labels where this legion can move.
     *  Include teleport moves only if teleport is true.
     *  Change the legion's entry sides only if affectEntrySides
     *  is true. Include moves currently blocked by friendly
     *  legions if ignoreFriends is true. */
    public Set listMoves(Legion legion, boolean teleport, boolean
        affectEntrySides, boolean ignoreFriends)
    {
        return listMoves(legion, teleport, legion.getCurrentHex(),
             legion.getPlayer().getMovementRoll(), affectEntrySides,
             ignoreFriends);
    }


    /** Return set of hex labels where this legion can move.
     *  Include teleport moves only if teleport is true. */
    public Set listMoves(Legion legion, boolean teleport, MasterHex hex,
        int movementRoll, boolean affectEntrySides, boolean ignoreFriends)
    {
        HashSet set = new HashSet();
        if (legion.hasMoved())
        {
            return set;
        }

        Player player = legion.getPlayer();

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
            NOWHERE, affectEntrySides, ignoreFriends));

        if (teleport && movementRoll == 6)
        {
            // Tower teleport
            if (hex.getTerrain() == 'T' && legion.numLords() > 0 &&
                !player.hasTeleported())
            {
                // Mark every unoccupied hex within 6 hexes.
                set.addAll(findTowerTeleportMoves(hex, player, legion, 6,
                    NOWHERE, ignoreFriends));

                // Mark every unoccupied tower.
                for (int tower = 100; tower <= 600; tower += 100)
                {
                    String hexLabel = String.valueOf(tower);
                    if (!isOccupied(hexLabel) || ignoreFriends)
                    {
                        set.add(hexLabel);

                        // Mover can choose side of entry.
                        legion.setTeleported(hexLabel, true);
                    }
                }
            }

            // Titan teleport
            if (player.canTitanTeleport() && legion.hasTitan())
            {
                // Mark every hex containing an enemy stack that does not
                // already contain a friendly stack.
                Iterator it = getAllEnemyLegions(player).iterator();
                while (it.hasNext())
                {
                    Legion other = (Legion)it.next();
                    {
                        String hexLabel = other.getCurrentHexLabel();
                        if (!isEngagement(hexLabel) || ignoreFriends)
                        {
                            set.add(hexLabel);
                            // Mover can choose side of entry.
                            legion.setTeleported(hexLabel, true);
                        }
                    }
                }
            }
        }
        return set;
    }


    boolean isEngagement(String hexLabel)
    {
        if (getNumLegions(hexLabel) > 1)
        {
            ArrayList markerIds = getLegionMarkerIds(hexLabel);
            Iterator it = markerIds.iterator();
            String markerId = (String)it.next();
            Player player = getPlayerByMarkerId(markerId);
            while (it.hasNext())
            {
                markerId = (String)it.next();
                if (getPlayerByMarkerId(markerId) != player)
                {
                    return true;
                }
            }
        }
        return false;
    }


    /** Return set of hexLabels for engagements found. */
    public Set findEngagements()
    {
        HashSet set = new HashSet();
        Player player = getActivePlayer();

        Iterator it = player.getLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            String hexLabel = legion.getCurrentHexLabel();
            if (getNumEnemyLegions(hexLabel, player) > 0)
            {
                set.add(hexLabel);
            }
        }
        return set;
    }


    public void createSummonAngel(Legion attacker)
    {
        Player player = getActivePlayer();
        if (server.getClientOption(player.getName(), Options.autoSummonAngels))
        {
            String typeColonDonor = player.aiSummonAngel(attacker);
            int split = typeColonDonor.indexOf(':');
            String angelType = typeColonDonor.substring(0, split);
            String donorId = typeColonDonor.substring(split + 1);

            // Set up the donor in case the summon gets cancelled.
            Legion donor = player.getLegionByMarkerId(donorId);
            player.setDonorId(donorId);
            Creature angel = Creature.getCreatureByName(angelType);
            doSummon(attacker, donor, angel);
        }
        else
        {
            summoningAngel = true;
            server.createSummonAngel(attacker);
        }
    }

    public void doSummon(Legion legion, Legion donor, Creature angel)
    {
        Player player = getActivePlayer();

        if (angel != null && donor != null && legion.canSummonAngel())
        {
            // Only one angel can be summoned per turn.
            player.setSummoned(true);

            // Move the angel or archangel.
            donor.removeCreature(angel, false, false);
            legion.addCreature(angel, false);

            Log.event("An " + angel.getName() +
                " is summoned from legion " + donor.getLongMarkerName() +
                " into legion " + legion.getLongMarkerName());
        }

        if (battle != null)
        {
            battle.finishSummoningAngel(player.hasSummoned());
        }
        summoningAngel = false;
    }


    /** For AI. */
    public Battle getBattle()
    {
        return battle;
    }


    private void aiSetupEngagements()
    {
        Player player = getActivePlayer();
        String engagementHexLabel = player.aiPickEngagement();
        if (engagementHexLabel != null)
        {
            server.engage(engagementHexLabel);
        }

        // XXX Still advancing when SummonAngel is visible.
        if (findEngagements().size() == 0 && !summoningAngel)
        {
            advancePhase(FIGHT);
        }
    }


    public void finishBattle(String hexLabel, boolean attackerEntered)
    {
        battle = null;
        Client.clearUndoStack();
        server.allDisposeBattleMap();
        server.allDeiconifyBoard();

        // Handle any after-battle angel summoning or recruiting.
        if (getNumLegions(hexLabel) == 1)
        {
            Legion legion = getFirstLegion(hexLabel);
            // Make all creatures in the victorious legion visible.
            legion.revealAllCreatures();
            // Remove battle info from legion and its creatures.
            legion.clearBattleInfo();

            if (legion.getPlayer() == getActivePlayer())
            {
                // Attacker won, so possibly summon angel.
                if (legion.canSummonAngel())
                {
                    createSummonAngel(legion);
                }
            }
            else
            {
                // Defender won, so possibly recruit reinforcement.
                if (attackerEntered && legion.canRecruit())
                {
                    Creature recruit = null;
                    Player player = legion.getPlayer();
                    if (server.getClientOption(player.getName(),
                        Options.autoRecruit))
                    {
                        recruit = player.aiReinforce(legion);
                    }
                    else
                    {
                        String recruitName = server.pickRecruit(legion);
                        if (recruitName != null)
                        {
                            recruit = Creature.getCreatureByName(recruitName);
                        }
                    }
                    if (recruit != null)
                    {
                        doRecruit(recruit, legion);
                    }
                }
            }
        }
        engagementInProgress = false;
        battleInProgress = false;
        server.allUpdateStatusScreen();
        if (!summoningAngel)
        {
            server.allHighlightEngagements();
        }
        aiSetupEngagements();
    }


    public Set findSummonableAngels(Legion legion)
    {
        HashSet set = new HashSet();
        Iterator it = legion.getPlayer().getLegions().iterator();
        while (it.hasNext())
        {
            Legion candidate = (Legion)it.next();
            if (candidate != legion)
            {
                String hexLabel = candidate.getCurrentHexLabel();
                if ((candidate.numCreature(Creature.angel) > 0 ||
                    candidate.numCreature(Creature.archangel) > 0) &&
                    !isEngagement(hexLabel))
                {
                    set.add(hexLabel);
                }
            }
        }
        return set;
    }


    /** Only used for human players, not for auto splits */
    public boolean doSplit(Legion legion, Player player)
    {
        // Need a legion marker to split.
        if (player.getNumMarkersAvailable() == 0)
        {
            server.showMessageDialog(player.getName(),
                "No markers are available.");
            return false;
        }
        // Don't allow extra splits in turn 1.
        if (getTurnNumber() == 1 && player.getNumLegions() > 1)
        {
            server.showMessageDialog(player.getName(),
                "Cannot split twice on Turn 1.");
            return false;
        }

        int oldHeight = legion.getHeight();

        legion.sortCritters();

        String selectedMarkerId = player.pickMarker();

        String results = server.splitLegion(legion, selectedMarkerId);
        if (results != null)
        {
            List strings = Utils.split(',', results);
            String newMarkerId = (String)strings.remove(0);

            // Need to replace strings with creatures.
            ArrayList creatures = new ArrayList();
            Iterator it = strings.iterator();
            while (it.hasNext())
            {
                String name = (String)it.next();
                Creature creature = Creature.getCreatureByName(name);
                creatures.add(creature);
            }
            Legion newLegion = legion.split(creatures, newMarkerId);

            // Hide all creatures in both legions.
            legion.hideAllCreatures();
            newLegion.hideAllCreatures();
        }


        int newHeight = legion.getHeight();

        // If we split, unselect this hex.
        MasterHex hex = legion.getCurrentHex();
        if (newHeight < 7)
        {
            server.allUpdateStatusScreen();
            server.allUnselectHexByLabel(hex.getLabel());
            server.allAlignLegions(hex.getLabel());
        }

        return (newHeight < oldHeight);
    }


    /** Move the legion to the hex if legal.  Return true if the
     *  legion was moved. */
    public boolean doMove(String markerId, String hexLabel)
    {
        boolean moved = false;

        Legion legion = getLegionByMarkerId(markerId);

        // Verify that the move is legal.
        if (legion != null && listMoves(legion, true, true,
            false).contains(hexLabel))
        {
            Player player = legion.getPlayer();
            String playerName = player.getName();
            MasterHex hex = MasterBoard.getHexByLabel(hexLabel);

            // Pick teleport or normal move if necessary.
            if (legion.getTeleported(hexLabel) &&
                legion.canEnterViaLand(hexLabel))
            {
                boolean answer;
                if (server.getClientOption(playerName,
                    Options.autoPickEntrySide))
                {
                    // Always choose to move normally rather
                    // than teleport if auto-picking entry sides.
                    answer = false;
                }
                else
                {
                    answer = server.getClient(playerName).
                        chooseWhetherToTeleport();
                }
                legion.setTeleported(hexLabel, answer);
            }

            // If this is a tower hex, set the entry side
            // to '3', regardless.
            if (hex.getTerrain() == 'T')
            {
                legion.clearAllEntrySides(hexLabel);
                legion.setEntrySide(hexLabel, 3);
            }
            // If this is a teleport to a non-tower hex,
            // then allow entry from all three sides.
            else if (legion.getTeleported(hexLabel))
            {
                legion.setEntrySide(hexLabel, 1);
                legion.setEntrySide(hexLabel, 3);
                legion.setEntrySide(hexLabel, 5);
            }

            // Pick entry side if hex is enemy-occupied and there is
            // more than one possibility.
            if (isOccupied(hexLabel) && legion.getNumEntrySides(hexLabel) > 1)
            {
                int side;
                if (server.getClientOption(playerName,
                    Options.autoPickEntrySide))
                {
                    side = player.aiPickEntrySide(hexLabel, legion);
                }
                else
                {
                    side = server.pickEntrySide(hexLabel, legion);
                }
                legion.clearAllEntrySides(hexLabel);
                if (side == 1 || side == 3 || side == 5)
                {
                    legion.setEntrySide(hexLabel, side);
                }
            }

            // Unless a PickEntrySide was cancelled or disallowed,
            // execute the move.
            if (!isOccupied(hexLabel) ||
                legion.getNumEntrySides(hexLabel) == 1)
            {
                // If the legion teleported, reveal a lord.
                if (legion.getTeleported(hexLabel))
                {
                    // If it was a Titan teleport, that
                    // lord must be the titan.
                    if (isOccupied(hexLabel))
                    {
                        legion.revealCreatures(Creature.titan, 1);
                    }
                    else
                    {
                        legion.revealTeleportingLord(
                            server.getClientOption(playerName, 
                                Options.autoPlay) || server.getClientOption(
                                playerName, Options.allStacksVisible));
                    }
                }
                legion.moveToHex(hex);
                moved = true;
            }
        }
        return moved;
    }


    public void engage(String hexLabel)
    {
        // Do not allow clicking on engagements if one is
        // already being resolved.
        if (isEngagement(hexLabel) && !engagementInProgress)
        {
            engagementInProgress = true;
            Player player = getActivePlayer();
            Legion attacker = getFirstFriendlyLegion(hexLabel, player);
            Legion defender = getFirstEnemyLegion(hexLabel, player);

            attacker.sortCritters();
            defender.sortCritters();

            if (defender.canFlee())
            {
                // Fleeing gives half points and denies the
                // attacker the chance to summon an angel.

                boolean flees;
                if (server.getClientOption(defender.getPlayer().getName(),
                    Options.autoFlee))
                {
                    flees = defender.getPlayer().aiFlee(defender, attacker);
                }
                else
                {
                    flees = server.askFlee(defender, attacker);
                }
                if (flees)
                {
                    handleConcession(defender, attacker, true);
                    return;
                }
            }

            // The attacker may concede now without
            // allowing the defender a reinforcement.
            boolean concedes = false;
            if (server.getClientOption(attacker.getPlayer().getName(),
                Options.autoFlee))
            {
                concedes = attacker.getPlayer().aiConcede(attacker, defender);
            }
            else
            {
                concedes = server.askConcede(attacker, defender);
            }

            if (concedes)
            {
                handleConcession(attacker, defender, false);
                return;
            }

            offers[Battle.DEFENDER] = new HashSet();
            offers[Battle.ATTACKER] = new HashSet();
            // XXX Skip negotiation for now, and just do the fight.
            // server.twoNegotiate(attacker, defender);
            fight(hexLabel);
        }
    }


    public void negotiate(String playerName, NegotiationResults offer)
    {
        // If it's too late to negotiate, just throw this away.
        if (battleInProgress)
        {
            return;
        }

        int thisPlayerSet;
        if (playerName.equals(getActivePlayerName()))
        {
            thisPlayerSet = Battle.ATTACKER;
        }
        else
        {
            thisPlayerSet = Battle.DEFENDER;
        }
        int otherSet = (thisPlayerSet + 1) & 1;

        // If this player wants to fight, cancel negotiations.
        if (offer.isFight())
        {
            Legion attacker = getLegionByMarkerId(offer.getAttackerId());
            String hexLabel = attacker.getCurrentHexLabel();
            fight(hexLabel);
        }

        // If this offer matches an earlier one from the other player,
        // settle the engagement.
        else if (offers[otherSet].contains(offer))
        {
            handleNegotiation(offer);
        }

        // Otherwise remember this offer and continue.
        else
        {
            offers[thisPlayerSet].add(offer);
        }
    }


    public void fight(String hexLabel)
    {
        if (!battleInProgress)
        {
            Player player = getActivePlayer();
            Legion attacker = getFirstFriendlyLegion(hexLabel, player);
            Legion defender = getFirstEnemyLegion(hexLabel, player);

            // If the second player clicks Fight from the negotiate
            // dialog late, just exit.
            if (attacker == null || defender == null)
            {
                return;
            }

            battleInProgress = true;

            // Reveal both legions to all players.
            attacker.revealAllCreatures();
            defender.revealAllCreatures();

            battle = new Battle(this, attacker.getMarkerId(),
                defender.getMarkerId(), Battle.DEFENDER, hexLabel,
                1, Battle.MOVE);
            battle.init();
        }
    }


    private void handleConcession(Legion loser, Legion winner, boolean fled)
    {
        // Figure how many points the victor receives.
        int points = loser.getPointValue();
        if (fled)
        {
            points /= 2;
            Log.event("Legion " + loser.getLongMarkerName() +
                " flees from legion " + winner.getLongMarkerName());
        }
        else
        {
            Log.event("Legion " + loser.getLongMarkerName() +
                " concedes to legion " + winner.getLongMarkerName());
        }

        // Need to grab the player reference before the legion is
        // removed.
        Player losingPlayer = loser.getPlayer();

        // Remove the dead legion.
        loser.remove();

        // Add points, and angels if necessary.
        winner.addPoints(points);
        // Remove any fractional points.
        winner.getPlayer().truncScore();

        // If this was the titan stack, its owner dies and gives half
        // points to the victor.
        if (loser.hasTitan())
        {
            losingPlayer.die(winner.getPlayerName(), true);
        }

        // Unselect and repaint the hex.
        String hexLabel = winner.getCurrentHexLabel();
        server.allUnselectHexByLabel(hexLabel);

        // No recruiting or angel summoning is allowed after the
        // defender flees or the attacker concedes before entering
        // the battle.
        engagementInProgress = false;
        server.allUpdateStatusScreen();
        server.allHighlightEngagements();
        aiSetupEngagements();
    }


    private void handleNegotiation(NegotiationResults results)
    {
        Legion attacker = getLegionByMarkerId(results.getAttackerId());
        Legion defender = getLegionByMarkerId(results.getAttackerId());

        if (results.isMutual())
        {
             // Remove both legions and give no points.
             attacker.remove();
             defender.remove();

             Log.event(attacker.getLongMarkerName() + " and " +
                 defender.getLongMarkerName() +
                 " agree to mutual elimination");

             // If both Titans died, eliminate both players.
             if (attacker.hasTitan() && defender.hasTitan())
             {
                 // Make defender die first, to simplify turn advancing.
                 defender.getPlayer().die(null, false);
                 attacker.getPlayer().die(null, true);
             }

             // If either was the titan stack, its owner dies and gives
             // half points to the victor.
             else if (attacker.hasTitan())
             {
                 attacker.getPlayer().die(defender.getPlayerName(), true);
             }

             else if (defender.hasTitan())
             {
                 defender.getPlayer().die(attacker.getPlayerName(), true);
             }
        }
        else
        {
            // One legion was eliminated during negotiations.
            Legion winner = getLegionByMarkerId(results.getWinnerId());
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
            Set winnerLosses = results.getWinnerLosses();
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
            Log.event(log.toString());

            int points = loser.getPointValue();

            // Remove the losing legion.
            loser.remove();

            // Add points, and angels if necessary.
            winner.addPoints(points);

            Log.event("Legion " + loser.getLongMarkerName() +
               " is eliminated by legion " + winner.getLongMarkerName() +
               " via negotiation");

            // If this was the titan stack, its owner dies and gives half
            // points to the victor.
            if (loser.hasTitan())
            {
                loser.getPlayer().die(winner.getPlayerName(), true);
            }

            if (winner == defender)
            {
                if (defender.canRecruit())
                {
                    // If the defender won the battle by agreement,
                    // he may recruit.
                    Creature recruit;
                    Player player = defender.getPlayer();
                    if (server.getClientOption(player.getName(),
                        Options.autoRecruit))
                    {
                        recruit = player.aiReinforce(defender);
                    }
                    else
                    {
                        String recruitName = server.pickRecruit(defender);
                        recruit = Creature.getCreatureByName(recruitName);
                    }
                    if (recruit != null)
                    {
                        doRecruit(recruit, defender);
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
        engagementInProgress = false;
        server.allUpdateStatusScreen();
        if (!summoningAngel)
        {
            server.allHighlightEngagements();
        }
        aiSetupEngagements();
    }


    /** Return a list of all players' legions. */
    public ArrayList getAllLegions()
    {
        ArrayList list = new ArrayList();
        for (Iterator it = players.iterator(); it.hasNext();)
        {
            Player player = (Player)it.next();
            list.addAll(player.getLegions());
        }
        return list;
    }

    /** Return a list of all players' legions' marker ids. */
    public List getAllLegionIds()
    {
        List list = new ArrayList();
        for (Iterator it = players.iterator(); it.hasNext();)
        {
            Player player = (Player)it.next();
            list.addAll(player.getLegionIds());
        }
        return list;
    }

    /** Return a list of all legions not belonging to player. */
    public ArrayList getAllEnemyLegions(Player player)
    {
        ArrayList list = new ArrayList();
        for (Iterator it = players.iterator(); it.hasNext();)
        {
            Player nextPlayer = (Player)it.next();
            if (nextPlayer != player)
            {
                list.addAll(nextPlayer.getLegions());
            }
        }
        return list;
    }

    /** Return a list of ids for all legions not belonging to player. */
    public List getAllEnemyLegionIds(Player player)
    {
        List list = new ArrayList();
        for (Iterator it = players.iterator(); it.hasNext();)
        {
            Player nextPlayer = (Player)it.next();
            if (nextPlayer != player)
            {
                list.addAll(nextPlayer.getLegionIds());
            }
        }
        return list;
    }

    public Legion getLegionByMarkerId(String markerId)
    {
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            Legion legion = player.getLegionByMarkerId(markerId);
            if (legion != null)
            {
                return legion;
            }
        }
        return null;
    }

    public Player getPlayerByMarkerId(String markerId)
    {
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            Legion legion = player.getLegionByMarkerId(markerId);
            if (legion != null)
            {
                return player;
            }
        }
        return null;
    }


    /** Get the average point value of all legions in the game. This is
     *  somewhat of a cheat. */
    public int getAverageLegionPointValue()
    {
        int total = 0;
        ArrayList legions = getAllLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            total += legion.getPointValue();
        }
        return total / legions.size();
    }


    public int getNumLegions(String hexLabel)
    {
        int count = 0;
        Iterator it = getAllLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                count++;
            }
        }
        return count;
    }

    public boolean isOccupied(String hexLabel)
    {
        Iterator it = getAllLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                return true;
            }
        }
        return false;
    }

    public Legion getFirstLegion(String hexLabel)
    {
        Iterator it = getAllLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                return legion;
            }
        }
        return null;
    }

    public ArrayList getLegionMarkerIds(String hexLabel)
    {
        ArrayList markerIds = new ArrayList();
        Iterator it = getAllLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                markerIds.add(legion.getMarkerId());
            }
        }
        return markerIds;
    }

    public int getNumFriendlyLegions(String hexLabel, Player player)
    {
        int count = 0;
        Iterator it = player.getLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                count++;
            }
        }
        return count;
    }

    public Legion getFirstFriendlyLegion(String hexLabel, Player player)
    {
        Iterator it = player.getLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                return legion;
            }
        }
        return null;
    }

    public ArrayList getFriendlyLegions(String hexLabel, Player player)
    {
        ArrayList legions = new ArrayList();
        Iterator it = player.getLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                legions.add(legion);
            }
        }
        return legions;
    }

    public int getNumEnemyLegions(String hexLabel, Player player)
    {
        String playerName = player.getName();
        int count = 0;
        Iterator it = getAllEnemyLegions(player).iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                count++;
            }
        }
        return count;
    }

    public Legion getFirstEnemyLegion(String hexLabel, Player player)
    {
        String playerName = player.getName();
        Iterator it = getAllEnemyLegions(player).iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                return legion;
            }
        }
        return null;
    }

    private GameListener m_oListener = new GameAdapter()
    {
        public void legionChange(LegionEvent evt)
        {
            System.out.println("Game.GameListener.legionChange: " + evt);
               fireEvent(evt);
        }
    };

    public GameListener getListener()
    {
        return m_oListener;
    }
}
