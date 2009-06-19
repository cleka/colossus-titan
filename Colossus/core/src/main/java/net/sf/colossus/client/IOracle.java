package net.sf.colossus.client;


import java.util.List;

import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.MasterHex;


/**
 * Knows a lot of things, but cannot actually take any actions.
 *
 * Everything returned by this interface must be immutable, or a copy.
 * An attempt to reduce the God-class nature of Client.
 *
 * TODO this should be replaced with classes from the game package, mostly
 *      Game itself
 *
 * @author David Ripton
 */
public interface IOracle
{
    boolean isBattleOngoing();

    MasterHex getBattleSite();

    Legion getAttacker();

    Legion getDefender();

    int getBattleTurnNumber();

    int getTurnNumber();

    List<String> getLegionImageNames(Legion legion);

    List<Boolean> getLegionCreatureCertainties(Legion legion);

    int getNumPlayers();

    Player getActivePlayer();

    Player getBattleActivePlayer();

    BattlePhase getBattlePhase();
}
