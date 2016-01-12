package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 * This will be a thread that executes all the client side processing;
 * additional to the actual SocketClientThread which did that earlier.
 *
 * SCT will only handle the reading from Socket and push incoming
 * messages to a queue and return back to listening on the socket.
 * This way, it should be able to read all data in time so that no
 * data pile up, and reply to ping requests almost instantly.
 *
 * ClientThread gets most of messages to parse already via a queue.
 * Should also sending use a queue, and/or, the sending methods be
 * inside ClientThread, and SocketClientThread merely provide the
 * sendToServer method (which could be implemented differently e.g.
 * in a "send via queue, instead of via socket", for local clients)?
 *
 *  @author Clemens Katzer
 */

public class ClientThread extends Thread implements EventExecutor
{
    private static final Logger LOGGER = Logger.getLogger(ClientThread.class
        .getName());

    private static int threadNumberCounter = 0;

    private final int threadNr;

    private final Client client;

    private final LinkedBlockingQueue<ServerEvent> queue;

    private boolean done = false;

    private final boolean _DEBUG_MSGS = false;

    // if we enable that, things get stuck... (perhaps because logger
    // of all threads need then to get a lock on the logger too often?)
    private final boolean LOG_PROCESSING_TIMES = false;

    // this is enqueued to get the thread out of the "take()"-waiting
    private final static ServerEvent END_EVENT = new ClientThread.ServerEvent(
        0L, "END", new ArrayList<String>());

    public ClientThread(Client client)
    {
        this.client = client;
        this.threadNr = nextThreadNumber();

        queue = new LinkedBlockingQueue<ServerEvent>();
    }

    private static synchronized int nextThreadNumber()
    {
        return ++threadNumberCounter;
    }

    public int getThreadNumber()
    {
        return this.threadNr;
    }

    public void enqueue(String method, List<String> args)
    {
        queue.offer(new ServerEvent(ClientThread.getNow(), method, args));
    }

    // Client checks whether still something to do from "here" before
    // switching to new connectio (after reconnect)
    public int getQueueLen()
    {
        return queue.size();
    }

    /**
     * Take the method names of all events currently in the queue, and return
     * them as a String, separated by comma.
     * @return String with list of method names, comma-separated
     */
    public String getQueueContentSummary()
    {
        Object items[] = queue.toArray();
        List<String> methodNameList = new LinkedList<String>();
        for (Object item : items)
        {
            if (item instanceof ServerEvent)
            {
                ServerEvent event = (ServerEvent)item;
                methodNameList.add(event.getMethod());
            }
            else
            {
                methodNameList.add("<???>");
            }
        }

        return Glob.glob(", ", methodNameList);
    }

    public void disposeQueue()
    {
        // Get thread out of it's "take" waiting
        done = true;
        boolean success = queue.offer(END_EVENT);
        if (!success)
        {
            LOGGER.warning("CT " + getName()
                + ": failed to offer END signal to queue!");
        }
    }

    public boolean isEngagementStartupOngoing()
    {
        return client.isEngagementStartupOngoing();
    }

    public void clearEngagementStartupOngoing()
    {
        client.setEngagementStartupOngoing(false);
    }

    public void disposeClient()
    {
        client.disposeClient();
    }

    public void setClosedByServer()
    {
        client.setClosedByServer();
    }

    public String getNameMaybe()
    {
        if (client != null && client.getOwningPlayer() != null)
        {
            return client.getOwningPlayer().getName();
        }
        else
        {
            return "<no client or no owning player name yet>";
        }
    }

    private ServerEvent lastInactivityRelevantEvent;

    private boolean retriggeredEventOngoing;

    public void retriggerEventAsThread()
    {
        Runnable eventRunner = new Runnable()
        {
            public void run()
            {
                //
            }
        };

        new Thread(eventRunner).start();
    }

    public void retriggerEvent()
    {
        if (lastInactivityRelevantEvent == null)
        {
            LOGGER
                .severe("retriggerEvent called, but lastInactivityRelevantEvent is null ???");
            return;
        }

        if (lastInactivityRelevantEvent.getIsRetriggered())
        {
            LOGGER
                .finest("\n..., ah, but this is the retriggered one. Doing nothing.");
            return;
        }
        try
        {
            lastInactivityRelevantEvent.setIsRetriggered();
            ServerEvent tmp = lastInactivityRelevantEvent;
            lastInactivityRelevantEvent = null;
            queue.put(tmp);
        }
        catch (InterruptedException e)
        {
            LOGGER.severe("queue.put() interrupted?!?!");
        }
    }

