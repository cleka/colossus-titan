import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public final class BattleMap extends HexMap implements MouseListener,
    WindowListener
{
    private int chitScale;
    private static Point location;

    private MasterBoard board;
    private Battle battle;

    private JFrame battleFrame;
    private JMenuBar menuBar;
    private JMenu phaseMenu;

    static final String undoLastMove = "Undo Last Move";
    static final String undoAllMoves = "Undo All Moves";
    static final String doneWithMoves = "Done with Moves";
    static final String doneWithStrikes = "Done with Strikes";
    static final String concedeBattle = "Concede Battle";

    private AbstractAction undoLastMoveAction;
    private AbstractAction undoAllMovesAction;
    private AbstractAction doneWithMovesAction;
    private AbstractAction doneWithStrikesAction;
    private AbstractAction concedeBattleAction;


    public BattleMap(MasterBoard board, MasterHex masterHex, Battle battle,
        boolean inProgress)
    {
        super(masterHex);

        battleFrame = new JFrame();

        Legion attacker = battle.getAttacker();
        Legion defender = battle.getDefender();

        Game.logEvent("\n" + attacker.getLongMarkerName() + " (" +
            attacker.getPlayer().getName() + ") attacks " +
            defender.getLongMarkerName() + " (" +
            defender.getPlayer().getName() + ")" + " in " +
            masterHex.getDescription());

        this.board = board;
        this.battle = battle;

        Container contentPane = battleFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        chitScale = 2 * scale;

        battleFrame.setSize(getPreferredSize());

        setupIcon();

        battleFrame.addWindowListener(this);
        addMouseListener(this);

        setupActions();
        setupTopMenu();

        setupEntrances();

        placeLegion(attacker, false, inProgress);
        placeLegion(defender, true, inProgress);

        if (location == null)
        {
            location = new Point(0, 2 * scale);
        }
        battleFrame.setLocation(location);

        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);
        battleFrame.pack();

        battleFrame.setVisible(true);
    }


    private void setupActions()
    {
        undoLastMoveAction = new AbstractAction(undoLastMove)
        {
            public void actionPerformed(ActionEvent e)
            {
                battle.undoLastMove();
            }
        };

        undoAllMovesAction = new AbstractAction(undoAllMoves)
        {
            public void actionPerformed(ActionEvent e)
            {
                battle.undoAllMoves();
            }
        };

        doneWithMovesAction = new AbstractAction(doneWithMoves)
        {
            public void actionPerformed(ActionEvent e)
            {
                battle.doneWithMoves();
            }
        };

        doneWithStrikesAction = new AbstractAction(doneWithStrikes)
        {
            public void actionPerformed(ActionEvent e)
            {
                battle.doneWithStrikes();
            }
        };

        concedeBattleAction = new AbstractAction(concedeBattle)
        {
            public void actionPerformed(ActionEvent e)
            {
                // XXX: Since the UI is shared between players for the
                // hotseat game, we will assume that the active player
                // is the one conceding.  This will change later.
                battle.tryToConcede();
            }
        };
    }


    private void setupTopMenu()
    {
        menuBar = new JMenuBar();
        battleFrame.setJMenuBar(menuBar);

        // Phase menu items change by phase and will be set up later.
        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(phaseMenu);
    }


    public void setupSummonMenu()
    {
         battleFrame.setTitle(battle.getActivePlayer().getName() + " Turn " +
            battle.getTurnNumber() + " : Summon");
         phaseMenu.removeAll();
    }


    public void setupRecruitMenu()
    {
         battleFrame.setTitle(battle.getActivePlayer().getName() + " Turn " +
            battle.getTurnNumber() + " : Recruit");
         phaseMenu.removeAll();
    }


    public void setupMoveMenu()
    {
        battleFrame.setTitle(battle.getActivePlayer().getName() +
            " Turn " + battle.getTurnNumber() + " : Move");

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(undoLastMoveAction);
        mi.setMnemonic(KeyEvent.VK_U);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));

        mi = phaseMenu.add(undoAllMovesAction);
        mi.setMnemonic(KeyEvent.VK_A);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));

        mi = phaseMenu.add(doneWithMovesAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));
    }


    public void setupFightMenu()
    {
         battleFrame.setTitle(battle.getActivePlayer().getName() +
                ((battle.getPhase() == Battle.FIGHT) ?
                " : Strike" : " : Strikeback"));

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(doneWithStrikesAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));
    }


    public JFrame getFrame()
    {
        return battleFrame;
    }


    private void setupIcon()
    {
        if (board != null && !board.getGame().isApplet())
        {
            try
            {
                battleFrame.setIconImage(
                    Toolkit.getDefaultToolkit().getImage(
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
        critter.addBattleInfo(entrance, entrance, this, chit, battle);
        entrance.addCritter(critter);

        chit.repaint();
    }


    private void placeLegion(Legion legion, boolean inverted,
        boolean inProgress)
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

            BattleHex currentHex;
            BattleHex startingHex;
            if (inProgress)
            {
                String currentHexLabel = critter.getCurrentHexLabel();
                String startingHexLabel = critter.getStartingHexLabel();

                if (currentHexLabel.equals("entrance"))
                {
                    currentHex = entrance;
                }
                else
                {
                    currentHex = getHexFromLabel(critter.getCurrentHexLabel());
                }
                if (startingHexLabel.equals("entrance"))
                {
                    startingHex = entrance;
                }
                else
                {
                    startingHex = getHexFromLabel(
                        critter.getStartingHexLabel());
                }
            }
            else
            {
                currentHex = startingHex = entrance;
            }
            critter.addBattleInfo(currentHex, startingHex, this, chit, battle);
            currentHex.addCritter(critter);
        }
    }


    public BattleHex getEntrance(Legion legion)
    {
        Legion attacker = battle.getAttacker();
        if (legion == attacker)
        {
            return entrances[attacker.getEntrySide()];
        }
        else
        {
            return entrances[Hex.oppositeHexsideNum(attacker.getEntrySide())];
        }
    }


    /** Return the Critter whose chit contains the given point,
     *  or null if none does. */
    private Critter getCritterAtPoint(Point point)
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


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        Critter critter = getCritterAtPoint(point);
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


    public void windowClosing(WindowEvent e)
    {
        if (board != null)
        {
            board.getGame().dispose();
        }
        battle.cleanup();
    }


    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Rectangle rectClip = g.getClipBounds();

        // Abort if called too early.
        if (rectClip == null)
        {
            return;
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
                chit.paintComponent(g);
            }
        }
    }


    public void dispose()
    {
        // Save location for next object.
        location = getLocation();

        battleFrame.dispose();
    }


    public static void main(String [] args)
    {
        Battle.main(args);
    }
}
