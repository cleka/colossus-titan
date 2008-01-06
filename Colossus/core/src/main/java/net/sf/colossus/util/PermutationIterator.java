package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/** 
 * An iterator that returns permutations of the originally passed list.
 * 
 * The first permutation is the unmodified list.
 */
public final class PermutationIterator<T> implements Iterator<List<T>>
{
    private final List<T> permList;
    private final PermGen pg;
    private boolean foundNext = false;
    private boolean anyLeft = true;
    private boolean first = true;
    private int nextSwap;

    /** Set up a permutation generator for the passed list. */
    public PermutationIterator(List<T> list)
    {
        pg = new PermGen(list.size());

        // Since we're not going to mess with the elements, just
        // their order, a shallow copy should be fine.
        permList = new ArrayList<T>(list);
    }

    /** hasNext should not change things if called repeatedly,
    *  so when it's called we'll lazily evaluate the next
    *  permutation, and then keep returning true until next()
    *  is called. */
    public boolean hasNext()
    {
        if (first)
        {
            return true;
        }
        if (foundNext)
        {
            return anyLeft;
        }
        else
        {
            nextSwap = pg.getNext();
            foundNext = true;
            anyLeft = (nextSwap != -1);
            return anyLeft;
        }
    }

    public List<T> next()
    {
        // Return the unmodified list the first time.
        if (first)
        {
            first = false;
            return permList;
        }

        // If we haven't already found the next permutation, find it.
        if (!foundNext)
        {
            nextSwap = pg.getNext();
            foundNext = true;
            anyLeft = (nextSwap != -1);
        }
        // All done.
        if (!anyLeft)
        {
            return null;
        }
        // Modify and return the list.
        swap(nextSwap);
        foundNext = false;
        return permList;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /** Swap elements lower and lower + 1 of permList */
    private void swap(int lower)
    {
        T temp = permList.get(lower);
        permList.set(lower, permList.get(lower + 1));
        permList.set(lower + 1, temp);
    }
}
