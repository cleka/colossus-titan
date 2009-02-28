package Balrog;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.HintInterface;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import Default.DefaultHint;


public class BalrogHint extends DefaultHint
{
    public String getRecruitHint(MasterBoardTerrain terrain,
        LegionClientSide legion, List<CreatureType> recruits,
        HintOracleInterface oracle, String[] section)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = creaturesToStrings(recruits);
        List<String> sect = Arrays.asList(section);

        if (terrainId.equals("Tower")) {
            for (String n : recruitNames) {
                if (n.startsWith("Balrog")) {
                    return n;
                }
            }
        }

        return super.getRecruitHint(terrain, legion, recruits, oracle, section);
    }
}
