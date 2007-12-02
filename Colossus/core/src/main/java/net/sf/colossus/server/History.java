package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Element;


/**
 * Stores game history as XML.
 * @version $Id$
 * @author David Ripton
 */
public class History
{
    private static final Logger LOGGER = Logger.getLogger(History.class.getName());

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

    void splitEvent(String parentId, String childId, List splitoffs, int turn)
    {
        Element event = new Element("Split");
        event.setAttribute("parentId", parentId);
        event.setAttribute("childId", childId);
        event.setAttribute("turn", "" + turn);
        Element creatures = new Element("splitoffs");
        event.addContent(creatures);
        Iterator it = splitoffs.iterator();
        while (it.hasNext())
        {
            String creatureName = (String)it.next();
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

    void revealEvent(boolean allPlayers, List playerNames, String markerId,
        List creatureNames, int turn)
    {
        if (creatureNames.isEmpty())
        {
            // this happens e.g. when in final battle (titan vs. titan)
            // angel was called out of legion which was then empty,
            // and in the final updateAllLegionContents there is then
            // this empty legion...
            LOGGER.log(Level.WARNING, "Called revealEvent("+allPlayers+", " +
                (playerNames != null ? playerNames.toString() : "-null-") +
                ", " + markerId + ", " + creatureNames.toString() + ", " +
                turn + ") with empty creatureNames");
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
            Iterator it = playerNames.iterator();
            while (it.hasNext())
            {
                String playerName = (String)it.next();
                Element viewer = new Element("viewer");
                viewer.addContent(playerName);
                viewers.addContent(viewer);
            }
        }
        Element creatures = new Element("creatures");
        event.addContent(creatures);
        Iterator it = creatureNames.iterator();
        while (it.hasNext())
        {
            String creatureName = (String)it.next();
            Element creature = new Element("creature");
            creature.addContent(creatureName);
            creatures.addContent(creature);
        }
        root.addContent(event);
    }

    void playerElimEvent(String playerName, String slayerName, int turn)
    {
        Element event = new Element("PlayerElim");
        event.setAttribute("name", playerName);
        if (slayerName != null)
        {
            event.setAttribute("slayer", slayerName);
        }
        event.setAttribute("turn", "" + turn);
        root.addContent(event);
    }

    void copyTree(Element his)
    {
        root = (Element)his.clone();
    }

    void fireEventsFromXML(Server server)
    {
        if (root == null)
        {
            return;
        }
        List kids = root.getChildren();
        Iterator it = kids.iterator();
        while (it.hasNext())
        {
            Element el = (Element)it.next();
            fireEventFromElement(server, el);
        }
    }

    void fireEventFromElement(Server server, Element el)
    {
        if (el.getName().equals("Reveal"))
        {
            String allPlayers = el.getAttributeValue("allPlayers");
            boolean all = allPlayers != null && allPlayers.equals("true");
            String markerId = el.getAttributeValue("markerId");
            List playerNames = new ArrayList();
            Element viewEl = el.getChild("viewers");
            String playerName = null;
            if (viewEl != null)
            {
                List viewers = viewEl.getChildren();
                Iterator it = viewers.iterator();
                while (it.hasNext())
                {
                    Element viewer = (Element)it.next();
                    playerName = viewer.getTextNormalize();
                    playerNames.add(playerName);
                }
            }
            List creatureNames = new ArrayList();
            List creatures = el.getChild("creatures").getChildren();
            for (Iterator it = creatures.iterator(); it.hasNext(); )
            {
                Element creature = (Element)it.next();
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
            List creatureNames = new ArrayList();
            List splitoffs = el.getChild("splitoffs").getChildren();
            Iterator it = splitoffs.iterator();
            while (it.hasNext())
            {
                Element creature = (Element)it.next();
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
            server.allTellRemoveCreature(markerId, creatureName, false,
                reason);
        }
        else if (el.getName().equals("PlayerElim"))
        {
            String playerName = el.getAttributeValue("name");
            String slayerName = el.getAttributeValue("slayer");
            server.allTellPlayerElim(playerName, slayerName, false);
        }
    }
}
