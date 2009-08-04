package net.sf.colossus.ai.objectives;


import java.util.List;


/**
 * Trivial interface for getting a list of objectives, depending on whether
 * we are the attacker or the defender.
 * @author Romain Dolbeau
 */
public interface IObjectiveHelper
{
    List<TacticalObjective> attackerObjective();

    List<TacticalObjective> defenderObjective();
}
