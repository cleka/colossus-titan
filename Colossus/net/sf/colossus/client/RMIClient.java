package net.sf.colossus.client;


import java.util.*;
import java.rmi.*;
import java.rmi.server.*;


/**
 *  RMIClient is a RMI implementation of Client.
 *  @version $Id$
 *  @author David Ripton
 */


public class RMIClient extends UnicastRemoteObject implements IRMIClient
{
    private IClient client;


    public RMIClient(IClient client) throws RemoteException
    {
        super();
        this.client = client;
    }


    public void tellMovementRoll(int roll) throws RemoteException
    {
        client.tellMovementRoll(roll);
    }

    public void setOption(String optname, String value) throws RemoteException
    {
        client.setOption(optname, value);
    }

    public void updatePlayerInfo(String [] infoStrings) throws RemoteException
    {
        client.updatePlayerInfo(infoStrings);
    }

    public void setColor(String color) throws RemoteException
    {
        client.setColor(color);
    }

    public void updateCreatureCount(String creatureName, int count)
        throws RemoteException
    {
        client.updateCreatureCount(creatureName, count);
    }

    public void dispose() throws RemoteException
    {
        client.dispose();
    }

    public void removeLegion(String id) throws RemoteException
    {
        client.removeLegion(id);
    }

    public void setLegionHeight(String markerId, int height)
        throws RemoteException
    {
        client.setLegionHeight(markerId, height);
    }

    public void setLegionContents(String markerId, java.util.List names)
        throws RemoteException
    {
        client.setLegionContents(markerId, names);
    }

    public void addCreature(String markerId, String name)
        throws RemoteException
    {
        client.addCreature(markerId, name);
    }

    public void removeCreature(String markerId, String name)
        throws RemoteException
    {
        client.removeCreature(markerId, name);
    }

    public void revealCreatures(String markerId, final java.util.List names)
        throws RemoteException
    {
        client.revealCreatures(markerId, names);
    }

    public void removeDeadBattleChits() throws RemoteException
    {
        client.removeDeadBattleChits();
    }

    public void placeNewChit(String imageName, boolean inverted, int tag,
        String hexLabel) throws RemoteException
    {
        client.placeNewChit(imageName, inverted, tag, hexLabel);
    }

    public void initBoard() throws RemoteException
    {
        client.initBoard();
    }

    public void setPlayerName(String playerName) throws RemoteException
    {
        client.setPlayerName(playerName);
    }

    public void createSummonAngel(String markerId, String longMarkerName)
        throws RemoteException
    {
        client.createSummonAngel(markerId, longMarkerName);
    }

    public void askAcquireAngel(String markerId, java.util.List recruits)
        throws RemoteException
    {
        client.askAcquireAngel(markerId, recruits);
    }

    public void askChooseStrikePenalty(java.util.List choices)
        throws RemoteException
    {
        client.askChooseStrikePenalty(choices);
    }

    public void showMessageDialog(String message) throws RemoteException
    {
        client.showMessageDialog(message);
    }

    public void tellGameOver(String message) throws RemoteException
    {
        client.tellGameOver(message);
    }

    public void askConcede(String longMarkerName, String hexDescription,
        String allyMarkerId, String enemyMarkerId) throws RemoteException
    {
        client.askConcede(longMarkerName, hexDescription, allyMarkerId,
            enemyMarkerId);
    }

    public void askFlee(String longMarkerName, String hexDescription,
        String allyMarkerId, String enemyMarkerId) throws RemoteException
    {
        client.askFlee(longMarkerName, hexDescription, allyMarkerId,
            enemyMarkerId);
    }

    public void askNegotiate(String attackerLongMarkerName,
        String defenderLongMarkerName, String attackerId, String defenderId,
        String hexLabel) throws RemoteException
    {
        client.askNegotiate(attackerLongMarkerName, defenderLongMarkerName,
            attackerId, defenderId, hexLabel);
    }

    public void tellProposal(Proposal proposal) throws RemoteException
    {
        client.tellProposal(proposal);
    }

