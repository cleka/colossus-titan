package net.sf.colossus.gui;


import java.awt.GraphicsDevice;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.variant.BattleHex;
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

    public void actOnAddCreature(Legion legion, String name, String reason)
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
        MasterHex currentHex, boolean teleport, String teleportingLord,
        boolean splitLegionHasForcedMove)
    {
        // TODO Auto-generated method stub

    }

    public void actOnDidRecruit(Legion legion, String recruitName,
        List<String> recruiters, String reason)
    {
        // TODO Auto-generated method stub

    }

    public void actOnDidSplit(int turn, Legion parent, Legion child,
        MasterHex hex)
    {
        // TODO Auto-generated method stub

    }

    public void actOnDidSplitPart2(MasterHex hex)
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

    public void actOnNextEngagement()
    {
        // TODO Auto-generated method stub

    }

    public void addBattleChit(GUIBattleChit battleChit)
    {
        // TODO Auto-generated method stub

    }

    public void actOnPlaceNewChit(BattleHex hex)
    {
        // TODO Auto-generated method stub

    }

    public void resetStrikeNumbers()
    {
        // TODO Auto-generated method stub
    }

    public void actOnRemoveCreature(Legion legion, String name, String reason)
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

    public void actOnSetupBattleFight(BattlePhase battlePhase,
        int battleTurnNumber)
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

    public void actOnSetupSplit()
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellBattleMove(BattleHex startingHex,
        BattleHex endingHex)
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellEngagementResults(Legion winner, String method,
        int points, int turns)
    {
        // TODO Auto-generated method stub

    }

    public void actOnTellGameOver(String message, boolean disposeFollows)
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

    public void actOnTellStrikeResults(boolean wasCarry, int strikeNumber,
        List<String> rolls,BattleCritter striker, BattleCritter target)
    {
        // TODO Auto-generated method stub

    }

    public void actOnUndidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove,
        boolean didTeleport)
    {
        // TODO Auto-generated method stub

    }

    public void actOnUndidRecruitPart2(Legion legion,
        boolean wasReinforcement, int turnNumber)
    {
        // TODO Auto-generated method stub

    }

    public void addPossibleRecruitChits(LegionClientSide legion,
        Set<MasterHex> hexes)
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

    public void boardActOnUndidSplit(Legion survivor, int turn)
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

    public void clearUndoStack()
    {
        // TODO Auto-generated method stub

    }

    public void closePerhapsWithMessage()
    {
        // TODO Auto-generated method stub

    }

    public void defaultCursor()
    {
        // TODO Auto-generated method stub

    }

    public void didSummon(Legion summoner, Legion donor, String summon)
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

    public void doAcquireAngel(Legion legion, List<String> recruits)
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

    public PlayerColor doPickColor(String playerName,
        List<PlayerColor> colorsLeft)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public EntrySide doPickEntrySide(MasterHex hex, Set<EntrySide> entrySides)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String doPickLord(List<String> lords)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String doPickMarker(Set<String> markersAvailable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String doPickMarkerUntilGotOne(Set<String> markersAvailable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String doPickRecruit(Legion legion, String hexDescription)
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

    public String doPickSplitLegion(Legion parent, String childMarker)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void doPickStrikePenalty(Client client, List<String> choices)
    {
        // TODO Auto-generated method stub

    }

    public SummonInfo doPickSummonAngel(Legion legion,
        SortedSet<Legion> possibleDonors)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void engagementResultsMaybeShow()
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerAttackerSetCreatureDead(String name, int height)
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerCancelReinforcement(String recruitName, int turnNr)
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerDefenderSetCreatureDead(String name, int height)
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerNewSplitEvent(int turn, Legion parent, Legion child)
    {
        // TODO Auto-generated method stub

    }

    public void eventViewerRevealCreatures(Legion legion, List<String> names,
        String reason)
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

    public MasterBoard getBoard()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public PickCarry getPickCarryDialog()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getRecruitChitMode()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getViewMode()
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

    public Object popUndoStack()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void pushUndoStack(Object object)
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

    public void revealEngagedCreatures(Legion legion, List<String> names,
        boolean isAttacker, String reason)
    {
        // TODO Auto-generated method stub

    }

    public void serverConfirmsConnection()
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

    public void setMulliganOldRoll(int movementRoll)
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

    public void setWebClient(WebClient wc)
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

    public void showHexRecruitTree(GUIMasterHex hex)
    {
        // TODO Auto-generated method stub

    }

    public void showMarker(Marker marker)
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

    public void tellMovementRoll(int roll)
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

}
