package net.sf.colossus.server;


import java.util.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Options;
import net.sf.colossus.parser.TerrainRecruitLoader;



/**
 * Class Player holds the data for one player in a Titan game.
 * @version $Id$
 * @author David Ripton
 */

public final class Player implements Comparable
{
    private Game game;
    private String name;
    private String color;              // Black, Blue, Brown, Gold, Green, Red
    private String startingTower;      // hex label
    private double score;              // track half-points, then round
    private boolean summoned;
    private boolean teleported;
    private String playersEliminated = "";  // RdBkGr
    private int mulligansLeft = 1;
    private int movementRoll;          // 0 if movement has not been rolled.
    private List legions = new ArrayList();
    private boolean dead;
    private boolean titanEliminated;
    private String donorId;
    private SortedSet markersAvailable = new TreeSet();
    private String type;               // "Human" or ".*AI"
    private String firstMarker;


    Player(String name, Game game)
    {
        this.name = name;
        this.game = game;
        type = "Human";
    }


    boolean isHuman()
    {
        return type.endsWith(Constants.human) || isNetwork();
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
        Log.debug("Called Player.setType() for " + name + " " + type);
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


    boolean isDead()
    {
        return dead;
    }

    void setDead(boolean dead)
    {
        this.dead = dead;
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
        for (int i = 1; i <= 9; i++)
        {
            addLegionMarker(shortColor + '0' + Integer.toString(i));
        }
        for (int i = 10; i <= 12; i++)
        {
            addLegionMarker(shortColor + Integer.toString(i));
        }
    }

    /** Set markersAvailable based on other available information. */
    void computeMarkersAvailable()
    {
        if (dead)
        {
            markersAvailable.clear();
        }
        else
        {
            initMarkersAvailable();
            StringBuffer allVictims = new StringBuffer(playersEliminated);
            for (int i = 0; i < allVictims.length(); i += 2)
            {
                String shortColor = allVictims.substring(i, i + 2);
                initMarkersAvailable(shortColor);
                Player victim = game.getPlayerByShortColor(shortColor);
                allVictims.append(victim.getPlayersElim());
            }
            Iterator it = getLegionIds().iterator();
            while (it.hasNext())
            {
                String markerId = (String)it.next();
                markersAvailable.remove(markerId);
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
    public int compareTo(Object object)
    {
        if (object instanceof Player)
        {
            Player other = (Player)object;
            return (other.getTower().compareTo(this.getTower()));
        }
        else
        {
            throw new ClassCastException();
        }
    }


    int getScore()
    {
        return (int) score;
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

    String getDonorId()
    {
        return donorId;
    }

    Legion getDonor()
    {
        return getLegionByMarkerId(donorId);
    }

    void setDonorId(String markerId)
    {
        donorId = markerId;
    }

    void setDonor(Legion donor)
    {
        setDonorId(donor.getMarkerId());
    }

    /** Remove all of this player's zero-height legions. */
    synchronized void removeEmptyLegions()
    {
        Iterator it = legions.iterator(); 
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getHeight() == 0)
            {
                if (donorId != null && donorId.equals(legion.getMarkerId()))
                {
                    donorId = null;
                }
                legion.prepareToRemove();
                it.remove();
            }
        }
    }


    int getTitanPower()
    {
        return (int)(6 + (getScore() / 
                          TerrainRecruitLoader.getTitanImprovementValue()));
    }


    synchronized int getNumLegions()
    {
        return legions.size();
    }

    synchronized Legion getLegion(int i)
    {
        return (Legion)legions.get(i);
    }

    synchronized Legion getLegionByMarkerId(String markerId)
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getMarkerId().equals(markerId))
            {
                return legion;
            }
        }
        return null;
    }

