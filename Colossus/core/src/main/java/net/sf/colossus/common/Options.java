package net.sf.colossus.common;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class Options lists game options for Colossus.
 *
 * @author David Ripton
 *
 * TODO constants should be all uppercase
 */
public final class Options implements IOptions
{
    private static final Logger LOGGER = Logger.getLogger(Options.class
        .getName());

    // Everything is public because we use this class in both the client
    // and server packages.  (With separate data.)

    // Non-options that use the options framework
    // Will add player numbers 0 through n-1 to the end of these.
    public static final String playerName = "Player name ";
    public static final String playerType = "Player type ";

    public static final String runClientPlayer = "Network client playername";
    public static final String runClientHost = "Network client hostname";
    public static final String runClientPort = "Network client port";
    public static final String runSpectatorClient = "Spectator client";

    public static final String loadGameFileName = "Load game file name";
    public static final String webFlagFileName = "Web game flag file";
    public static final String serveAtPort = "Run server on port";
    public static final String FORCE_BOARD = "Force View Board";

    public static final String webServerHost = "Web server name";
    public static final String webServerPort = "Web server port";
    public static final String webClientLogin = "Web client login";
    public static final String webClientPassword = "Web client password";
    public static final String proposedGamesTableOption = "Proposed Games Table Column Widths";

    // Option names

    // Server administrative options
    public static final String autosave = "Autosave";
    public static final String autosaveMaxKeep = "Max autosave files";
    public static final String autosaveVerboseNames = "Verbose autosave names";
    public static final String autoStop = "AIs stop when humans dead";
    public static final String autoQuit = "Auto quit when game over";
    public static final String goOnWithoutObserver = "Go on without observer";
    public static final String hotSeatMode = "Hot seat mode";
    public static final String keepAccepting = "Keep accepting clients";
    public static final String diceStatisticsFile = "Dice statistics file";
    public static final String lastJava7Warning = "Last Java 7 warning";

    // Rules options
    public static final String variant = "Variant";
    public static final String variantFileWithFullPath = "Variant full path";
    public static final String viewMode = "ViewMode";
    public static final String dubiousAsBlanks = "Uncertain as blank (Autoinspector etc.)";
    public static final String showMarker = "Show Marker (Autoinspector etc.)";
    public static final String localOnlyOwn = "Locally only own legions";

    // Web Client specific:
    public static final String minPlayersWeb = "Min Players Web Client";
    public static final String targPlayersWeb = "Target Players Web Client";
    public static final String maxPlayersWeb = "Max Players Web Client";
    public static final String uponMessageToFront = "Window to front for message";

    /* selections for viewable Legions */
    public static final String viewableOwn = "Only own legions";
    public static final String viewableLast = "Revealed during last turn";
    public static final String viewableEver = "Ever revealed (or concludable) since start";
    public static final String viewableAll = "True content for all legions";

    public static final String[] viewModeArray = { viewableOwn, viewableEver,
        viewableAll };

    public static final int viewableOwnNum = 1;
    public static final int viewableLastNum = 2;
    public static final int viewableEverNum = 3;
    public static final int viewableAllNum = 4;

    public static final String eventExpiring = "EventExpire";
    public static final String eventExpiringNever = "never";
    public static final String[] eventExpiringChoices = { "1", "2", "5", "10",
        eventExpiringNever };

    public static final String balancedTowers = "Balanced starting towers";
    public static final String allStacksVisible = "All stacks visible";
    public static final String onlyOwnLegions = "Only own legions viewable";
    public static final String cumulativeSlow = "Slowing is cumulative";
    public static final String oneHexAllowed = "Always allows one hex";
    public static final String fixedSequenceBattleDice = "Fixed-sequence battle dice";
    public static final String pbBattleHits = "Probability-based battle hits";
    public static final String sansLordAutoBattle = "Need lord for battle control";
    public static final String inactivityTimeout = "Inactivity timeout";

