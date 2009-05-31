package net.sf.colossus.client;

import java.util.logging.Logger;

import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;

public class BattleClientSide extends Battle
{
    private static final Logger LOGGER = Logger
        .getLogger(BattleClientSide.class.getName());

    private BattlePhase battlePhase;
    private int battleTurnNumber = -1;
    private Player battleActivePlayer;

    public BattleClientSide(Game game, Legion attacker, Legion defender,
        MasterBoardTerrain land)
    {
        super(game, attacker, defender, land);

        LOGGER.info("Battle client side instantiated for "
            + attacker.getMarkerId() + " attacking " + defender.getMarkerId()
            + " in land " + land.getDisplayName());
    }

    public void init(int battleTurnNumber, Player battleActivePlayer,
        BattlePhase battlePhase)
    {
        this.battleTurnNumber = battleTurnNumber;
        this.battleActivePlayer = battleActivePlayer;
        this.battlePhase = battlePhase;

        this.getDefendingLegion().setEntrySide(
            this.getAttackingLegion().getEntrySide().getOpposingSide());

    }

    @Override
    protected boolean isOccupied(BattleHex hex)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void setBattleTurnNumber(int battleTurnNumber)
    {
        this.battleTurnNumber = battleTurnNumber;
    }

    public int getBattleTurnNumber()
    {
        return battleTurnNumber;
    }

    public Player getBattleActivePlayer()
    {
        return battleActivePlayer;
    }

    public void cleanupBattle()
    {
        setBattlePhase(null);
        battleTurnNumber = -1;
        battleActivePlayer = ((GameClientSide)game).getNoonePlayer();
    }

    public Legion getBattleActiveLegion()
    {
        if (battleActivePlayer.equals(getDefendingLegion().getPlayer()))
        {
            return getDefendingLegion();
        }
        else
        {
            return getAttackingLegion();
        }
    }

    public BattlePhase getBattlePhase()
    {
        return battlePhase;
    }

    public void setBattlePhase(BattlePhase battlePhase)
    {
        this.battlePhase = battlePhase;
    }

    public boolean isBattlePhase(BattlePhase phase)
    {
        return this.battlePhase == phase;
    }

    public void setupPhase(BattlePhase phase, Player battleActivePlayer,
        int battleTurnNumber)
    {
        setBattlePhase(phase);
        setBattleActivePlayer(battleActivePlayer);
        setBattleTurnNumber(battleTurnNumber);
    }

    // public for IOracle
    public String getBattlePhaseName()
    {
        if (game.isPhase(Phase.FIGHT))
        {
            if (battlePhase != null)
            {
                return battlePhase.toString();
            }
        }
        return "";
    }

    public void setBattleActivePlayer(Player battleActivePlayer)
    {
        this.battleActivePlayer = battleActivePlayer;
    }

}
