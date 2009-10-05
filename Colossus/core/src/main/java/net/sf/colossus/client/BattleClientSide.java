package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.CompareDoubles;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterHex;


public class BattleClientSide extends Battle
{
    private static final Logger LOGGER = Logger
        .getLogger(BattleClientSide.class.getName());

    private BattlePhase battlePhase;
    private int battleTurnNumber = -1;
    private Player battleActivePlayer;

    private final List<BattleUnit> battleUnits = new ArrayList<BattleUnit>();

    public BattleClientSide(Game game, Legion attacker, Legion defender,
        MasterHex location)
    {
        super(game, attacker, defender, location);

        LOGGER.info("Battle client side instantiated for "
            + attacker.getMarkerId() + " attacking " + defender.getMarkerId()
            + " in land " + location.getTerrain().getDisplayName());
    }

    public void init(int battleTurnNumber, Player battleActivePlayer,
        BattlePhase battlePhase)
    {
        this.battleTurnNumber = battleTurnNumber;
        this.battleActivePlayer = battleActivePlayer;
        this.battlePhase = battlePhase;

        this.getDefendingLegion().setEntrySide(
            this.getAttackingLegion().getEntrySide().getOpposingSide());
    }

    @Override
    protected boolean isOccupied(BattleHex hex)
    {
        for (BattleCritter battleUnit : getBattleUnits())
        {
            if (battleUnit.getCurrentHex().equals(hex))
            {
                return true;
            }
        }
        return false;
    }

    // Helper method
    public GameClientSide getGameClientSide()
    {
        return (GameClientSide)game;
    }

    public void setBattleTurnNumber(int battleTurnNumber)
    {
        this.battleTurnNumber = battleTurnNumber;
    }

    public int getBattleTurnNumber()
    {
        return battleTurnNumber;
    }

    public Player getBattleActivePlayer()
    {
        return battleActivePlayer;
    }

    public void cleanupBattle()
    {
        battleUnits.clear();

        setBattlePhase(null);
        battleTurnNumber = -1;
        battleActivePlayer = ((GameClientSide)game).getNoonePlayer();
    }

    public Legion getBattleActiveLegion()
    {
        if (battleActivePlayer.equals(getDefendingLegion().getPlayer()))
        {
            return getDefendingLegion();
        }
        else
        {
            return getAttackingLegion();
        }
    }

    public BattlePhase getBattlePhase()
    {
        return battlePhase;
    }

    public void setBattlePhase(BattlePhase battlePhase)
    {
        this.battlePhase = battlePhase;
    }

    public boolean isBattlePhase(BattlePhase phase)
    {
        return this.battlePhase == phase;
    }

    public void setupPhase(BattlePhase phase, Player battleActivePlayer,
        int battleTurnNumber)
    {
        setBattlePhase(phase);
        setBattleActivePlayer(battleActivePlayer);
        setBattleTurnNumber(battleTurnNumber);
    }

    // public for IOracle
    public String getBattlePhaseName()
    {
        if (game.isPhase(Phase.FIGHT))
        {
            if (battlePhase != null)
            {
                return battlePhase.toString();
            }
        }
        return "";
    }

    public void setBattleActivePlayer(Player battleActivePlayer)
    {
        this.battleActivePlayer = battleActivePlayer;
    }

    public void setupBattleFight(BattlePhase battlePhase,
        Player battleActivePlayer)
    {
        setBattlePhase(battlePhase);
        setBattleActivePlayer(battleActivePlayer);
        if (isBattlePhase(BattlePhase.FIGHT))
        {
            markOffboardCreaturesDead();
        }
    }

    public BattleUnit createBattleUnit(String imageName, boolean inverted,
        int tag, BattleHex hex, CreatureType type, Legion legion)
    {
        BattleUnit battleUnit = new BattleUnit(imageName, inverted, tag, hex,
            type, legion);
        battleUnits.add(battleUnit);

        return battleUnit;
    }

    public boolean anyOffboardCreatures()
    {
        for (BattleCritter battleUnit : getActiveBattleUnits())
        {
            if (battleUnit.getCurrentHex().getLabel().startsWith("X"))
            {
                return true;
            }
        }
        return false;
    }

    public List<BattleUnit> getActiveBattleUnits()
    {
        return CollectionHelper.selectAsList(battleUnits,
            new Predicate<BattleUnit>()
            {
                public boolean matches(BattleUnit battleUnit)
                {
                    return getBattleActivePlayer().equals(
                        getGameClientSide()
                            .getPlayerByTag(battleUnit.getTag()));
                }
            });
    }

