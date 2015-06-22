package net.sf.colossus.game;


import java.util.logging.Logger;

import net.sf.colossus.variant.MasterHex;


/**
 *  Holds the basic data for an engagement.
 *
 *  TODO: use also on server side.
 *
 *  TODO: unify with EngagementResults.Engagement
 */
public class Engagement
{
    private static final Logger LOGGER = Logger.getLogger(Engagement.class
        .getName());

    /**
     *  If engagement is ongoing, the masterBoard hex, attacker and defender
     */
    private final Legion attacker;
    private final Legion defender;
    private final MasterHex location;

    public Engagement(MasterHex hex, Legion attacker, Legion defender)
    {
        this.location = hex;
        this.attacker = attacker;
        this.defender = defender;
        LOGGER.info("A new engagement: " + location + " attacker " + attacker
            + " defender " + defender);
    }

    public MasterHex getLocation()
    {
        return location;
    }

    public String getLocationLabel()
    {
        return location.getLabel();
    }

    public Legion getDefendingLegion()
    {
        return defender;
    }

    public Legion getAttackingLegion()
    {
        return attacker;
    }

    @Override
    public String toString()
    {
        return location + " attacker " + attacker + " defender " + defender;
    }

}