    synchronized Legion getTitanLegion()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.hasTitan())
            {
                return legion;
            }
        }
        return null;
    }

    synchronized List getLegions()
    {
        return legions;
    }

    synchronized List getLegionIds()
    {
        List ids = new ArrayList();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
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
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
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

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
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
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.hasConventionalMove())
            {
                count++;
            }
        }
        return count;
    }


    synchronized void commitMoves()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.commitMove();
        }
    }


    String getName()
    {
        return name;
    }

    void setName(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
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
        donorId = null;

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
            Log.warn("Called rollMovement() more than once");
        }
        else
        {
            movementRoll = Dice.rollDie();
            Log.event(getName() + " rolls a " + movementRoll + 
                " for movement");
        }
        game.getServer().allTellMovementRoll(movementRoll);
    }


    void takeMulligan()
    {
        if (mulligansLeft > 0)
        {
            undoAllMoves();
            Log.event(getName() + " takes a mulligan");
            mulligansLeft--;
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
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.undoMove();
        }
    }


    /** Return true if two or more of this player's legions share
     *  a hex and they have a legal non-teleport move. */
    synchronized boolean splitLegionHasForcedMove()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            String hexLabel = legion.getCurrentHexLabel();
            if (game.getNumFriendlyLegions(hexLabel, this) > 1 &&
                legion.hasConventionalMove())
            {
                return true;
            }
        }
        return false;
    }


    /** Return true if any legion can recruit. */
    synchronized boolean canRecruit()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
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
        String recruitName = legion.getRecruitName();
        legion.undoRecruit();

        // Update number of creatures in status window.
        game.getServer().allUpdatePlayerInfo();
        game.getServer().undidRecruit(legion, recruitName);
    }


    void undoSplit(String splitoffId)
    {
        Legion splitoff = getLegionByMarkerId(splitoffId);
        Legion parent = splitoff.getParent();
        splitoff.recombine(parent, true);
        game.getServer().allUpdatePlayerInfo();
        game.getServer().undidSplit(splitoffId, parent.getMarkerId());
    }


    synchronized void recombineIllegalSplits()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            // Don't use the legion's real parent, as there could have been
            // a 3-way split and the parent could be gone.
            Legion parent = game.getFirstFriendlyLegion(
                legion.getCurrentHexLabel(), this);
            if (legion != parent)
            {
                game.getServer().undidSplit(legion.getMarkerId(),
                    parent.getMarkerId());
                legion.recombine(parent, false);
                it.remove();
            }
        }
        game.getServer().allUpdatePlayerInfo();
    }


    synchronized int getNumCreatures()
    {
        int count = 0;
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
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

    Set getMarkersAvailable()
    {
        return Collections.unmodifiableSortedSet(markersAvailable);
    }

    String getFirstAvailableMarker()
    {
        if (markersAvailable.isEmpty())
        {
            return null;
        }
        return (String)markersAvailable.first();
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
        markersAvailable.addAll(victim.getMarkersAvailable());
        victim.markersAvailable.clear();
    }


    /** Add points to this player's score.  Update the status window
     *  to reflect the addition. */
    void addPoints(double points)
    {
        if (points > 0)
        {
            score += points;
            if (game != null)
            {
                game.getServer().allUpdatePlayerInfo();
            }

            Log.event(getName() + " earns " + points + " points");
        }
    }


    /** Remove half-points. */
    void truncScore()
    {
        score = Math.floor(score);
    }


    synchronized void die(String slayerName, boolean checkForVictory)
    {
        // Engaged legions give half points to the player they're
        // engaged with.  All others give half points to slayer,
        // if non-null.

        Player slayer = game.getPlayer(slayerName);

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            String hexLabel = legion.getCurrentHexLabel();
            Legion enemyLegion = game.getFirstEnemyLegion(hexLabel, this);
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
            legion.prepareToRemove();
            it.remove();
        }

        // Truncate every player's score to an integer value.
        Collection players = game.getPlayers();
        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
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
            
        game.getServer().allUpdatePlayerInfo();

        Log.event(getName() + " dies");
        game.getServer().allTellPlayerElim(name, slayerName);
            
        // See if the game is over.
        if (checkForVictory)
        {
            game.checkForVictory();
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



    // XXX Prohibit colons in player names.
    /** Return a colon:separated string with a bunch of info for
     *  the status screen. */
    String getStatusInfo(boolean treatDeadAsAlive)
    {
        StringBuffer buf = new StringBuffer();
        if (treatDeadAsAlive)
        {
            buf.append(false);
        }
        else
        {
            buf.append(isDead());
        }
        buf.append(':');
        buf.append(name);
        buf.append(':');
        buf.append(getTower());
        buf.append(':');
        buf.append(getColor());
        buf.append(':');
        buf.append(getPlayersElim());
        buf.append(':');
        buf.append(getNumLegions());
        buf.append(':');
        buf.append(getNumMarkersAvailable());
        buf.append(':');
        buf.append(getNumCreatures());
        buf.append(':');
        buf.append(getTitanPower());
        buf.append(':');
        buf.append(getScore());
        buf.append(':');
        buf.append(getTotalPointValue());
        buf.append(':');
        buf.append(getMulligansLeft());

        return buf.toString();
    }

    /** Return the total value of all of this player's creatures. */
    synchronized int getTotalPointValue()
    {
        int total = 0;
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            total += legion.getPointValue();
        }
        return total;
    }
}
