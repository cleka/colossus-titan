package net.sf.colossus.client;


import java.util.*;
import java.net.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.Glob;
import net.sf.colossus.server.Constants;

/**
 *  Thread to handle inbound server connection on client side.
 *  @version $Id$
 *  @author David Ripton
 */


final class SocketClientThread extends Thread
{
    private Socket socket;
    private Client client;
    private BufferedReader in;

    private final String sep = Constants.protocolTermSeparator;


    SocketClientThread(Client client, Socket socket)
    {
        super("SocketServerThread");
        this.client = client;
        this.socket = socket;
    }


    public void run()
    {
        try
        {
            in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        }
        catch (IOException ex)
        {
            Log.error(ex.toString());
            return;
        }

        String fromServer = null;
        try
        {
            while ((fromServer = in.readLine()) != null)
            {
                // XXX recursive logging
                if (fromServer.length() > 0 && 
                    !fromServer.startsWith(Constants.log))
                {
                    Log.debug("From server: " + fromServer);
                    parseLine(fromServer);
                }
            }
Log.debug("End of SocketClientThread while loop");
        }
        catch (IOException ex)
        {
            Log.error(ex.toString());
        }
    }


    private void parseLine(String s)
    {
        List li = Split.split(sep, s);
        String method = (String)li.remove(0);
        callMethod(method, li);
    }

