import java.util.*;

/**
 * Class NegotiationResults holds the results of a settlement attempt.
 * @version $Id$
 * @author David Ripton
 */

public final class NegotiationResults
{
    private boolean fight;
    private boolean mutual;
    private String attackerId;
    private String defenderId;
    private String winnerId;
    private Set winnerLosses;


    public NegotiationResults(String attackerId, String defenderId,
        boolean fight, boolean mutual, String winnerId, Set winnerLosses)
    {
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.fight = fight;
        this.mutual = mutual;
        this.winnerId = winnerId;
        this.winnerLosses = winnerLosses;
    }


    public String getAttackerId()
    {
        return attackerId;
    }

    public String getDefenderId()
    {
        return defenderId;
    }

    public boolean isFight()
    {
        return fight;
    }

    public boolean isMutual()
    {
        return mutual;
    }

    public String getWinnerId()
    {
        return winnerId;
    }

    public Set getWinnerLosses()
    {
        return winnerLosses;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof NegotiationResults))
        {
            return false;
        }
        NegotiationResults otherResults = (NegotiationResults)other;

        if (fight && otherResults.isFight())
        {
            return true;
        }
        if (fight != otherResults.isFight())
        {
            return false;
        }

        if (mutual && otherResults.isMutual())
        {
            return true;
        }
        if (mutual != otherResults.isMutual())
        {
            return false;
        }

        if (!winnerId.equals(otherResults.getWinnerId()))
        {
            return false;
        }
        if (!winnerLosses.equals(otherResults.getWinnerLosses()))
        {
            return false;
        }
        return true;
    }
}
