package DinoTitan;


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


@SuppressWarnings("unused")
public class DinoTitanHint extends AbstractHintProvider
{
    private final CreatureType allosaur;
    private final CreatureType ankylosaur;
    private final CreatureType archaopteryx;
    private final CreatureType ceratopsian;
    private final CreatureType ceratosaurus;
    private final CreatureType crocodile;
    private final CreatureType deinosuchus;
    private final CreatureType eoraptor;
    private final CreatureType flightlessbird;
    private final CreatureType guardian;
    private final CreatureType golddragon;
    private final CreatureType pachycephalosaur;
    private final CreatureType pachyrhinosaurus;
    private final CreatureType prosauropod;
    private final CreatureType pteranodon;
    private final CreatureType pterodactyl;
    private final CreatureType quetzalcoatlas;
    private final CreatureType reddragon;
    private final CreatureType sauropod;
    private final CreatureType serpent;
    private final CreatureType silverdragon;
    private final CreatureType snake;
    private final CreatureType spinosaur;
    private final CreatureType spittingcobra;
    private final CreatureType stegosaur;
    private final CreatureType titan;
    private final CreatureType triceratops;
    private final CreatureType tyrannosaur;
    private final CreatureType velociraptor;
    private final CreatureType warlock;
    private final CreatureType wingedserpent;
    private final CreatureType wyvern;

    public DinoTitanHint(Variant variant)
    {
        super(variant);
        this.allosaur = getCreatureType("Allosaur");
        this.ankylosaur = getCreatureType("Ankylosaur");
        this.archaopteryx = getCreatureType("Archaopteryx");
        this.ceratopsian = getCreatureType("Ceratopsian");
        this.ceratosaurus = getCreatureType("Ceratosaurus");
        this.crocodile = getCreatureType("Crocodile");
        this.deinosuchus = getCreatureType("Deinosuchus");
        this.eoraptor = getCreatureType("Eoraptor");
        this.flightlessbird = getCreatureType("FlightlessBird");
        this.guardian = getCreatureType("Guardian");
        this.golddragon = getCreatureType("GoldDragon");
        this.pachycephalosaur = getCreatureType("Pachycephalosaur");
        this.pachyrhinosaurus = getCreatureType("Pachyrhinosaurus");
        this.prosauropod = getCreatureType("Prosauropod");
        this.pteranodon = getCreatureType("Pteranodon");
        this.pterodactyl = getCreatureType("Pterodactyl");
        this.quetzalcoatlas = getCreatureType("Quetzalcoatlas");
        this.reddragon = getCreatureType("RedDragon");
        this.sauropod = getCreatureType("Sauropod");
        this.serpent = getCreatureType("Serpent");
        this.silverdragon = getCreatureType("SilverDragon");
        this.snake = getCreatureType("Snake");
        this.spinosaur = getCreatureType("Spinosaur");
        this.spittingcobra = getCreatureType("SpittingCobra");
        this.stegosaur = getCreatureType("Stegosaur");
        this.titan = getCreatureType("Titan");
        this.triceratops = getCreatureType("Triceratops");
        this.tyrannosaur = getCreatureType("Tyrannosaur");
        this.velociraptor = getCreatureType("Velociraptor");
        this.warlock = getCreatureType("Warlock");
        this.wingedserpent = getCreatureType("WingedSerpent");
        this.wyvern = getCreatureType("Wyvern");
    }

    private final DevRandom rnd = new DevRandom();

