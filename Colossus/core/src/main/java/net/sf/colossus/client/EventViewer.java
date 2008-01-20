package net.sf.colossus.client;


import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import net.sf.colossus.game.Legion;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


/**
 * Event Revealing dialog.
 *
 * It collects all revealed events and displays all or
 * only the recent ones of them.
 *
 * @version $Id$
 * @author Clemens Katzer
 */

/*
 * @TODO:
 * @IMPORTANT
 * - Check compatibility with previous version
 * - History: make history events carry the "reason" tag so that reloaded
 *    games have same event view as in running game     
 *  "TODO: investigate, why does server gives us different turn numbers
 *         in those Split/undoSplit events?" (=> see undoEvent)
 *  
 * @Nice to have:
 * - Player dead events
 * - Legion eliminated (when player dead) events
 * - Game over, who wins event :)
 * - scale change: own setting, relative to main board scale or absolut?
 *       react more quickly when scale changed? 
 * - checkbox "don't show Dead creatures"
 * - RevealedCreature could cache the panels
 * - combined "battle event" - one bordered panel, containing
 *    all relevant events ( [teleport?], summon, acquire, won, lost)
 *     plus text info what now in Engagement result window 
 * - interface for inspector & Co: for viewMode return info revealed during
 *     one last turn
 * - Engagement won (fled...), instead of "not revealed" show contents 
 *     depending on what the viewMode says
 * - "SolidMarker" instead of Titan for mulligan
 * 
 */

