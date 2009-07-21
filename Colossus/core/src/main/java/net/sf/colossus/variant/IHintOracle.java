package net.sf.colossus.variant;


/**
 * Interface for an Oracle used for AI Hints.
 *
 * These methods are to be used in the context of a given legion,
 * on a given master board hex and with a list of recruit options.
 * This is currently implemented by {@linkplain net.sf.colossus.ai.AbstractAI}.
 *
 * @author Romain Dolbeau
 */
public interface IHintOracle
{
    /**
     * A creature with the given name could be recruited.
     */
    public boolean canRecruit(String name);

    /**
     * A terrain can be reached by the legion with one move.
     */
    public boolean canReach(String terrain);

    /**
     * The number of currently available creatures of the given type.
     */
    public int creatureAvailable(String name);

    int creatureAvailable(CreatureType creatureType);

    /**
     * The height of the tallest legion that can attack
     * the legion we consider.
     *
     * 0 if none can attack.
     */
    public int biggestAttackerHeight();

    /**
     * The label of the master board hex under consideration.
     */
    public String hexLabel();
}
