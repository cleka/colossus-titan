import java.util.*;

/** Class Options lists game options for Colossus.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Options
{
    // Option names

    // Server options
    public static final String autosave = "Autosave";
    public static final String allStacksVisible = "All stacks visible";

    // Debug options (server)
    public static final String chooseMovement = "Choose movement roll";
    public static final String chooseHits= "Choose number of hits";
    public static final String chooseTowers = "Choose towers";
    public static final String chooseCreatures = "Choose creatures";

    // Graphics options (client)
    public static final String showStatusScreen = "Show game status";
    public static final String showDice = "Show dice";
    public static final String antialias = "Antialias";

    // AI options (player)
    public static final String autoPickMarker = "Auto pick markers";
    public static final String autoSplit = "Auto split";
    public static final String autoMasterMove = "Auto masterboard move";
    public static final String autoPickEntrySide = "Auto pick entry sides";
    public static final String autoFlee = "Auto flee";
    public static final String autoPickEngagement = "Auto pick engagements";
    public static final String autoBattleMove = "Auto battle move";
    public static final String autoForcedStrike = "Auto forced strike";
    public static final String autoStrike = "Auto strike";
    public static final String autoSummonAngels = "Auto summon angels";
    public static final String autoAcquireAngels = "Auto acquire angels";
    public static final String autoRecruit = "Auto recruit";
    public static final String autoPickRecruiter = "Auto pick recruiters";

    public static final String autoPlay = "Auto play";

    private static HashSet perPlayerSet = new HashSet();

    static
    {
        perPlayerSet.add(autoPickMarker);
        perPlayerSet.add(autoSplit);
        perPlayerSet.add(autoMasterMove);
        perPlayerSet.add(autoPickEntrySide);
        perPlayerSet.add(autoPickEngagement);
        perPlayerSet.add(autoFlee);
        perPlayerSet.add(autoBattleMove);
        perPlayerSet.add(autoForcedStrike);
        perPlayerSet.add(autoStrike);
        perPlayerSet.add(autoSummonAngels);
        perPlayerSet.add(autoAcquireAngels);
        perPlayerSet.add(autoRecruit);
        perPlayerSet.add(autoPickRecruiter);

        perPlayerSet.add(Options.autoPlay);
    }


    public static Set getPerPlayerOptions()
    {
        return perPlayerSet;
    }
}
