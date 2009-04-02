package net.sf.colossus.server;




/**
 * Interface for an Oracle used for AI Hints.
 * @version $Id$
 * @author Romain Dolbeau
 */
public interface HintOracleInterface
{
    /** name could be recruited */
    public boolean canRecruit(String name);

    /** terrain can be reached by the Stack/Legion */
    public boolean canReach(String terrain);

    /** how many in caretaker */
    public int creatureAvailable(String name);

    /** height of the bigger [height-wise] legion that can attack
     * the Stack/Legion - 0 if none can attack.
     */
    public int biggestAttackerHeight();

    /** label of the (master)hex */
    public String hexLabel();
}
