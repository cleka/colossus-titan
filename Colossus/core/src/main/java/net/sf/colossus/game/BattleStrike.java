package net.sf.colossus.game;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.BattleClientSide;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;


public class BattleStrike
{
    private static final Logger LOGGER = Logger.getLogger(BattleStrike.class
        .getName());

    private final Game game;

    public BattleStrike(Game game)
    {
        this.game = game;
        LOGGER.finest(("BattleStrike instantiated."));
    }

    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain.
     *  WARNING: this is duplicated in CreatureServerSide
     *  (moved from Strike to here)
     * @param striker TODO
     * @param target TODO
     */
    public int getDice(BattleCritter striker, BattleCritter target)
    {
        BattleHex hex = striker.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();
        // TODO when BattleCritter / BattleUnit would extend Creature,
        // could ask directly instead of this helper variable
        CreatureType strikerCreType = striker.getType();

        int dice = striker.getPower();

        boolean rangestrike = !getBattle().isInContact(striker, true);
        HazardTerrain terrain = hex.getTerrain();
        if (rangestrike)
        {
            // Divide power in half, rounding down.
            dice /= 2;

            // volcanoNative rangestriking from volcano: +2
            if (terrain.equals(HazardTerrain.VOLCANO)
                && strikerCreType.isNativeIn(terrain))
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // volcanoNative striking from volcano: +2
            if (terrain.equals(HazardTerrain.VOLCANO)
                && strikerCreType.isNativeIn(terrain))
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = Battle.getDirection(hex, targetHex, false);
            HazardHexside hazard = hex.getHexsideHazard(direction);

            // Native striking down a dune hexside: +2
            if (hazard == HazardHexside.DUNE && strikerCreType.isNativeDune())
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hazard == HazardHexside.SLOPE
                && strikerCreType.isNativeSlope())
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (!strikerCreType.isNativeDune()
                && hex.getOppositeHazard(direction) == HazardHexside.DUNE)
            {
                dice--;
            }
        }

