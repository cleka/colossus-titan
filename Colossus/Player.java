import java.util.*;

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
    private TreeSet markersAvailable = new TreeSet(new markerComparator());
    private ArrayList legions = new ArrayList();
    private boolean dead;
    private boolean titanEliminated;
    private Legion donor;
    private Legion mover;
    private Legion lastLegionSummonedFrom;

    /** Stack of legions, to allow multiple levels of undo for splits,
     *  moves, and recruits. */
    private LinkedList undoStack = new LinkedList();



    public Player(String name, Game game)
    {
        this.name = name;
        this.game = game;
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


    public boolean hasSummoned()
    {
        return summoned;
    }


    public void setSummoned(boolean summoned)
    {
        this.summoned = summoned;
    }


    public boolean hasTeleported()
    {
        return teleported;
    }


    public void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }


    public Legion getLastLegionSummonedFrom()
    {
        return lastLegionSummonedFrom;
    }


    public void setLastLegionSummonedFrom(Legion legion)
    {
        lastLegionSummonedFrom = legion;
    }


    public int getTitanPower()
    {
        return (int) (6 + (getScore() / 100));
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


    public List getLegions()
    {
        return legions;
    }


    public Game getGame()
    {
        return game;
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
        teleported = false;
        movementRoll = 0;

        clearUndoStack();

        // Make sure that all legions are allowed to move and recruit.
        commitMoves();

        // Clear old entry side and teleport information.
        game.getBoard().clearAllEntrySides();

        // Hide all stack contents from other players.
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            legion.hideAllCreatures();
        }
    }


    public void rollMovement()
    {
        // It's a new turn, so once-per-turn things are allowed again.

        if (game.getOption(Game.chooseMovement))
        {
            movementRoll = PickRoll.pickRoll(game.getMasterFrame(),
                "Pick movement roll");
            Game.logEvent(getName() + " chooses a " + movementRoll +
                " for movement");
        }
        else
        {
            movementRoll = Game.rollDie();
            Game.logEvent(getName() + " rolls a " + movementRoll +
                " for movement");
        }
        game.showMovementRoll(movementRoll);
    }


    public void takeMulligan()
    {
        if (mulligansLeft > 0)
        {
            undoAllMoves();
            Game.logEvent(getName() + " takes a mulligan");
            mulligansLeft--;
            movementRoll = 0;
        }
    }


    /** Clear the legion undo stack.  This should be called at the
     *  beginning of each phase. */
    public void clearUndoStack()
    {
        undoStack.clear();
    }


    public void setLastLegionMoved()
    {
        undoStack.addFirst(mover);
        mover = null;
    }


    public void setLastLegionSplitOff(Legion legion)
    {
        undoStack.addFirst(legion);
    }


    public void undoLastMove()
    {
        if (!undoStack.isEmpty())
        {
            Legion legion = (Legion)undoStack.removeFirst();
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
            MasterHex hex = legion.getCurrentHex();
            if (hex.getNumFriendlyLegions(this) > 1 &&
                game.countConventionalMoves(legion) > 0)
            {
                return true;
            }
        }
        return false;
    }


    public void undoLastRecruit()
    {
        if (!undoStack.isEmpty())
        {
            Legion legion = (Legion)undoStack.removeFirst();
            legion.undoRecruit();
        }

        // Update number of creatures in status window.
        game.updateStatusScreen();
    }


    public void setLastLegionRecruited(Legion legion)
    {
        undoStack.addFirst(legion);
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
        game.updateStatusScreen();
    }


    public void highlightTallLegions()
    {
        HashSet set = new HashSet();

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getHeight() >= 7)
            {
                MasterHex hex = legion.getCurrentHex();
                set.add(hex.getLabel());
            }
        }

        game.getBoard().selectHexesByLabels(set);
    }


    public void undoLastSplit()
    {
        if (!undoStack.isEmpty())
        {
            Legion splitoff = (Legion)undoStack.removeFirst();
            splitoff.recombine(splitoff.getParent(), true);
            highlightTallLegions();
        }
    }


    public void undoAllSplits()
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            Legion parent = legion.getParent();
            if (parent != null && parent.getCurrentHex() ==
                legion.getCurrentHex())
            {
                legion.recombine(parent, false);
                it.remove();
            }
        }

        highlightTallLegions();
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
    }


    public void setDonor(Legion donor)
    {
        this.donor = donor;
    }


    public Legion getDonor()
    {
        return donor;
    }


    public void setMover(Legion mover)
    {
        this.mover = mover;
    }


    public Legion getMover()
    {
        return mover;
    }


    public int getNumMarkersAvailable()
    {
        return markersAvailable.size();
    }


    public Collection getMarkersAvailable()
    {
        return markersAvailable;
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
                game.updateStatusScreen();
            }

            Game.logEvent(getName() + " earns " + points + " points");
        }
    }


    /** Remove half-points. */
    public void truncScore()
    {
        score = Math.floor(score);
    }


    public void die(Player slayer, boolean checkForVictory)
    {
        // Engaged legions give half points to the player they're
        // engaged with.  All others give half points to slayer,
        // if non-null.

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            MasterHex hex = legion.getCurrentHex();
            Legion enemyLegion = hex.getEnemyLegion(this);
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
        dead = true;

        // Record the slayer and give him this player's legion markers.
        if (slayer != null)
        {
            slayer.addPlayerElim(this);
            slayer.addLegionMarkers(this);
        }

        game.updateStatusScreen();

        game.getBoard().repaint();

        Game.logEvent(getName() + " dies");

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


    /** Comparator that forces this player's legion markers to come
     *  before captured markers in sort order. */
    final class markerComparator implements Comparator
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
