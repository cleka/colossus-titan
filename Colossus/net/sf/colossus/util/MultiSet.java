package net.sf.colossus.util;


import java.util.*;


/**
 * Class MultiSet is a set that can contain more than one of the same
 * element, built around a HashMap that maps objects to Integer counts.
 */
public class MultiSet
{
    private Map map;

    public MultiSet()
    {
        map = new HashMap();
    }

    public void add(Object key)
    {
        if (!contains(key))
        {
            map.put(key, new Integer(1));
        }
        else
        {
            Integer val = (Integer)map.get(key);
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
        Integer val = (Integer)map.get(key);
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
        Integer val = (Integer)map.get(key);
        return val.intValue();
    }

    public Collection keySet()
    {
        return map.keySet();
    }

    public Collection values()
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
        SortedSet sorted = new TreeSet(values());
        Integer val = (Integer)sorted.last();
        return val.intValue();
    }
}
