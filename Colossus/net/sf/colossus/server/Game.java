package net.sf.colossus.server;


import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

import com.werken.opt.CommandLine;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.client.MasterBoard;
import net.sf.colossus.client.MasterHex;
import net.sf.colossus.client.Proposal;
import net.sf.colossus.client.BattleMap;
import net.sf.colossus.parser.TerrainRecruitLoader;
import net.sf.colossus.client.VariantSupport;
import net.sf.colossus.client.HexMap;

/**
 * Class Game gets and holds high-level data about a Titan game.
 * @version $Id$
 * @author David Ripton
 * @author Bruce Sherrod
 * @author Romain Dolbeau
 */

public final class Game
{
    private java.util.List players = new ArrayList(6);
    private int activePlayerNum;
    private int turnNumber;    // Advance when every player has a turn
    private boolean engagementInProgress;
    private boolean battleInProgress;
    private boolean summoning;
    private boolean reinforcing;
    private boolean acquiring;
    private boolean pendingAdvancePhase;
    private boolean loadingGame;
    private boolean gameOver;
    private Battle battle;
    private static Random random = new Random();
    private Caretaker caretaker = new Caretaker(this);
    private int phase;
    private Server server;
    // Negotiation
    private Set [] proposals = new HashSet[2];

    private LinkedList colorPickOrder = new LinkedList();
    private Set colorsLeft;
    private PhaseAdvancer phaseAdvancer = new GamePhaseAdvancer();
    private Options options = new Options(Constants.optionsServerName);

    // XXX Need to clear cl after initialization is done? 
    private CommandLine cl = null;

    /** Server port number. */
    private int port = Constants.defaultPort;


    Game()
    {
    }

    Game(CommandLine cl)
    {
        this.cl = cl;
    }


    private void initServer()
    {
Log.debug("Called Game.initServer()");
        if (server != null)
        {
            server.disposeAllClients();
        }
        server = new Server(this, port);
        server.initSocketServer();
    }


    /**
     * Make a deep copy for the AI to use.
     * This preserves all game state but throws away a lot of the UI stuff
     */
    Game AICopy()
    {
        Game game2 = new Game();

        for (int i = 0; i < players.size(); i++)
        {
            Player player = ((Player)players.get(i)).AICopy(game2);
            game2.players.add(i, player);
        }
        game2.activePlayerNum = activePlayerNum;
        game2.turnNumber = turnNumber;
        if (battle != null)
        {
            game2.battle = battle.AICopy(game2);
        }
        game2.caretaker = caretaker.AICopy();
        game2.phase = phase;
        game2.engagementInProgress = engagementInProgress;
        game2.battleInProgress = battleInProgress;
        game2.summoning = summoning;
        game2.reinforcing = reinforcing;
        game2.acquiring = acquiring;
        game2.server = server;

        return game2;
    }

    private void clearFlags()
    {
        engagementInProgress = false;
        battleInProgress = false;
        summoning = false;
        reinforcing = false;
        acquiring = false;
        pendingAdvancePhase = false;
        gameOver = false;
        loadingGame = false;
    }


    /** Modify options from command-line args if possible.  Clear
     *  options to abort if something is wrong. */
    private void setupOptionsFromCommandLine(CommandLine cl)
    {
        int numHumans = 0;
        int numAIs = 0;
        int numNetworks = 0;

        if (cl.optIsSet('v'))
        {
            String variantName = cl.getOptValue('v');
            // XXX Check that this variant is in the list.
            options.setOption(Options.variant, variantName);
        }
        if (cl.optIsSet('q'))
        {
            options.setOption(Options.autoQuit, true);
        }
        if (cl.optIsSet('u'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('u');
            numHumans = Integer.parseInt(buf);
        }
        if (cl.optIsSet('i'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('i');
            numAIs = Integer.parseInt(buf);
        }
        if (cl.optIsSet('n'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('n');
            numNetworks = Integer.parseInt(buf);
        }
        if (cl.optIsSet('p'))
        {
            String buf = cl.getOptValue('p');
            port = Integer.parseInt(buf);
        }
        // Quit if values are bogus.
        if (numHumans < 0 || numAIs < 0 || numNetworks < 0 ||
            numHumans + numAIs + numNetworks > Constants.MAX_PLAYERS)
        {
            Log.error("Illegal number of players");
            options.clear();
            return;
        }

        for (int i = 0; i < numHumans; i++)
        {
            String name = null;
            if (i == 0)
            {
                name = Constants.username;
            }
            else
            {
                name = Constants.byColor + i;
            }
            options.setOption(Options.playerName + i, name);
            options.setOption(Options.playerType + i, Constants.human);
        }
        for (int j = numHumans; j < numNetworks + numHumans; j++)
        {
            String name = Constants.byClient + j;
            options.setOption(Options.playerName + j, name);
            options.setOption(Options.playerType + j, Constants.network);
        }
        for (int k = numHumans + numNetworks; 
            k < numAIs + numHumans + numNetworks; k++)
        {
            String name = Constants.byColor + k;
            options.setOption(Options.playerName + k, name);
            options.setOption(Options.playerType + k, Constants.defaultAI);
        }
    }

    private void addPlayersFromOptions()
    {
        for (int i = 0; i < Constants.MAX_PLAYERS; i++)
        {
            String name = options.getStringOption(Options.playerName + i);
            String type = options.getStringOption(Options.playerType + i);

            if (name != null && type != null && !type.equals(Constants.none))
            {
                addPlayer(name, type);
                Log.event("Add " + type + " player " + name);
            }
        }
        // No longer need the player name and type options. 
        options.clearPlayerInfo();
    }


    /** Start a new game. */
    void newGame()
    {
        clearFlags();
        turnNumber = 1;
        phase = Constants.SPLIT;
        caretaker.resetAllCounts();
        players.clear();

        // Need to load options early, so we can use them to initialize
        // GetPlayers, and easily override them from command line.
        options.loadOptions();

        if (cl != null)
        {
            setupOptionsFromCommandLine(cl);
        }

        // Load game options 'l' and 'z' are handled separately.
        if (!cl.optIsSet('g')) 
        {
            new GetPlayers(new JFrame(), options);
        }

        if (options.isEmpty())
        {
            // Bad input, or user selected Quit.
            dispose();
        }

        // See if user hit the Load game button, and we should
        // load a game instead of starting a new one.
        String filename = options.getStringOption(Constants.loadGame);
        if (filename != null && filename.length() > 0)
        {
            options.clearPlayerInfo();
            loadGame(filename);
            return;
        }

        options.saveOptions();
        VariantSupport.loadVariant(options.getStringOption(Options.variant));
        Log.event("Starting new game");
        addPlayersFromOptions();
        initServer();
    }

    void newGame2()
    {
Log.debug("Called Game.newGame2()");
        // We need to set the autoPlay option before loading the board,
        // so that we can avoid showing boards for AI players.
        syncAutoPlay();
        syncOptions();
        server.allInitBoard();
        assignTowers();

        // Renumber players in descending tower order.
        Collections.sort(players);
        activePlayerNum = 0;
        assignColors();
    }


    private boolean nameIsTaken(String name)
    {
        for (int i = 0; i < getNumPlayers(); i++)
        {
            Player player = (Player)players.get(i);
            if (player.getName().equals(name))
            {
                return true;
            }
        }
        return false;
    }

    /** If the name is taken, add random digits to the end. */ 
    String getUniqueName(final String name)
    {
        if (!nameIsTaken(name))
        {
            return name;
        }
        return getUniqueName(name + rollDie());
    }


    /** Return the index of the correct player for a new remote client.
     *  If loading a game, this is the network player with a matching
     *  player name.  If a new game, it's the first network player whose
     *  name is still set to <By client> */
    int findNetworkSlot(final String playerName)
    {
        for (int i = 0; i < getNumPlayers(); i++)
        {
            Player player = (Player)players.get(i);
            if (player.getType().endsWith(Constants.network))
            {
                if (isLoadingGame())
                {
                   if (playerName.equals(player.getName()))
                   {
                       return i;
                   }
                }
                else
                {
                   if (player.getName().startsWith(Constants.byClient))
                   {
                       return i;
                   }
                }
            }
        }
        return -1;
    }


    private void syncAutoPlay()
    {
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            server.oneSetOption(player.getName(), Options.autoPlay,
                player.isAI());
        }
    }

    /** Send all current game option values to all clients. */
    private void syncOptions()
    {
        Enumeration en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            String value = options.getStringOption(name);
            server.allSetOption(name, value);
        }
    }

    private void assignColors()
    {
        colorsLeft = new HashSet();
        for (int i = 0; i < Constants.colorNames.length; i++)
        {
            colorsLeft.add(Constants.colorNames[i]);
        }

        // Let human players pick colors first, followed by AI players.
        // Within each group, players pick colors in ascending tower order.
        colorPickOrder.clear(); 

        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            Player player = (Player)players.get(i);
            if (player.isHuman())
            {
                colorPickOrder.add(player.getName());
            }
        }
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            Player player = (Player)players.get(i);
            if (player.isAI())
            {
                colorPickOrder.add(player.getName());
            }
        }

