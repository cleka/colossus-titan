package net.sf.colossus.server;


import java.util.*;
import java.net.*;
import javax.swing.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.client.Client;
import net.sf.colossus.client.Proposal;
import net.sf.colossus.parser.TerrainRecruitLoader;


/**
 *  IServer is an interface for the client-accessible parts of Server.
 *  @version $Id$
 *  @author David Ripton
 */
public interface IServer
{
    public void leaveCarryMode();

    public void doneWithBattleMoves();
    
    public void doneWithStrikes(String playerName);

    public void makeForcedStrikes(String playerName, boolean rangestrike);

    public void acquireAngel(String markerId, String angelType);

    public void doSummon(String markerId, String donorId, String angel);

    /** Handle mustering for legion. */ 
    public void doRecruit(String markerId, String recruitName,
        String recruiterName);

    public void engage(String hexLabel);

    public void concede(String markerId);

    public void doNotConcede(String markerId);

    public void flee(String markerId);

    public void doNotFlee(String markerId);

    /** playerName makes a proposal. */
    public void makeProposal(String playerName, Proposal proposal);

    public void fight(String hexLabel);

    public void doBattleMove(int tag, String hexLabel);

    public void strike(int tag, String hexLabel);

    public void applyCarries(String hexLabel);

    public void undoBattleMove(String hexLabel);

    public void assignStrikePenalty(String playerName, String prompt);

    public void mulligan(String playerName);

    public void undoSplit(String playerName, String splitoffId);

    public void undoMove(String playerName, String markerId);

    public void undoRecruit(String playerName, String markerId);

    public void doneWithSplits(String playerName);

    public void doneWithMoves(String playerName);

    public void doneWithEngagements(String playerName);

    public void doneWithRecruits(String playerName);

    public void withdrawFromGame(String playerName);

    public void setDonor(String markerId);

    public void doSplit(String parentId, String childId, String results);

    public void doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord);

    // XXX Disallow in network games
    public void newGame();

    // XXX Disallow in network games
    public void loadGame(String filename);

    // XXX Disallow in network games
    public void saveGame();

    // XXX Disallow in network games
    public void saveGame(String filename);

    public void assignColor(String playerName, String color);
}
