package net.sf.colossus.server;


import java.util.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.client.MarkerComparator;


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
    private int startingTower;         // 1-6
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
    private TreeSet markersAvailable;
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
        newPlayer.markersAvailable = (TreeSet)markersAvailable.clone();

        return newPlayer;
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
        markersAvailable = new TreeSet(
            MarkerComparator.getMarkerComparator(getShortColor()));
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


    void setTower(int startingTower)
    {
        this.startingTower = startingTower;
    }

    int getTower()
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
            return (other.getTower() - this.getTower());
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
        return (score >= 400 && !teleported);
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
        return (int)(6 + (getScore() / 100));
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

        teleported = false;
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
        Server server = game.getServer();
        server.allShowMovementRoll(movementRoll);
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
        legion.undoRecruit();
        // Update number of creatures in status window.
        game.getServer().allUpdateStatusScreen();
    }


    void undoSplit(String splitoffId)
    {
        Legion splitoff = getLegionByMarkerId(splitoffId);
        String hexLabelToAlign = splitoff.getCurrentHexLabel();
        splitoff.recombine(splitoff.getParent(), true);
        game.getServer().allAlignLegions(hexLabelToAlign);
        game.getServer().allUpdateStatusScreen();
    }


    void recombineIllegalSplits()
    {
        Set hexLabelsToAlign = new HashSet();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            Legion parent = legion.getParent();
            if (parent != null && parent != legion &&
                parent.getCurrentHexLabel().equals(
                legion.getCurrentHexLabel()))
            {
                hexLabelsToAlign.add(legion.getCurrentHexLabel());
                legion.recombine(parent, false);
                it.remove();
            }
        }
        game.getServer().allAlignLegions(hexLabelsToAlign);
        game.getServer().allUpdateStatusScreen();
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
        Server server = game.getServer();
        if (server != null)
        {
            server.allAlignLegions(legion.getCurrentHexLabel());
        }
    }


    int getNumMarkersAvailable()
    {
        return markersAvailable.size();
    }

    Collection getMarkersAvailable()
    {
        return markersAvailable;
    }

    boolean isMarkerAvailable(String markerId)
    {
        return (markersAvailable.contains(markerId));
    }

    String getFirstAvailableMarker()
    {
        if (markersAvailable.size() == 0)
        {
            return null;
        }
        return (String)markersAvailable.first();
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

    void addLegionMarkers(Player player)
    {
        Collection newMarkers = player.getMarkersAvailable();
        markersAvailable.addAll(newMarkers);
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
                game.getServer().allUpdateStatusScreen();
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

        Set hexLabelsToAlign = new HashSet();
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
            hexLabelsToAlign.add(legion.getCurrentHexLabel());
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
        dead = true;

        // Record the slayer and give him this player's legion markers.
        if (slayer != null)
        {
            slayer.addPlayerElim(this);
            slayer.addLegionMarkers(this);
        }

        game.getServer().allAlignLegions(hexLabelsToAlign);
        game.getServer().allUpdateStatusScreen();

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


    String pickMarker()
    {
        String markerId = null;
        if (game.getServer().getClientOption(name, Options.autoPickMarker))
        {
            markerId = aiPickMarker();
        }
        else
        {
            markerId = game.getServer().pickMarker(name,
                getMarkersAvailable());
        }
        return markerId;
    }

    String aiPickMarker()
    {
        if (game.getServer().getClientOption(name, Options.autoPickMarker))
        {
            return ai.pickMarker(getMarkersAvailable());
        }
        return null;
    }

    void aiSplit()
    {
        if (game.getServer().getClientOption(name, Options.autoSplit))
        {
            ai.split(game);
            // Keep the AI from continuing to play after winning.
            if (!game.isOver())
            {
                game.advancePhase(Constants.SPLIT);
            }
        }
    }

    void aiMasterMove()
    {
        if (game.getServer().getClientOption(name, Options.autoMasterMove))
        {
            ai.masterMove(game);
            recombineIllegalSplits();
            game.advancePhase(Constants.MOVE);
        }
    }

    void aiRecruit()
    {
        if (game.getServer().getClientOption(name, Options.autoRecruit))
        {
            ai.muster(game);
            // Keep the AI from continuing to play after winning.
            if (!game.isOver())
            {
                game.advancePhase(Constants.MUSTER);
            }
        }
    }

    Creature aiReinforce(Legion legion)
    {
        if (game.getServer().getClientOption(name, Options.autoRecruit))
        {
            return ai.reinforce(legion, game);
        }
        return null;
    }

    boolean aiFlee(Legion legion, Legion enemy)
    {
        if (game.getServer().getClientOption(name, Options.autoFlee))
        {
            return ai.flee(legion, enemy, game);
        }
        return false;
    }

    boolean aiConcede(Legion legion, Legion enemy)
    {
        if (game.getServer().getClientOption(name, Options.autoFlee))
        {
            return ai.concede(legion, enemy, game);
        }
        return false;
    }

    void aiStrike(Legion legion, Battle battle, boolean fakeDice,
        boolean forced)
    {
        if (forced || game.getServer().getClientOption(name,
            Options.autoStrike))
        {
            ai.strike(legion, battle, game, fakeDice);
        }
    }

    boolean aiChooseStrikePenalty(Critter critter, Critter target,
        Critter carryTarget, Battle battle)
    {
        if (game.getServer().getClientOption(name, Options.autoStrike))
        {
            return ai.chooseStrikePenalty(critter, target, carryTarget,
                battle, game);
        }
        return false;
    }

    void aiBattleMove()
    {
        if (game.getServer().getClientOption(name, Options.autoBattleMove))
        {
            ai.battleMove(game);
            game.getBattle().doneWithMoves();
        }
    }

    int aiPickEntrySide(String hexLabel, Legion legion, boolean left, 
        boolean bottom, boolean right)
    {
        if (game.getServer().getClientOption(name, Options.autoPickEntrySide))
        {
            return ai.pickEntrySide(hexLabel, legion, game, left, bottom,
                right);
        }
        return -1;
    }

    String aiPickEngagement()
    {
        if (game.getServer().getClientOption(name, Options.autoPickEngagement))
        {
            return ai.pickEngagement(game);
        }
        return null;
    }

    String aiAcquireAngel(Legion legion, List recruits, Game game)
    {
        if (game.getServer().getClientOption(name, Options.autoAcquireAngels))
        {
            return ai.acquireAngel(legion, recruits, game);
        }
        return null;
    }

    /** Return a String of form "Angeltype:donorId", or null if no
      * angel is to be summoned. */
    String aiSummonAngel(Legion legion)
    {
        if (game.getServer().getClientOption(name, Options.autoSummonAngels))
        {
            return ai.summonAngel(legion, game);
        }
        return null;
    }

    String aiPickColor(Set colors)
    {
        // Convert favorite colors from a comma-separated string to a list.
        String favorites = game.getServer().getClientStringOption(name,
            Options.favoriteColors);
        List favoriteColors = null;
        if (favorites != null)
        {
            favoriteColors = Split.split(',', favorites);
        }
        else
        {
            favoriteColors = new ArrayList();
        }
        return ai.pickColor(colors, favoriteColors);
    }

    // XXX Need to not allow colons in player names.
    /** Return a colon:separated string with a bunch of info for
     *  the status screen. */
    String getStatusInfo()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(isDead());
        buf.append(':');
        buf.append(name);
        buf.append(':');
        buf.append(100 * getTower());
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

        return buf.toString();
    }
}
