package net.sf.colossus.webserver;


import java.io.PrintWriter;
import java.util.LinkedList;


public class RoundtripTimeBookkeeper
{
    // private static final Logger LOGGER = Logger
    //    .getLogger(RoundtripTimeBookkeeper.class.getName());

    private long indexCounter = 0;

    private final int LAST_N_TO_KEEP;

    LinkedList<RoundtripTimeEntry> lastNTimes = new LinkedList<RoundtripTimeEntry>();

    public RoundtripTimeBookkeeper(int howManyLastToKeep)
    {
        this.LAST_N_TO_KEEP = howManyLastToKeep;
    }

    /**
     * Create and store one RTT entry
     * @param requestResponseArriveTime When response arrived
     * @param roundtripTime Actual roundtrip time
     **/
    public void storeEntry(long when, long roundtripTime)
    {
        RoundtripTimeEntry entry = new RoundtripTimeEntry(indexCounter++,
            when, roundtripTime);
        lastNTimes.add(entry);
        if (lastNTimes.size() > this.LAST_N_TO_KEEP)
        {
            lastNTimes.removeFirst();
        }

        if (indexCounter % 5 == 0)
        {
            // TODO: print to file instead
            // showLastNEntries(new PrintWriter(System.out));
        }
    }

    @SuppressWarnings("boxing")
    public void showLastNEntries(PrintWriter pw)
    {
        pw.println("\nLast N Roundtrip time entries:\n");
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        long whenForMin = -1;
        long indexForMin = -1;

        long whenForMax = -1;
        long indexForMax = -1;

        for (RoundtripTimeEntry e : lastNTimes)
        {
            long index = e.getIndex();
            long when = e.getWhen();
            long time = e.getRTTime();

            if (time > max)
            {
                max = time;
                whenForMax = when;
                indexForMax = index;
            }

            if (time < min)
            {
                min = time;
                whenForMin = when;
                indexForMin = index;
            }

            pw.printf("i=%6d: At %20d RT-time=%10d\n", index, when, time);
        }

        pw.println("");
        pw.printf("MIN     : At %20d RT-time=%10d (i=%6d)\n", whenForMin, min,
            indexForMin);
        pw.printf("MAX     : At %20d RT-time=%10d (i=%6d)\n", whenForMax, max,
            indexForMax);

    }

    private class RoundtripTimeEntry
    {
        private final long index;
        private final long when;
        private final long rtTime;

        private RoundtripTimeEntry(long index, long when, long rtTime)
        {
            this.index = index;
            this.when = when;
            this.rtTime = rtTime;
        }

        public long getIndex()
        {
            return index;
        }

        public long getWhen()
        {
            return when;
        }

        public long getRTTime()
        {
            return rtTime;
        }
    }

}
