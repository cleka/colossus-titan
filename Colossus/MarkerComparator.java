import java.util.*;

/** 
 * Comparator that forces one player's own legion markers to come
 *  before captured markers in sort order.
 * @version $Id$
 * @author David Ripton
 */

final class MarkerComparator implements Comparator
{
    private String myPrefix;

    private MarkerComparator(String myPrefix)
    {
        if (myPrefix == null)
        {
            myPrefix = "";
        }
        this.myPrefix = myPrefix;
    }

    public static MarkerComparator getMarkerComparator(String myPrefix)
    {
        return new MarkerComparator(myPrefix);
    }

    public int compare(Object o1, Object o2)
    {
        if (!(o1 instanceof String) || !(o2 instanceof String))
        {
            throw new ClassCastException();
        }
        String s1 = (String)o1;
        String s2 = (String)o2;
        boolean mine1 = s1.startsWith(myPrefix);
        boolean mine2 = s2.startsWith(myPrefix);
        if (mine1 && !mine2)
        {
            return -1;
        }
        else if (mine2 && !mine1)
        {
            return 1;
        }
        else
        {
            return s1.compareTo(s2);
        }
    }
}
