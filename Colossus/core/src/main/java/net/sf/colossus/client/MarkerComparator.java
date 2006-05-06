package net.sf.colossus.client;


import java.util.Comparator;


/**
 *  Compare markers.
 *  @version $Id$
 *  @author David Ripton
 */
public class MarkerComparator implements Comparator
{
    private String shortColor;

    public MarkerComparator(String shortColor)
    {
        if (shortColor == null)
        {
            this.shortColor = "None";
        }
        else
        {
            this.shortColor = shortColor;
        }
    }

    public int compare(Object o1, Object o2)
    {
        if (!(o1 instanceof String) || !(o2 instanceof String))
        {
            throw new ClassCastException();
        }
        String s1 = (String)o1;
        String s2 = (String)o2;
        if (s1.startsWith(shortColor) && !s2.startsWith(shortColor))
        {
            return -1;
        }
        if (!s1.startsWith(shortColor) && s2.startsWith(shortColor))
        {
            return 1;
        }
        return s1.compareTo(s2);
    }
}

