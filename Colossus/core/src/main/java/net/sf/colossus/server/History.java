package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.actions.AddCreatureAction;
import net.sf.colossus.game.actions.Recruitment;
import net.sf.colossus.util.Glob;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;

import org.jdom.Element;


/**
 * Stores game history as XML.
 *
 * @author David Ripton
 */
public class History
{
    private static final Logger LOGGER = Logger.getLogger(History.class
        .getName());

    /**
     * History: events that happened before last commit point
     */
    private final Element root;

    /**
     * History elements/events that happened since the last commit/"snapshot".
     */
    private final List<Element> recentEvents = new LinkedList<Element>();

    /**
     * Set to true during the processing of {@link #fireEventsFromXML(Server)}
     * to avoid triggering events we just restored again.
     */
    private boolean loading = false;

    /**
     *
     */
    private final Element loadedRedoLog;

    private boolean isRedo = false;

    /**
     * Stores the surviving legions (this variable is not needed any more)
     *
     * While the history should contain all information to reproduce the game
     * state, the last set of legions is currently still loaded upfront since
     * they contain the battle-specific information. This collides with
     * replaying the game from history...
     * Now, since 08/2008, they are not stored as "survivorlegions" any more.
     * Instead, they are backed up internally (done inside PlayerServerSide),
     * all the history is replayed. This creates proper split prediction data
     * in all clients. After that, backup data is compared with result of
     * replay.
     * E.g. Legion count, their content, players eliminated must be in sync.
     * Then the replayed ones are discarded and the backedup ones restored
     * - which have the right legion state (moved, donor, summoned, ...)
     *
     * TODO align the history replay more with the original gameplay so we
     *      don't need this anymore;
     *      08/2008:==> this is now to some part done. Still replay
     *      events could be closer to original events (split, summon,
     *      acquire, teleport, ...) , not just the "result" of that
     *      event (reveal,add,remove effects).
     *
     * TODO instead: model the actual events instead of just result,
     * or at least add relevant info to history elements, so that all
     * replayed events carry all needed data so that they could also be
     * processed by event viewer (currently EV does not process anything
     * during replay).
     */

    public History()
    {
        root = new Element("History");
        // Dummy:
        loadedRedoLog = new Element("LoadedRedoLog");
    }

    /**
     * Constructor used by "LoadGame"
     */
    public History(Element loadGameRoot)
    {
        // Get the history elements and store them to "root"
        root = (Element)loadGameRoot.getChild("History").clone();

        // Get the redo log content
        loadedRedoLog = (Element)loadGameRoot.getChild("Redo").clone();
    }

    /**
     *  All events before last commit
     */
    Element getCopy()
    {
        return (Element)root.clone();
    }

    /**
     * Reached a commit point: append all recent events to the history,
     * clear list of recent events; caller should do this together with creating
     * the next snapshot.
     */
    void flushRecentToRoot()
    {
        for (Element el : recentEvents)
        {
            el.detach();
            String name = el.getName();
            // TODO later, when this are proper events (not XML elements),
            // ask rather from the Event whether it belongs copied to
            // history or not.
            // TODO At some point in future, put also those Move events
            // that reveal something to history, and make either the history
            // replay only send the relevant reveal messages, or make the
            // Clients during replay (but not redo part) ignore the "move"
            // and just process the revealing part.
            // Preferrably the latter, so that proper events show up in the
            // EventViewer.
            if (name.equals("Move") || name.equals("UndoMove"))
            {
                LOGGER.finest("Flush Redo to History: skipping " + name);
            }
            else if (name.equals("Recruit") || name.equals("UndoRecruit"))
            {
                // Skipping for now, because there are also the addCreature,
                // removeCreature and reveal Events still in history.
                // TODO make the Recruit/UndoRecruit history events during
                // replay properly, get rid of the "side effect" type of
                // entries in save game.
                LOGGER.finest("Flush Redo to History: skipping " + name);
            }
            else
            {
                root.addContent(el);
            }
        }
        recentEvents.clear();
    }

    /**
     *  @return A Redo Element, containing all events since last commit
     *  i.e. which need to be REDOne on top of last commit point/snapshot
     */
    Element getNewRedoLogElement()
    {
        Element redoLogElement = new Element("Redo");
        for (Element el : recentEvents)
        {
            el.detach();
            redoLogElement.addContent(el);
        }

        return redoLogElement;
    }

