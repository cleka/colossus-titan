package net.sf.colossus.client;


import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Window;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import net.sf.colossus.Player;
import net.sf.colossus.ai.AI;
import net.sf.colossus.ai.SimpleAI;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.PlayerState;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.IServer;
import net.sf.colossus.server.Start;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.ChildThreadManager;
import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.LogWindow;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 *  Lives on the client side and handles all communication
 *  with the server.  It talks to the client classes locally, and to
 *  Server via the network protocol.  There is one client per player.
 *  
 *  TODO the logic for the battles could probably be separated from the
 *  rest of this code. At the moment the battle logic seems to bounce
 *  back and forth between BattleMap (which is really a GUI class) and
 *  this class.
 *  
 *  @version $Id$
 *  @author David Ripton
 *  @author Romain Dolbeau
 */

public final class Client implements IClient, IOracle, IOptions
{
    private static final Logger LOGGER = Logger.getLogger(Client.class
        .getName());

    /** This will eventually be a network interface rather than a
     *  direct reference.  So don't share this reference. */
    private IServer server;
    private ChildThreadManager threadMgr;
    private final boolean stresstestStopOnFatal = false;

    private WebClient webClient = null;
    private boolean startedByWebClient = false;

    public boolean failed = false;

    private MasterBoard board;
    private StatusScreen statusScreen;
    private CreatureCollectionView caretakerDisplay;
    private SummonAngel summonAngel;
    private MovementDie movementDie;
    private EngagementResults engagementResults;
    private AutoInspector autoInspector;
    private EventViewer eventViewer;
    private PreferencesWindow preferencesWindow;

    /** hexLabel of MasterHex for current or last engagement. */
    private String battleSite;
    private BattleBoard battleBoard;

    private final List<BattleChit> battleChits = new ArrayList<BattleChit>();

    /** Stack of legion marker ids, to allow multiple levels of undo for
     *  splits, moves, and recruits. */
    private final LinkedList<Object> undoStack = new LinkedList<Object>();

    // Information on the current moving legion.
    private String moverId;

    /** The end of the list is on top in the z-order. */
    private final List<Marker> markers = new ArrayList<Marker>();

    private final List<Chit> recruitedChits = new ArrayList<Chit>();
    private final List<Chit> possibleRecruitChits = new ArrayList<Chit>();

    // Per-client and per-player options.
    private final Options options;

    /** Player who owns this client. */
    private final Player player;
    private boolean playerAlive = true;

    /**
     * The game in progress.
     */
    private final Game game;

    /** Starting marker color of player who owns this client. */
    private String color;

    /** Last movement roll for any player. */
    private int movementRoll = -1;

    /** the parent frame for secondary windows */
    private JFrame secondaryParent = null;

    private String parentId;
    private int numSplitsThisTurn;

    // show best potential recruit chit needs exactly "SimpleAI".
    private SimpleAI simpleAI;
    private AI ai;

    private final CaretakerInfo caretakerInfo;

    private int turnNumber = -1;
    private String activePlayerName = "";
    private Constants.Phase phase;

    private int battleTurnNumber = -1;
    private String battleActivePlayerName = null;
    private Constants.BattlePhase battlePhase;
    private String attackerMarkerId = "none";
    private String defenderMarkerId = "none";

    /** Summon angel donor legion, for this client's player only. */
    private String donorId;

    /** If the game is over, then quitting does not require confirmation. */
    private boolean gameOver;

    /** One per player. */
    private PlayerInfo[] playerInfo;
    private Hashtable<String, PlayerInfo> playerInfoByName = new Hashtable<String, PlayerInfo>(
        12);

    /** One per player. */
    private PredictSplits[] predictSplits;

    /** One LegionInfo per legion, keyed by markerId.  Never null. */
    private SortedMap<String, LegionInfo> legionInfo = new TreeMap<String, LegionInfo>();

    private int numPlayers;

    private Movement movement;
    private BattleMovement battleMovement;
    private Strike strike;

    private boolean remote;
    private SocketClientThread sct;

    // For negotiation.  (And AI battle.)
    private Negotiate negotiate;
    private ReplyToProposal replyToProposal;

    // Constants for closedBy:
    public static final int notYet = 0;
    public static final int byServer = 1;
    public static final int byClient = 2;
    public static final int byWebClient = 3;
    public int closedBy = notYet;

    // XXX temporary until things are synched
    private boolean tookMulligan;

    private int delay = -1;

    /** For battle AI. */
    private List<CritterMove> bestMoveOrder = null;
    private List<CritterMove> failedBattleMoves = null;

    // XXX Make private and wrap consistently.
    boolean showAllRecruitChits = false;
    private final Hashtable<String, Integer> recruitReservations = new Hashtable<String, Integer>();

    private LogWindow logWindow;
    private int viewMode;
    private int recruitChitMode;

    // Once we got dispose from server (or user initiated it himself), 
    // we'll ignore it if we we get it from server again 
    // - it's then up to the user to do some "disposing" action.
    private boolean gotDisposeAlready = false;

    private boolean disposeInProgress = false;

    public Client(String host, int port, Game game, Player player,
        boolean remote, boolean byWebClient)
    {
        super();

        assert player != null;

        this.game = game;
        // TODO the caretaker should be attached to the Game class
        this.caretakerInfo = new CaretakerInfo(game);

        this.player = player;
        this.remote = remote;
        this.startedByWebClient = byWebClient;
        this.threadMgr = new ChildThreadManager("Client " + player.getName());

        this.simpleAI = new SimpleAI(this);
        this.ai = this.simpleAI;

        this.movement = new Movement(this);
        this.battleMovement = new BattleMovement(this);
        this.strike = new Strike(this);

        ViableEntityManager.register(this, "Client " + player.getName());
        net.sf.colossus.webcommon.InstanceTracker.register(this, "Client "
            + player.getName());

        options = new Options(player.getName());
        // Need to load options early so they don't overwrite server options.
        loadOptions();

        sct = new SocketClientThread(this, host, port);

        String reasonFail = sct.getReasonFail();
        if (reasonFail != null)
        {
            // If this failed here, it is usually a "could not connect"-problem
            // (wrong host or port or server not yet up).
            // In this case we just do cleanup and end.

            // start needs to be run, otherwise thread won't be GC'd.
            sct.start();
            sct = null;

            String title = "Socket initialialization failed!";
            showErrorMessage(reasonFail, title);

            if (remote)
            {
                Start.setCurrentWhatToDoNext(Start.NetClientDialog);
            }
            failed = true;
            ViableEntityManager.unregister(this);
        }
        else
        {
            this.server = sct;
            if (remote)
            {
                net.sf.colossus.util.ResourceLoader.setDataServer(host,
                    port + 1);
            }
            else
            {
                net.sf.colossus.util.ResourceLoader.setDataServer(null, 0);
            }

            sct.start();

            TerrainRecruitLoader.setCaretakerInfo(caretakerInfo);
            net.sf.colossus.server.CustomRecruitBase
                .addCaretakerInfo(caretakerInfo);
            failed = false;
        }
    }

    public ChildThreadManager getThreadMgr()
    {
        return threadMgr;
    }

    boolean isRemote()
    {
        return remote;
    }

    boolean isAlive()
    {
        return playerAlive;
    }

    public void setWebClient(WebClient wc)
    {
        this.webClient = wc;
    }

    public boolean getFailed()
    {
        return failed;
    }

    /*
     * If webclient is just hidden, bring it back; 
     * if it had been used, ask whether to restore;
     * Otherwise just do nothing
     */
    void handleWebClientRestore()
    {
        if (this.webClient != null)
        {
            // was only Hidden, so bring it up without asking
            this.webClient.setVisible(true);
        }
        else
        {
            // webclient never used (local game), or explicitly closed
            // - don't bother user with it
            // If he now said quit -- he probably wants quit.
            // if he now used close or new game, he can get to web client
            // from GetPlayers dialog.
        }
    }

    public void showWebClient()
    {
        if (this.webClient == null)
        {
            this.webClient = new WebClient(null, -1, null, null);
            this.webClient.setGameClient(this);
        }
        else
        {
            this.webClient.setVisible(true);
        }
    }

    /** Take a mulligan. */
    public void mulligan()
    {
        undoAllMoves(); // XXX Maybe move entirely to server
        clearUndoStack();
        clearRecruitChits();
        tookMulligan = true;

        // TODO: should not be needed any more here?
        if (eventViewer != null)
        {
            eventViewer.setMulliganOldRoll(movementRoll);
        }
        server.mulligan();
    }

    // XXX temp
    public boolean tookMulligan()
    {
        return tookMulligan;
    }

    /** Resolve engagement in land. */
    void engage(String land)
    {
        server.engage(land);
    }

    public String getMyEngagedMarkerId()
    {
        String markerId = null;
        if (isMyLegion(attackerMarkerId))
        {
            markerId = attackerMarkerId;
        }
        else if (isMyLegion(defenderMarkerId))
        {
            markerId = defenderMarkerId;
        }
        return markerId;
    }

    void concede()
    {
        concede(getMyEngagedMarkerId());
    }

    private void concede(String markerId)
    {
        if (markerId != null)
        {
            server.concede(markerId);
        }
    }

    private void doNotConcede(String markerId)
    {
        server.doNotConcede(markerId);
    }

    /** Cease negotiations and fight a battle in land. */
    void fight(String land)
    {
        server.fight(land);
    }

    private List<String> _tellEngagementResults_attackerStartingContents = null;
    private List<String> _tellEngagementResults_defenderStartingContents = null;
    private List<Boolean> _tellEngagementResults_attackerLegionCertainities = null;
    private List<Boolean> _tellEngagementResults_defenderLegionCertainities = null;

    public void tellEngagement(String hexLabel, String attackerId,
        String defenderId)
    {
        this.battleSite = hexLabel;
        this.attackerMarkerId = attackerId;
        this.defenderMarkerId = defenderId;
        if (eventViewer != null)
        {
            eventViewer.tellEngagement(attackerId, defenderId, turnNumber);
        }

        // remember for end of battle.
        _tellEngagementResults_attackerStartingContents = getLegionImageNames(attackerId);
        _tellEngagementResults_defenderStartingContents = getLegionImageNames(defenderId);
        // TODO: I have the feeling that getLegionCertainties()
        //   does not work here.
        //   I always seem to get either ALL true or ALL false.
        _tellEngagementResults_attackerLegionCertainities = getLegionCreatureCertainties(attackerId);
        _tellEngagementResults_defenderLegionCertainities = getLegionCreatureCertainties(defenderId);

        highlightBattleSite();
    }

    private JFrame getPreferredParent()
    {
        JFrame parent = secondaryParent;
        if ((parent == null) && (board != null))
        {
            parent = board.getFrame();
        }
        return parent;
    }

    private void initShowEngagementResults()
    {
        JFrame parent = getPreferredParent();
        // no board at all, e.g. AI - nothing to do.
        if (parent == null)
        {
            return;
        }

        engagementResults = new EngagementResults(parent, this, this);
        engagementResults.maybeShow();
    }

    private void showOrHideAutoInspector(boolean bval)
    {
        JFrame parent = getPreferredParent();
        if (parent == null)
        {
            // No board yet, or no board at all - nothing to do.
            // Initial show will be done in initBoard.
            return;
        }
        if (bval)
        {
            if (autoInspector == null)
            {
                autoInspector = new AutoInspector(parent, this, player
                    .getName(), viewMode, getOption(Options.dubiousAsBlanks));
            }
        }
        else
        {
            disposeInspector();
        }
    }

    private void showOrHideCaretaker(boolean bval)
    {
        if (board == null)
        {
            // No board yet, or no board at all - nothing to do.
            // Initial show will be done in initBoard.
            return;
        }

        if (bval)
        {
            if (caretakerDisplay == null)
            {
                JFrame parent = getPreferredParent();
                caretakerDisplay = new CreatureCollectionView(parent, this);
            }
        }
        else
        {
            disposeCaretakerDisplay();
        }
    }

    void highlightBattleSite()
    {
        if (board != null && battleSite != null && battleSite.length() > 0)
        {
            board.unselectAllHexes();
            board.selectHexByLabel(battleSite);
        }
    }

    public void tellEngagementResults(String winnerId, String method,
        int points, int turns)
    {
        JFrame frame = getMapOrBoardFrame();
        if (frame == null)
        {
            return;
        }

        if (eventViewer != null)
        {
            eventViewer.tellEngagementResults(winnerId, method, turns);
        }

        if (engagementResults != null)
        {
            engagementResults.addData(winnerId, method, points, turns,
                _tellEngagementResults_attackerStartingContents,
                _tellEngagementResults_defenderStartingContents,
                _tellEngagementResults_attackerLegionCertainities,
                _tellEngagementResults_defenderLegionCertainities, isMyTurn());
        }
    }

    /* Create the event viewer, so that it can collect data from beginning on.
     * EventViewer shows itself depending on whether the option for it is set. 
     */
    private void initEventViewer()
    {
        if (eventViewer == null)
        {
            JFrame parent = getPreferredParent();
            eventViewer = new EventViewer(parent, this, this);
        }
    }

    private void initPreferencesWindow()
    {
        if (preferencesWindow == null)
        {
            preferencesWindow = new PreferencesWindow(this, this);
        }
    }

    public void setPreferencesWindowVisible(boolean val)
    {
        if (preferencesWindow != null)
        {
            preferencesWindow.setVisible(val);
        }
    }

    /**
     * Displays the marker and its legion if possible.
     */
    public void showMarker(Marker marker)
    {
        if (autoInspector != null)
        {
            String markerId = marker.getId();
            LegionInfo legion = getLegionInfo(markerId);
            autoInspector.showLegion(legion);
        }
    }

    /**
     * Displays the recruit tree of the hex if possible.
     */
    public void showHexRecruitTree(GUIMasterHex hex)
    {
        if (autoInspector != null)
        {
            autoInspector.showHexRecruitTree(hex);
        }
    }

    /** Legion summoner summons unit from legion donor. */
    void doSummon(String summoner, String donor, String unit)
    {
        server.doSummon(summoner, donor, unit);

        if (board != null)
        {
            board.repaint();
            summonAngel = null;

            highlightEngagements();
            board.repaint();
        }
    }

    public void didSummon(String summonerId, String donorId, String summon)
    {
        if (eventViewer != null)
        {
            eventViewer.newCreatureRevealEvent(RevealEvent.eventSummon,
                donorId, getLegionInfo(donorId).getHeight(), summon,
                summonerId, getLegionInfo(summonerId).getHeight());
        }
    }

    /** This player quits the whole game. The server needs to always honor
     *  this request, because if it doesn't players will just drop
     *  connections when they want to quit in a hurry. */
    public void withdrawFromGame()
    {
        if (!isGameOver())
        {
            server.withdrawFromGame();
        }
    }

    private void repaintAllWindows()
    {
        if (statusScreen != null)
        {
            statusScreen.repaint();
        }
        if (caretakerDisplay != null)
        {
            caretakerDisplay.repaint();
        }
        if (board != null)
        {
            board.getFrame().repaint();
        }
        if (battleBoard != null)
        {
            battleBoard.repaint();
        }
    }

    void rescaleAllWindows()
    {
        clearRecruitChits();

        if (statusScreen != null)
        {
            statusScreen.rescale();
        }
        if (board != null)
        {
            board.rescale();
        }
        if (battleBoard != null)
        {
            battleBoard.rescale();
        }
        repaintAllWindows();
    }

    public void tellMovementRoll(int roll)
    {
        if (eventViewer != null)
        {
            eventViewer.tellMovementRoll(roll);
        }

        movementRoll = roll;
        if (movementDie == null || roll != movementDie.getLastRoll())
        {
            initMovementDie(roll);
            if (board != null)
            {
                if (board.getFrame().getExtendedState() != JFrame.ICONIFIED)
                {
                    board.repaint();
                }
            }
        }
        kickMoves();
    }

    private void kickMoves()
    {
        if (isMyTurn() && getOption(Options.autoMasterMove) && !isGameOver())
        {
            doAutoMoves();
        }
    }

    private void doAutoMoves()
    {
        boolean again = ai.masterMove();
        aiPause();
        if (!again)
        {
            doneWithMoves();
        }
    }

