package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.IServer;
import net.sf.colossus.server.Player;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.LogWindow;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Split;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 *  Lives on the client side and handles all communication
 *  with the server.  It talks to the client classes locally, and to
 *  Server via the network protocol.  There is one client per player.
 *  @version $Id$
 *  @author David Ripton
 *  @author Romain Dolbeau
 */


public final class Client implements IClient, IOracle, IOptions
{

    /** This will eventually be a network interface rather than a
     *  direct reference.  So don't share this reference. */
    private IServer server;

    private MasterBoard board;
    private StatusScreen statusScreen;
    private CreatureCollectionView caretakerDisplay;
    private SummonAngel summonAngel;
    private MovementDie movementDie;
    private EngagementResults engagementResults;
    private AutoInspector autoInspector;

    /** hexLabel of MasterHex for current or last engagement. */
    private String battleSite;
    private BattleMap map;
    private BattleDice battleDice;

    private List battleChits = new ArrayList();

    /** Stack of legion marker ids, to allow multiple levels of undo for
     *  splits, moves, and recruits. */
    private LinkedList undoStack = new LinkedList();

    // Information on the current moving legion.
    private String moverId;

    /** The end of the list is on top in the z-order. */
    private List markers = new ArrayList();

    private List recruitChits = new ArrayList();

    // Per-client and per-player options.
    private Options options;

    /** Player who owns this client. */
    private String playerName;

    /** Starting marker color of player who owns this client. */
    private String color;

    /** Last movement roll for any player. */
    private int movementRoll = -1;

    /** the parent frame for secondary windows */
    private JFrame secondaryParent = null;

    private String parentId;
    private int numSplitsThisTurn;

    private AI ai = new SimpleAI(this);

    private CaretakerInfo caretakerInfo = new CaretakerInfo();

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

    /** One per player. */
    private PredictSplits[] predictSplits;

    /** One LegionInfo per legion, keyed by markerId.  Never null. */
    private SortedMap legionInfo = new TreeMap();

    private int numPlayers;

    private Movement movement = new Movement(this);
    private BattleMovement battleMovement = new BattleMovement(this);
    private Strike strike = new Strike(this);

    private boolean remote;
    private SocketClientThread sct;

    // For negotiation.  (And AI battle.)
    private Negotiate negotiate;
    private ReplyToProposal replyToProposal;

    // XXX temporary until things are synched
    private boolean tookMulligan;

    private int delay = -1;

    /** For battle AI. */
    private List bestMoveOrder = null;
    private List failedBattleMoves = null;

    // XXX Make private and wrap consistently.
    boolean showAllRecruitChits = false;

    private LogWindow logWindow;

    public Client(String host, int port, String playerName, boolean remote)
    {
        super();

        this.playerName = playerName;
        this.remote = remote;

        options = new Options(playerName);
        // Need to load options early so they don't overwrite server options.
        loadOptions();

        sct = new SocketClientThread(this, host, port);
        this.server = sct;

        if (remote)
        {
            net.sf.colossus.util.ResourceLoader.setDataServer(host, port + 1);
        }
        else
        {
            net.sf.colossus.util.ResourceLoader.setDataServer(null, 0);
        }

        sct.start();

        TerrainRecruitLoader.setCaretakerInfo(caretakerInfo);
        net.sf.colossus.server.CustomRecruitBase.addCaretakerInfo(
            caretakerInfo);
    }

    boolean isRemote()
    {
        return remote;
    }

    /** Take a mulligan. */
    void mulligan()
    {
        undoAllMoves();   // XXX Maybe move entirely to server
        clearUndoStack();
        clearRecruitChits();
        tookMulligan = true;

        server.mulligan();
    }

    // XXX temp
    boolean tookMulligan()
    {
        return tookMulligan;
    }

    /** Resolve engagement in land. */
    void engage(String land)
    {
        server.engage(land);
    }

    String getMyEngagedMarkerId()
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
        server.concede(markerId);
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

    private List _tellEngagementResults_attackerStartingContents = null;
    private List _tellEngagementResults_defenderStartingContents = null;
    private List _tellEngagementResults_attackerLegionCertainities = null;
    private List _tellEngagementResults_defenderLegionCertainities = null;
    public void tellEngagement(String hexLabel, String attackerId,
        String defenderId)
    {
        this.battleSite = hexLabel;
        this.attackerMarkerId = attackerId;
        this.defenderMarkerId = defenderId;

        // remember for end of battle.
        _tellEngagementResults_attackerStartingContents =
            getLegionImageNames(attackerId);
        _tellEngagementResults_defenderStartingContents =
            getLegionImageNames(defenderId);
        // TODO: I have the feeling that getLegionCertainties()
        //   does not work here.
        //   I always seem to get either ALL true or ALL false.
        _tellEngagementResults_attackerLegionCertainities =
            getLegionCreatureCertainties(attackerId);
        _tellEngagementResults_defenderLegionCertainities =
            getLegionCreatureCertainties(defenderId);

        highlightBattleSite();
    }

