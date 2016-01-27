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
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
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
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.Client.ConnectionInitException;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.gui.ColumnWidthPersistingJTable;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.server.INotifyWebServer;
import net.sf.colossus.server.Server;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.webclient.WebClientSocketThread.WcstException;
import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.GameInfo.GameState;
import net.sf.colossus.webcommon.IGameRunner;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;
import net.sf.colossus.webcommon.User;


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

    // 1: webclient understands deliverGeneralMessage
    // 2: webclient supports ping
    // 3: webclient knows DinoTitan variant;
    // 4: now all options can be selected in webclient, also
    //    teleport options added
    // 5: can properly display suspended games
    // 6: version from 6.1.2016. Scratch-reconnect stuff ongoing
    // 7: admin can delete suspened games via gui
    // 8: joinToWatch should work again. Also other changes that
    //    might help with the 'no BattleBoard' problem
    public final static int WC_VERSION_GENERAL_MESSAGE = 1;
    public final static int WC_VERSION_SUPPORTS_PING = 2;
    public final static int WC_VERSION_DINO_OK = 3;
    public final static int WC_VERSION_SUPPORTS_EXTRA_OPTIONS = 4;
    public final static int WC_VERSION_RESUME = 5;
    public final static int WC_VERSION_SCRATCH_RECONN_WIP = 6;
    public final static int WC_VERSION_DELETE_SUSPENDED_GAME = 7;
    public final static int WC_VERSION_WATCH_GAME_FIXED = 8;

    final static int WEB_CLIENT_VERSION = WC_VERSION_WATCH_GAME_FIXED;

    // TODO make this all based on Locale.getDefault()
    // Initially: use German. To make it variable, need also to set
    // the default values / info texts according to Locale.
    final static Locale myLocale = Locale.GERMANY;

    final static String CARD_PROPOSED = "proposed";

    final static String TYPE_SCHEDULED = "scheduled";
    final static String TYPE_INSTANTLY = "instantly";

    // they are used both as button text and key in Options
    public final static String AutoLoginCBText = "Auto-login on start";
    public final static String AutoGamePaneCBText = "After login Game pane";

    public static final String PLAYERS_MISSING = "MissingPlayers";

    private final WhatNextManager whatNextManager;

    private String hostname;
    private int port;
    private String login;
    private String username;
    private String password;
    private String email;

    private boolean isAdmin = false;

    private final Options options;
    private Client gameClient;
    private RunGameInSameJVM gameRunner;
    private Server localServer = null;
    private String startedGameId = null;
    private int startedAtPort;
    private String startedAtHost;

    private RegisterPasswordPanel registerPanel;

    private final Object comingUpMutex = new Object();
    private boolean timeIsUp;
    private boolean clientIsUp;
    private boolean clientStartFailed;

    private final static int NotLoggedIn = 1;
    private final static int LoggedIn = 2;
    // Enrolled to an instant game
    private final static int EnrolledInstantGame = 3;
    private final static int Playing = 4;
    private final static int Watching = 5;
    // private final static int PlayingButDead = 6;

    private boolean gameResumeInitiated = false;

    private GameInfo startingGame = null;

    // boundaries which port nr user may enter in the ServerPort field:
    private final static int minPort = 1;
    private final static int maxPort = 65535;

    // needed basically only to validate field contents (they must not contain
    // the separator)
    private final static String sep = IWebServer.WebProtocolSeparator;

    private boolean failedDueToDuplicateLogin = false;
    private boolean failedDueToOwnCancel = false;

    private int state = NotLoggedIn;
    private String enrolledInstantGameId = null;
    private String watchingInstantGameId = null;
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
    private JPanel settingsPanel;
    private JPanel gameOptionsPanel;

    private Box createGamesTab;
    private Box runningGamesTab;
    private Box suspendedGamesTab;
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
    private JButton contactAdminButton;

    private JLabel registerOrPasswordLabel;
    private JButton registerOrPasswordButton;

    private JButton debugSubmitButton;
    private JButton rereadLoginMsgButton;
    private JButton shutdownButton;
    private JButton dumpInfoButton;

    private JTextField notifyMessageField;
    private JTextField notifyUserField;
    private JTextField beepCountField;
    private JTextField beepIntervalField;

    private JLabel statusField;
    private String statusText = "";

    // Game browsing pane:
    private JComboBox<String> variantBox;
    private JComboBox<String> viewmodeBox;
    private JComboBox<String> eventExpiringBox;

    private JSpinner spinner1;
    private JSpinner spinner2;
    private JSpinner spinner3;
    private JLabel maxLabel;

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

    private JButton watchButton;
    private JButton resumeButton;
    private JButton deleteButton;
    private JLabel reasonWhyNotLabel;
    private JButton recoverButton;
    private JButton hideButton;
    private JLabel hideButtonText;

    private JRadioButton autoGSNothingRB;
    private JRadioButton autoGSHideRB;
    private JRadioButton autoGSCloseRB;

    private JLabel infoTextLabel;
    final static String needLoginText = "You need to login to browse or propose Games.";
    final static String enrollText = "Propose or Enroll, and when enough players have enrolled, the game creator can press 'Start'.";
    final static String startClickedText = "Request to start game was sent to server, please wait...";
    final static String waitingText = "Client connected successfully, waiting for all other players. Please wait...";
    final static String enrolledText = "NOTE: While enrolled to an instant game, you can't propose or enroll to other instant games.";
    final static String playingText = "While playing, you can't propose or enroll to other instant games.";
    final static String watchingText = "While playing or watching, you can't propose or enroll to other instant games.";

    private ChatHandler generalChat;

    private final ArrayList<GameInfo> gamesUpdates = new ArrayList<GameInfo>();

    /**
     * NOTE: shared with SocketThread, because WCST needs it to restore
     * game tokens to an GameInfo object
     */
    private final HashMap<String, GameInfo> gameHash = new HashMap<String, GameInfo>();

    private final HashSet<String> deletedGames = new HashSet<String>();

    // so that we can for all checked checkboxes add the corresponding option
    // to the options string
    private final HashMap<String, JCheckBox> checkboxForOption = new HashMap<String, JCheckBox>();

    // Need a list to preserve order
    private final List<String> gameOptions = new LinkedList<String>(
        Arrays.asList(Options.unlimitedMulligans, Options.balancedTowers,
            Options.sansLordAutoBattle, Options.pbBattleHits,
            Options.inactivityTimeout));

    // Need a list to preserve order
    private final List<String> teleportOptions = new LinkedList<String>(
        Arrays.asList(Options.noFirstTurnT2TTeleport,
            Options.noFirstTurnTeleport, Options.towerToTowerTeleportOnly,
            Options.noTowerTeleport, Options.noTitanTeleport,
            Options.noFirstTurnWarlockRecruit));

    private JPanel gamesTablesPanel;
    private JPanel gamesCards;
    private JPanel propGamesCard;

    // proposed games
    private JTable proposedGameTable;
    private GameTableModel proposedGameDataModel;

    // running games
    private JTable runGameTable;
    private GameTableModel runGameDataModel;
    // private ListSelectionModel runGameListSelectionModel;

    private JTable suspGameTable;
    private GameTableModel suspGameDataModel;

    private static String windowTitle = "Web Client";

    private final static String LoginButtonText = "Login";
    private final static String LogoutButtonText = "Logout";
    private final static String CancelLoginButtonText = "Cancel";
    private final static String quitButtonText = "Quit";
    private final static String contactAdminButtonText = "Contact the administrator";
    private final static String contactAdminButtonDisabledText = " <dialog still open> ";
    private final static String HideButtonText = "Hide Web Client";
    private final static String WatchButtonText = "Join game as spectator";
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

    private final static String createAccountLabelText = "No login yet? Create one:";
    private final static String chgPasswordLabelText = "Change your password:";

    private final static String AutoGameStartActionNothing = "Do nothing";
    private final static String AutoGameStartActionHide = "Hide WebClient";
    private final static String AutoGameStartActionClose = "Close WebClient";

    private final static String optAutoGameStartAction = "Auto Game Start Action";

    private final static String defaultSummaryText = "Let's play!";

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

    public int getClientVersion()
    {
        return WEB_CLIENT_VERSION;
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
            if (state == Playing || state == Watching)
            {
                enrolledInstantGameId = null;
                clearWatching();
                updateGUI();
            }
        }
        else
        {
            hideButton.setEnabled(true);
            hideButtonText.setText(HowtoUnhideText);
        }
    }

    private String getOngoingGameId()
    {
        if (gameClient != null)
        {
            return gameClient.getGUI().getGameId();
        }
        return "<unknown>";
    }

    private void setScheduledGamesMode(boolean scheduled)
    {
        scheduledGamesMode = scheduled;
        atDateField.setEnabled(scheduled);
        atTimeField.setEnabled(scheduled);

        if (propGamesCard == null || gamesCards == null)
        {
            return;
        }
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

        createSuspendedGamesTab();
        tabbedPane.addTab("Suspended Games", suspendedGamesTab);

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
        if (options.getOption(WebClient.AutoLoginCBText))
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

    public void restoreStatusField()
    {
        if (state == NotLoggedIn)
        {
            updateStatus("Not connected", Color.red);
        }
        else
        {
            updateStatus("Logged in", Color.green);
        }
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

        AbstractAction showPwAction = new AbstractAction("*")
        {
            public void actionPerformed(ActionEvent e)
            {
                AbstractButton abstractButton = (AbstractButton)e.getSource();
                boolean selected = abstractButton.getModel().isSelected();
                passwordField.setEchoChar(selected ? '*' : (char)0);

            }
        };

        Box pwFieldPanel = new Box(BoxLayout.X_AXIS);
        passwordField = new JPasswordField(this.password);
        pwFieldPanel.add(passwordField);
        JCheckBox showPwCheckbox = new JCheckBox(showPwAction);
        showPwCheckbox.setSelected(true);
        pwFieldPanel.add(showPwCheckbox);
        loginPane.add(new JLabel("Password"));

        // passwordField.addActionListener(this);
        loginPane.add(pwFieldPanel);
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

        loginPane.add(new JLabel("Connection status:"));
        statusField = new JLabel(statusText);
        loginPane.add(statusField);
        updateStatus("Not connected", Color.red);

        ActionListener cbActionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleAction(e);
            }
        };

        boolean alos = this.options.getOption(AutoLoginCBText);
        JCheckBox autoLoginCB = new JCheckBox(AutoLoginCBText, alos);
        autoLoginCB.addActionListener(cbActionListener);
        loginPane.add(autoLoginCB);
        loginPane.add(new JLabel(""));

        boolean algp = this.options.getOption(AutoGamePaneCBText);
        JCheckBox autoGamePaneCB = new JCheckBox(AutoGamePaneCBText, algp);
        autoGamePaneCB.addActionListener(cbActionListener);
        loginPane.add(autoGamePaneCB);
        loginPane.add(new JLabel(""));

        serverTab.add(connectionPane);

        connectionPane.add(loginPane);
        connectionPane.add(Box.createHorizontalGlue());
        connectionPane.add(Box.createVerticalGlue());

        serverTab.add(Box.createVerticalGlue());
        serverTab.add(Box.createHorizontalGlue());

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

        boolean SHOW_CONTACT_ADMIN_BUTTON = true;
        if (SHOW_CONTACT_ADMIN_BUTTON)
        {
            createContactAdminButton(loginPane);
        }
    }

    private void handleAction(ActionEvent e)
    {
        String text = e.getActionCommand();
        Object component = e.getSource();
        if (component instanceof JCheckBox)
        {
            JCheckBox cb = (JCheckBox)component;
            options.setOption(text, cb.isSelected());
        }
        else
        {
            LOGGER.severe("Event source with text " + text
                + " is not a checkbox??");
        }
    }

    private void createContactAdminButton(JPanel loginPane)
    {
        contactAdminButton = new JButton(contactAdminButtonText);
        contactAdminButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                contactAdmin();
            }
        });

        contactAdminButton.setEnabled(true);
        loginPane.add(new JLabel("Problems, Questions, Feedback?"));
        loginPane.add(contactAdminButton);
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

    protected static JLabel nonBoldLabel(String text)
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

    private Box makeTextBox2(Component c, Component c2)
    {
        Box labelBox = new Box(BoxLayout.X_AXIS);
        labelBox.add(c);
        labelBox.add(c2);
        labelBox.add(Box.createHorizontalGlue());
        return labelBox;
    }

    private void initFormats()
    {
        myDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, myLocale);
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

        String infoString = " (current date and time is:  " + nowDateString
            + "  " + nowTimeString + ")";
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
        createSettingsPanel();
        createGameOptionsPanel();
        createTeleportCheckboxPanel();

        Box proposeGamePane = new Box(BoxLayout.Y_AXIS);
        proposeGamePane.setAlignmentX(Box.CENTER_ALIGNMENT);
        proposeGamePane.setBorder(new TitledBorder("Creating games:"));

        // proposeGamePane.add(Box.createRigidArea(new Dimension(0, 10)));

        proposeGamePane.add(makeTextBox(nonBoldLabel("Set your preferences, "
            + "fill in  the 'Summary' text, "
            + "then press 'Propose' to create a game:")));

        summaryText = new JTextField(defaultSummaryText);
        summaryText.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                summaryText.selectAll();
            }
        });
        proposeGamePane
            .add(makeTextBox2(new JLabel("Summary: "), summaryText));

        ButtonGroup group = new ButtonGroup();
        Box scheduleModes = new Box(BoxLayout.X_AXIS);
        // NOTE: the actual radioButtons will be added later, see below

        scheduleModes
            .add(new JLabel("Choose the when-to-start-type of game: "));
        proposeGamePane.add(makeTextBox(scheduleModes));

        // The panel with all GUI stuff needed to schedule a game:
        Box schedulingPanel = new Box(BoxLayout.Y_AXIS);
        schedulingPanel.setAlignmentY(Box.TOP_ALIGNMENT);

        Calendar now = Calendar.getInstance(myLocale);

        initFormats();
        String nowDateAndTimeInfoString = makeDateTimeInfoString(now);

        nowDateAndTimeLabel = nonBoldLabel(nowDateAndTimeInfoString);
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
        schedulePanel.add(nowDateAndTimeLabel);

        schedulePanel.add(Box.createHorizontalGlue());

        schedulingPanel.add(schedulePanel);
        proposeGamePane.add(schedulingPanel);

        Box durationPanel = new Box(BoxLayout.X_AXIS);
        durationPanel.add(new JLabel("Duration: "));
        durationField = new JTextField("90");
        durationPanel.add(durationField);
        durationPanel.setAlignmentY(Box.TOP_ALIGNMENT);

        // " (the purpose of the duration value is: "
        // + " one should only enroll to that game if one knows that one "
        // + " will be available for at least that time)"

        durationPanel.add(makeTextBox(nonBoldLabel("...")));
        durationPanel.add(Box.createHorizontalGlue());

        proposeGamePane.add(durationPanel);
        proposeGamePane.add(Box.createRigidArea(new Dimension(0, 10)));

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

        // Table for proposed games:
        propGamesCard = new JPanel(new BorderLayout());
        propGamesCard.setBorder(new TitledBorder("Proposed Games"));
        propGamesCard.add(nonBoldLabel("The following games are proposed:"),
            BorderLayout.NORTH);

        proposedGameDataModel = new GameTableModel(myLocale);

        proposedGameTable = new ColumnWidthPersistingJTable(
            Options.proposedGamesTableOption, options, proposedGameDataModel)
        {
            //Implement table cell tool tips.
            @Override
            public String getToolTipText(MouseEvent e)
            {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);

                // Variant name
                switch (realColumnIndex)
                {
                    case 5:
                    case 6:
                    case 7:
                        tip = (String)proposedGameDataModel.getValueAt(
                            rowIndex, realColumnIndex);
                        break;
                    case 9:
                        tip = proposedGameDataModel
                            .getOptionsTooltipText(rowIndex);
                        break;
                    case 10:
                        tip = proposedGameDataModel
                            .getTeleportOptionsTooltipText(rowIndex);
                        break;
                    case 15:
                        tip = "" + getValueAt(rowIndex, colIndex);
                        break;

                    default:
                        tip = super.getToolTipText(e);
                }
                return tip;
            }
        };

        // Note: This affects globally!
        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setInitialDelay(100);

        proposedGameTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    updateGUI();
                }
            });

        proposedGameTable
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane propScrollpane = new JScrollPane(proposedGameTable);
        propGamesCard.add(propScrollpane, BorderLayout.CENTER);
        propGamesCard.add(nonBoldLabel("For some of the longer fields, "
            + "hovering mouse over a column gives more information!"),
            BorderLayout.SOUTH);

        JPanel dummyCard = new JPanel(new BorderLayout());
        dummyCard.add(Box.createRigidArea(new Dimension(0, 50)));

        gamesCards.add(dummyCard, "dummy");
        gamesCards.add(propGamesCard, CARD_PROPOSED);

        CardLayout cl = (CardLayout)gamesCards.getLayout();
        cl.show(gamesCards, CARD_PROPOSED);

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
                updateGUI();
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
    }

    private void createSettingsPanel()
    {
        settingsPanel = new JPanel(new GridLayout(0, 2));
        settingsPanel.setBorder(new TitledBorder("Game settings"));

        // Variant:
        String variantName = options.getStringOption(Options.variant);
        if (variantName == null || variantName.length() == 0)
        {
            variantName = Constants.variantArray[0]; // Default variant
        }

        variantBox = new JComboBox<String>(Constants.variantArray);
        variantBox.setSelectedItem(variantName);
        variantBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String variant = (String)variantBox.getSelectedItem();
                options.setOption(Options.variant, variant);
                updateMaxSpinner(variant);
            }
        });

        settingsPanel.add(new JLabel("Select variant:"));
        settingsPanel.add(variantBox);

        // Viewmode:
        // String viewmodesArray[] = { "all-public", "auto-tracked", "only-own" };
        String viewmodeName = options.getStringOption(Options.viewMode);
        if (viewmodeName == null)
        {
            viewmodeName = Options.viewableAll;
        }

        viewmodeBox = new JComboBox<String>(Options.viewModeArray);
        viewmodeBox.setSelectedItem(viewmodeName);
        viewmodeBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(Options.viewMode,
                    (String)viewmodeBox.getSelectedItem());
            }
        });

        settingsPanel.add(new JLabel("Select view mode:"));
        settingsPanel.add(viewmodeBox);

        // event expiring policy:
        String eventExpiringVal = options
            .getStringOption(Options.eventExpiring);
        if (eventExpiringVal == null)
        {
            eventExpiringVal = "5";
        }

        eventExpiringBox = new JComboBox<String>(Options.eventExpiringChoices);
        eventExpiringBox.setSelectedItem(eventExpiringVal);
        eventExpiringBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(Options.eventExpiring,
                    (String)eventExpiringBox.getSelectedItem());
            }
        });

        settingsPanel.add(new JLabel("Events expire after (turns):"));
        settingsPanel.add(eventExpiringBox);

        // min, target and max nr. of players:
        settingsPanel.add(new JLabel("Select player count preferences:"));
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
        maxLabel = new JLabel(" max=?");
        playerSelection.add(maxLabel);
        updateMaxSpinner(variantName);
        settingsPanel.add(playerSelection);

        createGamesTab.add(settingsPanel);
    }

    private void createGameOptionsPanel()
    {
        gameOptionsPanel = new JPanel(new GridLayout(0, 3));
        gameOptionsPanel.setBorder(new TitledBorder("Game options"));
        for (String optionName : gameOptions)
        {
            createTpCheckbox(optionName, gameOptionsPanel);
        }
        /*
        Box noAttackBeforePanel = new Box(BoxLayout.X_AXIS);
        noAttackBeforePanel.add(new JLabel("No attack before turn: "));
        noAttackBeforePanel.add(new JTextField("1", 5));
        checkboxPanel.add(noAttackBeforePanel);
        */
        createGamesTab.add(gameOptionsPanel);
    }

    private void createTeleportCheckboxPanel()
    {
        JPanel tpPanel = new JPanel(new GridLayout(0, 3));
        tpPanel.setBorder(new TitledBorder("Teleport options"));
        for (String optionName : teleportOptions)
        {
            createTpCheckbox(optionName, tpPanel);
        }
        createGamesTab.add(tpPanel);
    }

    private void createTpCheckbox(final String optionName, Container cpPanel)
    {
        boolean optionValue = options.getOption(optionName);
        final JCheckBox checkbox = new JCheckBox(optionName, optionValue);
        checkbox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(optionName, checkbox.isSelected());
            }
        });
        cpPanel.add(checkbox);
        checkboxForOption.put(optionName, checkbox);
    }

    private void updateMaxSpinner(String variant)
    {
        SpinnerNumberModel model = (SpinnerNumberModel)spinner3.getModel();
        int newMax = getMaxForVariant(variant);
        model.setMaximum(new Integer(newMax));
        maxLabel.setText(" max=" + newMax);
        adjustToPossibleMax(spinner1, newMax);
        adjustToPossibleMax(spinner2, newMax);
        adjustToPossibleMax(spinner3, newMax);
    }

    private void adjustToPossibleMax(JSpinner spinner, int max)
    {
        if (((Integer)spinner.getValue()).intValue() > max)
        {
            spinner.setValue(new Integer(max));
        }
    }

    private int getMaxForVariant(String variant)
    {
        int max = 6;
        if (variant.equals("Abyssal9"))
        {
            max = 9;
        }
        else if (variant.equals("Abyssal3") || variant.equals("SmallTitan"))
        {
            max = 3;
        }
        else if (variant.equals("Beelzebub12")
            || variant.equals("BeelzeGods12"))
        {
            max = 12;
        }
        else if (variant.equals("ExtTitan"))
        {
            max = 8;
        }
        return max;
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

        Box joinGamePanel = new Box(BoxLayout.X_AXIS);
        joinGamePanel.setBorder(new TitledBorder("Watch an ongoing game"));

        watchButton = new JButton(WatchButtonText);
        watchButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                watchButtonAction();
            }
        });
        watchButton.setEnabled(false);
        watchButton.setAlignmentX(Box.LEFT_ALIGNMENT);
        joinGamePanel.add(watchButton);
        joinGamePanel.add(Box.createHorizontalGlue());
        joinGamePanel.setPreferredSize(joinGamePanel.getMinimumSize());
        joinGamePanel.setSize(joinGamePanel.getMinimumSize());

        runningGamesTab.add(Box.createVerticalGlue());
        runningGamesTab.add(joinGamePanel);

        runningGamesTab.add(Box.createRigidArea(new Dimension(0, 20)));
        runningGamesTab.add(Box.createVerticalGlue());

        Box hideClientPanel = new Box(BoxLayout.Y_AXIS);
        hideClientPanel.setBorder(new TitledBorder("Hiding the Web Client"));

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

    private void createSuspendedGamesTab()
    {
        suspendedGamesTab = new Box(BoxLayout.Y_AXIS);

        // ----------------- First the table ---------------------
        suspendedGamesTab.setAlignmentY(0);
        suspendedGamesTab.setBorder(new TitledBorder("Suspended Games"));
        suspendedGamesTab
            .add(new JLabel("The following games are suspended:"));

        suspGameDataModel = new GameTableModel(myLocale);
        suspGameTable = new JTable(suspGameDataModel);
        suspGameTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    updateGUI();
                }
            });

        suspGameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane susptablescrollpane = new JScrollPane(suspGameTable);
        suspendedGamesTab.add(susptablescrollpane);

        suspendedGamesTab.add(Box.createVerticalGlue());

        // ----------------- Resume button -------------------
        Box resumeGamePanel = new Box(BoxLayout.X_AXIS);
        resumeGamePanel.setBorder(new TitledBorder(
            "Resume/Load a suspended game"));
        resumeButton = new JButton("Resume Game");
        resumeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                resumeGameButtonAction();
            }
        });
        resumeGamePanel.add(resumeButton);
        resumeGamePanel.add(new JLabel("    "));
        reasonWhyNotLabel = nonBoldLabel("Not logged in");
        resumeGamePanel.add(reasonWhyNotLabel);
        resumeGamePanel.add(Box.createHorizontalGlue());
        resumeGamePanel.setPreferredSize(resumeGamePanel.getMinimumSize());
        resumeGamePanel.setSize(resumeGamePanel.getMinimumSize());

        suspendedGamesTab.add(resumeGamePanel);
        suspendedGamesTab.add(Box.createVerticalGlue());

        // ----------------- Delete button -------------------
        Box deletePanel = new Box(BoxLayout.X_AXIS);
        deletePanel.setBorder(new TitledBorder("Delete a suspended game"));
        deleteButton = new JButton("Delete Game");
        deleteButton.setEnabled(true);
        deleteButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                deleteButtonAction();
            }
        });
        deletePanel.add(deleteButton);
        deletePanel.add(new JLabel("    "));
        // reasonWhyNotLabel = nonBoldLabel("Not logged in");
        // deletePanel .add(reasonWhyNotLabel);
        deletePanel.add(Box.createHorizontalGlue());
        deletePanel.setPreferredSize(deletePanel.getMinimumSize());
        deletePanel.setSize(deletePanel.getMinimumSize());

        /*
         * Hardcoded clemens or admin, because at this time we don't have
         * the "isAdmin" property set yet.
         *
         * TODO: do this properly
         * (perhaps when we make this available that everybody could delete
         * his own games, then the button would be always there and then we
         * just need to enable/disable accordingly)
         */
        if (username != null
            && (username.equals("clemens") || username.equals("admin")))
        {
            suspendedGamesTab.add(deletePanel);
            suspendedGamesTab.add(Box.createVerticalGlue());
        }

        // ----------------- Recover button -------------------
        Box recoverGamePanel = new Box(BoxLayout.X_AXIS);
        recoverGamePanel.setBorder(new TitledBorder("Recover a crashed game"));
        recoverButton = new JButton("Browse");
        recoverButton.setEnabled(false);
        recoverButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                recoverGame();
            }
        });
        recoverGamePanel.add(recoverButton);

        JLabel niLabel = new JLabel("    (not implemented yet)");
        niLabel.setFont(niLabel.getFont().deriveFont(Font.ITALIC));
        recoverGamePanel.add(niLabel);
        recoverGamePanel.add(Box.createHorizontalGlue());
        recoverGamePanel.setPreferredSize(recoverGamePanel.getMinimumSize());
        recoverGamePanel.setSize(recoverGamePanel.getMinimumSize());

        suspendedGamesTab.add(recoverGamePanel);
        suspendedGamesTab.add(Box.createVerticalGlue());

    }

    private void recoverGame()
    {
        new RecoverGameDialog(this, options);
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

        rereadLoginMsgButton = new JButton("Reread Login Message");
        rereadLoginMsgButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                rereadLoginMsgButtonAction();
            }
        });
        adminPane.add(rereadLoginMsgButton);

        shutdownButton = new JButton("Shutdown Server");
        shutdownButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                shutdownButtonAction();
            }
        });
        adminPane.add(shutdownButton);

        dumpInfoButton = new JButton("Dump Info on Server");
        dumpInfoButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dumpInfoButtonAction();
            }
        });
        adminPane.add(dumpInfoButton);

        notifyMessageField = new JTextField();
        adminPane.add(notifyMessageField);
        Box notifyPane = new Box(BoxLayout.X_AXIS);
        notifyPane.add(new JLabel("User: "));
        notifyUserField = new JTextField();
        notifyPane.add(notifyUserField);
        notifyPane.add(new JLabel("beep count: "));
        beepCountField = new JTextField();
        notifyPane.add(beepCountField);
        notifyPane.add(new JLabel("beep interval (ms): "));
        beepIntervalField = new JTextField();
        notifyPane.add(beepIntervalField);
        JButton beepButton = new JButton("Beep");
        beepButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                beepButtonAction();
            }
        });
        notifyPane.add(beepButton);
        adminPane.add(notifyPane);

        adminTab.add(adminPane);
    }

    public void beepButtonAction()
    {
        if (isAdmin())
        {
            long when = new Date().getTime();
            String sender = username;
            boolean isAdmin = true;
            String recipient = notifyUserField.getText();
            String message = notifyMessageField.getText();
            int beepCount = Integer.parseInt(beepCountField.getText());
            long beepInterval = Long.parseLong(beepIntervalField.getText());
            boolean windows = true;
            server.requestUserAttention(when, sender, isAdmin, recipient,
                message, beepCount, beepInterval, windows);
        }
    }

    public String createLoginWebClientSocketThread(boolean force)
    {
        String reason = null;
        failedDueToDuplicateLogin = false;
        failedDueToOwnCancel = false;

        updateStatus("Trying to connect...", Color.blue);
        loginLogoutButton.setText(CancelLoginButtonText);

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
            loginLogoutButton.setText(LoginButtonText);

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

            if (e.wasCancelled())
            {
                failedDueToOwnCancel = true;
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

    private void logout()
    {
        // When watching a game and it ended with Draw, when I did logout
        // the server.logout() caused a NPE ... prevent it, just in case.
        if (server != null)
        {
            server.logout();
            server = null;
            wcst = null;
        }

        updateStatus("Not connected", Color.red);
        return;
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
            gameClient.getGUI().clearWebClient();
            gameClient = null;
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

    public String getSelectedGameIdFromProposedTable()
    {
        return getSelectedGameId(proposedGameTable);
    }

    public String getSelectedGameIdFromRunTable()
    {
        return getSelectedGameId(runGameTable);
    }

    public String getSelectedGameIdFromSuspTable()
    {
        return getSelectedGameId(suspGameTable);
    }

    public String getSelectedGameId(JTable table)
    {
        String id = null;

        int selRow = table.getSelectedRow();
        if (selRow != -1)
        {
            id = (String)table.getValueAt(selRow, 0);
            if (!gameHash.containsKey(id))
            {
                LOGGER.warning("Game with id " + id
                    + " is not in game hash any more...");
                return null;
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

            case Watching:
                return username + " (watching)";

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

            case Watching:
                return watchingText;

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

            case EnrolledInstantGame:
                return "As " + username + " - enrolled to instant game "
                    + enrolledInstantGameId;

            case Playing:
                return "As " + username + " - playing game #" + getOngoingGameId();

            case Watching:
                return "As " + username + " - watching game #"
                    + watchingInstantGameId;

            case LoggedIn:
                return "logged in as " + username;

            default:
                return "<info text - undefined state?>";
        }

    }

    /**
     * Returns true if this user would be allowed to start this game
     * (given that all other conditions are fulfilled).
     * Usually the allowed player is the one who created it, but if
     * that one is not enrolled, the first of the enrolled ones is
     * allowed then to do it.
     *
     * @param gi
     * @return Whether this player would be allowed to start this game
     */
    private boolean isEligibleToStart(GameInfo gi)
    {
        if (gi.getInitiator().equals(username))
        {
            return true;
        }

        if (!gi.isEnrolled(gi.getInitiator())
            && gi.isFirstInEnrolledList(username))
        {
            return true;
        }
        return false;
    }

    private boolean checkIfCouldWatch(int state)
    {
        switch (state)
        {
            case NotLoggedIn:
            case EnrolledInstantGame:
            case Playing:
            case Watching:
                return false;

            case LoggedIn:
            default:

                boolean watchPossible = false;
                String id = getSelectedGameIdFromRunTable();
                if (id != null)
                {
                    watchPossible = true;
                }
                return watchPossible;
        }
    }

    private String checkIfCouldResume(int state)
    {
        boolean DEBUG = false;
        if (DEBUG)
        {
            return null;
        }

        String reasonWhyNot = null;
        switch (state)
        {
            case NotLoggedIn:
            case EnrolledInstantGame:
            case Playing:
            case Watching:
                return "Can't resume while enrolled, playing or watching another game.";

            case LoggedIn:
            default:
                if (gameResumeInitiated)
                {
                    return "Resume of suspended game is ongoing";
                }
                if (startingGame != null)
                {
                    return "Resume of suspended game is ongoing";
                }
                if (suspGameDataModel.getRowCount() == 0)
                {
                    return "You don't have any suspended games.";
                }
                reasonWhyNot = "You are not enrolled into this game.";
                String id = getSelectedGameIdFromSuspTable();
                if (id != null)
                {
                    GameInfo gi = findGameById(id);
                    if (gi.getOnlineCount() < 2)
                    {
                        reasonWhyNot = "No other player online for game #"
                            + gi.getGameId();
                    }
                    else if (gi.getOnlineCount() != gi.getPlayers().size())
                    {
                        reasonWhyNot = PLAYERS_MISSING;
                    }
                    else if (gi.isEnrolled(username) || isAdmin)
                    {
                        reasonWhyNot = null;
                    }
                    // else case defaults to 'you are not enrolled'
                }
                else
                {
                    reasonWhyNot = "Select a game in the table.";
                }
                return reasonWhyNot;
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
                    if (gi.enoughPlayersEnrolled() && gi.allEnrolledOnline()
                        && gi.isStartable() && isEligibleToStart(gi))
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
                String id = getSelectedGameIdFromProposedTable();
                if (id != null && isScheduledGameAndStartable(id))
                {
                    startPossible = true;
                }
                return startPossible;

            case NotLoggedIn:
            case Playing:
            case Watching:
            default:
                return false;
        }

    }

    private boolean isScheduledGameAndStartable(String id)
    {
        assert id != null : "Not a valid game id: " + id;

        GameInfo gi = findGameById(id);

        if (gi == null)
        {
            return false;
        }

        if (gi.isStartable() && gi.isEnrolled(username)
            && isEligibleToStart(gi) && gi.isScheduledGame() && gi.isDue()
            && gi.hasEnoughPlayers() && gi.allEnrolledOnline())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean checkIfCouldPropose()
    {
        if (state == NotLoggedIn)
        {
            return false;
        }

        boolean scheduled = getScheduledGamesMode();
        if (!scheduled && (state == Playing || state == Watching))
        {
            return false;
        }
        return true;
    }

    private boolean checkIfCouldCancel()
    {
        if (state == NotLoggedIn)
        {
            return false;
        }

        String selectedGameId = getSelectedGameIdFromProposedTable();
        if (selectedGameId != null && (isOwner(selectedGameId) || isAdmin()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean checkIfCouldEnroll()
    {
        if (state == NotLoggedIn || state == Playing || state == Watching)
        {
            return false;
        }

        String selectedGameId = getSelectedGameIdFromProposedTable();
        if (selectedGameId == null)
        {
            return false;
        }

        GameInfo gi = findGameById(selectedGameId);
        // only if not enrolled yet, and still in PROPOSED or DUE state
        if (gi != null && !gi.isEnrolled(username) && gi.isStartable())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean checkIfCouldUnenroll()
    {
        if (state == NotLoggedIn)
        {
            return false;
        }

        String selectedGameId = getSelectedGameIdFromProposedTable();
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

    private boolean checkIfCouldContactAdmin()
    {
        return (state != NotLoggedIn);
    }

    // this should always be called inside a invokeLater (i.e. in EDT)!!
    public void doUpdateGUI()
    {
        // =============================================================
        // First calculate new state for all kind of things...

        String newTitle = makeWindowTitleForState(state);
        String newInfoText;
        if (startingGame != null)
        {
            newInfoText = "Game was started by user "
                + startingGame.getStartingUser().getName()
                + "; MasterBoard should appear soon. Please wait...";
        }
        else
        {
            newInfoText = makeInfoTextForState(state);
        }

        String newStatusText = makeStatusTextForState(state);
        boolean couldPropose = checkIfCouldPropose();
        boolean couldDebugSubmit = (state == LoggedIn);
        boolean couldCancel = checkIfCouldCancel();
        boolean couldEnroll = checkIfCouldEnroll();
        boolean couldUnenroll = checkIfCouldUnenroll();
        boolean couldStartOnServer = checkIfCouldStartOnServer(state);
        // feature currently disabled (( => hardcoded to false)):
        boolean couldStartLocally = false;
        boolean couldWatch = checkIfCouldWatch(state);
        String reasonWhyCantResume = checkIfCouldResume(state);
        boolean couldContactAdmin = checkIfCouldContactAdmin();

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

        contactAdminButton.setEnabled(couldContactAdmin);

        proposeButton.setEnabled(couldPropose);
        cancelButton.setEnabled(couldCancel);
        enrollButton.setEnabled(couldEnroll);
        unenrollButton.setEnabled(couldUnenroll);

        startButton.setEnabled(couldStartOnServer);
        startLocallyButton.setEnabled(couldStartLocally);
        watchButton.setEnabled(couldWatch);
        resumeButton.setEnabled(reasonWhyCantResume == null
            || reasonWhyCantResume.equals(PLAYERS_MISSING));
        String text;
        if (reasonWhyCantResume == null)
        {
            text = "Click to resume the game game #"
                + getSelectedGameIdFromSuspTable() + "!";
        }
        else if (reasonWhyCantResume.equals(PLAYERS_MISSING))
        {
            text = "NOTE: not all players online. Resume will succeed "
                + "only if missing players are dead!";
        }
        else
        {
            text = reasonWhyCantResume;
        }
        reasonWhyNotLabel.setText(text);

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

    private GameInfo findGameByIdNoComplaint(String gameId)
    {
        GameInfo gi = gameHash.get(gameId);
        return gi;
    }

    private GameInfo findGameById(String gameId)
    {
        GameInfo gi = gameHash.get(gameId);
        if (gi == null)
        {
            LOGGER.log(Level.SEVERE, "Game from hash for gameId " + gameId
                + " is null!!");
            Thread.dumpStack();
        }
        return gi;
    }

    private boolean isOwner(String gameId)
    {
        GameInfo gi = findGameById(gameId);
        if (gi == null)
        {
            return false;
        }
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
        else if (message != null && failedDueToOwnCancel)
        {
            state = NotLoggedIn;
            loginField.setEnabled(true);
            updateStatus("Login attempt cancelled... - Not logged in",
                Color.BLUE);
            updateGUI();
        }
        if (message == null)
        {
            enrolledInstantGameId = null;
            watchingInstantGameId = null;
            state = LoggedIn;
            loginField.setEnabled(false);
            updateGUI();

            options.setOption(Options.webServerHost, this.hostname);
            options.setOption(Options.webServerPort, this.port);
            options.setOption(Options.webClientLogin, this.login);
            options.setOption(Options.webClientPassword, this.password);

            options.saveOptions();
            if (options.getOption(WebClient.AutoGamePaneCBText))
            {
                tabbedPane.setSelectedComponent(createGamesTab);
            }
        }
        else
        {
            LOGGER.log(Level.FINEST, "connect/login failed...");
        }
    }

    public void doCancelConnect()
    {
        WebClientSocketThread.cancelConnectAttempt();
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
        /*
         * Commented out because it caused a problem: if the gameInfo update
         * from server both for unenroll and cancel both go the gameUpdates
         * list, it sometimes happened that it re-added it to the table again.
         */
        /*
        if (state == EnrolledInstantGame)
        {
            if (enrolledInstantGameId != null)
            {
                doUnenroll(enrolledInstantGameId);
            }
            else
            {
                LOGGER.warning("state enrolledInstantGame but id is null?");
            }
        }
        */
        cancelOwnInstantGameOnLogout();

        logout();

        synchronized (gamesUpdates)
        {
            gamesUpdates.clear();
        }

        proposedGameDataModel.resetTable();
        runGameDataModel.resetTable();
        suspGameDataModel.resetTable();

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
        for (GameInfo gi : findMyInstantGames())
        {
            server.cancelGame(gi.getGameId(), username);
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
        int duration, String summary, String expire, List<String> gameOptions,
        List<String> teleportOptions, int min, int target, int max)
    {
        server.proposeGame(username, variant, viewmode, startAt, duration,
            summary, expire, gameOptions, teleportOptions, min, target, max);
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

    /**
     * Called when user presses the "Start" button in "Create or Join" tab
     * and then sends the necessary message to server
     * @param gameId
     * @return if things went OK or not (ATM always true)
     */
    boolean doStart(String gameId)
    {
        startButton.setEnabled(false);
        startLocallyButton.setEnabled(false);
        cancelButton.setEnabled(false);
        unenrollButton.setEnabled(false);
        // TODO better handle with changing state, but not today...
        infoTextLabel.setText(startClickedText);
        server.startGame(gameId, new User(username));

        return true;
    }

    /**
     * Called when user presses the "Resume" button in "Suspended Games" tab
     * to send the necessary message to server
     * @param gameId
     * @return if things went OK or not (ATM always true)
     */
    boolean doResume(final String gameId)
    {
        String filename = "dummy";
        server.resumeGame(gameId, filename, new User(username));
        return true;
    }

    /**
     * Called when user presses the "Delete" button in "Suspended Games" tab
     * to send the necessary message to server
     *
     * TODO: minimal functionality to be used by admin, perhaps this does not
     * completely all update GUI stuff...
     * @param gameId
     * @return if things went OK or not (ATM always true)
     */
    boolean doDeleteSuspendedGame(String gameId)
    {
        server.deleteSuspendedGame(gameId, new User(username));
        return true;
    }

    // Called when user presses the "Start Locally" button in
    // "Create or Join" tab
    // TODO: Dead functionality!!
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
        server.startGameOnPlayerHost(getSelectedGameIdFromProposedTable(), hostingPlayer,
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
        if (gi.getGameState().equals(GameState.SUSPENDED))
        {
            return;
        }
        boolean scheduled = gi.isScheduledGame();

        int index = proposedGameDataModel.getRowIndex(gi).intValue();
        proposedGameTable.setRowSelectionInterval(index, index);

        if (!scheduled)
        {
            state = EnrolledInstantGame;
            enrolledInstantGameId = gameId;
        }
        updateGUI();
    }

    public void didUnenroll(String gameId, String user)
    {
        // do not set it back to LoggedIn if this didUnenroll is the
        // result of a automatic unenroll before logout
        if (watchingInstantGameId != null)
        {
            // should never happen, in any case prevent it from setting state
            // to plain "LoggedIn"
        }
        else if (state != NotLoggedIn)
        {
            state = LoggedIn;
        }
        enrolledInstantGameId = null;

        updateGUI();
    }

    public void setWatching(String gameId)
    {
        watchingInstantGameId = gameId;
        state = Watching;
    }

    public void clearWatching()
    {
        watchingInstantGameId = null;
        state = LoggedIn;
    }

    public void gameStartsSoon(String gameId, String startUser)
    {
        LOGGER.fine("Game starts soon, gameid " + gameId);
        GameInfo gi = findGameById(gameId);
        if (gi != null)
        {
            gi.markStarting(new User(startUser));
            gameResumeInitiated = false;
            startingGame = gi;
        }
        updateGUI();
    }

    // Client calls this
    public void notifyComingUp(boolean success)
    {
        synchronized (comingUpMutex)
        {
            // skip if timer already did it (perhaps shortly ago)
            if (!timeIsUp)
            {
                if (success)
                {
                    clientIsUp = true;
                }
                else
                {
                    clientStartFailed = true;
                }
                comingUpMutex.notify();
            }
        }
    }

    public void notifyGameSuspended()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                JOptionPane
                    .showMessageDialog(
                        WebClient.this,
                        "Your board was closed because the game was suspended.\n\n"
                            + "The game can be resumed in 'Suspended Games' tab,\n"
                            + "now or also later.", "Game suspended",
                        JOptionPane.INFORMATION_MESSAGE);
                state = LoggedIn;
                enrolledInstantGameId = null;
                updateGUI();
            }
        });
    }

    private Timer setupTimer()
    {
        // java.util.Timer, not Swing Timer
        Timer timer = new Timer();
        timeIsUp = false;
        clientStartFailed = false;
        clientIsUp = false;

        long timeout = 60; // secs

        timer.schedule(new TriggerTimeIsUp(), timeout * 1000);
        return timer;
    }

    class TriggerTimeIsUp extends TimerTask
    {
        @Override
        public void run()
        {
            synchronized (comingUpMutex)
            {
                if (clientIsUp || clientStartFailed)
                {
                    // skip, timer was triggered just when start succeeded
                }
                else
                {
                    timeIsUp = true;
                    comingUpMutex.notify();
                }
            }
        }
    }

    public void gameStartsNow(String gameId, int port, String hostingHost,
        final int inactivityCheckInterval,
        final int inactivityWarningInterval, final int inactivityTimeout)
    {
        LOGGER.info("Game starts now, gameid " + gameId);
        if (hostingHost == null || hostingHost.equals("null"))
        {
            // Hosted on Game Server
            hostingHost = hostname;
        }

        // For now, just always use runnable (that's why the "&& IN_USE" which evals to false)
        // TODO Is the runnable necessary?
        boolean IN_USE = false;
        if (startedGameId == null && IN_USE)
        {
            // is null means: it was not this webclient that started locally
            startOwnClient(gameId, port, hostingHost,
                inactivityWarningInterval);
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

                    startOwnClient(gameId, port, host,
                        inactivityWarningInterval);
                }
            };
            new Thread(r).start();
        }
    }

    public void startOwnClient(String gameId, int port, String hostingHost,
        int inactivityWarningInterval)
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

            gc = Client
                .createClient(hostingHost, p, username, type, whatNextManager,
                    localServer, true, noOptionsFile, true, false);

            // Right now this waitingText is probably directly overwritten by
            // updateGUI putting there again the startingText (started by player ...)
            // but I don't want to fix even that still today...

            // TODO make this behave properly...
            infoTextLabel.setText(waitingText);

            setGameClient(gc);
            gc.getGUI().setWebClient(this, inactivityWarningInterval, gameId,
                username, password);

            Timer timeoutStartup = setupTimer();

            while (!clientIsUp && !timeIsUp && !clientStartFailed)
            {
                synchronized (comingUpMutex)
                {
                    try
                    {
                        comingUpMutex.wait();
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
            startingGame = null;

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
                enrolledInstantGameId = null;
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
            JOptionPane.showMessageDialog(
                this,
                "Unexpected exception while starting the game client: "
                    + e.toString(), "Starting game failed!",
                JOptionPane.ERROR_MESSAGE);
            state = LoggedIn;
            updateGUI();
        }
    }

    public void startSpectatorClient(String gameId, int port,
        String hostingHost)
    {
        LOGGER.info("Starting Spectator Client for gameId " + gameId
            + " hostingHost " + hostingHost + " port " + port);

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

            gc = Client.createClient(hostingHost, p, username, type,
                whatNextManager, localServer, true, noOptionsFile, true, true);

            // Right now this waitingText is probably directly overwritten by
            // updateGUI putting there again the startingText (started by player ...)
            // but I don't want to fix even that still today...

            // TODO make this behave properly...

            infoTextLabel.setText(waitingText);

            setGameClient(gc);
            gc.getGUI().setWebClient(this, -1, gameId, username, password);
            Timer timeoutStartup = setupTimer();

            while (!clientIsUp && !timeIsUp && !clientStartFailed)
            {
                synchronized (comingUpMutex)
                {
                    try
                    {
                        comingUpMutex.wait();
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
            startingGame = null;

            if (clientIsUp)
            {
                setWatching(gameId);
                updateGUI();
                // onGameStartAutoAction();
            }
            else
            {
                JOptionPane.showMessageDialog(this,
                    "Trouble when connecting to game to watch?",
                    "Watching game failed!", JOptionPane.ERROR_MESSAGE);
                clearWatching();
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
                "Unexpected exception while starting the spectator client: "
                    + e.toString(), "Starting game failed!",
                JOptionPane.ERROR_MESSAGE);

            state = LoggedIn;
            updateGUI();
        }
    }

    public void gameCancelled(String gameId, String byUser)
    {
        deletedGames.add(gameId);

        GameInfo gi = findGameByIdNoComplaint(gameId);
        if (gi != null)
        {
            // Remove it from table
            handleGameInfoUpdates(gi);
        }

        if (state == EnrolledInstantGame
            && enrolledInstantGameId.equals(gameId))
        {
            if (!byUser.equals(username))
            {
                String message = "Instant game " + gameId
                    + " was cancelled by user " + byUser;
                JOptionPane.showMessageDialog(this, message);
            }
            if (watchingInstantGameId != null)
            {
                // should never happen, in any case prevent it from setting state
                // to plain "LoggedNn"
            }
            else
            {
                state = LoggedIn;
                enrolledInstantGameId = null;
            }
            updateGUI();
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

    public void notifyItsPlayersTurn(boolean shouldDoSomethingOrNot)
    {
        Color color = shouldDoSomethingOrNot ? Color.yellow : HTMLColor.white;
        generalChat.setBackgroundColor(color);
    }

    public void watchGameInfo(String gameId, String host, int port)
    {
        LOGGER.info("Got watchgame info for game " + gameId + ": host=" + host
            + ", port=" + port);
        startSpectatorClient(gameId, port, host);
    }

    public void tellOwnInfo(String email)
    {
        this.email = email;
    }

    public void requestAttention(long when, String byUser, boolean byAdmin,
        String message, int beepCount, long beepInterval, boolean windows)
    {
        String whenText = "";
        if (when != 0)
        {
            Calendar now = Calendar.getInstance(myLocale);
            String nowDateString = myDateFormat.format(now.getTime());
            String nowTimeString = myTimeFormat.format(now.getTime());
            whenText = "At " + nowDateString + " " + nowTimeString;
        }

        String who = (byAdmin ? "Administrator" : "User") + " '" + byUser;
        final String dialogTitle = who + "' requests your attention!";
        final String dialogMessage = whenText + ", " + dialogTitle + "\n\n"
            + message;
        final boolean toFront = options.getOption(Options.uponMessageToFront,
            true);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                showRequestAttentionMessage(dialogTitle, dialogMessage,
                    toFront);
            }
        });

        for (int i = 1; i <= beepCount; i++)
        {
            getToolkit().beep();
            if (i < beepCount)
            {
                WhatNextManager.sleepFor(beepInterval);
            }
        }
    }

    /*
     * Must be called inside EDT
     */
    private void showRequestAttentionMessage(String dialogTitle,
        String dialogMessage, boolean toFront)
    {
        if (toFront)
        {
            KFrame thisFrame = WebClient.this;
            if (thisFrame.getState() != KFrame.NORMAL)
            {
                thisFrame.setState(KFrame.NORMAL);
            }
            thisFrame.toFront();
            thisFrame.repaint();
        }
        JOptionPane.showMessageDialog(WebClient.this, dialogMessage,
            dialogTitle, JOptionPane.INFORMATION_MESSAGE);
    }

    public void deliverGeneralMessage(long when, boolean error, String title,
        String message)
    {
        String whenText = "";
        if (when != 0)
        {
            Calendar now = Calendar.getInstance(myLocale);
            String nowDateString = myDateFormat.format(now.getTime());
            String nowTimeString = myTimeFormat.format(now.getTime());
            whenText = "At " + nowDateString + " " + nowTimeString + ":\n";
        }
        JOptionPane.showMessageDialog(this, whenText + message, title,
            error ? JOptionPane.ERROR_MESSAGE
                : JOptionPane.INFORMATION_MESSAGE);
    }

    // TODO instead of to chat, add such stuff to a system log tab or similar
    public void systemMessage(long when, String message)
    {
        generalChat.chatDeliver(when, "SYSTEM", message, false);
    }

    // Game Client tells us this when user closes the masterboard
    public void tellGameEnds()
    {
        enrolledInstantGameId = null;
        clearWatching();
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
        if (deletedGames.contains(gi.getGameId()))
        {
            LOGGER.info("Still GameInfo update to gameId " + gi.getGameId()
                + " which is already deleted - ignoring it.");
            return;
        }
        LOGGER.finest("In WC.gameInfo() for not deleted game with id " + gi);
        handleGameInfoUpdates(gi);
    }

    private void handleGameInfoUpdates(GameInfo gi)
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
                        String gameId = game.getGameId();
                        if (deletedGames.contains(gameId))
                        {
                            proposedGameDataModel.removeGame(gameId);
                            break;
                        }
                        GameState gameState = game.getGameState();

                        switch (gameState)
                        {
                            case PROPOSED:
                                replaceInTable(proposedGameTable, game);
                                break;

                            case DUE:
                            case ACTIVATED:
                                replaceInTable(proposedGameTable, game);
                                break;

                            case STARTING:
                                // TODO: state never used anywhere else - remove it ?
                                break;

                            case READY_TO_CONNECT:
                                replaceInTable(proposedGameTable, game);
                                break;

                            case RUNNING:
                                replaceInTable(runGameTable, game);
                                proposedGameDataModel.removeGame(game
                                    .getGameId());
                                suspGameDataModel.removeGame(game.getGameId());
                                break;

                            case SUSPENDED:
                                if (game.isEnrolled(username) || isAdmin())
                                {
                                    replaceInTable(suspGameTable, game);
                                }
                                else
                                {
                                    suspGameDataModel.removeGame(game
                                        .getGameId());
                                }
                                runGameDataModel.removeGame(game.getGameId());
                                break;

                            case ENDING:
                                // Normally happens during change to RUNNING,
                                // but if starting fails, it's changed to
                                // ENDING immediately. So do it here again.
                                // If it is not there anymore, no harm done.
                                proposedGameDataModel.removeGame(game
                                    .getGameId());
                                runGameDataModel.removeGame(game.getGameId());
                                break;

                            case DELETED:
                                proposedGameDataModel.removeGame(game
                                    .getGameId());
                                runGameDataModel.removeGame(game.getGameId());
                                suspGameDataModel.removeGame(game.getGameId());
                                break;

                            default:
                                LOGGER.log(
                                    Level.WARNING,
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

    public void connectionReset(boolean forced)
    {
        String message = (forced ? "Some other connection to server with same login name forced your logout."
            : "Connection reset by server! Your WebClient was logged out.\n"
                + "(A possibly running game is _NOT_ affected by this!)");
        JOptionPane.showMessageDialog(this, message);
        setAdmin(false);
        state = NotLoggedIn;
        enrolledInstantGameId = null;
        watchingInstantGameId = null;
        receivedField.setText("Connection reset by server!");
        updateStatus("Not connected", Color.red);

        loginField.setEnabled(true);
        synchronized (gamesUpdates)
        {
            gamesUpdates.clear();
        }
        gameHash.clear();

        proposedGameDataModel.resetTable();
        runGameDataModel.resetTable();

        updateGUI();

        tabbedPane.setSelectedComponent(serverTab);
    }

    // ======================================================================
    // Below methods are called my GUI event listeners

    private void hideButtonAction()
    {
        setVisible(false);
    }

    private void watchButtonAction()
    {
        String gameId = getSelectedGameIdFromRunTable();
        LOGGER.info("Watch button pressed for gameId = " + gameId
            + " - requesting info from server");
        watchButton.setEnabled(false);
        server.watchGame(gameId, username);
    }

    private void resumeGameButtonAction()
    {
        String gameId = getSelectedGameIdFromSuspTable();
        LOGGER.fine("Resume Button, game nr " + gameId);
        suspGameTable.clearSelection();
        resumeButton.setEnabled(false);
        startButton.setEnabled(false);
        startLocallyButton.setEnabled(false);
        cancelButton.setEnabled(false);
        unenrollButton.setEnabled(false);
        // TODO better handle with changing state, but not today...
        reasonWhyNotLabel.setText(startClickedText);
        gameResumeInitiated = true;
        updateGUI();
        Thread.yield();
        // Tell server
        doResume(gameId);
    }

    private void deleteButtonAction()
    {
        String gameId = getSelectedGameIdFromSuspTable();
        if (gameId == null)
        {
            LOGGER
                .fine("Delete Suspended Game Button, but no game selected in table?");
            return;
        }
        suspGameTable.clearSelection();
        LOGGER.fine("Delete Suspended Game Button, game nr " + gameId);
        doDeleteSuspendedGame(gameId);
    }

    private void quitButtonAction()
    {
        doQuit();
    }

    void loginLogoutButtonAction(final String command)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                executeLoginLogoutButtonAction(command);
            }
        };

        Thread doButtonAction = new Thread(r);
        doButtonAction.start();
    }

    private void executeLoginLogoutButtonAction(String command)
    {
        if (command.equals(LoginButtonText))
        {
            doLogin();
        }
        else if (command.equals(LogoutButtonText))
        {
            doLogout();
        }
        else if (command.equals(CancelLoginButtonText))
        {
            doCancelConnect();
        }
        else
        {
            LOGGER.warning("unexpected command " + command
                + " on LoginButton?");
        }
    }

    private void rereadLoginMsgButtonAction()
    {
        if (isAdmin())
        {
            server.rereadLoginMessage();
        }
    }

    private void shutdownButtonAction()
    {
        if (isAdmin())
        {
            server.shutdownServer();
        }
    }

    private void dumpInfoButtonAction()
    {
        server.dumpInfo();
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

    private void contactAdmin()
    {
        contactAdminButton.setText(contactAdminButtonDisabledText);
        contactAdminButton.setEnabled(false);
        new ContactAdminDialog(this, options, username, email);
    }

    public void reEnableContactAdminButton()
    {
        contactAdminButton.setText(contactAdminButtonText);
        contactAdminButton.setEnabled(true);
    }

    void sendTheMessageToAdmin(String senderName, String senderEmail,
        String message)
    {
        long now = new Date().getTime();
        List<String> lines = Split.split("\n", message);
        server.messageToAdmin(now, senderName, senderEmail, lines);
    }

    private void startLocallyButtonAction()
    {
        String selectedGameId = getSelectedGameIdFromProposedTable();
        if (selectedGameId != null)
        {
            boolean ok = doStartLocally(selectedGameId);
            if (ok)
            {
                // proposedGameTable.setEnabled(false);
            }
        }
    }

    private void startButtonAction()
    {
        String selectedGameId = getSelectedGameIdFromProposedTable();
        if (selectedGameId != null)
        {
            boolean ok = doStart(selectedGameId);
            if (ok)
            {
                // proposedGameTable.setEnabled(false);
            }
        }
    }

    private void cancelButtonAction()
    {
        String selectedGameId = getSelectedGameIdFromProposedTable();
        if (selectedGameId != null)
        {

            GameInfo gi = findGameById(selectedGameId);
            if (gi == null)
            {
                return;
            }
            if (gi.isEnrolled(username))
            {
                doUnenroll(selectedGameId);
            }
            doCancel(selectedGameId);
        }
    }

    private void unenrollButtonAction()
    {
        String selectedGameId = getSelectedGameIdFromProposedTable();
        if (selectedGameId != null)
        {
            boolean ok = doUnenroll(selectedGameId);
            if (ok)
            {
                // proposedGameTable.setEnabled(true);
            }
        }
    }

    /**
     * Find all "relevant" instant games owned by this player
     * (relevant means except those who are running, ending or deleted).
     * Normally there should ever be only one, but in strange cases...
     * (like, game start failed or something...)
     */
    private List<GameInfo> findMyInstantGames()
    {
        List<GameInfo> list = new ArrayList<GameInfo>();
        for (GameInfo gi : gameHash.values())
        {
            // is instant, is mine, and don't bother about running, ending,
            // suspended or deleted games, nor old ones.
            if (gi.getInitiator().equals(username) && !gi.isScheduledGame()
                && !gi.getGameState().equals(GameState.RUNNING)
                && !gi.getGameState().equals(GameState.ENDING)
                && !gi.getGameState().equals(GameState.SUSPENDED)
                && !gi.getGameState().equals(GameState.DELETED)
                && !deletedGames.contains(gi.getGameId()))
            {
                list.add(gi);
            }
        }
        return list;
    }

    /**
     * If there is at least one instant game by this player,
     * return it (one of it if many), otherwise null.
     * Normally there should ever be only one, but in strange cases...
     * (like, game start failed or something...)
     * @return The (or: any) instant game or null
     */
    private GameInfo ownInstantGameIfAny()
    {
        List<GameInfo> list = findMyInstantGames();
        return list.isEmpty() ? null : list.get(0);
    }

    private void displayOnlyOneInstantGameMessage(String action, String message)
    {
        String title = "Can't " + action + " instant game now!";
        JOptionPane.showMessageDialog(this, message, title,
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void enrollButtonAction()
    {
        String selectedGameId = getSelectedGameIdFromProposedTable();
        if (selectedGameId != null)
        {
            GameInfo gi = findGameById(selectedGameId);
            if (gi != null && !gi.isScheduledGame())
            {
                if (enrolledInstantGameId != null)
                {
                    displayOnlyOneInstantGameMessage("enroll to",
                        "You can only be active for one instant game at a time, "
                            + "and you are already enrolled to instant game #"
                            + enrolledInstantGameId + "!");
                    return;
                }
                GameInfo instantGame = ownInstantGameIfAny();
                if (instantGame != null
                    && !instantGame.getGameId().equals(selectedGameId))
                {
                    displayOnlyOneInstantGameMessage("enroll to",
                        "You can only be active for one instant game at a time, "
                            + "and there is already instant game #"
                            + instantGame.getGameId() + " proposed by you!");
                    return;
                }
            }
            doEnroll(selectedGameId);
        }
    }

    private void proposeButtonAction()
    {
        if (!getScheduledGamesMode())
        {
            if (enrolledInstantGameId != null)
            {
                displayOnlyOneInstantGameMessage("propose",
                    "You can only be active for one instant game at a time, "
                        + "and you are already enrolled to instant game #"
                        + enrolledInstantGameId + "!");
                return;
            }
            GameInfo instantGame = null;
            if ((instantGame = ownInstantGameIfAny()) != null)
            {
                displayOnlyOneInstantGameMessage(
                    "propose",
                    "You can only be active for one instant game at a time, "
                        + "and there is already instant game #"
                        + instantGame.getGameId() + " proposed by you!");
                return;
            }
            if (gameClient != null)
            {
                displayOnlyOneInstantGameMessage("propose",
                    "You can only be connected to one game at a time, "
                        + "but you are still connected to game #"
                        + getOngoingGameId() + "." + "\n\n"
                        + "Close the MasterBoard of that game first.");
                return;
            }
            if (watchingInstantGameId != null)
            {
                displayOnlyOneInstantGameMessage("propose",
                    "You can only be connected to one game at a time, "
                        + "but you are still connected to (watching) game #"
                        + watchingInstantGameId + "." + "\n\n"
                        + "Close the MasterBoard of that game first.");
                return;
            }
        }

        int min = ((Integer)spinner1.getValue()).intValue();
        int target = ((Integer)spinner2.getValue()).intValue();
        int max = ((Integer)spinner3.getValue()).intValue();

        List<String> gameOptionsList = new ArrayList<String>();
        for (String optionName : gameOptions)
        {
            if (checkboxForOption.get(optionName).isSelected())
            {
                gameOptionsList.add(optionName);
            }
        }

        List<String> teleportOptionsList = new ArrayList<String>();
        for (String optionName : teleportOptions)
        {
            if (checkboxForOption.get(optionName).isSelected())
            {
                teleportOptionsList.add(optionName);
            }
        }

        boolean scheduled = getScheduledGamesMode();
        long startAt = scheduled ? getStartTime() : -1;
        int duration = getDuration();
        String summaryText = getSummaryText();

        do_proposeGame(variantBox.getSelectedItem().toString(), viewmodeBox
            .getSelectedItem().toString(), startAt, duration, summaryText,
            eventExpiringBox.getSelectedItem().toString(), gameOptionsList,
            teleportOptionsList, min, target, max);
    }
}
