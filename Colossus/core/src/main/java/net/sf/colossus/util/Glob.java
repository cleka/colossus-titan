package net.sf.colossus.util;


import java.util.Collection;
import java.util.Iterator;


/**
 * Stringify contents of various container classes.
 *
 * @see Split
 *
 * @author David Ripton
 */

public final class Glob
{
    public static final String sep = " %@% ";

    public static String glob(String sep, String[] a)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length - 1; i++)
        {
            sb.append(a[i]);
            sb.append(sep);
        }
        sb.append(a[a.length - 1]);
        return sb.toString();
    }

    public static String glob(String[] a)
    {
        return Glob.glob(Glob.sep, a);
    }

    public static String glob(String sep, int[] a)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length - 1; i++)
        {
            sb.append(a[i]);
            sb.append(sep);
        }
        sb.append(a[a.length - 1]);
        return sb.toString();
    }

    public static String glob(int[] a)
    {
        return Glob.glob(Glob.sep, a);
    }

    public static String glob(String sep, Collection<?> col)
    {
        StringBuilder sb = new StringBuilder();
        if (col != null)
        {
            Iterator<?> it = col.iterator();
            while (it.hasNext())
            {
                Object ob = it.next();
                if (ob == null)
                {
                    ob = "null";
                }
                sb.append(ob.toString());
                if (it.hasNext())
                {
                    sb.append(sep);
                }
            }
        }
        return sb.toString();
    }

    public static String glob(Collection<?> col)
    {
        return Glob.glob(Glob.sep, col);
    }
}
