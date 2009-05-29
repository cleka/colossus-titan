package net.sf.colossus.client;

import java.util.logging.Logger;

import net.sf.colossus.game.Battle;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;

public class BattleClientSide extends Battle
{
    private static final Logger LOGGER = Logger
        .getLogger(BattleClientSide.class.getName());

    public BattleClientSide(Game game, Legion attacker, Legion defender,
        MasterBoardTerrain land)
    {
        super(game, attacker, defender, land);

        LOGGER.info("Battle client side instantiated for "
            + attacker.getMarkerId() + " attacking " + defender.getMarkerId()
            + " in land " + land.getDisplayName());
    }


    @Override
    protected boolean isOccupied(BattleHex hex)
    {
        // TODO Auto-generated method stub
        return false;
    }

}
