import java.awt.*;

/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 */

public class Legion
{
    private Marker marker;
    private int height;
    private String markerId;    // Bk03, Rd12, etc.
    private Legion splitFrom;
    private Critter [] critters = new Critter[8];  // 8 before initial splits
    private MasterHex currentHex;
    private MasterHex startingHex;
    private boolean moved;
    private boolean recruited;
    private boolean summoned;
    private Player player;
    private int battleTally;


    public Legion(int scale, String markerId, Legion splitFrom,
        Container container, int height, MasterHex hex,
        Creature creature0, Creature creature1, Creature creature2,
        Creature creature3, Creature creature4, Creature creature5,
        Creature creature6, Creature creature7, Player player)
    {
        this.markerId = markerId;
        this.splitFrom = splitFrom;
        this.marker = new Marker(scale, getImageName(), container, this);
        this.height = height;
        this.currentHex = hex;
        this.startingHex = hex;
        this.player = player;

        if (creature0 != null)
        {
            critters[0] = new Critter(creature0, false, this);
        }
        if (creature1 != null)
        {
            critters[1] = new Critter(creature1, false, this);
        }
        if (creature2 != null)
        {
            critters[2] = new Critter(creature2, false, this);
        }
        if (creature3 != null)
        {
            critters[3] = new Critter(creature3, false, this);
        }
        if (creature4 != null)
        {
            critters[4] = new Critter(creature4, false, this);
        }
        if (creature5 != null)
        {
            critters[5] = new Critter(creature5, false, this);
        }
        if (creature6 != null)
        {
            critters[6] = new Critter(creature6, false, this);
        }
        if (creature7 != null)
        {
            critters[7] = new Critter(creature7, false, this);
        }

        // Initial legion contents are public; contents of legions created 
        // by splits are private.
        if (height == 8)
        {
            revealAllCreatures();
        }
        else
        {
            // When loading a game, we handle revealing visible creatures
            // after legion creation.
            hideAllCreatures();
        }
    }


    public int getPointValue()
    {
        int pointValue = 0;
        for (int i = 0; i < height; i++)
        {
            pointValue += critters[i].getPointValue();
        }
        return pointValue;
    }


    public void addPoints(int points)
    {
        try
        {
            player.addPoints(points);
            
            MasterBoard board = player.getGame().getBoard();
            int score = player.getScore();
            int tmpScore = score;
            boolean didArchangel = false;
            
            while (height < 7 && tmpScore / 100 > (score - points) / 100)
            {
                if (tmpScore / 500 > (score - points) / 500 &&
                    !didArchangel)
                {
                    // Allow Archangel.
                    new AcquireAngel(board, this, true);
                    tmpScore -= 100;
                    didArchangel = true;
                }
                else
                {
                    // Disallow Archangel.
                    new AcquireAngel(board, this, false);
                    tmpScore -= 100;
                }
            }
        }
        catch (NullPointerException e)
        {
            // If we're testing battles with player or game or board
            // null, don't crash.
            e.printStackTrace();
        }
    }


    public void clearBattleTally()
    {
        battleTally = 0;
    }


    public void addToBattleTally(int points)
    {
        battleTally += points;
    }


    public void addBattleTallyToPoints()
    {
        addPoints(battleTally);
        clearBattleTally();
    }


    public String getMarkerId()
    {
        return markerId;
    }


    public String getImageName()
    {
        return "images/" + markerId + ".gif";
    }


    public Marker getMarker()
    {
        return marker;
    }


    public boolean canFlee()
    {
        for (int i = 0; i < height; i++)
        {
            if (critters[i].isLord())
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
            if (critters[i].getName().equals(creature.getName()))
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
            if (critters[i].isLord())
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
        boolean teleported = hex.teleported();
        Game.logEvent("Legion " + getMarkerId() + " in " +
            currentHex.getDescription() + 
            (teleported ?  " teleports " : " moves ") + 
            "to " + hex.getDescription());

        currentHex.removeLegion(this);
        currentHex = hex;
        currentHex.addLegion(this);
        moved = true;
        player.markLastLegionMoved(this);
        // If we teleported, no more teleports are allowed this turn.
        if (teleported)
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
        Game.logEvent("Legion " + getMarkerId() + " undoes its move");
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
        if (recruited || height > 6 || !getPlayer().isAlive() ||
            Game.findEligibleRecruits(this, new Creature[5]) == 0)
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
            Critter critter = critters[height - 1];

            // removeCreature() will automatically put immortal critters back
            // on the stack, but mortal ones must be handled manually.
            if (!critter.isImmortal())
            {
                critter.putOneBack();
            }
            removeCreature(height - 1);

            clearRecruited();

            Game.logEvent("Legion " + getMarkerId() + " undoes its recruit");
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
            // Newly added critters are visible.
            critters[height - 1] = new Critter(creature, true, this);
        }
    }


