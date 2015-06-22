package net.sf.colossus.ai.helper;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import net.sf.colossus.ai.AbstractAI;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.common.Options;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.BattleHex;


/**
 * On-the-fly generation of the Collection of all possible LegionMove.
 * This doesn't fully respect the Collection interface:
 * The random generation may fail before all elements have been returned,
 * so to iterators may return different subsets of the entire collection.
 * Also, this may cause size() to return a value higher than what is really
 * accessible.
 *
 * @author Romain Dolbeau
 */
@SuppressWarnings("boxing")
public class OnTheFlyLegionMove implements Collection<LegionMove>
{

    /** Maximum number of try before giving up generating a new element.
     * Ideally this is only a safety belt.
     */
    final static private int RANDOM_MAX_TRY = 50;
    /** number of elements to put in each new batch of element.
     * From my experiments, should be about 1-3 second worth of evaluation.
     */
    final static private int REFILL_SIZE = 2000;
    /* genetic stuff */
    /** Percentage that a gene will be random instead of inherited.
     * Low will densify exploration around the current maximums.
     * High will widen the explorated space around the current maximums.
     */
    final static private int RANDOM_GENE_PERCENT = 10;
    /** Percentage of a randomly chosen parent.
     * Another parameter to avoid inbreeding and missing not-yet-detected
     * local maximums.
     */
    final static private int RANDOM_PARENT_PERCENT = 10;
    /** Percentage from the top (of the already avaluated space) to pick
     * a 'good' parent.
     * Low will pick parent only from very near the local maximums.
     * High will give not-so-good parents a chance.to breed.
     */
    final static private int GOOD_PARENT_TOP_PERCENT = 20;
    /** Minimum number of possible 'good' parents.
     * For small exploration space, this avoid excessive inbreeding.
     */
    final static private int MIN_PARENT_CHOICE = 50;
    /** Percentage of fully random new elements
     * This helps diversifying the gene pool.
     */
    final static private int SPONTANEOUS_GENERATION_PERCENT = 5;
    /** Amount of memory needed before a refill.
     * This avoid crashing low-mem JVM.
     * The constant part is of the pulled-out-of-a-hat variety.
     */
    final static private long MIN_MEMORY_REFILL = 10 * 1024 * REFILL_SIZE;
    private static final Logger LOGGER = Logger
        .getLogger(OnTheFlyLegionMove.class.getName());
    private final List<List<CritterMove>> allCritterMoves;
    private final int mysize;

