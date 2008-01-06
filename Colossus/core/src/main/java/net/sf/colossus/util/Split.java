package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.List;


/** Perl-style split function.
 * 
 * TODO check if this is obsolete since JDK 1.4 introduced String.split(..)
 *  and document why not if not. 
 * 
 *  @version $Id$
 *  @author David Ripton
 */

public final class Split
{

    /** Split the string into a list of substrings delimited by sep. */
    public static List<String> split(final char sep, final String s)
    {
        return split("" + sep, s);
    }

    public static List<String> split(final String sep, final String s)
    {
        List<String> list = new ArrayList<String>();

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