    /**
     * TODO reconsider name
     * TODO decide if we should move it all into one big handleEvent(GameEvent) method
     */
    void addCreatureEvent(AddCreatureAction event, int turn, String reason)
    {
        if (loading)
        {
            return;
        }
        Element element = new Element("AddCreature");
        element.setAttribute("markerId", event.getLegion().getMarkerId());
        element.setAttribute("creatureName", event.getAddedCreatureType()
            .getName());
        element.setAttribute("turn", "" + turn);
        element.setAttribute("reason", reason);
        recentEvents.add(element);
    }

    void removeCreatureEvent(Legion legion, CreatureType creature, int turn,
        String reason)
    {
        if (loading)
        {
            return;
        }
        Element event = new Element("RemoveCreature");
        event.setAttribute("markerId", legion.getMarkerId());
        event.setAttribute("creatureName", creature.getName());
        event.setAttribute("turn", "" + turn);
        event.setAttribute("reason", reason);
        recentEvents.add(event);
    }

    void relocateLegionEvent(Legion legion)
    {
        Element event = new Element("RelocateLegion");
        event.setAttribute("markerId", legion.getMarkerId());
        event.setAttribute("destination", legion.getCurrentHex().getLabel());
        recentEvents.add(event);
    }

    void splitEvent(Legion parent, Legion child, List<CreatureType> splitoffs,
        int turn)
    {
        if (loading)
        {
            return;
        }
        Element event = new Element("Split");
        event.setAttribute("parentId", parent.getMarkerId());
        event.setAttribute("childId", child.getMarkerId());
        event.setAttribute("turn", "" + turn);
        Element creatures = new Element("splitoffs");
        event.addContent(creatures);
        for (CreatureType creatureType : splitoffs)
        {
            Element cr = new Element("creature");
            cr.addContent(creatureType.getName());
            creatures.addContent(cr);
        }
        recentEvents.add(event);
    }

    void mergeEvent(String splitoffId, String survivorId, int turn)
    {
        if (loading)
        {
            return;
        }
        Element event = new Element("Merge");
        event.setAttribute("splitoffId", splitoffId);
        event.setAttribute("survivorId", survivorId);
        event.setAttribute("turn", "" + turn);
        recentEvents.add(event);
    }

    void revealEvent(boolean allPlayers, List<Player> players, Legion legion,
        List<CreatureType> creatures, int turn, String reason)
    {
        if (loading)
        {
            return;
        }
        if (creatures.isEmpty())
        {
            // this happens e.g. when in final battle (titan vs. titan)
            // angel was called out of legion which was then empty,
            // and in the final updateAllLegionContents there is then
            // this empty legion...
            // TODO if this case can happen in a regular game no warning
            // should be logged
            LOGGER.log(Level.WARNING, "Called revealEvent(" + allPlayers
                + ", " + (players != null ? players.toString() : "-null-")
                + ", " + legion + ", " + creatures.toString() + ", " + turn
                + ") with empty creatureNames");
            return;
        }
        Element event = new Element("Reveal");
        event.setAttribute("markerId", legion.getMarkerId());
        event.setAttribute("allPlayers", "" + allPlayers);
        event.setAttribute("turn", "" + turn);
        event.setAttribute("reason", reason);
        if (!allPlayers)
        {
            Element viewers = new Element("viewers");
            event.addContent(viewers);
            Iterator<Player> it = players.iterator();
            while (it.hasNext())
            {
                String playerName = it.next().getName();
                Element viewer = new Element("viewer");
                viewer.addContent(playerName);
                viewers.addContent(viewer);
            }
        }
        Element creaturesElem = new Element("creatures");
        event.addContent(creaturesElem);
        for (CreatureType creatureType : creatures)
        {
            Element creatureElem = new Element("creature");
            creatureElem.addContent(creatureType.getName());
            creaturesElem.addContent(creatureElem);
        }
        recentEvents.add(event);
    }

    void playerElimEvent(Player player, Player slayer, int turn)
    {
        if (loading)
        {
            return;
        }
        Element event = new Element("PlayerElim");
        event.setAttribute("name", player.getName());
        if (slayer != null)
        {
            event.setAttribute("slayer", slayer.getName());
        }
        event.setAttribute("turn", "" + turn);
        recentEvents.add(event);
    }

