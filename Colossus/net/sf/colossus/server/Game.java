package net.sf.colossus.server;


import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.output.*;
import org.jdom.input.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.client.MasterBoard;
import net.sf.colossus.client.MasterHex;
import net.sf.colossus.client.Proposal;
import net.sf.colossus.client.BattleMap;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;
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
    private List players = new ArrayList(6);
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
    private Caretaker caretaker = new Caretaker(this);
    private int phase;
    private Server server;
    // Negotiation
    private Set[] proposals = new HashSet[2];

    private LinkedList colorPickOrder = new LinkedList();
    private java.util.List colorsLeft;
    private PhaseAdvancer phaseAdvancer = new GamePhaseAdvancer();
    private Options options = new Options(Constants.optionsServerName);

    /** Server port number. */
    private int port = Constants.defaultPort;

    History history;

    /** Public only for JUnit test setup. */
    public Game()
    {

    }

    /** For Start */
    Options getOptions()
    {
        return options;
    }

    // TODO: Set from option instead.
    void setPort(int port)
    {
        this.port = port;
    }

    private void initServer()
    {
        if (server != null)
        {
            server.disposeAllClients();
        }
        server = new Server(this, port);
        server.initSocketServer();
        server.initFileServer();
    }

    private synchronized void clearFlags()
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

    private void addPlayersFromOptions()
    {
        for (int i = 0; i < VariantSupport.getMaxPlayers(); i++)
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

        Creature.getCreatureByName("Titan").setMaxCount(getNumPlayers());
    }

    /** Start a new game. */
    void newGame()
    {
        clearFlags();

        turnNumber = 1;
        phase = Constants.SPLIT;
        caretaker.resetAllCounts();
        players.clear();

        options.saveOptions();

        VariantSupport.loadVariant(options.getStringOption(Options.variant),
                true);

        Log.event("Starting new game");

        CustomRecruitBase.resetAllInstances();
        CustomRecruitBase.setCaretaker(caretaker);
        CustomRecruitBase.setGame(this);

        addPlayersFromOptions();

        history = new History();

        initServer();
    }

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
        return getUniqueName(name + Dice.rollDie());
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
            server.oneSetOption(player.getName(), Options.playerType,
                    player.getType());
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
        java.util.List cli = new ArrayList();

        colorsLeft = new ArrayList();
        for (int i = 0; i < Constants.colorNames.length; i++)
        {
            cli.add(Constants.colorNames[i]);
        }

        /* Add the first 6 colors in random order, ... */
        for (int i = 0; i < Constants.DEFAULT_MAX_PLAYERS; i++)
        {
            colorsLeft.add(cli.remove(Dice.rollDie(
                    Constants.DEFAULT_MAX_PLAYERS - i) - 1));
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

    void nextPickColor()
    {
        if (colorPickOrder.size() >= 1)
        {
            String playerName = (String)colorPickOrder.getFirst();

            server.askPickColor(playerName, colorsLeft);
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
            playerName = color;
        }
        Log.event(player.getName() + " chooses color " + color);
        player.initMarkersAvailable();
        server.allUpdatePlayerInfo();
        server.askPickFirstMarker(playerName);
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
            placeInitialLegion(player, player.getFirstMarker());
            server.allRevealLegion(player.getLegion(0));
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
        Set towerSet = MasterBoard.getTowerSet();
        ArrayList towerList = new ArrayList();

        Iterator it = towerSet.iterator();
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
        int startingTower = Dice.rollDie(numTowers) - 1;

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

    Player getPlayerByShortColor(String shortColor)
    {
        if (shortColor != null)
        {
            Iterator it = players.iterator();

            while (it.hasNext())
            {
                Player player = (Player)it.next();

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
                Log.event("Game over -- Draw at " + new Date().getTime());
                setGameOver(true);
                server.allTellGameOver("Draw");
                break;

            case 1:
                String winnerName = getWinner().getName();

                Log.event("Game over -- " + winnerName + " wins at " +
                        new Date().getTime());
                setGameOver(true);
                server.allTellGameOver(winnerName + " wins");
                break;

            default:
                break;
        }
    }

    synchronized boolean isOver()
    {
        return gameOver;
    }

    private synchronized void setGameOver(boolean gameOver)
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
            Log.error("Called advancePhase illegally (reason: " +
                    (oldPhase != phase ? "oldPhase (" +
                    Constants.getBattlePhaseName(oldPhase) + ") != phase (" +
                    Constants.getBattlePhaseName(phase) + ")" :
                    (pendingAdvancePhase ? "pendingAdvancePhase is true " :
                    (!playerName.equals(getActivePlayerName()) ?
                    "wrong player [" + playerName +
                    " vs. " + getActivePlayerName() +
                    "]" :
                    "UNKNOWN"))) + ")");
            return;
        }
        if (getOption(Options.autoStop) && getNumHumansRemaining() < 1)
        {
            Log.event("Not advancing because no humans remain");
            // XXX buggy?  
            server.allTellGameOver("All humans eliminated");
            setGameOver(true);
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

        public void advanceTurn()
        {
            clearFlags();
            activePlayerNum++;
            if (activePlayerNum == getNumPlayers())
            {
                activePlayerNum = 0;
                turnNumber++;
            }

            /* notify all CustomRecruitBase object that we change the 
             active player, for bookkeeping purpose */
            CustomRecruitBase.everyoneAdvanceTurn(activePlayerNum);

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
    }

    private void setupMove()
    {
        Player player = getActivePlayer();

        player.rollMovement();
        server.allSetupMove();
    }

    private synchronized void setupFight()
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

    synchronized void saveGame(final String filename)
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

            fn = Constants.saveDirname + Constants.xmlSnapshotStart +
                    date.getTime() + Constants.xmlExtension;
        }
        else
        {
            fn = new String(filename);
            Log.event("Saving game to " + filename);
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
            el.addContent("" + getPhase());
            root.addContent(el);

            Element car = new Element("Caretaker");

            root.addContent(car);

            // Caretaker stacks
            java.util.List creatures = Creature.getCreatures();
            Iterator it = creatures.iterator();

            while (it.hasNext())
            {
                Creature creature = (Creature)it.next();

                el = new Element("Creature");
                el.setAttribute("name", creature.getName());
                el.setAttribute("remaining", "" +
                        caretaker.getCount(creature));
                el.setAttribute("dead", "" + caretaker.getDeadCount(creature));
                car.addContent(el);
            }

            // Players
            it = players.iterator();
            while (it.hasNext())
            {
                Player player = (Player)it.next();

                el = new Element("Player");
                el.setAttribute("name", player.getName());
                el.setAttribute("type", player.getType());
                el.setAttribute("color", player.getColor());
                el.setAttribute("startingTower", player.getTower());
                el.setAttribute("score", "" + player.getScore());
                el.setAttribute("dead", "" + player.isDead());
                el.setAttribute("mulligansLeft", "" +
                        player.getMulligansLeft());
                el.setAttribute("colorsElim", player.getPlayersElim());
                el.setAttribute("movementRoll", "" + player.getMovementRoll());
                el.setAttribute("teleported", "" + player.hasTeleported());
                el.setAttribute("summoned", "" + player.hasSummoned());

                Collection legions = player.getLegions();
                Iterator it2 = legions.iterator();

                while (it2.hasNext())
                {
                    Legion legion = (Legion)it2.next();

                    el.addContent(dumpLegion(doc, legion, 
                            battleInProgress && (
                              legion == battle.getAttacker() || 
                              legion == battle.getDefender()
                            )
                        ));
                }
                root.addContent(el);
            }

            // Dump the file cache, so that generated files are preserved
            it = ResourceLoader.getFileCacheDump().iterator();
            while (it.hasNext())
            {
                root.addContent((Element)it.next());
            }

            // Battle stuff
            if (engagementInProgress && battle != null)
            {
                Element bat = new Element("Battle");

                bat.setAttribute("masterHexLabel", battle.getMasterHexLabel());
                bat.setAttribute("turnNumber", "" + battle.getTurnNumber());
                bat.setAttribute("activePlayer", "" +
                        battle.getActivePlayerName());
                bat.setAttribute("phase", "" + battle.getPhase());
                bat.setAttribute("summonState", "" + battle.getSummonState());
                bat.setAttribute("carryDamage", "" + battle.getCarryDamage());
                bat.setAttribute("driftDamageApplied", "" +
                        battle.isDriftDamageApplied());

                it = battle.getCarryTargets().iterator();
                while (it.hasNext())
                {
                    Element ct = new Element("CarryTarget");
                    String carryTarget = (String)it.next();

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
            Log.error("Error writing XML savegame: " + ex.toString());
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

    private Element dumpLegion(Document doc, Legion legion, boolean inBattle)
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

        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();

        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Element cre = new Element("Creature");

            cre.setAttribute("name", critter.getName());
            if (inBattle)
            {
                cre.setAttribute("hits", "" + critter.getHits());
                cre.setAttribute("currentHex", critter.getCurrentHexLabel());
                cre.setAttribute("startingHex", critter.getStartingHexLabel());
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
    void loadGame(String filename)
    {
        File file = null;

        if (filename.equals("--latest"))
        {
            File dir = new File(Constants.saveDirname);

            if (!dir.exists() || !dir.isDirectory())
            {
                Log.error("No saves directory");
                dispose();
            }
            String[] filenames = dir.list(new XMLSnapshotFilter());

            if (filenames.length < 1)
            {
                Log.error("No XML savegames found in saves directory");
                dispose();
            }
            file = new File(Constants.saveDirname +
                    latestSaveFilename(filenames));
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
            Log.event("Loading game from " + file);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(file);

            Element root = doc.getRootElement();
            Attribute ver = root.getAttribute("version");

            if (!ver.getValue().equals(Constants.xmlSnapshotVersion))
            {
                Log.error("Can't load this savegame version.");
                dispose();
            }

            // Reset flags that are not in the savegame file.
            clearFlags();
            loadingGame = true;

            Element el = root.getChild("Variant");
            Attribute dir = el.getAttribute("dir");
            Attribute fil = el.getAttribute("file");

            VariantSupport.freshenVariant(fil.getValue(), dir.getValue());

            // then load data files
            java.util.List datafilesElements = root.getChildren("DataFile");
            Iterator it = datafilesElements.iterator();
            while (it.hasNext())
            {
                Element dea = (Element)it.next();
                String mapKey = dea.getAttributeValue("DataFileKey");
                java.util.List contentList = dea.getContent();
                if (contentList.size() > 0)
                {
                    String content = ((CDATA)contentList.get(0)).getText();
                    Log.debug("DataFileKey: " + mapKey + " DataFileContent :\n" 
                        + content);
                    ResourceLoader.putIntoFileCache(mapKey,
                        content.getBytes());
                }
                else
                {
                    ResourceLoader.putIntoFileCache(mapKey,
                            new byte[0]);
                }
            }

            // we're server, but the file generation process has been done
            // by loading the savefile.
            VariantSupport.loadVariant(fil.getValue(), dir.getValue(), false);

            el = root.getChild("TurnNumber");
            turnNumber = Integer.parseInt(el.getTextTrim());

            el = root.getChild("CurrentPlayer");
            activePlayerNum = Integer.parseInt(el.getTextTrim());

            el = root.getChild("CurrentPhase");
            phase = Integer.parseInt(el.getTextTrim());

            Element ct = root.getChild("Caretaker");
            java.util.List kids = ct.getChildren();
            it = kids.iterator();

            while (it.hasNext())
            {
                el = (Element)it.next();
                String creatureName = el.getAttribute("name").getValue();
                int remaining = el.getAttribute("remaining").getIntValue();
                int dead = el.getAttribute("dead").getIntValue();
                Creature creature = Creature.getCreatureByName(creatureName);

                caretaker.setCount(creature, remaining);
                caretaker.setDeadCount(creature, dead);
            }

            players.clear();
            if (battle != null)
            {
                server.allCleanupBattle();
            }

            // Players
            java.util.List playerElements = root.getChildren("Player");

            it = playerElements.iterator();
            while (it.hasNext())
            {
                Element pla = (Element)it.next();

                String name = pla.getAttribute("name").getValue();
                Player player = new Player(name, this);
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

                int mulligansLeft =
                        pla.getAttribute("mulligansLeft").getIntValue();
                player.setMulligansLeft(mulligansLeft);

                player.setMovementRoll(
                        pla.getAttribute("movementRoll").getIntValue());

                player.setTeleported(
                        pla.getAttribute("teleported").getBooleanValue());

                player.setSummoned(
                        pla.getAttribute("summoned").getBooleanValue());

                String playersElim =
                        pla.getAttribute("colorsElim").getValue();
                if (playersElim == "null")
                {
                    playersElim = "";
                }
                player.setPlayersElim(playersElim);

                java.util.List legionElements = pla.getChildren("Legion");
                Iterator it2 = legionElements.iterator();
                while (it2.hasNext())
                {
                    Element leg = (Element)it2.next();
                    readLegion(leg, player);
                }
            }
            // Need all players' playersElim set up before we can do this.
            it = players.iterator();
            while (it.hasNext())
            {
                Player player = (Player)it.next();
                player.computeMarkersAvailable();
            }

            // Battle stuff
            Element bat = root.getChild("Battle");
            if (bat != null)
            {
                String engagementHexLabel =
                        bat.getAttribute("masterHexLabel").getValue();
                int battleTurnNum =
                        bat.getAttribute("turnNumber").getIntValue();
                String battleActivePlayerName =
                        bat.getAttribute("activePlayer").getValue();
                int battlePhase = bat.getAttribute("phase").getIntValue();
                int summonState =
                        bat.getAttribute("summonState").getIntValue();
                int carryDamage =
                        bat.getAttribute("carryDamage").getIntValue();
                boolean driftDamageApplied =
                    bat.getAttribute("driftDamageApplied").getBooleanValue();

                java.util.List cts = bat.getChildren("CarryTarget");
                Set carryTargets = new HashSet();
                Iterator it2 = cts.iterator();
                while (it2.hasNext())
                {
                    Element cart = (Element)it2.next();
                    carryTargets.add(cart.getTextTrim());
                }

                Player attackingPlayer = getActivePlayer();
                Legion attacker = getFirstFriendlyLegion(engagementHexLabel,
                        attackingPlayer);
                Legion defender = getFirstEnemyLegion(engagementHexLabel,
                        attackingPlayer);

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

            // History
            history = new History();
            Element his = root.getChild("History");
            history.copyTree(his);

            initServer();
            // Remaining stuff has been moved to loadGame2()
        }
        catch (Exception ex)
        {
            Log.error("Tried to load corrupt savegame");
            ex.printStackTrace();
            dispose();
        }
    }

    private void readLegion(Element leg, Player player)
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
        Critter[] critters = new Critter[8];
        java.util.List creatureElements = leg.getChildren("Creature");
        Iterator it = creatureElements.iterator();
        int k = 0;

        while (it.hasNext())
        {
            Element cre = (Element)it.next();
            String name = cre.getAttribute("name").getValue();
            Critter critter = new Critter(
                    Creature.getCreatureByName(name), null, this);

            // Battle stuff
            if (cre.getAttribute("hits") != null)
            {
                int hits = cre.getAttribute("hits").getIntValue();

                critter.setHits(hits);

                String currentBattleHexLabel =
                        cre.getAttribute("currentHex").getValue();

                critter.setCurrentHexLabel(currentBattleHexLabel);
                String startingBattleHexLabel =
                        cre.getAttribute("startingHex").getValue();

                critter.setStartingHexLabel(startingBattleHexLabel);

                boolean struck = cre.getAttribute("struck").getBooleanValue();

                critter.setStruck(struck);
            }

            critters[k] = critter;
            k++;
        }

        // If this legion already exists, modify it in place.
        Legion legion = player.getLegionByMarkerId(markerId);

        if (legion != null)
        {
            for (k = 0; k < legion.getHeight(); k++)
            {
                legion.setCritter(k, critters[k]);
            }
        }
        else
        {
            legion = new Legion(
                    markerId,
                    parentId,
                    currentHexLabel,
                    startingHexLabel,
                    critters[0] == null ? null : critters[0].getCreature(),
                    critters[1] == null ? null : critters[1].getCreature(),
                    critters[2] == null ? null : critters[2].getCreature(),
                    critters[3] == null ? null : critters[3].getCreature(),
                    critters[4] == null ? null : critters[4].getCreature(),
                    critters[5] == null ? null : critters[5].getCreature(),
                    critters[6] == null ? null : critters[6].getCreature(),
                    critters[7] == null ? null : critters[7].getCreature(),
                    player.getName(),
                    this);
            player.addLegion(legion);
        }

        legion.setMoved(moved);
        legion.setRecruitName(recruitName);
        legion.setEntrySide(entrySide);
        legion.addToBattleTally(battleTally);
    }

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
        }
        );
    }

    /** Return a list of eligible recruits, as Creatures. */
    java.util.List findEligibleRecruits(String markerId, String hexLabel)
    {
        Legion legion = getLegionByMarkerId(markerId);
        java.util.List recruits;

        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        String terrain = hex.getTerrain();

        recruits = new ArrayList();
        java.util.List tempRecruits =
                TerrainRecruitLoader.getPossibleRecruits(terrain, hexLabel);
        java.util.List recruiters =
                TerrainRecruitLoader.getPossibleRecruiters(terrain, hexLabel);

        ListIterator lit = tempRecruits.listIterator();

        while (lit.hasNext())
        {
            Creature creature = (Creature)lit.next();
            ListIterator liter = recruiters.listIterator();

            while (liter.hasNext())
            {
                Creature lesser = (Creature)liter.next();

                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser,
                        creature, terrain, hexLabel) <=
                        legion.numCreature(lesser)) &&
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
        String terrain = hex.getTerrain();
        recruiters = TerrainRecruitLoader.getPossibleRecruiters(
                terrain, hexLabel);
        Iterator it = recruiters.iterator();
        while (it.hasNext())
        {
            Creature possibleRecruiter = (Creature)it.next();
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
    private boolean anonymousRecruitLegal(Legion legion, Creature recruit)
    {
        return TerrainRecruitLoader.anonymousRecruitLegal(recruit,
                legion.getCurrentHex().getTerrain(),
                legion.getCurrentHex().getLabel());
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
            // If recruiter can be anonymous, then this is okay.
            if (!anonymousRecruitLegal(legion, recruit))
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

            Log.event("Legion " + legion.getLongMarkerName() + " in " +
                    hex.getDescription() + " recruits " + recruit.getName() +
                    " with " + (numRecruiters == 0 ? "nothing" :
                    numRecruiters + " " + (numRecruiters > 1 ?
                    recruiter.getPluralName() : recruiter.getName())));

            // Recruits are one to a customer.
            legion.setRecruitName(recruit.getName());
            reinforcing = false;
        }
    }

    /** Return a list of names of angel types that can be acquired. */
    java.util.List findEligibleAngels(Legion legion, int score)
    {
        if (legion.getHeight() >= 7)
        {
            return null;
        }
        java.util.List recruits = new ArrayList();
        String terrain = legion.getCurrentHex().getTerrain();
        java.util.List allRecruits =
                TerrainRecruitLoader.getRecruitableAcquirableList(terrain,
                score);
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
    private synchronized Set findNormalMoves(MasterHex hex, Legion legion,
            int roll, int block, int cameFrom, boolean ignoreFriends)
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
            java.util.List legions = player.getLegions();
            Iterator it = legions.iterator();
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
                                getNumEnemyLegions(hexLabel, player) == 0)) &&
                                (!(hexLabel.equals(hex.getLabel()))))
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
        set.remove(null);
        set.remove("null");
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
    synchronized Set findEngagements()
    {
        Set set = new HashSet();
        Player player = getActivePlayer();

        java.util.List legions = player.getLegions();
        Iterator it = legions.iterator();

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
        if (!isOver())
        {
            summoning = true;
            server.createSummonAngel(attacker);
        }
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

            server.allTellRemoveCreature(donor.getMarkerId(), angel.getName(),
                    true);
            server.allTellAddCreature(legion.getMarkerId(), angel.getName(),
                    true);

            Log.event("One " + angel.getName() +
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

    Battle getBattle()
    {
        return battle;
    }

    private synchronized void kickEngagements()
    {
        server.nextEngagement();
        if (findEngagements().size() == 0 && !summoning && !reinforcing &&
                !acquiring)
        {
            advancePhase(Constants.FIGHT, getActivePlayerName());
        }
    }

    synchronized void finishBattle(String hexLabel, boolean attackerEntered,
            int points)
    {
        battle = null;
        server.allCleanupBattle();
        Legion winner = null;

        // Handle any after-battle angel summoning or recruiting.
        if (getNumLegions(hexLabel) == 1)
        {
            winner = getFirstLegion(hexLabel);

            // Make all creatures in the victorious legion visible.
            server.allRevealLegion(winner);
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
                    Log.debug(
                        "Calling Game.reinforce() from Game.finishBattle()");
                    reinforce(winner);
                }
            }
        }
        engagementInProgress = false;
        battleInProgress = false;

        server.allUpdatePlayerInfo();
        String winnerId = null;
        if (winner != null)
        {
            winnerId = winner.getMarkerId();
        }
        server.allTellEngagementResults(winnerId, "fight", points);
        if (!summoning && !reinforcing && !acquiring)
        {
            server.allHighlightEngagements();
            kickEngagements();
        }
    }

    /** Return a set of hexLabels. */
    synchronized Set findSummonableAngels(String markerId)
    {
        Legion legion = getLegionByMarkerId(markerId);
        Set set = new HashSet();
        java.util.List legions = legion.getPlayer().getLegions();
        Iterator it = legions.iterator();
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

    /** Return true and call Server.didSplit() if the split succeeded. 
     *  Return false if it failed. */
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

        // Pre-split legion must have 4+ creatures.
        if (legion.getHeight() < 4)
        {
            server.showMessageDialog(player.getName(),
                    "Legion " + parentId + " is too short to split.");
            return false;
        }

        if (results == null)
        {
            Log.debug("Empty split list (" + parentId + ", " +
                    childId + ")");
            return false;
        }
        java.util.List strings = Split.split(',', results);
        java.util.List creatures = new ArrayList();
        Iterator it = strings.iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            Creature creature = Creature.getCreatureByName(name);
            creatures.add(creature);
        }

        // Each legion must have 2+ creatures after the split.
        if (creatures.size() < 2 || legion.getHeight() - creatures.size() < 2)
        {
            Log.debug("Too small/big split list (" + parentId + ", " +
                    childId + ")");
            return false;
        }

        // All creatures in results must be in the legion.
        // WARNING: Legion.getCritters() return Critters,
        // not Creature - different things now.
        // so we must clone "by hand" the List.
        java.util.List tempCritters = legion.getCritters();
        java.util.List tempCreatures = new ArrayList();

        it = tempCritters.iterator();
        while (it.hasNext())
        {
            tempCreatures.add(((Critter)it.next()).getCreature());
        }
        it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            if (!tempCreatures.remove(creature))
            {
                Log.debug("Unavailable creature in split list (" +
                        parentId + ", " +
                        childId + ") : " + creature.getName());
                return false;
            }
        }

        if (getTurnNumber() == 1)
        {
            // Only allow a single split on turn 1.
            if (player.getNumLegions() > 1)
            {
                server.showMessageDialog(player.getName(),
                        "Cannot split twice on Turn 1.");
                return false;
            }
            // Each stack must contain exactly 4 creatures.
            if (creatures.size() != 4)
            {
                return false;
            }
            // Each stack must contain exactly 1 lord.
            int numLords = 0;

            it = creatures.iterator();
            while (it.hasNext())
            {
                Creature creature = (Creature)it.next();
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

        Legion newLegion = legion.split(creatures, childId);
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

            server.allTellEngagement(hexLabel, attacker, defender);

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
        else
        {
            Log.debug("illegal call to Game.engage() " + engagementInProgress);
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

        Proposal proposal = Proposal.makeFromString(proposalString);
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

    /** Synchronized to keep both negotiators from racing to fight. */
    synchronized void fight(String hexLabel)
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
            server.allRevealEngagedLegion(attacker, true);
            server.allRevealEngagedLegion(defender, false);

            battle = new Battle(this, attacker.getMarkerId(),
                    defender.getMarkerId(), Constants.DEFENDER, hexLabel,
                    1, Constants.MOVE);
            battle.init();
        }
    }

    private synchronized void handleConcession(Legion loser, Legion winner,
            boolean fled)
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
        server.allTellEngagementResults(winner.getMarkerId(),
                fled ? "flee" : "concede", points);
        server.allHighlightEngagements();
        if (!acquiring)
        {
            kickEngagements();
        }
    }

    private synchronized void handleNegotiation(Proposal results)
    {
        Legion attacker = getLegionByMarkerId(results.getAttackerId());
        Legion defender = getLegionByMarkerId(results.getDefenderId());
        Legion winner = null;
        int points = 0;

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
            winner = getLegionByMarkerId(results.getWinnerId());
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
                server.allTellRemoveCreature(winner.getMarkerId(),
                        creatureName, true);
            }
            Log.event(log.toString());

            server.oneRevealLegion(winner, attacker.getPlayerName());
            server.oneRevealLegion(winner, defender.getPlayerName());

            points = loser.getPointValue();

            Player losingPlayer = loser.getPlayer();

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
                losingPlayer.die(winner.getPlayerName(), true);
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
        server.allTellEngagementResults(winner == null ? null :
                winner.getMarkerId(), "negotiate", points);
        if (!summoning && !reinforcing && !acquiring)
        {
            server.allHighlightEngagements();
        }
        kickEngagements();
    }

    synchronized void askAcquireAngel(String playerName, String markerId,
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
    synchronized java.util.List getAllLegions()
    {
        java.util.List list = new ArrayList();
        for (Iterator it = players.iterator(); it.hasNext();)
        {
            Player player = (Player)it.next();
            java.util.List legions = player.getLegions();

            list.addAll(legions);
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
    synchronized java.util.List getAllEnemyLegions(Player player)
    {
        java.util.List list = new ArrayList();
        for (Iterator it = players.iterator(); it.hasNext();)
        {
            Player nextPlayer = (Player)it.next();

            if (nextPlayer != player)
            {
                java.util.List legions = nextPlayer.getLegions();

                list.addAll(legions);
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

    synchronized int getNumFriendlyLegions(String hexLabel, Player player)
    {
        int count = 0;
        java.util.List legions = player.getLegions();
        Iterator it = legions.iterator();
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

    synchronized Legion getFirstFriendlyLegion(String hexLabel, Player player)
    {
        java.util.List legions = player.getLegions();
        Iterator it = legions.iterator();
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

    synchronized java.util.List getFriendlyLegions(String hexLabel,
            Player player)
    {
        java.util.List newLegions = new ArrayList();
        java.util.List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();

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
}
