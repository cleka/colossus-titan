import java.awt.*;
import java.util.*;

/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 */

public class Legion
{
    private Marker marker;
    private String markerId;    // Bk03, Rd12, etc.
    private Legion splitFrom;
    private ArrayList critters = new ArrayList();
    private MasterHex currentHex;
    private MasterHex startingHex;
    private boolean moved;
    private boolean recruited;
    private boolean summoned;
    private Player player;
    private int battleTally;


    public Legion(int scale, String markerId, Legion splitFrom,
        Container container, MasterHex hex, Creature creature0, 
        Creature creature1, Creature creature2, Creature creature3, 
        Creature creature4, Creature creature5, Creature creature6, 
        Creature creature7, Player player)
    {
        this.markerId = markerId;
        this.splitFrom = splitFrom;
        this.marker = new Marker(scale, getImageName(), container, this);
        this.currentHex = hex;
        this.startingHex = hex;
        this.player = player;

        if (creature0 != null)
        {
            critters.add(new Critter(creature0, false, this));
        }
        if (creature1 != null)
        {
            critters.add(new Critter(creature1, false, this));
        }
        if (creature2 != null)
        {
            critters.add(new Critter(creature2, false, this));
        }
        if (creature3 != null)
        {
            critters.add(new Critter(creature3, false, this));
        }
        if (creature4 != null)
        {
            critters.add(new Critter(creature4, false, this));
        }
        if (creature5 != null)
        {
            critters.add(new Critter(creature5, false, this));
        }
        if (creature6 != null)
        {
            critters.add(new Critter(creature6, false, this));
        }
        if (creature7 != null)
        {
            critters.add(new Critter(creature7, false, this));
        }

        // Initial legion contents are public; contents of legions created
        // by splits are private.
        if (getHeight() == 8)
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
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            pointValue += critter.getPointValue();
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

            while (getHeight() < 7 && tmpScore / 100 > (score - points) / 100)
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


    public String toString()
    {
        return markerId;
    }


    public String getName()
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
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isLord())
            {
                return false;
            }
        }
        return true;
    }


    public int numCreature(Creature creature)
    {
        int count = 0;
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getName().equals(creature.getName()))
            {
                count++;
            }
        }
        return count;
    }


    public int numLords()
    {
        int count = 0;
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.isLord())
            {
                count++;
            }
        }
        return count;
    }


    public int getHeight()
    {
        return critters.size();
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
        boolean teleported = hex.getTeleported();
        Game.logEvent("Legion " + getMarkerId() + " in " +
            currentHex.getDescription() +
            (teleported ? " teleports " : " moves ") +
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
        if (currentHex.getTeleported())
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
        recruited = false;
        summoned = false;
    }


    public boolean hasRecruited()
    {
        return recruited;
    }


    // hasMoved() is a separate check, so that this function can be used in
    // battle as well as during the muster phase.
    public boolean canRecruit()
    {
        if (recruited || getHeight() > 6 || getPlayer().isDead() ||
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


    public void undoRecruit()
    {
        if (hasRecruited())
        {
            ListIterator lit = critters.listIterator(critters.size());
            Critter critter = (Critter)lit.previous();

            critter.putOneBack();
            removeCreature(critter, false, true);

            recruited = false;

            Game.logEvent("Legion " + getMarkerId() + " undoes its recruit");
        }
    }


    // Return true if this legion can summon an angel or archangel.
    public boolean canSummonAngel()
    {
        if (getHeight() >= 7 || summoned || !player.canSummonAngel())
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


    public boolean hasSummoned()
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


    /** Add a creature to this legion.  If takeFromStack is true,
        then do this only if such a creature remains in the stacks,
        and decrement the number of this creature type remaining. */
    public void addCreature(Creature creature, boolean takeFromStack)
    {
        if (takeFromStack)
        {
            if (creature.getCount() > 0)
            {
                creature.takeOne();
            }
            else
            {
                return;
            }
        }
        
        // Newly added critters are visible.
        critters.add(new Critter(creature, true, this));
    }

    /** Remove the creature in position i in the legion.  Return the
        removed creature. Put immortal creatures back on the stack
        if returnImmortalToStack is true. */
    public Creature removeCreature(int i, boolean returnImmortalToStack,
        boolean disbandIfEmpty)
    {
        Critter critter = (Critter)critters.remove(i);

        // If the creature is a lord or demi-lord, put it back in the stacks.
        if (returnImmortalToStack && critter.isImmortal())
        {
            critter.putOneBack();
        }

        // If there are no critters left, disband the legion.
        if (disbandIfEmpty && getHeight() == 0)
        {
            removeLegion();
        }

        return critter;
    }


    /** Remove the first creature matching the passed creature's type 
        from the legion.  Return the removed creature. */
    public Creature removeCreature(Creature creature, boolean
        returnImmortalToStack, boolean disbandIfEmpty)
    {
        // indexOf wants the same object, not just the same type.
        // So use getCritter() to get the correct object.
        Critter critter = getCritter(creature);
        if (critter == null)
        {
            return null;
        }
        else
        {
            int i = critters.indexOf(critter);
            return removeCreature(i, returnImmortalToStack, disbandIfEmpty);
        }
    }


    public Creature getCreature(int i)
    {
        Critter critter = getCritter(i);
        return critter.getCreature();
    }


    public Critter getCritter(int i)
    {
        return (Critter)critters.get(i);
    }


    /** Gets the first critter in this legion with the same creature
        type as the passed creature. */
    public Critter getCritter(Creature creature)
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getName().equals(creature.getName()))
            {
                return critter;
            }
        }
        return null;
    }


    /** Recombine this legion into another legion. Only remove this
        legion if remove is true.  If it's false, the caller is
        responsible for removing the legion, which can avoid
        concurrent access problems. */
    public void recombine(Legion legion, boolean remove)
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();

            legion.addCreature(critter, false);
            
            // Keep removeLegion from returning lords to stacks.
            if (critter.isLord())
            {
                critter.takeOne();
            }
        }
        if (remove)
        {
            player.removeLegion(this);
        }
        else
        {
            player.prepareToRemoveLegion(this);
        }

        Game.logEvent("Legion " + getMarkerId() +
            " recombined into legion " + legion.getMarkerId());
    }


    public void hideAllCreatures()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setVisible(false);
        }
    }


    public void revealAllCreatures()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setVisible(true);
        }
    }


    public void healAllCreatures()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.heal();
        }
    }


    public void revealCreature(int index)
    {
        Critter critter = (Critter)critters.get(index);
        critter.setVisible(true);
    }


    public void revealCreatures(Creature creature, int numberToReveal)
    {
        int numberAlreadyRevealed = 0;
        
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCreature().getName().equals(
                creature.getName()) && critter.isVisible())
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

        it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCreature().getName().equals(
                creature.getName()) && !critter.isVisible())
            {
                critter.setVisible(true);
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
