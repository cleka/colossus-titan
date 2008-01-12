package net.sf.colossus.client;


import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.server.Constants;


/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public final class BattleMap extends HexMap implements MouseListener,
    WindowListener
{
    private static final Logger LOGGER = Logger.getLogger(BattleMap.class
        .getName());

    private static int count = 1;

    private final Client client;

    private final Marker attackerMarker;
    private final Marker defenderMarker;

    public BattleMap(Client client, String masterHexLabel,
        String attackerMarkerId, String defenderMarkerId)
    {
        super(masterHexLabel);

        this.client = client;

        attackerMarker = new Marker(3 * Scale.get(), attackerMarkerId, false);
        defenderMarker = new Marker(3 * Scale.get(), defenderMarkerId, true);
        attackerMarker.setOpaque(false);
        defenderMarker.setOpaque(false);
        attackerMarker.setToolTipText("Attacking Legion");
        defenderMarker.setToolTipText("Defending Legion");
        this.add(attackerMarker);
        this.add(defenderMarker);

        String instanceId = client.getOwningPlayer().getPlayer().getName()
            + ": " + attackerMarkerId + "/" + defenderMarkerId + " (" + count
            + ")";
        count++;
        net.sf.colossus.webcommon.InstanceTracker.setId(this, instanceId);

    }

    public void setBattleMarkerLocation(boolean isDefender, String hexLabel)
    {
        GUIBattleHex hex = getGUIHexByLabel(hexLabel);
        Rectangle rect = hex.getBounds();
        Point point;
        if ("X1".equals(hexLabel) || "X4".equals(hexLabel))
        {
            point = new Point(rect.x, rect.height + rect.y);
        }
        else
        {
            point = new Point(rect.x + rect.width, rect.y);
        }
        if (isDefender)
        {
            defenderMarker.setLocation(point, hexLabel);
        }
        else
        {
            attackerMarker.setLocation(point, hexLabel);
        }
    }

    public static BattleHex getEntrance(String terrain, int entrySide)
    {
        return HexMap.getHexByLabel(terrain, "X" + entrySide);
    }

    /** Select all hexes containing critters eligible to move. */
    public void highlightMobileCritters()
    {
        Set<String> set = client.findMobileCritterHexes();
        unselectAllHexes();
        unselectEntranceHexes();
        selectHexesByLabels(set);
        selectEntranceHexes(set);
    }

    /** Select hexes containing critters that have valid strike targets. */
    public void highlightCrittersWithTargets()
    {
        Set<String> set = client.findCrittersWithTargets();
        unselectAllHexes();
        selectHexesByLabels(set);
        // XXX Needed?
        repaint();
    }

    public void selectEntranceHexes(Set<String> labels)
    {
        Iterator<String> it = labels.iterator();
        while (it.hasNext())
        {
            String hexLabel = it.next();
            if (hexLabel.startsWith("X"))
            {
                if (hexLabel.equals(defenderMarker.hexLabel))
                {
                    defenderMarker.highlightMarker();
                }
                if (hexLabel.equals(attackerMarker.hexLabel))
                {
                    attackerMarker.highlightMarker();
                }
            }
        }
    }

    public void unselectEntranceHexes()
    {
        defenderMarker.resetMarkerHighlight();
        attackerMarker.resetMarkerHighlight();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Rectangle rectClip = g.getClipBounds();

        // Abort if called too early.
        if (rectClip == null)
        {
            return;
        }

        try
        {
            List<BattleChit> battleChits = client.getBattleChits();
            ListIterator<BattleChit> lit = battleChits
                .listIterator(battleChits.size());
            while (lit.hasPrevious())
            {
                BattleChit chit = lit.previous();
                if (rectClip.intersects(chit.getBounds()))
                {
                    chit.paintComponent(g);
                }
            }
            if (attackerMarker.getLocation().x > 0) // don't paint till placed
            {
                attackerMarker.paintComponent(g);
            }
            if (defenderMarker.getLocation().x > 0) // don't paint till placed
            {
                defenderMarker.paintComponent(g);
            }
        }
        catch (ConcurrentModificationException ex)
        {
            // Let the next repaint clean up.
            LOGGER.log(Level.FINEST, "harmless " + ex.toString());
        }
    }

    public static String entrySideName(int side)
    {
        switch (side)
        {
            case 1:
                return Constants.right;

            case 3:
                return Constants.bottom;

            case 5:
                return Constants.left;

            default:
                return "";
        }
    }

    public static int entrySideNum(String side)
    {
        if (side == null)
        {
            return -1;
        }
        if (side.equals(Constants.right))
        {
            return 1;
        }
        else if (side.equals(Constants.bottom))
        {
            return 3;
        }
        else if (side.equals(Constants.left))
        {
            return 5;
        }
        else
        {
            return -1;
        }
    }
}
