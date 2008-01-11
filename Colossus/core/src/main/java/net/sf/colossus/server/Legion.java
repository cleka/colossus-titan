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

    private final String markerId; // Bk03, Rd12, etc.
    private final String parentId;
    private final List<Critter> critters = new ArrayList<Critter>();
    private String currentHexLabel;
    private String startingHexLabel;
    private boolean moved;
    private int entrySide = -1;
    private boolean teleported;
    private String recruitName;
    private int battleTally;
    private final Game game;
    private int angelsToAcquire;

    Legion(String markerId, String parentId, String currentHexLabel,
        String startingHexLabel, Creature creature0, Creature creature1,
        Creature creature2, Creature creature3, Creature creature4,
        Creature creature5, Creature creature6, Creature creature7,
        String playerName, Game game)
    {
        // TODO we just fake a playerstate here
        super(new PlayerState(game, net.sf.colossus.Player
            .getPlayerByName(playerName)));
        this.markerId = markerId;
        this.parentId = parentId;
        // Sanity check
        if (parentId != null && parentId.equals(markerId))
        {
            parentId = null;
        }
        this.currentHexLabel = currentHexLabel;
        this.startingHexLabel = startingHexLabel;
        this.game = game;

        if (creature0 != null)
        {
            critters.add(new Critter(creature0, markerId, game));
        }
        if (creature1 != null)
        {
            critters.add(new Critter(creature1, markerId, game));
        }
        if (creature2 != null)
        {
            critters.add(new Critter(creature2, markerId, game));
        }
        if (creature3 != null)
        {
            critters.add(new Critter(creature3, markerId, game));
        }
        if (creature4 != null)
        {
            critters.add(new Critter(creature4, markerId, game));
        }
        if (creature5 != null)
        {
            critters.add(new Critter(creature5, markerId, game));
        }
        if (creature6 != null)
        {
            critters.add(new Critter(creature6, markerId, game));
        }
        if (creature7 != null)
        {
            critters.add(new Critter(creature7, markerId, game));
        }
    }

    static Legion getStartingLegion(String markerId, String hexLabel,
        String playerName, Game game)
    {
        Creature[] startCre = TerrainRecruitLoader.getStartingCreatures(game
            .getVariant().getMasterBoard().getHexByLabel(hexLabel)
            .getTerrain());
        Legion legion = new Legion(markerId, null, hexLabel, hexLabel,
            (Creature)VariantSupport.getCurrentVariant().getCreatureByName(
                Constants.titan),
            (Creature)VariantSupport.getCurrentVariant().getCreatureByName(
                TerrainRecruitLoader.getPrimaryAcquirable()), startCre[2],
            startCre[2], startCre[0], startCre[0], startCre[1], startCre[1],
            playerName, game);

        Iterator<Critter> it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            game.getCaretaker().takeOne(critter.getCreature());
        }
        return legion;
    }

    static Legion getEmptyLegion(String markerId, String parentId,
        String hexLabel, String playerName, Game game)
    {
        Legion legion = new Legion(markerId, parentId, hexLabel, hexLabel,
            null, null, null, null, null, null, null, null, playerName, game);
        return legion;
    }

    public int getPointValue()
    {
        int pointValue = 0;
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
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
        Player player = getPlayerState();
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

        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
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

    public String getMarkerId()
    {
        return markerId;
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
        return getPlayerState().getLegionByMarkerId(parentId);
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
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (critter.isLord())
            {
                return false;
            }
        }
        return true;
    }

    int numCreature(CreatureType creature)
    {
        int count = 0;
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (critter.getName().equals(creature.getName()))
            {
                count++;
            }
        }
        return count;
    }

    public int numLords()
    {
        int count = 0;
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
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
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (critter.isRangestriker())
            {
                count++;
            }
        }
        return count;
    }

    public boolean hasTitan()
    {
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (critter.isTitan())
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasSummonable()
    {
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (critter.isSummonable())
            {
                return true;
            }
        }
        return false;
    }

    public int getHeight()
    {
        return critters.size();
    }

    public String getPlayerName()
    {
        return getPlayer().getPlayer().getName();
    }

    Player getPlayerState()
    {
        return game.getPlayerByMarkerId(markerId);
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
        if (getPlayerState() != null)
        {
            getPlayerState().removeLegion(this);
        }
    }

    void remove()
    {
        remove(true, true);
    }

    /** Do the cleanup required before this legion can be removed. */
    void prepareToRemove(boolean returnCrittersToStacks, boolean updateHistory)
    {
        LOGGER.log(Level.INFO, "Legion " + markerId + critters.toString()
            + " is eliminated");
        if (getHeight() > 0)
        {
            // Return immortals to the stacks, others to the Graveyard
            Iterator<Critter> it = critters.iterator();
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
        if (getPlayerState() != null)
        {
            getPlayerState().addLegionMarker(getMarkerId());
        }
    }

    void moveToHex(MasterHex hex, String entrySide, boolean teleported,
        String teleportingLord)
    {
        Player player = getPlayerState();
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
            getPlayerState().getMovementRoll(), false).isEmpty();
    }

    void undoMove()
    {
        if (moved)
        {
            // If this legion teleported, allow teleporting again.
            if (hasTeleported())
            {
                setTeleported(false);
                getPlayerState().setTeleported(false);
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
            && !getPlayerState().isDead() && !(game.findEligibleRecruits(
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
        Player player = getPlayerState();
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

        critters.add(new Critter(creature, markerId, game));
        return true;
    }

    /** Remove the creature in position i in the legion.  Return the
     removed creature. Put immortal creatures back on the stack
     and others to the Graveyard if returnImmortalToStack is true. */
    Creature removeCreature(int i, boolean returnToStack,
        boolean disbandIfEmpty)
    {
        Critter critter = critters.remove(i);

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
            int i = critters.indexOf(critter);
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
        if (critter == null || !critters.contains(critter))
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

    List<Critter> getCritters()
    {
        return critters;
    }

    Critter getCritter(int i)
    {
        return critters.get(i);
    }

    void setCritter(int i, Critter critter)
    {
        critters.set(i, critter);
        critter.setMarkerId(markerId);
    }

    /** Return the first critter with a matching tag. */
    Critter getCritterByTag(int tag)
    {
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
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
        int i = critters.indexOf(critter);
        if (i <= 0)
        {
            // Not found, or already first in the list.
            return false;
        }
        else
        {
            critters.remove(i);
            critters.add(0, critter);
            return true;
        }
    }

    /** Gets the first critter in this legion with the same creature
     type as the passed creature. */
    Critter getCritter(Creature creature)
    {
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (critter.getName().equals(creature.getName()))
            {
                return critter;
            }
        }
        return null;
    }

    /** Sort critters into descending order of importance. */
    void sortCritters()
    {
        Collections.sort(critters);
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
        Iterator<Critter> it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
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
        Player player = getPlayerState();
        if (newMarkerId == null)
        {
            return null;
        }

        player.selectMarkerId(newMarkerId);
        Legion newLegion = Legion.getEmptyLegion(newMarkerId, markerId,
            currentHexLabel, getPlayerName(), game);

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
        if (game.getNumEnemyLegions(hexLabel, getPlayerState()) >= 1)
        {
            if (hasTitan())
            {
                lords.add(Constants.titan);
            }
        }

        // Tower teleport
        else
        {
            Iterator<Critter> it = critters.iterator();
            while (it.hasNext())
            {
                Critter critter = it.next();
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
