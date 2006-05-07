package net.sf.colossus.client;

import net.sf.colossus.server.Creature;


// the AI wants to save some useful information along with 
//   each creature. i thought about extending the Creature class
//   by a aiData field, but we can have multiple AIs in a game.
//   so the data must be stored in the AI instance itself.
//   there is a 1:1 map from each Creature instance to AiCreatureInfo.

/**
 * TODO: this class breaks a few rules of good design: overriding hashCode() without
 * overriding equals(), extensive use of package-private access, odd declaration of
 * final methods and funny naming (what does the "1" behind "killValue" mean?). 
 */
class AiCreatureInfo
{
    /**
     * @param killValue1 - the non-terrainified killValue.
     */
    AiCreatureInfo(final Creature creature)
    {
        this.creature = creature;
        this.killValue1 = creature.getKillValue();
    }
    
    /** only internal book keeping for now. 
     * wanna make it readable? go ahead. */
    private final Creature creature;
    
    /** is the same if creature is the same */
    public int hashCode()
    { 
        return creature.hashCode();
    }
    
    /** killvalue without terrain */
    private int killValue1; 
    final int getKillValue1()
    { 
        return killValue1;
    }
    final void setKillValue1(final int v)
    {
        this.killValue1 = v;
    }         
}
