/**
 * Class Player holds the data for one player in a Titan game.
 * @version $Id$
 * @author David Ripton
 */

public class Player
{
    private String name;
    private String color;       // Black, Blue, Brown, Gold, Green, Red
    private int startingTower;  // 1-6
    private double score;    // track half-points, then round
    private boolean canSummonAngel = true;
    private String playersEliminated;  // RdBkGr
    private int numMarkersAvailable; 
    private String [] markersAvailable = new String[72];
    private String markerSelected;
    private int numLegions; 
    private Legion [] legions = new Legion[72];
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
        numMarkersAvailable = 0;
        markersAvailable = new String[72];

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
        return numLegions;
    }


    public void setNumLegions(int numLegions)
    {
        this.numLegions = numLegions;
    }


    public Legion getLegion(int i)
    {
        return legions[i];
    }


    public Game getGame()
    {
        return game;
    }


    public int legionsMoved()
    {
        int total = 0;
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].hasMoved())
            {
                total++;
            }
        }

        return total;
    }


    // Return the number of this player's legions that have legal 
    // non-teleport moves remaining.
    public int countMobileLegions()
    {
        int count = 0;

        for (int i = 0; i < getNumLegions(); i++)
        {
            if (game.countConventionalMoves(getLegion(i)) > 0)
            {
                count++;
            }
        }

        return count;
    }


    public void commitMoves()
    {
        for (int i = 0; i < numLegions; i++)
        {
            legions[i].commitMove();
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
        for (int i = 0; i < numLegions; i++)
        {
            legions[i].hideAllCreatures();
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
        for (int i = 0; i < numLegions; i++)
        {
            legions[i].undoMove();
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
        for (int i = 0; i < numLegions; i++)
        {
            legions[i].undoRecruit();
        }
        
        // Update number of creatures in status window. 
        game.updateStatusScreen();
    }


    public void highlightTallLegions()
    {
        for (int i = 0; i < numLegions; i++)
        {
            Legion legion = legions[i];
            if (legion.getHeight() >= 7)
            {
                MasterHex hex = legion.getCurrentHex();
                hex.select();
                hex.repaint();
            }
        }
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
                    splitoff.recombine(parent);
                }
            }
            
            lastLegionSplit = null;

            highlightTallLegions();
        }
    }


    public void undoAllSplits()
    {
        for (int i = numLegions - 1; i >= 0; i--)
        {
            Legion legion = legions[i];
            MasterHex hex = legion.getCurrentHex();
            if (hex.getNumLegions() > 1)
            {
                Legion parent = hex.getLegion(0);
                if (parent != legion)
                {
                    legion.recombine(parent);
                }
            }
        }

        highlightTallLegions();
    }


    public int getMaxLegionHeight()
    {
        int height = 0;
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getHeight() > height)
            {
                height = legions[i].getHeight();
            }
        }
        return height;
    }
    
    
    public int getNumCreatures()
    {
        int total = 0;
        for (int i = 0; i < numLegions; i++)
        {
            total += legions[i].getHeight();
        }
        return total;
    }


    public void addLegion(Legion legion)
    {
        numLegions++;
        legions[numLegions - 1] = legion;
    }


    /** Eliminate this legion */
    public void removeLegion(Legion legion)
    {
        for (int i = 0; i < numLegions; i++)
        {
            if (legion == legions[i])
            {
                // Remove the legion from its current hex.
                legion.getCurrentHex().removeLegion(legion);

                StringBuffer log = new StringBuffer("Legion ");
                log.append(name);
                log.append("(");

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
                markersAvailable[numMarkersAvailable] = legion.getMarkerId();
                numMarkersAvailable++;

                // Compress the legions array.
                for (int j = i; j < numLegions - 1; j++)
                {
                    legions[j] = legions[j + 1];
                }
                legions[numLegions - 1] = null;
                numLegions--;
            }
        }

        sortLegionMarkers();
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
        return numMarkersAvailable;
    }


    public String [] getMarkersAvailable()
    {
        return markersAvailable;
    }


    public String getMarker(int i)
    {
        return markersAvailable[i];
    }


    public String getSelectedMarker()
    {
        return markerSelected;
    }


    public void addSelectedMarker()
    {
        markersAvailable[numMarkersAvailable] = new String(markerSelected);
        numMarkersAvailable++;
        sortLegionMarkers();
        markerSelected = null;
    }


    public void clearSelectedMarker()
    {
        markerSelected = null;
    }


    public void selectMarker(int i)
    {
        if (i < 0 || i >= numMarkersAvailable)
        {
            markerSelected = null;
        }
        else
        {
            markerSelected = new String(markersAvailable[i]);

            // Adjust other markers because this one is taken.
            for (int j = i; j < numMarkersAvailable - 1; j++)
            {
                markersAvailable[j] = new String(markersAvailable[j + 1]);
            }

            markersAvailable[numMarkersAvailable - 1] = new String("");
            numMarkersAvailable--;
        }
    }


    public void addLegionMarker(String markerId)
    {
        markersAvailable[numMarkersAvailable] = markerId;
        numMarkersAvailable++;
        sortLegionMarkers();
    }


    public void addLegionMarkers(Player player)
    {
        String [] newMarkers = player.getMarkersAvailable();
        int len;
        for (len = 0; newMarkers[len] != null; len++);
        for (int i = 0; i < len; i++)
        {
            markersAvailable[numMarkersAvailable + i] = newMarkers[i];
        }
        numMarkersAvailable += len;
        sortLegionMarkers();
    }


    public void sortLegionMarkers()
    {
        for (int i = 0; i < numMarkersAvailable; i++)
        {
            for (int j = i; j > 0 && markersAvailable[j - 1].compareTo(
                markersAvailable[j]) > 0; j--)
            {
                String temp = markersAvailable[j - 1];
                markersAvailable[j - 1] = markersAvailable[j];
                markersAvailable[j] = temp;
            }
        }
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

        for (int i = 0; i < numLegions; i++)
        {
            Legion legion = legions[i];
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
        }

        // Truncate every player's score to an integer value.
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            game.getPlayer(i).truncScore();
        }


        // Removing all legions is tricky because the array shrinks as
        // each is removed.
        for (int i = numLegions - 1; i >= 0; i--)
        {
            removeLegion(legions[i]);
        }

        // Mark this player as dead.
        dead = true;

        // Mark the slayer and give him this player's legion markers.
        if (slayer != null)
        {
            slayer.addPlayerElim(this);
            slayer.addLegionMarkers(this);
        }

        numMarkersAvailable = 0;

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
