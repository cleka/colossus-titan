package net.sf.colossus.client;


import java.util.*;
import java.rmi.*;


/**
 *  IRMIClient is a remote interface for the client-accessible parts of Client.
 *  @version $Id$
 *  @author David Ripton
 */
public interface IRMIClient extends Remote
{
    public void tellMovementRoll(int roll) throws RemoteException;

    public void setOption(String optname, String value) throws RemoteException;

    public void updatePlayerInfo(String [] infoStrings) throws RemoteException;

    public void setColor(String color) throws RemoteException;

    public void updateCreatureCount(String creatureName, int count)
        throws RemoteException;

    public void dispose() throws RemoteException;

    public void removeLegion(String id) throws RemoteException;

    public void setLegionHeight(String markerId, int height)
        throws RemoteException;

    public void setLegionContents(String markerId, java.util.List names)
        throws RemoteException;

    public void addCreature(String markerId, String name)
        throws RemoteException;

    public void removeCreature(String markerId, String name)
        throws RemoteException;

    /** Reveal creatures in this legion, some of which already may be known. */
    public void revealCreatures(String markerId, final java.util.List names)
        throws RemoteException;

    public void removeDeadBattleChits() throws RemoteException;

    public void placeNewChit(String imageName, boolean inverted, int tag,
        String hexLabel) throws RemoteException;

    public void initBoard() throws RemoteException;

    public void setPlayerName(String playerName) throws RemoteException;

    public void createSummonAngel(String markerId, String longMarkerName)
        throws RemoteException;

    public void askAcquireAngel(String markerId, java.util.List recruits)
        throws RemoteException;

    public void askChooseStrikePenalty(java.util.List choices)
        throws RemoteException;

    public void showMessageDialog(String message) throws RemoteException;

    public void tellGameOver(String message) throws RemoteException;

    public void askConcede(String longMarkerName, String hexDescription,
        String allyMarkerId, String enemyMarkerId) throws RemoteException;

    public void askFlee(String longMarkerName, String hexDescription,
        String allyMarkerId, String enemyMarkerId) throws RemoteException;

    public void askNegotiate(String attackerLongMarkerName,
        String defenderLongMarkerName, String attackerId, String defenderId,
        String hexLabel) throws RemoteException;

    public void tellProposal(Proposal proposal) throws RemoteException;

    public void tellStrikeResults(String strikerDesc, int strikerTag,
        String targetDesc, int targetTag, int strikeNumber, int [] rolls,
        int damage, boolean killed, boolean wasCarry, int carryDamageLeft,
        Set carryTargetDescriptions) throws RemoteException;

    public void initBattle(String masterHexLabel, int battleTurnNumber,
        String battleActivePlayerName, int battlePhase,
        String attackerMarkerId, String defenderMarkerId)
        throws RemoteException;

    public void cleanupBattle() throws RemoteException;

    public void highlightEngagements() throws RemoteException;

    public void doReinforce(String markerId) throws RemoteException;

    public void didRecruit(String markerId, String recruitName,
        String recruiterName, int numRecruiters) throws RemoteException;

    public void undidRecruit(String markerId, String recruitName)
        throws RemoteException;

    public void setupTurnState(String activePlayerName, int turnNumber)
        throws RemoteException;

    public void setupSplit(Set markersAvailable, String activePlayerName,
        int turnNumber) throws RemoteException;

    public void setupMove() throws RemoteException;

    public void setupFight() throws RemoteException;

    public void setupMuster() throws RemoteException;

    public void setupBattleSummon(String battleActivePlayerName,
        int battleTurnNumber) throws RemoteException;

    public void setupBattleRecruit(String battleActivePlayerName,
        int battleTurnNumber) throws RemoteException;

    public void setupBattleMove() throws RemoteException;

    public void setupBattleFight(int battlePhase,
        String battleActivePlayerName) throws RemoteException;

    public void tellLegionLocation(String markerId, String hexLabel)
        throws RemoteException;

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo) throws RemoteException;

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel, boolean teleport) throws RemoteException;

    public void undidMove(String markerId, String formerHexLabel,
        String currentHexLabel) throws RemoteException;

    public void undidSplit(String splitoffId) throws RemoteException;

    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight) throws RemoteException;

    public void askPickColor(Set colorsLeft) throws RemoteException;

    public void log(String message) throws RemoteException;
}
