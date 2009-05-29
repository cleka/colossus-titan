package net.sf.colossus.client;


import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.gui.Marker;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.webclient.WebClient;


public interface IClientGUI
{

    public abstract void setStartedByWebClient(boolean byWebClient);

    public abstract void setWebClient(WebClient wc);

    public abstract void setClientInWebClientNull();

    /*
     * If webclient is just hidden, bring it back;
     * if it had been used, ask whether to restore;
     * Otherwise just do nothing
     */
    public abstract void handleWebClientRestore();

    public abstract void showWebClient();

    public abstract void initBoard();

    public abstract void actOnInitBattle();

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

    public abstract void highlightEngagements();

    public abstract int getViewMode();

    public abstract int getRecruitChitMode();

    public abstract void addPossibleRecruitChits(LegionClientSide legion,
        Set<MasterHex> hexes);

    public abstract void eventViewerSetVisibleMaybe();

    public abstract void autoInspectorSetDubiousAsBlanks(boolean newValue);

    public abstract void engagementResultsMaybeShow();

    public abstract void actOnTellLegionLocation(Legion legion, MasterHex hex);

    public abstract void actOnDidSplit(int turn, Legion parent, Legion child,
        MasterHex hex);

    public abstract void actOnDidSplitPart2(MasterHex hex);

    public abstract void actOnDoneWithMoves();

    public abstract void actOnDoneWithSplits();

    public abstract void actOnDidRecruit(Legion legion,
        CreatureType recruitName, List<CreatureType> recruiters, String reason);

    public abstract void actOnRemoveCreature(Legion legion, CreatureType type,
        String reason);

    public abstract void actOnRemoveCreaturePart2(Legion legion);

    public abstract void actOnAddCreature(Legion legion, CreatureType type,
        String reason);

    public abstract void boardActOnUndidSplit(Legion survivor, int turn);

    public abstract void actOnUndidRecruitPart2(Legion legion,
        boolean wasReinforcement, int turnNumber);

    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    public abstract boolean chooseWhetherToTeleport();

    public abstract void actOnDidMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, boolean teleport, CreatureType teleportingLord,
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

    public abstract void eventViewerCancelReinforcement(CreatureType recruit,
        int turnNr);

    public abstract void eventViewerDefenderSetCreatureDead(
        CreatureType creature,
        int height);

    public abstract void eventViewerAttackerSetCreatureDead(
        CreatureType creature,
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

    public abstract void didSummon(Legion summoner, Legion donor,
        CreatureType summon);

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
        final List<CreatureType> creatures, boolean isAttacker, String reason);

    public abstract void eventViewerRevealCreatures(Legion legion,
        final List<CreatureType> creatures, String reason);

    public abstract void doAcquireAngel(Legion legion,
        List<CreatureType> recruits);

    public abstract void setBoardActive(boolean val);

    public abstract SummonInfo doPickSummonAngel(Legion legion,
        SortedSet<Legion> possibleDonors);

    public abstract List<CreatureType> doPickSplitLegion(Legion parent,
        String childMarker);

    public abstract void doPickCarries(Client client, int carryDamage,
        Set<String> carryTargetDescriptions);

    public abstract boolean isPickCarryOngoing();

    public abstract PlayerColor doPickColor(String playerName,
        List<PlayerColor> colorsLeft);

    public abstract String doPickMarker(Set<String> markersAvailable);

    public abstract String doPickMarkerUntilGotOne(Set<String> markersAvailable);

    public abstract String doPickRecruit(Legion legion, String hexDescription);

    public abstract String doPickRecruiter(List<String> recruiters,
        String hexDescription, Legion legion);

    public abstract EntrySide doPickEntrySide(MasterHex hex,
        Set<EntrySide> entrySides);

    public abstract CreatureType doPickLord(List<CreatureType> lords);

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

    public abstract void actOnPlaceNewChit(String imageName,
        BattleUnit battleUnit, BattleHex hex);

    public abstract void resetStrikeNumbers();

    public abstract void actOnTellStrikeResults(boolean wasCarry,
        int strikeNumber, List<String> rolls,BattleCritter striker, BattleCritter target);

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

    public abstract void showMessageDialogAndWait(String message);

    // called by WebClient
    public abstract void doConfirmAndQuit();

    public abstract void closePerhapsWithMessage();

    // At least the following two are from gui package classes,
    // not from Client directly.

    // for ChooseScreen
    public abstract void setChosenDevice(GraphicsDevice chosen);

    // for PreferencesWindow
    public abstract void setLookAndFeel(String text);

    public abstract void removeBattleChit(BattleUnit battleUnit);
}