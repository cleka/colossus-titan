import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public class BattleMap extends JFrame implements MouseListener,
    WindowListener
{
    private static BattleHex[][] h = new BattleHex[6][6];
    private ArrayList hexes = new ArrayList(27);

    // ne, e, se, sw, w, nw
    private BattleHex [] entrances = new BattleHex[6];

    private Image offImage;
    private Graphics offGraphics;
    private Dimension offDimension;

    private boolean eraseFlag;

    private int scale;
    private int chitScale;

    private MasterBoard board;
    private MasterHex masterHex;
    private Battle battle;

    private static Point location;


    public BattleMap(MasterBoard board, MasterHex masterHex, Battle battle)
    {
        super(battle.getAttacker().getMarkerId() + " (" +
            battle.getAttacker().getPlayer().getName() +
            ") attacks " + battle.getDefender().getMarkerId() + " (" +
            battle.getDefender().getPlayer().getName() + ")" + " in " +
            masterHex.getDescription());

        Legion attacker = battle.getAttacker();
        Legion defender = battle.getDefender();

        Game.logEvent("\n" + attacker.getMarkerId() + " (" +
            attacker.getPlayer().getName() + ") attacks " +
            defender.getMarkerId() + " (" + defender.getPlayer().getName() +
            ")" + " in " + masterHex.getDescription());

        this.masterHex = masterHex;
        this.board = board;
        this.battle = battle;

        getContentPane().setLayout(null);

        scale = getScale();
        chitScale = 2 * scale;

        pack();
        setSize(getPreferredSize());

        setupIcon();

        setBackground(Color.white);
        addWindowListener(this);
        addMouseListener(this);

        validate();

        // Initialize the hexmap.
        SetupBattleHexes.setupHexes(h, masterHex.getTerrain(), this, hexes);
        SetupBattleHexes.setupNeighbors(h);
        setupEntrances();

        placeLegion(attacker, false);
        placeLegion(defender, true);

        pack();

        if (location == null)
        {
            location = new Point(0, 2 * scale);
        }
        setLocation(location);

        setVisible(true);
        repaint();
    }


    private void setupIcon()
    {
        if (board != null && !board.getGame().isApplet())
        {
            try
            {
                setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource(Chit.getImagePath(
                    Creature.colossus.getImageName()))));
            }
            catch (NullPointerException e)
            {
                System.out.println(e.toString() + " Couldn't find " +
                    Creature.colossus.getImageName());
                dispose();
            }
        }
    }


    public void placeNewChit(Legion legion)
    {
        BattleHex entrance = getEntrance(legion);
        int height = legion.getHeight();
        Critter critter = legion.getCritter(height - 1);
        battle.addCritter(critter);

        BattleChit chit = new BattleChit(chitScale,
            critter.getImageName(legion == battle.getDefender()), this,
            critter);
        critter.addBattleInfo(entrance, this, chit, battle);
        entrance.addCritter(critter);

        entrance.alignChits();

        chit.repaint();
    }


    private void placeLegion(Legion legion, boolean inverted)
    {
        BattleHex entrance = getEntrance(legion);
        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            battle.addCritter(critter);
            BattleChit chit = new BattleChit(chitScale,
                critter.getImageName(inverted), this, critter);
            critter.addBattleInfo(entrance, this, chit, battle);
            entrance.addCritter(critter);
        }
        entrance.alignChits();
    }


    public void unselectAllHexes()
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.isSelected())
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }


    public void unselectHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
                return;
            }
        }
    }


    public void unselectHexesByLabels(Set labels)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }


    public void selectHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (!hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
                return;
            }
        }
    }


    public void selectHexesByLabels(Set labels)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (!hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
            }
        }
    }


    public static int getScale()
    {
        int scale;

        // Make sure the map fits on the screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000)
        {
            scale = 30 * d.height / 1000;
        }
        else
        {
            scale = 30;
        }

        return scale;
    }


    private void setupEntrances()
    {
        int cx = 6 * scale;
        int cy = 3 * scale;

        // Initialize entrances.
        entrances[0] = new BattleHex(cx + 15 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 0);
        entrances[1] = new BattleHex(cx + 21 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 1);
        entrances[2] = new BattleHex(cx + 17 * scale,
            (int) Math.round(cy + 22 * scale), scale, this, -1, 2);
        entrances[3] = new BattleHex(cx + 2 * scale,
            (int) Math.round(cy + 21 * scale), scale, this, -1, 3);
        entrances[4] = new BattleHex(cx - 3 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 4);
        entrances[5] = new BattleHex(cx + 1 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 5);

        // Add neighbors to entrances.
        entrances[0].setNeighbor(3, h[3][0]);
        entrances[0].setNeighbor(4, h[4][1]);
        entrances[0].setNeighbor(5, h[5][1]);

        entrances[1].setNeighbor(3, h[5][1]);
        entrances[1].setNeighbor(4, h[5][2]);
        entrances[1].setNeighbor(5, h[5][3]);
        entrances[1].setNeighbor(0, h[5][4]);

        entrances[2].setNeighbor(4, h[5][4]);
        entrances[2].setNeighbor(5, h[4][5]);
        entrances[2].setNeighbor(0, h[3][5]);

        entrances[3].setNeighbor(5, h[3][5]);
        entrances[3].setNeighbor(0, h[2][5]);
        entrances[3].setNeighbor(1, h[1][4]);
        entrances[3].setNeighbor(2, h[0][4]);

        entrances[4].setNeighbor(0, h[0][4]);
        entrances[4].setNeighbor(1, h[0][3]);
        entrances[4].setNeighbor(2, h[0][2]);

        entrances[5].setNeighbor(1, h[0][2]);
        entrances[5].setNeighbor(2, h[1][1]);
        entrances[5].setNeighbor(3, h[2][1]);
        entrances[5].setNeighbor(4, h[3][0]);
    }


    public BattleHex getEntrance(Legion legion)
    {
        if (legion == battle.getAttacker())
        {
            return entrances[masterHex.getEntrySide()];
        }
        else
        {
            return entrances[(masterHex.getEntrySide() + 3) % 6];
        }
    }


    // Do a brute-force search through the hex array, looking for
    //    a match.  Return the hex, or null.
    public BattleHex getHexFromLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }

        System.out.println("Could not find hex " + label);
        return null;
    }


    // Return the BattleHex that contains the given point, or
    //    null if none does.
    private BattleHex getHexContainingPoint(Point point)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.contains(point))
            {
                return hex;
            }
        }

        return null;
    }


    // Return the Critter whose chit contains the given point,
    //   or null if none does.
    private Critter getCritterWithChitContainingPoint(Point point)
    {
        Collection critters = battle.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = critter.getChit();
            if (chit.contains(point))
            {
                return critter;
            }
        }

        return null;
    }


    public static BattleHex getCenterTowerHex()
    {
        return h[3][2];
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        Critter critter = getCritterWithChitContainingPoint(point);
        BattleHex hex = getHexContainingPoint(point);

        // Only the active player can move or strike.
        if (critter != null && critter.getPlayer() == battle.getActivePlayer())
        {
            battle.actOnCritter(critter);
        }

        // No hits on chits, so check map.
        else if (hex != null && hex.isSelected())
        {
            battle.actOnHex(hex);
        }

        // No hits on selected hexes, so clean up.
        else
        {
            battle.actOnMisclick();
        }
    }


    public void mouseReleased(MouseEvent e)
    {
    }


    public void mouseClicked(MouseEvent e)
    {
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        if (board != null)
        {
            board.getGame().dispose();
        }
        battle.cleanup();
    }


    public void windowDeactivated(WindowEvent e)
    {
    }


    public void windowDeiconified(WindowEvent e)
    {
    }


    public void windowIconified(WindowEvent e)
    {
    }


    public void windowOpened(WindowEvent e)
    {
    }


    // This is used to fix artifacts from chits outside visible hexes.
    public void setEraseFlag()
    {
        eraseFlag = true;
    }


    public void update(Graphics g)
    {
        Rectangle rectClip = g.getClipBounds();

        // Abort if called too early.
        if (rectClip == null)
        {
            return;
        }

        Dimension d = getSize();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(d.width, d.height);
            offGraphics = offImage.getGraphics();
        }

        // If the erase flag is set, erase the background.
        if (eraseFlag)
        {
            offGraphics.clearRect(0, 0, d.width, d.height);
            eraseFlag = false;
        }

        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (rectClip.intersects(hex.getBounds()))
            {
                hex.paint(offGraphics);
            }
        }

        // Draw chits from back to front.
        ArrayList critters = (ArrayList)battle.getCritters();
        ListIterator lit = critters.listIterator(critters.size());
        while (lit.hasPrevious())
        {
            Critter critter = (Critter)lit.previous();
            Chit chit = critter.getChit();
            if (rectClip.intersects(chit.getBounds()))
            {
                chit.paint(offGraphics);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }

    /** Double-buffer everything. */
    public void paint(Graphics g)
    {
        update(g);
    }


    public void dispose()
    {
        // Save location for next object.
        location = getLocation();

        super.dispose();
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }


    public Dimension getPreferredSize()
    {
        return new Dimension(30 * scale, 28 * scale);
    }


    public static void main(String [] args)
    {
        Battle.main(args);
    }
}
