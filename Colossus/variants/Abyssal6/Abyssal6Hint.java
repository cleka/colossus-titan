package Abyssal6;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.ai.AbstractHintProvider;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IHintOracle;
import net.sf.colossus.variant.IOracleLegion;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


public class Abyssal6Hint extends AbstractHintProvider
{
    public Abyssal6Hint(Variant variant)
    {
        super(variant);
    }

    private final DevRandom rnd = new DevRandom();

    public CreatureType getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle,
        List<AIStyle> aiStyles)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = AbstractHintProvider.creaturesToStrings(recruits);

        boolean clemens = (aiStyles.contains(AIStyle.Clemens));
        if (clemens)
        {
            return getRecruitHintClemens(terrain, legion, recruits, oracle,
                aiStyles);
        }
        if (terrainId.equals("Brush"))
        {
            if (recruitNames.contains("Cyclops")
                && legion.numCreature("Cyclops") == 2
                && !legion.contains("Behemoth") //  Comment here and sometimes below to prevent Eclipse from wrapping
                && legion.getHeight() != 6
                && oracle.canReach("Jungle")
                && oracle.creatureAvailable("Behemoth") >= 2)
            {
                return getCreatureType("Cyclops");
            }
        }
        else if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Lion")
                && legion.numCreature("Lion") == 2
                && !legion.contains("Griffon") //
                && legion.getHeight() != 6
                && oracle.canReach("Desert")
                && oracle.creatureAvailable("Griffon") >= 2)
            {
                return getCreatureType("Lion");
            }
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (recruitNames.contains("Centaur")
                    && legion.numCreature("Centaur") == 2
                    && !legion.contains("Warbear")
                    && oracle.biggestAttackerHeight() == 0
                    && legion.getHeight() < 6
                    && oracle.canReach("Woods")
                    && !oracle.hexLabel().equals("1")
                    && !oracle.hexLabel().equals("15")
                    && !oracle.hexLabel().equals("29")
                // why no 'is available' here ?
                )

                {
                    return getCreatureType("Centaur");
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (recruitNames.contains("Centaur")
                    && legion.numCreature("Centaur") == 2
                    && !legion.contains("Warbear")
                    && oracle.biggestAttackerHeight() == 0
                    && legion.getHeight() <= 2
                    && oracle.canReach("Woods")
                // why no 'is available' here ?
                )
                {
                    return getCreatureType("Centaur");
                }
            }
        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruitNames.contains("Troll")
                && legion.numCreature("Troll") == 2
                && !legion.contains("Wyvern")
                && legion.getHeight() != 6
                && oracle.canReach("Swamp")
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return getCreatureType("Troll");
            }
            if (recruitNames.contains("Ranger")
                && legion.numCreature("Ranger") == 2
                && !legion.contains("AirElemental")
                && legion.getHeight() != 6 //
                && oracle.canReach("Plains") //
                && oracle.creatureAvailable("AirElemental") >= 3)
            {
                return getCreatureType("Ranger");
            }
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (recruitNames.contains("Ogre")
                    && legion.numCreature("Ogre") == 2
                    && !legion.contains("Minotaur")
                    && oracle.biggestAttackerHeight() == 0
                    && legion.getHeight() < 6 //
                    && oracle.canReach("Hills") //
                    && !oracle.hexLabel().equals("8")
                    && !oracle.hexLabel().equals("22")
                    && !oracle.hexLabel().equals("36"))
                {
                    return getCreatureType("Ogre");
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (recruitNames.contains("Ogre")
                    && legion.numCreature("Ogre") == 2
                    && !legion.contains("Minotaur")
                    && oracle.biggestAttackerHeight() == 0
                    && legion.getHeight() <= 2 //
                    && oracle.canReach("Hills")
                // why no 'is available' here ?
                )
                {
                    return getCreatureType("Ogre");
                }
            }
        }
        else if (terrainId.equals("Woods"))
        {
            if (recruitNames.contains("Unicorn")
                && legion.numCreature("Unicorn") == 2
                && !legion.contains("EarthElemental")
                && legion.getHeight() != 6
                && oracle.canReach("Hills")
                && oracle.creatureAvailable("EarthElemental") >= 3
            )
            {
                return getCreatureType("Unicorn");
            }
        }
        else if (terrainId.equals("Desert"))
        {
            if (recruitNames.contains("Hydra")
                && legion.numCreature("Hydra") == 2
                && !legion.contains("WaterElemental")
                && legion.getHeight() != 6 //
                && oracle.canReach("Swamp")
                && oracle.creatureAvailable("WaterElemental") >= 3)
            {
                return getCreatureType("Hydra");
            }
        }
        else if (terrainId.equals("Tundra"))
        {
            if (recruitNames.contains("Colossus")
                && legion.numCreature("Colossus") == 2
                && !legion.contains("FireElemental")
                && legion.getHeight() != 6
                && oracle.canReach("Mountains")
                && oracle.creatureAvailable("FireElemental") >= 3)
            {
                return getCreatureType("Colossus");
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruitNames.contains("Knight"))
            {
                return getCreatureType("Knight");
            }
            if (recruitNames.contains("Warlock"))
            {
                return getCreatureType("Warlock");
            }
            if (recruitNames.contains("Guardian"))
            {
                return getCreatureType("Guardian");
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 2)
            {
                return getCreatureType("Ogre");
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 2)
            {
                return getCreatureType("Centaur");
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 1
                && oracle.creatureAvailable("Cyclops") >= 3)
            {
                return getCreatureType("Gargoyle");
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 1
                && oracle.creatureAvailable("Troll") >= 2)
            {
                return getCreatureType("Ogre");
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 1
                && oracle.creatureAvailable("Lion") >= 2)
            {
                return getCreatureType("Centaur");
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 0
                && oracle.creatureAvailable("Cyclops") >= 6)
            {
                return getCreatureType("Gargoyle");
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 0
                && oracle.creatureAvailable("Troll") >= 6)
            {
                return getCreatureType("Ogre");
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 0
                && oracle.creatureAvailable("Lion") >= 6)
            {
                return getCreatureType("Centaur");
            }
        }

        return recruits.get(recruits.size() - 1);
    }

    public CreatureType getRecruitHintClemens(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits, IHintOracle oracle,
        List<AIStyle> aiStyles)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = AbstractHintProvider
            .creaturesToStrings(recruits);

        boolean clemens = (aiStyles.contains(AIStyle.Clemens));

        if (terrainId.equals("Brush"))
        {
            if (recruitNames.contains("Cyclops")
                && legion.numCreature("Cyclops") == 2
                && !legion.contains("Behemoth") //  Comment here and sometimes below to prevent Eclipse from wrapping
                && (clemens || legion.getHeight() != 6)
                && (clemens || oracle.canReach("Jungle"))
                && oracle.creatureAvailable("Behemoth") >= 2)
            {
                return getCreatureType("Cyclops");
            }
        }
        else if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Lion")
                && legion.numCreature("Lion") == 2
                && !legion.contains("Griffon") //
                && (clemens || legion.getHeight() != 6)
                && (clemens || oracle.canReach("Desert"))
                && oracle.creatureAvailable("Griffon") >= 2)
            {
                return getCreatureType("Lion");
            }
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (recruitNames.contains("Centaur")
                    && legion.numCreature("Centaur") == 2
                    && !legion.contains("Warbear")
                    && oracle.biggestAttackerHeight() == 0
                    && (clemens || legion.getHeight() < 6)
                    && (clemens || oracle.canReach("Woods"))
                    && !oracle.hexLabel().equals("1")
                    && !oracle.hexLabel().equals("15")
                    && !oracle.hexLabel().equals("29")
                // why no 'is available' here ?
                )
                {
                    return getCreatureType("Centaur");
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (recruitNames.contains("Centaur")
                    && legion.numCreature("Centaur") == 2
                    && !legion.contains("Warbear")
                    && oracle.biggestAttackerHeight() == 0
                    && legion.getHeight() <= 2
                    && (clemens || oracle.canReach("Woods"))
                // why no 'is available' here ?
                )
                {
                    return getCreatureType("Centaur");
                }
            }
        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruitNames.contains("Troll")
                && legion.numCreature("Troll") == 2
                && !legion.contains("Wyvern")
                && (clemens || legion.getHeight() != 6)
                && (clemens || oracle.canReach("Swamp"))
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return getCreatureType("Troll");
            }
            if (recruitNames.contains("Ranger")
                && legion.numCreature("Ranger") == 2
                && !legion.contains("AirElemental")
                && (clemens || legion.getHeight() != 6)
                && (clemens || oracle.canReach("Plains"))
                && oracle.creatureAvailable("AirElemental") >= 3)
            {
                return getCreatureType("Ranger");
            }
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (recruitNames.contains("Ogre")
                    && legion.numCreature("Ogre") == 2
                    && !legion.contains("Minotaur")
                    && oracle.biggestAttackerHeight() == 0
                    && (clemens || legion.getHeight() < 6)
                    && (clemens || oracle.canReach("Hills"))
                    && !oracle.hexLabel().equals("8")
                    && !oracle.hexLabel().equals("22")
                    && !oracle.hexLabel().equals("36"))
                {
                    return getCreatureType("Ogre");
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (recruitNames.contains("Ogre")
                    && legion.numCreature("Ogre") == 2
                    && !legion.contains("Minotaur")
                    && oracle.biggestAttackerHeight() == 0
                    && legion.getHeight() <= 2
                    && (clemens || oracle.canReach("Hills"))
                // why no 'is available' here ?
                )
                {
                    return getCreatureType("Ogre");
                }
            }
        }
        else if (terrainId.equals("Woods"))
        {
            if (recruitNames.contains("Unicorn")
                && legion.numCreature("Unicorn") == 2
                && !legion.contains("EarthElemental")
                && (clemens || legion.getHeight() != 6)
                && (clemens || oracle.canReach("Hills"))
                && oracle.creatureAvailable("EarthElemental") >= 3)
            {
                return getCreatureType("Unicorn");
            }
        }
        else if (terrainId.equals("Desert"))
        {
            if (recruitNames.contains("Hydra")
                && legion.numCreature("Hydra") == 2
                && !legion.contains("WaterElemental")
                && (clemens || legion.getHeight() != 6)
                && (clemens || oracle.canReach("Swamp"))
                && oracle.creatureAvailable("WaterElemental") >= 3)
            {
                return getCreatureType("Hydra");
            }
        }
        else if (terrainId.equals("Tundra"))
        {
            if (recruitNames.contains("Colossus")
                && legion.numCreature("Colossus") == 2
                && !legion.contains("FireElemental")
                && (clemens || legion.getHeight() != 6)
                && (clemens || oracle.canReach("Mountains"))
                && oracle.creatureAvailable("FireElemental") >= 3)
            {
                return getCreatureType("Colossus");
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruitNames.contains("Knight"))
            {
                return getCreatureType("Knight");
            }
            if (recruitNames.contains("Warlock"))
            {
                return getCreatureType("Warlock");
            }
            if (recruitNames.contains("Guardian"))
            {
                return getCreatureType("Guardian");
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 2)
            {
                return getCreatureType("Ogre");
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 2)
            {
                return getCreatureType("Centaur");
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 1
                && oracle.creatureAvailable("Cyclops") >= 3)
            {
                return getCreatureType("Gargoyle");
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 1
                && oracle.creatureAvailable("Troll") >= 2)
            {
                return getCreatureType("Ogre");
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 1
                && oracle.creatureAvailable("Lion") >= 2)
            {
                return getCreatureType("Centaur");
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 0
                && oracle.creatureAvailable("Cyclops") >= 6)
            {
                return getCreatureType("Gargoyle");
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 0
                && oracle.creatureAvailable("Troll") >= 6)
            {
                return getCreatureType("Ogre");
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 0
                && oracle.creatureAvailable("Lion") >= 6)
            {
                return getCreatureType("Centaur");
            }
        }

        return recruits.get(recruits.size() - 1);
    }

    public List<CreatureType> getInitialSplitHint(MasterHex hex,
        List<AIStyle> aiStyles)
    {
        List<CreatureType> li = new ArrayList<CreatureType>();
        if (hex.getLabel().equals("100"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Ogre"));
            }
        }
        else if (hex.getLabel().equals("200"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Gargoyle"));
            li.add(getCreatureType("Gargoyle"));
            li.add(getCreatureType("Ogre"));
        }
        else if (hex.getLabel().equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Ogre"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Ogre"));
            }
        }
        else if (hex.getLabel().equals("400"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Ogre"));
                li.add(getCreatureType("Ogre"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Centaur"));
            }
        }
        else if (hex.getLabel().equals("500"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Gargoyle"));
            li.add(getCreatureType("Gargoyle"));
            li.add(getCreatureType("Centaur"));
        }
        else if (hex.getLabel().equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Centaur"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Ogre"));
                li.add(getCreatureType("Ogre"));
                li.add(getCreatureType("Centaur"));
            }
        }
        else
        {
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }

    @Override
    public int getHintedRecruitmentValueOffset(CreatureType creature,
        List<AIStyle> styles)
    {
        if (creature.getName().equals("Druid"))
        {
            return -10;
        }
        return 0;
    }
}
