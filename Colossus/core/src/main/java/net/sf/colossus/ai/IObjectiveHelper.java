package net.sf.colossus.ai;

import java.util.List;

/**
 *
 * @author Romain Dolbeau
 */
interface IObjectiveHelper
{
    List<TacticalObjective> attackerObjective();
    List<TacticalObjective> defenderObjective();
}
