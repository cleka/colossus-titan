package net.sf.colossus.client;


import net.sf.colossus.util.Log;


/*
 * Contains info about one revealed creature.
 */
public class RevealedCreature
{
    private String creatureName;
    
    // possible reasons why this creature was revealed:
    private boolean didRecruit = false;
    private boolean wasRecruited = false;
    private boolean didTeleport = false;
    // private boolean didTowerTeleport = false;
    // private boolean didTitanTeleport = false;
    private boolean wasSummoned = false;
    private boolean dead = false;
    
    public RevealedCreature(String name)
    {
        if (name == null)
        {
            Log.error("Tried to create RevealedCreature with null name");
            return;
        }
        this.creatureName = name;
    }

    public String getName()
    {
        return creatureName;
    }

    public String toString()
    {
        String infoString = creatureName + ": "
            + (didRecruit ? "did recruit; " : "")
            + (wasRecruited ? "was recruited; " : "")
            + (didTeleport ? "teleported; " : "")            
//            + (didTowerTeleport ? "tower teleported; " : "")
//            + (didTitanTeleport ? "titan teleported; " : "")
            + (wasSummoned ? "was summoned; " : "")
            + (dead ? "is dead; " : "");
         
        return infoString;
    }

    public String toDetails()
    {
        String infoString = creatureName
            + ": didRecruit " + didRecruit + ", wasRecruited " + wasRecruited
            + ", didTeleport" + didTeleport
//            + ", didTowerTP " + didTowerTeleport 
//            + ", didTitanTeleport" + didTitanTeleport
            + ", wasSummoned " + wasSummoned + "; dead " + dead;
         
        return infoString;
    }
    
    public void setDidRecruit(boolean value)
    {
        this.didRecruit = value;
    }
    
    public boolean didRecruit()
    {
        return didRecruit;
    }

    public void setWasRecruited(boolean value)
    {
        this.wasRecruited = value;
    }

    public boolean wasRecruited()
    {
        return wasRecruited;
    }

    public void setDidTeleport(boolean value)
    {
        this.didTeleport = value;
    }

    public boolean didTeleport()
    {
        return didTeleport;
    }

/*    
    public void setDidTowerTeleport(boolean value)
    {
        this.didTowerTeleport = value;
    }

    public boolean didTowerTeleport()
    {
        return didTowerTeleport;
    }

    public void setDidTitanTeleport(boolean value)
    {
        this.didTitanTeleport = value;
    }

    public boolean didTitanTeleport()
    {
        return didTitanTeleport;
    }
*/
    public void setWasSummoned(boolean value)
    {
        this.wasSummoned = value;
    }

    public boolean wasSummoned()
    {
        return wasSummoned;
    }
    
    public void setDead(boolean value)
    {
        this.dead = value;
    }
    
    public boolean isDead()
    {
        return dead;
    }

}
