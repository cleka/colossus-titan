package net.sf.colossus.server;

/**
 * Interface for an Oracle used for AI Hints.
 * @version $Id$
 * @author Romain Dolbeau
 */
public interface HintOracleInterface
{
    public boolean hasCreature(String name); // in the Stack/Legion
    public boolean otherFriendlyStackHasCreature(java.util.List allNames); // in a different Stack/Legion
    public boolean canRecruit(String name); // name could be recruited
    public boolean canReach(String terrain); // terrain can be reached by the Stack/Legion

    public int creatureAvailable(String name); // how many in caretaker
    public int stackHeight(); // height of the Stack/Legion
    public int biggestAttackerHeight(); // height of the bigger [height-wise] legion that can attack the Stack/Legion - 0 if none can attack.

    public String hexLabel(); // label of the (master)hex
}
