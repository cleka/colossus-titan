package net.sf.colossus.game;


import net.sf.colossus.variant.BattleLand;


/**
 * An ongoing battle.
 */
public class Battle
{
    private final Game game;
    private final Legion attacker;
    private final Legion defender;
    private final BattleLand land;

    public Battle(Game game, Legion attacker, Legion defender, BattleLand land)
    {
        this.game = game;
        this.attacker = attacker;
        this.defender = defender;
        this.land = land;
    }

    public Game getGame()
    {
        return game;
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
