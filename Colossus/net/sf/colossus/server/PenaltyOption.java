package net.sf.colossus.server;


/**
 * Class PenaltyOption holds the information needed to decide whether to take
 * a strike penalty, for one chit.
 * @version $Id$
 * @author David Ripton
 */

public final class PenaltyOption implements Comparable
{
    private Critter critter;
    private int dice;
    private int strikeNumber;


    public PenaltyOption(Critter critter, int dice, int strikeNumber)
    {
        this.critter = critter;
        this.dice = dice;
        this.strikeNumber = strikeNumber;
    }


    public Critter getCritter()
    {
        return critter;
    }


    public int getDice()
    {
        return dice;
    }


    public int getStrikeNumber()
    {
        return strikeNumber;
    }


    /** Sort first by ascending dice, then by descending strike number.
        This is inconsistent with equals(). */
    public int compareTo(Object object)
    {
        PenaltyOption other;
        if (object instanceof PenaltyOption)
        {
            other = (PenaltyOption)object;
        }
        else
        {
            throw new ClassCastException();
        }

        if (dice < other.getDice())
        {
            return -1;
        }
        else if (dice > other.getDice())
        {
            return 1;
        }
        else if (strikeNumber > other.getStrikeNumber())
        {
            return -1;
        }
        else if (strikeNumber < other.getStrikeNumber())
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }
}
