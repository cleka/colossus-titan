package net.sf.colossus.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.IOptions;
import net.sf.colossus.server.Constants;


/** Class Options lists game options for Colossus.
 *  @version $Id$
 *  @author David Ripton
 *  
 *  TODO constants should be all uppercase
 */

public final class Options implements IOptions
{
    private static final Logger LOGGER = Logger.getLogger(Options.class.getName());

    // Everything is public because we use this class in both the client
    // and server packages.  (With separate data.)


    // Non-options that use the options framework
    // Will add player numbers 0 through n-1 to the end of these.
    public static final String playerName = "Player name ";
    public static final String playerType = "Player type ";

    // Option names

    // Server administrative options
    public static final String autosave = "Autosave";
    public static final String logDebug = "Log debug messages";
    public static final String autoStop = "AIs stop when humans dead";
    public static final String autoQuit = "Auto quit when game over";

    // Rules options
    public static final String variant = "Variant";
    public static final String viewMode = "ViewMode";
    public static final String dubiousAsBlanks = "Uncertain as blank (Autoinspector etc.)";

    /* selections for viewable Legions */
    public static final String viewableOwn  = "Only own legions";
    public static final String viewableLast = "Revealed during last turn";
    public static final String viewableEver = "Ever revealed (or concludable) since start";
    public static final String viewableAll  =  "True content for all legions";

    public static final String[] viewModeArray =
        {
        viewableOwn,
        //        viewableLast,  // not implemented yet.
        viewableEver,
        viewableAll
    };

    public static final int viewableOwnNum  = 1;
    public static final int viewableLastNum = 2;
    public static final int viewableEverNum = 3;
    public static final int viewableAllNum  = 4;

    public static final String eventExpiring = "EventExpire";
    public static final String eventExpiringNever = "never";
    public static final String[] eventExpiringChoices =
        {
        "1", "2", "5", "10", eventExpiringNever
    };

    public static final String balancedTowers = "Balanced starting towers";
    public static final String allStacksVisible = "All stacks visible";
    public static final String onlyOwnLegions = "Only own legions viewable";
    public static final String cumulativeSlow = "Slowing is cumulative";
    public static final String oneHexAllowed = "Always allows one hex";
    public static final String nonRandomBattleDice =
        "Use non-random battle dice";

    public static final String noFirstTurnT2TTeleport =
        "No tower-to-tower Teleport on first turn";
    public static final String noFirstTurnTeleport =
        "No Teleport on first turn";
    public static final String towerToTowerTeleportOnly =
        "Tower-to-Tower Teleport only";
    public static final String noTowerTeleport = "No Tower Teleport";
    public static final String noTitanTeleport = "No Titan Teleport";

    public static final String unlimitedMulligans = "Unlimited Mulligans";

    // Display options (client only)
    public static final String useSVG = "Use SVG chits";
    public static final String stealFocus = "Steal focus";
    public static final String showCaretaker = "Show Caretaker's stacks";
    public static final String showStatusScreen = "Show game status";
    public static final String showAutoInspector = "Show inspector";
    public static final String showEventViewer = "Show event window";
    public static final String showLogWindow = "Show log window";
    public static final String showEngagementResults =
        "Show engagement results";
    public static final String useOverlay = "Use Graphical Overlay";
    public static final String noBaseColor = "Use black overlay on Chits";
    public static final String useColoredBorders =
        "Use colored borders on Battle Chits";
    public static final String doNotInvertDefender =
        "Do not invert defender's Battle Chits";
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

    // AI timing options (client only)
    public static final String aiTimeLimit = "AI time limit";
    public static final String aiDelay = "AI delay";

    // General per-player options (client only)
    public static final String favoriteColors = "Favorite colors";
    public static final String favoriteLookFeel = "Favorite Look And Feel";
    public static final String serverName = "Server name";

    private Properties props = new Properties();
    private String owner;      // playerName, or Constants.optionsServerName

    public Options(String owner)
    {
        this.owner = owner;
    }

    public String getOptionsFilename()
    {
        return Constants.gameDataPath + Constants.optionsBase + owner +
            Constants.optionsExtension;
    }

    public void loadOptions()
    {
        // Don't load from temporary player names.
        if (owner.startsWith(Constants.byColor) ||
            owner.startsWith(Constants.byType) ||
            owner.startsWith(Constants.byClient))
        {
            return;
        }

        String optionsFile = getOptionsFilename();
        LOGGER.log(Level.INFO, "Loading options from " + optionsFile);
        try
        {
            FileInputStream in = new FileInputStream(optionsFile);
            props.load(in);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.INFO,
                "Couldn't read options from " + optionsFile);
            return;
        }
    }

    public void saveOptions()
    {
        // Don't save from temporary player names.
        if (owner.startsWith(Constants.byColor) ||
            owner.startsWith(Constants.byType) ||
            owner.startsWith(Constants.byClient))
        {
            return;
        }
        String optionsFile = getOptionsFilename();

        File optionsDir = new File(Constants.gameDataPath);
        if (!optionsDir.exists() || !optionsDir.isDirectory())
        {
            LOGGER.log(Level.INFO,
                "Trying to make directory " + Constants.gameDataPath);
            if (!optionsDir.mkdirs())
            {
                LOGGER.log(Level.SEVERE, 
                    "Could not create options directory " + 
                    optionsDir.toString());
                return;
            }
        }

        try
        {
            FileOutputStream out = new FileOutputStream(optionsFile);
            props.store(out, Constants.configVersion);
            out.close();
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, 
                "Couldn't write options to " + optionsFile, e);
        }
    }

    public void setOption(String optname, String value)
    {
        props.setProperty(optname, value);
    }

    public void setOption(String optname, boolean value)
    {
        setOption(optname, String.valueOf(value));
    }

    public void setOption(String optname, int value)
    {
        setOption(optname, String.valueOf(value));
    }

    public String getStringOption(String optname)
    {
        String value = props.getProperty(optname);
        return value;
    }

    public String getStringOption(String optname, String defaultValue)
    {
        String value = props.getProperty(optname, defaultValue);
        return value;
    }

    public boolean getOption(String optname)
    {
        String value = getStringOption(optname);
        return (value != null && value.equals("true"));
    }

    /** Return -1 if the option's value has not been set. */
    public int getIntOption(String optname)
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

    public void removeOption(String optname)
    {
        props.remove(optname);
    }

    public Enumeration propertyNames()
    {
        return props.propertyNames();
    }

    /** Remove all playerName and playerType entries. */
    public void clearPlayerInfo()
    {
        Enumeration en = props.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            if (name.startsWith(playerName) || name.startsWith(playerType))
            {
                props.remove(name);
            }
        }
    }

    /** Wipe everything. */
    public void clear()
    {
        props.clear();
    }

    public boolean isEmpty()
    {
        return props.isEmpty();
    }

    public String toString()
    {
        return props.toString();
    }

    // client compares then only numeric modes (easier and faster in runtime)
    public int getNumberForViewMode(String viewMode)
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

    // client compares then only numeric modes (easier and faster in runtime)
    // ((can use switch case statement))
    public int getNumberForRecruitChitSelection(String s)
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

}
