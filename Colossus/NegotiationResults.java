import java.util.*;

/**
 * Class NegotiationResults holds the results of a settlement attempt.
 * @version $Id$
 * @author David Ripton
 */

public class NegotiationResults
{
    private boolean settled;
    private boolean mutual;
    private Legion winner;
    private ArrayList winnerLosses;


    public NegotiationResults(boolean settled, boolean mutual, Legion
        winner, ArrayList winnerLosses)
    {
        this.settled = settled;
        this.mutual = mutual;
        this.winner = winner;
        this.winnerLosses = winnerLosses;
    }


    public boolean isSettled()
    {
        return settled;
    }


    public boolean isMutual()
    {
        return mutual;
    }


    public Legion getWinner()
    {
        return winner;
    }


    public ArrayList getWinnerLosses()
    {
        return winnerLosses;
    }
}
