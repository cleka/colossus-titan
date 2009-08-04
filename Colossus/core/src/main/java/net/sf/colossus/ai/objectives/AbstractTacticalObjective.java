package net.sf.colossus.ai.objectives;


/** Abstract implementation of @TacticalObjective, handling the priority
 * stuff to avoid duplication.
 *
 * @author Romain Dolbeau
 */
public abstract class AbstractTacticalObjective implements TacticalObjective
{
    private float priority;

    public AbstractTacticalObjective(float priority)
    {
        this.priority = priority;
    }

    public float getPriority()
    {
        return priority;
    }

    public float changePriority(float newPriority)
    {
        float oldPriority = priority;
        priority = newPriority;
        return oldPriority;
    }
}
