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
    String splitFrom;   // Bk03, Rd12, etc. or null
    Creature [] creatures = new Creature[8];  // 8 before initial splits
    private int currentHex;
    private int startingHex;
    boolean moved = false;

    Legion(int cx, int cy, int scale, String markerId, String splitFrom,
        Container container, int height, int currentHex, 
        Creature creature0, Creature creature1, Creature creature2, 
        Creature creature3, Creature creature4, Creature creature5, 
        Creature creature6, Creature creature7)
    {
        this.markerId = markerId;
        String imageFilename = "images/" + markerId + ".gif";
        this.chit = new Chit(cx, cy, scale, imageFilename, container);
        this.height = height;
        this.currentHex = currentHex;
        this.startingHex = currentHex;
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


    void moveToHex(MasterHex hex)
    {
        currentHex = hex.label;
        chit.setLocationAbs(hex.getOffCenter());
        moved = true;
    }


    void undoMove()
    {
        currentHex = startingHex;
        MasterHex hex = MasterBoard.getHexFromLabel(startingHex);
        chit.setLocationAbs(hex.getOffCenter());
        moved = false;
    }


    int getCurrentHex()
    {
        return currentHex;
    }
}
