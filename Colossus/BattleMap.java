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

    private static BattleTurn turn;
    private static MasterBoard board;
    private static MasterHex masterHex;
    private static ShowDice showDice;
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

        BattleHex entrance = getEntrance(attacker);
        for (int i = 0; i < attacker.getHeight(); i++)
        {
            Critter critter = attacker.getCritter(i);
            battle.addCritter(critter);
            BattleChit chit = new BattleChit(chitScale, 
                critter.getImageName(false), this, critter);
            tracker.addImage(chit.getImage(), 0);
            critter.addBattleInfo(entrance, this, chit, battle);
            entrance.addCritter(critter);
        }
        entrance.alignChits();

        entrance = getEntrance(defender);
        for (int i = 0; i < defender.getHeight(); i++)
        {
            Critter critter = defender.getCritter(i);
            battle.addCritter(critter);
            BattleChit chit = new BattleChit(chitScale, 
                critter.getImageName(true), this, critter);
            tracker.addImage(chit.getImage(), 0);
            critter.addBattleInfo(entrance, this, chit, battle);
            entrance.addCritter(critter);
        }
        entrance.alignChits();

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, e.toString() + " waitForAll was interrupted");
        }
        imagesLoaded = true;

        turn = new BattleTurn(this, this, battle);
        showDice = new ShowDice(this);

        pack();
        
        if (location == null)
        {
            location = new Point(scale, scale);
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


    public void cleanup()
    {
        try
        {
            // Handle any after-battle angel summoning or recruiting.
            if (masterHex.getNumLegions() == 1)
            {
                Legion legion = masterHex.getLegion(0);
                if (legion == battle.getAttacker())
                {
                    // Summon angel
                    if (legion.canSummonAngel())
                    {
                        if (board != null)
                        {
                            // Make sure the MasterBoard is visible.
                            board.deiconify();
                            // And bring it to the front.
                            board.show();
        
                            SummonAngel summonAngel = new SummonAngel(board,
                                battle.getAttacker());
                            board.setSummonAngel(summonAngel);
                        }
                    }
                }
                else
                {
                    // Recruit reinforcement
                    if (legion.canRecruit())
                    {
                        new PickRecruit(this, legion);
                    }
                }
    
                // Make all creatures in the victorious legion visible.
                legion.revealAllCreatures();
    
                // Heal all creatures in the winning legion.
                legion.healAllCreatures();
            }
    
            if (turn != null)
            {
                turn.cleanup();
            }
    
            // Save location for next object.
            location = getLocation();
    
            dispose();
    
            masterHex.unselect();
            masterHex.repaint();
            board.finishBattle();
        }
        catch (NullPointerException e)
        {
            // Don't crash if we're testing battles with no MasterBoard.
            e.printStackTrace();
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


    public static BattleTurn getTurn()
    {
        return turn;
    }


    public static ShowDice getShowDice()
    {
        return showDice;
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


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        Player player = battle.getActivePlayer();

        Critter critter = battle.getCritterWithChitContainingPoint(point);

        // Only the active player can move or strike.
        if (critter != null && critter.getPlayer() == player)
        {
            battle.setChitSelected(); 

            // Put selected chit at the top of the Z-order.
            battle.moveToTop(critter);

            switch (battle.getPhase())
            {
                case Battle.MOVE:
                    // Highlight all legal destinations for this chit.
                    battle.showMoves(critter);
                    break;

                case Battle.FIGHT:
                case Battle.STRIKEBACK:
                    // Highlight all legal strikes for this chit.
                    battle.highlightStrikes(critter);

                    // Leave carry mode.
                    battle.clearAllCarries();
                    break;

                default:
                    break;
            }

            return;
        }
    
        // No hits on chits, so check map.
        BattleHex hex = getHexContainingPoint(point);
        if (hex != null && hex.isSelected())
        {
            switch (battle.getPhase())
            {
                case Battle.MOVE:
                    if (battle.isChitSelected())
                    {
                        battle.getCritter(0).moveToHex(hex);
                        battle.clearChitSelected();
                    }
                    battle.highlightMovableChits();
                    return;
    
                case Battle.FIGHT:
                case Battle.STRIKEBACK:
                    if (battle.getCarryDamage() > 0)
                    {
                        battle.applyCarries(hex.getCritter());
                    }
                    else if (battle.isChitSelected())
                    {
                        battle.getCritter(0).strike(hex.getCritter());
                        battle.clearChitSelected();
                    }
    
                    if (battle.getCarryDamage() == 0)
                    {
                        battle.highlightChitsWithTargets();
                    }
                    return;
    
                default:
                    return;
            }
        }
    
        // No hits on selected hexes, so clean up.
        switch (battle.getPhase())
        {
            case Battle.MOVE:
                battle.highlightMovableChits();
                break;
    
            case Battle.FIGHT:
            case Battle.STRIKEBACK:
                battle.highlightChitsWithTargets();
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
        if (board != null)
        {
            board.getGame().dispose();
        }
        dispose();
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
    
        // Abort if called too early.
        Rectangle rectClip = g.getClipBounds();
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
    
    
    public Dimension getMinimumSize()
    {
        return new Dimension(30 * scale, 28 * scale);
    }
    
    
    public Dimension getPreferredSize()
    {
        return new Dimension(30 * scale, 30 * scale);
    }
    
    
    public static void main(String [] args)
    {
        Player player1 = new Player("Attacker", null);
        Player player2 = new Player("Defender", null);
        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('J');
        hex.setEntrySide(3);
        Legion attacker = new Legion(chitScale, "Bk01", null, null, 7,
            hex, Creature.archangel, Creature.troll, Creature.ranger,
            Creature.hydra, Creature.griffon, Creature.angel,
            Creature.warlock, null, player1);
        Legion defender = new Legion(chitScale, "Rd01", null, null, 7,
            hex, Creature.serpent, Creature.lion, Creature.gargoyle,
            Creature.cyclops, Creature.gorgon, Creature.guardian,
            Creature.minotaur, null, player2);

        new Battle(null, attacker, defender, hex);
    }
}
