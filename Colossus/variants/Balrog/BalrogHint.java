package Balrog;


import java.util.List;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IHintOracle;
import net.sf.colossus.variant.IOracleLegion;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.Variant;
import Default.DefaultHint;


public class BalrogHint extends DefaultHint
{
    public BalrogHint(Variant variant)
    {
        super(variant);
    }

    @Override
    public String getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle,
        List<AIStyle> aiStyles)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = creaturesToStrings(recruits);
        // TODO: Is this "sect" needed / planned to be needed for something?
        // List<String> sect = Arrays.asList(section);

        if (terrainId.equals("Tower"))
        {
            for (String n : recruitNames)
            {
                if (n.startsWith("Balrog"))
                {
                    return n;
                }
            }
        }

        return super
            .getRecruitHint(terrain, legion, recruits, oracle,
            aiStyles);
    }
}
