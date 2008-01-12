package net.sf.colossus.client;


import java.util.List;

import net.sf.colossus.game.PlayerState;


/** Knows a lot of things, but cannot actually take any actions.
 *  Everything returned by this interface must be immutable, or a copy.
 *  An attempt to reduce the God-class nature of Client.
 *  
 *  TODO get rid of all the Strings in this interface
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

    PlayerState getActivePlayer();

    String getPhaseName();

    PlayerState getBattleActivePlayer();

    String getBattlePhaseName();
}