final class EventViewer extends KDialog implements WindowListener,
    ItemListener, ActionListener
{
    private static final Logger LOGGER = Logger.getLogger(EventViewer.class
        .getName());

    private final static String windowTitle = "Event Viewer";

    private IOptions options;
    private Client client;
    private SaveWindow saveWindow;

    private boolean visible;

    private final List<RevealEvent> syncdEventList = Collections
        .synchronizedList(new ArrayList<RevealEvent>());
    private int bookmark = 0;
    final private ArrayList<JPanel> displayQueue = new ArrayList<JPanel>();

    private int turnNr;
    private int playerNr;

    // how long back are they kept (from global settings)
    private int expireTurns;
    private String maxString;

    private Container eventPane;
    private Box settingsPane;
    private JScrollPane eventScrollPane;
    private JScrollBar eventScrollBar;

    // Event Viewer Filter (evf) settings (= option names):
    public static final String evfSplit = "Split events";
    public static final String evfRecruit = "Recruit events";
    public static final String evfSummon = "Summon events";
    public static final String evfTeleport = "Teleport events";
    public static final String evfAcquire = "Acquire events";
    public static final String evfWon = "Engagement won events";
    public static final String evfLoser = "Engagement lost events";
    public static final String evfMulligan = "Mulligans";
    public static final String evfMoveRoll = "Movement rolls";

    public static final String evfTurnChange = "Turn change info";
    public static final String evfPlayerChange = "Player change info";

    public static final String evAutoScroll = "Auto-scroll to end";
    public static final String evHideUndone = "Hide undone events";
    public static final String evMaxTurns = "Maximum number of turns to display";

    // boolean flags for them as local flags for quick access:
    // Index for this are the EventXXX constants in RevealEvent.java.
    private final boolean[] showEventType;

    private boolean autoScroll;
    private boolean hideUndoneEvents;
    private JComboBox maxTurnsDisplayExpiringBox;

    // how many back are currently displayed
    private int maxTurns = 1;

    private int mulliganOldRoll = -2;

    private Legion attacker;
    private Legion defender;

    private RevealEvent attackerEventLegion = null;
    private RevealEvent defenderEventLegion = null;

    private RevealEvent lastAttackerEventLegion = null;
    private RevealEvent lastDefenderEventLegion = null;

    private RevealEvent winnerLegion = null;
    private RevealEvent loserLegion = null;

    /** 
     * Inits the dialog, not necessarily displays it.
     * 
     * @param frame is the parent window frame (MasterBoard)
     * @param options IOptions reference to the client
     * @param client The client, needed to ask all kind of info  
     */

    EventViewer(final JFrame frame, final IOptions options, Client client)
    {
        super(frame, windowTitle, false);
        setFocusable(false);

        this.options = options;
        this.client = client;

        initExpireTurnsFromOptions();

        showEventType = new boolean[RevealEvent.NUMBEROFEVENTS];
        showEventType[RevealEvent.eventRecruit] = getBoolOption(evfRecruit,
            true);
        showEventType[RevealEvent.eventSplit] = getBoolOption(evfSplit, true);
        showEventType[RevealEvent.eventTeleport] = getBoolOption(evfTeleport,
            true);
        showEventType[RevealEvent.eventSummon] = getBoolOption(evfSummon, true);
        showEventType[RevealEvent.eventAcquire] = getBoolOption(evfAcquire,
            true);
        showEventType[RevealEvent.eventWon] = getBoolOption(evfWon, true);
        showEventType[RevealEvent.eventLost] = getBoolOption(evfLoser, true);

        showEventType[RevealEvent.eventMulligan] = getBoolOption(evfMulligan,
            true);
        showEventType[RevealEvent.eventMoveRoll] = getBoolOption(evfMoveRoll,
            true);

        showEventType[RevealEvent.eventTurnChange] = getBoolOption(
            evfTurnChange, true);
        showEventType[RevealEvent.eventPlayerChange] = getBoolOption(
            evfPlayerChange, false);

        autoScroll = getBoolOption(evAutoScroll, true);
        hideUndoneEvents = getBoolOption(evHideUndone, false);

        setupGUI();

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                options.setOption(Options.showEventViewer, false);
            }
        });

        setVisibleMaybe();
    }

    // How many turns back data is kept; default to 1 if no such
    // option found from user options file.
    private void initExpireTurnsFromOptions()
    {
        int turnsToKeep = 1;
        String expOption = options.getStringOption(Options.eventExpiring);
        if (expOption != null)
        {
            if (expOption.equals(Options.eventExpiringNever))
            {
                turnsToKeep = -1;
            }
            else
            {
                int exp;
                try
                {
                    exp = Integer.parseInt(expOption);
                    if (exp > 0 && exp < 9999)
                    {
                        turnsToKeep = exp;
                    }
                    else
                    {
                        LOGGER.log(Level.SEVERE, "Invalid value " + exp
                            + " from option '" + Options.eventExpiring
                            + "' - using default " + turnsToKeep);
                    }
                }
                catch (NumberFormatException e)
                {
                    LOGGER.log(Level.SEVERE, "Invalid value " + expOption
                        + " from option '" + Options.eventExpiring
                        + "' - using default " + turnsToKeep);
                }
            }
        }
        this.expireTurns = turnsToKeep;
    }

    private boolean getBoolOption(String name, boolean defaultVal)
    {
        boolean bVal = defaultVal;

        String sVal = options.getStringOption(name);
        if (sVal == null)
        {
            options.setOption(name, defaultVal);
        }
        else
        {
            bVal = sVal.equals("true");
        }

        return bVal;
    }

    private void addCheckbox(String optname, Container pane)
    {
        JCheckBox cb = new JCheckBox(optname);
        boolean selected = getBoolOption(optname, true);
        cb.setSelected(selected);
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setAlignmentY(Component.TOP_ALIGNMENT);
        cb.addItemListener(this);
        pane.add(cb);
    }

    private void setupGUI()
    {
        // A tabbed pane, one tab the events, one tab the settings
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(270, 520));

        // Events:
        Box eventTabPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Events", eventTabPane);
        eventTabPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        eventTabPane.setPreferredSize(new Dimension(250, 500));
        eventTabPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        eventPane = new Box(BoxLayout.Y_AXIS);
        eventPane.add(Box.createVerticalGlue());
        eventScrollPane = new JScrollPane(eventPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        eventScrollBar = eventScrollPane.getVerticalScrollBar();
        eventTabPane.add(eventScrollPane);

        // The settings:
        settingsPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Settings", settingsPane);

        JPanel checkboxPane = new JPanel(new GridLayout(0, 1));
        checkboxPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkboxPane.setBorder(new TitledBorder("Event Filter"));
        checkboxPane.setPreferredSize(new Dimension(200, 300));
        checkboxPane.setMaximumSize(new Dimension(600, 300));
        settingsPane.add(checkboxPane);
        settingsPane.add(Box.createRigidArea(new Dimension(0, 5)));

        addCheckbox(evfRecruit, checkboxPane);
        addCheckbox(evfSplit, checkboxPane);
        addCheckbox(evfSummon, checkboxPane);
        addCheckbox(evfTeleport, checkboxPane);
        addCheckbox(evfAcquire, checkboxPane);
        addCheckbox(evfWon, checkboxPane);
        addCheckbox(evfLoser, checkboxPane);
        checkboxPane.add(Box.createRigidArea(new Dimension(0, 5)));
        addCheckbox(evHideUndone, checkboxPane);
        checkboxPane.add(Box.createRigidArea(new Dimension(0, 5)));

        addCheckbox(evfMulligan, checkboxPane);
        addCheckbox(evfMoveRoll, checkboxPane);
        addCheckbox(evfTurnChange, checkboxPane);
        addCheckbox(evfPlayerChange, checkboxPane);

        JPanel miscPane = new JPanel(new GridLayout(0, 2));
        miscPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        miscPane.setBorder(new TitledBorder("Other Settings"));
        miscPane.setMinimumSize(new Dimension(200, 100));
        miscPane.setPreferredSize(new Dimension(200, 100));
        miscPane.setMaximumSize(new Dimension(600, 100));
        settingsPane.add(miscPane);

        settingsPane.add(Box.createVerticalGlue());
        settingsPane.add(Box.createRigidArea(new Dimension(0, 5)));

        addCheckbox(evAutoScroll, miscPane);
        miscPane.add(Box.createRigidArea(new Dimension(0, 5)));

        // selection for how many turns to display the data
        // (must be less or equal the expireTurns value set in getPlayers)

        int maxVal = this.expireTurns == -1 ? 1000 : this.expireTurns;

        List<String> alChoices = new ArrayList<String>();
        int i;
        for (i = 1; i <= maxVal; i++)
        {
            // 1, 2, 3, 4, 5, 
            if (i <= 5 || i == maxVal)
            {
                alChoices.add(new Integer(i).toString());
            }

            /* right now: no big values due to performance issues...
             // 10, 50, 100, 500, 1000 if applicable.
             else if (i==10 || i==50 || i==100 || i==500 || i==1000)
             */
            else if (i == 10)
            {
                alChoices.add(new Integer(i).toString());
            }
        }
        if (this.expireTurns == -1)
        {
            alChoices.add("all");
        }
        else
        {
            maxString = "max (=" + this.expireTurns + ")";
            alChoices.add(maxString);
        }

        Object[] Choices = alChoices.toArray();

        // read user's setting for this, but cannot exceed the Game's
        // general setting.
        String maxTurnsOptString = options.getStringOption(evMaxTurns);
        if (maxTurnsOptString == null)
        {
            maxTurnsOptString = "3";
        }

        if (maxTurnsOptString.equals("all")
            || maxTurnsOptString.startsWith("max"))
        {
            if (this.expireTurns == -1)
            {
                maxTurns = -1;
                maxTurnsOptString = "all";
            }
            else
            {
                maxTurns = this.expireTurns;
                maxTurnsOptString = maxString;
            }
        }
        else
        {
            int maxTurnsOpt = 1;
            try
            {
                maxTurnsOpt = Integer.parseInt(maxTurnsOptString);
                if (maxTurnsOpt > maxVal)
                {
                    maxTurnsOpt = maxVal;
                    maxTurnsOptString = new Integer(maxTurnsOpt).toString();
                }
            }
            catch (NumberFormatException e)
            {
                LOGGER.log(Level.SEVERE, "Illegal value '" + maxTurnsOptString
                    + "' for option '" + evMaxTurns + "' - using default 1");
                maxTurnsOptString = "1";
                maxTurnsOpt = 1;
            }
        }

        maxTurnsDisplayExpiringBox = new JComboBox(Choices);
        maxTurnsDisplayExpiringBox.addActionListener(this);
        maxTurnsDisplayExpiringBox.setSelectedItem(maxTurnsOptString);
        miscPane.add(new JLabel("Display max. (turns):"));
        miscPane.add(maxTurnsDisplayExpiringBox);

        // add all to the main contentPane
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(tabbedPane);
        contentPane.validate();

        this.pack();
    }

    private boolean isEventTooOld(RevealEvent e)
    {
        int oldEventTurn = e.getTurn();
        int oldPlayerNr = e.getPlayerNr();

        if (maxTurns != -1
            && turnNr - oldEventTurn > maxTurns
                - (playerNr >= oldPlayerNr ? 1 : 0))
        {
            // Log.debug("Not displaying event "+e.getEventTypeText()+" "+
            //     e.getMarkerId() + " - older than max turns value!");
            return true;
        }
        return false;
    }

    private boolean isEventRelevant(RevealEvent e)
    {
        int type = e.getEventType();
        boolean display = true;

        if (!showEventType[type])
        {
            display = false;
        }
        else if (hideUndoneEvents && e.wasUndone())
        {
            LOGGER.log(Level.FINEST, "Not displaying event "
                + e.getEventTypeText() + " " + e.getMarkerId()
                + " - was undone and hideUndoneEvents is true.");
            display = false;
        }
        return display;
    }

    private void queueForDisplaying(JPanel eventPanel)
    {
        synchronized (displayQueue)
        {
            displayQueue.add(eventPanel);
        }
    }

    /** Remove all pending events, and queue a null event to signal the
     *  displayer to remove all from panel first before adding again. */

    private void queueSignalRemoveAllForDisplaying()
    {
        synchronized (displayQueue)
        {
            displayQueue.clear();
            displayQueue.add(null);
        }
    }

    private void displayFromQueue()
    {
        synchronized (displayQueue)
        {
            if (displayQueue.size() > 0)
            {
                Iterator<JPanel> it = displayQueue.iterator();
                while (it.hasNext())
                {
                    JPanel panelForEvent = it.next();
                    if (panelForEvent == null)
                    {
                        eventPane.removeAll();
                    }
                    else
                    {
                        eventPane.add(panelForEvent);
                        eventPane
                            .add(Box.createRigidArea(new Dimension(0, 5)));
                    }
                }
                displayQueue.clear();
                postAddEventActions();
            }
            else
            {
                // ok, queue was empty, nothing to do
            }
        }
    }

    private void postAddEventActions()
    {
        getContentPane().validate();
        if (autoScroll)
        {
            eventScrollBar.setValue(eventScrollBar.getMaximum());
        }
        getContentPane().validate();
    }

    private void addEventToEventPane(RevealEvent e)
    {
        JPanel panelForEvent = e.toPanel();
        if (panelForEvent != null)
        {
            queueForDisplaying(panelForEvent);
        }
        else
        {
            LOGGER
                .log(Level.WARNING,
                    "EventViewer.addEventToEventPane: event.toPanel returned null!");
        }
    }

    private synchronized void addEventToList(RevealEvent e)
    {
        synchronized (syncdEventList)
        {
            syncdEventList.add(e);
        }
    }

    private void triggerDisplaying()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            displayFromQueue();
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    displayFromQueue();
                }
            });
        }
    }

    public void addEvent(RevealEvent e)
    {
        addEventToList(e);

        if (this.visible)
        {
            boolean display = isEventRelevant(e);
            if (display)
            {
                addEventToEventPane(e);
                triggerDisplaying();
            }
        }
    }

    /*
     * Remove all, and add those again which are still in the eventList
     * @param forceAll: reset the bookmark, start from begin of list.
     *                  => if not set, can start searching from last 
     *                  remembered position.
     */
    private void updatePanels(boolean forceAll)
    {
        queueSignalRemoveAllForDisplaying();

        synchronized (syncdEventList)
        {
            // if never expires, we never delete, so bookmark stays ok.
            // But if expiring is happening (!= -1) or force is given
            // (e.g. when maxTurns changed) then need to start searching 
            // from start.
            if (this.expireTurns != -1 || forceAll)
            {
                bookmark = 0;
            }

            if (bookmark > syncdEventList.size())
            {
                // sanity check...
                LOGGER.log(Level.SEVERE, "bookmark " + bookmark
                    + " out of range, size=" + syncdEventList.size());
                bookmark = 0;
            }

            ListIterator<RevealEvent> it = syncdEventList
                .listIterator(bookmark);
            while (it.hasNext())
            {
                RevealEvent e = it.next();
                if (isEventTooOld(e))
                {
                    bookmark++;
                }
                else if (isEventRelevant(e))
                {
                    addEventToEventPane(e);
                }
            }
        }

        triggerDisplaying();
        this.repaint();
    }

    // Helper methods to ask something from client:
    private int getActivePlayerNum()
    {
        return client.getActivePlayer().getNumber();
    }

    private LegionClientSide getLegionInfo(String marker)
    {
        return client.getLegion(marker);
    }

    // shortcuts:
    private void newRollEvent(int eventType, int roll1, int roll2)
    {
        RevealEvent e = new RevealEvent(client, client.getTurnNumber(),
            getActivePlayerNum(), eventType, roll1, roll2);
        addEvent(e);
    }

    // creature related event:
    private void newEvent(int eventType, String markerId1, int height1,
        ArrayList<RevealedCreature> rcList, String markerId2, int height2)
    {
        RevealEvent e = new RevealEvent(client, client.getTurnNumber(),
            getActivePlayerNum(), eventType, markerId1, height1, rcList,
            markerId2, height2);
        addEvent(e);
    }

    // Now come the methods with which Client can add/modify event data:
    public void turnOrPlayerChange(Client client, int turnNr, int playerNr)
    {
        setMulliganOldRoll(-2);
        if (turnNr != this.turnNr)
        {
            RevealEvent e = new RevealEvent(client, turnNr, playerNr,
                RevealEvent.eventTurnChange, null, 0, null, null, 0);
            addEvent(e);
        }
        if (playerNr != this.playerNr || turnNr != this.turnNr)
        {
            RevealEvent e = new RevealEvent(client, turnNr, playerNr,
                RevealEvent.eventPlayerChange, null, 0, null, null, 0);
            addEvent(e);
        }

        this.turnNr = turnNr;
        this.playerNr = playerNr;
        if (this.expireTurns != -1)
        {
            purgeOldEvents();
            if (this.visible)
            {
                updatePanels(true);
            }
        }
        else
        // expire turns -1 ==> no purging. 
        {
            if (this.maxTurns != -1)
            {
                // something will have got expired now
                // but we can use the bookmark.
                updatePanels(false);
            }
            // else: maxTurns -1 => stays displaying from begin on,
            //       so no update needed at all.
        }
    }

    public void setMulliganOldRoll(int roll)
    {
        mulliganOldRoll = roll;
    }

    public void tellMovementRoll(int roll)
    {
        // if oldroll is -2, this is the first roll;
        // otherwise, player took mulligan.
        if (mulliganOldRoll == -2)
        {
            mulliganOldRoll = roll;
            newRollEvent(RevealEvent.eventMoveRoll, roll, -1);
        }
        else
        {
            newRollEvent(RevealEvent.eventMulligan, mulliganOldRoll, roll);
        }
    }

    public void tellEngagement(Legion attacker, Legion defender, int turnNumber)
    {
        this.attacker = attacker;
        this.defender = defender;

        attackerEventLegion = new RevealEvent(client, turnNumber,
            getActivePlayerNum(), RevealEvent.eventBattle, attacker
                .getMarkerId(), ((LegionClientSide)attacker).getHeight(),
            new ArrayList<RevealedCreature>(), null, 0);
        attackerEventLegion.setEventInfo(Constants.reasonBattleStarts);
        attackerEventLegion.setRealPlayer((PlayerClientSide)attacker
            .getPlayer());

        defenderEventLegion = new RevealEvent(client, turnNumber,
            getActivePlayerNum(), RevealEvent.eventBattle, defender
                .getMarkerId(), ((LegionClientSide)defender).getHeight(),
            new ArrayList<RevealedCreature>(), null, 0);

        defenderEventLegion.setEventInfo(Constants.reasonBattleStarts);
        defenderEventLegion.setRealPlayer((PlayerClientSide)defender
            .getPlayer());
    }

    public void tellEngagementResults(Legion winner, String method, int turns)
    {
        // if those are not set, we are new version client with old
        // version server, who does not provide the reason argument
        // to some other methods; then they do not set up those
        // eventLegions we would need here. So, can't do anything.
        if (attackerEventLegion == null || defenderEventLegion == null)
        {
            LOGGER.log(Level.FINEST,
                "tellEngagementResultHandling, both are null");
            return;
        }

        if (winner == null)
        {
            LOGGER.log(Level.FINEST, "winner is null value");
        }
        else
        {
            LOGGER.log(Level.FINEST, "winner is '" + winner + "'");
        }

        if (winner == null)
        {
            // null value = normal mutual
            // string with content "null": one legion contained titan, 
            // titan killed, some others survived, 
            // titan-killing-legion eliminated.
            // The above is for normal game. What if load from history??
            LOGGER.log(Level.FINEST,
                "tellEngagementResultHandling, winner null");

            // mutual elimination
            // attackerEventLegion.setAllDead();
            attackerEventLegion.setEventType(RevealEvent.eventLost);
            attackerEventLegion.setEventInfo("mutual");
            addEvent(attackerEventLegion);

            // defenderEventLegion.setAllDead();
            defenderEventLegion.setEventType(RevealEvent.eventLost);
            defenderEventLegion.setEventInfo("mutual");
            addEvent(defenderEventLegion);
        }
        else
        {
            LOGGER.log(Level.INFO, "tellEngagementResultHandling, winner = "
                + winner);
            if (winner.equals(this.attacker))
            { // attacker won:
                winnerLegion = attackerEventLegion;
                loserLegion = defenderEventLegion;
            }
            else
            { // defender
                winnerLegion = defenderEventLegion;
                loserLegion = attackerEventLegion;
            }

            // fled or concession there didn't come removeCreature messages,
            // thus make sure they are really shown dead.
            loserLegion.setAllDead();
            loserLegion.setEventType(RevealEvent.eventLost);
            if (turns > 7)
            {
                method = Constants.erMethodTimeLoss;
            }
            loserLegion.setEventInfo(method);
            addEvent(loserLegion);

            int winnerHeight = winnerLegion.getHeight();
            int winnerEventHeight = winnerLegion.getHeight();
            if (winnerEventHeight != winnerHeight)
            {
                if (winnerEventHeight != 0)
                {
                    // @TODO: is that a problem?
                    LOGGER.log(Level.FINEST, "Winner legion " + winner
                        + " event height mismatch: Eventheight="
                        + winnerLegion.getHeight() + ", actual height="
                        + winnerHeight);
                }
            }
            winnerLegion.setEventType(RevealEvent.eventWon);
            winnerLegion.setEventInfo(method);
            addEvent(winnerLegion);
        }

        lastAttackerEventLegion = attackerEventLegion;
        lastDefenderEventLegion = defenderEventLegion;

        attackerEventLegion = null;
        defenderEventLegion = null;
        winnerLegion = null;
        loserLegion = null;
    }

    public void newCreatureRevealEvent(int eventType, String markerId1,
        int height1, String creature, String markerId2, int height2)
    {
        RevealedCreature rc = new RevealedCreature(creature);
        switch (eventType)
        {
            case RevealEvent.eventSummon:
                rc.setWasSummoned(true);
                break;
            case RevealEvent.eventTeleport:
                rc.setDidTeleport(true);
                break;
            default:
                LOGGER.log(Level.SEVERE, "Invalid event type "
                    + RevealEvent.getEventTypeText(eventType)
                    + " in newCreatureRevealEvent: markerId " + markerId1
                    + ", creature " + creature);
        }

        ArrayList<RevealedCreature> rcList = new ArrayList<RevealedCreature>(1);
        rcList.add(rc);

        newEvent(eventType, markerId1, height1, rcList, markerId2, height2);
    }

    public void newSplitEvent(String markerId1, int height1,
        ArrayList<RevealedCreature> rcList, String markerId2, int height2)
    {
        RevealEvent e = new RevealEvent(client, client.getTurnNumber(),
            getActivePlayerNum(), RevealEvent.eventSplit, markerId1, height1,
            rcList, markerId2, height2);
        addEvent(e);
    }

    public void revealCreatures(Legion legion, final List<String> names,
        String reason)
    {
        // EventViewer stuff:
        // looks as if right now we need this revealedInfo only for
        // engagements in which we are envolved.
        // E.g. recruit info is handled by separate didRecruit...

        // If this player is involved in an engagement, then server reveals 
        // us the opponent, and our own legion we know anyway.
        // Thus, update the knownCreatures info in the events so that both
        // the attacker and defender are known in EventViewer (in THIS client)
        if (reason.equals(Constants.reasonEngaged))
        {
            RevealEvent ownEvent = null;
            RevealEvent otherEvent = null;

            if (legion.equals(attacker))
            {
                otherEvent = attackerEventLegion;
                ownEvent = defenderEventLegion;
            }
            else if (legion.equals(defender))
            {
                otherEvent = defenderEventLegion;
                ownEvent = attackerEventLegion;
            }
            // else: Fine as well. Client just not involved in this engagement.

            if (otherEvent != null)
            {
                List<RevealedCreature> rcNames = new ArrayList<RevealedCreature>();
                Iterator<String> it = names.iterator();
                while (it.hasNext())
                {
                    String name = it.next();
                    RevealedCreature rc = new RevealedCreature(name);
                    rcNames.add(rc);
                }
                otherEvent.updateKnownCreatures(rcNames);
            }
            if (ownEvent != null)
            {
                String ownMarkerId = ownEvent.getMarkerId();
                LegionClientSide info = getLegionInfo(ownMarkerId);
                List<String> ownNames = info.getContents();
                ArrayList<RevealedCreature> rcNames = new ArrayList<RevealedCreature>();
                Iterator<String> it = ownNames.iterator();
                while (it.hasNext())
                {
                    String name = it.next();
                    RevealedCreature rc = new RevealedCreature(name);
                    rcNames.add(rc);
                }
                ownEvent.updateKnownCreatures(rcNames);
            }
        }
    }

    public void revealEngagedCreatures(final List<String> names,
        boolean isAttacker, String reason)
    {
        // can't do anything if (old) server or history do not provide 
        // us the reason
        if (reason == null || reason.equals("<Unknown>"))
        {
            return;
        }

        if (reason.equals(Constants.reasonBattleStarts)
            || reason.equals(Constants.reasonFled)
            || reason.equals(Constants.reasonConcession))
        {
            RevealEvent event;
            event = isAttacker ? attackerEventLegion : defenderEventLegion;

            ArrayList<RevealedCreature> rcNames = new ArrayList<RevealedCreature>();
            Iterator<String> it = names.iterator();
            while (it.hasNext())
            {
                String name = it.next();
                RevealedCreature rc = new RevealedCreature(name);
                rcNames.add(rc);
            }

            event.updateKnownCreatures(rcNames);
            event.setEventInfo(reason);
        }
        else
        {
            // perhaps load from history?
            LOGGER.log(Level.SEVERE, "revealEngagedCreatures, unknown reason "
                + reason);
        }
    }

    public void addCreature(String markerId, String name, String reason)
    {
        RevealEvent battleEvent = null;
        if (attackerEventLegion != null
            && attackerEventLegion.getMarkerId().equals(markerId))
        {
            battleEvent = attackerEventLegion;

        }
        else if (defenderEventLegion != null
            && defenderEventLegion.getMarkerId().equals(markerId))
        {
            battleEvent = defenderEventLegion;
        }

        if (battleEvent != null)
        {
            RevealedCreature rc = new RevealedCreature(name);
            rc.setReason(reason);
            battleEvent.addCreature(rc);
        }
        else
        {
            // no battle events where to add creature
        }

        // create also the separate acquire event:
        if (reason.equals(Constants.reasonAcquire))
        {
            int newHeight = getLegionInfo(markerId).getHeight();
            RevealedCreature rc = new RevealedCreature(name);
            rc.setWasAcquired(true);
            ArrayList<RevealedCreature> rcList = new ArrayList<RevealedCreature>(
                1);
            rcList.add(rc);
            newEvent(RevealEvent.eventAcquire, markerId, newHeight, rcList,
                null, 0);

            if (attackerEventLegion == null || defenderEventLegion == null)
            {
                // This should now never happen any more:
                LOGGER.log(Level.SEVERE, "no attacker nor defender "
                    + " legion event for acquiring!!" + " turn"
                    + client.getTurnNumber() + " player "
                    + client.getActivePlayer().getName() + " phase "
                    + client.getPhase() + " markerid " + markerId
                    + " marker owner"
                    + getLegionInfo(markerId).getPlayer().getName()
                    + "last engagement were" + " attacker "
                    + lastAttackerEventLegion.getMarkerId() + " defender "
                    + lastDefenderEventLegion.getMarkerId());
                System.exit(1);
            }
        }
        else if (reason.equals(Constants.reasonUndoSummon))
        {
            // addCreature adds summoned creature back to donor:
            undoEvent(RevealEvent.eventSummon, markerId, null, client
                .getTurnNumber());
            if (!attackerEventLegion.undoSummon(client.getTurnNumber(), name))
            {
                // this should never happen...
                LOGGER.log(Level.WARNING, "Un-Summon " + name
                    + " out of attacker event failed!");
            }
        }
    }

    public void removeCreature(String markerId, String name)
    {
        if (attacker != null && attackerEventLegion != null
            && attacker.getMarkerId().equals(markerId))
        {
            LOGGER.log(Level.FINEST, "During battle, remove creature " + name
                + " from attacker legion " + markerId);

            attackerEventLegion.setCreatureDied(name,
                ((LegionClientSide)attacker).getHeight());
        }

        else if (defender != null && defenderEventLegion != null
            && defender.getMarkerId().equals(markerId))
        {
            LOGGER.log(Level.FINEST, "During battle, remove creature " + name
                + " from defender legion " + markerId);
            defenderEventLegion.setCreatureDied(name,
                ((LegionClientSide)defender).getHeight());
        }
    }

    public void recruitEvent(String markerId, int height, String recruitName,
        List<String> recruiters)
    {
        ArrayList<RevealedCreature> rcList = new ArrayList<RevealedCreature>();
        RevealedCreature rc;

        Iterator<String> it = recruiters.iterator();
        while (it.hasNext())
        {
            String recruiterName = it.next();
            rc = new RevealedCreature(recruiterName);
            rc.setDidRecruit(true);
            rcList.add(rc);
        }

        rc = new RevealedCreature(recruitName);
        rc.setWasRecruited(true);
        rcList.add(rc);

        newEvent(RevealEvent.eventRecruit, markerId, height, rcList, null, 0);
    }

    // next two are for removeDeadBattleChits:
    public void attackerSetCreatureDead(String name, int height)
    {
        attackerEventLegion.setCreatureDied(name, height);
    }

    public void defenderSetCreatureDead(String name, int height)
    {
        defenderEventLegion.setCreatureDied(name, height);
    }

    /*
     * User undid one action. Event is just marked as undone, but not deleted
     * - information once revealed is known to the public, as in real life :) 
     */
    public void undoEvent(int type, String parentId, String childId, int turn)
    {
        int found = 0;
        if (type == RevealEvent.eventSplit)
        {
            synchronized (syncdEventList)
            {
                int last = syncdEventList.size();
                ListIterator<RevealEvent> it = syncdEventList
                    .listIterator(last);
                while (it.hasPrevious() && found == 0)
                {
                    RevealEvent e = it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn
                        && e.getMarkerId().equals(parentId)
                        && e.getMarkerId2().equals(childId) && !e.wasUndone())
                    {
                        found++;
                        e.setUndone(true);
                    }
                }
            }

            // HACK: if not found, search split event also from previous round
            // Sometimes server/game gives us wrong (different) turn number for
            // the split and the unsplit event. For now, if this happens, 
            // search also in the previous round.
            // TODO: investigate, why does server gives us different turn numbers
            //       in those events?
            if (found == 0)
            {
                synchronized (syncdEventList)
                {
                    int last = syncdEventList.size();
                    ListIterator<RevealEvent> it2 = syncdEventList
                        .listIterator(last);
                    while (it2.hasPrevious() && found == 0)
                    {
                        RevealEvent e = it2.previous();
                        if (e.getEventType() == type
                            && e.getTurn() + 1 == turn
                            && e.getMarkerId().equals(parentId)
                            && e.getMarkerId2().equals(childId)
                            && !e.wasUndone())
                        {
                            found++;
                            e.setUndone(true);
                        }
                    }
                }
            }
        }
        else if (type == RevealEvent.eventRecruit)
        {
            synchronized (syncdEventList)
            {
                int last = syncdEventList.size();
                ListIterator<RevealEvent> it = syncdEventList
                    .listIterator(last);
                while (it.hasPrevious() && found == 0)
                {
                    RevealEvent e = it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn
                        && e.getMarkerId().equals(parentId) && !e.wasUndone())
                    {
                        found++;
                        e.setUndone(true);
                    }
                }
            }
        }
        else if (type == RevealEvent.eventSummon)
        {
            synchronized (syncdEventList)
            {
                int last = syncdEventList.size();
                ListIterator<RevealEvent> it = syncdEventList
                    .listIterator(last);
                while (it.hasPrevious() && found == 0)
                {
                    RevealEvent e = it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn
                        && e.getMarkerId().equals(parentId) && !e.wasUndone())
                    {
                        found++;
                        e.setUndone(true);
                    }
                }
            }
        }
        else if (type == RevealEvent.eventTeleport)
        {
            synchronized (syncdEventList)
            {
                int last = syncdEventList.size();
                ListIterator<RevealEvent> it = syncdEventList
                    .listIterator(last);
                while (it.hasPrevious() && found == 0)
                {
                    RevealEvent e = it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn
                        && e.getMarkerId().equals(parentId) && !e.wasUndone())
                    {
                        found++;
                        e.setUndone(true);
                    }
                }
            }
        }

        else
        {
            LOGGER.log(Level.WARNING, "undo event for unknown type " + type
                + " attempted.");
            return;
        }

        if (found == 0)
        {
            LOGGER.log(Level.SEVERE, "Requested '" + type
                + "' EVENT to undo (" + parentId + ", " + childId + ", turn "
                + turn + ") not found");
        }

        if (this.visible)
        {
            updatePanels(false);
        }
    }

    // Now the methods for internal data management:
    /* throw away all events which are expireTurns turns older
     * than the given turnNr/playerNr combination.
     */
    public void purgeOldEvents()
    {
        if (this.expireTurns == -1)
        {
            LOGGER.log(Level.WARNING, "expireTurns -1 - no purging needed.");
            return;
        }
        int purged = 0;

        synchronized (syncdEventList)
        {
            Iterator<RevealEvent> it = syncdEventList.iterator();
            boolean done = false;
            while (it.hasNext() && !done)
            {
                RevealEvent e = it.next();
                int oldEventTurn = e.getTurn();
                int oldPlayerNr = e.getPlayerNr();

                if (turnNr - oldEventTurn > expireTurns
                    - (playerNr >= oldPlayerNr ? 1 : 0))
                {
                    it.remove();
                    purged++;
                }
                else
                {
                    done = true;
                }
            }
        }
    }

    public void cleanup()
    {
        synchronized (syncdEventList)
        {
            syncdEventList.clear();
        }
        synchronized (displayQueue)
        {
            displayQueue.clear();
        }
        this.options = null;
        this.client = null;

        attackerEventLegion = null;
        defenderEventLegion = null;

        lastAttackerEventLegion = null;
        lastDefenderEventLegion = null;
    }

    @Override
    public void dispose()
    {
        if (saveWindow != null)
        {
            saveWindow.save(this);
        }
        cleanup();
        super.dispose();
    }

    public void setVisibleMaybe()
    {
        boolean visible = options.getOption(Options.showEventViewer);
        setVisible(visible);
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            if (this.saveWindow == null)
            {
                this.saveWindow = new SaveWindow(options, windowTitle);
            }
            Point defaultLoc = getUpperRightCorner(getWidth());
            defaultLoc.setLocation(defaultLoc.x, 120);
            saveWindow.restore(this, defaultLoc);
            settingsPane.setMinimumSize(settingsPane.getSize());
            // eventPane.setMinimumSize(eventPane.getSize());
            this.visible = true;
            updatePanels(true);
        }
        else
        {
            if (this.isVisible())
            {
                // do not save when not visible - in particular this
                // would be run when a new user first time creates a 
                // board - Inspector option not selected, but EventViewer
                // gets created anyway and "setVisibleMaybe()" - that 
                // would save (0, 0) as initial location...
                saveWindow.save(this);
            }
            this.visible = false;
        }
        super.setVisible(visible);
    }

    public synchronized void actionPerformed(ActionEvent e)
    {
        // A combo box was changed.
        Object source = e.getSource();
        if (source == maxTurnsDisplayExpiringBox)
        {
            String value = (String)maxTurnsDisplayExpiringBox
                .getSelectedItem();
            options.setOption(evMaxTurns, value);
            if (value.equals("all") || value.equals(maxString))
            {
                maxTurns = -1;
            }
            else
            {
                maxTurns = Integer.parseInt(value);
            }
            updatePanels(true);
        }
    }

    public void itemStateChanged(ItemEvent e)
    {
        // one of the checkboxes was changed.
        JToggleButton source = (JToggleButton)e.getSource();
        String text = source.getText();
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
        options.setOption(text, selected);

        if (text.equals(evAutoScroll))
        {
            this.autoScroll = selected;
        }
        else if (text.equals(evHideUndone))
        {
            this.hideUndoneEvents = selected;
        }

        else if (text.equals(evfRecruit))
        {
            this.showEventType[RevealEvent.eventRecruit] = selected;
        }
        else if (text.equals(evfSplit))
        {
            this.showEventType[RevealEvent.eventSplit] = selected;
        }
        else if (text.equals(evfTeleport))
        {
            this.showEventType[RevealEvent.eventTeleport] = selected;
        }
        else if (text.equals(evfSummon))
        {
            this.showEventType[RevealEvent.eventSummon] = selected;
        }
        else if (text.equals(evfWon))
        {
            this.showEventType[RevealEvent.eventWon] = selected;
        }
        else if (text.equals(evfLoser))
        {
            this.showEventType[RevealEvent.eventLost] = selected;
        }
        else if (text.equals(evfAcquire))
        {
            this.showEventType[RevealEvent.eventAcquire] = selected;
        }
        else if (text.equals(evfTurnChange))
        {
            this.showEventType[RevealEvent.eventTurnChange] = selected;
        }
        else if (text.equals(evfMulligan))
        {
            this.showEventType[RevealEvent.eventMulligan] = selected;
        }
        else if (text.equals(evfMoveRoll))
        {
            this.showEventType[RevealEvent.eventMoveRoll] = selected;
        }
        else if (text.equals(evfPlayerChange))
        {
            this.showEventType[RevealEvent.eventPlayerChange] = selected;
        }

        updatePanels(false);
    }
}
