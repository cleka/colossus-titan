/**
 * Class Critter represents an individual Titan Character.
 * @version $Id$
 * @author David Ripton
 */

class Critter extends Creature
{
    private boolean visible;
    private Creature creature;

    
    Critter(Creature creature, boolean visible)
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
}
