package net.sf.colossus.client;


import java.util.*;


/**
 *  IClient is a remote interface for the client-accessible parts of Client.
 *  @version $Id$
 *  @author David Ripton
 */
public interface IClient
{
    // Not in network protocol
    public String getPlayerName();

    public void tellMovementRoll(int roll);

    public void setOption(String optname, String value);

    public void updatePlayerInfo(List infoStrings);

    public void setColor(String color);

    public void updateCreatureCount(String creatureName, int count, 
        int deadCount);

    public void dispose();

    public void removeLegion(String id);

    public void setLegionHeight(String markerId, int height);

    public void setLegionContents(String markerId, List names);

    public void addCreature(String markerId, String name);

    public void removeCreature(String markerId, String name);

    public void revealCreatures(String markerId, final List names);

    public void removeDeadBattleChits();

    public void placeNewChit(String imageName, boolean inverted, int tag,
        String hexLabel);

    public void initBoard();

    public void setPlayerName(String playerName);

    public void createSummonAngel(String markerId);

    public void askAcquireAngel(String markerId, List recruits);

    public void askChooseStrikePenalty(List choices);

    public void showMessageDialog(String message);

    public void tellGameOver(String message);

    public void askConcede(String allyMarkerId, String enemyMarkerId);

    public void askFlee(String allyMarkerId, String enemyMarkerId);

    public void askNegotiate(String attackerId, String defenderId);

    public void tellProposal(String proposalString);

    public void tellStrikeResults(int strikerTag, int targetTag, 
        int strikeNumber, List rolls, int damage, boolean killed, 
        boolean wasCarry, int carryDamageLeft, Set carryTargetDescriptions);

    public void initBattle(String masterHexLabel, int battleTurnNumber,
        String battleActivePlayerName, int battlePhase,
        String attackerMarkerId, String defenderMarkerId);

    public void cleanupBattle();

    public void highlightEngagements();

    public void nextEngagement();

    public void doReinforce(String markerId);

    public void didRecruit(String markerId, String recruitName,
        String recruiterName, int numRecruiters);

    public void undidRecruit(String markerId, String recruitName);

    public void setupTurnState(String activePlayerName, int turnNumber);

    public void setupSplit(Set markersAvailable, String activePlayerName,
        int turnNumber);

    public void setupMove();

    public void setupFight();

    public void setupMuster();

    public void setupBattleSummon(String battleActivePlayerName,
        int battleTurnNumber);

    public void setupBattleRecruit(String battleActivePlayerName,
        int battleTurnNumber);

    public void setupBattleMove(String battleActivePlayerName,
        int battleTurnNumber);

    public void setupBattleFight(int battlePhase,
        String battleActivePlayerName);

    public void tellLegionLocation(String markerId, String hexLabel);

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo);

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel, String entrySide, boolean teleport);

    public void undidMove(String markerId, String formerHexLabel,
        String currentHexLabel);

    public void undidSplit(String splitoffId);

    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight);

    public void askPickColor(java.util.List colorsLeft);

    public void log(String message);

    public void showChatMessage(String from, String text);
}