    private void callMethod(String method, List args)
    {
        if (method.equals(Constants.tellMovementRoll))
        {
            int roll = Integer.parseInt((String)args.remove(0));
            client.tellMovementRoll(roll);
        }
        else if (method.equals(Constants.setOption))
        {
            String optname = (String)args.remove(0);
            String value = (String)args.remove(0);
            client.setOption(optname, value);
        }
        else if (method.equals(Constants.updatePlayerInfo))
        {
            List infoStrings = Split.split(Glob.sep, (String)args.remove(0));
            client.updatePlayerInfo(infoStrings);
        }
        else if (method.equals(Constants.setColor))
        {
            String color = (String)args.remove(0);
            client.setColor(color);
        }
        else if (method.equals(Constants.updateCreatureCount))
        {
            String creatureName = (String)args.remove(0);
            int count = Integer.parseInt((String)args.remove(0));
            client.updateCreatureCount(creatureName, count);
        }
        else if (method.equals(Constants.dispose))
        {
            client.dispose();
        }
        else if (method.equals(Constants.removeLegion))
        {
            String id = (String)args.remove(0);
            client.removeLegion(id);
        }
        else if (method.equals(Constants.setLegionHeight))
        {
            String markerId = (String)args.remove(0);
            int height = Integer.parseInt((String)args.remove(0));
            client.setLegionHeight(markerId, height);
        }
        else if (method.equals(Constants.setLegionContents))
        {
            String markerId = (String)args.remove(0);
            List names = Split.split(Glob.sep, (String)args.remove(0));
            client.setLegionContents(markerId, names);
        }
        else if (method.equals(Constants.addCreature))
        {
            String markerId = (String)args.remove(0);
            String name = (String)args.remove(0);
            client.addCreature(markerId, name);
        }
        else if (method.equals(Constants.removeCreature))
        {
            String markerId = (String)args.remove(0);
            String name = (String)args.remove(0);
            client.removeCreature(markerId, name);
        }
        else if (method.equals(Constants.revealCreatures))
        {
            String markerId = (String)args.remove(0);
            List names = Split.split(Glob.sep, (String)args.remove(0));
            client.revealCreatures(markerId, names);
        }
        else if (method.equals(Constants.removeDeadBattleChits))
        {
            client.removeDeadBattleChits();
        }
        else if (method.equals(Constants.placeNewChit))
        {
            String imageName = (String)args.remove(0);
            boolean inverted = 
                Boolean.valueOf((String)args.remove(0)).booleanValue();
            int tag = Integer.parseInt((String)args.remove(0));
            String hexLabel = (String)args.remove(0);
            client.placeNewChit(imageName, inverted, tag, hexLabel);
        }
        else if (method.equals(Constants.initBoard))
        {
            client.initBoard();
        }
        else if (method.equals(Constants.setPlayerName))
        {
            String playerName = (String)args.remove(0);
            client.setPlayerName(playerName);
        }
        else if (method.equals(Constants.createSummonAngel))
        {
            String markerId = (String)args.remove(0);
            client.createSummonAngel(markerId);
        }
        else if (method.equals(Constants.askAcquireAngel))
        {
            String markerId = (String)args.remove(0);
            List recruits = Split.split(Glob.sep, (String)args.remove(0));
            client.askAcquireAngel(markerId, recruits);
        }
        else if (method.equals(Constants.askChooseStrikePenalty))
        {
            List choices = Split.split(Glob.sep, (String)args.remove(0));
            client.askChooseStrikePenalty(choices);
        }
        else if (method.equals(Constants.showMessageDialog))
        {
            String message = (String)args.remove(0);
            client.showMessageDialog(message);
        }
        else if (method.equals(Constants.tellGameOver))
        {
            String message = (String)args.remove(0);
            client.tellGameOver(message);
        }
        else if (method.equals(Constants.askConcede))
        {
            String allyMarkerId = (String)args.remove(0);
            String enemyMarkerId = (String)args.remove(0);
            client.askConcede(allyMarkerId, enemyMarkerId);
        }
        else if (method.equals(Constants.askFlee))
        {
            String allyMarkerId = (String)args.remove(0);
            String enemyMarkerId = (String)args.remove(0);
            client.askFlee(allyMarkerId, enemyMarkerId);
        }
        else if (method.equals(Constants.askNegotiate))
        {
            String attackerId = (String)args.remove(0);
            String defenderId = (String)args.remove(0);
            client.askNegotiate(attackerId, defenderId);
        }
        else if (method.equals(Constants.tellProposal))
        {
            String proposalString = (String)args.remove(0);
            client.tellProposal(proposalString);
        }
        else if (method.equals(Constants.tellStrikeResults))
        {
            int strikerTag = Integer.parseInt((String)args.remove(0));
            int targetTag = Integer.parseInt((String)args.remove(0));
            int strikeNumber = Integer.parseInt((String)args.remove(0));

            List rollStrings = Split.split(Glob.sep, (String)args.remove(0));
            // Convert from list of Strings to list of Integers.
            List rolls = new ArrayList();
            Iterator it = rollStrings.iterator();
            while (it.hasNext())
            {
                String s = (String)it.next();
                Integer i = new Integer(s);
                rolls.add(i);
            }

            int damage = Integer.parseInt((String)args.remove(0));
            boolean killed = 
                Boolean.valueOf((String)args.remove(0)).booleanValue();
            boolean wasCarry = 
                Boolean.valueOf((String)args.remove(0)).booleanValue();
            int carryDamageLeft = Integer.parseInt((String)args.remove(0));

            Set carryTargetDescriptions = new HashSet();
            if (!args.isEmpty())
            {
                String buf = (String)args.remove(0);
                if (buf != null && buf.length() > 0)
                {
                    List ctdList = Split.split(Glob.sep, buf);
                    carryTargetDescriptions.addAll(ctdList);
                }
            }

            client.tellStrikeResults(strikerTag, targetTag, strikeNumber,
                rolls, damage, killed, wasCarry, carryDamageLeft,
                carryTargetDescriptions);
        }
        else if (method.equals(Constants.initBattle))
        {
            String masterHexLabel = (String)args.remove(0);
            int battleTurnNumber = Integer.parseInt((String)args.remove(0));
            String battleActivePlayerName = (String)args.remove(0);
            int battlePhase = Integer.parseInt((String)args.remove(0));
            String attackerMarkerId = (String)args.remove(0);
            String defenderMarkerId = (String)args.remove(0);
            client.initBattle(masterHexLabel, battleTurnNumber,
                battleActivePlayerName, battlePhase, attackerMarkerId,
                defenderMarkerId);
        }
        else if (method.equals(Constants.cleanupBattle))
        {
            client.cleanupBattle();
        }
        else if (method.equals(Constants.highlightEngagements))
        {
            client.highlightEngagements();
        }
        else if (method.equals(Constants.doReinforce))
        {
            String markerId = (String)args.remove(0);
            client.doReinforce(markerId);
        }
        else if (method.equals(Constants.didRecruit))
        {
            String markerId = (String)args.remove(0);
            String recruitName = (String)args.remove(0);
            String recruiterName = (String)args.remove(0);
            int numRecruiters = Integer.parseInt((String)args.remove(0));
            client.didRecruit(markerId, recruitName, recruiterName, 
                numRecruiters);
        }
        else if (method.equals(Constants.undidRecruit))
        {
            String markerId = (String)args.remove(0);
            String recruitName = (String)args.remove(0);
            client.undidRecruit(markerId, recruitName);
        }
        else if (method.equals(Constants.setupTurnState))
        {
            String activePlayerName = (String)args.remove(0);
            int turnNumber = Integer.parseInt((String)args.remove(0));
            client.setupTurnState(activePlayerName, turnNumber);
        }
        else if (method.equals(Constants.setupSplit))
        {
            List maList = Split.split(Glob.sep, (String)args.remove(0));
            Set markersAvailable = new HashSet();
            markersAvailable.addAll(maList);

            String activePlayerName = (String)args.remove(0);
            int turnNumber = Integer.parseInt((String)args.remove(0));
            client.setupSplit(markersAvailable, activePlayerName, turnNumber);
        }
        else if (method.equals(Constants.setupMove))
        {
            client.setupMove();
        }
        else if (method.equals(Constants.setupFight))
        {
            client.setupFight();
        }
        else if (method.equals(Constants.setupMuster))
        {
            client.setupMuster();
        }
        else if (method.equals(Constants.setupBattleSummon))
        {
            String battleActivePlayerName = (String)args.remove(0);
            int battleTurnNumber = Integer.parseInt((String)args.remove(0));
            client.setupBattleSummon(battleActivePlayerName, battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleRecruit))
        {
            String battleActivePlayerName = (String)args.remove(0);
            int battleTurnNumber = Integer.parseInt((String)args.remove(0));
            client.setupBattleRecruit(battleActivePlayerName, 
                battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleMove))
        {
            client.setupBattleMove();
        }
        else if (method.equals(Constants.setupBattleFight))
        {
            int battlePhase = Integer.parseInt((String)args.remove(0));
            String battleActivePlayerName = (String)args.remove(0);
            client.setupBattleFight(battlePhase, battleActivePlayerName);
        }
        else if (method.equals(Constants.tellLegionLocation))
        {
            String markerId = (String)args.remove(0);
            String hexLabel = (String)args.remove(0);
            client.tellLegionLocation(markerId, hexLabel);
        }
        else if (method.equals(Constants.tellBattleMove))
        {
            int tag = Integer.parseInt((String)args.remove(0));
            String startingHexLabel = (String)args.remove(0);
            String endingHexLabel = (String)args.remove(0);
            boolean undo = 
                Boolean.valueOf((String)args.remove(0)).booleanValue();
            client.tellBattleMove(tag, startingHexLabel, endingHexLabel, undo);
        }
        else if (method.equals(Constants.didMove))
        {
            String markerId = (String)args.remove(0);
            String startingHexLabel = (String)args.remove(0);
            String currentHexLabel = (String)args.remove(0);
            boolean teleport = 
                Boolean.valueOf((String)args.remove(0)).booleanValue();
            client.didMove(markerId, startingHexLabel, currentHexLabel,
                teleport);
        }
        else if (method.equals(Constants.undidMove))
        {
            String markerId = (String)args.remove(0);
            String formerHexLabel = (String)args.remove(0);
            String currentHexLabel = (String)args.remove(0);
            client.undidMove(markerId, formerHexLabel, currentHexLabel);
        }
        else if (method.equals(Constants.undidSplit))
        {
            String splitoffId = (String)args.remove(0);
            client.undidSplit(splitoffId);
        }
        else if (method.equals(Constants.didSplit))
        {
            String hexLabel = (String)args.remove(0);
            String parentId = (String)args.remove(0);
            String childId = (String)args.remove(0);
            int childHeight = Integer.parseInt((String)args.remove(0));
            client.didSplit(hexLabel, parentId, childId, childHeight);
        }
        else if (method.equals(Constants.askPickColor))
        {
            List clList = Split.split(Glob.sep, (String)args.remove(0));
            Set colorsLeft = new HashSet();
            colorsLeft.addAll(clList);
            client.askPickColor(colorsLeft);
        }
        else if (method.equals(Constants.log))
        {
            String message = (String)args.remove(0);
            client.log(message);
        }
        else
        {
            Log.error("Bogus packet");
        }
    }
}
