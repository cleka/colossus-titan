import java.util.*;

/** Class PerPlayerOptions holds per player options.
 *  @version $Id$
 *  @author David Ripton
 */

public final class PerPlayerOptions
{
    private static HashSet set = new HashSet();

    static
    {
        set.add(Options.autoPickMarker);
        set.add(Options.autoSplit);
        set.add(Options.autoMasterMove);
        set.add(Options.autoPickEntrySide);
        set.add(Options.autoPickEngagement);
        set.add(Options.autoFlee);
        set.add(Options.autoBattleMove);
        set.add(Options.autoForcedStrike);
        set.add(Options.autoStrike);
        set.add(Options.autoRecruit);
        set.add(Options.autoPickRecruiter);

        set.add(Options.autoPlay);
    }


    public static Iterator iterator()
    {
        return set.iterator();
    }


    public static boolean contains(String optname)
    {
        return set.contains(optname);
    }
}
