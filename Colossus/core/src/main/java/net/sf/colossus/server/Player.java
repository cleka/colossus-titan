package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.PlayerState;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Options;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Class Player holds the data for one player in a Titan game.
 * 
 * @version $Id$
 * @author David Ripton
 */

public final class Player extends PlayerState implements Comparable<Player>
{
    private static final Logger LOGGER = Logger.getLogger(Player.class
        .getName());

    private String color; // Black, Blue, Brown, Gold, Green, Red
    private String startingTower; // hex label
    // TODO the half-points are really used only in the die(..) method,
    // they could be summed up there and then added all in one go. Save
    // us from storing a double and truncating things later
    private double score; // track half-points, then round
    private boolean summoned;
    private boolean teleported;
    private String playersEliminated = ""; // RdBkGr
    private int mulligansLeft = 1;
    private int movementRoll; // 0 if movement has not been rolled.

    /**
     * The server-specific copy of the legions.
     * 
     * TODO The base class currently has a list of legions, but that is not yet
     * maintained. Things should move up.
     */
    private final List<Legion> legions = new ArrayList<Legion>();
    private boolean titanEliminated;

    /**
     * The legion which gave a summonable creature.
     */
    private Legion donor;
    private final SortedSet<String> markersAvailable = Collections
        .synchronizedSortedSet(new TreeSet<String>());
    private String type; // "Human" or ".*AI"
    private String firstMarker;

    Player(String name, Game game)
    {
        // TODO why are the players on the client side numbered but not here?
        super(game, name, 0);
        type = Constants.human;

        net.sf.colossus.webcommon.InstanceTracker.register(this, name);
    }

    /**
     * Overridden to return specific flavor of Game until the upper class is sufficient. 
     */
    @Override
    public Game getGame()
    {
        return (Game)super.getGame();
    }

    boolean isHuman()
    {
        return type.endsWith(Constants.human) || isNetwork();
    }

    boolean isLocalHuman()
    {
        return type.endsWith(Constants.human);
    }

    boolean isNetwork()
    {
        return type.endsWith(Constants.network);
    }

    boolean isAI()
    {
        return type.endsWith(Constants.ai);
    }

    boolean isNone()
    {
        return type.endsWith(Constants.none);
    }

    String getType()
    {
        return type;
    }

    void setType(final String aType)
    {
        String type = new String(aType);
        LOGGER.log(Level.FINEST, "Called Player.setType() for " + getName()
            + " " + type);
        if (type.endsWith(Constants.anyAI))
        {
            int whichAI = Dice.rollDie(Constants.numAITypes) - 1;
            type = Constants.aiArray[whichAI];
        }
        if (!type.startsWith(Constants.aiPackage))
        {
            type = Constants.aiPackage + type;
        }
        this.type = type;
    }

    String getColor()
    {
        return color;
    }

    void setColor(String color)
    {
        this.color = color;
    }

    void initMarkersAvailable()
    {
        initMarkersAvailable(getShortColor());
    }

    void initMarkersAvailable(String shortColor)
    {
        synchronized (markersAvailable)
        {
            for (int i = 1; i <= 9; i++)
            {
                addLegionMarker(shortColor + '0' + Integer.toString(i));
            }
            for (int i = 10; i <= 12; i++)
            {
                addLegionMarker(shortColor + Integer.toString(i));
            }
        }
    }

    /** Set markersAvailable based on other available information. */
    void computeMarkersAvailable()
    {
        if (isDead())
        {
            markersAvailable.clear();
        }
        else
        {
            synchronized (markersAvailable)
            {
                initMarkersAvailable();
                StringBuffer allVictims = new StringBuffer(playersEliminated);
                for (int i = 0; i < allVictims.length(); i += 2)
                {
                    String shortColor = allVictims.substring(i, i + 2);
                    initMarkersAvailable(shortColor);
                    Player victim = getGame()
                        .getPlayerByShortColor(shortColor);
                    allVictims.append(victim.getPlayersElim());
                }
                Iterator<String> it = getLegionIds().iterator();
                while (it.hasNext())
                {
                    String markerId = it.next();
                    markersAvailable.remove(markerId);
                }
            }
        }
    }

