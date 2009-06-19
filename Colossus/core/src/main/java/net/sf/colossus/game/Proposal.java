package net.sf.colossus.game;


import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Split;


/**
 * Class Proposal holds the results of a settlement attempt.
 *
 * @author David Ripton
 */
public final class Proposal
{
    private final Legion attacker;
    private final Legion defender;
    private final boolean fight;
    private final boolean mutual;
    private final Legion winner;
    private final List<String> winnerLosses;

    private static final String sep = Glob.sep;

    public Proposal(Legion attacker, Legion defender, boolean fight,
        boolean mutual, Legion winner, List<String> winnerLosses)
    {
        this.attacker = attacker;
        this.defender = defender;
        this.fight = fight;
        this.mutual = mutual;
        this.winner = winner;
        this.winnerLosses = winnerLosses;
        if (winnerLosses != null)
        {
            Collections.sort(winnerLosses);
        }
    }

    public Legion getAttacker()
    {
        return attacker;
    }

    public Legion getDefender()
    {
        return defender;
    }

    public Legion getWinner()
    {
        return winner;
    }

    // Mostly needed for toString()
    private String getAttackerId()
    {
        return attacker == null ? null : attacker.getMarkerId();
    }

    private String getDefenderId()
    {
        return defender == null ? null : defender.getMarkerId();
    }

    private String getWinnerId()
    {
        return winner == null ? null : winner.getMarkerId();
    }

    public boolean isFight()
    {
        return fight;
    }

    public boolean isMutual()
    {
        return mutual;
    }

    public List<String> getWinnerLosses()
    {
        return winnerLosses;
    }

    @Override
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

        if (!winner.equals(otherProposal.getWinner()))
        {
            return false;
        }
        if (!winnerLosses.equals(otherProposal.getWinnerLosses()))
        {
            return false;
        }
        return true;
    }

    @Override
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
        return getWinnerId().hashCode() + winnerLosses.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(fight);
        sb.append(sep);
        sb.append(mutual);
        sb.append(sep);
        sb.append(getAttackerId());
        sb.append(sep);
        sb.append(getDefenderId());
        sb.append(sep);
        sb.append(getWinnerId());
        sb.append(sep);
        if (winnerLosses != null)
        {
            Iterator<String> it = winnerLosses.iterator();
            while (it.hasNext())
            {
                String creatureName = it.next();
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
    public static Proposal makeFromString(String s, Game game)
    {
        List<String> li = Split.split(sep, s);

        boolean fight = Boolean.valueOf(li.remove(0)).booleanValue();
        boolean mutual = Boolean.valueOf(li.remove(0)).booleanValue();
        Legion attacker = game.getLegionByMarkerId(li.remove(0));
        Legion defender = game.getLegionByMarkerId(li.remove(0));
        Legion winner = null;
        String winnerId = li.remove(0);
        // Do not try to re-instantiate if markerId is null
        // (client, esp. AI, selected "Fight!" and probably also for mutual)
        if (!fight && winnerId != null)
        {
            winner = game.getLegionByMarkerId(winnerId);
        }
        List<String> winnerLosses = li;

        return new Proposal(attacker, defender, fight, mutual, winner,
            winnerLosses);
    }
}
