package net.sf.colossus.util;


import java.util.*;


/** Perl-style split function.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Split
{

    /** Split the string into a list of substrings delimited by sep. */
    public static List split(final char sep, final String s)
    {
        return split("" + sep, s);
    }

    public static List split(final String sep, final String s)
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
            pos = splitAt + sep.length();
        }
        while (pos < len);
        return list;
    }
}
