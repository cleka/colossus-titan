package net.sf.colossus.server;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.jdom.Element;

/**
 * Stores a battle progress/history as XML.
 *
 * @author Clemens Katzer
 */

public class BattleHistory
{
    private static final Logger LOGGER = Logger
        .getLogger(BattleHistory.class.getName());

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

    private boolean isRedo = false;

    private final Element loadedRedoLog;


    public BattleHistory()
    {
        root = new Element("BattleHistory");
        // Dummy:
        loadedRedoLog = new Element("LoadedRedoLog");
    }

    /**
     * Constructor used by "LoadGame"
     */
    public BattleHistory(Element loadGameRoot)
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

            if (name.equals("ignorable-element"))
            {
                LOGGER.finest("Flush Redo to BattleHistory: skipping " + name);
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
    // @SuppressWarnings("unchecked")
    void fireEventFromElement(Server server, Element el)
    {
        GameServerSide game = server.getGame();
        boolean engagementOngoing = game.isEngagementOngoing();
        String eventName = el.getName();
        String reasonPerhaps = el.getAttributeValue("reason");
        String reason = (reasonPerhaps != null && !reasonPerhaps
            .equals("null")) ? reasonPerhaps : "<undefinedReason>";
        if (eventName.equals("Battle Redo Whatever"))
        {
            LOGGER
                .finest("Whatever redo event: \n" + " reason: " + reason
                    + "; eng. ongoing: " + engagementOngoing);

            /*
                String markerId = el.getAttributeValue("markerId");
                LegionServerSide legion = game.getLegionByMarkerId(markerId);
                LOGGER
                    .finest("Whatever redo event: \n" + " marker " + markerId);
                server.overrideProcessingCH(legion.getPlayer());
                server.undoRecruit(legion);
                server.restoreProcessingCH();
             */
        }
        else
        {
            LOGGER.warning("Unknown Battle Redo element " + eventName);
        }
    }

    public boolean isRedo()
    {
        return this.isRedo;
    }

    public boolean isLoading()
    {
        return this.loading;
    }
}
