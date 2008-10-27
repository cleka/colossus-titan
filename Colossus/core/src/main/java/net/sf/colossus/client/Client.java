package net.sf.colossus.client;


import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.undo.UndoManager;

import net.sf.colossus.ai.AI;
import net.sf.colossus.ai.SimpleAI;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.GameServerSide;
import net.sf.colossus.server.IServer;
import net.sf.colossus.server.Start;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.ChildThreadManager;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.LogWindow;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.webcommon.InstanceTracker;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 *  Lives on the client side and handles all communication
 *  with the server.  It talks to the client classes locally, and to
 *  Server via the network protocol.  There is one client per player.
 *  
 *  TODO the logic for the battles could probably be separated from the
 *  rest of this code. At the moment the battle logic seems to bounce
 *  back and forth between BattleBoard (which is really a GUI class) and
 *  this class.
 *  
 *  TODO there are quite a few spots where the existence of GUI elements
 *  is checked, e.g. "board == null" or "getPreferredParent() == null".
 *  Having a GUI class whose GUI may not be initialized seems utterly
 *  wrong -- it probably relates to the todo above about splitting the
 *  game logic out. AIs without visible GUI should not use GUI classes.
 *  And if someone wants to watch AIs play it might be a better idea to
 *  create a notion of a passive observer of a game.
 *  
 *  TODO this class also has the functionality of a GameClientSide class,
 *  which should be separated and ideally moved up into the {@link Game}
 *  class. The whole {@link IOracle} interface is part of that. 
 *  One approach would be moving code from {@link GameServerSide}
 *  up into {@link Game} and then reuse it here in the matching methods,
 *  then inlining it into the calling code. Another one would be creating
 *  the GameClientSide for now and relocating code there.
 *  
 *  TODO there are a few places where an Iterator is used to remove all elements
 *  of a list -- an enhanced for loop with a Collection.clear() would probably
 *  look better and be more efficient (not that the latter would be significant
 *  in any of the cases)
 *  
 *  @version $Id$
 *  @author David Ripton
 *  @author Romain Dolbeau
 */

public final class Client implements IClient, IOracle
{
    private static final Logger LOGGER = Logger.getLogger(Client.class
        .getName());

    /** This will eventually be a network interface rather than a
     *  direct reference.  So don't share this reference. */
    private IServer server;
    private ChildThreadManager threadMgr;

    private WebClient webClient = null;
    private boolean startedByWebClient = false;

    public boolean failed = false;
    private boolean replayOngoing = false;
    private int replayLastTurn = -1;
    private int replayMaxTurn = 0;

    // TODO the naming of these classes is confusing, they should be clearly named
    // as dialogs
    private MasterBoard board;
    private StatusScreen statusScreen;
    private CreatureCollectionView caretakerDisplay;
    private MovementDie movementDie;
    private EngagementResults engagementResults;
    private AutoInspector autoInspector;
    private EventViewer eventViewer;
    private PreferencesWindow preferencesWindow;

    private PickCarry pickCarryDialog = null;


    /** hexLabel of MasterHex for current or last engagement. */
    private MasterHex battleSite;
    private BattleBoard battleBoard;

    private final List<BattleChit> battleChits = new ArrayList<BattleChit>();

    /** 
     * Stack of legion marker ID's, to allow multiple levels of undo for
     * splits, moves, and recruits.
     * (for battle actions, the Strings are not actually marker ID's, 
     *  it's battle hex ID's there instead).
     * 
     * TODO it would probably be good to have a full Command pattern here, similar
     * to Swing's {@link UndoManager} stuff. In the GUI client we could/should
     * probably just use that. A list of objects (which are mostly the string
     * identifiers of something) isn't that safe.
     */
    private final LinkedList<Object> undoStack = new LinkedList<Object>();

    // Information on the current moving legion.
    private LegionClientSide mover;

    // @TODO: Those should be moved to MasterBoard, too - attached to
    // the respective legion/marker.
    private final List<Chit> recruitedChits = new ArrayList<Chit>();
    private final List<Chit> possibleRecruitChits = new ArrayList<Chit>();

    // Per-client and per-player options.
    private final Options options;

    /** 
     * Player who owns this client.
     * 
     * TODO should be final but can't be until the constructor gets all the data
     * needed
     */
    private PlayerClientSide owningPlayer;
    private boolean playerAlive = true;

    /**
     * The game in progress.
     */
    private final Game game;

    /** 
     * Starting marker color of player who owns this client.
     * 
     * TODO most likely redundant with owningPlayer.getColor() 
     */
    private String color;

    /** Last movement roll for any player. */
    private int movementRoll = -1;

    /** the parent frame for secondary windows */
    private JFrame secondaryParent = null;

    private Legion parent;
    private int numSplitsThisTurn;

    // This ai is either the actual ai player for an AI player, but is also
    // used by human clients for the autoXXX actions.
    private AI ai;

    /**
     * This is used as a placeholder for activePlayer and battleActivePlayer since they
     * are sometimes accessed when they are not available.
     * 
     * TODO this is a hack. Those members should just not be accessed at times where they
     * are not available. It seems to happen during startup (the not yet set case) and in
     * some GUI parts after battles, when battleActivePlayer has been reset already.
     */
    private final PlayerClientSide noone;

    private int turnNumber = -1;
    private PlayerClientSide activePlayer;
    private Constants.Phase phase;

    private int battleTurnNumber = -1;
    private Player battleActivePlayer;
    private Constants.BattlePhase battlePhase;
    private Legion attacker;
    private Legion defender;

    /** If the game is over, then quitting does not require confirmation. */
    private boolean gameOver;

    /** One per player. */
    private PlayerClientSide[] players;

    private int numPlayers;

    private Movement movement;
    private BattleMovement battleMovement;
    private Strike strike;

    private boolean remote;
    private SocketClientThread sct;

    // For negotiation.  (And AI battle.)
    private Negotiate negotiate;
    private ReplyToProposal replyToProposal;

    /**
     * Constants modeling the party who closed this client.
     * 
     * TODO the CLOSED_BY_WEBCLIENT seems unused
     */
    private enum ClosedByConstant
    {
        NOT_CLOSED, CLOSED_BY_SERVER, CLOSED_BY_CLIENT, CLOSED_BY_WEBCLIENT
    }

    public ClosedByConstant closedBy = ClosedByConstant.NOT_CLOSED;

    // XXX temporary until things are synched
    private boolean tookMulligan;

    private int delay = -1;

    /** For battle AI. */
    private List<CritterMove> bestMoveOrder = null;
    private List<CritterMove> failedBattleMoves = null;

    private final Hashtable<CreatureType, Integer> recruitReservations = new Hashtable<CreatureType, Integer>();

    private LogWindow logWindow;
    private int viewMode;
    private int recruitChitMode;

    // Once we got dispose from server (or user initiated it himself), 
    // we'll ignore it if we we get it from server again 
    // - it's then up to the user to do some "disposing" action.
    private boolean gotDisposeAlready = false;

    private boolean disposeInProgress = false;

    /**
     * TODO since Client is currently still the equivalent of GameClientSide it should
     *      create the Game instance instead of getting it passed in. The problem is
     *      getting all the player names to begin with.
     *      
     * TODO try to make the Client class agnostic of the network or not question by
     *      having the SCT outside and behaving like a normal server -- that way it
     *      would be easier to run the local clients without the detour across the
     *      network and the serialization/deserialization of all objects
     */
    public Client(String host, int port, Game game, String playerName,
        boolean remote, boolean byWebClient, boolean noOptionsFile)
    {
        this(game, playerName, noOptionsFile);

        this.remote = remote;
        this.startedByWebClient = byWebClient;

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
                ResourceLoader.setDataServer(host, port + 1);
            }
            else
            {
                ResourceLoader.setDataServer(null, 0);
            }

            sct.start();

