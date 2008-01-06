package net.sf.colossus.server;


import java.util.List;


/**
 * Interface for an Oracle used for AI Hints.
 * @version $Id$
 * @author Romain Dolbeau
 */
public interface HintOracleInterface
{

    /** in the Stack/Legion */
    public boolean hasCreature(String name);

    /** in a different Stack/Legion */
    public boolean otherFriendlyStackHasCreature(List<String> allNames);

    /** name could be recruited */
    public boolean canRecruit(String name);

    /** terrain can be reached by the Stack/Legion */
    public boolean canReach(String terrain);

    /** how many in caretaker */
    public int creatureAvailable(String name);

    /** height of the Stack/Legion */
    public int stackHeight();

    /** height of the bigger [height-wise] legion that can attack 
     * the Stack/Legion - 0 if none can attack.
     */
    public int biggestAttackerHeight();

    /** label of the (master)hex */
    public String hexLabel();
}
