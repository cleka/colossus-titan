package net.sf.colossus.ai.objectives;


import java.util.List;


/**
 *
 * @author Romain Dolbeau
 */
public interface IObjectiveHelper
{
    List<TacticalObjective> attackerObjective();

    List<TacticalObjective> defenderObjective();
}
