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
    private boolean summoningAngel;
    private SummonAngel summonAngel;
    private Battle battle;
    private BattleMap map;
    private Turn turn;

    // Keep multiple quick clicks from popping up multiples
    // of the same dialog.
    private boolean dialogLock;

    private PopupMenu popupMenu;
    private MenuItem menuItemHex; 
    private MenuItem menuItemMap;

    // Last point clicked is needed for popup menus.
    private Point lastPoint;


    public MasterBoard(Game game, boolean newgame)
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

        // Initialize the popup menu.
        popupMenu = new PopupMenu();
        menuItemHex = new MenuItem("View Recruit Info");
        menuItemMap = new MenuItem("View BattleMap");
        popupMenu.add(menuItemHex);
        popupMenu.add(menuItemMap);
        add(popupMenu);
        menuItemHex.addActionListener(this);
        menuItemMap.addActionListener(this);

        // Initialize the hexmap.
        SetupMasterHexes.setupHexes(h, this);

        // This initialization needs to be skipped if we loaded a
        // game rather than starting a new one.
        if (newgame)
        {
            // Each player needs to pick his first legion marker.
            for (int i = 0; i < game.getNumPlayers(); i++)
            {
                do
                {
                    new PickMarker(this, game.getPlayer(i));
                }
                while (game.getPlayer(i).getSelectedMarker() == null);
                Game.logEvent(game.getPlayer(i).getName() + 
                    " selected initial marker");
                // Update status window to reflect marker taken.
                game.updateStatusScreen();
            }

            // Place initial legions.
            for (int i = 0; i < game.getNumPlayers(); i++)
            {
                // Lookup coords for chit starting from player[i].getTower()
                MasterHex hex = getHexFromLabel(100 *
                    game.getPlayer(i).getTower());
    
                Creature.titan.takeOne();
                Creature.angel.takeOne();
                Creature.ogre.takeOne();
                Creature.ogre.takeOne();
                Creature.centaur.takeOne();
                Creature.centaur.takeOne();
                Creature.gargoyle.takeOne();
                Creature.gargoyle.takeOne();

                Legion legion = new Legion(3 * scale,
                    game.getPlayer(i).getSelectedMarker(), null, this, 8,
                    hex, Creature.titan, Creature.angel, Creature.ogre,
                    Creature.ogre, Creature.centaur, Creature.centaur,
                    Creature.gargoyle, Creature.gargoyle, game.getPlayer(i));
    
                game.getPlayer(i).addLegion(legion);
                hex.addLegion(legion);
            }
        }

        // Update status window to reflect new legions.
        game.updateStatusScreen();

        tracker = new MediaTracker(this);

        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            for (int j = 0; j < player.getNumLegions(); j++)
            {
                tracker.addImage(player.getLegion(j).getMarker().getImage(), 0);
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

        if (newgame)
        {
            finishInit(true);
        }
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


    // These steps need to be delayed if we're loading a game. 
    public void finishInit(boolean newgame)
    {
        if (!newgame)
        {
            // Move all legions into their hexes.
            for (int i = 0; i < game.getNumPlayers(); i++)
            {
                Player player = game.getPlayer(i);
                for (int j = 0; j < player.getNumLegions(); j++)
                {
                    Legion legion = player.getLegion(j);
                    MasterHex hex = legion.getCurrentHex();
                    hex.addLegion(legion);
                }
            }
        }

        turn = new Turn(game, this);
        
        setVisible(true);
        repaint();
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


    public void dispose()
    {
        if (map != null)
        {
            map.dispose();
        }

        super.dispose();
    }


    // Do a brute-force search through the hex array, looking for
    //    a match.  Return the hex.
    public MasterHex getHexFromLabel(int label)
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
    private void clearAllNonFriendlyOccupiedEntrySides(Player player)
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


    public void highlightUnmovedLegions()
    {
        unselectAllHexes();

        Player player = game.getActivePlayer();
        player.unselectLegion();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            if (!legion.hasMoved())
            {
                MasterHex hex = legion.getCurrentHex();
                hex.select();
            }
        }

        repaint();
    }


    // Returns number of engagements found.
    public int highlightEngagements()
    {
        int count = 0;
        Player player = game.getActivePlayer();

        unselectAllHexes();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            MasterHex hex = legion.getCurrentHex();
            if (hex.getNumEnemyLegions(player) > 0)
            {
                count++;
                hex.select();
                hex.repaint();
            }
        }

        return count;
    }


    // Returns number of legions with summonable angels.
    public int highlightSummonableAngels(Legion legion)
    {
        unselectAllHexes();

        Player player = legion.getPlayer();
        player.unselectLegion();

        int count = 0;

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion candidate = player.getLegion(i);
            if (candidate != legion)
            {
                MasterHex hex = candidate.getCurrentHex();
                if ((candidate.numCreature(Creature.angel) > 0 ||
                    candidate.numCreature(Creature.archangel) > 0) &&
                    !hex.isEngagement())
                {

                    count++;
                    hex.select();
                    hex.repaint();
                }
            }
        }

        if (count > 0)
        {
            summoningAngel = true;
        }

        return count;
    }


    // Returns number of legions that can recruit.
    public int highlightPossibleRecruits()
    {
        int count = 0;
        Player player = game.getActivePlayer();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            if (legion.hasMoved() && legion.canRecruit())
            {
                Creature [] recruits = new Creature[5];
                if (Game.findEligibleRecruits(legion, recruits) >= 1)
                {
                    MasterHex hex = legion.getCurrentHex();
                    hex.select();
                    hex.repaint();
                    count++;
                }
            }
        }

        return count;
    }


    public void finishSummoningAngel()
    {
        summoningAngel = false;
        highlightEngagements();
        summonAngel = null;
        if (battle != null)
        {
            battle.finishSummoningAngel();
        }
    }


    public void setSummonAngel(SummonAngel summonAngel)
    {
        this.summonAngel = summonAngel;
    }


    public void finishBattle()
    {
        show();

        if (summoningAngel && summonAngel != null)
        {
            highlightSummonableAngels(summonAngel.getLegion());
            summonAngel.repaint();
        }
        else
        {
            highlightEngagements();
        }
        battle = null;
        map = null;

        turn.setVisible(true);
        turn.setEnabled(true);

        // Insert a blank line in the log file after each battle.
        Game.logEvent("\n");
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
                    switch (game.getPhase())
                    {
                        case Game.SPLIT:
                            // Need a legion marker to split.
                            if (player.getNumMarkersAvailable() == 0)
                            {
                                new MessageBox(this,
                                    "No markers are available.");
                                return;
                            }
                            // Don't allow extra splits in turn 1.
                            if (game.getTurnNumber() == 1 &&
                                player.getNumLegions() > 1)
                            {
                                new MessageBox(this,
                                    "Cannot split twice on Turn 1.");
                                return;
                            }

                            if (!dialogLock)
                            {
                                dialogLock = true;
                                new SplitLegion(this, legion, player);
                                dialogLock = false;
                            }
                                        
                            // Update status window.
                            game.updateStatusScreen();
                            // If we split, unselect this hex.
                            if (legion.getHeight() < 7)
                            {
                                MasterHex hex = legion.getCurrentHex();
                                hex.unselect();
                                hex.repaint();
                            }
                            return;

                        case Game.MOVE:
                            // Mark this legion as active.
                            player.selectLegion(legion);

                            // Highlight all legal destinations
                            // for this legion.
                            showMoves(legion);
                            return;

                        case Game.FIGHT:
                            // Fall through, to allow clicking on
                            // either engaged legion or the hex.
                            break;

                        case Game.MUSTER:
                            if (legion.hasMoved() && legion.canRecruit())
                            {
                                if (!dialogLock)
                                {
                                    dialogLock = true;
                                    new PickRecruit(this, legion);
                                    if (!legion.canRecruit())
                                    {
                                        legion.getCurrentHex().unselect();
                                        legion.getCurrentHex().repaint();

                                        game.updateStatusScreen();
                                    }
                                    dialogLock = false;
                                }
                            }

                            return;
                    }
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
            
            Player player = game.getActivePlayer();

            // Otherwise, the action to take depends on the phase.
            switch (game.getPhase())
            {
                // If we're moving, and have selected a legion which
                // has not yet moved, and this hex is a legal
                // destination, move the legion here.
                case Game.MOVE:
                    legion = player.getSelectedLegion();
                    if (legion != null && hex.isSelected())
                    {
                        // Pick teleport or normal move if necessary.
                        if (hex.getTeleported() && hex.canEnterViaLand())
                        {
                            chooseWhetherToTeleport(hex);
                        }

                        // If this is a tower hex, set the entry side
                        // to '3', regardless.
                        if (hex.getTerrain() == 'T')
                        {
                            hex.clearAllEntrySides();
                            hex.setEntrySide(3);
                        }
                        // If this is a teleport to a non-tower hex,
                        // then allow entry from all three sides.
                        else if (hex.getTeleported())
                        {
                            hex.setEntrySide(1);
                            hex.setEntrySide(3);
                            hex.setEntrySide(5);
                        }

                        // Pick entry side if hex is enemy-occupied
                        // and there is more than one possibility.
                        if (hex.isOccupied() &&
                            hex.getNumEntrySides() > 1)
                        {
                            // Only allow one PickEntrySide dialog.
                            if (!dialogLock)
                            {
                                dialogLock = true;
                                new PickEntrySide(this, hex);
                                dialogLock = false;
                            }
                        }

                        // Unless a PickEntrySide was cancelled or
                        // disallowed, execute the move.
                        if (!hex.isOccupied() ||
                            hex.getNumEntrySides() == 1)
                        {
                            // If the legion teleported, reveal a lord.
                            if (hex.getTeleported())
                            {

                                // If it was a Titan teleport, that 
                                // lord must be the titan.
                                if (hex.isOccupied())
                                {
                                    legion.revealCreatures(
                                        Creature.titan, 1);
                                }
                                else
                                {
                                    legion.revealTeleportingLord(this);
                                }
                            }

                            legion.moveToHex(hex);
                            legion.getStartingHex().repaint();
                            hex.repaint();
                        }

                        highlightUnmovedLegions();
                    }
                    else
                    {
                        highlightUnmovedLegions();
                    }
                    break;

                // If we're fighting and there is an engagement here,
                // resolve it.  If an angel is being summoned, mark
                // the donor legion instead.
                case Game.FIGHT:
                    if (summoningAngel)
                    {
                        Legion donor = hex.getFriendlyLegion(player);
                        player.selectLegion(donor);
                        if (summonAngel == null)
                        {
                            summonAngel = battle.getSummonAngel();
                        }
                        summonAngel.repaint();
                        donor.getMarker().repaint();
                    }

                    // Do not allow clicking on engagements if one is
                    // already being resolved.
                    else if (hex.isEngagement() && !dialogLock)
                    {
                        dialogLock = true;
                        Legion attacker = hex.getFriendlyLegion(player);
                        Legion defender = hex.getEnemyLegion(player);

                        if (defender.canFlee())
                        {
                            // Fleeing gives half points and denies the
                            // attacker the chance to summon an angel.
                            new Concede(this, defender, attacker, true);
                        }

                        if (hex.isEngagement())
                        {
                            // The attacker may concede now without
                            // allowing the defender a reinforcement.
                            new Concede(this, attacker, defender, false);

                            // The players may agree to a negotiated 
                            // settlement.
                            if (hex.isEngagement())
                            {
                                new Negotiate(this, attacker, defender);
                            }


                            if (!hex.isEngagement())
                            {
                                if (hex.getLegion(0) == defender &&
                                    defender.canRecruit())
                                {
                                    // If the defender won the battle
                                    // by agreement, he may recruit.
                                    if (!dialogLock)
                                    {
                                        dialogLock = true;
                                        new PickRecruit(this, defender);
                                        dialogLock = false;
                                    }
                                }
                                else if (hex.getLegion(0) == attacker && 
                                    attacker.getHeight() < 7 && 
                                    player.canSummonAngel())
                                {
                                    // If the attacker won the battle
                                    // by agreement, he may summon an
                                    // angel.
                                    summonAngel = new SummonAngel(this, 
                                        attacker);
                                }
                            }

                            // Battle
                            if (hex.isEngagement())
                            {
                                // Hide turn to keep it out of the way.
                                turn.setVisible(false);
                                turn.setEnabled(false);

                                // Reveal both legions to all players.
                                attacker.revealAllCreatures();
                                defender.revealAllCreatures();
                                battle = new Battle(this, attacker, defender,
                                    hex);
                                map = battle.getBattleMap();
                            }
                        }

                        highlightEngagements();
                        dialogLock = false;
                    }
                    break;

                default:
                    break;
            }

            hex.repaint();
            return;
        }

        // No hits on chits or map, so re-highlight.
        switch (game.getPhase())
        {
            case Game.MOVE:
                highlightUnmovedLegions();
                break;

            case Game.FIGHT:
                if (summoningAngel && summonAngel != null)
                {
                    highlightSummonableAngels(summonAngel.getLegion());
                    summonAngel.repaint();
                }
                else
                {
                    highlightEngagements();
                }
                break;

            default:
                break;
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
        return new Dimension(64 * scale, 60 * scale);
    }
}
