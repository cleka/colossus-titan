package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Class Perms returns all possible permutations of an ArrayList.
 * 
 * @version $Id$
 * @author David Ripton
 */
public final class Perms<T>
{
    private final ArrayList<T> permList;
    private final PermGen pg;
    private boolean foundNext = false;
    private boolean anyLeft = true;
    private boolean first = true;
    private int nextSwap;

    /** Set up a permutation generator for the passed list. */
    // ArrayList.clone() not covariant in return type
    @SuppressWarnings("unchecked")
    public Perms(ArrayList<T> list)
    {
        pg = new PermGen(list.size());

        // Since we're not going to mess with the elements, just
        // their order, a shallow copy should be fine.
        permList = (ArrayList<T>)list.clone();
    }

    /** Returns an iterator that returns permutations of the originally
     *  passed list.  The first permutation is the unmodified list. */
    public Iterator<List<T>> iterator()
    {
        return new Iterator<List<T>>()
        {

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
        };
    }
}
