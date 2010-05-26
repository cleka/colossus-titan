package net.sf.colossus.gui;


import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.variant.CreatureType;


/**
 * Contains info about one revealed creature for RevealEvent.
 *
 * @author Clemens Katzer
 */
public class RevealedCreature
{
    private static final Logger LOGGER = Logger
        .getLogger(RevealedCreature.class.getName());

    private final CreatureType creatureType;
    private String titanBaseName = null;
    private boolean dead = false;

    // possible reasons why this creature was revealed:
    private boolean didRecruit = false;
    private boolean wasRecruited = false;
    private boolean wasReinforced = false;
    private boolean didTeleport = false;
    // private boolean didTowerTeleport = false;
    // private boolean didTitanTeleport = false;
    private boolean wasSummoned = false;
    private boolean wasAcquired = false;

    public RevealedCreature(CreatureType type)
    {
        this.creatureType = type;
        if (type == null)
        {
            LOGGER.log(Level.SEVERE,
                "Tried to create RevealedCreature with null type");
            assert false : "Can not reveal creature without type";
            return;
        }
    }

    // EventViewer does this when necessary. Would perhaps be cleaner
    // if our own constructor does this checking, but to construct
    // the basename needs the client and the marker, which in 95%
    // of the cases are not needed here in the RevealedCreature.
    public void setTitanBaseName(String tbName)
    {
        titanBaseName = tbName;
    }

    public String getName()
    {
        return titanBaseName != null ? titanBaseName : creatureType.getName();
    }

    public String getPlainName()
    {
        return creatureType.getName();
    }

    public boolean matches(String name)
    {
        if (name.equals(getPlainName()))
        {
            return true;
        }
        else if (titanBaseName != null && name.equals(titanBaseName))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        String infoString = getName() + ": "
            + (didRecruit ? "did recruit; " : "")
            + (wasRecruited ? "was recruited; " : "")
            + (wasReinforced ? "was reinforced; " : "")
            + (didTeleport ? "teleported; " : "")
            + (wasSummoned ? "was summoned; " : "")
            + (wasAcquired ? "was acquired; " : "")
            + (dead ? "is dead; " : "");

        return infoString;
    }

    public Chit toChit(int scale)
    {
        String name = getName();
        if (name == null)
        {
            LOGGER.log(Level.SEVERE,
                "revealedCreature.toChit, creature name null!");
            return null;
        }

        // TODO can't simply use creature type because does getName() here does
        // special magic for Titans
        Chit creature = Chit.newCreatureChit(scale, name);
        if (isDead())
        {
            creature.setDead(true);
        }

        return creature;
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

    public void setWasReinforced(boolean value)
    {
        this.wasReinforced = value;
    }

    public boolean wasReinforced()
    {
        return wasReinforced;
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

    public void setWasAcquired(boolean value)
    {
        this.wasAcquired = value;
    }

    public boolean wasAcquired()
    {
        return wasAcquired;
    }

    public void setDead(boolean value)
    {
        this.dead = value;
    }

    public boolean isDead()
    {
        return dead;
    }

    // Reason why this creature was added to the legion
    public void setReason(String reason)
    {
        if (reason == null)
        {
            LOGGER.log(Level.SEVERE,
                "RevealedCreature.setReason: reason null!!");
            return;
        }
        if (reason.equals(Constants.reasonRecruited))
        {
            setWasRecruited(true);
        }
        else if (reason.equals(Constants.reasonReinforced))
        {
            setWasReinforced(true);
        }
        else if (reason.equals(Constants.reasonSummon))
        {
            setWasSummoned(true);
        }
        else if (reason.equals(Constants.reasonAcquire))
        {
            setWasAcquired(true);
        }
        else if (reason.equals("<Unknown>"))
        {
            // That's ok, probably just old server version does not
            // send this argument, so socketclientthread sets this dummy.
        }
        else
        {
            LOGGER.log(Level.SEVERE,
                "RevealedCreature.setReason: unknown reason " + reason + "!!");
        }
    }
}
