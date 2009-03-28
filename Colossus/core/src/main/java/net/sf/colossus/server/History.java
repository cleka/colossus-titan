package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;

import org.jdom.Element;


/**
 * Stores game history as XML.
 * @version $Id$
 * @author David Ripton
 */
public class History
{
    private static final Logger LOGGER = Logger.getLogger(History.class
        .getName());

    private Element root = new Element("History");

    /**
     * Set to true during the processing of {@link #fireEventsFromXML(Server)}
     * to avoid triggering events we just restored again.
     */
    private boolean loading = false;

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

    Element getCopy()
    {
        return (Element)root.clone();
    }

    void addCreatureEvent(Legion legion, String creatureName, int turn)
    {
        if (loading)
        {
            return;
        }
        Element event = new Element("AddCreature");
        event.setAttribute("markerId", legion.getMarkerId());
        event.setAttribute("creatureName", creatureName);
        event.setAttribute("turn", "" + turn);
        root.addContent(event);
    }

    void removeCreatureEvent(Legion legion, String creatureName, int turn)
    {
        if (loading)
        {
            return;
        }
        Element event = new Element("RemoveCreature");
        event.setAttribute("markerId", legion.getMarkerId());
        event.setAttribute("creatureName", creatureName);
        event.setAttribute("turn", "" + turn);
        root.addContent(event);
    }

    void splitEvent(Legion parent, Legion child, List<String> splitoffs,
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
        Iterator<String> it = splitoffs.iterator();
        while (it.hasNext())
        {
            String creatureName = it.next();
            Element cr = new Element("creature");
            cr.addContent(creatureName);
            creatures.addContent(cr);
        }
        root.addContent(event);
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
        root.addContent(event);
    }

    void revealEvent(boolean allPlayers, List<String> playerNames,
        Legion legion, List<String> creatureNames, int turn)
    {
        if (loading)
        {
            return;
        }
        if (creatureNames.isEmpty())
        {
            // this happens e.g. when in final battle (titan vs. titan)
            // angel was called out of legion which was then empty,
            // and in the final updateAllLegionContents there is then
            // this empty legion...
            // TODO if this case can happen in a regular game no warning
            // should be logged
            LOGGER.log(Level.WARNING, "Called revealEvent(" + allPlayers
                + ", "
                + (playerNames != null ? playerNames.toString() : "-null-")
                + ", " + legion + ", " + creatureNames.toString() + ", "
                + turn + ") with empty creatureNames");
            return;
        }
        Element event = new Element("Reveal");
        event.setAttribute("markerId", legion.getMarkerId());
        event.setAttribute("allPlayers", "" + allPlayers);
        event.setAttribute("turn", "" + turn);
        if (!allPlayers)
        {
            Element viewers = new Element("viewers");
            event.addContent(viewers);
            Iterator<String> it = playerNames.iterator();
            while (it.hasNext())
            {
                String playerName = it.next();
                Element viewer = new Element("viewer");
                viewer.addContent(playerName);
                viewers.addContent(viewer);
            }
        }
        Element creatures = new Element("creatures");
        event.addContent(creatures);
        Iterator<String> it = creatureNames.iterator();
        while (it.hasNext())
        {
            String creatureName = it.next();
            Element creature = new Element("creature");
            creature.addContent(creatureName);
            creatures.addContent(creature);
        }
        root.addContent(event);
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
        root.addContent(event);
    }

    void copyTree(Element his)
    {
        root = (Element)his.clone();
    }

