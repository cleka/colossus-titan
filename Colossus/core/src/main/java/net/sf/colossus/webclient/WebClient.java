package net.sf.colossus.webclient;


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.Client.ConnectionInitException;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.server.INotifyWebServer;
import net.sf.colossus.server.Server;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.webclient.WebClientSocketThread.WcstException;
import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IGameRunner;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;
import net.sf.colossus.webcommon.User;
import net.sf.colossus.webcommon.GameInfo.GameState;


/**
 *  This is the main class for one user client for the web server.
 *  One such client can register and/or login to the web server,
 *  propose a game, browse proposed games and enroll to such a game.
 *  When a game has enough players, it can be started, and this
 *  brings up the MasterBoard like the network client would do.
 *
 *  @author Clemens Katzer
 */
public class WebClient extends KFrame implements IWebClient
{
    private static final Logger LOGGER = Logger.getLogger(WebClient.class
        .getName());

    // TODO make this all based on Locale.getDefault()
    // Initially: use German. To make it variable, need also to set
    // the default values / info texts according to Locale.
    final static Locale myLocale = Locale.GERMANY;

    final static String TYPE_SCHEDULED = "scheduled";
    final static String TYPE_INSTANTLY = "instantly";

    private final WhatNextManager whatNextManager;

    private String hostname;
    private int port;
    private String login;
    private String username;
    private String password;

    private boolean isAdmin = false;

    private final Options options;
    private Client gameClient;
    private RunGameInSameJVM gameRunner;
    private Server localServer = null;
    private String startedGameId = null;
    private int startedAtPort;
    private String startedAtHost;

    private RegisterPasswordPanel registerPanel;

    private final static int NotLoggedIn = 1;
    private final static int LoggedIn = 2;
    // Enrolled to an instant game
    private final static int EnrolledInstantGame = 3;
    private final static int Playing = 4;
    // private final static int PlayingButDead = 5;

    // boundaries which port nr user may enter in the ServerPort field:
    private final static int minPort = 1;
    private final static int maxPort = 65535;

    // needed basically only to validate field contents (they must not contain
    // the separator)
    private final static String sep = IWebServer.WebProtocolSeparator;

    private boolean failedDueToDuplicateLogin = false;

    private int state = NotLoggedIn;
    private String enrolledInstantGameId = null;
    private boolean scheduledGamesMode;

    private int usersLoggedIn = 0;
    private int usersEnrolled = 0;
    private int usersPlaying = 0;
    private int usersDead = 0;
    private long usersLogoffAgo = 0;
    private String usersText = "";

    private IWebServer server = null;
    private WebClientSocketThread wcst = null;

    private JTabbedPane tabbedPane;
    private Box serverTab;
    private JPanel preferencesPane;
    private Box createGamesTab;
    private Box runningGamesTab;
    private Box adminTab;

    private final Point defaultLocation = new Point(600, 100);

    private JLabel statusLabel;
    private JLabel userinfoLabel; // Server/Login pane:

    private JTextField webserverHostField;
    private JTextField webserverPortField;
    private JTextField loginField;
    private JPasswordField passwordField;

    private JTextField commandField;
    private JLabel receivedField;

    private JButton loginLogoutButton;
    private JButton quitButton;

    private JCheckBox autologinCB;
    private JCheckBox autoGamePaneCB;

    private JLabel registerOrPasswordLabel;
    private JButton registerOrPasswordButton;

    private JButton debugSubmitButton;
    private JButton shutdownButton;

    private JLabel statusField;
    private String statusText = "";

    // Game browsing pane:
    private JComboBox variantBox;
    private JComboBox viewmodeBox;
    private JComboBox eventExpiringBox;

    private JSpinner spinner1;
    private JSpinner spinner2;
    private JSpinner spinner3;

    private JCheckBox unlimitedMulligansCB;
    private JCheckBox balancedTowersCB;

    private JLabel nowDateAndTimeLabel;
    private JTextField atDateField;
    private JTextField atTimeField;
    private JTextField durationField;
    private JTextField summaryText;
    private DateFormat myDateFormat;
    private DateFormat myTimeFormat;

    private JButton proposeButton;
    private JButton cancelButton;
    private JButton enrollButton;
    private JButton unenrollButton;
    private JButton startButton;
    private JButton startLocallyButton;

    private JButton hideButton;
    private JLabel hideButtonText;

    private JRadioButton autoGSNothingRB;
    private JRadioButton autoGSHideRB;
    private JRadioButton autoGSCloseRB;

    private JLabel infoTextLabel;
    final static String needLoginText = "You need to login to browse or propose Games.";
    final static String enrollText = "Propose or Enroll, and when enough players have enrolled, one of them can press 'Start'.";
    final static String startingText = "Game is starting, MasterBoard should appear soon. Please wait...";
    final static String startedText = "Game was started...";
    final static String waitingText = "Client connected successfully, waiting for all other players. Please wait...";
    final static String enrolledText = "While enrolled, you can't propose or enroll to other games.";
    final static String playingText = "While playing, you can't propose or enroll to other games.";

    private ChatHandler generalChat;

    private final ArrayList<GameInfo> gamesUpdates = new ArrayList<GameInfo>();

    /**
     * NOTE: shared with SocketThread, because WCST needs it to restore
     * game tokens to an GameInfo object
     */
    private final HashMap<String, GameInfo> gameHash = new HashMap<String, GameInfo>();

    private JPanel gamesTablesPanel;
    private JPanel gamesCards;
    private JPanel schedGamesCard;
    private JPanel instGamesCard;

    // scheduled games
    private JTable schedGameTable;
    private GameTableModel schedGameDataModel;

    // instant games (can start as soon as enough players have joined):
    private JTable instGameTable;
    private GameTableModel instGameDataModel;

    // running games
    private JTable runGameTable;
    private GameTableModel runGameDataModel;
    // private ListSelectionModel runGameListSelectionModel;

    private static String windowTitle = "Web Client";

    private final static String LoginButtonText = "Login";
    private final static String LogoutButtonText = "Logout";
    private final static String quitButtonText = "Quit";
    private final static String HideButtonText = "Hide Web Client";
    private final static String CantHideText = "(You can hide web client only if game client is open)";
    private final static String HowtoUnhideText = "You can get web client back from MasterBoard - Window menu";

    private final static String createAccountButtonText = "Register";
    private final static String chgPasswordButtonText = "Change password";

    private final static String ProposeButtonText = "Propose";
    private final static String EnrollButtonText = "Enroll";
    private final static String UnenrollButtonText = "Unenroll";
    private final static String CancelButtonText = "Cancel";
    private final static String StartButtonText = "Start";
    private final static String StartLocallyButtonText = "Start locally";

    private final static String AutoLoginCBText = "Auto-login on start";
    private final static String AutoGamePaneCBText = "After login Game pane";

    private final static String createAccountLabelText = "No login yet? Create one:";
    private final static String chgPasswordLabelText = "Change your password:";

    private final static String AutoGameStartActionNothing = "Do nothing";
    private final static String AutoGameStartActionHide = "Hide WebClient";
    private final static String AutoGameStartActionClose = "Close WebClient";

    private final static String optAutoGameStartAction = "Auto Game Start Action";

    private final static String defaultSummaryText = "Type here a short "
        + "summary what kind of game you would wish to play";