    private void initMovementDie(int roll)
    {
        movementRoll = roll;
        if (board != null)
        {
            movementDie = new MovementDie(4 * Scale.get(), MovementDie
                .getDieImageName(roll));
        }
    }

    private void disposeMovementDie()
    {
        movementDie = null;
    }

    MovementDie getMovementDie()
    {
        return movementDie;
    }

    // public for IOptions
    public boolean getOption(String optname, boolean defaultValue)
    {
        return options.getOption(optname, defaultValue);
    }

    // public for IOptions
    public boolean getOption(String optname)
    {
        // If autoplay is set, then return true for all other auto* options.
        if (optname.startsWith("Auto") && !optname.equals(Options.autoPlay))
        {
            if (options.getOption(Options.autoPlay))
            {
                return true;
            }
        }
        return options.getOption(optname);
    }

    // Public for IOptions
    public String getStringOption(String optname)
    {
        return options.getStringOption(optname);
    }

    /** Return -1 if the option's value has not been set.
     public for LogWindow */
    public int getIntOption(String optname)
    {
        return options.getIntOption(optname);
    }

    /** public so that server can set autoPlay for AIs. */
    public void setOption(String optname, String value)
    {
        boolean optionChanged = false;
        if (!value.equals(getStringOption(optname)))
        {
            optionChanged = true;
        }
        options.setOption(optname, value);
        if (optionChanged)
        {
            optionTrigger(optname, value);
            syncOptions();
        }
    }

    /** public for LogWindow */
    public void setOption(String optname, boolean value)
    {
        setOption(optname, String.valueOf(value));
    }

    /** public for LogWindow */
    public void setOption(String optname, int value)
    {
        setOption(optname, String.valueOf(value));
    }

    public int getViewMode()
    {
        return this.viewMode;
    }

