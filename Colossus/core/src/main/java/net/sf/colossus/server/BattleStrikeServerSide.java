package net.sf.colossus.server;


import java.util.logging.Logger;

import net.sf.colossus.game.Battle;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Game;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;


public class BattleStrikeServerSide
{
    private static final Logger LOGGER = Logger
        .getLogger(BattleStrikeServerSide.class.getName());

    private final Game game;

    public BattleStrikeServerSide(Game game)
    {
        this.game = game;

        LOGGER.finest("BattleStrikeServerSide instantiated.");
    }

    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain.
     * WARNING: this is currently still duplicated in game.BattleStrike
     * @param striker TODO
     * @param target TODO
     */
    protected int getDice(CreatureServerSide striker, Creature target)
    {
        assert getBattle() != null : "getDice called when there is no battle!";
        return getDice(striker, target, !getBattle()
            .isInContact(striker, true));
    }

    /** WARNING: this is currently still duplicated in game.BattleStrike
     */
    public int getDice(Creature striker, Creature target, boolean rangestrike)
    {
        BattleHex hex = striker.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int dice = striker.getPower();

        if (rangestrike)
        {
            // Divide power in half, rounding down.
            dice /= 2;

            // volcanoNative rangestriking from volcano: +2
            if (striker.isNativeIn(HazardTerrain.VOLCANO)
                && hex.getTerrain().equals(HazardTerrain.VOLCANO))
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // volcanoNative striking from volcano: +2
            if (striker.isNativeIn(HazardTerrain.VOLCANO)
                && hex.getTerrain().equals(HazardTerrain.VOLCANO))
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = Battle.getDirection(hex, targetHex, false);
            HazardHexside hazard = hex.getHexsideHazard(direction);

            // Native striking down a dune hexside: +2
            if (hazard == HazardHexside.DUNE
                && striker.isNativeAt(HazardHexside.DUNE))
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hazard == HazardHexside.SLOPE
                && striker.isNativeAt(HazardHexside.SLOPE))
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (!striker.isNativeAt(HazardHexside.DUNE)
                && hex.getOppositeHazard(direction) == HazardHexside.DUNE)
            {
                dice--;
            }

            /* TODO: remove TEST TEST TEST TEST TEST */
            /* getStrikingPower should be used instead of the logic above, but
             * 1) I'm not sure everyone will agree it belongs in Creature
             * 2) I haven't had time to verify it's correct.
             * Incidentally, if you're reading this after noticing the warning
             * below in your logfile, then it isn't correct ;-)
             */
            int checkStrikingPower = striker.getStrikingPower(target, hex
                .getElevation(), targetHex.getElevation(), hex.getTerrain(),
                targetHex.getTerrain(), hex.getHexsideHazard(BattleServerSide
                    .getDirection(hex, targetHex, false)), targetHex
                    .getHexsideHazard(BattleServerSide.getDirection(targetHex,
                        hex, false)));

            if (checkStrikingPower != dice)
            {
                LOGGER.warning("attackerPower says " + dice
                    + " but checkStrikingPower says " + checkStrikingPower);
            }
            /* END TODO: remove TEST TEST TEST TEST TEST */
        }