    public WebClient(WhatNextManager whatNextManager, String hostname,
        int port, String login, String password)
    {
        super(windowTitle);

        this.whatNextManager = whatNextManager;

        options = new Options(Constants.OPTIONS_WEB_CLIENT_NAME);
        options.loadOptions();

        // Initialize those 4 values + username from either given
        // arguments, loaded from cf file, or reasonable default.
        initValues(hostname, port, login, password);

        ViableEntityManager.register(this, "WebClient " + login);
        net.sf.colossus.util.InstanceTracker.register(this, "WebClient "
            + login);

        if (SwingUtilities.isEventDispatchThread())
        {
            setupGUI();
            autoActions();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        setupGUI();
                        autoActions();
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

    private void initValues(String hostname, int port, String login,
        String password)
    {
        if (hostname != null && !hostname.equals(""))
        {
            this.hostname = hostname;
        }
        else
        {
            String cfHostname = options.getStringOption(Options.webServerHost);
            if (cfHostname != null && !cfHostname.equals(""))
            {
                this.hostname = cfHostname;
            }
            else
            {
                this.hostname = Constants.defaultWebServer;
            }
        }

        if (port > 0)
        {
            this.port = port;
        }
        else
        {
            int cfPort = options.getIntOption(Options.webServerPort);
            if (cfPort >= 1)
            {
                this.port = cfPort;
            }
            else
            {
                this.port = Constants.defaultWebPort;
            }
        }

        // Initialize this already here, password login depends on it.
        String cfLogin = options.getStringOption(Options.webClientLogin);

        // Use -m argument, if given; otherwise from cf or default.
        if (login != null && !login.equals(""))
        {
            this.login = login;
        }
        else
        {
            if (cfLogin != null && !cfLogin.equals(""))
            {
                this.login = cfLogin;
            }
            else
            {
                this.login = Constants.username;
            }
        }
        // Right now, login and username are the same, but we may change
        // that one point.
        this.username = this.login;

        // right now password is never given but...
        if (password != null)
        {
            this.password = password;
        }
        else
        {
            String cfPassword = options
                .getStringOption(Options.webClientPassword);
            if (cfPassword != null && !cfPassword.equals("")
                &&
                // Use stored password only if its same user:
                cfLogin != null && cfLogin.equals(this.login)
                && !this.login.equals(""))
            {
                this.password = cfPassword;
            }
            else
            {
                // For debug/development purposes (when usernames do not
                // match), instead of empty password use the one from property
                // - this is helpful because I usually need 2 or more clients
                // and without this here I would have to type in a real
                // password for at least one of them, every time...
                String DEBUG_PASSWD_PROP = "wcpasswd";
                String debugPassword = System.getProperty(DEBUG_PASSWD_PROP);
                if (debugPassword != null)
                {
                    this.password = debugPassword;
                }
                else
                {
                    this.password = "";
                }
            }
        }
    }

    public void setGameClient(Client c)
    {
        this.gameClient = c;
        if (c == null)
        {
            hideButton.setEnabled(false);
            hideButtonText.setText(CantHideText);
            if (state == Playing)
            {
                state = LoggedIn;
                enrolledInstantGameId = "";
                updateGUI();
            }
        }
        else
        {
            hideButton.setEnabled(true);
            hideButtonText.setText(HowtoUnhideText);
        }
    }

    private void setScheduledGamesMode(boolean scheduled)
    {
        scheduledGamesMode = scheduled;
        atDateField.setEnabled(scheduled);
        atTimeField.setEnabled(scheduled);
        durationField.setEnabled(scheduled);

        if (instGamesCard == null || schedGamesCard == null
            || gamesCards == null)
        {
            return;
        }
        CardLayout cl = (CardLayout)(gamesCards.getLayout());
        cl.show(gamesCards, scheduled ? TYPE_SCHEDULED : TYPE_INSTANTLY);
    }

    public boolean getScheduledGamesMode()
    {
        return scheduledGamesMode;
    }

    public void onGameStartAutoAction()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            doAutoGSAction();
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    doAutoGSAction();
                }
            });
        }
    }

    private void setupGUI()
    {
        getContentPane().setLayout(new BorderLayout());

        // Top of the frame: login and users status/infos:
        Box headPane = new Box(BoxLayout.Y_AXIS);
        statusLabel = new JLabel("login status");
        userinfoLabel = new JLabel("user info status");
        headPane.add(statusLabel);
        headPane.add(userinfoLabel);
        getContentPane().add(headPane, BorderLayout.NORTH);

        // A tabbed pane for the various tabs in the CENTER:
        tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(900, 600)); // width x height
        tabbedPane.setMinimumSize(new Dimension(900, 530)); // width x height

        // Box mainPane = new Box(BoxLayout.Y_AXIS);
        // mainPane.add(tabbedPane);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Now the different tabs of the tabbed pane:
        createServerTab();
        tabbedPane.addTab("Server", serverTab);

        createCreateGamesTab();
        tabbedPane.addTab("Create or Join", createGamesTab);

        createRunningGamesTab();
        tabbedPane.addTab("Running Games", runningGamesTab);

        generalChat = new ChatHandler(IWebServer.generalChatName, "Chat",
            server, username);
        tabbedPane.addTab(generalChat.getTitle(), generalChat.getTab());

        createAdminTab();
        // adminTab is added to tabbedPane then/only when user has
        // logged in and server informed us that this user is admin user

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                WhatNextManager whatNextManager = WebClient.this.whatNextManager;
                whatNextManager.setWhatToDoNext(
                    WhatToDoNext.GET_PLAYERS_DIALOG, false);
                dispose();
            }
        });

        // TODO read from preferences?
        // setScheduledGamesMode(getScheduledGamesMode());

        // now finish all
        pack();

        useSaveWindow(options, "WebClient", defaultLocation);
        setVisible(true);
    }

    private void autoActions()
    {
        if (autologinCB.isSelected())
        {
            String login = loginField.getText();
            String password = new String(passwordField.getPassword());

            // Eclipse warning says password can never be null. Well...
            if (login != null && !login.equals("") && !password.equals(""))
            {
                doLogin();
            }
        }
        else
        {
            doUpdateGUI();
        }
    }

    private void doAutoGSAction()
    {
        String whatToDo = options.getStringOption(optAutoGameStartAction);
        if (whatToDo == null)
        {
            return;
        }

        if (whatToDo.equals(AutoGameStartActionNothing))
        {
            // ok, nothing to do.
        }
        else if (whatToDo.equals(AutoGameStartActionHide))
        {
            this.setVisible(false);
        }
        else if (whatToDo.equals(AutoGameStartActionClose))
        {
            WebClient.this.whatNextManager.setWhatToDoNext(
                WhatToDoNext.GET_PLAYERS_DIALOG, false);
            dispose();
        }
        else
        {
            LOGGER.log(Level.WARNING,
                "ooops! auto Game Start Action option is '" + whatToDo
                    + "' ???");
        }
    }

    public void updateStatus(String text, Color color)
    {
        this.statusText = text;
        statusField.setText(text);
        statusField.setForeground(color);
    }

    private void addAdminTab()
    {
        tabbedPane.addTab("Admin", adminTab);
    }

    private void removeAdminTab()
    {
        tabbedPane.remove(adminTab);
    }

    private void setAdmin(boolean isAdmin)
    {
        this.isAdmin = isAdmin;
        if (this.isAdmin)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addAdminTab();
                }
            });
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeAdminTab();
                }
            });
        }
    }

    public boolean isAdmin()
    {
        return isAdmin;
    }

    public void showAnswer(String s)
    {
        receivedField.setText(s);
    }

    public String getHost()
    {
        return webserverHostField.getText();
    }

    public String getPort()
    {
        return webserverPortField.getText();
    }

    public IGameRunner getGameRunner()
    {
        return gameRunner;
    }

    public INotifyWebServer getWhomToNotify()
    {
        return gameRunner;
    }

    private void createServerTab()
    {
        serverTab = new Box(BoxLayout.Y_AXIS);

        Box connectionPane = new Box(BoxLayout.X_AXIS);

        JPanel loginPane = new JPanel(new GridLayout(0, 2));
        loginPane.setBorder(new TitledBorder("Connection information"));

        loginPane.setPreferredSize(new Dimension(150, 200));

        loginPane.add(new JLabel("Web Server"));
        webserverHostField = new JTextField(this.hostname);
        webserverHostField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                webserverHostField.selectAll();
            }
        });

        // webserverHostField.addActionListener(this);
        loginPane.add(webserverHostField);

        loginPane.add(new JLabel("Port"));
        webserverPortField = new JTextField(this.port + "");
        webserverPortField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                webserverPortField.selectAll();
            }
        });

        // webserverPortField.addActionListener(this);
        loginPane.add(webserverPortField);

        loginPane.add(new JLabel("Login id"));
        loginField = new JTextField(this.login);
        loginField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                loginField.selectAll();
            }
        });

        // nameField.addActionListener(this);
        loginPane.add(loginField);

        loginPane.add(new JLabel("Password"));
        passwordField = new JPasswordField(this.password);
        // passwordField.addActionListener(this);
        loginPane.add(passwordField);
        passwordField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                passwordField.selectAll();
            }
        });

        loginLogoutButton = new JButton(LoginButtonText);
        loginLogoutButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                loginLogoutButtonAction(e.getActionCommand());
            }
        });

        loginLogoutButton.setEnabled(true);
        loginPane.add(loginLogoutButton);

        quitButton = new JButton(quitButtonText);
        quitButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                quitButtonAction();
            }
        });

        quitButton.setEnabled(true);
        loginPane.add(quitButton);

        loginPane.add(new JLabel("Status:"));
        statusField = new JLabel(statusText);
        loginPane.add(statusField);
        updateStatus("Not connected", Color.red);

        serverTab.add(connectionPane);

        connectionPane.add(loginPane);
        connectionPane.add(Box.createHorizontalGlue());
        connectionPane.add(Box.createVerticalGlue());

        serverTab.add(Box.createVerticalGlue());
        serverTab.add(Box.createHorizontalGlue());

        boolean alos = this.options.getOption(AutoLoginCBText);
        autologinCB = new JCheckBox(AutoLoginCBText, alos);
        autologinCB.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(AutoLoginCBText, autologinCB.isSelected());
            }
        });

        loginPane.add(autologinCB);
        loginPane.add(new JLabel(""));

        boolean algp = this.options.getOption(AutoGamePaneCBText);
        autoGamePaneCB = new JCheckBox(AutoGamePaneCBText, algp);
        autoGamePaneCB.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(AutoGamePaneCBText, autoGamePaneCB
                    .isSelected());
            }
        });

        loginPane.add(autoGamePaneCB);
        loginPane.add(new JLabel(""));

        // Label can show: registerLabelText or chgPasswordLabelText
        registerOrPasswordLabel = new JLabel(createAccountLabelText);
        // Button can show: createAccountButtonText or chgPasswordButtonText
        registerOrPasswordButton = new JButton(createAccountButtonText);
        registerOrPasswordButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                registerOrPasswordButtonAction(e.getActionCommand());
            }
        });

        loginPane.add(registerOrPasswordLabel);
        loginPane.add(registerOrPasswordButton);
    }

    private void addRadioButton(Container cont, ButtonGroup group,
        String text, String current, ItemListener listener)
    {
        // use same word for cmd as the text on it:
        String cmd = text;

        JRadioButton rb = new JRadioButton(text);
        if (cmd != null && !cmd.equals(""))
        {
            rb.setActionCommand(cmd);
        }
        rb.addItemListener(listener);
        group.add(rb);
        cont.add(rb);
        boolean selected = (text.equals(current));
        rb.setSelected(selected);
    }

    private JLabel nonBoldLabel(String text)
    {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.PLAIN));
        // l.setAlignmentX(Box.RIGHT_ALIGNMENT);
        return l;
    }

    private Box makeTextBox(Component c)
    {
        Box labelBox = new Box(BoxLayout.X_AXIS);
        labelBox.add(c);
        labelBox.add(Box.createHorizontalGlue());
        return labelBox;
    }

    private void initFormats()
    {
        myDateFormat = DateFormat
            .getDateInstance(DateFormat.MEDIUM, myLocale);
        myDateFormat.setTimeZone(TimeZone.getDefault());
        myDateFormat.setLenient(false);

        myTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, myLocale);
        myTimeFormat.setTimeZone(TimeZone.getDefault());
        myTimeFormat.setLenient(false);
    }

    private String makeDateTimeInfoString(Calendar now)
    {
        String nowDateString = myDateFormat.format(now.getTime());
        String nowTimeString = myTimeFormat.format(now.getTime());

        String infoString = "Current date and time is:  " + nowDateString
            + "  " + nowTimeString;
        return infoString;
    }

    private void updateDateTimeInfoString()
    {
        Calendar now = Calendar.getInstance(myLocale);
        String nowDateAndTimeInfoString = makeDateTimeInfoString(now);
        nowDateAndTimeLabel.setText(nowDateAndTimeInfoString);
    }

    private void createCreateGamesTab()
    {
        createGamesTab = new Box(BoxLayout.Y_AXIS);

        createPreferencesPane();
        createGamesTab.add(preferencesPane);

        Box proposeGamePane = new Box(BoxLayout.Y_AXIS);
        proposeGamePane.setAlignmentX(Box.CENTER_ALIGNMENT);
        proposeGamePane.setBorder(new TitledBorder("Creating games:"));

        proposeGamePane
            .add(makeTextBox(nonBoldLabel("Set your preferences, fill in "
                + "the 'Summary' text, then press 'Propose' to create a game:")));

        proposeGamePane.add(Box.createRigidArea(new Dimension(0, 20)));
        proposeGamePane.add(makeTextBox(new JLabel("Summary: ")));
        summaryText = new JTextField(defaultSummaryText);
        summaryText.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                summaryText.selectAll();
            }
        });
        proposeGamePane.add(summaryText);

        ButtonGroup group = new ButtonGroup();
        Box scheduleModes = new Box(BoxLayout.Y_AXIS);
        // NOTE: the actual radioButtons will be added later, see below

        proposeGamePane.add(Box.createRigidArea(new Dimension(0, 20)));
        proposeGamePane.add(makeTextBox(new JLabel("Choose the start time:")));
        proposeGamePane.add(makeTextBox(scheduleModes));

        // The panel with all GUI stuff needed to schedule a game:
        Box schedulingPanel = new Box(BoxLayout.Y_AXIS);
        schedulingPanel.setAlignmentY(Box.TOP_ALIGNMENT);

        Calendar now = Calendar.getInstance(myLocale);

        initFormats();
        String nowDateAndTimeInfoString = makeDateTimeInfoString(now);

        nowDateAndTimeLabel = nonBoldLabel(nowDateAndTimeInfoString);
        schedulingPanel
            .add(makeTextBox(nowDateAndTimeLabel));

        nowDateAndTimeLabel.setText(nowDateAndTimeInfoString);

        schedulingPanel
            .add(makeTextBox(nonBoldLabel("Give a start date and time (dd.mm.yyyy and hh:mm) "
                + "and a minimum duration in minutes:")));

        // The panel for the actual schedule: date, time and duration fields
        Box schedulePanel = new Box(BoxLayout.X_AXIS);
        schedulePanel.add(new JLabel("Start at: "));

        int days = 0;
        int hours = 24;

        Calendar defaultStart = getNowPlusOffset(now, days, hours);
        String defaultStartDateString = myDateFormat.format(defaultStart
            .getTime());

        atDateField = new JTextField(defaultStartDateString);
        schedulePanel.add(atDateField);

        String defaultStartTimeString = myTimeFormat.format(defaultStart
            .getTime());

        atTimeField = new JTextField(defaultStartTimeString);
        schedulePanel.add(atTimeField);

        schedulePanel.add(new JLabel(" Duration: "));
        durationField = new JTextField("90");
        schedulePanel.add(durationField);
        schedulePanel.setAlignmentY(Box.TOP_ALIGNMENT);

        schedulingPanel.add(schedulePanel);
        schedulingPanel
            .add(makeTextBox(nonBoldLabel("(the purpose of the duration value is: ")));
        schedulingPanel
            .add(makeTextBox(nonBoldLabel(" one should only enroll to that game if one "
                + "knows that one "
                + " will be available for at least that time)")));
        schedulingPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        proposeGamePane.add(schedulingPanel);

        // Panel for the propose + cancel buttons, left most field empty:
        JPanel pcButtonPane = new JPanel(new GridLayout(0, 3));
        proposeGamePane.add(pcButtonPane);
        pcButtonPane.add(new JLabel(""));
        proposeButton = new JButton(ProposeButtonText);
        proposeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                proposeButtonAction();
            }
        });

        proposeButton.setEnabled(false);
        pcButtonPane.add(proposeButton);
        cancelButton = new JButton(CancelButtonText);
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                cancelButtonAction();
            }
        });

        cancelButton.setEnabled(false);
        pcButtonPane.add(cancelButton);

        createGamesTab.add(proposeGamePane);

        // Panel for the enroll + unenroll buttons:
        Box joinGamePane = new Box(BoxLayout.Y_AXIS);
        joinGamePane.setBorder(new TitledBorder(
            "Joining games someone else has proposed:"));
        joinGamePane
            .add(makeTextBox(nonBoldLabel("Select a game from the table below, "
                + "and then click enroll to register for that game.")));

        JPanel euButtonPane = new JPanel(new GridLayout(0, 3));
        euButtonPane.add(new JLabel(""));
        enrollButton = new JButton(EnrollButtonText);
        enrollButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                enrollButtonAction();
            }
        });
        enrollButton.setEnabled(false);
        euButtonPane.add(enrollButton);

        unenrollButton = new JButton(UnenrollButtonText);
        unenrollButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                unenrollButtonAction();
            }
        });

        unenrollButton.setEnabled(false);
        euButtonPane.add(unenrollButton);
        joinGamePane.add(euButtonPane);

        createGamesTab.add(joinGamePane);

        gamesTablesPanel = new JPanel(new BorderLayout());
        createGamesTab.add(gamesTablesPanel);

        // One panel, either the one for instant games, or the one for
        // scheduled games, will be added later based on the "current"
        // value of the radiobutton.
        startButton = new JButton(StartButtonText);
        startButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                startButtonAction();
            }
        });

        startButton.setEnabled(false);

        startLocallyButton = new JButton(StartLocallyButtonText);
        startLocallyButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                startLocallyButtonAction();
            }
        });

        startLocallyButton.setEnabled(false);

        Box startButtonPane = new Box(BoxLayout.X_AXIS);
        startButtonPane.add(startButton);
        // startButtonPane.add(startLocallyButton);
        startButtonPane.add(Box.createHorizontalGlue());

        gamesCards = new JPanel(new CardLayout());
        gamesTablesPanel.add(gamesCards, BorderLayout.CENTER);

        // Panel/Table for upcoming instant games:
        instGamesCard = new JPanel(new BorderLayout());
        instGamesCard.setBorder(new TitledBorder("Instant Games"));
        instGamesCard.add(
            nonBoldLabel("The following games are accepting players:"),
            BorderLayout.NORTH);

        instGameDataModel = new GameTableModel(myLocale);
        instGameTable = new JTable(instGameDataModel);

        instGameTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    updateGUI();
                }
            });

        instGameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane instScrollpane = new JScrollPane(instGameTable);
        instGamesCard.add(instScrollpane, BorderLayout.CENTER);

        // Table for scheduled games:
        schedGamesCard = new JPanel(new BorderLayout());
        schedGamesCard.setBorder(new TitledBorder("Scheduled Games"));
        schedGamesCard.add(
            nonBoldLabel("The following are already scheduled:"),
            BorderLayout.NORTH);

        schedGameDataModel = new GameTableModel(myLocale);
        schedGameTable = new JTable(schedGameDataModel);

        schedGameTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    updateGUI();
                }
            });

        schedGameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane schedScrollpane = new JScrollPane(schedGameTable);
        schedGamesCard.add(schedScrollpane, BorderLayout.CENTER);

        JPanel dummyCard = new JPanel(new BorderLayout());
        dummyCard.add(Box.createRigidArea(new Dimension(0, 50)));

        gamesCards.add(instGamesCard, TYPE_INSTANTLY);
        gamesCards.add(schedGamesCard, TYPE_SCHEDULED);
        gamesCards.add(dummyCard, "dummy");

        CardLayout cl = (CardLayout)gamesCards.getLayout();
        cl.show(gamesCards, "dummy");

        Box bottomPanel = new Box(BoxLayout.Y_AXIS);
        bottomPanel.add(startButtonPane);
        infoTextLabel = new JLabel(enrollText);
        bottomPanel.add(infoTextLabel);
        gamesTablesPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Now that we have the scheduling fields and the tables,
        // we can add the buttons; because now the fields and tables
        // can be enabled or disabled based on "current" (initial) state
        // of the radio buttons:
        String current = TYPE_INSTANTLY;
        ItemListener iListener = new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                reactOnScheduleRadioButtonChange(e);
            }
        };
        addRadioButton(scheduleModes, group, TYPE_INSTANTLY, current,
            iListener);
        addRadioButton(scheduleModes, group, TYPE_SCHEDULED, current,
            iListener);

    }

    /**
     * Determine a point in time given amount of days and hours from now.
     * Round it to a full hour (down if min <= 10, next hour otherwise).
     *
     * @param days
     * @param hours
     * @return
     */
    private Calendar getNowPlusOffset(Calendar now, int days, int hours)
    {
        Calendar nowPlusOffset = now;
        nowPlusOffset.add(Calendar.DAY_OF_MONTH, days);
        nowPlusOffset.add(Calendar.HOUR_OF_DAY, hours);

        // Round to full hour: down if <= 10
        int min = nowPlusOffset.get(Calendar.MINUTE);
        if (min > 10)
        {
            nowPlusOffset.add(Calendar.HOUR_OF_DAY, 1);
        }
        nowPlusOffset.set(Calendar.MINUTE, 0);

        return nowPlusOffset;
    }

    public void reactOnScheduleRadioButtonChange(ItemEvent e)
    {
        if (atDateField == null)
        {
            // called too early (during creation of buttons)
            return;
        }

        boolean switchToScheduling = false;
        Object o = e.getItem();
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
        if (!selected)
        {
            // ignore the DESELECT - we act based on what was selected.
            return;
        }

        if (o instanceof JRadioButton)
        {
            JRadioButton b = (JRadioButton)o;
            String text = b.getText();
            if (text == null)
            {
                LOGGER.warning("ItemEvent Object Text is null???");
                return;
            }
            if (text.equals(TYPE_SCHEDULED))
            {
                switchToScheduling = true;
            }
            else if (text.equals(TYPE_INSTANTLY))
            {
                switchToScheduling = false;
            }
            else
            {
                LOGGER
                    .warning("ItemEvent Object Text is neither 'scheduled' nor 'instantly'??");
                return;
            }
        }
        else
        {
            LOGGER.warning("ItemEvent Object is not a JRadioButton??");
            return;
        }
        setScheduledGamesMode(switchToScheduling);
        if (switchToScheduling)
        {
            /*
            JOptionPane
                .showMessageDialog(
                    this,
                    "Please note! The 'scheduled games' feature is not ready implemented,\n"
                        + "and trying to use it might cause the application to behave unpredictable...",
                    "Feature not ready!", JOptionPane.INFORMATION_MESSAGE);
            */
        }
        updateGUI();
    }

    private void createPreferencesPane()
    {
        preferencesPane = new JPanel(new GridLayout(0, 2));
        preferencesPane.setBorder(new TitledBorder("Game preferences"));

        // Variant:
        String variantName = options.getStringOption(Options.variant);
        if (variantName == null || variantName.length() == 0)
        {
            variantName = Constants.variantArray[0]; // Default variant
        }

        variantBox = new JComboBox(Constants.variantArray);
        variantBox.setSelectedItem(variantName);
        variantBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(Options.variant, (String)variantBox
                    .getSelectedItem());
            }
        });

        preferencesPane.add(new JLabel("Select variant:"));
        preferencesPane.add(variantBox);

        // Viewmode:
        // String viewmodesArray[] = { "all-public", "auto-tracked", "only-own" };
        String viewmodeName = options.getStringOption(Options.viewMode);
        if (viewmodeName == null)
        {
            viewmodeName = Options.viewableAll;
        }

        viewmodeBox = new JComboBox(Options.viewModeArray);
        viewmodeBox.setSelectedItem(viewmodeName);
        viewmodeBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(Options.viewMode, (String)viewmodeBox
                    .getSelectedItem());
            }
        });

        preferencesPane.add(new JLabel("Select view mode:"));
        preferencesPane.add(viewmodeBox);

        // event expiring policy:
        String eventExpiringVal = options
            .getStringOption(Options.eventExpiring);
        if (eventExpiringVal == null)
        {
            eventExpiringVal = "5";
        }

        eventExpiringBox = new JComboBox(Options.eventExpiringChoices);
        eventExpiringBox.setSelectedItem(eventExpiringVal);
        eventExpiringBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(Options.eventExpiring,
                    (String)eventExpiringBox.getSelectedItem());
            }
        });

        preferencesPane.add(new JLabel("Events expire after (turns):"));
        preferencesPane.add(eventExpiringBox);

        // checkboxes (unlimited mulligans and balanced tower):
        Box checkboxPane = new Box(BoxLayout.X_AXIS);
        boolean unlimitedMulligans = options
            .getOption(Options.unlimitedMulligans);
        unlimitedMulligansCB = new JCheckBox(Options.unlimitedMulligans,
            unlimitedMulligans);
        unlimitedMulligansCB.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(Options.unlimitedMulligans,
                    unlimitedMulligansCB.isSelected());
            }
        });

        boolean balancedTowers = options.getOption(Options.balancedTowers);
        balancedTowersCB = new JCheckBox(Options.balancedTowers,
            balancedTowers);
        balancedTowersCB.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(Options.balancedTowers, balancedTowersCB
                    .isSelected());
            }
        });

        checkboxPane.add(unlimitedMulligansCB);
        checkboxPane.add(balancedTowersCB);

        preferencesPane.add(new JLabel("Various settings:"));
        preferencesPane.add(checkboxPane);

        // min, target and max nr. of players:
        preferencesPane.add(new JLabel("Select player count preferences:"));
        Box playerSelection = new Box(BoxLayout.X_AXIS);

        int min = options.getIntOption(Options.minPlayersWeb);
        min = (min < 2 || min > 6 ? 2 : min);

        int max = options.getIntOption(Options.maxPlayersWeb);
        max = (max < min || max > 6 ? 6 : max);

        int middle = java.lang.Math.round(((float)min + (float)max) / 2);

        int targ = options.getIntOption(Options.targPlayersWeb);
        targ = (targ < min || targ > max ? middle : targ);

        playerSelection.add(new JLabel("min.:"));
        SpinnerNumberModel model = new SpinnerNumberModel(min, 2, 6, 1);
        spinner1 = new JSpinner(model);
        playerSelection.add(spinner1);
        // spinner uses ChangeListener instead of ActionListener.
        // Setting them up is quite laborous, and would be called every time
        // user modifies it by one. So, we rather query the value then when we
        // need it (before exiting (saveOptions) or before propose).
        //spinner.addActionListener(this);

        playerSelection.add(new JLabel("target.:"));
        SpinnerNumberModel model2 = new SpinnerNumberModel(targ, 2, 6, 1);
        spinner2 = new JSpinner(model2);
        playerSelection.add(spinner2);

        playerSelection.add(new JLabel("max.:"));
        SpinnerNumberModel model3 = new SpinnerNumberModel(max, 2, 6, 1);
        spinner3 = new JSpinner(model3);
        playerSelection.add(spinner3);

        preferencesPane.add(playerSelection);
    }

    private void createRunningGamesTab()
    {
        runningGamesTab = new Box(BoxLayout.Y_AXIS);

        // ----------------- First the table ---------------------

        Box runningGamesPane = new Box(BoxLayout.Y_AXIS);
        runningGamesPane.setAlignmentY(0);
        runningGamesPane.setBorder(new TitledBorder("Running Games"));
        runningGamesPane.add(new JLabel(
            "The following games are already running:"));

        runGameDataModel = new GameTableModel(myLocale);
        runGameTable = new JTable(runGameDataModel);
        runGameTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    updateGUI();
                }
            });

        runGameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane runtablescrollpane = new JScrollPane(runGameTable);
        runningGamesPane.add(runtablescrollpane);

        runningGamesTab.add(runningGamesPane);

        // ------------------ Hide WebClient stuff ---------------

        Box hideClientPanel = new Box(BoxLayout.Y_AXIS);
        hideClientPanel.setBorder(new TitledBorder("Hiding the Web Client"));

        runningGamesTab.add(Box.createRigidArea(new Dimension(0, 20)));
        runningGamesTab.add(Box.createVerticalGlue());

        hideClientPanel.setAlignmentX(Box.LEFT_ALIGNMENT);

        hideButton = new JButton(HideButtonText);
        hideButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                hideButtonAction();
            }
        });
        hideButton.setEnabled(false);
        hideButton.setAlignmentX(Box.LEFT_ALIGNMENT);
        hideClientPanel.add(hideButton);
        hideButtonText = new JLabel(CantHideText);
        hideClientPanel.add(hideButtonText);

        // automatic actions when game starts (client masterboard comes up):
        JLabel autoDoLabel = new JLabel("When game starts, automatically:");
        autoDoLabel.setAlignmentX(Box.LEFT_ALIGNMENT);
        hideClientPanel.add(autoDoLabel);
        Box autoDoButtonPane = new Box(BoxLayout.X_AXIS);
        autoGSNothingRB = new JRadioButton(AutoGameStartActionNothing);
        autoGSHideRB = new JRadioButton(AutoGameStartActionHide);
        autoGSCloseRB = new JRadioButton(AutoGameStartActionClose);
        autoDoButtonPane.add(autoGSNothingRB);
        autoDoButtonPane.add(autoGSHideRB);
        autoDoButtonPane.add(autoGSCloseRB);

        ActionListener autoGSActionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options
                    .setOption(optAutoGameStartAction, e.getActionCommand());
            }
        };

        autoGSNothingRB.addActionListener(autoGSActionListener);
        autoGSHideRB.addActionListener(autoGSActionListener);
        autoGSCloseRB.addActionListener(autoGSActionListener);

        ButtonGroup autoGSActionGroup = new ButtonGroup();
        autoGSActionGroup.add(autoGSNothingRB);
        autoGSActionGroup.add(autoGSHideRB);
        autoGSActionGroup.add(autoGSCloseRB);

        String autoGSAction = options.getStringOption(optAutoGameStartAction);
        if (autoGSAction == null || autoGSAction.equals(""))
        {
            autoGSAction = AutoGameStartActionNothing;
            options.setOption(optAutoGameStartAction, autoGSAction);
        }

        if (autoGSAction.equals(AutoGameStartActionNothing))
        {
            autoGSNothingRB.setSelected(true);
        }
        else if (autoGSAction.equals(AutoGameStartActionHide))
        {
            autoGSHideRB.setSelected(true);
        }
        else if (autoGSAction.equals(AutoGameStartActionClose))
        {
            autoGSCloseRB.setSelected(true);
        }
        else
        {
            autoGSAction = AutoGameStartActionNothing;
            options.setOption(optAutoGameStartAction, autoGSAction);
            autoGSNothingRB.setSelected(true);
        }

        autoDoButtonPane.setAlignmentX(Box.LEFT_ALIGNMENT);
        hideClientPanel.add(autoDoButtonPane);
        hideClientPanel.add(Box.createVerticalGlue());

        runningGamesTab.add(Box.createVerticalGlue());
        runningGamesTab.add(hideClientPanel);

        /*      // Somehow this does not work at all...

                // as wide as the running games table, as high as needed:
                int width = runningGamesPane.getMinimumSize().width;
                int height = hideClientPanel.getMinimumSize().height;
                Dimension prefSize = new Dimension(width, height);
                hideClientPanel.setPreferredSize(prefSize);
                hideClientPanel.setMinimumSize(prefSize);
        */

    }

    private void createAdminTab()
    {
        adminTab = new Box(BoxLayout.Y_AXIS);

        JPanel adminPane = new JPanel(new GridLayout(0, 1));
        adminPane.setBorder(new TitledBorder("Admin mode"));
        adminPane.setPreferredSize(new Dimension(30, 200));

        commandField = new JTextField("");
        adminPane.add(commandField);

        debugSubmitButton = new JButton("Submit");
        debugSubmitButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                debugSubmitButtonAction();
            }
        });
        debugSubmitButton.setEnabled(false);
        adminPane.add(debugSubmitButton);

        adminPane.add(new JLabel("Server answered:"));
        receivedField = new JLabel("");
        adminPane.add(receivedField);

        shutdownButton = new JButton("Shutdown Server");
        shutdownButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                shutdownButtonAction();
            }
        });
        adminPane.add(shutdownButton);

        adminTab.add(adminPane);
    }

    public String createLoginWebClientSocketThread(boolean force)
    {
        String reason = null;
        failedDueToDuplicateLogin = false;

        // email is null: WCST does login
        wcst = new WebClientSocketThread(this, hostname, port, username,
            password, force, null, null, gameHash);
        WcstException e = wcst.getException();
        if (e == null)
        {
            wcst.start();
            server = wcst;

            updateStatus("Logged in", Color.green);
        }
        else
        {
            // I would have liked to let the constructor throw an exception
            // and catch this here, but then the returned pointer was null,
            // so could not do anything with it (and start() not be run),
            // so GC did not clean it up. Sooo... let's do it this way,
            // a little bit clumsy...

            if (wcst.stillNeedsRun())
            {
                wcst.start();
            }

            wcst = null;
            server = null;

            reason = e.getMessage();

            if (reason == null)
            {
                reason = "Unknown reason";
            }

            // if it is the duplicate login case: if force was not set, give
            // user a 2nd chance to login with force, without the original message.
            if (!force && e.failedBecauseAlreadyLoggedIn())
            {
                failedDueToDuplicateLogin = true;
                return reason;
            }

            // otherwise just show what's wrong
            JOptionPane.showMessageDialog(this, reason);
            updateStatus("Login failed", Color.red);
            return reason;
        }

        return reason;
    }

    public String createRegisterWebClientSocketThread(String username,
        String password, String email, String confCode)
    {
        LOGGER.info("Creating a RegisterWCST, username " + username
            + " password " + password + " and confcode " + confCode);

        String reason = null;
        boolean force = false; // dummy

        // 1) confCode is not null: WCST does the confirmation
        // 2) email is NOT null: WCST does register first instead
        // 3) otherwise: normal login
        wcst = new WebClientSocketThread(this, hostname, port, username,
            password, force, email, confCode, gameHash);

        WcstException e = wcst.getException();
        if (e == null)
        {
            LOGGER.info("Account for user" + username
                + " created successfully!");
            wcst.start();
            server = wcst;
            updateStatus("Successfully registered", Color.green);
            JOptionPane.showMessageDialog(registerPanel,
                "Account was created successfully!\nYou can Login now.",
                "Registration OK", JOptionPane.INFORMATION_MESSAGE);
            loginField.setText(username);
            passwordField.setText(password);
            WebClient.this.login = username;
            WebClient.this.username = username;
            WebClient.this.password = password;

            registerPanel.dispose();
        }
        else
        {
            reason = e.getMessage();
            if (reason == null)
            {
                reason = "Unknown reason";
            }

            if (reason.equals(User.PROVIDE_CONFCODE))
            {
                LOGGER.info("As expected, server asks us now for "
                    + "the confirmation code for user " + username);
            }
            else
            {
                LOGGER.info("Failed to create account for user " + username
                    + "; reason: '" + reason + "'");
            }

            // Register password panel handles this from here on,
            // namely let the user input the confirmation code and
            // submitting it.
        }

        return reason;
    }

    private boolean logout()
    {
        boolean success = false;

        server.logout();
        server = null;
        wcst = null;

        updateStatus("Not connected", Color.red);
        return success;
    }

    private void doQuit()
    {
        if (gameClient != null)
        {
            // Game client handles confirmation if necessary,
            // asks what to do next, and sets startObj accordingly,
            // and it also disposes this WebClient window.
            gameClient.getGUI().doConfirmAndQuit();
        }
        else
        {
            whatNextManager.setWhatToDoNext(WhatToDoNext.QUIT_ALL, true);
            dispose();
        }
    }

    @Override
    public void dispose()
    {
        // we have a server ( = a WebClientSocketThread)
        // if and only if we are logged in.
        if (server != null)
        {
            doLogout();
        }

        super.dispose();

        int min = ((Integer)spinner1.getValue()).intValue();
        int target = ((Integer)spinner2.getValue()).intValue();
        int max = ((Integer)spinner3.getValue()).intValue();
        options.setOption(Options.minPlayersWeb, min);
        options.setOption(Options.maxPlayersWeb, max);
        options.setOption(Options.targPlayersWeb, target);

        // options.setStringOption(Options.)
        options.saveOptions();

        if (gameClient != null)
        {
            gameClient.getGUI().setWebClient(null);
            gameClient = null;
        }

        gameHash.clear();
        synchronized (gamesUpdates)
        {
            gamesUpdates.clear();
        }

        ViableEntityManager.unregister(this);
    }

    private String getUserinfoText()
    {
        String text;
        if (state == NotLoggedIn)
        {
            text = "<unknown>";
        }
        else if (usersLoggedIn <= 1)
        {
            text = "No other users logged in.";
        }
        else
        {
            text = usersLoggedIn + " logged in (" + usersText + ").";
            // just to get rid of the "never read locally" warning...:
            String dummy = (usersEnrolled + usersPlaying + usersDead + usersLogoffAgo)
                + usersText;
            LOGGER.log(Level.FINEST, "Loggedin: " + usersLoggedIn
                + ", others dummy: " + dummy);
            // // Server doesn't tell actual values for the other stuff yet.
            // text = usersLoggedIn + " logged in, of that " +
            //     usersEnrolled + " enrolled, " +
            //     usersPlaying + "playing and " +
            //     usersDead + "playing but eliminated.";
        }
        return text;
    }

    public void updateGUI()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                doUpdateGUI();
            }
        });

    }

    public String getSelectedGameId()
    {
        String id = null;

        if (getScheduledGamesMode())
        {
            int selRow = schedGameTable.getSelectedRow();
            if (selRow != -1)
            {
                id = (String)schedGameTable.getValueAt(selRow, 0);
            }
        }
        else
        {
            int selRow = instGameTable.getSelectedRow();
            if (selRow != -1)
            {
                id = (String)instGameTable.getValueAt(selRow, 0);
            }
        }
        return id;
    }

    private String makeWindowTitleForState(int state)
    {
        switch (state)
        {
            case NotLoggedIn:
                return "(not logged in)";

            case LoggedIn:
                return username + " (logged in)";

            case EnrolledInstantGame:
                return username + " (enrolled)";

            case Playing:
                return username + " (playing)";

            default:
                return "<window title - undefined state?>";
        }
    }

    private String makeInfoTextForState(int state)
    {
        switch (state)
        {
            case NotLoggedIn:
                return needLoginText;

            case LoggedIn:
                return enrollText;

            case EnrolledInstantGame:
                return enrolledText;

            case Playing:
                return playingText;

            default:
                return "<info text - undefined state?>";
        }

    }

    // TODO mostly duplicate with makeWindowTitleForState
    private String makeStatusTextForState(int state)
    {
        switch (state)
        {
            case NotLoggedIn:
                return "not logged in";

            case LoggedIn:
                return "logged in as " + username;

            case EnrolledInstantGame:
                return "As " + username + " - enrolled to game "
                    + enrolledInstantGameId;

            case Playing:
                return "As " + username + " - playing game "
                    + enrolledInstantGameId;

            default:
                return "<info text - undefined state?>";
        }

    }

    private boolean checkIfCouldStartOnServer(int state)
    {
        switch (state)
        {
            case EnrolledInstantGame:
                GameInfo gi = findGameById(enrolledInstantGameId);
                if (gi != null)
                {
                    if (gi.getEnrolledCount().intValue() >= gi.getMin()
                        .intValue())
                    {
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    LOGGER.warning("Huuuh? UpdateGUI, Enrolled, but get game "
                        + "from hash for enrolledGameId is null??");
                }
                return false;

            case LoggedIn:
                boolean startPossible = false;
                String id = getSelectedGameId();
                if (id != null && isScheduledGameStartable(id))
                {
                    startPossible = true;
                }
                return startPossible;

            case NotLoggedIn:
            case Playing:
            default:
                return false;
        }

    }

    private boolean isScheduledGameStartable(String id)
    {
        assert id != null : "Not a valid game id: " + id;

        GameInfo gi = findGameById(id);

        if (gi == null)
        {
            return false;
        }

        if (gi.isEnrolled(username) && gi.hasEnoughPlayers()
            && gi.isDue()
            && gi.allEnrolledOnline())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean checkIfCouldCancel(int state)
    {
        if (state == LoggedIn)
        {
            String selectedGameId = getSelectedGameId();
            if (selectedGameId != null && isOwner(selectedGameId))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    private boolean checkIfCouldEnroll(int state)
    {
        if (getScheduledGamesMode())
        {
            String selectedGameId = getSelectedGameId();
            if (selectedGameId == null)
            {
                return false;
            }
            GameInfo gi = findGameById(selectedGameId);
            if (gi != null && !gi.isEnrolled(username))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            if (state == LoggedIn && getSelectedGameId() != null)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    private boolean checkIfCouldUnenroll(int state)
    {
        if (getScheduledGamesMode())
        {
            String selectedGameId = getSelectedGameId();
            if (selectedGameId == null)
            {
                return false;
            }
            GameInfo gi = findGameById(selectedGameId);
            if (gi != null && gi.isEnrolled(username))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else if (state == EnrolledInstantGame)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    // this should always be called inside a invokeLater (i.e. in EDT)!!
    public void doUpdateGUI()
    {
        // =============================================================
        // First calculate new state for all kind of things...

        String newTitle = makeWindowTitleForState(state);
        String newInfoText = makeInfoTextForState(state);
        String newStatusText = makeStatusTextForState(state);

        boolean couldPropose = (state == LoggedIn);
        boolean couldDebugSubmit = (state == LoggedIn);
        boolean couldCancel = checkIfCouldCancel(state);
        boolean couldEnroll = checkIfCouldEnroll(state);
        boolean couldUnenroll = checkIfCouldUnenroll(state);
        boolean couldStartOnServer = checkIfCouldStartOnServer(state);
        // feature currently disabled (( => hardcoded to false)):
        boolean couldStartLocally = false;

        // ----------------------------------------------------------------
        // ... and now actually change the GUI

        if (!newTitle.equals(""))
        {
            this.setTitle(windowTitle + " " + newTitle);
        }

        // Server tab
        loginLogoutButton.setText(state != NotLoggedIn ? LogoutButtonText
            : LoginButtonText);

        if (state != NotLoggedIn)
        {
            registerOrPasswordLabel.setText(chgPasswordLabelText);
            registerOrPasswordButton.setText(chgPasswordButtonText);
        }
        else
        {
            registerOrPasswordLabel.setText(createAccountLabelText);
            registerOrPasswordButton.setText(createAccountButtonText);
        }

        // Games tab
        updateDateTimeInfoString();

        userinfoLabel.setText("Userinfo: " + getUserinfoText());
        statusLabel.setText("Status: " + newStatusText);
        infoTextLabel.setText(newInfoText);

        proposeButton.setEnabled(couldPropose);
        cancelButton.setEnabled(couldCancel);
        enrollButton.setEnabled(couldEnroll);
        unenrollButton.setEnabled(couldUnenroll);

        startButton.setEnabled(couldStartOnServer);
        startLocallyButton.setEnabled(couldStartLocally);

        // Chat tab
        generalChat.setLoginState(state != NotLoggedIn, server, username);

        // Admin tab
        debugSubmitButton.setEnabled(couldDebugSubmit);
    }

    // SocketThread needs this to find games when "reinstantiating" it
    // from tokens got from server
    public HashMap<String, GameInfo> getGameHash()
    {
        return gameHash;
    }

    private GameInfo findGameById(String gameId)
    {
        GameInfo gi = gameHash.get(gameId);
        if (gi == null)
        {
            LOGGER.log(Level.SEVERE, "Game from hash is null!!");
        }

        return gi;
    }

    private boolean isOwner(String gameId)
    {
        GameInfo gi = findGameById(gameId);
        String initiator = gi.getInitiator();
        return (username.equals(initiator));
    }

    /* Validate that the given field does not contain any substring which
     * could cause a wrong splitting it by separator at the recipients side.
     *
     * As the separator currently is " ~ ", in practice this means
     * the userName and password must not start or end with whitespaces
     * or the '~' character, nor contain the separator as a whole.
     *
     * If invalid, displays a message box telling what is wrong and returns
     * false; if valid, returns true.
     */

    public boolean validateField(Component parent, String content,
        String fieldName)
    {
        String problem = null;
        String temp = content.trim();

        if (!temp.equals(content))
        {
            problem = fieldName + " must not start or end with whitespaces!";
        }
        else if (temp.equalsIgnoreCase(""))
        {
            problem = fieldName + " is missing!";
        }
        else if (temp.equalsIgnoreCase("null"))
        {
            problem = fieldName
                + " must not be the string 'null', no matter which case!";
        }
        else if (content.indexOf(sep) != -1)
        {
            problem = fieldName + " must not contain the string '" + sep
                + "'!";
        }
        else
        {
            for (int i = 0; i < sep.length() && problem == null; i++)
            {
                String critChar = sep.substring(i, i + 1);

                if (content.startsWith(critChar))
                {
                    problem = fieldName + " must not start with '" + critChar
                        + "'!";
                }
                else if (content.endsWith(critChar))
                {
                    problem = fieldName + " must not end with '" + critChar
                        + "'!";
                }
            }
        }

        if (problem != null)
        {
            JOptionPane.showMessageDialog(parent, problem);
            return false;
        }
        return true;
    }

    boolean validatePort(Component parent, String portText)
    {
        boolean ok = true;
        int port = -1;
        try
        {
            port = Integer.parseInt(portText);
            if (port < minPort || port > maxPort)
            {
                ok = false;
            }
        }
        catch (Exception e)
        {
            ok = false;
        }

        if (!ok)
        {
            JOptionPane.showMessageDialog(parent, "Invalid port number!");
        }
        return ok;
    }

    public void doLogin()
    {
        boolean ok = validateServerAndPort();

        ok = ok
            && validateField(this, loginField.getText(), "Login name")
            && validateField(this, new String(passwordField.getPassword()),
                "Password");

        if (!ok)
        {
            return;
        }

        this.login = loginField.getText();
        this.username = this.login;
        this.password = new String(passwordField.getPassword());

        // first try without force
        String message = createLoginWebClientSocketThread(false);
        if (message != null && failedDueToDuplicateLogin)
        {
            Object[] options = { "Force", "Cancel" };
            int answer = JOptionPane.showOptionDialog(this,
                "Server has already/still another connection open with "
                    + "that login name. Click Force to forcefully logout the "
                    + "other connection, or Cancel to abort.",
                "WebClient login: Force logout of other connection?",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[1]);

            if (answer == 0)
            {
                // if user clicked 'Force', try to connect/login with force argument
                message = createLoginWebClientSocketThread(true);
            }
        }
        if (message == null)
        {
            state = LoggedIn;
            loginField.setEnabled(false);
            updateGUI();

            options.setOption(Options.webServerHost, this.hostname);
            options.setOption(Options.webServerPort, this.port);
            options.setOption(Options.webClientLogin, this.login);
            options.setOption(Options.webClientPassword, this.password);

            options.saveOptions();
            if (autoGamePaneCB.isSelected())
            {
                tabbedPane.setSelectedComponent(createGamesTab);
            }
        }
        else
        {
            LOGGER.log(Level.FINEST, "connect/login failed...");
        }
    }

    public boolean validateServerAndPort()
    {
        String portText = webserverPortField.getText();
        boolean ok = validatePort(this, portText)
            && validateField(this, webserverHostField.getText(), "Host name");

        if (ok)
        {
            this.port = Integer.parseInt(webserverPortField.getText());
            this.hostname = webserverHostField.getText();
        }

        return ok;
    }

    public void doLogout()
    {
        if (state == EnrolledInstantGame)
        {
            doUnenroll(enrolledInstantGameId);
        }

        cancelOwnInstantGameOnLogout();

        logout();

        schedGameTable.removeAll();
        instGameTable.removeAll();
        runGameTable.removeAll();

        gameHash.clear();
        state = NotLoggedIn;
        setAdmin(false);
        loginField.setEnabled(true);

        updateGUI();
        options.setOption(Options.webClientLogin, this.login);
        options.setOption(Options.webClientPassword, this.password);

        options.saveOptions();
    }

    private void cancelOwnInstantGameOnLogout()
    {
        for (GameInfo gi : gameHash.values())
        {
            if (!gi.isScheduledGame()
                && gi.getGameState().equals(GameState.PROPOSED)
                && gi.getInitiator().equals(username))
            {
                server.cancelGame(gi.getGameId(), username);
            }
        }
    }

    private void doRegisterOrPasswordDialog(boolean register)
    {
        boolean ok = validateServerAndPort();
        if (ok)
        {
            String username = loginField.getText();
            registerPanel = new RegisterPasswordPanel(this, register, username);
            registerPanel.packAndShow();
        }
    }

    public String tryChangePassword(String name, String oldPW, String newPW1)
    {
        // email and isAdminObj are null, this signals: do not change them
        String email = null;
        Boolean isAdminObj = null;

        String reason = server.changeProperties(name, oldPW, newPW1, email,
            isAdminObj);

        if (reason == null || reason.equals("null"))
        {
            passwordField.setText(newPW1);
            password = newPW1;
            // all went fine, panel shows ok.
            return null;
        }
        else
        {
            // panel shows failure message and reason
            return reason;
        }
    }

    private void doCancel(String gameId)
    {
        server.cancelGame(gameId, username);
        updateGUI();
    }

    public void doScheduleDummy()
    {
        // just a dummy as long as we still have ScheduledGamesTab class...
    }

    private void do_proposeGame(String variant, String viewmode, long startAt,
        int duration, String summary, String expire, boolean unlimMulli,
        boolean balTowers, int min, int target, int max)
    {
        server.proposeGame(username, variant, viewmode, startAt, duration,
            summary, expire, unlimMulli, balTowers, min, target, max);
    }

    private long getStartTime()
    {
        long when = -1;

        String atDate = atDateField.getText();
        String atTime = atTimeField.getText();

        String schedule = atDate + " " + atTime;

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
        catch (ParseException e)
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

    private String getSummaryText()
    {
        return summaryText.getText();
    }

    private boolean doEnroll(String gameId)
    {
        server.enrollUserToGame(gameId, username);
        return true;
    }

    private boolean doUnenroll(String gameId)
    {
        server.unenrollUserFromGame(gameId, username);
        return true;
    }

    // Called when user presses the "Start" button in
    // "Create or Join" tab
    boolean doStart(String gameId)
    {
        startButton.setEnabled(false);
        startLocallyButton.setEnabled(false);
        server.startGame(gameId);

        return true;
    }

    // Called when user presses the "Start Locally" button in
    // "Create or Join" tab
    private boolean doStartLocally(String gameId)
    {
        startButton.setEnabled(false);
        startLocallyButton.setEnabled(false);

        boolean ok = true;
        GameInfo gi = findGameById(gameId);

        gameRunner = new RunGameInSameJVM(gi, whatNextManager, username, this);
        gameRunner.start();

        return ok;
    }

    public void informStartingOnPlayerHost(String hostingPlayer,
        String hostingHost, int hostingPort)
    {
        server.startGameOnPlayerHost(getSelectedGameId(), hostingPlayer,
            hostingHost, hostingPort);
    }

    public void informGameStartedLocally()
    {
        server.informStartedByPlayer(this.startedGameId);
    }

    public void informLocallyGameOver()
    {
        server.informLocallyGameOver(this.startedGameId);
    }

    public void setLocalServer(Server server)
    {
        localServer = server;
    }

    // ================= those come from server ============

    public void grantAdminStatus()
    {
        setAdmin(true);
    }

    public void didEnroll(String gameId, String user)
    {
        GameInfo gi = findGameById(gameId);
        boolean scheduled = gi.isScheduledGame();

        if (scheduled)
        {
            int index = schedGameDataModel.getRowIndex(gi).intValue();
            schedGameTable.setRowSelectionInterval(index, index);
        }
        else
        {
            state = EnrolledInstantGame;
            enrolledInstantGameId = gameId;

            int index = instGameDataModel.getRowIndex(gi).intValue();
            instGameTable.setRowSelectionInterval(index, index);
        }
        updateGUI();
    }

    public void didUnenroll(String gameId, String user)
    {
        // do not set it back to LoggedIn if this didUnenroll is the
        // result of a automatic unenroll before logout
        if (state != NotLoggedIn)
        {
            state = LoggedIn;
        }
        enrolledInstantGameId = "";

        updateGUI();
    }

    public void gameStartsSoon(String gameId)
    {
        startButton.setEnabled(false);
        startLocallyButton.setEnabled(false);
        infoTextLabel.setText(startingText);
    }

    // TODO Remove?
    public void gameStarted(String gameId)
    {
        startButton.setEnabled(false);
        startLocallyButton.setEnabled(false);
        infoTextLabel.setText(startedText);
    }

    private final Object comingUpMutex = new Object();
    private boolean clientIsUp = false;

    // Client calls this
    public void notifyComingUp()
    {
        synchronized (comingUpMutex)
        {
            clientIsUp = true;
            comingUpMutex.notify();
        }
    }

    private boolean timeIsUp = false;

    private Timer setupTimer()
    {
        // java.util.Timer, not Swing Timer
        Timer timer = new Timer();
        timeIsUp = false;
        long timeout = 60; // secs

        timer.schedule(new TriggerTimeIsUp(), timeout * 1000);
        return timer;
    }

    class TriggerTimeIsUp extends TimerTask
    {
        @Override
        public void run()
        {
            timeIsUp = true;
            synchronized (comingUpMutex)
            {
                comingUpMutex.notify();
            }
        }
    }

    public void gameStartsNow(String gameId, int port, String hostingHost)
    {
        if (hostingHost == null || hostingHost.equals("null"))
        {
            // Hosted on Game Server
            hostingHost = hostname;
        }

        // For now, just always use runnable (that's why the "&& false")
        // TODO Is the runnable necessary?
        if (startedGameId == null && false)
        {
            // is null means: it was not this webclient that started locally
            startOwnClient(gameId, port, hostingHost);
        }
        // This WebClient did start it...
        else
        {
            // ... then we need to start the Client in own runnable,
            // otherwise we (in WebClientSocketThread) will not be
            // back at the socket to receive the "Game is now up" message
            startedAtPort = port;
            startedAtHost = hostingHost;
            startedGameId = gameId;

            Runnable r = new Runnable()
            {
                public void run()
                {
                    String gameId = WebClient.this.startedGameId;
                    int port = WebClient.this.startedAtPort;
                    String host = WebClient.this.startedAtHost;

                    startOwnClient(gameId, port, host);
                }
            };
            new Thread(r).start();
            // System.out
            //     .println("++ Runnable to start the Game Client now started...\n");
        }
    }

    public void startOwnClient(String gameId, int port, String hostingHost)
    {
        LOGGER.info("StartingOwnClient for gameId " + gameId + " hostingHost "
            + hostingHost + " port " + port);

        Client gc;
        try
        {
            int p = port;
            String type;

            if (hostingHost == null || hostingHost.equals(""))
            {
                // Game runs on WebServer
                hostingHost = hostname;
                type = Constants.network;
            }
            else
            {
                // Runs on a players computer
                type = Constants.human;
            }

            boolean noOptionsFile = false;
            // System.out.println("in webclient, before new Client for username "
            //     + username);

            gc = Client.createClient(hostingHost, p, username, type,
                whatNextManager, localServer, true, noOptionsFile, true);

            infoTextLabel.setText(waitingText);
            setGameClient(gc);
            gc.getGUI().setWebClient(this);

            Timer timeoutStartup = setupTimer();

            while (!clientIsUp && !timeIsUp)
            {
                synchronized (comingUpMutex)
                {
                    try
                    {
                        // System.out.println("before comingUpMutex.wait()");
                        comingUpMutex.wait();
                        // System.out.println("after  comingUpMutex.wait()");
                    }
                    catch (InterruptedException e)
                    {
                        // ignored
                    }
                    catch (Exception e)
                    {
                        // just to be sure...
                    }
                }
            }

            timeoutStartup.cancel();

            if (clientIsUp)
            {
                state = Playing;
                updateGUI();
                onGameStartAutoAction();
            }
            else
            {
                JOptionPane.showMessageDialog(this,
                    "Own client could connect, but game did not start "
                        + "(probably some other player connecting failed?)",
                    "Starting game failed!", JOptionPane.ERROR_MESSAGE);
                state = LoggedIn;
                updateGUI();
            }
        }
        catch (ConnectionInitException e)
        {
            gc = null;
            JOptionPane.showMessageDialog(this,
                "Connecting to the game server hosting the game ("
                    + hostingHost + ") or starting own MasterBoard failed!\n"
                    + "Reason: " + e.getMessage(), "Starting game failed!",
                JOptionPane.ERROR_MESSAGE);

            state = LoggedIn;
            updateGUI();
        }
        catch (Exception e)
        {
            gc = null;
            // client startup failed for some reason
            JOptionPane.showMessageDialog(this,
                "Unexpected exception while starting the game client: "
                    + e.toString(), "Starting game failed!",
                JOptionPane.ERROR_MESSAGE);
            state = LoggedIn;
            updateGUI();
        }
    }

    public void gameCancelled(String gameId, String byUser)
    {
        if (state == EnrolledInstantGame
            && enrolledInstantGameId.equals(gameId))
        {
            String message = "Game " + gameId + " was cancelled by user "
                + byUser;
            JOptionPane.showMessageDialog(this, message);
            state = LoggedIn;
            enrolledInstantGameId = "";
            updateGUI();
        }

        if (getScheduledGamesMode())
        {
            schedGameDataModel.removeGame(gameId);
        }
        else
        {
            instGameDataModel.removeGame(gameId);
        }
    }

    public void chatDeliver(String chatId, long when, String sender,
        String message, boolean resent)
    {
        if (chatId.equals(IWebServer.generalChatName))
        {
            generalChat.chatDeliver(when, sender, message, resent);
        }
        else
        {
            // chat delivery to chat other than general not implemented
        }
    }

    // Game Client tells us this when user closes the masterboard
    public void tellGameEnds()
    {
        state = LoggedIn;
        enrolledInstantGameId = "";
        updateGUI();
    }

    // Server tells us the amount of players in the different states
    public void userInfo(int loggedin, int enrolled, int playing, int dead,
        long ago, String text)
    {
        usersLoggedIn = loggedin;
        usersEnrolled = enrolled;
        usersPlaying = playing;
        usersDead = dead;
        usersLogoffAgo = ago;
        usersText = text;
        updateGUI();
    }

    /*
     * Server tells us news about games in "proposed" state
     * - created ones, enrolled player count changed, or
     *   when it is removed (cancelled or started)
     *
     **/
    public void gameInfo(GameInfo gi)
    {
        synchronized (gamesUpdates)
        {
            gamesUpdates.add(gi);
        }

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                synchronized (gamesUpdates)
                {
                    Iterator<GameInfo> it = gamesUpdates.iterator();
                    while (it.hasNext())
                    {
                        GameInfo game = it.next();
                        GameState state = game.getGameState();

                        switch (state)
                        {
                            case PROPOSED:
                                if (game.isScheduledGame())
                                {
                                    // System.out
                                    //    .println("Got a scheduled game, replacing in sched list");
                                    replaceInTable(schedGameTable, game);
                                }
                                else
                                {
                                    // System.out
                                    //     .println("Got an instant game, replacing in instant list");
                                    replaceInTable(instGameTable, game);
                                }
                                break;

                            case DUE:
                            case ACTIVATED:
                                break;

                            case STARTING:
                                infoTextLabel.setText(startingText);
                                break;

                            case READY_TO_CONNECT:

                                break;

                            case RUNNING:
                                startButton.setEnabled(false);
                                startLocallyButton.setEnabled(false);
                                infoTextLabel.setText(startedText);
                                // System.out
                                //     .println("Got a running game, replacing in run game list and remove in inst game list");
                                replaceInTable(runGameTable, game);
                                if (game.isScheduledGame())
                                {
                                    schedGameDataModel.removeGame(game
                                        .getGameId());
                                }
                                else
                                {
                                    instGameDataModel.removeGame(game
                                        .getGameId());
                                }

                                break;

                            case ENDING:
                                runGameDataModel.removeGame(game.getGameId());
                                break;

                            default:
                                LOGGER.log(Level.WARNING,
                                    "Huups, unhandled game state "
                                        + game.getStateString());

                        }
                    }
                    gamesUpdates.clear();
                }
                updateGUI();
            }
        });

    }

    private void replaceInTable(JTable table, GameInfo gi)
    {
        GameTableModel model = (GameTableModel)table.getModel();

        int index = model.getRowIndex(gi).intValue();
        model.setRowAt(gi, index);
        table.repaint();
    }

    private void resetTable(JTable table)
    {
        GameTableModel model = (GameTableModel)table.getModel();
        model.resetTable();
    }

    public void connectionReset(boolean forced)
    {
        String message = (forced ? "Some other connection to server with same login name forced your logout."
            : "Connection reset by server! You are logged out.");
        JOptionPane.showMessageDialog(this, message);
        setAdmin(false);
        state = NotLoggedIn;
        enrolledInstantGameId = "";
        receivedField.setText("Connection reset by server!");
        updateStatus("Not connected", Color.red);

        loginField.setEnabled(true);
        gameHash.clear();
        synchronized (gamesUpdates)
        {
            gamesUpdates.clear();
        }
        resetTable(instGameTable);
        resetTable(runGameTable);
        updateGUI();

        tabbedPane.setSelectedComponent(serverTab);
    }

    // ======================================================================
    // Below methods are called my GUI event listeners

    private void hideButtonAction()
    {
        setVisible(false);
    }

    private void quitButtonAction()
    {
        doQuit();
    }

    private void loginLogoutButtonAction(String command)
    {
        if (command.equals(LoginButtonText))
        {
            doLogin();
        }
        else if (command.equals(LogoutButtonText))
        {
            doLogout();
        }
        else
        {
            LOGGER.warning("unexpected command " + command
                + " on LoginButton?");
        }
    }

    private void shutdownButtonAction()
    {
        if (isAdmin())
        {
            server.shutdownServer();
        }
    }

    private void debugSubmitButtonAction()
    {
        String text = commandField.getText();
        ((WebClientSocketThread)server).submitAnyText(text);
        commandField.setText("");
    }

    private void registerOrPasswordButtonAction(String command)
    {
        // createAccountButtonText chgPasswordButtonText
        if (command.equals(createAccountButtonText))
        {
            doRegisterOrPasswordDialog(true);
        }
        else if (command.equals(chgPasswordButtonText))
        {
            doRegisterOrPasswordDialog(false);
        }
    }

    private void startLocallyButtonAction()
    {
        String selectedGameId = getSelectedGameId();
        if (selectedGameId != null)
        {
            boolean ok = doStartLocally(selectedGameId);
            if (ok)
            {
                schedGameTable.setEnabled(false);
                instGameTable.setEnabled(false);
            }
        }
    }

    private void startButtonAction()
    {
        String selectedGameId = getSelectedGameId();
        if (selectedGameId != null)
        {
            boolean ok = doStart(selectedGameId);
            if (ok)
            {
                schedGameTable.setEnabled(false);
                instGameTable.setEnabled(false);
            }
        }
    }

    private void cancelButtonAction()
    {
        String selectedGameId = getSelectedGameId();
        if (selectedGameId != null)
        {
            doCancel(selectedGameId);
        }
    }

    private void unenrollButtonAction()
    {
        String selectedGameId = getSelectedGameId();
        if (selectedGameId != null)
        {
            boolean ok = doUnenroll(selectedGameId);
            if (ok)
            {
                schedGameTable.setEnabled(true);
                instGameTable.setEnabled(true);
            }
        }
    }

    private void enrollButtonAction()
    {
        String selectedGameId = getSelectedGameId();
        if (selectedGameId != null)
        {
            boolean ok = doEnroll(selectedGameId);
            if (ok)
            {
                // instGameTable.setEnabled(false);
            }
        }
    }

    private void proposeButtonAction()
    {
        int min = ((Integer)spinner1.getValue()).intValue();
        int target = ((Integer)spinner2.getValue()).intValue();
        int max = ((Integer)spinner3.getValue()).intValue();

        boolean scheduled = getScheduledGamesMode();
        long startAt = scheduled ? getStartTime() : -1;
        int duration = getDuration();
        String summaryText = getSummaryText();

        do_proposeGame(variantBox.getSelectedItem().toString(), viewmodeBox
            .getSelectedItem().toString(), startAt, duration, summaryText,
            eventExpiringBox.getSelectedItem().toString(),
            unlimitedMulligansCB.isSelected(), balancedTowersCB.isSelected(),
            min, target, max);
    }
}
