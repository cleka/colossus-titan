package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;


public abstract class Legion
{
    /**
     * A comparator to order legions by points, with Titan armies first.
     *
     * This only works properly if all legions are owned by the same player.
     * The case of two legions with titans is not handled.
     *
     * WARNING: This is not consistent with equals() since two legions with
     * the same point value are considered equal.
     */
    public static final Comparator<Legion> ORDER_TITAN_THEN_POINTS = new Comparator<Legion>()
    {
        /**
         * Legions are sorted in descending order of known total point value,
         * with the titan legion always coming first.
         *
         * Really only useful for comparing legions of one player.
         */
        public int compare(Legion o1, Legion o2)
        {
            if (o1.hasTitan())
            {
                return Integer.MIN_VALUE;
            }
            else if (o2.hasTitan())
            {
                return Integer.MAX_VALUE;
            }
            else
            {
                return (o2.getPointValue() - o1.getPointValue());
            }
        }
    };

    /**
     * A comparator to order legions by points, with Titan armies first.
     * If same points, by MarkerId.
     *
     * This only works properly if all legions are owned by the same player.
     * The case of two legions with titans is not handled.
     */
    public static final Comparator<Legion> ORDER_TITAN_THEN_POINTS_THEN_MARKER = new Comparator<Legion>()
    {
        /**
         * Legions are sorted in descending order of known total point value,
         * with the titan legion always coming first, and same points by
         * markerId. (Otherwise some legions could be "equals" which is not
         * a good idea when one wants to store them in a sorted Set.
         *
         * Really only useful for comparing legions of one player.
         */
        public int compare(Legion o1, Legion o2)
        {
            int titan_then_points = ORDER_TITAN_THEN_POINTS.compare(o1, o2);
            if (titan_then_points != 0)
            {
                return (titan_then_points);
            }
            else
            {
                return o1.getMarkerId().compareTo(o2.getMarkerId());
            }
        }
    };

    /**
     * The player/game combination owning this Legion.
     *
     * Never null.
     */
    private final Player player;

    /**
     * The current position of the legion on the masterboard.
     *
     * Never null.
     */
    private MasterHex currentHex;

    /**
     * The creatures in this legion.
     */
    private final List<Creature> creatures = new ArrayList<Creature>();

    /**
     * The ID of the marker of this legion.
     *
     * Used as identifier for serialization purposes. Never null.
     */
    private final String markerId;

    /**
     * Flag if the legion has moved in the current masterboard round.
     */
    private boolean moved;

    /**
     * Flag if the legion has teleported in the current masterboard round.
     */
    private boolean teleported;

    /**
     * The side this legion entered a battle in.
     */
    private EntrySide entrySide = EntrySide.NOT_SET;

    protected List<AcquirableDecision> decisions = null;
    protected int angelsToAcquire;

    /**
     * Has doSplit already been sent to server. Only needed on client side,
     * but this way we avoid casting.
     */
    private boolean splitRequestSent = false;

    /**
     * Has undoSplit already been sent to server. Only needed on client side,
     * but this way we avoid casting. Marked in both parent and child.
     */
    private boolean undoSplitRequestSent = false;

    /**
     * The creature recruited in last recruit phase
     */
    private CreatureType recruit;

    /**
     * Flag to remember a "skip (split|move|recruit) this time"
     */
    private boolean skipThisTime = false;

    /**
     * Flag to remember that legion has been visited this phase
     */

    private boolean visitedThisPhase = false;

    // TODO legions should be created through factory from the player instances
    public Legion(final Player player, String markerId, MasterHex hex)
    {
        assert player != null : "Legion has to have a player";
        assert markerId != null : "Legion has to have a markerId";
        assert hex != null : "Legion nees to be placed somewhere";
        this.player = player;
        this.markerId = markerId;
        this.currentHex = hex;
    }

    /**
     * Retrieves the player this legion belongs to.
     *
     * @return The matching player. Never null.
     */
    public Player getPlayer()
    {
        return player;
    }

    /**
     * Places the legion into the new position.
     *
     * @param newPosition the hex that will be the new position. Not null.
     * @see #getCurrentHex()
     */
    public void setCurrentHex(MasterHex newPosition)
    {
        assert newPosition != null : "Need position to move legion to";
        this.currentHex = newPosition;
    }

