package net.sf.colossus.server;


import java.util.*;

import net.sf.colossus.client.IClient;
import net.sf.colossus.client.Proposal;   // XXX


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

    public void makeForcedStrikes(boolean rangestrike);

    public void acquireAngel(String markerId, String angelType);

    public void doSummon(String markerId, String donorId, String angel);

    public void doRecruit(String markerId, String recruitName,
        String recruiterName);

    public void engage(String hexLabel);

    public void concede(String markerId);

    public void doNotConcede(String markerId);

    public void flee(String markerId);

    public void doNotFlee(String markerId);

    public void makeProposal(String proposalString);

    public void fight(String hexLabel);

    public void doBattleMove(int tag, String hexLabel);

    public void strike(int tag, String hexLabel);

    public void applyCarries(String hexLabel);

    public void undoBattleMove(String hexLabel);

    public void assignStrikePenalty(String prompt);

    public void mulligan();

    public void undoSplit(String splitoffId);

    public void undoMove(String markerId);

    public void undoRecruit(String markerId);

    public void doneWithSplits();

    public void doneWithMoves();

    public void doneWithEngagements();

    public void doneWithRecruits();

    public void withdrawFromGame();

    public void setDonor(String markerId);

    public void doSplit(String parentId, String childId, String results);

    public void doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord);

    public void assignColor(String color);

    public void relayChatMessage(String target, String text);

    // XXX Disallow the following methods in network games
    public void newGame();

    public void loadGame(String filename);

    public void saveGame(String filename);
}
