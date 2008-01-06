package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Returns all possible combinations of n elements of an ArrayList.
 * @version $Id$
 * @author David Ripton
 */
public final class Combos
{
    private ArrayList<List> retlist;

    public Combos(List list, int n)
    {
        retlist = new ArrayList<List>();
        findCombinations(list, n, new ArrayList());
    }

    void findCombinations(List alist, int n, List blist)
    {
        if (n < 0 || n > alist.size())
        {
            return;
        }
        if (n == 0)
        {
            List copy = new ArrayList();
            copy.addAll(blist);
            retlist.add(copy);
            return;
        }
        for (int i = 0; i < alist.size(); i++)
        {
            blist.add(alist.get(i));
            List sub = alist.subList(i + 1, alist.size());
            List subclone = new ArrayList();
            subclone.addAll(sub);
            findCombinations(subclone, n - 1, blist);
            blist.remove(blist.size() - 1);
        }
    }

    public Iterator<List> iterator()
    {
        return retlist.iterator();
    }
}
