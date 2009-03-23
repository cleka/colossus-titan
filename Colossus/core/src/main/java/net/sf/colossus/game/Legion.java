package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


public abstract class Legion
{
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
    protected final String markerId;

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
     * Name of the creature recruited in last recruit phase
     */
    private String recruitName;

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
     * Returns the number of creatures in this legion.
     * 
     * @return the number of creatures in the legion
     */
    public int getHeight()
    {
        return getCreatures().size();
    }

    public void setMoved(boolean moved)
    {
        this.moved = moved;
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

    public boolean contains(CreatureType type)
    {
        return getCreatures().contains(type);
    }

    public void setEntrySide(EntrySide entrySide)
    {
        this.entrySide = entrySide;
    }

    public EntrySide getEntrySide()
    {
        return entrySide;
    }

    /**
     * TODO unify between the two derived classes or even better: replace with code
     *      for getting the image 
     */
    public abstract List<String> getImageNames();

    /**
     * TODO unify between the two derived classes if possible -- the handling of Titans
     *      is quite different, although it should have the same result
     */
    public abstract int getPointValue();

    public String getRecruitName()
    {
        return recruitName;
    }

    public void setRecruitName(String recruitName)
    {
        if (recruitName == null || recruitName.equals("null"))
        {
            this.recruitName = null;
        }
        else
        {
            this.recruitName = recruitName;
        }
    }

    public boolean hasRecruited()
    {
        return (recruitName != null);
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

    /**
     * TODO unify between the two derived classes
     */
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
     * Calculate which angels this legion can get in its current land 
     * when crossing the given points threshold
     * 
     * @param points Score threshold (100, ..., 400, 500) for which to get angel 
     * @return list of creatures that can be get at that threshold
     */
    public List<String> findEligibleAngels(int points)
    {
        MasterBoardTerrain terrain = getCurrentHex().getTerrain();
        return player.getGame().findAvailableEligibleAngels(terrain, points);
    }

    /**
     * Data for one pending decision. For example, for crossing the 500 
     * there will be a decision, whether the player takes for this legion
     * an angel or an archangel.
     */
    public class AcquirableDecision
    {
        private final Legion legion;
        private final int points;
        private final List<String> acquirableNames;

        public AcquirableDecision(Legion legion, int points, List<String> names)
        {
            this.legion = legion;
            this.points = points;
            this.acquirableNames = names;
        }

        public List<String> getNames()
        {
            return acquirableNames;
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
