package net.sf.colossus.client;


import java.util.*;
import java.net.*;
import java.io.*;
import net.sf.colossus.server.IServer;
import net.sf.colossus.server.Constants;

import net.sf.colossus.util.Log;


/**
 *  Client-side socket handler 
 *  @version $Id$
 *  @author David Ripton
 */


final class SocketClient implements IServer
{
    private Client client;
    private Socket socket;
    private PrintWriter out;

    private final String sep = Constants.protocolTermSeparator;


    SocketClient(Client client, String host, int port)
    {
        this.client = client;

        try
        {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (Exception ex)
        {
            Log.error(ex.toString());
            return;
        }

        out.println(Constants.signOn + sep + client.getPlayerName() + sep +
            client.isRemote());

        new SocketClientThread(client, socket).start();
    }


    // Setup method
    private void signOn() 
    {
        out.println(Constants.signOn + sep + client.getPlayerName() + sep +
            client.isRemote());
    }


    // IServer methods, called from client and sent over the
    // socket to the server.

    public void leaveCarryMode() 
    {
        out.println(Constants.leaveCarryMode);
    }

    public void doneWithBattleMoves() 
    {
        out.println(Constants.doneWithBattleMoves);
    }

    public void doneWithStrikes() 
    {
        out.println(Constants.doneWithStrikes);
    }

    public void makeForcedStrikes(boolean rangestrike)
    {
        out.println(Constants.makeForcedStrikes + sep + rangestrike);
    }

    public void acquireAngel(String markerId, String angelType)
    {
        out.println(Constants.acquireAngel + sep + markerId + sep + angelType);
    }

    public void doSummon(String markerId, String donorId, String angel)
    {
        out.println(Constants.doSummon + sep + markerId + sep + donorId + 
            sep + angel);
    }

    public void doRecruit(String markerId, String recruitName,
        String recruiterName) 
    {
        out.println(Constants.doRecruit + sep + markerId + sep + recruitName +
            sep + recruiterName);
    }

    public void engage(String hexLabel) 
    {
        out.println(Constants.engage + sep + hexLabel);
    }

    public void concede(String markerId) 
    {
        out.println(Constants.concede + sep + markerId);
    }

    public void doNotConcede(String markerId) 
    {
        out.println(Constants.doNotConcede + sep + markerId);
    }

    public void flee(String markerId) 
    {
        out.println(Constants.flee + sep + markerId);
    }

    public void doNotFlee(String markerId) 
    {
        out.println(Constants.doNotFlee + sep + markerId);
    }

    public void makeProposal(String proposalString)
    {
        out.println(Constants.makeProposal + sep + proposalString);
    }

    public void fight(String hexLabel) 
    {
        out.println(Constants.fight + sep + hexLabel);
    }

    public void doBattleMove(int tag, String hexLabel) 
    {
        out.println(Constants.doBattleMove + sep + tag + sep + hexLabel);
    }

    public void strike(int tag, String hexLabel) 
    {
        out.println(Constants.strike + sep + tag + sep + hexLabel);
    }

    public void applyCarries(String hexLabel) 
    {
        out.println(Constants.applyCarries + sep + hexLabel);
    }

    public void undoBattleMove(String hexLabel) 
    {
        out.println(Constants.undoBattleMove + sep + hexLabel);
    }

    public void assignStrikePenalty(String prompt)
    {
        out.println(Constants.assignStrikePenalty + sep + prompt);
    }

    public void mulligan() 
    {
        out.println(Constants.mulligan);
    }

    public void undoSplit(String splitoffId)
    {
        out.println(Constants.undoSplit + sep + splitoffId);
    }

    public void undoMove(String markerId)
    {
        out.println(Constants.undoMove + sep + markerId);
    }

    public void undoRecruit(String markerId)
    {
        out.println(Constants.undoRecruit + sep + markerId);
    }

    public void doneWithSplits() 
    {
        out.println(Constants.doneWithSplits);
    }

    public void doneWithMoves() 
    {
        out.println(Constants.doneWithMoves);
    }

    public void doneWithEngagements() 
    {
        out.println(Constants.doneWithEngagements);
    }

    public void doneWithRecruits() 
    {
        out.println(Constants.doneWithRecruits);
    }

    public void withdrawFromGame() 
    {
        out.println(Constants.withdrawFromGame);
    }

    public void setDonor(String markerId) 
    {
        out.println(Constants.setDonor + sep + markerId);
    }

    public void doSplit(String parentId, String childId, String results)
    {
        out.println(Constants.doSplit + sep + parentId + sep + childId + sep +
            results);
    }

    public void doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord) 
    {
        out.println(Constants.doMove + sep + markerId + sep + hexLabel + sep +
            entrySide + sep + teleport + sep + teleportingLord);
    }

    public void assignColor(String color)
    {
        out.println(Constants.assignColor + sep + color);
    }

    // XXX Disallow the following methods in network games
    public void newGame() 
    {
        out.println(Constants.newGame);
    }

    public void loadGame(String filename) 
    {
        out.println(Constants.loadGame + sep + filename);
    }

    public void saveGame(String filename) 
    {
        out.println(Constants.saveGame + sep + filename);
    }
}