    public void tellStrikeResults(String strikerDesc, int strikerTag,
        String targetDesc, int targetTag, int strikeNumber, int [] rolls,
        int damage, boolean killed, boolean wasCarry, int carryDamageLeft,
        Set carryTargetDescriptions) throws RemoteException
    {
        client.tellStrikeResults(strikerDesc, strikerTag, targetDesc,
            targetTag, strikeNumber, rolls, damage, killed, wasCarry,
            carryDamageLeft, carryTargetDescriptions);
    }

    public void initBattle(String masterHexLabel, int battleTurnNumber,
        String battleActivePlayerName, int battlePhase,
        String attackerMarkerId, String defenderMarkerId)
        throws RemoteException
    {
        client.initBattle(masterHexLabel, battleTurnNumber,
            battleActivePlayerName, battlePhase, attackerMarkerId,
            defenderMarkerId);
    }

    public void cleanupBattle() throws RemoteException
    {
        client.cleanupBattle();
    }

    public void highlightEngagements() throws RemoteException
    {
        client.highlightEngagements();
    }

    public void doReinforce(String markerId) throws RemoteException
    {
        client.doReinforce(markerId);
    }

    public void didRecruit(String markerId, String recruitName,
        String recruiterName, int numRecruiters) throws RemoteException
    {
        client.didRecruit(markerId, recruitName, recruiterName, numRecruiters);
    }

    public void undidRecruit(String markerId, String recruitName)
        throws RemoteException
    {
        client.undidRecruit(markerId, recruitName);
    }

    public void setupTurnState(String activePlayerName, int turnNumber)
        throws RemoteException
    {
        client.setupTurnState(activePlayerName, turnNumber);
    }

    public void setupSplit(Set markersAvailable, String activePlayerName,
        int turnNumber) throws RemoteException
    {
        client.setupSplit(markersAvailable, activePlayerName, turnNumber);
    }

    public void setupMove() throws RemoteException
    {
        client.setupMove();
    }

    public void setupFight() throws RemoteException
    {
        client.setupFight();
    }

    public void setupMuster() throws RemoteException
    {
        client.setupMuster();
    }

    public void setupBattleSummon(String battleActivePlayerName,
        int battleTurnNumber) throws RemoteException
    {
        client.setupBattleSummon(battleActivePlayerName, battleTurnNumber);
    }

    public void setupBattleRecruit(String battleActivePlayerName,
        int battleTurnNumber) throws RemoteException
    {
        client.setupBattleRecruit(battleActivePlayerName, battleTurnNumber);
    }

    public void setupBattleMove() throws RemoteException
    {
        client.setupBattleMove();
    }

    public void setupBattleFight(int battlePhase,
        String battleActivePlayerName) throws RemoteException
    {
        client.setupBattleFight(battlePhase, battleActivePlayerName);
    }

    public void tellLegionLocation(String markerId, String hexLabel)
        throws RemoteException
    {
        client.tellLegionLocation(markerId, hexLabel);
    }

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo) throws RemoteException
    {
        client.tellBattleMove(tag, startingHexLabel, endingHexLabel, undo);
    }

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel, boolean teleport) throws RemoteException
    {
        client.didMove(markerId, startingHexLabel, currentHexLabel, teleport);
    }

    public void undidMove(String markerId, String formerHexLabel,
        String currentHexLabel) throws RemoteException
    {
        client.undidMove(markerId, formerHexLabel, currentHexLabel);
    }

    public void undidSplit(String splitoffId) throws RemoteException
    {
        client.undidSplit(splitoffId);
    }

    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight) throws RemoteException
    {
        client.didSplit(hexLabel, parentId, childId, childHeight);
    }

    public void askPickColor(Set colorsLeft) throws RemoteException
    {
        client.askPickColor(colorsLeft);
    }


    public static void main(String [] args)
    {
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new RMISecurityManager());
        }
        String name = "//host/RMIClient";
        try
        {
            RMIClient client = new RMIClient(null);
            Naming.rebind(name, client);
            System.out.println("RMIClient bound");
        }
        catch (Exception e)
        {
            System.err.println("RMIClient main() exception: " +
                e.getMessage());
            e.printStackTrace();
        }
    }
}
