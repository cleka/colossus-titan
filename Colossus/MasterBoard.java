import java.awt.*;
import java.awt.event.*;

/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
 * @version $Id$
 * @author David Ripton
 */

public class MasterBoard extends Frame implements MouseListener,
    WindowListener, ActionListener
{
    // There are a total of 96 hexes
    // Their Titan labels are:
    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

    // For easy of mapping to the GUI, they'll be stored
    // in a 15x8 array, with some empty elements.

    private static MasterHex[][] h = new MasterHex[15][8];

    private Image offImage;
    private Graphics offGraphics;
    private Dimension offDimension;
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private static int scale;
    private static Game game;
    private boolean eraseFlag;

    private PopupMenu popupMenu;
    private MenuItem menuItemHex; 
    private MenuItem menuItemMap;

    // Last point clicked is needed for popup menus.
    private Point lastPoint;


    public MasterBoard(Game game)
    {
        super("MasterBoard");

        this.game = game;

        setLayout(null);

        scale = getScale();

        setSize(getPreferredSize());

        setupIcon();

        setBackground(Color.black);

        addWindowListener(this);
        addMouseListener(this);

        imagesLoaded = false;

        initializePopupMenu();

        SetupMasterHexes.setupHexes(h, this);
    }


    private void initializePopupMenu()
    {
        popupMenu = new PopupMenu();
        menuItemHex = new MenuItem("View Recruit Info");
        menuItemMap = new MenuItem("View BattleMap");
        popupMenu.add(menuItemHex);
        popupMenu.add(menuItemMap);
        add(popupMenu);
        menuItemHex.addActionListener(this);
        menuItemMap.addActionListener(this);
    }
                
                
    public void loadInitialMarkerImages()
    {
        tracker = new MediaTracker(this);

        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            for (int j = 0; j < player.getNumLegions(); j++)
            {
                tracker.addImage(player.getLegion(j).getMarker().getImage(),
                    0);
            }
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, e.toString() + " waitForAll was interrupted");
        }

        imagesLoaded = true;
    }
    
    
    private void setupIcon()
    {
        if (!game.isApplet())
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
                game.dispose();
            }
        }
    }


    public static int getScale()
    {
        int scale = 17;

        // Make sure that the board fits on the screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000)
        {
            scale = scale * d.height / 1000;
        }

        return scale;
    }


    public Game getGame()
    {
        return game;
    }


    // Do a brute-force search through the hex array, looking for
    //    a match.  Return the hex.
    public static MasterHex getHexFromLabel(int label)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupMasterHexes.show[i][j] && h[i][j].getLabel().equals(
                    Integer.toString(label)))
                {
                    return h[i][j];
                }
            }
        }

        // Error, so return a bogus hex.
        System.out.println("Could not find hex " + label);
        return new MasterHex(-1, -1, -1, false, null);
    }


    // Return the MasterHex that contains the given point, or
    //    null if none does.
    private MasterHex getHexContainingPoint(Point point)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupMasterHexes.show[i][j] && h[i][j].contains(point))
                {
                    return h[i][j];
                }
            }
        }

        return null;
    }
    
    
    // Return the Legion whose marker contains the given
    //    point, or null if none does.
    private Legion getLegionWithMarkerContainingPoint(Point point)
    {
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            for (int j = 0; j < player.getNumLegions(); j++)
            {
                Legion legion = player.getLegion(j);
                if (legion.getMarker().contains(point))
                {
                    return legion;
                }
            }
        }

        return null;
    }


    public void unselectAllHexes()
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupMasterHexes.show[i][j] && h[i][j].isSelected())
                {
                    h[i][j].unselect();
                    h[i][j].repaint();
                }
            }
        }
    }


    // Clear all entry side and teleport information from all hexes occupied
    // by one or fewer legions.
    public void clearAllNonFriendlyOccupiedEntrySides(Player player)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupMasterHexes.show[i][j] &&
                    h[i][j].getNumFriendlyLegions(player) == 0)
                {
                    h[i][j].clearAllEntrySides();
                    h[i][j].setTeleported(false);
                }
            }
        }
    }


    // Clear all entry side and teleport information from all hexes.
    public void clearAllEntrySides()
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupMasterHexes.show[i][j])
                {
                    h[i][j].clearAllEntrySides();
                    h[i][j].setTeleported(false);
                }
            }
        }
    }


    // Recursively find conventional moves from this hex.  Select
    //    all legal final destinations.  If block >= 0, go only
    //    that way.  If block == -1, use arches and arrows.  If
    //    block == -2, use only arrows.  Do not double back in
    //    the direction you just came from.  Return the number of
    //    moves found.
    private int findMoves(MasterHex hex, Player player, Legion legion,
        int roll, int block, int cameFrom, boolean show)
    {
        int count = 0;

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (hex.getNumEnemyLegions(player) > 0)
        {
            if (hex.getNumFriendlyLegions(player) == 0)
            {
                if (show)
                {
                    hex.select();
                    hex.repaint();

                    // Set the entry side relative to the hex label.
                    hex.setEntrySide((6 + cameFrom - hex.getLabelSide()) % 6);
                }
                
            }
            return count;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            for (int i = 0; i < player.getNumLegions(); i++)
            {
                // Account for spin cycles.
                if (player.getLegion(i).getCurrentHex() == hex &&
                    player.getLegion(i) != legion)
                {
                    return count;
                }
            }
            if (show)
            {
                hex.select();
                hex.repaint();
                // Need to set entry sides even if no possible engagement,
                // for MasterHex.chooseWhetherToTeleport()
                hex.setEntrySide((6 + cameFrom - hex.getLabelSide()) % 6);
            }

            count++;
            return count;
        }


        if (block >= 0)
        {
            count += findMoves(hex.getNeighbor(block), player, legion,
                roll - 1, -2, (block + 3) % 6, show);
        }
        else if (block == -1)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARCH && i != cameFrom)
                {
                    count += findMoves(hex.getNeighbor(i), player, legion,
                        roll - 1, -2, (i + 3) % 6, show);
                }
            }
        }
        else if (block == -2)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARROW && i != cameFrom)
                {
                    count += findMoves(hex.getNeighbor(i), player, legion,
                        roll - 1, -2, (i + 3) % 6, show);
                }
            }
        }

        return count;
    }


    // Recursively find tower teleport moves from this hex.  That's
    // all unoccupied hexes within 6 hexes.  Teleports to towers
    // are handled separately.  Do not double back.
    private void findTowerTeleportMoves(MasterHex hex, Player player,
        Legion legion, int roll, int cameFrom, boolean show)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.

        if (!hex.isOccupied())
        {
            if (show)
            {
                hex.select();
                hex.repaint();
            }
            // Mover can choose side of entry.
            hex.setTeleported(true);
        }

        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom && (hex.getExitType(i) != MasterHex.NONE ||
                   hex.getEntranceType(i) != MasterHex.NONE))
                {
                    findTowerTeleportMoves(hex.getNeighbor(i), player, legion,
                        roll - 1, (i + 3) % 6, show);
                }
            }
        }
    }

    
    // Return number of legal non-teleport moves.
    public int countMoves(Legion legion)
    {
        return countAndMaybeShowMoves(legion, false);
    }
    
    
    // Return number of legal non-teleport moves.
    public int showMoves(Legion legion)
    {
        return countAndMaybeShowMoves(legion, true);
    }


    // Return number of legal non-teleport moves.
    private int countAndMaybeShowMoves(Legion legion, boolean show)
    {
        if (show)
        {
            unselectAllHexes();
        }

        if (legion.hasMoved())
        {
            return 0;
        }
        
        Player player = legion.getPlayer();

        clearAllNonFriendlyOccupiedEntrySides(player);

        int count = 0;

        MasterHex hex = legion.getCurrentHex();

        // Conventional moves

        // First, look for a block.
        int block = -1;
        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == MasterHex.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }

        count += findMoves(hex, player, legion, player.getMovementRoll(),
            block, -1, show);

        if (player.getMovementRoll() == 6)
        {
            // Tower teleport
            if (hex.getTerrain() == 'T' && legion.numLords() > 0 &&
                player.canTeleport())
            {
                // Mark every unoccupied hex within 6 hexes.
                findTowerTeleportMoves(hex, player, legion, 6, -1, show);

                // Mark every unoccupied tower.
                for (int tower = 100; tower <= 600; tower += 100)
                {
                    hex = getHexFromLabel(tower);
                    if (!hex.isOccupied())
                    {
                        if (show)
                        {
                            hex.select();
                            hex.repaint();
                            // Mover can choose side of entry.
                            hex.setTeleported(true);
                        }
                    }
                }
            }

            // Titan teleport
            if (player.canTitanTeleport() &&
                legion.numCreature(Creature.titan) > 0)
            {
                // Mark every hex containing an enemy stack that does not
                // already contain a friendly stack.
                for (int i = 0; i < game.getNumPlayers(); i++)
                {
                    if (game.getPlayer(i) != player)
                    {
                        for (int j = 0; j < game.getPlayer(i).getNumLegions();
                            j++)
                        {
                            hex = game.getPlayer(i).getLegion(j).
                                getCurrentHex();
                            if (!hex.isEngagement())
                            {
                                if (show)
                                {
                                    hex.select();
                                    hex.repaint();
                                    // Mover can choose side of entry.
                                    hex.setTeleported(true);
                                }
                            }
                        }
                    }
                }
            }
        }

        return count;
    }


    // Present a dialog allowing the player to enter via land or teleport.
    private void chooseWhetherToTeleport(MasterHex hex)
    {
        new OptionDialog(this, "Teleport?", "Teleport?", "Teleport", 
            "Move Normally");

        // If Teleport, then leave teleported set.
        if (OptionDialog.getLastAnswer() == OptionDialog.NO_OPTION)
        {
            hex.setTeleported(false);
        }
    }


    public void deiconify()
    {
        // setState(Frame.NORMAL) does not work under 1.1
        // removeNotify() then addNotify() causes problems with the Turn dialog
    }
                    
                    


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();

        Legion legion = getLegionWithMarkerContainingPoint(point);

        if (legion != null)
        {
            Player player = legion.getPlayer();

            // What to do depends on which mouse button was used
            // and the current phase of the turn.

            // Right-click means to show the contents of the 
            // legion.
            if (((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
                InputEvent.BUTTON2_MASK) || ((e.getModifiers() &
                InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK))
            {
                new ShowLegion(this, legion, point,
                    game.getAllVisible() || player == game.getActivePlayer());
                return;
            }
            else
            {
                // Only the current player can manipulate his legions.
                if (player == game.getActivePlayer())
                {
                    game.actOnLegion(legion);
                    return;
                }
            }
        }

        // No hits on chits, so check map.

        MasterHex hex = getHexContainingPoint(point);
        if (hex != null)
        {
            if (((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
                InputEvent.BUTTON2_MASK) || ((e.getModifiers() &
                InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK))
            {
                lastPoint = point;
                popupMenu.setLabel(hex.getDescription());
                popupMenu.show(e.getComponent(), point.x, point.y);

                return;
            }
            
            // Otherwise, the action to take depends on the phase.
            game.actOnHex(hex);
            hex.repaint();
            return;
        }

        // No hits on chits or map, so re-highlight.
        game.actOnMisclick();
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
        game.dispose();
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


    public void actionPerformed(ActionEvent e)
    {
        MasterHex hex = getHexContainingPoint(lastPoint);
        if (hex != null)
        {
            if (e.getActionCommand().equals("View Recruit Info"))
            {
                new ShowMasterHex(this, hex, lastPoint);
            }
            else if (e.getActionCommand().equals("View BattleMap"))
            {
                new ShowBattleMap(this, hex);
            }
        }
    }


    // Double-buffer everything.
    public void paint(Graphics g)
    {
        update(g);
    }


    // This is used to fix artifacts from legions hanging outside hexes.
    public void setEraseFlag()
    {
        eraseFlag = true;
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();

        Rectangle rectClip = g.getClipBounds();

        // Abort if called too early.
        if (rectClip == null)
        {
            return;
        }


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

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupMasterHexes.show[i][j] && 
                    rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(offGraphics);
                }
            }
        }

        // Paint in reverse order to make visible z-order match clicks.
        for (int i = game.getNumPlayers() - 1; i >= 0; i--)
        {
            Player player = game.getPlayer(i);
            for (int j = player.getNumLegions() - 1; j >= 0; j--)
            {
                if (rectClip.intersects(
                    player.getLegion(j).getMarker().getBounds()))
                {
                    player.getLegion(j).getMarker().paint(offGraphics);
                }
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public Dimension getMinimumSize()
    {
        return new Dimension(64 * scale, 58 * scale);
    }


    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }
}
