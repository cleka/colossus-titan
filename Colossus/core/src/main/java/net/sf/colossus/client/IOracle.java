package net.sf.colossus.client;


import java.util.List;

import net.sf.colossus.game.Player;


/** Knows a lot of things, but cannot actually take any actions.
 *  Everything returned by this interface must be immutable, or a copy.
 *  An attempt to reduce the God-class nature of Client.
 *  
 *  TODO this should be replaced with classes from the game package, mostly
 *       Game itself
 *  
 *  @version $Id$
 *  @author David Ripton
 */
public interface IOracle
{
    String getBattleSite();

    String getAttackerMarkerId();

    String getDefenderMarkerId();

    int getBattleTurnNumber();

    int getTurnNumber();

    List<String> getLegionImageNames(String markerId);

    List<Boolean> getLegionCreatureCertainties(String markerId);

    int getNumPlayers();

    Player getActivePlayer();

    String getPhaseName();

    Player getBattleActivePlayer();

    String getBattlePhaseName();
}