        return dice;
    }

    /** WARNING: this is duplicated in BattleClientSide
     * @param striker TODO
     * @param target TODO
     * @param rangestrike TODO*/
    @SuppressWarnings("deprecation")
    int getAttackerSkill(Creature striker, Creature target, boolean rangestrike)
    {
        // rangestrike calc depends on countBrambleHexes which needs battle.
        // (it's called when there is no battle by ShowCreatureDetails)
        assert getBattle() != null || !rangestrike : "getAttackerSkill "
            + "called for rangestrike when there is no battle!";

        BattleHex hex = striker.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = striker.getSkill();

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            // striking out of possible hazard
            attackerSkill -= hex.getTerrain().getSkillPenaltyStrikeFrom(
                striker.isNativeIn(hex.getTerrain()),
                target.isNativeIn(hex.getTerrain()));

            if (hex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = BattleServerSide.getDirection(hex, targetHex,
                    false);
                HazardHexside hazard = hex.getHexsideHazard(direction);

                // Striking down across wall: +1
                if (hazard.equals(HazardHexside.TOWER))
                {
                    attackerSkill++;
                }
            }
            else if (hex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = BattleServerSide.getDirection(targetHex, hex,
                    false);
                HazardHexside hazard = targetHex.getHexsideHazard(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                if ((hazard.equals(HazardHexside.SLOPE) && !striker
                    .isNativeAt(HazardHexside.SLOPE))
                    || hazard.equals(HazardHexside.TOWER))
                {
                    attackerSkill--;
                }
            }
            /* TODO: remove TEST TEST TEST TEST TEST */
            /* getStrikingSkill should be used instead of the logic above, but
             * 1) I'm not sure everyone will agree it belongs in Creature
             * 2) I haven't had time to verify it's correct.
             * Incidentally, if you're reading this after noticing the warning
             * below in your logfile, then it isn't correct ;-)
             */
            int checkStrikingSkill = striker.getStrikingSkill(target, hex
                .getElevation(), targetHex.getElevation(), hex.getTerrain(),
                targetHex.getTerrain(), hex.getHexsideHazard(BattleServerSide
                    .getDirection(hex, targetHex, false)), targetHex
                    .getHexsideHazard(BattleServerSide.getDirection(targetHex,
                        hex, false)));

            if (checkStrikingSkill != attackerSkill)
            {
                LOGGER
                    .warning(String
                        .format(
                            "For creature %s striking %s from %s(%d) to %s(%d) via %s/%s, "
                                + "we calculated %d as attacker skill, but getStrikingSkill says %d",
                            striker, target, hex.getTerrain(), Integer
                                .valueOf(hex.getElevation()), targetHex
                                .getTerrain(), Integer.valueOf(targetHex
                                .getElevation()), hex
                                .getHexsideHazard(BattleServerSide
                                    .getDirection(hex, targetHex, false)),
                            targetHex.getHexsideHazard(BattleServerSide
                                .getDirection(targetHex, hex, false)), Integer
                                .valueOf(attackerSkill), Integer
                                .valueOf(checkStrikingSkill)));
            }
            /* END TODO: remove TEST TEST TEST TEST TEST */
        }
        else if (!striker.useMagicMissile())
        {
            // Range penalty
            /* Range 4 means a penalty of 1 to the attacker.
             * I hereby extend this so that range 5 means a penalty of 2,
             * and so one, for creature with higher skill.
             */
            int range = BattleServerSide.getRange(hex, targetHex, false);
            if (range >= 4)
            {
                attackerSkill -= (range - 3);
            }
            int bramblesPenalty = 0;
            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!striker.isNativeIn(HazardTerrain.BRAMBLES))
            {
                bramblesPenalty += getBattle().countBrambleHexes(
                    striker.getCurrentHex(), targetHex);
            }
            /* TODO: remove TEST TEST TEST TEST TEST */
            /* computeSkillPenaltyRangestrikeThrough should be used instead of the logic above, but
             * 1) I'm not sure everyone will agree it belongs in Battle
             * 2) I haven't had time to verify it's correct.
             * Incidentally, if you're reading this after noticing the warning
             * below in your logfile, then it isn't correct ;-)
             */
            int interveningPenalty = getBattle()
                .computeSkillPenaltyRangestrikeThrough(
                    striker.getCurrentHex(), targetHex, striker);
            if (interveningPenalty != bramblesPenalty)
            {
                LOGGER.warning("bramblesPenalty says " + bramblesPenalty
                    + " but interveningPenalty says " + interveningPenalty);
            }
            /* END TODO: remove TEST TEST TEST TEST TEST */

            attackerSkill -= bramblesPenalty;

            // Rangestrike up across wall: -1 per wall
            if (targetHex.hasWall())
            {
                int heightDeficit = targetHex.getElevation()
                    - hex.getElevation();
                if (heightDeficit > 0)
                {
                    // Because of the design of the tower map, a strike to
                    // a higher tower hex always crosses one wall per
                    // elevation difference.
                    /* actually this need some better logic, as some Wall are
                     * in a completely different patterns that the Tower
                     * nowaday
                     */
                    attackerSkill -= heightDeficit;
                }
            }

            // Rangestrike into volcano: -1
            /* actually, it's only for native ... but then non-native are
             * blocked. Anyway this will should to HazardTerrain someday.
             */
            if (targetHex.getTerrain().equals(HazardTerrain.VOLCANO))
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }

    /** WARNING: this is duplicated in BattleClientSide
     * @param striker TODO
     * @param target TODO*/
    public int getStrikeNumber(CreatureServerSide striker, Creature target)
    {
        return getStrikeNumber(striker, target,
            !getBattle().isInContact(striker, true));
    }

    public int getStrikeNumber(Creature striker, Creature target,
        boolean rangestrike)
    {
        int attackerSkill = getAttackerSkill(striker, target, rangestrike);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        HazardTerrain terrain = target.getCurrentHex().getTerrain();

        if (!rangestrike)
        {
            // Strike number can be modified directly by terrain.
            strikeNumber += terrain.getSkillBonusStruckIn(
                striker.isNativeIn(terrain), target.isNativeIn(terrain));
        }
        else
        {
            // Native defending in bramble, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.BRAMBLES)
                && target.isNativeIn(HazardTerrain.BRAMBLES)
                && !striker.isNativeIn(HazardTerrain.BRAMBLES)
                && !striker.useMagicMissile())
            {
                strikeNumber++;
            }

            // Native defending in stone, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.STONE)
                && target.isNativeIn(HazardTerrain.STONE)
                && !striker.isNativeIn(HazardTerrain.STONE)
                && !striker.useMagicMissile())
            {
                strikeNumber++;
            }
        }

        // Sixes always hit.
        if (strikeNumber > 6)
        {
            strikeNumber = 6;
        }

        return strikeNumber;
    }

    private Battle getBattle()
    {
        return game.getBattle();
    }
}
