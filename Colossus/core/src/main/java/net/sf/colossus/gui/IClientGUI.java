package net.sf.colossus.gui;


import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.webclient.WebClient;


public interface IClientGUI
{

    public abstract void setStartedByWebClient(boolean byWebClient);

    public abstract void setWebClient(WebClient wc);

    public abstract void setClientInWebClientNull();

    public abstract MasterBoard getBoard();

    /*
     * If webclient is just hidden, bring it back;
     * if it had been used, ask whether to restore;
     * Otherwise just do nothing
     */
    public abstract void handleWebClientRestore();

    public abstract void showWebClient();

    public abstract void initBoard();

    public abstract void ensureEdtSetupClientGUI();

    public abstract void ensureEdtNewBattleBoard();

    public abstract void actOnInitBattle();

    public abstract void doNewBattleBoard();

    public abstract void updateStatusScreen();

    public abstract void menuCloseBoard();

    public abstract void menuQuitGame();

    /** Upon request with checkServerConnection, server sends a confirmation.
     *  This method here processes the confirmation.
     */
    public abstract void serverConfirmsConnection();

    /** Timeout reached. Cancel timer and show error message
     */
    public abstract void timeoutAbortsConnectionCheck();

    public abstract void menuNewGame();

    public abstract void menuLoadGame(String filename);

    public abstract void focusMap();

    public abstract void focusBoard();

    public abstract void highlightEngagements();

    public abstract int getViewMode();

    public abstract int getRecruitChitMode();

    public abstract void addPossibleRecruitChits(LegionClientSide legion,
        Set<MasterHex> hexes);

    public abstract void setupGUIOptionListeners();

    public abstract void eventViewerSetVisibleMaybe();

    public abstract void autoInspectorSetDubiousAsBlanks(boolean newValue);

    public abstract void engagementResultsMaybeShow();

    public abstract void actOnTellLegionLocation(Legion legion, MasterHex hex);

    public abstract void actOnDidSplit(int turn, Legion parent, Legion child,
        MasterHex hex);

    public abstract void actOnDidSplitPart2(MasterHex hex);

    public abstract void actOnDoneWithMoves();

    public abstract void actOnDoneWithSplits();

    public abstract void actOnDidRecruit(Legion legion, String recruitName,
        List<String> recruiters, String reason);

    public abstract void actOnRemoveCreature(Legion legion, String name,
        String reason);

    public abstract void actOnRemoveCreaturePart2(Legion legion);

    public abstract void actOnAddCreature(Legion legion, String name,
        String reason);

    public abstract void boardActOnUndidSplit(Legion survivor, int turn);

    public abstract void actOnUndidRecruitPart2(Legion legion,
        boolean wasReinforcement, int turnNumber);

    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    public abstract boolean chooseWhetherToTeleport();

