package net.sf.colossus.server;

/**
 * Interface for an Oracle used for AI Hints.
 * @version $Id$
 * @author Romain Dolbeau
 */
public interface HintOracleInterface
{
    public boolean hasCreature(String name); // in the Stack/Legion
    public boolean canRecruit(String name); // name could be recruited
    public boolean canReach(char t); // t can be reached by the Stack/Legion

    public int creatureAvailable(String name); // how many in caretaker
    public int stackHeight(); // height of the Stack/Legion

    public String hexLabel(); // label of the (master)hex
}