    /** Fully sync the board state by running all option triggers. */
    private void runAllOptionTriggers()
    {
        Enumeration<String> en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = en.nextElement();
            String value = getStringOption(name);
            optionTrigger(name, value);
        }
        syncOptions();
    }

    private void optionTrigger(String optname)
    {
        String value = getStringOption(optname);
        optionTrigger(optname, value);
    }

    /** Trigger side effects after changing an option value. */
    private void optionTrigger(String optname, String value)
    {
        LOGGER.log(Level.INFO, "optionTrigger " + optname + " : " + value);
        boolean bval = Boolean.valueOf(value).booleanValue();

        if (optname.equals(Options.antialias))
        {
            GUIHex.setAntialias(bval);
            repaintAllWindows();
        }
        else if (optname.equals(Options.useOverlay))
        {
            GUIHex.setOverlay(bval);
            if (board != null)
            {
                board.repaintAfterOverlayChanged();
            }
        }
        else if (optname.equals(Options.showAllRecruitChits))
        {
            showAllRecruitChits = bval;
        }
        else if (optname.equals(Options.showRecruitChitsSubmenu))
        {
            recruitChitMode = options.getNumberForRecruitChitSelection(value);
        }
        else if (optname.equals(Options.noBaseColor))
        {
            Creature.setNoBaseColor(bval);
            net.sf.colossus.util.ResourceLoader.purgeImageCache();
            repaintAllWindows();
        }
        else if (optname.equals(Options.useColoredBorders))
        {
            BattleChit.setUseColoredBorders(bval);
            repaintAllWindows();
        }
        else if (optname.equals(Options.logDebug))
        {
            if (bval)
            {
                Logger.getLogger("net.sf.colossus").setLevel(Level.ALL);
            }
            else
            {
                Logger.getLogger("net.sf.colossus").setLevel(Level.OFF);
            }
        }
        else if (optname.equals(Options.showCaretaker))
        {
            showOrHideCaretaker(bval);
        }
        else if (optname.equals(Options.showLogWindow))
        {
            if (board != null && bval)
            {
                if (logWindow == null)
                {
                    // the logger with the empty name is parent to all loggers
                    // and thus catches all messages
                    logWindow = new LogWindow(this, Logger.getLogger(""));
                }
            }
            else
            {
                disposeLogWindow();
            }
        }
        else if (optname.equals(Options.showStatusScreen))
        {
            updateStatusScreen();
        }
        else if (optname.equals(Options.showAutoInspector))
        {
            showOrHideAutoInspector(bval);
        }
        else if (optname.equals(Options.showEventViewer))
        {
            // if null: no board (not yet, or not at all) => no eventviewer
            if (eventViewer != null)
            {
                // Eventviewer takes care of showing/hiding itself
                eventViewer.setVisibleMaybe();
            }
        }
        else if (optname.equals(Options.viewMode))
        {
            String viewModeName = options.getStringOption(Options.viewMode);
            viewMode = options.getNumberForViewMode(viewModeName);
        }
        else if (optname.equals(Options.dubiousAsBlanks))
        {
            if (autoInspector != null)
            {
                autoInspector.setDubiousAsBlanks(bval);
            }
        }
        else if (optname.equals(Options.showEngagementResults))
        {
            // null if there is no board, e.g. AI
            if (engagementResults != null)
            {
                // maybeShow decides by itself based on the current value
                // of the option whether to hide or show. 
                engagementResults.maybeShow();
            }
        }
        else if (optname.equals(Options.favoriteLookFeel))
        {
            setLookAndFeel(value);
        }
        else if (optname.equals(Options.scale))
        {
            int scale = Integer.parseInt(value);
            if (scale > 0)
            {
                Scale.set(scale);
                rescaleAllWindows();
            }
        }
        else if (optname.equals(Options.playerType))
        {
            setType(value);
        }
        else if (optname.equals(Options.useSVG))
        {
            ResourceLoader.setUseSVG(bval);
        }
    }

    /** Save player options to a file. */
    void saveOptions()
    {
        options.saveOptions();
    }

    /** Load player options from a file. */
    private void loadOptions()
    {
        options.loadOptions();
        syncOptions();
        runAllOptionTriggers();
    }

    /** Synchronize menu checkboxes and cfg file after an option change. */
    private void syncOptions()
    {
        syncCheckboxes();
        saveOptions();
    }

    /** Ensure that Player menu checkboxes reflect the correct state. */
    private void syncCheckboxes()
    {
        if (board == null)
        {
            return;
        }
        Enumeration<String> en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = en.nextElement();
            boolean value = getOption(name);
            board.twiddleOption(name, value);
        }
    }

    // public for IOracle
    public int getNumPlayers()
    {
        return numPlayers;
    }

    public int getNumLivingPlayers()
    {
        int total = 0;
        for (PlayerInfo info : playerInfo)
        {
            if (!info.isDead())
            {
                total++;
            }
        }
        return total;
    }

    public void updatePlayerInfo(List<String> infoStrings)
    {
        numPlayers = infoStrings.size();
        if (playerInfo == null)
        {
            playerInfo = new PlayerInfo[numPlayers];
            for (int i = 0; i < numPlayers; i++)
            {
                List<String> data = Split.split(":", infoStrings.get(i));
                Player player = Player.getPlayerByName(data.get(1));
                PlayerInfo info = new PlayerInfo(this, player);
                playerInfo[i] = info;
            }
        }
        for (int i = 0; i < numPlayers; i++)
        {
            playerInfo[i].update(infoStrings.get(i));
        }
        updateStatusScreen();
    }

    private void updateStatusScreen()
    {
        if (getNumPlayers() < 1)
        {
            // Called too early.
            return;
        }
        if (getOption(Options.showStatusScreen))
        {
            if (statusScreen != null)
            {
                statusScreen.updateStatusScreen();
            }
            else
            {
                if (board != null)
                {
                    statusScreen = new StatusScreen(getPreferredParent(),
                        this, this, this);
                }
            }
        }
        else
        {
            if (board != null)
            {
                board.twiddleOption(Options.showStatusScreen, false);
            }
            if (statusScreen != null)
            {
                statusScreen.dispose();
            }
            this.statusScreen = null;
        }

        // XXX Should be called somewhere else, just once.
        setupPlayerLabel();
    }

    PlayerInfo getPlayerInfo(int playerNum)
    {
        return playerInfo[playerNum];
    }

    public PlayerInfo getPlayerInfo(String name)
    {
        int num = getPlayerNum(name);
        return (num == -1 ? null : playerInfo[num]);
    }

    public PlayerInfo getPlayerInfo(Player player)
    {
        return getPlayerInfo(player.getName());
    }

    public PlayerInfo getPlayerInfo()
    {
        return getPlayerInfo(player);
    }

    public Player getPlayer()
    {
        return player;
    }

    public List<String> getPlayerNames()
    {
        List<String> names = new ArrayList<String>();
        for (PlayerInfo info : playerInfo)
        {
            names.add(info.getPlayer().getName());
        }
        return names;
    }

    // TODO: temporarily, do both hash and loop lookup and compare.
    // if it turns out that this works reliably, we can change it 
    // so that it loops only if it does not find it ( = first call?)
    int getPlayerNum(String pName)
    {
        assert pName != null : "Player name must not be null";

        PlayerInfo tryHash = playerInfoByName.get(pName);
        for (int i = 0; i < playerInfo.length; i++)
        {
            if (pName.equals(playerInfo[i].getPlayer().getName()))
            {
                if (tryHash == null)
                {
                    playerInfoByName.put(pName, playerInfo[i]);
                }
                // should never happen... just for to be sure.
                else if (tryHash != playerInfo[i])
                {
                    LOGGER.log(Level.SEVERE,
                        "Wooooah! Found by hash differs from loop?");
                }
                return i;
            }
        }
        return -1;
    }

    /** Return the average point value of all legions in the game. */
    public int getAverageLegionPointValue()
    {
        int totalValue = 0;
        int totalLegions = 0;

        Iterator<LegionInfo> it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = it.next();
            totalLegions++;
            totalValue = info.getPointValue();
        }
        return (int)(Math.round((double)totalValue / totalLegions));
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public void updateCreatureCount(String creatureName, int count,
        int deadCount)
    {
        caretakerInfo.updateCount(creatureName, count, deadCount);
        updateCreatureCountDisplay();
    }

    private void updateCreatureCountDisplay()
    {
        if (board == null)
        {
            return;
        }
        if (caretakerDisplay != null)
        {
            caretakerDisplay.update();
        }
    }

    private void disposeMasterBoard()
    {
        if (board != null)
        {
            board.dispose();
            board = null;
        }
    }

    private void disposeBattleMap()
    {
        if (battleBoard != null)
        {
            battleBoard.dispose();
            battleBoard = null;
        }
    }

    private void disposeStatusScreen()
    {
        if (statusScreen != null)
        {
            statusScreen.dispose();
            statusScreen = null;
        }
    }

    private void disposeLogWindow()
    {
        if (logWindow != null)
        {
            logWindow.setVisible(false);
            logWindow.dispose();
            logWindow = null;
        }
    }

    private void disposeEventViewer()
    {
        if (eventViewer != null)
        {
            eventViewer.dispose();
            eventViewer = null;
        }
    }

    private void disposePreferencesWindow()
    {
        if (preferencesWindow != null)
        {
            preferencesWindow.dispose();
            preferencesWindow = null;
        }
    }

    void disposeEngagementResults()
    {
        if (engagementResults != null)
        {
            engagementResults.dispose();
            engagementResults = null;
        }
    }

    private void disposeCaretakerDisplay()
    {
        if (caretakerDisplay != null)
        {
            caretakerDisplay.dispose();
            caretakerDisplay = null;
        }
    }

    private void disposeInspector()
    {
        if (autoInspector != null)
        {
            autoInspector.setVisible(false);
            autoInspector.dispose();
            autoInspector = null;
        }
    }

    public void setClosedByServer()
    {
        closedBy = byServer;
    }

    // called by WebClient
    public void doConfirmAndQuit()
    {
        if (board != null)
        {
            // Board does the "Really Quit?" confirmation and initiates
            // then (if user confirmed) the disposal of everything.
            board.doQuitGameAction();
        }
    }

    // Used by MasterBoard and by BattleMap
    public void askNewCloseQuitCancel(JFrame frame, boolean fromBattleBoard)
    {
        String[] options = new String[4];
        options[0] = "New Game";
        options[1] = "Quit";
        options[2] = "Close";
        options[3] = "Cancel";
        int answer = JOptionPane
            .showOptionDialog(
                frame,
                "Choose one of: Play another game, Quit, Close just this board, or Cancel",
                "Play another game?", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[3]);
        frame = null;
        if (answer == -1 || answer == 3)
        {
            return;
        }
        else
        {
            if (fromBattleBoard)
            {
                concede();
            }
        }
        if (answer == 0)
        {
            menuNewGame();
        }
        else if (answer == 1)
        {
            menuQuitGame();
        }
        else if (answer == 2)
        {
            disposeClientOriginated();
        }
    }

    private void disposeClientOriginated()
    {
        if (disposeInProgress)
        {
            return;
        }
        closedBy = byClient;

        if (sct != null && !sct.isAlreadyDown())
        {
            {
                // SCT will then end the loop and do the dispose.
                // So nothing else to do any more here in EDT.
                sct.stopSocketClientThread();
            }
        }
        else
        {
            // SCT already closed and requested to dispose client,
            // but user declined. Now, when user manually wants to
            // close the board, have to do it directly.
            disposeWholeClient();
        }
    }

    // used from server, when game is over and server closes all sockets
    public synchronized void dispose()
    {
        if (gotDisposeAlready)
        {
            return;
        }
        gotDisposeAlready = true;
        disposeWholeClient();
    }

    // Clean up everything related to _this_ client:

    public void disposeWholeClient()
    {
        handleWebClientRestore();

        // -----------------------------------------------
        // Now a long decision making, whether to actually close
        // everything or not... - depending on the situation.
        boolean close = true;

        try
        {
            close = decideWhetherClose();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Exception " + e.toString()
                + " while deciding whether to close", e);
        }

        if (close)
        {
            try
            {
                disposeInProgress = true;
                disposeAll();
                if (webClient != null)
                {
                    webClient.setGameClient(null);
                    webClient = null;
                }
            }
            // just in case, so we are sure to get the unregistering done
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE, "During close in client "
                    + player.getName() + ": got Exception!!!" + e.toString(),
                    e);
            }
            ViableEntityManager.unregister(this);
        }
    }

    private boolean decideWhetherClose()
    {
        boolean close = true;

        // I don't use "getPlayerInfo().isAI() here, because if done
        // so very early, getPlayerInfo delivers null. 
        boolean isAI = true;
        String pType = getStringOption(Options.playerType);
        if (pType != null
            && (pType.endsWith("Human") || pType.endsWith("Network")))
        {
            isAI = false;
        }

        // AIs in general, and any (local or remote) client during 
        // stresstesting should close without asking...
        if (isAI || Options.isStresstest())
        {
            close = true;
        }
        else if (closedBy == byServer)
        {
            if (remote)
            {
                defaultCursor();
                board.setServerClosedMessage(gameOver);

                String message = (gameOver ? "Game over: Connection closed from server side."
                    : "Connection to server unexpectedly lost?");

                JOptionPane.showMessageDialog(getMapOrBoardFrame(), message,
                    "Server closed connection",
                    JOptionPane.INFORMATION_MESSAGE);
                close = false;
            }
            else
            {
                // NOT remote, forced closed: just closing without asking
            }
        }
        return close;
    }

    /* Dispose all windows, and clean up lot of references, 
     * so that GC can do it's job
     * - in case we keep JVM open to play another one...
     */
    private void disposeAll()
    {
        disposeInProgress = true;

        if (sct == null)
        {
            return;
        }

        sct = null;
        server = null;

        threadMgr.cleanup();
        threadMgr = null;

        if (ai != simpleAI)
        {
            ((SimpleAI)ai).dispose();
        }
        simpleAI.dispose();
        simpleAI = null;
        ai = null;

        if (SwingUtilities.isEventDispatchThread())
        {
            cleanupGUI();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        cleanupGUI();
                    }
                });
            }
            catch (InterruptedException e)
            {/* ignore */
            }
            catch (InvocationTargetException e2)
            {/* ignore */
            }
        }
    }

    private void cleanupGUI()
    {
        try
        {
            disposeInspector();
            disposeCaretakerDisplay();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE,
                "During disposal of Inspector and Caretaker: " + e.toString(),
                e);
        }

        cleanupBattle();
        disposeLogWindow();
        disposeMovementDie();
        disposeStatusScreen();
        disposeEventViewer();
        disposePreferencesWindow();
        disposeEngagementResults();
        disposeBattleMap();
        disposeMasterBoard();

        movement.dispose();

        this.movement = null;
        this.battleMovement = null;
        this.strike = null;
        this.secondaryParent = null;
        this.legionInfo = null;
        this.playerInfoByName = null;
        this.playerInfo = null;

        net.sf.colossus.server.CustomRecruitBase.reset();
    }

    /** Called from BattleMap to leave carry mode. */
    public void leaveCarryMode()
    {
        server.leaveCarryMode();
        doAutoStrikes();
    }

    void doneWithBattleMoves()
    {
        aiPause();
        clearUndoStack();
        server.doneWithBattleMoves();
    }

    boolean anyOffboardCreatures()
    {
        Iterator<BattleChit> it = getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (chit.getCurrentHexLabel().startsWith("X"))
            {
                return true;
            }
        }
        return false;
    }

    public List<BattleChit> getActiveBattleChits()
    {
        List<BattleChit> chits = new ArrayList<BattleChit>();
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (getBattleActivePlayer().equals(getPlayerByTag(chit.getTag())))
            {
                chits.add(chit);
            }
        }
        return chits;
    }

    List<BattleChit> getInactiveBattleChits()
    {
        List<BattleChit> chits = new ArrayList<BattleChit>();
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (!getBattleActivePlayer().equals(getPlayerByTag(chit.getTag())))
            {
                chits.add(chit);
            }
        }
        return chits;
    }

    private void markOffboardCreaturesDead()
    {
        Iterator<BattleChit> it = getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (chit.getCurrentHexLabel().startsWith("X"))
            {
                chit.setDead(true);
                chit.repaint();
            }
        }
    }

    void doneWithStrikes()
    {
        aiPause();
        server.doneWithStrikes();
    }

    /** Return true if any strikes were taken. */
    boolean makeForcedStrikes()
    {
        if (isMyBattlePhase() && getOption(Options.autoForcedStrike))
        {
            return strike
                .makeForcedStrikes(getOption(Options.autoRangeSingle));
        }
        return false;
    }

    /** Handle both forced strikes and AI strikes. */
    void doAutoStrikes()
    {
        if (isMyBattlePhase())
        {
            if (getOption(Options.autoPlay))
            {
                aiPause();
                boolean struck = makeForcedStrikes();
                if (!struck)
                {
                    struck = ai
                        .strike(getLegionInfo(getBattleActiveMarkerId()));
                }
                if (!struck)
                {
                    doneWithStrikes();
                }
            }
            else
            {
                boolean struck = makeForcedStrikes();
                if (battleBoard != null)
                {
                    battleBoard.highlightCrittersWithTargets();
                }
                if (!struck && findCrittersWithTargets().isEmpty())
                {
                    doneWithStrikes();
                }
            }
        }
    }

    List<Marker> getMarkers()
    {
        return Collections.unmodifiableList(markers);
    }

    LegionInfo createLegionInfo(String markerId)
    {
        LegionInfo info = new LegionInfo(markerId, this);
        legionInfo.put(markerId, info);
        return info;
    }

    /** Get this legion's info.  Create it first if necessary. */
    public LegionInfo getLegionInfo(String markerId)
    {
        LegionInfo info = legionInfo.get(markerId);
        return info;
    }

    /** Get the marker with this id. */
    Marker getMarker(String id)
    {
        return getLegionInfo(id).getMarker();
    }

    /** Add the marker to the end of the list and to the LegionInfo.
     If it's already in the list, remove the earlier entry. */
    void setMarker(String id, Marker marker)
    {
        markers.remove(marker);
        markers.add(marker);
        getLegionInfo(id).setMarker(marker);
    }

    /** Remove this eliminated legion, and clean up related stuff. */
    public void removeLegion(String id)
    {
        Marker marker = getMarker(id);
        markers.remove(marker);

        // TODO Do for all players
        if (isMyLegion(id))
        {
            getPlayerInfo().addMarkerAvailable(id);
        }

        LegionInfo info = getLegionInfo(id);
        String hexLabel = info.getHexLabel();

        // XXX Not perfect -- Need to track recruitChits by legion.
        removeRecruitChit(hexLabel);

        legionInfo.remove(id);
        if (board != null)
        {
            board.alignLegions(hexLabel);
        }
    }

    int getLegionHeight(String markerId)
    {
        LegionInfo legionInfo = getLegionInfo(markerId);
        if (legionInfo == null)
        {
            return 0; //no legion, no height
        }
        return legionInfo.getHeight();
    }

    /** Needed when loading a game outside split phase. */
    public void setLegionStatus(String markerId, boolean moved,
        boolean teleported, int entrySide, String lastRecruit)
    {
        LegionInfo info = getLegionInfo(markerId);
        info.setMoved(moved);
        info.setTeleported(teleported);
        info.setEntrySide(entrySide);
        info.setLastRecruit(lastRecruit);
    }

    /** Return the full basename for a titan in legion markerId,
     *  first finding that legion's player, player color, and titan size.
     *  Default to Constants.titan if the info is not there. */
    String getTitanBasename(String markerId)
    {
        return getLegionInfo(markerId).getTitanBasename();
    }

    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    // public for IOracle
    public List<String> getLegionImageNames(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        if (info != null)
        {
            return info.getImageNames();
        }
        return new ArrayList<String>();
    }

    /** Return a list of Booleans */
    // public for IOracle
    public List<Boolean> getLegionCreatureCertainties(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        if (info != null)
        {
            return info.getCertainties();
        }
        else
        {
            // LOGGER.log(Level.SEVERE,
            //    "getLegionCreatureCertainties getLegionInfo for " + 
            //    markerId + " returned null!");

            // TODO: is this the right thing?
            List<Boolean> l = new ArrayList<Boolean>(42 / 4); // just longer then max
            for (int idx = 0; idx < (42 / 4); idx++)
            {
                l.add(new Boolean(true)); // all true
            }
            return l;
        }
    }

    /** Add a new creature to this legion. */
    public void addCreature(String markerId, String name, String reason)
    {
        getLegionInfo(markerId).addCreature(name);
        if (board != null)
        {
            String hexLabel = getHexForLegion(markerId);
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            hex.repaint();
        }

        if (eventViewer != null)
        {
            eventViewer.addCreature(markerId, name, reason);
        }
    }

    public void removeCreature(String markerId, String name, String reason)
    {
        LegionInfo info = getLegionInfo(markerId);

        if (eventViewer != null)
        {
            eventViewer.removeCreature(markerId, name);
        }

        String hexLabel = info.getHexLabel();
        int height = info.getHeight();
        info.removeCreature(name);
        if (height <= 1)
        {
            // dont remove this, sever will give explicit order to remove it
            // removeLegion(markerId);
        }
        if (height <= 1 && getTurnNumber() == -1)
        {
            // hack to remove legions correctly during load
            removeLegion(markerId);
        }
        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            hex.repaint();
        }
    }

    /** Reveal creatures in this legion, some of which already may be known. 
     *  - this "reveal" is related to data coming from server being  
     *  revealed to the split prediction 
     * */

    public void revealCreatures(String markerId, final List<String> names,
        String reason)
    {
        if (eventViewer != null)
        {
            eventViewer.revealCreatures(markerId, names, reason);
        }

        // "Normal" split prediction stuff:
        String pName = getPlayerByMarkerId(markerId).getPlayer().getName();
        if (predictSplits == null || getPredictSplits(pName) == null)
        {
            initPredictSplits(pName, markerId, names);
            createLegionInfo(markerId);
        }

        try
        {
            LegionInfo info = getLegionInfo(markerId);
            if (info == null)
            {
                if (reason.equals(Constants.reasonGameOver))
                {

                    /* Should not happen any more anyway since in server side
                     * the unregisterSocket is now done before the withdraw. 
                     * (See r2757 in "web" branch, and see also the longer
                     *  comment in Client.java in r2755, also in web branch.)
                     */
                    LOGGER.log(Level.SEVERE,
                        "revealCreatures - no legioninfo " + "for marker "
                            + markerId + " -- ignoring it, "
                            + "because reason is GameOver");
                }
                else
                {
                    LOGGER.log(Level.SEVERE,
                        "revealCreatures - no legioninfo " + "for marker "
                            + markerId + " and it is NOT "
                            + "the Game Over case!!!");
                }
            }
            else
            {
                info.revealCreatures(names);
            }
        }
        catch (NullPointerException e)
        {
            // I believe we will not get those any more now, but let's
            // leave it in place for a while. (17.11.2007, CleKa).
            LOGGER.log(Level.WARNING, "NPE in revealCreatures, markerId="
                + markerId + ", reason=" + reason + ", names="
                + names.toString(), e);

            // in stresstest activeate those, to get logs and autosaves
            // copied for troubleshooting...
            if (stresstestStopOnFatal)
            {
                LOGGER.log(Level.SEVERE, "NPE in revealCreatures, markerId="
                    + markerId + ", names=" + names.toString());
                System.exit(1);
            }

        }
    }

    /* pass revealed info to EventViewer and SplitPrediction, and
     * additionally remember the images list for later, the engagement report
     */
    public void revealEngagedCreatures(String markerId,
        final List<String> names, boolean isAttacker, String reason)
    {
        revealCreatures(markerId, names, reason);
        // in engagment we need to update the remembered list, too.
        if (isAttacker)
        {
            _tellEngagementResults_attackerStartingContents = getLegionImageNames(markerId);
            // towi: should return a list of trues, right?
            _tellEngagementResults_attackerLegionCertainities = getLegionCreatureCertainties(markerId);
        }
        else
        {
            _tellEngagementResults_defenderStartingContents = getLegionImageNames(markerId);
            // towi: should return a list of trues, right?
            _tellEngagementResults_defenderLegionCertainities = getLegionCreatureCertainties(markerId);
        }

        if (eventViewer != null)
        {
            eventViewer.revealEngagedCreatures(names, isAttacker, reason);
        }
    }

    List<BattleChit> getBattleChits()
    {
        return Collections.unmodifiableList(battleChits);
    }

    List<BattleChit> getBattleChits(String hexLabel)
    {
        List<BattleChit> chits = new ArrayList<BattleChit>();

        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (hexLabel.equals(chit.getCurrentHexLabel()))
            {
                chits.add(chit);
            }
        }
        return chits;
    }

    public BattleChit getBattleChit(String hexLabel)
    {
        List<BattleChit> chits = getBattleChits(hexLabel);
        if (chits.isEmpty())
        {
            return null;
        }
        return chits.get(0);
    }

    /** Get the BattleChit with this tag. */
    BattleChit getBattleChit(int tag)
    {
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (chit.getTag() == tag)
            {
                return chit;
            }
        }
        return null;
    }

    public void removeDeadBattleChits()
    {
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (chit.isDead())
            {
                it.remove();

                // Also remove it from LegionInfo.
                String name = chit.getId();
                if (chit.isInverted())
                {
                    LegionInfo info = getLegionInfo(defenderMarkerId);
                    info.removeCreature(name);
                    if (eventViewer != null)
                    {
                        eventViewer.defenderSetCreatureDead(name, info
                            .getHeight());
                    }
                }
                else
                {
                    LegionInfo info = getLegionInfo(attackerMarkerId);
                    info.removeCreature(name);
                    if (eventViewer != null)
                    {
                        eventViewer.attackerSetCreatureDead(name, info
                            .getHeight());
                    }
                }
            }
        }
        if (battleBoard != null)
        {
            battleBoard.repaint();
        }
    }

    public void placeNewChit(String imageName, boolean inverted, int tag,
        String hexLabel)
    {
        addBattleChit(imageName, inverted, tag, hexLabel);
        if (battleBoard != null)
        {
            battleBoard.alignChits(hexLabel);
            // Make sure map is visible after summon or muster.
            focusMap();
        }
    }

    /** Create a new BattleChit and add it to the end of the list. */
    private void addBattleChit(final String bareImageName, boolean inverted,
        int tag, String hexLabel)
    {
        String imageName = bareImageName;
        if (imageName.equals(Constants.titan))
        {
            if (inverted)
            {
                imageName = getTitanBasename(defenderMarkerId);
            }
            else
            {
                imageName = getTitanBasename(attackerMarkerId);
            }
        }
        String colorName;
        if (inverted)
        {
            colorName = getColorByMarkerId(defenderMarkerId);
        }
        else
        {
            colorName = getColorByMarkerId(attackerMarkerId);
        }
        BattleChit chit = new BattleChit(5 * Scale.get(), imageName, inverted,
            tag, hexLabel, colorName, this);
        battleChits.add(chit);
    }

    List<Chit> getRecruitedChits()
    {
        return Collections.unmodifiableList(recruitedChits);
    }

    List<Chit> getPossibleRecruitChits()
    {
        return Collections.unmodifiableList(possibleRecruitChits);
    }

    void addRecruitedChit(String imageName, String hexLabel)
    {
        int scale = 2 * Scale.get();
        GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
        Chit chit = new Chit(scale, imageName);
        Point startingPoint = hex.getOffCenter();
        Point point = new Point(startingPoint);
        point.x -= scale / 2;
        point.y -= scale / 2;
        chit.setLocation(point);
        recruitedChits.add(chit);
    }

    // one chit, one hex
    void addPossibleRecruitChit(String imageName, String hexLabel)
    {
        int scale = 2 * Scale.get();
        GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
        Chit chit = new Chit(scale, imageName);
        Point startingPoint = hex.getOffCenter();
        Point point = new Point(startingPoint);
        point.x -= scale / 2;
        point.y -= scale / 2;
        chit.setLocation(point);
        possibleRecruitChits.add(chit);
    }

    // all possible recuit chits, one hex
    void addPossibleRecruitChits(List<CreatureType> imageNameList,
        String hexLabel)
    {
        Iterator<CreatureType> it = imageNameList.iterator();
        int size = imageNameList.size();
        int num = size;

        while (it.hasNext())
        {
            Object o = it.next();
            String imageName;
            if (o instanceof String)
            {
                imageName = (String)o;
            }
            else if (o instanceof Creature)
            {
                imageName = ((Creature)o).getName();
            }
            else
            {
                LOGGER.log(Level.SEVERE,
                    "Only String or Creature in addPossibleRecruitChits() !");
                return;
            }
            int scale = 2 * Scale.get();
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            Chit chit = new Chit(scale, imageName);
            Point startingPoint = hex.getOffCenter();
            Point point = new Point(startingPoint);
            point.x -= scale / 2;
            point.y -= scale / 2;
            int offset = (num - ((size / 2) + 1));
            point.x += ((offset * scale) + ((size % 2 == 0 ? (scale / 2) : 0)))
                / size;
            point.y += ((offset * scale) + ((size % 2 == 0 ? (scale / 2) : 0)))
                / size;
            num--;
            chit.setLocation(point);
            possibleRecruitChits.add(chit);
        }
    }

    // all hexes
    public void addPossibleRecruitChits(String markerId, Set<String> set)
    {
        clearPossibleRecruitChits();

        if (recruitChitMode == Options.showRecruitChitsNumNone)
        {
            return;
        }

        // set is a set of possible target hexes
        List<CreatureType> oneElemList = new ArrayList<CreatureType>();

        Iterator<String> it = set.iterator();
        while (it.hasNext())
        {
            String hexLabel = it.next();
            List<CreatureType> recruits = findEligibleRecruits(markerId,
                hexLabel);

            if (recruits != null && recruits.size() > 0)
            {
                switch (recruitChitMode)
                {
                    case Options.showRecruitChitsNumAll:
                        break;

                    case Options.showRecruitChitsNumRecruitHint:
                        oneElemList.clear();
                        Creature hint = chooseBestPotentialRecruit(markerId,
                            hexLabel, recruits);
                        oneElemList.add(hint);
                        recruits = oneElemList;
                        break;

                    case Options.showRecruitChitsNumStrongest:
                        oneElemList.clear();
                        CreatureType strongest = recruits
                            .get(recruits.size() - 1);
                        oneElemList.add(strongest);
                        recruits = oneElemList;
                        break;
                }
                addPossibleRecruitChits(recruits, hexLabel);
            }
        }
    }

    Creature chooseBestPotentialRecruit(String markerId, String hexLabel,
        List<CreatureType> recruits)
    {
        LegionInfo legion = getLegionInfo(markerId);
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        // NOTE! Below the simpleAI is an object, not class! 
        Creature recruit = (Creature)simpleAI.getVariantRecruitHint(legion,
            hex, recruits);
        return recruit;
    }

    void removeRecruitChit(String hexLabel)
    {
        Iterator<Chit> it = recruitedChits.iterator();
        while (it.hasNext())
        {
            Chit chit = it.next();
            // TODO the next line can cause an NPE when the user closes the client app
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            if (hex != null && hex.contains(chit.getCenter()))
            {
                it.remove();
                return;
            }
        }
        Iterator<Chit> it2 = possibleRecruitChits.iterator();
        while (it2.hasNext())
        {
            Chit chit = it2.next();
            // TODO the next line can cause an NPE when the user closes the client app
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            if (hex != null && hex.contains(chit.getCenter()))
            {
                it2.remove();
                return;
            }
        }
    }

    void clearPossibleRecruitChits()
    {
        clearRecruitChits();
    }

    void clearRecruitChits()
    {
        recruitedChits.clear();
        possibleRecruitChits.clear();
        // XXX Only repaint needed hexes.
        if (board != null)
        {
            board.repaint();
        }
    }

    private void clearUndoStack()
    {
        undoStack.clear();
    }

    private Object popUndoStack()
    {
        Object object = undoStack.removeFirst();
        return object;
    }

    private void pushUndoStack(Object object)
    {
        undoStack.addFirst(object);
    }

    private boolean isUndoStackEmpty()
    {
        return undoStack.isEmpty();
    }

    String getMoverId()
    {
        return moverId;
    }

    void setMoverId(String moverId)
    {
        this.moverId = moverId;
    }

    MasterBoard getBoard()
    {
        return board;
    }

    private static String propNameForceViewBoard = "net.sf.colossus.forceViewBoard";

    public void initBoard()
    {
        LOGGER.log(Level.FINEST, player.getName() + " Client.initBoard()");
        if (isRemote())
        {
            VariantSupport.loadVariant(options
                .getStringOption(Options.variant), false);
        }

        String viewModeName = options.getStringOption(Options.viewMode);
        viewMode = options.getNumberForViewMode(viewModeName);

        String rcMode = options
            .getStringOption(Options.showRecruitChitsSubmenu);
        if (rcMode == null || rcMode.equals(""))
        {
            // not set: convert from old "showAllRecruitChits" option
            boolean showAll = options.getOption(Options.showAllRecruitChits);
            if (showAll)
            {
                rcMode = Options.showRecruitChitsAll;
            }
            else
            {
                rcMode = Options.showRecruitChitsStrongest;
            }
            // initialize new option
            options.setOption(Options.showRecruitChitsSubmenu, rcMode);
            // clean up obsolete option from cfg file
            options.removeOption(Options.showAllRecruitChits);
        }
        recruitChitMode = options.getNumberForRecruitChitSelection(rcMode);

        // Intended for stresstest, to see whats happening, and that graphics
        // stuff is there done, too.
        // This here works only if name setting is done "by-type", so that
        // at least one AI gets a name ending with "1".
        boolean forceViewBoard = false;
        String propViewBoard = System.getProperty(propNameForceViewBoard);
        if (propViewBoard != null && propViewBoard.equalsIgnoreCase("yes"))
        {
            forceViewBoard = true;
            options.setOption(Options.showEventViewer, new String("true"));
            options.setOption(Options.showStatusScreen, new String("true"));
        }

        if (!getOption(Options.autoPlay)
            || (forceViewBoard && (player.getName().endsWith("1")
                || getStringOption(Options.playerType).endsWith("Human") || getStringOption(
                Options.playerType).endsWith("Network"))))
        {
            disposeEventViewer();
            disposePreferencesWindow();
            disposeEngagementResults();
            disposeInspector();
            disposeCaretakerDisplay();
            disposeLogWindow();
            disposeMasterBoard();

            board = new MasterBoard(this);
            initEventViewer();
            initShowEngagementResults();
            initPreferencesWindow();
            optionTrigger(Options.showAutoInspector);
            optionTrigger(Options.showLogWindow);
            optionTrigger(Options.showCaretaker);

            focusBoard();
        }

        if (startedByWebClient)
        {
            if (webClient != null)
            {
                webClient.notifyComingUp();
            }
        }
    }

    public String getPlayerName()
    {
        return player.getName();
    }

    public void setPlayerName(String playerName)
    {
        this.player.setName(playerName);
        // all those just for debugging purposes:
        net.sf.colossus.webcommon.InstanceTracker.setId(this, "Client "
            + playerName);
        // set name right for the autoplay-AI
        net.sf.colossus.webcommon.InstanceTracker.setId(this.simpleAI,
            "Autoplay-SimpleAI " + playerName);
        if (ai != simpleAI)
        {
            // set name right for the AI if this is an AI player
            net.sf.colossus.webcommon.InstanceTracker.setId(ai, "AI: "
                + playerName);
        }
        sct.fixName(playerName);
    }

    SummonAngel getSummonAngel()
    {
        return summonAngel;
    }

    public void createSummonAngel(String markerId)
    {
        if (getOption(Options.autoSummonAngels))
        {
            String typeColonDonor = ai.summonAngel(markerId);
            List<String> parts = Split.split(':', typeColonDonor);
            String unit = parts.get(0);
            String donor = parts.get(1);
            doSummon(markerId, donor, unit);
        }
        else
        {
            board.deiconify();
            focusBoard();
            summonAngel = SummonAngel.summonAngel(this, markerId);
        }
    }

    String getDonorId()
    {
        return donorId;
    }

    boolean donorHas(String name)
    {
        if (donorId == null)
        {
            return false;
        }
        LegionInfo info = getLegionInfo(donorId);
        return info.getContents().contains(name);
    }

    public void askAcquireAngel(String markerId, List<String> recruits)
    {
        if (getOption(Options.autoAcquireAngels))
        {
            acquireAngelCallback(markerId, ai.acquireAngel(markerId, recruits));
        }
        else
        {
            board.deiconify();
            new AcquireAngel(board.getFrame(), this, markerId, recruits);
        }
    }

    void acquireAngelCallback(String markerId, String angelType)
    {
        server.acquireAngel(markerId, angelType);
    }

    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    private boolean chooseWhetherToTeleport(String hexLabel)
    {
        if (getOption(Options.autoMasterMove))
        {
            return false;
        }
        // No point in teleporting if entry side is moot.
        if (!isOccupied(hexLabel))
        {
            return false;
        }

        String[] options = new String[2];
        options[0] = "Teleport";
        options[1] = "Move Normally";
        int answer = JOptionPane.showOptionDialog(board, "Teleport?",
            "Teleport?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

        return (answer == JOptionPane.YES_OPTION);
    }

    /** Allow the player to choose whether to take a penalty (fewer dice
     *  or higher strike number) in order to be allowed to carry. */
    public void askChooseStrikePenalty(List<String> choices)
    {
        if (getOption(Options.autoPlay))
        {
            String choice = ai.pickStrikePenalty(choices);
            assignStrikePenalty(choice);
        }
        else
        {
            new PickStrikePenalty(battleBoard, this, choices);
        }
    }

    void assignStrikePenalty(String prompt)
    {
        server.assignStrikePenalty(prompt);
    }

    private JFrame getMapOrBoardFrame()
    {
        JFrame frame = null;
        if (battleBoard != null)
        {
            frame = battleBoard;
        }
        else if (board != null)
        {
            frame = board.getFrame();
        }
        return frame;
    }

    public void showErrorMessage(String reason, String title)
    {
        // I do not use null or a simple frame, because then the System.exit(0)
        // does not exit by itself (due to some bug in Swing/AWT).
        KFrame f = new KFrame("Dummyframe for Client error message dialog");
        JOptionPane.showMessageDialog(f, reason, title,
            JOptionPane.ERROR_MESSAGE);
        f.dispose();
        f = null;
    }

    String getMessage()
    {
        return this.message;
    }

    String message = "";

    public void showMessageDialog(String message)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            doShowMessageDialog(message);
        }
        else
        {
            this.message = message;
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        doShowMessageDialog(getMessage());
                    }
                });
            }
            catch (InterruptedException e)
            {/* ignore */
            }
            catch (InvocationTargetException e2)
            {/* ignore */
            }
        }
    }

    void doShowMessageDialog(String message)
    {
        // Don't bother showing messages to AI players.  Perhaps we
        // should log them.
        if (getOption(Options.autoPlay))
        {
            boolean isAI = getPlayerInfo().isAI();
            if ((message.equals("Draw") || message.endsWith(" wins")) && !isAI
                && !getOption(Options.autoQuit))
            {
                // show it for humans, even in autoplay,
                //  but not when autoQuit set (=> remote stresstest)
            }
            else
            {
                return;
            }
        }
        JFrame frame = getMapOrBoardFrame();
        if (frame != null)
        {
            JOptionPane.showMessageDialog(frame, message);
        }
    }

    // TODO Move legion markers to slayer on client side.
    public void tellPlayerElim(String deadPlayerName, String slayerName)
    {
        LOGGER.log(Level.FINEST, this.player.getName() + " tellPlayerElim("
            + deadPlayerName + ", " + slayerName + ")");
        PlayerInfo info = getPlayerInfo(deadPlayerName);

        if (info != null)
        {
            // TODO Merge these
            info.setDead(true);
            info.removeAllLegions();
            // otherwise called too early, e.g. someone quitted
            // already during game start...
            if (predictSplits != null)
            {
                predictSplits[getPlayerNum(deadPlayerName)] = null;
            }

        }

        if (this.player.getName().equals(deadPlayerName))
        {
            playerAlive = false;
        }
    }

    public void tellGameOver(String message)
    {
        gameOver = true;
        if (webClient != null)
        {
            webClient.tellGameEnds();
        }

        if (board != null)
        {
            // @@ TODO CleKa: take those in use?
            // defaultCursor();
            // board.setGameOverState();
            showMessageDialog(message);
        }
    }

    boolean isGameOver()
    {
        return gameOver;
    }

    void doFight(String hexLabel)
    {
        if (!isMyTurn())
        {
            return;
        }
        if (summonAngel != null)
        {
            List<String> legions = getLegionsByHex(hexLabel);
            if (legions.size() != 1)
            {
                LOGGER
                    .log(Level.SEVERE, "Not exactly one legion in donor hex");
                return;
            }
            String markerId = legions.get(0);
            donorId = markerId;

            server.setDonor(markerId);
            summonAngel.updateChits();
            summonAngel.repaint();
            getLegionInfo(markerId).getMarker().repaint();
        }
        else
        {
            engage(hexLabel);
        }
    }

    public void askConcede(String allyMarkerId, String enemyMarkerId)
    {
        if (getOption(Options.autoConcede))
        {
            answerConcede(allyMarkerId, ai.concede(
                getLegionInfo(allyMarkerId), getLegionInfo(enemyMarkerId)));
        }
        else
        {
            Concede.concede(this, board.getFrame(), allyMarkerId,
                enemyMarkerId);
        }
    }

    public void askFlee(String allyMarkerId, String enemyMarkerId)
    {
        if (getOption(Options.autoFlee))
        {
            answerFlee(allyMarkerId, ai.flee(getLegionInfo(allyMarkerId),
                getLegionInfo(enemyMarkerId)));
        }
        else
        {
            Concede.flee(this, board.getFrame(), allyMarkerId, enemyMarkerId);
        }
    }

    void answerFlee(String markerId, boolean answer)
    {
        if (answer)
        {
            server.flee(markerId);
        }
        else
        {
            server.doNotFlee(markerId);
        }
    }

    void answerConcede(String markerId, boolean answer)
    {
        if (answer)
        {
            concede(markerId);
        }
        else
        {
            doNotConcede(markerId);
        }
    }

    public void askNegotiate(String attackerId, String defenderId)
    {
        this.attackerMarkerId = attackerId;
        this.defenderMarkerId = defenderId;

        if (getOption(Options.autoNegotiate))
        {
            // XXX AI players just fight for now.
            Proposal proposal = new Proposal(attackerId, defenderId, true,
                false, null, null);
            makeProposal(proposal);
        }
        else
        {
            negotiate = new Negotiate(this, attackerId, defenderId);
        }
    }

    /** Called from both Negotiate and ReplyToProposal. */
    void negotiateCallback(Proposal proposal, boolean respawn)
    {
        if (proposal != null && proposal.isFight())
        {
            fight(getHexForLegion(attackerMarkerId));
            return;
        }
        else if (proposal != null)
        {
            makeProposal(proposal);
        }

        if (respawn)
        {
            negotiate = new Negotiate(this, attackerMarkerId, defenderMarkerId);
        }
    }

    private void makeProposal(Proposal proposal)
    {
        server.makeProposal(proposal.toString());
    }

    /** Inform this player about the other player's proposal. */
    public void tellProposal(String proposalString)
    {
        Proposal proposal = Proposal.makeFromString(proposalString);
        new ReplyToProposal(this, proposal);
    }

    public BattleHex getBattleHex(BattleChit chit)
    {
        return HexMap.getHexByLabel(getBattleTerrain(), chit
            .getCurrentHexLabel());
    }

    BattleHex getStartingBattleHex(BattleChit chit)
    {
        return HexMap.getHexByLabel(getBattleTerrain(), chit
            .getStartingHexLabel());
    }

    public boolean isOccupied(BattleHex hex)
    {
        return !getBattleChits(hex.getLabel()).isEmpty();
    }

    private String getBattleChitDescription(BattleChit chit)
    {
        if (chit == null)
        {
            return "";
        }
        BattleHex hex = getBattleHex(chit);
        return chit.getCreatureName() + " in " + hex.getDescription();
    }

    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List<String> rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft,
        Set<String> carryTargetDescriptions)
    {
        BattleChit chit = getBattleChit(strikerTag);
        if (chit != null)
        {
            chit.setStruck(true);
        }

        BattleChit targetChit = getBattleChit(targetTag);
        if (battleBoard != null)
        {
            battleBoard.setDiceValues(getBattleChitDescription(chit),
                getBattleChitDescription(targetChit), strikeNumber, rolls);
            battleBoard.unselectAllHexes();
        }

        if (targetChit != null)
        {
            if (killed)
            {
                targetChit.setDead(true);
            }
            else
            {
                if (damage > 0)
                {
                    targetChit.setHits(targetChit.getHits() + damage);
                }
            }
        }

        if (strikerTag == Constants.hexDamage)
        {
            // Do not trigger auto strikes in parallel with setupBattleFight()
        }
        else if (carryDamageLeft >= 1 && !carryTargetDescriptions.isEmpty())
        {
            pickCarries(carryDamageLeft, carryTargetDescriptions);
        }
        else
        {
            doAutoStrikes();
        }
    }

    public void nak(String reason, String errmsg)
    {
        LOGGER.log(Level.WARNING, player.getName() + " got nak for " + reason
            + " " + errmsg);
        recoverFromNak(reason, errmsg);
    }

    private void recoverFromNak(String reason, String errmsg)
    {
        LOGGER.log(Level.FINEST, player.getName() + " recoverFromNak "
            + reason + " " + errmsg);
        if (reason == null)
        {
            LOGGER.log(Level.SEVERE, "recoverFromNak with null reason!");
        }
        else if (reason.equals(Constants.doSplit))
        {
            showMessageDialog(errmsg);
            kickSplit();
        }
        else if (reason.equals(Constants.doneWithSplits))
        {
            showMessageDialog(errmsg);
            kickSplit();
        }
        else if (reason.equals(Constants.doMove))
        {
            showMessageDialog(errmsg);
            kickMoves();
        }
        else if (reason.equals(Constants.doneWithMoves))
        {
            showMessageDialog(errmsg);
            kickMoves();
        }
        else if (reason.equals(Constants.doBattleMove))
        {
            handleFailedBattleMove();
        }
        else if (reason.equals(Constants.doneWithBattleMoves))
        {
            // @todo: why can we ignore this?
        }
        else if (reason.equals(Constants.assignStrikePenalty))
        {
            doAutoStrikes();
        }
        else if (reason.equals(Constants.strike))
        {
            doAutoStrikes();
        }
        else if (reason.equals(Constants.doneWithStrikes))
        {
            showMessageDialog(errmsg);
        }
        else if (reason.equals(Constants.doneWithEngagements))
        {
            showMessageDialog(errmsg);
        }
        else if (reason.equals(Constants.doRecruit))
        {
            // @todo: why can we ignore this?
        }
        else if (reason.equals(Constants.doneWithRecruits))
        {
            // @todo: why can we ignore this?
        }
        else
        {
            LOGGER.log(Level.WARNING, player.getName() + " unexpected nak "
                + reason + " " + errmsg);
        }
    }

    private void pickCarries(int carryDamage,
        Set<String> carryTargetDescriptions)
    {
        if (!isMyBattlePhase())
        {
            return;
        }

        if (carryDamage < 1 || carryTargetDescriptions.isEmpty())
        {
            leaveCarryMode();
        }
        else if (carryTargetDescriptions.size() == 1
            && getOption(Options.autoCarrySingle))
        {
            Iterator<String> it = carryTargetDescriptions.iterator();
            String desc = it.next();
            String targetHex = desc.substring(desc.length() - 2);
            applyCarries(targetHex);
        }
        else
        {
            if (getOption(Options.autoPlay))
            {
                aiPause();
                ai.handleCarries(carryDamage, carryTargetDescriptions);
            }
            else
            {
                new PickCarry(battleBoard, this, carryDamage,
                    carryTargetDescriptions);
            }
        }
    }

    void cleanupNegotiationDialogs()
    {
        if (negotiate != null)
        {
            negotiate.dispose();
            negotiate = null;
        }
        if (replyToProposal != null)
        {
            replyToProposal.dispose();
            replyToProposal = null;
        }
    }

    public void initBattle(String masterHexLabel, int battleTurnNumber,
        String battleActivePlayerName, Constants.BattlePhase battlePhase,
        String attackerMarkerId, String defenderMarkerId)
    {
        cleanupNegotiationDialogs();

        this.battleTurnNumber = battleTurnNumber;
        setBattleActivePlayerName(battleActivePlayerName);
        this.battlePhase = battlePhase;
        this.attackerMarkerId = attackerMarkerId;
        this.defenderMarkerId = defenderMarkerId;
        this.battleSite = masterHexLabel;

        int attackerSide = getLegionInfo(attackerMarkerId).getEntrySide();
        int defenderSide = (attackerSide + 3) % 6;
        getLegionInfo(defenderMarkerId).setEntrySide(defenderSide);

        if (board != null)
        {
            // TODO should be done on the EDT
            battleBoard = new BattleBoard(this, masterHexLabel,
                attackerMarkerId, defenderMarkerId);
            battleBoard.setPhase(battlePhase);
            battleBoard.setTurn(battleTurnNumber);
            battleBoard.setBattleMarkerLocation(false, "X" + attackerSide);
            battleBoard.setBattleMarkerLocation(true, "X" + defenderSide);
            focusMap();
        }
    }

    public void cleanupBattle()
    {
        LOGGER.log(Level.FINEST, player.getName() + " Client.cleanupBattle()");
        if (battleBoard != null)
        {
            battleBoard.dispose();
            battleBoard = null;
        }
        battleChits.clear();
        battlePhase = null;
        battleTurnNumber = -1;
        battleActivePlayerName = null;
    }

    public int[] getReinforcementTurns()
    {
        int[] reinforcementTurns = { 4 };
        return reinforcementTurns;
    }

    public int getMaxBattleTurns()
    {
        return 7;
    }

    private void highlightEngagements()
    {
        if (board != null)
        {
            if (getPlayerName().equals(getActivePlayerName()))
            {
                focusBoard();
            }
            board.highlightEngagements();
        }
    }

    public void nextEngagement()
    {
        highlightEngagements();
        if (isMyTurn())
        {
            if (getOption(Options.autoPickEngagements))
            {
                aiPause();
                String hexLabel = ai.pickEngagement();
                if (hexLabel != null)
                {
                    engage(hexLabel);
                }
                else
                {
                    doneWithEngagements();
                }
            }
            else
            {
                defaultCursor();

                /* XXX Only if not summoning, acquiring, etc.
                 if (findEngagements().isEmpty())
                 {
                 doneWithEngagements();
                 }
                 */
            }
            if (findEngagements().isEmpty() && board != null)
            {
                board.enableDoneAction();
            }
        }
    }

    /** Used for human players only.  */
    void doRecruit(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        if (isMyTurn() && isMyLegion(markerId) && info.hasRecruited())
        {
            undoRecruit(markerId);
            return;
        }

        if (info == null || !info.canRecruit() || !isMyTurn()
            || !isMyLegion(markerId))
        {
            return;
        }

        String hexLabel = getHexForLegion(markerId);
        List<CreatureType> recruits = findEligibleRecruits(markerId, hexLabel);
        String hexDescription = MasterBoard.getHexByLabel(hexLabel)
            .getDescription();

        String recruitName = PickRecruit.pickRecruit(board.getFrame(),
            recruits, hexDescription, markerId, this);

        if (recruitName == null)
        {
            return;
        }

        String recruiterName = findRecruiterName(markerId, recruitName,
            hexDescription);
        if (recruiterName == null)
        {
            return;
        }

        doRecruit(markerId, recruitName, recruiterName);
    }

    public void doRecruit(String markerId, String recruitName,
        String recruiterName)
    {
        // Call server even if some arguments are null, to get past
        // reinforcement.
        server.doRecruit(markerId, recruitName, recruiterName);
    }

    /** Always needs to call server.doRecruit(), even if no recruit is
     *  wanted, to get past the reinforcing phase. */
    public void doReinforce(String markerId)
    {
        if (getOption(Options.autoReinforce))
        {
            ai.reinforce(getLegionInfo(markerId));
        }
        else
        {
            String hexLabel = getHexForLegion(markerId);

            List<CreatureType> recruits = findEligibleRecruits(markerId,
                hexLabel);
            String hexDescription = MasterBoard.getHexByLabel(hexLabel)
                .getDescription();

            String recruitName = PickRecruit.pickRecruit(board.getFrame(),
                recruits, hexDescription, markerId, this);

            String recruiterName = null;
            if (recruitName != null)
            {
                recruiterName = findRecruiterName(markerId, recruitName,
                    hexDescription);
            }
            doRecruit(markerId, recruitName, recruiterName);
        }
    }

    public void didRecruit(String markerId, String recruitName,
        String recruiterName, int numRecruiters)
    {
        String hexLabel = getHexForLegion(markerId);
        if (hexLabel == null)
        {
            LOGGER.log(Level.SEVERE, "Client.didRecruit() null hexLabel for "
                + markerId);
        }
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }

        List<String> recruiters = new ArrayList<String>();
        if (numRecruiters >= 1 && recruiterName != null)
        {
            for (int i = 0; i < numRecruiters; i++)
            {
                recruiters.add(recruiterName);
            }
            revealCreatures(markerId, recruiters, Constants.reasonRecruiter);
        }
        addCreature(markerId, recruitName, Constants.reasonRecruited);

        LegionInfo info = getLegionInfo(markerId);
        info.setRecruited(true);
        info.setLastRecruit(recruitName);

        if (eventViewer != null)
        {
            eventViewer.recruitEvent(markerId, info.getHeight(), recruitName,
                recruiters);
        }

        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            addRecruitedChit(recruitName, hexLabel);
            hex.repaint();
            board.highlightPossibleRecruits();
        }
    }

    public void undidRecruit(String markerId, String recruitName)
    {
        String hexLabel = getHexForLegion(markerId);
        removeCreature(markerId, recruitName, Constants.reasonUndidRecruit);
        getLegionInfo(markerId).setRecruited(false);
        if (eventViewer != null)
        {
            eventViewer.undoEvent(RevealEvent.eventRecruit, markerId, null,
                turnNumber);
        }
        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            removeRecruitChit(hexLabel);
            hex.repaint();
            board.highlightPossibleRecruits();
        }
    }

    /** null means cancel.  "none" means no recruiter (tower creature). */
    private String findRecruiterName(String markerId, String recruitName,
        String hexDescription)
    {
        String recruiterName = null;

        List<String> recruiters = findEligibleRecruiters(markerId, recruitName);

        int numEligibleRecruiters = recruiters.size();
        if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiterName = "none";
        }
        else if (getOption(Options.autoPickRecruiter)
            || numEligibleRecruiters == 1)
        {
            // If there's only one possible recruiter, or if
            // the user has chosen the autoPickRecruiter option,
            // then just reveal the first possible recruiter.
            recruiterName = recruiters.get(0);
        }
        else
        {
            recruiterName = PickRecruiter.pickRecruiter(board.getFrame(),
                recruiters, hexDescription, markerId, this);
        }
        return recruiterName;
    }

    /** Needed if we load a game outside the split phase, where
     *  active player and turn are usually set. */
    public void setupTurnState(String activePlayerName, int turnNumber)
    {
        this.activePlayerName = activePlayerName;
        this.turnNumber = turnNumber;
        if (eventViewer != null)
        {
            eventViewer.turnOrPlayerChange(this, turnNumber,
                getPlayerNum(activePlayerName));
        }
    }

    private void resetAllMoves()
    {
        Iterator<LegionInfo> it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = it.next();
            info.setMoved(false);
            info.setTeleported(false);
            info.setRecruited(false);
        }
    }

    private void defaultCursor()
    {
        if (board != null)
        {
            board.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void waitCursor()
    {
        if (board != null)
        {
            board.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }

    public void setBoardActive(boolean val)
    {
        board.setBoardActive(val);
    }

    public void setupSplit(String activePlayerName, int turnNumber)
    {
        clearUndoStack();
        cleanupNegotiationDialogs();

        this.activePlayerName = activePlayerName;
        this.turnNumber = turnNumber;

        if (eventViewer != null)
        {
            eventViewer.turnOrPlayerChange(this, turnNumber,
                getPlayerNum(activePlayerName));
        }

        this.phase = Constants.Phase.SPLIT;

        numSplitsThisTurn = 0;

        resetAllMoves();

        if (board != null)
        {
            disposeMovementDie();
            board.setupSplitMenu();
            board.fullRepaint(); // Ensure that movement die goes away
            if (isMyTurn())
            {
                if (turnNumber == 1)
                {
                    board.disableDoneAction("Split legions in first round");
                }
                focusBoard();
                defaultCursor();
                if (!getOption(Options.autoSplit)
                    && (getPlayerInfo().getMarkersAvailable().size() < 1 || findTallLegionHexes(
                        4).isEmpty()))
                {
                    doneWithSplits();
                }
            }
            else
            {
                waitCursor();
            }
        }
        updateStatusScreen();
        kickSplit();
    }

    private void kickSplit()
    {
        if (isMyTurn() && getOption(Options.autoSplit) && !isGameOver())
        {
            boolean done = ai.split();
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    public void setupMove()
    {
        this.phase = Constants.Phase.MOVE;
        clearUndoStack();
        if (board != null)
        {
            board.setupMoveMenu();
        }
        if (isMyTurn())
        {
            defaultCursor();
        }
        updateStatusScreen();
    }

    public void setupFight()
    {
        clearUndoStack();
        this.phase = Constants.Phase.FIGHT;
        if (board != null)
        {
            board.setupFightMenu();
        }
        updateStatusScreen();

        if (isMyTurn())
        {
            defaultCursor();
            if (getOption(Options.autoPickEngagements))
            {
                aiPause();
                ai.pickEngagement();
            }
            else
            {
                if (findEngagements().isEmpty())
                {
                    doneWithEngagements();
                }
            }
        }
    }

    public void setupMuster()
    {
        clearUndoStack();
        cleanupNegotiationDialogs();

        this.phase = Constants.Phase.MUSTER;

        if (board != null)
        {
            board.setupMusterMenu();
            if (isMyTurn())
            {
                focusBoard();
                defaultCursor();
                if (!getOption(Options.autoRecruit)
                    && getPossibleRecruitHexes().isEmpty())
                {
                    doneWithRecruits();
                }
            }
        }
        updateStatusScreen();

        // I changed the "&& !isGameOver()" to "&& I am not dead";
        // before, this makes autorecruit stop working also for human
        // when they won against all others and continue playing
        // (just for growing bigger creatures ;-)
        if (getOption(Options.autoRecruit) && playerAlive && isMyTurn()
            && this.phase == Constants.Phase.MUSTER)
        {
            ai.muster();
            // Do not automatically say we are done.
            // Allow humans to override.
            if (options.getOption(Options.autoPlay))
            {
                doneWithRecruits();
            }
        }
    }

    public void setupBattleSummon(String battleActivePlayerName,
        int battleTurnNumber)
    {
        this.battlePhase = Constants.BattlePhase.SUMMON;
        setBattleActivePlayerName(battleActivePlayerName);
        this.battleTurnNumber = battleTurnNumber;

        if (battleBoard != null)
        {
            battleBoard.setPhase(battlePhase);
            battleBoard.setTurn(battleTurnNumber);
            if (isMyBattlePhase())
            {
                focusMap();
                battleBoard.setupSummonMenu();
                defaultCursor();
            }
            else
            {
                waitCursor();
            }
        }
        updateStatusScreen();
    }

    public void setupBattleRecruit(String battleActivePlayerName,
        int battleTurnNumber)
    {
        this.battlePhase = Constants.BattlePhase.RECRUIT;
        setBattleActivePlayerName(battleActivePlayerName);
        this.battleTurnNumber = battleTurnNumber;

        if (battleBoard != null)
        {
            battleBoard.setPhase(battlePhase);
            battleBoard.setTurn(battleTurnNumber);
            if (isMyBattlePhase())
            {
                focusMap();
                battleBoard.setupRecruitMenu();
            }
        }
        updateStatusScreen();
    }

    private void resetAllBattleMoves()
    {
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            chit.setMoved(false);
            chit.setStruck(false);
        }
    }

    public void setupBattleMove(String battleActivePlayerName,
        int battleTurnNumber)
    {
        setBattleActivePlayerName(battleActivePlayerName);
        this.battleTurnNumber = battleTurnNumber;

        // Just in case the other player started the battle
        // really quickly.
        cleanupNegotiationDialogs();
        resetAllBattleMoves();
        this.battlePhase = Constants.BattlePhase.MOVE;
        if (battleBoard != null)
        {
            battleBoard.setPhase(battlePhase);
            battleBoard.setTurn(battleTurnNumber);
            if (isMyBattlePhase())
            {
                focusMap();
                defaultCursor();
                battleBoard.setupMoveMenu();
            }
        }
        updateStatusScreen();
        if (isMyBattlePhase() && getOption(Options.autoPlay))
        {
            bestMoveOrder = ai.battleMove();
            failedBattleMoves = new ArrayList<CritterMove>();
            kickBattleMove();
        }
    }

    private void kickBattleMove()
    {
        if (bestMoveOrder == null || bestMoveOrder.isEmpty())
        {
            if (failedBattleMoves == null || failedBattleMoves.isEmpty())
            {
                doneWithBattleMoves();
            }
            else
            {
                retryFailedBattleMoves();
            }
        }
        else
        {
            Iterator<CritterMove> it = bestMoveOrder.iterator();
            CritterMove cm = it.next();
            tryBattleMove(cm);
        }
    }

    public void tryBattleMove(CritterMove cm)
    {
        BattleChit critter = cm.getCritter();
        String hexLabel = cm.getEndingHexLabel();
        doBattleMove(critter.getTag(), hexLabel);
        aiPause();
    }

    private void retryFailedBattleMoves()
    {
        bestMoveOrder = failedBattleMoves;
        failedBattleMoves = null;
        ai.retryFailedBattleMoves(bestMoveOrder);
        kickBattleMove();
    }

    /** Used for both strike and strikeback. */
    public void setupBattleFight(Constants.BattlePhase battlePhase,
        String battleActivePlayerName)
    {
        this.battlePhase = battlePhase;
        setBattleActivePlayerName(battleActivePlayerName);
        if (battlePhase == Constants.BattlePhase.FIGHT)
        {
            markOffboardCreaturesDead();
        }

        if (battleBoard != null)
        {
            battleBoard.setPhase(battlePhase);
            battleBoard.setTurn(battleTurnNumber);
            if (isMyBattlePhase())
            {
                focusMap();
                defaultCursor();
            }
            else
            {
                waitCursor();
            }
            battleBoard.setupFightMenu();
        }
        updateStatusScreen();

        doAutoStrikes();
    }

    /** Create marker if necessary, and place it in hexLabel. */
    public void tellLegionLocation(String markerId, String hexLabel)
    {
        LegionInfo info = getLegionInfo(markerId);
        info.setHexLabel(hexLabel);

        if (board != null)
        {
            Marker marker = new Marker(3 * Scale.get(), markerId, this);
            setMarker(markerId, marker);
            info.setMarker(marker);
            board.alignLegions(hexLabel);
        }
    }

    /** Create new markers in response to a rescale. */
    void recreateMarkers()
    {
        markers.clear();

        Iterator<Entry<String, LegionInfo>> it = legionInfo.entrySet()
            .iterator();
        while (it.hasNext())
        {
            Entry<String, LegionInfo> entry = it.next();
            LegionInfo info = entry.getValue();
            String markerId = info.getMarkerId();
            String hexLabel = info.getHexLabel();
            Marker marker = new Marker(3 * Scale.get(), markerId, this);
            info.setMarker(marker);
            markers.add(marker);
            board.alignLegions(hexLabel);
        }
    }

    private void setupPlayerLabel()
    {
        if (board != null)
        {
            board.setupPlayerLabel();
        }
    }

    String getColor()
    {
        return color;
    }

    String getShortColor()
    {
        return net.sf.colossus.server.Player.getShortColor(getColor());
    }

    // public for RevealEvent
    public String getShortColor(int playerNum)
    {
        PlayerInfo player = getPlayerInfo(playerNum);
        return net.sf.colossus.server.Player.getShortColor(player.getColor());
    }

    // public for IOracle
    public String getBattleActivePlayerName()
    {
        return battleActivePlayerName;
    }

    public Player getBattleActivePlayer()
    {
        // TODO this should be the other way around -- the Player should be stored
        return Player.getPlayerByName(battleActivePlayerName);
    }

    void setBattleActivePlayerName(String name)
    {
        battleActivePlayerName = name;
    }

    String getBattleActiveMarkerId()
    {
        LegionInfo info = getLegionInfo(defenderMarkerId);
        if (battleActivePlayerName.equals(info.getPlayerName()))
        {
            return defenderMarkerId;
        }
        else
        {
            return attackerMarkerId;
        }
    }

    String getBattleInactiveMarkerId()
    {
        LegionInfo info = getLegionInfo(defenderMarkerId);
        if (battleActivePlayerName.equals(info.getPlayerName()))
        {
            return attackerMarkerId;
        }
        else
        {
            return defenderMarkerId;
        }
    }

    // public for IOracle
    public String getDefenderMarkerId()
    {
        return defenderMarkerId;
    }

    // public for IOracle
    public String getAttackerMarkerId()
    {
        return attackerMarkerId;
    }

    Constants.BattlePhase getBattlePhase()
    {
        return battlePhase;
    }

    // public for IOracle
    public String getBattlePhaseName()
    {
        if (phase == Constants.Phase.FIGHT)
        {
            if (battlePhase != null)
            {
                return battlePhase.toString();
            }
        }
        return "";
    }

    // public for IOracle
    public int getBattleTurnNumber()
    {
        return battleTurnNumber;
    }

    void doBattleMove(int tag, String hexLabel)
    {
        server.doBattleMove(tag, hexLabel);
    }

    private void markBattleMoveSuccessful(int tag, String endingHexLabel)
    {
        if (bestMoveOrder != null)
        {
            Iterator<CritterMove> it = bestMoveOrder.iterator();
            while (it.hasNext())
            {
                CritterMove cm = it.next();
                if (tag == cm.getTag()
                    && endingHexLabel.equals(cm.getEndingHexLabel()))
                {
                    // Remove this CritterMove from the list to show
                    // that it doesn't need to be retried.
                    it.remove();
                }
            }
        }
        kickBattleMove();
    }

    private void handleFailedBattleMove()
    {
        LOGGER.log(Level.FINEST, player.getName() + "handleFailedBattleMove");
        if (bestMoveOrder != null)
        {
            Iterator<CritterMove> it = bestMoveOrder.iterator();
            if (it.hasNext())
            {
                CritterMove cm = it.next();
                it.remove();
                if (failedBattleMoves != null)
                {
                    failedBattleMoves.add(cm);
                }
            }
        }
        kickBattleMove();
    }

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo)
    {
        if (isMyCritter(tag) && !undo)
        {
            pushUndoStack(endingHexLabel);
            if (getOption(Options.autoPlay))
            {
                markBattleMoveSuccessful(tag, endingHexLabel);
            }
        }
        BattleChit chit = getBattleChit(tag);
        if (chit != null)
        {
            chit.setHexLabel(endingHexLabel);
            chit.setMoved(!undo);
        }
        if (battleBoard != null)
        {
            battleBoard.alignChits(startingHexLabel);
            battleBoard.alignChits(endingHexLabel);
            battleBoard.repaint();
            battleBoard.highlightMobileCritters();
        }
    }

    /** Attempt to have critter tag strike the critter in hexLabel. */
    public void strike(int tag, String hexLabel)
    {
        resetStrikeNumbers();
        server.strike(tag, hexLabel);
    }

    /** Attempt to apply carries to the critter in hexLabel. */
    public void applyCarries(String hexLabel)
    {
        server.applyCarries(hexLabel);
        if (battleBoard != null)
        {
            battleBoard.unselectHexByLabel(hexLabel);
            battleBoard.repaint();
        }
    }

    void undoLastBattleMove()
    {
        if (!isUndoStackEmpty())
        {
            String hexLabel = (String)popUndoStack();
            server.undoBattleMove(hexLabel);
        }
    }

    void undoAllBattleMoves()
    {
        while (!isUndoStackEmpty())
        {
            undoLastBattleMove();
        }
    }

    // public for IOracle
    public String getBattleSite()
    {
        return battleSite;
    }

    public String getBattleTerrain()
    {
        MasterHex mHex = MasterBoard.getHexByLabel(battleSite);
        return mHex.getTerrain();
    }

    /** Return true if there are any enemies adjacent to this chit.
     *  Dead critters count as being in contact only if countDead is true. */
    public boolean isInContact(BattleChit chit, boolean countDead)
    {
        BattleHex hex = getBattleHex(chit);

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return false;
        }

        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    BattleChit other = getBattleChit(neighbor.getLabel());
                    if (other != null
                        && (other.isInverted() != chit.isInverted())
                        && (countDead || !other.isDead()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    boolean isActive(BattleChit chit)
    {
        return battleActivePlayerName.equals(getPlayerByTag(chit.getTag())
            .getName());
    }

    /** Return a set of hexLabels. */
    Set<String> findMobileCritterHexes()
    {
        Set<String> set = new HashSet<String>();
        Iterator<BattleChit> it = getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (!chit.hasMoved() && !isInContact(chit, false))
            {
                set.add(chit.getCurrentHexLabel());
            }
        }
        return set;
    }

    /** Return a set of BattleChits. */
    public Set<BattleChit> findMobileBattleChits()
    {
        Set<BattleChit> set = new HashSet<BattleChit>();
        Iterator<BattleChit> it = getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (!chit.hasMoved() && !isInContact(chit, false))
            {
                set.add(chit);
            }
        }
        return set;
    }

    /** Return a set of hexLabels. */
    public Set<String> showBattleMoves(int tag)
    {
        return battleMovement.showMoves(tag);
    }

    /** Return a set of hexLabels. */
    Set<String> findCrittersWithTargets()
    {
        return strike.findCrittersWithTargets();
    }

    /** Return a set of hexLabels. */
    public Set<String> findStrikes(int tag)
    {
        return strike.findStrikes(tag);
    }

    void setStrikeNumbers(int tag, Set<String> targetHexes)
    {
        BattleChit chit = getBattleChit(tag);
        Iterator<String> it = targetHexes.iterator();
        while (it.hasNext())
        {
            String targetHex = it.next();
            BattleChit target = getBattleChit(targetHex);
            target.setStrikeNumber(strike.getStrikeNumber(chit, target));
            Creature striker = (Creature)game.getVariant().getCreatureByName(
                chit.getCreatureName());
            int dice;
            if (striker.isTitan())
            {
                dice = chit.getTitanPower();
            }
            else
            {
                dice = striker.getPower();
            }
            int baseDice = 0;
            int strikeDice = strike.getDice(chit, target, baseDice);
            if (baseDice == dice || getOption(Options.showDiceAjustmentsRange))
            {
                target.setStrikeDice(strikeDice - dice);
            }
        }
    }

    /** reset all strike numbers on chits */
    void resetStrikeNumbers()
    {
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            chit.setStrikeNumber(0);
            chit.setStrikeDice(0);
        }
    }

    Player getPlayerByTag(int tag)
    {
        BattleChit chit = getBattleChit(tag);
        assert chit != null : "Illegal value for tag parameter";

        if (chit.isInverted())
        {
            return getPlayerByMarkerId(defenderMarkerId).getPlayer();
        }
        else
        {
            return getPlayerByMarkerId(attackerMarkerId).getPlayer();
        }
    }

    boolean isMyCritter(int tag)
    {
        return (player.equals(getPlayerByTag(tag)));
    }

    // public for IOracle
    public String getActivePlayerName()
    {
        return activePlayerName;
    }

    public Player getActivePlayer()
    {
        // TODO the Player instance should be stored instead
        return Player.getPlayerByName(activePlayerName);
    }

    public PlayerState getActivePlayerState()
    {
        return getPlayerInfo(getActivePlayer());
    }

    public int getActivePlayerNum()
    {
        return getPlayerNum(activePlayerName);
    }

    Constants.Phase getPhase()
    {
        return phase;
    }

    // public for IOracle
    public String getPhaseName()
    {
        if (phase != null)
        {
            return phase.toString();
        }
        return "";
    }

    // public for IOracle
    public int getTurnNumber()
    {
        return turnNumber;
    }

    private String figureTeleportingLord(String moverId, String hexLabel)
    {
        List<String> lords = listTeleportingLords(moverId, hexLabel);
        String lordName = null;
        switch (lords.size())
        {
            case 0:
                return null;

            case 1:
                lordName = lords.get(0);
                if (lordName.startsWith(Constants.titan))
                {
                    lordName = Constants.titan;
                }
                return lordName;

            default:
                if (getOption(Options.autoPickLord))
                {
                    lordName = lords.get(0);
                    if (lordName.startsWith(Constants.titan))
                    {
                        lordName = Constants.titan;
                    }
                    return lordName;
                }
                else
                {
                    return PickLord.pickLord(this, board.getFrame(), lords);
                }
        }
    }

    /** List the lords eligible to teleport this legion to hexLabel,
     *  as strings. */
    private List<String> listTeleportingLords(String moverId, String hexLabel)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        List<String> lords = new ArrayList<String>();

        LegionInfo info = getLegionInfo(moverId);

        // Titan teleport
        List<String> legions = getLegionsByHex(hexLabel);
        if (!legions.isEmpty())
        {
            String markerId = legions.get(0);
            if (markerId != null && !isMyLegion(markerId) && info.hasTitan())
            {
                lords.add(info.getTitanBasename());
            }
        }

        // Tower teleport
        else
        {
            Iterator<String> it = info.getContents().iterator();
            while (it.hasNext())
            {
                String name = it.next();
                Creature creature = (Creature)game.getVariant()
                    .getCreatureByName(name);
                if (creature != null && creature.isLord()
                    && !lords.contains(name))
                {
                    if (creature.isTitan())
                    {
                        lords.add(info.getTitanBasename());
                    }
                    else
                    {
                        lords.add(name);
                    }
                }
            }
        }
        return lords;
    }

    boolean doMove(String hexLabel)
    {
        return doMove(moverId, hexLabel);
    }

    /** Return true if the move looks legal. */
    public boolean doMove(String moverId, String hexLabel)
    {
        if (moverId == null)
        {
            return false;
        }

        boolean teleport = false;

        Set<String> teleports = listTeleportMoves(moverId);
        Set<String> normals = listNormalMoves(moverId);
        if (teleports.contains(hexLabel) && normals.contains(hexLabel))
        {
            teleport = chooseWhetherToTeleport(hexLabel);
        }
        else if (teleports.contains(hexLabel))
        {
            teleport = true;
        }
        else if (normals.contains(hexLabel))
        {
            teleport = false;
        }
        else
        {
            return false;
        }

        Set<String> entrySides = listPossibleEntrySides(moverId, hexLabel,
            teleport);

        String entrySide = null;
        if (getOption(Options.autoPickEntrySide))
        {
            entrySide = ai.pickEntrySide(hexLabel, moverId, entrySides);
        }
        else
        {
            entrySide = PickEntrySide.pickEntrySide(board.getFrame(),
                hexLabel, entrySides);
        }

        if (!goodEntrySide(entrySide))
        {
            return false;
        }

        String teleportingLord = null;
        if (teleport)
        {
            teleportingLord = figureTeleportingLord(moverId, hexLabel);
        }

        // if this hex is already occupied, return false
        LegionInfo li = getLegionInfo(moverId);
        if (!hexLabel.equals(li.getHexLabel()))
        {
            int friendlyLegions = getNumFriendlyLegions(hexLabel,
                getActivePlayerState());
            if (friendlyLegions > 0)
            {
                return false;
            }
        }

        server.doMove(moverId, hexLabel, entrySide, teleport, teleportingLord);
        return true;
    }

    private boolean goodEntrySide(String entrySide)
    {
        return (entrySide != null && (entrySide.equals(Constants.left)
            || entrySide.equals(Constants.bottom) || entrySide
            .equals(Constants.right)));
    }

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel, String entrySide, boolean teleport,
        String teleportingLord, boolean splitLegionHasForcedMove)
    {
        removeRecruitChit(startingHexLabel);
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }
        LegionInfo info = getLegionInfo(markerId);
        info.setHexLabel(currentHexLabel);
        info.setMoved(true);
        info.setEntrySide(BattleMap.entrySideNum(entrySide));

        // old version server does not send the teleportingLord,
        // socketCLientThread sets then it to null.
        if (teleport && teleportingLord != null)
        {
            getLegionInfo(markerId).setTeleported(true);
            if (eventViewer != null)
            {
                eventViewer.newCreatureRevealEvent(RevealEvent.eventTeleport,
                    markerId, info.getHeight(), teleportingLord, null, 0);
            }
        }
        if (board != null)
        {
            board.alignLegions(startingHexLabel);
            board.alignLegions(currentHexLabel);
            board.highlightUnmovedLegions();
            board.repaint();
            if (isMyLegion(markerId) && !splitLegionHasForcedMove)
            {
                board.enableDoneAction();
            }
        }
        kickMoves();
    }

    public void undidMove(String markerId, String formerHexLabel,
        String currentHexLabel, boolean splitLegionHasForcedMove)
    {
        removeRecruitChit(formerHexLabel);
        removeRecruitChit(currentHexLabel);
        getLegionInfo(markerId).setHexLabel(currentHexLabel);
        getLegionInfo(markerId).setMoved(false);
        boolean didTeleport = getLegionInfo(markerId).hasTeleported();
        getLegionInfo(markerId).setTeleported(false);
        if (board != null)
        {
            board.alignLegions(formerHexLabel);
            board.alignLegions(currentHexLabel);
            board.highlightUnmovedLegions();
            if (isUndoStackEmpty())
            {
                board.disableDoneAction("At least one legion must move");
            }
            if (splitLegionHasForcedMove)
            {
                board.disableDoneAction("Split legion needs to move");
            }

            if (didTeleport && eventViewer != null)
            {
                eventViewer.undoEvent(RevealEvent.eventTeleport, markerId,
                    null, turnNumber);
            }
        }
    }

    /*
     * Reset the cached reservations. 
     * Should be called at begin of each recruit turn, if 
     * reserveRecruit and getReservedCount() are going to be used.
     * 
     */
    public void resetRecruitReservations()
    {
        recruitReservations.clear();
    }

    /*
     * Reserve one. Expects that getReservedCount() had been called in this 
     * turn for same creature before called reserveRecruit (= to cache the 
     * caretakers stack value).
     * Returns whether creature can still be recruited (=is available according
     * to caretakers stack plus reservations)
     */
    public boolean reserveRecruit(String recruitName)
    {
        boolean ok = false;
        int remain;

        Integer count = recruitReservations.get(recruitName);
        if (count != null)
        {
            remain = count.intValue();
            recruitReservations.remove(recruitName);
        }
        else
        {
            LOGGER.log(Level.WARNING, player.getName()
                + " reserveRecruit creature " + recruitName
                + " not fround from hash, should have been created"
                + " during getReservedCount!");
            remain = getCreatureCount(recruitName);
        }

        if (remain > 0)
        {
            remain--;
            ok = true;
        }

        recruitReservations.put(recruitName, new Integer(remain));
        return ok;
    }

    /*
     * On first call (during a turn), cache remaining count from recruiter,
     * decrement on each further reserve for this creature.
     * This way we are independent of when the changes which are triggered by 
     * didRecruit influence the caretaker Stack. 
     * Returns how many creatures can still be recruited (=according
     * to caretakers stack plus reservations)
     */
    public int getReservedRemain(String recruitName)
    {
        int remain;

        Integer count = recruitReservations.get(recruitName);
        if (count == null)
        {
            remain = getCreatureCount(recruitName);
        }
        else
        {
            remain = count.intValue();
            recruitReservations.remove(recruitName);
        }

        // in case someone called getReservedRemain with bypassing the 
        // reset or reserve methods, to be sure doublecheck against the 
        // real remaining value.
        int realCount = getCreatureCount(recruitName);
        if (realCount < remain)
        {
            remain = realCount;
        }
        recruitReservations.put(recruitName, new Integer(remain));

        return remain;
    }

    /** Return a list of Creatures (ignore reservations). */
    public List<CreatureType> findEligibleRecruits(String markerId,
        String hexLabel)
    {
        return findEligibleRecruits(markerId, hexLabel, false);
    }

    /** Return a list of Creatures. Consider reservations if wanted */
    public List<CreatureType> findEligibleRecruits(String markerId,
        String hexLabel, boolean considerReservations)
    {
        List<CreatureType> recruits = new ArrayList<CreatureType>();

        LegionInfo info = getLegionInfo(markerId);
        if (info == null)
        {
            return recruits;
        }

        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        if (hex == null)
        {
            LOGGER.log(Level.WARNING,
                "null hex in Client.findEligibleRecruits()");
            LOGGER.log(Level.WARNING, "hexLabel is " + hexLabel);
            return recruits;
        }
        String terrain = hex.getTerrain();

        List<CreatureType> tempRecruits = TerrainRecruitLoader
            .getPossibleRecruits(terrain, hexLabel);
        List<CreatureType> recruiters = TerrainRecruitLoader
            .getPossibleRecruiters(terrain, hexLabel);

        Iterator<CreatureType> lit = tempRecruits.iterator();
        while (lit.hasNext())
        {
            CreatureType creature = lit.next();
            Iterator<CreatureType> liter = recruiters.iterator();
            while (liter.hasNext())
            {
                CreatureType lesser = liter.next();
                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser,
                    creature, terrain, hexLabel) <= info.numCreature(lesser))
                    && (recruits.indexOf(creature) == -1))
                {
                    recruits.add(creature);
                }
            }
        }

        // Make sure that the potential recruits are available.
        Iterator<CreatureType> it = recruits.iterator();
        while (it.hasNext())
        {
            CreatureType recruit = it.next();
            int remaining = getCreatureCount(recruit);

            if (remaining > 0 && considerReservations)
            {
                String recruitName = recruit.toString();
                remaining = getReservedRemain(recruitName);
            }
            if (remaining < 1)
            {
                it.remove();
            }
        }

        return recruits;
    }

    /** Return a list of creature name strings. */
    public List<String> findEligibleRecruiters(String markerId,
        String recruitName)
    {
        Set<CreatureType> recruiters;
        Creature recruit = (Creature)game.getVariant().getCreatureByName(
            recruitName);
        if (recruit == null)
        {
            return new ArrayList<String>();
        }

        LegionInfo info = getLegionInfo(markerId);
        String hexLabel = info.getHexLabel();
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        String terrain = hex.getTerrain();

        recruiters = new HashSet<CreatureType>(TerrainRecruitLoader
            .getPossibleRecruiters(terrain, hexLabel));
        Iterator<CreatureType> it = recruiters.iterator();
        while (it.hasNext())
        {
            CreatureType possibleRecruiter = it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain, hexLabel);
            if (needed < 1 || needed > info.numCreature(possibleRecruiter))
            {
                // Zap this possible recruiter.
                it.remove();
            }
        }

        List<String> strings = new ArrayList<String>();
        it = recruiters.iterator();
        while (it.hasNext())
        {
            CreatureType creature = it.next();
            strings.add(creature.getName());
        }
        return strings;
    }

    /** Return a set of hexLabels. */
    Set<String> getPossibleRecruitHexes()
    {
        Set<String> set = new HashSet<String>();

        Iterator<LegionInfo> it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = it.next();
            if (activePlayerName.equals(info.getPlayerName())
                && info.canRecruit())
            {
                set.add(info.getHexLabel());
            }
        }
        return set;
    }

    /** Return a set of hexLabels for all other unengaged legions of
     *  markerId's player that have summonables.
     * public for client-side AI -- do not call from server side. */
    public Set<String> findSummonableAngelHexes(String summonerId)
    {
        Set<String> set = new HashSet<String>();
        LegionInfo summonerInfo = getLegionInfo(summonerId);
        Player player = summonerInfo.getPlayer().getPlayer();
        Iterator<String> it = getLegionsByPlayer(player).iterator();
        while (it.hasNext())
        {
            String markerId = it.next();
            if (!markerId.equals(summonerId))
            {
                LegionInfo info = getLegionInfo(markerId);
                if (info.hasSummonable() && !(info.isEngaged()))
                {
                    set.add(info.getHexLabel());
                }
            }
        }
        return set;
    }

    public Movement getMovement()
    {
        return movement;
    }

    public Strike getStrike()
    {
        return strike;
    }

    /** Return a set of hexLabels. */
    Set<String> listTeleportMoves(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        MasterHex hex = MasterBoard.getHexByLabel(info.getHexLabel());
        return movement.listTeleportMoves(info, hex, movementRoll);
    }

    /** Return a set of hexLabels. */
    Set<String> listNormalMoves(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        MasterHex hex = MasterBoard.getHexByLabel(info.getHexLabel());
        return movement.listNormalMoves(info, hex, movementRoll);
    }

    Set<String> listPossibleEntrySides(String moverId, String hexLabel,
        boolean teleport)
    {
        return movement.listPossibleEntrySides(moverId, hexLabel, teleport);
    }

    int getCreatureCount(String creatureName)
    {
        return caretakerInfo.getCount(creatureName);
    }

    int getCreatureCount(CreatureType creature)
    {
        return caretakerInfo.getCount(creature);
    }

    int getCreatureDeadCount(String creatureName)
    {
        return caretakerInfo.getDeadCount(creatureName);
    }

    int getCreatureDeadCount(CreatureType creature)
    {
        return caretakerInfo.getDeadCount(creature);
    }

    int getCreatureMaxCount(String creatureName)
    {
        return caretakerInfo.getMaxCount(creatureName);
    }

    int getCreatureMaxCount(CreatureType creature)
    {
        return caretakerInfo.getMaxCount(creature);
    }

    /** Returns a list of markerIds. */
    public List<String> getLegionsByHex(String hexLabel)
    {
        List<String> markerIds = new ArrayList<String>();
        Iterator<Entry<String, LegionInfo>> it = legionInfo.entrySet()
            .iterator();
        while (it.hasNext())
        {
            Entry<String, LegionInfo> entry = it.next();
            LegionInfo info = entry.getValue();
            if (info != null && info.getHexLabel() != null && hexLabel != null
                && hexLabel.equals(info.getHexLabel()))
            {
                markerIds.add(info.getMarkerId());
            }
        }
        return markerIds;
    }

    /** 
     * Returns a list of markerIds.
     * 
     * TODO this should be replaced with a call to query all legions of the
     * PlayerState instance.
     */
    public List<String> getLegionsByPlayer(Player player)
    {
        List<String> markerIds = new ArrayList<String>();
        Iterator<Map.Entry<String, LegionInfo>> it = legionInfo.entrySet()
            .iterator();
        while (it.hasNext())
        {
            Entry<String, LegionInfo> entry = it.next();
            LegionInfo info = entry.getValue();
            if (player.equals(info.getPlayer().getPlayer()))
            {
                markerIds.add(info.getMarkerId());
            }
        }
        LOGGER.log(Level.FINER, "Found " + markerIds.size()
            + " legions for player " + player.getName());
        return markerIds;
    }

    Set<String> findUnmovedLegionHexes()
    {
        Set<String> set = new HashSet<String>();

        Iterator<LegionInfo> it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = it.next();
            if (!info.hasMoved()
                && getActivePlayerName().equals(info.getPlayerName()))
            {
                set.add(info.getHexLabel());
            }
        }
        return set;
    }

    /** Return a set of hexLabels for the active player's legions with
     *  7 or more creatures. */
    Set<String> findTallLegionHexes()
    {
        return findTallLegionHexes(7);
    }

    /** Return a set of hexLabels for the active player's legions with
     *  minHeight or more creatures. */
    Set<String> findTallLegionHexes(int minHeight)
    {
        Set<String> set = new HashSet<String>();

        Iterator<Entry<String, LegionInfo>> it = legionInfo.entrySet()
            .iterator();
        while (it.hasNext())
        {
            Entry<String, LegionInfo> entry = it.next();
            LegionInfo info = entry.getValue();
            if (info.getHeight() >= minHeight
                && activePlayerName.equals(info.getPlayerName()))
            {
                set.add(info.getHexLabel());
            }
        }
        return set;
    }

    /** Return a set of hexLabels for all hexes with engagements. */
    public Set<String> findEngagements()
    {
        Set<String> set = new HashSet<String>();
        Iterator<String> it = MasterBoard.getAllHexLabels().iterator();
        while (it.hasNext())
        {
            String hexLabel = it.next();
            List<String> markerIds = getLegionsByHex(hexLabel);
            if (markerIds.size() == 2)
            {
                String marker0 = markerIds.get(0);
                LegionInfo info0 = getLegionInfo(marker0);
                String pName0 = info0.getPlayerName();

                String marker1 = markerIds.get(1);
                LegionInfo info1 = getLegionInfo(marker1);
                String pName1 = info1.getPlayerName();

                if (!pName0.equals(pName1))
                {
                    set.add(hexLabel);
                }
            }
        }
        return set;
    }

    boolean isOccupied(String hexLabel)
    {
        return !getLegionsByHex(hexLabel).isEmpty();
    }

    boolean isEngagement(String hexLabel)
    {
        List<String> markerIds = getLegionsByHex(hexLabel);
        if (markerIds.size() == 2)
        {
            String marker0 = markerIds.get(0);
            LegionInfo info0 = getLegionInfo(marker0);
            String pName0 = info0.getPlayerName();

            String marker1 = markerIds.get(1);
            LegionInfo info1 = getLegionInfo(marker1);
            String pName1 = info1.getPlayerName();

            return !pName0.equals(pName1);
        }
        return false;
    }

    List<String> getEnemyLegions(Player player)
    {
        List<String> markerIds = new ArrayList<String>();
        Iterator<LegionInfo> it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = it.next();
            String markerId = info.getMarkerId();
            if (!player.equals(info.getPlayer().getPlayer()))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    List<String> getEnemyLegions(String hexLabel, PlayerState player)
    {
        List<String> markerIds = new ArrayList<String>();
        List<String> legions = getLegionsByHex(hexLabel);
        Iterator<String> it = legions.iterator();
        while (it.hasNext())
        {
            String markerId = it.next();
            if (!player.equals(getPlayerStateByMarkerId(markerId)))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    public String getFirstEnemyLegion(String hexLabel, PlayerState player)
    {
        List<String> markerIds = getEnemyLegions(hexLabel, player);
        if (markerIds.isEmpty())
        {
            return null;
        }
        return markerIds.get(0);
    }

    public int getNumEnemyLegions(String hexLabel, PlayerState player)
    {
        return getEnemyLegions(hexLabel, player).size();
    }

    public List<String> getFriendlyLegions(String pName)
    {
        List<String> markerIds = new ArrayList<String>();
        Iterator<LegionInfo> it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = it.next();
            String markerId = info.getMarkerId();
            if (pName.equals(info.getPlayerName()))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    public List<String> getFriendlyLegions(String hexLabel, PlayerState player)
    {
        List<String> markerIds = new ArrayList<String>();
        List<String> legions = getLegionsByHex(hexLabel);
        Iterator<String> it = legions.iterator();
        while (it.hasNext())
        {
            String markerId = it.next();
            if (player.equals(getPlayerByMarkerId(markerId)))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    public String getFirstFriendlyLegion(String hexLabel, PlayerState player)
    {
        List<String> markerIds = getFriendlyLegions(hexLabel, player);
        if (markerIds.isEmpty())
        {
            return null;
        }
        return markerIds.get(0);
    }

    public int getNumFriendlyLegions(String hexLabel, PlayerState player)
    {
        return getFriendlyLegions(hexLabel, player).size();
    }

    // Used by File=>Close and Window closing 
    public void setWhatToDoNextForClose()
    {
        if (startedByWebClient)
        {
            Start.setCurrentWhatToDoNext(Start.StartWebClient);
        }
        else if (remote)
        {
            // Remote clients get back to Network Client dialog
            Start.setCurrentWhatToDoNext(Start.NetClientDialog);
        }
        else
        {
            Start.setCurrentWhatToDoNext(Start.GetPlayersDialog);
        }
    }

    public void notifyServer()
    {
        clearUndoStack();
        if (!remote)
        {
            server.stopGame();
        }
        disposeClientOriginated();
    }

    boolean quitAlreadyTried = false;

    public void menuCloseBoard()
    {
        clearUndoStack();
        Start.setCurrentWhatToDoNext(Start.GetPlayersDialog);
        disposeClientOriginated();
    }

    public void menuQuitGame()
    {
        // Note that if this called from webclient, webclient has already 
        // beforehand called client to set webclient to null :)
        if (webClient != null)
        {
            webClient.dispose();
            webClient = null;
        }

        // as a fallback/safety: if the close/dispose chain does not work,
        // on 2nd attempt directly do System.exit() so that user can somehow
        // get rid of the game "cleanly"...
        if (quitAlreadyTried)
        {
            JOptionPane.showMessageDialog(getMapOrBoardFrame(),
                "Arggh!! Seems the standard Quit procedure does not work.\n"
                    + "Doing System.exit() now the hard way.",
                "Proper quitting failed!", JOptionPane.INFORMATION_MESSAGE);

            System.exit(1);
        }
        quitAlreadyTried = true;

        Start.setCurrentWhatToDoNext(Start.QuitAll);
        notifyServer();
    }

    void menuNewGame()
    {
        if (webClient != null)
        {
            webClient.dispose();
            webClient = null;
        }
        setWhatToDoNextForClose();
        notifyServer();
    }

    void menuLoadGame(String filename)
    {
        if (webClient != null)
        {
            webClient.dispose();
            webClient = null;
        }
        Start.setCurrentWhatToDoNext(Start.LoadGame, filename);
        notifyServer();
    }

    void menuSaveGame(String filename)
    {
        server.saveGame(filename);
    }

    void undoLastSplit()
    {
        if (!isUndoStackEmpty())
        {
            String splitoffId = (String)popUndoStack();
            undoSplit(splitoffId);
        }
    }

    // because of synchronization issues we need to
    // be able to pass an undo split request to the server even if it is not
    // yet in the client UndoStack
    public void undoSplit(String splitoffId)
    {
        server.undoSplit(splitoffId);
        getPlayerInfo().addMarkerAvailable(splitoffId);
        numSplitsThisTurn--;
        if (turnNumber == 1 && numSplitsThisTurn == 0)
        {
            // must split in first turn - Done not allowed now
            if (board != null)
            {
                board.disableDoneAction("Split required in first round");
            }
        }
        LOGGER.log(Level.FINEST, "called server.undoSplit");
    }

    void undoLastMove()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            server.undoMove(markerId);
            clearRecruitChits();
        }
    }

    public void undidSplit(String splitoffId, String survivorId, int turn)
    {
        LegionInfo info = getLegionInfo(survivorId);
        info.merge(splitoffId);
        removeLegion(splitoffId);
        // do the eventViewer stuff before the board, so we are sure to get
        // a repaint.
        if (eventViewer != null)
        {
            eventViewer.undoEvent(RevealEvent.eventSplit, survivorId,
                splitoffId, turn);
        }
        else
        {
            // fine. So this client does not even have eventViewer 
            // (probably then not even a masterBoard, i.e. AI)
        }

        if (board != null)
        {
            board.alignLegions(info.getHexLabel());
            board.highlightTallLegions();
        }
        if (isMyTurn() && this.phase == Constants.Phase.SPLIT
            && getOption(Options.autoSplit) && !isGameOver())
        {
            boolean done = ai.splitCallback(null, null);
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    void undoLastRecruit()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            server.undoRecruit(markerId);
        }
    }

    void undoRecruit(String markerId)
    {
        if (undoStack.contains(markerId))
        {
            undoStack.remove(markerId);
        }
        server.undoRecruit(markerId);
    }

    void undoAllSplits()
    {
        while (!isUndoStackEmpty())
        {
            undoLastSplit();
        }
    }

    void undoAllMoves()
    {
        while (!isUndoStackEmpty())
        {
            undoLastMove();
        }
    }

    void undoAllRecruits()
    {
        while (!isUndoStackEmpty())
        {
            undoLastRecruit();
        }
    }

    /**
     * Finishes the current phase.
     * 
     * Depending on the current phase this method dispatches to
     * the different done methods.
     * 
     * @see Client#doneWithSplits()
     * @see Client#doneWithMoves()
     * @see Client#doneWithEngagements()()
     * @see Client#doneWithRecruits()()
     */
    void doneWithPhase()
    {
        if (phase == Constants.Phase.SPLIT)
        {
            doneWithSplits();
        }
        else if (phase == Constants.Phase.MOVE)
        {
            doneWithMoves();
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            doneWithEngagements();
        }
        else if (phase == Constants.Phase.MUSTER)
        {
            doneWithRecruits();
        }
        else
        {
            throw new IllegalStateException("Client has unknown phase value");
        }
    }

    void doneWithSplits()
    {
        if (!isMyTurn())
        {
            return;
        }
        server.doneWithSplits();
        clearRecruitChits();
    }

    void doneWithMoves()
    {
        if (!isMyTurn())
        {
            return;
        }
        aiPause();
        clearRecruitChits();
        server.doneWithMoves();
    }

    void doneWithEngagements()
    {
        if (!isMyTurn())
        {
            return;
        }
        aiPause();
        server.doneWithEngagements();
    }

    void doneWithRecruits()
    {
        if (!isMyTurn())
        {
            return;
        }
        aiPause();
        server.doneWithRecruits();
    }

    public PlayerState getPlayerByMarkerId(String markerId)
    {
        assert markerId != null : "Parameter must not be null";

        String shortColor = markerId.substring(0, 2);
        return getActivePlayerStateUsingColor(shortColor);
    }

    public PlayerState getPlayerStateByMarkerId(String markerId)
    {
        assert markerId != null : "Parameter must not be null";

        String shortColor = markerId.substring(0, 2);
        return getActivePlayerStateUsingColor(shortColor);
    }

    private PlayerState getActivePlayerStateUsingColor(String shortColor)
    {
        assert this.playerInfo != null : "Client not yet initialized";
        assert shortColor != null : "Parameter must not be null";

        // Stage 1: See if the player who started with this color is alive.
        for (PlayerInfo info : playerInfo)
        {
            if (shortColor.equals(info.getShortColor()) && !info.isDead())
            {
                return info;
            }
        }

        // Stage 2: He's dead.  Find who killed him and see if he's alive.
        for (PlayerInfo info : playerInfo)
        {
            if (info.getPlayersElim().indexOf(shortColor) != -1)
            {
                // We have the killer.
                if (!info.isDead())
                {
                    return info;
                }
                else
                {
                    return getActivePlayerStateUsingColor(info.getShortColor());
                }
            }
        }
        return null;
    }

    String getColorByMarkerId(String markerId)
    {
        PlayerInfo player = (PlayerInfo)getPlayerByMarkerId(markerId);
        return player.getColor();
    }

    boolean isMyLegion(String markerId)
    {
        return (player.getName().equals(getPlayerByMarkerId(markerId)
            .getPlayer().getName()));
    }

    boolean isMyTurn()
    {
        return player.getName().equals(getActivePlayerName());
    }

    boolean isMyBattlePhase()
    {
        // check also for phase, because delayed callbacks could come
        // after our phase is over but activePlayerName not updated yet
        return playerAlive
            && player.getName().equals(getBattleActivePlayerName())
            && this.phase == Constants.Phase.FIGHT;
    }

    public int getMovementRoll()
    {
        return movementRoll;
    }

    int getMulligansLeft()
    {
        PlayerInfo info = getPlayerInfo(player.getName());
        return info.getMulligansLeft();
    }

    void doSplit(String parentId)
    {
        LOGGER.log(Level.FINEST, "Client.doSplit " + parentId);
        this.parentId = null;

        if (!isMyTurn())
        {
            LOGGER.log(Level.SEVERE, "Not my turn!");
            kickSplit();
            return;
        }
        // Can't split other players' legions.
        if (!isMyLegion(parentId))
        {
            LOGGER.log(Level.SEVERE, "Not my legion!");
            kickSplit();
            return;
        }
        Set<String> markersAvailable = getPlayerInfo().getMarkersAvailable();
        // Need a legion marker to split.
        if (markersAvailable.size() < 1)
        {
            showMessageDialog("No legion markers");
            kickSplit();
            return;
        }
        // Legion must be tall enough to split.
        if (getLegionHeight(parentId) < 4)
        {
            showMessageDialog("Legion is too short to split");
            kickSplit();
            return;
        }
        // Enforce only one split on turn 1.
        if (getTurnNumber() == 1 && numSplitsThisTurn > 0)
        {
            showMessageDialog("Can only split once on the first turn");
            kickSplit();
            return;
        }

        this.parentId = parentId;

        if (getOption(Options.autoPickMarker))
        {
            String childId = ai.pickMarker(markersAvailable, getShortColor());
            pickMarkerCallback(childId);
        }
        else
        {
            new PickMarker(board.getFrame(), player.getName(),
                markersAvailable, this);
        }
    }

    /** Called after a marker is picked, either first marker or split. */
    void pickMarkerCallback(String childId)
    {
        if (childId == null)
        {
            return;
        }
        if (parentId == null)
        {
            // Picking first marker.
            server.assignFirstMarker(childId);
            return;
        }
        String results = SplitLegion.splitLegion(this, parentId, childId);
        if (results != null)
        {
            doSplit(parentId, childId, results);
        }
    }

    /** Called by AI, and by pickMarkerCallback() */
    public void doSplit(String parentId, String childId, String results)
    {
        LOGGER.log(Level.FINEST, "Client.doSplit " + parentId + " " + childId
            + " " + results);
        server.doSplit(parentId, childId, results);
    }

    /** Callback from server after any successful split. */
    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight, List<String> splitoffs, int turn)
    {
        LOGGER.log(Level.FINEST, "Client.didSplit " + hexLabel + " "
            + parentId + " " + childId + " " + childHeight + " " + turn);

        LegionInfo parentInfo = getLegionInfo(parentId);
        LegionInfo childInfo = createLegionInfo(childId);
        parentInfo.split(childHeight, childId, turn);

        childInfo.setHexLabel(hexLabel);

        if (eventViewer != null)
        {
            eventViewer.newSplitEvent(parentId, parentInfo.getHeight(), null,
                childId, childInfo.getHeight());
        }

        if (board != null)
        {
            Marker marker = new Marker(3 * Scale.get(), childId, this);
            setMarker(childId, marker);
            board.alignLegions(hexLabel);
        }

        if (isMyLegion(childId))
        {
            clearRecruitChits();
            pushUndoStack(childId);
            getPlayerInfo().removeMarkerAvailable(childId);
        }

        numSplitsThisTurn++;
        if (turnNumber == 1 && board != null)
        {
            board.enableDoneAction();
        }

        if (board != null)
        {
            board.alignLegions(hexLabel);
            board.highlightTallLegions();
        }

        // check also for phase, because delayed callbacks could come
        // after our phase is over but activePlayerName not updated yet.
        if (isMyTurn() && this.phase == Constants.Phase.SPLIT
            && getOption(Options.autoSplit) && !isGameOver())
        {
            boolean done = ai.splitCallback(parentId, childId);
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    public void askPickColor(List<String> colorsLeft)
    {
        String color = null;
        if (getOption(Options.autoPickColor))
        {
            // Convert favorite colors from a comma-separated string to a list.
            String favorites = getStringOption(Options.favoriteColors);
            List<String> favoriteColors = null;
            if (favorites != null)
            {
                favoriteColors = Split.split(',', favorites);
            }
            else
            {
                favoriteColors = new ArrayList<String>();
            }
            color = ai.pickColor(colorsLeft, favoriteColors);
        }
        else
        {
            do
            {
                color = PickColor.pickColor(board.getFrame(),
                    player.getName(), colorsLeft, this);
            }
            while (color == null);
        }

        setColor(color);

        server.assignColor(color);
    }

    public void askPickFirstMarker()
    {
        Set<String> markersAvailable = getPlayerInfo().getMarkersAvailable();
        if (getOption(Options.autoPickMarker))
        {
            String markerId = ai.pickMarker(markersAvailable, getShortColor());
            pickMarkerCallback(markerId);
        }
        else
        {
            new PickMarker(board.getFrame(), player.getName(),
                markersAvailable, this);
        }
    }

    String getHexForLegion(String markerId)
    {
        return getLegionInfo(markerId).getHexLabel();
    }

    void setLookAndFeel(String lfName)
    {
        try
        {
            UIManager.setLookAndFeel(lfName);
            UIManager.LookAndFeelInfo[] lnfInfos = UIManager
                .getInstalledLookAndFeels();
            boolean exist = false;
            for (LookAndFeelInfo lnfInfo : lnfInfos)
            {
                exist = exist || lnfInfo.getClassName().equals(lfName);
            }
            if (!exist)
            {
                UIManager.installLookAndFeel(new UIManager.LookAndFeelInfo(
                    UIManager.getLookAndFeel().getName(), lfName));
            }
            updateEverything();
            LOGGER.log(Level.FINEST, "Switched to Look & Feel: " + lfName);
            setOption(Options.favoriteLookFeel, lfName);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Look & Feel " + lfName + " not usable",
                e);
        }
    }

    private void updateTreeAndPack(Window window)
    {
        if (window != null)
        {
            SwingUtilities.updateComponentTreeUI(window);
            window.pack();
        }
    }

    private void updateEverything()
    {
        if (board != null)
        {
            board.updateComponentTreeUI();
            board.pack();
        }
        updateTreeAndPack(statusScreen);
        updateTreeAndPack(caretakerDisplay);
        updateTreeAndPack(preferencesWindow);
        repaintAllWindows();
    }

    public void log(String message)
    {
        LOGGER.log(Level.INFO, message);
    }

    public static String getVersion()
    {
        byte[] bytes = new byte[8]; // length of an ISO date
        String version = "unknown";
        try
        {
            ClassLoader cl = Client.class.getClassLoader();
            InputStream is = cl.getResourceAsStream("version");
            if (is != null)
            {
                is.read(bytes);
                version = new String(bytes, 0, bytes.length);
            }
            else
            {
                LOGGER.log(Level.WARNING, "Version file not found");
                version = "UNKNOWN";
            }
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.WARNING, "Problem reading version file", ex);
        }
        return version;
    }

    public boolean testBattleMove(BattleChit chit, String hexLabel)
    {
        if (showBattleMoves(chit.getTag()).contains(hexLabel))
        {
            chit.setHexLabel(hexLabel);
            return true;
        }
        return false;
    }

    private void setType(final String aType)
    {
        LOGGER.log(Level.FINEST, "Called setType for " + aType);
        String type = new String(aType);
        if (type.endsWith(Constants.anyAI))
        {
            int whichAI = Dice.rollDie(Constants.numAITypes) - 1;
            type = Constants.aiArray[whichAI];
        }
        if (!type.startsWith(Constants.aiPackage))
        {
            type = Constants.aiPackage + type;
        }
        LOGGER.log(Level.FINEST, "final type: " + type);
        if (type.endsWith("AI"))
        {
            LOGGER.log(Level.FINEST, "new type is AI. current ai is "
                + ai.getClass().getName());
            if (!(ai.getClass().getName().equals(type)))
            {
                LOGGER.log(Level.FINEST, "need to change type");
                LOGGER.log(Level.INFO, "Changing client " + player.getName()
                    + " from " + ai.getClass().getName() + " to " + type);
                try
                {
                    // TODO these seem to be classes of either AI or Client, there
                    // should be a common ancestor
                    Class<?>[] classArray = new Class<?>[1];
                    classArray[0] = Class
                        .forName("net.sf.colossus.client.Client");
                    Object[] objArray = new Object[1];
                    objArray[0] = this;
                    ai = (AI)Class.forName(type).getDeclaredConstructor(
                        classArray).newInstance(objArray);
                }
                catch (Exception ex)
                {
                    LOGGER.log(Level.SEVERE, "Failed to change client "
                        + player.getName() + " from "
                        + ai.getClass().getName() + " to " + type, ex);
                }
            }
        }
    }

    /** Wait for aiDelay. */
    void aiPause()
    {
        if (delay < 0)
        {
            setupDelay();
        }

        try
        {
            Thread.sleep(delay);
        }
        catch (InterruptedException ex)
        {
            LOGGER.log(Level.SEVERE, "Client.aiPause() interrupted", ex);
        }
    }

    private void setupDelay()
    {
        delay = getIntOption(Options.aiDelay);
        if (!getOption(Options.autoPlay) || delay < Constants.MIN_AI_DELAY)
        {
            delay = Constants.MIN_AI_DELAY;
        }
        else if (delay > Constants.MAX_AI_DELAY)
        {
            delay = Constants.MAX_AI_DELAY;
        }
    }

    void setChosenDevice(GraphicsDevice chosen)
    {
        if (chosen != null)
        {
            secondaryParent = new JFrame(chosen.getDefaultConfiguration());
            disposeStatusScreen();
            updateStatusScreen();
            disposeCaretakerDisplay();
            boolean bval = getOption(Options.showCaretaker);
            showOrHideCaretaker(bval);
        }
    }

    private void focusMap()
    {
        if (battleBoard != null)
        {
            battleBoard.reqFocus();
        }
    }

    private void focusBoard()
    {
        if (board != null)
        {
            board.reqFocus();
        }
    }

    private void initPredictSplits(String pName, String rootMarkerId,
        List<String> creatureNames)
    {
        if (predictSplits == null)
        {
            predictSplits = new PredictSplits[numPlayers];
        }
        int playerNum = getPlayerNum(pName);
        predictSplits[playerNum] = new PredictSplits(rootMarkerId,
            creatureNames);
    }

    PredictSplits getPredictSplits(String pName)
    {
        try
        {
            return predictSplits[getPlayerNum(pName)];
        }
        catch (NullPointerException ex)
        {
            return null;
        }
    }

    class MarkerComparator implements Comparator<String>
    {
        public int compare(String s1, String s2)
        {
            String shortColor = "None"; // In case not initialized yet.
            if (color != null)
            {
                shortColor = getShortColor();
            }
            if (s1.startsWith(shortColor) && !s2.startsWith(shortColor))
            {
                return -1;
            }
            if (!s1.startsWith(shortColor) && s2.startsWith(shortColor))
            {
                return 1;
            }
            return s1.compareTo(s2);
        }
    }

    public Game getGame()
    {
        return game;
    }
}
