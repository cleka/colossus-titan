package net.sf.colossus.server;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Game;
import net.sf.colossus.variant.BattleHex;


/**
 * Holds the information for one possible strike penalty, including
 * the null no-penalty option.
 *
 * @author David Ripton
 */
final class PenaltyOption implements Comparable<PenaltyOption>
{
    private static final Logger LOGGER = Logger.getLogger(PenaltyOption.class
        .getName());

    private final Game game;
    // Use BattleCritter instead?
    private final Creature striker;
    private final Creature target;
    private final Set<BattleHex> carryTargets = new HashSet<BattleHex>();
    private final int dice;
    private final int strikeNumber;

    PenaltyOption(Game game, Creature striker, Creature target, int dice,
        int strikeNumber)
    {
        this.game = game;
        this.striker = striker;
        this.target = target;
        this.dice = dice;
        this.strikeNumber = strikeNumber;
        if (striker == target)
        {
            LOGGER.log(Level.SEVERE,
                "Penalty option with striker and target identical!");
        }
    }

    Creature getStriker()
    {
        return striker;
    }

    Creature getTarget()
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

    // TODO use critters instead of hexes?
    // Then we would not even need game as instance variable here
    void addCarryTarget(BattleHex carryTarget)
    {
        carryTargets.add(carryTarget);
    }

    // TODO use critters instead of hexes?
    void addCarryTargets(Set<BattleHex> targets)
    {
        carryTargets.addAll(targets);
    }

    Set<BattleHex> getCarryTargets()
    {
        return Collections.unmodifiableSet(carryTargets);
    }

    int numCarryTargets()
    {
        return carryTargets.size();
    }

    /** Sort first by ascending dice, then by descending strike number,
     *  then by striker and target.  Do not consider carryTargets. */
    public int compareTo(PenaltyOption other)
    {
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
        else if (Creature.IMPORTANCE_ORDER.compare(striker, other.striker) != 0)
        {
            return Creature.IMPORTANCE_ORDER.compare(striker, other.striker);
        }
        else
        {
            return Creature.IMPORTANCE_ORDER.compare(target, other.target);
        }
    }

    /** Do not consider carryTargets. */
    @Override
    public boolean equals(Object object)
    {
        if (!(object instanceof PenaltyOption))
        {
            return false;
        }
        PenaltyOption other = (PenaltyOption)object;
        return (dice == other.dice && strikeNumber == other.strikeNumber
            && striker.equals(other.striker) && target.equals(other.target));
    }

    /** Do not consider carryTargets. */
    @Override
    public int hashCode()
    {
        return dice + 100 * strikeNumber + striker.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
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
            boolean first = true;
            for (BattleHex hex : carryTargets)
            {
                if (!first)
                {
                    sb.append(", ");
                }
                first = false;
                BattleCritter target = game.getBattle().getCritter(hex);
                sb.append(target.getDescription());
            }
        }
        return sb.toString();
    }
}