    public CreatureType getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle,
        List<AIStyle> aiStyles)
    {
        String terrainId = terrain.getId();
        if (terrainId.equals("Mountains"))
        {
            // If less than two ceratosaurus, get one
            if (recruits.contains(ceratosaurus)
                && legion.numCreature(ceratosaurus) < 2)
            {
                return ceratosaurus;
            }

            // If have two, get a third if can get to wasteland
            // where three recruits an allosaur
            if (recruits.contains(ceratosaurus)
                && legion.numCreature(ceratosaurus) == 2
                && oracle.canReach("Wasteland"))
            {
                return ceratosaurus;
            }
            //  If don't have velociraptors yet, get them
            if (recruits.contains(velociraptor)
                && oracle.creatureAvailable(ceratosaurus) >= 2
                && legion.numCreature(velociraptor) < 2)
            {
                return velociraptor;
            }

            // Consider a third eoraptor so can get a triceratops
            if (aiStyles.contains(AIStyle.Defensive))
            {
               if (recruits.contains(eoraptor) && legion.numCreature(eoraptor) == 2
                   && !legion.contains(triceratops) && legion.getHeight() < 6
                   && (oracle.biggestAttackerHeight() == 0
                      || (oracle.biggestAttackerHeight() + 2 <= legion.getHeight()))
                   && oracle.canReach("Jungle"))
                   return eoraptor;
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
               if (recruits.contains(eoraptor) && legion.numCreature(eoraptor) == 2
                   && !legion.contains(triceratops) && legion.getHeight() <=2
                   && (oracle.biggestAttackerHeight() == 0
                      || (legion.getHeight() + 2 <= oracle.biggestAttackerHeight()))
                   && oracle.canReach("Jungle"))
                   return eoraptor;
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
            if (recruits.contains(pachycephalosaur) && legion.numCreature(pachycephalosaur) == 1
                && ((oracle.creatureAvailable(ceratopsian) >= 2) ||
                    (oracle.creatureAvailable(ceratosaurus) >= 3)))
            {
                return pachycephalosaur;
            }
            if (recruits.contains(eoraptor)
                && legion.numCreature(eoraptor) == 1
                && oracle.creatureAvailable(velociraptor) >= 2)
            {
                return eoraptor;
            }
            if (recruits.contains(pterodactyl) && legion.numCreature(pterodactyl) == 1
                && oracle.creatureAvailable(pteranodon) >= 2)
            {
                return pterodactyl;
            }
            if (recruits.contains(pachycephalosaur) && legion.numCreature(pachycephalosaur) == 0
                && ((oracle.creatureAvailable(ceratopsian) >= 6) ||
                    (oracle.creatureAvailable(ceratosaurus) >= 6)))
            {
                return pachycephalosaur;
            }
            if (recruits.contains(pterodactyl)
                && legion.numCreature(pterodactyl) == 0
                && oracle.creatureAvailable(pteranodon) >= 6)
            {
                return pterodactyl;
            }
            if (recruits.contains(eoraptor) && legion.numCreature(eoraptor) == 0
                && oracle.creatureAvailable(velociraptor) >= 6)
            {
                return eoraptor;
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
                li.add(pterodactyl);
                li.add(pterodactyl);
                li.add(eoraptor);
            }
            else
            {
                li.add(titan);
                li.add(pterodactyl);
                li.add(pterodactyl);
                li.add(pachycephalosaur);
            }
        }
        else if (hex.getLabel().equals("200"))
        {
            li.add(titan);
            li.add(pterodactyl);
            li.add(pterodactyl);
            li.add(pachycephalosaur);
        }
        else if (hex.getLabel().equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(titan);
                li.add(eoraptor);
                li.add(eoraptor);
                li.add(pterodactyl);
            }
            else
            {
                li.add(titan);
                li.add(eoraptor);
                li.add(eoraptor);
                li.add(pachycephalosaur);
            }
        }
        else if (hex.getLabel().equals("400"))
        {
            li.add(titan);
            li.add(pterodactyl);
            li.add(pachycephalosaur);
            li.add(pachycephalosaur);
        }
        else if (hex.getLabel().equals("500"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(titan);
                li.add(pterodactyl);
                li.add(pachycephalosaur);
                li.add(pachycephalosaur);
            }
            else
            {
                li.add(titan);
                li.add(eoraptor);
                li.add(eoraptor);
                li.add(pterodactyl);
            }
        }
        else if (hex.getLabel().equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(titan);
                li.add(pachycephalosaur);
                li.add(pachycephalosaur);
                li.add(pterodactyl);
            }
            else
            {
                li.add(titan);
                li.add(pachycephalosaur);
                li.add(pachycephalosaur);
                li.add(eoraptor);
            }
        }
        else
        {
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }
}
