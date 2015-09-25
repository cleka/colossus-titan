package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Caretaker;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.actions.Acquisition;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */

public final class LegionServerSide extends Legion implements
    Comparable<LegionServerSide>
{
    private static final Logger LOGGER = Logger
        .getLogger(LegionServerSide.class.getName());

    private final Legion parent;

    /**
     * The label of the starting hex of the last move.
     */
    private MasterHex startingHex;

    /**
     * TODO this should be a {@link Creature} or {@link CreatureType}
     */
    private int battleTally;
    private final GameServerSide game;

    /**
     * Creates a new Legion instance.
     *
     * Not that this class does not constraint the number of creatures. In a
     * normal Titan game it is between 0 and 8 creatures, but this class does
     * not enforce this.
     *
     * TODO the game parameter should be redundant since it should be the same
     *      as player.getGame()
     */
    public LegionServerSide(String markerId, Legion parent,
        MasterHex currentHex, MasterHex startingHex, Player player,
        GameServerSide game, CreatureType... creatureTypes)
    {
        super(player, markerId, currentHex);
        assert markerId != null : "MarkerId must not be null";
        assert (parent == null) || !markerId.equals(parent.getMarkerId()) : "Parent can not have the same markerId as we have";
        assert startingHex != null : "Legion needs to start on some hex";
        assert game != null : "Legion needs to be part of some game";
        assert game == player.getGame() : "Legion needs to be part of the same game as its player";

        this.parent = parent;
        this.startingHex = startingHex;
        this.game = game;

        for (CreatureType creature : creatureTypes)
        {
            assert creature != null : "Null creature not allowed";
            getCreatures().add(new CreatureServerSide(creature, this, game));
        }
    }

    @Override
    public int getPointValue()
    {
        int pointValue = 0;
        for (Creature critter : getCreatures())
        {
            pointValue += critter.getPointValue();
        }
        return pointValue;
    }

    /**
     * For each acquirable decision, ask the client to choose one
     * of the possible acquirables.
     */
    public void askAcquirablesDecisions()
    {
        PlayerServerSide player = getPlayer();
        for (AcquirableDecision decision : decisions)
        {
            game.askAcquireAngel(player, this, decision.getAcquirables());
        }
    }

    void addAngel(CreatureType angelType)
    {
        if (angelsToAcquire <= 0)
        {
            return;
        }
        if (angelType == null)
        {
            // Declined to acquire.
        }
        else
        {
            if (getHeight() >= 7)
            {
                LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
                    + " is full and cannot acquire: " + angelType);
            }
            else
            {
                LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
                    + " is going to call addCreature() to add "
                    + "one acquired " + angelType);

                if (addCreature(angelType, true))
                {
                    LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
                        + " acquired one " + angelType);
                    game.getServer().allTellAddCreature(
                        new Acquisition(this, angelType), true,
                        Constants.reasonAcquire);
                }
                else
                {
                    LOGGER.log(Level.WARNING, "Legion " + getLongMarkerName()
                        + " attempting to acquire one " + angelType
                        + " failed!!");
                }
            }
        }
        angelsToAcquire--;
        if (angelsToAcquire <= 0)
        {
            this.decisions = null;
            game.doneAcquiringAngels();
        }
    }

    int getBattleTally()
    {
        return battleTally;
    }

    void clearBattleTally()
    {
        battleTally = 0;
    }

    void clearBattleInfo()
    {
        clearBattleTally();

        for (CreatureServerSide critter : getCreatures())
        {
            critter.heal();
            critter.setBattleInfo(null, null, null);
        }
    }

    void addToBattleTally(int points)
    {
        battleTally += points;
    }

    void addBattleTallyToPoints()
    {
        PlayerServerSide player = getPlayer();
        player.awardPoints(battleTally, this, false);
        clearBattleTally();
    }

    public String getMarkerName()
    {
        return getMarkerName(getMarkerId());
    }

    public static String getMarkerName(String markerId)
    {
        return VariantSupport.getMarkerNamesProperties().getProperty(markerId);
    }

    public static String getLongMarkerName(String markerId)
    {
        StringBuilder sb = new StringBuilder(markerId);
        sb.append(" (");
        sb.append(getMarkerName(markerId));
        sb.append(")");
        return sb.toString();
    }

    public String getLongMarkerName()
    {
        return getLongMarkerName(getMarkerId());
    }

    public Legion getParent()
    {
        return parent;
    }

    @Override
    public PlayerServerSide getPlayer()
    {
        return (PlayerServerSide)super.getPlayer();
    }

    /** Eliminate this legion. */
    void remove(boolean returnCrittersToStacks, boolean updateHistory)
    {
        prepareToRemove(returnCrittersToStacks, updateHistory);
        if (getPlayer() != null)
        {
            getPlayer().removeLegion(this);
        }
    }

    void remove()
    {
        remove(true, true);
    }

    /** Do the cleanup required before this legion can be removed. */
    void prepareToRemove(boolean returnCrittersToStacks, boolean updateHistory)
    {
        LOGGER.log(Level.INFO, "Legion " + getMarkerId()
            + getCreatures().toString() + " is eliminated");
        if (getHeight() > 0)
        {
            // Return immortals to the stacks, others to the Graveyard
            Iterator<CreatureServerSide> it = getCreatures().iterator();
            while (it.hasNext())
            {
                CreatureServerSide critter = it.next();
                prepareToRemoveCritter(critter, returnCrittersToStacks,
                    updateHistory);
                it.remove();
            }
        }

        // TODO during replay / and/or if returnCrToSt is not set,
        // this is probably unnecessary and creates enormous amount
        // of extra messages during reload of a long game.
        // But its safer, there might be a small risk that caretaker
        // gets empty even if its not really empty???
        // To be safe, we just do it...
        /*
        if (returnCrittersToStacks)
        {
            game.getCaretaker().resurrectImmortals();
        }
        */

        // 8.2.2015: Moved to GameServerSide.finishBattle()
        // game.getCaretaker().resurrectImmortals();

        // Let the clients clean up the legion marker, etc.
        if (updateHistory)
        {
            game.getServer().allRemoveLegion(this);
        }
        if (getPlayer() != null)
        {
            getPlayer().addMarkerAvailable(getMarkerId());
        }
    }

    void moveToHex(MasterHex hex, EntrySide entrySide, boolean teleported,
        CreatureType teleportingLord)
    {
        PlayerServerSide player = getPlayer();

        setCurrentHex(hex);
        setMoved(true);

        setEntrySide(entrySide);

        // If we teleported, no more teleports are allowed this turn.
        if (teleported)
        {
            setTeleported(true);
            player.setTeleported(true);
        }

        LOGGER
            .log(
                Level.INFO,
                "Legion "
                    + getLongMarkerName()
                    + " in "
                    + getStartingHex()
                    + (teleported ? (game.getNumEnemyLegions(hex, getPlayer()) > 0 ? " titan teleports "
                        : " tower teleports (" + teleportingLord + ") ")
                        : " moves ") + "to " + hex.getDescription()
                    + " entering on " + entrySide);
    }

    boolean hasConventionalMove()
    {
        return game.hasConventionalMove(this, getCurrentHex(), getPlayer()
            .getMovementRoll(), false);
    }

    void undoMove()
    {
        if (hasMoved())
        {
            // If this legion teleported, allow teleporting again.
            if (hasTeleported())
            {
                setTeleported(false);
                getPlayer().setTeleported(false);
            }

            setCurrentHex(startingHex);

            setMoved(false);
            LOGGER.log(Level.INFO, "Legion " + this + " undoes its move");
        }
    }

    /** Called at end of player turn. */
    void commitMove()
    {
        startingHex = getCurrentHex();
        setMoved(false);
        setRecruit(null);
    }

    /**
     * hasMoved() is a separate check, so that this function can be used in
     * battle as well as during the muster phase.
     * TODO move up
     */
    boolean canRecruit()
    {
        return (getRecruit() == null && getHeight() <= 6
            && !getPlayer().isDead() && !(game.findEligibleRecruits(this,
            getCurrentHex()).isEmpty()));
    }

    String cantRecruitBecause()
    {
        CreatureType recruit = getRecruit();
        if (recruit != null)
        {
            return "Has already recruited " + recruit.getName();
        }
        if (getHeight() > 6)
        {
            return "Legion already 7 high";
        }
        if (getPlayer().isDead())
        {
            return "Player is dead";
        }
        if (game.findEligibleRecruits(this, getCurrentHex()).isEmpty())
        {
            return "No eligible recruits";
        }
        return null;
    }

    void undoRecruit()
    {
        if (hasRecruited())
        {
            CreatureType creature = getRecruit();
            game.getCaretaker().putOneBack(creature);
            removeCreature(creature, false, true);
            setRecruit(null);
            LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
                + " undoes its recruit");
        }
        else
        {
            LOGGER.warning("Legion " + getMarkerId()
                + " can't undoRecruit: hasRecruited() is false?");
        }
    }

    void editRemoveCreature(CreatureType creature)
    {
        removeCreature(creature, false, true);
        game.getCaretaker().putOneBack(creature);
    }

    /*
     * Can't use undoRecruit, because undoRecruit directly removes it,
     * and the iterator in removeAllDead removes as well
     * Even if not so, undoRecruit removes the first it finds, not
     * the one which was reinforced.
     */
    void undoReinforcement()
    {
        if (getRecruit() != null)
        {
            CreatureType creature = getRecruit();
            game.getCaretaker().putOneBack(creature);
            setRecruit(null);
            LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
                + " undoes its reinforcement");
        }
    }

    /**
     * Return true if this legion can summon.
     * TODO likely candidate for pulling up
     */
    boolean canSummonAngel()
    {
        PlayerServerSide player = getPlayer();
        if (getHeight() >= 7 || player.hasSummoned())
        {
            return false;
        }
        return !game.findLegionsWithSummonables(this).isEmpty();
    }

    MasterHex getStartingHex()
    {
        return startingHex;
    }

    /** Add a creature to this legion.  If takeFromStack is true,
      * then do this only if such a creature remains in the stacks,
      * and decrement the number of this creature type remaining.
      */
    boolean addCreature(CreatureType creature, boolean takeFromStack)
    {
        assert getHeight() < 7
            || (getHeight() == 7 && game.getTurnNumber() == 1) : "Tried "
            + "to add to 7-high legion!";
        if (takeFromStack)
        {
            Caretaker caretaker = game.getCaretaker();
            if (caretaker.getAvailableCount(creature) > 0)
            {
                caretaker.takeOne(creature);
                int count = caretaker.getAvailableCount(creature);
                LOGGER.log(Level.INFO, "Added " + creature.toString() + " to "
                    + getMarkerId() + " - now there are " + count + " left.");
            }
            else
            {
                LOGGER.log(Level.SEVERE,
                    "Tried to addCreature " + creature.toString()
                        + " to legion " + getMarkerId()
                        + " when there were none of those left!");
                return false;
            }
        }

        getCreatures().add(new CreatureServerSide(creature, this, game));
        return true;
    }

    /** Remove the creature in position i in the legion.  Return the
     removed creature. Put immortal creatures back on the stack
     and others to the Graveyard if returnImmortalToStack is true. */
    CreatureType removeCreature(int i, boolean returnToStack,
        boolean disbandIfEmpty)
    {
        Creature critter = getCreatures().remove(i);

        // If the creature is an immortal, put it back in the stacks.
        if (returnToStack)
        {
            if (critter.isImmortal())
            {
                game.getCaretaker().putOneBack(critter.getType());
            }
            else
            {
                game.getCaretaker().putDeadOne(critter.getType());
            }
        }

        // If there are no critters left, disband the legion.
        if (disbandIfEmpty && getHeight() == 0)
        {
            remove(false, true);
        }
        // return a Creature, not a Critter
        return critter.getType();
    }

    /** Remove the first creature matching the passed creature's type
     from the legion.  Return the removed creature. */
    CreatureType removeCreature(CreatureType creature,
        boolean returnImmortalToStack, boolean disbandIfEmpty)
    {
        // indexOf wants the same object, not just the same type.
        // So use getCritter() to get the correct object.
        Creature critter = getCritter(creature);
        if (critter == null)
        {
            LOGGER.warning("Attempt to remove creature type "
                + creature.getName() + " from legion " + this.getMarkerId()
                + " but no such critter can be found!");
            return null;
        }
        else
        {
            int i = getCreatures().indexOf(critter);
            return removeCreature(i, returnImmortalToStack, disbandIfEmpty);
        }
    }

    /** Do the cleanup associated with removing the critter from this
     *  legion.  Do not actually remove it, to prevent co-modification
     *  errors.  Do not disband the legion if empty, since the critter
     *  has not actually been removed. */
    void prepareToRemoveCritter(Creature critter, boolean returnToStacks,
        boolean updateHistory)
    {
        if (critter == null || !getCreatures().contains(critter))
        {
            LOGGER.log(Level.SEVERE,
                "Called prepareToRemoveCritter with bad critter");
            return;
        }
        // Put even immortal creatures in the graveyard; they will be
        // pulled back out later.
        if (returnToStacks)
        {
            game.getCaretaker().putDeadOne(critter.getType());
        }
        if (updateHistory)
        {
            game.removeCreatureEvent(this, critter.getType(),
                Constants.reasonKilled);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CreatureServerSide> getCreatures()
    {
        return (List<CreatureServerSide>)super.getCreatures();
    }

    /**
     * TODO avoid index-based access
     */
    CreatureServerSide getCritter(int i)
    {
        return getCreatures().get(i);
    }

    void addCritter(CreatureServerSide critter)
    {
        getCreatures().add(critter);
        critter.setLegion(this);
    }

    /** Return the first critter with a matching tag. */
    CreatureServerSide getCritterByTag(int tag)
    {
        for (CreatureServerSide critter : getCreatures())
        {
            if (tag == critter.getTag())
            {
                return critter;
            }
        }
        return null;
    }

    /** Move critter to the first position in the critters list.
     *  Return true if it was moved. */
    boolean moveToTop(CreatureServerSide critter)
    {
        int i = getCreatures().indexOf(critter);
        if (i <= 0)
        {
            // Not found, or already first in the list.
            return false;
        }
        else
        {
            getCreatures().remove(i);
            getCreatures().add(0, critter);
            return true;
        }
    }

    /** Gets the first critter in this legion with the same creature
     type as the passed creature. */
    Creature getCritter(CreatureType creatureType)
    {
        for (Creature creature : getCreatures())
        {
            if (creature.getType().equals(creatureType))
            {
                return creature;
            }
        }
        return null;
    }

    /**
     * Sort critters into descending order of importance.
     *
     * TODO maybe a SortedSet would be better instead of sorting every now and then
     */
    void sortCritters()
    {
        Collections.sort(getCreatures(), Creature.IMPORTANCE_ORDER);
    }

    /** Recombine this legion into another legion. Only remove this
     legion from the Player if remove is true.  If it's false, the
     caller is responsible for removing this legion, which can avoid
     concurrent access problems. Someone needs to call
     MasterBoard.alignLegions() on the remaining legion's hexLabel
     after the recombined legion is actually removed. */
    void recombine(Legion legion, boolean remove)
    {
        // Sanity check
        if (legion == this)
        {
            LOGGER.log(Level.WARNING,
                "Tried to recombine a legion with itself!");
            return;
        }
        for (Creature critter : getCreatures())
        {
            ((LegionServerSide)legion).addCreature(critter.getType(), false);
        }

        if (remove)
        {
            remove(false, false);
        }
        else
        {
            prepareToRemove(false, false);
        }

        LOGGER.log(Level.INFO, "Legion " + this + " recombined into legion "
            + legion);

        sortCritters();

        // Let the clients know that the legions have recombined.
        game.getServer().undidSplit(this, legion, true, game.getTurnNumber());
    }

    /**
     * Split off creatures into a new legion using legion marker markerId.
     * (Or the first available marker, if markerId is null.)
     * Return the new legion, or null if there's an error.
     */
    LegionServerSide split(List<CreatureType> creatures, String newMarkerId)
    {
        assert newMarkerId != null : "We need a marker to split";
        PlayerServerSide player = getPlayer();

        player.selectMarkerId(newMarkerId);
        LegionServerSide newLegion = new LegionServerSide(newMarkerId, this,
            getCurrentHex(), getCurrentHex(), getPlayer(), game);

        Iterator<CreatureType> it = creatures.iterator();
        while (it.hasNext())
        {
            CreatureType creature = it.next();
            creature = removeCreature(creature, false, false);
            if (creature == null)
            {
                // Abort the split.
                LOGGER
                    .warning("Split aborted since removeCreature(..) returned null");
                newLegion.recombine(this, true);
                return null;
            }
            newLegion.addCreature(creature, false);
        }

        player.addLegion(newLegion);

        game.getServer().allUpdatePlayerInfo("Split");
        LOGGER.log(Level.INFO, newLegion.getHeight()
            + " creatures are split off from legion " + this
            + " into new legion " + newLegion);

        sortCritters();
        newLegion.sortCritters();

        // game.getServer().allTellLegionLocation(newMarkerId);

        return newLegion;
    }

    /**
     * List the lords eligible to teleport this legion to hexLabel.
     */
    List<CreatureType> listTeleportingLords(MasterHex hex)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        List<CreatureType> lords = new ArrayList<CreatureType>();

        // Titan teleport
        if (game.getNumEnemyLegions(hex, getPlayer()) >= 1)
        {
            for (Creature creature : getCreatures())
            {
                if (creature.isTitan())
                {
                    lords.add(creature.getType());
                }
            }
        }

        // Tower teleport
        else
        {
            for (Creature critter : getCreatures())
            {
                if (critter.isLord())
                {
                    if (!lords.contains(critter))
                    {
                        lords.add(critter.getType());
                    }
                }
            }
        }

        return lords;
    }

    /**
     * Legions are sorted in descending order of total point value,
     * with the titan legion always coming first.
     *
     * TODO This is inconsistent with equals() which means the Comparable
     *      contract is not fulfilled. Probably better of in a Comparator
     *      in any case.
     */
    public int compareTo(LegionServerSide other)
    {
        if (hasTitan())
        {
            return Integer.MIN_VALUE;
        }
        else if (other.hasTitan())
        {
            return Integer.MAX_VALUE;
        }
        else
        {
            return (other.getPointValue() - this.getPointValue());
        }
    }

    @Override
    public void addCreature(CreatureType type)
    {
        addCreature(type, true);
    }

    @Override
    public void removeCreature(CreatureType type)
    {
        removeCreature(type, true, true);
    }
}