    public static final String noFirstTurnT2TTeleport = "No tower-to-tower Teleport on first turn";
    public static final String noFirstTurnTeleport = "No Teleport on first turn";
    public static final String towerToTowerTeleportOnly = "Tower-to-Tower Teleport only";
    public static final String noTowerTeleport = "No Tower Teleport";
    public static final String noTitanTeleport = "No Titan Teleport";
    public static final String noFirstTurnWarlockRecruit = "No Warlock recruiting on first turn";

    public static final String unlimitedMulligans = "Unlimited Mulligans";
    public static final String enableEditingMode = "Enable Editing Mode";

    // Those are global options which need to be transferred to clients, even if
    // not set (meaning false), but if Client has them stored from earlier synchronizations
    // (e.g. removed server.cf file, or now playing on public server)
    public static final String[] globalGameOptions = { cumulativeSlow,
        oneHexAllowed, fixedSequenceBattleDice, sansLordAutoBattle,
        noFirstTurnT2TTeleport, noFirstTurnTeleport, towerToTowerTeleportOnly,
        noTowerTeleport, noTitanTeleport, unlimitedMulligans,
        noFirstTurnWarlockRecruit, enableEditingMode, inactivityTimeout };

    // Display options (client only)
    public static final String stealFocus = "Steal focus";
    public static final String turnStartBeep = "When my turns starts, beep";
    public static final String turnStartToFront = "When my turn starts, window to front";
    public static final String turnStartBottomBarYellow = "When my turn starts, turn BottomBar yellow";
    public static final String turnStartChatYellow = "When my turn starts, turn Chat background yellow";
    public static final String BattleTerrainHazardWindow = "Show Terrain and Hazards";
    public static final String showCaretaker = "Show Caretaker's stacks";
    public static final String showStatusScreen = "Show game status";
    public static final String showAutoInspector = "Show inspector";
    public static final String showEventViewer = "Show event window";
    public static final String showLogWindow = "Show log window";
    public static final String showConnectionLogWindow = "Show connection log window";
    public static final String showWebClient = "Show web client";
    public static final String suppressedWelcomeDialog = "Suppressed Welcome Dialog";

    public static final String showEngagementResults = "Show engagement results";
    public static final String useOverlay = "Use Graphical Overlay";
    public static final String noBaseColor = "Use black overlay on Chits";
    public static final String playerColoredAngels = "Use player colored Angels";

    public static final String useColoredBorders = "Use colored borders on Battle Chits";
    public static final String doNotInvertDefender = "Do not invert defender's Battle Chits";
    public static final String showHitThreshold = "Show needed roll for hit";
    public static final String showDiceAjustmentsTerrain = "Show added/lost dice due to terrain";
    public static final String showDiceAjustmentsRange = "Show lost dice due to rangestrike";
    public static final String showAllRecruitChits = "Show all recruit Chits";
    public static final String showRecruitChitsSubmenu = "Show recruit preview chits...";
    public static final String showRecruitChitsNone = "None";
    public static final String showRecruitChitsStrongest = "Strongest";
    public static final String showRecruitChitsRecruitHint = "Recruit Hint";
    public static final String showRecruitChitsAll = "All";

    public static final int showRecruitChitsNumNone = 0;
    public static final int showRecruitChitsNumStrongest = 1;
    public static final int showRecruitChitsNumRecruitHint = 2;
    public static final int showRecruitChitsNumAll = 3;

    public static final String antialias = "Antialias";
    public static final String scale = "Scale";

    // Window locations and sizes (client only)
    public static final String locX = "Location X";
    public static final String locY = "Location Y";
    public static final String sizeX = "Size X";
    public static final String sizeY = "Size Y";

