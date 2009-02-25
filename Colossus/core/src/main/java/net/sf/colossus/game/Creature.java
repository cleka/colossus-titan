package net.sf.colossus.game;


import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;


/**
 * A particular creature in a game.
 * 
 * This represents a creature in a game, such as a specific Cyclops as part of
 * a legion.
 * 
 * It includes the state for battles, i.e. the damage taken so far.
 */
public class Creature
{
    private final CreatureType type;

    public Creature(CreatureType type)
    {
        this.type = type;
    }

    public CreatureType getType()
    {
        return type;
    }

    /** get the Striking Skill of this Creature when striking directly at
     * target undet the circumstances in parameters.
     * @param target The Creature that is struck by the current Creature
     * @param myElevation Height of the Hex on which stands the current Creature
     * @param targetElevation Height of the hex on which stands the target Creature
     * @param myHexTerrain Type of Hazard of the current Hex
     * @param targetHexTerrain Type of Hazard of the target hex
     * @param myHexside Type of hexside hazard between the current hex and the target hex
     * @param targetHexside Type of hexside hazard between the target hex and the current hex
     * @return The Skill Factor of the current Creature when all modifiers are factored in
     */
    public int getStrikingSkill(Creature target,
            int myElevation,
            int targetElevation,
            HazardTerrain myHexTerrain,
            HazardTerrain targetHexTerrain,
            HazardHexside myHexside,
            HazardHexside targetHexside)
    {
        CreatureType myType = this.getType();
        CreatureType targetType = target.getType();
        int attackerSkill = myType.getSkill();

        // Skill can be modified by terrain.
        // striking out of possible hazard
        attackerSkill -=
                myHexTerrain.getSkillPenaltyStrikeFrom(
                myType.isNativeIn(myHexTerrain));

        if (myElevation > targetElevation)
        {
            // Striking down across wall: +1
            if (myHexside.equals(HazardHexside.TOWER))
            {
                attackerSkill++;
            }
        }
        else if (myElevation < targetElevation)
        {
            // Non-native striking up slope: -1
            // Striking up across wall: -1
            if ((targetHexside.equals(HazardHexside.SLOPE) &&
                    !myType.isNativeSlope()) ||
                    targetHexside.equals(HazardHexside.TOWER))
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }
}
