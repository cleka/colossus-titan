package net.sf.colossus.game;


import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;


/**
 * A particular creature in a game.
 *
 * This represents a creature in a game, such as a specific Cyclops as part of
 * a legion.
 *
 * TODO it should include the state for battles, i.e. the damage taken so far.
 *      Currently that happens only on the server side.
 */
public class Creature
{
    private static final Logger LOGGER = Logger.getLogger(Creature.class
        .getName());
    private final CreatureType type;

    public Creature(CreatureType type)
    {
        this.type = type;
    }

    public CreatureType getType()
    {
        return type;
    }

    /**
     * Calculates the Striking Power of this Creature when striking directly at
     * target under the circumstances in parameters.
     *
     * @param target The Creature that is struck by the current Creature
     * @param myElevation Height of the Hex on which stands the current Creature
     * @param targetElevation Height of the hex on which stands the target Creature
     * @param myHexTerrain Type of Hazard of the current Hex
     * @param targetHexTerrain Type of Hazard of the target hex
     * @param myHexside Type of hexside hazard between the current hex and the target hex
     * @param targetHexside Type of hexside hazard between the target hex and the current hex
     * @return The Power Factor of the current Creature when all modifiers are factored in
     */
    public int getStrikingPower(Creature target, int myElevation,
        int targetElevation, HazardTerrain myHexTerrain,
        HazardTerrain targetHexTerrain, HazardHexside myHexside,
        HazardHexside targetHexside)
    {
        CreatureType myType = this.getType();
        CreatureType targetType = target.getType();
        int dice = type.getPower();

        // Dice can be modified by terrain.
        dice += myHexTerrain.getPowerBonusStrikeFrom(myType
            .isNativeIn(myHexTerrain), targetType.isNativeIn(myHexTerrain));

        // Native striking down a dune hexside: +2
        if (myHexside.equals(HazardHexside.DUNE) && myType.isNativeDune())
        {
            dice += 2;
        }
        // Native striking down a slope hexside: +1
        else if (myHexside.equals(HazardHexside.SLOPE)
            && myType.isNativeSlope())
        {
            dice++;
        }
        // Non-native striking up a dune hexside: -1
        else if (!myType.isNativeDune()
            && targetHexside.equals(HazardHexside.DUNE))
        {
            dice--;
        }
        LOGGER.finest("Found " + dice + " dice for " + myType.getName() + " ["
            + myHexTerrain.getName() + "  " + myElevation + ", "
            + myHexside.getName() + " ] vs. " + targetType.getName() + " ["
            + targetHexTerrain.getName() + " " + targetElevation + ", "
            + targetHexside.getName() + " ]");
        return dice;
    }

    /**
     * Calculates the Striking Skill of this Creature when striking directly at
     * target under the circumstances in parameters.
     *
     * @param target The Creature that is struck by the current Creature
     * @param myElevation Height of the Hex on which stands the current Creature
     * @param targetElevation Height of the hex on which stands the target Creature
     * @param myHexTerrain Type of Hazard of the current Hex
     * @param targetHexTerrain Type of Hazard of the target hex
     * @param myHexside Type of hexside hazard between the current hex and the target hex
     * @param targetHexside Type of hexside hazard between the target hex and the current hex
     * @return The Skill Factor of the current Creature when all modifiers are factored in
     */
    public int getStrikingSkill(Creature target, int myElevation,
        int targetElevation, HazardTerrain myHexTerrain,
        HazardTerrain targetHexTerrain, HazardHexside myHexside,
        HazardHexside targetHexside)
    {
        CreatureType myType = this.getType();
        CreatureType targetType = target.getType();
        int attackerSkill = myType.getSkill();

        // Skill can be modified by terrain.
        // striking out of possible hazard
        attackerSkill -= myHexTerrain.getSkillPenaltyStrikeFrom(myType
            .isNativeIn(myHexTerrain), targetType.isNativeIn(myHexTerrain));

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
            if ((targetHexside.equals(HazardHexside.SLOPE) && !myType
                .isNativeSlope())
                || targetHexside.equals(HazardHexside.TOWER))
            {
                attackerSkill--;
            }
        }
        LOGGER.finest("Found skill " + attackerSkill + "  for "
            + myType.getName() + " [" + myHexTerrain.getName() + "  "
            + myElevation + ", " + myHexside.getName() + " ] vs. "
            + targetType.getName() + " [" + targetHexTerrain.getName() + " "
            + targetElevation + ", " + targetHexside.getName() + " ]");
        return attackerSkill;
    }
}
