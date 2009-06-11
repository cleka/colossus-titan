package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Returns all possible combinations of n elements of a List.
 * 
 * @author David Ripton
 */
public final class Combos<T>
{
    private final List<List<T>> retlist;

    public Combos(List<T> list, int n)
    {
        retlist = new ArrayList<List<T>>();
        findCombinations(list, n, new ArrayList<T>());
    }

    private void findCombinations(List<T> alist, int n, List<T> blist)
    {
        if (n < 0 || n > alist.size())
        {
            return;
        }
        if (n == 0)
        {
            List<T> copy = new ArrayList<T>();
            copy.addAll(blist);
            retlist.add(copy);
            return;
        }
        for (int i = 0; i < alist.size(); i++)
        {
            blist.add(alist.get(i));
            List<T> sub = alist.subList(i + 1, alist.size());
            List<T> subclone = new ArrayList<T>();
            subclone.addAll(sub);
            findCombinations(subclone, n - 1, blist);
            blist.remove(blist.size() - 1);
        }
    }

    public Iterator<List<T>> iterator()
    {
        return retlist.iterator();
    }
}
