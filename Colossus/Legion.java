import java.awt.*;
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
    int numVisibleCreatures;
    private Creature [] visibleCreatures = new Creature[8];
    private MasterHex currentHex;
    private MasterHex startingHex;
    private boolean moved = false;
    private boolean recruited = false;
    private boolean summoned = false;
    private Player player;


    Legion(int cx, int cy, int scale, String markerId, Legion splitFrom,
        Container container, int height, MasterHex hex,
        Creature creature0, Creature creature1, Creature creature2,
        Creature creature3, Creature creature4, Creature creature5,
        Creature creature6, Creature creature7, Player player)
    {
        this.markerId = markerId;
        this.splitFrom = splitFrom;
        this.marker = new Marker(cx, cy, scale, getImageName(), container, this);
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
        // Initial legion contents are public; contents of legions created 
        // by splits are private.
        if (height == 8)
        {
            revealAllCreatures();
        }
        else
        {
            hideAllCreatures();
        }
    }


    public int getPointValue()
    {
        int pointValue = 0;
        for (int i = 0; i < height; i++)
        {
            pointValue += creatures[i].getPointValue();
        }
        return pointValue;
    }


    public void addPoints(int points)
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


    public String getMarkerId()
    {
        return markerId;
    }


    public String getImageName()
    {
        return "images" + File.separator + markerId + ".gif";
    }


    public Chit getMarker()
    {
        return marker;
    }


    public boolean canFlee()
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


    public int numCreature(Creature creature)
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


    public int numLords()
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


    public int getHeight()
    {
        return height;
    }


    public void setHeight(int height)
    {
        this.height = height;
    }


    public Player getPlayer()
    {
        return player;
    }


    public boolean hasMoved()
    {
        return moved;
    }


    public void removeLegion()
    {
        player.removeLegion(this);
    }


    public void moveToHex(MasterHex hex)
    {
        currentHex.removeLegion(this);
        currentHex = hex;
        currentHex.addLegion(this);
        moved = true;
        player.markLastLegionMoved(this);
        // If we teleported, no more teleports are allowed this turn.
        if (currentHex.teleported())
        {
            player.disallowTeleport();
        }
    }


    public void undoMove()
    {
        // If this legion teleported, allow teleporting again.
        if (currentHex.teleported())
        {
            player.allowTeleport();
        }
        currentHex.removeLegion(this);
        currentHex = startingHex;
        currentHex.addLegion(this);
        moved = false;
    }


    public void commitMove()
    {
        startingHex = currentHex;
        moved = false;
        clearRecruited();
        summoned = false;
    }


    public boolean recruited()
    {
        return recruited;
    }


    // hasMoved() is a separate check, so that this function can be used in
    // battle as well as during the muster phase.
    public boolean canRecruit()
    {
        if (recruited || height > 6 ||
            PickRecruit.findEligibleRecruits(this, new Creature[5]) == 0)
        {
            return false;
        }

        return true;
    }


    public void markRecruited()
    {
        recruited = true;
    }


    public void clearRecruited()
    {
        recruited = false;
    }


    public void undoRecruit()
    {
        if (recruited())
        {
            Creature creature = creatures[height - 1];

            // removeCreature() will automatically put immortal creatures back
            // on the stack, but mortal ones must be handled manually.
            if (!creature.isImmortal())
            {
                creature.putOneBack();
            }
            removeCreature(height - 1);

            clearRecruited();
        }
    }


    // Return true if this legion can summon an angel or archangel.
    public boolean canSummonAngel()
    {
        if (height >= 7 || summoned || !player.canSummonAngel())
        {
            return false;
        }

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion candidate = player.getLegion(i);
            if (candidate != this &&
                (candidate.numCreature(Creature.angel) > 0 ||
                candidate.numCreature(Creature.archangel) > 0) &&
                !candidate.getCurrentHex().isEngagement())
            {
                return true;
            }
        }

        return false;
    }


    public boolean summoned()
    {
        return summoned;
    }


    public void markSummoned()
    {
        summoned = true;
    }


    public MasterHex getCurrentHex()
    {
        return currentHex;
    }


    public MasterHex getStartingHex()
    {
        return startingHex;
    }


    public void addCreature(Creature creature)
    {
        if (creature.getCount() > 0)
        {
            creature.takeOne();
            height++;
            creatures[height - 1] = creature;
        }
    }


    public void removeCreature(int i)
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


    public void removeCreature(Creature creature)
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


    public Creature getCreature(int i)
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


    public void setCreature(int i, Creature creature)
    {
        creatures[i] = creature;
    }


    // Recombine this legion into another legion.
    public void recombine(Legion legion)
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


    public void hideAllCreatures()
    {
        for (int i = 0; i < visibleCreatures.length; i++)
        {
            visibleCreatures[i] = null;
        }
        numVisibleCreatures = 0;
    }
    
    
    public void revealAllCreatures()
    {
        for (int i = 0; i < height; i++)
        {
            visibleCreatures[i] = creatures[i];
        }
        numVisibleCreatures = height;
    }


    public void revealCreatures(Creature creature, int numberToReveal)
    {
        int numberAlreadyRevealed = 0;
        for (int i = 0; i < numVisibleCreatures; i++)
        {
            if (visibleCreatures[i] == creature)
            {
                numberAlreadyRevealed++;
            }
        }
        int excess = numberAlreadyRevealed + numberToReveal -
            numCreature(creature);
        if (excess > 0)
        {
            numberToReveal -= excess;
        }

        // Added a hard bounds check here after getting an
        // ArrayIndexOutOfBoundsException when called from
        // AcquireAngel
        for (int i = numVisibleCreatures; i < 7 && i < 
            numVisibleCreatures + numberToReveal; i++)
        {
            visibleCreatures[i] = creature;
        }
        if (numberToReveal > 0)
        {
            numVisibleCreatures += numberToReveal;
        }
    }


    // Remove one creature from the visible list.  This is needed to
    // remove summoned-out angels.
    public void hideCreature(Creature creature)
    {
        for (int i = 0; i < numVisibleCreatures; i++)
        {
            if (visibleCreatures[i] == creature)
            {
                for (int j = i; j < numVisibleCreatures - 1; j++)
                {
                    visibleCreatures[j] = visibleCreatures[j + 1];
                }
                visibleCreatures[numVisibleCreatures - 1] = null;
                numVisibleCreatures--;
                return;
            }
        }
    }


    public int getNumVisibleCreatures()
    {
        return numVisibleCreatures;
    }
    
    
    public String getVisibleCreatureImageName(int i)
    {
        if (i > numVisibleCreatures - 1)
        {
            return "images" + File.separator + "Question.gif";
        }
        else
        {
            return visibleCreatures[i].getImageName();
        }
    }
}
