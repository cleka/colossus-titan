package net.sf.colossus.server;

import net.sf.colossus.client.LegionInfo;
import java.util.List;

/**
 * Interface for the use of AI Hints.
 * @version $Id$
 * @author Romain Dolbeau
 */
public interface HintInterface
{
    public String getRecruitHint(char terrain,
                                 LegionInfo legion,
                                 List recruits,
                                 HintOracleInterface oracle,
                                 String[] section);
    public List getInitialSplitHint(String label,
                                    String[] section);
    public int getHintedRecruitmentValueOffset(String name,
                                               String[] section);    
}