    // AI options (client only)
    public static final String autoPickColor = "Auto pick color";
    public static final String autoPickMarker = "Auto pick markers";
    public static final String autoSplit = "Auto split";
    public static final String autoMasterMove = "Auto masterboard move";
    public static final String autoPickEntrySide = "Auto pick entry sides";
    public static final String autoPickLord = "Auto pick teleporting lord";
    public static final String autoPickEngagements = "Auto pick engagements";
    public static final String autoFlee = "Auto flee";
    public static final String autoConcede = "Auto concede";
    public static final String autoNegotiate = "Auto negotiate";
    public static final String autoForcedStrike = "Auto forced strike";
    public static final String autoCarrySingle = "Auto carry single";
    public static final String autoRangeSingle = "Auto rangestrike single";
    public static final String autoSummonAngels = "Auto summon angels";
    public static final String autoAcquireAngels = "Auto acquire angels";
    public static final String autoRecruit = "Auto recruit";
    public static final String autoPickRecruiter = "Auto pick recruiters";
    public static final String autoReinforce = "Auto reinforce";
    public static final String autoPlay = "Auto play";

    // Confirmations (client only)
    public static final String confirmNoRecruit = "Confirm when not all possible recruits taken";
    public static final String confirmNoMove = "Confirm when not all possible moves made";
    public static final String confirmNoSplit = "Confirm when not all full legions split";
    public static final String confirmConcedeWithTitan = "Confirm when you concede with your Titan";
    public static final String legionMoveConfirmationSubMenu = "Legion Movement Confirmation";
    public static final String legionMoveConfirmationNoMove = "Any legion has not moved";
    public static final String legionMoveConfirmationNoUnvisitedMove = "Unvisited legion has not moved";
    public static final String legionMoveConfirmationNoConfirm = "No movement confirmation";

    public static final int legionMoveConfirmationNumNoConfirm = 0;
    public static final int legionMoveConfirmationNumMove = 1;
    public static final int legionMoveConfirmationNumUnvisitedMove = 2;

    public static final String nextSplitSubMenu = "Split Phase Next Key Mouse Click";
    public static final String nextMove = "Move Phase Next Key Left Click";
    public static final String nextMuster = "Muster Phase Next Key Left Click";
    public static final String nextSplitAllSplitable = "Split Phase Visit All Splittable Legions";
    public static final String nextSplitLeftClick = "Split Phase Next Key Left Click (show markers)";
    public static final String nextSplitRightClick = "Split Phase Next Key Right Click (show creatures)";
    public static final String nextSplitNoClick = "Split Phase Next Key No Mouse Click";

    public static final int nextSplitNumNoClick = 0;
    public static final int nextSplitNumLeftClick = 1;
    public static final int nextSplitNumRightClick = 2;

    // AI timing options (client only)
    public static final String aiTimeLimit = "AI time limit";
    public static final String aiDelay = "AI delay";

    // General per-player options (client only)
    public static final String favoriteColors = "Favorite colors";
    public static final String favoriteLookFeel = "Favorite Look And Feel";
    public static final String serverName = "Server name";
    public static final String activePreferencesTab = "Active preferences tab";

    public static final String editModeActive = "Edit Mode";
    public static final String legionListByMarkerId = "Sort legion list by marker Id";

    private final Properties props = new Properties();
    private final String owner; // playerName, or Constants.optionsServerName
    private final String dataPath; // WebServer sets to create a server.cfg file
    // AIs and the "startOption" do not read nor write to an actual file
    private boolean noFile;
    // Unit and functional tests read a file, but should not write anything back
    private final boolean readOnly;

    private final Map<String, List<Listener>> listeners = new HashMap<String, List<Listener>>();

    public Options(String owner, String customPath, boolean noFile,
        boolean readOnly)
    {
        this.owner = owner;
        this.dataPath = customPath;
        this.noFile = noFile;
        this.readOnly = readOnly;
    }

    public Options(String owner, String customPath, boolean noFile)
    {
        this.owner = owner;
        this.dataPath = customPath;
        this.noFile = noFile;
        this.readOnly = false;
    }

    public Options(String owner)
    {
        this(owner, Constants.DEFAULT_COLOSSUS_HOME, false);
    }

    public Options(String owner, boolean noFile)
    {
        this(owner, Constants.DEFAULT_COLOSSUS_HOME, noFile);
    }

    public String getOptionsFilename()
    {
        return dataPath + "/" + Constants.OPTIONS_BASE + owner
            + Constants.OPTIONS_EXTENSION;
    }