    void setFirstMarker(String firstMarker)
    {
        this.firstMarker = firstMarker;
    }

    String getFirstMarker()
    {
        return firstMarker;
    }

    String getShortColor()
    {
        return getShortColor(getColor());
    }

    public static String getShortColor(String color)
    {
        if (color == null)
        {
            return null;
        }
        else
        {
            return Constants.getShortColorName(color);
        }
    }

    void setTower(String startingTower)
    {
        this.startingTower = startingTower;
    }

    String getTower()
    {
        return startingTower;
    }

    /** Players are sorted in order of decreasing starting tower.
     This is inconsistent with equals(). */
    public int compareTo(Player other)
    {
        return (other.getTower().compareTo(this.getTower()));
    }

    int getScore()
    {
        return (int)score;
    }

    void setScore(int score)
    {
        this.score = score;
    }

    String getPlayersElim()
    {
        return playersEliminated;
    }

    void setPlayersElim(String playersEliminated)
    {
        this.playersEliminated = playersEliminated;
    }

    void addPlayerElim(Player player)
    {
        playersEliminated = playersEliminated + player.getShortColor();
    }

    boolean canTitanTeleport()
    {
        return (score >= TerrainRecruitLoader.getTitanTeleportValue());
    }

    boolean hasTeleported()
    {
        return teleported;
    }

