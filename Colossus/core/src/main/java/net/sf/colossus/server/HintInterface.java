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

    /**
     * Give the suggested recruit in this terrain for this legion.
     * @param terrain Type of terrain
     * @param legion The recruiting legion
     * @param recruits List of all recruitable Creature 
     *     (returned value must be the name of one of those)
     * @param oracle An oracle that answers some questions 
     *     about the situation of the legion
     * @param section Array of AI section to be used 
     *     (usually one or more of "AllAI:", "DefensiveAI:", "OffensiveAI:")
     * @return The name os the suggested recruit
     */
    public String getRecruitHint(String terrain,
        LegionInfo legion,
        List recruits,
        HintOracleInterface oracle,
        String[] section);

    /**
     * To obtain the list of creature to split on turn one.
     * @param label Label of the starting Tower.
     * @param section Array of AI section to be used 
     *     (usually one or more of "AllAI:", "DefensiveAI:", "OffensiveAI:")
     * @return The list of creature to split (listed by name)
     */
    public List getInitialSplitHint(String label,
        String[] section);

    /**
     * Give an offset to apply to the creature Point Value 
     *     before estimating its recruitment value.
     * @param name Name of the Creature
     * @param section Array of AI section to be used 
     *     (usually one or more of "AllAI:", "DefensiveAI:", "OffensiveAI:")
     * @return An offset to the Point Value of the creature.
     */
    public int getHintedRecruitmentValueOffset(String name,
        String[] section);
}
