import java.util.*;

/** Class PerPlayerOptions holds a static set of per player options.
 *  @version $Id$
 *  @author David Ripton
 */

public class PerPlayerOptions
{
    private static HashSet set = new HashSet();

    static
    {
        set.add(Game.autoRecruit);
        set.add(Game.autoPickRecruiter);
        set.add(Game.autoPickMarker);
        set.add(Game.autoPickEntrySide);
        set.add(Game.autoForcedStrike);
        set.add(Game.autoSplit);
        set.add(Game.autoMasterMove);
        set.add(Game.autoFlee);
        set.add(Game.autoPlay);
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
