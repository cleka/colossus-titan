package net.sf.colossus.client;


import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Split;


/**
 * Class Proposal holds the results of a settlement attempt.
 * @version $Id$
 * @author David Ripton
 */

public final class Proposal
{
    private String attackerId;
    private String defenderId;
    private boolean fight;
    private boolean mutual;
    private String winnerId;
    private List winnerLosses;

    private static final String sep = Glob.sep;

    Proposal(String attackerId, String defenderId, boolean fight,
        boolean mutual, String winnerId, List winnerLosses)
    {
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.fight = fight;
        this.mutual = mutual;
        this.winnerId = winnerId;
        this.winnerLosses = winnerLosses;
        if (winnerLosses != null)
        {
            Collections.sort(winnerLosses);
        }
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

    public int hashCode()
    {
        if (fight)
        {
            return 1;
        }
        if (mutual)
        {
            return 2;
        }
        return winnerId.hashCode() + winnerLosses.hashCode();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(fight);
        sb.append(sep);
        sb.append(mutual);
        sb.append(sep);
        sb.append(attackerId);
        sb.append(sep);
        sb.append(defenderId);
        sb.append(sep);
        sb.append(winnerId);
        sb.append(sep);
        sb.append(sep);
        if (winnerLosses != null)
        {
            Iterator it = winnerLosses.iterator();
            while (it.hasNext())
            {
                String creatureName = (String)it.next();
                sb.append(creatureName);
                sb.append(sep);
            }
        }
        if (sb.toString().endsWith("~"))
        {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /** Create a Proposal from a {sep}-separated list of fields. */
    public static Proposal makeFromString(String s)
    {
        List li = Split.split(sep, s);

        boolean fight = Boolean.valueOf((String)li.remove(0)).booleanValue();
        boolean mutual = Boolean.valueOf((String)li.remove(0)).booleanValue();
        String attackerId = (String)li.remove(0);
        String defenderId = (String)li.remove(0);
        String winnerId = (String)li.remove(0);
        List winnerLosses = li;

        return new Proposal(attackerId, defenderId, fight, mutual, winnerId,
            winnerLosses);
    }
}
