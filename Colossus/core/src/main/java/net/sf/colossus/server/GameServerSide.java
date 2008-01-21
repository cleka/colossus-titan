package net.sf.colossus.server;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.BattleMap;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.client.Proposal;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;

import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;


/**
 * Class Game gets and holds high-level data about a Titan game.
 * 
 * This is the old design with the game information in the server. Some
 * of the functionality here is supposed to be moved into the {@link net.sf.colossus.game.Game}
 * class which then can be shared between server and clients (the class, not the instances).
 * Other parts should be moved into the {@link Server} class or elsewhere.
 * 
 * @version $Id$
 * @author David Ripton
 * @author Bruce Sherrod
 * @author Romain Dolbeau
 */

public final class GameServerSide extends net.sf.colossus.game.Game
{
    private static final Logger LOGGER = Logger.getLogger(GameServerSide.class
        .getName());

    private final List<PlayerServerSide> players = new ArrayList<PlayerServerSide>(
        6);
    private int activePlayerNum;
    private int turnNumber; // Advance when every player has a turn
    private int lastRecruitTurnNumber;
    private boolean engagementInProgress;
    private boolean battleInProgress;
    private boolean summoning;
    private boolean reinforcing;
    private boolean acquiring;
    private int pointsScored;
    private int turnCombatFinished;
    private Legion winner;
    private String engagementResult;
    private boolean pendingAdvancePhase;
    private boolean loadingGame;
    private boolean gameOver;
    private BattleServerSide battle;
    private final Caretaker caretaker = new Caretaker(this);
    private Constants.Phase phase;
    private Server server;
    // Negotiation
    private final Set<Proposal> attackerProposals = new HashSet<Proposal>();
    private final Set<Proposal> defenderProposals = new HashSet<Proposal>();

    private final LinkedList<Player> colorPickOrder = new LinkedList<Player>();
    private List<String> colorsLeft;
    private final PhaseAdvancer phaseAdvancer = new GamePhaseAdvancer();
    private Options options = null;

    /** Server port number. */
    private int port;
    private String flagFilename = null;
    private NotifyWebServer notifyWebServer = null;

    private History history;

    private static int gameCounter = 1;
    private final String gameId;

    private boolean hotSeatMode = false;
    // currently visible board-player (for hotSeatMode)
    Player cvbPlayer = null;

    /** Package-private only for JUnit test setup. */
    GameServerSide()
    {
        super(null, new String[0]);
        // later perhaps from cmdline, GUI, or WebServer set it?
        gameId = "#" + (gameCounter++);
    }

    // TODO: Get via Options instead?
    public void setPort(int portNr)
    {
        this.port = portNr;
        net.sf.colossus.webcommon.InstanceTracker.register(this,
            "Game at port " + port);
    }

    public void setOptions(Options options)
    {
        this.options = options;
    }

    public void setFlagFilename(String flagFilename)
    {
        this.flagFilename = flagFilename;
    }

    private void initServer()
    {
        // create it even if not needed (=no web server). 
        // This way we can have all the "if <there is a webserver>"
        // wrappers inside the notify Class, instead spread over the code...
        notifyWebServer = new NotifyWebServer(flagFilename);

        if (server != null)
        {
            server.setObsolete();
            server.disposeAllClients();
        }
        server = new Server(this, port);
        try
        {
            // Clemens 12/2007:
            // initFileServer can now done before creating local clients,
            // starting the SSTs and accepting the clients.
            // It decides whether to start a FST or not based on whether there
            // are *Network type* players in the Player list (instead of
            // whether there is something in the socket list, which was bogus,
            // because also local clients will be in socket list).
            // It's also not necessary to be after the socket server-start 
            //   / client-accepting, "because FST accepts only request from known
            //       client addresses"  -- in fact a client won't request files
            // before he is registered. So, do the FST start first and we are safe.
            server.initFileServer();
            server.initSocketServer();
            notifyWebServer.readyToAcceptClients();
            if (server.waitForClients())
            {
                ViableEntityManager.register(this, "Server/Game " + gameId);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Game.initServer(): got Exception " + e);
        }
    }

    private void cleanupWhenGameOver()
    {
        server.waitUntilGameFinishes();
        server.cleanup();
        server = null;

        ViableEntityManager.unregister(this);
    }

    private synchronized void clearFlags()
    {
        engagementInProgress = false;
        battleInProgress = false;
        summoning = false;
        reinforcing = false;
        acquiring = false;
        pendingAdvancePhase = false;
        // gameOver = false;  // Nope. Because advanceTurn calls this.
        loadingGame = false;
        engagementResult = null;
    }

    private void addPlayersFromOptions()
    {
        for (int i = 0; i < VariantSupport.getMaxPlayers(); i++)
        {
            String name = options.getStringOption(Options.playerName + i);
            String type = options.getStringOption(Options.playerType + i);

            if (name != null && type != null && !type.equals(Constants.none))
            {
                addPlayer(name, type);
                LOGGER.log(Level.INFO, "Add " + type + " player " + name);
            }
        }
        // No longer need the player name and type options.
        options.clearPlayerInfo();

        ((CreatureTypeServerSide)getVariant().getCreatureByName("Titan"))
            .setMaxCount(getNumPlayers());
    }

    /** Start a new game. */
    void newGame()
    {
        clearFlags();
        // additionally, because clearFlags must NOT do that:
        gameOver = false;

        turnNumber = 1;
        lastRecruitTurnNumber = -1;
        phase = Constants.Phase.SPLIT;
        caretaker.resetAllCounts();
        players.clear();

        VariantSupport.loadVariant(options.getStringOption(Options.variant),
            true);

        LOGGER.log(Level.INFO, "Starting new game");

        CustomRecruitBase.resetAllInstances();
        CustomRecruitBase.setCaretaker(caretaker);
        CustomRecruitBase.setGame(this);

        addPlayersFromOptions();

        hotSeatMode = options.getOption(Options.hotSeatMode);

        history = new History();

        initServer();
        // Some more stuff is done from newGame2() when the last
        // expected client has connected.
        // Main thread has now nothing to do any more, can wait
        // until game finishes.

        if (server.isServerRunning())
        {
            cleanupWhenGameOver();
        }
        else
        {
            server.cleanup();
            server = null;
        }
    }

    /* Called from the last SocketServerThread connecting 
     *  ( = when expected nr. of clients has connected).
     */
    void newGame2()
    {
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
            Player player = players.get(i);

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
        return getUniqueName(name + Dice.rollDie());
    }

    /** Find a Player for a new remote client.
     *  If loading a game, this is the network player with a matching
     *  player name.  If a new game, it's the first network player whose
     *  name is still set to <By client> */
    Player findNetworkPlayer(final String playerName)
    {
        for (int i = 0; i < getNumPlayers(); i++)
        {
            PlayerServerSide curPlayer = players.get(i);

            if (curPlayer.getType().endsWith(Constants.network))
            {
                if (isLoadingGame())
                {
                    if (curPlayer.getName().equals(playerName))
                    {
                        return curPlayer;
                    }
                }
                else
                {
                    if (curPlayer.getName().startsWith(Constants.byClient))
                    {
                        return curPlayer;
                    }
                }
            }
        }
        return null;
    }

    private void syncAutoPlay()
    {
        Iterator<PlayerServerSide> it = players.iterator();

        while (it.hasNext())
        {
            PlayerServerSide player = it.next();

            server.oneSetOption(player, Options.autoPlay, player.isAI());
            server.oneSetOption(player, Options.playerType, player.getType());
        }
    }

    /** Send all current game option values to all clients. */
    private void syncOptions()
    {
        Enumeration<String> en = options.propertyNames();

        while (en.hasMoreElements())
        {
            String name = en.nextElement();
            String value = options.getStringOption(name);

            server.allSetOption(name, value);
        }
    }

    private void assignColors()
    {
        List<String> cli = new ArrayList<String>();

        colorsLeft = new ArrayList<String>();
        for (String colorName : Constants.colorNames)
        {
            cli.add(colorName);
        }

        /* Add the first 6 colors in random order, ... */
        for (int i = 0; i < Constants.DEFAULT_MAX_PLAYERS; i++)
        {
            colorsLeft.add(cli.remove(Dice
                .rollDie(Constants.DEFAULT_MAX_PLAYERS - i) - 1));
        }

        /* ... and finish with the newer ones, also in random order */
        int newer = cli.size();

        for (int i = 0; i < newer; i++)
        {
            colorsLeft.add(cli.remove(Dice.rollDie(newer - i) - 1));
        }

        // Let human players pick colors first, followed by AI players.
        // Within each group, players pick colors in ascending tower order.
        colorPickOrder.clear();

        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            PlayerServerSide player = players.get(i);

            if (player.isHuman())
            {
                colorPickOrder.add(player);
            }
        }
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            PlayerServerSide player = players.get(i);

            if (player.isAI())
            {
                colorPickOrder.add(player);
            }
        }