    synchronized public void loadOptions()
    {
        // As long as we don't prevent e.g. Human players to
        // choose "<byColor>" have to block it here - otherwise
        // it gives File Exceptions due to the "<" character.
        if (owner.startsWith(Constants.byColor)
            || owner.startsWith(Constants.byType))
        {
            this.noFile = true;
        }

        // Don't load if noFile is set - typically AI players
        if (this.noFile)
        {
            return;
        }

        String optionsFile = getOptionsFilename();
        LOGGER.info("Loading options from " + optionsFile);
        FileInputStream in = null;
        try
        {
            in = new FileInputStream(optionsFile);
            props.load(in);
            // The pure "Player\ Type\ " (without number) is not needed,
            // it causes on Client side trouble if there is a listener
            props.remove(Options.playerType);
            triggerAllOptions();
        }
        catch (IOException e)
        {
            // this can happen for the netclient-config and others, so
            // don't warn
            LOGGER.info("Couldn't read options from " + optionsFile);
            return;
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    LOGGER.log(Level.WARNING,
                        "Could not close options file properly", e);
                }
            }
        }
    }

    synchronized public void saveOptions()
    {
        // Don't save if noFile is set - typically AI players
        if (this.noFile)
        {
            return;
        }

        // Don't save if readOnly mode was set - typically test cases
        if (this.readOnly)
        {
            return;
        }

        String optionsFile = getOptionsFilename();

        File optionsDir = new File(dataPath);
        if (!optionsDir.exists() || !optionsDir.isDirectory())
        {
            LOGGER.log(Level.INFO, "Trying to make directory " + dataPath);
            if (!optionsDir.mkdirs())
            {
                LOGGER.log(Level.SEVERE, "Could not create options directory "
                    + optionsDir.toString());
                return;
            }
        }

        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(optionsFile);
            props.store(out, Constants.CONFIG_VERSION);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Couldn't write options to "
                + optionsFile, e);
        }

        try
        {
            if (out != null)
            {
                out.close();
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE,
                "Couldn't close outputstream after options written to "
                    + optionsFile, e);
        }
    }

    synchronized public void setOption(String optname, String value)
    {
        String oldValue = getStringOption(optname);
        if (!value.equals(oldValue))
        {
            props.setProperty(optname, value);
            triggerStringOption(optname, oldValue, value);
        }
    }

    synchronized public void setOption(String optname, boolean value)
    {
        boolean undefined = isOptionUndefined(optname);
        boolean oldValue = getOption(optname);
        if (undefined || oldValue != value)
        {
            setOption(optname, String.valueOf(value));
            triggerBooleanOption(optname, oldValue, value);
        }
    }

    synchronized public void setOption(String optname, int value)
    {
        int oldValue = getIntOption(optname);
        if (oldValue != value)
        {
            setOption(optname, String.valueOf(value));
            triggerIntOption(optname, oldValue, value);
        }
    }

    synchronized public String getStringOption(String optname)
    {
        String value = props.getProperty(optname);
        return value;
    }

    synchronized public String getStringOption(String optname,
        String defaultValue)
    {
        String value = props.getProperty(optname, defaultValue);
        return value;
    }

    synchronized public boolean getOption(String optname)
    {
        // the "if start with "Auto "... removed, that part is
        // now handled by AutoPlay class.
        String value = getStringOption(optname);
        return (value != null && value.equals("true"));
    }

    synchronized public boolean getOption(String optname, boolean defaultValue)
    {
        String value = getStringOption(optname, (defaultValue ? "true"
            : "false"));
        return (value != null && value.equals("true"));
    }

    /** Return -1 if the option's value has not been set. */
    synchronized public int getIntOption(String optname)
    {
        String buf = getStringOption(optname);
        int value = -1;
        try
        {
            value = Integer.parseInt(buf);
        }
        catch (NumberFormatException ex)
        {
            value = -1;
        }
        return value;
    }

    public boolean isOptionUndefined(String optname)
    {
        return (getStringOption(optname) == null);
    }

    synchronized public void removeOption(String optname)
    {
        props.remove(optname);
    }

    // we know we didn't mistreat Properties by adding non-Strings
    @SuppressWarnings("unchecked")
    synchronized public Enumeration<String> propertyNames()
    {
        return (Enumeration<String>)props.propertyNames();
    }

    /** Remove all playerName and playerType entries. */
    synchronized public void clearPlayerInfo()
    {
        Enumeration<String> en = propertyNames();
        while (en.hasMoreElements())
        {
            String name = en.nextElement();
            if (name.startsWith(playerName) || name.startsWith(playerType))
            {
                props.remove(name);
            }
        }
    }

    /** Wipe everything. */
    synchronized public void clear()
    {
        props.clear();
    }

    synchronized public boolean isEmpty()
    {
        return props.isEmpty();
    }

    @Override
    public String toString()
    {
        return props.toString();
    }

    // client compares then only numeric modes (easier and faster in runtime)
    // TODO this one and the next one could be better solved with enums
    synchronized public int getNumberForViewMode(String viewMode)
    {
        int val = Options.viewableAllNum;
        if (viewMode == null)
        {
            return Options.viewableAllNum;
        }
        if (viewMode.equals(Options.viewableAll))
        {
            val = Options.viewableAllNum;
        }
        if (viewMode.equals(Options.viewableEver))
        {
            val = Options.viewableEverNum;
        }
        if (viewMode.equals(Options.viewableLast))
        {
            val = Options.viewableLastNum;
        }
        if (viewMode.equals(Options.viewableOwn))
        {
            val = Options.viewableOwnNum;
        }
        return val;
    }

    /*
     // Right now (09/2007) not used anywhere; but perhaps web server should
     // store and transmit only the numerical modes, then this would be needed.
     // so I keep it here for now.
     public static String xgetStringForViewMode(int val)
     {
         String text = Options.viewableAll;
         switch(val)
         {
         case Options.viewableAllNum: text = Options.viewableAll; break;
         case Options.viewableEverNum: text = Options.viewableEver; break;
         case Options.viewableLastNum: text = Options.viewableLast; break;
         case Options.viewableOwnNum: text = Options.viewableOwn; break;
         default: text = Options.viewableAll; break;
         }

         return text;
     }
     */

    // client compares then only numeric modes (easier and faster in runtime)
    // ((can use switch case statement))
    synchronized public int getNumberForRecruitChitSelection(String s)
    {
        if (s == null || s.equals(""))
        {
            return Options.showRecruitChitsNumNone;
        }

        int val = Options.showRecruitChitsNumAll;
        if (s.equals(Options.showRecruitChitsNone))
        {
            val = Options.showRecruitChitsNumNone;
        }
        if (s.equals(Options.showRecruitChitsStrongest))
        {
            val = Options.showRecruitChitsNumStrongest;
        }
        if (s.equals(Options.showRecruitChitsRecruitHint))
        {
            val = Options.showRecruitChitsNumRecruitHint;
        }
        if (s.equals(Options.showRecruitChitsAll))
        {
            val = Options.showRecruitChitsNumAll;
        }
        return val;
    }

    synchronized public int getNumberForLegionMoveConfirmation(String s)
    {
        if (s == null || s.equals(""))
        {
            return Options.legionMoveConfirmationNumNoConfirm;
        }

        int val = Options.legionMoveConfirmationNumNoConfirm;
        if (s.equals(Options.legionMoveConfirmationNoMove))
        {
            val = Options.legionMoveConfirmationNumMove;
        }
        if (s.equals(Options.legionMoveConfirmationNoUnvisitedMove))
        {
            val = Options.legionMoveConfirmationNumUnvisitedMove;
        }
        return val;
    }

    synchronized public int getNumberForNextSplit(String s)
    {
        if (s == null || s.equals(""))
        {
            return Options.nextSplitNumNoClick;
        }

        int val = Options.nextSplitNumLeftClick;
        if (s.equals(Options.nextSplitRightClick))
        {
            val = Options.nextSplitNumRightClick;
        }
        if (s.equals(Options.nextSplitNoClick))
        {
            val = Options.nextSplitNumNoClick;
        }
        return val;
    }

    private static boolean functionalTestOngoing = false;

    public static void setFunctionalTest(boolean val)
    {
        functionalTestOngoing = val;
    }

    public static boolean isFunctionalTest()
    {
        return functionalTestOngoing;
    }

    private static boolean startupTestOngoing = false;

    public static void setStartupTest(boolean val)
    {
        setFunctionalTest(val);
        startupTestOngoing = val;
    }

    public static boolean isStartupTest()
    {
        return startupTestOngoing;
    }

    private static String propNameStresstestRounds = "net.sf.colossus.stressTestRounds";

    public static boolean isStresstest()
    {
        return System.getProperty(propNameStresstestRounds) != null;
    }

    synchronized public static int getHowManyStresstestRoundsProperty()
    {
        String propHowMany = System.getProperty(propNameStresstestRounds);
        int howMany = 0;

        if (propHowMany != null)
        {
            try
            {
                int i = Integer.parseInt(propHowMany);
                howMany = i;
            }
            catch (NumberFormatException ex)
            {
                howMany = 1;
                LOGGER.log(Level.WARNING, "NOTE: Value '" + propHowMany
                    + "' from property " + propNameStresstestRounds
                    + " is not a valid number - using default value 1!");
            }
        }
        return howMany;
    }

    synchronized public void addListener(String optname, Listener listener)
    {
        List<Listener> optionListeners = getListenersForOption(optname);
        optionListeners.add(listener);
    }

    private List<Listener> getListenersForOption(String optname)
    {
        List<Listener> optionListeners = listeners.get(optname);
        if (optionListeners == null)
        {
            optionListeners = new ArrayList<Listener>();
            listeners.put(optname, optionListeners);
        }
        return optionListeners;
    }

    synchronized public void removeListener(Listener listener)
    {
        for (List<Listener> optionListeners : listeners.values())
        {
            optionListeners.remove(listener);
        }
    }

    private void triggerBooleanOption(String optname, boolean oldValue,
        boolean newValue)
    {
        List<Listener> optionListeners = getListenersForOption(optname);
        for (Listener listener : optionListeners)
        {
            listener.booleanOptionChanged(optname, oldValue, newValue);
        }
    }

    private void triggerIntOption(String optname, int oldValue, int newValue)
    {
        List<Listener> optionListeners = getListenersForOption(optname);
        for (Listener listener : optionListeners)
        {
            listener.intOptionChanged(optname, oldValue, newValue);
        }
    }

    private void triggerStringOption(String optname, String oldValue,
        String newValue)
    {
        List<Listener> optionListeners = getListenersForOption(optname);
        for (Listener listener : optionListeners)
        {
            listener.stringOptionChanged(optname, oldValue, newValue);
        }
    }

    private void triggerAllOptions()
    {
        for (Map.Entry<Object, Object> option : props.entrySet())
        {
            String optname = (String)option.getKey();
            String stringVal = (String)option.getValue();
            // try triggering things as integer or boolean first
            // (which implies a string trigger)
            // make sure the oldVal is guaranteed to be different
            try
            {
                int intVal = Integer.parseInt(stringVal);
                // don't just negate the value, it might be Integer.MIN_VALUE
                // used by someone as a marker
                if (intVal == 0)
                {
                    triggerIntOption(optname, 1, 0);
                }
                else
                {
                    triggerIntOption(optname, 0, intVal);
                }
            }
            catch (NumberFormatException e)
            {
                // so it is not a number, let's try boolean
                if (stringVal.equalsIgnoreCase("true"))
                {
                    triggerBooleanOption(optname, false, true);
                }
                else if (stringVal.equalsIgnoreCase("false"))
                {
                    triggerBooleanOption(optname, true, false);
                }
                else
                {
                    // neither int nor boolean
                    triggerStringOption(optname, null, stringVal);
                }
            }
        }
    }
}
