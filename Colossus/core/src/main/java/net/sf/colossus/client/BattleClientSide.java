package net.sf.colossus.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;

public class BattleClientSide extends Battle
{
    private static final Logger LOGGER = Logger
        .getLogger(BattleClientSide.class.getName());

    private BattlePhase battlePhase;
    private int battleTurnNumber = -1;
    private Player battleActivePlayer;

    private final List<BattleUnit> battleUnits = new ArrayList<BattleUnit>();

    public BattleClientSide(Game game, Legion attacker, Legion defender,
        MasterHex location)
    {
        super(game, attacker, defender, location);

        LOGGER.info("Battle client side instantiated for "
            + attacker.getMarkerId() + " attacking " + defender.getMarkerId()
            + " in land " + location.getTerrain().getDisplayName());
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
        for (BattleCritter battleUnit : getBattleUnits())
        {
            if (battleUnit.getCurrentHex().equals(hex))
            {
                return true;
            }
        }
        return false;
    }

    // Helper method
    public GameClientSide getGameClientSide()
    {
        return (GameClientSide)game;
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
        battleUnits.clear();

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

    public void setupBattleFight(BattlePhase battlePhase,
        Player battleActivePlayer)
    {
        setBattlePhase(battlePhase);
        setBattleActivePlayer(battleActivePlayer);
        if (isBattlePhase(BattlePhase.FIGHT))
        {
            markOffboardCreaturesDead();
        }
    }

    public BattleUnit createBattleUnit(String imageName, boolean inverted,
        int tag, BattleHex hex, CreatureType type, Legion legion)
    {
        BattleUnit battleUnit = new BattleUnit(imageName, inverted, tag, hex,
            type, legion);
        battleUnits.add(battleUnit);

        return battleUnit;
    }

    public boolean anyOffboardCreatures()
    {
        for (BattleCritter battleUnit : getActiveBattleUnits())
        {
            if (battleUnit.getCurrentHex().getLabel().startsWith("X"))
            {
                return true;
            }
        }
        return false;
    }

    public List<BattleUnit> getActiveBattleUnits()
    {
        return CollectionHelper.selectAsList(battleUnits,
            new Predicate<BattleUnit>()
            {
                public boolean matches(BattleUnit battleUnit)
                {
                    return getBattleActivePlayer().equals(
                        getGameClientSide()
                            .getPlayerByTag(battleUnit.getTag()));
                }
            });
    }

    public List<BattleUnit> getInactiveBattleUnits()
    {
        return CollectionHelper.selectAsList(battleUnits,
            new Predicate<BattleUnit>()
            {
                public boolean matches(BattleUnit battleUnit)
                {
                    return !getBattleActivePlayer().equals(
                        getGameClientSide()
                            .getPlayerByTag(battleUnit.getTag()));
                }
            });
    }

    public List<BattleUnit> getBattleUnits()
    {
        return Collections.unmodifiableList(battleUnits);
    }

    public List<BattleUnit> getBattleUnits(final BattleHex hex)
    {
        return CollectionHelper.selectAsList(battleUnits,
            new Predicate<BattleUnit>()
            {
                public boolean matches(BattleUnit battleUnit)
                {
                    return hex.equals(battleUnit.getCurrentHex());
                }
            });
    }

    public BattleUnit getBattleUnit(BattleHex hex)
    {
        List<BattleUnit> lBattleUnits = getBattleUnits(hex);
        if (lBattleUnits.isEmpty())
        {
            return null;
        }
        return lBattleUnits.get(0);
    }

    /** Get the BattleUnit with this tag. */
    BattleUnit getBattleUnit(int tag)
    {
        for (BattleUnit battleUnit : battleUnits)
        {
            if (battleUnit.getTag() == tag)
            {
                return battleUnit;
            }
        }
        return null;
    }

    public void resetAllBattleMoves()
    {
        for (BattleCritter battleUnit : battleUnits)
        {
            battleUnit.setMoved(false);
            battleUnit.setStruck(false);
        }
    }

    public void markOffboardCreaturesDead()
    {
        for (BattleUnit battleUnit : getActiveBattleUnits())
        {
            if (battleUnit.getCurrentHex().getLabel().startsWith("X"))
            {
                battleUnit.setDead(true);
            }
        }
    }

    public void removeDeadBattleChits()
    {
        Iterator<BattleUnit> it = battleUnits.iterator();
        while (it.hasNext())
        {
            BattleUnit battleUnit = it.next();
            if (battleUnit.isDead())
            {
                it.remove();
            }
        }
    }

}
