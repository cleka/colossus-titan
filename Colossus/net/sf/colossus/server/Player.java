package net.sf.colossus.server;


import java.util.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;


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

    private AI ai = new SimpleAI();    // TODO Allow pluggable AIs.


    Player(String name, Game game)
    {
        this.name = name;
        this.game = game;
        type = "Human";
    }


    /**
     *  Deep copy for AI; preserves game state but ignores UI state
     */
    Player AICopy(Game game)
    {
        Player newPlayer = new Player(name, game);
        newPlayer.color = color;       // Black, Blue, Brown, Gold, Green, Red
        newPlayer.startingTower = startingTower;         // 1-6
        newPlayer.score = score;       // track half-points, then round
        newPlayer.summoned = summoned;
        newPlayer.teleported = teleported;
        newPlayer.playersEliminated = playersEliminated;  // RdBkGr
        newPlayer.mulligansLeft = mulligansLeft;
        newPlayer.movementRoll = movementRoll;
        newPlayer.dead = dead;
        newPlayer.ai = ai;
        newPlayer.titanEliminated = titanEliminated;
        newPlayer.donorId = donorId;
        newPlayer.type = type;
        for (int i = 0; i < legions.size(); i++)
        {
            newPlayer.legions.add(i, ((Legion)legions.get(i)).AICopy(game));
        }

        // Strings are immutable, so a shallow copy == a deep copy
        newPlayer.markersAvailable = new TreeSet();
        newPlayer.markersAvailable.addAll(markersAvailable);

        return newPlayer;
    }


    boolean isHuman()
    {
        return type.endsWith("Human");
    }

    boolean isAI()
    {
        return type.endsWith("AI");
    }

    boolean isNone()
    {
        return type.endsWith("none");
    }

    String getType()
    {
        return type;
    }

    void setType(String type)
    {
        if (!type.startsWith("net.sf.colossus.server."))
        {
            type = "net.sf.colossus.server." + type;
        }
        this.type = type;
        if (type.endsWith("AI"))
        {
            if (!(ai.getClass().getName().equals(type))) 
            {
                System.out.println("Changing player " + name + " from " +
                    ai.getClass().getName() + " to " + type);
                try 
                {
                    ai = (AI)Class.forName(type).getDeclaredConstructor(
                        new Class[0]).newInstance(new Object[0]);
                } 
                catch (Exception e) 
                {
                    System.out.println("Failed to change player " + name +
                        " from " + ai.getClass().getName() + " to " + type);
                }
            }
        }
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
        for (int i = 1; i <= 9; i++)
        {
            addLegionMarker(getShortColor() + '0' + Integer.toString(i));
        }
        for (int i = 10; i <= 12; i++)
        {
            addLegionMarker(getShortColor() + Integer.toString(i));
        }
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
        else if (color.equals("Black"))
        {
            return "Bk";
        }
        else if (color.equals("Blue"))
        {
            return "Bu";
        }
        else if (color.equals("Brown"))
        {
            return "Br";
        }
        else if (color.equals("Gold"))
        {
            return "Gd";
        }
        else if (color.equals("Green"))
        {
            return "Gr";
        }
        else if (color.equals("Red"))
        {
            return "Rd";
        }
        else
        {
            return "XX";
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
        return (score >= Game.getTitanTeleportValue() && !hasTeleported());
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

    void disbandEmptyDonor()
    {
        if (donorId != null)
        {
            Legion donor = getDonor();
            if (donor.getHeight() == 0)
            {
                donor.remove();
                donorId = null;
            }
        }
    }


    int getTitanPower()
    {
        return (int)(6 + (getScore() / Game.getTitanImprovementValue()));
    }


    int getNumLegions()
    {
        return legions.size();
    }

    Legion getLegion(int i)
    {
        return (Legion)legions.get(i);
    }

    Legion getLegionByMarkerId(String markerId)
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

    List getLegions()
    {
        return legions;
    }

    List getLegionIds()
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


    int getMaxLegionHeight()
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


    /** Sort legions into order of descending importance.  Titan legion
     *  first, then others by point value. */
    void sortLegions()
    {
        Collections.sort(legions);
    }


    /** Move legion to the first position in the legions list.
     *  Return true if it was moved. */
    boolean moveToTop(Legion legion)
    {
        int i = legions.indexOf(legion);
        if (i <= 0)
        {
            // Not found, or already first in the list.
            return false;
        }
        else
        {
            legions.remove(i);
            legions.add(0, legion);
            return true;
        }
    }


    /** Return the number of this player's legions that have moved. */
    int legionsMoved()
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
    int countMobileLegions()
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


    void commitMoves()
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
        // Only allow rolling if it hasn't already been done.
        if (movementRoll != 0)
        {
            Log.warn("Called rollMovement() illegally");
            return;
        }

        movementRoll = Game.rollDie();
        Log.event(getName() + " rolls a " + movementRoll + " for movement");
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

    void undoAllMoves()
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
    boolean splitLegionHasForcedMove()
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
    boolean canRecruit()
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
        String hexLabelToAlign = splitoff.getCurrentHexLabel();
        Legion parent = splitoff.getParent();
        splitoff.recombine(parent, true);
        game.getServer().allUpdatePlayerInfo();
        game.getServer().undidSplit(splitoffId);
        game.getServer().oneRevealLegion(parent, getName());
        game.getServer().allFullyUpdateLegionHeights();
    }


    void recombineIllegalSplits()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            Legion parent = legion.getParent();
            if (parent != null && parent != legion &&
                parent.getCurrentHexLabel().equals(
                legion.getCurrentHexLabel()))
            {
                game.getServer().undidSplit(legion.getMarkerId());
                legion.recombine(parent, false);
                it.remove();
            }
        }
        game.getServer().allUpdatePlayerInfo();
    }


    int getNumCreatures()
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


    void addLegion(Legion legion)
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


    void die(String slayerName, boolean checkForVictory)
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



    void aiSplit()
    {
        if (isAI())
        {
            ai.split(game);
            // Keep the AI from continuing to play after winning.
            if (!game.isOver())
            {
                game.advancePhase(Constants.SPLIT, getName());
            }
        }
    }

    void aiMasterMove()
    {
        if (isAI())
        {
            ai.masterMove(game);
            recombineIllegalSplits();
            // Keep the AI from continuing to play after winning.
            if (!game.isOver())
            {
                game.advancePhase(Constants.MOVE, getName());
            }
        }
    }

    void aiRecruit()
    {
        if (isAI())
        {
            ai.muster(game);
            // Keep the AI from continuing to play after winning.
            if (!game.isOver())
            {
                game.advancePhase(Constants.MUSTER, getName());
            }
        }
    }


    void aiReinforce(Legion legion)
    {
        if (isAI())
        {
            ai.reinforce(legion, game);
        }
    }


    boolean aiFlee(Legion legion, Legion enemy)
    {
        if (isAI())
        {
            return ai.flee(legion, enemy, game);
        }
        return false;
    }

    boolean aiConcede(Legion legion, Legion enemy)
    {
        if (isAI())
        {
            return ai.concede(legion, enemy, game);
        }
        return false;
    }

    void aiStrike(Legion legion, Battle battle, boolean forced)
    {
        if (forced || isAI())
        {
            ai.strike(legion, battle, game);
        }
    }

    PenaltyOption aiChooseStrikePenalty(SortedSet penaltyOptions)
    {
        if (isAI())
        {
            return ai.chooseStrikePenalty(penaltyOptions);
        }
        return null;
    }

    void aiBattleMove()
    {
        if (isAI())
        {
            ai.battleMove(game);
            game.getBattle().doneWithMoves();
        }
    }

    int aiPickEntrySide(String hexLabel, Legion legion, boolean left, 
        boolean bottom, boolean right)
    {
        if (isAI())
        {
            return ai.pickEntrySide(hexLabel, legion, game, left, bottom,
                right);
        }
        return -1;
    }

    String aiPickEngagement()
    {
        if (isAI())
        {
            return ai.pickEngagement(game);
        }
        return null;
    }


    // XXX Prohibit colons in player names.
    /** Return a colon:separated string with a bunch of info for
     *  the status screen. */
    String getStatusInfo()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(isDead());
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

        return buf.toString();
    }

    /** Return the total value of all of this player's creatures. */
    int getTotalPointValue()
    {
        int total = 0;
        Iterator it = getLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            total += legion.getPointValue();
        }
        return total;
    }
}
