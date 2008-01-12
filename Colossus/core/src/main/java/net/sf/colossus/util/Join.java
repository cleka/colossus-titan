package net.sf.colossus.util;


import java.util.List;
import java.util.Iterator;


/** Python-style join function.
 * 
 *  @author David Ripton
 */

public final class Join
{

    /** Join the list of Strings on the char separator. */
    public static String join(final List<String>list, final char sep)
    {
        return join(list, "" + sep);
    }

    /** Join the list of Strings on the String separator. */
    public static String join(final List<String>list, final String sep)
    {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = list.iterator(); it.hasNext();)
        {
            String str = it.next();
            sb.append(str);
            if (it.hasNext())
            {
                sb.append(sep);
            }
        }
        return sb.toString();
    }
}
