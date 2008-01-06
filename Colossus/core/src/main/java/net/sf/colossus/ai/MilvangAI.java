/*
 * Created on 07-01-2005
 *
 */
package net.sf.colossus.ai;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.Combos;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * @author kmilvangjens
 * @version $Id$
 */
public class MilvangAI extends RationalAI
{
    private static final Logger LOGGER = Logger.getLogger(MilvangAI.class
        .getName());

    private static final double PRIMARY_RECRUIT_FACTOR = 1.0;
    private static final double SECONDARY_RECRUIT_FACTOR = 0.1;

    public MilvangAI(Client client)
    {
        super(client);
        // TODO Auto-generated constructor stub
    }

    double findRecruitPotential(Map<Creature, Integer> critters, String terrain)
    {
        int recruitNow = 0;
        int recruitLater = 0;

        List<Creature> tempRecruits = TerrainRecruitLoader
            .getPossibleRecruits(terrain, "");
        List<Creature> recruiters = TerrainRecruitLoader
            .getPossibleRecruiters(terrain, "");

        recruiters.retainAll(critters.keySet());

        Iterator<Creature> lit = tempRecruits.iterator();
        while (lit.hasNext())
        {
            Creature creature = lit.next();
            Iterator<Creature> liter = recruiters.iterator();
            while (liter.hasNext())
            {
                Creature lesser = liter.next();
                int numNeeded = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    lesser, creature, terrain, "");
                int hintValue = creature.getHintedRecruitmentValue();
                if (hintValue > recruitNow
                    && numNeeded <= critters.get(lesser).intValue())
                {
                    recruitNow = hintValue;
                }
                if (hintValue > recruitLater)
                {
                    recruitLater = hintValue;
                }
            }
        }
        return recruitNow * recruitNow + 0.1 * recruitLater * recruitLater;
    }

    @Override
    MusteredCreatures chooseCreaturesToSplitOut(LegionInfo legion,
        boolean at_risk)
    {

        //
        // split a 5 to 8 high legion
        //
        if (legion.getHeight() == 8)
        {
            List<Creature> creatures = doInitialGameSplit(legion.getHexLabel());

            return new MusteredCreatures(true, creatures);
        }

        LOGGER.log(Level.FINEST,
            "sortCreaturesByValueName() in chooseCreaturesToSplitOut");

        boolean hasTitan = legion.contains("Titan");
        String[] terrains = TerrainRecruitLoader.getTerrains();

        List<Creature> critters = new ArrayList<Creature>();
        for (Iterator<String> it = legion.getContents().iterator(); it
            .hasNext();)
        {
            critters.add(Creature.getCreatureByName(it.next()));
        }

        double bestValue = 0;
        // make sure the list is never null even if we don't find anything
        List<Creature> bestKeep = new ArrayList<Creature>();

        Combos<Creature> combos = new Combos<Creature>(critters, critters
            .size() - 2);
        for (Iterator<List<Creature>> it = combos.iterator(); it.hasNext();)
        {
            List<Creature> keepers = it.next();
            double critterValue = 0;
            boolean keepTitan = false;
            Map<Creature, Integer> critterMap = new HashMap<Creature, Integer>();
            for (Iterator<Creature> it2 = keepers.iterator(); it2.hasNext();)
            {
                Creature critter = it2.next();
                keepTitan |= critter.getName().equals("Titan");
                int tmp = critter.getHintedRecruitmentValue();
                critterValue += tmp * tmp;
                Integer numCritters = critterMap.get(critter);
                if (numCritters == null)
                {
                    critterMap.put(critter, new Integer(1));
                }
                else
                {
                    critterMap.put(critter, new Integer(
                        numCritters.intValue() + 1));
                }
            }

            if (hasTitan && !keepTitan)
            {
                continue; // do no consider splitting Titan off
            }

            double totalRecruitValue = 0;
            double bestRecruitValue = 0;
            for (int i = 0; i < terrains.length; i++)
            {
                double currRecruitValue = findRecruitPotential(critterMap,
                    terrains[i]);
                if (currRecruitValue > bestRecruitValue
                    && !terrains[i].equals("Tower"))
                {
                    totalRecruitValue += SECONDARY_RECRUIT_FACTOR
                        * bestRecruitValue;
                    bestRecruitValue = currRecruitValue;
                }
                else
                {
                    totalRecruitValue += SECONDARY_RECRUIT_FACTOR
                        * currRecruitValue;
                }
            }
            totalRecruitValue += PRIMARY_RECRUIT_FACTOR * bestRecruitValue;

            if (critterValue + totalRecruitValue > bestValue)
            {
                bestValue = critterValue + totalRecruitValue;
                bestKeep = keepers;
            }
        }

        // remove the keep from critters to obtain the split
        for (Iterator<Creature> it2 = bestKeep.iterator(); it2.hasNext();)
        {
            critters.remove(it2.next());
        }
        LOGGER.log(Level.FINEST, "Splitting: " + bestKeep + "/" + critters);

        return new MusteredCreatures(false, critters);
    }
}
