/**
 * Class Player holds the data for one player in a Titan game.
 * @version $Id$
 * author David Ripton
 */

class Player
{
    private String name;
    private String color;       // Black, Blue, Brown, Gold, Green, Red 
    private int startingTower;  // 1-6
    private double score = 0;    // track half-points, then round
    private boolean canSummonAngel = true;
    private String playersEliminated;  // RdBkGr
    private int numMarkersAvailable = 12;
    private String [] markersAvailable = new String[72];
    private String markerSelected;
    private int numLegions = 0;
    private Legion [] legions = new Legion[72];
    private Legion selectedLegion = null;
    private boolean alive = true;
    private int mulligansLeft = 1;
    private int movementRoll;
    private Game game;
    private Legion lastLegionMoved;
    private boolean titanEliminated = false;
    private Legion lastLegionSummonedFrom;
    private boolean canTeleport = true;


    Player(String name, Game game)
    {
        this.name = name;
        this.game = game;
    }


    boolean isAlive()
    {
        return alive;
    }


    String getColor()
    {
        return color;
    }


    void setColor(String color)
    {
        this.color = color;
        for (int i = 0; i <= 8; i++)
        {
            markersAvailable[i] = getShortColor() + '0' + 
                Integer.toString(i + 1);
        }
        for (int i = 9; i <= 11; i++)
        {
            markersAvailable[i] = getShortColor() + Integer.toString(i + 1);
        }
    }


