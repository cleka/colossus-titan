package net.sf.colossus.util;


import java.util.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.server.Constants;


/** Class Options lists game options for Colossus.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Options
{
    // Everything is public because we use this class in both the client
    // and server packages.  (With separate data.)


    // Option names

    // Server options
    public static final String autosave = "Autosave";
    public static final String logDebug = "Log debug messages";
    public static final String balancedTowers = "Balanced starting towers";
    public static final String allStacksVisible = "All stacks visible";
    public static final String autoStop = "AIs stop when humans dead";
    public static final String autoQuit = "Auto quit when game over";
    public static final String aiDelay = "AI delay";  // int
    public static final String variant = "Variant";   // string
    // t-port option
    public static final String noFirstTurnT2TTeleport = 
        "No tower-to-tower Teleport on first turn";
    public static final String noFirstTurnTeleport = 
        "No Teleport on first turn";
    public static final String towerToTowerTeleportOnly = 
        "Tower-to-Tower Teleport only";
    public static final String noTowerTeleport = "No Tower Teleport";
    public static final String noTitanTeleport = "No Titan Teleport";
    

    // Will add player numbers 0-5 to the the of these.
    public static final String playerName = "Player name ";
    public static final String playerType = "Player type ";



    // Display options (client)
    public static final String showCaretaker = "Show Caretaker's stacks";
    public static final String showStatusScreen = "Show game status";
    public static final String showLogWindow = "Show log window";
    public static final String useOverlay = "Use Graphical Overlay";
    public static final String noBaseColor = "Use black overlay on Chits";
    public static final String antialias = "Antialias";
    public static final String scale = "Scale";

    // AI options (player - on client)
    public static final String autoPickColor = "Auto pick color";
    public static final String autoPickMarker = "Auto pick markers";
    public static final String autoPickEntrySide = "Auto pick entry sides";
    public static final String autoNegotiate = "Auto negotiate";
    public static final String autoForcedStrike = "Auto forced strike";
    public static final String autoCarrySingle = "Auto carry single";
    public static final String autoRangeSingle = "Auto rangestrike single";
    public static final String autoSummonAngels = "Auto summon angels";
    public static final String autoAcquireAngels = "Auto acquire angels";
    public static final String autoPickRecruiter = "Auto pick recruiters";
    public static final String autoPlay = "Auto play";

    // General per-player options
    public static final String favoriteColors = "Favorite colors";
    public static final String favoriteLookFeel = "Favorite Look And Feel";


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
        loadOptions(getOptionsFilename());
    }

    public void loadOptions(String optionsFile)
    {
        try
        {
            FileInputStream in = new FileInputStream(optionsFile);
            props.load(in);
        }
        catch (IOException e)
        {
            Log.debug("Couldn't read options from " + optionsFile);
            return;
        }
    }

    
    public void saveOptions()
    {
        File optionsDir = new File(Constants.gameDataPath);
        if (!optionsDir.exists() || !optionsDir.isDirectory())
        {
             Log.event("Trying to make directory " + Constants.gameDataPath);
             if (!optionsDir.mkdirs())
             {
                 Log.error("Could not create options directory");
                 return;
             }
        }

        String optionsFile = getOptionsFilename();
        try
        {
            FileOutputStream out = new FileOutputStream(optionsFile);
            props.store(out, Constants.configVersion);
            out.close();
        }
        catch (IOException e)
        {
            Log.error("Couldn't write options to " + optionsFile);
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
        catch (Exception ex)
        {
            value = -1;
        }
        return value;
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
}
