package net.sf.colossus.client;


import java.util.*;
import java.net.*;
import javax.swing.*;
import java.io.*;
import java.awt.event.*;
import java.awt.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.server.Server;
import net.sf.colossus.util.Options;
import net.sf.colossus.server.Player;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.AI;
import net.sf.colossus.server.SimpleAI;
import net.sf.colossus.server.Constants;
import net.sf.colossus.parser.TerrainRecruitLoader;


/**
 *  Interface to the network-accessible parts of Client.
 *  @version $Id$
 *  @author David Ripton
 */


public interface IClient
{
    public void tellMovementRoll(int roll);

    public void setOption(String optname, String value);

    public void updatePlayerInfo(String [] infoStrings);

    public void setColor(String color);

    public void updateCreatureCount(String creatureName, int count);

    public void dispose();

    public void removeLegion(String id);

    public void setLegionHeight(String markerId, int height);

    public void setLegionContents(String markerId, java.util.List names);

    public void addCreature(String markerId, String name);

    public void removeCreature(String markerId, String name);

    /** Reveal creatures in this legion, some of which already may be known. */
    public void revealCreatures(String markerId, final java.util.List names);

    public void removeDeadBattleChits();

    public void placeNewChit(String imageName, boolean inverted, int tag, 
        String hexLabel);

    public void initBoard();

    public void setPlayerName(String playerName);

    public void createSummonAngel(String markerId, String longMarkerName);

    public void askAcquireAngel(String markerId, java.util.List recruits);

    public void askChooseStrikePenalty(java.util.List choices);

    public void showMessageDialog(String message);

    public void tellGameOver(String message);

    public void askConcede(String longMarkerName, String hexDescription,
        String allyMarkerId, String enemyMarkerId);

    public void askFlee(String longMarkerName, String hexDescription,
        String allyMarkerId, String enemyMarkerId);

    public void askNegotiate(String attackerLongMarkerName, 
        String defenderLongMarkerName, String attackerId, String defenderId, 
        String hexLabel);

    public void tellProposal(Proposal proposal);

    public void tellStrikeResults(String strikerDesc, int strikerTag,
        String targetDesc, int targetTag, int strikeNumber, int [] rolls, 
        int damage, boolean killed, boolean wasCarry, int carryDamageLeft,
        Set carryTargetDescriptions);
        
    public void initBattle(String masterHexLabel, int battleTurnNumber,
        String battleActivePlayerName, int battlePhase,
        String attackerMarkerId, String defenderMarkerId);

    public void cleanupBattle();

    public void highlightEngagements();

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

    public void setupBattleMove();

    public void setupBattleFight(int battlePhase, 
        String battleActivePlayerName);

    public void tellLegionLocation(String markerId, String hexLabel);

    public void tellBattleMove(int tag, String startingHexLabel, 
        String endingHexLabel, boolean undo);

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel, boolean teleport);

    public void undidMove(String markerId, String formerHexLabel,
        String currentHexLabel);

    public void undidSplit(String splitoffId);

    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight);

    public void askPickColor(Set colorsLeft);
}
