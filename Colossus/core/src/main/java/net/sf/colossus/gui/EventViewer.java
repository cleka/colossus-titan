package net.sf.colossus.gui;


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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import net.sf.colossus.client.Client;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.variant.CreatureType;


/**
 * Event Revealing dialog.
 *
 * It collects all revealed events and displays all or
 * only the recent ones of them.
 *
 *
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

final class EventViewer extends KDialog
{
    private static final Logger LOGGER = Logger.getLogger(EventViewer.class
        .getName());

    private final static String WINDOW_TITLE = "Event Viewer";

    private IOptions options;
    private Client client;

    private boolean visible;

    private final List<RevealEvent> eventList = new LinkedList<RevealEvent>();
    private int bookmark = 0;
    final private List<JPanel> displayQueue = new LinkedList<JPanel>();

    private int turnNr;
    private Player currentPlayer;

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
    public static final String evfExtraRoll = "Extra rolls";
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
    private JComboBox<String> maxTurnsDisplayExpiringBox;

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
        super(frame, WINDOW_TITLE, false);
        setFocusable(false);

        this.options = options;
        this.client = client;

        initExpireTurnsFromOptions();

        showEventType = new boolean[RevealEvent.NUMBEROFEVENTS];
        showEventType[RevealEvent.eventRecruit] = getBoolOption(evfRecruit,
            true);
        // Reinforce uses same checkbox as Recruit!
        showEventType[RevealEvent.eventReinforce] = getBoolOption(evfRecruit,
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
        showEventType[RevealEvent.eventExtraRoll] = getBoolOption(
            evfExtraRoll, true);
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

        Point defaultLoc = getUpperRightCorner(getWidth());
        defaultLoc.setLocation(defaultLoc.x, 120);
        useSaveWindow(options, WINDOW_TITLE, defaultLoc);

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

    private void addCheckbox(final String optname, Container pane)
    {
        JCheckBox cb = new JCheckBox(optname);
        boolean selected = getBoolOption(optname, true);
        cb.setSelected(selected);
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setAlignmentY(Component.TOP_ALIGNMENT);
        cb.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
                options.setOption(optname, selected);

                if (optname.equals(evAutoScroll))
                {
                    autoScroll = selected;
                }
                else if (optname.equals(evHideUndone))
                {
                    hideUndoneEvents = selected;
                }

                else if (optname.equals(evfRecruit))
                {
                    showEventType[RevealEvent.eventRecruit] = selected;
                }
                else if (optname.equals(evfSplit))
                {
                    showEventType[RevealEvent.eventSplit] = selected;
                }
                else if (optname.equals(evfTeleport))
                {
                    showEventType[RevealEvent.eventTeleport] = selected;
                }
                else if (optname.equals(evfSummon))
                {
                    showEventType[RevealEvent.eventSummon] = selected;
                }
                else if (optname.equals(evfWon))
                {
                    showEventType[RevealEvent.eventWon] = selected;
                }
                else if (optname.equals(evfLoser))
                {
                    showEventType[RevealEvent.eventLost] = selected;
                }
                else if (optname.equals(evfAcquire))
                {
                    showEventType[RevealEvent.eventAcquire] = selected;
                }
                else if (optname.equals(evfTurnChange))
                {
                    showEventType[RevealEvent.eventTurnChange] = selected;
                }
                else if (optname.equals(evfMulligan))
                {
                    showEventType[RevealEvent.eventMulligan] = selected;
                }
                else if (optname.equals(evfMoveRoll))
                {
                    showEventType[RevealEvent.eventMoveRoll] = selected;
                }
                else if (optname.equals(evfExtraRoll))
                {
                    showEventType[RevealEvent.eventExtraRoll] = selected;
                }
                else if (optname.equals(evfPlayerChange))
                {
                    showEventType[RevealEvent.eventPlayerChange] = selected;
                }

                updatePanels(false);
            }
        });
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
        for (int i = 1; i <= maxVal; i++)
        {
            // 1, 2, 3, 4, 5,
            if (i <= 5 || i == maxVal)
            {
                alChoices.add(String.valueOf(i));
            }

            /* right now: no big values due to performance issues...
             // 10, 50, 100, 500, 1000 if applicable.
             else if (i==10 || i==50 || i==100 || i==500 || i==1000)
             */
            else if (i == 10)
            {
                alChoices.add(String.valueOf(i));
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
                    maxTurnsOptString = Integer.valueOf(maxTurnsOpt)
                        .toString();
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

        String[] choicesArray = alChoices.toArray(new String[0]);
        maxTurnsDisplayExpiringBox = new JComboBox<String>(choicesArray);
        maxTurnsDisplayExpiringBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
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
        });
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
        int oldPlayerNr = e.getPlayer().getNumber();

        if (maxTurns != -1
            && turnNr - oldEventTurn > maxTurns
                - (currentPlayer.getNumber() >= oldPlayerNr ? 1 : 0))
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
            LOGGER.log(
                Level.FINEST,
                "Not displaying event " + e.getEventTypeText() + " "
                    + e.getLongMarkerId()
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

    private void addEventToList(RevealEvent e)
    {
        synchronized (eventList)
        {
            eventList.add(e);
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

        synchronized (eventList)
        {
            // if never expires, we never delete, so bookmark stays ok.
            // But if expiring is happening (!= -1) or force is given
            // (e.g. when maxTurns changed) then need to start searching
            // from start.
            if (this.expireTurns != -1 || forceAll)
            {
                bookmark = 0;
            }

            if (bookmark > eventList.size())
            {
                // sanity check...
                LOGGER.log(Level.SEVERE, "bookmark " + bookmark
                    + " out of range, size=" + eventList.size());
                bookmark = 0;
            }

            ListIterator<RevealEvent> it = eventList.listIterator(bookmark);
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
    private Player getActivePlayer()
    {
        return client.getActivePlayer();
    }

    // shortcuts:
    private void newRollEvent(int eventType, int roll1, int roll2)
    {
        RevealEvent e = new RevealEvent(client.getTurnNumber(),
            getActivePlayer(), eventType, roll1, roll2);
        addEvent(e);
    }

    // creature related event:
    private void newEvent(int eventType, Legion legion1,
        ArrayList<RevealedCreature> rcList, Legion legion2)
    {
        RevealEvent e = new RevealEvent(client.getTurnNumber(),
            getActivePlayer(), eventType, legion1, rcList, legion2);
        addEvent(e);
    }

    // Now come the methods with which Client can add/modify event data:
    public void turnOrPlayerChange(int turnNr, Player player)
    {
        setMulliganOldRoll(-2);
        if (turnNr != this.turnNr)
        {
            RevealEvent e = new RevealEvent(turnNr, player,
                RevealEvent.eventTurnChange);
            addEvent(e);
        }
        if (player != this.currentPlayer || turnNr != this.turnNr)
        {
            RevealEvent e = new RevealEvent(turnNr, player,
                RevealEvent.eventPlayerChange);
            addEvent(e);
        }

        this.turnNr = turnNr;
        this.currentPlayer = player;
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

    public void tellMovementRoll(int roll, String reason)
    {
        // if oldroll is -2, this is the first roll;
        // otherwise, player took mulligan.
        if (mulliganOldRoll == -2 || reason.equals(Constants.reasonNormalRoll))
        {
            newRollEvent(RevealEvent.eventMoveRoll, roll, -1);
        }
        else if (reason.equals(Constants.reasonExtraRoll))
        {
            newRollEvent(RevealEvent.eventExtraRoll, mulliganOldRoll, roll);
        }
        else if (reason.equals(Constants.reasonMulligan))
        {
            newRollEvent(RevealEvent.eventMulligan, mulliganOldRoll, roll);
        }
        else
        {
            LOGGER.warning("Unrecognized reason '" + reason
                + "' for movement roll?");
            newRollEvent(RevealEvent.eventMoveRoll, roll, -1);
        }
        mulliganOldRoll = roll;
    }

    public void tellEngagement(Legion attacker, Legion defender, int turnNumber)
    {
        this.attacker = attacker;
        this.defender = defender;

        attackerEventLegion = new RevealEvent(turnNumber, getActivePlayer(),
            RevealEvent.eventBattle, attacker,
            new ArrayList<RevealedCreature>(), null);
        attackerEventLegion.setEventInfo(Constants.reasonBattleStarts);
        attackerEventLegion.setRealPlayer(attacker.getPlayer());

        defenderEventLegion = new RevealEvent(turnNumber, getActivePlayer(),
            RevealEvent.eventBattle, defender,
            new ArrayList<RevealedCreature>(), null);

        defenderEventLegion.setEventInfo(Constants.reasonBattleStarts);
        defenderEventLegion.setRealPlayer(defender.getPlayer());
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
            LOGGER
                .log(Level.INFO, "tellEngagementResultHandling, winner null");

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
            {
                // attacker won
                winnerLegion = attackerEventLegion;
                loserLegion = defenderEventLegion;
            }
            else
            {
                // defender won
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

    public void newCreatureRevealEvent(int eventType, Legion legion1,
        CreatureType creature, Legion legion2)
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
                String markerId1 = legion1 != null ? legion1.getMarkerId()
                    : "<null legion>";
                LOGGER.log(
                    Level.SEVERE,
                    "Invalid event type "
                        + RevealEvent.getEventTypeText(eventType)
                        + " in newCreatureRevealEvent: markerId " + markerId1
                        + ", creature " + creature);
        }

        ArrayList<RevealedCreature> rcList = new ArrayList<RevealedCreature>(1);
        rcList.add(rc);

        newEvent(eventType, legion1, rcList, legion2);
    }

    public void newSplitEvent(int turnNr, Legion legion1,
        ArrayList<RevealedCreature> rcList, Legion legion2)
    {
        RevealEvent e = new RevealEvent(turnNr, getActivePlayer(),
            RevealEvent.eventSplit, legion1, rcList, legion2);
        addEvent(e);
    }

    public void revealCreatures(Legion legion,
        List<CreatureType> creatureTypes, String reason)
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
                for (CreatureType type : creatureTypes)
                {
                    RevealedCreature rc = new RevealedCreature(type);
                    rcNames.add(rc);
                }
                otherEvent.updateKnownCreatures(rcNames);
            }
            if (ownEvent != null)
            {
                Legion ownLegion = ownEvent.getLegion1();
                ArrayList<RevealedCreature> rcNames = new ArrayList<RevealedCreature>();
                for (CreatureType creature : ownLegion.getCreatureTypes())
                {
                    RevealedCreature rc = new RevealedCreature(creature);
                    rcNames.add(rc);
                }
                ownEvent.updateKnownCreatures(rcNames);
            }
        }
    }

    public void revealEngagedCreatures(final List<CreatureType> creatures,
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
            for (CreatureType creatureType : creatures)
            {
                RevealedCreature rc = new RevealedCreature(creatureType);
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

    public void addCreature(Legion legion, CreatureType type, String reason)
    {
        RevealEvent battleEvent = null;
        if (attackerEventLegion == null && defenderEventLegion == null)
        {
            // no battle events where to add creature - ok!
        }
        else if (attackerEventLegion != null
            && attackerEventLegion.getLongMarkerId().equals(
                legion.getLongMarkerId()))
        {
            battleEvent = attackerEventLegion;

        }
        else if (defenderEventLegion != null
            && defenderEventLegion.getLongMarkerId().equals(
                legion.getLongMarkerId()))
        {
            battleEvent = defenderEventLegion;
        }
        else
        {
            LOGGER.warning("No battle event found for legion "
                + legion.getLongMarkerId()
                + " where to add creature "
                + type
                + "; attacker log marker id "
                + (attackerEventLegion == null ? "attEvLg null"
                    : attackerEventLegion.getLongMarkerId())
                + ", defender long marker id "
                + (defenderEventLegion == null ? "defEvLg null"
                    : defenderEventLegion.getLongMarkerId()));
        }

        if (battleEvent != null)
        {
            RevealedCreature rc = new RevealedCreature(type);
            rc.setReason(reason);
            battleEvent.addCreature(rc);
        }
        else
        {
            // no battle events where to add creature - fine as well.
        }

        if (reason.equals(Constants.reasonAcquire))
        {
            // create also the separate acquire event:
            RevealedCreature rc = new RevealedCreature(type);
            rc.setWasAcquired(true);
            ArrayList<RevealedCreature> rcList = new ArrayList<RevealedCreature>(
                1);
            rcList.add(rc);
            newEvent(RevealEvent.eventAcquire, legion, rcList, null);

            if (attackerEventLegion == null || defenderEventLegion == null)
            {
                // This should now never happen any more:
                LOGGER.log(
                    Level.SEVERE,
                    "no attacker and defender "
                        + " legion event for acquiring!!" + " turn"
                        + client.getTurnNumber() + " player "
                        + client.getActivePlayer().getName() + " phase "
                        + client.getPhase() + " markerid "
                        + legion.getMarkerId() + " marker owner"
                        + legion.getPlayer().getName()
                        + "last engagement were" + " attacker "
                        + lastAttackerEventLegion.getLongMarkerId()
                        + " defender "
                        + lastDefenderEventLegion.getLongMarkerId());
                System.exit(1);
            }
        }
        else if (reason.equals(Constants.reasonUndoSummon))
        {
            // addCreature adds summoned creature back to donor:
            int turn = client.getTurnNumber();
            undoEvent(RevealEvent.eventSummon, legion, null, turn);
            if (!attackerEventLegion.removeSummonedCreature(turn,
                type.getName()))
            {
                // this should never happen...
                LOGGER.log(Level.WARNING, "Un-Summon " + type.getName()
                    + " out of attacker event failed!");
            }
        }
        else if (reason.equals(Constants.reasonReinforced))
        {
            // Recruit/Reinforce event is created by didRecruit()
        }
    }

    public void cancelReinforcement(CreatureType creature, int turn)
    {
        defenderEventLegion.removeReinforcedCreature(turn, creature.getName());
    }

    public void removeCreature(Legion legion, CreatureType type, String reason)
    {
        if (attackerEventLegion == null && defenderEventLegion == null)
        {
            // ok, no battle event affected
            return;
        }

        if (attacker != null && attackerEventLegion != null
            && attacker.equals(legion))
        {
            LOGGER.log(Level.FINEST, "During battle, remove creature " + type
                + " from attacker legion " + legion);

            attackerEventLegion.setCreatureDied(type, attacker.getHeight());
        }

        else if (defender != null && defenderEventLegion != null
            && defender.equals(legion))
        {
            LOGGER.log(Level.FINEST, "During battle, remove creature " + type
                + " from defender legion " + legion);
            defenderEventLegion.setCreatureDied(type, defender.getHeight());
        }

        else if (reason.equals(Constants.reasonSummon))
        {
            // ok, nothing to do - this is about removing the summoned angel
            // from the donor legion, there is no event legion for that.
        }

        else
        {
            LOGGER.warning("No eventLegion found for legion "
                + legion.getLongMarkerId() + " to remove creature " + type);
        }
    }

    public void recruitEvent(Legion legion, CreatureType recruit,
        List<CreatureType> recruiters, String reason)
    {
        ArrayList<RevealedCreature> rcList = new ArrayList<RevealedCreature>();
        RevealedCreature rc;

        for (CreatureType creature : recruiters)
        {
            rc = new RevealedCreature(creature);
            rc.setDidRecruit(true);
            rcList.add(rc);
        }

        int recruitType;
        rc = new RevealedCreature(recruit);
        if (reason.equals(Constants.reasonReinforced))
        {
            recruitType = RevealEvent.eventReinforce;
            rc.setWasReinforced(true);
        }
        else
        {
            recruitType = RevealEvent.eventRecruit;
            rc.setWasRecruited(true);
        }

        rcList.add(rc);
        newEvent(recruitType, legion, rcList, null);
    }

    // for removeDeadBattleChits:
    public void setCreatureDead(BattleUnit battleUnit)
    {
        RevealEvent event = (battleUnit.isDefender() ? defenderEventLegion
            : attackerEventLegion);
        event.setCreatureDied(battleUnit.getType(), battleUnit.getLegion()
            .getHeight());
    }

    /*
     * User undid one action. Event is just marked as undone, but not deleted
     * - information once revealed is known to the public, as in real life :)
     */
    public void undoEvent(int type, Legion parent, Legion child, int turn)
    {
        assert parent != null : "undoEvent called for an event of type "
            + type + " but with null legion?";
        String parentId = parent.getLongMarkerId();
        String childId = child != null ? child.getLongMarkerId() : null;

        int found = 0;
        if (type == RevealEvent.eventSplit)
        {
            synchronized (eventList)
            {
                int last = eventList.size();
                ListIterator<RevealEvent> it = eventList.listIterator(last);
                while (it.hasPrevious() && found == 0)
                {
                    RevealEvent e = it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn
                        && e.getLongMarkerId().equals(parentId)
                        && e.getLongMarkerId2().equals(childId)
                        && !e.wasUndone())
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
            // TODO: Now newSplitEvent does not do client.getTurn() any more,
            //       which might have been the reason for the misfit;
            //       does this here still happen?
            assert found != 0 : "Mismatch for turnNr of SplitEvent still happens.";
            if (found == 0)
            {
                synchronized (eventList)
                {
                    int last = eventList.size();
                    ListIterator<RevealEvent> it2 = eventList
                        .listIterator(last);
                    while (it2.hasPrevious() && found == 0)
                    {
                        RevealEvent e = it2.previous();
                        if (e.getEventType() == type
                            && e.getTurn() + 1 == turn
                            && e.getLongMarkerId().equals(parentId)
                            && e.getLongMarkerId2().equals(childId)
                            && !e.wasUndone())
                        {
                            found++;
                            e.setUndone(true);
                        }
                    }
                }
            }
        }
        else if (type == RevealEvent.eventRecruit
            || type == RevealEvent.eventReinforce
            || type == RevealEvent.eventSummon
            || type == RevealEvent.eventTeleport)
        {
            synchronized (eventList)
            {
                int last = eventList.size();
                ListIterator<RevealEvent> it = eventList.listIterator(last);
                while (it.hasPrevious() && found == 0)
                {
                    RevealEvent e = it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn
                        && e.getLongMarkerId().equals(parentId)
                        && !e.wasUndone())
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
            if (type == RevealEvent.eventSplit
                && client.getPhase() == Phase.MOVE)
            {
                // OK. This can happen if game was saved with a split that is
                // now recombined by the server because the legions didn't have
                // valid moves. Now it was loaded in move phase, but replay
                // does (at least for now) NOT replay the actual event, it just
                // re-sends the add/remove info so that clients get the split
                // predict info right. Thus there is no such event here in
                // event viewer that could be undone.
            }
            else
            {
                LOGGER.log(Level.WARNING,
                    "Requested '" + RevealEvent.getEventTypeText(type)
                        + "' EVENT to undo (" + parentId + ", " + childId
                        + ", turn " + turn + ") not found");
            }
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
        // int purged = 0;

        synchronized (eventList)
        {
            Iterator<RevealEvent> it = eventList.iterator();
            boolean done = false;
            while (it.hasNext() && !done)
            {
                RevealEvent e = it.next();
                int oldEventTurn = e.getTurn();
                int oldPlayerNr = e.getPlayer().getNumber();

                if (turnNr - oldEventTurn > expireTurns
                    - (currentPlayer.getNumber() >= oldPlayerNr ? 1 : 0))
                {
                    it.remove();
                    // purged++;
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
        synchronized (eventList)
        {
            eventList.clear();
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
            settingsPane.setMinimumSize(settingsPane.getSize());
            // eventPane.setMinimumSize(eventPane.getSize());
            this.visible = true;
            updatePanels(true);
        }
        else
        {
            this.visible = false;
        }
        super.setVisible(visible);
    }
}
