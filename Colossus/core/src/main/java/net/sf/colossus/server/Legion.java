package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.BattleMap;
import net.sf.colossus.game.PlayerState;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public final class Legion extends net.sf.colossus.game.Legion implements
    Comparable<Legion>
{
    private static final Logger LOGGER = Logger.getLogger(Legion.class
        .getName());

    private final String parentId;
    private String currentHexLabel;
    private String startingHexLabel;
    private boolean moved;
    private int entrySide = -1;
    private boolean teleported;
    private String recruitName;
    private int battleTally;
    private final Game game;
    private int angelsToAcquire;

    /**
     * Creates a new Legion instance.
     * 
     * Not that this class does not constraint the number of creatures. In a 
     * normal Titan game it is between 0 and 8 creatures, but this class does
     * not enforce this.
     */
    public Legion(String markerId, String parentId, String currentHexLabel,
        String startingHexLabel, PlayerState player, Game game,
        Creature... creatureTypes)
    {
        // TODO we just fake a playerstate here
        super(player, markerId);
        this.parentId = parentId;
        // Sanity check
        if (parentId != null && parentId.equals(markerId))
        {
            parentId = null;
        }
        this.currentHexLabel = currentHexLabel;
        this.startingHexLabel = startingHexLabel;
        this.game = game;

        for (Creature creature : creatureTypes)
        {
            assert creature != null : "Null creature not allowed";
            getCritters().add(new Critter(creature, this, game));
        }
    }

    static Legion getStartingLegion(String markerId, String hexLabel,
        PlayerState player, Game game)
    {
        Creature[] startCre = TerrainRecruitLoader.getStartingCreatures(game
            .getVariant().getMasterBoard().getHexByLabel(hexLabel)
            .getTerrain());
        Legion legion = new Legion(markerId, null, hexLabel, hexLabel, player,
            game, (Creature)VariantSupport.getCurrentVariant()
                .getCreatureByName(Constants.titan), (Creature)VariantSupport
                .getCurrentVariant().getCreatureByName(
                    TerrainRecruitLoader.getPrimaryAcquirable()), startCre[2],
            startCre[2], startCre[0], startCre[0], startCre[1], startCre[1]);

        Iterator<Critter> it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            game.getCaretaker().takeOne(critter.getCreature());
        }
        return legion;
    }

    public int getPointValue()
    {
        int pointValue = 0;
        for (Critter critter : getCritters())
        {
            pointValue += critter.getPointValue();
        }
        return pointValue;
    }

    // Example: Start with 375, earn 150
    void addPoints(int points)
    {
        if (game == null)
        {
            return;
        }
        Player player = getPlayer();
        int score = player.getScore(); // 375
        player.addPoints(points); // 375 + 150 = 525
        int value = TerrainRecruitLoader.getAcquirableRecruitmentsValue();
        // 100
        int tmpScore = score; // 375
        int tmpPoints = points; // 150

        // round Score down, and tmpPoints by the same amount.
        // this allow to keep all points
        int round = (tmpScore % value); //  75
        tmpScore -= round; // 300
        tmpPoints += round; // 225

        List<String> recruits;

        while ((getHeight() < 7) && (tmpPoints >= value))
        {
            tmpScore += value; // 400   500
            tmpPoints -= value; // 125    25
            recruits = game.findEligibleAngels(this, tmpScore);
            if ((recruits != null) && (!recruits.isEmpty()))
            {
                angelsToAcquire++; // 1       2
                game.askAcquireAngel(getPlayerName(), getMarkerId(), recruits);
            }
        }
    }

    void addAngel(String angelType)
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
                Creature angel = (Creature)game.getVariant()
                    .getCreatureByName(angelType);
                if (angel != null)
                {
                    addCreature(angel, true);
                    LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
                        + " acquires one " + angelType);
                    game.getServer().allTellAddCreature(getMarkerId(),
                        angelType, true, Constants.reasonAcquire);
                }
            }
        }
        angelsToAcquire--;
        if (angelsToAcquire <= 0)
        {
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

        for (Critter critter : getCritters())
        {
            critter.heal();
            critter.addBattleInfo(null, null, null);
        }
    }

    void addToBattleTally(int points)
    {
        battleTally += points;
    }

    void addBattleTallyToPoints()
    {
        addPoints(battleTally);
        clearBattleTally();
    }

    public String getMarkerName()
    {
        return getMarkerName(markerId);
    }

    public static String getMarkerName(String markerId)
    {
        return VariantSupport.getMarkerNamesProperties().getProperty(markerId);
    }

    public static String getLongMarkerName(String markerId)
    {
        StringBuffer sb = new StringBuffer(markerId);
        sb.append(" (");
        sb.append(getMarkerName(markerId));
        sb.append(")");
        return sb.toString();
    }

    public String getLongMarkerName()
    {
        return getLongMarkerName(markerId);
    }

    public String getParentId()
    {
        return parentId;
    }

    Legion getParent()
    {
        return getPlayer().getLegionByMarkerId(parentId);
    }

    @Override
    public String toString()
    {
        return markerId;
    }

    /** Return a list of imageNames for all critters in this legion. */
    List<String> getImageNames()
    {
        sortCritters();
        List<String> imageNames = new ArrayList<String>();
        Iterator<Critter> it = getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();

            /*
             * Use getName(), not getImageName(), as
             * 1) Chit should be built with actual name to make sure
             * they find the Creature ;
             * 2) it seems that elements in this List somehow find their
             * way to Creature.getCreatureByName(), and if one uses the
             * Image Name in there, hell breaks loose.
             */
            String name = critter.getName();
            if (name == null || name.equals(""))
            {
                LOGGER.log(Level.SEVERE, "getImagenames: null or empty name");
            }
            imageNames.add(critter.getName());
        }
        return imageNames;
    }

    String getImageName()
    {
        return markerId;
    }

    boolean canFlee()
    {
        for (Critter critter : getCritters())
        {
            if (critter.isLord())
            {
                return false;
            }
        }
        return true;
    }

    int numCreature(CreatureType creatureType)
    {
        int count = 0;
        for (Critter critter : getCritters())
        {
            if (critter.getType().equals(creatureType))
            {
                count++;
            }
        }
        return count;
    }

    public int numLords()
    {
        int count = 0;
        for (Critter critter : getCritters())
        {
            if (critter.isLord())
            {
                count++;
            }
        }
        return count;
    }

    public int numRangestrikers()
    {
        int count = 0;
        for (Critter critter : getCritters())
        {
            if (critter.isRangestriker())
            {
                count++;
            }
        }
        return count;
    }

    public boolean hasTitan()
    {
        for (Critter critter : getCritters())
        {
            if (critter.isTitan())
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasSummonable()
    {
        for (Critter critter : getCritters())
        {
            if (critter.isSummonable())
            {
                return true;
            }
        }
        return false;
    }

    public int getHeight()
    {
        return getCritters().size();
    }

    public String getPlayerName()
    {
        return getPlayer().getName();
    }

    @Override
    public Player getPlayer()
    {
        return (Player)super.getPlayer();
    }

    public boolean hasMoved()
    {
        return moved;
    }

    void setMoved(boolean moved)
    {
        this.moved = moved;
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
        LOGGER.log(Level.INFO, "Legion " + markerId + getCritters().toString()
            + " is eliminated");
        if (getHeight() > 0)
        {
            // Return immortals to the stacks, others to the Graveyard
            Iterator<Critter> it = getCritters().iterator();
            while (it.hasNext())
            {
                Critter critter = it.next();
                prepareToRemoveCritter(critter, returnCrittersToStacks,
                    updateHistory);
                it.remove();
            }
        }

        game.getCaretaker().resurrectImmortals();

        // Let the clients clean up the legion marker, etc.
        if (updateHistory)
        {
            game.getServer().allRemoveLegion(markerId);
        }
        if (getPlayer() != null)
        {
            getPlayer().addLegionMarker(getMarkerId());
        }
    }

    void moveToHex(MasterHex hex, String entrySide, boolean teleported,
        String teleportingLord)
    {
        Player player = getPlayer();
        String hexLabel = hex.getLabel();

        currentHexLabel = hexLabel;
        moved = true;

        setEntrySide(entrySide);

        // If we teleported, no more teleports are allowed this turn.
        if (teleported)
        {
            setTeleported(true);
            player.setTeleported(true);
        }

        LOGGER.log(Level.INFO, "Legion "
            + getLongMarkerName()
            + " in "
            + getStartingHexLabel()
            + (teleported ? (game.getNumEnemyLegions(hexLabel, game
                .getPlayer(getPlayerName())) > 0 ? " titan teleports "
                : " tower teleports (" + teleportingLord + ") ") : " moves ")
            + "to " + hex.getDescription() + " entering on " + entrySide);
    }

    boolean hasConventionalMove()
    {
        return !game.listNormalMoves(this, getCurrentHex(),
            getPlayer().getMovementRoll(), false).isEmpty();
    }

    void undoMove()
    {
        if (moved)
        {
            // If this legion teleported, allow teleporting again.
            if (hasTeleported())
            {
                setTeleported(false);
                getPlayer().setTeleported(false);
            }

            currentHexLabel = startingHexLabel;

            moved = false;
            LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
                + " undoes its move");
        }
    }

    /** Called at end of player turn. */
    void commitMove()
    {
        startingHexLabel = currentHexLabel;
        moved = false;
        recruitName = null;
    }

    boolean hasRecruited()
    {
        return (recruitName != null);
    }

    String getRecruitName()
    {
        return recruitName;
    }

    void setRecruitName(String recruitName)
    {
        this.recruitName = recruitName;
    }

    /** hasMoved() is a separate check, so that this function can be used in
     *  battle as well as during the muster phase. */
    boolean canRecruit()
    {
        return (recruitName == null && getHeight() <= 6
            && !getPlayer().isDead() && !(game.findEligibleRecruits(
            getMarkerId(), getCurrentHexLabel()).isEmpty()));
    }

    void undoRecruit()
    {
        if (recruitName != null)
        {
            Creature creature = (Creature)game.getVariant().getCreatureByName(
                recruitName);
            game.getCaretaker().putOneBack(creature);
            removeCreature(creature, false, true);
            recruitName = null;
            LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
                + " undoes its recruit");
        }
    }

    /** Return true if this legion can summon. */
    boolean canSummonAngel()
    {
        Player player = getPlayer();
        if (getHeight() >= 7 || player.hasSummoned())
        {
            return false;
        }
        return !game.findSummonableAngels(markerId).isEmpty();
    }

    String getCurrentHexLabel()
    {
        return currentHexLabel;
    }

    MasterHex getCurrentHex()
    {
        return game.getVariant().getMasterBoard().getHexByLabel(
            currentHexLabel);
    }

    String getStartingHexLabel()
    {
        return startingHexLabel;
    }

    MasterHex getStartingHex()
    {
        return game.getVariant().getMasterBoard().getHexByLabel(
            startingHexLabel);
    }

    void setEntrySide(int entrySide)
    {
        this.entrySide = entrySide;
    }

    void setEntrySide(String entrySide)
    {
        this.entrySide = BattleMap.entrySideNum(entrySide);
    }

    int getEntrySide()
    {
        return entrySide;
    }

    boolean hasTeleported()
    {
        return teleported;
    }

    void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }

    /** Add a creature to this legion.  If takeFromStack is true,
     then do this only if such a creature remains in the stacks,
     and decrement the number of this creature type remaining. */
    boolean addCreature(Creature creature, boolean takeFromStack)
    {
        if (getHeight() > 7 || (getHeight() == 7 && game.getTurnNumber() > 1))
        {
            LOGGER.log(Level.SEVERE, "Tried to add to 7-high legion!");
            return false;
        }
        if (takeFromStack)
        {
            Caretaker caretaker = game.getCaretaker();
            if (caretaker.getCount(creature) > 0)
            {
                caretaker.takeOne(creature);
            }
            else
            {
                LOGGER.log(Level.SEVERE, "Tried to addCreature "
                    + creature.toString()
                    + " when there were none of those left!");
                return false;
            }
        }

        getCritters().add(new Critter(creature, this, game));
        return true;
    }

    /** Remove the creature in position i in the legion.  Return the
     removed creature. Put immortal creatures back on the stack
     and others to the Graveyard if returnImmortalToStack is true. */
    Creature removeCreature(int i, boolean returnToStack,
        boolean disbandIfEmpty)
    {
        Critter critter = getCritters().remove(i);

        // If the creature is an immortal, put it back in the stacks.
        if (returnToStack)
        {
            if (critter.isImmortal())
            {
                game.getCaretaker().putOneBack(critter.getCreature());
            }
            else
            {
                game.getCaretaker().putDeadOne(critter.getCreature());
            }
        }

        // If there are no critters left, disband the legion.
        if (disbandIfEmpty && getHeight() == 0)
        {
            remove(false, true);
        }
        // return a Creature, not a Critter
        return critter.getCreature();
    }

    /** Remove the first creature matching the passed creature's type
     from the legion.  Return the removed creature. */
    Creature removeCreature(Creature creature, boolean returnImmortalToStack,
        boolean disbandIfEmpty)
    {
        // indexOf wants the same object, not just the same type.
        // So use getCritter() to get the correct object.
        Critter critter = getCritter(creature);
        if (critter == null)
        {
            return null;
        }
        else
        {
            int i = getCritters().indexOf(critter);
            return removeCreature(i, returnImmortalToStack, disbandIfEmpty);
        }
    }

    /** Do the cleanup associated with removing the critter from this
     *  legion.  Do not actually remove it, to prevent comodification
     *  errors.  Do not disband the legion if empty, since the critter
     *  has not actually been removed. */
    void prepareToRemoveCritter(Critter critter, boolean returnToStacks,
        boolean updateHistory)
    {
        if (critter == null || !getCritters().contains(critter))
        {
            LOGGER.log(Level.SEVERE,
                "Called prepareToRemoveCritter with bad critter");
            return;
        }
        // Put even immortal creatures in the graveyard; they will be
        // pulled back out later.
        if (returnToStacks)
        {
            game.getCaretaker().putDeadOne(critter.getCreature());
        }
        if (updateHistory)
        {
            game.removeCreatureEvent(getMarkerId(), critter.getName());
        }
    }

    @SuppressWarnings("unchecked")
    List<Critter> getCritters()
    {
        return (List<Critter>)super.getCreatures();
    }

    Critter getCritter(int i)
    {
        return getCritters().get(i);
    }

    void setCritter(int i, Critter critter)
    {
        getCritters().set(i, critter);
        critter.setLegion(this);
    }

    /** Return the first critter with a matching tag. */
    Critter getCritterByTag(int tag)
    {
        for (Critter critter : getCritters())
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
    boolean moveToTop(Critter critter)
    {
        int i = getCritters().indexOf(critter);
        if (i <= 0)
        {
            // Not found, or already first in the list.
            return false;
        }
        else
        {
            getCritters().remove(i);
            getCritters().add(0, critter);
            return true;
        }
    }

    /** Gets the first critter in this legion with the same creature
     type as the passed creature. */
    Critter getCritter(Creature creatureType)
    {
        for (Critter critter : getCritters())
        {
            if (critter.getType().equals(creatureType))
            {
                return critter;
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
        Collections.sort(getCritters(), Critter.IMPORTANCE_ORDER);
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
        for (Critter critter : getCritters())
        {
            legion.addCreature(critter.getCreature(), false);
        }

        if (remove)
        {
            remove(false, false);
        }
        else
        {
            prepareToRemove(false, false);
        }

        LOGGER.log(Level.INFO, "Legion " + getLongMarkerName()
            + " recombined into legion " + legion.getLongMarkerName());

        sortCritters();

        // Let the clients know that the legions have recombined.
        game.getServer().undidSplit(getMarkerId(), legion.getMarkerId(), true,
            game.getTurnNumber());
    }

    /**
     * Split off creatures into a new legion using legion marker markerId.
     * (Or the first available marker, if markerId is null.)
     * Return the new legion, or null if there's an error.
     */
    Legion split(List<Creature> creatures, String newMarkerId)
    {
        Player player = getPlayer();
        if (newMarkerId == null)
        {
            return null;
        }

        player.selectMarkerId(newMarkerId);
        Legion newLegion = new Legion(newMarkerId, markerId, currentHexLabel,
            currentHexLabel, getPlayer(), game);

        Iterator<Creature> it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = it.next();
            creature = removeCreature(creature, false, false);
            if (creature == null)
            {
                // Abort the split.
                newLegion.recombine(this, true);
                return null;
            }
            newLegion.addCreature(creature, false);
        }

        player.addLegion(newLegion);

        game.getServer().allUpdatePlayerInfo();
        LOGGER.log(Level.INFO, newLegion.getHeight()
            + " creatures are split off from legion " + getLongMarkerName()
            + " into new legion " + newLegion.getLongMarkerName());

        sortCritters();
        newLegion.sortCritters();

        // game.getServer().allTellLegionLocation(newMarkerId);

        return newLegion;
    }

    /** List the lords eligible to teleport this legion to hexLabel,
     *  as strings. */
    List<String> listTeleportingLords(String hexLabel)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        List<String> lords = new ArrayList<String>();

        // Titan teleport
        if (game.getNumEnemyLegions(hexLabel, getPlayer()) >= 1)
        {
            if (hasTitan())
            {
                lords.add(Constants.titan);
            }
        }

        // Tower teleport
        else
        {
            for (Critter critter : getCritters())
            {
                if (critter.isLord())
                {
                    String name = critter.getName();
                    if (!lords.contains(name))
                    {
                        lords.add(name);
                    }
                }
            }
        }

        return lords;
    }

    /** Legions are sorted in descending order of total point value,
     with the titan legion always coming first.  This is inconsistent
     with equals(). */
    public int compareTo(Legion other)
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
}
