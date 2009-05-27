package net.sf.colossus.ai;

import java.util.List;
import net.sf.colossus.client.Client;

/**
 *
 * @author Romain Dolbeau
 */
interface IObjectiveHelper
{
    List<TacticalObjective> attackerObjective();
    List<TacticalObjective> defenderObjective();
}
