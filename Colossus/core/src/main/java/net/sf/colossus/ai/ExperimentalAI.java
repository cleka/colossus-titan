package net.sf.colossus.ai;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.colossus.client.BattleChit;
import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.client.Strike;
import net.sf.colossus.server.Constants;
import net.sf.colossus.variant.BattleHex;


/**
 * Yet Another AI, to test some stuff.
 * @version $Id$
 * @author Romain Dolbeau
 */

public class ExperimentalAI extends SimpleAI
{
    public ExperimentalAI(Client client)
    {
        super(client);
    }

    @Override
    Collection<LegionMove> findLegionMoves(final List<List<CritterMove>> allCritterMoves) {
        return new OnTheFlyLegionMove(allCritterMoves);
    }

    @Override
    protected int evaluateLegionBattleMoveAsAWhole(LegionMove lm, Map<String, Integer> strikeMap, StringBuffer why) {
        int value = 0;
        final LegionClientSide legion = (LegionClientSide)client.getMyEngagedLegion();
        if (legion.equals(client.getAttacker())) {
            // TODO, something
        } else {
            boolean nobodyGetsHurt = true;
            int numCanBeReached = 0;
            int maxThatCanReach = 0;
            //for (CritterMove cm : lm.getCritterMoves())
            for (BattleChit critter : client.getActiveBattleChits())
            {
                int canReachMe = 0;
                //BattleChit critter = cm.getCritter();
                BattleHex myHex = client.getBattleHex(critter);
                List<BattleChit> foes = client.getInactiveBattleChits();
                for (BattleChit foe : foes) {
                    BattleHex foeHex = client.getBattleHex(foe);
                    int range = Strike.getRange(foeHex, myHex, true);
                    if ((range != Constants.OUT_OF_RANGE) &&
                        ((range - 2) <= foe.getSkill())) {
                        canReachMe++;
                    }
                }
                if (canReachMe > 0) {
                    nobodyGetsHurt = false;
                    numCanBeReached ++;
                    if (maxThatCanReach < canReachMe)
                        maxThatCanReach = canReachMe;
                }
            }
            if (numCanBeReached == 1) // TODO: Rangestriker
            {
                value += bec.DEF__AT_MOST_ONE_IS_REACHABLE;
                why.append("+");
                why.append(bec.DEF__AT_MOST_ONE_IS_REACHABLE);
                why.append(" [Def_AtMostOneIsReachable]");
            }
            if (maxThatCanReach == 1) // TODO: Rangestriker
            {
                value += bec.DEF__NOONE_IS_GANGBANGED;
                why.append("+");
                why.append(bec.DEF__NOONE_IS_GANGBANGED);
                why.append(" [Def_NoOneIsGangbanged]");
            }
            if (nobodyGetsHurt) // TODO: Rangestriker
            {
                value += bec.DEF__NOBODY_GETS_HURT;
                why.append("+");
                why.append(bec.DEF__NOBODY_GETS_HURT);
                why.append(" [Def_NobodyGetsHurt]");
            }
        }
        return value;
    }
}
