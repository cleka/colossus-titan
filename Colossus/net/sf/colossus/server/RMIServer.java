package net.sf.colossus.server;


import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

import net.sf.colossus.client.Proposal;


/**
 *  RMIServer is a RMI implementation of Server
 *  @version $Id$
 *  @author David Ripton
 */
public class RMIServer extends UnicastRemoteObject implements IRMIServer
{
    private IServer server;


    RMIServer(IServer server) throws RemoteException
    {
        super();
        this.server = server;
    }


    public void leaveCarryMode() throws RemoteException
    {
        server.leaveCarryMode();
    }

    public void doneWithBattleMoves() throws RemoteException
    {
        server.doneWithBattleMoves();
    }

    public void doneWithStrikes(String playerName) throws RemoteException
    {
        server.doneWithStrikes(playerName);
    }

    public void makeForcedStrikes(String playerName, boolean rangestrike)
        throws RemoteException
    {
        server.makeForcedStrikes(playerName, rangestrike);
    }

    public void acquireAngel(String markerId, String angelType)
        throws RemoteException
    {
        server.acquireAngel(markerId, angelType);
    }

    public void doSummon(String markerId, String donorId, String angel)
        throws RemoteException
    {
        server.doSummon(markerId, donorId, angel);
    }

    public void doRecruit(String markerId, String recruitName,
        String recruiterName) throws RemoteException
    {
        server.doRecruit(markerId, recruitName, recruiterName);
    }

    public void engage(String hexLabel) throws RemoteException
    {
        server.engage(hexLabel);
    }

    public void concede(String markerId) throws RemoteException
    {
        server.concede(markerId);
    }

    public void doNotConcede(String markerId) throws RemoteException
    {
        server.doNotConcede(markerId);
    }

    public void flee(String markerId) throws RemoteException
    {
        server.flee(markerId);
    }

    public void doNotFlee(String markerId) throws RemoteException
    {
        server.doNotFlee(markerId);
    }

    public void makeProposal(String playerName, Proposal proposal)
        throws RemoteException
    {
        server.makeProposal(playerName, proposal);
    }

    public void fight(String hexLabel) throws RemoteException
    {
        server.fight(hexLabel);
    }

    public void doBattleMove(int tag, String hexLabel) throws RemoteException
    {
        server.doBattleMove(tag, hexLabel);
    }

    public void strike(int tag, String hexLabel) throws RemoteException
    {
        server.strike(tag, hexLabel);
    }

    public void applyCarries(String hexLabel) throws RemoteException
    {
        server.applyCarries(hexLabel);
    }

    public void undoBattleMove(String hexLabel) throws RemoteException
    {
        server.undoBattleMove(hexLabel);
    }

    public void assignStrikePenalty(String playerName, String prompt)
        throws RemoteException
    {
        server.assignStrikePenalty(playerName, prompt);
    }

    public void mulligan(String playerName) throws RemoteException
    {
        server.mulligan(playerName);
    }

    public void undoSplit(String playerName, String splitoffId)
        throws RemoteException
    {
        server.undoSplit(playerName, splitoffId);
    }

    public void undoMove(String playerName, String markerId)
        throws RemoteException
    {
        server.undoMove(playerName, markerId);
    }

    public void undoRecruit(String playerName, String markerId)
        throws RemoteException
    {
        server.undoRecruit(playerName, markerId);
    }

    public void doneWithSplits(String playerName) throws RemoteException
    {
        server.doneWithSplits(playerName);
    }

    public void doneWithMoves(String playerName) throws RemoteException
    {
        server.doneWithMoves(playerName);
    }

    public void doneWithEngagements(String playerName) throws RemoteException
    {
        server.doneWithEngagements(playerName);
    }

    public void doneWithRecruits(String playerName) throws RemoteException
    {
        server.doneWithRecruits(playerName);
    }

    public void withdrawFromGame(String playerName) throws RemoteException
    {
        server.withdrawFromGame(playerName);
    }

    public void setDonor(String markerId) throws RemoteException
    {
        server.setDonor(markerId);
    }

    public void doSplit(String parentId, String childId, String results)
        throws RemoteException
    {
        server.doSplit(parentId, childId, results);
    }

    public void doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord) throws RemoteException
    {
        server.doMove(markerId, hexLabel, entrySide, teleport,
            teleportingLord);
    }

    public void assignColor(String playerName, String color)
        throws RemoteException
    {
        server.assignColor(playerName, color);
    }

    // XXX Disallow the following methods in network games
    public void newGame() throws RemoteException
    {
        server.newGame();
    }

    public void loadGame(String filename) throws RemoteException
    {
        server.loadGame(filename);
    }

    public void saveGame() throws RemoteException
    {
        server.saveGame();
    }

    public void saveGame(String filename) throws RemoteException
    {
        server.loadGame(filename);
    }


    public static void main(String [] args)
    {
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new RMISecurityManager());
        }
        String name = "//host/RMIServer";
        try
        {
            RMIServer server = new RMIServer(null);
            Naming.rebind(name, server);
            System.out.println("RMIServer bound");
        }
        catch (Exception e)
        {
            System.err.println("RMIServer main() exception: " +
                e.getMessage());
            e.printStackTrace();
        }
    }
}
