package net.sf.colossus.game;

import java.util.logging.Logger;

import net.sf.colossus.variant.MasterHex;


/**
 *  Holds the basic data for an engagement.
 *
 *  TODO: use also on server side.
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
    private final MasterHex battleSite;

    public Engagement(MasterHex hex, Legion attacker, Legion defender)
    {
        this.battleSite = hex;
        this.attacker = attacker;
        this.defender = defender;
        LOGGER.info("A new engagement: hex " + hex + " attacker " + attacker
            + " defender " + defender);
    }

    public MasterHex getBattleSite()
    {
        return battleSite;
    }

    public Legion getDefender()
    {
        return defender;
    }

    public Legion getAttacker()
    {
        return attacker;
    }

}