    void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }

    boolean hasSummoned()
    {
        return summoned;
    }

    void setSummoned(boolean summoned)
    {
        this.summoned = summoned;
    }

    Legion getDonor()
    {
        return donor;
    }

    void setDonor(Legion donor)
    {
        this.donor = donor;
    }

    /** Remove all of this player's zero-height legions. */
    synchronized void removeEmptyLegions()
    {
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            if (legion.getHeight() == 0)
            {
                if (legion.equals(donor))
                {
                    donor = null;
                }
                legion.prepareToRemove(true, true);
                it.remove();
            }
        }
    }

    int getTitanPower()
    {
        return 6 + getScore()
            / TerrainRecruitLoader.getTitanImprovementValue();
    }

    synchronized int getNumLegions()
    {
        return legions.size();
    }

    synchronized Legion getLegion(int i)
    {
        return legions.get(i);
    }

    synchronized Legion getLegionByMarkerId(String markerId)
    {
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            if (legion.getMarkerId().equals(markerId))
            {
                return legion;
            }
        }
        return null;
    }

    synchronized Legion getTitanLegion()
    {
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            if (legion.hasTitan())
            {
                return legion;
            }
        }
        return null;
    }

    @Override
    synchronized public List<Legion> getLegions()
    {
        return legions;
    }

    synchronized List<String> getLegionIds()
    {
        List<String> ids = new ArrayList<String>();
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            ids.add(legion.getMarkerId());
        }
        return ids;
    }

    synchronized void removeLegion(Legion legion)
    {
        legions.remove(legion);
    }

    synchronized int getMaxLegionHeight()
    {
        int max = 0;
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            int height = legion.getHeight();
            if (height > max)
            {
                max = height;
            }
        }
        return max;
    }

    /** Return the number of this player's legions that have moved. */
    synchronized int legionsMoved()
    {
        int count = 0;

        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            if (legion.hasMoved())
            {
                count++;
            }
        }
        return count;
    }

    /** Return the number of this player's legions that have legal
     non-teleport moves remaining. */
    synchronized int countMobileLegions()
    {
        int count = 0;
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            if (legion.hasConventionalMove())
            {
                count++;
            }
        }
        return count;
    }

    synchronized void commitMoves()
    {
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            legion.commitMove();
        }
    }

    int getMovementRoll()
    {
        return movementRoll;
    }

    void setMovementRoll(int movementRoll)
    {
        this.movementRoll = movementRoll;
    }

    int getMulligansLeft()
    {
        return mulligansLeft;
    }

    void setMulligansLeft(int number)
    {
        mulligansLeft = number;
    }

    void resetTurnState()
    {
        summoned = false;
        donor = null;

        setTeleported(false);
        movementRoll = 0;

        // Make sure that all legions are allowed to move and recruit.
        commitMoves();
    }

    void rollMovement()
    {
        // Only roll if it hasn't already been done.
        if (movementRoll != 0)
        {
            LOGGER.log(Level.WARNING, "Called rollMovement() more than once");
        }
        else
        {
            movementRoll = Dice.rollDie();
            LOGGER.log(Level.INFO, getName() + " rolls a " + movementRoll
                + " for movement");
        }
        getGame().getServer().allTellMovementRoll(movementRoll);
    }

    void takeMulligan()
    {
        if (mulligansLeft > 0)
        {
            undoAllMoves();
            LOGGER.log(Level.INFO, getName() + " takes a mulligan");
            if (!getGame().getOption(Options.unlimitedMulligans))
            {
                mulligansLeft--;
            }
            movementRoll = 0;
        }
    }

    void undoMove(String markerId)
    {
        Legion legion = getLegionByMarkerId(markerId);
        if (legion != null)
        {
            legion.undoMove();
        }
    }

    synchronized void undoAllMoves()
    {
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            legion.undoMove();
        }
    }

    /** Return true if two or more of this player's legions share
     *  a hex and they have a legal non-teleport move. */
    synchronized boolean splitLegionHasForcedMove()
    {
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            String hexLabel = legion.getCurrentHexLabel();
            if (getGame().getNumFriendlyLegions(hexLabel, this) > 1
                && legion.hasConventionalMove())
            {
                LOGGER.log(Level.FINEST,
                    "Found unseparated split legions at hex " + hexLabel);
                return true;
            }
        }
        return false;
    }

    /** Return true if any legion can recruit. */
    synchronized boolean canRecruit()
    {
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            if (legion.hasMoved() && legion.canRecruit())
            {
                return true;
            }
        }
        return false;
    }

    void undoRecruit(String markerId)
    {
        Legion legion = getLegionByMarkerId(markerId);
        if (legion == null)
        {
            LOGGER.log(Level.SEVERE,
                "Player.undoRecruit: legion for markerId " + markerId
                    + " is null");
            return;
        }

        // This is now permanently fixed in Player.java, so this should
        // never happen again. Still, leaving this in place, just to be sure...
        String recruitName = legion.getRecruitName();
        if (recruitName == null)
        {
            LOGGER.log(Level.SEVERE,
                "Player.undoRecruit: Nothing to unrecruit for marker "
                    + markerId);
            return;
        }
        legion.undoRecruit();

        // Update number of creatures in status window.
        getGame().getServer().allUpdatePlayerInfo();
        getGame().getServer().undidRecruit(legion, recruitName);
    }

    void undoSplit(String splitoffId)
    {
        Legion splitoff = getLegionByMarkerId(splitoffId);
        Legion parent = splitoff.getParent();
        splitoff.recombine(parent, true);
        getGame().getServer().allUpdatePlayerInfo();
    }

    synchronized void recombineIllegalSplits()
    {
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            // Don't use the legion's real parent, as there could have been
            // a 3-way split and the parent could be gone.
            Legion parent = getGame().getFirstFriendlyLegion(
                legion.getCurrentHexLabel(), this);
            if (legion != parent)
            {
                legion.recombine(parent, false);
                it.remove();
            }
        }
        getGame().getServer().allUpdatePlayerInfo();
    }

    synchronized int getNumCreatures()
    {
        int count = 0;
        Iterator<Legion> it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = it.next();
            count += legion.getHeight();
        }
        return count;
    }

    synchronized void addLegion(Legion legion)
    {
        legions.add(legion);
    }

    int getNumMarkersAvailable()
    {
        return markersAvailable.size();
    }

    Set<String> getMarkersAvailable()
    {
        return Collections.unmodifiableSortedSet(markersAvailable);
    }

    String getFirstAvailableMarker()
    {
        synchronized (markersAvailable)
        {
            if (markersAvailable.isEmpty())
            {
                return null;
            }
            return markersAvailable.first();
        }
    }

    boolean isMarkerAvailable(String markerId)
    {
        return markersAvailable.contains(markerId);
    }

    /** Removes the selected marker from the list of those available.
     *  Returns the markerId if it was present, or null if it was not. */
    String selectMarkerId(String markerId)
    {
        if (markersAvailable.remove(markerId))
        {
            return markerId;
        }
        else
        {
            return null;
        }
    }

    void addLegionMarker(String markerId)
    {
        markersAvailable.add(markerId);
    }

    private void takeLegionMarkers(Player victim)
    {
        synchronized (victim.markersAvailable)
        {
            markersAvailable.addAll(victim.getMarkersAvailable());
            victim.markersAvailable.clear();
        }
    }

    /** Add points to this player's score.  Update the status window
     *  to reflect the addition. */
    void addPoints(double points)
    {
        if (points > 0)
        {
            score += points;
            if (getGame() != null)
            {
                getGame().getServer().allUpdatePlayerInfo();
            }

            LOGGER.log(Level.INFO, getName() + " earns " + points + " points");
        }
    }

    /** Remove half-points. */
    void truncScore()
    {
        score = Math.floor(score);
    }

    /**
     * Turns the player dead.
     * 
     * This method calculates the points other players get, adds them to their score and
     * then cleans up this player and marks him dead.
     * 
     * TODO is it really the Player's role to assign points? I'd rather see that responsibility
     * with the Game object
     * 
     * @param slayer The player who killed us. May be null if we just gave up.
     * @param checkForVictory If set the game will be asked to check for a victory after
     *      we are finished.
     */
    synchronized void die(Player slayer, boolean checkForVictory)
    {
        LOGGER
            .info("Player '" + getName() + "' is dying, killed by " + slayer == null ? "nobody"
                : slayer.getName());
        // Engaged legions give half points to the player they're
        // engaged with.  All others give half points to slayer,
        // if non-null.

        for (Iterator<Legion> itLeg = legions.iterator(); itLeg.hasNext();)
        {
            Legion legion = itLeg.next();
            String hexLabel = legion.getCurrentHexLabel();
            Legion enemyLegion = getGame().getFirstEnemyLegion(hexLabel, this);
            double halfPoints = legion.getPointValue() / 2.0;

            Player scorer;

            if (enemyLegion != null)
            {
                scorer = enemyLegion.getPlayer();
            }
            else
            {
                scorer = slayer;
            }
            if (scorer != null)
            {
                scorer.addPoints(halfPoints);
            }

            // Call the iterator's remove() method rather than
            // removeLegion() to avoid concurrent modification problems.
            legion.prepareToRemove(true, true);
            itLeg.remove();
        }

        // Truncate every player's score to an integer value.
        for (Player player : getGame().getPlayers())
        {
            player.truncScore();
        }

        // Mark this player as dead.
        setDead(true);

        // Record the slayer and give him this player's legion markers.
        if (slayer != null)
        {
            slayer.addPlayerElim(this);
            slayer.takeLegionMarkers(this);
        }

        getGame().getServer().allUpdatePlayerInfo();

        LOGGER.info(getName() + " is dead, telling everyone about it");

        getGame().getServer().allTellPlayerElim(this, slayer, true);

        // See if the game is over.
        if (checkForVictory)
        {
            getGame().checkForVictory();
        }
    }

    void eliminateTitan()
    {
        titanEliminated = true;
    }

    boolean isTitanEliminated()
    {
        return titanEliminated;
    }

    /** Return a colon-separated string with a bunch of info for
     *  the status screen. */
    String getStatusInfo(boolean treatDeadAsAlive)
    {
        List<String> li = new ArrayList<String>();
        li.add(Boolean.toString(!treatDeadAsAlive && isDead()));
        li.add(getName());
        li.add(getTower());
        li.add(getColor());
        li.add(getType());
        li.add(getPlayersElim());
        li.add(Integer.toString(getNumLegions()));
        li.add(Integer.toString(getNumCreatures()));
        li.add(Integer.toString(getTitanPower()));
        li.add(Integer.toString(getScore()));
        li.add(Integer.toString(getMulligansLeft()));
        synchronized (markersAvailable)
        {
            li.addAll(getMarkersAvailable());
        }
        return Glob.glob(":", li);
    }

    /** Return the total value of all of this player's creatures. */
    synchronized int getTotalPointValue()
    {
        int total = 0;
        for (Legion legion : legions)
        {
            total += legion.getPointValue();
        }
        return total;
    }
}
