package net.sf.colossus.client;


import java.awt.Color;
import java.awt.GraphicsDevice;
import java.util.List;
import java.util.Set;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.webclient.WebClient;


/**
 * A replacement for ClientGUI class which does nothing,
 * so that client can simply call it same way as the real
 * GUI but just nothing happens.
 * I.e. we get rid of all the "if (board != null)" stuff.
 *
 */
public class NullClientGUI implements IClientGUI
{

    @SuppressWarnings("unused")
    public NullClientGUI(Client client, IOptions options,
        WhatNextManager whatNextMgr)
    {
        // Nothing to do...
    }

    public void actOnAddCreature(Legion legion, CreatureType creature,
        String reason)
    {
        // TODO Auto-generated method stub

    }

    public void actOnApplyCarries(BattleHex hex)
    {
        // TODO Auto-generated method stub

    }

    public void actOnCleanupBattle()
    {
        // TODO Auto-generated method stub

    }

    public void actOnDidMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, boolean teleport, CreatureType teleportingLord,
        boolean splitLegionHasForcedMove)
    {
        // TODO Auto-generated method stub

    }

    public void actOnMoveNak()
    {
        // TODO Auto-generated method stub

    }

    public void actOnDidRecruit(Legion legion, CreatureType recruit,
        List<CreatureType> recruiters, String reason)
    {
        // TODO Auto-generated method stub

    }

    public void actOnDidSplit(int turn, Legion parent, Legion child,
        MasterHex hex)
    {
        // TODO Auto-generated method stub

    }

    public void actOnDoSummon()
    {
        // TODO Auto-generated method stub

    }

    public void actOnDoneWithMoves()
    {
        // TODO Auto-generated method stub

    }

    public void actOnDoneWithSplits()
    {
        // TODO Auto-generated method stub

    }

    public void actOnInitBattle()
    {
        // TODO Auto-generated method stub

    }

    public void actOnNoMoreEngagements()
    {
        // TODO Auto-generated method stub

    }

    public void actOnPlaceNewChit(String imageName, BattleUnit battleUnit,
        BattleHex hex)
    {
        // TODO Auto-generated method stub

    }

    public void resetStrikeNumbers()
    {
        // TODO Auto-generated method stub
    }

    public void actOnRemoveCreature(Legion legion, CreatureType type,
        String reason)
    {
        // TODO Auto-generated method stub

    }

    public void actOnRemoveCreaturePart2(Legion legion)
    {
        // TODO Auto-generated method stub

    }

    public void actOnRemoveLegion(Legion legion)
    {
        // TODO Auto-generated method stub

    }

    public void actOnSetupBattleFight()
    {
        // TODO Auto-generated method stub

    }

    public void actOnSetupBattleMove()
    {
        // TODO Auto-generated method stub

    }

    public void actOnSetupBattleRecruit()
    {
        // TODO Auto-generated method stub

    }

    public void actOnSetupBattleSummon()
    {
        // TODO Auto-generated method stub

    }

    public void actOnSetupFight()
    {
        // TODO Auto-generated method stub

    }

    public void actOnSetupMove()
    {
        // TODO Auto-generated method stub

    }

    public void actOnSetupMuster()
    {
        // TODO Auto-generated method stub

    }

    public void actOnTurnOrPlayerChange(Client client, int turnNr, Player p)
    {
        // TODO Auto-generated method stub

    }

    public void actOnGameStarting()
    {
        // TODO Auto-generated method stub

    }

    public void actOnSetupSplit()
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellBattleMove(BattleHex startingHex,
        BattleHex endingHex, boolean rememberForUndo)
    {
        // TODO Auto-generated method stub

    }

    public void actOnPendingBattleMoveOver()
    {
        // TODO Auto-generated method stub

    }

    public void actOnDoneWithBattleMoves()
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellEngagementResults(Legion winner, String method,
        int points, int turns)
    {
        // TODO Auto-generated method stub

    }

    public void actOnEngagementCompleted()
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellGameOver(String message, boolean disposeFollows, boolean suspended)
    {
        // TODO Auto-generated method stub

    }

    public void actOnGameStartingFailed()
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellLegionLocation(Legion legion, MasterHex hex)
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellReplay(int maxTurn)
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellRedoChange()
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellStrikeResults(boolean wasCarry, int strikeNumber,
        List<String> rolls, BattleCritter striker, BattleCritter target)
    {
        // TODO Auto-generated method stub

    }

    public void actOnUndidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove,
        boolean didTeleport)
    {
        // TODO Auto-generated method stub

    }

    public void actOnUndidRecruitPart(Legion legion, boolean wasReinforcement,
        int turnNumber)
    {
        // TODO Auto-generated method stub

    }

    public void alignLegionsMaybe(Legion legion)
    {
        // TODO Auto-generated method stub

    }

    public void autoInspectorSetDubiousAsBlanks(boolean newValue)
    {
        // TODO Auto-generated method stub

    }

    public void actOnUndidSplit(Legion survivor, int turn)
    {
        // TODO Auto-generated method stub

    }

    public boolean chooseWhetherToTeleport()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void cleanupNegotiationDialogs()
    {
        // TODO Auto-generated method stub

    }

    public void showConnectionClosedMessage()
    {
        // TODO Auto-generated method stub

    }

    public void defaultCursor()
    {
        // TODO Auto-generated method stub

    }

    public void didSummon(Legion summoner, Legion donor, CreatureType summon)
    {
        // TODO Auto-generated method stub

    }

    public void disposeInspector()
    {
        // TODO Auto-generated method stub

    }

    public void disposePickCarryDialog()
    {
        // TODO Auto-generated method stub

    }

    public void doAcquireAngel(Legion legion, List<CreatureType> recruits)
    {
        // TODO Auto-generated method stub

    }

    public void doCleanupGUI()
    {
        // TODO Auto-generated method stub

    }

    public void doConfirmAndQuit()
    {
        // TODO Auto-generated method stub

    }

    public void doPickCarries(Client client, int carryDamage,
        Set<String> carryTargetDescriptions)
    {
        // TODO Auto-generated method stub

    }

    public void doPickColor(String playerName, List<PlayerColor> colorsLeft)
    {
        // TODO Auto-generated method stub

    }

    public EntrySide doPickEntrySide(MasterHex hex, Set<EntrySide> entrySides)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public CreatureType doPickLord(List<CreatureType> lords)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void doPickSplitMarker(Legion parent, Set<String> markersAvailable)
    {
        // TODO Auto-generated method stub

    }

    public void doPickInitialMarker(Set<String> markersAvailable)
    {
        // TODO Auto-generated method stub

    }

    public CreatureType doPickRecruit(Legion legion, String hexDescription)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String doPickRecruiter(List<String> recruiters,
        String hexDescription, Legion legion)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<CreatureType> doPickSplitLegion(Legion parent,
        String childMarker)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void doPickStrikePenalty(Client client, List<String> choices)
    {
        // TODO Auto-generated method stub

    }

    public void doPickSummonAngel(Legion legion, List<Legion> possibleDonors)
    {
        // TODO Auto-generated method stub

    }

    public void engagementResultsMaybeShow()
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerCancelReinforcement(CreatureType recruit, int turnNr)
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerSetCreatureDead(BattleUnit battleUnit)
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerNewSplitEvent(int turn, Legion parent, Legion child)
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerRevealCreatures(Legion legion,
        List<CreatureType> creatures, String reason)
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerSetVisibleMaybe()
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerUndoEvent(Legion splitoff, Legion survivor, int turn)
    {
        // TODO Auto-generated method stub

    }

    public boolean isPickCarryOngoing()
    {
        return false;
    }

    public void handlePickCarry()
    {
        // TODO Auto-generated method stub
    }

    public int getRecruitChitMode()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getLegionMoveConfirmationMode()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getNextSplitClickMode()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getEffectiveViewMode()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public void handleWebClientRestore()
    {
        // TODO Auto-generated method stub

    }

    public void highlightCrittersWithTargets()
    {
        // TODO Auto-generated method stub

    }

    public void indicateStrikesDone(boolean auto)
    {
        // TODO Auto-generated method stub

    }

    public void revertDoneIndicator()
    {
        // TODO Auto-generated method stub

    }

    public void highlightEngagements()
    {
        // TODO Auto-generated method stub

    }

    public void informSplitRequiredFirstRound()
    {
        // TODO Auto-generated method stub

    }

    public void initBoard()
    {
        // TODO Auto-generated method stub

    }

    public boolean hasBoard()
    {
        return false;
    }

    public void initShowEngagementResults()
    {
        // TODO Auto-generated method stub

    }

    public void menuCloseBoard()
    {
        // TODO Auto-generated method stub

    }

    public void menuLoadGame(String filename)
    {
        // TODO Auto-generated method stub

    }

    public void menuNewGame()
    {
        // TODO Auto-generated method stub

    }

    public void menuQuitGame()
    {
        // TODO Auto-generated method stub

    }

    public void repaintAllWindows()
    {
        // TODO Auto-generated method stub

    }

    public void repaintBattleBoard()
    {
        // TODO Auto-generated method stub

    }

    public void replayTurnChange(int nowTurn)
    {
        // TODO Auto-generated method stub

    }

    public void rescaleAllWindows()
    {
        // TODO Auto-generated method stub

    }

    public void respawnNegotiate()
    {
        // TODO Auto-generated method stub

    }

    public void revealEngagedCreatures(Legion legion,
        List<CreatureType> creatures, boolean isAttacker, String reason)
    {
        // TODO Auto-generated method stub

    }

    public void serverConfirmsConnection()
    {
        // TODO Auto-generated method stub

    }

    public void appendToConnectionLog(String s)
    {
        // TODO Auto-generated method stub

    }

    public void actOnReconnectCompleted()
    {
        // TODO Auto-generated method stub

    }

    public void setBoardActive(boolean val)
    {
        // TODO Auto-generated method stub

    }

    public void setChosenDevice(GraphicsDevice chosen)
    {
        // TODO Auto-generated method stub

    }

    public void setClientInWebClientNull()
    {
        // TODO Auto-generated method stub

    }

    public void setLookAndFeel(String text)
    {
        // TODO Auto-generated method stub

    }

    public void setPreferencesWindowVisible(boolean val)
    {
        // TODO Auto-generated method stub

    }

    public void setStartedByWebClient(boolean byWebClient)
    {
        // TODO Auto-generated method stub

    }

    public void setWebClient(WebClient wc, int inactivityWarningInterval, String gameId, String username, String password)
    {
        // TODO Auto-generated method stub

    }

    public void clearWebClient()
    {
        // TODO Auto-generated method stub
    }

    public void setWhatToDoNextForClose()
    {
        // TODO Auto-generated method stub

    }

    public void showConcede(Client client, Legion ally, Legion enemy)
    {
        // TODO Auto-generated method stub

    }

    public void showFlee(Client client, Legion ally, Legion enemy)
    {
        // TODO Auto-generated method stub

    }

    public void showMessageDialogAndWait(String message)
    {
        // TODO Auto-generated method stub

    }

    public void showNegotiate(Legion attacker, Legion defender)
    {
        // TODO Auto-generated method stub

    }

    public void showWebClient()
    {
        // TODO Auto-generated method stub

    }

    public void tellEngagement(Legion attacker, Legion defender, int turnNumber)
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellMovementRoll(int roll, String reason)
    {
        // TODO Auto-generated method stub

    }

    public void tellProposal(String proposalString)
    {
        // TODO Auto-generated method stub

    }

    public void tellWhatsHappening(String message)
    {
        // TODO Auto-generated method stub

    }

    public void timeoutAbortsConnectionCheck()
    {
        // TODO Auto-generated method stub

    }

    public void undoAllBattleMoves()
    {
        // TODO Auto-generated method stub

    }

    public void undoAllMoves()
    {
        // TODO Auto-generated method stub

    }

    public void undoAllRecruits()
    {
        // TODO Auto-generated method stub

    }

    public void undoLastBattleMove()
    {
        // TODO Auto-generated method stub

    }

    public void undoRecruit(Legion legion)
    {
        // TODO Auto-generated method stub

    }

    public void updateCreatureCountDisplay()
    {
        // TODO Auto-generated method stub

    }

    public void updateEverything()
    {
        // TODO Auto-generated method stub

    }

    public void updateStatusScreen()
    {
        // TODO Auto-generated method stub

    }

    public void waitCursor()
    {
        // TODO Auto-generated method stub

    }

    public void removeBattleChit(BattleUnit battleUnit)
    {
        // TODO Auto-generated method stub

    }

    public void setPreferencesCheckBoxValue(String name, boolean value)
    {
        // TODO Auto-generated method stub

    }

    public void setPreferencesRadioButtonValue(String name, boolean value)
    {
        // TODO Auto-generated method stub

    }

    public void setMovePending(Legion mover, MasterHex currentHex,
        MasterHex targetHex)
    {
        // TODO Auto-generated method stub

    }

    public void setMoveCompleted(Legion mover, MasterHex current,
        MasterHex target)
    {
        // TODO Auto-generated method stub

    }

    public void actOnHitsSet(BattleUnit targetUnit)
    {
        // TODO Auto-generated method stub

    }

    public boolean getStartedByWebClient()
    {
        return false;
    }

    public boolean hasWatchdog()
    {
        return false;
    }

    public void displayInactivityDialogEnsureEDT(final String title,
        final String text, Color color)
    {
        // TODO Auto-generated method stub

    }

    public void inactivityAutoFleeOrConcede(boolean reply)
    {
        // TODO Auto-generated method stub

    }

    public void askExtraRollApproval(String requestorName, boolean ourself, int requestId)
    {
        // TODO Auto-generated method stub

    }

    public void askSuspendConfirmation(String requestorName, int timeout)
    {
        // TODO Auto-generated method stub
    }

    public String getGameId()
    {
        // TODO Auto-generated method stub
        return "Dummy";
    }

    public void actOnSplitRelatedRequestSent()
    {
        // TODO Auto-generated method stub
    }

}