            TerrainRecruitLoader.setCaretaker(getGame().getCaretaker());
            CustomRecruitBase.addCaretakerClientSide(getGame().getCaretaker());
            failed = false;
        }
    }

    private Client(Game game, String playerName, boolean noOptionsFile)
    {
        assert playerName != null;

        this.game = game;

        // TODO this is currently not set properly straight away, it is fixed in
        // updatePlayerInfo(..) when the PlayerInfos are initialized. Should really
        // happen here, but doesn't yet since we don't have all players (not even as
        // names) yet
        this.owningPlayer = new PlayerClientSide(getGame(), playerName, 0);

        this.noone = new PlayerClientSide(getGame(), "", 0);
        this.activePlayer = noone;
        this.battleActivePlayer = noone;

        this.threadMgr = new ChildThreadManager("Client " + playerName);

        this.ai = new SimpleAI(this);

        this.movement = new Movement(this);
        this.battleMovement = new BattleMovement(this);
        this.strike = new Strike(this);

        ViableEntityManager.register(this, "Client " + playerName);
        InstanceTracker.register(this, "Client " + playerName);

        options = new Options(playerName, noOptionsFile);
        setupOptionListeners();
        // Need to load options early so they don't overwrite server options.
        loadOptions();
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
    void engage(MasterHex hex)
    {
        server.engage(hex);
    }

    public Legion getMyEngagedLegion()
    {
        if (isMyLegion(attacker))
        {
            return attacker;
        }
        else if (isMyLegion(defender))
        {
            return defender;
        }
        return null;
    }

    void concede()
    {
        concede(getMyEngagedLegion());
    }

    private void concede(Legion legion)
    {
        if (legion != null)
        {
            server.concede(legion);
        }
    }

    private void doNotConcede(Legion legion)
    {
        server.doNotConcede(legion);
    }

    /** Cease negotiations and fight a battle in land. */
    void fight(MasterHex hex)
    {
        server.fight(hex);
    }

    private List<String> tellEngagementResultsAttackerStartingContents = null;
    private List<String> tellEngagementResultsDefenderStartingContents = null;
    private List<Boolean> tellEngagementResultsAttackerLegionCertainities = null;
    private List<Boolean> tellEngagementResultsDefenderLegionCertainities = null;

    public void tellEngagement(MasterHex hex, Legion attacker, Legion defender)
    {
        this.battleSite = hex;
        this.attacker = attacker;
        this.defender = defender;
        if (eventViewer != null)
        {
            eventViewer.tellEngagement(attacker, defender, turnNumber);
        }

        // remember for end of battle.
        tellEngagementResultsAttackerStartingContents = getLegionImageNames(attacker);
        tellEngagementResultsDefenderStartingContents = getLegionImageNames(defender);
        // TODO: I have the feeling that getLegionCertainties()
        //   does not work here.
        //   I always seem to get either ALL true or ALL false.
        tellEngagementResultsAttackerLegionCertainities = getLegionCreatureCertainties(attacker);
        tellEngagementResultsDefenderLegionCertainities = getLegionCreatureCertainties(defender);

        highlightBattleSite();
    }

    private JFrame getPreferredParent()
    {
        if ((secondaryParent == null) && (board != null))
        {
            return board.getFrame();
        }
        return secondaryParent;
    }

    private void initShowEngagementResults()
    {
        JFrame parent = getPreferredParent();
        // no board at all, e.g. AI - nothing to do.
        if (parent == null)
        {
            return;
        }

        engagementResults = new EngagementResults(parent, this, options);
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
                autoInspector = new AutoInspector(parent, options, viewMode,
                    options.getOption(Options.dubiousAsBlanks));
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
        if (board != null && battleSite != null)
        {
            board.unselectAllHexes();
            board.selectHexByLabel(battleSite.getLabel());
        }
    }

    public void tellEngagementResults(Legion winner, String method,
        int points, int turns)
    {
        JFrame frame = getMapOrBoardFrame();
        if (frame == null)
        {
            return;
        }

        if (eventViewer != null)
        {
            eventViewer.tellEngagementResults(winner, method, turns);
        }

        if (engagementResults != null)
        {
            engagementResults.addData(winner, method, points, turns,
                tellEngagementResultsAttackerStartingContents,
                tellEngagementResultsDefenderStartingContents,
                tellEngagementResultsAttackerLegionCertainities,
                tellEngagementResultsDefenderLegionCertainities, isMyTurn());
        }
        battleSite = null;
        attacker = null;
        defender = null;
    }

    /* Create the event viewer, so that it can collect data from beginning on.
     * EventViewer shows itself depending on whether the option for it is set. 
     */
    private void initEventViewer()
    {
        if (eventViewer == null)
        {
            JFrame parent = getPreferredParent();
            eventViewer = new EventViewer(parent, options, this);
        }
    }

    private void initPreferencesWindow()
    {
        if (preferencesWindow == null)
        {
            preferencesWindow = new PreferencesWindow(options, this);
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
            Legion legion = getLegion(markerId);
            autoInspector.showLegion((LegionClientSide)legion);
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
    void doSummon(Legion summoner, Legion donor, String unit)
    {
        server.doSummon(summoner, donor, unit);
        if (board != null)
        {
            highlightEngagements();
            board.repaint();
        }
    }

    public void didSummon(Legion summoner, Legion donor, String summon)
    {
        if (eventViewer != null)
        {
            eventViewer.newCreatureRevealEvent(RevealEvent.eventSummon, donor
                .getMarkerId(), (donor).getHeight(), summon, summoner
                .getMarkerId(), (summoner).getHeight());
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

    /**
     * TODO since we are doing Swing nowadays it would probably be much better to replace
     * all this rescaling code with just using {@link AffineTransform} on the right 
     * {@link Graphics2D} instances.
     */
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
        if (isMyTurn() && options.getOption(Options.autoMasterMove)
            && !isGameOver() && !replayOngoing)
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

    /** public so that server can set autoPlay for AIs.
     *  
     * TODO This it totally confusing: this method is declared
     * to fulfill the IClient interface, but it is never actually
     * used, since the SocketClientThread directly deals with
     * the actual Options object itself...
     */
    public void setOption(String optname, String value)
    {
        options.setOption(optname, value);
        syncCheckboxes();
        options.saveOptions();
    }

    public int getViewMode()
    {
        return this.viewMode;
    }

    /**
     * Trigger side effects after changing an option value.
     * 
     *  TODO now that there are listeners, many of the other classes could listen to the
     *  options relevant to them instead of dispatching it all through the Client class.
     */
    private void setupOptionListeners()
    {
        options.addListener(Options.antialias, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                GUIHex.setAntialias(newValue);
                repaintAllWindows();
            }
        });
        options.addListener(Options.useOverlay, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                GUIHex.setOverlay(newValue);
                if (board != null)
                {
                    board.repaintAfterOverlayChanged();
                }
            }
        });
        options.addListener(Options.showRecruitChitsSubmenu,
            new IOptions.Listener()
            {
                @Override
                public void stringOptionChanged(String optname,
                    String oldValue, String newValue)
                {
                    recruitChitMode = options
                        .getNumberForRecruitChitSelection(newValue);
                }
            });
        options.addListener(Options.noBaseColor, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                CreatureType.setNoBaseColor(newValue);
                net.sf.colossus.util.ResourceLoader.purgeImageCache();
                repaintAllWindows();
            }
        });
        options.addListener(Options.useColoredBorders, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                BattleChit.setUseColoredBorders(newValue);
                repaintAllWindows();
            }
        });
        options.addListener(Options.showCaretaker, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                showOrHideCaretaker(newValue);
                syncCheckboxes();
            }
        });
        options.addListener(Options.showLogWindow, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                showOrHideLogWindow(newValue);
                syncCheckboxes();
            }
        });
        options.addListener(Options.showStatusScreen, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                updateStatusScreen();
            }
        });
        options.addListener(Options.showAutoInspector, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                showOrHideAutoInspector(newValue);
                syncCheckboxes();
            }
        });
        options.addListener(Options.showEventViewer, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                // if null: no board (not yet, or not at all) => no eventviewer
                if (eventViewer != null)
                {
                    // Eventviewer takes care of showing/hiding itself
                    eventViewer.setVisibleMaybe();
                }
                syncCheckboxes();
            }
        });
        options.addListener(Options.viewMode, new IOptions.Listener()
        {
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                viewMode = options.getNumberForViewMode(newValue);
            }
        });
        options.addListener(Options.dubiousAsBlanks, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                if (autoInspector != null)
                {
                    autoInspector.setDubiousAsBlanks(newValue);
                }
            }
        });
        options.addListener(Options.showEngagementResults,
            new IOptions.Listener()
            {
                @Override
                public void booleanOptionChanged(String optname,
                    boolean oldValue, boolean newValue)
                {
                    // null if there is no board, e.g. AI
                    if (engagementResults != null)
                    {
                        // maybeShow decides by itself based on the current value
                        // of the option whether to hide or show. 
                        engagementResults.maybeShow();
                    }
                }
            });
        options.addListener(Options.favoriteLookFeel, new IOptions.Listener()
        {
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                setLookAndFeel(newValue);
            }
        });
        options.addListener(Options.scale, new IOptions.Listener()
        {
            // TODO check if we could use the intOptionChanged callback here
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                int scale = Integer.parseInt(newValue);
                if (scale > 0)
                {
                    Scale.set(scale);
                    rescaleAllWindows();
                }
            }
        });
        options.addListener(Options.playerType, new IOptions.Listener()
        {
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                setType(newValue);
            }
        });
    }

    /** Load player options from a file. */
    private void loadOptions()
    {
        options.loadOptions();
        syncCheckboxes();
    }

    /** 
     * Ensure that Player menu checkboxes reflect the correct state.
     * 
     * TODO let the checkboxes have their own listeners instead. Or even
     * better: use a binding framework.
     */
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
            boolean value = options.getOption(name);
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
        for (PlayerClientSide info : players)
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
        if (players == null)
        {
            // first time we get the player infos, store them locally and set our
            // own, too -- which has been a fake until now
            players = new PlayerClientSide[numPlayers];
            for (int i = 0; i < numPlayers; i++)
            {
                List<String> data = Split.split(":", infoStrings.get(i));
                String playerName = data.get(1);
                PlayerClientSide info = new PlayerClientSide(getGame(),
                    playerName, i);
                players[i] = info;
                if (playerName.equals(this.owningPlayer.getName()))
                {
                    this.owningPlayer = info;
                }
            }
        }
        for (int i = 0; i < numPlayers; i++)
        {
            players[i].update(infoStrings.get(i));
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
        if (options.getOption(Options.showStatusScreen))
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
                        this, options, this);
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

    // TODO fix this mess with lots of different methods for retrieving Player[Info]s
    PlayerClientSide getPlayer(int playerNum)
    {
        return players[playerNum];
    }

    public PlayerClientSide getPlayerInfo(String playerName)
    {
        for (PlayerClientSide info : players)
        {
            if (info.getName().equals(playerName))
            {
                return info;
            }
        }
        throw new IllegalArgumentException("No player info found for player '"
            + playerName + "'");
    }

    public PlayerClientSide getOwningPlayer()
    {
        return owningPlayer;
    }

    public List<PlayerClientSide> getPlayers()
    {
        return Collections.unmodifiableList(Arrays.asList(players));
    }

    /** Return the average point value of all legions in the game. */
    public int getAverageLegionPointValue()
    {
        int totalValue = 0;
        int totalLegions = 0;

        for (Player player : players)
        {
            totalLegions += player.getLegions().size();
            totalValue += player.getTotalPointValue();
        }
        return (int)(Math.round((double)totalValue / totalLegions));
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public void updateCreatureCount(CreatureType type, int count, int deadCount)
    {
        getGame().getCaretaker().setAvailableCount(type, count);
        getGame().getCaretaker().setDeadCount(type, deadCount);
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

    private void disposeBattleBoard()
    {
        if (battleBoard != null)
        {
            battleBoard.dispose();
            battleBoard = null;
        }
    }

    private void disposePickCarryDialog()
    {
        if (pickCarryDialog != null)
        {   
            if (battleBoard != null)
            {
                battleBoard.unselectAllHexes();
            }
            pickCarryDialog.dispose();
            pickCarryDialog = null;
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
        closedBy = ClosedByConstant.CLOSED_BY_SERVER;
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

    // Used by MasterBoard and by BattleBoard
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
        closedBy = ClosedByConstant.CLOSED_BY_CLIENT;

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
                    + owningPlayer.getName() + ": got Exception!!!"
                    + e.toString(), e);
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
        String pType = options.getStringOption(Options.playerType);
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
        else if (closedBy == ClosedByConstant.CLOSED_BY_SERVER)
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

        sct = null;
        server = null;

        threadMgr.cleanup();
        threadMgr = null;

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
        disposeBattleBoard();
        disposeMasterBoard();

        // Options must be saved after all satellites are disposed,
        // because they store their location and state (enabled or not, e.g.)
        // in disposal.
        options.saveOptions();
        movement.dispose();

        this.movement = null;
        this.battleMovement = null;
        this.strike = null;
        this.secondaryParent = null;
        this.players = null;

        net.sf.colossus.server.CustomRecruitBase.reset();
    }

    /** Called from BattleBoard to leave carry mode. */
    public void leaveCarryMode()
    {
        disposePickCarryDialog();
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
        for (BattleChit chit : getActiveBattleChits())
        {
            if (chit.getCurrentHexLabel().startsWith("X"))
            {
                return true;
            }
        }
        return false;
    }

    public List<BattleChit> getActiveBattleChits()
    {
        return CollectionHelper.selectAsList(battleChits,
            new Predicate<BattleChit>()
            {
                public boolean matches(BattleChit chit)
                {
                    return getBattleActivePlayer().equals(
                        getPlayerByTag(chit.getTag()));
                }
            });
    }

    List<BattleChit> getInactiveBattleChits()
    {
        return CollectionHelper.selectAsList(battleChits,
            new Predicate<BattleChit>()
            {
                public boolean matches(BattleChit chit)
                {
                    return !getBattleActivePlayer().equals(
                        getPlayerByTag(chit.getTag()));
                }
            });
    }

    private void markOffboardCreaturesDead()
    {
        for (BattleChit chit : getActiveBattleChits())
        {
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
        if (isMyBattlePhase() && options.getOption(Options.autoForcedStrike))
        {
            return strike.makeForcedStrikes(options
                .getOption(Options.autoRangeSingle));
        }
        return false;
    }

    /** Handle both forced strikes and AI strikes. */
    void doAutoStrikes()
    {
        if (isMyBattlePhase())
        {
            if (options.getOption(Options.autoPlay))
            {
                aiPause();
                boolean struck = makeForcedStrikes();
                if (!struck)
                {
                    struck = ai.strike(getBattleActiveLegion());
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

    /**
     * Get this legion's info or create if necessary.
     * 
     * TODO move legion creation into a factory on {@link Player}
     */
    public LegionClientSide getLegion(String markerId)
    {
        PlayerClientSide player = getPlayerByMarkerId(markerId);
        LegionClientSide legion = player.getLegionByMarkerId(markerId);
        // Added this logging only for the purpose that one gets a clue
        // when during the game this happened - the assertion appears only
        // on stderr. Now it's also in the log, so one sees what was logged
        // just before and after it.
        if (legion == null)
        {
            LOGGER.log(Level.SEVERE, "No legion with markerId '" + markerId
                + "'" + " (for player " + player + "), turn = " + turnNumber
                + " in client " + getOwningPlayer());
        }
        assert legion != null : "No legion with markerId '" + markerId + "'"
            + " (for player " + player + "), turn = " + turnNumber
            + " in client " + getOwningPlayer();
        return legion;
    }

    public boolean legionExists(String markerId)
    {
        Player player = getPlayerByMarkerId(markerId);
        return player.hasLegion(markerId);
    }

    /** Add the marker to the end of the list and to the LegionInfo.
     If it's already in the list, remove the earlier entry. */
    void setMarker(Legion legion, Marker marker)
    {
        if (board != null)
        {
            board.markerToTop(marker);
        }
        ((LegionClientSide)legion).setMarker(marker);
    }

    /** Remove this eliminated legion, and clean up related stuff. */
    public void removeLegion(Legion legion)
    {
        if (board != null)
        {
            board.removeMarkerForLegion(legion);
        }

        // TODO Do for all players
        if (isMyLegion(legion))
        {
            getOwningPlayer().addMarkerAvailable(legion.getMarkerId());
        }

        // XXX Not perfect -- Need to track recruitChits by legion.
        removeRecruitChit(legion.getCurrentHex());

        legion.getPlayer().removeLegion(legion);
        if (board != null)
        {
            if (!replayOngoing)
            {
                board.alignLegions(legion.getCurrentHex());
            }
        }
    }

    int getLegionHeight(String markerId)
    {
        Legion legionInfo = getLegion(markerId);
        if (legionInfo == null)
        {
            return 0; //no legion, no height
        }
        return legionInfo.getHeight();
    }

    /** Needed when loading a game outside split phase. */
    public void setLegionStatus(Legion legion, boolean moved,
        boolean teleported, int entrySide, String lastRecruit)
    {
        legion.setMoved(moved);
        legion.setTeleported(teleported);
        ((LegionClientSide)legion).setEntrySide(entrySide);
        ((LegionClientSide)legion).setLastRecruit(lastRecruit);
    }

    /** Return the full basename for a titan in legion,
     *  first finding that legion's player, player color, and titan size.
     */
    String getTitanBasename(Legion legion)
    {
        return ((LegionClientSide)legion).getTitanBasename();
    }

    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    // public for IOracle
    public List<String> getLegionImageNames(Legion legion)
    {
        LegionClientSide info = (LegionClientSide)legion;
        if (info != null)
        {
            return info.getImageNames();
        }
        return new ArrayList<String>();
    }

    /** Return a list of Booleans */
    // public for IOracle
    public List<Boolean> getLegionCreatureCertainties(Legion legion)
    {
        LegionClientSide info = (LegionClientSide)legion;
        if (info != null)
        {
            return info.getCertainties();
        }
        else
        {
            // TODO: is this the right thing?
            List<Boolean> l = new ArrayList<Boolean>(10); // just longer then max
            for (int idx = 0; idx < 10; idx++)
            {
                l.add(Boolean.valueOf(true)); // all true
            }
            return l;
        }
    }

    /** Add a new creature to this legion. */
    public void addCreature(Legion legion, String name, String reason)
    {
        ((LegionClientSide)legion).addCreature(name);
        if (board != null && !replayOngoing)
        {
            GUIMasterHex hex = board.getGUIHexByMasterHex(legion
                .getCurrentHex());
            hex.repaint();
        }

        if (eventViewer != null)
        {
            eventViewer.addCreature(legion.getMarkerId(), name, reason);
        }
    }

    public void removeCreature(Legion legion, String name, String reason)
    {
        if (legion == null || name == null)
        {
            return;
        }
        if (eventViewer != null)
        {
            if (reason.equals(Constants.reasonUndidReinforce))
            {
                LOGGER.warning("removeCreature should not be called for undidReinforce!");
            }
            else if (reason.equals(Constants.reasonUndidRecruit))
            {
                // normal undidRecruit does not use this, but during loading a game
                // they appear as add- and removeCreature calls.
                LOGGER.info("removeCreature called for undidRecruit - ignored");
            }
            else
            {
                eventViewer.removeCreature(legion.getMarkerId(), name);
            }
        }

        int height = legion.getHeight();
        ((LegionClientSide)legion).removeCreature(name);
        if (height <= 1)
        {
            // do not remove this, sever will give explicit order to remove it
            // removeLegion(markerId);
        }
        if (height <= 1 && getTurnNumber() == -1)
        {
            // hack to remove legions correctly during load
            removeLegion(legion);
        }
        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByMasterHex(legion
                .getCurrentHex());
            hex.repaint();
        }
    }

    /** Reveal creatures in this legion, some of which already may be known. 
     *  - this "reveal" is related to data coming from server being  
     *  revealed to the split prediction 
     * */

    public void revealCreatures(Legion legion, final List<String> names,
        String reason)
    {
        if (eventViewer != null)
        {
            eventViewer.revealCreatures(legion, names, reason);
        }

        ((LegionClientSide)legion).revealCreatures(names);
    }

    /* pass revealed info to EventViewer and SplitPrediction, and
     * additionally remember the images list for later, the engagement report
     */
    public void revealEngagedCreatures(Legion legion,
        final List<String> names, boolean isAttacker, String reason)
    {
        revealCreatures(legion, names, reason);
        // in engagement we need to update the remembered list, too.
        if (isAttacker)
        {
            tellEngagementResultsAttackerStartingContents = getLegionImageNames(legion);
            // towi: should return a list of trues, right?
            // TODO if comment above is right: add assertion
            tellEngagementResultsAttackerLegionCertainities = getLegionCreatureCertainties(legion);
        }
        else
        {
            tellEngagementResultsDefenderStartingContents = getLegionImageNames(legion);
            // towi: should return a list of trues, right?
            // TODO if comment above is right: add assertion
            tellEngagementResultsDefenderLegionCertainities = getLegionCreatureCertainties(legion);
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

    List<BattleChit> getBattleChits(final String hexLabel)
    {
        return CollectionHelper.selectAsList(battleChits,
            new Predicate<BattleChit>()
            {
                public boolean matches(BattleChit chit)
                {
                    return hexLabel.equals(chit.getCurrentHexLabel());
                }
            });
    }

    // TODO make typesafe
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
        for (BattleChit chit : battleChits)
        {
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
                    Legion legion = defender;
                    ((LegionClientSide)legion).removeCreature(name);
                    if (eventViewer != null)
                    {
                        eventViewer.defenderSetCreatureDead(name, legion
                            .getHeight());
                    }
                }
                else
                {
                    Legion info = attacker;
                    ((LegionClientSide)info).removeCreature(name);
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
                imageName = getTitanBasename(defender);
            }
            else
            {
                imageName = getTitanBasename(attacker);
            }
        }
        String colorName;
        if (inverted)
        {
            Player player = defender.getPlayer();
            colorName = player.getColor();
        }
        else
        {
            Player player = attacker.getPlayer();
            colorName = player.getColor();
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

    void addRecruitedChit(String imageName, MasterHex masterHex)
    {
        int scale = 2 * Scale.get();
        GUIMasterHex hex = board.getGUIHexByMasterHex(masterHex);
        Chit chit = new Chit(scale, imageName);
        Point startingPoint = hex.getOffCenter();
        Point point = new Point(startingPoint);
        point.x -= scale / 2;
        point.y -= scale / 2;
        chit.setLocation(point);
        recruitedChits.add(chit);
    }

    // one chit, one hex
    void addPossibleRecruitChit(String imageName, MasterHex masterHex)
    {
        int scale = 2 * Scale.get();
        GUIMasterHex hex = board.getGUIHexByMasterHex(masterHex);
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
        MasterHex masterHex)
    {
        int size = imageNameList.size();
        int num = size;
        for (CreatureType creatureType : imageNameList)
        {
            String imageName = creatureType.getName();
            int scale = 2 * Scale.get();
            GUIMasterHex hex = board.getGUIHexByMasterHex(masterHex);
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
    public void addPossibleRecruitChits(LegionClientSide legion,
        Set<MasterHex> hexes)
    {
        clearPossibleRecruitChits();

        if (recruitChitMode == Options.showRecruitChitsNumNone)
        {
            return;
        }

        // set is a set of possible target hexes
        List<CreatureType> oneElemList = new ArrayList<CreatureType>();

        for (MasterHex hex : hexes)
        {
            List<CreatureType> recruits = findEligibleRecruits(legion, hex);

            if (recruits != null && recruits.size() > 0)
            {
                switch (recruitChitMode)
                {
                    case Options.showRecruitChitsNumAll:
                        break;

                    case Options.showRecruitChitsNumRecruitHint:
                        oneElemList.clear();
                        CreatureType hint = chooseBestPotentialRecruit(legion,
                            hex, recruits);
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
                addPossibleRecruitChits(recruits, hex);
            }
        }
    }

    CreatureType chooseBestPotentialRecruit(LegionClientSide legion,
        MasterHex hex, List<CreatureType> recruits)
    {
        CreatureType recruit = ai.getVariantRecruitHint(legion, hex, recruits);
        return recruit;
    }

    // TODO is this code for the MasterBoard class?
    void removeRecruitChit(MasterHex masterHex)
    {
        Iterator<Chit> it = recruitedChits.iterator();
        while (it.hasNext())
        {
            Chit chit = it.next();
            // TODO the next line can cause an NPE when the user closes the client app
            GUIMasterHex hex = board.getGUIHexByMasterHex(masterHex);
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
            GUIMasterHex hex = board.getGUIHexByMasterHex(masterHex);
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
        // TODO Only repaint needed hexes.
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
        return undoStack.removeFirst();
    }

    private void pushUndoStack(Object object)
    {
        undoStack.addFirst(object);
    }

    private boolean isUndoStackEmpty()
    {
        return undoStack.isEmpty();
    }

    Legion getMover()
    {
        return mover;
    }

    void setMover(LegionClientSide legion)
    {
        this.mover = legion;
    }

    MasterBoard getBoard()
    {
        return board;
    }

    private static String propNameForceViewBoard = "net.sf.colossus.forceViewBoard";

    public void tellReplay(boolean val, int maxTurn)
    {
        replayOngoing = val;
        if (replayOngoing)
        {
            replayMaxTurn = maxTurn;
            if (board != null)
            {
                board.setReplayMode(0, replayMaxTurn);
            }
        }
        else
        {
            if (board != null)
            {
                board.recreateMarkers();
            }
        }
    }

    public void replayTurnChange(int nowTurn)
    {
        if (board != null)
        {
            board.setReplayMode(nowTurn, replayMaxTurn);
            if (nowTurn != replayLastTurn)
            {
                replayLastTurn = nowTurn;
            }
        }
    }

    public void confirmWhenCaughtUp()
    {
        sct.clientConfirmedCatchup();
    }

    public void initBoard()
    {
        LOGGER.finest(owningPlayer.getName() + " Client.initBoard()");
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
            options.setOption(Options.showEventViewer, "true");
            options.setOption(Options.showStatusScreen, "true");
        }

        if (!options.getOption(Options.autoPlay)
            || (forceViewBoard && (owningPlayer.getName().endsWith("1")
                || options.getStringOption(Options.playerType).endsWith(
                    "Human") || options.getStringOption(Options.playerType)
                .endsWith("Network"))))
        {
            ensureEdtSetupClientGUI();
        }

        if (startedByWebClient)
        {
            if (webClient != null)
            {
                webClient.notifyComingUp();
            }
        }
    }

    public void ensureEdtSetupClientGUI()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            setupClientGUI();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        setupClientGUI();
                    }
                });
            }
            catch (InvocationTargetException e)
            {
                LOGGER.log(Level.SEVERE,
                    "Failed to run setupClientGUI with invokeAndWait(): ", e);
            }
            catch (InterruptedException e2)
            {
                LOGGER.log(Level.SEVERE,
                    "Failed to run setupClientGUI with invokeAndWait(): ", e2);
            }

        }
    }

    public void setupClientGUI()
    {
        disposeEventViewer();
        disposePreferencesWindow();
        disposeEngagementResults();
        disposeInspector();
        disposeCaretakerDisplay();
        disposeLogWindow();
        disposeMasterBoard();

        int scale = options.getIntOption(Options.scale);
        if (scale == -1)
        {
            scale = 15;
            options.setOption(Options.scale, scale);
            options.saveOptions();
        }
        Scale.set(scale);

        board = new MasterBoard(this);
        initEventViewer();
        initShowEngagementResults();
        initPreferencesWindow();
        showOrHideAutoInspector(options.getOption(Options.showAutoInspector));
        showOrHideLogWindow(options.getOption(Options.showLogWindow));
        showOrHideCaretaker(options.getOption(Options.showCaretaker));

        focusBoard();
    }

    public void setPlayerName(String playerName)
    {
        this.owningPlayer.setName(playerName);

        InstanceTracker.setId(this, "Client " + playerName);
        InstanceTracker.setId(ai, "AI: " + playerName);

        if (sct != null)
        {
            sct.fixName(playerName);
        }
    }

    public void createSummonAngel(Legion legion)
    {
        String typeColonDonor;
        if (options.getOption(Options.autoSummonAngels))
        {
            typeColonDonor = ai.summonAngel(legion);
        }
        else
        {
            typeColonDonor = SummonAngel.summonAngel(this, legion);
        }
        if (typeColonDonor != null)
        {
            List<String> parts = Split.split(':', typeColonDonor);
            String unit = parts.get(0);
            String donor = parts.get(1);
            doSummon(legion, getLegion(donor), unit);
        }
        else
        {
            // necessary to keep the game moving
            doSummon(null, null, null);
        }
    }

    /**
     * recruits is the list of acquirables that can be chosen from
     * for a certain point value reached. E.g. for getting 180 points,
     * going from 380 + 180 = 560,
     * game would first call this for 400: recruits = [Angel]
     * and then call it once more for 500: recruits = [Angel, Archangel]
     */
    public void askAcquireAngel(Legion legion, List<String> recruits)
    {
        if (options.getOption(Options.autoAcquireAngels))
        {
            acquireAngelCallback(legion, ai.acquireAngel(legion, recruits));
        }
        else
        {
            board.deiconify();
            new AcquireAngel(board.getFrame(), this, legion, recruits);
        }
    }

    void acquireAngelCallback(Legion legion, String angelType)
    {
        server.acquireAngel(legion, angelType);
    }

    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    private boolean chooseWhetherToTeleport(MasterHex hex)
    {
        if (options.getOption(Options.autoMasterMove))
        {
            return false;
        }
        // No point in teleporting if entry side is moot.
        if (!isOccupied(hex))
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
        if (options.getOption(Options.autoPlay))
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
        if (battleBoard != null)
        {
            battleBoard.highlightCrittersWithTargets();
        }
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
        boolean isDummyFrame = false;
        JFrame f = getMapOrBoardFrame();
        // I do not use null or a simple frame, because then the System.exit(0)
        // does not exit by itself (due to some bug in Swing/AWT).
        if (f == null)
        {
            f = new KFrame("Dummyframe for Client error message dialog");
            isDummyFrame = true;
        }
        JOptionPane.showMessageDialog(f, reason, title,
            JOptionPane.ERROR_MESSAGE);
        if (isDummyFrame)
        {
            f.dispose();
            f = null;
        }
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
        if (options.getOption(Options.autoPlay))
        {
            boolean isAI = getOwningPlayer().isAI();
            if ((message.equals("Draw") || message.endsWith(" wins")) && !isAI
                && !options.getOption(Options.autoQuit))
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
    // TODO parameters should be PlayerState
    public void tellPlayerElim(Player deadPlayer, Player slayer)
    {
        assert deadPlayer != null;
        LOGGER.log(Level.FINEST, this.owningPlayer.getName()
            + " tellPlayerElim(" + deadPlayer + ", " + slayer + ")");

        // TODO Merge these
        // TODO should this be rather calling Player.die()?
        deadPlayer.setDead(true);
        ((PlayerClientSide)deadPlayer).removeAllLegions();
        // otherwise called too early, e.g. someone quitted
        // already during game start...

        if (this.owningPlayer.equals(deadPlayer))
        {
            playerAlive = false;
        }
    }

    public void tellGameOver(String message)
    {
        LOGGER.info("Client " + getOwningPlayer()
            + " received from server game over message: " + message);
        gameOver = true;
        if (webClient != null)
        {
            webClient.tellGameEnds();
        }

        if (board != null)
        {
            if (statusScreen != null)
            {
                statusScreen.repaint();
            }
            defaultCursor();
            board.setGameOverState(message);
            showMessageDialog(message);
        }
    }

    boolean isGameOver()
    {
        return gameOver;
    }

    void doFight(MasterHex hex)
    {
        if (!isMyTurn())
        {
            return;
        }
        engage(hex);
    }

    public void askConcede(Legion ally, Legion enemy)
    {
        if (options.getOption(Options.autoConcede))
        {
            answerConcede(ally, ai.concede(ally, enemy));
        }
        else
        {
            Concede.concede(this, board.getFrame(), ally, enemy);
        }
    }

    public void askFlee(Legion ally, Legion enemy)
    {
        if (options.getOption(Options.autoFlee))
        {
            answerFlee(ally, ai.flee(ally, enemy));
        }
        else
        {
            Concede.flee(this, board.getFrame(), ally, enemy);
        }
    }

    void answerFlee(Legion ally, boolean answer)
    {
        if (answer)
        {
            server.flee(ally);
        }
        else
        {
            server.doNotFlee(ally);
        }
    }

    void answerConcede(Legion legion, boolean answer)
    {
        if (answer)
        {
            concede(legion);
        }
        else
        {
            doNotConcede(legion);
        }
    }

    public void askNegotiate(Legion attacker, Legion defender)
    {
        this.attacker = attacker;
        this.defender = defender;

        if (options.getOption(Options.autoNegotiate))
        {
            // XXX AI players just fight for now.
            Proposal proposal = new Proposal(attacker.getMarkerId(), defender
                .getMarkerId(), true, false, null, null);
            makeProposal(proposal);
        }
        else
        {
            negotiate = new Negotiate(this, attacker, defender);
        }
    }

    /** Called from both Negotiate and ReplyToProposal. */
    void negotiateCallback(Proposal proposal, boolean respawn)
    {
        if (proposal != null && proposal.isFight())
        {
            fight(attacker.getCurrentHex());
            return;
        }
        else if (proposal != null)
        {
            makeProposal(proposal);
        }

        if (respawn)
        {
            if (negotiate != null)
            {
                negotiate.dispose();
            }
            negotiate = new Negotiate(this, attacker, defender);
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
        if (replyToProposal != null)
        {
            replyToProposal.dispose();
        }
        replyToProposal = new ReplyToProposal(this, proposal);
        
    }

    public BattleHex getBattleHex(BattleChit chit)
    {
        return HexMap.getHexByLabel(getBattleSite().getTerrain(), chit
            .getCurrentHexLabel());
    }

    BattleHex getStartingBattleHex(BattleChit chit)
    {
        return HexMap.getHexByLabel(getBattleSite().getTerrain(), chit
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

        disposePickCarryDialog();
                
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
        LOGGER.log(Level.WARNING, owningPlayer.getName() + " got nak for "
            + reason + " " + errmsg);
        recoverFromNak(reason, errmsg);
    }

    private void recoverFromNak(String reason, String errmsg)
    {
        LOGGER.log(Level.FINEST, owningPlayer.getName() + " recoverFromNak "
            + reason + " " + errmsg);
        if (reason == null)
        {
            LOGGER.log(Level.SEVERE, "recoverFromNak with null reason!");
        }
        else if (reason.equals(Constants.doSplit))
        {
            showMessageDialog(errmsg);

            // if AI just got NAK for split, there's no point for
            // kickSplit again. Instead, let's just be DoneWithSplits.
            if (isMyTurn() && options.getOption(Options.autoSplit)
                && !isGameOver())
            {
                // XXX This may cause advancePhance illegally messages,
                // if e.g. SimpleAI fires two splits, both gets rejected,
                // and it responds with dineWithSplits two times.
                // But this whole situation should normally never happen, did
                // happen now only because of server/client data regarding
                // available markers was out of sync and then as reaction
                // to nak for didSplit do kickSplit() did just end in an
                // endless loop. Now perhaps 2 error messages, but no hang.
                doneWithSplits();
            }
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
            LOGGER.log(Level.WARNING, owningPlayer.getName()
                + " unexpected nak " + reason + " " + errmsg);
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
            && options.getOption(Options.autoCarrySingle))
        {
            Iterator<String> it = carryTargetDescriptions.iterator();
            String desc = it.next();
            String targetHex = desc.substring(desc.length() - 2);
            applyCarries(targetHex);
        }
        else
        {
            if (options.getOption(Options.autoPlay))
            {
                aiPause();
                ai.handleCarries(carryDamage, carryTargetDescriptions);
            }
            else
            {
                Set<String> carryTargetHexes = new TreeSet<String>();
                for (String desc : carryTargetDescriptions)
                {
                    carryTargetHexes.add(desc.substring(desc.length() - 2));
                }
                battleBoard.highlightPossibleCarries(carryTargetHexes);
                pickCarryDialog = new PickCarry(battleBoard, this, carryDamage,
                    carryTargetDescriptions);
            }
        }
    }

    public PickCarry getPickCarryDialog()
    {
        return pickCarryDialog;
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

    public void initBattle(MasterHex hex, int battleTurnNumber,
        Player battleActivePlayer, Constants.BattlePhase battlePhase,
        Legion attacker, Legion defender)
    {
        cleanupNegotiationDialogs();

        this.battleTurnNumber = battleTurnNumber;
        setBattleActivePlayer(battleActivePlayer);
        this.battlePhase = battlePhase;
        this.attacker = attacker;
        this.defender = defender;
        this.battleSite = hex;

        int attackerSide = this.attacker.getEntrySide();
        int defenderSide = (attackerSide + 3) % 6;
        this.defender.setEntrySide(defenderSide);

        if (board != null)
        {
            ensureEdtNewBattleBoard();
        }
    }

    public void ensureEdtNewBattleBoard()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            doNewBattleBoard();
        }
        else
        {
            // @TODO: use invokeLater() instead of invokeAndWait() ?
            //
            // Right now I don't dare to use invokeLater() - this way here
            // it preserves the execution order as it was without EDT,
            // but GUI stuff is one in EDT so we are safe from exceptions.
            Exception e = null;
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        doNewBattleBoard();
                    }

                });
            }
            catch (InvocationTargetException e1)
            {
                e = e1;
            }
            catch (InterruptedException e2)
            {
                e = e2;
            }

            if (e != null)
            {
                String message = "Failed to run doNewBattleBoard with "
                    + "invokeAndWait(): ";
                LOGGER.log(Level.SEVERE, message, e);
            }
        }
    }

    public void doNewBattleBoard()
    {
        if (battleBoard != null)
        {
            LOGGER.warning("Old BattleBoard still in place? Disposing it.");
            battleBoard.dispose();
            battleBoard = null;
        }
        battleBoard = new BattleBoard(this, battleSite, attacker, defender);
    }

    public void cleanupBattle()
    {
        LOGGER.log(Level.FINEST, owningPlayer.getName()
            + " Client.cleanupBattle()");
        if (battleBoard != null)
        {
            battleBoard.dispose();
            battleBoard = null;
        }
        battleChits.clear();
        battlePhase = null;
        battleTurnNumber = -1;
        battleActivePlayer = noone;
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
            if (owningPlayer.equals(getActivePlayer()))
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
            if (options.getOption(Options.autoPickEngagements))
            {
                aiPause();
                MasterHex hex = ai.pickEngagement();
                if (hex != null)
                {
                    engage(hex);
                }
                else
                {
                    doneWithEngagements();
                }
            }
            else
            {
                defaultCursor();
            }
            if (findEngagements().isEmpty() && board != null)
            {
                board.enableDoneAction();
            }
        }
    }

    /** Used for human players only.  */
    void doRecruit(Legion legion)
    {
        if (isMyTurn() && isMyLegion(legion)
            && ((LegionClientSide)legion).hasRecruited())
        {
            undoRecruit(legion);
            return;
        }

        if (legion == null || !((LegionClientSide)legion).canRecruit()
            || !isMyTurn() || !isMyLegion(legion))
        {
            return;
        }

        List<CreatureType> recruits = findEligibleRecruits(legion, legion
            .getCurrentHex());
        String hexDescription = legion.getCurrentHex().getDescription();

        String recruitName = PickRecruit.pickRecruit(board.getFrame(),
            recruits, hexDescription, legion, this);

        if (recruitName == null)
        {
            return;
        }

        String recruiterName = findRecruiterName(legion, recruitName,
            hexDescription);
        if (recruiterName == null)
        {
            return;
        }

        doRecruit(legion, recruitName, recruiterName);
    }

    // TODO use CreatureType instead of String
    public void doRecruit(Legion legion, String recruitName,
        String recruiterName)
    {
        // Call server even if some arguments are null, to get past
        // reinforcement.
        server.doRecruit(legion, recruitName, recruiterName);
    }

    /** Always needs to call server.doRecruit(), even if no recruit is
     *  wanted, to get past the reinforcing phase. */
    public void doReinforce(Legion legion)
    {
        if (options.getOption(Options.autoReinforce))
        {
            ai.reinforce(legion);
        }
        else
        {
            List<CreatureType> recruits = findEligibleRecruits(legion, legion
                .getCurrentHex());
            String hexDescription = legion.getCurrentHex().getDescription();

            String recruitName = PickRecruit.pickRecruit(board.getFrame(),
                recruits, hexDescription, legion, this);

            String recruiterName = null;
            if (recruitName != null)
            {
                recruiterName = findRecruiterName(legion, recruitName,
                    hexDescription);
            }
            doRecruit(legion, recruitName, recruiterName);
        }
    }

    public void didRecruit(Legion legion, String recruitName,
        String recruiterName, int numRecruiters)
    {
        if (isMyLegion(legion))
        {
            pushUndoStack(legion.getMarkerId());
        }

        List<String> recruiters = new ArrayList<String>();
        if (numRecruiters >= 1 && recruiterName != null)
        {
            for (int i = 0; i < numRecruiters; i++)
            {
                recruiters.add(recruiterName);
            }
            revealCreatures(legion, recruiters, Constants.reasonRecruiter);
        }
        String reason = (battleSite != null ? 
            Constants.reasonReinforced : Constants.reasonRecruited);
        
        addCreature(legion, recruitName, reason);

        ((LegionClientSide)legion).setRecruited(true);
        ((LegionClientSide)legion).setLastRecruit(recruitName);

        if (eventViewer != null)
        {
            eventViewer.recruitEvent(legion.getMarkerId(), (legion)
                .getHeight(), recruitName, recruiters, reason);
        }

        if (board != null)
        {
            // TODO should be in MasterBoard?
            GUIMasterHex hex = board.getGUIHexByMasterHex(legion
                .getCurrentHex());
            addRecruitedChit(recruitName, legion.getCurrentHex());
            hex.repaint();
            board.highlightPossibleRecruits();
        }
    }

    public void undidRecruit(Legion legion, String recruitName)
    {
        int eventType;
        if (battlePhase != null)
        {
            eventType = RevealEvent.eventReinforce;
            eventViewer.cancelReinforcement(recruitName, getTurnNumber());
        }
        else
        {
            // normal undoRecruit
            eventType = RevealEvent.eventRecruit;
            ((LegionClientSide)legion).removeCreature(recruitName);
        }
            
        ((LegionClientSide)legion).setRecruited(false);
        if (eventViewer != null)
        {
            eventViewer.undoEvent(eventType, legion
                .getMarkerId(), null, turnNumber);
        }
        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByMasterHex(legion
                .getCurrentHex());
            removeRecruitChit(legion.getCurrentHex());
            hex.repaint();
            board.highlightPossibleRecruits();
        }
    }

    /** null means cancel.  "none" means no recruiter (tower creature). */
    private String findRecruiterName(Legion legion, String recruitName,
        String hexDescription)
    {
        String recruiterName = null;

        List<String> recruiters = findEligibleRecruiters(legion, recruitName);

        int numEligibleRecruiters = recruiters.size();
        if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiterName = "none";
        }
        else if (options.getOption(Options.autoPickRecruiter)
            || numEligibleRecruiters == 1)
        {
            // If there's only one possible recruiter, or if
            // the user has chosen the autoPickRecruiter option,
            // then just reveal the first possible recruiter.
            recruiterName = recruiters.get(0);
        }
        else
        {
            // Even if PickRecruiter dialog is modal, this only prevents mouse
            // and keyboard into; but with pressing "D" one could still end the
            // recruiting phase which leaves game in inconsisten state...
            // So, forcibly really disable the Done action for that time.
            board.disableDoneAction("Finish 'Pick Recruiter' first");
            recruiterName = PickRecruiter.pickRecruiter(board.getFrame(),
                recruiters, hexDescription, legion, this);
            board.enableDoneAction();
        }
        return recruiterName;
    }

    /** Needed if we load a game outside the split phase, where
     *  active player and turn are usually set. */
    public void setupTurnState(Player activePlayer, int turnNumber)
    {
        this.activePlayer = (PlayerClientSide)activePlayer;
        this.turnNumber = turnNumber;
        if (eventViewer != null)
        {
            eventViewer.turnOrPlayerChange(this, turnNumber, this.activePlayer
                .getNumber());
        }
    }

    private void resetAllMoves()
    {
        for (PlayerClientSide player : players)
        {
            for (LegionClientSide legion : player.getLegions())
            {
                legion.setMoved(false);
                legion.setTeleported(false);
                legion.setRecruited(false);
            }
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

    public void setupSplit(Player activePlayer, int turnNumber)
    {
        clearUndoStack();
        cleanupNegotiationDialogs();

        this.activePlayer = (PlayerClientSide)activePlayer;
        this.turnNumber = turnNumber;

        if (eventViewer != null)
        {
            eventViewer.turnOrPlayerChange(this, turnNumber, this.activePlayer
                .getNumber());
        }

        this.phase = Constants.Phase.SPLIT;

        numSplitsThisTurn = 0;

        resetAllMoves();

        if (board != null)
        {
            if (isMyTurn())
            {
                // for debug purposes. We had a bug where legions remain
                // on the board even if player is dead. So, let's check
                // for this once per turn and clean up.
                validateLegions();
            }
            
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
                if (!options.getOption(Options.autoSplit)
                    && (getOwningPlayer().getMarkersAvailable().size() < 1 || findTallLegionHexes(
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

    private void validateLegions()
    {
        boolean foundProblem = false;
        
        for (Player p : players)
        {
            if (p.isDead())
            {
                for (Legion l : p.getLegions())
                {
                    LOGGER.warning("Dead player " + p.getName() + " has "
                        + "still legion " + l.getMarkerId()
                        + ". Removing it.");
                    p.removeLegion(l);
                    foundProblem = true;
                }
            }
        }
        if (foundProblem)
        {
            LOGGER.info("Found legion(s) for dead player "
                + "- recreating markers");
            board.recreateMarkers();
        }
    }
    
    private void kickSplit()
    {
        if (isMyTurn() && options.getOption(Options.autoSplit)
            && !isGameOver())
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
            if (options.getOption(Options.autoPickEngagements))
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
                if (!options.getOption(Options.autoRecruit)
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
        if (options.getOption(Options.autoRecruit) && playerAlive
            && isMyTurn() && this.phase == Constants.Phase.MUSTER)
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

    public void setupBattleSummon(Player battleActivePlayer,
        int battleTurnNumber)
    {
        this.battlePhase = Constants.BattlePhase.SUMMON;
        setBattleActivePlayer(battleActivePlayer);
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

    public void setupBattleRecruit(Player battleActivePlayer,
        int battleTurnNumber)
    {
        this.battlePhase = Constants.BattlePhase.RECRUIT;
        setBattleActivePlayer(battleActivePlayer);
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
        for (BattleChit chit : battleChits)
        {
            chit.setMoved(false);
            chit.setStruck(false);
        }
    }

    public void setupBattleMove(Player battleActivePlayer, int battleTurnNumber)
    {
        setBattleActivePlayer(battleActivePlayer);
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
        if (isMyBattlePhase() && options.getOption(Options.autoPlay))
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
            CritterMove cm = bestMoveOrder.get(0);
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
        Player battleActivePlayer)
    {
        this.battlePhase = battlePhase;
        setBattleActivePlayer(battleActivePlayer);
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
    public void tellLegionLocation(Legion legion, MasterHex hex)
    {
        legion.setCurrentHex(hex);

        if (board != null)
        {
            // @TODO: this creates it every time, not only when necessary ?
            Marker marker = new Marker(3 * Scale.get(), legion.getMarkerId(),
                this);
            setMarker(legion, marker);
            // TODO next line seems redundant since setMarker(..) does set the marker on the legion
            ((LegionClientSide)legion).setMarker(marker);
            if (!replayOngoing)
            {
                board.alignLegions(hex);
            }
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
        return Constants.getShortColorName(color);
    }

    // public for RevealEvent
    public String getShortColor(int playerNum)
    {
        PlayerClientSide player = getPlayer(playerNum);
        return player.getShortColor();
    }

    // TODO this would probably work better as state in PlayerState
    public Player getBattleActivePlayer()
    {
        return battleActivePlayer;
    }

    void setBattleActivePlayer(Player player)
    {
        this.battleActivePlayer = player;
    }

    Legion getBattleActiveLegion()
    {
        if (battleActivePlayer.equals(defender.getPlayer()))
        {
            return defender;
        }
        else
        {
            return attacker;
        }
    }

    Legion getBattleInactiveLegion()
    {
        if (battleActivePlayer.equals(defender.getPlayer()))
        {
            return attacker;
        }
        else
        {
            return defender;
        }
    }

    // public for IOracle
    public Legion getDefender()
    {
        return defender;
    }

    // public for IOracle
    public Legion getAttacker()
    {
        return attacker;
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

    // public for IOracle and BattleBoard
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
        LOGGER.log(Level.FINEST, owningPlayer.getName()
            + "handleFailedBattleMove");
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
            if (options.getOption(Options.autoPlay))
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
    public MasterHex getBattleSite()
    {
        return battleSite;
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
        return battleActivePlayer.equals(getPlayerByTag(chit.getTag()));
    }

    /** Return a set of hexLabels. */
    Set<String> findMobileCritterHexes()
    {
        Set<String> set = new HashSet<String>();
        for (BattleChit chit : getActiveBattleChits())
        {
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
        for (BattleChit chit : getActiveBattleChits())
        {
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

    void setStrikeNumbers(int tag, Set<String> targetHexLabels)
    {
        BattleChit chit = getBattleChit(tag);
        for (String targetHexLabel : targetHexLabels)
        {
            BattleChit target = getBattleChit(targetHexLabel);
            target.setStrikeNumber(strike.getStrikeNumber(chit, target));
            // TODO this whole block of code was written under the assumption
            //      that the Strike.getDice(BattleChit,BattleChit,int) method
            //      (now deleted) would return a new baseDice value through the
            //      third parameter. Java's parameters are CallByValue and do
            //      not allow OUT parameters, thus the whole code does nothing
            //      at all.
            //
            // CreatureType striker = game.getVariant().getCreatureByName(
            //     chit.getCreatureName());
            // int dice;
            // if (striker.isTitan())
            // {
            //     dice = chit.getTitanPower();
            // }
            // else
            // {
            //     dice = striker.getPower();
            // }
            // int baseDice = 0;
            // int strikeDice = strike.getDice(chit, target, baseDice);
            // if (baseDice == dice
            //     || options.getOption(Options.showDiceAjustmentsRange))
            // {
            //     target.setStrikeDice(strikeDice - dice);
            // }
        }
    }

    /** reset all strike numbers on chits */
    void resetStrikeNumbers()
    {
        for (BattleChit chit : battleChits)
        {
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
            return defender.getPlayer();
        }
        else
        {
            return attacker.getPlayer();
        }
    }

    boolean isMyCritter(int tag)
    {
        return (owningPlayer.equals(getPlayerByTag(tag)));
    }

    // TODO active or not would probably work better as state in PlayerState
    public PlayerClientSide getActivePlayer()
    {
        return activePlayer;
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

    private String figureTeleportingLord(LegionClientSide legion, MasterHex hex)
    {
        List<String> lords = listTeleportingLords(legion, hex);
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
                if (options.getOption(Options.autoPickLord))
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
                    return PickLord.pickLord(options, board.getFrame(), lords);
                }
        }
    }

    /** List the lords eligible to teleport this legion to hexLabel,
     *  as strings. 
     *
     *  TODO return value should be List<Creature> or List<CreatureType>
     */
    private List<String> listTeleportingLords(LegionClientSide legion,
        MasterHex hex)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        List<String> lords = new ArrayList<String>();

        // Titan teleport
        List<LegionClientSide> legions = getLegionsByHex(hex);
        if (!legions.isEmpty())
        {
            Legion legion0 = legions.get(0);
            if (legion0 != null && !isMyLegion(legion0) && legion.hasTitan())
            {
                lords.add(legion.getTitanBasename());
            }
        }

        // Tower teleport
        else
        {
            for (String name : legion.getContents())
            {
                CreatureType creature = game.getVariant().getCreatureByName(
                    name);
                if (creature != null && creature.isLord()
                    && !lords.contains(name))
                {
                    if (creature.isTitan())
                    {
                        lords.add(legion.getTitanBasename());
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

    boolean doMove(MasterHex hex)
    {
        return doMove(mover, hex);
    }

    /** Return true if the move looks legal. */
    public boolean doMove(LegionClientSide mover, MasterHex hex)
    {
        if (mover == null)
        {
            return false;
        }

        boolean teleport = false;

        Set<MasterHex> teleports = listTeleportMoves(mover);
        Set<MasterHex> normals = listNormalMoves(mover);
        if (teleports.contains(hex) && normals.contains(hex))
        {
            teleport = chooseWhetherToTeleport(hex);
        }
        else if (teleports.contains(hex))
        {
            teleport = true;
        }
        else if (normals.contains(hex))
        {
            teleport = false;
        }
        else
        {
            return false;
        }

        Set<String> entrySides = listPossibleEntrySides(mover, hex, teleport);

        String entrySide = null;
        if (options.getOption(Options.autoPickEntrySide))
        {
            entrySide = ai.pickEntrySide(hex, mover, entrySides);
        }
        else
        {
            entrySide = PickEntrySide.pickEntrySide(board.getFrame(), hex,
                entrySides);
        }

        if (!goodEntrySide(entrySide))
        {
            return false;
        }

        String teleportingLord = null;
        if (teleport)
        {
            teleportingLord = figureTeleportingLord(mover, hex);
        }

        // if this hex is already occupied, return false
        int friendlyLegions = getFriendlyLegions(hex, getActivePlayer())
            .size();
        if (hex.equals(mover.getCurrentHex()))
        {
            // same hex as starting hex, but it might be occupied by
            // multiple legions after split
            if (friendlyLegions > 1)
            {
                return false;
            }
        }
        else
        {
            if (friendlyLegions > 0)
            {
                return false;
            }
        }

        server.doMove(mover, hex, entrySide, teleport, teleportingLord);
        return true;
    }

    private boolean goodEntrySide(String entrySide)
    {
        return (entrySide != null && (entrySide.equals(Constants.left)
            || entrySide.equals(Constants.bottom) || entrySide
            .equals(Constants.right)));
    }

    public void didMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, String entrySide, boolean teleport,
        String teleportingLord, boolean splitLegionHasForcedMove)
    {
        removeRecruitChit(startingHex);
        if (isMyLegion(legion))
        {
            pushUndoStack(legion.getMarkerId());
        }
        legion.setCurrentHex(currentHex);
        legion.setMoved(true);
        ((LegionClientSide)legion).setEntrySide(BattleMap
            .entrySideNum(entrySide));

        // old version server does not send the teleportingLord,
        // socketCLientThread sets then it to null.
        if (teleport && teleportingLord != null)
        {
            legion.setTeleported(true);
            if (eventViewer != null)
            {
                eventViewer.newCreatureRevealEvent(RevealEvent.eventTeleport,
                    legion.getMarkerId(), legion.getHeight(), teleportingLord,
                    null, 0);
            }
        }
        if (board != null)
        {
            board.alignLegions(startingHex);
            board.alignLegions(currentHex);
            board.highlightUnmovedLegions();
            board.repaint();
            if (isMyLegion(legion))
            {
                if (splitLegionHasForcedMove)
                {
                    board.disableDoneAction("Split legion needs to move");
                }
                else
                {
                    board.enableDoneAction();
                }
            }
        }
        kickMoves();
    }

    public void undidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove)
    {
        removeRecruitChit(formerHex);
        removeRecruitChit(currentHex);
        legion.setCurrentHex(currentHex);
        legion.setMoved(false);
        boolean didTeleport = legion.hasTeleported();
        legion.setTeleported(false);
        if (board != null)
        {
            board.alignLegions(formerHex);
            board.alignLegions(currentHex);
            board.highlightUnmovedLegions();
            if (isMyTurn())
            {
                if (isUndoStackEmpty())
                {
                    board.disableDoneAction("At least one legion must move");
                }
                if (splitLegionHasForcedMove)
                {
                    board.disableDoneAction("Split legion needs to move");
                }
            }

            if (didTeleport && eventViewer != null)
            {
                eventViewer.undoEvent(RevealEvent.eventTeleport, legion
                    .getMarkerId(), null, turnNumber);
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
    public boolean reserveRecruit(CreatureType recruitType)
    {
        boolean ok = false;
        int remain;

        Integer count = recruitReservations.get(recruitType);
        if (count != null)
        {
            remain = count.intValue();
            recruitReservations.remove(recruitType);
        }
        else
        {
            LOGGER.log(Level.WARNING, owningPlayer.getName()
                + " reserveRecruit creature " + recruitType
                + " not fround from hash, should have been created"
                + " during getReservedCount!");
            remain = getGame().getCaretaker().getAvailableCount(recruitType);
        }

        if (remain > 0)
        {
            remain--;
            ok = true;
        }

        recruitReservations.put(recruitType, Integer.valueOf(remain));
        return ok;
    }

    /*
     * On first call (during a turn), cache remaining count from recruiter,
     * decrement on each further reserve for this creature.
     * This way we are independent of when the changes which are triggered by 
     * didRecruit influence the caretaker Stack. 
     * Returns how many creatures can still be recruited (=according
     * to caretaker's stack plus reservations)
     */
    public int getReservedRemain(CreatureType recruitType)
    {
        assert recruitType != null : "Can not reserve recruit for null";
        int remain;

        Integer count = recruitReservations.get(recruitType);
        if (count == null)
        {
            remain = getGame().getCaretaker().getAvailableCount(recruitType);
        }
        else
        {
            remain = count.intValue();
            recruitReservations.remove(recruitType);
        }

        // in case someone called getReservedRemain with bypassing the 
        // reset or reserve methods, to be sure double check against the 
        // real remaining value.
        int realCount = getGame().getCaretaker()
            .getAvailableCount(recruitType);
        if (realCount < remain)
        {
            remain = realCount;
        }
        recruitReservations.put(recruitType, Integer.valueOf(remain));

        return remain;
    }

    /** Return a list of Creatures (ignore reservations). 
     * 
     * TODO the extra hexLabel parameter is probably not needed anymore
     */
    public List<CreatureType> findEligibleRecruits(Legion legion, MasterHex hex)
    {
        return findEligibleRecruits(legion, hex, false);
    }

    /** Return a list of Creatures. Consider reservations if wanted
     * 
     * TODO the extra hexLabel parameter is probably not needed anymore
     */
    public List<CreatureType> findEligibleRecruits(Legion legion,
        MasterHex hex, boolean considerReservations)
    {
        assert hex != null : "Null hex given to find recruits in";

        List<CreatureType> recruits = new ArrayList<CreatureType>();

        if (legion == null)
        {
            return recruits;
        }

        MasterBoardTerrain terrain = hex.getTerrain();

        List<CreatureType> tempRecruits = TerrainRecruitLoader
            .getPossibleRecruits(terrain, hex);
        List<CreatureType> recruiters = TerrainRecruitLoader
            .getPossibleRecruiters(terrain, hex);

        Iterator<CreatureType> lit = tempRecruits.iterator();
        while (lit.hasNext())
        {
            CreatureType creature = lit.next();
            Iterator<CreatureType> liter = recruiters.iterator();
            while (liter.hasNext())
            {
                CreatureType lesser = liter.next();
                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser,
                    creature, terrain, hex) <= ((LegionClientSide)legion)
                    .numCreature(lesser))
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
            int remaining = getGame().getCaretaker()
                .getAvailableCount(recruit);

            if (remaining > 0 && considerReservations)
            {
                remaining = getReservedRemain(recruit);
            }
            if (remaining < 1)
            {
                it.remove();
            }
        }

        return recruits;
    }

    /** 
     * Return a list of creature name strings.
     * 
     * TODO return List<CreatureType>
     */
    public List<String> findEligibleRecruiters(Legion legion,
        String recruitName)
    {
        Set<CreatureType> recruiters;
        CreatureType recruit = game.getVariant()
            .getCreatureByName(recruitName);
        if (recruit == null)
        {
            return new ArrayList<String>();
        }

        MasterHex hex = legion.getCurrentHex();
        MasterBoardTerrain terrain = hex.getTerrain();

        recruiters = new HashSet<CreatureType>(TerrainRecruitLoader
            .getPossibleRecruiters(terrain, hex));
        Iterator<CreatureType> it = recruiters.iterator();
        while (it.hasNext())
        {
            CreatureType possibleRecruiter = it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain, hex);
            if (needed < 1
                || needed > ((LegionClientSide)legion)
                    .numCreature(possibleRecruiter))
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
    Set<MasterHex> getPossibleRecruitHexes()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();

        for (Legion legion : activePlayer.getLegions())
        {
            if (((LegionClientSide)legion).canRecruit())
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /** Return a set of all other unengaged legions of the legion's player
     *  that have summonables.
     */
    public SortedSet<Legion> findLegionsWithSummonableAngels(Legion summoner)
    {
        SortedSet<Legion> result = new TreeSet<Legion>();
        Player player = summoner.getPlayer();
        for (Legion legion : player.getLegions())
        {
            if (!legion.equals(summoner))
            {
                if (legion.hasSummonable()
                    && !(((LegionClientSide)legion).isEngaged()))
                {
                    result.add(legion);
                }
            }
        }
        return result;
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
    Set<MasterHex> listTeleportMoves(Legion legion)
    {
        MasterHex hex = legion.getCurrentHex();
        return movement.listTeleportMoves(legion, hex, movementRoll);
    }

    /** Return a set of hexLabels. */
    Set<MasterHex> listNormalMoves(LegionClientSide legion)
    {
        return movement.listNormalMoves(legion, legion.getCurrentHex(),
            movementRoll);
    }

    Set<String> listPossibleEntrySides(LegionClientSide mover, MasterHex hex,
        boolean teleport)
    {
        return movement.listPossibleEntrySides(mover, hex, teleport);
    }

    public List<LegionClientSide> getLegionsByHex(MasterHex hex)
    {
        assert hex != null : "No hex given to find legions on.";
        List<LegionClientSide> legions = new ArrayList<LegionClientSide>();
        for (PlayerClientSide player : players)
        {
            for (LegionClientSide legion : player.getLegions())
            {
                if (hex.equals(legion.getCurrentHex()))
                {
                    legions.add(legion);
                }
            }
        }
        return legions;
    }

    Set<MasterHex> findUnmovedLegionHexes()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();
        for (Legion legion : activePlayer.getLegions())
        {
            if (!legion.hasMoved())
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /** Return a set of hexLabels for the active player's legions with
     *  7 or more creatures. */
    Set<MasterHex> findTallLegionHexes()
    {
        return findTallLegionHexes(7);
    }

    /** Return a set of hexLabels for the active player's legions with
     *  minHeight or more creatures. */
    Set<MasterHex> findTallLegionHexes(int minHeight)
    {
        Set<MasterHex> result = new HashSet<MasterHex>();

        for (Legion legion : activePlayer.getLegions())
        {
            if (legion.getHeight() >= minHeight)
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /**
     * Return a set of all hexes with engagements.
     * 
     * TODO if we can be sure that the activePlayer is set properly, we could
     *      just create a set of all hexes he is on and then check if someone
     *      else occupies any of the same
     */
    public Set<MasterHex> findEngagements()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();
        Map<MasterHex, Player> playersOnHex = new HashMap<MasterHex, Player>();
        for (Player player : players)
        {
            for (Legion legion : player.getLegions())
            {
                MasterHex hex = legion.getCurrentHex();
                if (playersOnHex.get(hex) == null)
                {
                    // no player on that hex found yet, set this one
                    playersOnHex.put(hex, player);
                }
                else
                {
                    if (!playersOnHex.get(hex).equals(player))
                    {
                        // someone else already on the hex -> engagement
                        result.add(hex);
                    }
                }
            }
        }
        return result;
    }

    boolean isOccupied(MasterHex hex)
    {
        return !getLegionsByHex(hex).isEmpty();
    }

    boolean isEngagement(MasterHex hex)
    {
        List<LegionClientSide> legions = getLegionsByHex(hex);
        if (legions.size() == 2)
        {
            Legion info0 = legions.get(0);
            Player player0 = info0.getPlayer();

            Legion info1 = legions.get(1);
            Player player1 = info1.getPlayer();

            return !player0.equals(player1);
        }
        return false;
    }

    public List<Legion> getEnemyLegions(final Player player)
    {
        List<Legion> result = new ArrayList<Legion>();
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                result.addAll(otherPlayer.getLegions());
            }
        }
        return result;
    }

    public List<Legion> getEnemyLegions(final MasterHex hex,
        final Player player)
    {
        List<Legion> result = new ArrayList<Legion>();
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                for (Legion legion : otherPlayer.getLegions())
                {
                    if (legion.getCurrentHex().equals(hex))
                    {
                        result.add(legion);
                    }
                }
            }
        }
        return result;
    }

    public Legion getFirstEnemyLegion(MasterHex hex, Player player)
    {
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                for (Legion legion : otherPlayer.getLegions())
                {
                    if (legion.getCurrentHex().equals(hex))
                    {
                        return legion;
                    }
                }
            }
        }
        return null;
    }

    public List<LegionClientSide> getFriendlyLegions(final MasterHex hex,
        final PlayerClientSide player)
    {
        return CollectionHelper.selectAsList(player.getLegions(),
            new Predicate<LegionClientSide>()
            {
                public boolean matches(LegionClientSide legion)
                {
                    return legion.getCurrentHex().equals(hex);
                }
            });
    }

    public Legion getFirstFriendlyLegion(final MasterHex hex, Player player)
    {
        return CollectionHelper.selectFirst(player.getLegions(),
            new Predicate<Legion>()
            {
                public boolean matches(Legion legion)
                {
                    return legion.getCurrentHex().equals(hex);
                }
            });
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
        Start.triggerTimedQuit();
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
            undoSplit(getLegion(splitoffId));
        }
    }

    // because of synchronization issues we need to
    // be able to pass an undo split request to the server even if it is not
    // yet in the client UndoStack
    public void undoSplit(Legion splitoff)
    {
        server.undoSplit(splitoff);
        getOwningPlayer().addMarkerAvailable(splitoff.getMarkerId());
        numSplitsThisTurn--;
        if (turnNumber == 1 && numSplitsThisTurn == 0)
        {
            // must split in first turn - Done not allowed now
            if (board != null && isMyTurn())
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
            server.undoMove(getLegion(markerId));
            clearRecruitChits();
        }
    }

    public void undidSplit(Legion splitoff, Legion survivor, int turn)
    {
        ((LegionClientSide)survivor).merge(splitoff);
        removeLegion(splitoff);
        // do the eventViewer stuff before the board, so we are sure to get
        // a repaint.
        if (eventViewer != null && !replayOngoing)
        {
            eventViewer.undoEvent(RevealEvent.eventSplit, survivor
                .getMarkerId(), splitoff.getMarkerId(), turn);
        }
        else
        {
            // fine. So this client does not even have eventViewer 
            // (probably then not even a masterBoard, i.e. AI)
        }

        if (board != null)
        {
            if (replayOngoing)
            {
                replayTurnChange(turn);
            }
            else
            {
                board.alignLegions(survivor.getCurrentHex());
                board.highlightTallLegions();
            }
        }
        if (isMyTurn() && this.phase == Constants.Phase.SPLIT && !replayOngoing
            && options.getOption(Options.autoSplit) && !isGameOver())
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
            server.undoRecruit(getLegion(markerId));
        }
    }

    void undoRecruit(Legion legion)
    {
        if (undoStack.contains(legion.getMarkerId()))
        {
            undoStack.remove(legion.getMarkerId());
        }
        server.undoRecruit(legion);
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

    public PlayerClientSide getPlayerByMarkerId(String markerId)
    {
        assert markerId != null : "Parameter must not be null";

        String shortColor = markerId.substring(0, 2);
        return getPlayerUsingColor(shortColor);
    }

    private PlayerClientSide getPlayerUsingColor(String shortColor)
    {
        assert this.players != null : "Client not yet initialized";
        assert shortColor != null : "Parameter must not be null";

        // Stage 1: See if the player who started with this color is alive.
        for (PlayerClientSide info : players)
        {
            if (shortColor.equals(info.getShortColor()) && !info.isDead())
            {
                return info;
            }
        }

        // Stage 2: He's dead.  Find who killed him and see if he's alive.
        for (PlayerClientSide info : players)
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
                    return getPlayerUsingColor(info.getShortColor());
                }
            }
        }
        return null;
    }

    public boolean isMyLegion(String markerId)
    {
        return owningPlayer.equals(getPlayerByMarkerId(markerId));
    }

    private boolean isMyLegion(Legion legion)
    {
        return owningPlayer.equals(legion.getPlayer());
    }

    boolean isMyTurn()
    {
        return owningPlayer.equals(getActivePlayer());
    }

    boolean isMyBattlePhase()
    {
        // check also for phase, because delayed callbacks could come
        // after our phase is over but activePlayerName not updated yet
        return playerAlive && owningPlayer.equals(getBattleActivePlayer())
            && this.phase == Constants.Phase.FIGHT;
    }

    public int getMovementRoll()
    {
        return movementRoll;
    }

    // TODO inline
    int getMulligansLeft()
    {
        return owningPlayer.getMulligansLeft();
    }

    void doSplit(Legion legion)
    {
        LOGGER.log(Level.FINER, "Client.doSplit " + legion);
        this.parent = null;

        if (!isMyTurn())
        {
            LOGGER.log(Level.SEVERE, "Not my turn!");
            kickSplit();
            return;
        }
        // Can't split other players' legions.
        if (!isMyLegion(legion))
        {
            LOGGER.log(Level.SEVERE, "Not my legion!");
            kickSplit();
            return;
        }
        Set<String> markersAvailable = getOwningPlayer().getMarkersAvailable();
        // Need a legion marker to split.
        if (markersAvailable.size() < 1)
        {
            showMessageDialog("No legion markers");
            kickSplit();
            return;
        }
        // Legion must be tall enough to split.
        if (legion.getHeight() < 4)
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

        this.parent = legion;
        String childId = null;

        if (options.getOption(Options.autoPickMarker))
        {
            childId = ai.pickMarker(markersAvailable, getShortColor());
        }
        else
        {
            childId = PickMarker.pickMarker(board.getFrame(), owningPlayer,
                markersAvailable, options);
        }
        pickMarkerCallback(childId);
    }

    /** Called after a marker is picked, either first marker or split. */
    void pickMarkerCallback(String childMarker)
    {
        if (childMarker == null)
        {
            return;
        }
        if (parent == null)
        {
            // Picking first marker.
            server.assignFirstMarker(childMarker);
            return;
        }
        String results = SplitLegion.splitLegion(this, parent, childMarker);
        if (results != null)
        {
            doSplit(parent, childMarker, results);
        }
    }

    /** Called by AI, and by pickMarkerCallback() */
    public void doSplit(Legion parent, String childMarker, String results)
    {
        LOGGER.log(Level.FINER, "Client.doSplit " + parent + " " + childMarker
            + " " + results);
        server.doSplit(parent, childMarker, results);
    }

    /** 
     * Callback from server after any successful split.
     * 
     * TODO childHeight is probably redundant now that we pass the legion object
     */
    public void didSplit(MasterHex hex, Legion parent, Legion child,
        int childHeight, List<String> splitoffs, int turn)
    {
        LOGGER.log(Level.FINEST, "Client.didSplit " + hex + " " + parent + " "
            + child + " " + childHeight + " " + turn);

        ((LegionClientSide)parent).split(childHeight, child, turn);

        child.setCurrentHex(hex);

        if (eventViewer != null && !replayOngoing)
        {
            eventViewer.newSplitEvent(turn, parent.getMarkerId(), (parent)
                .getHeight(), null, child.getMarkerId(), (child).getHeight());
        }

        if (board != null)
        {
            Marker marker = new Marker(3 * Scale.get(), child.getMarkerId(),
                this);
            setMarker(child, marker);
            
            if (replayOngoing)
            {
                replayTurnChange(turn);
            }
            else
            {
                board.alignLegions(hex);
            }
        }

        if (isMyLegion(child))
        {
            clearRecruitChits();
            pushUndoStack(child.getMarkerId());
            getOwningPlayer().removeMarkerAvailable(child.getMarkerId());
        }

        numSplitsThisTurn++;
        if (turnNumber == 1 && board != null && isMyTurn())
        {
            board.enableDoneAction();
        }

        if (board != null)
        {
            if (!replayOngoing)
            {
                board.alignLegions(hex);
                board.highlightTallLegions();
            }
        }

        // check also for phase, because delayed callbacks could come
        // after our phase is over but activePlayerName not updated yet.
        if (isMyTurn() && this.phase == Constants.Phase.SPLIT && !replayOngoing
            && options.getOption(Options.autoSplit) && !isGameOver())
        {
            boolean done = ai.splitCallback(parent, child);
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    public void askPickColor(List<String> colorsLeft)
    {
        String color = null;
        if (options.getOption(Options.autoPickColor))
        {
            // Convert favorite colors from a comma-separated string to a list.
            String favorites = options.getStringOption(Options.favoriteColors);
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
                color = PickColor.pickColor(board.getFrame(), owningPlayer
                    .getName(), colorsLeft, options);
            }
            while (color == null);
        }

        setColor(color);

        server.assignColor(color);
    }

    public void askPickFirstMarker()
    {
        String markerId = null;

        Set<String> markersAvailable = getOwningPlayer().getMarkersAvailable();
        if (options.getOption(Options.autoPickMarker))
        {
            markerId = ai.pickMarker(markersAvailable, getShortColor());
        }
        else
        {
            do
            {
                markerId = PickMarker.pickMarker(board.getFrame(),
                    owningPlayer, markersAvailable, options);
            }
            while (markerId == null);
        }
        pickMarkerCallback(markerId);
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
            options.setOption(Options.favoriteLookFeel, lfName);
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
        String type = aType;
        if (type.endsWith(Constants.anyAI))
        {
            int whichAI = Dice.rollDie(Constants.numAITypes) - 1;
            type = Constants.aiArray[whichAI];
        }
        if (!type.startsWith(Constants.aiPackage))
        {
            if (type.startsWith(Constants.oldAiPackage))
            {
                type = type.replace(Constants.oldAiPackage,
                    Constants.aiPackage);
            }
            else
            {
                type = Constants.aiPackage + type;
            }
        }
        LOGGER.log(Level.FINEST, "final type: " + type);
        if (type.endsWith("AI"))
        {
            LOGGER.log(Level.FINEST, "new type is AI. current ai is "
                + ai.getClass().getName());
            if (!(ai.getClass().getName().equals(type)))
            {
                LOGGER.log(Level.FINEST, "need to change type");
                LOGGER.log(Level.INFO, "Changing client "
                    + owningPlayer.getName() + " from "
                    + ai.getClass().getName() + " to " + type);
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
                        + owningPlayer.getName() + " from "
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
        delay = options.getIntOption(Options.aiDelay);
        if (!options.getOption(Options.autoPlay)
            || delay < Constants.MIN_AI_DELAY)
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
            boolean bval = options.getOption(Options.showCaretaker);
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

    public Options getOptions()
    {
        return options;
    }

    private void showOrHideLogWindow(boolean show)
    {
        if (board != null && show)
        {
            if (logWindow == null)
            {
                // the logger with the empty name is parent to all loggers
                // and thus catches all messages
                logWindow = new LogWindow(Client.this, Logger.getLogger(""));
            }
        }
        else
        {
            disposeLogWindow();
        }
    }
}