        nextPickColor();
    }

    private void nextPickColor()
    {
        if (colorPickOrder.size() > 1)
        {
            String playerName = (String)colorPickOrder.getFirst();
            server.askPickColor(playerName, colorsLeft);
        }
        else if (colorPickOrder.size() == 1)
        {
            String playerName = (String)colorPickOrder.getFirst();
            assignColor(playerName, (String)(colorsLeft.iterator().next()));
        }
        else
        {
            // All players are done picking colors; continue.
            newGame3();
        }
    }

    void assignColor(String playerName, String color)
    {
        Player player = getPlayer(playerName);
        colorPickOrder.remove(playerName);
        colorsLeft.remove(color);
        player.setColor(color);
        if (player.getName().startsWith(Constants.byColor))
        {
            server.setPlayerName(player.getName(), color);
            player.setName(color);
        }
        Log.event(player.getName() + " chooses color " + color);
        player.initMarkersAvailable();

        nextPickColor();
    }

    String getNextColorPicker()
    {
        return (String)colorPickOrder.getFirst();
    }

    /** Done picking player colors; proceed to start game. */
    private void newGame3()
    {
        server.allUpdatePlayerInfo();

        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            // XXX Need to let player pick first marker along with color.
            placeInitialLegion(player, player.getFirstAvailableMarker());
            server.allUpdatePlayerInfo();
        }

        server.allTellAllLegionLocations();
        autoSave();
        setupPhase();
        caretaker.fullySyncDisplays();
    }


    /** Randomize towers by rolling dice and rerolling ties. */
    private void assignTowers()
    {
        boolean balanced = getOption(Options.balancedTowers);

        int numPlayers = getNumPlayers();
        String[] playerTower = new String[numPlayers];
        Set towerSet = MasterBoard.getTowerSet();
        ArrayList towerList = new ArrayList();

        Iterator it = towerSet.iterator();
        while (it.hasNext())
        { // first, fill the list with all Label
            towerList.add(it.next());
        }

        if (balanced)
        {
            towerList = getBalancedTowers(numPlayers, towerList);
        }

        int playersLeft = numPlayers - 1;

        while ((playersLeft >= 0) && (!towerList.isEmpty()))
        {
            int which = rollDie(towerList.size());
            playerTower[playersLeft] = (String)towerList.remove(which - 1);
            playersLeft--;
        }

        for (int i = 0; i < numPlayers; i++)
        {
            Player player = getPlayer(i);
            Log.event(player.getName() + " gets tower " + playerTower[i]);
            player.setTower(playerTower[i]);
        }
    }

    /** Return a list with a balanced order of numPlayer towers chosen
        from towerList, which must hold numeric strings. */
    static ArrayList getBalancedTowers(int numPlayers, 
        final ArrayList towerList)
    {
        int numTowers = towerList.size();
        if (numPlayers > numTowers)
        {
            Log.error("More players than towers!");
            return towerList;
        }

        // Make a sorted copy, converting String to Integer.
        ArrayList numericList = new ArrayList();
        Iterator it = towerList.iterator();
        while (it.hasNext())
        {
            String s = (String)it.next();
            Integer i = new Integer(s);
            numericList.add(i);
        }
        Collections.sort(numericList);

        double towersPerPlayer = (double)numTowers / numPlayers;

        // First just find a balanced sequence starting at zero.
        double counter = 0.0;
        int numDone = 0;
        ArrayList sequence = new ArrayList();
        // Prevent floating-point roundoff error.
        double epsilon = 0.0000001;
        while (numDone < numPlayers)
        {
            sequence.add(new Integer((int)Math.floor(counter + epsilon)));
            numDone++;
            counter += towersPerPlayer;
        }

        // Pick a random starting point.  (Zero-based)
        int startingTower = rollDie(numTowers) - 1;

        // Offset the sequence by the starting point, and get only
        // the number of starting towers we need.
        ArrayList returnList = new ArrayList();
        it = sequence.iterator();
        numDone = 0;
        while (it.hasNext() && numDone < numPlayers)
        {
            Integer raw = (Integer)it.next();
            int cooked = (raw.intValue() + startingTower) % numTowers;
            Integer numericLabel = (Integer)numericList.get(cooked);
            returnList.add(numericLabel.toString());
            numDone++;
        }
        return returnList;
    }

    Caretaker getCaretaker()
    {
        return caretaker;
    }

    Server getServer()
    {
        return server;
    }


    void addPlayer(String name, String type)
    {
        Player player = new Player(name, this);
        player.setType(type);
        players.add(player);
    }


    int getNumPlayers()
    {
        return players.size();
    }

    int getNumLivingPlayers()
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

    Player getActivePlayer()
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

    String getActivePlayerName()
    {
        return getActivePlayer().getName();
    }

    int getActivePlayerNum()
    {
        return activePlayerNum;
    }

    /** for AI only */
    void setActivePlayerNum(int i)
    {
        activePlayerNum = i;
    }

    Player getPlayer(int i)
    {
        return (Player)players.get(i);
    }

    Collection getPlayers()
    {
        return Collections.unmodifiableCollection(players);
    }

    Player getPlayer(String name)
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


    int getNumPlayersRemaining()
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

    int getNumHumansRemaining()
    {
        int remaining = 0;
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            if (player.isHuman() && !player.isDead())
            {
                remaining++;
            }
        }
        return remaining;
    }


    Player getWinner()
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


    void checkForVictory()
    {
        int remaining = getNumPlayersRemaining();
        switch (remaining)
        {
            case 0:
                Log.event("Draw");
                server.allTellGameOver("Draw");
                setGameOver(true);
                break;

            case 1:
                String winnerName = getWinner().getName();
                Log.event(winnerName + " wins");
                server.allTellGameOver(winnerName + " wins");
                setGameOver(true);
                break;

            default:
                break;
        }
    }


    boolean isOver()
    {
        return gameOver;
    }

    private void setGameOver(boolean gameOver)
    {
        this.gameOver = gameOver;
        if (gameOver && getOption(Options.autoQuit))
        {
            dispose();
        }
        if (gameOver)
        {
            server.allFullyUpdateAllLegionContents();
        }
    }


    boolean isLoadingGame()
    {
        return loadingGame;
    }


    int getPhase()
    {
        return phase;
    }

    /** Advance to the next phase, only if the passed oldPhase and playerName
     *  are current. */
    synchronized void advancePhase(final int oldPhase, final String playerName)
    {
        if (oldPhase != phase || pendingAdvancePhase ||
            !playerName.equals(getActivePlayerName()))
        {
            Log.error("Called advancePhase illegally");
            return;
        }
        if (getOption(Options.autoStop) && getNumHumansRemaining() < 1)
        {
            Log.event("Not advancing because no humans remain");
            server.allTellGameOver("All humans eliminated");
            setGameOver(true);
            return;
        }
        phaseAdvancer.advancePhase();
    }

    /** Wrap the complexity of phase advancing. */
    class GamePhaseAdvancer extends PhaseAdvancer
    {
        /** Advance to the next phase, only if the passed oldPhase and 
         *  playerName are current. */
        void advancePhase()
        {
            pendingAdvancePhase = true;
            int delay = getDelay(server, getActivePlayer().isHuman());
            startTimer(delay);
        }

        /** Advance to the next phase, with no error checking. */
        void advancePhaseInternal()
        {
            phase++;
            if (phase > Constants.MUSTER ||
                (getActivePlayer().isDead() && getNumLivingPlayers() > 0))
            {
                advanceTurn();
            }
            else
            {
                Log.event("Phase advances to " + 
                    Constants.getPhaseName(phase));
            }
            pendingAdvancePhase = false;
            setupPhase();
        }

        void advanceTurn()
        {
            clearFlags();
            activePlayerNum++;
            if (activePlayerNum == getNumPlayers())
            {
                activePlayerNum = 0;
                turnNumber++;
            }
            phase = Constants.SPLIT;
            if (getActivePlayer().isDead() && getNumLivingPlayers() > 0)
            {
                advanceTurn();
            }
            else
            {
                Log.event(getActivePlayerName() + "'s turn, number " + 
                    turnNumber);
                autoSave();
            }
        }
    }


    private void setupPhase()
    {
        switch (getPhase())
        {
            case Constants.SPLIT:
                setupSplit();
                break;
            case Constants.MOVE:
                setupMove();
                break;
            case Constants.FIGHT:
                setupFight();
                break;
            case Constants.MUSTER:
                setupMuster();
                break;
            default:
                Log.error("Bogus phase");
        }
    }


    private void setupSplit()
    {
        Player player = getActivePlayer();
        if (player == null)
        {
            Log.error("No players");
            dispose();
        }
        player.resetTurnState();
        server.allSetupSplit();

        // If there are no markers available, or no legions tall enough
        // to split, skip forward to movement.
        if (player.getNumMarkersAvailable() == 0 ||
            player.getMaxLegionHeight() < 4)
        {
            advancePhase(Constants.SPLIT, player.getName());
        }
        else
        {
            player.aiSplit();
        }
    }


    private void setupMove()
    {
        Player player = getActivePlayer();
        player.rollMovement();
        server.allSetupMove();

        player.aiMasterMove();
    }

    private void setupFight()
    {
        // If there are no engagements, move forward to the muster phase.
        if (!summoning && !reinforcing && !acquiring &&
            findEngagements().size() == 0)
        {
            advancePhase(Constants.FIGHT, getActivePlayerName());
        }
        else
        {
            server.allSetupFight();
            kickEngagements();
        }
    }


    private void setupMuster()
    {
        Player player = getActivePlayer();
        player.removeEmptyLegions();

        // If this player was eliminated in combat, or can't recruit
        // anything, advance to the next turn.
        if (player.isDead() || !player.canRecruit())
        {
            advancePhase(Constants.MUSTER, player.getName());
        }
        else
        {
            server.allSetupMuster();
        }
    }


    int getTurnNumber()
    {
        return turnNumber;
    }


    /** Create a text file describing this game's state, in
     *  file filename, or <saveDirName>/<time>.sav if null.
     *  We don't use XML yet because we don't want to require 
     *  JDK 1.3.x users to install a parser.
     *  Format:
     *     Savegame version string
     *     Variant filename
     *     Variant name
     *     Number of players
     *     Turn number
     *     Whose turn
     *     Current phase
     *     Creature counts

     *     Player 1:
     *         Name
     *         Type
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
    void saveGame(final String filename)
    {
        String fn = null; 
        if (filename == null || filename.equals("null"))
        {
            Date date = new Date();
            File savesDir = new File(Constants.saveDirname);
            if (!savesDir.exists() || !savesDir.isDirectory())
            {
                Log.event("Trying to make directory " + Constants.saveDirname);
                if (!savesDir.mkdirs())
                {
                    Log.error("Could not create saves directory");
                    return;
                }
            }

            fn = Constants.saveDirname + date.getTime() + 
                Constants.saveExtension;
        }
        else
        {
            fn = new String(filename);
        }

        FileWriter fileWriter;
        try
        {
            fileWriter = new FileWriter(fn);
        }
        catch (IOException e)
        {
            Log.error(e.toString());
            Log.error("Couldn't open " + fn);
            return;
        }
        PrintWriter out = new PrintWriter(fileWriter);

        out.println(Constants.saveGameVersion);
        out.println(VariantSupport.getVarName());
        out.println(VariantSupport.getVarDirectory());
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

            Set markerIds = player.getMarkersAvailable();
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
            Log.error("Write error " + fn);
            return;
        }
    }

    private void dumpLegion(PrintWriter out, Legion legion, boolean inBattle)
    {
        out.println(legion.getMarkerId());
        out.println(legion.getCurrentHexLabel());
        out.println(legion.getStartingHexLabel());
        out.println(legion.hasMoved());
        out.println(legion.getEntrySide());
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
            if (inBattle)
            {
                out.println(critter.getHits());
                out.println(critter.getCurrentHexLabel());
                out.println(critter.getStartingHexLabel());
                out.println(critter.hasStruck());
            }
        }
    }

    void autoSave()
    {            
        if (getOption(Options.autosave))
        {
            saveGame(null);
        }
    }


    /** Try to load a game from saveDirName/filename.  If the filename is
     *  "--latest" then load the latest savegame found in saveDirName. */
    void loadGame(String filename)
    {
        options.loadOptions();

        File file = null;
        if (filename.equals("--latest"))
        {
            File dir = new File(Constants.saveDirname);
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
            file = new File(Constants.saveDirname + 
                latestSaveFilename(filenames));
        }
        else
        {
            file = new File(Constants.saveDirname + filename);
        }

        try
        {
            Log.event("Loading game from " + file);
            FileReader fileReader = new FileReader(file);
            BufferedReader in = new BufferedReader(fileReader);
            String buf;

            buf = in.readLine();
            if (!buf.equals(Constants.saveGameVersion))
            {
                Log.error("Can't load this savegame version.");
                dispose();
            }

            // Reset flags that are not in the savegame file.
            clearFlags();
            loadingGame = true;
            
            VariantSupport.loadVariant(in.readLine(), in.readLine());

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
                caretaker.setCount(creature, count);
            }

            players.clear();
            if (battle != null)
            {
                server.allCleanupBattle();
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

                String tower= in.readLine();
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
                    readLegion(in, player, false);
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
                Set carryTargets = new HashSet();
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
                    activeLegionNum = Constants.ATTACKER;
                }
                else
                {
                    activeLegionNum = Constants.DEFENDER;
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

            initServer();
            // Remaining stuff has been moved to loadGame2()
        }
        // FileNotFoundException, IOException, NumberFormatException
        catch (Exception e)
        {
            Log.error(e + " Tried to load corrupt savegame.");
            e.printStackTrace();
            dispose();
        }
    }

    Legion readLegion(BufferedReader in, Player player,
        boolean inBattle) throws IOException
    {
        String markerId = in.readLine();
        String currentHexLabel = in.readLine();
        String startingHexLabel = in.readLine();

        String buf = in.readLine();
        boolean moved = Boolean.valueOf(buf).booleanValue();

        buf = in.readLine();
        int entrySide = Integer.valueOf(buf).intValue();

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
                Creature.getCreatureByName(buf), null, this);

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
        legion.setEntrySide(entrySide);
        legion.addToBattleTally(battleTally);

        return legion;
    }

    void loadGame2()
    {
        server.allSetColor();

        // We need to set the autoPlay option before loading the board,
        // so that we can avoid showing boards for AI players.
        syncAutoPlay();
        syncOptions();

        server.allUpdatePlayerInfo();

        if (getOption(Options.allStacksVisible))
        {
            server.allFullyUpdateAllLegionContents();
        }
        else
        {
            server.allFullyUpdateLegionHeights();
            server.allFullyUpdateOwnLegionContents();
        }

        server.allInitBoard();
        server.allTellAllLegionLocations();

        server.allSetupTurnState();
        setupPhase();
        caretaker.fullySyncDisplays();
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


    /** Return a list of eligible recruits, as Creatures. */
    java.util.List findEligibleRecruits(String markerId, String hexLabel)
    {
        Legion legion = getLegionByMarkerId(markerId);
        java.util.List recruits;

        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        char terrain = hex.getTerrain();

        recruits = new ArrayList();
        java.util.List tempRecruits = 
            TerrainRecruitLoader.getPossibleRecruits(terrain);
        java.util.List recruiters = 
            TerrainRecruitLoader.getPossibleRecruiters(terrain);

        ListIterator lit = tempRecruits.listIterator();
            
        while (lit.hasNext())
        {
            Creature creature = (Creature)lit.next();
            ListIterator liter = recruiters.listIterator();
            while (liter.hasNext())
            {
                Creature lesser = (Creature)liter.next();
                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser, 
                    creature, terrain) <= legion.numCreature(lesser)) &&
                    (recruits.indexOf(creature) == -1))
                {
                    recruits.add(creature);
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

    /** Return a list of eligible recruiter creatures. */
    java.util.List findEligibleRecruiters(String markerId, String recruitName)
    {
        java.util.List recruiters;
        Creature recruit = Creature.getCreatureByName(recruitName);
        if (recruit == null)
        {
            return new ArrayList();
        }

        Legion legion = getLegionByMarkerId(markerId);
        String hexLabel = legion.getCurrentHexLabel();
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        char terrain = hex.getTerrain();

        recruiters = TerrainRecruitLoader.getPossibleRecruiters(terrain);
        Iterator it = recruiters.iterator();
        while (it.hasNext())
        {
            Creature possibleRecruiter = (Creature)it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain);
            if (needed < 1 || needed > legion.numCreature(possibleRecruiter))
            {
                // Zap this possible recruiter.
                it.remove();
            }
        }

        return recruiters;
    }

    /** Return true if every single creature in legion is eligible
     *  to recruit this recruit in this terrain.  XXX We really should
     *  explicitly check for "Anything" instead. */
    private boolean allCanRecruit(Legion legion, Creature recruit)
    {
        java.util.List recruiters = findEligibleRecruiters(
            legion.getMarkerId(), recruit.getName());
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Creature creature = critter.getCreature();
            if (!recruiters.contains(creature))
            {
                return false;
            }
        }
        return true;
    }

    /** Add recruit to legion. */
    void doRecruit(Legion legion, Creature recruit, Creature recruiter)
    {
        // Check for recruiter legality.
        java.util.List recruiters = findEligibleRecruiters(
            legion.getMarkerId(), recruit.getName());
        if (recruit == null)
        {
            Log.error("null recruit in Game.doRecruit()");
            return;
        }
        if (recruiter == null)
        {
            // If anything can recruit here, then this is okay.
            if (!allCanRecruit(legion, recruit))
            {
                Log.error("null recruiter in Game.doRecruit()");
                // XXX Let it go for now  Should return later
            }
            else
            {
                Log.debug("null recruiter okay");
            }
        }
        else if (!recruiters.contains(recruiter))
        {
            Log.error("Illegal recruiter " + recruiter.getName() + 
                " for recruit " + recruit.getName());
            return;
        }

        legion.addCreature(recruit, true);
        MasterHex hex = legion.getCurrentHex();
        int numRecruiters = 0;
        if (recruiter != null)
        {
            // Mark the recruiter(s) as visible.
            numRecruiters = TerrainRecruitLoader.numberOfRecruiterNeeded(
                recruiter, recruit, hex.getTerrain());
        }

        Log.event("Legion " + legion.getLongMarkerName() + " in " +
            hex.getDescription() + " recruits " + recruit.getName() +
            " with " + (numRecruiters == 0 ? "nothing" :
            numRecruiters + " " + (numRecruiters > 1 ?
            recruiter.getPluralName() : recruiter.getName())));

        // Recruits are one to a customer.
        legion.setRecruitName(recruit.getName());

        reinforcing = false;
    }
    
    
    /** Return a list of names of angel types that can be acquired. */
    java.util.List findEligibleAngels(Legion legion, int score)
    {
        if (legion.getHeight() >= 7)
        {
            return null;
        }
        java.util.List recruits = new ArrayList();
        char t = legion.getCurrentHex().getTerrain();
        java.util.List allRecruits = 
            TerrainRecruitLoader.getRecruitableAcquirableList(t, score);
        java.util.Iterator it = allRecruits.iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            if (caretaker.getCount(Creature.getCreatureByName(name)) >= 1 &&
                !recruits.contains(name))
            {
                recruits.add(name);
            }
        }
        return recruits;
    }

    void dispose()
    {
        if (server != null)
        {
            server.disposeAllClients();
        }
        System.exit(0);
    }


    /** Put all die rolling in one place, in case we decide to change random
     *  number algorithms, use an external dice server, etc. */
    static int rollDie()
    {
        return rollDie(6);
    }

    static int rollDie(int size)
    {
        return random.nextInt(size) + 1;
    }


    private void placeInitialLegion(Player player, String markerId)
    {
        String name = player.getName();
        player.selectMarkerId(markerId);
        Log.event(name + " selects initial marker");

        // Lookup coords for chit starting from player[i].getTower()
        String hexLabel = player.getTower();

        Legion legion = Legion.getStartingLegion(markerId, hexLabel, 
            player.getName(), this);
        player.addLegion(legion);
    }

    /** Set the entry side relative to the hex label. */
    private int findEntrySide(MasterHex hex, int cameFrom)
    {
        int entrySide = -1;
        if (cameFrom != -1)
        {
            if (HexMap.terrainHasStartlist(hex.getTerrain())) 
            {
                entrySide = 3;
            }
            else
            {
                entrySide = (6 + cameFrom - hex.getLabelSide()) % 6;
            }
        }
        return entrySide;
    }


    /** Recursively find conventional moves from this hex.  
     *  If block >= 0, go only that way.  If block == -1, use arches and 
     *  arrows.  If block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.  Return a set of 
     *  hexLabel:entrySide tuples. */
    private Set findNormalMoves(MasterHex hex, Legion legion, int roll, 
        int block, int cameFrom, boolean ignoreFriends)
    {
        Set set = new HashSet();
        String hexLabel = hex.getLabel();
        Player player = legion.getPlayer();

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (getNumEnemyLegions(hexLabel, player) > 0)
        {
            if (getNumFriendlyLegions(hexLabel, player) == 0 || ignoreFriends)
            {
                // Set the entry side relative to the hex label.
                if (cameFrom != -1)
                {
                    set.add(hexLabel + ":" + BattleMap.entrySideName(
                        findEntrySide(hex, cameFrom)));
                }
            }
            return set;
        }

        if (roll == 0)
        {
            // XXX fix
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

            if (cameFrom != -1)
            {
                set.add(hexLabel + ":" + BattleMap.entrySideName(
                    findEntrySide(hex, cameFrom)));
                return set;
            }
        }

        if (block >= 0)
        {
            set.addAll(findNormalMoves(hex.getNeighbor(block), legion,
                roll - 1, Constants.ARROWS_ONLY, (block + 3) % 6, 
                ignoreFriends));
        }
        else if (block == Constants.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= Constants.ARCH && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), legion, 
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6,
                        ignoreFriends));
                }
            }
        }
        else if (block == Constants.ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= Constants.ARROW && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), legion, 
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6,
                        ignoreFriends));
                }
            }
        }

        return set;
    }


    /** Recursively find all unoccupied hexes within roll hexes, for
     *  tower teleport. */
    private Set findNearbyUnoccupiedHexes(MasterHex hex, Legion legion, 
        int roll, int cameFrom, boolean ignoreFriends)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.
        String hexLabel = hex.getLabel();
        Set set = new HashSet();

        if (!isOccupied(hexLabel))
        {
            set.add(hexLabel);
        }

        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom && (hex.getExitType(i) != Constants.NONE ||
                   hex.getEntranceType(i) != Constants.NONE))
                {
                    set.addAll(findNearbyUnoccupiedHexes(hex.getNeighbor(i),
                        legion, roll - 1, (i + 3) % 6, ignoreFriends));
                }
            }
        }

        return set;
    }


    /** Return set of hexLabels describing where this legion can move.
     *  Include moves currently blocked by friendly
     *  legions if ignoreFriends is true. */
    Set listAllMoves(Legion legion, MasterHex hex, int movementRoll, 
        boolean ignoreFriends)
    {
        Set set = listNormalMoves(legion, hex, movementRoll, ignoreFriends);
        set.addAll(listTeleportMoves(legion, hex, movementRoll, 
            ignoreFriends));
        return set;
    }


    private int findBlock(MasterHex hex)
    {
        int block = Constants.ARCHES_AND_ARROWS;
        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == Constants.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }
        return block;
    }


    /** Return set of hexLabels describing where this legion can move
     *  without teleporting.  Include moves currently blocked by friendly
     *  legions if ignoreFriends is true. */
    Set listNormalMoves(Legion legion, MasterHex hex, int movementRoll,
        boolean ignoreFriends)
    {
        if (legion.hasMoved())
        {
            return new HashSet();
        }

        Set tuples = findNormalMoves(hex, legion, movementRoll, findBlock(hex),
            Constants.NOWHERE, ignoreFriends);

        // Extract just the hexLabels from the hexLabel:entrySide tuples.
        Set hexLabels = new HashSet();
        Iterator it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = (String)it.next();
            java.util.List parts = Split.split(':', tuple);
            String hexLabel = (String)parts.get(0);
            hexLabels.add(hexLabel);
        }
        return hexLabels;
    }

    private boolean towerTeleportAllowed()
    {
        if (getOption(Options.noTowerTeleport))
        {
            return false;
        }
        if (getTurnNumber() == 1 && getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }

    private boolean towerToTowerTeleportAllowed()
    {
        if (!towerTeleportAllowed())
        {
            return false;
        }
        if (getTurnNumber() == 1 && getOption(Options.noFirstTurnT2TTeleport))
        {
            return false;
        }
        return true;
    }

    private boolean towerToNonTowerTeleportAllowed()
    {
        if (!towerTeleportAllowed())
        {
            return false;
        }
        if (getOption(Options.towerToTowerTeleportOnly))
        {
            return false;
        }
        return true;
    }

    
    private boolean titanTeleportAllowed()
    {
        if (getOption(Options.noTitanTeleport))
        {
            return false;
        }
        if (getTurnNumber() == 1 && getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }


    /** Return set of hexLabels describing where this legion can teleport.
     *  Include moves currently blocked by friendly legions if 
     *  ignoreFriends is true. */
    Set listTeleportMoves(Legion legion, MasterHex hex, int movementRoll, 
        boolean ignoreFriends)
    {
        Player player = legion.getPlayer();

        Set set = new HashSet();
        if (movementRoll != 6 || legion.hasMoved() || player.hasTeleported())
        {
            return set;
        }

        // Tower teleport
        if (HexMap.terrainIsTower(hex.getTerrain()) && legion.numLords() > 0 &&
            towerTeleportAllowed())
        {
            // Mark every unoccupied hex within 6 hexes.
            if (towerToNonTowerTeleportAllowed())
            {
                set.addAll(findNearbyUnoccupiedHexes(hex, legion, 6,
                    Constants.NOWHERE, ignoreFriends));
            }

            if (towerToTowerTeleportAllowed())
            {
                // Mark every unoccupied tower.
                Set towerSet = MasterBoard.getTowerSet();
                Iterator it = towerSet.iterator();
                while (it.hasNext())
                {
                    String hexLabel = (String)it.next();
                    if (MasterBoard.getHexByLabel(hexLabel) != null)
                    {
                        if ((!isOccupied(hexLabel) || (ignoreFriends &&
                            getNumEnemyLegions(hexLabel, player) == 0))
                            && (!(hexLabel.equals(hex.getLabel()))))
                        {
                            set.add(hexLabel);
                        }
                    }
                }
            }
            else
            {
                // Remove nearby towers from set.
                Set towerSet = MasterBoard.getTowerSet();
                Iterator it = towerSet.iterator();
                while (it.hasNext())
                {
                    String hexLabel = (String)it.next();
                    set.remove(hexLabel);
                }
            }
        }

        // Titan teleport
        if (player.canTitanTeleport() && legion.hasTitan() &&
            titanTeleportAllowed())
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
                    }
                }
            }
        }
        return set;
    }


    /** Return a Set of Strings "Left" "Right" or "Bottom" describing
     *  possible entry sides.  If the hex is unoccupied, just return 
     *  one entry side since it doesn't matter. */
    Set listPossibleEntrySides(String markerId, String targetHexLabel,
        boolean teleport)
    {
        Set entrySides = new HashSet();
        Legion legion = getLegionByMarkerId(markerId);
        Player player = legion.getPlayer();
        int movementRoll = player.getMovementRoll();
        MasterHex currentHex = legion.getCurrentHex();
        MasterHex targetHex = MasterBoard.getHexByLabel(targetHexLabel);

        if (teleport)
        {
            if (listTeleportMoves(legion, currentHex, movementRoll, 
                false).contains(targetHexLabel))
            {
                // Startlisted terrain only have bottom entry side.
                // Don't bother finding more than one entry side if unoccupied.
                if (!isOccupied(targetHexLabel) ||
                    HexMap.terrainHasStartlist(targetHex.getTerrain()))
                    
                {
                    entrySides.add(Constants.bottom);  
                    return entrySides;
                }
                else
                {
                    entrySides.add(Constants.bottom);
                    entrySides.add(Constants.left);  
                    entrySides.add(Constants.right);  
                    return entrySides;
                }
            }
            else
            {
                return entrySides;
            }
        }

        // Normal moves.
        Set tuples = findNormalMoves(currentHex, legion, movementRoll, 
            findBlock(currentHex), Constants.NOWHERE, false);
        Iterator it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = (String)it.next();
            java.util.List parts = Split.split(':', tuple);
            String hl = (String)parts.get(0);
            if (hl.equals(targetHexLabel))
            {
                String buf = (String)parts.get(1);
                entrySides.add(buf);
                // Don't bother finding more than one entry side if unoccupied.
                if (!isOccupied(targetHexLabel))
                {
                    return entrySides;
                }
            }
        }
        return entrySides;
    }


    boolean isEngagement(String hexLabel)
    {
        if (getNumLegions(hexLabel) > 1)
        {
            java.util.List markerIds = getLegionMarkerIds(hexLabel);
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
    Set findEngagements()
    {
        Set set = new HashSet();
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

    void createSummonAngel(Legion attacker)
    {
        summoning = true;
        server.createSummonAngel(attacker);
    }

    /** Called locally and from Battle. */
    void reinforce(Legion legion)
    {
        reinforcing = true;
        server.reinforce(legion);
    }

    void doneReinforcing()
    {
        reinforcing = false;
        if (battle == null)
        {
            kickEngagements();
        }
    }

    // Called by both human and AI.
    void doSummon(Legion legion, Legion donor, Creature angel)
    {
        Player player = getActivePlayer();

        if (angel != null && donor != null && legion.canSummonAngel())
        {
            // Only one angel can be summoned per turn.
            player.setSummoned(true);

            // Move the angel or archangel.
            donor.removeCreature(angel, false, false);
            legion.addCreature(angel, false);

            // XXX Maybe replace with a special-purpose summon notification?
            server.allTellRemoveCreature(donor.getMarkerId(), angel.getName());
            server.allTellAddCreature(legion.getMarkerId(), angel.getName());

            Log.event("An " + angel.getName() +
                " is summoned from legion " + donor.getLongMarkerName() +
                " into legion " + legion.getLongMarkerName());
        }

        // Need to call this regardless to advance past the summon phase.
        if (battle != null)
        {
            battle.finishSummoningAngel(player.hasSummoned());
            summoning = false;
        }
        else
        {
            summoning = false;
            kickEngagements();
        }
    }


    /** For AI. */
    Battle getBattle()
    {
        return battle;
    }


    private synchronized void kickEngagements()
    {
        Player player = getActivePlayer();
        String engagementHexLabel = player.aiPickEngagement();
        if (engagementHexLabel != null)
        {
            engage(engagementHexLabel);
        }
Log.debug("in kickEngagements() summoning=" + summoning + " reinforcing=" +
reinforcing + " acquiring=" + acquiring); 
Log.debug("" + findEngagements().size() + " engagements left");

        if (findEngagements().size() == 0 && !summoning && !reinforcing && 
            !acquiring)
        {
            advancePhase(Constants.FIGHT, player.getName());
        }
    }


    void finishBattle(String hexLabel, boolean attackerEntered)
    {
        battle = null;
        server.allCleanupBattle();

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
Log.debug("Calling Game.reinforce() from Game.finishBattle()");
                    reinforce(legion);
                }
            }
        }
        engagementInProgress = false;
        battleInProgress = false;
        server.allUpdatePlayerInfo();
        if (!summoning && !reinforcing && !acquiring)
        {
            server.allHighlightEngagements();
            kickEngagements();
        }
    }

    /** Return a set of hexLabels. */
    Set findSummonableAngels(String markerId)
    {
        Legion legion = getLegionByMarkerId(markerId);
        Set set = new HashSet();
        Iterator it = legion.getPlayer().getLegions().iterator();
        while (it.hasNext())
        {
            Legion candidate = (Legion)it.next();
            if (candidate != legion)
            {
                String hexLabel = candidate.getCurrentHexLabel();
                boolean hasSummonable = false;
                java.util.List summonableList =
                    Creature.getSummonableCreatures();
                Iterator sumIt = summonableList.iterator();
                while (sumIt.hasNext() && !hasSummonable)
                {
                    Creature c = (Creature)sumIt.next();
                    hasSummonable = hasSummonable ||
                        (candidate.numCreature(c) > 0);
                }
                if (hasSummonable && !isEngagement(hexLabel))
                {
                    set.add(hexLabel);
                }
            }
        }
        return set;
    }


    /** Return true if the split succeeded. */
    boolean doSplit(String parentId, String childId, String results)
    {
        Legion legion = getLegionByMarkerId(parentId);
        Player player = legion.getPlayer();

        // Need a legion marker to split.
        if (!player.isMarkerAvailable(childId))
        {
            server.showMessageDialog(player.getName(),
                "Marker " + childId + " is not available.");
            return false;
        }
        // Don't allow extra splits in turn 1.
        if (getTurnNumber() == 1 && player.getNumLegions() > 1)
        {
            server.showMessageDialog(player.getName(),
                "Cannot split twice on Turn 1.");
            return false;
        }
        if (results == null)
        {
            return false;
        }

        Legion newLegion = null;

        java.util.List strings = Split.split(',', results);
        strings.remove(0);

        // Need to replace strings with creatures.
        java.util.List creatures = new ArrayList();
        Iterator it = strings.iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            Creature creature = Creature.getCreatureByName(name);
            creatures.add(creature);
        }
        newLegion = legion.split(creatures, childId);

        if (newLegion == null)
        {
            return false;
        }

        String hexLabel = legion.getCurrentHexLabel();
        server.didSplit(hexLabel, parentId, childId, newLegion.getHeight());

        if (getOption(Options.allStacksVisible))
        {
            server.allRevealLegion(legion);
            server.allRevealLegion(newLegion);
        }
        else
        {
            server.oneRevealLegion(legion, player.getName());
            server.oneRevealLegion(newLegion, player.getName());
        }

        return true;
    }


    /** Move the legion to the hex if legal.  Return true if the
     *  legion was moved or false if the move was illegal. */
    boolean doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord)
    {
        Legion legion = getLegionByMarkerId(markerId);
        if (legion == null)
        {
            return false;
        }

        Player player = legion.getPlayer();

        // Verify that the move is legal.
        if (teleport) 
        {
            if (!listTeleportMoves(legion, legion.getCurrentHex(), 
                player.getMovementRoll(), false).contains(hexLabel))
            {
                return false;
            }
        }
        else
        {
            if (!listNormalMoves(legion, legion.getCurrentHex(), 
                player.getMovementRoll(), false).contains(hexLabel))
            {
                return false;
            }
        }

        // Verify that the entry side is legal.
        Set legalSides = listPossibleEntrySides(markerId, hexLabel, teleport);
        if (!legalSides.contains(entrySide))
        {
            return false;
        }

        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        // If this is a tower hex, the only entry side is the bottom.
        if (HexMap.terrainHasStartlist(hex.getTerrain()) &&
            !entrySide.equals(Constants.bottom))
        {
            Log.warn("Tried to enter invalid side of tower");
            entrySide = Constants.bottom;
        }

        // If the legion teleported, reveal a lord.
        if (teleport)
        {
            // Verify teleporting lord.
            if (teleportingLord == null || !legion.listTeleportingLords(
                hexLabel).contains(teleportingLord))
            {
                return false;
            }

            server.allRevealCreature(legion, teleportingLord);
        }

        legion.moveToHex(hex, entrySide, teleport, teleportingLord);
        return true;
    }


    // XXX Try to merge with the other version.
    /** Simplified version for AIs and clients that really don't care about
     *  teleport versus ground or entry sides or teleporting lords. */
    boolean doMove(String markerId, String hexLabel)
    {
        Legion legion = getLegionByMarkerId(markerId);
        if (legion == null)
        {
            return false;
        }
        Player player = legion.getPlayer();
        String startingHexLabel = legion.getCurrentHexLabel();
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);

        // Teleport only if it's the only option.
        boolean teleport = !(listNormalMoves(legion, hex, 
            player.getMovementRoll(), false).contains(hexLabel)) &&
            (listTeleportMoves(legion, hex, player.getMovementRoll(), 
            false).contains(hexLabel));

        String teleportingLord = "";
        if (teleport)
        {
            teleportingLord = (String)(legion.listTeleportingLords(
                hexLabel).get(0));
        }

        Set sides = listPossibleEntrySides(markerId, hexLabel, teleport);
        String entrySide = "";
        if (!sides.isEmpty())
        {
            entrySide = (String)(sides.iterator().next());
        }

        boolean moved = doMove(markerId, hexLabel, entrySide, teleport, 
            teleportingLord);
        if (moved)
        {
            server.allTellDidMove(markerId, startingHexLabel, hexLabel, 
                teleport);
        }
        return moved;
    }


    synchronized void engage(String hexLabel)
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

            server.oneRevealLegion(attacker, defender.getPlayerName());
            server.oneRevealLegion(defender, attacker.getPlayerName());

            if (defender.canFlee())
            {
                // Fleeing gives half points and denies the
                // attacker the chance to summon an angel.
                server.askFlee(defender, attacker);
            }
            else
            {
                engage2(hexLabel);
            }
        }
    }

    // Defender did not flee; attacker may concede early.
    private void engage2(String hexLabel)
    {
        Player player = getActivePlayer();
        Legion attacker = getFirstFriendlyLegion(hexLabel, player);
        Legion defender = getFirstEnemyLegion(hexLabel, player);
        server.askConcede(attacker, defender);
    }


    // Attacker did not concede early; negotiate. 
    private void engage3(String hexLabel)
    {
        Player player = getActivePlayer();
        Legion attacker = getFirstFriendlyLegion(hexLabel, player);
        Legion defender = getFirstEnemyLegion(hexLabel, player);
        proposals[0] = new HashSet();
        proposals[1] = new HashSet();
        server.twoNegotiate(attacker, defender);
    }


    void flee(String markerId)
    {
        Legion defender = getLegionByMarkerId(markerId);
        String hexLabel = defender.getCurrentHexLabel();
        Legion attacker = getFirstEnemyLegion(hexLabel, defender.getPlayer());
        handleConcession(defender, attacker, true);
    }

    void concede(String markerId)
    {
        if (battleInProgress)
        {
            battle.concede(getLegionByMarkerId(markerId).getPlayerName());
        }
        else
        {
            Legion attacker = getLegionByMarkerId(markerId);
            String hexLabel = attacker.getCurrentHexLabel();
            Legion defender = getFirstEnemyLegion(hexLabel, 
                attacker.getPlayer());
            handleConcession(attacker, defender, false);
        }
    }

    void doNotFlee(String markerId)
    {
        Legion defender = getLegionByMarkerId(markerId);
        String hexLabel = defender.getCurrentHexLabel();
        engage2(hexLabel);
    }

    /** Used only for pre-battle attacker concession. */
    void doNotConcede(String markerId)
    {
        Legion attacker = getLegionByMarkerId(markerId);
        String hexLabel = attacker.getCurrentHexLabel();
        engage3(hexLabel);
    }

    /** playerName offers proposal. */
    void makeProposal(String playerName, String proposalString)
    {
        // If it's too late to negotiate, just throw this away.
        if (battleInProgress)
        {
            return;
        }

        Proposal proposal = Proposal.makeProposal(proposalString);

        int thisPlayerNum;
        if (playerName.equals(getActivePlayerName()))
        {
            thisPlayerNum = Constants.ATTACKER;
        }
        else
        {
            thisPlayerNum = Constants.DEFENDER;
        }
        int otherSet = (thisPlayerNum + 1) & 1;

        // If this player wants to fight, cancel negotiations.
        if (proposal.isFight())
        {
            Legion attacker = getLegionByMarkerId(proposal.getAttackerId());
            String hexLabel = attacker.getCurrentHexLabel();
            fight(hexLabel);
        }

        // If this proposal matches an earlier one from the other player,
        // settle the engagement.
        else if (proposals[otherSet].contains(proposal))
        {
            handleNegotiation(proposal);
        }

        // Otherwise remember this proposal and continue.
        else
        {
            proposals[thisPlayerNum].add(proposal);

            String other = null;
            if (playerName.equals(getActivePlayerName()))
            {
                Legion defender = getLegionByMarkerId(
                    proposal.getDefenderId());
                other = defender.getPlayerName();
            }
            else
            {
                other = getActivePlayerName();
            }

            // Tell the other player about the proposal.
            server.tellProposal(other, proposal);
        }
    }


    void fight(String hexLabel)
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
                defender.getMarkerId(), Constants.DEFENDER, hexLabel,
                1, Constants.MOVE);
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

        // No recruiting or angel summoning is allowed after the
        // defender flees or the attacker concedes before entering
        // the battle.
        engagementInProgress = false;
        server.allUpdatePlayerInfo();
        server.allHighlightEngagements();
        if (!acquiring)
        {
            kickEngagements();
        }
    }


    private void handleNegotiation(Proposal results)
    {
        Legion attacker = getLegionByMarkerId(results.getAttackerId());
        Legion defender = getLegionByMarkerId(results.getDefenderId());

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
            java.util.List winnerLosses = results.getWinnerLosses();
            Iterator it = winnerLosses.iterator();
            while (it.hasNext())
            {
                String creatureName = (String)it.next();
                log.append(creatureName);
                if (it.hasNext())
                {
                    log.append(", ");
                }
                Creature creature = Creature.getCreatureByName(creatureName);
                winner.removeCreature(creature, true, true);
            }
            Log.event(log.toString());

            server.allFullyUpdateLegionHeights();
            server.oneRevealLegion(winner, attacker.getPlayerName());
            server.oneRevealLegion(winner, defender.getPlayerName());

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
                    reinforce(defender);
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
        server.allUpdatePlayerInfo();
        if (!summoning && !reinforcing && !acquiring)
        {
            server.allHighlightEngagements();
        }
        kickEngagements();
    }


    void askAcquireAngel(String playerName, String markerId,
        java.util.List recruits)
    {
        acquiring = true;
        server.askAcquireAngel(playerName, markerId, recruits);
    }

    synchronized void doneAcquiringAngels()
    {
        acquiring = false;
        if (!summoning && !reinforcing)
        {
            server.allHighlightEngagements();
            kickEngagements();
        }
    }


    /** Return a list of all players' legions. */
    java.util.List getAllLegions()
    {
        java.util.List list = new ArrayList();
        for (Iterator it = players.iterator(); it.hasNext();)
        {
            Player player = (Player)it.next();
            list.addAll(player.getLegions());
        }
        return list;
    }

    /** Return a list of all players' legions' marker ids. */
    java.util.List getAllLegionIds()
    {
        java.util.List list = new ArrayList();
        for (Iterator it = players.iterator(); it.hasNext();)
        {
            Player player = (Player)it.next();
            list.addAll(player.getLegionIds());
        }
        return list;
    }

    /** Return a list of all legions not belonging to player. */
    java.util.List getAllEnemyLegions(Player player)
    {
        java.util.List list = new ArrayList();
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
    java.util.List getAllEnemyLegionIds(Player player)
    {
        java.util.List list = new ArrayList();
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

    Legion getLegionByMarkerId(String markerId)
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

    Player getPlayerByMarkerId(String markerId)
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
    int getAverageLegionPointValue()
    {
        int total = 0;
        java.util.List legions = getAllLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            total += legion.getPointValue();
        }
        return total / legions.size();
    }


    int getNumLegions(String hexLabel)
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

    boolean isOccupied(String hexLabel)
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

    Legion getFirstLegion(String hexLabel)
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

    java.util.List getLegionMarkerIds(String hexLabel)
    {
        java.util.List markerIds = new ArrayList();
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

    int getNumFriendlyLegions(String hexLabel, Player player)
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

    Legion getFirstFriendlyLegion(String hexLabel, Player player)
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

    java.util.List getFriendlyLegions(String hexLabel, Player player)
    {
        java.util.List legions = new ArrayList();
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

    int getNumEnemyLegions(String hexLabel, Player player)
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

    Legion getFirstEnemyLegion(String hexLabel, Player player)
    {
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


    int mulligan()
    {
        if (getPhase() != Constants.MOVE)
        {
            return -1;
        }
        Player player = getActivePlayer();
        player.takeMulligan();
        setupPhase();
        return player.getMovementRoll();
    }


    boolean getOption(String optname)
    {
        return options.getOption(optname);
    }

    int getIntOption(String optname)
    {
        return options.getIntOption(optname);
    }

    void setOption(String optname, String value)
    {
        String oldValue = options.getStringOption(optname);
        if (!value.equals(oldValue))
        {
            options.setOption(optname, value);
            syncOptions();
        }
    }
}
