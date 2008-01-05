package net.sf.colossus.game;


import net.sf.colossus.variant.BattleLand;


/**
 * An ongoing battle.
 */
public class Battle
{
    private final Legion attacker;
    private final Legion defender;
    private final BattleLand land;

    public Battle(Legion attacker, Legion defender, BattleLand land)
    {
        this.attacker = attacker;
        this.defender = defender;
        this.land = land;
    }

    public Legion getAttackingLegion()
    {
        return attacker;
    }

    public Legion getDefendingLegion()
    {
        return defender;
    }

    public BattleLand getLand()
    {
        return land;
    }
}
