package net.sf.colossus.game;


import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.variant.BattleHex;
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
    /**
     * Implements an order on Critters by some definition of importance.
     *
     * The order is:
     * - titans first
     * - then sorted by points value
     * - then sorted by rangestriker or not
     * - then sorted by flyer or not
     * - then by name
     */
    public static final Comparator<Creature> IMPORTANCE_ORDER = new Comparator<Creature>()
    {
        public int compare(Creature critter1, Creature critter2)
        {
            if (critter1.isTitan() && !critter2.isTitan())
            {
                return -1;
            }
            if (critter2.isTitan() && !critter1.isTitan())
            {
                return 1;
            }
            int diff = critter2.getPointValue() - critter1.getPointValue();
            if (diff != 0)
            {
                return diff;
            }
            if (critter1.isRangestriker() && !critter2.isRangestriker())
            {
                return -1;
            }
            if (!critter1.isRangestriker() && critter2.isRangestriker())
            {
                return 1;
            }
            if (critter1.isFlier() && !critter2.isFlier())
            {
                return -1;
            }
            if (!critter1.isFlier() && critter2.isFlier())
            {
                return 1;
            }
            return critter1.getName().compareTo(critter2.getName());
        }
    };

    private static final Logger LOGGER = Logger.getLogger(Creature.class
        .getName());
    final private CreatureType type;
    protected Legion legion;
    private BattleHex currentHex;
    private BattleHex startingHex;

    /**
     * Damage taken
     */
    private int hits = 0;
    private int poisonDamage = 0;
    private int slowed = 0;
    private boolean struck;

    public Creature(CreatureType type, Legion legion)
    {
        this.type = type;
        this.legion = legion;
        currentHex = null;
        startingHex = null;
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
        int dice = getPower();

        // Dice can be modified by terrain.
        dice += myHexTerrain.getPowerBonusStrikeFrom(
            myType.isNativeIn(myHexTerrain),
            targetType.isNativeIn(myHexTerrain));

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
        attackerSkill -= myHexTerrain.getSkillPenaltyStrikeFrom(
            myType.isNativeIn(myHexTerrain),
            targetType.isNativeIn(myHexTerrain));

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
                .isNativeSlope()) || targetHexside.equals(HazardHexside.TOWER))
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

    public Legion getLegion()
    {
        return legion;
    }

    public Player getPlayer()
    {
        return legion.getPlayer();
    }

    public int getPower()
    {
        if (isTitan())
        {
            return getTitanPower();
        }
        return getType().getPower();
    }

    public int getTitanPower()
    {
        Player player = getPlayer();
        if (player != null)
        {
            return player.getTitanPower();
        }
        else
        {
            // Just in case player is dead.
            LOGGER.warning("asked for Titan power of dead (null) player!");
            return 6;
        }
    }

    public String getMarkerId()
    {
        return legion.getMarkerId();
    }

    public String getName()
    {
        return getType().getName();
    }

    public boolean isTitan()
    {
        return getType().isTitan();
    }

    public String getDescription()
    {
        return getName() + " in " + getCurrentHex().getDescription();
    }

    public BattleHex getStartingHex()
    {
        return startingHex;
    }

    public void setStartingHex(BattleHex hex)
    {
        this.startingHex = hex;
    }

    public BattleHex getCurrentHex()
    {
        return currentHex;
    }

    public void setCurrentHex(BattleHex hex)
    {
        this.currentHex = hex;
    }

    public void moveToHex(BattleHex hex)
    {
        setCurrentHex(hex);
    }

    public void commitMove()
    {
        setStartingHex(getCurrentHex());
    }

    public boolean hasMoved()
    {
        return !getCurrentHex().equals(getStartingHex());
    }

    // TODO never used?
    public void setMoved(boolean moved)
    {
        assert (moved == hasMoved()) : "Oups, setMoved on immobile Creature";
    }

    public boolean isDemiLord()
    {
        return getType().isDemiLord();
    }

    public boolean isFlier()
    {
        return getType().isFlier();
    }

    public boolean isImmortal()
    {
        return getType().isImmortal();
    }

    public boolean isLord()
    {
        return getType().isLord();
    }

    public boolean isLordOrDemiLord()
    {
        return getType().isLordOrDemiLord();
    }

    public boolean isRangestriker()
    {
        return getType().isRangestriker();
    }

    public boolean useMagicMissile()
    {
        return getType().useMagicMissile();
    }

    public boolean isSummonable()
    {
        return getType().isSummonable();
    }

    public boolean isNativeAt(HazardHexside hexside)
    {
        return getType().isNativeAt(hexside);
    }

    public boolean isNativeIn(HazardTerrain terrain)
    {
        return getType().isNativeIn(terrain);
    }

    public int getPointValue()
    {
        // Must use our local, Titan-aware getPower()
        // return getCreature().getPointValue();
        return getPower() * getSkill();
    }

    public int getSkill()
    {
        return getType().getSkill();
    }

    public int getHits()
    {
        return hits;
    }

    public int getPoison()
    {
        return getType().getPoison();
    }

    public int getPoisonDamage()
    {
        return poisonDamage;
    }

    public int getSlowed()
    {
        return slowed;
    }

    public int getSlows()
    {
        return getType().getSlows();
    }

    public boolean hasStruck()
    {
        return struck;
    }

    public void setHits(int hits)
    {
        this.hits = hits;
    }

    public void setPoisonDamage(int damage)
    {
        poisonDamage = damage;
    }

    public void addPoisonDamage(int damage)
    {
        poisonDamage += damage;
    }

    public void setSlowed(int slowValue)
    {
        slowed = slowValue;
    }

    public void addSlowed(int slowValue)
    {
        slowed += slowValue;
    }

    public void setStruck(boolean struck)
    {
        this.struck = struck;
    }

    public boolean isDead()
    {
        return getHits() >= getPower();
    }

    public void setDead(boolean dead)
    {
        if (dead)
        {
            setHits(getPower());
        }
        else
        {
            assert (getHits() < getPower()) : "Oups, making NOT dead but should be";
        }
    }

    public String[] getImageNames()
    {
        return getType().getImageNames();
    }

    public String getPluralName()
    {
        return getType().getPluralName();
    }

    public int getMaxCount()
    {
        return getType().getMaxCount();
    }

    public void heal()
    {
        setHits(0);
        setPoisonDamage(0);
        setSlowed(0);
    }

    public boolean wouldDieFrom(int additionalDamage)
    {
        return (this.hits + additionalDamage >= getPower());
    }

    /**
     * Apply damage or healing to this critter.  Return the amount of excess damage
     * done, which may sometimes carry to another target.
     */
    public int adjustHits(int damage)
    {
        int excess = 0;
        int tmp_hits = getHits();
        int oldhits = tmp_hits;

        tmp_hits = (tmp_hits + damage) > 0 ? tmp_hits + damage : 0;
        if (tmp_hits > getPower())
        {
            excess = tmp_hits - getPower();
            tmp_hits = getPower();
        }
        LOGGER.log(Level.INFO, "Critter " + getDescription() + ": " + oldhits
            + " + " + damage + " => " + tmp_hits + "; " + excess + " excess");
        // Check for death.
        if (tmp_hits >= getPower())
        {
            LOGGER.log(Level.INFO, "Critter " + getDescription()
                + " is now dead: (hits=" + tmp_hits + " > power=" + getPower()
                + ")");
            setDead(true);
        }
        setHits(tmp_hits);
        return excess;
    }
}
