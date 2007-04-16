package net.sf.colossus.client;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;

// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import javax.swing.*;
// import javax.swing.JScrollPane;
// import javax.swing.JTextArea;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;


import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


/**
 * Event Revealing dialog.
 *
 * It collects all revealed events and displays all or
 * only the recent ones of them.
 *
 *
 * @version $Id: EventViewer.java 2591 2006-01-01 23:41:26Z cleka $
 * @author Clemens Katzer
 */

final class EventViewer extends KDialog implements WindowListener,
ItemListener, ActionListener
{

    private IOptions options;
    
    // private int viewMode;
   
    private boolean visible;
    
    // private ArrayList eventList = new ArrayList();

    private List syncdEventList = Collections.synchronizedList(new ArrayList());
    
    
    private int turnNr;
    private int playerNr;
    
    private int expireTurns;
    
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
    public static final String evfWinner = "Battle won events";
    public static final String evfEliminated = "Battle lost events";
    public static final String evfMulligan = "Mulligans";
    
    public static final String evfTurnChange = "Turn change info";
    public static final String evfPlayerChange = "Player change info";

    public static final String evAutoScroll = "Auto-scroll to end";
    public static final String evMaxTurns = "Maximum number of turns to display";
    
    // boolean flags for them as local flags for quick access:
    // Index for this are the EventXXX constants in RevealEvent.java.
    private boolean[] showEventType;
    
    private boolean autoScroll;
    private JComboBox maxTurnsDisplayExpiringBox;
    
    private int maxTurns = 1;

    /** 
     * Inits the dialog, not necessarily displays it.
     * 
     * @param frame is the parent window
     * @param options
     * @viewMode viewMode, related to AutoInspector, currently not used
     * @expireTurns events older than that are deleted from list
     */

        
    EventViewer(final JFrame frame, final IOptions options, int viewMode, int expireTurns)
    {
        super(frame, windowTitle, false);
        setFocusable(false);

        this.expireTurns = expireTurns;
        this.options = options;
        // this.viewMode = viewMode;

        showEventType = new boolean[10];
        showEventType[RevealEvent.eventRecruit] = getBoolOption(evfRecruit, true);
        showEventType[RevealEvent.eventSplit] = getBoolOption(evfRecruit, true);
        showEventType[RevealEvent.eventTeleport] = getBoolOption(evfTeleport, true);
        showEventType[RevealEvent.eventSummon] = getBoolOption(evfSummon, true);
        showEventType[RevealEvent.eventAcquire] = getBoolOption(evfAcquire, true);
        showEventType[RevealEvent.eventWinner] = getBoolOption(evfWinner, false);
        showEventType[RevealEvent.eventEliminated] = getBoolOption(evfEliminated, false);

        showEventType[RevealEvent.eventMulligan] = getBoolOption(evfMulligan, true);
        showEventType[RevealEvent.eventTurnChange] = getBoolOption(evfTurnChange, true);
        showEventType[RevealEvent.eventPlayerChange] = getBoolOption(evfPlayerChange, false);
        
        autoScroll = getBoolOption(evAutoScroll, true);
        
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
        tabbedPane.setPreferredSize(new Dimension(220, 250));

        // Events:
        Box eventTabPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Events", eventTabPane);
        eventTabPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        eventTabPane.setPreferredSize(new Dimension(200, 400));      
        
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
        // addCheckbox(evfAcquire, checkboxPane);
        addCheckbox(evfWinner, checkboxPane);
        addCheckbox(evfEliminated, checkboxPane);
        checkboxPane.add(new JLabel("NOTE: Teleport events remain in list even if undone."));
        checkboxPane.add(Box.createRigidArea(new Dimension(0,5)));
        
        addCheckbox(evfMulligan, checkboxPane);
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
        miscPane.add(new JLabel(""));
        
        // selection for how many turns to display the data
        // (must be less or equal the expiry Option from getPlayers setting)
        String eventExpiringVal = options.getStringOption(Options.eventExpiring);
        if ( eventExpiringVal == null )
        {
            eventExpiringVal = "1"; 
        }
        int maxVal = Integer.parseInt(eventExpiringVal);

        ArrayList alChoices = new ArrayList();
        int i;
        for (i=1 ; i <= maxVal ; i++)
        {
            alChoices.add(new Integer(i).toString());
        }
                
        Object[] Choices = alChoices.toArray(); 

        // read user's setting for this, but cannot exceed the Game's
        // general setting.
        String maxTurnsOptString = 
            options.getStringOption(evMaxTurns);
        if ( maxTurnsOptString == null )
        {
            maxTurnsOptString = "3";
        }
        int maxTurnsOpt = Integer.parseInt(maxTurnsOptString);
        if (maxTurnsOpt > maxVal)
        {
            maxTurnsOpt = maxVal;
            maxTurnsOptString = new Integer(maxTurnsOpt).toString();
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


    private void addEventToEventPane(RevealEvent e)
    {
        Container pane = eventPane;
    
        // System.out.println("Adding event panel for event " + e.getEventTypeText());
            
        int oldEventTurn = e.getTurn();
        int oldPlayerNr  = e.getPlayerNr();
        int type = e.getEventType();
        boolean display = true;
        
        if (turnNr-oldEventTurn > maxTurns-(playerNr>=oldPlayerNr?1:0))
        {
//            System.out.println("Not displaying event "+e.getEventTypeText()+" "+e.getMarkerId() +
//            " - older than max turns value!");
            display = false;
        }
        else if ( !showEventType[type] )
        {
//          System.out.println("Not displaying event "+e.getEventTypeText()+" "+e.getMarkerId() +
//          " - type " + type + " false.");
            display = false;
        }
            
        if (display)
        {
            JPanel panelForEvent = e.toPanel();
            if (panelForEvent != null)
            {
                pane.add(panelForEvent);
                pane.add(Box.createRigidArea(new Dimension(0,5)));
            }
            else
            {
                System.out.println("WARNING: EventViewer.addEventToEventPane: event.toPanel returned null!!");
            }
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
            addEventToEventPane(e);
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
     */
    private void updatePanels()
    {
        Container pane = this.eventPane;

        pane.removeAll();
    
        synchronized(syncdEventList)
        {
            Iterator it = syncdEventList.iterator();
            while (it.hasNext())
            {
                RevealEvent e = (RevealEvent) it.next();
                addEventToEventPane(e);
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
        purgeOldEvents();
        if (this.visible)
        {
            updatePanels();
        }
    }

    

    /*
     * User undid one action. 
     * So far, this deletes that event from the event list and redisplays all. 
     * 
     * Better (long term plan/future improvement idea): 
     *   mark as "undone"; if viewMode/Inspector asks for "what do we know" 
     *   the information revealed by the undone event is 
     *   - even if undone - nevertheless "known to the public". As in real life :)
     * Until then, we simply remove e.g. Split and Recruit events.
     * They are removed, because they have side effect to their legion content.
     * Teleport event is not removed (not "necessary", because no side effects, 
     * and somewhat difficult to detect in client)
     */
    public void undoEvent(int type, String parentId, String childId, int turn, String recruitName)
    {
//        System.out.println("Undoing event "+type+ ": splitoff "+childId+
//                ", parent "+parentId+" turn "+turn);

        
        int found = 0;
        if (type == RevealEvent.eventSplit)
        {
            synchronized(syncdEventList)
            {
                Iterator it = syncdEventList.iterator();
                while (it.hasNext())
                {
                    RevealEvent e = (RevealEvent)it.next();
                    if (e.getEventType() == type && e.getTurn() == turn &&
                            e.getMarkerId().equals(parentId) &&
                            e.getMarkerId2().equals(childId) )

                    {
                        // System.out.println("Split event to be undone found.");
                        found++;
                        it.remove();
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
                    Iterator it2 = syncdEventList.iterator();
                    while (it2.hasNext())
                    {
                        RevealEvent e = (RevealEvent)it2.next();
                        if (e.getEventType() == type && e.getTurn()+1 == turn &&
                                e.getMarkerId().equals(parentId) &&
                                e.getMarkerId2().equals(childId)
                                )
                        {
                            // System.out.println("NOTE: Split event to be undone found only from previous turn!!");
                            found++;
                            it2.remove();
                        }
                    }
                }
            }
        }
        else if (type == RevealEvent.eventRecruit)
        {
            synchronized(syncdEventList)
            {
                Iterator it = syncdEventList.iterator();
                while (it.hasNext())
                {
                    RevealEvent e = (RevealEvent)it.next();
                    if (e.getEventType() == type && e.getTurn() == turn &&
                            e.getMarkerId().equals(parentId) )
                    {
                        // System.out.println("Recruit event to be undone found.");
                        found++;
                        it.remove();
                    }
                }
            }
        }
        else
        {
            System.out.println("WARNING: undo event for unknown type "+type+" attempted.");
            return;
        }
       
        if (found == 0)
        {
            System.out.println("WARNING: '"+type+"' EVENT to undo ("+
                    parentId+", "+childId+", turn "+turn+") not found");
            System.exit(1);
        }
        if (found > 1)
        {
            System.out.println("ERROR: '"+type+"' EVENT found " + found + " times!!");
            System.exit(1);
        }
        if (this.visible)
        {
            updatePanels();
        }
    }
    
    /* throw away all events which are expireTurns turns older
     * than the given turnNr/playerNr combination.
     */
    public void purgeOldEvents()
    {
//        System.out.println("Purging events, if necessary...");

        int purged = 0;

        synchronized(syncdEventList)
        {
            Iterator it = syncdEventList.iterator();
        
            while (it.hasNext())
            {
                RevealEvent e = (RevealEvent)it.next();
                int oldEventTurn = e.getTurn();
                int oldPlayerNr  = e.getPlayerNr();
            
//            System.out.println("turn "+turnNr+ "oldTurn "+oldEventTurn+" plNum" + 
//                    playerNr + " oldPlNum "+oldPlayerNr);
                if (turnNr-oldEventTurn > expireTurns-(playerNr>=oldPlayerNr?1:0))
                {
//                int oldEventPlayer = e.getPlayerNr();
//                System.out.println("Purging: newest event turn "+turnNr+", player" + playerNr +
//                        " - purging old event (turn="+oldEventTurn+", player " + oldEventPlayer + ")");
                    it.remove();
                    purged++;
                }
            }
        }
        if (purged > 0)
        {
            // System.out.println("Purged " + purged + " old events.");
            getContentPane().validate();
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
            saveWindow.restore(this, new Point(0,0));
            settingsPane.setMinimumSize(settingsPane.getSize());
            // eventPane.setMinimumSize(eventPane.getSize());
            this.visible = true;
            updatePanels();
        }
        else
        {
            saveWindow.save(this);
            this.visible = false;
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
            maxTurns = Integer.parseInt(value);
            updatePanels();
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

        if (text.equals(evfRecruit))
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
        else if (text.equals(evfWinner))
        {
            this.showEventType[RevealEvent.eventWinner] = selected;
        }
        else if (text.equals(evfEliminated))
        {
            this.showEventType[RevealEvent.eventEliminated] = selected;
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
        else if (text.equals(evfPlayerChange))
        {
            this.showEventType[RevealEvent.eventPlayerChange] = selected;
        }
        
        updatePanels();
    }
}
