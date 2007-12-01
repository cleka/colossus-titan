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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;

/**
 * Event Revealing dialog.
 *
 * It collects all revealed events and displays all or
 * only the recent ones of them.
 *
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
 * @version $Id: EventViewer.java 2591 2006-01-01 23:41:26Z cleka $
 * @author Clemens Katzer
 */

final class EventViewer extends KDialog implements WindowListener,
ItemListener, ActionListener
{
	private static final Logger LOGGER = Logger.getLogger(EventViewer.class.getName());

	private IOptions options;
    
    private boolean visible;
    
    private List syncdEventList = Collections.synchronizedList(new ArrayList());
    
    private int turnNr;
    private int playerNr;
    
    // how long back are they kept (from global settings)
    private int expireTurns;
    private String maxString;
    
    private SaveWindow saveWindow;

    private final static String windowTitle = "Event Viewer";

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
    private boolean[] showEventType;
    
    private boolean autoScroll;
    private boolean hideUndoneEvents;
    private JComboBox maxTurnsDisplayExpiringBox;
    
    // how many back are currently displayed
    private int maxTurns = 1;

    /** 
     * Inits the dialog, not necessarily displays it.
     * 
     * @param frame is the parent window
     * @param options
     * @viewMode viewMode, related to AutoInspector, currently not used
     * @expireTurns events older than that are deleted from list
     */

        
    EventViewer(final JFrame frame, final IOptions options)
    {
        super(frame, windowTitle, false);
        setFocusable(false);

        this.options = options;

        initExpireTurnsFromOptions();
        
        showEventType = new boolean[RevealEvent.NUMBEROFEVENTS];
        showEventType[RevealEvent.eventRecruit] = getBoolOption(evfRecruit, true);
        showEventType[RevealEvent.eventSplit] = getBoolOption(evfSplit, true);
        showEventType[RevealEvent.eventTeleport] = getBoolOption(evfTeleport, true);
        showEventType[RevealEvent.eventSummon] = getBoolOption(evfSummon, true);
        showEventType[RevealEvent.eventAcquire] = getBoolOption(evfAcquire, true);
        showEventType[RevealEvent.eventWon] = getBoolOption(evfWon, true);
        showEventType[RevealEvent.eventLost] = getBoolOption(evfLoser, true);

        showEventType[RevealEvent.eventMulligan] = getBoolOption(evfMulligan, true);
        showEventType[RevealEvent.eventMoveRoll] = getBoolOption(evfMoveRoll, true);
        
        showEventType[RevealEvent.eventTurnChange] = getBoolOption(evfTurnChange, true);
        showEventType[RevealEvent.eventPlayerChange] = getBoolOption(evfPlayerChange, false);
        
        autoScroll = getBoolOption(evAutoScroll, true);
        hideUndoneEvents = getBoolOption(evHideUndone, false);
        
        setupGUI();
        
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() 
            {
                public void windowClosing(WindowEvent e) 
                {
                    options.setOption(Options.showEventViewer, false);
                }
            }
        );

        this.saveWindow = new SaveWindow(options, windowTitle);        
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
            if ( expOption.equals(Options.eventExpiringNever))
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
                        LOGGER.log(Level.SEVERE, "Invalid value "+exp +" from option '"+
						Options.eventExpiring+"' - using default " + turnsToKeep, (Throwable)null);
                    }
                }
                catch(NumberFormatException e)
                {
                    LOGGER.log(Level.SEVERE, "Invalid value "+ expOption +" from option '"+
					Options.eventExpiring + "' - using default " + turnsToKeep, (Throwable)null);
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
        
        eventPane = new Box(BoxLayout.Y_AXIS);
        eventTabPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        //eventPane.setPreferredSize(new Dimension(200, 200));
        
        eventScrollPane = new JScrollPane(eventPane,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        eventTabPane.add(eventScrollPane);

        eventScrollBar = eventScrollPane.getVerticalScrollBar();
        
        // eventScrollPane.setPreferredSize(new Dimension(200, 380));
        
        eventPane.add(Box.createVerticalGlue());
                
        // The settings:
        settingsPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Settings", settingsPane);

        JPanel checkboxPane = new JPanel(new GridLayout(0,1));
        checkboxPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkboxPane.setBorder(new TitledBorder("Event Filter"));
//        checkboxPane.setMinimumSize(new Dimension(200, 100));
        checkboxPane.setPreferredSize(new Dimension(200, 300));
        checkboxPane.setMaximumSize(new Dimension(600, 300));
        settingsPane.add(checkboxPane);
        settingsPane.add(Box.createRigidArea(new Dimension(0,5)));

        addCheckbox(evfRecruit, checkboxPane);
        addCheckbox(evfSplit, checkboxPane);
        addCheckbox(evfSummon, checkboxPane);
        addCheckbox(evfTeleport, checkboxPane);
        addCheckbox(evfAcquire, checkboxPane);
        addCheckbox(evfWon, checkboxPane);
        addCheckbox(evfLoser, checkboxPane);
        checkboxPane.add(Box.createRigidArea(new Dimension(0,5)));
        addCheckbox(evHideUndone, checkboxPane);
        checkboxPane.add(Box.createRigidArea(new Dimension(0,5)));
        
        addCheckbox(evfMulligan, checkboxPane);
        addCheckbox(evfMoveRoll, checkboxPane);
        addCheckbox(evfTurnChange, checkboxPane);
        addCheckbox(evfPlayerChange, checkboxPane);

        
        JPanel miscPane = new JPanel(new GridLayout(0,2));
        miscPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        miscPane.setBorder(new TitledBorder("Other Settings"));
        miscPane.setMinimumSize(new Dimension(200, 100));
        miscPane.setPreferredSize(new Dimension(200, 100));
        miscPane.setMaximumSize(new Dimension(600, 100));
        settingsPane.add(miscPane);

        settingsPane.add(Box.createVerticalGlue());
        settingsPane.add(Box.createRigidArea(new Dimension(0,5)));
       
        addCheckbox(evAutoScroll, miscPane);
        miscPane.add(Box.createRigidArea(new Dimension(0,5)));

        
        // selection for how many turns to display the data
        // (must be less or equal the expireTurns value set in getPlayers)
        
        int maxVal = this.expireTurns == -1 ? 1000 : this.expireTurns;

        ArrayList alChoices = new ArrayList();
        int i;
        for (i=1 ; i <= maxVal ; i++)
        {
            // 1, 2, 3, 4, 5, 
            if (i<=5 || i==maxVal)
            {
                alChoices.add(new Integer(i).toString());
            }
            /* right now: no big values due to performance issues...
            // 10, 50, 100, 500, 1000 if applicable.
            else if (i==10 || i==50 || i==100 || i==500 || i==1000)
            */
            else if (i==10)
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
            maxString = "max (="+this.expireTurns+")";
            alChoices.add(maxString);
        }
                
        Object[] Choices = alChoices.toArray(); 

        // read user's setting for this, but cannot exceed the Game's
        // general setting.
        String maxTurnsOptString = 
            options.getStringOption(evMaxTurns);
        if (maxTurnsOptString == null )
        {
            maxTurnsOptString = "3";
        }
        
        if (maxTurnsOptString.equals("all") || maxTurnsOptString.startsWith("max"))
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
            catch(NumberFormatException e)
            {
                LOGGER.log(Level.SEVERE, "Illegal value '"+maxTurnsOptString + "' for option '" + 
				evMaxTurns + "' - using default 1", (Throwable)null);
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
        int oldPlayerNr  = e.getPlayerNr();

        if ( maxTurns != -1 &&
               turnNr-oldEventTurn > maxTurns-(playerNr>=oldPlayerNr?1:0))
        {
//            Log.debug("Not displaying event "+e.getEventTypeText()+" "+
//                      e.getMarkerId() + " - older than max turns value!");
            return true;
        }
        return false;
    }
    
    private boolean isEventRelevant(RevealEvent e)
    {
        int type = e.getEventType();
        boolean display = true;
        
        if ( !showEventType[type] )
        {
            display = false;
        }
        else if ( hideUndoneEvents && e.wasUndone())
        {
          LOGGER.log(Level.FINEST, "Not displaying event "+e.getEventTypeText()+" "+e.getMarkerId() +
		  " - was undone and hideUndoneEvents is true.");
            display = false;
        }
        return display;
    }

    private void addEventToEventPane(RevealEvent e)
    {
        Container pane = eventPane;
        JPanel panelForEvent = e.toPanel();
        if (panelForEvent != null)
        {
            pane.add(panelForEvent);
            pane.add(Box.createRigidArea(new Dimension(0,5)));
        }
        else    
        {   
            LOGGER.log(Level.WARNING, "EventViewer.addEventToEventPane: event.toPanel returned null!");
        }
    }

    private synchronized void addEventToList(RevealEvent e)
    {
        synchronized(syncdEventList) 
        {
            syncdEventList.add(e);
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
            }
            getContentPane().validate();
            if (autoScroll)
            {
                eventScrollBar.setValue(eventScrollBar.getMaximum());
            }
            getContentPane().validate();
        }
    }

    /*
     * Remove all, and add those again which are still in the eventList
     * @param forceAll: reset the bookmark, start from begin of list.
     *                  => if not set, can start searching from last 
     *                  remembered position.
     */
    private static int bookmark = 0;
    
    private void updatePanels(boolean forceAll)
    {
        Container pane = this.eventPane;

        pane.removeAll();
    
        synchronized(syncdEventList)
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
                LOGGER.log(Level.SEVERE, "bookmark " + bookmark + " out of range, size=" +
				syncdEventList.size(), (Throwable)null);
                bookmark = 0;
            }

            ListIterator it = syncdEventList.listIterator(bookmark);
            while (it.hasNext())
            {
                RevealEvent e = (RevealEvent) it.next();
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

        getContentPane().validate();
        if (autoScroll)
        {
            eventScrollBar.setValue(eventScrollBar.getMaximum());
        }
        getContentPane().validate();
        this.repaint();
    }

    public void turnOrPlayerChange(Client client, int turnNr, int playerNr)
    {
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
        else   // expire turns -1 ==> no purging. 
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

    

    /*
     * User undid one action. Event is just marked as undone, but not deleted
     * - information once revealed is known to the public, as in real life :) 
     */
    public void undoEvent(int type, String parentId, String childId, int turn)
    {
        int found = 0;
        if (type == RevealEvent.eventSplit)
        {
            synchronized(syncdEventList)
            {
                int last = syncdEventList.size();
                ListIterator it = syncdEventList.listIterator(last);
                while (it.hasPrevious() && found==0)
                {
                    RevealEvent e = (RevealEvent)it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn &&
                            e.getMarkerId().equals(parentId) &&
                            e.getMarkerId2().equals(childId) && 
                            ! e.wasUndone() )
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
                synchronized(syncdEventList)
                {
                    int last = syncdEventList.size();
                    ListIterator it2 = syncdEventList.listIterator(last);
                    while (it2.hasPrevious() && found==0)
                    {
                        RevealEvent e = (RevealEvent)it2.previous();
                        if (e.getEventType() == type && e.getTurn()+1 == turn &&
                                e.getMarkerId().equals(parentId) &&
                                e.getMarkerId2().equals(childId) &&
                                ! e.wasUndone() )
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
            synchronized(syncdEventList)
            {
                int last = syncdEventList.size();
                ListIterator it = syncdEventList.listIterator(last);
                while (it.hasPrevious() && found==0)
                {
                    RevealEvent e = (RevealEvent)it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn &&
                            e.getMarkerId().equals(parentId) &&
                            ! e.wasUndone())
                    {
                        found++;
                        e.setUndone(true);
                    }
                }
            }
        }
        else if (type == RevealEvent.eventSummon)
        {
            synchronized(syncdEventList)
            {
                int last = syncdEventList.size();
                ListIterator it = syncdEventList.listIterator(last);
                while (it.hasPrevious() && found==0)
                {
                    RevealEvent e = (RevealEvent)it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn &&
                            e.getMarkerId().equals(parentId) && 
                            ! e.wasUndone())
                    {
                        found++;
                        e.setUndone(true);
                    }
                }
            }
        }
        else if (type == RevealEvent.eventTeleport)
        {
            synchronized(syncdEventList)
            {
                int last = syncdEventList.size();
                ListIterator it = syncdEventList.listIterator(last);
                while (it.hasPrevious() && found==0)
                {
                    RevealEvent e = (RevealEvent)it.previous();
                    if (e.getEventType() == type && e.getTurn() == turn &&
                            e.getMarkerId().equals(parentId) && 
                            ! e.wasUndone())
                    {
                        found++;
                        e.setUndone(true);
                    }
                }
            }
        }

        else
        {
            LOGGER.log(Level.WARNING, "undo event for unknown type "+type+" attempted.");
            return;
        }
       
        if (found == 0)
        {
            LOGGER.log(Level.SEVERE, "Requested '"+type+"' EVENT to undo ("+
			parentId+", "+childId+", turn "+turn+") not found", (Throwable)null);
        }

        if (this.visible)
        {
            updatePanels(false);
        }
    }
    
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

        synchronized(syncdEventList)
        {
            Iterator it = syncdEventList.iterator();
            boolean done = false;
            while (it.hasNext() && !done)
            {
                RevealEvent e = (RevealEvent)it.next();
                int oldEventTurn = e.getTurn();
                int oldPlayerNr  = e.getPlayerNr();
            
                if (turnNr-oldEventTurn > expireTurns-(playerNr>=oldPlayerNr?1:0))
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
        synchronized(syncdEventList)
        {
            syncdEventList.clear();
        }
    }
    
    public void dispose()
    {
        saveWindow.save(this);
        cleanup();
        super.dispose();
    }

    public void setVisibleMaybe()
    {
        boolean visible = options.getOption(Options.showEventViewer);
        setVisible(visible);
    }
    
    public void setVisible(boolean visible)
    {
        if (visible)
        {
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
                this.visible = false;
            }
        }
        super.setVisible(visible);
    }
    
    public synchronized void actionPerformed(ActionEvent e)
    {
        // A combo box was changed.
        Object source = e.getSource();
        if ( source == maxTurnsDisplayExpiringBox )
        {
            String value = (String) maxTurnsDisplayExpiringBox.getSelectedItem();
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
