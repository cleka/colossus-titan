package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.Player;

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

    Element getCopy()
    {
        return (Element)root.clone();
    }

    void addCreatureEvent(String markerId, String creatureName, int turn)
    {
        Element event = new Element("AddCreature");
        event.setAttribute("markerId", markerId);
        event.setAttribute("creatureName", creatureName);
        event.setAttribute("turn", "" + turn);
        root.addContent(event);
    }

    void removeCreatureEvent(String markerId, String creatureName, int turn)
    {
        Element event = new Element("RemoveCreature");
        event.setAttribute("markerId", markerId);
        event.setAttribute("creatureName", creatureName);
        event.setAttribute("turn", "" + turn);
        root.addContent(event);
    }

    void splitEvent(String parentId, String childId, List<String> splitoffs,
        int turn)
    {
        Element event = new Element("Split");
        event.setAttribute("parentId", parentId);
        event.setAttribute("childId", childId);
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
        Element event = new Element("Merge");
        event.setAttribute("splitoffId", splitoffId);
        event.setAttribute("survivorId", survivorId);
        event.setAttribute("turn", "" + turn);
        root.addContent(event);
    }

    void revealEvent(boolean allPlayers, List<String> playerNames,
        String markerId, List<String> creatureNames, int turn)
    {
        if (creatureNames.isEmpty())
        {
            // this happens e.g. when in final battle (titan vs. titan)
            // angel was called out of legion which was then empty,
            // and in the final updateAllLegionContents there is then
            // this empty legion...
            LOGGER.log(Level.WARNING, "Called revealEvent(" + allPlayers
                + ", "
                + (playerNames != null ? playerNames.toString() : "-null-")
                + ", " + markerId + ", " + creatureNames.toString() + ", "
                + turn + ") with empty creatureNames");
            return;
        }
        Element event = new Element("Reveal");
        event.setAttribute("markerId", markerId);
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
        if (root == null)
        {
            return;
        }
        List<Element> kids = root.getChildren();
        Iterator<Element> it = kids.iterator();
        while (it.hasNext())
        {
            Element el = it.next();
            fireEventFromElement(server, el);
        }
    }

    // unchecked conversions from JDOM
    @SuppressWarnings("unchecked")
    void fireEventFromElement(Server server, Element el)
    {
        if (el.getName().equals("Reveal"))
        {
            String allPlayers = el.getAttributeValue("allPlayers");
            boolean all = allPlayers != null && allPlayers.equals("true");
            String markerId = el.getAttributeValue("markerId");
            List<String> playerNames = new ArrayList<String>();
            Element viewEl = el.getChild("viewers");
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
            List<Element> creatures = el.getChild("creatures").getChildren();
            for (Element creature : creatures)
            {
                String creatureName = creature.getTextNormalize();
                creatureNames.add(creatureName);
            }
            String reason = "<unknown>";
            if (all)
            {
                server.allRevealCreatures(markerId, creatureNames, reason);
            }
            else
            {
                server.oneRevealCreatures(playerName, markerId, creatureNames,
                    reason);
            }
        }
        else if (el.getName().equals("Split"))
        {
            String parentId = el.getAttributeValue("parentId");
            String childId = el.getAttributeValue("childId");
            String turnString = el.getAttributeValue("turn");
            int turn = Integer.parseInt(turnString);
            List<String> creatureNames = new ArrayList<String>();
            List<Element> splitoffs = el.getChild("splitoffs").getChildren();
            Iterator<Element> it = splitoffs.iterator();
            while (it.hasNext())
            {
                Element creature = it.next();
                String creatureName = creature.getTextNormalize();
                creatureNames.add(creatureName);
            }
            server.didSplit(parentId, childId, creatureNames, turn);
        }
        else if (el.getName().equals("Merge"))
        {
            String splitoffId = el.getAttributeValue("splitoffId");
            String survivorId = el.getAttributeValue("survivorId");
            String turnString = el.getAttributeValue("turn");
            int turn = Integer.parseInt(turnString);
            server.undidSplit(splitoffId, survivorId, false, turn);
        }
        else if (el.getName().equals("AddCreature"))
        {
            String markerId = el.getAttributeValue("markerId");
            String creatureName = el.getAttributeValue("creatureName");
            String reason = "<unknown>";
            server.allTellAddCreature(markerId, creatureName, false, reason);
        }
        else if (el.getName().equals("RemoveCreature"))
        {
            String markerId = el.getAttributeValue("markerId");
            String creatureName = el.getAttributeValue("creatureName");
            String reason = "<unknown>";
            server
                .allTellRemoveCreature(markerId, creatureName, false, reason);
        }
        else if (el.getName().equals("PlayerElim"))
        {
            String playerName = el.getAttributeValue("name");
            String slayerName = el.getAttributeValue("slayer");
            server.allTellPlayerElim(playerName, slayerName, false);
        }
    }
}
