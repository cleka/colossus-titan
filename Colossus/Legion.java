import java.awt.*;
import java.util.*;

/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 */

class Legion
{
    Chit chit;
    int height;
    String markerId;    // Bk03, Rd12, etc.
    private String splitFrom;   // Bk03, Rd12, etc. or null
    Creature [] creatures = new Creature[8];  // 8 before initial splits
    private int currentHex;
    private int startingHex;
    private boolean moved = false;
    private Player player;

    Legion(int cx, int cy, int scale, String markerId, String splitFrom,
        Container container, int height, int currentHex, 
        Creature creature0, Creature creature1, Creature creature2, 
        Creature creature3, Creature creature4, Creature creature5, 
        Creature creature6, Creature creature7, Player player)
    {
        this.markerId = markerId;
        String imageFilename = "images/" + markerId + ".gif";
        this.chit = new Chit(cx, cy, scale, imageFilename, container);
        this.height = height;
        this.currentHex = currentHex;
        this.startingHex = currentHex;
        this.player = player;
        creatures[0] = creature0;
        creatures[1] = creature1;
        creatures[2] = creature2;
        creatures[3] = creature3;
        creatures[4] = creature4;
        creatures[5] = creature5;
        creatures[6] = creature6;
        creatures[7] = creature7;
    }


    int getPointValue()
    {
        int pointValue = 0;
        for (int i = 0; i < height; i++)
        {
            pointValue += creatures[i].getPointValue();
        }
        return pointValue;
    }


    boolean canFlee()
    {
        for (int i = 0; i < height; i++)
        {
            if (creatures[i].lord)
            {
                return false;
            }
        }
        return true;
    }


    int numCreature(Creature creature)
    {
        int count = 0;
        for (int i = 0; i < height; i++)
        {
            if (creatures[i] == creature)
            {
                count++;
            }
        }
        return count;
    }


    int numLords()
    {
        int count = 0;
        for (int i = 0; i < height; i++)
        {
            if (creatures[i].lord)
            {
                count++;
            }
        }
        return count;
    }


    int getHeight()
    {
        return height;
    }


    Player getPlayer()
    {
        return player;
    }


    boolean hasMoved()
    {
        return moved;
    }


    void moveToHex(MasterHex hex)
    {
        MasterBoard.getHexFromLabel(currentHex).removeLegion(this);
        currentHex = hex.label;
        MasterBoard.getHexFromLabel(currentHex).addLegion(this);
        moved = true;
    }


    void undoMove()
    {
        MasterBoard.getHexFromLabel(currentHex).removeLegion(this);
        currentHex = startingHex;
        MasterBoard.getHexFromLabel(currentHex).addLegion(this);
        moved = false;
    }


    void commitMove()
    {
        startingHex = currentHex;
        moved = false;
    }


    int getCurrentHex()
    {
        return currentHex;
    }


    void addCreature(Creature creature)
    {
        height++;
        creatures[height - 1] = creature;
    }
}
