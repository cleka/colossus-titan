/**
 * Class Player holds the data for one player in a Titan game.
 * @version $Id$
 * author David Ripton
 */

class Player
{
    private String name;
    private String color;       // Black, Blue, Brown, Gold, Green, Red 
    int startingTower;  // 1-6
    int score = 0;
    int angels = 1;     // number of angels + archangels in legions
    boolean canSummonAngel = true;
    String playersEliminated;  // e.g. 1356, based on starting tower
    int numMarkersAvailable = 12;
    String [] markersAvailable = new String[60];
    String markerSelected;
    int numLegions = 0;
    Legion [] legions = new Legion[60];
    boolean alive = true;
    int mulligansLeft = 1;
    int movementRoll;
    int selectedLegion = -1;

    Player(String name)
    {
        this.name = name;
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


    boolean canTitanTeleport()
    {
        return (score >= 400);
    }


    int titanPower()
    {
        return 6 + (score / 100);
    }


    int legionsMoved()
    {
        int total = 0;
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].moved)
            {
                total++;
            }
        }

        return total;
    }

    String getName()
    {
        return name;
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

}
