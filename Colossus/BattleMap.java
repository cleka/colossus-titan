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
    private BattleHex[][] h = new BattleHex[6][6];

    // ne, e, se, sw, w, nw
    private BattleHex [] entrances = new BattleHex[6];

    private static final boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };
    private Rectangle rectClip = new Rectangle();
    private Image offImage;
    private Graphics offGraphics;
    private Dimension offDimension;
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private boolean eraseFlag = false;

    private static int scale;
    private static int chitScale;

    private BattleTurn turn;
    private MasterBoard board;
    private MasterHex masterHex;
    private ShowDice showDice;
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

        setLayout(null);

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
        chitScale = 2 * scale;

        pack();
        setSize(getPreferredSize());

        setupIcon();

        setBackground(Color.white);
        addWindowListener(this);
        addMouseListener(this);

        validate();

        // Initialize the hexmap.
        setupHexes();

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


    public void unselectAllHexes()
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].isSelected())
                {
                    h[i][j].unselect();
                    h[i][j].repaint();
                }
            }
        }
    }


    public void highlightUnoccupiedTowerHexes()
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


    public BattleTurn getTurn()
    {
        return turn;
    }


    public ShowDice getShowDice()
    {
        return showDice;
    }


    private void setupHexes()
    {
        int cx = 6 * scale;
        int cy = 3 * scale;

        // Initialize hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new BattleHex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + (i & 1)) *
                        BattleHex.SQRT3 * scale), scale, this, i, j);
                }
            }
        }


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


        // Add terrain, hexsides, elevation, and exits to hexes.
        // Cliffs are bidirectional; other hexside obstacles are noted
        // only on the high side, since they only interfere with
        // uphill movement.
        switch (masterHex.getTerrain())
        {
            case 'P':
                break;

            case 'W':
                h[0][2].setTerrain('t');
                h[2][3].setTerrain('t');
                h[3][5].setTerrain('t');
                h[4][1].setTerrain('t');
                h[4][3].setTerrain('t');

                h[0][2].setElevation(1);
                h[2][3].setElevation(1);
                h[3][5].setElevation(1);
                h[4][1].setElevation(1);
                h[4][3].setElevation(1);
                break;

            case 'D':
                h[0][3].setTerrain('s');
                h[0][4].setTerrain('s');
                h[1][3].setTerrain('s');
                h[3][0].setTerrain('s');
                h[3][1].setTerrain('s');
                h[3][2].setTerrain('s');
                h[3][5].setTerrain('s');
                h[4][1].setTerrain('s');
                h[4][2].setTerrain('s');
                h[4][5].setTerrain('s');
                h[5][1].setTerrain('s');

                h[0][3].setHexside(0, 'd');
                h[0][3].setHexside(1, 'd');
                h[1][3].setHexside(0, 'd');
                h[1][3].setHexside(1, 'd');
                h[1][3].setHexside(2, 'd');
                h[1][3].setHexside(3, 'c');
                h[3][1].setHexside(4, 'd');
                h[3][2].setHexside(2, 'd');
                h[3][2].setHexside(3, 'c');
                h[3][2].setHexside(4, 'c');
                h[3][2].setHexside(5, 'd');
                h[3][5].setHexside(0, 'd');
                h[3][5].setHexside(5, 'd');
                h[4][2].setHexside(2, 'd');
                h[4][2].setHexside(3, 'd');
                h[4][5].setHexside(0, 'c');
                h[4][5].setHexside(1, 'd');
                h[4][5].setHexside(5, 'd');
                break;

            case 'B':
                h[0][2].setTerrain('r');
                h[1][3].setTerrain('r');
                h[2][2].setTerrain('r');
                h[3][1].setTerrain('r');
                h[3][4].setTerrain('r');
                h[3][5].setTerrain('r');
                h[4][3].setTerrain('r');
                h[5][1].setTerrain('r');
                break;

            case 'J':
                h[0][3].setTerrain('r');
                h[1][1].setTerrain('t');
                h[2][1].setTerrain('r');
                h[2][3].setTerrain('r');
                h[2][5].setTerrain('r');
                h[3][2].setTerrain('r');
                h[3][3].setTerrain('t');
                h[4][4].setTerrain('r');
                h[5][1].setTerrain('r');
                h[5][2].setTerrain('t');

                h[1][1].setElevation(1);
                h[3][3].setElevation(1);
                h[5][2].setElevation(1);
                break;

            case 'M':
                h[0][2].setTerrain('o');
                h[2][3].setTerrain('o');
                h[2][4].setTerrain('o');
                h[3][1].setTerrain('o');
                h[4][3].setTerrain('o');
                h[4][5].setTerrain('o');
                break;

            case 'S':
                h[1][3].setTerrain('o');
                h[2][1].setTerrain('o');
                h[2][2].setTerrain('t');
                h[2][4].setTerrain('t');
                h[3][3].setTerrain('o');
                h[3][5].setTerrain('o');
                h[4][2].setTerrain('t');
                h[5][3].setTerrain('o');

                h[2][2].setElevation(1);
                h[2][4].setElevation(1);
                h[4][2].setElevation(1);
                break;

            case 'H':
                h[2][2].setTerrain('t');
                h[2][4].setTerrain('t');
                h[5][3].setTerrain('t');

                h[1][2].setElevation(1);
                h[1][4].setElevation(1);
                h[2][2].setElevation(1);
                h[2][4].setElevation(1);
                h[3][0].setElevation(1);
                h[3][4].setElevation(1);
                h[4][3].setElevation(1);
                h[5][3].setElevation(1);

                h[1][2].setHexside(0, 's');
                h[1][2].setHexside(1, 's');
                h[1][2].setHexside(2, 's');
                h[1][2].setHexside(3, 's');
                h[1][2].setHexside(4, 's');
                h[1][2].setHexside(5, 's');
                h[1][4].setHexside(0, 's');
                h[1][4].setHexside(1, 's');
                h[1][4].setHexside(2, 's');
                h[1][4].setHexside(5, 's');
                h[3][0].setHexside(2, 's');
                h[3][0].setHexside(3, 's');
                h[3][0].setHexside(4, 's');
                h[3][4].setHexside(0, 's');
                h[3][4].setHexside(1, 's');
                h[3][4].setHexside(2, 's');
                h[3][4].setHexside(3, 's');
                h[3][4].setHexside(4, 's');
                h[3][4].setHexside(5, 's');
                h[4][3].setHexside(0, 's');
                h[4][3].setHexside(1, 's');
                h[4][3].setHexside(2, 's');
                h[4][3].setHexside(3, 's');
                h[4][3].setHexside(4, 's');
                h[4][3].setHexside(5, 's');
                break;

            case 'm':
                h[3][2].setTerrain('v');

                h[0][4].setElevation(1);
                h[1][1].setElevation(1);
                h[1][3].setElevation(1);
                h[1][4].setElevation(2);
                h[2][1].setElevation(2);
                h[2][2].setElevation(1);
                h[2][5].setElevation(1);
                h[3][0].setElevation(2);
                h[3][1].setElevation(1);
                h[3][2].setElevation(2);
                h[3][3].setElevation(1);
                h[4][1].setElevation(1);
                h[4][2].setElevation(1);
                h[4][3].setElevation(1);
                h[5][1].setElevation(2);
                h[5][2].setElevation(1);
                h[5][3].setElevation(2);
                h[5][4].setElevation(1);

                h[0][4].setHexside(0, 's');
                h[1][1].setHexside(3, 's');
                h[1][1].setHexside(4, 's');
                h[1][3].setHexside(0, 's');
                h[1][3].setHexside(1, 's');
                h[1][3].setHexside(2, 's');
                h[1][3].setHexside(5, 's');
                h[1][4].setHexside(0, 's');
                h[1][4].setHexside(1, 'c');
                h[1][4].setHexside(2, 's');
                h[1][4].setHexside(5, 's');
                h[2][1].setHexside(2, 's');
                h[2][1].setHexside(3, 's');
                h[2][1].setHexside(4, 's');
                h[2][2].setHexside(3, 's');
                h[2][2].setHexside(4, 's');
                h[2][5].setHexside(0, 's');
                h[2][5].setHexside(1, 's');
                h[2][5].setHexside(2, 's');
                h[3][0].setHexside(2, 's');
                h[3][0].setHexside(3, 's');
                h[3][2].setHexside(0, 's');
                h[3][2].setHexside(1, 's');
                h[3][2].setHexside(2, 's');
                h[3][2].setHexside(3, 's');
                h[3][2].setHexside(4, 'c');
                h[3][2].setHexside(5, 's');
                h[3][3].setHexside(2, 's');
                h[3][3].setHexside(3, 's');
                h[3][3].setHexside(4, 's');
                h[3][3].setHexside(5, 's');
                h[4][3].setHexside(3, 's');
                h[5][1].setHexside(3, 's');
                h[5][1].setHexside(4, 's');
                h[5][1].setHexside(5, 's');
                h[5][3].setHexside(0, 's');
                h[5][3].setHexside(3, 's');
                h[5][3].setHexside(4, 'c');
                h[5][3].setHexside(5, 's');
                h[5][4].setHexside(4, 's');
                h[5][4].setHexside(5, 's');
                break;

            case 't':
                h[0][4].setTerrain('d');
                h[1][3].setTerrain('d');
                h[2][1].setTerrain('d');
                h[2][2].setTerrain('d');
                h[2][4].setTerrain('d');
                h[3][3].setTerrain('d');
                h[4][2].setTerrain('d');
                h[4][5].setTerrain('d');
                h[5][3].setTerrain('d');
                break;

            case 'T':
                h[2][2].setElevation(1);
                h[2][3].setElevation(1);
                h[3][1].setElevation(1);
                h[3][2].setElevation(2);
                h[3][3].setElevation(1);
                h[4][2].setElevation(1);
                h[4][3].setElevation(1);

                h[2][2].setHexside(0, 'w');
                h[2][2].setHexside(4, 'w');
                h[2][2].setHexside(5, 'w');
                h[2][3].setHexside(3, 'w');
                h[2][3].setHexside(4, 'w');
                h[2][3].setHexside(5, 'w');
                h[3][1].setHexside(0, 'w');
                h[3][1].setHexside(1, 'w');
                h[3][1].setHexside(5, 'w');
                h[3][2].setHexside(0, 'w');
                h[3][2].setHexside(1, 'w');
                h[3][2].setHexside(2, 'w');
                h[3][2].setHexside(3, 'w');
                h[3][2].setHexside(4, 'w');
                h[3][2].setHexside(5, 'w');
                h[3][3].setHexside(2, 'w');
                h[3][3].setHexside(3, 'w');
                h[3][3].setHexside(4, 'w');
                h[4][2].setHexside(0, 'w');
                h[4][2].setHexside(1, 'w');
                h[4][2].setHexside(2, 'w');
                h[4][3].setHexside(1, 'w');
                h[4][3].setHexside(2, 'w');
                h[4][3].setHexside(3, 'w');
                break;
        }


        // Add references to neighbor hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    if (j > 0 && show[i][j - 1])
                    {
                        h[i][j].setNeighbor(0, h[i][j - 1]);
                    }

                    if (i < 5 && show[i + 1][j - ((i + 1) & 1)])
                    {
                        h[i][j].setNeighbor(1, h[i + 1][j - ((i + 1) & 1)]);
                    }

                    if (i < 5 && j + (i & 1) < 6 && show[i + 1][j + (i & 1)])
                    {
                        h[i][j].setNeighbor(2, h[i + 1][j + (i & 1)]);
                    }

                    if (j < 5 && show[i][j + 1])
                    {
                        h[i][j].setNeighbor(3, h[i][j + 1]);
                    }

                    if (i > 0 && j + (i & 1) < 6 && show[i - 1][j + (i & 1)])
                    {
                        h[i][j].setNeighbor(4, h[i - 1][j + (i & 1)]);
                    }

                    if (i > 0 && show[i - 1][j - ((i + 1) & 1)])
                    {
                        h[i][j].setNeighbor(5, h[i - 1][j - ((i + 1) & 1)]);
                    }
                }
            }
        }


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


    // Return the BattleHex that contains the given point, or
    //    null if none does.
    private BattleHex getHexContainingPoint(Point point)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].contains(point))
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

        Critter critter = battle.getCritterContainingPoint(point);

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
                    if (battle.chitSelected())
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
                    else if (battle.chitSelected())
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
            board.disposeGame();
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
    
        // Abort if called too early.
        rectClip = g.getClipBounds();
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
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
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
    
    
    public Dimension getMapSize()
    {
        Rectangle xRect = h[5][3].getBounds();
        Rectangle yRect = h[3][5].getBounds();
        return new Dimension(xRect.x + xRect.width, yRect.y + yRect.height);
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
