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
import net.sf.colossus.util.Predicate;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
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

    // Helper method
    public GameClientSide getGameClientSide()
    {
        return (GameClientSide)game;
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

    @Override
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

    public BattleUnit createBattleUnit(String imageName, boolean isDefender,
        int tag, BattleHex hex, CreatureType type, Legion legion)
    {
        BattleUnit battleUnit = new BattleUnit(imageName, isDefender, tag,
            hex, type, legion);
        battleUnits.add(battleUnit);

        return battleUnit;
    }

    public boolean anyOffboardCreatures()
    {
        for (BattleCritter critter : getActiveBattleUnits())
        {
            if (isCritterOffboard(critter))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isCritterOffboard(BattleCritter critter)
    {
        return critter.getCurrentHex().getLabel().startsWith("X");
    }

    public boolean isTitanOffboard(Player player)
    {
        for (BattleCritter critter : getBattleUnits())
        {
            if (critter.isTitan()
                && isCritterOffboard(critter)
                && player.equals(getGameClientSide().getPlayerByTag(
                    critter.getTag())))
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

    @Override
    public List<BattleCritter> getAllCritters()
    {
        List<BattleCritter> critters = new ArrayList<BattleCritter>();
        for (BattleCritter critter : getBattleUnits())
        {
            critters.add(critter);
        }
        return critters;
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

    public BattleUnit getBattleUnitCS(BattleHex hex)
    {
        List<BattleUnit> lBattleUnits = getBattleUnits(hex);
        if (lBattleUnits.isEmpty())
        {
            return null;
        }
        return lBattleUnits.get(0);
    }

    public BattleUnit getBattleUnit(BattleHex hex)
    {
        BattleUnit unit = getBattleUnitCS(hex);
        BattleCritter critter = getCritter(hex);

        if (unit == null && critter == null)
        {
            // ok.
        }
        else if ( // unit == null && critter != null
        // || unit != null && critter == null
        unit != critter)
        {
            LOGGER
                .warning("getBattleUnit(hex) returns different result than getCritter(hex)!");
        }
        return unit;
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

    /** Return the set of hexes with critters that have
     *  valid strike targets.
     *  @param client The client.
     */
    Set<BattleHex> findCrittersWithTargets(Client client)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (BattleCritter battleUnit : getActiveBattleUnits())
        {
            if (findTargets(battleUnit, true).size() > 0)
            {
                set.add(battleUnit.getCurrentHex());
            }
        }

        return set;
    }

    /**
     *  Tell whether a given creature can strike (rangestrike included)
     *  the given potential target
     *  TODO duplicated in CreatureServerSide
     *
     *  @param striker The creature striking
     *  @param target The potential target
     *  @return whether striking target is a valid strike
     */
    public boolean canStrike(BattleCritter striker, BattleCritter target)
    {
        BattleHex targetHex = target.getCurrentHex();
        return findTargets(striker, true).contains(targetHex);
    }

    // Not a candidate to pull up: tag-to-BattleUnit resolving is
    // purely a client side issue. Even resolve-data-from-network issue?
    public Set<BattleHex> findTargets(int tag)
    {
        BattleCritter battleUnit = getBattleUnit(tag);
        return findTargets(battleUnit, true);
    }

    /**
     *  Return a set of hexes containing targets that the critter may strike
     *  TODO duplicated in BattleServerSide
     *
     *  @param battleUnit the striking creature
     *  @param rangestrike Whether to include rangestrike targets
     *  @return a set of hexes containing targets
     */
    public Set<BattleHex> findTargets(BattleCritter battleUnit,
        boolean rangestrike)
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

        boolean isDefender = battleUnit.isDefender();
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
                    if (target.isDefender() != isDefender)
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

        CreatureType creature = battleUnit.getType();

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
                    if (isRangestrikePossible(battleUnit, target))
                    {
                        set.add(targetHex);
                    }
                }
            }
        }
        return set;
    }

    /** Return true if the rangestrike is possible.
     *
    /*
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike, with
     *   extension of Creature instead of BattleCritter and extra BattleHex
     */
    @Deprecated
    private boolean isRangestrikePossible(BattleCritter striker,
        BattleCritter target)
    {
        CreatureType creature = striker.getType();
        CreatureType targetCreature = target.getType();

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
            && (range < 3 || targetCreature.isLord() || isLOSBlocked(
                currentHex, targetHex)))
        {
            return false;
        }

        return true;
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
    public int minRangeToEnemy(BattleCritter critter)
    {
        BattleHex hex = critter.getCurrentHex();
        int min = Constants.OUT_OF_RANGE;

        for (BattleCritter target : getBattleUnits())
        {
            if (critter.isDefender() != target.isDefender())
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

    // TODO pull up to game.Battle
    /** Return true if there are any enemies adjacent to this battleChit.
     *  Dead critters count as being in contact only if countDead is true. */
    @Override
    public boolean isInContact(BattleCritter striker, boolean countDead)
    {
        BattleHex hex = striker.getCurrentHex();

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return false;
        }

        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    BattleCritter other = getBattleUnit(neighbor);
                    if (other != null
                        && (other.isDefender() != striker.isDefender())
                        && (countDead || !other.isDead()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
