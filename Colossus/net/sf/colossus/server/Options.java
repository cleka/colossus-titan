package net.sf.colossus.server;


import java.util.*;


/** Class Options lists game options for Colossus.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Options
{
    // Constants related to the options config files
    public static final String optionsPath = "Colossus";
    public static final String optionsSep = "-";
    public static final String optionsExtension = ".cfg";

    public static final String configVersion =
        "Colossus config file version 2";


    // Option names

    // Server options
    public static final String autosave = "Autosave";
    public static final String allStacksVisible = "All stacks visible";
    public static final String logDebug = "Log debug messages";


    // Display options (client)
    public static final String showCaretaker = "Show Caretaker's stacks";
    public static final String showStatusScreen = "Show game status";
    public static final String antialias = "Antialias";
    public static final String scale = "Scale";
    public static final String aiDelay = "AI delay";

    // AI options (player - put on client)
    public static final String autoPickColor = "Auto pick color";
    public static final String autoPickMarker = "Auto pick markers";
    public static final String autoSplit = "Auto split";
    public static final String autoMasterMove = "Auto masterboard move";
    public static final String autoPickEntrySide = "Auto pick entry sides";
    public static final String autoFlee = "Auto flee";
    public static final String autoNegotiate = "Auto negotiate";
    public static final String autoPickEngagement = "Auto pick engagements";
    public static final String autoBattleMove = "Auto battle move";
    public static final String autoForcedStrike = "Auto forced strike";
    public static final String autoStrike = "Auto strike";
    public static final String autoSummonAngels = "Auto summon angels";
    public static final String autoAcquireAngels = "Auto acquire angels";
    public static final String autoRecruit = "Auto recruit";
    public static final String autoPickRecruiter = "Auto pick recruiters";
    public static final String autoPlay = "Auto play";

    // General per-player options
    public static final String favoriteColors = "Favorite colors";
}
