package net.sf.colossus.ai;


import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.sf.colossus.client.CritterMove;
import net.sf.colossus.util.DevRandom;


/**
 * On-the-fly generation of the Collection of all possible LegionMove.
 * This doesn't full respect the Collection interface:
 * The random generation may fail before all alements have been returned,
 * so to iterators may return different subsets of the entire collection.
 * Also, this may cause size() to return a value higher than what is really
 * accessible.
 * @version $Id$
 * @author Romain Dolbeau
 */
class OnTheFlyLegionMove implements Collection<LegionMove>
{

    final static private int RANDOM_MAX_TRY = 100;
    final static private int REFILL_SIZE = 2000;
    private static final Logger LOGGER = Logger
        .getLogger(OnTheFlyLegionMove.class.getName());
    private final List<List<CritterMove>> allCritterMoves;
    private final int mysize;

    OnTheFlyLegionMove(final List<List<CritterMove>> acm)
    {
        allCritterMoves = acm;

        long realcount = 1;
        for (List<CritterMove> lcm : allCritterMoves)
        {
            realcount *= lcm.size();
        }
        if (realcount > Integer.MAX_VALUE)
        {
            mysize = Integer.MAX_VALUE;
        }
        else
        {
            mysize = (int)realcount;
        }
    }

    int getDim()
    {
        return allCritterMoves.size();
    }

    class OnTheFlyLegionMoveIterator implements Iterator<LegionMove>
    {

        class myIntArrayComparator implements Comparator<int[]>
        {

            public int compare(int[] t1, int[] t2)
            {
                if (t1.length > t2.length)
                {
                    return 1;
                }
                if (t1.length < t2.length)
                {
                    return -1;
                }
                for (int i = 0; i < t1.length; i++)
                {
                    if (t1[i] > t2[i])
                    {
                        return 1;
                    }
                    if (t1[i] < t2[i])
                    {
                        return -1;
                    }
                }
                return 0;
            }
        }

        /** map from indexes to LegionMove, what we have already sent to the AI */
        private final TreeMap<int[], LegionMove> alreadydone = new TreeMap<int[], LegionMove>(
            new myIntArrayComparator());
        /** map from indexes to LegionMove, the next batch to send to the AI */
        private final TreeMap<int[], LegionMove> beingdone = new TreeMap<int[], LegionMove>(
            new myIntArrayComparator());
        private final OnTheFlyLegionMove daddy;
        private final Random rand = new DevRandom();
        private final int dim;

        OnTheFlyLegionMoveIterator(OnTheFlyLegionMove d)
        {
            daddy = d;
            dim = daddy.getDim();
            firstfill();
        }

        public boolean hasNext()
        {
            if (beingdone.isEmpty())
            {
                refill(REFILL_SIZE);
            }
            return (!beingdone.isEmpty());
        }

        @SuppressWarnings("unused")
        private boolean isBad(int[] indexes)
        {
            Set<String> duplicateHexChecker = new HashSet<String>();
            boolean offboard = false;
            boolean isBad = false;
            for (int j = 0; j < dim; j++)
            {
                List<CritterMove> moveList = daddy.allCritterMoves.get(j);
                CritterMove cm = moveList.get(indexes[j]);
                String endingHexLabel = cm.getEndingHexLabel();
                if (endingHexLabel.startsWith("X"))
                {
                    offboard = true;
                }
                else if (duplicateHexChecker.contains(endingHexLabel))
                {
                    isBad = true;
                }
                duplicateHexChecker.add(cm.getEndingHexLabel());
            }
            return isBad;
        }

        private int recurseGenerate(int index, int[] counts, int[] actual)
        {
            int total = 0;
            if (index < dim)
            {
                for (int i = 0; i < counts[index]; i++)
                {
                    actual[index] = i;
                    total += recurseGenerate(index + 1, counts, actual);
                }
            }
            else
            {
                total = 1;
                int[] indexes = new int[dim];
                for (int i = 0; i < dim; i++)
                {
                    indexes[i] = actual[i];
                }
                if (!isBad(indexes))
                {
                    if (!beingdone.keySet().contains(indexes))
                    {
                        LegionMove current = AbstractAI.makeLegionMove(
                            indexes, daddy.allCritterMoves);
                        beingdone.put(indexes, current);
                        //LOGGER.finest("Generated a good one.");
                    }
                }
                else
                {
                    //LOGGER.finest("Tested combination was bad.");
                }
            }
            return total;
        }

