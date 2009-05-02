package net.sf.colossus.gui;


import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;


/**
 * Class BattleUnit represents a Creature in a specific Battle.
 * GUI aspects moved to new Class GUIBattleChit, still a member variable,
 * but that might change too.
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
    private BattleHex currentHex;
    private BattleHex startingHex;
    private boolean moved;
    private boolean struck;
    private boolean dead;
    private GUIBattleChit battleChit;

    /** Listeners to be informed when something changes, e.g. right now only
     *  GUIBattleChit that needs to repaint if dead or hits change.
     */
    private final Set<Listener> listeners = new TreeSet<Listener>();


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

    public void setBattleChit(GUIBattleChit battleChit)
    {
        this.battleChit = battleChit;
    }

    public GUIBattleChit getGUIBattleChit()
    {
        return battleChit;
    }

    public String deriveCreatureNameFromId()
    {
        String id = getId();
        if (id == null)
        {
            LOGGER.log(Level.SEVERE, "Chit.getId() returned null id ?");
            return null;
        }
        else if (id.startsWith(Constants.titan))
        {
            id = Constants.titan;
        }
        return id;
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

    public boolean wouldDieFrom(int hits)
    {
        return (hits + getHits() >= getPower());
    }

    public void setDead(boolean dead)
    {
        this.dead = dead;
        if (dead)
        {
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

    public CreatureType getCreatureType()
    {
        return creatureType;
    }

    public boolean isInverted()
    {
        return defender;
    }

    public String getId()
    {
        return id;
    }

    public boolean isTitan()
    {
        return getCreatureType().isTitan();
    }

    public int getPower()
    {
        if (isTitan())
        {
            return getTitanPower();
        }
        else
        {
            return getCreatureType().getPower();
        }
    }

    // TODO copied from Chit - replace or let Creature deal with it
    public int getTitanPower()
    {
        if (!id.startsWith("Titan-"))
        {
            return -1;
        }
        String[] parts = id.split("-");
        int power = Integer.parseInt(parts[1]);
        return power;
    }

    public int getSkill()
    {
        return getCreatureType().getSkill();
    }

    public int getPointValue()
    {
        return getPower() * getSkill();
    }

    public boolean isRangestriker()
    {
        return getCreatureType().isRangestriker();
    }

    // TODO does this give plain Titan name or user specific one?
    public String getDescription()
    {
        return getCreatureType().getName() + " in "
            + getCurrentHex().getLabel();
    }

    @Override
    public String toString()
    {
        return getDescription();
    }

    /**
     * Listeners who needs to be notified if (currently) hits or dead
     * values change, to trigger a repaint: GUIBattleChit
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
