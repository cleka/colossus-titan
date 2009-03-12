package net.sf.colossus.server;


import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.MasterHex;


/**
 *  IServer is an interface for the client-accessible parts of Server.
 *  @version $Id$
 *  @author David Ripton
 */
public interface IServer
{
    public void leaveCarryMode();

    public void doneWithBattleMoves();

    public void doneWithStrikes();

    public void acquireAngel(Legion legion, String angelType);

    public void doSummon(Legion receivingLegion, Legion donorLegion,
        String angel);

    public void doRecruit(Legion legion, String recruitName,
        String recruiterName);

    public void engage(MasterHex hex);

    public void concede(Legion legion);

    public void doNotConcede(Legion legion);

    public void flee(Legion legion);

    public void doNotFlee(Legion legion);

    public void makeProposal(String proposalString);

    public void fight(MasterHex hex);

    public void doBattleMove(int tag, String hexLabel);

    public void strike(int tag, String hexLabel);

    public void applyCarries(String hexLabel);

    public void undoBattleMove(String hexLabel);

    public void assignStrikePenalty(String prompt);

    public void mulligan();

    public void undoSplit(Legion splitoff);

    public void undoMove(Legion legion);

    public void undoRecruit(Legion legion);

    public void doneWithSplits();

    public void doneWithMoves();

    public void doneWithEngagements();

    public void doneWithRecruits();

    public void withdrawFromGame();

    public void disconnect();

    public void stopGame();

    public void doSplit(Legion parent, String childMarker, String results);

    public void doMove(Legion legion, MasterHex hex, String entrySide,
        boolean teleport, String teleportingLord);

    public void assignColor(Constants.PlayerColor color);

    public void assignFirstMarker(String markerId);

    // XXX Disallow the following methods in network games
    public void newGame();

    public void loadGame(String filename);

    public void saveGame(String filename);

    public void checkServerConnection();

    public void clientConfirmedCatchup();
}
