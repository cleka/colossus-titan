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
    private String markerId;    // Bk03, Rd12, etc.
    private Legion splitFrom;
    Creature [] creatures = new Creature[8];  // 8 before initial splits
    private MasterHex currentHex;
    private MasterHex startingHex;
    private boolean moved = false;
    private Player player;

    Legion(int cx, int cy, int scale, String markerId, Legion splitFrom,
        Container container, int height, MasterHex currentHex, 
        Creature creature0, Creature creature1, Creature creature2, 
        Creature creature3, Creature creature4, Creature creature5, 
        Creature creature6, Creature creature7, Player player)
    {
        this.markerId = markerId;
        this.splitFrom = splitFrom;
        this.chit = new Chit(cx, cy, scale, getImageName(), container);
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


    void addPoints(int points)
    {
        player.addPoints(points);        

        int score = player.getScore();

        while (height < 7 && score / 100 > (score - points) / 100)
        {
            if (score / 500 > (score - points) / 500)
            {
                // XXX Need archangel / angel dialog
                if (Creature.archangel.getCount() > 0)
                {
                    Creature.archangel.takeOne();
                    addCreature(Creature.archangel);
                }
                else
                {
                    Creature.angel.takeOne();
                    addCreature(Creature.angel);
                }
                score -= 100;
            }
            else
            {
                if (Creature.angel.getCount() > 0)
                {
                    Creature.angel.takeOne();
                    addCreature(Creature.angel);
                }
                score -= 100;
            }
        }
    }


    String getMarkerId()
    {
        return markerId;
    }


    String getImageName()
    {
        return "images/" + markerId + ".gif";
    }


    boolean canFlee()
    {
        for (int i = 0; i < height; i++)
        {
            if (creatures[i].isLord())
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
            if (creatures[i].isLord())
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


    void removeLegion()
    {
        currentHex.removeLegion(this);
        player.removeLegion(this);
    }


    void moveToHex(MasterHex hex)
    {
        currentHex.removeLegion(this);
        currentHex = hex;
        currentHex.addLegion(this);
        moved = true;
    }


    void undoMove()
    {
        currentHex.removeLegion(this);
        currentHex = startingHex;
        currentHex.addLegion(this);
        moved = false;
    }


    void commitMove()
    {
        startingHex = currentHex;
        moved = false;
    }


    MasterHex getCurrentHex()
    {
        return currentHex;
    }


    void addCreature(Creature creature)
    {
        if (creature.getCount() > 0)
        {
            creature.takeOne();
            height++;
            creatures[height - 1] = creature;
        }
    }


    // Recombine this legion into another legion.
    void recombine(Legion legion)
    {
        for (int i = 0; i < height; i++)
        {
            creatures[i].putOneBack();
            legion.addCreature(creatures[i]); 
        }
        // Prevent double-returning lords to stacks.
        height = 0;
        removeLegion();
    }
}