    public abstract void actOnDidMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, boolean teleport, String teleportingLord,
        boolean splitLegionHasForcedMove);

    public abstract void actOnUndidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove,
        boolean didTeleport);

    public abstract void actOnNextEngagement();

    public abstract void alignLegionsMaybe(Legion legion);

    public abstract void actOnRemoveLegion(Legion legion);

    public abstract void actOnDoSummon();

    public abstract void updateEverything();

    public abstract void replayTurnChange(int nowTurn);

    public abstract void actOnTellReplay(int maxTurn);

    public abstract void clearUndoStack();

    public abstract Object popUndoStack();

    public abstract void pushUndoStack(Object object);

    public abstract void eventViewerCancelReinforcement(String recruitName,
        int turnNr);

    public abstract void eventViewerDefenderSetCreatureDead(String name,
        int height);

    public abstract void eventViewerAttackerSetCreatureDead(String name,
        int height);

    public abstract void eventViewerNewSplitEvent(int turn, Legion parent,
        Legion child);

    public abstract void eventViewerUndoEvent(Legion splitoff,
        Legion survivor, int turn);

    public abstract void setPreferencesWindowVisible(boolean val);

    /**
     * Displays the marker and its legion if possible.
     */
    public abstract void showMarker(Marker marker);

    /**
     * Displays the recruit tree of the hex if possible.
     */
    public abstract void showHexRecruitTree(GUIMasterHex hex);

    public abstract void didSummon(Legion summoner, Legion donor, String summon);

    public abstract void repaintBattleBoard();

    public abstract void repaintAllWindows();

    /**
     * TODO since we are doing Swing nowadays it would probably be much better to replace
     * all this rescaling code with just using {@link AffineTransform} on the right
     * {@link Graphics2D} instances.
     */
    public abstract void rescaleAllWindows();

    public abstract void disposeInspector();

    public abstract void updateCreatureCountDisplay();

    public abstract void disposePickCarryDialog();

    public abstract void showNegotiate(Legion attacker, Legion defender);

    public abstract void respawnNegotiate();

    public abstract void showConcede(Client client, Legion ally, Legion enemy);

    public abstract void showFlee(Client client, Legion ally, Legion enemy);

    public abstract void initShowEngagementResults();

    public abstract void tellEngagement(Legion attacker, Legion defender,
        int turnNumber);

    public abstract void actOnTellEngagementResults(Legion winner,
        String method, int points, int turns);

    public abstract void setMulliganOldRoll(int movementRoll);

    public abstract void tellWhatsHappening(String message);

    public abstract void tellMovementRoll(int roll);

    /* pass revealed info to EventViewer and
     * additionally remember the images list for later, the engagement report
     */
    public abstract void revealEngagedCreatures(Legion legion,
        final List<String> names, boolean isAttacker, String reason);

    public abstract void eventViewerRevealCreatures(Legion legion,
        final List<String> names, String reason);

    /**
     * Ensure that Player menu checkboxes reflect the correct state.
     *
     * TODO let the checkboxes have their own listeners instead. Or even
     * better: use a binding framework.
     */
    public abstract void syncCheckboxes();

    public abstract void doAcquireAngel(Legion legion, List<String> recruits);

    public abstract void setBoardActive(boolean val);

    public abstract SummonInfo doPickSummonAngel(Legion legion,
        SortedSet<Legion> possibleDonors);

    public abstract String doPickSplitLegion(Legion parent, String childMarker);

    public abstract void doPickCarries(Client client, int carryDamage,
        Set<String> carryTargetDescriptions);

    public abstract PickCarry getPickCarryDialog();

    public abstract PlayerColor doPickColor(String playerName,
        List<PlayerColor> colorsLeft);

    public abstract String doPickMarker(Set<String> markersAvailable);

    public abstract String doPickMarkerUntilGotOne(Set<String> markersAvailable);

    public abstract String doPickRecruit(Legion legion, String hexDescription);

    public abstract String doPickRecruiter(List<String> recruiters,
        String hexDescription, Legion legion);

    public abstract EntrySide doPickEntrySide(MasterHex hex,
        Set<EntrySide> entrySides);

    public abstract String doPickLord(List<String> lords);

    public abstract void doPickStrikePenalty(Client client,
        List<String> choices);

    /** Inform this player about the other player's proposal. */
    public abstract void tellProposal(String proposalString);

    public abstract void cleanupNegotiationDialogs();

    public abstract void actOnTurnOrPlayerChange(Client c, int turnNr, Player p);

    public abstract void actOnSetupSplit();

    public abstract void actOnSetupMuster();

    public abstract void actOnSetupMove();

    public abstract void actOnSetupFight();

    public abstract void actOnSetupBattleFight(BattlePhase battlePhase,
        int battleTurnNumber);

    public abstract void actOnSetupBattleMove();

    public abstract void actOnTellBattleMove(BattleHex startingHex,
        BattleHex endingHex);

    public abstract void actOnSetupBattleRecruit();

    public abstract void actOnSetupBattleSummon();

    public abstract void actOnPlaceNewChit(BattleHex hex);

    public abstract void actOnTellStrikeResults(boolean wasCarry,
        int strikeNumber, List<String> rolls, BattleChit striker,
        BattleChit target);

    public abstract void highlightCrittersWithTargets();

    public abstract void actOnApplyCarries(BattleHex hex);

    public abstract void actOnCleanupBattle();

    public abstract void undoRecruit(Legion legion);

    public abstract void informSplitRequiredFirstRound();

    public abstract void undoLastBattleMove();

    public abstract void undoAllBattleMoves();

    public abstract void undoAllMoves();

    public abstract void undoAllRecruits();

    public abstract void defaultCursor();

    public abstract void waitCursor();

    public abstract void doCleanupGUI();

    public abstract void actOnTellGameOver(String message,
        boolean disposeFollows);

    public abstract void showMessageDialog(String message);

    public abstract void showNonModalMessageDialog(String message);

    // called by WebClient
    public abstract void doConfirmAndQuit();

    public abstract void closePerhapsWithMessage();

    public abstract void showErrorMessage(String reason, String title);

    // At least the following two are from gui package classes,
    // not from Client directly.

    // for ChooseScreen
    public abstract void setChosenDevice(GraphicsDevice chosen);

    // for PreferencesWindow
    public abstract void setLookAndFeel(String text);

}