        nextPickColor();
    }

    void nextPickColor()
    {
        if (colorPickOrder.size() >= 1)
        {
            Player playerName = colorPickOrder.getFirst();

            server.askPickColor(playerName, colorsLeft);
        }
        else
        {
            // All players are done picking colors; continue.
            newGame3();
        }
    }

    String makeNameByType(String templateName, String type)
    {
        String number = templateName.substring(Constants.byType.length());
        // type is the full class name of client, e.g.
        //   "net.sf.colossus.client.SimpleAI"
        String prefix = Constants.aiPackage;
        int len = prefix.length();

        String shortName = type.substring(len);
        String newName;

        if (shortName.equals("Human"))
        {
            newName = "Human" + number;
        }
        else if (shortName.equals("SimpleAI"))
        {
            newName = "Simple" + number;
        }
        else if (shortName.equals("CowardSimpleAI"))
        {
            newName = "Coward" + number;
        }
        else if (shortName.equals("RationalAI"))
        {
            newName = "Rational" + number;
        }
        else if (shortName.equals("HumanHaterRationalAI"))
        {
            newName = "Hater" + number;
        }
        else if (shortName.equals("MilvangAI"))
        {
            newName = "Milvang" + number;
        }
        else
        {
            newName = null;
        }
        return newName;
    }

    void assignColor(Player player, String color)
    {
        colorPickOrder.remove(player);
        colorsLeft.remove(color);
        ((PlayerServerSide)player).setColor(color);
        String type = ((PlayerServerSide)player).getType();
        String gotName = player.getName();
        if (gotName.startsWith(Constants.byType))
        {
            String newName = makeNameByType(gotName, type);
            if (newName != null)
            {
                LOGGER.log(Level.INFO, "Setting for \"" + gotName
                    + "\" new name: " + newName);
                server.setPlayerName(player, newName);
                player.setName(newName);
            }
            else
            {
                LOGGER.log(Level.WARNING, "Type " + type + " not recognized"
                    + ". Giving name by color instead (" + color + ")");
                gotName = Constants.byColor;
            }
        }

        if (gotName.startsWith(Constants.byColor))
        {
            server.setPlayerName(player, color);
            player.setName(color);
        }
        LOGGER.log(Level.INFO, player + " chooses color " + color);
        ((PlayerServerSide)player).initMarkersAvailable();
        server.allUpdatePlayerInfo();
        server.askPickFirstMarker(player);
    }

    Player getNextColorPicker()
    {
        return colorPickOrder.getFirst();
    }

    /** Done picking player colors; proceed to start game. */
    private void newGame3()
    {
        server.allUpdatePlayerInfo();

        Iterator<PlayerServerSide> it = players.iterator();
        while (it.hasNext())
        {
            PlayerServerSide player = it.next();
            placeInitialLegion(player, player.getFirstMarker());
            server.allRevealLegion(player.getLegion(0),
                Constants.reasonInitial);
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
        int numPlayers = getNumPlayers();
        String[] playerTower = new String[numPlayers];
        Set<String> towerSet = getVariant().getMasterBoard().getTowerSet();
        List<String> towerList = new ArrayList<String>();

        Iterator<String> it = towerSet.iterator();
        while (it.hasNext())
        { // first, fill the list with all Label
            towerList.add(it.next());
        }

        if (getOption(Options.balancedTowers))
        {
            towerList = getBalancedTowers(numPlayers, towerList);
        }

        int playersLeft = numPlayers - 1;

        while ((playersLeft >= 0) && (!towerList.isEmpty()))
        {
            int which = Dice.rollDie(towerList.size());
            playerTower[playersLeft] = towerList.remove(which - 1);
            playersLeft--;
        }

        for (int i = 0; i < numPlayers; i++)
        {
            PlayerServerSide player = getPlayer(i);
            LOGGER.log(Level.INFO, player.getName() + " gets tower "
                + playerTower[i]);
            player.setTower(playerTower[i]);
        }
    }

    /** Return a list with a balanced order of numPlayer towers chosen
     from towerList, which must hold numeric strings. */
    static List<String> getBalancedTowers(int numPlayers,
        final List<String> towerList)
    {
        int numTowers = towerList.size();

        if (numPlayers > numTowers)
        {
            LOGGER.log(Level.SEVERE, "More players than towers!");
            return towerList;
        }

        // Make a sorted copy, converting String to Integer.
        ArrayList<Integer> numericList = new ArrayList<Integer>();
        Iterator<String> itTwr = towerList.iterator();

        while (itTwr.hasNext())
        {
            String s = itTwr.next();
            Integer i = new Integer(s);

            numericList.add(i);
        }
        Collections.sort(numericList);

        double towersPerPlayer = (double)numTowers / numPlayers;

        // First just find a balanced sequence starting at zero.
        double counter = 0.0;
        int numDone = 0;
        List<Integer> sequence = new ArrayList<Integer>();
        // Prevent floating-point roundoff error.
        double epsilon = 0.0000001;

        while (numDone < numPlayers)
        {
            sequence.add(new Integer((int)Math.floor(counter + epsilon)));
            numDone++;
            counter += towersPerPlayer;
        }

        // Pick a random starting point.  (Zero-based)
        int startingTower = Dice.rollDie(numTowers) - 1;

        // Offset the sequence by the starting point, and get only
        // the number of starting towers we need.
        List<String> returnList = new ArrayList<String>();

        Iterator<Integer> it = sequence.iterator();
        numDone = 0;
        while (it.hasNext() && numDone < numPlayers)
        {
            Integer raw = it.next();
            int cooked = (raw.intValue() + startingTower) % numTowers;
            Integer numericLabel = numericList.get(cooked);

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

    Player addPlayer(String name, String type)
    {
        PlayerServerSide player = new PlayerServerSide(name, this);

        player.setType(type);
        players.add(player);
        return player;
    }

    int getNumPlayers()
    {
        return players.size();
    }

    int getNumLivingPlayers()
    {
        int count = 0;
        Iterator<PlayerServerSide> it = players.iterator();

        while (it.hasNext())
        {
            Player player = it.next();

            if (!player.isDead())
            {
                count++;
            }
        }
        return count;
    }

    PlayerServerSide getActivePlayer()
    {
        // Sanity check in case called before all players are loaded.
        if (activePlayerNum < players.size())
        {
            return players.get(activePlayerNum);
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

    PlayerServerSide getPlayer(int i)
    {
        return players.get(i);
    }

    Collection<PlayerServerSide> getPlayers()
    {
        return Collections.unmodifiableCollection(players);
    }

    PlayerServerSide getPlayer(String name)
    {
        if (name != null)
        {
            Iterator<PlayerServerSide> it = players.iterator();

            while (it.hasNext())
            {
                PlayerServerSide player = it.next();

                if (name.equals(player.getName()))
                {
                    return player;
                }
            }
        }
        return null;
    }

    PlayerServerSide getPlayerByShortColor(String shortColor)
    {
        if (shortColor != null)
        {
            Iterator<PlayerServerSide> it = players.iterator();

            while (it.hasNext())
            {
                PlayerServerSide player = it.next();

                if (shortColor.equals(player.getShortColor()))
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
        Iterator<PlayerServerSide> it = players.iterator();

        while (it.hasNext())
        {
            Player player = it.next();

            if (!player.isDead())
            {
                remaining++;
            }
        }
        return remaining;
    }

    /**
     * Returns the number of real players (Human or Network)
     * which are still alive.
     */
    int getNumHumansRemaining()
    {
        int remaining = 0;
        Iterator<PlayerServerSide> it = players.iterator();

        while (it.hasNext())
        {
            PlayerServerSide player = it.next();

            if (player.isHuman() && !player.isDead())
            {
                remaining++;
            }
        }
        return remaining;
    }

    // Server uses this to decide whether it needs to start a file server
    int getNumRemoteRemaining()
    {
        int remaining = 0;
        Iterator<PlayerServerSide> it = players.iterator();

        while (it.hasNext())
        {
            PlayerServerSide player = it.next();

            if (player.isNetwork() && !player.isDead())
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
        Iterator<PlayerServerSide> it = players.iterator();

        while (it.hasNext())
        {
            Player player = it.next();

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

    synchronized void checkForVictory()
    {
        if (gameOver)
        {
            LOGGER.log(Level.SEVERE,
                "checkForVictory called although game is already over!!");
            return;
        }

        int remaining = getNumPlayersRemaining();

        switch (remaining)
        {
            case 0:
                LOGGER.log(Level.INFO, "Game over -- Draw at "
                    + new Date().getTime());
                server.allTellGameOver("Draw");
                setGameOver(true);
                break;

            case 1:
                String winnerName = getWinner().getName();
                LOGGER.log(Level.INFO, "Game over -- " + winnerName
                    + " wins at " + new Date().getTime());
                server.allTellGameOver(winnerName + " wins");
                setGameOver(true);
                break;

            default:
                break;
        }
    }

    synchronized boolean isOver()
    {
        return gameOver;
    }

    public synchronized void setGameOver(boolean gameOver)
    {
        this.gameOver = gameOver;
        if (gameOver)
        {
            server.allFullyUpdateAllLegionContents(Constants.reasonGameOver);
        }
    }

    boolean isLoadingGame()
    {
        return loadingGame;
    }

    Constants.Phase getPhase()
    {
        return phase;
    }

    /** Advance to the next phase, only if the passed oldPhase and playerName
     *  are current. */
    synchronized void advancePhase(final Constants.Phase oldPhase,
        final Player player)
    {
        if (oldPhase != phase || pendingAdvancePhase
            || !player.equals(getActivePlayer()))
        {
            LOGGER
                .log(
                    Level.SEVERE,
                    "Player "
                        + player.getName()
                        + " called advancePhase illegally (reason: "
                        + (oldPhase != phase ? "oldPhase (" + oldPhase
                            + ") != phase (" + phase + ")"
                            : (pendingAdvancePhase ? "pendingAdvancePhase is true "
                                : (!player.equals(getActivePlayer()) ? "wrong player ["
                                    + player.getName()
                                    + " vs. "
                                    + getActivePlayerName() + "]"
                                    : "UNKNOWN"))) + ")");
            return;
        }
        if (getOption(Options.autoStop) && getNumHumansRemaining() < 1
            && !gameOver)
        {
            LOGGER.log(Level.INFO, "Not advancing because no humans remain");
            // XXX buggy?
            server.allTellGameOver("All humans eliminated");
            setGameOver(true);
            checkAutoQuitOrGoOn();
            return;
        }
        phaseAdvancer.advancePhase();
    }

    /** Wrap the complexity of phase advancing. */
    class GamePhaseAdvancer implements PhaseAdvancer
    {

        /** Advance to the next phase, only if the passed oldPhase and
         *  playerName are current. */
        public void advancePhase()
        {
            pendingAdvancePhase = true;
            advancePhaseInternal();
        }

        /** Advance to the next phase, with no error checking. */
        public void advancePhaseInternal()
        {
            Constants.Phase oldPhase = phase;
            if (oldPhase == Constants.Phase.SPLIT)
            {
                phase = Constants.Phase.MOVE;
            }
            else if (oldPhase == Constants.Phase.MOVE)
            {
                phase = Constants.Phase.FIGHT;
            }
            else if (oldPhase == Constants.Phase.FIGHT)
            {
                phase = Constants.Phase.MUSTER;
            }

            if (oldPhase == Constants.Phase.MUSTER
                || (getActivePlayer().isDead() && getNumLivingPlayers() > 0))
            {
                advanceTurn();
            }
            else
            {
                LOGGER.log(Level.INFO, "Phase advances to " + phase);
            }

            pendingAdvancePhase = false;
            setupPhase();
        }

        public void advanceTurn()
        {
            clearFlags();
            activePlayerNum++;
            if (activePlayerNum == getNumPlayers())
            {
                activePlayerNum = 0;
                turnNumber++;
                if (turnNumber - lastRecruitTurnNumber > 100
                    && Options.isStresstest())
                {
                    LOGGER.log(Level.INFO,
                        "\nLast recruiting is 100 turns ago - "
                            + "exiting to prevent AIs from endlessly "
                            + "running around...\n");
                    System.exit(0);
                }
            }

            /* notify all CustomRecruitBase object that we change the
             active player, for bookkeeping purpose */
            CustomRecruitBase.everyoneAdvanceTurn(activePlayerNum);

            phase = Constants.Phase.SPLIT;
            if (getActivePlayer().isDead() && getNumLivingPlayers() > 0)
            {
                advanceTurn();
            }
            else
            {
                LOGGER.log(Level.INFO, getActivePlayerName()
                    + "'s turn, number " + turnNumber);
                autoSave();
            }
        }
    }

    private void setupPhase()
    {
        Constants.Phase phase = getPhase();
        if (phase == Constants.Phase.SPLIT)
        {
            setupSplit();
        }
        else if (phase == Constants.Phase.MOVE)
        {
            setupMove();
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            setupFight();
        }
        else if (phase == Constants.Phase.MUSTER)
        {
            setupMuster();
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Bogus phase");
        }
    }

    private void setupSplit()
    {
        PlayerServerSide player = getActivePlayer();

        if (player == null)
        {
            LOGGER.log(Level.SEVERE, "No players");
            dispose();
            return;
        }
        player.resetTurnState();
        server.allSetupSplit();
        if (hotSeatMode)
        {
            hotSeatModeChangeBoards();
        }
    }

    public void hotSeatModeChangeBoards()
    {
        PlayerServerSide activePlayer = getActivePlayer();

        // game just started - find the local player which shall
        // get the board first, and hide all other local ones.
        if (cvbPlayer == null)
        {
            int i;
            for (i = 0; i < getNumPlayers(); i++)
            {
                PlayerServerSide iPlayer = getPlayer(i);
                if (iPlayer.isLocalHuman() && !server.isClientGone(iPlayer))
                {
                    // This is a local alive player.
                    if (cvbPlayer == null)
                    {
                        cvbPlayer = iPlayer;
                        server.setBoardVisibility(iPlayer, true);
                    }
                    else
                    {
                        server.setBoardVisibility(iPlayer, false);
                    }
                }
            }
            return;
        }

        // otherwise, switch board to next, then and only then
        // if activePlayer is now the next local human which is
        // still connected ( = has not closed his board).
        if (activePlayer.isLocalHuman() && !server.isClientGone(activePlayer))
        {
            server.setBoardVisibility(cvbPlayer, false);
            server.setBoardVisibility(activePlayer, true);
            cvbPlayer = activePlayer;
        }
    }

    private void setupMove()
    {
        PlayerServerSide player = getActivePlayer();

        player.rollMovement();
        server.allSetupMove();
    }

    private synchronized void setupFight()
    {
        server.allSetupFight();
        server.nextEngagement();
    }

    private void setupMuster()
    {
        PlayerServerSide player = getActivePlayer();
        player.removeEmptyLegions();
        // If a player has been eliminated we can't count on his client
        // still being around to advance the turn.
        if (player.isDead())
        {
            advancePhase(Constants.Phase.MUSTER, player);
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

    synchronized void saveGame(final String filename)
    {
        String fn = null;

        if (filename == null || filename.equals("null"))
        {
            Date date = new Date();
            File savesDir = new File(Constants.saveDirname);

            if (!savesDir.exists() || !savesDir.isDirectory())
            {
                LOGGER.log(Level.INFO, "Trying to make directory "
                    + Constants.saveDirname);
                if (!savesDir.mkdirs())
                {
                    LOGGER.log(Level.SEVERE,
                        "Could not create saves directory");
                    JOptionPane
                        .showMessageDialog(
                            null,
                            "Could not create directory "
                                + savesDir
                                + "\n- saving game failed! Unless the directory "
                                + "can be created, you can't use File=>Save, and "
                                + "make sure Autosave (in Game Setup) is disabled.",
                            "Can't save game!", JOptionPane.ERROR_MESSAGE);

                    return;
                }
            }

            fn = Constants.saveDirname + Constants.xmlSnapshotStart
                + date.getTime() + Constants.xmlExtension;
        }
        else
        {
            fn = new String(filename);
            LOGGER.log(Level.INFO, "Saving game to " + filename);
        }

        FileWriter fileWriter;
        try
        {
            fileWriter = new FileWriter(fn);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Couldn't open " + fn, e);
            return;
        }
        PrintWriter out = new PrintWriter(fileWriter);

        try
        {
            Element root = new Element("ColossusSnapshot");

            root.setAttribute("version", Constants.xmlSnapshotVersion);

            Document doc = new Document(root);

            Element el = new Element("Variant");

            el.setAttribute("dir", VariantSupport.getVarDirectory());
            el.setAttribute("file", VariantSupport.getVarName());
            root.addContent(el);

            el = new Element("TurnNumber");
            el.addContent("" + getTurnNumber());
            root.addContent(el);

            el = new Element("CurrentPlayer");
            el.addContent("" + getActivePlayerNum());
            root.addContent(el);

            el = new Element("CurrentPhase");
            el.addContent("" + getPhase().toInt());
            root.addContent(el);

            Element car = new Element("Caretaker");

            root.addContent(car);

            // Caretaker stacks
            List<CreatureType> creatures = getVariant().getCreatureTypes();
            Iterator<CreatureType> itCre = creatures.iterator();

            while (itCre.hasNext())
            {
                CreatureType creature = itCre.next();

                el = new Element("Creature");
                el.setAttribute("name", creature.getName());
                el
                    .setAttribute("remaining", ""
                        + caretaker.getCount(creature));
                el.setAttribute("dead", "" + caretaker.getDeadCount(creature));
                car.addContent(el);
            }

            // Players
            Iterator<PlayerServerSide> itPl = players.iterator();
            while (itPl.hasNext())
            {
                PlayerServerSide player = itPl.next();

                el = new Element("Player");
                el.setAttribute("name", player.getName());
                el.setAttribute("type", player.getType());
                el.setAttribute("color", player.getColor());
                el.setAttribute("startingTower", player.getTower());
                el.setAttribute("score", "" + player.getScore());
                el.setAttribute("dead", "" + player.isDead());
                el.setAttribute("mulligansLeft", ""
                    + player.getMulligansLeft());
                el.setAttribute("colorsElim", player.getPlayersElim());
                el.setAttribute("movementRoll", "" + player.getMovementRoll());
                el.setAttribute("teleported", "" + player.hasTeleported());
                el.setAttribute("summoned", "" + player.hasSummoned());

                Collection<LegionServerSide> legions = player.getLegions();
                Iterator<LegionServerSide> it2 = legions.iterator();

                while (it2.hasNext())
                {
                    LegionServerSide legion = it2.next();

                    el.addContent(dumpLegion(legion, battleInProgress
                        && (legion == battle.getAttacker() || legion == battle
                            .getDefender())));
                }
                root.addContent(el);
            }

            // Dump the file cache, so that generated files are preserved
            Iterator<Element> itEl = ResourceLoader.getFileCacheDump()
                .iterator();
            while (itEl.hasNext())
            {
                root.addContent(itEl.next());
            }

            // Battle stuff
            if (engagementInProgress && battle != null)
            {
                Element bat = new Element("Battle");

                bat.setAttribute("masterHexLabel", battle.getMasterHexLabel());
                bat.setAttribute("turnNumber", "" + battle.getTurnNumber());
                bat.setAttribute("activePlayer", ""
                    + battle.getActivePlayer().getName());
                bat
                    .setAttribute("phase", ""
                        + battle.getBattlePhase().toInt());
                bat.setAttribute("summonState", "" + battle.getSummonState());
                bat.setAttribute("carryDamage", "" + battle.getCarryDamage());
                bat.setAttribute("driftDamageApplied", ""
                    + battle.isDriftDamageApplied());

                Iterator<String> itCT = battle.getCarryTargets().iterator();
                while (itCT.hasNext())
                {
                    Element ct = new Element("CarryTarget");
                    String carryTarget = itCT.next();

                    ct.addContent(carryTarget);
                    bat.addContent(ct);
                }
                root.addContent(bat);
            }
            root.addContent(history.getCopy());
            XMLOutputter putter = new XMLOutputter("    ", true);
            putter.output(doc, out);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "Error writing XML savegame.", ex);
        }
    }

    private String notnull(String in)
    {
        if (in == null)
        {
            return "null";
        }
        return in;
    }

    private Element dumpLegion(LegionServerSide legion, boolean inBattle)
    {
        Element leg = new Element("Legion");

        leg.setAttribute("name", legion.getMarkerId());
        leg.setAttribute("currentHex", legion.getCurrentHexLabel());
        leg.setAttribute("startingHex", legion.getStartingHexLabel());
        leg.setAttribute("moved", "" + legion.hasMoved());
        leg.setAttribute("entrySide", "" + legion.getEntrySide());
        leg.setAttribute("parent", notnull(legion.getParentId()));
        leg.setAttribute("recruitName", notnull(legion.getRecruitName()));
        leg.setAttribute("battleTally", "" + legion.getBattleTally());

        Collection<CreatureServerSide> critters = legion.getCritters();
        Iterator<CreatureServerSide> it = critters.iterator();

        while (it.hasNext())
        {
            CreatureServerSide critter = it.next();
            Element cre = new Element("Creature");

            cre.setAttribute("name", critter.getName());
            if (inBattle)
            {
                cre.setAttribute("hits", "" + critter.getHits());
                cre.setAttribute("currentHex", critter.getCurrentHex()
                    .getLabel());
                cre.setAttribute("startingHex", critter.getStartingHex()
                    .getLabel());
                cre.setAttribute("struck", "" + critter.hasStruck());
            }
            leg.addContent(cre);
        }
        return leg;
    }

    synchronized void autoSave()
    {
        if (getOption(Options.autosave) && !isOver())
        {
            saveGame(null);
        }
    }

    /** Try to load a game from saveDirName/filename.  If the filename is
     *  "--latest" then load the latest savegame found in saveDirName. */
    // JDOM lacks generics, so we need casts
    @SuppressWarnings("unchecked")
    void loadGame(String filename)
    {
        File file = null;

        if (filename.equals("--latest"))
        {
            File dir = new File(Constants.saveDirname);

            if (!dir.exists() || !dir.isDirectory())
            {
                LOGGER.log(Level.SEVERE, "No saves directory");
                dispose();
                return;
            }
            String[] filenames = dir.list(new XMLSnapshotFilter());

            if (filenames.length < 1)
            {
                LOGGER.log(Level.SEVERE,
                    "No XML savegames found in saves directory");
                dispose();
                return;
            }
            file = new File(Constants.saveDirname
                + latestSaveFilename(filenames));
        }
        else if (filename.indexOf("/") >= 0 || filename.indexOf("\\") >= 0)
        {
            // Already a full path
            file = new File(filename);
        }
        else
        {
            file = new File(Constants.saveDirname + filename);
        }

        try
        {
            LOGGER.log(Level.INFO, "Loading game from " + file);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(file);

            Element root = doc.getRootElement();
            Attribute ver = root.getAttribute("version");

            if (!ver.getValue().equals(Constants.xmlSnapshotVersion))
            {
                LOGGER.log(Level.SEVERE, "Can't load this savegame version.");
                dispose();
                return;
            }

            // Reset flags that are not in the savegame file.
            clearFlags();
            loadingGame = true;

            Element el = root.getChild("Variant");
            Attribute dir = el.getAttribute("dir");
            Attribute fil = el.getAttribute("file");

            VariantSupport.freshenVariant(fil.getValue(), dir.getValue());

            // then load data files
            List<Element> datafilesElements = root.getChildren("DataFile");
            Iterator<Element> it = datafilesElements.iterator();
            while (it.hasNext())
            {
                Element dea = it.next();
                String mapKey = dea.getAttributeValue("DataFileKey");
                List<?> contentList = dea.getContent();
                if (contentList.size() > 0)
                {
                    String content = ((CDATA)contentList.get(0)).getText();
                    LOGGER.log(Level.FINEST, "DataFileKey: " + mapKey
                        + " DataFileContent :\n" + content);
                    ResourceLoader
                        .putIntoFileCache(mapKey, content.getBytes());
                }
                else
                {
                    ResourceLoader.putIntoFileCache(mapKey, new byte[0]);
                }
            }

            // we're server, but the file generation process has been done
            // by loading the savefile.
            VariantSupport.loadVariant(fil.getValue(), dir.getValue(), false);

            el = root.getChild("TurnNumber");
            turnNumber = Integer.parseInt(el.getTextTrim());
            // not quite the same as it was when saved, but the idea of lastRTN
            // is only to prevent stresstest games from hanging forever... 
            lastRecruitTurnNumber = turnNumber;

            el = root.getChild("CurrentPlayer");
            activePlayerNum = Integer.parseInt(el.getTextTrim());

            el = root.getChild("CurrentPhase");
            phase = Constants.Phase
                .fromInt(Integer.parseInt(el.getTextTrim()));

            Element ct = root.getChild("Caretaker");
            List<Element> kids = ct.getChildren();
            it = kids.iterator();

            while (it.hasNext())
            {
                el = it.next();
                String creatureName = el.getAttribute("name").getValue();
                int remaining = el.getAttribute("remaining").getIntValue();
                int dead = el.getAttribute("dead").getIntValue();
                CreatureTypeServerSide creature = (CreatureTypeServerSide)getVariant()
                    .getCreatureByName(creatureName);

                caretaker.setCount(creature, remaining);
                caretaker.setDeadCount(creature, dead);
            }

            players.clear();
            if (battle != null)
            {
                server.allCleanupBattle();
            }

            // Players
            List<Element> playerElements = root.getChildren("Player");

            it = playerElements.iterator();
            while (it.hasNext())
            {
                Element pla = it.next();

                String name = pla.getAttribute("name").getValue();
                PlayerServerSide player = new PlayerServerSide(name, this);
                players.add(player);

                String type = pla.getAttribute("type").getValue();
                player.setType(type);

                String color = pla.getAttribute("color").getValue();
                player.setColor(color);

                String tower = pla.getAttribute("startingTower").getValue();
                player.setTower(tower);

                int score = pla.getAttribute("score").getIntValue();
                player.setScore(score);

                player.setDead(pla.getAttribute("dead").getBooleanValue());

                int mulligansLeft = pla.getAttribute("mulligansLeft")
                    .getIntValue();
                player.setMulligansLeft(mulligansLeft);

                player.setMovementRoll(pla.getAttribute("movementRoll")
                    .getIntValue());

                player.setTeleported(pla.getAttribute("teleported")
                    .getBooleanValue());

                player.setSummoned(pla.getAttribute("summoned")
                    .getBooleanValue());

                String playersElim = pla.getAttribute("colorsElim").getValue();
                if (playersElim == "null")
                {
                    playersElim = "";
                }
                player.setPlayersElim(playersElim);

                List<Element> legionElements = pla.getChildren("Legion");
                Iterator<Element> it2 = legionElements.iterator();
                while (it2.hasNext())
                {
                    Element leg = it2.next();
                    readLegion(leg, player);
                }
            }
            // Need all players' playersElim set up before we can do this.
            Iterator<PlayerServerSide> itPl = players.iterator();
            while (itPl.hasNext())
            {
                PlayerServerSide player = itPl.next();
                player.computeMarkersAvailable();
            }

            // Battle stuff
            Element bat = root.getChild("Battle");
            if (bat != null)
            {
                String engagementHexLabel = bat.getAttribute("masterHexLabel")
                    .getValue();
                int battleTurnNum = bat.getAttribute("turnNumber")
                    .getIntValue();
                String battleActivePlayerName = bat.getAttribute(
                    "activePlayer").getValue();
                Constants.BattlePhase battlePhase = Constants.BattlePhase
                    .fromInt(bat.getAttribute("phase").getIntValue());
                int summonState = bat.getAttribute("summonState")
                    .getIntValue();
                int carryDamage = bat.getAttribute("carryDamage")
                    .getIntValue();
                boolean driftDamageApplied = bat.getAttribute(
                    "driftDamageApplied").getBooleanValue();

                List<Element> cts = bat.getChildren("CarryTarget");
                Set<String> carryTargets = new HashSet<String>();
                Iterator<Element> it2 = cts.iterator();
                while (it2.hasNext())
                {
                    Element cart = it2.next();
                    carryTargets.add(cart.getTextTrim());
                }

                PlayerServerSide attackingPlayer = getActivePlayer();
                LegionServerSide attacker = getFirstFriendlyLegion(
                    engagementHexLabel, attackingPlayer);
                LegionServerSide defender = getFirstEnemyLegion(
                    engagementHexLabel, attackingPlayer);

                int activeLegionNum;
                if (battleActivePlayerName.equals(attackingPlayer.getName()))
                {
                    activeLegionNum = Constants.ATTACKER;
                }
                else
                {
                    activeLegionNum = Constants.DEFENDER;
                }

                battle = new BattleServerSide(this, attacker.getMarkerId(),
                    defender.getMarkerId(), activeLegionNum,
                    engagementHexLabel, battleTurnNum, battlePhase);
                battle.setSummonState(summonState);
                battle.setCarryDamage(carryDamage);
                battle.setDriftDamageApplied(driftDamageApplied);
                battle.setCarryTargets(carryTargets);
                battle.init();
            }

            // History
            history = new History();
            Element his = root.getChild("History");
            history.copyTree(his);

            initServer();
            // Remaining stuff has been moved to loadGame2()

            // Some more stuff is done from loadGame2() when the last
            // expected client has connected.
            // Main thread has now nothing to do any more, can wait
            // until game finishes.
            cleanupWhenGameOver();
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE, "Tried to load corrupt savegame", ex);
            dispose();
            return;
        }
    }

    // JDOM lacks generics, so we need casts
    @SuppressWarnings("unchecked")
    private void readLegion(Element leg, PlayerServerSide player)
        throws DataConversionException
    {
        String markerId = leg.getAttribute("name").getValue();
        String currentHexLabel = leg.getAttribute("currentHex").getValue();
        String startingHexLabel = leg.getAttribute("startingHex").getValue();
        boolean moved = leg.getAttribute("moved").getBooleanValue();
        int entrySide = leg.getAttribute("entrySide").getIntValue();
        String parentId = leg.getAttribute("parent").getValue();
        if (parentId.equals("null"))
        {
            parentId = null;
        }
        String recruitName = leg.getAttribute("recruitName").getValue();
        if (recruitName.equals("null"))
        {
            recruitName = null;
        }

        int battleTally = leg.getAttribute("battleTally").getIntValue();

        // Critters
        // find legion for them, if it doesn't exist create one
        net.sf.colossus.server.LegionServerSide legion = player
            .getLegionByMarkerId(markerId);
        if (legion == null)
        {
            // TODO can there ever be a legion before? If not: collect all data
            // first (including critters) and then create the legion in one go
            legion = new net.sf.colossus.server.LegionServerSide(markerId,
                parentId, currentHexLabel, startingHexLabel, player, this);
            player.addLegion(legion);
        }

        List<Element> creatureElements = leg.getChildren("Creature");
        for (Element cre : creatureElements)
        {
            String name = cre.getAttribute("name").getValue();
            CreatureServerSide critter = new CreatureServerSide(
                (CreatureTypeServerSide)getVariant().getCreatureByName(name),
                null, this);

            // Battle stuff
            if (cre.getAttribute("hits") != null)
            {
                int hits = cre.getAttribute("hits").getIntValue();

                critter.setHits(hits);

                String terrain = getVariant().getMasterBoard().getHexByLabel(
                    currentHexLabel).getTerrainName();
                String currentBattleHexLabel = cre.getAttribute("currentHex")
                    .getValue();
                BattleHex currentBattleHex = HexMap.getHexByLabel(terrain,
                    currentBattleHexLabel);
                critter.setCurrentHex(currentBattleHex);

                String startingBattleHexLabel = cre
                    .getAttribute("startingHex").getValue();
                BattleHex startingBattleHex = HexMap.getHexByLabel(terrain,
                    startingBattleHexLabel);
                critter.setStartingHex(startingBattleHex);

                boolean struck = cre.getAttribute("struck").getBooleanValue();

                critter.setStruck(struck);
            }

            legion.addCritter(critter);
        }

        legion.setMoved(moved);
        legion.setRecruitName(recruitName);
        legion.setEntrySide(entrySide);
        legion.addToBattleTally(battleTally);
    }

    /* Called from the last SocketServerThread connecting 
     *  ( = when expected nr. of clients has connected).
     */
    void loadGame2()
    {
        server.allSetColor();

        // We need to set the autoPlay option before loading the board,
        // so that we can avoid showing boards for AI players.
        syncAutoPlay();
        syncOptions();

        server.allUpdatePlayerInfo(true);
        history.fireEventsFromXML(server);
        server.allFullyUpdateLegionStatus();
        server.allUpdatePlayerInfo(false);

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
    private String latestSaveFilename(String[] filenames)
    {
        return Collections.max(Arrays.asList(filenames),
            new Comparator<String>()
            {
                public int compare(String s1, String s2)
                {
                    long diff = (numberValue(s1) - numberValue(s2));

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
    List<CreatureTypeServerSide> findEligibleRecruits(String markerId,
        String hexLabel)
    {
        LegionServerSide legion = getLegionByMarkerId(markerId);
        List<CreatureTypeServerSide> recruits;

        MasterHex hex = getVariant().getMasterBoard().getHexByLabel(hexLabel);
        String terrain = hex.getTerrain();

        recruits = new ArrayList<CreatureTypeServerSide>();
        List<CreatureType> tempRecruits = TerrainRecruitLoader
            .getPossibleRecruits(terrain, hexLabel);
        List<CreatureType> recruiters = TerrainRecruitLoader
            .getPossibleRecruiters(terrain, hexLabel);

        Iterator<CreatureType> lit = tempRecruits.iterator();

        while (lit.hasNext())
        {
            CreatureTypeServerSide creature = (CreatureTypeServerSide)lit
                .next();
            Iterator<CreatureType> itRec = recruiters.iterator();

            while (itRec.hasNext())
            {
                CreatureTypeServerSide lesser = (CreatureTypeServerSide)itRec
                    .next();

                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser,
                    creature, terrain, hexLabel) <= legion.numCreature(lesser))
                    && (recruits.indexOf(creature) == -1))
                {
                    recruits.add(creature);
                }
            }
        }

        // Make sure that the potential recruits are available.
        Iterator<CreatureTypeServerSide> it = recruits.iterator();
        while (it.hasNext())
        {
            CreatureTypeServerSide recruit = it.next();
            if (caretaker.getCount(recruit) < 1)
            {
                it.remove();
            }
        }
        return recruits;
    }

    /** Return a list of eligible recruiter creatures. */
    List<CreatureType> findEligibleRecruiters(String markerId,
        String recruitName)
    {
        List<CreatureType> recruiters;
        CreatureTypeServerSide recruit = (CreatureTypeServerSide)getVariant()
            .getCreatureByName(recruitName);
        if (recruit == null)
        {
            return new ArrayList<CreatureType>();
        }

        LegionServerSide legion = getLegionByMarkerId(markerId);
        String hexLabel = legion.getCurrentHexLabel();
        MasterHex hex = getVariant().getMasterBoard().getHexByLabel(hexLabel);
        String terrain = hex.getTerrain();
        recruiters = TerrainRecruitLoader.getPossibleRecruiters(terrain,
            hexLabel);
        Iterator<CreatureType> it = recruiters.iterator();
        while (it.hasNext())
        {
            CreatureType possibleRecruiter = it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain, hexLabel);

            if (needed < 1 || needed > legion.numCreature(possibleRecruiter))
            {
                // Zap this possible recruiter.
                it.remove();
            }
        }
        return recruiters;
    }

    /**
     * Return true if this legion can recruit this recruit
     * without disclosing a recruiter.
     */
    private boolean anonymousRecruitLegal(LegionServerSide legion,
        CreatureTypeServerSide recruit)
    {
        return TerrainRecruitLoader.anonymousRecruitLegal(recruit, legion
            .getCurrentHex().getTerrain(), legion.getCurrentHex().getLabel());
    }

    /** Add recruit to legion. */
    void doRecruit(LegionServerSide legion, CreatureTypeServerSide recruit,
        CreatureTypeServerSide recruiter)
    {
        if (recruit == null)
        {
            LOGGER.log(Level.SEVERE, "null recruit in Game.doRecruit()");
            return;
        }
        // Check for recruiter legality.
        List<CreatureType> recruiters = findEligibleRecruiters(legion
            .getMarkerId(), recruit.getName());

        if (recruiter == null)
        {
            // If recruiter can be anonymous, then this is okay.
            if (!anonymousRecruitLegal(legion, recruit))
            {
                LOGGER.log(Level.SEVERE, "null recruiter in Game.doRecruit()");
                // XXX Let it go for now  Should return later
            }
            else
            {
                LOGGER.log(Level.FINEST, "null recruiter okay");
            }
        }
        else if (!recruiters.contains(recruiter))
        {
            LOGGER.log(Level.SEVERE, "Illegal recruiter "
                + recruiter.getName() + " for recruit " + recruit.getName());
            return;
        }

        lastRecruitTurnNumber = turnNumber;

        if (legion.addCreature(recruit, true))
        {
            MasterHex hex = legion.getCurrentHex();
            int numRecruiters = 0;

            if (recruiter != null)
            {
                // Mark the recruiter(s) as visible.
                numRecruiters = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    recruiter, recruit, hex.getTerrain(), hex.getLabel());
            }

            LOGGER.log(Level.INFO, "Legion "
                + legion.getLongMarkerName()
                + " in "
                + hex.getDescription()
                + " recruits "
                + recruit.getName()
                + " with "
                + (recruiter == null ? "nothing" : numRecruiters
                    + " "
                    + (numRecruiters > 1 ? recruiter.getPluralName()
                        : recruiter.getName())));

            // Recruits are one to a customer.
            legion.setRecruitName(recruit.getName());
            reinforcing = false;
        }
    }

    /** Return a list of names of angel types that can be acquired. */
    List<String> findEligibleAngels(LegionServerSide legion, int score)
    {
        if (legion.getHeight() >= 7)
        {
            return null;
        }
        List<String> recruits = new ArrayList<String>();
        String terrain = legion.getCurrentHex().getTerrain();
        List<String> allRecruits = TerrainRecruitLoader
            .getRecruitableAcquirableList(terrain, score);
        Iterator<String> it = allRecruits.iterator();
        while (it.hasNext())
        {
            String name = it.next();

            if (caretaker.getCount(getVariant().getCreatureByName(name)) >= 1
                && !recruits.contains(name))
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
            server.stopServerRunning();
        }
        notifyWebServer.serverStoppedRunning();
        notifyWebServer = null;
    }

    private void placeInitialLegion(PlayerServerSide player, String markerId)
    {
        String name = player.getName();
        player.selectMarkerId(markerId);
        LOGGER.log(Level.INFO, name + " selects initial marker");

        // Lookup coords for chit starting from player[i].getTower()
        String hexLabel = player.getTower();
        LegionServerSide legion = LegionServerSide.getStartingLegion(markerId,
            hexLabel, player, this);
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
    private synchronized Set<String> findNormalMoves(MasterHex hex,
        LegionServerSide legion, int roll, int block, int cameFrom,
        boolean ignoreFriends)
    {
        Set<String> set = new HashSet<String>();
        String hexLabel = hex.getLabel();
        PlayerServerSide player = legion.getPlayer();

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
                    set.add(hexLabel
                        + ":"
                        + BattleMap
                            .entrySideName(findEntrySide(hex, cameFrom)));
                }
            }
            return set;
        }

        if (roll == 0)
        {
            // XXX fix
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            List<LegionServerSide> legions = player.getLegions();
            Iterator<LegionServerSide> it = legions.iterator();
            while (it.hasNext())
            {
                // Account for spin cycles.
                LegionServerSide otherLegion = it.next();

                if (!ignoreFriends && otherLegion != legion
                    && hexLabel.equals(otherLegion.getCurrentHexLabel()))
                {
                    return set;
                }
            }

            if (cameFrom != -1)
            {
                set.add(hexLabel + ":"
                    + BattleMap.entrySideName(findEntrySide(hex, cameFrom)));
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
    private Set<String> findNearbyUnoccupiedHexes(MasterHex hex,
        LegionServerSide legion, int roll, int cameFrom, boolean ignoreFriends)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.
        String hexLabel = hex.getLabel();
        Set<String> set = new HashSet<String>();
        if (!isOccupied(hexLabel))
        {
            set.add(hexLabel);
        }
        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom
                    && (hex.getExitType(i) != Constants.NONE || hex
                        .getEntranceType(i) != Constants.NONE))
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
    Set<String> listAllMoves(LegionServerSide legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends)
    {
        Set<String> set = listNormalMoves(legion, hex, movementRoll,
            ignoreFriends);
        set
            .addAll(listTeleportMoves(legion, hex, movementRoll, ignoreFriends));
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
    Set<String> listNormalMoves(LegionServerSide legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends)
    {
        if (legion.hasMoved())
        {
            return new HashSet<String>();
        }
        Set<String> tuples = findNormalMoves(hex, legion, movementRoll,
            findBlock(hex), Constants.NOWHERE, ignoreFriends);

        // Extract just the hexLabels from the hexLabel:entrySide tuples.
        Set<String> hexLabels = new HashSet<String>();
        Iterator<String> it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = it.next();
            List<String> parts = Split.split(':', tuple);
            String hexLabel = parts.get(0);

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
    Set<String> listTeleportMoves(LegionServerSide legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends)
    {
        PlayerServerSide player = legion.getPlayer();
        Set<String> set = new HashSet<String>();
        if (movementRoll != 6 || legion.hasMoved() || player.hasTeleported())
        {
            return set;
        }

        // Tower teleport
        if (HexMap.terrainIsTower(hex.getTerrain()) && legion.numLords() > 0
            && towerTeleportAllowed())
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
                Set<String> towerSet = getVariant().getMasterBoard()
                    .getTowerSet();
                Iterator<String> it = towerSet.iterator();
                while (it.hasNext())
                {
                    String hexLabel = it.next();

                    if (getVariant().getMasterBoard().getHexByLabel(hexLabel) != null)
                    {
                        if ((!isOccupied(hexLabel) || (ignoreFriends && getNumEnemyLegions(
                            hexLabel, player) == 0))
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
                Set<String> towerSet = getVariant().getMasterBoard()
                    .getTowerSet();
                Iterator<String> it = towerSet.iterator();
                while (it.hasNext())
                {
                    String hexLabel = it.next();

                    set.remove(hexLabel);
                }
            }
        }

        // Titan teleport
        if (player.canTitanTeleport() && legion.hasTitan()
            && titanTeleportAllowed())
        {
            // Mark every hex containing an enemy stack that does not
            // already contain a friendly stack.
            Iterator<LegionServerSide> it = getAllEnemyLegions(player)
                .iterator();
            while (it.hasNext())
            {
                LegionServerSide other = it.next();
                {
                    String hexLabel = other.getCurrentHexLabel();
                    if (!isEngagement(hexLabel) || ignoreFriends)
                    {
                        set.add(hexLabel);
                    }
                }
            }
        }
        set.remove(null);
        set.remove("null");
        return set;
    }

    /** Return a Set of Strings "Left" "Right" or "Bottom" describing
     *  possible entry sides.  If the hex is unoccupied, just return
     *  one entry side since it doesn't matter. */
    Set<String> listPossibleEntrySides(String markerId, String targetHexLabel,
        boolean teleport)
    {
        Set<String> entrySides = new HashSet<String>();
        LegionServerSide legion = getLegionByMarkerId(markerId);
        PlayerServerSide player = legion.getPlayer();
        int movementRoll = player.getMovementRoll();
        MasterHex currentHex = legion.getCurrentHex();
        MasterHex targetHex = getVariant().getMasterBoard().getHexByLabel(
            targetHexLabel);

        if (teleport)
        {
            if (listTeleportMoves(legion, currentHex, movementRoll, false)
                .contains(targetHexLabel))
            {
                // Startlisted terrain only have bottom entry side.
                // Don't bother finding more than one entry side if unoccupied.
                if (!isOccupied(targetHexLabel)
                    || HexMap.terrainHasStartlist(targetHex.getTerrain()))
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
        Set<String> tuples = findNormalMoves(currentHex, legion, movementRoll,
            findBlock(currentHex), Constants.NOWHERE, false);
        Iterator<String> it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = it.next();
            List<String> parts = Split.split(':', tuple);
            String hl = parts.get(0);

            if (hl.equals(targetHexLabel))
            {
                String buf = parts.get(1);

                entrySides.add(buf);

                // Clemens 4.10.2007:
                // This optimization can lead to problems ("Illegal entry side")
                // in mountains/tundra on a movement roll 4, when client and 
                // server store the items in their Move-hashmaps in different
                // order (different java version, platform, ... ?)
                // So, removed this optimization to see whether it fixes the bug:
                //  [colossus-Bugs-1789116 ] illegal move: 29 plain to 2000 tundra 
                /*
                 // Don't bother finding more than one entry side if unoccupied.
                 if (!isOccupied(targetHexLabel))
                 {
                 return entrySides;
                 }
                 */
            }
        }
        return entrySides;
    }

    boolean isEngagement(String hexLabel)
    {
        if (getNumLegions(hexLabel) > 1)
        {
            List<String> markerIds = getLegionMarkerIds(hexLabel);
            Iterator<String> it = markerIds.iterator();
            String markerId = it.next();
            Player player = getPlayerByMarkerId(markerId);

            while (it.hasNext())
            {
                markerId = it.next();
                if (getPlayerByMarkerId(markerId) != player)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return set of hexLabels for engagements found. */
    synchronized Set<String> findEngagements()
    {
        Set<String> set = new HashSet<String>();
        PlayerServerSide player = getActivePlayer();

        List<LegionServerSide> legions = player.getLegions();
        Iterator<LegionServerSide> it = legions.iterator();

        while (it.hasNext())
        {
            LegionServerSide legion = it.next();
            String hexLabel = legion.getCurrentHexLabel();

            if (getNumEnemyLegions(hexLabel, player) > 0)
            {
                set.add(hexLabel);
            }
        }
        return set;
    }

    void createSummonAngel(LegionServerSide attacker)
    {
        if (!isOver())
        {
            summoning = true;
            server.createSummonAngel(attacker);
        }
    }

    /** Called locally and from Battle. */
    void reinforce(LegionServerSide legion)
    {
        reinforcing = true;
        server.reinforce(legion);
    }

    void doneReinforcing()
    {
        reinforcing = false;
        checkEngagementDone();
    }

    // Called by both human and AI.
    void doSummon(LegionServerSide legion, LegionServerSide donor,
        CreatureTypeServerSide angel)
    {
        PlayerServerSide player = getActivePlayer();

        if (angel != null && donor != null && legion.canSummonAngel())
        {
            // Only one angel can be summoned per turn.
            player.setSummoned(true);

            // Move the angel or archangel.
            donor.removeCreature(angel, false, false);
            legion.addCreature(angel, false);

            server.allTellRemoveCreature(donor.getMarkerId(), angel.getName(),
                true, Constants.reasonSummon);
            server.allTellAddCreature(legion.getMarkerId(), angel.getName(),
                true, Constants.reasonSummon);

            server.allTellDidSummon(legion.getMarkerId(), donor.getMarkerId(),
                angel.getName());

            LOGGER.log(Level.INFO, "One " + angel.getName()
                + " is summoned from legion " + donor.getLongMarkerName()
                + " into legion " + legion.getLongMarkerName());
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
            checkEngagementDone();
        }
    }

    BattleServerSide getBattle()
    {
        return battle;
    }

    synchronized void finishBattle(String hexLabel, boolean attackerEntered,
        int points, int turnDone)
    {
        battle.cleanRefs();
        battle = null;
        server.allCleanupBattle();
        LegionServerSide winner = null;

        // Handle any after-battle angel summoning or recruiting.
        if (getNumLegions(hexLabel) == 1)
        {
            winner = getFirstLegion(hexLabel);

            // Make all creatures in the victorious legion visible.
            server.allRevealLegion(winner, Constants.reasonWinner);
            // Remove battle info from winning legion and its creatures.
            winner.clearBattleInfo();

            if (winner.getPlayer() == getActivePlayer())
            {
                // Attacker won, so possibly summon angel.
                if (winner.canSummonAngel())
                {
                    createSummonAngel(winner);
                }
            }
            else
            {
                // Defender won, so possibly recruit reinforcement.
                if (attackerEntered && winner.canRecruit())
                {
                    LOGGER.log(Level.FINEST,
                        "Calling Game.reinforce() from Game.finishBattle()");
                    reinforce(winner);
                }
            }
        }
        battleInProgress = false;

        setEngagementResult(Constants.erMethodFight, winner, points, turnDone);
        checkEngagementDone();
    }

    /** Return a set of hexLabels. */
    synchronized Set<String> findSummonableAngels(String markerId)
    {
        LegionServerSide legion = getLegionByMarkerId(markerId);
        Set<String> set = new HashSet<String>();
        List<LegionServerSide> legions = legion.getPlayer().getLegions();
        Iterator<LegionServerSide> it = legions.iterator();
        while (it.hasNext())
        {
            LegionServerSide candidate = it.next();
            if (candidate != legion)
            {
                String hexLabel = candidate.getCurrentHexLabel();
                boolean hasSummonable = false;
                List<CreatureType> summonableList = getVariant()
                    .getSummonableCreatureTypes();
                Iterator<CreatureType> sumIt = summonableList.iterator();
                while (sumIt.hasNext() && !hasSummonable)
                {
                    CreatureType c = sumIt.next();

                    hasSummonable = hasSummonable
                        || (candidate.numCreature(c) > 0);
                }
                if (hasSummonable && !isEngagement(hexLabel))
                {
                    set.add(hexLabel);
                }
            }
        }
        return set;
    }

    /** Return true and call Server.didSplit() if the split succeeded.
     *  Return false if it failed. */
    boolean doSplit(String parentId, String childId, String results)
    {
        LegionServerSide legion = getLegionByMarkerId(parentId);
        PlayerServerSide player = legion.getPlayer();

        // Need a legion marker to split.
        if (!player.isMarkerAvailable(childId))
        {
            LOGGER.log(Level.SEVERE, "Marker " + childId
                + " is not available.");
            return false;
        }

        // Pre-split legion must have 4+ creatures.
        if (legion.getHeight() < 4)
        {
            LOGGER.log(Level.SEVERE, "Legion " + parentId
                + " is too short to split.");
            return false;
        }

        if (results == null)
        {
            LOGGER.log(Level.FINEST, "Empty split list (" + parentId + ", "
                + childId + ")");
            return false;
        }
        List<String> strings = Split.split(',', results);
        List<CreatureTypeServerSide> creatures = new ArrayList<CreatureTypeServerSide>();
        Iterator<String> it = strings.iterator();
        while (it.hasNext())
        {
            String name = it.next();
            CreatureTypeServerSide creature = (CreatureTypeServerSide)getVariant()
                .getCreatureByName(name);
            creatures.add(creature);
        }

        // Each legion must have 2+ creatures after the split.
        if (creatures.size() < 2 || legion.getHeight() - creatures.size() < 2)
        {
            LOGGER.log(Level.FINEST, "Too small/big split list (" + parentId
                + ", " + childId + ")");
            return false;
        }

        // All creatures in results must be in the legion.
        // WARNING: Legion.getCritters() return Critters,
        // not Creature - different things now.
        // so we must clone "by hand" the List.
        List<CreatureServerSide> tempCritters = legion.getCritters();
        List<CreatureTypeServerSide> tempCreatures = new ArrayList<CreatureTypeServerSide>();

        Iterator<CreatureServerSide> itCrit = tempCritters.iterator();
        while (itCrit.hasNext())
        {
            tempCreatures.add((itCrit.next()).getCreature());
        }
        Iterator<CreatureTypeServerSide> itCre = creatures.iterator();
        while (itCre.hasNext())
        {
            CreatureTypeServerSide creature = itCre.next();
            if (!tempCreatures.remove(creature))
            {
                LOGGER.log(Level.FINEST,
                    "Unavailable creature in split list (" + parentId + ", "
                        + childId + ") : " + creature.getName());
                return false;
            }
        }

        if (getTurnNumber() == 1)
        {
            // Only allow a single split on turn 1.
            if (player.getNumLegions() > 1)
            {
                LOGGER.log(Level.SEVERE, "Cannot split twice on Turn 1.");
                return false;
            }
            // Each stack must contain exactly 4 creatures.
            if (creatures.size() != 4)
            {
                return false;
            }
            // Each stack must contain exactly 1 lord.
            int numLords = 0;

            itCre = creatures.iterator();
            while (itCre.hasNext())
            {
                CreatureTypeServerSide creature = itCre.next();
                if (creature.isLord())
                {
                    numLords++;
                }
            }
            if (numLords != 1)
            {
                return false;
            }
        }

        LegionServerSide newLegion = legion.split(creatures, childId);
        if (newLegion == null)
        {
            return false;
        }

        String hexLabel = legion.getCurrentHexLabel();
        server.didSplit(hexLabel, parentId, childId, newLegion.getHeight());

        // viewableAll depends on the splitPrediction to tell then true contents,
        // and viewableOwn it does not harm; it only helps the AIs :)

        String viewModeOpt = options.getStringOption(Options.viewMode);
        int viewModeOptNum = options.getNumberForViewMode(viewModeOpt);

        if (viewModeOptNum == Options.viewableAllNum
            || viewModeOptNum == Options.viewableOwnNum)
        {
            server.allRevealLegion(legion, Constants.reasonSplit);
            server.allRevealLegion(newLegion, Constants.reasonSplit);
        }
        else
        {
            server.oneRevealLegion(legion, player, Constants.reasonSplit);
            server.oneRevealLegion(newLegion, player, Constants.reasonSplit);
        }
        return true;
    }

    /** Move the legion to the hex if legal.  Return a string telling
     *  the reason why it is illegal, or null if ok and move was done.
     */
    String doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord)
    {
        LegionServerSide legion = getLegionByMarkerId(markerId);
        if (legion == null)
        {
            return "Legion null";
        }

        PlayerServerSide player = legion.getPlayer();
        // Verify that the move is legal.
        if (teleport)
        {
            if (!listTeleportMoves(legion, legion.getCurrentHex(),
                player.getMovementRoll(), false).contains(hexLabel))
            {
                String marker = legion.getMarkerId() + " "
                    + legion.getMarkerName();
                String from = legion.getCurrentHex().getLabel();
                Set<String> set = listTeleportMoves(legion, legion
                    .getCurrentHex(), player.getMovementRoll(), false);
                return "List for teleport moves " + set.toString() + " of "
                    + marker + " from " + from + " does not contain '"
                    + hexLabel + "'";
            }
        }
        else
        {
            if (!listNormalMoves(legion, legion.getCurrentHex(),
                player.getMovementRoll(), false).contains(hexLabel))
            {
                String marker = legion.getMarkerId() + " "
                    + legion.getMarkerName();
                String from = legion.getCurrentHex().getLabel();
                Set<String> set = listNormalMoves(legion, legion
                    .getCurrentHex(), player.getMovementRoll(), false);
                return "List for normal moves " + set.toString() + " + of "
                    + marker + " from " + from + " does not contain '"
                    + hexLabel + "'";
            }
        }

        // Verify that the entry side is legal.
        Set<String> legalSides = listPossibleEntrySides(markerId, hexLabel,
            teleport);
        if (!legalSides.contains(entrySide))
        {
            return "EntrySide '" + entrySide + "' is not valid, valid are: "
                + legalSides.toString();
        }

        MasterHex hex = getVariant().getMasterBoard().getHexByLabel(hexLabel);
        // If this is a tower hex, the only entry side is the bottom.
        if (HexMap.terrainHasStartlist(hex.getTerrain())
            && !entrySide.equals(Constants.bottom))
        {
            LOGGER.log(Level.WARNING, "Tried to enter invalid side of tower");
            entrySide = Constants.bottom;
        }

        // If the legion teleported, reveal a lord.
        if (teleport)
        {
            // Verify teleporting lord.
            if (teleportingLord == null
                || !legion.listTeleportingLords(hexLabel).contains(
                    teleportingLord))
            {
                if (teleportingLord == null)
                {
                    return "teleportingLord null";
                }
                return "list of telep. lords "
                    + legion.listTeleportingLords(hexLabel).toString()
                    + " does not contain '" + teleportingLord + "'";
            }
            List<String> creatureNames = new ArrayList<String>();
            creatureNames.add(teleportingLord);
            server.allRevealCreatures(legion, creatureNames,
                Constants.reasonTeleport);
        }
        legion.moveToHex(hex, entrySide, teleport, teleportingLord);
        return null;
    }

    synchronized void engage(String hexLabel)
    {
        // Do not allow clicking on engagements if one is
        // already being resolved.
        if (isEngagement(hexLabel) && !engagementInProgress)
        {
            engagementInProgress = true;
            PlayerServerSide player = getActivePlayer();
            LegionServerSide attacker = getFirstFriendlyLegion(hexLabel,
                player);
            LegionServerSide defender = getFirstEnemyLegion(hexLabel, player);

            server.allTellEngagement(hexLabel, attacker, defender);

            attacker.sortCritters();
            defender.sortCritters();

            server.oneRevealLegion(attacker, defender.getPlayer(),
                Constants.reasonEngaged);
            server.oneRevealLegion(defender, attacker.getPlayer(),
                Constants.reasonEngaged);

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
        else
        {
            LOGGER.log(Level.FINEST, "illegal call to Game.engage() "
                + engagementInProgress);
        }
    }

    // Defender did not flee; attacker may concede early.
    private void engage2(String hexLabel)
    {
        PlayerServerSide player = getActivePlayer();
        LegionServerSide attacker = getFirstFriendlyLegion(hexLabel, player);
        LegionServerSide defender = getFirstEnemyLegion(hexLabel, player);

        server.askConcede(attacker, defender);
    }

    // Attacker did not concede early; negotiate.
    private void engage3(String hexLabel)
    {
        PlayerServerSide player = getActivePlayer();
        LegionServerSide attacker = getFirstFriendlyLegion(hexLabel, player);
        LegionServerSide defender = getFirstEnemyLegion(hexLabel, player);

        attackerProposals.clear();
        defenderProposals.clear();
        server.twoNegotiate(attacker, defender);
    }

    void flee(String markerId)
    {
        LegionServerSide defender = getLegionByMarkerId(markerId);
        String hexLabel = defender.getCurrentHexLabel();
        LegionServerSide attacker = getFirstEnemyLegion(hexLabel, defender
            .getPlayer());

        handleConcession(defender, attacker, true);
    }

    void concede(String markerId)
    {
        if (battleInProgress)
        {
            battle.concede(getLegionByMarkerId(markerId).getPlayer());
        }
        else
        {
            LegionServerSide attacker = getLegionByMarkerId(markerId);
            String hexLabel = attacker.getCurrentHexLabel();
            LegionServerSide defender = getFirstEnemyLegion(hexLabel, attacker
                .getPlayer());

            handleConcession(attacker, defender, false);
        }
    }

    void doNotFlee(String markerId)
    {
        LegionServerSide defender = getLegionByMarkerId(markerId);
        String hexLabel = defender.getCurrentHexLabel();

        engage2(hexLabel);
    }

    /** Used only for pre-battle attacker concession. */
    void doNotConcede(String markerId)
    {
        LegionServerSide attacker = getLegionByMarkerId(markerId);
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

        Proposal proposal = Proposal.makeFromString(proposalString);
        final Set<Proposal> ourProposals;
        final Set<Proposal> opponentProposals;

        if (playerName.equals(getActivePlayerName()))
        {
            ourProposals = attackerProposals;
            opponentProposals = defenderProposals;
        }
        else
        {
            ourProposals = defenderProposals;
            opponentProposals = attackerProposals;
        }

        // If this player wants to fight, cancel negotiations.
        if (proposal.isFight())
        {
            LegionServerSide attacker = getLegionByMarkerId(proposal
                .getAttackerId());
            String hexLabel = attacker.getCurrentHexLabel();
            fight(hexLabel);
        }

        // If this proposal matches an earlier one from the other player,
        // settle the engagement.
        else if (opponentProposals.contains(proposal))
        {
            handleNegotiation(proposal);
        }

        // Otherwise remember this proposal and continue.
        else
        {
            ourProposals.add(proposal);
            Player other = null;
            if (playerName.equals(getActivePlayerName()))
            {
                LegionServerSide defender = getLegionByMarkerId(proposal
                    .getDefenderId());

                other = defender.getPlayer();
            }
            else
            {
                other = getActivePlayer();
            }

            // Tell the other player about the proposal.
            server.tellProposal(other, proposal);
        }
    }

    /** Synchronized to keep both negotiators from racing to fight. */
    synchronized void fight(String hexLabel)
    {
        if (!battleInProgress)
        {
            PlayerServerSide player = getActivePlayer();
            LegionServerSide attacker = getFirstFriendlyLegion(hexLabel,
                player);
            LegionServerSide defender = getFirstEnemyLegion(hexLabel, player);

            // If the second player clicks Fight from the negotiate
            // dialog late, just exit.
            if (attacker == null || defender == null)
            {
                return;
            }

            battleInProgress = true;

            // Reveal both legions to all players.
            server.allRevealEngagedLegion(attacker, true,
                Constants.reasonBattleStarts);
            server.allRevealEngagedLegion(defender, false,
                Constants.reasonBattleStarts);

            battle = new BattleServerSide(this, attacker.getMarkerId(),
                defender.getMarkerId(), Constants.DEFENDER, hexLabel, 1,
                Constants.BattlePhase.MOVE);
            battle.init();
        }
    }

    private synchronized void handleConcession(LegionServerSide loser,
        LegionServerSide winner, boolean fled)
    {
        // Figure how many points the victor receives.
        int points = loser.getPointValue();

        if (fled)
        {
            points /= 2;
            LOGGER.log(Level.INFO, "Legion " + loser.getLongMarkerName()
                + " flees from legion " + winner.getLongMarkerName());
        }
        else
        {
            LOGGER.log(Level.INFO, "Legion " + loser.getLongMarkerName()
                + " concedes to legion " + winner.getLongMarkerName());
        }

        // Add points, and angels if necessary.
        winner.addPoints(points);
        // Remove any fractional points.
        winner.getPlayer().truncScore();

        // Need to grab the player reference before the legion is
        // removed.
        PlayerServerSide losingPlayer = loser.getPlayer();

        String reason = fled ? Constants.reasonFled
            : Constants.reasonConcession;
        server.allRevealEngagedLegion(loser, losingPlayer
            .equals(getActivePlayer()), reason);

        // server.allRemoveLegion(loser.getMarkerId());

        // If this was the titan stack, its owner dies and gives half
        // points to the victor.
        if (loser.hasTitan())
        {
            // first remove dead legion, then the rest. Cannot do the
            // loser.remove outside/before the if (or would need to store
            // the hasTitan information as extra boolean)
            loser.remove();
            losingPlayer.die(winner.getPlayer(), true);
        }
        else
        {
            // simply remove the dead legion.
            loser.remove();
        }

        // No recruiting or angel summoning is allowed after the
        // defender flees or the attacker concedes before entering
        // the battle.
        String method = fled ? Constants.erMethodFlee
            : Constants.erMethodConcede;
        setEngagementResult(method, winner, points, 0);
        checkEngagementDone();
    }

    private synchronized void handleNegotiation(Proposal results)
    {
        LegionServerSide attacker = getLegionByMarkerId(results
            .getAttackerId());
        LegionServerSide defender = getLegionByMarkerId(results
            .getDefenderId());
        LegionServerSide winner = null;
        int points = 0;

        if (results.isMutual())
        {
            // Remove both legions and give no points.
            attacker.remove();
            defender.remove();

            LOGGER.log(Level.INFO, attacker.getLongMarkerName() + " and "
                + defender.getLongMarkerName()
                + " agree to mutual elimination");

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
                attacker.getPlayer().die(defender.getPlayer(), true);
            }

            else if (defender.hasTitan())
            {
                defender.getPlayer().die(attacker.getPlayer(), true);
            }
        }
        else
        {
            // One legion was eliminated during negotiations.
            winner = getLegionByMarkerId(results.getWinnerId());
            LegionServerSide loser;
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
            List<String> winnerLosses = results.getWinnerLosses();
            Iterator<String> it = winnerLosses.iterator();
            while (it.hasNext())
            {
                String creatureName = it.next();
                log.append(creatureName);
                if (it.hasNext())
                {
                    log.append(", ");
                }
                CreatureTypeServerSide creature = (CreatureTypeServerSide)getVariant()
                    .getCreatureByName(creatureName);
                winner.removeCreature(creature, true, true);
                server.allTellRemoveCreature(winner.getMarkerId(),
                    creatureName, true, Constants.reasonNegotiated);
            }
            LOGGER.log(Level.INFO, log.toString());

            server.oneRevealLegion(winner, attacker.getPlayer(),
                Constants.reasonNegotiated);
            server.oneRevealLegion(winner, defender.getPlayer(),
                Constants.reasonNegotiated);

            points = loser.getPointValue();

            PlayerServerSide losingPlayer = loser.getPlayer();

            // Remove the losing legion.
            loser.remove();

            // Add points, and angels if necessary.
            winner.addPoints(points);

            LOGGER.log(Level.INFO, "Legion " + loser.getLongMarkerName()
                + " is eliminated by legion " + winner.getLongMarkerName()
                + " via negotiation");

            // If this was the titan stack, its owner dies and gives half
            // points to the victor.
            if (loser.hasTitan())
            {
                losingPlayer.die(winner.getPlayer(), true);
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
                if (attacker.getHeight() < 7
                    && !attacker.getPlayer().hasSummoned())
                {
                    // If the attacker won the battle by agreement,
                    // he may summon an angel.
                    createSummonAngel(attacker);
                }
            }
        }

        setEngagementResult(Constants.erMethodNegotiate, winner, points, 0);
        checkEngagementDone();
    }

    synchronized void askAcquireAngel(PlayerServerSide player,
        String markerId, List<String> recruits)
    {
        acquiring = true;
        server.askAcquireAngel(player, markerId, recruits);
    }

    synchronized void doneAcquiringAngels()
    {
        acquiring = false;
        checkEngagementDone();
    }

    private void setEngagementResult(String aResult, Legion winner,
        int aPoints, int aTurn)
    {
        engagementResult = aResult;
        this.winner = winner;
        pointsScored = aPoints;
        turnCombatFinished = aTurn;
    }

    private synchronized void checkEngagementDone()
    {
        if (summoning || reinforcing || acquiring || engagementResult == null)
        {
            return;
        }

        engagementInProgress = false;

        server.allUpdatePlayerInfo();

        server.allTellEngagementResults(winner, engagementResult,
            pointsScored, turnCombatFinished);

        engagementResult = null;
        if (checkAutoQuitOrGoOn())
        {
            server.nextEngagement();
        }
    }

    /*
     * returns true if game should go on.
     */
    public boolean checkAutoQuitOrGoOn()
    {
        if (gameOver && getOption(Options.autoQuit))
        {
            Start.setCurrentWhatToDoNext(Start.QuitAll);
            dispose();
            // if dispose does System.exit(), we would never come here, 
            // but when we get rid of all System.exit()'s ...
            return false;
        }
        else
        {
            return true;
        }
    }

    /** Return a list of all players' legions. */
    synchronized List<LegionServerSide> getAllLegions()
    {
        List<LegionServerSide> list = new ArrayList<LegionServerSide>();
        for (PlayerServerSide player : players)
        {
            List<LegionServerSide> legions = player.getLegions();

            list.addAll(legions);
        }
        return list;
    }

    /** Return a list of all players' legions' marker ids. */
    List<String> getAllLegionIds()
    {
        List<String> list = new ArrayList<String>();
        for (PlayerServerSide player : players)
        {
            list.addAll(player.getLegionIds());
        }
        return list;
    }

    /** Return a list of all legions not belonging to player. */
    synchronized List<LegionServerSide> getAllEnemyLegions(Player player)
    {
        List<LegionServerSide> list = new ArrayList<LegionServerSide>();
        for (PlayerServerSide nextPlayer : players)
        {
            if (nextPlayer != player)
            {
                List<LegionServerSide> legions = nextPlayer.getLegions();

                list.addAll(legions);
            }
        }
        return list;
    }

    /** Return a list of ids for all legions not belonging to player. */
    List<String> getAllEnemyLegionIds(Player player)
    {
        List<String> list = new ArrayList<String>();
        for (PlayerServerSide nextPlayer : players)
        {
            if (nextPlayer != player)
            {
                list.addAll(nextPlayer.getLegionIds());
            }
        }
        return list;
    }

    LegionServerSide getLegionByMarkerId(String markerId)
    {
        Iterator<PlayerServerSide> it = players.iterator();
        while (it.hasNext())
        {
            PlayerServerSide player = it.next();
            LegionServerSide legion = player.getLegionByMarkerId(markerId);
            if (legion != null)
            {
                return legion;
            }
        }
        return null;
    }

    PlayerServerSide getPlayerByMarkerId(String markerId)
    {
        Iterator<PlayerServerSide> it = players.iterator();
        while (it.hasNext())
        {
            PlayerServerSide player = it.next();
            LegionServerSide legion = player.getLegionByMarkerId(markerId);

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
        List<LegionServerSide> legions = getAllLegions();
        Iterator<LegionServerSide> it = legions.iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            total += legion.getPointValue();
        }
        return total / legions.size();
    }

    int getNumLegions(String hexLabel)
    {
        int count = 0;
        Iterator<LegionServerSide> it = getAllLegions().iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                count++;
            }
        }
        return count;
    }

    boolean isOccupied(String hexLabel)
    {
        Iterator<LegionServerSide> it = getAllLegions().iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                return true;
            }
        }
        return false;
    }

    LegionServerSide getFirstLegion(String hexLabel)
    {
        Iterator<LegionServerSide> it = getAllLegions().iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();
            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                return legion;
            }
        }
        return null;
    }

    List<String> getLegionMarkerIds(String hexLabel)
    {
        List<String> markerIds = new ArrayList<String>();
        Iterator<LegionServerSide> it = getAllLegions().iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                markerIds.add(legion.getMarkerId());
            }
        }
        return markerIds;
    }

    synchronized int getNumFriendlyLegions(String hexLabel,
        PlayerServerSide player)
    {
        int count = 0;
        List<LegionServerSide> legions = player.getLegions();
        Iterator<LegionServerSide> it = legions.iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                count++;
            }
        }
        return count;
    }

    synchronized LegionServerSide getFirstFriendlyLegion(String hexLabel,
        PlayerServerSide player)
    {
        List<LegionServerSide> legions = player.getLegions();
        Iterator<LegionServerSide> it = legions.iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                return legion;
            }
        }
        return null;
    }

    synchronized List<LegionServerSide> getFriendlyLegions(String hexLabel,
        PlayerServerSide player)
    {
        List<LegionServerSide> newLegions = new ArrayList<LegionServerSide>();
        List<LegionServerSide> legions = player.getLegions();
        Iterator<LegionServerSide> it = legions.iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                newLegions.add(legion);
            }
        }
        return newLegions;
    }

    int getNumEnemyLegions(String hexLabel, Player player)
    {
        int count = 0;
        Iterator<LegionServerSide> it = getAllEnemyLegions(player).iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                count++;
            }
        }
        return count;
    }

    LegionServerSide getFirstEnemyLegion(String hexLabel, Player player)
    {
        Iterator<LegionServerSide> it = getAllEnemyLegions(player).iterator();
        while (it.hasNext())
        {
            LegionServerSide legion = it.next();

            if (hexLabel.equals(legion.getCurrentHexLabel()))
            {
                return legion;
            }
        }
        return null;
    }

    int mulligan()
    {
        if (getPhase() != Constants.Phase.MOVE)
        {
            return -1;
        }
        PlayerServerSide player = getActivePlayer();
        player.takeMulligan();
        server.allUpdatePlayerInfo();
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

    // History wrappers.  Time to start obeying the Law of Demeter.
    void addCreatureEvent(String markerId, String creatureName)
    {
        lastRecruitTurnNumber = turnNumber;
        history.addCreatureEvent(markerId, creatureName, turnNumber);
    }

    void removeCreatureEvent(String markerId, String creatureName)
    {
        history.removeCreatureEvent(markerId, creatureName, turnNumber);
    }

    void splitEvent(String parentId, String childId, List<String> splitoffs)
    {
        history.splitEvent(parentId, childId, splitoffs, turnNumber);
    }

    void mergeEvent(String splitoffId, String survivorId)
    {
        history.mergeEvent(splitoffId, survivorId, turnNumber);
    }

    void revealEvent(boolean allPlayers, List<String> playerNames,
        String markerId, List<String> creatureNames)
    {
        history.revealEvent(allPlayers, playerNames, markerId, creatureNames,
            turnNumber);
    }

    void playerElimEvent(Player player, Player slayer)
    {
        history.playerElimEvent(player, slayer, turnNumber);
    }

    public NotifyWebServer getNotifyWebServer()
    {
        return this.notifyWebServer;
    }

    /* Interface from Game/Server to WebServer who started this.
     * Perhaps later replaced with a two-way socket connection?
     * Class is always created, no matter whether we have a web
     * server ( => active == true) or not ( => active == false); 
     * but this way, we can have all the
     *    "if (we have a web server) { } " 
     * checking done inside this class and do not clutter the 
     * main server code.
     */

    class NotifyWebServer
    {
        private String flagFilename = null;
        private PrintWriter out;
        private File flagFile = null;
        // Do we even have a web server to notify at all?
        private boolean active = false;

        public NotifyWebServer(String name)
        {
            if (name != null && !name.equals(""))
            {
                this.flagFilename = name;
                active = true;
            }
        }

        public boolean isActive()
        {
            return active;
        }

        public void readyToAcceptClients()
        {
            if (active)
            {
                createFlagfile();
            }
        }

        public void gotClient(Player player, boolean remote)
        {
            if (active)
            {
                out.println("Client (type " +
                    (remote ? "remote" : "local") + ") connected: " +
                    player.getName());
                out.flush();
            }
        }

        public void allClientsConnected()
        {
            if (active)
            {
                out.println("All clients connected");
                out.flush();
            }
        }

        public void serverStoppedRunning()
        {
            if (active)
            {
                removeFlagfile();
            }
        }

        public void createFlagfile()
        {
            if (active)
            {
                flagFile = new File(flagFilename);
                try
                {
                    // flagFile.createNewFile();
                    out = new PrintWriter(new FileWriter(flagFile));
                }
                catch (IOException e)
                {
                    LOGGER.log(Level.SEVERE,
                        "Could not create web server flag file "
                            + flagFilename + "!!", (Throwable)null);
                }
            }
        }

        public void removeFlagfile()
        {
            out.close();
            if (active)
            {
                try
                {
                    flagFile.delete();
                }
                catch (Exception e)
                {
                    LOGGER.log(Level.SEVERE,
                        "Could not delete web server flag file "
                            + flagFilename + "!!" + e.toString(),
                        (Throwable)null);
                }
            }
        }
    } // END Class NotifyWebServer
}