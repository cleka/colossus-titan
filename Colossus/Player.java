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
    private int score = 0;
    private boolean canSummonAngel = true;
    private String playersEliminated;  // e.g. 1356, based on starting tower
    private int numMarkersAvailable = 12;
    private String [] markersAvailable = new String[60];
    private String markerSelected;
    int numLegions = 0;
    Legion [] legions = new Legion[60];
    private Legion selectedLegion = null;
    private boolean alive = true;
    private int mulligansLeft = 1;
    private int movementRoll;
    private Game game;

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
        return score;
    }


    String getPlayersElim()
    {
        return playersEliminated;
    }


    boolean canTitanTeleport()
    {
        return (score >= 400);
    }


    int titanPower()
    {
        return 6 + (score / 100);
    }


    int getNumLegions()
    {
        return numLegions;
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
        // Wipe out any remaining mulligans.
        mulligansLeft = 0;

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


    void rollMovementDie()
    {
        movementRoll = (int) Math.ceil(6 * Math.random());
    }


    void takeMulligan()
    {
        if (mulligansLeft > 0)
        {
            rollMovementDie();
            mulligansLeft--;
        }
    }


    void undoAllMoves()
    {
        for (int i = 0; i < numLegions; i++)
        {
            legions[i].undoMove();
        }
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


    Legion getSelectedLegion()
    {
        return selectedLegion;
    }


    int getNumMarkersAvailable()
    {
        return numMarkersAvailable;
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
        if (i < 0 && i >= numMarkersAvailable)
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


    void addPoints(int points)
    {
        score += points;
        game.updateStatusScreen();
    }


    void die(Player player)
    {
        // Engaged legions give half points to the player they're
        // engaged with.  All others give half points to player,
        // if non-null.

        // XXX Roundoff errors?
        for (int i = 0; i < numLegions; i++)
        {
            MasterHex hex = legions[i].getCurrentHex();
            Legion legion = hex.getEnemyLegion(this);
            if (legion != null)
            {
                Player enemy = legion.getPlayer();
                enemy.addPoints(legions[i].getPointValue() / 2);
            }
            else
            {
                player.addPoints(legions[i].getPointValue() / 2);
            }
        }

        // Removing all legions is icky because the array shrinks as
        // each is removed.
        int num = numLegions;
        for (int i = 0; i < num; i++)
        {
            removeLegion(legions[0]);
        }

        // Mark this player as dead.
        alive = false;

        game.updateStatusScreen();

        game.getBoard().repaint();
    }
}
