import java.util.*;

/**
 * Class Player holds the data for one player in a Titan game.
 * @version $Id$
 * @author David Ripton
 */

public class Player implements Comparable
{
    private String name;
    private String color;              // Black, Blue, Brown, Gold, Green, Red
    private int startingTower;         // 1-6
    private double score;              // track half-points, then round
    private boolean canSummonAngel = true;
    private String playersEliminated;  // RdBkGr

    private TreeSet markersAvailable = new TreeSet();
    private String markerSelected;

    private ArrayList legions = new ArrayList();
    private Legion selectedLegion = null;
    private boolean dead;
    private int mulligansLeft = 1;
    private int movementRoll;
    private Game game;
    private Legion lastLegionMoved;
    private Legion lastLegionSplit;
    private boolean titanEliminated;
    private Legion lastLegionSummonedFrom;
    private boolean canTeleport = true;
    private Legion lastLegionRecruited;


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
            return new String("Bk");
        }
        else if (color.equals("Blue"))
        {
            return new String("Bl");
        }
        else if (color.equals("Brown"))
        {
            return new String("Br");
        }
        else if (color.equals("Gold"))
        {
            return new String("Gd");
        }
        else if (color.equals("Green"))
        {
            return new String("Gr");
        }
        else if (color.equals("Red"))
        {
            return new String("Rd");
        }
        else
        {
            return new String("XX");
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
        This is inconsistent with equals() */
    public int compareTo(Object object) throws ClassCastException
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
        this.playersEliminated = new String(playersEliminated);
    }


    public void addPlayerElim(Player player)
    {
        if (playersEliminated == null)
        {
            playersEliminated = new String(player.getShortColor());
        }
        else
        {
            playersEliminated = new String(playersEliminated + 
                player.getShortColor());
        }
    }


    public boolean canTitanTeleport()
    {
        return (score >= 400 && canTeleport());
    }


    public boolean canSummonAngel()
    {
        return canSummonAngel;
    }


    public void allowSummoningAngel()
    {
        canSummonAngel = true;
    }


    public void disallowSummoningAngel()
    {
        canSummonAngel = false;
    }


    public boolean canTeleport()
    {
        return canTeleport;
    }


    public void allowTeleport()
    {
        canTeleport = true;
    }


    public void disallowTeleport()
    {
        canTeleport = false;
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


    public Collection getLegions()
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
        int total = 0;

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.hasMoved())
            {
                total++;
            }
        }

        return total;
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


    public int getMulligansLeft()
    {
        return mulligansLeft;
    }


    public void setMulligansLeft(int number)
    {
        mulligansLeft = number;
    }


    public void rollMovement()
    {
        // It's a new turn, so once-per-turn things are allowed again.
        allowSummoningAngel();
        allowTeleport();

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

        if (game.getForcedMovementRoll() != 0)
        {
            movementRoll = game.getForcedMovementRoll();
            Game.logEvent(getName() + " rolled a " + movementRoll + 
                " for movement (forced!)");
            game.clearForcedMovementRoll();
        }
        else
        {
            movementRoll = Game.rollDie();
            Game.logEvent(getName() + " rolled a " + movementRoll + 
                " for movement");
        }
    }


    public void takeMulligan()
    {
        if (mulligansLeft > 0)
        {
            undoAllMoves();
            Game.logEvent(getName() + " took a mulligan");
            mulligansLeft--;
        }
    }


    public void markLastLegionMoved(Legion legion)
    {
        lastLegionMoved = legion;
    }
    
    
    public void markLastLegionSplit(Legion legion)
    {
        lastLegionSplit = legion;
    }


    public void undoLastMove()
    {
        if (lastLegionMoved != null)
        {
            lastLegionMoved.undoMove();
            lastLegionMoved = null;
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

    
    public void undoLastRecruit()
    {
        if (lastLegionRecruited != null)
        {
            lastLegionRecruited.undoRecruit();
            lastLegionRecruited = null;
        }

        // Update number of creatures in status window. 
        game.updateStatusScreen();
    }


    public void markLastLegionRecruited(Legion legion)
    {
        lastLegionRecruited = legion;
    }


    public void clearLastLegionRecruited()
    {
        lastLegionRecruited = null;
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
        TreeSet set = new TreeSet();

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
        if (lastLegionSplit != null)
        {
            MasterHex hex = lastLegionSplit.getCurrentHex();
            hex.getNumLegions();
            while (hex.getNumLegions() > 1)
            {
                Legion parent = hex.getLegion(0);
                Legion splitoff = hex.getLegion(1);
                if (parent != splitoff)
                {
                    splitoff.recombine(parent, true);
                }
            }
            
            lastLegionSplit = null;

            highlightTallLegions();
        }
    }


    public void undoAllSplits()
    {
        // Run through the legion list backwards, since the newly split 
        // legions that need to be combined will appear at the end.
        ListIterator lit = legions.listIterator(legions.size());
        while (lit.hasPrevious())
        {
            Legion legion = (Legion)lit.previous();
            MasterHex hex = legion.getCurrentHex();
            if (hex.getNumLegions() > 1)
            {
                Legion parent = hex.getLegion(0);
                if (parent != legion)
                {
                    legion.recombine(parent, false);
                    lit.remove();
                }
            }
        }

        highlightTallLegions();
    }


    public int getMaxLegionHeight()
    {
        int height = 0;

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getHeight() > height)
            {
                height = legion.getHeight();
            }
        }

        return height;
    }
    
    
    public int getNumCreatures()
    {
        int total = 0;

        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            total += legion.getHeight();
        }

        return total;
    }


    public void addLegion(Legion legion)
    {
        legions.add(legion);
    }

    /** Do the cleanup required before this legion can be removed. */
    public void prepareToRemoveLegion(Legion legion)
    {
        // Remove the legion from its current hex.
        legion.getCurrentHex().removeLegion(legion);

        StringBuffer log = new StringBuffer("Legion ");
        log.append(legion.getName());
        log.append(" (");

        int height = legion.getHeight();
        // Return lords and demi-lords to the stacks.
        for (int j = 0; j < height; j++)
        {
            Creature creature = legion.getCreature(j);
            log.append(creature.getName());
            if (j < height - 1)
            {
                log.append(", ");
            }
            if (creature.isImmortal())
            {
                creature.putOneBack();
            }
        }
        log.append(") is eliminated");
        Game.logEvent(log.toString());

        // Free up the legion marker.
        markersAvailable.add(legion.getMarkerId());
    }


    /** Eliminate this legion */
    public void removeLegion(Legion legion)
    {
        prepareToRemoveLegion(legion);

        legions.remove(legion);
    }


    public void selectLegion(Legion legion)
    {
        selectedLegion = legion;
    }


    public void unselectLegion()
    {
        selectedLegion = null;
    }


    public Legion getSelectedLegion()
    {
        return selectedLegion;
    }


    public int getNumMarkersAvailable()
    {
        return markersAvailable.size();
    }


    public Collection getMarkersAvailable()
    {
        return (Collection)markersAvailable;
    }


    public String getSelectedMarker()
    {
        return markerSelected;
    }


    public void addSelectedMarker()
    {
        markersAvailable.add(new String(markerSelected));
        markerSelected = null;
    }


    public void clearSelectedMarker()
    {
        markerSelected = null;
    }


    public void selectMarker(String markerId)
    {
        // Remove the selected marker from the list of those available.
        boolean found = markersAvailable.remove(markerId);
        if (found)
        {
            markerSelected = markerId;
        }
        else
        {
            markerSelected = null;
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
        score += points;
        if (game != null)
        {
            game.updateStatusScreen();
        }

        Game.logEvent(getName() + " earns " + points + " points");
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
            prepareToRemoveLegion(legion);
            it.remove();
        }

        // Truncate every player's score to an integer value.
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            game.getPlayer(i).truncScore();
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
}