    public boolean isThereALastEvent()
    {
        return lastInactivityRelevantEvent != null;
    }

    public boolean getRetriggeredEventOngoing()
    {
        return retriggeredEventOngoing;
    }

    @Override
    public void run()
    {
        LOGGER.finest("ClientThread run() started.");
        while (!done)
        {
            ServerEvent event = null;
            try
            {
                event = queue.take();
                if (event == END_EVENT)
                {
                    continue;
                }
                event.executionStarts(ClientThread.getNow());
                retriggeredEventOngoing = event.isRetriggered;
                callMethod(event);
                retriggeredEventOngoing = false;
                event.executionCompleted(ClientThread.getNow());
            }
            catch (InterruptedException e)
            {
                LOGGER.severe("queue.take() interrupted?!?!");
            }

            if (event != null)
            {
                if (LOG_PROCESSING_TIMES)
                {
                    event.logProcessing();
                }
            }
            else
            {
                LOGGER.severe("null event - bailed out with exception??");
            }

        }
        LOGGER.finest("ClientThread run() ending.");
    }

    public void notifyUserIfGameIsPaused(String message)
    {
        if (client.isPaused())
        {
            LOGGER.info("Game in \"Pause\" state - "
                + "sending message anyway.");
            if (!message.startsWith(Constants.replyToPing))
            {
                client.showMessageDialog("NOTE: Game is paused "
                    + "- server will not confirm/react to any of\n"
                    + "your GUI activities (move, split, ...), and thus "
                    + "they will not show effect on the Board yet!");
            }
        }
    }

    public void notifyThatNotConnected()
    {
        client.notifyThatNotConnected();
    }

    public void appendToConnectionLog(String s)
    {
        client.appendToConnectionLog(s);
    }

    static int _MAXLEN = 80;

