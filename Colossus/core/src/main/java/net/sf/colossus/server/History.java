package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
     * Stores the surviving legions.
     * 
     * While the history should contain all information to reproduce the game state,
     * the last set of legions is currently still loaded upfront since they contain
     * the battle-specific information. This collides with replaying the game from
     * history, so we need to know which legions we have to reconstruct and which
     * ones we don't touch.
     * 
     * TODO align the history replay more with the original gameplay so we don't
     *      need this anymore
     */
    private Set<Legion> survivorLegions;

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

        // the game should have a number of legions preset now (all surviving ones)
        // make sure we don't touch them when replaying the history
        this.survivorLegions = new HashSet<Legion>();
        for (Legion legion : server.getGame().getAllLegions())
        {
            this.survivorLegions.add(legion);
        }
        List<Element> kids = root.getChildren();
        Iterator<Element> it = kids.iterator();
        while (it.hasNext())
        {
            Element el = it.next();
            fireEventFromElement(server, el);
        }
        this.survivorLegions = null; // free memory -- TODO could be passed around instead of member
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
            if (turn == 1 && !player.hasLegion(markerId))
            {
                // there is no create event for the startup legions, so we might need 
                // to create them for the reveal event
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
            if (all)
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
            Legion childLegion;
            if (player.hasLegion(childId))
            {
                childLegion = game.getLegionByMarkerId(childId);
            }
            else
            {
                childLegion = new LegionServerSide(childId, null, parentLegion
                    .getCurrentHex(), parentLegion.getCurrentHex(), player,
                    game, creatures
                        .toArray(new CreatureType[creatures.size()]));
                player.addLegion(childLegion);
            }
            if (!this.survivorLegions.contains(parentLegion))
            {
                for (CreatureType creature : creatures)
                {
                    parentLegion.removeCreature(creature, false, false);
                }
            }

            server.didSplit(parentLegion, childLegion, creatureNames, turn);
        }
        else if (el.getName().equals("Merge"))
        {
            String splitoffId = el.getAttributeValue("splitoffId");
            String survivorId = el.getAttributeValue("survivorId");
            String turnString = el.getAttributeValue("turn");
            int turn = Integer.parseInt(turnString);
            LegionServerSide splitoff = game.getLegionByMarkerId(splitoffId);
            server.undidSplit(splitoff, game.getLegionByMarkerId(survivorId),
                false, turn);
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
            if (!this.survivorLegions.contains(legion))
            {
                legion.addCreature(game.getVariant().getCreatureByName(
                    creatureName), false);
            }
            server.allTellAddCreature(legion, creatureName, false, reason);
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
            if (!this.survivorLegions.contains(legion))
            {
                // don't use disbandIfEmpty parameter since that'll fire another history event
                legion.removeCreature(game.getVariant().getCreatureByName(
                    creatureName), false, false);
            }
            server.allTellRemoveCreature(legion, creatureName, false, reason);
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
            server.allTellPlayerElim(playerName, slayerName, false);
        }
    }
}
