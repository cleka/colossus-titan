/**
 * Class Critter represents an individual Titan Character.
 * @version $Id$
 * @author David Ripton
 */

class Critter extends Creature
{
    private boolean visible;
    private Creature creature;
    private Legion legion;

    
    Critter(Creature creature, boolean visible, Legion legion)
    {
        super(creature.name, creature.power, creature.skill, 
            creature.rangeStrikes, creature.flies, creature.nativeBramble, 
            creature.nativeDrift, creature.nativeBog, 
            creature.nativeSandDune, creature.nativeSlope, creature.lord, 
            creature.demilord, creature.count);

        if (name != null) 
        {
            this.creature = Creature.getCreatureFromName(name);
        }
        else
        {
            this.creature = null;
        }

        this.visible = visible; 
        this.legion = legion;
        if (getName().equals("Titan"))
        {
            setPower(legion.getPlayer().getTitanPower());
        }
    }


    public boolean isVisible()
    {
        return visible;
    }


    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }


    public Creature getCreature()
    {
        return creature;
    }


    // All count-related functions must use the Creature archetype,
    // not this copy.

    public int getCount()
    {
        return creature.getCount();
    }


    public void setCount(int count)
    {
        creature.setCount(count);
    }


    public void takeOne()
    {
        creature.takeOne();
    }


    public void putOneBack()
    {
        creature.putOneBack();
    }


    // File.separator does not work right in jar files.  A hardcoded 
    // forward-slash does, and works in *x and Windows.  I have
    // no idea if it works on the Mac, etc.
    public String getImageName(boolean inverted)
    {
        String myName;
        if (getName().equals("Titan") && getPower() >= 6 && getPower() <= 20)
        {
            // Use Titan14.gif for a 14-power titan, etc.  Use the generic
            // Titan.gif (with X-4) for ridiculously big titans, to avoid 
            // the need for an infinite number of images.
            
            myName = getName() + getPower();
        }
        else
        {
            myName = name;
        }

        if (inverted)
        {
            return "images/i_" + myName + ".gif";
        }
        else
        {
            return "images/" + myName + ".gif";
        }
    }


    public String getImageName()
    {
        return getImageName(false);
    }



    public int getPower()
    {
        // Update Titan power if necessary.
        if (getName().equals("Titan"))
        {
            setPower(legion.getPlayer().getTitanPower());
        }

        return power;
    }


    public int getPointValue()
    {
        return getPower() * skill;
    }
}
