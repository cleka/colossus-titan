import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public class BattleMap extends Frame implements MouseListener,
    WindowListener
{
    private static BattleHex[][] h = new BattleHex[6][6];

    // ne, e, se, sw, w, nw
    private static BattleHex [] entrances = new BattleHex[6];

    private static Image offImage;
    private static Graphics offGraphics;
    private static Dimension offDimension;
    private static MediaTracker tracker;
    private static boolean imagesLoaded;
    private static boolean eraseFlag;

    private static int scale;
    private static int chitScale;

    private static MasterBoard board;
    private static MasterHex masterHex;
    private static Battle battle;

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

        setLayout(null);

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
        SetupBattleHexes.setupHexes(h, masterHex.getTerrain(), this);
        setupEntrances();

        tracker = new MediaTracker(this);

        placeLegion(attacker, false);
        placeLegion(defender, true);

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, e.toString() + " waitForAll was interrupted");
        }
        imagesLoaded = true;

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
                    getClass().getResource(Creature.colossus.getImageName())));
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
        imagesLoaded = false;
        tracker = new MediaTracker(this);

        BattleHex entrance = getEntrance(legion);
        int height = legion.getHeight();
        Critter critter = legion.getCritter(height - 1);
        battle.addCritter(critter);

        BattleChit chit = new BattleChit(chitScale,
            critter.getImageName(legion == battle.getDefender()), this,
            critter);
        tracker.addImage(chit.getImage(), 0);
        critter.addBattleInfo(entrance, this, chit, battle);
        entrance.addCritter(critter);

        entrance.alignChits();

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, e.toString() + "waitForAll was interrupted");
        }
        imagesLoaded = true;
    }


    private void placeLegion(Legion legion, boolean inverted)
    {
        BattleHex entrance = getEntrance(legion);
        for (int i = 0; i < legion.getHeight(); i++)
        {
            Critter critter = legion.getCritter(i);
            battle.addCritter(critter);
            BattleChit chit = new BattleChit(chitScale,
                critter.getImageName(inverted), this, critter);
            tracker.addImage(chit.getImage(), 0);
            critter.addBattleInfo(entrance, this, chit, battle);
            entrance.addCritter(critter);
        }
        entrance.alignChits();
    }


    public static void unselectAllHexes()
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupBattleHexes.show[i][j] && h[i][j].isSelected())
                {
                    h[i][j].unselect();
                    h[i][j].repaint();
                }
            }
        }
    }


    public static boolean listContains(String [] list, String item)
    {
        for (int i = list.length - 1; i >= 0; i--)
        {
            if (item.equals(list[i]))
            {
                return true;
            }
        }
        return false;
    }


    public static void unselectHexesByLabels(String [] labels)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupBattleHexes.show[i][j])
                {
                    BattleHex hex = h[i][j];

                    if (hex.isSelected() && listContains(labels, 
                        hex.getLabel()))
                    {
                        hex.unselect();
                        hex.repaint();
                    }
                }
            }
        }
    }
    
    
    public static void selectHexesByLabels(String [] labels)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupBattleHexes.show[i][j])
                {
                    BattleHex hex = h[i][j];

                    if (!hex.isSelected() && listContains(labels,
                        hex.getLabel()))
                    {
                        hex.select();
                        hex.repaint();
                    }
                }
            }
        }
    }


    public static void highlightUnoccupiedTowerHexes()
    {
        if (!h[3][1].isOccupied())
        {
            h[3][1].select();
            h[3][1].repaint();
        }
        for (int i = 2; i <= 4; i++)
        {
            for (int j = 2; j <= 3; j++)
            {
                if (!h[i][j].isOccupied())
                {
                    h[i][j].select();
                    h[i][j].repaint();
                }
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


    public static BattleHex getEntrance(Legion legion)
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
    public static BattleHex getHexFromLabel(String label)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupBattleHexes.show[i][j] && h[i][j].getLabel().equals(
                    (label)))
                {
                    return h[i][j];
                }
            }
        }

        System.out.println("Could not find hex " + label);
        return null;
    }


    // Return the BattleHex that contains the given point, or
    //    null if none does.
    private static BattleHex getHexContainingPoint(Point point)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupBattleHexes.show[i][j] && h[i][j].contains(point))
                {
                    return h[i][j];
                }
            }
        }

        return null;
    }


    // Return the Critter whose chit contains the given point,
    //   or null if none does.
    private Critter getCritterWithChitContainingPoint(Point point)
    {
        int numCritters = battle.getNumCritters();
        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = battle.getCritter(i);
            Chit chit = critter.getChit();
            if (chit.contains(point))
            {
                return critter;
            }
        }

        return null;
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
    public static void setEraseFlag()
    {
        eraseFlag = true;
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

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
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        // If the erase flag is set, erase the background.
        if (eraseFlag)
        {
            offGraphics.clearRect(0, 0, d.width, d.height);
            eraseFlag = false;
        }

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupBattleHexes.show[i][j] &&
                    rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(offGraphics);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = battle.getNumCritters() - 1; i >= 0; i--)
        {
            Chit chit = battle.getCritter(i).getChit();
            if (rectClip.intersects(chit.getBounds()))
            {
                chit.paint(offGraphics);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
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
