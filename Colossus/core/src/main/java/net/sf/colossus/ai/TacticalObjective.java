package net.sf.colossus.ai;

/**
 *
 * @author Romain Dolbeau
 */
public interface TacticalObjective
{
    boolean objectiveAttained();

    int situationContributeToTheObjective();

    int getPriority();

    String getDescription();
}
