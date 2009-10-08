package net.sf.colossus.client;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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


/**
 *  Contains a lot of Battle related data
 *
 *  Currently contains also many methods that were earlier in "Strike.java"
 *  (client package).
 *  First moved to here to make it easier to unify them with the server side
 *  version or possibly even with Battle from game package.
 *
 *  TODO One handicap right now is isInContact(...)
 *
 *  This method is used by getDice, getAttackerSkill and getStrikeNumber;
 *  they ask this from Client (and thus need client as argument).
 *  On server side, those methods are in CreatureServerSide
 *  (do they belong there?? IMHO not, because those calls are valid to
 *  to only during a battle, which might not always be the case and nothing
 *  prevents calling it then) and CreatureServerSide is able to resolve that
 *  question by itself.
 *
 */
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

    // TODO move all this strike stuff to BattleClientSide / Battle
    public Set<BattleHex> findStrikes(int tag, Client client)
    {
        BattleCritter battleUnit = getBattleUnit(tag);
        return findStrikes(battleUnit, true, client);
    }

    @SuppressWarnings("unused")
    private Set<BattleHex> nosuchmethodfindStrikes(BattleCritter battleUnit,
        boolean dummy)
    {
        LOGGER.severe("called crappy dummy method!");
        return null;
    }

    /** Return the set of hexes with critters that have
     *  valid strike targets.
     * @param client TODO*/
    Set<BattleHex> findCrittersWithTargets(Client client)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (BattleCritter battleUnit : getActiveBattleUnits())
        {
            if (countStrikes(battleUnit, true, client) > 0)
            {
                set.add(battleUnit.getCurrentHex());
            }
        }

        return set;
    }

    private int countStrikes(BattleCritter battleUnit, boolean rangestrike,
        Client client)
    {
        return findStrikes(battleUnit, rangestrike, client).size();
    }

    public boolean canStrike(BattleCritter striker, BattleCritter target,
        Client client)
    {
        BattleHex targetHex = target.getCurrentHex();
        return findStrikes(striker, true, client).contains(targetHex);
    }

    /** Return a set of hexes containing targets that the
     *  critter may strike.  Only include rangestrikes if rangestrike
     *  is true. */
    public Set<BattleHex> findStrikes(BattleCritter battleUnit,
        boolean rangestrike, Client client)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();

        // Each creature may strike only once per turn.
        if (battleUnit.hasStruck())
        {
            return set;
        }
        // Offboard creatures can't strike.
        if (battleUnit.getCurrentHex().getLabel().startsWith("X"))
        {
            return set;
        }

        boolean inverted = battleUnit.isDefender();
        BattleHex currentHex = battleUnit.getCurrentHex();

        boolean adjacentEnemy = false;

        // First mark and count normal strikes.
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not engaged.
            if (!currentHex.isCliff(i))
            {
                BattleHex targetHex = currentHex.getNeighbor(i);
                if (targetHex != null && isOccupied(targetHex)
                    && !targetHex.isEntrance())
                {
                    BattleCritter target = getBattleUnit(targetHex);
                    if (target.isDefender() != inverted)
                    {
                        adjacentEnemy = true;
                        if (!target.isDead())
                        {
                            set.add(targetHex);
                        }
                    }
                }
            }
        }

        CreatureType creature = battleUnit.getCreatureType();

        // Then do rangestrikes if applicable.  Rangestrikes are not allowed
        // if the creature can strike normally, so only look for them if
        // no targets have yet been found.
        if (rangestrike && !adjacentEnemy && creature.isRangestriker()
            && getBattlePhase() != BattlePhase.STRIKEBACK)
        {
            for (BattleCritter target : getInactiveBattleUnits())
            {
                if (!target.isDead())
                {
                    BattleHex targetHex = target.getCurrentHex();
                    if (isRangestrikePossible(battleUnit, target, client))
                    {
                        set.add(targetHex);
                    }
                }
            }
        }
        return set;
    }

    /** Return true if the rangestrike is possible.
     * @param client TODO*/
    /*
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike, with
     *   extension of Creature instead of BattleCritter and extra BattleHex
     */
    @Deprecated
    private boolean isRangestrikePossible(BattleCritter striker,
        BattleCritter target, Client client)
    {
        CreatureType creature = striker.getCreatureType();
        CreatureType targetCreature = target.getCreatureType();

        BattleHex currentHex = striker.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        if (currentHex.isEntrance() || targetHex.isEntrance())
        {
            return false;
        }

        int range = Battle.getRange(currentHex, targetHex, false);
        int skill = creature.getSkill();

        if (range > skill)
        {
            return false;
        }

        // Only magicMissile can rangestrike at range 2, rangestrike Lords,
        // or rangestrike without LOS.
        else if (!creature.useMagicMissile()
            && (range < 3 || targetCreature.isLord() || isLOSBlocked3(client,
                currentHex, targetHex)))
        {
            return false;
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean isLOSBlocked3(Client client, BattleHex currentHex,
        BattleHex targetHex)
    {
        boolean strikeIsLOSBlocked = client.getStrike().isLOSBlocked(currentHex,
            targetHex);

        boolean bcsIsLOSBlocked = isLOSBlocked(currentHex, targetHex);

        boolean bssIsLOSBlocked = super.isLOSBlocked(currentHex, targetHex);

        if (strikeIsLOSBlocked != bcsIsLOSBlocked
            || strikeIsLOSBlocked != bssIsLOSBlocked
            || bssIsLOSBlocked != bcsIsLOSBlocked)
        {
            LOGGER.warning("\n"
                + "Strike.LOSBlocked: " + strikeIsLOSBlocked + "\n"
                + "BtleCS.LOSBlocked: " + bcsIsLOSBlocked + "\n"
                + "Battle.LOSBlocked: " + bssIsLOSBlocked + "\n" );
        }
        return strikeIsLOSBlocked;

    }
    /*
     * TODO When it feels safe, remove the validating call to
     *      the old isLOSBlockedClientSide method and use only the one
     *      from game.Battle.
     */
    @Override
    public boolean isLOSBlocked(BattleHex currentHex, BattleHex targetHex)
    {
        return isLOSBlockedClientSide(currentHex,
            targetHex);
    }

    public boolean superIsLOSBlocked(BattleHex currentHex, BattleHex targetHex)
    {
        // The version in game.Battle, which came from server side
        boolean isBlocked = super.isLOSBlocked(currentHex, targetHex);
        return isBlocked;
    }

    /** Check to see if the LOS from hex1 to hex2 is blocked.  If the LOS
     *  lies along a hexspine, check both and return true only if both are
     *  blocked.
     *  This is the version that was earlier in Strike.java (client package).
     *
     *  TODO unify with game.Battle version and if it's safe that they
     *  are identical move up / get rid of this one here
     *
     * @deprecated Duplicate with game.Battke (=parent)
     */
    @Deprecated
    private boolean isLOSBlockedClientSide(BattleHex hex1, BattleHex hex2)
    {
        if (hex1 == hex2)
        {
            return false;
        }

        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();

        // Offboard hexes are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return true;
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

        // Creatures below the level of the strike do not block LOS.
        int strikeElevation = Math.min(hex1.getElevation(), hex2
            .getElevation());

        if (CompareDoubles.almostEqual(yDist, 0.0)
            || CompareDoubles.almostEqual(Math.abs(yDist), 1.5 * Math
                .abs(xDist)))
        {
            // Hexspine; try both sides.
            return (isLOSBlockedDirClientSide(hex1, hex1, hex2, true,
                strikeElevation, false, false, false, false, false, false, 0,
                0) && isLOSBlockedDirClientSide(hex1, hex1, hex2,
                false,
                strikeElevation, false, false, false, false, false, false, 0,
                0));
        }
        else
        {
            return isLOSBlockedDirClientSide(hex1, hex1, hex2, Battle.toLeft(
                xDist, yDist), strikeElevation, false, false, false, false,
                false, false, 0, 0);
        }
    }

    /** Check LOS, going to the left of hexspines if argument left is true,
     *  or to the right if it is false.
     *
     *  TODO unify with game.Battle version and if it's safe that they
     *  are identical move up / get rid of this one here
     *
     * @deprecated
     */
    @Deprecated
    private boolean isLOSBlockedDirClientSide(BattleHex initialHex,
        BattleHex currentHex, BattleHex finalHex, boolean left,
        int strikeElevation, boolean strikerAtop, boolean strikerAtopCliff,
        boolean strikerAtopWall, boolean midObstacle, boolean midCliff,
        boolean midChit, int totalObstacles, int totalWalls)
    {
        boolean targetAtop = false;
        boolean targetAtopCliff = false;
        boolean targetAtopWall = false;
        if (currentHex == finalHex)
        {
            return false;
        }
        // Offboard hexes are not allowed.
        if (currentHex.getXCoord() == -1 || finalHex.getXCoord() == -1)
        {
            return true;
        }
        int direction = BattleClientSide.getDirection(currentHex, finalHex,
            left);
        BattleHex nextHex = currentHex.getNeighbor(direction);
        if (nextHex == null)
        {
            return true;
        }
        char hexside = currentHex.getHexsideHazard(direction).getCode();
        char hexside2 = currentHex.getOppositeHazard(direction).getCode();
        if (currentHex == initialHex)
        {
            if (isObstacle(hexside))
            {
                strikerAtop = true;
                totalObstacles++;
                if (hexside == 'c')
                {
                    strikerAtopCliff = true;
                }
                else if (hexside == 'w')
                {
                    strikerAtopWall = true;
                    totalWalls++;
                }
            }

            if (isObstacle(hexside2))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside2 == 'c' || hexside2 == 'd')
                {
                    midCliff = true;
                }
                else if (hexside2 == 'w')
                {
                    return true;
                }
            }
        }
        else if (nextHex == finalHex)
        {
            if (isObstacle(hexside))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c' || hexside == 'd')
                {
                    midCliff = true;
                }
                else if (hexside == 'w')
                {
                    return true;
                }
            }

            if (isObstacle(hexside2))
            {
                targetAtop = true;
                totalObstacles++;
                if (hexside2 == 'c')
                {
                    targetAtopCliff = true;
                }
                else if (hexside2 == 'w')
                {
                    totalWalls++;
                    targetAtopWall = true;
                }
            }
            if (midChit && !targetAtopCliff)
            {
                return true;
            }
            if (midCliff && (!strikerAtopCliff || !targetAtopCliff))
            {
                return true;
            }
            if (midObstacle && !strikerAtop && !targetAtop)
            {
                return true;
            }
            // If there are three slopes, striker and target must each
            //     be atop one.
            if (totalObstacles >= 3 && (!strikerAtop || !targetAtop)
                && (!strikerAtopCliff && !targetAtopCliff))
            {
                return true;
            }
            if (totalWalls >= 2)
            {
                if (!(strikerAtopWall || targetAtopWall))
                {
                    return true;
                }
            }
            // Success!
            return false;
        }
        else
        // not leaving first or entering last hex
        {
            if (midChit)
            {
                // We're not in the initial or final hex, and we have already
                // marked a mid chit, so it's not adjacent to the base of a
                // cliff that the target is atop.
                return true;
            }
            if (isObstacle(hexside) || isObstacle(hexside2))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c' || hexside2 == 'c' || hexside == 'd'
                    || hexside2 == 'd')
                {
                    midCliff = true;
                }
            }
        }
        if (nextHex.blocksLineOfSight())
        {
            return true;
        }
        // Creatures block LOS, unless both striker and target are at higher
        //     elevation than the creature, or unless the creature is at
        //     the base of a cliff and the striker or target is atop it.
        if (isOccupied(nextHex)
            && nextHex.getElevation() >= strikeElevation
            && (!strikerAtopCliff || currentHex != initialHex))
        {
            midChit = true;
        }

        return isLOSBlockedDirClientSide(initialHex, nextHex, finalHex, left,
            strikeElevation, strikerAtop, strikerAtopCliff, strikerAtopWall,
            midObstacle, midCliff, midChit, totalObstacles, totalWalls);
    }

    /** Return the titan range (inclusive at both ends) from the critter to the
     *  closest enemy critter.  Return OUT_OF_RANGE if there are none.
     *
     * // BEGIN OLD COMMENT (when it was in Strike.java):
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike
     * // END OLD COMMENT
     *
     * Now this is moved from Strike to BattleClientSide.
     * IMHO this is not a total duplicate of a method in Battle: Battle
     * does not have a minRangeToEnemy, just minRange between concrete hexes,
     * which IS actually called here.
     * TODO can they be unified? Or move to e.g. some class in ai.helper package?
     */
    @Deprecated
    public int minRangeToEnemy(BattleCritter battleUnit)
    {
        BattleHex hex = battleUnit.getCurrentHex();
        int min = Constants.OUT_OF_RANGE;

        for (BattleCritter target : getBattleUnits())
        {
            if (battleUnit.isDefender() != target.isDefender())
            {
                BattleHex targetHex = target.getCurrentHex();
                int range = Battle.getRange(hex, targetHex, false);
                // Exit early if adjacent.
                if (range == 2)
                {
                    return range;
                }
                else if (range < min)
                {
                    min = range;
                }
            }
        }
        return min;
    }

}
