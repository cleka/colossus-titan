package net.sf.colossus.util;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Class MultiSet is a set that can contain more than one of the same
 * element, built around a HashMap that maps objects to Integer counts.
 * 
 * @version $Id$
 */
public class MultiSet
{
    private Map<Object, Integer> map;

    public MultiSet()
    {
        map = new HashMap<Object, Integer>();
    }

    public void add(Object key)
    {
        if (!contains(key))
        {
            map.put(key, new Integer(1));
        }
        else
        {
            Integer val = map.get(key);
            int prev = val.intValue();
            int cur = prev + 1;
            map.put(key, new Integer(cur));
        }
    }

    /** Remove one of key from the set, if present.  Return true iff it
     *  was present.
     */
    public boolean remove(Object key)
    {
        if (!contains(key))
        {
            return false;
        }
        Integer val = map.get(key);
        int prev = val.intValue();
        int cur = prev - 1;
        if (cur >= 1)
        {
            map.put(key, new Integer(cur));
        }
        else
        {
            map.remove(key);
        }
        return true;
    }

    public int size()
    {
        return map.size();
    }

    public boolean contains(Object key)
    {
        return map.containsKey(key);
    }

    public int count(Object key)
    {
        if (!contains(key))
        {
            return 0;
        }
        Integer val = map.get(key);
        return val.intValue();
    }

    public Collection<Object> keySet()
    {
        return map.keySet();
    }

    public Collection<Integer> values()
    {
        return map.values();
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public int max()
    {
        if (isEmpty())
        {
            return 0;
        }
        SortedSet<Integer> sorted = new TreeSet<Integer>(values());
        Integer val = sorted.last();
        return val.intValue();
    }
}
