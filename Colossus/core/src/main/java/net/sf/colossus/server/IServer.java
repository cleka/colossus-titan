package net.sf.colossus.server;


import java.util.List;

import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.actions.Recruitment;
import net.sf.colossus.game.actions.Summoning;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 *  IServer is an interface for the client-accessible parts of Server.
 *
 *  @author David Ripton
 */
public interface IServer
{
    public static final int CLIENT_VERSION_UNDERSTANDS_PING = 2;
    public static final int CLIENT_VERSION_CAN_RECONNECT = 3;
    public static final int CLIENT_VERSION_CAN_HANDLE_NAK = 4;
    public static final int CLIENT_VERSION_VARIANT_XML_OK = 5;
    public static final int CLIENT_VERSION_INACTIVITY_TIMEOUT = 6;
    public static final int CLIENT_VERSION_REQUEST_ROLL = 7;
    public static final int CLIENT_VERSION_CAN_SUSPEND = 8;
    public static final int CLIENT_VERSION_NEW_PLAYER_INFO = 9;
    public static final int CLIENT_VERSION_MORE_DEBUG_INFO = 10;

    // New in version 2: replies to pingRequest
    // New in version 3: ability to reconnect (simple case only, so far)
    // New in version 4: Client can handle the NAK for an illegal battle move
    // New in version 5: Client always fetches xml files from server
    // New in version 6: AI kicks in after too long inactivity
    // New in version 7: Client can ask user to approve/deny extra roll request
    // New in version 8: Client can ask user to approve/deny suspend request
    // New in version 9: Server sends only changed information
    public static final int CLIENT_VERSION = CLIENT_VERSION_MORE_DEBUG_INFO;

    // Clients that do not send version yet at all, are treated as version -1.
    // For those, even show on server side an error dialog and refuse them to
    // connect. Thus, minimum "meaningful version" at the moment is zero.
    public static final int MINIMUM_CLIENT_VERSION = 0;

    public void leaveCarryMode();

    public void doneWithBattleMoves();

    public void doneWithStrikes();

    public void acquireAngel(Legion legion, CreatureType angelType);

    /**
     * Handles a summon event
     *
     * @param event The summon event or null if summoning is not wanted.
     */
    public void doSummon(Summoning event);

    // TODO extend or subclass event to include recruiter
    public void doRecruit(Recruitment event);

    public void engage(MasterHex hex);

    public void concede(Legion legion);

    public void doNotConcede(Legion legion);

    public void flee(Legion legion);

    public void doNotFlee(Legion legion);

    public void makeProposal(String proposalString);

    public void fight(MasterHex hex);

    public void doBattleMove(int tag, BattleHex hex);

    public void strike(int tag, BattleHex hex);

    public void applyCarries(BattleHex hex);

    public void undoBattleMove(BattleHex hex);

    public void assignStrikePenalty(String prompt);

    public void mulligan();

    public void requestExtraRoll();

    public void extraRollResponse(boolean approved, int requestId);

    public void suspendResponse(boolean approved);

    public void undoSplit(Legion splitoff);

    public void undoMove(Legion legion);

    public void undoRecruit(Legion legion);

    public void doneWithSplits();

    public void doneWithMoves();

    public void doneWithEngagements();

    public void doneWithRecruits();

    public void withdrawFromGame();

    public void sendDisconnect();

    public void stopGame();

    /**
     * Executes a split of certain creatures from a legion.
     *
     * @param parent The legion to split the creatures out of.
     * @param childMarker A marker for the new legion.
     * @param creaturesToSplit The creatures to split out.
     */
    public void doSplit(Legion parent, String childMarker,
        List<CreatureType> creaturesToSplit);

    public void doMove(Legion legion, MasterHex hex, EntrySide entrySide,
        boolean teleport, CreatureType teleportingLord);

    public void assignColor(PlayerColor color);

    public void assignFirstMarker(String markerId);

    // XXX Disallow the following methods in network games
    public void newGame();

    public void loadGame(String filename);

    public void saveGame(String filename);

    public void requestToSuspendGame(boolean save);

    public void checkServerConnection();

    public void checkAllConnections(String requestingClientName);

    public void peerRequestReceived(String requestingClientName, int queueLen);

    public void peerRequestProcessed(String requestingClientName);

    public void clientConfirmedCatchup();

    public void joinGame(String playerName);

    public void logMsgToServer(String severity, String message);

    public void cheatModeDestroyLegion(Legion legion);

    public void watchGame();
}
