import java.awt.*;
import java.util.*;
import java.io.*;

/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 */

class Legion
{
    private Chit marker;
    private int height;
    private String markerId;    // Bk03, Rd12, etc.
    private Legion splitFrom;
    private Creature [] creatures = new Creature[8];  // 8 before initial splits
    private MasterHex currentHex;
    private MasterHex startingHex;
    private boolean moved = false;
    private boolean recruited = false;
    private boolean summoned = false;
    private Player player;
    private int entrySide;


    Legion(int cx, int cy, int scale, String markerId, Legion splitFrom,
        Container container, int height, MasterHex hex, 
        Creature creature0, Creature creature1, Creature creature2, 
        Creature creature3, Creature creature4, Creature creature5, 
        Creature creature6, Creature creature7, Player player)
    {
        this.markerId = markerId;
        this.splitFrom = splitFrom;
        this.marker = new Chit(cx, cy, scale, getImageName(), container, 
            false);
        this.height = height;
        this.currentHex = hex;
        this.startingHex = hex;
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
        if (player != null)
        {
            player.addPoints(points);        

            int score = player.getScore();
            int tmpScore = score;
            boolean didArchangel = false;

            while (height < 7 && tmpScore / 100 > (score - points) / 100)
            {
                if (tmpScore / 500 > (score - points) / 500 && !didArchangel)
                {
                    // Allow Archangel.
                    new AcquireAngel(player.getGame().getBoard(), this, true);
                    tmpScore -= 100;
                    didArchangel = true;
                }
                else
                {
                    // Disallow Archangel.
                    new AcquireAngel(player.getGame().getBoard(), this, false);
                    tmpScore -= 100;
                }
            }
        }
    }


    String getMarkerId()
    {
        return markerId;
    }


    String getImageName()
    {
        return "images" + File.separator + markerId + ".gif";
    }


    Chit getMarker()
    {
        return marker;
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


    void setHeight(int height)
    {
        this.height = height;
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
        player.removeLegion(this);
    }


    void moveToHex(MasterHex hex)
    {
        currentHex.removeLegion(this);
        currentHex = hex;
        currentHex.addLegion(this);
        moved = true;
        player.markLastLegionMoved(this);
        setEntrySide(hex.getEntrySide());
        // If we teleported, no more teleports are allowed this turn.
        if (getEntrySide() == -1)
        {
            player.disallowTeleport();
        }
    }


    void undoMove()
    {
        // If this legion teleported, allow teleporting again.
        if (currentHex.getEntrySide() == -1)
        {
            player.allowTeleport();
        }
        currentHex.removeLegion(this);
        currentHex = startingHex;
        currentHex.addLegion(this);
        moved = false;
    }


    void commitMove()
    {
        startingHex = currentHex;
        moved = false;
        recruited = false;
        summoned = false;
    }


    int getEntrySide()
    {
        return entrySide;
    }
    
    
    void setEntrySide(int side)
    {
        entrySide = side;
    }


    boolean recruited()
    {
        return recruited;
    }


    boolean canRecruit()
    {
        if (recruited || height > 6)
        {
            return false;
        }

        return true;
    }


    void markRecruited()
    {
        recruited = true;
    }


    boolean summoned()
    {
        return summoned;
    }

    
    void markSummoned()
    {
        summoned = true;
    }


    MasterHex getCurrentHex()
    {
        return currentHex;
    }


    MasterHex getStartingHex()
    {
        return startingHex;
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


    void removeCreature(int i)
    {
        if (i < 0 && i > height - 1)
        {
            return;
        }

        // If the creature is a lord or demi-lord, put it back in the stacks.
        if (creatures[i].isImmortal())
        {
            creatures[i].putOneBack();
        }

        for (int j = i; j < height - 1; j++)
        {
            creatures[j] = creatures[j + 1];
        }
        creatures[height - 1] = null;
        height--;

        // If there are no creatures left, disband the legion.
        if (height == 0)
        {
            removeLegion();
        }
    }
    
    
    void removeCreature(Creature creature)
    {
        for (int i = 0; i < height; i++)
        {
            if (creatures[i] == creature)
            {
                removeCreature(i);
                return;
            }
        }
    }


    Creature getCreature(int i)
    {
        if (i > height - 1)
        {
            return null;
        }
        else
        {
            return creatures[i];
        }
    }


    void setCreature(int i, Creature creature)
    {
        creatures[i] = creature;
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
