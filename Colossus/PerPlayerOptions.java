import java.util.*;

/** Class PerPlayerOptions holds per player options.
 *  @version $Id$
 *  @author David Ripton
 */

public class PerPlayerOptions
{
    private static HashSet set = new HashSet();

    static
    {
        set.add(Options.autoRecruit);
        set.add(Options.autoPickRecruiter);
        set.add(Options.autoPickMarker);
        set.add(Options.autoPickEntrySide);
        set.add(Options.autoForcedStrike);
        set.add(Options.autoSplit);
        set.add(Options.autoMasterMove);
        set.add(Options.autoFlee);
        set.add(Options.autoPlay);
        set.add(Options.autoStrike);
        set.add(Options.autoBattleMove);
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
