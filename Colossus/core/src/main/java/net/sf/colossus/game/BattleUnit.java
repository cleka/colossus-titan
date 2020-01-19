package net.sf.colossus.game;


import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;


/**
 * Class BattleUnit represents a Creature in a specific Battle.
 * GUI aspects moved to new Class GUIBattleChit.
 *
 * TODO This should at some point extend Creature, or perhaps Creature can
 * take care of all so no extend is needed; but right now, Creature handles
 * some things (e.g. how to change the hexes) differently than how it's done
 * here, so can not "just delegate it" - needs investigation and checking.
 *
 * @author David Ripton
 * @author Clemens Katzer (strip GUI issues out, to own new Class)
 */
@SuppressWarnings("serial")
public final class BattleUnit implements BattleCritter
{
    private static final Logger LOGGER = Logger.getLogger(BattleUnit.class
        .getName());

    private final int tag;
    private final String id;
    private final boolean defender;
    private final CreatureType creatureType;
    private final Legion legion;
    private int hits = 0;
    private int poisonDamage = 0;
    private int poison = 0;
    private int slows = 0;
    private int slowed = 0;
    private BattleHex currentHex;
    private BattleHex startingHex;
    private boolean moved;
    private boolean struck;
    private boolean dead;

    /** Listeners to be informed when something changes, e.g. right now only
     *  GUIBattleChit that needs to repaint if dead or hits change.
     */
    private final Set<Listener> listeners = new HashSet<Listener>();

    public BattleUnit(String id, boolean defender, int tag,
        BattleHex currentHex, CreatureType type, Legion legion)
    {
        if (id == null)
        {
            LOGGER.log(Level.WARNING, "Created BattleUnit with null id!");
        }

        this.tag = tag;
        this.id = id;
        this.defender = defender;
        this.currentHex = currentHex;

        this.creatureType = type;
        this.legion = legion;
    }

    public Legion getLegion()
    {
        return legion;
    }

    public int getTag()
    {
        return tag;
    }

    public int getHits()
    {
        return hits;
    }

    public void setHits(int hits)
    {
        this.hits = hits;
        notifyListeners();
    }

    public void setPoison(int damage)
    {
        this.poison = damage;
    }

    public void setPoisonDamage(int damage)
    {
        this.poisonDamage = damage;
    }

    public void addPoisonDamage(int damage)
    {
        // Poison damage is cumulative, so add to existing value
        this.poisonDamage += damage;
    }

    public void setSlowed(int slowValue)
    {
        this.slowed = slowValue;
    }

    public void addSlowed(int slowValue)
    {
        // Slowing is cumulative, so add to existing value
        this.slowed += slowValue;
    }

    public void setSlows(int slowValue)
    {
        this.slows = slowValue;
    }

    public boolean wouldDieFrom(int hits)
    {
        // TODO what if critter / creature is already dead anyway?
        return (hits + getHits() >= getPower());
    }

    public void setDead(boolean dead)
    {
        this.dead = dead;
        if (dead)
        {
            // TODO is this "if dead, set hits to 0" still needed?
            // Probably originated from the GUI issue, don't paint a damage nr
            // on a dead chit, but nowadays GUIBattleChit takes care of that
            // by itself (I believe).
            setHits(0);
            // setHits() calls notifyListeners
        }
        else
        {
            // otherwise we need to call here
            notifyListeners();
        }
    }

    public boolean isDead()
    {
        return dead;
    }

    public BattleHex getCurrentHex()
    {
        return currentHex;
    }

    public BattleHex getStartingHex()
    {
        return startingHex;
    }

    public void setCurrentHex(BattleHex hex)
    {
        this.currentHex = hex;
    }

    public void moveToHex(BattleHex hex)
    {
        startingHex = currentHex;
        currentHex = hex;
    }

    // TODO make package private
    public boolean hasMoved()
    {
        return moved;
    }

    // TODO make package private
    public void setMoved(boolean moved)
    {
        this.moved = moved;
    }

    public boolean hasStruck()
    {
        return struck;
    }

    public void setStruck(boolean struck)
    {
        this.struck = struck;
    }

    public CreatureType getType()
    {
        return creatureType;
    }

    public boolean isDefender()
    {
        return defender;
    }

    public String getId()
    {
        return id;
    }

    public boolean isTitan()
    {
        return getType().isTitan();
    }

    public boolean isLord()
    {
        return getType().isLord();
    }

    public boolean isDemiLord()
    {
        return getType().isDemiLord();
    }

    public int getPower()
    {
        if (isTitan())
        {
            return getTitanPower();
        }
        else
        {
            return getType().getPower();
        }
    }

    // TODO copied from Chit - replace or let Creature deal with it

    // TODO this validation against "via legion and player" (that's how
    // Creature.java is doing it) is only temporary; on the long run get
    // rid of the parsing based method.

    public int getTitanPower()
    {
        int parsedPower = getIdBasedTitanPower();
        int playerBasedPower = getTitanPowerViaLegionAndPlayer();

        if (playerBasedPower != parsedPower)
        {
            LOGGER.warning("id/parsing based power is " + parsedPower
                + ", but Power via Legion and Player is " + playerBasedPower);
        }
        return playerBasedPower;

    }

    public int getIdBasedTitanPower()
    {
        if (!id.startsWith("Titan-"))
        {
            LOGGER.warning("Asked Titan Power from non-Titan BattleUnit '"
                + getType() + "'!");
            return -1;
        }
        String[] parts = id.split("-");
        return Integer.parseInt(parts[1]);
    }

    public int getTitanPowerViaLegionAndPlayer()
    {
        Player player = legion.getPlayer();
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

    public int getSkill()
    {
        return getType().getSkill();
    }

    public int getPointValue()
    {
        return getPower() * getSkill();
    }

    public int getPoison()
    {
        return poison;
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
        return slows;
    }

    public boolean isRangestriker()
    {
        return getType().isRangestriker();
    }

    public boolean useMagicMissile()
    {
        return getType().useMagicMissile();
    }

    // TODO does this give plain Titan name or user specific one?
    public String getDescription()
    {
        return getType().getName() + " in " + getCurrentHex().getLabel();
    }

    @Override
    public String toString()
    {
        return getDescription();
    }

    /**
     * Listeners who needs to be notified if (currently) hits or dead values
     * change, to trigger repaint: a GUIBattleChit representing this creature
     */
    abstract public class Listener
    {
        abstract public void actOnHitOrDeadChanged();
    }

    public void addListener(Listener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(Listener listener)
    {
        listeners.remove(listener);
    }

    public void notifyListeners()
    {
        for (Listener listener : listeners)
        {
            listener.actOnHitOrDeadChanged();
        }
    }

}
