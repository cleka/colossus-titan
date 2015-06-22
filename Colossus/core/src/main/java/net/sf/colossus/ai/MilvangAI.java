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
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.Combos;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * @author kmilvangjens
 */
public class MilvangAI extends RationalAI // NO_UCD
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

    double findRecruitPotential(Map<CreatureType, Integer> critters,
        MasterBoardTerrain terrain)
    {
        int recruitNow = 0;
        int recruitLater = 0;

        // TODO why do we pass null here? RD/ it's (not quite) a bug, the Hex
        // is required only for special recruiting. This will only miss
        // Balrogs in variant Balrogs.
        List<CreatureType> tempRecruits = TerrainRecruitLoader
            .getPossibleRecruits(terrain, null);
        List<CreatureType> recruiters = TerrainRecruitLoader
            .getPossibleRecruiters(terrain, null);

        recruiters.retainAll(critters.keySet());

        Iterator<CreatureType> lit = tempRecruits.iterator();
        while (lit.hasNext())
        {
            CreatureType creature = lit.next();
            Iterator<CreatureType> liter = recruiters.iterator();
            while (liter.hasNext())
            {
                CreatureType lesser = liter.next();
                // TODO another null for the TerranRecruitLoader -> why? Same
                // reason: it's required for custom recruiting.
                int numNeeded = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    lesser, creature, terrain, null);
                // TODO Not sure whether it's totally clean to directly use the
                // NonTitan form. But Titan cannot be a recruit anyway, and
                // before I changed it it used the method from CreatureType
                // directly anyway.
                int hintValue = getHintedRecruitmentValueNonTitan(creature);
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
    MusteredCreatures chooseCreaturesToSplitOut(Legion legion, boolean at_risk)
    {

        //
        // split a 5 to 8 high legion
        //
        if (legion.getHeight() == 8)
        {
            List<CreatureType> creatures = doInitialGameSplit(legion
                .getCurrentHex());

            return new MusteredCreatures(true, creatures);
        }

        LOGGER.log(Level.FINEST,
            "sortCreaturesByValueName() in chooseCreaturesToSplitOut");

        boolean hasTitan = legion.hasTitan();

        List<CreatureType> critters = new ArrayList<CreatureType>();
        for (Creature creature : legion.getCreatures())
        {
            critters.add(creature.getType());
        }

        double bestValue = 0;
        // make sure the list is never null even if we don't find anything
        List<CreatureType> bestKeep = new ArrayList<CreatureType>();

        Combos<CreatureType> combos = new Combos<CreatureType>(critters,
            critters.size() - 2);
        for (Iterator<List<CreatureType>> it = combos.iterator(); it.hasNext();)
        {
            List<CreatureType> keepers = it.next();
            double critterValue = 0;
            boolean keepTitan = false;
            Map<CreatureType, Integer> critterMap = new HashMap<CreatureType, Integer>();
            for (CreatureType creatureType : keepers)
            {
                keepTitan |= creatureType.getName().equals("Titan");
                // TODO: should this use the Titan-aware form of the method?
                int tmp = getHintedRecruitmentValueNonTitan(creatureType);
                critterValue += tmp * tmp;
                Integer numCritters = critterMap.get(creatureType);
                if (numCritters == null)
                {
                    critterMap.put(creatureType, Integer.valueOf(1));
                }
                else
                {
                    critterMap.put(creatureType,
                        Integer.valueOf(numCritters.intValue() + 1));
                }
            }

            if (hasTitan && !keepTitan)
            {
                continue; // do no consider splitting Titan off
            }

            double totalRecruitValue = 0;
            double bestRecruitValue = 0;
            for (MasterBoardTerrain terrain : variant.getTerrains())
            {
                double currRecruitValue = findRecruitPotential(critterMap,
                    terrain);
                // TODO shouldn't that rather be terrain.isTower()?
                if (currRecruitValue > bestRecruitValue
                    && !terrain.getId().equals("Tower"))
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
        for (CreatureType creature : bestKeep)
        {
            critters.remove(creature);
        }
        LOGGER.log(Level.FINEST, "Splitting: " + bestKeep + "/" + critters);

        return new MusteredCreatures(false, critters);
    }
}
