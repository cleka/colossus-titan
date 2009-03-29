package net.sf.colossus.gui;


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

import net.sf.colossus.client.Client;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;


/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * 
 * TODO there is still model code in here, thus we still have dependencies
 *      from the AI and server packages into this class.
 * 
 * @version $Id$
 * @author David Ripton
 */
@SuppressWarnings("serial")
public final class BattleMap extends HexMap implements MouseListener,
    WindowListener
{
    private static final Logger LOGGER = Logger.getLogger(BattleMap.class
        .getName());

    private static int count = 1;

    private final Client client;

    private final Marker attackerMarker;
    private final Marker defenderMarker;

    // TODO pass Legions instead of markerIds
    public BattleMap(Client client, MasterHex masterHex,
        String attackerMarkerId, String defenderMarkerId)
    {
        super(masterHex);

        this.client = client;

        attackerMarker = new Marker(3 * Scale.get(), attackerMarkerId, false);
        defenderMarker = new Marker(3 * Scale.get(), defenderMarkerId, true);
        attackerMarker.setOpaque(false);
        defenderMarker.setOpaque(false);
        attackerMarker.setToolTipText("Attacking Legion");
        defenderMarker.setToolTipText("Defending Legion");
        this.add(attackerMarker);
        this.add(defenderMarker);

        String instanceId = client.getOwningPlayer().getName() + ": "
            + attackerMarkerId + "/" + defenderMarkerId + " (" + count + ")";
        count++;
        InstanceTracker.setId(this, instanceId);
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

    public static BattleHex getEntrance(MasterBoardTerrain terrain,
        EntrySide entrySide)
    {
        return HexMap.getHexByLabel(terrain, "X" + entrySide.getId());
    }

    // Following four override to make them public because they are needed
    // by MasterBoard. Is there a better way?
    @Override
    public void setupHexes()
    {
        super.setupHexes();
    }

    @Override
    public void selectHexesByLabels(Set<String> set)
    {
        super.selectHexesByLabels(set);
    }

    @Override
    public void unselectHexByLabel(String hexLabel)
    {
        super.unselectHexByLabel(hexLabel);
    }

    @Override
    public void unselectAllHexes()
    {
        super.unselectAllHexes();
    }

    @Override
    public Set<String> getAllHexLabels()
    {
        return super.getAllHexLabels();
    }

    @Override
    public MasterHex getMasterHex()
    {
        return super.getMasterHex();
    }

    @Override
    public GUIBattleHex getGUIHexByLabel(String hexLabel)
    {
        return super.getGUIHexByLabel(hexLabel);
    }

    @Override
    public GUIBattleHex getHexContainingPoint(Point point)
    {
        return super.getHexContainingPoint(point);
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
}