    private void showEngagementResults(boolean show)
    {
        if (engagementResults != null)
        {
            engagementResults.maybeShow();
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

        if (getOption(Options.showEngagementResults))
        {
            if (engagementResults == null)
            {
                engagementResults = new EngagementResults(frame, this, this);
            }
            engagementResults.addData(winnerId, method, points, turns,
                _tellEngagementResults_attackerStartingContents,
                _tellEngagementResults_defenderStartingContents,
                _tellEngagementResults_attackerLegionCertainities,
                _tellEngagementResults_defenderLegionCertainities,
                isMyTurn());
        }
        else
        {
            engagementResults = null;
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
            autoInspector.showLegion(marker, legion);
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

    /** This player quits the whole game. The server needs to always honor
     *  this request, because if it doesn't players will just drop
     *  connections when they want to quit in a hurry. */
    void withdrawFromGame()
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
        if (battleDice != null)
        {
            battleDice.repaint();
        }
        if (map != null)
        {
            map.repaint();
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
        if (battleDice != null)
        {
            battleDice.rescale();
        }
        if (map != null)
        {
            map.rescale();
        }
        repaintAllWindows();
    }

    public void tellMovementRoll(int roll)
    {
        movementRoll = roll;
        if (movementDie == null || roll != movementDie.getLastRoll())
        {
            initMovementDie(roll);
            if (board != null)
            {
                board.repaint();
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
            movementDie = new MovementDie(4 * Scale.get(),
                MovementDie.getDieImageName(roll));
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

    /** Fully sync the board state by running all option triggers. */
    private void runAllOptionTriggers()
    {
        Enumeration en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            String value = getStringOption(name);
            optionTrigger(name, value);
        }
        syncOptions();
    }

    /** Trigger side effects after changing an option value. */
    private void optionTrigger(String optname, String value)
    {
        Log.event("optionTrigger " + optname + " : " + value);
        boolean bval = Boolean.valueOf(value).booleanValue();

        if (optname.equals(Options.antialias))
        {
            GUIHex.setAntialias(bval);
            repaintAllWindows();
        }
        else if (optname.equals(Options.useOverlay))
        {
            GUIHex.setOverlay(bval);
            repaintAllWindows();
        }
        else if (optname.equals(Options.showAllRecruitChits))
        {
            showAllRecruitChits = bval;
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
            // XXX move this to server
            // XXX Now this uses JDK logging should this be moved
            // to the server still?
            if (bval) {
                Logger.global.setLevel(Level.ALL);
            }
            else {
                Logger.global.setLevel(Level.INFO);
            }
        }
        else if (optname.equals(Options.showCaretaker))
        {
            updateCreatureCountDisplay();
        }
        else if (optname.equals(Options.showLogWindow))
        {
            if (bval)
            {
                if (logWindow == null) {
                    // the logger with the empty name is parent to all loggers
                    // and thus catches all messages
                    logWindow = new LogWindow(this, Logger.getLogger(""));
                }
            }
            else
            {
                if (logWindow != null) {
                    logWindow.setVisible(false);
                    logWindow.dispose();
                    logWindow = null;
                }
            }
        }
        else if (optname.equals(Options.showStatusScreen))
        {
            updateStatusScreen();
        }
        else if (optname.equals(Options.showAutoInspector))
        {
            if (bval)
            {
                if (autoInspector == null)
                {
                    JFrame parent = secondaryParent;
                    if((parent == null) && (board != null))
                    {
                        parent = board.getFrame();
                    }
                    boolean onlyOwnLegions = getOption(Options.onlyOwnLegions);
                    autoInspector = new AutoInspector(parent, this, playerName,
                    		onlyOwnLegions);
                }
            }
            else
            {
                if (autoInspector != null)
                {
                    autoInspector.setVisible(false);
                    autoInspector.dispose();
                    autoInspector = null;
                }
            }
        }
        else if (optname.equals(Options.showEngagementResults))
        {
            showEngagementResults(bval);
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
        Enumeration en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            boolean value = getOption(name);
            board.twiddleOption(name, value);
        }
    }

    // public for IOracle
    public int getNumPlayers()
    {
        return numPlayers;
    }

    int getNumLivingPlayers()
    {
        int total = 0;
        for (int i = 0; i < playerInfo.length; i++)
        {
            PlayerInfo info = playerInfo[i];
            if (!info.isDead())
            {
                total++;
            }
        }
        return total;
    }

    public void updatePlayerInfo(List infoStrings)
    {
        numPlayers = infoStrings.size();
        if (playerInfo == null)
        {
            playerInfo = new PlayerInfo[numPlayers];
            for (int i = 0; i < numPlayers; i++)
            {
                playerInfo[i] = new PlayerInfo(this);
            }
        }
        for (int i = 0; i < numPlayers; i++)
        {
            playerInfo[i].update((String)infoStrings.get(i));
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
                    statusScreen = new StatusScreen((secondaryParent == null ?
                        board.getFrame() : secondaryParent), this, this,
                        this);
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

    PlayerInfo getPlayerInfo(String name)
    {
        for (int i = 0; i < playerInfo.length; i++)
        {
            if (name.equals(playerInfo[i].getName()))
            {
                return playerInfo[i];
            }
        }
        return null;
    }

    PlayerInfo getPlayerInfo()
    {
        return getPlayerInfo(playerName);
    }

    List getPlayerNames()
    {
        List names = new ArrayList();
        for (int i = 0; i < playerInfo.length; i++)
        {
            names.add(playerInfo[i].getName());
        }
        return names;
    }

    int getPlayerNum(String pName)
    {
        for (int i = 0; i < playerInfo.length; i++)
        {
            if (pName.equals(playerInfo[i].getName()))
            {
                return i;
            }
        }
        return -1;
    }

    /** Return the average point value of all legions in the game. */
    int getAverageLegionPointValue()
    {
        int totalValue = 0;
        int totalLegions = 0;

        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
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
        if (getOption(Options.showCaretaker))
        {
            if (caretakerDisplay == null)
            {
                if (board != null)
                {
                    caretakerDisplay = new CreatureCollectionView(
                        (secondaryParent == null ? board.getFrame() :
                        secondaryParent), this);
                    caretakerDisplay.addWindowListener(new WindowAdapter()
                    {
                        public void windowClosing(WindowEvent e)
                        {
                            setOption(Options.showCaretaker, false);
                        }
                    }
                    );
                }
            }
            else
            {
                caretakerDisplay.update();
            }
        }
        else
        {
            if (board != null)
            {
                board.twiddleOption(Options.showCaretaker, false);
            }
            if (caretakerDisplay != null)
            {
                caretakerDisplay.dispose();
                caretakerDisplay = null;
            }
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

    void disposeStatusScreen()
    {
        if (statusScreen != null)
        {
            statusScreen.dispose();
            statusScreen = null;
        }
    }

    public void dispose()
    {
        cleanupBattle();
        disposeMovementDie();
        disposeStatusScreen();
        disposeMasterBoard();
        if (isRemote())
        {
            System.exit(0);
        }
    }

    /** Called from BattleMap to leave carry mode. */
    void leaveCarryMode()
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
        Iterator it = getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.getCurrentHexLabel().startsWith("X"))
            {
                return true;
            }
        }
        return false;
    }

    List getActiveBattleChits()
    {
        List chits = new ArrayList();
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (getBattleActivePlayerName().equals(getPlayerNameByTag(
                chit.getTag())))
            {
                chits.add(chit);
            }
        }
        return chits;
    }

    List getInactiveBattleChits()
    {
        List chits = new ArrayList();
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (!getBattleActivePlayerName().equals(getPlayerNameByTag(
                chit.getTag())))
            {
                chits.add(chit);
            }
        }
        return chits;
    }

    private void markOffboardCreaturesDead()
    {
        Iterator it = getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
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
            return strike.makeForcedStrikes(getOption(
                Options.autoRangeSingle));
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
                    struck = ai.strike(getLegionInfo(
                        getBattleActiveMarkerId()));
                }
                if (!struck)
                {
                    doneWithStrikes();
                }
            }
            else
            {
                boolean struck = makeForcedStrikes();
                if (map != null)
                {
                    map.highlightCrittersWithTargets();
                }
                if (!struck && findCrittersWithTargets().isEmpty())
                {
                    doneWithStrikes();
                }
            }
        }
    }

    List getMarkers()
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
    LegionInfo getLegionInfo(String markerId)
    {
        LegionInfo info = (LegionInfo)legionInfo.get(markerId);
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

    /** Needed after loadGame() outside split phase. */
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
    public List getLegionImageNames(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        if(info != null)
        {
            return info.getImageNames();
        }
        return new ArrayList();
    }

    /** Return a list of Booleans */
    // public for IOracle
    public List getLegionCreatureCertainties(String markerId)
    {
        try
        {
            return getLegionInfo(markerId).getCertainties();
        }
        catch (NullPointerException exc)
        {  // TODO: is this the right thing?
            List l = new ArrayList(42/4);  // just longer then max
            for (int idx = 0; idx < (42/4); idx++)
            {
                l.add(new Boolean(true));  // all true
            }
            return l;
        }
    }

    /** Add a new creature to this legion. */
    public void addCreature(String markerId, String name)
    {
        getLegionInfo(markerId).addCreature(name);
        if (board != null)
        {
            String hexLabel = getHexForLegion(markerId);
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            hex.repaint();
        }
    }

    public void removeCreature(String markerId, String name)
    {
        LegionInfo info = getLegionInfo(markerId);
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

    /** Reveal creatures in this legion, some of which already may be known. */
    public void revealCreatures(String markerId, final List names)
    {
        String pName = getPlayerNameByMarkerId(markerId);
        if (predictSplits == null || getPredictSplits(pName) == null)
        {
            initPredictSplits(pName, markerId, names);
            createLegionInfo(markerId);
        }
        getLegionInfo(markerId).revealCreatures(names);
    }

    /** additionally remember the images list for later, the engagement report
     * */
    public void revealEngagedCreatures(
        String markerId, final List names, boolean isAttacker)
    {
        revealCreatures(markerId, names);
        // in engagment we need to update the remembered list, too.
        if (isAttacker)
        {
            _tellEngagementResults_attackerStartingContents =
                getLegionImageNames(markerId);
            // towi: should return a list of trues, right?
            _tellEngagementResults_attackerLegionCertainities =
                getLegionCreatureCertainties(markerId);
        }
        else
        {
            _tellEngagementResults_defenderStartingContents =
                getLegionImageNames(markerId);
            // towi: should return a list of trues, right?
            _tellEngagementResults_defenderLegionCertainities =
                getLegionCreatureCertainties(markerId);
        }

    }

    List getBattleChits()
    {
        return Collections.unmodifiableList(battleChits);
    }

    List getBattleChits(String hexLabel)
    {
        List chits = new ArrayList();

        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (hexLabel.equals(chit.getCurrentHexLabel()))
            {
                chits.add(chit);
            }
        }
        return chits;
    }

    BattleChit getBattleChit(String hexLabel)
    {
        List chits = getBattleChits(hexLabel);
        if (chits.isEmpty())
        {
            return null;
        }
        return (BattleChit)chits.get(0);
    }

    /** Get the BattleChit with this tag. */
    BattleChit getBattleChit(int tag)
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.getTag() == tag)
            {
                return chit;
            }
        }
        return null;
    }

    public void removeDeadBattleChits()
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.isDead())
            {
                it.remove();

                // Also remove it from LegionInfo.
                String name = chit.getId();
                if (chit.isInverted())
                {
                    getLegionInfo(defenderMarkerId).removeCreature(name);
                }
                else
                {
                    getLegionInfo(attackerMarkerId).removeCreature(name);
                }
            }
        }
        if (map != null)
        {
            map.repaint();
        }
    }

    public void placeNewChit(String imageName, boolean inverted,
        int tag, String hexLabel)
    {
        addBattleChit(imageName, inverted, tag, hexLabel);
        if (map != null)
        {
            map.alignChits(hexLabel);
            // Make sure map is visible after summon or muster.
            focusMap();
        }
    }

    /** Create a new BattleChit and add it to the end of the list. */
    private void addBattleChit(final String bareImageName,
        boolean inverted, int tag, String hexLabel)
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
        BattleChit chit = new BattleChit(5 * Scale.get(), imageName,
            inverted, tag, hexLabel, colorName, this);
        battleChits.add(chit);
    }

    List getRecruitChits()
    {
        return Collections.unmodifiableList(recruitChits);
    }

    void addRecruitChit(String imageName, String hexLabel)
    {
        int scale = 2 * Scale.get();
        GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
        Chit chit = new Chit(scale, imageName);
        Point startingPoint = hex.getOffCenter();
        Point point = new Point(startingPoint);
        point.x -= scale / 2;
        point.y -= scale / 2;
        chit.setLocation(point);
        recruitChits.add(chit);
    }

    void addRecruitChit(List imageNameList, String hexLabel)
    {
        Iterator it = imageNameList.iterator();
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
                Log.error("Only String or Creature in addRecruitChit() !");
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
            point.x += ((offset * scale) +
                ((size % 2 == 0 ? (scale / 2) : 0))) / size;
            point.y += ((offset * scale) +
                ((size % 2 == 0 ? (scale / 2) : 0))) / size;
            num--;
            chit.setLocation(point);
            recruitChits.add(chit);
        }
    }

    void removeRecruitChit(String hexLabel)
    {
        Iterator it = recruitChits.iterator();
        while (it.hasNext())
        {
            Chit chit = (Chit)it.next();
            // TODO the next line can cause an NPE when the user closes the client app
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            if (hex != null && hex.contains(chit.getCenter()))
            {
                it.remove();
                return;
            }
        }
    }

    void clearRecruitChits()
    {
        recruitChits.clear();
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

    public void initBoard()
    {
        Log.debug(playerName + " Client.initBoard()");
        if (isRemote())
        {
            VariantSupport.loadVariant(options.getStringOption(
                Options.variant), false);
        }

        if (!getOption(Options.autoPlay))
        {
            disposeMasterBoard();
            board = new MasterBoard(this);
            if ( getOption(Options.showAutoInspector) )
            {
                // Recreate the AutoInspector since it does not have the
                // parent set if it is created during game initialization.
                // This problem is due to the order in which windows are
                // created and should normally not exist if the master
                // board would be initialized before the AutoInspector
                // window gets created. It is not and this little hack
                // works around the issue of having a detached window.
                // A proper fix would involve major changes to the
                // initialization process.
                optionTrigger(Options.showAutoInspector, String.valueOf(false));
                optionTrigger(Options.showAutoInspector, String.valueOf(true));
            }
            focusBoard();
        }
    }

    BattleMap getBattleMap()
    {
        return map;
    }

    void setBattleMap(BattleMap map)
    {
        this.map = map;
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
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
            List parts = Split.split(':', typeColonDonor);
            String unit = (String)parts.get(0);
            String donor = (String)parts.get(1);
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

    public void askAcquireAngel(String markerId, List recruits)
    {
        if (getOption(Options.autoAcquireAngels))
        {
            acquireAngelCallback(markerId, ai.acquireAngel(markerId,
                recruits));
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
    public void askChooseStrikePenalty(List choices)
    {
        if (getOption(Options.autoPlay))
        {
            String choice = ai.pickStrikePenalty(choices);
            assignStrikePenalty(choice);
        }
        else
        {
            new PickStrikePenalty(map.getFrame(), this, choices);
        }
    }

    void assignStrikePenalty(String prompt)
    {
        server.assignStrikePenalty(prompt);
    }

    private JFrame getMapOrBoardFrame()
    {
        JFrame frame = null;
        if (map != null)
        {
            frame = map.getFrame();
        }
        else if (board != null)
        {
            frame = board.getFrame();
        }
        return frame;
    }

    void showMessageDialog(String message)
    {
        // Don't bother showing messages to AI players.  Perhaps we
        // should log them.
        if (getOption(Options.autoPlay))
        {
            return;
        }
        JFrame frame = getMapOrBoardFrame();
        if (frame != null)
        {
            JOptionPane.showMessageDialog(frame, message);
        }
    }

    // TODO Move legion markers to slayer on client side.
    public void tellPlayerElim(String playerName, String slayerName)
    {
        Log.debug(this.playerName + " tellPlayerElim(" + playerName +
            ", " + slayerName + ")");
        PlayerInfo info = getPlayerInfo(playerName);

        // TODO Merge these
        info.setDead(true);
        info.removeAllLegions();

        predictSplits[getPlayerNum(playerName)] = null;
    }

    public void tellGameOver(String message)
    {
        gameOver = true;
        if (board != null)
        {
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
            List legions = getLegionsByHex(hexLabel);
            if (legions.size() != 1)
            {
                Log.error("Not exactly one legion in donor hex");
                return;
            }
            String markerId = (String)legions.get(0);
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
            answerConcede(allyMarkerId, ai.concede(getLegionInfo(
                allyMarkerId), getLegionInfo(enemyMarkerId)));
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
            negotiate = new Negotiate(this, attackerMarkerId,
                defenderMarkerId);
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

    BattleHex getBattleHex(BattleChit chit)
    {
        return HexMap.getHexByLabel(getBattleTerrain(),
            chit.getCurrentHexLabel());
    }

    BattleHex getStartingBattleHex(BattleChit chit)
    {
        return HexMap.getHexByLabel(getBattleTerrain(),
            chit.getStartingHexLabel());
    }

    boolean isOccupied(BattleHex hex)
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
        int strikeNumber, List rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft, Set carryTargetDescriptions)
    {
        BattleChit chit = getBattleChit(strikerTag);
        if (chit != null)
        {
            chit.setStruck(true);
        }

        BattleChit targetChit = getBattleChit(targetTag);
        if (battleDice != null)
        {
            battleDice.setValues(getBattleChitDescription(chit),
                getBattleChitDescription(targetChit), strikeNumber,
                rolls);
            battleDice.showRoll();
        }
        if (map != null)
        {
            map.unselectAllHexes();
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
        Log.error(playerName + " got nak for " + reason + " " + errmsg);
        recoverFromNak(reason, errmsg);
    }

    private void recoverFromNak(String reason, String errmsg)
    {
        Log.debug(playerName + " recoverFromNak " + reason + " " + errmsg);
        if (reason == null)
        {
            Log.error("recoverFromNak with null reason!");
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
            Log.warn(playerName + " unexpected nak " + reason + " " + errmsg);
        }
    }

    private void pickCarries(int carryDamage, Set carryTargetDescriptions)
    {
        if (!isMyBattlePhase())
        {
            return;
        }

        if (carryDamage < 1 || carryTargetDescriptions.isEmpty())
        {
            leaveCarryMode();
        }
        else if (carryTargetDescriptions.size() == 1 &&
            getOption(Options.autoCarrySingle))
        {
            Iterator it = carryTargetDescriptions.iterator();
            String desc = (String)it.next();
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
                new PickCarry(map.getFrame(), this, carryDamage,
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

        getLegionInfo(defenderMarkerId).setEntrySide((getLegionInfo(
            attackerMarkerId).getEntrySide() + 3) % 6);

        if (board != null)
        {
            map = new BattleMap(this, masterHexLabel, attackerMarkerId,
                defenderMarkerId);
            JFrame frame = map.getFrame();
            battleDice = new BattleDice();
            frame.getContentPane().add(battleDice, BorderLayout.SOUTH);
            frame.pack();
            frame.setVisible(true);
            focusMap();
        }
    }

    public void cleanupBattle()
    {
        Log.debug(playerName + " Client.cleanupBattle()");
        if (map != null)
        {
            map.dispose();
            map = null;
        }
        battleChits.clear();
        battlePhase = null;
        battleTurnNumber = -1;
        battleActivePlayerName = null;
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
            if (findEngagements().isEmpty() && board != null )
            {
                board.enableBottomBarDoneButton();
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

        if (info == null || !info.canRecruit() || !isMyTurn() ||
            !isMyLegion(markerId))
        {
            return;
        }

        String hexLabel = getHexForLegion(markerId);
        List recruits = findEligibleRecruits(markerId, hexLabel);
        String hexDescription =
            MasterBoard.getHexByLabel(hexLabel).getDescription();

        String recruitName = PickRecruit.pickRecruit(board.getFrame(),
            recruits, hexDescription, markerId, this);

        if (recruitName == null)
        {
            return;
        }

        String recruiterName = findRecruiterName(hexLabel, markerId,
            recruitName, hexDescription);
        if (recruiterName == null)
        {
            return;
        }

        doRecruit(markerId, recruitName, recruiterName);
    }

    void doRecruit(String markerId, String recruitName, String recruiterName)
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

            List recruits = findEligibleRecruits(markerId, hexLabel);
            String hexDescription =
                MasterBoard.getHexByLabel(hexLabel).getDescription();

            String recruitName = PickRecruit.pickRecruit(board.getFrame(),
                recruits, hexDescription, markerId, this);

            String recruiterName = null;
            if (recruitName != null)
            {
                recruiterName = findRecruiterName(hexLabel, markerId,
                    recruitName, hexDescription);
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
            Log.error("Client.didRecruit() null hexLabel for " + markerId);
        }
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }

        if (numRecruiters >= 1 && recruiterName != null)
        {
            List recruiters = new ArrayList();
            for (int i = 0; i < numRecruiters; i++)
            {
                recruiters.add(recruiterName);
            }
            revealCreatures(markerId, recruiters);
        }
        addCreature(markerId, recruitName);
        getLegionInfo(markerId).setRecruited(true);
        getLegionInfo(markerId).setLastRecruit(recruitName);

        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            addRecruitChit(recruitName, hexLabel);
            hex.repaint();
            board.highlightPossibleRecruits();
        }
    }

    public void undidRecruit(String markerId, String recruitName)
    {
        String hexLabel = getHexForLegion(markerId);
        removeCreature(markerId, recruitName);
        getLegionInfo(markerId).setRecruited(false);
        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            removeRecruitChit(hexLabel);
            hex.repaint();
            board.highlightPossibleRecruits();
        }
    }

    /** null means cancel.  "none" means no recruiter (tower creature). */
    private String findRecruiterName(String hexLabel, String markerId, String
        recruitName, String hexDescription)
    {
        String recruiterName = null;

        List recruiters = findEligibleRecruiters(markerId,
            recruitName);

        int numEligibleRecruiters = recruiters.size();
        if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiterName = "none";
        }
        else if (getOption(Options.autoPickRecruiter) ||
            numEligibleRecruiters == 1)
        {
            // If there's only one possible recruiter, or if
            // the user has chosen the autoPickRecruiter option,
            // then just reveal the first possible recruiter.
            recruiterName = (String)recruiters.get(0);
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
    }

    private void resetAllMoves()
    {
        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
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

    public void setupSplit(String activePlayerName, int turnNumber)
    {
        clearUndoStack();
        cleanupNegotiationDialogs();

        this.activePlayerName = activePlayerName;
        this.turnNumber = turnNumber;
        this.phase = Constants.Phase.SPLIT;

        numSplitsThisTurn = 0;

        resetAllMoves();

        if (board != null)
        {
            disposeMovementDie();
            board.setupSplitMenu();
            board.fullRepaint();  // Ensure that movement die goes away
            if (isMyTurn())
            {
                if ( turnNumber == 1 )
                {
                    board.disableBottomBarDoneButton();
                }
                focusBoard();
                defaultCursor();
                if (!getOption(Options.autoSplit) &&
                    (getPlayerInfo().getMarkersAvailable().size() < 1 ||
                    findTallLegionHexes(4).isEmpty()))
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
                if (!getOption(Options.autoRecruit) &&
                    getPossibleRecruitHexes().isEmpty())
                {
                    doneWithRecruits();
                }
            }
        }
        updateStatusScreen();

        if (getOption(Options.autoRecruit) && !isGameOver())
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

        if (map != null)
        {
            if (isMyBattlePhase())
            {
                focusMap();
                map.setupSummonMenu();
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

        if (map != null)
        {
            if (isMyBattlePhase())
            {
                focusMap();
                map.setupRecruitMenu();
            }
        }
        updateStatusScreen();
    }

    private void resetAllBattleMoves()
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
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
        if (map != null && isMyBattlePhase())
        {
            focusMap();
            defaultCursor();
            map.setupMoveMenu();
        }
        updateStatusScreen();
        if (isMyBattlePhase() && getOption(Options.autoPlay))
        {
            bestMoveOrder = ai.battleMove();
            failedBattleMoves = new ArrayList();
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
            Iterator it = bestMoveOrder.iterator();
            CritterMove cm = (CritterMove)it.next();
            tryBattleMove(cm);
        }
    }

    void tryBattleMove(CritterMove cm)
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

        if (map != null)
        {
            if (isMyBattlePhase())
            {
                focusMap();
                defaultCursor();
            }
            else
            {
                waitCursor();
            }
            map.setupFightMenu();
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

        Iterator it = legionInfo.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            LegionInfo info = (LegionInfo)entry.getValue();
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
        return Player.getShortColor(getColor());
    }

    // public for IOracle
    public String getBattleActivePlayerName()
    {
        return battleActivePlayerName;
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
            Iterator it = bestMoveOrder.iterator();
            while (it.hasNext())
            {
                CritterMove cm = (CritterMove)it.next();
                if (tag == cm.getTag() &&
                    endingHexLabel.equals(cm.getEndingHexLabel()))
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
        Log.debug(playerName + "handleFailedBattleMove");
        if (bestMoveOrder != null)
        {
            Iterator it = bestMoveOrder.iterator();
            if (it.hasNext())
            {
                CritterMove cm = (CritterMove)it.next();
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
        if (map != null)
        {
            map.alignChits(startingHexLabel);
            map.alignChits(endingHexLabel);
            map.repaint();
            map.highlightMobileCritters();
        }
    }

    /** Attempt to have critter tag strike the critter in hexLabel. */
    void strike(int tag, String hexLabel)
    {
        server.strike(tag, hexLabel);
    }

    /** Attempt to apply carries to the critter in hexLabel. */
    void applyCarries(String hexLabel)
    {
        server.applyCarries(hexLabel);
        if (map != null)
        {
            map.unselectHexByLabel(hexLabel);
            map.repaint();
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

    String getBattleTerrain()
    {
        MasterHex mHex = MasterBoard.getHexByLabel(battleSite);
        return mHex.getTerrain();
    }

    /** Return true if there are any enemies adjacent to this chit.
     *  Dead critters count as being in contact only if countDead is true. */
    boolean isInContact(BattleChit chit, boolean countDead)
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
                    if (other != null &&
                        (other.isInverted() != chit.isInverted()) &&
                        (countDead || !other.isDead()))
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
        return battleActivePlayerName.equals(getPlayerNameByTag(
            chit.getTag()));
    }

    /** Return a set of hexLabels. */
    Set findMobileCritterHexes()
    {
        Set set = new HashSet();
        Iterator it = getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (!chit.hasMoved() && !isInContact(chit, false))
            {
                set.add(chit.getCurrentHexLabel());
            }
        }
        return set;
    }

    /** Return a set of BattleChits. */
    Set findMobileBattleChits()
    {
        Set set = new HashSet();
        Iterator it = getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (!chit.hasMoved() && !isInContact(chit, false))
            {
                set.add(chit);
            }
        }
        return set;
    }

    /** Return a set of hexLabels. */
    Set showBattleMoves(int tag)
    {
        return battleMovement.showMoves(tag);
    }

    /** Return a set of hexLabels. */
    Set findCrittersWithTargets()
    {
        return strike.findCrittersWithTargets();
    }

    /** Return a set of hexLabels. */
    Set findStrikes(int tag)
    {
        return strike.findStrikes(tag);
    }

    String getPlayerNameByTag(int tag)
    {
        BattleChit chit = getBattleChit(tag);
        if (chit == null)
        {
            return "???";
        }
        if (chit.isInverted())
        {
            return getPlayerNameByMarkerId(defenderMarkerId);
        }
        else
        {
            return getPlayerNameByMarkerId(attackerMarkerId);
        }
    }

    boolean isMyCritter(int tag)
    {
        return (playerName.equals(getPlayerNameByTag(tag)));
    }

    // public for IOracle
    public String getActivePlayerName()
    {
        return activePlayerName;
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
        List lords = listTeleportingLords(moverId, hexLabel);
        String lordName = null;
        switch (lords.size())
        {
            case 0:
                return null;

            case 1:
                lordName = (String)lords.get(0);
                if (lordName.startsWith(Constants.titan))
                {
                    lordName = Constants.titan;
                }
                return lordName;

            default:
                if (getOption(Options.autoPickLord))
                {
                    lordName = (String)lords.get(0);
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
    private List listTeleportingLords(String moverId,
        String hexLabel)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        List lords = new ArrayList();

        LegionInfo info = getLegionInfo(moverId);

        // Titan teleport
        List legions = getLegionsByHex(hexLabel);
        if (!legions.isEmpty())
        {
            String markerId = (String)legions.get(0);
            if (markerId != null && !isMyLegion(markerId) && info.hasTitan())
            {
                lords.add(info.getTitanBasename());
            }
        }

        // Tower teleport
        else
        {
            Iterator it = info.getContents().iterator();
            while (it.hasNext())
            {
                String name = (String)it.next();
                Creature creature = Creature.getCreatureByName(name);
                if (creature != null && creature.isLord() &&
                    !lords.contains(name))
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
    boolean doMove(String moverId, String hexLabel)
    {
        if (moverId == null)
        {
            return false;
        }

        boolean teleport = false;

        Set teleports = listTeleportMoves(moverId);
        Set normals = listNormalMoves(moverId);
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

        Set entrySides = listPossibleEntrySides(moverId, hexLabel, teleport);

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
                activePlayerName);
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
        return (entrySide != null && (entrySide.equals(Constants.left) ||
            entrySide.equals(Constants.bottom) ||
            entrySide.equals(Constants.right)));
    }

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel, String entrySide, boolean teleport,
        boolean splitLegionHasForcedMove)
    {
        removeRecruitChit(startingHexLabel);
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }
        getLegionInfo(markerId).setHexLabel(currentHexLabel);
        getLegionInfo(markerId).setMoved(true);
        getLegionInfo(markerId).setEntrySide(
            BattleMap.entrySideNum(entrySide));
        if (teleport)
        {
            getLegionInfo(markerId).setTeleported(true);
        }
        if (board != null)
        {
            board.alignLegions(startingHexLabel);
            board.alignLegions(currentHexLabel);
            board.highlightUnmovedLegions();
            board.repaint();
            if ( isMyLegion(markerId) && ! splitLegionHasForcedMove )
            {
                board.enableBottomBarDoneButton();
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
        getLegionInfo(markerId).setTeleported(false);
        if (board != null)
        {
            board.alignLegions(formerHexLabel);
            board.alignLegions(currentHexLabel);
            board.highlightUnmovedLegions();
            if ( isUndoStackEmpty() || splitLegionHasForcedMove )
            {
                board.disableBottomBarDoneButton();
            }
        }
    }

    /** Return a list of Creatures. */
    List findEligibleRecruits(String markerId, String hexLabel)
    {
        List recruits = new ArrayList();

        LegionInfo info = getLegionInfo(markerId);
        if (info == null)
        {
            return recruits;
        }

        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        if (hex == null)
        {
            Log.warn("null hex in Client.findEligibleRecruits()");
            Log.warn("hexLabel is " + hexLabel);
            return recruits;
        }
        String terrain = hex.getTerrain();

        List tempRecruits =
            TerrainRecruitLoader.getPossibleRecruits(terrain, hexLabel);
        List recruiters =
            TerrainRecruitLoader.getPossibleRecruiters(terrain, hexLabel);

        Iterator lit = tempRecruits.iterator();
        while (lit.hasNext())
        {
            Creature creature = (Creature)lit.next();
            Iterator liter = recruiters.iterator();
            while (liter.hasNext())
            {
                Creature lesser = (Creature)liter.next();
                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser,
                    creature, terrain, hexLabel) <=
                    info.numCreature(lesser)) &&
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
            if (getCreatureCount(recruit) < 1)
            {
                it.remove();
            }
        }

        return recruits;
    }

    /** Return a list of creature name strings. */
    List findEligibleRecruiters(String markerId, String recruitName)
    {
        java.util.Set recruiters;
        Creature recruit = Creature.getCreatureByName(recruitName);
        if (recruit == null)
        {
            return new ArrayList();
        }

        LegionInfo info = getLegionInfo(markerId);
        String hexLabel = info.getHexLabel();
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        String terrain = hex.getTerrain();

        recruiters =
            new HashSet(TerrainRecruitLoader.getPossibleRecruiters(terrain,
            hexLabel));
        Iterator it = recruiters.iterator();
        while (it.hasNext())
        {
            Creature possibleRecruiter = (Creature)it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain, hexLabel);
            if (needed < 1 || needed > info.numCreature(possibleRecruiter))
            {
                // Zap this possible recruiter.
                it.remove();
            }
        }

        List strings = new ArrayList();
        it = recruiters.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            strings.add(creature.getName());
        }
        return strings;
    }

    /** Return a set of hexLabels. */
    Set getPossibleRecruitHexes()
    {
        Set set = new HashSet();

        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
            if (activePlayerName.equals(info.getPlayerName()) &&
                info.canRecruit())
            {
                set.add(info.getHexLabel());
            }
        }
        return set;
    }

    /** Return a set of hexLabels for all other unengaged legions of
     *  markerId's player that have summonables.
     * public for client-side AI -- do not call from server side. */
    Set findSummonableAngelHexes(String summonerId)
    {
        Set set = new HashSet();
        LegionInfo summonerInfo = getLegionInfo(summonerId);
        String pName = summonerInfo.getPlayerName();
        Iterator it = getLegionsByPlayer(pName).iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
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

    Movement getMovement()
    {
        return movement;
    }

    Strike getStrike()
    {
        return strike;
    }

    /** Return a set of hexLabels. */
    Set listTeleportMoves(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        MasterHex hex = MasterBoard.getHexByLabel(info.getHexLabel());
        return movement.listTeleportMoves(info, hex, movementRoll);
    }

    /** Return a set of hexLabels. */
    Set listNormalMoves(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        MasterHex hex = MasterBoard.getHexByLabel(info.getHexLabel());
        return movement.listNormalMoves(info, hex, movementRoll);
    }

    Set listPossibleEntrySides(String moverId, String hexLabel,
        boolean teleport)
    {
        return movement.listPossibleEntrySides(moverId, hexLabel, teleport);
    }

    int getCreatureCount(String creatureName)
    {
        return caretakerInfo.getCount(creatureName);
    }

    int getCreatureCount(Creature creature)
    {
        return caretakerInfo.getCount(creature);
    }

    int getCreatureDeadCount(String creatureName)
    {
        return caretakerInfo.getDeadCount(creatureName);
    }

    int getCreatureDeadCount(Creature creature)
    {
        return caretakerInfo.getDeadCount(creature);
    }

    int getCreatureMaxCount(String creatureName)
    {
        return caretakerInfo.getMaxCount(creatureName);
    }

    int getCreatureMaxCount(Creature creature)
    {
        return caretakerInfo.getMaxCount(creature);
    }

    /** Returns a list of markerIds. */
    List getLegionsByHex(String hexLabel)
    {
        List markerIds = new ArrayList();
        Iterator it = legionInfo.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            LegionInfo info = (LegionInfo)entry.getValue();
            if (info != null && info.getHexLabel() != null &&
                hexLabel != null && hexLabel.equals(info.getHexLabel()))
            {
                markerIds.add(info.getMarkerId());
            }
        }
        return markerIds;
    }

    /** Returns a list of markerIds. */
    List getLegionsByPlayer(String name)
    {
        List markerIds = new ArrayList();
        Iterator it = legionInfo.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            LegionInfo info = (LegionInfo)entry.getValue();
            if (name.equals(info.getPlayerName()))
            {
                markerIds.add(info.getMarkerId());
            }
        }
        return markerIds;
    }

    Set findUnmovedLegionHexes()
    {
        Set set = new HashSet();

        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
            if (!info.hasMoved() &&
                getActivePlayerName().equals(info.getPlayerName()))
            {
                set.add(info.getHexLabel());
            }
        }
        return set;
    }

    /** Return a set of hexLabels for the active player's legions with
     *  7 or more creatures. */
    Set findTallLegionHexes()
    {
        return findTallLegionHexes(7);
    }

    /** Return a set of hexLabels for the active player's legions with
     *  minHeight or more creatures. */
    Set findTallLegionHexes(int minHeight)
    {
        Set set = new HashSet();

        Iterator it = legionInfo.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            LegionInfo info = (LegionInfo)entry.getValue();
            if (info.getHeight() >= minHeight &&
                activePlayerName.equals(info.getPlayerName()))
            {
                set.add(info.getHexLabel());
            }
        }
        return set;
    }

    /** Return a set of hexLabels for all hexes with engagements. */
    Set findEngagements()
    {
        Set set = new HashSet();
        Iterator it = MasterBoard.getAllHexLabels().iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            List markerIds = getLegionsByHex(hexLabel);
            if (markerIds.size() == 2)
            {
                String marker0 = (String)markerIds.get(0);
                LegionInfo info0 = getLegionInfo(marker0);
                String pName0 = info0.getPlayerName();

                String marker1 = (String)markerIds.get(1);
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
        List markerIds = getLegionsByHex(hexLabel);
        if (markerIds.size() == 2)
        {
            String marker0 = (String)markerIds.get(0);
            LegionInfo info0 = getLegionInfo(marker0);
            String pName0 = info0.getPlayerName();

            String marker1 = (String)markerIds.get(1);
            LegionInfo info1 = getLegionInfo(marker1);
            String pName1 = info1.getPlayerName();

            return !pName0.equals(pName1);
        }
        return false;
    }

    List getEnemyLegions(String pName)
    {
        List markerIds = new ArrayList();
        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
            String markerId = info.getMarkerId();
            if (!pName.equals(info.getPlayerName()))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    List getEnemyLegions(String hexLabel, String pName)
    {
        List markerIds = new ArrayList();
        List legions = getLegionsByHex(hexLabel);
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            if (!pName.equals(getPlayerNameByMarkerId(markerId)))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    String getFirstEnemyLegion(String hexLabel, String pName)
    {
        List markerIds = getEnemyLegions(hexLabel, pName);
        if (markerIds.isEmpty())
        {
            return null;
        }
        return (String)markerIds.get(0);
    }

    int getNumEnemyLegions(String hexLabel, String pName)
    {
        return getEnemyLegions(hexLabel, pName).size();
    }

    List getFriendlyLegions(String pName)
    {
        List markerIds = new ArrayList();
        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
            String markerId = info.getMarkerId();
            if (pName.equals(info.getPlayerName()))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    List getFriendlyLegions(String hexLabel, String pName)
    {
        List markerIds = new ArrayList();
        List legions = getLegionsByHex(hexLabel);
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            if (pName.equals(getPlayerNameByMarkerId(markerId)))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    String getFirstFriendlyLegion(String hexLabel, String pName)
    {
        List markerIds = getFriendlyLegions(hexLabel, pName);
        if (markerIds.isEmpty())
        {
            return null;
        }
        return (String)markerIds.get(0);
    }

    int getNumFriendlyLegions(String hexLabel, String pName)
    {
        return getFriendlyLegions(hexLabel, pName).size();
    }

    void newGame()
    {
        clearUndoStack();
        server.newGame();
    }

    void loadGame(String filename)
    {
        clearUndoStack();
        server.loadGame(filename);
    }

    void saveGame(String filename)
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
    void undoSplit(String splitoffId)
    {
        server.undoSplit(splitoffId);
        getPlayerInfo().addMarkerAvailable(splitoffId);
        numSplitsThisTurn--;
        if ( turnNumber == 1 && numSplitsThisTurn == 0 )
        {
            // must split in first turn - Done not allowed now
            if ( board != null )
            {
                board.disableBottomBarDoneButton();
            }
        }
        Log.debug("called server.undoSplit");
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
        info.merge(splitoffId, turn);
        removeLegion(splitoffId);
        if (board != null)
        {
            board.alignLegions(info.getHexLabel());
            board.highlightTallLegions();
        }
        if (isMyTurn() && getOption(Options.autoSplit) && !isGameOver())
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

    String getPlayerNameByMarkerId(String markerId)
    {
        String shortColor = markerId.substring(0, 2);
        return getPlayerNameForShortColor(shortColor);
    }

    String getPlayerNameForShortColor(String shortColor)
    {
        if (playerInfo == null)
        {
            return null;
        }
        PlayerInfo info = null;

        // Stage 1: See if the player who started with this color is alive.
        for (int i = 0; i < playerInfo.length; i++)
        {
            info = playerInfo[i];
            if (shortColor.equals(info.getShortColor()) && !info.isDead())
            {
                return info.getName();
            }
        }

        // Stage 2: He's dead.  Find who killed him and see if he's alive.
        for (int i = 0; i < playerInfo.length; i++)
        {
            info = playerInfo[i];
            if (info.getPlayersElim().indexOf(shortColor) != -1)
            {
                // We have the killer.
                if (!info.isDead())
                {
                    return info.getName();
                }
                else
                {
                    return getPlayerNameForShortColor(info.getShortColor());
                }
            }
        }
        return null;
    }

    String getColorByMarkerId(String markerId)
    {
        String playerName = getPlayerNameByMarkerId(markerId);
        PlayerInfo info = getPlayerInfo(playerName);
        return info.getColor();
    }

    boolean isMyLegion(String markerId)
    {
        return (playerName.equals(getPlayerNameByMarkerId(markerId)));
    }

    boolean isMyTurn()
    {
        return playerName.equals(getActivePlayerName());
    }

    boolean isMyBattlePhase()
    {
        return playerName.equals(getBattleActivePlayerName());
    }

    int getMovementRoll()
    {
        return movementRoll;
    }

    int getMulligansLeft()
    {
        PlayerInfo info = getPlayerInfo(playerName);
        return info.getMulligansLeft();
    }

    void doSplit(String parentId)
    {
        Log.debug("Client.doSplit " + parentId);
        this.parentId = null;

        if (!isMyTurn())
        {
            Log.error("Not my turn!");
            kickSplit();
            return;
        }
        // Can't split other players' legions.
        if (!isMyLegion(parentId))
        {
            Log.error("Not my legion!");
            kickSplit();
            return;
        }
        Set markersAvailable = getPlayerInfo().getMarkersAvailable();
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
            new PickMarker(board.getFrame(), playerName, markersAvailable,
                this);
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
    void doSplit(String parentId, String childId, String results)
    {
        Log.debug("Client.doSplit " + parentId + " " + childId + " " +
            results);
        server.doSplit(parentId, childId, results);
    }

    /** Callback from server after any successful split. */
    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight, List splitoffs, int turn)
    {
        Log.debug("Client.didSplit " + hexLabel + " " + parentId + " " +
            childId + " " + childHeight + " " + turn);

        LegionInfo parentInfo = getLegionInfo(parentId);
        LegionInfo childInfo = createLegionInfo(childId);
        parentInfo.split(childHeight, childId, turn);

        childInfo.setHexLabel(hexLabel);

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
        if ( turnNumber == 1 && board != null )
        {
            board.enableBottomBarDoneButton();
        }

        if (board != null)
        {
            board.alignLegions(hexLabel);
            board.highlightTallLegions();
        }
        if (isMyTurn() && getOption(Options.autoSplit) && !isGameOver())
        {
            boolean done = ai.splitCallback(parentId, childId);
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    public void askPickColor(List colorsLeft)
    {
        String color = null;
        if (getOption(Options.autoPickColor))
        {
            // Convert favorite colors from a comma-separated string to a list.
            String favorites = getStringOption(Options.favoriteColors);
            List favoriteColors = null;
            if (favorites != null)
            {
                favoriteColors = Split.split(',', favorites);
            }
            else
            {
                favoriteColors = new ArrayList();
            }
            color = ai.pickColor(colorsLeft, favoriteColors);
        }
        else
        {
            do
            {
                color = PickColor.pickColor(board.getFrame(), playerName,
                    colorsLeft, this);
            }
            while (color == null);
        }

        setColor(color);

        server.assignColor(color);
    }

    public void askPickFirstMarker()
    {
        Set markersAvailable = getPlayerInfo().getMarkersAvailable();
        if (getOption(Options.autoPickMarker))
        {
            String markerId = ai.pickMarker(markersAvailable, getShortColor());
            pickMarkerCallback(markerId);
        }
        else
        {
            new PickMarker(board.getFrame(), playerName, markersAvailable,
                this);
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
            UIManager.LookAndFeelInfo[] lfInfo =
                UIManager.getInstalledLookAndFeels();
            boolean exist = false;
            for (int i = 0; i < lfInfo.length; i++)
            {
                exist = exist || lfInfo[i].getClassName().equals(lfName);
            }
            if (!exist)
            {
                UIManager.installLookAndFeel(
                    new UIManager.LookAndFeelInfo(
                    UIManager.getLookAndFeel().getName(), lfName));
            }
            updateEverything();
            Log.debug("Switched to Look & Feel: " + lfName);
            setOption(Options.favoriteLookFeel, lfName);
        }
        catch (Exception e)
        {
            Log.error("Look & Feel " + lfName + " not usable (" + e + ")");
        }
    }

    private void updateEverything()
    {
        if (board != null)
        {
            board.updateComponentTreeUI();
            board.pack();
        }
        if (statusScreen != null)
        {
            SwingUtilities.updateComponentTreeUI(statusScreen);
            statusScreen.pack();
        }
        if (caretakerDisplay != null)
        {
            SwingUtilities.updateComponentTreeUI(caretakerDisplay);
            caretakerDisplay.pack();
        }
        repaintAllWindows();
    }

    public void log(String message)
    {
        Log.event(message);
    }

    public static String getVersion()
    {
        byte[] bytes = new byte[8];  // length of an ISO date
        String version = "unknown";
        try
        {
            ClassLoader cl = Client.class.getClassLoader();
            InputStream is = cl.getResourceAsStream("version");
            is.read(bytes);
            version = new String(bytes, 0, bytes.length);
        }
        catch (Exception ex)
        {
            Log.error("Problem reading version file " + ex);
        }
        return version;
    }

    boolean testBattleMove(BattleChit chit, String hexLabel)
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
        Log.debug("Called setType for " + aType);
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
        Log.debug("final type: " + type);
        if (type.endsWith("AI"))
        {
            Log.debug("new type is AI. current ai is " +
                ai.getClass().getName());
            if (!(ai.getClass().getName().equals(type)))
            {
                Log.debug("need to change type");
                Log.event("Changing client " + playerName + " from " +
                    ai.getClass().getName() + " to " + type);
                try
                {
                    Class[] classArray = new Class[1];
                    classArray[0] = Class.forName(
                        "net.sf.colossus.client.Client");
                    Object[] objArray = new Object[1];
                    objArray[0] = this;
                    ai = (AI)Class.forName(type).getDeclaredConstructor(
                        classArray).newInstance(objArray);
                }
                catch (Exception ex)
                {
                    Log.error("Failed to change client " + playerName +
                        " from " + ai.getClass().getName() + " to " + type, ex);
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
            Log.error("Client.aiPause() " + ex.toString());
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
            if (caretakerDisplay != null)
            {
                caretakerDisplay.dispose();
            }
            caretakerDisplay = null;
            updateCreatureCountDisplay();
        }
    }

    private void focusMap()
    {
        if (map != null)
        {
            map.reqFocus();
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
        List creatureNames)
    {
        if (predictSplits == null)
        {
            predictSplits = new PredictSplits[numPlayers];
        }
        int playerNum = getPlayerNum(pName);
        predictSplits[playerNum] = new PredictSplits(pName, rootMarkerId,
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

    class MarkerComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            String shortColor = "None";  // In case not initialized yet.
            if (color != null)
            {
                shortColor = getShortColor();
            }
            if (!(o1 instanceof String) || !(o2 instanceof String))
            {
                throw new ClassCastException();
            }
            String s1 = (String)o1;
            String s2 = (String)o2;
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
}
