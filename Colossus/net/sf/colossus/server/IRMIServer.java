package net.sf.colossus.server;


import java.util.*;
import java.rmi.*;

import net.sf.colossus.client.IRMIClient;
import net.sf.colossus.client.Proposal;


/**
 *  IRMIServer is a remote interface for the client-accessible parts of Server.
 *  @version $Id$
 *  @author David Ripton
 */
public interface IRMIServer extends Remote
{
    public void addRemoteClient(IRMIClient client, String playerName) 
        throws RemoteException;

    public void leaveCarryMode() throws RemoteException;

    public void doneWithBattleMoves() throws RemoteException;

    public void doneWithStrikes(String playerName) throws RemoteException;

    public void makeForcedStrikes(String playerName, boolean rangestrike)
        throws RemoteException;

    public void acquireAngel(String markerId, String angelType)
        throws RemoteException;

    public void doSummon(String markerId, String donorId, String angel)
        throws RemoteException;

    public void doRecruit(String markerId, String recruitName,
        String recruiterName) throws RemoteException;

    public void engage(String hexLabel) throws RemoteException;

    public void concede(String markerId) throws RemoteException;

    public void doNotConcede(String markerId) throws RemoteException;

    public void flee(String markerId) throws RemoteException;

    public void doNotFlee(String markerId) throws RemoteException;

    public void makeProposal(String playerName, Proposal proposal)
        throws RemoteException;

    public void fight(String hexLabel) throws RemoteException;

    public void doBattleMove(int tag, String hexLabel) throws RemoteException;

    public void strike(int tag, String hexLabel) throws RemoteException;

    public void applyCarries(String hexLabel) throws RemoteException;

    public void undoBattleMove(String hexLabel) throws RemoteException;

    public void assignStrikePenalty(String playerName, String prompt)
        throws RemoteException;

    public void mulligan(String playerName) throws RemoteException;

    public void undoSplit(String playerName, String splitoffId)
        throws RemoteException;

    public void undoMove(String playerName, String markerId)
        throws RemoteException;

    public void undoRecruit(String playerName, String markerId)
        throws RemoteException;

    public void doneWithSplits(String playerName) throws RemoteException;

    public void doneWithMoves(String playerName) throws RemoteException;

    public void doneWithEngagements(String playerName) throws RemoteException;

    public void doneWithRecruits(String playerName) throws RemoteException;

    public void withdrawFromGame(String playerName) throws RemoteException;

    public void setDonor(String markerId) throws RemoteException;

    public void doSplit(String parentId, String childId, String results)
        throws RemoteException;

    public void doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord) throws RemoteException;

    public void assignColor(String playerName, String color)
        throws RemoteException;

    // XXX Disallow the following methods in network games
    public void newGame() throws RemoteException;

    public void loadGame(String filename) throws RemoteException;

    public void saveGame() throws RemoteException;

    public void saveGame(String filename) throws RemoteException;
}
