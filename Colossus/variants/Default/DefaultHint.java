package Default;


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


public class DefaultHint extends AbstractHintProvider
{
    private final CreatureType behemoth;
    private final CreatureType centaur;
    private final CreatureType cyclops;
    private final CreatureType gargoyle;
    private final CreatureType griffon;
    private final CreatureType guardian;
    private final CreatureType lion;
    private final CreatureType minotaur;
    private final CreatureType ogre;
    private final CreatureType serpent;
    private final CreatureType titan;
    private final CreatureType troll;
    private final CreatureType warbear;
    private final CreatureType warlock;
    private final CreatureType wyvern;

    public DefaultHint(Variant variant)
    {
        super(variant);
        this.behemoth = getCreatureType("Behemoth");
        this.centaur = getCreatureType("Centaur");
        this.cyclops = getCreatureType("Cyclops");
        this.gargoyle = getCreatureType("Gargoyle");
        this.griffon = getCreatureType("Griffon");
        this.guardian = getCreatureType("Guardian");
        this.lion = getCreatureType("Lion");
        this.minotaur = getCreatureType("Minotaur");
        this.ogre = getCreatureType("Ogre");
        this.serpent = getCreatureType("Serpent");
        this.titan = getCreatureType("Titan");
        this.troll = getCreatureType("Troll");
        this.warbear = getCreatureType("Warbear");
        this.warlock = getCreatureType("Warlock");
        this.wyvern = getCreatureType("Wyvern");
    }

    private final DevRandom rnd = new DevRandom();

    public CreatureType getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle,
        List<AIStyle> aiStyles)
    {
        String terrainId = terrain.getId();
        if (terrainId.equals("Brush") || terrainId.equals("Jungle"))
        {
            int numCyclops = legion.numCreature(cyclops);
            if (numCyclops > 0 && numCyclops < 3
                && !legion.contains(behemoth)
                && !legion.contains(serpent)
                && oracle.creatureAvailable(behemoth) >= 2
                && oracle.creatureAvailable(cyclops) >= 1)
            {
                return cyclops;
            }
        }
        else if (terrainId.equals("Plains"))
        {
            if (recruits.contains(lion) && !legion.contains(griffon)
                && legion.numCreature(lion) == 2
                && oracle.canReach("Desert")
                && oracle.creatureAvailable(griffon) >= 2)
            {
                return lion;
            }
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (recruits.contains(centaur)
                    && legion.numCreature(centaur) == 2
                    && !legion.contains(warbear) && legion.getHeight() < 6
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Woods")
                    && !oracle.hexLabel().equals("1")
                    && !oracle.hexLabel().equals("15")
                    && !oracle.hexLabel().equals("29"))
                {
                    return centaur;
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (recruits.contains(centaur)
                    && legion.numCreature(centaur) == 2
                    && !legion.contains(warbear) && legion.getHeight() <= 2
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Woods"))
                {
                    return centaur;
                }
            }
        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruits.contains(troll) && !legion.contains(wyvern)
                && legion.numCreature(troll) == 2
                && oracle.canReach("Swamp")
                && oracle.creatureAvailable(wyvern) >= 2)
            {
                return troll;
            }
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (recruits.contains(ogre) && legion.numCreature(ogre) == 2
                    && !legion.contains(minotaur) && legion.getHeight() < 6
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Hills")
                    && !oracle.hexLabel().equals("8")
                    && !oracle.hexLabel().equals("22")
                    && !oracle.hexLabel().equals("36"))
                {
                    return ogre;
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (recruits.contains(ogre) && legion.numCreature(ogre) == 2
                    && !legion.contains(minotaur) && legion.getHeight() <= 2
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Hills"))
                {
                    return ogre;
                }
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruits.contains(warlock))
            {
                return warlock;
            }
            if (recruits.contains(guardian))
            {
                return guardian;
            }
            if (recruits.contains(ogre) && legion.numCreature(ogre) == 2)
            {
                return ogre;
            }
            if (recruits.contains(centaur) && legion.numCreature(centaur) == 2)
            {
                return centaur;
            }
            if (recruits.contains(gargoyle)
                && legion.numCreature(gargoyle) == 1
                && oracle.creatureAvailable(cyclops) >= 3)
            {
                return gargoyle;
            }
            if (recruits.contains(ogre) && legion.numCreature(ogre) == 1
                && oracle.creatureAvailable(troll) >= 2)
            {
                return ogre;
            }
            if (recruits.contains(centaur) && legion.numCreature(centaur) == 1
                && oracle.creatureAvailable(lion) >= 2)
            {
                return centaur;
            }
            if (recruits.contains(gargoyle)
                && legion.numCreature(gargoyle) == 0
                && oracle.creatureAvailable(cyclops) >= 6)
            {
                return gargoyle;
            }
            if (recruits.contains(ogre) && legion.numCreature(ogre) == 0
                && oracle.creatureAvailable(troll) >= 6)
            {
                return ogre;
            }
            if (recruits.contains(centaur) && legion.numCreature(centaur) == 0
                && oracle.creatureAvailable(lion) >= 6)
            {
                return centaur;
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
                li.add(titan);
                li.add(gargoyle);
                li.add(centaur);
                li.add(centaur);
            }
            else
            {
                li.add(titan);
                li.add(gargoyle);
                li.add(gargoyle);
                li.add(ogre);
            }
        }
        else if (hex.getLabel().equals("200"))
        {
            li.add(titan);
            li.add(gargoyle);
            li.add(gargoyle);
            li.add(ogre);
        }
        else if (hex.getLabel().equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(titan);
                li.add(gargoyle);
                li.add(gargoyle);
                li.add(ogre);
            }
            else
            {
                li.add(titan);
                li.add(centaur);
                li.add(centaur);
                li.add(ogre);
            }
        }
        else if (hex.getLabel().equals("400"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(titan);
                li.add(gargoyle);
                li.add(ogre);
                li.add(ogre);
            }
            else
            {
                li.add(titan);
                li.add(gargoyle);
                li.add(gargoyle);
                li.add(centaur);
            }
        }
        else if (hex.getLabel().equals("500"))
        {
            li.add(titan);
            li.add(gargoyle);
            li.add(gargoyle);
            li.add(centaur);
        }
        else if (hex.getLabel().equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(titan);
                li.add(gargoyle);
                li.add(gargoyle);
                li.add(centaur);
            }
            else
            {
                li.add(titan);
                li.add(ogre);
                li.add(ogre);
                li.add(centaur);
            }
        }
        else
        {
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }
}
