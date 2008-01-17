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
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.server.CreatureTypeServerSide;
import net.sf.colossus.util.Combos;
import net.sf.colossus.variant.CreatureType;
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

    double findRecruitPotential(Map<CreatureTypeServerSide, Integer> critters, String terrain)
    {
        int recruitNow = 0;
        int recruitLater = 0;

        List<CreatureType> tempRecruits = TerrainRecruitLoader
            .getPossibleRecruits(terrain, "");
        List<CreatureType> recruiters = TerrainRecruitLoader
            .getPossibleRecruiters(terrain, "");

        recruiters.retainAll(critters.keySet());

        Iterator<CreatureType> lit = tempRecruits.iterator();
        while (lit.hasNext())
        {
            CreatureTypeServerSide creature = (CreatureTypeServerSide)lit.next();
            Iterator<CreatureType> liter = recruiters.iterator();
            while (liter.hasNext())
            {
                CreatureTypeServerSide lesser = (CreatureTypeServerSide)liter.next();
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
    MusteredCreatures chooseCreaturesToSplitOut(LegionClientSide legion,
        boolean at_risk)
    {

        //
        // split a 5 to 8 high legion
        //
        if (legion.getHeight() == 8)
        {
            List<CreatureTypeServerSide> creatures = doInitialGameSplit(legion.getHexLabel());

            return new MusteredCreatures(true, creatures);
        }

        LOGGER.log(Level.FINEST,
            "sortCreaturesByValueName() in chooseCreaturesToSplitOut");

        boolean hasTitan = legion.contains("Titan");
        String[] terrains = TerrainRecruitLoader.getTerrains();

        List<CreatureTypeServerSide> critters = new ArrayList<CreatureTypeServerSide>();
        for (String string : legion.getContents())
        {
            critters.add((CreatureTypeServerSide)client.getGame().getVariant()
                .getCreatureByName(string));
        }

        double bestValue = 0;
        // make sure the list is never null even if we don't find anything
        List<CreatureTypeServerSide> bestKeep = new ArrayList<CreatureTypeServerSide>();

        Combos<CreatureTypeServerSide> combos = new Combos<CreatureTypeServerSide>(critters, critters
            .size() - 2);
        for (Iterator<List<CreatureTypeServerSide>> it = combos.iterator(); it.hasNext();)
        {
            List<CreatureTypeServerSide> keepers = it.next();
            double critterValue = 0;
            boolean keepTitan = false;
            Map<CreatureTypeServerSide, Integer> critterMap = new HashMap<CreatureTypeServerSide, Integer>();
            for (CreatureTypeServerSide critter : keepers)
            {
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
        for (CreatureTypeServerSide creature : bestKeep)
        {
            critters.remove(creature);
        }
        LOGGER.log(Level.FINEST, "Splitting: " + bestKeep + "/" + critters);

        return new MusteredCreatures(false, critters);
    }
}