    // unchecked conversions from JDOM
    @SuppressWarnings("unchecked")
    void fireEventsFromXML(Server server)
    {
        this.loading = true;
        assert root != null : "History should always have a JDOM root element as backing store";

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
        if (el.getName().equals("Reveal"))
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
            List<String> creatureNames = new ArrayList<String>();
            List<Element> creatureElements = el.getChild("creatures")
                .getChildren();
            List<CreatureType> creatures = new ArrayList<CreatureType>();
            for (Element creature : creatureElements)
            {
                String creatureName = creature.getTextNormalize();
                creatureNames.add(creatureName);
                creatures.add(game.getVariant()
                    .getCreatureByName(creatureName));
            }
            Player player = game.getPlayerByMarkerId(markerId);
            Legion legion;

            if (turn == 1 && player.getLegionByMarkerId(markerId) == null)
            {
                // there is no create event for the startup legions,
                // so we might need to create them for the reveal event
                legion = new LegionServerSide(markerId, null, player
                    .getStartingTower(), player.getStartingTower(), player,
                    game, creatures
                        .toArray(new CreatureType[creatures.size()]));
                player.addLegion(legion);
            }
            else
            {
                legion = player.getLegionByMarkerId(markerId);
            }
            String reason = "<unknown>";
            if (((PlayerServerSide)player).getDeadBeforeSave())
            {
                // Skip for players that will be dead by end of replay
            }
            else if (all)
            {
                server.allRevealCreatures(legion, creatureNames, reason);
            }
            else
            {
                server.oneRevealLegion(game.getPlayer(playerName), legion,
                    creatureNames, reason);
            }
        }
        else if (el.getName().equals("Split"))
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
            // LegionServerSide.split(..) doesn't like us here since the parent
            // legion can't remove creatures (not there?) -- create child directly
            // instead
            PlayerServerSide player = parentLegion.getPlayer();
            LegionServerSide childLegion;
            if (player.hasLegion(childId))
            {
                childLegion = game.getLegionByMarkerId(childId);
                List<String> content = childLegion.getImageNames();
                LOGGER.severe("During replay of history: child legion "
                    + childId + " should not " + "exist yet (turn=" + turn
                    + ")!!\n" + "Exists already with: " + content.toString()
                    + " but " + "should now be created with creatures: "
                    + creatures);

                childLegion.remove();
            }

            childLegion = new LegionServerSide(childId, null, parentLegion
                .getCurrentHex(), parentLegion.getCurrentHex(), player, game,
                creatures.toArray(new CreatureType[creatures.size()]));

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
        else if (el.getName().equals("Merge"))
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
        else if (el.getName().equals("AddCreature"))
        {
            String markerId = el.getAttributeValue("markerId");
            String creatureName = el.getAttributeValue("creatureName");
            String reason = "<unknown>";
            LOGGER.finer("Adding creature '" + creatureName
                + "' to legion with markerId '" + markerId + "', reason '"
                + reason + "'");
            LegionServerSide legion = game.getLegionByMarkerId(markerId);
            legion.addCreature(game.getVariant().getCreatureByName(
                creatureName), false);
            // Skip for players that will be dead by end of replay
            if (!legion.getPlayer().getDeadBeforeSave())
            {
                server.allTellAddCreature(legion, creatureName, false, reason);
            }
            LOGGER.finest("Legion '" + markerId + "' now contains "
                + legion.getCreatures());
        }
        else if (el.getName().equals("RemoveCreature"))
        {
            String markerId = el.getAttributeValue("markerId");
            String creatureName = el.getAttributeValue("creatureName");
            String reason = "<unknown>";
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
                List<CreatureServerSide> cres = legion.getCreatures();
                List<String> crenames = new ArrayList<String>();
                for (CreatureServerSide c : cres)
                {
                    crenames.add(c.getName());
                }
            }

            // don't use disbandIfEmpty parameter since that'll fire another history event
            CreatureType removedCritter = legion.removeCreature(game.
                getVariant().getCreatureByName(creatureName), false, false);

            // Skip for players that will be dead by end of replay
            // Skip if removedCritter is null => removeCreature did not find it,
            // so there is something wrong with the save game. No use to bother
            // all the clients with it.
            if (removedCritter != null
                && !legion.getPlayer().getDeadBeforeSave())
            {
                server.allTellRemoveCreature(legion, creatureName, false,
                    reason);
            }
            LOGGER.finest("Legion '" + markerId + "' now contains "
                + legion.getCreatures());
            if (legion.getHeight() == 0)
            {
                legion.remove(false, false);
                LOGGER.finer("Legion '" + markerId + "' removed");
            }
        }
        else if (el.getName().equals("PlayerElim"))
        {
            String playerName = el.getAttributeValue("name");
            String slayerName = el.getAttributeValue("slayer");
            PlayerServerSide player = game.getPlayer(playerName);
            PlayerServerSide slayer = game.getPlayer(slayerName);
            // Record the slayer and give him this player's legion markers.
            if (slayer != null)
            {
                player.handleSlaying(slayer);
            }
            player.setDead(true);
            server.allUpdatePlayerInfo();
            server.allTellPlayerElim(player, slayer, false);
        }
    }
}