    public List<BattleUnit> getInactiveBattleUnits()
    {
        return CollectionHelper.selectAsList(battleUnits,
            new Predicate<BattleUnit>()
            {
                public boolean matches(BattleUnit battleUnit)
                {
                    return !getBattleActivePlayer().equals(
                        getGameClientSide()
                            .getPlayerByTag(battleUnit.getTag()));
                }
            });
    }

    public List<BattleUnit> getBattleUnits()
    {
        return Collections.unmodifiableList(battleUnits);
    }

    public List<BattleUnit> getBattleUnits(final BattleHex hex)
    {
        return CollectionHelper.selectAsList(battleUnits,
            new Predicate<BattleUnit>()
            {
                public boolean matches(BattleUnit battleUnit)
                {
                    return hex.equals(battleUnit.getCurrentHex());
                }
            });
    }

    public BattleUnit getBattleUnit(BattleHex hex)
    {
        List<BattleUnit> lBattleUnits = getBattleUnits(hex);
        if (lBattleUnits.isEmpty())
        {
            return null;
        }
        return lBattleUnits.get(0);
    }

    /** Get the BattleUnit with this tag. */
    BattleUnit getBattleUnit(int tag)
    {
        for (BattleUnit battleUnit : battleUnits)
        {
            if (battleUnit.getTag() == tag)
            {
                return battleUnit;
            }
        }
        return null;
    }

    public void resetAllBattleMoves()
    {
        for (BattleCritter battleUnit : battleUnits)
        {
            battleUnit.setMoved(false);
            battleUnit.setStruck(false);
        }
    }

    public void markOffboardCreaturesDead()
    {
        for (BattleUnit battleUnit : getActiveBattleUnits())
        {
            if (battleUnit.getCurrentHex().getLabel().startsWith("X"))
            {
                battleUnit.setDead(true);
            }
        }
    }

    public void removeDeadBattleChits()
    {
        Iterator<BattleUnit> it = battleUnits.iterator();
        while (it.hasNext())
        {
            BattleUnit battleUnit = it.next();
            if (battleUnit.isDead())
            {
                it.remove();
            }
        }
    }

    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain.
     *  WARNING: this is duplicated in CreatureServerSide
     *  (moved from Strike to here)
     */
    public int getDice(BattleCritter battleUnit, BattleCritter target,
        Client client)
    {
        BattleHex hex = battleUnit.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();
        CreatureType striker = battleUnit.getCreatureType();

        int dice;
        if (striker.isTitan())
        {
            dice = battleUnit.getTitanPower();
        }
        else
        {
            dice = striker.getPower();
        }

        boolean rangestrike = !client.isInContact(battleUnit, true);
        HazardTerrain terrain = hex.getTerrain();
        if (rangestrike)
        {
            // Divide power in half, rounding down.
            dice /= 2;

            // volcanoNative rangestriking from volcano: +2
            if (terrain.equals(HazardTerrain.VOLCANO)
                && striker.isNativeIn(terrain))
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // volcanoNative striking from volcano: +2
            if (terrain.equals(HazardTerrain.VOLCANO)
                && striker.isNativeIn(terrain))
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = Battle.getDirection(hex, targetHex, false);
            HazardHexside hazard = hex.getHexsideHazard(direction);

            // Native striking down a dune hexside: +2
            if (hazard == HazardHexside.DUNE && striker.isNativeDune())
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hazard == HazardHexside.SLOPE && striker.isNativeSlope())
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (!striker.isNativeDune()
                && hex.getOppositeHazard(direction) == HazardHexside.DUNE)
            {
                dice--;
            }
        }

