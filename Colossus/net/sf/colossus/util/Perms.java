package net.sf.colossus.util;


import java.util.*;


/**
 * Class Perms returns all possible permutations of an ArrayList.
 * @version $Id$
 * @author Peter Unold
 * @author David Ripton
 */
public final class Perms
{
    private ArrayList permList = new ArrayList();
    private PermGen pg;
    private boolean foundNext = false;
    private boolean anyLeft = true;
    private boolean first = true;
    private int nextSwap;


    /** Set up a permutation generator for the passed list. */
    public Perms(ArrayList list)
    {
        pg = new PermGen(list.size());

        // Since we're not going to mess with the elements, just
        // their order, a shallow copy should be fine.
        permList = (ArrayList)list.clone();
    }

    /** Returns an iterator that returns permutations of the originally
     *  passed list.  The first permutation is the unmodified list. */
    public Iterator iterator()
    {
        return new Iterator()
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

            public Object next()
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
                Object temp = permList.get(lower);
                permList.set(lower, permList.get(lower + 1));
                permList.set(lower + 1, temp);
            }
        };
    }


    /**
      * This class is a permutation generator. The permutations are generated
      * using Dershowitz's method, meaning that a permutation only differs
      * by the previous permutation by a single interchange of two adjacent
      * elements. In many problem domains this allows a efficient dynamic
      * update of a permutation function.
      * @author Peter Unold
      * @see "Dershowitz, Nachum. "A simplified loop - free algorithm for generating permutations" BIT - 15 1975 158 - 164"
      * @see <a href="http://www.daimi.aau.dk/~pjunold/alg/dershowitz.html">Dershowitz's Permutation Generator</a>
     */
    class PermGen
    {
        int[]    m_p; // permutation
        int[]    m_l; // location
        int[]    m_t; // linked list
        int[]    m_d; // direction
        int      m_size;

        PermGen(int size)
        {
            m_size = size;
            m_p = new int[size];
            m_l = new int[size];
            m_t = new int[size + 1];
            m_d = new int[size];

            for (int i = 0; i < size; ++i)
            {
                m_p[i] = i;
                m_l[i] = i;
                m_d[i] = -1;
            }

            for (int j = 1; j <= size; ++j)
            {
                m_t[j] = j - 1;
            }
        }

        /** generates the next permutation. If the function returns n, then
            the elements at position n and n + 1 in the previous permutation
            were interchanged to get the new permutation.
            @return the index of the lower element which was interchanged or
            - 1 if the last permutation has been reached.  */
        public int getNext()
        {
            int cur, neig, curpos, neigpos, neigpos2;

            // 3
            if (m_t[m_size] < 1)
            {
                return -1;
            }

            // 4
            cur = m_t[m_size];
            curpos = m_l[cur];
            neigpos = curpos + m_d[cur];
            neig = m_p[neigpos];

            // 5
            m_l[cur] = neigpos;
            m_l[neig] = curpos;
            m_p[curpos] = neig;
            m_p[neigpos] = cur;

            // 6
            m_t[m_size] = m_size - 1;

            // 7
            neigpos2 = neigpos + m_d[cur];

            if (neigpos2 < 0 || neigpos2 >= m_size || cur < m_p[neigpos2])
            {
                m_d[cur] = -m_d[cur];
                m_t[cur + 1] = m_t[cur];
                m_t[cur] = cur - 1;
            }
            return (curpos < neigpos) ? curpos : neigpos;
        }


        /** get the current permutation */
        public int[] getCurrent()
        {
            return m_p;
        }

        // TODO JUnit
        /** Unit test for PermGen. */
        public void main(String [] args)
        {
            if (args.length == 0)
            {
                System.out.println("Need to provide a size.");
                return;
            }
            int size = Integer.parseInt(args[0]);
            int [] perm = new int[size];
            int count = 0;
            PermGen pg = new PermGen(size);
            do
            {
                perm = pg.getCurrent();
                System.out.print(++count + " : ");
                for (int i = 0; i < size; i++)
                {
                    System.out.print(perm[i]);
                }
                System.out.println();
            }
            while (pg.getNext() != -1);
        }
    }
}
