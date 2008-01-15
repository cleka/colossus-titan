package net.sf.colossus.client;


import java.util.List;
import java.util.Set;

import net.sf.colossus.game.PlayerState;
import net.sf.colossus.server.Constants;


/**
 *  IClient is a remote interface for the client-accessible parts of Client.
 *  @version $Id$
 *  @author David Ripton
 */
public interface IClient
{
    public void tellEngagement(String hexLabel, String attackerId,
        String defenderId);

    public void tellEngagementResults(String winnerId, String method,
        int points, int turns);

    public void tellMovementRoll(int roll);

    public void setOption(String optname, String value);

    public void updatePlayerInfo(List<String> infoStrings);

    public void setColor(String color);

    public void updateCreatureCount(String creatureName, int count,
        int deadCount);

    public void dispose();

    public void removeLegion(String id);

    public void setLegionStatus(String markerId, boolean moved,
        boolean teleported, int entrySide, String lastRecruit);

    public void addCreature(String markerId, String name, String reason);

    public void removeCreature(String markerId, String name, String reason);

    public void revealCreatures(String markerId, final List<String> names,
        String reason);

    public void revealEngagedCreatures(String markerId,
        final List<String> names, boolean isAttacker, String reason);

    public void removeDeadBattleChits();

    public void placeNewChit(String imageName, boolean inverted, int tag,
        String hexLabel);

    public void initBoard();

    public void setPlayerName(String newPlayerName);

    public void createSummonAngel(String markerId);

    public void askAcquireAngel(String markerId, List<String> recruits);

    public void askChooseStrikePenalty(List<String> choices);

    public void tellGameOver(String message);

    public void tellPlayerElim(PlayerState player, PlayerState slayer);

    public void askConcede(String allyMarkerId, String enemyMarkerId);

    public void askFlee(String allyMarkerId, String enemyMarkerId);

    public void askNegotiate(String attackerId, String defenderId);

    public void tellProposal(String proposalString);

    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List<String> rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft,
        Set<String> carryTargetDescriptions);

    public void initBattle(String masterHexLabel, int battleTurnNumber,
        PlayerState battleActivePlayer, Constants.BattlePhase battlePhase,
        String attackerMarkerId, String defenderMarkerId);

    public void cleanupBattle();

    public void nextEngagement();

    public void doReinforce(String markerId);

    public void didRecruit(String markerId, String recruitName,
        String recruiterName, int numRecruiters);

    public void undidRecruit(String markerId, String recruitName);

    public void setupTurnState(PlayerState activePlayer, int turnNumber);

    public void setupSplit(PlayerState activePlayer, int turnNumber);

    public void setupMove();

    public void setupFight();

    public void setupMuster();

    public void setupBattleSummon(PlayerState battleActivePlayer,
        int battleTurnNumber);

    public void setupBattleRecruit(PlayerState battleActivePlayer,
        int battleTurnNumber);

    public void setupBattleMove(PlayerState battleActivePlayer,
        int battleTurnNumber);

    public void setupBattleFight(Constants.BattlePhase battlePhase,
        PlayerState battleActivePlayer);

    public void tellLegionLocation(String markerId, String hexLabel);

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo);

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel, String entrySide, boolean teleport,
        String teleportingLord, boolean splitLegionHasForcedMove);

    public void undidMove(String markerId, String formerHexLabel,
        String currentHexLabel, boolean splitLegionHasForcedMove);

    public void didSummon(String summonerId, String donorId, String summon);

    public void undidSplit(String splitoffId, String survivorId, int turn);

    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight, List<String> splitoffs, int turn);

    public void askPickColor(List<String> colorsLeft);

    public void askPickFirstMarker();

    public void log(String message);

    public void nak(String reason, String errmsg);

    public void setBoardActive(boolean val);
}