    /**
     * Returns the current position of the legion.
     *
     * @return the hex the legion currently is on.
     *
     * @see #setCurrentHex(MasterHex)
     */
    public MasterHex getCurrentHex()
    {
        assert currentHex != null : "getCurrentHex() called on Legion before position was set";
        return currentHex;
    }

    /**
     * TODO should be an unmodifiable List, but can't at the moment since both
     * derived classes and users might still expect to change it
     * TODO should be List<Creature>, but subtypes are still covariant
     */
    public List<? extends Creature> getCreatures()
    {
        return creatures;
    }

    public String getMarkerId()
    {
        return markerId;
    }

    public String getLongMarkerId()
    {
        return markerId + "-" + getPlayer().getColor().getName();
    }

    public boolean hasTitan()
    {
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isTitan())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @return returns the Titan Creature, if this legions has the titan,
     * or null if it hasn't.
     */
    public Creature getTitan()
    {
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isTitan())
            {
                return critter;
            }
        }
        return null;
    }

    /**
     * Returns the number of creatures in this legion.
     *
     * @return the number of creatures in the legion
     */
    public int getHeight()
    {
        return getCreatures().size();
    }

    public void setSplitRequestSent(boolean val)
    {
        this.splitRequestSent = val;
    }

    public boolean getSplitRequestSent()
    {
        return this.splitRequestSent;
    }

    public void setUndoSplitRequestSent(boolean val)
    {
        this.undoSplitRequestSent = val;
    }

    public boolean getUndoSplitRequestSent()
    {
        return this.undoSplitRequestSent;
    }

    public void setMoved(boolean moved)
    {
        this.moved = moved;
        this.skipThisTime = false;
    }

    public boolean hasMoved()
    {
        return moved;
    }

    public void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }

    public boolean hasTeleported()
    {
        return teleported;
    }

    public void setSkipThisTime(boolean skipIt)
    {
        this.skipThisTime = skipIt;
    }

    public boolean getSkipThisTime()
    {
        return skipThisTime;
    }

    public void setVisitedThisPhase(boolean visited)
    {
        this.visitedThisPhase = visited;
    }

    public boolean getVisitedThisPhase()
    {
        return visitedThisPhase;
    }

    public boolean contains(CreatureType type)
    {
        return getCreatureTypes().contains(type);
    }

    public abstract void addCreature(CreatureType type);

    public abstract void removeCreature(CreatureType type);

    public void setEntrySide(EntrySide entrySide)
    {
        this.entrySide = entrySide;
    }

    public EntrySide getEntrySide()
    {
        return entrySide;
    }

    /**
     * TODO unify between the two derived classes if possible -- the handling of Titans
     *      is quite different, although it should have the same result
     */
    public abstract int getPointValue();

    public CreatureType getRecruit()
    {
        return recruit;
    }

    public void setRecruit(CreatureType recruit)
    {
        this.recruit = recruit;
    }

    public boolean hasRecruited()
    {
        return (recruit != null);
    }

    public boolean hasSummonable()
    {
        for (Creature creature : getCreatures())
        {
            if (creature.getType().isSummonable())
            {
                return true;
            }
        }
        return false;
    }

    public boolean canFlee()
    {
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isLord())
            {
                return false;
            }
        }
        return true;
    }

    public int numCreature(CreatureType creatureType)
    {
        int count = 0;
        for (Creature critter : getCreatures())
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
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isLord())
            {
                count++;
            }
        }
        return count;
    }

    public int numRangestrikers()
    {
        int count = 0;
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isRangestriker())
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Calculate the acquirableDecisions and store them in the legion.
     * @param score
     * @param points
     */
    public void setupAcquirableDecisions(int score, int points)
    {
        this.decisions = calculateAcquirableDecisions(score, points);
    }

    /**
     * From the given score, awarding given points, calculate the choices for
     * each threshold that will be crossed. E.g. 375+150 => 525 will cross
     * 400 and 500, so one has to make two decisions:
     *  400: take angel (or not);
     *  500: take angel, archangel (or nothing).
     * This only calculates them, does not set them in the legion yet; so a client
     * or AI could use this for theoretical calculations "how much / which angels
     * would I get if..." without modifying the legions state itself.
     * The limits for "which one can get" due to legion height, creatures left count
     * and terrain are considered (implicitly, because findEligibleAngels(tmpScore)
     * checks them).
     *
     * @param score Current score of player
     * @param points Points to be added which entitle to acquiring
     * @return List of decisions
     */
    List<AcquirableDecision> calculateAcquirableDecisions(int score, int points)
    {
        ArrayList<AcquirableDecision> acquirablesDecisions = new ArrayList<AcquirableDecision>();

        // Example: Start with (score) 375, earn (points) 150 => 525

        int value = player.getGame().getVariant()
            .getAcquirableRecruitmentsValue();
        // 100
        int tmpScore = score; // 375
        int tmpPoints = points; // 150

        // round Score down, and tmpPoints by the same amount.
        // this allow to keep all points
        int round = (tmpScore % value); //  75
        tmpScore -= round; // 300
        tmpPoints += round; // 225

        List<CreatureType> recruits;

        // @TODO: the constraint by height would make only sense, if askAquire's
        // would be fired sequentially - 2nd not before decision response for 1st
        // from client did arrive. Right now they are fired all at same time
        // (and have to), because client (e.g. human player) should get all of the
        // choices at once, to take e.g. the 500 archangel and skip the 400 angel.
        // AI does not handle that well yet, and humans get several modal dialogs.
        // Should be improved generally...
        while (tmpPoints >= value)
        {
            tmpScore += value; // 400   500
            tmpPoints -= value; // 125    25

            recruits = findEligibleAngels(tmpScore);

            if ((recruits != null) && (!recruits.isEmpty()))
            {
                AcquirableDecision decision = new AcquirableDecision(this,
                    tmpScore, recruits);
                // {legion, 400, [Angel]}   {legion, 500, [Angel, Archangel]}

                acquirablesDecisions.add(decision); //  [d1]       [d1, d2]
                angelsToAcquire++; // 1       2
            }
        }

        return acquirablesDecisions; // 2 decisions
    }

    /**
     * Retrieves a list of all creature types in this legion.
     *
     * This matches getCreatures() but lists the types instead of the
     * individual creatures.
     *
     * @return A list of all creature types in this legion.
     */
    public List<CreatureType> getCreatureTypes()
    {
        List<CreatureType> result = new ArrayList<CreatureType>();
        for (Creature creature : getCreatures())
        {
            result.add(creature.getType());
        }
        return result;
    }

    /**
     * Calculate which angels this legion can get in its current land
     * when crossing the given points threshold
     *
     * @param points Score threshold (100, ..., 400, 500) for which to get angel
     * @return list of creatures that can be get at that threshold
     */
    public List<CreatureType> findEligibleAngels(int points)
    {
        MasterBoardTerrain terrain = getCurrentHex().getTerrain();
        return player.getGame().findAvailableEligibleAngels(terrain, points);
    }

    /**
     * Returns the markerId for debug and serialisation purposes.
     *
     * Since this is relevant for the network protocol, the method
     * is declared final.
     */
    @Override
    public final String toString()
    {
        return getMarkerId();
    }

    @Override
    public final int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((markerId == null) ? 0 : markerId.hashCode());
        return result;
    }

    /**
     * Two legions are considered equal if they have the same marker.
     *
     * Even though contents may change over time, we consider two legions
     * to be the same as long as they have the same marker. This notion of
     * equality is used throughout the code, so we enforce it by having both
     * {{@link #equals(Object)} and {@link #hashCode()} declared final.
     */
    @Override
    public final boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Legion other = (Legion)obj;
        if (markerId == null)
        {
            if (other.markerId != null)
                return false;
        }
        else if (!markerId.equals(other.markerId))
            return false;
        return true;
    }

    /**
     * Data for one pending decision. For example, for crossing the 500
     * there will be a decision, whether the player takes for this legion
     * an angel or an archangel.
     *
     * TODO this should not be here, it should probably be modelled as a
     * game action
     */
    public class AcquirableDecision
    {
        private final Legion legion;
        private final int points;
        private final List<CreatureType> acquirables;

        public AcquirableDecision(Legion legion, int points,
            List<CreatureType> acquirables)
        {
            this.legion = legion;
            this.points = points;
            this.acquirables = acquirables;
        }

        public List<CreatureType> getAcquirables()
        {
            return acquirables;
        }

        // so far not used anywhere
        public int getPoints()
        {
            return points;
        }

        // so far not used anywhere
        public Legion getLegion()
        {
            return legion;
        }
    }
}
