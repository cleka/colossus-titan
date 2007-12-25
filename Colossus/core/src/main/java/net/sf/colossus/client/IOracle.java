package net.sf.colossus.client;


import java.util.List;


/** Knows a lot of things, but cannot actually take any actions.
 *  Everything returned by this interface must be immutable, or a copy.
 *  An attempt to reduce the God-class nature of Client.
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

    List getLegionImageNames(String markerId);

    List getLegionCreatureCertainties(String markerId);

    int getNumPlayers();

    String getActivePlayerName();

    String getPhaseName();

    String getBattleActivePlayerName();

    String getBattlePhaseName();
}