        return dice;
    }


    /** WARNING: this is duplicated in CreatureServerSide
     *  (moved from Strike to here)
     */
    private int getAttackerSkill(BattleCritter striker, BattleCritter target, Client client)
    {
        BattleHex hex = striker.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = striker.getSkill();

        boolean rangestrike = !client.isInContact(striker, true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            HazardTerrain terrain = hex.getTerrain();
            // striking out of possible hazard
            attackerSkill -= hex.getTerrain().getSkillPenaltyStrikeFrom(
                striker.getCreatureType().isNativeIn(terrain),
                target.getCreatureType().isNativeIn(terrain));

            if (hex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = getDirection(hex, targetHex, false);
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
                int direction = getDirection(targetHex, hex, false);
                HazardHexside hazard = targetHex.getHexsideHazard(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                // TODO Tower vs. Wall ...
                if ((hazard == HazardHexside.SLOPE && !striker
                    .getCreatureType().isNativeSlope())
                    || hazard == HazardHexside.TOWER)
                {
                    attackerSkill--;
                }
            }
        }
        else if (!striker.getCreatureType().useMagicMissile())
        {
            // Range penalty
            int range = Battle.getRange(hex, targetHex, false);
            if (range >= 4)
            {
                attackerSkill -= (range - 3);
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!striker.getCreatureType().isNativeIn(HazardTerrain.BRAMBLES))
            {
                attackerSkill -= countBrambleHexes(hex, targetHex);
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
     */
    public int getStrikeNumber(BattleCritter striker, BattleCritter target,
        Client client)
    {
        boolean rangestrike = !client.isInContact(striker, true);

        int attackerSkill = getAttackerSkill(striker, target, client);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        HazardTerrain terrain = target.getCurrentHex().getTerrain();

        if (!rangestrike)
        {
            // Strike number can be modified directly by terrain.
            strikeNumber += terrain.getSkillBonusStruckIn(striker
                .getCreatureType().isNativeIn(terrain), target
                .getCreatureType().isNativeIn(terrain));
        }
        else
        {
            // Native defending in bramble, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.BRAMBLES)
                && target.getCreatureType().isNativeIn(HazardTerrain.BRAMBLES)
                && !striker.getCreatureType().isNativeIn(
                    HazardTerrain.BRAMBLES)
                && !striker.getCreatureType().useMagicMissile())
            {
                strikeNumber++;
            }

            // Native defending in stone, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.STONE)
                && target.getCreatureType().isNativeIn(HazardTerrain.STONE)
                && !striker.getCreatureType().isNativeIn(HazardTerrain.STONE)
                && !striker.getCreatureType().useMagicMissile())
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

    /** Return the number of intervening bramble hexes.  If LOS is along a
     *  hexspine, go left if argument left is true, right otherwise.  If
     *  LOS is blocked, return a large number.
     * @deprecated another function with explicit reference to Bramble
     * that should be fixed.
     */
    @Deprecated
    private int countBrambleHexesDir(BattleHex hex1, BattleHex hex2,
        boolean left, int previousCount)
    {
        int count = previousCount;

        // Offboard hexes are not allowed.
        if (hex1.getXCoord() == -1 || hex2.getXCoord() == -1)
        {
            return Constants.BIGNUM;
        }

        int direction = getDirection(hex1, hex2, left);

        BattleHex nextHex = hex1.getNeighbor(direction);
        if (nextHex == null)
        {
            return Constants.BIGNUM;
        }

        if (nextHex == hex2)
        {
            // Success!
            return count;
        }

        // Add one if it's bramble.
        if (nextHex.getTerrain().equals(HazardTerrain.BRAMBLES))
        {
            count++;
        }

        return countBrambleHexesDir(nextHex, hex2, left, count);
    }

    /** Return the number of intervening bramble hexes.  If LOS is along a
     * hexspine and there are two choices, pick the lower one.
     * @deprecated another function with explicit reference to Bramble
     * that should be fixed.
     */
    @Deprecated
    public int countBrambleHexes(BattleHex hex1, BattleHex hex2)
    {
        if (hex1 == hex2)
        {
            return 0;
        }

        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();

        // Offboard hexes are not allowed.
        if (CompareDoubles.almostEqual(x1, -1.0)
            || CompareDoubles.almostEqual(x2, -1))
        {
            return Constants.BIGNUM;
        }

        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }

        double xDist = x2 - x1;
        double yDist = y2 - y1;

        if (CompareDoubles.almostEqual(yDist, 0.0)
            || CompareDoubles.almostEqual(Math.abs(yDist), 1.5 * Math
                .abs(xDist)))
        {
            int strikeElevation = Math.min(hex1.getElevation(), hex2
                .getElevation());
            // Hexspine; try unblocked side(s).
            if (isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, false, 0, 0))
            {
                return countBrambleHexesDir(hex1, hex2, false, 0);
            }
            else if (isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                false, false, false, false, false, false, 0, 0))
            {
                return countBrambleHexesDir(hex1, hex2, true, 0);
            }
            else
            {
                return Math.min(countBrambleHexesDir(hex1, hex2, true, 0),
                    countBrambleHexesDir(hex1, hex2, false, 0));
            }
        }
        else
        {
            return countBrambleHexesDir(hex1, hex2, Battle
                .toLeft(xDist, yDist), 0);
        }
    }

}