    public OnTheFlyLegionMove(final List<List<CritterMove>> acm)
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
        LOGGER.finest("OnTheFlyLegionMove created for " + realcount
            + " combinations"
            + (mysize != realcount ? " limited to " + mysize : ""));
    }

    int getDim()
    {
        return allCritterMoves.size();
    }

    class OnTheFlyLegionMoveIterator implements Iterator<LegionMove>
    {

        String intArrayToString(int[] t)
        {
            StringBuffer buf = new StringBuffer();
            buf.append(t[0]);
            for (int i = 1; i < t.length; i++)
            {
                buf.append(" " + t[i]);
            }
            return buf.toString();
        }

        class myIntArrayComparator implements Comparator<int[]>
        {

            long baseXvalue(int[] t)
            {
                long temp = 0;
                long factor = 1;
                /* interpret the array as a base-X number */
                /* we have 27 hexes and change, this should fit */
                for (int i = 0; i < t.length; i++)
                {
                    temp += t[i] * factor;
                    factor *= daddy.allCritterMoves.get(i).size();
                }
                return temp;
            }

            int[] roundNextUp(int[] t, int factor)
            {
                int[] temp = new int[t.length];
                for (int i = 0; i < t.length; i++)
                {
                    if (i < factor)
                    {
                        temp[i] = 0;
                    }
                    else
                    {
                        temp[i] = t[i];
                    }
                }
                return nextValue(temp, factor);
            }

            int[] nextValue(int[] t)
            {
                return nextValue(t, 0);
            }

            int[] nextValue(int[] t, int factor)
            {
                int[] temp = new int[t.length];
                for (int i = 0; i < t.length; i++)
                {
                    temp[i] = t[i];
                }
                int j = factor;
                boolean ok = false;
                while (!ok && j < t.length)
                {
                    int size = daddy.allCritterMoves.get(j).size();
                    temp[j]++;
                    if (temp[j] < size)
                    {
                        ok = true;
                    }
                    else
                    {
                        temp[j] -= size;
                        j++;
                    }
                }
                if (j == t.length)
                {
                    return null;
                }
                return temp;
            }

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
                long temp = baseXvalue(t1) - baseXvalue(t2);
                if (temp > Integer.MAX_VALUE)
                {
                    temp = Integer.MAX_VALUE;
                }
                if (temp < Integer.MIN_VALUE)
                {
                    temp = Integer.MIN_VALUE;
                }
                return (int)temp;
            }
        }

        class myIntArrayLegionValueComparator extends myIntArrayComparator
        {

            @Override
            public int compare(int[] t1, int[] t2)
            {
                int v1 = alreadydone.get(t1).getValue();
                int v2 = alreadydone.get(t2).getValue();
                if (v1 > v2)
                {
                    return 1;
                }
                if (v2 > v1)
                {
                    return -1;
                }
                return super.compare(t1, t2);
            }
        }

        /** map from indexes to LegionMove, what we have already sent to the AI */
        private final SortedMap<int[], LegionMove> alreadydone = new TreeMap<int[], LegionMove>(
            new myIntArrayComparator());
        /** already done & evaluated, sorted by legion value */
        private final List<int[]> byValues = new ArrayList<int[]>();
        private final myIntArrayLegionValueComparator byValuesComparator = new myIntArrayLegionValueComparator();
        /** the previously returned object */
        private int[] lastone = null;
        /** map from indexes to LegionMove, the next batch to send to the AI */
        private final SortedMap<int[], LegionMove> beingdone = new TreeMap<int[], LegionMove>(
            new myIntArrayComparator());
        private final OnTheFlyLegionMove daddy;
        private final Random rand = new DevRandom();
        private final int dim;
        private boolean abort = false;
        private boolean failoverOnly = false;
        /** The 'incompatibility map'.
         * first index is which position in the source int[] is checked.
         * second index is which position in the destination source int[] is checked.
         * third index is value in the source int[].
         * The Set is all the incompatible values in the dest int[].
         */
        private final Set<Integer>[][][] incomps;

        @SuppressWarnings("unchecked")
        OnTheFlyLegionMoveIterator(OnTheFlyLegionMove d)
        {
            daddy = d;
            dim = daddy.getDim();
            incomps = new Set[dim][dim][30];//never more than 30 hexes ???
            buildIncompMap();
            firstfill();
        }

        private void buildIncompMap()
        {
            LOGGER.finest("BuildIncompMap started");
            for (int i = 0; i < dim; i++)
            {
                List<CritterMove> li = daddy.allCritterMoves.get(i);
                for (int j = 0; j < dim; j++)
                {
                    if (i != j)
                    {
                        List<CritterMove> lj = daddy.allCritterMoves.get(j);
                        for (int k = 0; k < li.size(); k++)
                        {
                            BattleHex a = li.get(k).getEndingHex();
                            Set<Integer> s = new TreeSet<Integer>();
                            incomps[i][j][k] = s;

                            if (!a.isEntrance())
                            {
                                for (int l = 0; l < lj.size(); l++)
                                {
                                    BattleHex b = lj.get(l).getEndingHex();
                                    if (a.equals(b))
                                    {
                                        s.add(l);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            LOGGER.finest("BuildIncompMap done");
        }

        public boolean hasNext()
        {
            if (lastone != null)
            { // the previously returned value has been evaluated now
              // so we can put it in the byvalues set.
                byValues.add(lastone);
                lastone = null;
            }
            if (beingdone.isEmpty() && !abort)
            {
                int real_refill_size = REFILL_SIZE;
                long freemem = Runtime.getRuntime().freeMemory();
                LOGGER.finest("Memory available (before GC) = " + freemem
                    + " bytes (" + freemem / (1024 * 1024) + " MiB)");
                Runtime.getRuntime().gc();
                freemem = Runtime.getRuntime().freeMemory();
                LOGGER.finest("Memory available (after GC) = " + freemem
                    + " bytes (" + freemem / (1024 * 1024) + " MiB)");
                /* Don't refill is there's not much memory left. What will
                 * already have will do
                 */
                if (freemem < (MIN_MEMORY_REFILL))
                {
                    // Yeah, I know about that memory issue, and really don't
                    // want them to spoil my stresstest statistics...
                    if (!Options.isStresstest())
                    {
                        LOGGER.warning("Memory is still low (" + freemem
                            + " bytes), no more refill.");
                    }
                }
                else
                {
                    /* If we're a bit short on memory, do smaller refill
                     * so that we don't overuse the memory.
                     * Also, this avoids spending too much time in the GC
                     * next time.
                     */
                    if (freemem < (2 * MIN_MEMORY_REFILL))
                    {
                        real_refill_size /= 6;
                    }
                    else if (freemem < (3 * MIN_MEMORY_REFILL))
                    {
                        real_refill_size /= 4;
                    }
                    else if (freemem < (4 * MIN_MEMORY_REFILL))
                    {
                        real_refill_size /= 3;
                    }
                    else if (freemem < (6 * MIN_MEMORY_REFILL))
                    {
                        real_refill_size /= 2;
                    }
                    refill(real_refill_size);
                }
            }
            return (!beingdone.isEmpty());
        }

        private int higherRankIncomp(int[] indexes)
        {
            /* not i >= 0, because we return 0 anyway, so why waste time
             * checking ?*/
            for (int i = dim - 1; i > 0; i--)
            {
                for (int j = dim - 1; j > i; j--)
                {
                    Set<Integer> inc = incomps[i][j][indexes[i]];
                    if (inc.contains(indexes[j]))
                    {
                        return i;
                    }
                }
            }
            return 0;
        }

        private boolean isBad(int[] indexes)
        {
            boolean isBad = false;
            for (int i = 0; i < dim && !isBad; i++)
            {
                for (int k = 0; k < i && !isBad; k++)
                {
                    Set<Integer> inc = incomps[i][k][indexes[i]];
                    if (inc.contains(indexes[k]))
                    {
                        isBad = true;
                    }
                }
            }
            return isBad;
        }

        /** full recursive generation */
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
                actual[i] = 0;
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
            if (count <= 0)
            {
                LOGGER
                    .warning("Firstfill generated 0 combinations. This is bad.");
            }
            return count;
        }

        /** deterministically make up a on-used combination */
        private int[] lastDense = null;

        private int[] failoverGeneration()
        {
            //LOGGER.finest("failoverGeneration start");
            // int count = 0;
            int[] temp = new int[dim];
            if (lastDense == null)
            {
                lastDense = new int[dim];
            }
            for (int i = 0; i < dim; i++)
            {
                temp[i] = lastDense[i];
            }
            myIntArrayComparator comp = new myIntArrayComparator();
            while (temp != null
                && (alreadydone.keySet().contains(temp)
                    || beingdone.keySet().contains(temp) || isBad(temp)))
            {
                /* check if some of the higher index positions are
                 * incompatible. If yes, try to break the higher index
                 * incompatibility.
                 * In practice the then branch could be folded into the
                 * else branch, as they do the same thing for hr == 0.
                 */
                // count++;
                int hr = higherRankIncomp(temp);
                if (hr == 0)
                {
                    temp = comp.nextValue(temp);
                }
                else
                {
                    temp = comp.roundNextUp(temp, hr);
                }
            }

            if (temp != null)
            {
                for (int i = 0; i < dim; i++)
                {
                    lastDense[i] = temp[i];
                }
            }
            //LOGGER.finest("failoverGeneration done after " + count + " attempts.");
            return temp;
        }

        /** create a fully random combination */
        private int[] spontaneousGeneration()
        {
            int[] child = new int[dim];
            for (int i = 0; i < dim; i++)
            {
                child[i] = rand.nextInt(daddy.allCritterMoves.get(i).size());
            }
            return child;
        }

        /** create a genetic combination */
        private int[] geneticGeneration()
        {
            int[] mom = getParent(RANDOM_PARENT_PERCENT,
                GOOD_PARENT_TOP_PERCENT);
            int[] dad = getParent(RANDOM_PARENT_PERCENT,
                GOOD_PARENT_TOP_PERCENT);

            if ((mom == null) || (dad == null))
            {
                return null;
            }

            int[] child = breed(mom, dad, RANDOM_GENE_PERCENT);

            return child;
        }

        /** pick a parent */
        private int[] getParent(int percentRandom, int percentTop)
        {
            int[] parent;
            int length = byValues.size();
            if (length <= 0)
            {
                LOGGER
                    .warning("getParent called but byValues has no element.");

                System.err.println("Dumping....");
                Thread.dumpStack();
                for (int i = 0; i < daddy.allCritterMoves.size(); i++)
                {
                    System.err.println("# of moves @ " + i + " = "
                        + daddy.allCritterMoves.get(i).size());
                    for (int j = 0; j < daddy.allCritterMoves.get(i).size(); j++)
                    {
                        System.err.println("Move for " + i + "/" + j + " is "
                            + daddy.allCritterMoves.get(i).get(j).toString());
                    }
                }

                ErrorUtils
                    .showErrorDialog(
                        null,
                        "Experimenal AI data inconsistency!",
                        "During AI OnTheFlyLegionMove calculation, encountered a "
                            + "'getParent called but byValues has no element' "
                            + "situation. Can't continue - application will exit!");
                System.exit(-50);
            }
            if (rand.nextInt(100) < percentRandom)
            {
                parent = byValues.get(rand.nextInt(length));
            }
            else
            {
                int nChoice = length / 100 * percentTop;
                if (nChoice < MIN_PARENT_CHOICE)
                {
                    nChoice = MIN_PARENT_CHOICE;
                }
                if (nChoice > length)
                {
                    nChoice = length;
                }
                parent = byValues.get(((length - nChoice) + rand
                    .nextInt(nChoice)));
            }
            return parent;
        }

        /** breed a combination from parents */
        private int[] breed(int[] mom, int[] dad, int percentRandom)
        {
            if (dim != dad.length)
            {
                return null;
            }
            if (dim != mom.length)
            {
                return null;
            }
            int[] child = new int[dim];

            for (int i = 0; i < dim; i++)
            {
                if (rand.nextInt(100) < percentRandom)
                {
                    child[i] = rand.nextInt(daddy.allCritterMoves.get(i)
                        .size());
                }
                else
                {
                    if (rand.nextInt(100) < 50)
                    {
                        child[i] = mom[i];
                    }
                    else
                    {
                        child[i] = dad[i];
                    }
                }
            }

            return child;
        }

        /** fill beingdone with up to n genetically generated, not-yet-done
         * combinations.
         * @param n The number of requeste combinations.
         * @return The number of combinations generated.
         */
        private int refill(int n)
        {
            if (beingdone.size() > 0)
            {
                return 0;
            }
            int ngenetic = 0;
            int nfailover = 0;
            Collections.sort(byValues, byValuesComparator);
            LOGGER.finest("Refill started ; current best score is "
                + ((byValues.size() > 0) ? ""
                    + alreadydone.get(byValues.get(byValues.size() - 1))
                        .getValue() : "(empty!!!!)"));
            /* we have n elements to make */
            for (int k = 0; (k < n) && !abort; k++)
            {
                int[] indexes;
                LegionMove current = null;
                /* after too many failover, do only failovers */
                if (failoverOnly)
                {
                    indexes = failoverGeneration();
                    if (indexes != null)
                    {
                        current = AbstractAI.makeLegionMove(indexes,
                            daddy.allCritterMoves);
                        beingdone.put(indexes, current);
                        nfailover++;
                    }
                    else
                    {
                        LOGGER.finest("Even failover didn't produce a result");
                        abort = true;
                    }
                }
                else
                {
                    int ntry = 0;
                    /* make at most ntry at generating current, using genetic */
                    while ((current == null) && (ntry < RANDOM_MAX_TRY)
                        && (!abort))
                    {
                        boolean genetic;
                        ntry++;
                        if (rand.nextInt(100) < SPONTANEOUS_GENERATION_PERCENT)
                        {
                            indexes = spontaneousGeneration();
                            genetic = false;
                        }
                        else
                        {
                            indexes = geneticGeneration();
                            genetic = true;
                        }
                        if ((indexes != null) && !isBad(indexes))
                        {
                            if (!beingdone.keySet().contains(indexes)
                                && !alreadydone.keySet().contains(indexes))
                            {
                                current = AbstractAI.makeLegionMove(indexes,
                                    daddy.allCritterMoves);
                                beingdone.put(indexes, current);
                            }
                        }
                        if (current != null)
                        {
                            /*
                            LOGGER.finest("Try " + ntry + " for move #" + k +
                            " found something (" +
                            (genetic ? "genetic" : "random") + ")");
                             * */
                            if (genetic)
                            {
                                ngenetic++;
                            }
                        }
                        else
                        {
                            /* if all else fail, try failover */
                            if (ntry == RANDOM_MAX_TRY)
                            {
                                /*
                                LOGGER.finest("Try " + ntry + " for move #" + k +
                                " still hasn't found anything.");
                                 */
                                indexes = failoverGeneration();
                                if (indexes != null)
                                {
                                    current = AbstractAI.makeLegionMove(
                                        indexes, daddy.allCritterMoves);
                                    beingdone.put(indexes, current);
                                    nfailover++;
                                    if (nfailover > n / 10)
                                    {
                                        failoverOnly = true;
                                    }
                                }
                                else
                                {
                                    LOGGER
                                        .finest("Even failover didn't produce a result");
                                    abort = true;
                                }
                            }
                        }
                    }
                }
            }
            int count = beingdone.keySet().size();
            LOGGER.finer("Refill generated " + count + " out of " + n
                + " requested ; " + ngenetic + " genetic ("
                + ((100. * ngenetic) / count) + " %) ; " + nfailover
                + " sequential (" + ((100. * nfailover) / count) + " %)");
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
            lastone = anext;
            return lmnext;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void finalize() throws Throwable
        {
            if (lastone != null)
            { // the previously returned value has been evaluated now
              // so we can put it in the byvalues set.
                byValues.add(lastone);
                lastone = null;
            }
            LOGGER.finest("From our " + mysize + " combinations, "
                + byValues.size() + " we evaluated ("
                + ((100. * byValues.size()) / mysize) + " %)");
            super.finalize();
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
