package net.sf.colossus.client;


import java.util.*;


/**
 * Class Proposal holds the results of a settlement attempt.
 * @version $Id$
 * @author David Ripton
 */

public final class Proposal
{
    private boolean fight;
    private boolean mutual;
    private String attackerId;
    private String defenderId;
    private String winnerId;
    private List winnerLosses;  // Must sort before comparing.
    private String hexLabel;


    Proposal(String attackerId, String defenderId, boolean fight,
        boolean mutual, String winnerId, List winnerLosses, String hexLabel)
    {
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.fight = fight;
        this.mutual = mutual;
        this.winnerId = winnerId;
        this.winnerLosses = winnerLosses;
        this.hexLabel = hexLabel;
    }


    public String getAttackerId()
    {
        return attackerId;
    }

    public String getDefenderId()
    {
        return defenderId;
    }

    public String getHexLabel()
    {
        return hexLabel;
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

    public List getWinnerLosses()
    {
        return winnerLosses;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Proposal))
        {
            return false;
        }
        Proposal otherProposal = (Proposal)other;

        if (fight && otherProposal.isFight())
        {
            return true;
        }
        if (fight != otherProposal.isFight())
        {
            return false;
        }

        if (mutual && otherProposal.isMutual())
        {
            return true;
        }
        if (mutual != otherProposal.isMutual())
        {
            return false;
        }

        if (!winnerId.equals(otherProposal.getWinnerId()))
        {
            return false;
        }
        if (!winnerLosses.equals(otherProposal.getWinnerLosses()))
        {
            return false;
        }
        return true;
    }
}