        return dice;
    }

    /** WARNING: this is duplicated in CreatureServerSide
     *  (moved from Strike to here)
     * @param striker TODO
     * @param target TODO
     */
    @SuppressWarnings("deprecation")
    public int getAttackerSkill(BattleCritter striker, BattleCritter target)
    {
        BattleHex hex = striker.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = striker.getSkill();

        boolean rangestrike = !getBattle().isInContact(striker, true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            HazardTerrain terrain = hex.getTerrain();
            // striking out of possible hazard
            attackerSkill -= hex.getTerrain().getSkillPenaltyStrikeFrom(
                striker.getType().isNativeIn(terrain),
                target.getType().isNativeIn(terrain));

            if (hex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = BattleClientSide.getDirection(hex, targetHex,
                    false);
                // TODO the hexside should be called WALL...
                // Striking down across wall: +1
                if (hex.getHexsideHazard(direction) == HazardHexside.TOWER)
                {
                    attackerSkill++;
                }
            }
            else if (hex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = BattleClientSide.getDirection(targetHex, hex,
                    false);
                HazardHexside hazard = targetHex.getHexsideHazard(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                // TODO Tower vs. Wall ...
                if ((hazard == HazardHexside.SLOPE && !striker.getType()
                    .isNativeSlope()) || hazard == HazardHexside.TOWER)
                {
                    attackerSkill--;
                }
            }
        }
        else if (!striker.getType().useMagicMissile())
        {
            // Range penalty
            int range = Battle.getRange(hex, targetHex, false);
            if (range >= 4)
            {
                attackerSkill -= (range - 3);
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!striker.getType().isNativeIn(HazardTerrain.BRAMBLES))
            {
                attackerSkill -= getBattle().countBrambleHexes(hex, targetHex);
            }

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
                    attackerSkill -= heightDeficit;
                }
            }

            // Rangestrike into volcano: -1
            if (targetHex.getTerrain().equals(HazardTerrain.VOLCANO))
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }

    /** WARNING: this is duplicated in CreatureServerSide
     *  (moved from Strike to here)
     * @param striker TODO
     * @param target TODO
     */
    public int getStrikeNumber(BattleCritter striker, BattleCritter target)
    {
        boolean rangestrike = !getBattle().isInContact(striker, true);

        int attackerSkill = getAttackerSkill(striker, target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        HazardTerrain terrain = target.getCurrentHex().getTerrain();

        if (!rangestrike)
        {
            // Strike number can be modified directly by terrain.
            strikeNumber += terrain.getSkillBonusStruckIn(striker.getType()
                .isNativeIn(terrain), target.getType().isNativeIn(terrain));
        }
        else
        {
            // Native defending in bramble, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.BRAMBLES)
                && target.getType().isNativeIn(HazardTerrain.BRAMBLES)
                && !striker.getType().isNativeIn(HazardTerrain.BRAMBLES)
                && !striker.getType().useMagicMissile())
            {
                strikeNumber++;
            }

            // Native defending in stone, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.STONE)
                && target.getType().isNativeIn(HazardTerrain.STONE)
                && !striker.getType().isNativeIn(HazardTerrain.STONE)
                && !striker.getType().useMagicMissile())
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

    public int determineProbabilityBasedHits(Creature striker,
        Creature target, int dice, int strikeNumber, List<String> rolls)
    {
        // for logging
        StringBuilder rollString = new StringBuilder(36);

        /*
        sn 6:       1/6    6-6+1 = 1
        sn 5:       2/6    6-5+1 = 2
        sn 4:       3/6    6-4+1 = 3
        sn 3:       4/6    6-3+1 = 4
        sn 2:       5/6    6-2+1 = 5
        sn 1:       6/6    6-1+1 = 6

        sn N: (6-N+1)/6    6-N+1 => 7.0-N
        */

        int damage = 0;

        double expected = dice * (7.0 - strikeNumber) / 6.0;
        int calculatedMin = (int)Math.floor(expected);
        damage = calculatedMin;

        // 0 < wastedLuck < 1
        double wastedLuck = expected - calculatedMin;

        /*
         * effective accumulated wasted luck, for logging;
         * applyAWL() will fill in the content string.
         */
        StringBuffer eawlString = new StringBuffer();
        String bonus = "no bonus";

        boolean getsOne = striker.getLegion().getPlayer()
            .applyAccumulatedWastedLuck(strikeNumber, wastedLuck, eawlString);
        if (getsOne)
        {
            damage++;
            bonus = "bonus +1";
        }

        String dispName = striker.getType().getName();
        if (dispName.length() > 4)
        {
            dispName = dispName.substring(0, 4);
        }

        if (LOGGER.isLoggable(Level.FINEST))
        {
            String result = String.format(
                "%-4s rolls %2d dice for %1d's, results in %2d hits "
                    + "(exp. %5.2f =>%2d, %8s: EAWL=%s)", dispName,
                Integer.valueOf(dice), Integer.valueOf(strikeNumber),
                Integer.valueOf(damage), Double.valueOf(expected),
                Integer.valueOf(calculatedMin), bonus, eawlString.toString());
            LOGGER.log(Level.FINEST, result);
        }

        // create a fakeList, damage times strikeNumer and rest "1"s.
        for (int i = 0; i < dice; i++)
        {
            int roll = (i < damage ? strikeNumber : 1);
            rolls.add("" + roll);
            rollString.append(roll);
        }

        LOGGER.log(Level.INFO,
            striker.getName() + " in " + striker.getCurrentHex() + " strikes "
                + target.getDescription() + " with strike number "
                + strikeNumber + ", probabilityBased damage is " + damage
                + (damage == 1 ? " hit" : " hits (fake rolls: " + rollString));

        return damage;
    }

    public int rollDice(Creature striker, Creature target, int dice,
        int strikeNumber, List<String> rolls, boolean randomized)
    {
        // for logging
        StringBuilder rollString = new StringBuilder(36);

        int damage = 0;

        // usual rolling, OR roll according to sequence
        for (int i = 0; i < dice; i++)
        {
            int roll = (randomized ? rollPlayersBattleDice(striker) : Dice
                .rollDieNonRandom());
            rolls.add("" + roll);
            rollString.append(roll);

            if (roll >= strikeNumber)
            {
                damage++;
            }
        }

        LOGGER.log(Level.INFO,
            striker.getName() + " in " + striker.getCurrentHex() + " strikes "
                + target.getDescription() + " with strike number "
                + strikeNumber + ", rolling: " + rollString + ": " + damage
                + (damage == 1 ? " hit" : " hits"));

        return damage;
    }

    /**
     * @param striker The creature that strikes
     * @return a battle roll
     */

    private int rollPlayersBattleDice(Creature striker)
    {
        return striker.getPlayer().makeBattleRoll();
    }

    // Helper method
    public Battle getBattle()
    {
        return game.getBattle();
    }

}
