package net.sf.colossus.client;


import java.util.List;
import java.util.Set;

import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.server.Constants;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 *  IClient is a remote interface for the client-accessible parts of Client.
 *  @version $Id$
 *  @author David Ripton
 */
public interface IClient
{
    public void tellEngagement(MasterHex hex, Legion attacker, Legion defender);

    public void tellEngagementResults(Legion winner, String method,
        int points, int turns);

    public void tellMovementRoll(int roll);

    public void setOption(String optname, String value);

    public void updatePlayerInfo(List<String> infoStrings);

    public void setColor(String color);

    public void updateCreatureCount(CreatureType type, int count, int deadCount);

    public void dispose();

    public void removeLegion(Legion legion);

    public void setLegionStatus(Legion legion, boolean moved,
        boolean teleported, int entrySide, String lastRecruit);

    public void addCreature(Legion legion, String name, String reason);

    public void removeCreature(Legion legion, String name, String reason);

    public void revealCreatures(Legion legion, final List<String> names,
        String reason);

    public void revealEngagedCreatures(Legion legion,
        final List<String> names, boolean isAttacker, String reason);

    public void removeDeadBattleChits();

    public void placeNewChit(String imageName, boolean inverted, int tag,
        String hexLabel);

    public void initBoard();

    public void tellReplay(boolean val, int maxTurn);
    
    public void confirmWhenCaughtUp();

    public void serverConfirmsConnection();

    public void setPlayerName(String newPlayerName);

    public void createSummonAngel(Legion legion);

    public void askAcquireAngel(Legion legion, List<String> recruits);

    public void askChooseStrikePenalty(List<String> choices);

    public void tellGameOver(String message);

    public void tellPlayerElim(Player player, Player slayer);

    public void askConcede(Legion ally, Legion enemy);

    public void askFlee(Legion ally, Legion enemy);

    public void askNegotiate(Legion attacker, Legion defender);

    public void tellProposal(String proposalString);

    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List<String> rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft,
        Set<String> carryTargetDescriptions);

    public void initBattle(MasterHex masterHex, int battleTurnNumber,
        Player battleActivePlayer, Constants.BattlePhase battlePhase,
        Legion attacker, Legion defender);

    public void cleanupBattle();

    public void nextEngagement();

    public void doReinforce(Legion legion);

    public void didRecruit(Legion legion, String recruitName,
        String recruiterName, int numRecruiters);

    public void undidRecruit(Legion legion, String recruitName);

    public void setupTurnState(Player activePlayer, int turnNumber);

    public void setupSplit(Player activePlayer, int turnNumber);

    public void setupMove();

    public void setupFight();

    public void setupMuster();

    public void setupBattleSummon(Player battleActivePlayer,
        int battleTurnNumber);

    public void setupBattleRecruit(Player battleActivePlayer,
        int battleTurnNumber);

    public void setupBattleMove(Player battleActivePlayer, int battleTurnNumber);

    public void setupBattleFight(Constants.BattlePhase battlePhase,
        Player battleActivePlayer);

    // TODO the extra hex parameter is probably not needed anymore
    public void tellLegionLocation(Legion legion, MasterHex hex);

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo);

    public void didMove(Legion legion, MasterHex startingHex, MasterHex hex,
        String entrySide, boolean teleport, String teleportingLord,
        boolean splitLegionHasForcedMove);

    public void undidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove);

    public void didSummon(Legion receivingLegion, Legion donorLegion,
        String summon);

    public void undidSplit(Legion splitoff, Legion survivor, int turn);

    public void didSplit(MasterHex hex, Legion parent, Legion child,
        int childHeight, List<String> splitoffs, int turn);

    public void askPickColor(List<String> colorsLeft);

    public void askPickFirstMarker();

    public void log(String message);

    public void nak(String reason, String errmsg);

    public void setBoardActive(boolean val);
}