    String getShortColor()
    {
        if (color == "Black") 
        {
            return new String("Bk");
        }
        else if (color == "Blue") 
        {
            return new String("Bl");
        }
        else if (color == "Brown") 
        {
            return new String("Br");
        }
        else if (color == "Gold") 
        {
            return new String("Gd");
        }
        else if (color == "Green") 
        {
            return new String("Gr");
        }
        else if (color == "Red") 
        {
            return new String("Rd");
        }
        else
        {
            return new String("XX");
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


    int getScore()
    {
        return (int) score;
    }


    String getPlayersElim()
    {
        return playersEliminated;
    }


    void addPlayerElim(Player player)
    {
        if (playersEliminated == null)
        {
            playersEliminated = player.getShortColor();
        }
        else
        {
            playersEliminated += player.getShortColor();
        }
    }


    boolean canTitanTeleport()
    {
        return (score >= 400 && canTeleport());
    }


    boolean canSummonAngel()
    {
        return canSummonAngel;
    }


    void allowSummoningAngel()
    {
        canSummonAngel = true;
    }


    void disallowSummoningAngel()
    {
        canSummonAngel = false;
    }


    boolean canTeleport()
    {
        return canTeleport;
    }


    void allowTeleport()
    {
        canTeleport = true;
    }


    void disallowTeleport()
    {
        canTeleport = false;
    }


    Legion getLastLegionSummonedFrom()
    {
        return lastLegionSummonedFrom;
    }


    void setLastLegionSummonedFrom(Legion legion)
    {
        lastLegionSummonedFrom = legion; 
    }


    int getTitanPower()
    {
        return (int) (6 + (score / 100));
    }


    int getNumLegions()
    {
        return numLegions;
    }


    Legion getLegion(int i)
    {
        return legions[i];
    }


    Game getGame()
    {
        return game;
    }


    int legionsMoved()
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


    void commitMoves()
    {
        for (int i = 0; i < numLegions; i++)
        {
            legions[i].commitMove();
        }
    }


    String getName()
    {
        return name;
    }


    int getMovementRoll()
    {
        return movementRoll;
    }


    int getMulligansLeft()
    {
        return mulligansLeft;
    }


    void setMulligansLeft(int number)
    {
        mulligansLeft = number;
    }


    void rollMovement()
    {
        // It's a new turn, so once-per-turn things are allowed again.
        allowSummoningAngel();
        allowTeleport();

        // Make sure that all legions are allowed to move and recruit.
        commitMoves();

        movementRoll = (int) Math.ceil(6 * Math.random());
    }


    void takeMulligan()
    {
        if (mulligansLeft > 0)
        {
            rollMovement();
            mulligansLeft--;
        }
    }


    void markLastLegionMoved(Legion legion)
    {
        lastLegionMoved = legion;
    }


    void undoLastMove()
    {
        if (lastLegionMoved != null)
        {
            lastLegionMoved.undoMove();
            lastLegionMoved = null;
        }
    }


    void undoAllMoves()
    {
        for (int i = 0; i < numLegions; i++)
        {
            legions[i].undoMove();
        }

        allowTeleport();
    }


    void highlightTallLegions()
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


    void undoAllSplits()
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


    int getMaxLegionHeight()
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


    void addLegion(Legion legion)
    {
        numLegions++;
        legions[numLegions - 1] = legion;
    }


    void removeLegion(Legion legion)
    {
        for (int i = 0; i < numLegions; i++)
        {
            if (legion == legions[i])
            {
                // Remove the legion from its current hex.
                legion.getCurrentHex().removeLegion(legion);

                // Return lords and demi-lords to the stacks.
                for (int j = 0; j < legion.getHeight(); j++)
                {
                    Creature creature = legion.getCreature(j);
                    if (creature.isImmortal())
                    {
                        creature.putOneBack();
                    }
                }

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
    }


    void selectLegion(Legion legion)
    {
        selectedLegion = legion;
    }


    void unselectLegion()
    {
        selectedLegion = null;
    }


    Legion getSelectedLegion()
    {
        return selectedLegion;
    }


    int getNumMarkersAvailable()
    {
        return numMarkersAvailable;
    }


    String [] getMarkersAvailable()
    {
        return markersAvailable;
    }


    String getMarker(int i)
    {
        return markersAvailable[i];
    }


    String getSelectedMarker()
    {
        return markerSelected;
    }


    void addSelectedMarker()
    {
        markersAvailable[numMarkersAvailable] = new String(markerSelected);
        numMarkersAvailable++;
        markerSelected = null;
    }


    void clearSelectedMarker()
    {
        markerSelected = null;
    }


    void selectMarker(int i)
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


    void addLegionMarkers(Player player)
    {
        String [] newMarkers = player.getMarkersAvailable();
        int len;
        for (len = 0; newMarkers[len] != null; len++);
        for (int i = 0; i < len; i++)
        {
            markersAvailable[numMarkersAvailable + i] = newMarkers[i];
        }
        numMarkersAvailable += len;
    }


    void addPoints(double points)
    {
        score += points;
        if (game != null)
        {
            game.updateStatusScreen();
        }
    }


    // Remove half-points.
    void truncScore()
    {
        score = Math.floor(score);
    }


    void die(Player player)
    {
        // Engaged legions give half points to the player they're
        // engaged with.  All others give half points to player,
        // if non-null.
        for (int i = 0; i < numLegions; i++)
        {
            MasterHex hex = legions[i].getCurrentHex();
            Legion legion = hex.getEnemyLegion(this);
            if (legion != null)
            {
                Player enemy = legion.getPlayer();
                enemy.addPoints(legions[i].getPointValue() / 2.0);
            }
            else
            {
                if (player != null)
                {
                    player.addPoints(legions[i].getPointValue() / 2.0);
                }
            }
        }

        // Truncate every player's score to an integer value.
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            game.getPlayer(i).truncScore();
        }


        // Removing all legions is icky because the array shrinks as
        // each is removed.
        for (int i = numLegions - 1; i >= 0; i--)
        {
            removeLegion(legions[i]);
        }

        // Mark this player as dead.
        alive = false;

        // Mark who eliminated this player and give him this player's 
        // legion markers.
        if (player != null)
        {
            player.addPlayerElim(this);
            player.addLegionMarkers(this);
        }
        
        numMarkersAvailable = 0;

        game.updateStatusScreen();

        game.getBoard().repaint();

        // See if the game is over.
        game.checkForVictory();
    }


    void eliminateTitan()
    {
        titanEliminated = true;
    }

    boolean isTitanEliminated()
    {
        return titanEliminated;
    }
}
