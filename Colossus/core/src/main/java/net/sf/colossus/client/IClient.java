package net.sf.colossus.client;


import java.util.List;
import java.util.Set;

import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 *  IClient is a remote interface for the server-accessible parts of Client.
 *
 *  @author David Ripton
 */
public interface IClient
{
    public void tellEngagement(MasterHex hex, Legion attacker, Legion defender);

    public void tellEngagementResults(Legion winner, String method,
        int points, int turns);

    public void tellMovementRoll(int roll, String reason);

    public void tellWhatsHappening(String message);

    public void syncOption(String optname, String value);

    public void updatePlayerInfo(List<String> infoStrings);

    public void updateChangedPlayerValues(String valuesString, String reason);

    public void setColor(PlayerColor color);

    public void updateCreatureCount(CreatureType type, int count, int deadCount);

    public void disposeClient();

    public void removeLegion(Legion legion);

    public void setLegionStatus(Legion legion, boolean moved,
        boolean teleported, EntrySide entrySide, CreatureType lastRecruit);

    public void addCreature(Legion legion, CreatureType type, String reason);

    public void removeCreature(Legion legion, CreatureType type, String reason);

    public void revealCreatures(Legion legion, final List<CreatureType> names,
        String reason);

    public void revealEngagedCreatures(Legion legion,
        final List<CreatureType> names, boolean isAttacker, String reason);

    public void removeDeadBattleChits();

    public void placeNewChit(String imageName, boolean inverted, int tag,
        BattleHex hex);

    public void initBoard();

    public void tellReplay(boolean val, int maxTurn);

    public void tellRedo(boolean val);

    public void confirmWhenCaughtUp();

    public void serverConfirmsConnection();

    public void relayedPeerRequest(String requestingClientName);

    public void peerRequestReceivedBy(String respondingPlayerName, int queueLen);

    public void peerRequestProcessedBy(String respondingPlayerName);

    public void setPlayerName(String newPlayerName);

    public void createSummonAngel(Legion legion);

    public void askAcquireAngel(Legion legion, List<CreatureType> recruits);

    public void askChooseStrikePenalty(List<String> choices);

    public void tellGameOver(String message, boolean disposeFollows, boolean suspended);

    public void tellPlayerElim(Player player, Player slayer);

    public void askConcede(Legion ally, Legion enemy);

    public void askFlee(Legion ally, Legion enemy);

    public void askNegotiate(Legion attacker, Legion defender);

    public void tellProposal(String proposalString);

    public void tellSlowResults(int targetTag, int slowValue);

    // TODO the last parameter could probably be a list of Creatures
    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List<String> rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft,
        Set<String> carryTargetDescriptions);

    public void initBattle(MasterHex masterHex, int battleTurnNumber,
        Player battleActivePlayer, BattlePhase battlePhase, Legion attacker,
        Legion defender);

    public void cleanupBattle();

    public void nextEngagement();

    public void doReinforce(Legion legion);

    public void didRecruit(Legion legion, CreatureType recruitName,
        CreatureType recruiterName, int numRecruiters);

    public void undidRecruit(Legion legion, CreatureType recruitName);

    public void setupTurnState(Player activePlayer, int turnNumber);

    public void setupSplit(Player activePlayer, int turnNumber);

    public void setupMove();

    public void setupFight();

    public void setupMuster();

    public void kickPhase();

    public void setupBattleSummon(Player battleActivePlayer,
        int battleTurnNumber);

    public void setupBattleRecruit(Player battleActivePlayer,
        int battleTurnNumber);

    public void setupBattleMove(Player battleActivePlayer, int battleTurnNumber);

    public void setupBattleFight(BattlePhase battlePhase,
        Player battleActivePlayer);

    // TODO the extra hex parameter is probably not needed anymore
    public void tellLegionLocation(Legion legion, MasterHex hex);

    public void tellBattleMove(int tag, BattleHex startingHex,
        BattleHex endingHex, boolean undo);

    public void didMove(Legion legion, MasterHex startingHex, MasterHex hex,
        EntrySide entrySide, boolean teleport, CreatureType teleportingLord,
        boolean splitLegionHasForcedMove);

    public void undidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove);

    public void didSummon(Legion receivingLegion, Legion donorLegion,
        CreatureType summon);

    public void undidSplit(Legion splitoff, Legion survivor, int turn);

    // TODO splitoffs is not actually used anymore, but it is still part
    // of the network protocol
    public void didSplit(MasterHex hex, Legion parent, Legion child,
        int childHeight, List<CreatureType> splitoffs, int turn);

    public void askPickColor(List<PlayerColor> colorsLeft);

    public void askPickFirstMarker();

    public void log(String message);

    public void nak(String reason, String errmsg);

    public void setBoardActive(boolean val);

    public void pingRequest(long requestTime);

    public void messageFromServer(String message);

    public void appendToConnectionLog(String s);

    public void tellSyncCompleted(int syncRequestNumber);

    public void requestExtraRollApproval(String requestorName, int requestId);

    public void askSuspendConfirmation(String requestorName, int timeout);

    public boolean canHandleChangedValuesOnlyStyle();

    public String getClientName();

}
