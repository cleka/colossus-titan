package net.sf.colossus.client;


import java.util.List;

import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.Engagement;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;


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
    Battle getBattleCS();

    Engagement getEngagement();

    int getTurnNumber();

    List<String> getLegionImageNames(Legion legion);

    List<Boolean> getLegionCreatureCertainties(Legion legion);

    // TODO: the sole function of this seems to be avoiding premature content
    // rendering in StatusScreen, which doesn't seem a good reason for this
    // method
    int getNumPlayers();

    Player getActivePlayer();

    Player getBattleActivePlayer();

    BattlePhase getBattlePhase();
}