    public void removeCreature(int i)
    {
        if (i < 0 && i > height - 1)
        {
            return;
        }

        // If the creature is a lord or demi-lord, put it back in the stacks.
        if (critters[i].isImmortal())
        {
            critters[i].putOneBack();
        }

        for (int j = i; j < height - 1; j++)
        {
            critters[j] = critters[j + 1];
        }
        critters[height - 1] = null;
        height--;

        // If there are no critters left, disband the legion.
        if (height == 0)
        {
            removeLegion();
        }
    }


    public void removeCreature(Creature creature)
    {
        for (int i = 0; i < height; i++)
        {
            if (critters[i].getName().equals(creature.getName()))
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
            return critters[i].getCreature();
        }
    }
    
    
    public Critter getCritter(int i)
    {
        if (i > height - 1)
        {
            return null;
        }
        else
        {
            return critters[i];
        }
    }
    
    
    public Critter getCritter(Creature creature)
    {
        for (int i = 0; i < height; i++)
        {
            if (critters[i].getName().equals(creature.getName()))
            {
                return critters[i];
            }
        }
        return null;
    }


    public void setCreature(int i, Creature creature)
    {
        // This is called from SplitLegion, so critter will be invisible.
        if (creature == null)
        {
            critters[i] = null;
        }
        else
        {
            critters[i] = new Critter(creature, false, this);
        }
    }


    // Recombine this legion into another legion.
    public void recombine(Legion legion)
    {
        for (int i = 0; i < height; i++)
        {
            critters[i].putOneBack();
            legion.addCreature(critters[i]);
        }
        // Prevent double-returning lords to stacks.
        height = 0;
        removeLegion();

        Game.logEvent("Legion " + getMarkerId() + 
            " recombined into legion " + legion.getMarkerId());
    }


    public void hideAllCreatures()
    {
        for (int i = 0; i < height; i++)
        {
            critters[i].setVisible(false);
        }
    }
    
    
    public void revealAllCreatures()
    {
        for (int i = 0; i < height; i++)
        {
            critters[i].setVisible(true);
        }
    }
    
    
    public void healAllCreatures()
    {
        for (int i = 0; i < height; i++)
        {
            critters[i].heal();
        }
    }
    
    
    public void revealCreature(int index)
    {
        if (index < getHeight())
        {
            critters[index].setVisible(true);
        }
    }


    public void revealCreatures(Creature creature, int numberToReveal)
    {
        int numberAlreadyRevealed = 0;
        for (int i = 0; i < height; i++)
        {
            if (critters[i].getCreature().getName().equals(
                creature.getName()) && critters[i].isVisible())
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

        for (int i = 0; i < height; i++)
        {
            if (critters[i].getCreature().getName().equals(
                creature.getName()) && !critters[i].isVisible())
            {
                critters[i].setVisible(true);
                numberToReveal--;
                if (numberToReveal == 0)
                {
                    return;
                }
            }
        }
    }


    // Reveal the lord who teleported the legion.  Pick one if necessary.
    public void revealTeleportingLord(Frame parentFrame)
    {
        // Count how many types of lords are in the stack.  If only one,
        // reveal it.

        // "There can be only one."
        int titans = (numCreature(Creature.titan)); 
        int angels = (numCreature(Creature.angel)); 
        if (angels > 1)
        {
            angels = 1;
        }
        int archangels = (numCreature(Creature.archangel));
        if (archangels > 1)
        {
            archangels = 1;
        }

        int lordTypes = titans + angels + archangels;

        if (lordTypes == 1)
        {
            if (titans == 1)
            {
                revealCreatures(Creature.titan, 1);
            }
            else if (angels == 1)
            {
                revealCreatures(Creature.angel, 1);
            }
            else if (archangels == 1)
            {
                revealCreatures(Creature.archangel, 1);
            }
        }
        else
        {
            new PickLord(parentFrame, this);
        }
    }
}
