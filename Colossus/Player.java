import java.util.*;
import java.io.*;

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
    private String playersEliminated;  // RdBkGr
    private int mulligansLeft = 1;
    private int movementRoll;          // 0 if movement has not been rolled.
    private ArrayList legions = new ArrayList();
    private boolean dead;
    private boolean titanEliminated;
    private String donorId;
    private final MarkerComparator markerComparator = new MarkerComparator();
    private TreeSet markersAvailable = new TreeSet(markerComparator);
    private String type;               // "Human" or ".*AI"

    private AI ai = new SimpleAI();    // TODO Allow pluggable AIs.


    public Player(String name, Game game)
    {
        this.name = name;
        this.game = game;
        type = "Human";
    }


    /**
     *  Deep copy for AI; preserves game state but ignores UI state
     */
    public Player AICopy(Game game)
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
        newPlayer.markersAvailable = (TreeSet) markersAvailable.clone();

        return newPlayer;
    }


    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
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


    public boolean isDead()
    {
        return dead;
    }

    public void setDead(boolean dead)
    {
        this.dead = dead;
    }


    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }


    public void initMarkersAvailable()
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


    public String getShortColor()
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


    public void setTower(int startingTower)
    {
        this.startingTower = startingTower;
    }

    public int getTower()
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


    public int getScore()
    {
        return (int) score;
    }


    public void setScore(int score)
    {
        this.score = score;
    }


    public String getPlayersElim()
    {
        return playersEliminated;
    }


    public void setPlayersElim(String playersEliminated)
    {
        this.playersEliminated = playersEliminated;
    }


    public void addPlayerElim(Player player)
    {
        if (playersEliminated == null)
        {
            playersEliminated = player.getShortColor();
        }
        else
        {
            playersEliminated = playersEliminated + player.getShortColor();
        }
    }


    public boolean canTitanTeleport()
    {
        return (score >= 400 && !teleported);
    }


    public boolean hasTeleported()
    {
        return teleported;
    }

    public void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }


    public boolean hasSummoned()
    {
        return summoned;
    }

    public void setSummoned(boolean summoned)
    {
        this.summoned = summoned;
    }

    public String getDonorId()
    {
        return donorId;
    }

    public Legion getDonor()
    {
        return getLegionByMarkerId(donorId);
    }

    public void setDonorId(String markerId)
    {
        donorId = markerId;
    }

    public void setDonor(Legion donor)
    {
        setDonorId(donor.getMarkerId());
    }

    public void disbandEmptyDonor()
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


    public int getTitanPower()
    {
        return (int)(6 + (getScore() / 100));
    }


    public int getNumLegions()
    {
        return legions.size();
    }

    public Legion getLegion(int i)
    {
        return (Legion)legions.get(i);
    }

    public Legion getLegionByMarkerId(String markerId)
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

    public ArrayList getLegions()
    {
        return legions;
    }

    public List getLegionIds()
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


    public int getMaxLegionHeight()
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
    public void sortLegions()
    {
        Collections.sort(legions);
    }


    public MarkerComparator getMarkerComparator()
    {
        return markerComparator;
    }


    /** Move legion to the first position in the legions list.
     *  Return true if it was moved. */
    public boolean moveToTop(Legion legion)
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
    public int legionsMoved()
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
    public int countMobileLegions()
    {
        int count = 0;
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (game.countConventionalMoves(legion) > 0)
            {
                count++;
            }
        }

        return count;
    }


    public void commitMoves()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.commitMove();
        }
    }


    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }


    public int getMovementRoll()
    {
        return movementRoll;
    }

    public void setMovementRoll(int movementRoll)
    {
        this.movementRoll = movementRoll;
    }


    public int getMulligansLeft()
    {
        return mulligansLeft;
    }

    public void setMulligansLeft(int number)
    {
        mulligansLeft = number;
    }


    public void resetTurnState()
    {
        summoned = false;
        donorId = null;

        teleported = false;
        movementRoll = 0;

        Client.clearUndoStack();

        // Make sure that all legions are allowed to move and recruit.
        commitMoves();

        clearAllHexInfo();
    }

    /** Clear entry side and teleport information from all of this
     *  player's legions. */
    public void clearAllHexInfo()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.clearAllHexInfo();
        }
    }


    public void rollMovement()
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


    public void takeMulligan()
    {
        if (mulligansLeft > 0)
        {
            undoAllMoves();
            Log.event(getName() + " takes a mulligan");
            mulligansLeft--;
            movementRoll = 0;

            Iterator it = legions.iterator();
            while (it.hasNext())
            {
                ((Legion)it.next()).clearAllHexInfo();
            }
        }
    }


    public void setLastLegionSplitOff(Legion legion)
    {
        Client.pushUndoStack(legion.getMarkerId());
    }


    public void undoLastMove()
    {
        if (!Client.isUndoStackEmpty())
        {
            String markerId = (String)Client.popUndoStack();
            Legion legion = getLegionByMarkerId(markerId);
            legion.undoMove();
        }
    }

    public void undoAllMoves()
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
    public boolean splitLegionHasForcedMove()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            String hexLabel = legion.getCurrentHexLabel();
            if (game.getNumFriendlyLegions(hexLabel, this) > 1 &&
                game.countConventionalMoves(legion) > 0)
            {
                return true;
            }
        }
        return false;
    }


    /** Return true if any legion can recruit. */
    public boolean canRecruit()
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


    public void undoLastRecruit()
    {
        if (!Client.isUndoStackEmpty())
        {
            String markerId = (String)Client.popUndoStack();
            Legion legion = getLegionByMarkerId(markerId);
            legion.undoRecruit();
            // Update number of creatures in status window.
            game.getServer().allUpdateStatusScreen();
        }
    }


    public void undoAllRecruits()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.undoRecruit();
        }

        // Update number of creatures in status window.
        game.getServer().allUpdateStatusScreen();
    }


    public void undoLastSplit()
    {
        if (!Client.isUndoStackEmpty())
        {
            String splitoffId = (String)Client.popUndoStack();
            Legion splitoff = getLegionByMarkerId(splitoffId);
            String hexLabelToAlign = splitoff.getCurrentHexLabel();
            splitoff.recombine(splitoff.getParent(), true);
            game.getServer().allAlignLegions(hexLabelToAlign);
            game.getServer().allUpdateStatusScreen();
        }
    }

    public void undoAllSplits()
    {
        HashSet hexLabelsToAlign = new HashSet();
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


    public int getNumCreatures()
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


    public void addLegion(Legion legion)
    {
        legions.add(legion);
        Server server = game.getServer();
        if (server != null)
        {
            server.allAlignLegions(legion.getCurrentHexLabel());
        }
    }


    public int getNumMarkersAvailable()
    {
        return markersAvailable.size();
    }

    public Collection getMarkersAvailable()
    {
        return markersAvailable;
    }

    public boolean isMarkerAvailable(String markerId)
    {
        return (markersAvailable.contains(markerId));
    }

    public String getFirstAvailableMarker()
    {
        if (markersAvailable.size() == 0)
        {
            return null;
        }
        return (String)markersAvailable.first();
    }

    /** Removes the selected marker from the list of those available.
     *  Returns the markerId if it was present, or null if it was not. */
    public String selectMarkerId(String markerId)
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

    public void addLegionMarker(String markerId)
    {
        markersAvailable.add(markerId);
    }

    public void addLegionMarkers(Player player)
    {
        Collection newMarkers = player.getMarkersAvailable();
        markersAvailable.addAll(newMarkers);
    }


    /** Add points to this player's score.  Update the status window
     *  to reflect the addition. */
    public void addPoints(double points)
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
    public void truncScore()
    {
        score = Math.floor(score);
    }


    public void die(String slayerName, boolean checkForVictory)
    {
        // Engaged legions give half points to the player they're
        // engaged with.  All others give half points to slayer,
        // if non-null.

        Player slayer = game.getPlayer(slayerName);

        HashSet hexLabelsToAlign = new HashSet();
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


    public void eliminateTitan()
    {
        titanEliminated = true;
    }


    public boolean isTitanEliminated()
    {
        return titanEliminated;
    }


    public String pickMarker()
    {
        String markerId = null;
        if (game.getServer().getClientOption(name, Options.autoPickMarker))
        {
            markerId = aiPickMarker();
        }
        else
        {
            markerId = PickMarker.pickMarker(game.getServer().
                getClient(name).getBoard().getFrame(), name,
                getMarkersAvailable());
        }
        return markerId;
    }

    public String aiPickMarker()
    {
        if (game.getServer().getClientOption(name, Options.autoPickMarker))
        {
            return ai.pickMarker(getMarkersAvailable());
        }
        return null;
    }

    public void aiSplit()
    {
        if (game.getServer().getClientOption(name, Options.autoSplit))
        {
            ai.split(game);
            // Keep the AI from continuing to play after winning.
            if (!game.isOver())
            {
                game.advancePhase(Game.SPLIT);
            }
        }
    }

    public void aiMasterMove()
    {
        if (game.getServer().getClientOption(name, Options.autoMasterMove))
        {
            ai.masterMove(game);
            // XXX This should fix illegal legions, but it would be better
            // to merge this logic and the logic in MasterBoard to create
            // a server-side check.
            undoAllSplits();
            game.advancePhase(Game.MOVE);
        }
    }

    public void aiRecruit()
    {
        if (game.getServer().getClientOption(name, Options.autoRecruit))
        {
            ai.muster(game);
            // Keep the AI from continuing to play after winning.
            if (!game.isOver())
            {
                game.advancePhase(Game.MUSTER);
            }
        }
    }

    public Creature aiReinforce(Legion legion)
    {
        if (game.getServer().getClientOption(name, Options.autoRecruit))
        {
            return ai.reinforce(legion, game);
        }
        return null;
    }

    public boolean aiFlee(Legion legion, Legion enemy)
    {
        if (game.getServer().getClientOption(name, Options.autoFlee))
        {
            return ai.flee(legion, enemy, game);
        }
        return false;
    }

    public boolean aiConcede(Legion legion, Legion enemy)
    {
        if (game.getServer().getClientOption(name, Options.autoFlee))
        {
            return ai.concede(legion, enemy, game);
        }
        return false;
    }

    public void aiStrike(Legion legion, Battle battle, boolean fakeDice,
        boolean forced)
    {
        if (forced || game.getServer().getClientOption(name,
            Options.autoStrike))
        {
            ai.strike(legion, battle, game, fakeDice);
        }
    }

    public boolean aiChooseStrikePenalty(Critter critter, Critter target,
        Critter carryTarget, Battle battle)
    {
        if (game.getServer().getClientOption(name, Options.autoStrike))
        {
            return ai.chooseStrikePenalty(critter, target, carryTarget,
                battle, game);
        }
        return false;
    }

    public void aiBattleMove()
    {
        if (game.getServer().getClientOption(name, Options.autoBattleMove))
        {
            ai.battleMove(game);
            game.getBattle().doneWithMoves();
        }
    }

    public int aiPickEntrySide(String hexLabel, Legion legion)
    {
        if (game.getServer().getClientOption(name, Options.autoPickEntrySide))
        {
            return ai.pickEntrySide(hexLabel, legion, game);
        }
        return -1;
    }

    public String aiPickEngagement()
    {
        if (game.getServer().getClientOption(name, Options.autoPickEngagement))
        {
            return ai.pickEngagement(game);
        }
        return null;
    }

    public String aiAcquireAngel(Legion legion, ArrayList recruits, Game game)
    {
        if (game.getServer().getClientOption(name, Options.autoAcquireAngels))
        {
            return ai.acquireAngel(legion, recruits, game);
        }
        return null;
    }

    /** Return a String of form "Angeltype:donorId", or null if no
      * angel is to be summoned. */
    public String aiSummonAngel(Legion legion)
    {
        if (game.getServer().getClientOption(name, Options.autoSummonAngels))
        {
            return ai.summonAngel(legion, game);
        }
        return null;
    }

    public String aiPickColor(Set colors)
    {
        // Convert favorite colors from a comma-separated string to a list.
        String favorites = game.getServer().getClientStringOption(name,
            Options.favoriteColors);
        List favoriteColors = null;
        if (favorites != null)
        {
            favoriteColors = Utils.split(',', favorites);
        }
        else
        {
            favoriteColors = new ArrayList();
        }
        return ai.pickColor(colors, favoriteColors);
    }


    /** Comparator that forces this player's legion markers to come
     *  before captured markers in sort order. */
    final class MarkerComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            if (!(o1 instanceof String) || !(o2 instanceof String))
            {
                throw new ClassCastException();
            }
            String s1 = (String)o1;
            String s2 = (String)o2;
            String myPrefix = getShortColor();
            if (myPrefix == null)
            {
                myPrefix = "";
            }
            boolean mine1 = s1.startsWith(myPrefix);
            boolean mine2 = s2.startsWith(myPrefix);
            if (mine1 && !mine2)
            {
                return -1;
            }
            else if (mine2 && !mine1)
            {
                return 1;
            }
            else
            {
                return s1.compareTo(s2);
            }
        }
    }
}
