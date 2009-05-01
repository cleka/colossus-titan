package net.sf.colossus.gui;


import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;


/**
 * Class BattleUnit represents a Creature in a specific Battle.
 * GUI aspects moved to new Class GUIBattleChit, still a member variable,
 * but that might change too.
 *
 * @author David Ripton
 */
@SuppressWarnings("serial")
public final class BattleUnit implements BattleCritter
{
    private static final Logger LOGGER = Logger.getLogger(BattleUnit.class
        .getName());

    private final int tag;
    private final String id;
    private final boolean inverted;
    private final CreatureType creatureType;
    private final Legion legion;
    private int hits = 0;
    private BattleHex currentHex;
    private BattleHex startingHex;
    private boolean moved;
    private boolean struck;
    private boolean dead;
    private final GUIBattleChit battleChit;

    public BattleUnit(int scale, String id, boolean inverted, int tag,
        BattleHex currentHex, PlayerColor playerColor, Client client,
        CreatureType type, Legion legion)
    {
        if (id == null)
        {
            LOGGER.log(Level.WARNING, "Created BattleUnit with null id!");
        }
        this.battleChit = new GUIBattleChit(scale, id, inverted, tag,
            currentHex, playerColor, client);

        this.tag = tag;
        this.id = id;
        this.inverted = inverted;
        this.currentHex = currentHex;

        this.creatureType = type;
        this.legion = legion;
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

    public GUIBattleChit getGUIBattleChit()
    {
        return battleChit;
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

    public void setHex(BattleHex hex)
    {
        this.currentHex = hex;
        battleChit.setHex(hex);
    }

    public void moveToHex(BattleHex hex)
    {
        startingHex = currentHex;
        currentHex = hex;
        battleChit.moveToHex(hex);
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
        return inverted;
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
}
