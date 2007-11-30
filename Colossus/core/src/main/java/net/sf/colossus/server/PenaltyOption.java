package net.sf.colossus.server;


import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Holds the information for one possible strike penalty, including
 * the null no-penalty option.
 * @version $Id$
 * @author David Ripton
 */
final class PenaltyOption implements Comparable
{
	private static final Logger LOGGER = Logger.getLogger(PenaltyOption.class.getName());

    private Critter striker;
    private Critter target;
    private Set carryTargets = new HashSet();    // of hexLabels
    private int dice;
    private int strikeNumber;


    PenaltyOption(Critter striker, Critter target, int dice, int strikeNumber)
    {
        this.striker = striker;
        this.target = target;
        this.dice = dice;
        this.strikeNumber = strikeNumber;
        if (striker == target)
        {
            LOGGER.log(Level.SEVERE, "Penalty option with striker and target identical!", (Throwable)null);
        }
    }


    Critter getStriker()
    {
        return striker;
    }

    Critter getTarget()
    {
        return target;
    }

    int getDice()
    {
        return dice;
    }

    int getStrikeNumber()
    {
        return strikeNumber;
    }

    /** Add a hexLabel String to carry targets list. */
    void addCarryTarget(String carryTarget)
    {
        carryTargets.add(carryTarget);
    }

    /** Add all hexLabel Strings in Set to carry targets list. */
    void addCarryTargets(Set targets)
    {
        carryTargets.addAll(targets);
    }

    Set getCarryTargets()
    {
        return Collections.unmodifiableSet(carryTargets);
    }

    int numCarryTargets()
    {
        return carryTargets.size();
    }


    /** Sort first by ascending dice, then by descending strike number,
     *  then by striker and target.  Do not consider carryTargets. */
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

        if (dice < other.dice)
        {
            return -1;
        }
        else if (dice > other.dice)
        {
            return 1;
        }
        else if (strikeNumber > other.strikeNumber)
        {
            return -1;
        }
        else if (strikeNumber < other.strikeNumber)
        {
            return 1;
        }
        else if (striker.compareTo(other.striker) != 0)
        {
            return striker.compareTo(other.striker);
        }
        else
        {
            return target.compareTo(other.target);
        }
    }

    /** Do not consider carryTargets. */
    public boolean equals(Object object)
    {
        if (!(object instanceof PenaltyOption))
        {
            return false;
        }
        PenaltyOption other = (PenaltyOption)object;
        return (dice == other.dice && strikeNumber == other.strikeNumber &&
            striker.equals(other.striker) && target.equals(other.target));
    }

    /** Do not consider carryTargets. */
    public int hashCode()
    {
        return dice + 100 * strikeNumber + striker.hashCode();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(striker.getDescription());
        sb.append(" strikes ");
        sb.append(target.getDescription());
        sb.append(" with ");
        sb.append(dice);
        sb.append(" dice and strike number ");
        sb.append(strikeNumber);
        if (!carryTargets.isEmpty())
        {
            sb.append(", able to carry to "); 
            Iterator it = carryTargets.iterator();
            boolean first = true;
            while (it.hasNext())
            {
                if (!first)
                {
                    sb.append(", ");
                }
                first = false;
                String hexLabel = (String)it.next();
                Critter critter = striker.getBattle().getCritter(hexLabel);
                sb.append(critter.getDescription());
            }
        }
        return sb.toString();
    }
}