        /** fill beingdone with the first, supposedly most interesting
         * combinatione.
         * @return The number of combinations generated.
         */
        private int firstfill()
        {
            int[] counts = new int[dim];
            int[] actual = new int[dim];
            int total = 0;
            for (int i = 0; i < dim; i++)
            {
                counts[i] = i + 2;
                if (counts[i] > daddy.allCritterMoves.get(i).size())
                {
                    counts[i] = daddy.allCritterMoves.get(i).size();
                }
            }
            total = recurseGenerate(0, counts, actual);

            int count = beingdone.keySet().size();
            LOGGER.finer("Firstfill generated " + count + " out of " + total
                + " checked");
            return count;
        }

        /** fill beingdone with up to n random, not-yet-done combinations.
         * Should be replaced by a genetic algorithm, ideally.
         * @param n The number of requeste combinations.
         * @return The number of combinations generated.
         */
        private int refill(int n)
        {
            if (beingdone.size() > 0)
            {
                return 0;
            }
            for (int k = 0; k < n; k++)
            {
                int[] indexes = new int[dim];
                int ntry = 0;
                LegionMove current = null;
                while ((current == null) && (ntry < RANDOM_MAX_TRY))
                {
                    ntry++;
                    for (int i = 0; i < dim; i++)
                    {
                        indexes[i] = rand.nextInt(daddy.allCritterMoves.get(i)
                            .size());
                    }
                    if (!isBad(indexes))
                    {
                        if (!beingdone.keySet().contains(indexes))
                        {
                            current = AbstractAI.makeLegionMove(indexes,
                                daddy.allCritterMoves);
                            beingdone.put(indexes, current);
                        }
                    }
                    if (current != null)
                    {
                        //LOGGER.finest("Try " + ntry + " for move #" + k + " found something.");
                    }
                    else
                    {
                        if (ntry == RANDOM_MAX_TRY)
                        {
                            //LOGGER.finest("Try " + ntry + " for move #" + k + " STILL hasn't found anything.");
                        }
                    }
                }
            }
            int count = beingdone.keySet().size();
            LOGGER.finer("Refill generated " + count + " out of " + n
                + " requested");
            return count;
        }

        public LegionMove next()
        {
            if (beingdone.isEmpty())
            {
                LOGGER.warning("next() call but beingdone is empty!");
                return null;
            }
            int[] anext = beingdone.firstKey();
            LegionMove lmnext = beingdone.get(anext);
            beingdone.remove(anext);
            alreadydone.put(anext, lmnext);
            return lmnext;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    public boolean add(LegionMove o)
    {
        //Ensures that this collection contains the specified element (optional operation).
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends LegionMove> c)
    {
        //Adds all of the elements in the specified collection to this collection (optional operation).
        throw new UnsupportedOperationException();
    }

    public void clear()
    {
        //Removes all of the elements from this collection (optional operation).
        throw new UnsupportedOperationException();
    }

    public boolean contains(Object o)
    {
        // Returns true if this collection contains the specified element.
        LOGGER.warning(" should be implemented ...");
        return false;
    }

    public boolean containsAll(Collection<?> c)
    {
        //Returns true if this collection contains all of the elements in the specified collection.
        LOGGER.warning(" should be implemented ...");
        return false;
    }

    @Override
    public boolean equals(Object o)
    {
        //Compares the specified object with this collection for equality.
        LOGGER.warning(" should be implemented ...");
        return false;
    }

    @Override
    public int hashCode()
    {
        //Returns the hash code value for this collection.
        return super.hashCode();
    }

    public boolean isEmpty()
    {
        //Returns true if this collection contains no elements.
        return allCritterMoves.isEmpty();
    }

    public Iterator<LegionMove> iterator()
    {
        //Returns an iterator over the elements in this collection.
        return new OnTheFlyLegionMoveIterator(this);
    }

    public boolean remove(Object o)
    {
        // Removes a single instance of the specified element from this collection, if it is present (optional operation).
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c)
    {
        // Removes all this collection's elements that are also contained in the specified collection (optional operation).
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c)
    {
        //Retains only the elements in this collection that are contained in the specified collection (optional operation).
        throw new UnsupportedOperationException();
    }

    public int size()
    {
        //Returns the number of elements in this collection.
        return mysize;
    }

    public Object[] toArray()
    {
        //Returns an array containing all of the elements in this collection.
        LOGGER.warning(" should be implemented ...");
        return null;
    }

    public <T> T[] toArray(T[] a)
    {
        //Returns an array containing all of the elements in this collection; the runtime type of the returned array is that of the specified array.
        LOGGER.warning(" should be implemented ...");
        return null;
    }
}
