package net.sf.colossus.ai;

/**
 *
 * @author dolbeau
 */
public abstract class AbstractTacticalObjective implements TacticalObjective {
    private int priority;

    public AbstractTacticalObjective(int priority)
    {
        this.priority = priority;
    }

    public int getPriority()
    {
        return priority;
    }

    public int changePriority(int newPriority)
    {
        int oldPriority = priority;
        priority = newPriority;
        return oldPriority;
    }
}
