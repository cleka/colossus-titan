package ExtTitan;

import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.HintOracleInterface;
import java.util.List;

public class ExtTitanHint implements net.sf.colossus.server.HintInterface
{
    public String getRecruitHint(char terrain,
                                 LegionInfo legion,
                                 List recruits,
                                 HintOracleInterface oracle,
                                 String[] section)
    {
        return (String)recruits.get(recruits.size() - 1);
    }
    public List getInitialSplitHint(String label,
                                    String[] section)
    {
        return null;
    }
    public int getHintedRecruitmentValueOffset(String name,
                                               String[] section)
    {
        return 0;
    }   
}
