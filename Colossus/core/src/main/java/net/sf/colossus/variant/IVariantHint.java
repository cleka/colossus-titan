package net.sf.colossus.variant;


import java.util.List;


/**
 * Interface for the use of AI Hints.
 *
 * @author Romain Dolbeau
 */
public interface IVariantHint
{
    /**
     * The style of play the AI should prefer.
     *
     * This can be used to make an AI prefer recruits that are geared
     * towards a certain style of play, e.g. recruiting strong attackers
     * for an offensive AI or a more long-term recruitment strategy for
     * AIs that play more defensively (and thus don't get involved in
     * battles as soon).
     */
    enum AIStyle
    {
        /**
         * Default value if no particular play style is preferred.
         */
        Any,

        /**
         * Marks a preference for offensive recruiting, i.e. strong
         * creatures are usually preferred over long-term recruiting
         * strategies.
         */
        Offensive,

        /**
         * Marks a defensive play, with the assumption that battles
         * are often avoided. Thus long-term recruiting strategies are
         * important.
         */
        Defensive
    }

    /**
     * Give the suggested recruit in this terrain for this legion.
     *
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
        IOracleLegion legion, List<CreatureType> recruits, IHintOracle oracle,
        List<AIStyle> aiStyles);

    /**
     * Obtains the list of creatures to split on turn one.
     *
     * @param startingTower The starting Tower.
     * @param aiStyles The style the AI prefers to play.
     * @return The list of creatures to split.
     */
    public List<CreatureType> getInitialSplitHint(MasterHex startingTower,
        List<AIStyle> aiStyles);

    /**
     * Give an offset to apply to the creature Point Value before
     * estimating its recruitment value.
     *
     * @param creature Type of the creature
     * @param styles List of AI styles to hint for
     * @return An offset to the Point Value of the creature.
     */
    public int getHintedRecruitmentValueOffset(CreatureType creature,
        List<AIStyle> styles);
}