    void movementRollEvent(Player player, int roll)
    {
        if (loading)
        {
            return;
        }

        Element event = new Element("MovementRoll");
        event.setAttribute("playerName", player.getName());
        event.setAttribute("roll", "" + roll);
        recentEvents.add(event);
    }

    void legionMoveEvent(Legion legion, MasterHex newHex, EntrySide entrySide,
        boolean teleport, CreatureType lord)
    {
        if (loading)
        {
            return;
        }

        Element event = new Element("Move");
        event.setAttribute("markerId", legion.getMarkerId());
        event.setAttribute("newHex", newHex.getLabel());
        event.setAttribute("entrySide", entrySide.getLabel());
        event.setAttribute("teleport", "" + teleport);
        String creNameOrTextNull = lord == null ? "null" : lord.getName();
        event.setAttribute("revealedLord", creNameOrTextNull);
        recentEvents.add(event);
    }

    void legionUndoMoveEvent(Legion legion)
    {
        if (loading)
        {
            return;
        }

        Element event = new Element("UndoMove");
        event.setAttribute("markerId", legion.getMarkerId());
        recentEvents.add(event);
    }

    void recruitEvent(Legion legion, CreatureType recruit,
        CreatureType recruiter)
    {
        if (loading)
        {
            return;
        }

        Element event = new Element("Recruit");
        event.setAttribute("markerId", legion.getMarkerId());
        event.setAttribute("recruit", recruit.getName());
        event.setAttribute("recruiter",
            recruiter == null ? "null" : recruiter.getName());

        recentEvents.add(event);
    }

    void undoRecruitEvent(Legion legion)
    {
        if (loading)
        {
            return;
        }

        Element event = new Element("UndoRecruit");
        event.setAttribute("markerId", legion.getMarkerId());

        recentEvents.add(event);
    }

    /**
     * Fire all events from redoLog.
     * Elements from RedoLog are processed one by one and the corresponding
     * method is called on the Server object, pretty much as if a
     * ClientHandler would call it when receiving such a request from Client.
     * Note that in some cases overriding the processingCH is necessary
     * (because technically, this all currently happens while still the
     * connecting of last joining player is processed, so processingCH is
     * set to his ClientHandler).
     *
     * Note that "loading" is not set to true, so they DO GET ADDED to the
     * recentEvents list again.
     *
     * @param server The server on which to call all the actions to be redone
     */
    void processRedoLog(Server server)
    {
        assert loadedRedoLog != null : "Loaded RedoLog should always "
            + "have a JDOM root element as backing store";

        LOGGER.info("History: Start processing redo log");
        isRedo = true;
        for (Object obj : loadedRedoLog.getChildren())
        {
            Element el = (Element)obj;
            LOGGER.info("processing redo event " + el.getName());
            fireEventFromElement(server, el);
        }
        isRedo = false;
        // TODO clear loadedRedoLog?
        LOGGER.info("Completed processing redo log");
    }

    // unchecked conversions from JDOM
    @SuppressWarnings("unchecked")
    void fireEventsFromXML(Server server)
    {
        this.loading = true;
        assert root != null : "History should always have a "
            + " JDOM root element as backing store";

        List<Element> kids = root.getChildren();
        Iterator<Element> it = kids.iterator();
        while (it.hasNext())
        {
            Element el = it.next();
            fireEventFromElement(server, el);
        }
        this.loading = false;
    }

