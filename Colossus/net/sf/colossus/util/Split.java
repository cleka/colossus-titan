package net.sf.colossus.util;


import java.util.*;


/** Miscellaneous utility functions.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Split
{
    /** Split the string into a list of substrings delimited by sep. */
    public static List split(final char sep, String s)
    {
        List list = new ArrayList();

        int pos = 0;
        int len = s.length();
        do
        {
            int splitAt = s.indexOf(sep, pos);
            if (splitAt == -1)
            {
                list.add(s.substring(pos));
                return list;
            }
            list.add(s.substring(pos, splitAt));
            pos = splitAt + 1;
        }
        while (pos < len);
        return list;
    }
}
