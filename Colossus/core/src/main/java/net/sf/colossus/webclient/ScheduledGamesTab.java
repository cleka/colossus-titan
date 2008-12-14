package net.sf.colossus.webclient;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.colossus.webclient.GameTableModel;


/**
 *  The GUI component for scheduled games: the controls for 
 *  setting values and submitting a schedule, a table to display
 *  the already scheduled ones, and controls to enroll/unenroll/cancel. 
 */
class ScheduledGamesTab extends Box implements ActionListener
{
    private static final Logger LOGGER = Logger
        .getLogger(ScheduledGamesTab.class.getName());

    private final static String ScheduleButtonText = "Submit";

    private final WebClient webClient;
    private final Locale myLocale;

    private JTextField atDateField;
    private JTextField atTimeField;
    private JTextField durationField;
    private JTextField summaryText;
//    private JTextArea infoTextArea;
    public JButton submitButton;
    
    // scheduled games:
    private GameTableModel schedGameDataModel;
    private JTable schedGameTable;
    private ListSelectionModel schedGameListSelectionModel;

   
    ScheduledGamesTab(WebClient webClient, Locale myLocale)
    {
        super(BoxLayout.Y_AXIS);
     
        this.webClient = webClient;
        this.myLocale = myLocale;
        
        this.add(makeControlsPanel(this));
        this.add(Box.createVerticalGlue());
        this.add(makeScheduledGamesTablePanel());
    }

    public JTable getSchedGameTable()
    {
        return schedGameTable; 
    }

    public ListSelectionModel getschedGameListSelectionModel()
    {
        return schedGameListSelectionModel;
    }

    private Box makeControlsPanel(ActionListener listener)
    {
        Box controlPanel = new Box(BoxLayout.Y_AXIS);
        controlPanel.setBorder(new TitledBorder("Schedule a game: "));

        controlPanel.setAlignmentX(Box.LEFT_ALIGNMENT);
        controlPanel.setAlignmentY(Box.TOP_ALIGNMENT);

        controlPanel.add(new JLabel("Give a start date and time (dd.mm.yyyy and hh:mm) "
            + "and a minimum duration in minutes:"));
        Box schedulePanel = new Box(BoxLayout.X_AXIS);
        schedulePanel.add(new JLabel("Start at: "));
        
        atDateField = new JTextField("27.11.2008");
        schedulePanel.add(atDateField);
        
        atTimeField = new JTextField("10:00");
        schedulePanel.add(atTimeField);
                   
        schedulePanel.add(new JLabel(" Duration: "));
        durationField = new JTextField("90");
        schedulePanel.add(durationField);
        
        schedulePanel.setAlignmentX(Box.LEFT_ALIGNMENT);
        schedulePanel.setAlignmentY(Box.TOP_ALIGNMENT);
        
        controlPanel.add(schedulePanel);
             
        JLabel label1 = new JLabel("(the purpose of the duration value is: ");
        label1.setFont(label1.getFont().deriveFont(Font.PLAIN));
        controlPanel.add(label1);
        JLabel label2 = new JLabel(" one should only enroll to that game if one "
            + "knows that one "
            + " will be available for at least that time)");
        label2.setFont(label2.getFont().deriveFont(Font.PLAIN));
        controlPanel.add(label2);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        controlPanel.add(new JLabel("Summary: "));
        summaryText = new JTextField("short summary what kind of game");
        summaryText.setAlignmentX(Box.LEFT_ALIGNMENT);
        summaryText.setAlignmentY(Box.TOP_ALIGNMENT);
        controlPanel.add(summaryText);

        controlPanel.add(Box.createVerticalGlue());
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
/*
        JLabel label = new JLabel("Detailed info: ");
        label.setAlignmentX(Box.LEFT_ALIGNMENT);
        label.setAlignmentY(Box.TOP_ALIGNMENT);
        controlPanel.add(label);

        String text = "Type here some more details\n"
            + "(preferences which variant, "
            + "how many players, beginner or advanced, ...";
        infoTextArea = new JTextArea(text, 5, 60);
        infoTextArea.setWrapStyleWord(true);
        
        infoTextArea.setAlignmentX(Box.LEFT_ALIGNMENT);
        infoTextArea.setAlignmentY(Box.TOP_ALIGNMENT);
        controlPanel.add(infoTextArea);
*/            
        controlPanel.add(Box.createVerticalGlue());
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        Box submitPanel = new Box(BoxLayout.X_AXIS);
        submitPanel.add(Box.createHorizontalGlue());
        submitButton = new JButton(ScheduleButtonText);
        submitButton.setAlignmentX(Box.CENTER_ALIGNMENT);
        submitButton.setEnabled(false);
        submitPanel.add(submitButton);
        submitPanel.add(Box.createHorizontalGlue());
        controlPanel.add(submitPanel);

        controlPanel.add(Box.createVerticalGlue());
        
        submitButton.addActionListener(listener);
        
        return controlPanel;
    }

    private Container makeScheduledGamesTablePanel()
    {
        Box tablePanel = new Box(BoxLayout.Y_AXIS);
        tablePanel.setBorder(new TitledBorder("Schedules Games: "));
        
        Box schedGamesPane = new Box(BoxLayout.Y_AXIS);
        schedGamesPane.setBorder(new TitledBorder("Scheduled Games"));
        JLabel dummyField = new JLabel(
            "The following games were scheduled so far:");
        schedGamesPane.add(dummyField);

        schedGameDataModel = new GameTableModel(myLocale);
        schedGameTable = new JTable(schedGameDataModel);

        schedGameListSelectionModel = schedGameTable.getSelectionModel();
        ListSelectionListener listener = new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                // ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                System.out.println("update to scheduled game list selection model");
                // updateGUI();
            }
        };
        schedGameListSelectionModel.addListSelectionListener(listener);
        
        schedGameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tablescrollpane = new JScrollPane(schedGameTable);
        schedGamesPane.add(tablescrollpane);

        tablePanel.add(schedGamesPane);
        tablePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        tablePanel.add(new JButton("Enroll"));
        
        return tablePanel;
    }
    
    public void setLoginState(boolean loggedIn)
    {
        submitButton.setEnabled(loggedIn);
    }

    
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();

        if (command.equals(ScheduleButtonText))
        {
            long when = getStartTime();
            int duration = getDuration();
            String summary = getSummary();
            
            if (when == -1 || duration == -1 || summary == null
                || summary.trim().equals(""))
            {
                System.out.println("Invalud date, time, duration or summary text!");
            }
            else
            {
                webClient.doScheduling(when, duration, summary);
            }
        }
    }
    
    private long getStartTime()
    {
        long when = -1;
        
        String atDate = atDateField.getText();
        String atTime = atTimeField.getText();
        
        String schedule = atDate + " " + atTime;
        // System.out.println("schedule is " + schedule);
        
        
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
            DateFormat.SHORT, myLocale);
        df.setTimeZone(TimeZone.getDefault());
        df.setLenient(false);
        
        // System.out.println("default locale    is " + Locale.getDefault());
        // System.out.println("default time zone is " + TimeZone.getDefault());

        try
        {
            Date whenDate = df.parse(schedule);
            when = whenDate.getTime();
        }
        catch(ParseException e)
        {
            LOGGER.warning("Illegal date/time '" + schedule + "'");
        }
        
        return when;
    }
    
    private int getDuration()
    {
        int duration = -1;
        
        String durationString = durationField.getText();
        duration = Integer.parseInt(durationString);
        return duration;
    }
    
    private String getSummary()
    {
        return summaryText.getText();
    }
}