    // unchecked conversions from JDOM
    @SuppressWarnings("unchecked")
    void fireEventFromElement(Server server, Element el)
    {
        GameServerSide game = server.getGame();
        String eventName = el.getName();
        String reasonPerhaps = el.getAttributeValue("reason");
        String reason = (reasonPerhaps != null && !reasonPerhaps
            .equals("null")) ? reasonPerhaps : "<undefinedReason>";
        if (eventName.equals("Reveal") && isRedo
            && reason.equals(Constants.reasonRecruiter))
        {
            // Skip this because we redo the full recruit event
            // TODO
            LOGGER.finest("Skipping Reveal event (reason " + reason
                + ") during redo.");
        }
        else if (eventName.equals("AddCreature") && isRedo
            && reason.equals(Constants.reasonRecruited))
        {
            // Skip this because we redo the full recruit event
            LOGGER.finest("Skipping AddCreature event (reason " + reason
                + ") during redo.");
        }
        else if (eventName.equals("RemoveCreature") && isRedo
            && reason.equals(Constants.reasonRecruited))
        {
            // Skip this because we redo the full recruit event
            LOGGER.finest("Skipping RemoveCreature event (reason " + reason
                + ") during redo.");
        }
        else if (eventName.equals("Reveal"))
        {
            String allPlayers = el.getAttributeValue("allPlayers");
            boolean all = allPlayers != null && allPlayers.equals("true");
            String markerId = el.getAttributeValue("markerId");
            List<String> playerNames = new ArrayList<String>();
            Element viewEl = el.getChild("viewers");
            int turn = Integer.parseInt(el.getAttributeValue("turn"));
            String playerName = null;
            if (viewEl != null)
            {
                List<Element> viewers = viewEl.getChildren();
                Iterator<Element> it = viewers.iterator();
                while (it.hasNext())
                {
                    Element viewer = it.next();
                    playerName = viewer.getTextNormalize();
                    playerNames.add(playerName);
                }
            }
            List<Element> creatureElements = el.getChild("creatures")
                .getChildren();
            List<CreatureType> creatures = new ArrayList<CreatureType>();
            for (Element creature : creatureElements)
            {
                String creatureName = creature.getTextNormalize();
                creatures.add(game.getVariant()
                    .getCreatureByName(creatureName));
            }
            Player player = game.getPlayerByMarkerId(markerId);
            Legion legion;

            if (turn == 1 && player.getLegionByMarkerId(markerId) == null)
            {
                // there is no create event for the startup legions,
                // so we might need to create them for the reveal event
                legion = new LegionServerSide(markerId, null,
                    player.getStartingTower(), player.getStartingTower(),
                    player, game, creatures.toArray(new CreatureType[creatures
                        .size()]));
                player.addLegion(legion);
            }
            else
            {
                legion = player.getLegionByMarkerId(markerId);
            }
            // TODO Now we get the reason from history element - does this
            // change effect/break anything?
            // String reason = "<unknown>";
            if (((PlayerServerSide)player).getDeadBeforeSave())
            {
                // Skip for players that will be dead by end of replay
            }
            else if (all)
            {
                server.allRevealCreatures(legion, creatures, reason);
            }
            else
            {
                server.oneRevealLegion(game.getPlayerByName(playerName),
                    legion, creatures, reason);
            }
        }
        else if (eventName.equals("Split"))
        {
            String parentId = el.getAttributeValue("parentId");
            String childId = el.getAttributeValue("childId");
            String turnString = el.getAttributeValue("turn");
            int turn = Integer.parseInt(turnString);
            List<String> creatureNames = new ArrayList<String>();
            List<CreatureType> creatures = new ArrayList<CreatureType>();
            List<Element> splitoffs = el.getChild("splitoffs").getChildren();
            Iterator<Element> it = splitoffs.iterator();
            while (it.hasNext())
            {
                Element creature = it.next();
                String creatureName = creature.getTextNormalize();
                creatureNames.add(creatureName);
                creatures.add(game.getVariant()
                    .getCreatureByName(creatureName));
            }
            LegionServerSide parentLegion = game.getLegionByMarkerId(parentId);

            if (isRedo)
            {
                server.overrideProcessingCH(parentLegion.getPlayer());
                server.doSplit(parentLegion, childId, creatures);
                server.overrideProcessingCH(parentLegion.getPlayer());
                return;
            }

            // LegionServerSide.split(..) doesn't like us here since the parent
            // legion can't remove creatures (not there?) -- create child directly
            // instead
            PlayerServerSide player = parentLegion.getPlayer();
            LegionServerSide childLegion;
            if (player.hasLegion(childId))
            {
                childLegion = game.getLegionByMarkerId(childId);
                LOGGER.severe("During replay of history: child legion "
                    + childId + " should not " + "exist yet (turn=" + turn
                    + ")!!\n" + "Exists already with: "
                    + Glob.glob(",", childLegion.getCreatureTypes()) + " but "
                    + "should now be created with creatures: " + creatures);

                childLegion.remove();
            }

            childLegion = new LegionServerSide(childId, null,
                parentLegion.getCurrentHex(), parentLegion.getCurrentHex(),
                player, game, creatures.toArray(new CreatureType[creatures
                    .size()]));

            player.addLegion(childLegion);

            for (CreatureType creature : creatures)
            {
                parentLegion.removeCreature(creature, false, false);
            }

            // Skip for players that will be dead by end of replay
            if (!player.getDeadBeforeSave())
            {
                server.allTellDidSplit(parentLegion, childLegion, turn, false);
            }
        }
        else if (eventName.equals("Merge"))
        {
            String splitoffId = el.getAttributeValue("splitoffId");
            String survivorId = el.getAttributeValue("survivorId");
            String turnString = el.getAttributeValue("turn");
            int turn = Integer.parseInt(turnString);

            LegionServerSide splitoff = game.getLegionByMarkerId(splitoffId);
            LegionServerSide survivor = game.getLegionByMarkerId(survivorId);

            // Skip for players that will be dead by end of replay
            if (!survivor.getPlayer().getDeadBeforeSave())
            {
                server.undidSplit(splitoff, survivor, false, turn);
            }
            // Add them back to parent:
            while (splitoff.getHeight() > 0)
            {
                CreatureType type = splitoff.removeCreature(0, false, false);
                survivor.addCreature(type, false);
            }
            splitoff.remove(false, false);
        }
        else if (eventName.equals("AddCreature"))
        {
            String markerId = el.getAttributeValue("markerId");
            String creatureName = el.getAttributeValue("creatureName");
            // TODO Now we get the reason from history element - does this
            // change effect/break anything?
            // String reason = "<unknown>";
            LOGGER.finer("Adding creature '" + creatureName
                + "' to legion with markerId '" + markerId + "', reason '"
                + reason + "'");
            LegionServerSide legion = game.getLegionByMarkerId(markerId);
            CreatureType creatureType = game.getVariant().getCreatureByName(
                creatureName);
            legion.addCreature(creatureType, false);
            // Skip for players that will be dead by end of replay
            if (!legion.getPlayer().getDeadBeforeSave())
            {
                boolean doHistory = (reason.equals(Constants.reasonEdit));
                server.allTellAddCreature(new AddCreatureAction(legion,
                    creatureType), doHistory, reason);
            }
            LOGGER.finest("Legion '" + markerId + "' now contains "
                + legion.getCreatures());
        }
        else if (eventName.equals("RemoveCreature"))
        {
            String markerId = el.getAttributeValue("markerId");
            String creatureName = el.getAttributeValue("creatureName");
            // TODO Now we get the reason from history element - does this
            // change effect/break anything?
            // String reason = "<unknown>";
            LOGGER.finer("Removing creature '" + creatureName
                + "' from legion with markerId '" + markerId + "', reason '"
                + reason + "'");
            LegionServerSide legion = game.getLegionByMarkerId(markerId);
            if (legion == null)
            {
                LOGGER.warning("removeCreature " + creatureName
                    + " from legion " + markerId + ", legion is null");
                return;
            }
            else
            {
                List<? extends Creature> cres = legion.getCreatures();
                List<String> crenames = new ArrayList<String>();
                for (Creature c : cres)
                {
                    crenames.add(c.getName());
                }
            }

            // don't use disbandIfEmpty parameter since that'll fire another history event
            CreatureType removedCritter = legion.removeCreature(game
                .getVariant().getCreatureByName(creatureName), false, false);

            // Skip for players that will be dead by end of replay
            // Skip if removedCritter is null => removeCreature did not find it,
            // so there is something wrong with the save game. No use to bother
            // all the clients with it.
            if (removedCritter != null
                && !legion.getPlayer().getDeadBeforeSave())
            {
                boolean doHistory = (reason.equals(Constants.reasonEdit));
                server.allTellRemoveCreature(legion, removedCritter,
                    doHistory, reason);
            }
            LOGGER.finest("Legion '" + markerId + "' now contains "
                + legion.getCreatures());
            if (legion.getHeight() == 0)
            {
                legion.remove(false, false);
                LOGGER.finer("Legion '" + markerId + "' removed");
            }
        }

        else if (eventName.equals("RelocateLegion"))
        {
            String markerId = el.getAttributeValue("markerId");
            String hexLabel = el.getAttributeValue("destination");
            // Other events come via server from client, and history replay does
            // the same. This one here came direct via localServer to game, and
            // thus we keep it the same.
            game.editModeRelocateLegion(markerId, hexLabel);
        }

        else if (eventName.equals("PlayerElim"))
        {
            String playerName = el.getAttributeValue("name");
            String slayerName = el.getAttributeValue("slayer");
            Player player = game.getPlayerByName(playerName);
            Player slayer = game.getPlayerByNameIgnoreNull(slayerName);
            // Record the slayer and give him this player's legion markers.
            if (slayer != null)
            {
                ((PlayerServerSide)player).handleSlaying(slayer);
            }
            player.setDead(true);
            server.allUpdatePlayerInfo("FireEvent-PlayerElim");
            server.allTellPlayerElim(player, slayer, false);
        }
        else if (eventName.equals("MovementRoll"))
        {
            String playerName = el.getAttributeValue("playerName");
            Player player = game.getPlayerByName(playerName);
            int roll = Integer.parseInt(el.getAttributeValue("roll"));

            ((PlayerServerSide)player).setMovementRoll(roll);
            game.movementRollEvent(player, roll);
            server.allTellMovementRoll(roll, null);
        }
        else if (eventName.equals("Move"))
        {
            String markerId = el.getAttributeValue("markerId");
            String lordName = el.getAttributeValue("revealedLord");
            String tele = el.getAttributeValue("teleport");
            String newHexLabel = el.getAttributeValue("newHex");
            String entrySideName = el.getAttributeValue("entrySide");

            LegionServerSide legion = game.getLegionByMarkerId(markerId);
            CreatureType revealedLord = lordName.equals("null") ? null : game
                .getVariant().getCreatureByName(lordName);
            MasterHex newHex = server.getGame().getVariant().getMasterBoard()
                .getHexByLabel(newHexLabel);
            EntrySide entrySide = EntrySide.fromLabel(entrySideName);
            boolean teleport = tele != null && tele.equals("true");
            LOGGER.finest("Legion Move redo event: \n" + " marker " + markerId
                + ", lordName " + revealedLord + " teleported " + teleport
                + " to hex " + newHex.getLabel() + " entrySide "
                + entrySide.toString());
            server.overrideProcessingCH(legion.getPlayer());
            server.doMove(legion, newHex, entrySide, teleport, revealedLord);
            server.restoreProcessingCH();
        }
        else if (eventName.equals("UndoMove"))
        {
            String markerId = el.getAttributeValue("markerId");
            LegionServerSide legion = game.getLegionByMarkerId(markerId);
            LOGGER.finest("Legion Undo Move redo event: \n" + " marker "
                + markerId);
            server.overrideProcessingCH(legion.getPlayer());
            server.undoMove(legion);
            server.restoreProcessingCH();
        }
        else if (eventName.equals("Recruit"))
        {
            String markerId = el.getAttributeValue("markerId");
            String recruitName = el.getAttributeValue("recruit");
            String recruiterName = el.getAttributeValue("recruiter");
            LegionServerSide legion = game.getLegionByMarkerId(markerId);
            CreatureType recruit = game.getVariant().getCreatureByName(
                recruitName);
            CreatureType recruiter = recruiterName.equals("null") ? null
                : game.getVariant().getCreatureByName(recruiterName);

            LOGGER.finest("Recruit redo event: \n" + " marker " + markerId
                + " recruit " + recruit + " recruiter " + recruiter);
            server.overrideProcessingCH(legion.getPlayer());
            server.doRecruit(new Recruitment(legion, recruit, recruiter));
            server.restoreProcessingCH();
        }
        else if (eventName.equals("UndoRecruit"))
        {
            String markerId = el.getAttributeValue("markerId");
            LegionServerSide legion = game.getLegionByMarkerId(markerId);
            LOGGER
                .finest("UndoRecruit redo event: \n" + " marker " + markerId);
            server.overrideProcessingCH(legion.getPlayer());
            server.undoRecruit(legion);
            server.restoreProcessingCH();
        }
        else
        {
            LOGGER.warning("Unknown Redo element " + eventName);
        }
    }

}
