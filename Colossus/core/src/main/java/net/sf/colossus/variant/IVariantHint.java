package net.sf.colossus.variant;


import java.util.List;


/**
 * Interface for the use of AI Hints.
 *
 * @author Romain Dolbeau
 */
public interface IVariantHint
{
    enum AIStyle
    {
        Any, Offensive, Defensive
    }

    /**
     * Give the suggested recruit in this terrain for this legion.
     * @param terrain Type of terrain
     * @param legion The recruiting legion
     * @param recruits List of all recruitable Creature
     *     (returned value must be the name of one of those)
     * @param oracle An oracle that answers some questions
     *     about the situation of the legion
     * @param aiStyles The styles the AI prefers to play
     * @return The suggested recruit type, a null means recruiting should be
     *         skipped.
     */
    public CreatureType getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle,
        List<IVariantHint.AIStyle> aiStyles);

    /**
     * To obtain the list of creature to split on turn one.
     * @param startingTower The starting Tower.
     * @param aiStyles The style the AI prefers to play.
     * @return The list of creature to split (listed by name)
     */
    public List<String> getInitialSplitHint(MasterHex startingTower,
        List<IVariantHint.AIStyle> aiStyles);

    /**
     * Give an offset to apply to the creature Point Value
     *     before estimating its recruitment value.
     * @param creature Type of the creature
     * @param style List of AI styles to hint for
     * @return An offset to the Point Value of the creature.
     */
    public int getHintedRecruitmentValueOffset(CreatureType creature,
        List<AIStyle> styles);
}
