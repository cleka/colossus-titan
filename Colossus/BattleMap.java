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
    private static Point location;
    private JFrame battleFrame;
    private JMenuBar menuBar;
    private JMenu phaseMenu;
    private Battle battle;
    private int tag = 0;

    public static final String undoLastMove = "Undo Last Move";
    public static final String undoAllMoves = "Undo All Moves";
    public static final String doneWithMoves = "Done with Moves";
    public static final String doneWithStrikes = "Done with Strikes";
    public static final String concedeBattle = "Concede Battle";

    private AbstractAction undoLastMoveAction;
    private AbstractAction undoAllMovesAction;
    private AbstractAction doneWithMovesAction;
    private AbstractAction doneWithStrikesAction;
    private AbstractAction concedeBattleAction;


    public BattleMap(MasterBoard board, String masterHexLabel, Battle battle)
    {
        super(board, masterHexLabel);

        battleFrame = new JFrame();
        Legion attacker = battle.getAttacker();
        Legion defender = battle.getDefender();

        Game.logEvent(attacker.getLongMarkerName() + " (" +
            attacker.getPlayerName() + ") attacks " +
            defender.getLongMarkerName() + " (" +
            defender.getPlayerName() + ")" + " in " +
            board.getHexByLabel(masterHexLabel).getDescription());

        this.battle = battle;

        Container contentPane = battleFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        battleFrame.setSize(getPreferredSize());

        setupIcon();

        battleFrame.addWindowListener(this);
        addMouseListener(this);

        setupActions();
        setupTopMenu();

        setupEntrances();

        placeLegion(attacker, false);
        placeLegion(defender, true);

        if (location == null)
        {
            location = new Point(0, 4 * Scale.get());
        }
        battleFrame.setLocation(location);

        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);
        battleFrame.pack();

        battleFrame.setVisible(true);
    }


    // Simple constructor for testing and AICopy()
    public BattleMap(MasterBoard board, String masterHexLabel)
    {
        super(board, masterHexLabel);
        setupEntrances();
    }


    public BattleMap AICopy(Battle battle)
    {
        BattleMap newMap = new BattleMap(board, masterHexLabel);
        newMap.battle = battle;
        return newMap;
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
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(battle.getActivePlayer().getName() +
            " Turn " + battle.getTurnNumber() + " : Summon");
        phaseMenu.removeAll();
    }


    public void setupRecruitMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(battle.getActivePlayer().getName() +
            " Turn " + battle.getTurnNumber() + " : Recruit");
        if (phaseMenu != null)
        {
            phaseMenu.removeAll();
        }
    }


    public void setupMoveMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

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
        if (battleFrame == null)
        {
            return;
        }

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
        if (battle.getGame() != null && !battle.getGame().isApplet())
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
                Game.logError(e.toString() + " Couldn't find " +
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

        BattleChit chit = new BattleChit(4 * Scale.get(),
            critter.getImageName(legion == battle.getDefender()), this,
            critter);
        critter.addBattleInfo(entrance.getLabel(), entrance.getLabel(),
            chit, battle, ++tag);
        alignChits(entrance.getLabel());
    }


    private void placeLegion(Legion legion, boolean inverted)
    {
        BattleHex entrance = getEntrance(legion);
        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            BattleChit chit = new BattleChit(4 * Scale.get(),
                critter.getImageName(inverted), this, critter);

            String currentHexLabel = critter.getCurrentHexLabel();
            String startingHexLabel = critter.getStartingHexLabel();

            if (currentHexLabel == null)
            {
                currentHexLabel = entrance.getLabel();
            }
            if (startingHexLabel == null)
            {
                startingHexLabel = entrance.getLabel();
            }
            critter.addBattleInfo(currentHexLabel, startingHexLabel,
                chit, battle, ++tag);
            alignChits(currentHexLabel);
        }
    }


    public BattleHex getEntrance(Legion legion)
    {
        int side = battle.getAttacker().getEntrySide(
            battle.getMasterHexLabel());
        if (side == 1 || side == 3 || side ==5)
        {
            if (legion.getMarkerId().equals(battle.getAttackerId()))
            {
                return entrances[side];
            }
            else
            {
                return entrances[(side + 3) % 6];
            }
        }
        return null;
    }


    /** Return the Critter whose chit contains the given point,
     *  or null if none does. */
    private Critter getCritterAtPoint(Point point)
    {
        Iterator it = battle.getAllCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = critter.getChit();
            if (chit != null && chit.contains(point))
            {
                return critter;
            }
        }
        return null;
    }


    public void alignChits(String hexLabel)
    {
        BattleHex hex = getHexByLabel(hexLabel);
        ArrayList critters = battle.getCritters(hexLabel);
        int numCritters = critters.size();
        Point point = new Point(hex.findCenter());

        if (!hex.isEntrance() && numCritters > 1)
        {
            // The AI sometimes pretends two critters share a hex
            // for a while.  Don't paint that.
            numCritters = 1;
        }

        // Cascade chits diagonally.
        int chitScale4 = Scale.get();
        int offset = (4 * Scale.get() * (1 + (numCritters))) / 4;
        point.x -= offset;
        point.y -= offset;

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = (Critter)critters.get(i);
            BattleChit chit = critter.getChit();
            chit.setLocation(point);
            point.x += chitScale4;
            point.y += chitScale4;
        }
        hex.repaint();
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
        if (battle.getGame() != null)
        {
            battle.getGame().dispose();
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
        ArrayList critters = battle.getAllCritters();
        ListIterator lit = critters.listIterator(critters.size());
        while (lit.hasPrevious())
        {
            Critter critter = (Critter)lit.previous();
            Chit chit = critter.getChit();
            if (chit != null && rectClip.intersects(chit.getBounds()))
            {
                chit.paintComponent(g);
            }
        }
    }


    public void dispose()
    {
        // Save location for next object.
        location = getLocation();

        if (battleFrame != null)
        {
            battleFrame.dispose();
        }
    }
}