    private void callMethod(ServerEvent event)
    {
        String method = event.getMethod();
        List<String> args = event.getArgs();
        if (client != null && !client.getOwningPlayer().isAI()
            && client.isFightPhase())
        {
            String allArgs = new String();
            allArgs = Glob.glob(", ", args);
            if (_MAXLEN != 0)
            {
                int len = allArgs.length();
                if (len > _MAXLEN)
                {
                    allArgs = allArgs.substring(0, _MAXLEN) + "...";
                }
            }
            if (isEngagementStartupOngoing())
            {
                client.logMsgToServer("I", "CT, message: " + method + "("
                    + allArgs + ")");
            }
            LOGGER.info("CT, message: " + method + "(" + allArgs + ")");
        }
        else
        {
            LOGGER.finest("Client (CT) '" + getNameMaybe()
                + "' processing message: " + method);
        }

        showDebugOutputMaybe(method, args);

        if (method.equals(Constants.tellMovementRoll))
        {
            int roll = Integer.parseInt(args.remove(0));
            String reason = "";
            if (args.size() > 0)
            {
                reason = args.remove(0);
            }
            LOGGER.finest("tellMovementRoll, roll=" + roll + ", reason='"
                + reason + ".");
            client.tellMovementRoll(roll, reason);

        }
        else if (method.equals(Constants.syncOption))
        {
            String optname = args.remove(0);
            String value = args.remove(0);
            client.syncOption(optname, value);
        }
        else if (method.equals(Constants.updatePlayerInfo))
        {
            List<String> infoStrings = Split.split(Glob.sep, args.remove(0));
            client.updatePlayerInfo(infoStrings);
        }
        else if (method.equals(Constants.updateChangedValues))
        {
            String valuesString = args.remove(0);
            String reason = args.remove(0);
            client.updateChangedPlayerValues(valuesString, reason);
        }
        else if (method.equals(Constants.setColor))
        {
            String colorName = args.remove(0);
            client.setColor(PlayerColor.getByName(colorName));
        }
        else if (method.equals(Constants.updateCreatureCount))
        {
            String creatureName = args.remove(0);
            int count = Integer.parseInt(args.remove(0));
            int deadCount = Integer.parseInt(args.remove(0));
            client.updateCreatureCount(resolveCreatureType(creatureName),
                count, deadCount);
        }
        else if (method.equals(Constants.removeLegion))
        {
            String id = args.remove(0);
            client.removeLegion(resolveLegion(id));
        }
        else if (method.equals(Constants.setLegionStatus))
        {
            String markerId = args.remove(0);
            boolean moved = Boolean.valueOf(args.remove(0)).booleanValue();
            boolean teleported = Boolean.valueOf(args.remove(0))
                .booleanValue();
            int entrySideId = Integer.parseInt(args.remove(0));
            String lastRecruit = args.remove(0);
            client.setLegionStatus(resolveLegion(markerId), moved, teleported,
                EntrySide.values()[entrySideId],
                resolveCreatureType(lastRecruit));
        }
        else if (method.equals(Constants.addCreature))
        {
            String markerId = args.remove(0);
            String name = args.remove(0);
            String reason = args.isEmpty() ? "<Unknown>" : args.remove(0);
            client.addCreature(resolveLegion(markerId),
                resolveCreatureType(name), reason);
        }
        else if (method.equals(Constants.removeCreature))
        {
            String markerId = args.remove(0);
            String name = args.remove(0);
            String reason = args.isEmpty() ? "<Unknown>" : args.remove(0);
            client.removeCreature(resolveLegion(markerId),
                resolveCreatureType(name), reason);
        }
        else if (method.equals(Constants.revealCreatures))
        {
            String markerId = args.remove(0);
            String namesString = args.remove(0);
            List<String> names = Split.split(Glob.sep, namesString);

            // safeguard against getting empty string list from server
            // TODO: should split be fixed instead??
            if (namesString.equals("") && names.size() > 0
                && names.get(0).equals(""))
            {
                names.remove(0);
            }
            String reason = args.isEmpty() ? "<Unknown>" : args.remove(0);
            Player player = client.getGameClientSide().getPlayerByMarkerId(
                markerId);
            Legion legion;
            if (player.hasLegion(markerId))
            {
                legion = player.getLegionByMarkerId(markerId);
            }
            else
            {
                // this can happen on game startup since there is no explicit
                // event creating the first legions
                // TODO try to make this less implicit
                assert client.getTurnNumber() == -1 : "Implicit legion creation should happen only "
                    + "before the first round";
                legion = new LegionClientSide(player, markerId,
                    player.getStartingTower());
                player.addLegion(legion);
            }
            List<CreatureType> creatures = new ArrayList<CreatureType>();
            for (String name : names)
            {
                creatures.add(resolveCreatureType(name));
            }
            client.revealCreatures(legion, creatures, reason);
        }
        else if (method.equals(Constants.revealEngagedCreatures))
        {
            String markerId = args.remove(0);
            boolean isAttacker = Boolean.valueOf(args.remove(0))
                .booleanValue();
            String names = args.remove(0);
            String reason = args.isEmpty() ? "<Unknown>" : args.remove(0);
            client.revealEngagedCreatures(resolveLegion(markerId),
                resolveCreatureTypes(names), isAttacker, reason);
        }
        else if (method.equals(Constants.removeDeadBattleChits))
        {
            client.removeDeadBattleChits();
        }
        else if (method.equals(Constants.placeNewChit))
        {
            clearEngagementStartupOngoing();
            String imageName = args.remove(0);
            boolean inverted = Boolean.valueOf(args.remove(0)).booleanValue();
            int tag = Integer.parseInt(args.remove(0));
            String hexLabel = args.remove(0);
            BattleHex hex = resolveBattleHex(hexLabel);
            client.placeNewChit(imageName, inverted, tag, hex);
        }
        else if (method.equals(Constants.replayOngoing))
        {
            boolean val = Boolean.valueOf(args.remove(0)).booleanValue();
            // older servers may not send this...
            // TODO obsolete... nowadays they do, and there are other,
            // incompatiblities added since then...
            String turnArgMaybe = args.isEmpty() ? "0" : args.remove(0);
            int maxTurn = Integer.parseInt(turnArgMaybe);
            client.tellReplay(val, maxTurn);

        }
        else if (method.equals(Constants.redoOngoing))
        {
            boolean val = Boolean.valueOf(args.remove(0)).booleanValue();
            client.tellRedo(val);
        }
        else if (method.equals(Constants.initBoard))
        {
            client.initBoard();
            client.setEventExecutor(this);
        }
        else if (method.equals(Constants.setPlayerName))
        {
            String playerName = args.remove(0);
            client.setPlayerName(playerName);
        }
        else if (method.equals(Constants.createSummonAngel))
        {
            rememberEvent(event);
            String markerId = args.remove(0);
            client.createSummonAngel(resolveLegion(markerId));
        }
        else if (method.equals(Constants.askAcquireAngel))
        {
            rememberEvent(event);
            String markerId = args.remove(0);
            List<CreatureType> recruits = resolveCreatureTypes(args.remove(0));
            client.askAcquireAngel(resolveLegion(markerId), recruits);
        }
        else if (method.equals(Constants.askChooseStrikePenalty))
        {
            rememberEvent(event);
            List<String> choices = Split.split(Glob.sep, args.remove(0));
            client.askChooseStrikePenalty(choices);
        }
        else if (method.equals(Constants.tellGameOver))
        {
            String message = args.remove(0);
            boolean disposeFollows = false;
            if (!args.isEmpty())
            {
                disposeFollows = Boolean.valueOf(args.remove(0))
                    .booleanValue();
            }
            boolean suspended = false;
            if (!args.isEmpty())
            {
                suspended = Boolean.valueOf(args.remove(0)).booleanValue();
            }

            client.tellGameOver(message, disposeFollows, suspended);
        }
        else if (method.equals(Constants.tellPlayerElim))
        {
            String playerName = args.remove(0);
            String slayerName = args.remove(0);
            // TODO use the "noone" player instead of null if no slayer?
            client.tellPlayerElim(
                client.getPlayerByName(playerName),
                slayerName.equals("null") ? null : (client.getGameClientSide()
                    .getPlayerByName(slayerName)));
        }
        else if (method.equals(Constants.askConcede))
        {
            rememberEvent(event);
            String allyMarkerId = args.remove(0);
            String enemyMarkerId = args.remove(0);
            client.askConcede(resolveLegion(allyMarkerId),
                resolveLegion(enemyMarkerId));
        }
        else if (method.equals(Constants.askFlee))
        {
            rememberEvent(event);
            String allyMarkerId = args.remove(0);
            String enemyMarkerId = args.remove(0);
            client.askFlee(resolveLegion(allyMarkerId),
                resolveLegion(enemyMarkerId));
        }
        else if (method.equals(Constants.askNegotiate))
        {
            rememberEvent(event);
            String attackerId = args.remove(0);
            String defenderId = args.remove(0);
            client.askNegotiate(resolveLegion(attackerId),
                resolveLegion(defenderId));
        }
        else if (method.equals(Constants.tellProposal))
        {
            String proposalString = args.remove(0);
            client.tellProposal(proposalString);
        }
        else if (method.equals(Constants.tellSlowResults))
        {
            int targetTag = Integer.parseInt(args.remove(0));
            int slowValue = Integer.parseInt(args.remove(0));
            client.tellSlowResults(targetTag, slowValue);
        }
        else if (method.equals(Constants.tellStrikeResults))
        {
            int strikerTag = Integer.parseInt(args.remove(0));
            int targetTag = Integer.parseInt(args.remove(0));
            int strikeNumber = Integer.parseInt(args.remove(0));
            List<String> rolls = Split.split(Glob.sep, args.remove(0));
            int damage = Integer.parseInt(args.remove(0));
            boolean killed = Boolean.valueOf(args.remove(0)).booleanValue();
            boolean wasCarry = Boolean.valueOf(args.remove(0)).booleanValue();
            int carryDamageLeft = Integer.parseInt(args.remove(0));

            Set<String> carryTargetDescriptions = new HashSet<String>();
            if (!args.isEmpty())
            {
                String buf = args.remove(0);
                if (buf != null && buf.length() > 0)
                {
                    List<String> ctdList = Split.split(Glob.sep, buf);
                    carryTargetDescriptions.addAll(ctdList);
                }
            }

            client.tellStrikeResults(strikerTag, targetTag, strikeNumber,
                rolls, damage, killed, wasCarry, carryDamageLeft,
                carryTargetDescriptions);
        }
        else if (method.equals(Constants.initBattle))
        {
            String masterHexLabel = args.remove(0);
            int battleTurnNumber = Integer.parseInt(args.remove(0));
            String battleActivePlayerName = args.remove(0);
            BattlePhase battlePhase = BattlePhase.values()[Integer
                .parseInt(args.remove(0))];
            String attackerMarkerId = args.remove(0);
            String defenderMarkerId = args.remove(0);
            client.initBattle(resolveHex(masterHexLabel), battleTurnNumber,
                client.getPlayerByName(battleActivePlayerName), battlePhase,
                resolveLegion(attackerMarkerId),
                resolveLegion(defenderMarkerId));
        }
        else if (method.equals(Constants.cleanupBattle))
        {
            client.cleanupBattle();
        }
        else if (method.equals(Constants.nextEngagement))
        {
            rememberEvent(event);
            client.nextEngagement();
        }
        else if (method.equals(Constants.doReinforce))
        {
            rememberEvent(event);
            String markerId = args.remove(0);
            client.doReinforce(resolveLegion(markerId));
        }
        else if (method.equals(Constants.didRecruit))
        {
            String markerId = args.remove(0);
            String recruitName = args.remove(0);
            String recruiterName = args.remove(0);
            int numRecruiters = Integer.parseInt(args.remove(0));
            client.didRecruit(resolveLegion(markerId),
                resolveCreatureType(recruitName),
                resolveCreatureType(recruiterName), numRecruiters);
        }
        else if (method.equals(Constants.undidRecruit))
        {
            String markerId = args.remove(0);
            String recruitName = args.remove(0);
            client.undidRecruit(resolveLegion(markerId),
                resolveCreatureType(recruitName));
        }
        else if (method.equals(Constants.setupTurnState))
        {
            rememberEvent(event);
            String activePlayerName = args.remove(0);
            int turnNumber = Integer.parseInt(args.remove(0));
            client.setupTurnState(client.getPlayerByName(activePlayerName),
                turnNumber);
        }
        else if (method.equals(Constants.setupSplit))
        {
            String activePlayerName = args.remove(0);
            int turnNumber = Integer.parseInt(args.remove(0));
            client.setupSplit(client.getPlayerByName(activePlayerName),
                turnNumber);
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
        else if (method.equals(Constants.kickPhase))
        {
            client.kickPhase();
        }

        else if (method.equals(Constants.setupBattleSummon))
        {
            rememberEvent(event);
            Player battleActivePlayer = client.getPlayerByName(args.remove(0));
            int battleTurnNumber = Integer.parseInt(args.remove(0));
            if (battleActivePlayer.equals(client.getOwningPlayer()))
            {
                rememberEvent(event);
            }
            client.setupBattleSummon(battleActivePlayer, battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleRecruit))
        {
            Player battleActivePlayer = client.getPlayerByName(args.remove(0));
            int battleTurnNumber = Integer.parseInt(args.remove(0));
            if (battleActivePlayer.equals(client.getOwningPlayer()))
            {
                rememberEvent(event);
            }
            client.setupBattleRecruit(battleActivePlayer, battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleMove))
        {
            Player battleActivePlayer = client.getPlayerByName(args.remove(0));
            int battleTurnNumber = Integer.parseInt(args.remove(0));
            client.setupBattleMove(battleActivePlayer, battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleFight))
        {
            BattlePhase battlePhase = BattlePhase.values()[Integer
                .parseInt(args.remove(0))];
            Player battleActivePlayer = client.getPlayerByName(args.remove(0));
            if (battleActivePlayer.equals(client.getOwningPlayer()))
            {
                rememberEvent(event);
            }
            client.setupBattleFight(battlePhase, battleActivePlayer);
        }
        else if (method.equals(Constants.tellLegionLocation))
        {
            String markerId = args.remove(0);
            String hexLabel = args.remove(0);
            client.tellLegionLocation(resolveLegion(markerId),
                resolveHex(hexLabel));
        }
        else if (method.equals(Constants.tellBattleMove))
        {
            int tag = Integer.parseInt(args.remove(0));
            String startingHexLabel = args.remove(0);
            String endingHexLabel = args.remove(0);
            boolean undo = Boolean.valueOf(args.remove(0)).booleanValue();
            BattleHex startingHex = resolveBattleHex(startingHexLabel);
            BattleHex endingHex = resolveBattleHex(endingHexLabel);
            client.tellBattleMove(tag, startingHex, endingHex, undo);
        }
        else if (method.equals(Constants.didMove))
        {
            String markerId = args.remove(0);
            String startingHexLabel = args.remove(0);
            String currentHexLabel = args.remove(0);
            String entrySideLabel = args.remove(0);
            boolean teleport = Boolean.valueOf(args.remove(0)).booleanValue();
            // servers from older versions might not send this arg
            String teleportingLord = null;
            if (!args.isEmpty())
            {
                teleportingLord = args.remove(0);
                if (teleportingLord.equals("null"))
                {
                    teleportingLord = null;
                }
            }
            boolean splitLegionHasForcedMove = false;
            // servers from older versions might not send this arg
            if (!args.isEmpty())
            {
                splitLegionHasForcedMove = Boolean.valueOf(args.remove(0))
                    .booleanValue();
            }
            client
                .didMove(resolveLegion(markerId),
                    resolveHex(startingHexLabel), resolveHex(currentHexLabel),
                    EntrySide.fromLabel(entrySideLabel), teleport,
                    resolveCreatureType(teleportingLord),
                    splitLegionHasForcedMove);
        }
        else if (method.equals(Constants.undidMove))
        {
            String markerId = args.remove(0);
            String formerHexLabel = args.remove(0);
            String currentHexLabel = args.remove(0);
            boolean splitLegionHasForcedMove = false;
            // servers from older versions might not send this arg
            if (!args.isEmpty())
            {
                splitLegionHasForcedMove = Boolean.valueOf(args.remove(0))
                    .booleanValue();
            }
            client.undidMove(resolveLegion(markerId),
                resolveHex(formerHexLabel), resolveHex(currentHexLabel),
                splitLegionHasForcedMove);
        }
        else if (method.equals(Constants.didSummon))
        {
            String summonerId = args.remove(0);
            String donorId = args.remove(0);
            String summon = args.remove(0);
            client.didSummon(resolveLegion(summonerId),
                resolveLegion(donorId), resolveCreatureType(summon));
        }
        else if (method.equals(Constants.undidSplit))
        {
            String splitoffId = args.remove(0);
            String survivorId = args.remove(0);
            int turn = Integer.parseInt(args.remove(0));
            client.undidSplit(resolveLegion(splitoffId),
                resolveLegion(survivorId), turn);
        }
        else if (method.equals(Constants.didSplit))
        {
            String hexLabel = args.remove(0);
            String parentId = args.remove(0);
            String childId = args.remove(0);
            int childHeight = Integer.parseInt(args.remove(0));
            List<CreatureType> splitoffs = resolveCreatureTypes(args.remove(0));
            int turn = Integer.parseInt(args.remove(0));
            // create client-side copy of new legion
            MasterHex hex = resolveHex(hexLabel);
            Legion parentLegion = resolveLegion(parentId);
            Player player = parentLegion.getPlayer();
            Legion newLegion = new LegionClientSide(player, childId, hex);
            player.addLegion(newLegion);
            client.didSplit(hex, parentLegion, newLegion, childHeight,
                splitoffs, turn);
        }
        else if (method.equals(Constants.askPickColor))
        {
            rememberEvent(event);
            List<String> clList = Split.split(Glob.sep, args.remove(0));
            List<PlayerColor> colorsLeft = new ArrayList<PlayerColor>();
            for (String colorName : clList)
            {
                colorsLeft.add(PlayerColor.getByName(colorName));
            }
            client.askPickColor(colorsLeft);
        }
        else if (method.equals(Constants.askPickFirstMarker))
        {
            rememberEvent(event);
            client.askPickFirstMarker();
        }
        else if (method.equals(Constants.log))
        {
            if (!args.isEmpty())
            {
                String message = args.remove(0);
                client.log(message);
            }
        }
        else if (method.equals(Constants.nak))
        {
            String reason = args.remove(0);
            String message = args.remove(0);
            // NOTE: nak for SignOn is already handled in SCT at the moment...
            client.nak(reason, message);

        }
        else if (method.equals(Constants.boardActive))
        {
            boolean val = Boolean.valueOf(args.remove(0)).booleanValue();
            client.setBoardActive(val);
        }
        else if (method.equals(Constants.tellEngagement))
        {
            client.tellEngagement(resolveHex(args.remove(0)),
                client.getLegion(args.remove(0)),
                client.getLegion(args.remove(0)));
        }
        else if (method.equals(Constants.tellEngagementResults))
        {
            String winnerId = args.remove(0);
            String resMethod = args.remove(0);
            int points = Integer.parseInt(args.remove(0));
            int turns = Integer.parseInt(args.remove(0));
            Legion legion;
            if (winnerId.equals("null"))
            {
                legion = null;
            }
            else
            {
                legion = resolveLegion(winnerId);
            }
            client.tellEngagementResults(legion, resMethod, points, turns);
        }

        else if (method.equals(Constants.tellWhatsHappening))
        {
            String message = args.remove(0);
            client.tellWhatsHappening(message);
        }

        // a popup message
        else if (method.equals(Constants.messageFromServer))
        {
            String message = args.remove(0);
            client.messageFromServer(message);
        }

        // just written to log (which might become visible by itself if needed)
        else if (method.equals(Constants.appendToConnectionLog))
        {
            String message = args.remove(0);
            client.appendToConnectionLog(message);
        }

        else if (method.equals(Constants.syncCompleted))
        {
            int syncRequestNr = Integer.parseInt(args.remove(0));
            client.tellSyncCompleted(syncRequestNr);
        }

        else if (method.equals(Constants.requestExtraRollApproval))
        {
            String requestorName = args.remove(0);
            int requestId = Integer.parseInt(args.remove(0));
            client.requestExtraRollApproval(requestorName, requestId);
        }

        else if (method.equals(Constants.askSuspendConfirmation))
        {
            String requestorName = args.remove(0);
            int timeout = Integer.parseInt(args.remove(0));
            client.askSuspendConfirmation(requestorName, timeout);
        }

        else if (method.equals(Constants.askConfirmCatchUp))
        {
            client.confirmWhenCaughtUp();
        }

        else if (method.equals(Constants.serverConnectionOK))
        {
            LOGGER.info("Received server connection OK message from server "
                + "for player " + getNameMaybe());
            client.serverConfirmsConnection();
        }

        else if (method.equals(Constants.relayedPeerRequest))
        {
            String requestingClientName = args.remove(0);
            client.relayedPeerRequest(requestingClientName);
        }

        else if (method.equals(Constants.relayBackReceivedMsg))
        {
            String respondingClientName = args.remove(0);
            int queueLen = Integer.parseInt(args.remove(0));
            LOGGER.info("In client " + getNameMaybe()
                + ", got back 'Received' message from client "
                + respondingClientName);
            client.peerRequestReceivedBy(respondingClientName, queueLen);
        }

        else if (method.equals(Constants.relayBackProcessedMsg))
        {
            String respondingClientName = args.remove(0);
            LOGGER.info("In client " + getNameMaybe()
                + ", got back 'Processed' message from client "
                + respondingClientName);
            client.peerRequestProcessedBy(respondingClientName);
        }

        else
        {
            LOGGER.log(Level.SEVERE, "Bogus packet (Client, method: '"
                + method + "', args: " + args + ")");
        }
        LOGGER.finest("Client '" + getNameMaybe()
            + "' finished method processing");
    }

    private void showDebugOutputMaybe(String method, List<String> args)
    {
        if (!_DEBUG_MSGS)
        {
            return;
        }

        Player p = client.getOwningPlayer();
        if (p == null)
        {
            LOGGER.warning("No owning player? Skipping debug output.");
            return;
        }

        String allArgs = Glob.glob(", ", args);

        boolean show = true;

        if (method.startsWith("setOption") && allArgs.startsWith("ViewMode"))
        {
            // show this as the only one
        }
        else if (method.startsWith(Constants.updateCreatureCount)
            // || method.startsWith(Constants.setLegionStatus)
            // || method.startsWith(Constants.tellLegionLocation)
            || method.startsWith(Constants.serverConnectionOK)
            || method.startsWith(Constants.relayBackProcessedMsg)
            || method.startsWith(Constants.relayBackReceivedMsg)
            // || method.startsWith(Constants.)
            || method.startsWith(Constants.pingRequest)
            || method.startsWith(Constants.syncOption) // setOption
        )
        {
            show = false;
        }

        if (!show)
        {
            return;
        }

        String name = p.getName();
        if (name.equals("remote") || name.equals("spectator"))
        {
            String indent = (client.isReplayOngoing() ? "  " : "")
                + (client.isRedoOngoing() ? "  " : "");
            // System.out.println(indent + "!!!" + method + ":" + allArgs);

            String printLine = allArgs;
            int _MAXLEN = 20;
            int len = allArgs.length();
            if (len > _MAXLEN)
            {
                printLine = allArgs.substring(0, _MAXLEN) + "...";
            }

            if (method.startsWith(Constants.setupTurnState))
            {
                System.out.println("\n\n");
            }

            else if (client.getGame().getTurnNumber() >= 1)
            {
                // WhatNextManager.sleepFor(2000);
            }

            System.out.println(indent + "!!!" + method + ": " + printLine);
        }
    }

    private void rememberEvent(ServerEvent event)
    {
        if (!client.needsWatchdog())
        {
            return;
        }

        String argsList = Glob.glob("::", event.getArgs());
        if (event.getIsRetriggered())
        {
            LOGGER.finest("NOT Storing retriggered event "
                + event.getMethod() + "'" + argsList + "'");
        }
        else
        {
            LOGGER.finest("\n\nStoring event " + event.getMethod() + "'"
                + argsList + "'");
            lastInactivityRelevantEvent = event;
        }

    }

    private MasterHex resolveHex(String label)
    {
        MasterHex hexByLabel = client.getGame().getVariant().getMasterBoard()
            .getHexByLabel(label);
        assert hexByLabel != null : "Client got unknown hex label '" + label
            + "' from server";
        return hexByLabel;
    }

    private BattleHex resolveBattleHex(String hexLabel)
    {
        return client.getGame().getBattleSite().getTerrain()
            .getHexByLabel(hexLabel);
    }

    private List<CreatureType> resolveCreatureTypes(String nameList)
    {
        List<CreatureType> creatures = new ArrayList<CreatureType>();
        if (!nameList.equals(""))
        {
            List<String> names = Split.split(Glob.sep, nameList);
            for (String creatureName : names)
            {
                creatures.add(resolveCreatureType(creatureName));
            }
        }
        return creatures;
    }

    private CreatureType resolveCreatureType(String creatureName)
    {
        if ((creatureName == null) || (creatureName.equals("null")))
        {
            return null;
        }
        CreatureType creatureByName = client.getGame().getVariant()
            .getCreatureByName(creatureName);
        assert creatureByName != null : "Client got unknown creature name '"
            + creatureName + "' from server";
        return creatureByName;
    }

    private Legion resolveLegion(String markerId)
    {
        Legion legion = client.getLegion(markerId);

        // it's not just "unknown" - it might also be at any point during
        // the replay of a loaded game that there is no legion for that
        // marker *at that particular moment*.
        // Whereas delayed painting in EDT may cause similar error, they
        // should be carefully distincted.

        if (legion == null)
        {
            // If such a thing happens, there is something seriously wrong,
            // so I don't see a use in continuing the game.
            // This I call it severe, and log it always,
            // not just an assertion.
            LOGGER.severe("CT ResolveLegion for " + markerId + " in client "
                + getNameMaybe() + " gave null!");
        }

        // Peter made this assertion, I guess...
        assert legion != null : "ClientThread.resolveLegion(" + markerId
            + " in client of player " + getNameMaybe() + " returned null!";

        return legion;
    }

    public static long getNow()
    {
        return new Date().getTime();
    }

    public static class ServerEvent
    {
        private final long received;
        private final long enqueued;
        private long executionStarted;
        private long executionCompleted;

        private final String method;
        private final List<String> args;

        private boolean isRetriggered = false;

        public ServerEvent(long received, String method, List<String> args)
        {
            this.received = received;
            this.enqueued = ClientThread.getNow();
            this.method = method;
            this.args = new ArrayList<String>(args);
        }

        public String getMethod()
        {
            return method;
        }

        public void setIsRetriggered()
        {
            isRetriggered = true;
        }

        public boolean getIsRetriggered()
        {
            return isRetriggered;
        }

        /* Returns now a LinkedList, instead of the original list
         * which was an ArrayList.
         * We return a copy, so that processing the args does not
         * leave an empty arg list, in case we need to retrigger it
         * (too long inactivity).
         * LinkedList suits actually better for processing, since
         * the argument deserialisation use remove(0).
         */
        public List<String> getArgs()
        {
            return new LinkedList<String>(args);
        }

        public void executionStarts(long when)
        {
            this.executionStarted = when;
        }

        public long getExecutionStarted()
        {
            return this.executionStarted;
        }

        public void executionCompleted(long when)
        {
            this.executionCompleted = when;
        }

        public long getExecutionCompleted()
        {
            return this.executionCompleted;
        }

        public void logProcessing()
        {
            long enqueuing = enqueued - received;
            long execution = executionCompleted - executionStarted;
            long inQueue = executionStarted - enqueued;
            long processing = executionCompleted - received;

            Level loglevel;
            if (processing > 5000)
            {
                loglevel = Level.WARNING;
            }
            else
            {
                loglevel = Level.FINEST;
            }
            ClientThread thisThread = (ClientThread)Thread.currentThread();
            LOGGER.log(loglevel, "Event " + method + " in thread #"
                + thisThread.getThreadNumber() + " received at " + received
                + ": overall processing took: " + processing
                + "ms (enqueuing took " + enqueuing + "ms, inQueue " + inQueue
                + "ms, processing took " + execution + "ms)");
        }
    }
}
