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
    private Client client;
    // XXX Remove battle reference
    private Battle battle;
    private boolean critterSelected;

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


    public BattleMap(Client client, String masterHexLabel, Battle battle)
    {
        super(masterHexLabel);

        this.client = client;

        battleFrame = new JFrame();
        Legion attacker = battle.getAttacker();
        Legion defender = battle.getDefender();

        Log.event(attacker.getLongMarkerName() + " (" +
            attacker.getPlayerName() + ") attacks " +
            defender.getLongMarkerName() + " (" +
            defender.getPlayerName() + ")" + " in " +
            MasterBoard.getHexByLabel(masterHexLabel).getDescription());

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
    public BattleMap(String masterHexLabel)
    {
        super(masterHexLabel);
        setupEntrances();
    }


    public BattleMap AICopy(Battle battle)
    {
        BattleMap newMap = new BattleMap(masterHexLabel);
        newMap.battle = battle;
        return newMap;
    }


    private void setupActions()
    {
        undoLastMoveAction = new AbstractAction(undoLastMove)
        {
            public void actionPerformed(ActionEvent e)
            {
                critterSelected = false;
                battle.undoLastMove();
                highlightMobileCritters();
            }
        };

        undoAllMovesAction = new AbstractAction(undoAllMoves)
        {
            public void actionPerformed(ActionEvent e)
            {
                critterSelected = false;
                battle.undoAllMoves();
                highlightMobileCritters();
            }
        };

        doneWithMovesAction = new AbstractAction(doneWithMoves)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (battle.anyOffboardCreatures())
                {
                    if (!client.getOption(Options.autoBattleMove) &&
                        !confirmLeavingCreaturesOffboard())
                    {
                        return;
                    }
                }
                battle.doneWithMoves();
            }
        };

        doneWithStrikesAction = new AbstractAction(doneWithStrikes)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (battle.isForcedStrikeRemaining())
                {
                    highlightCrittersWithTargets();
                    JOptionPane.showMessageDialog(battleFrame,
                        "Engaged creatures must strike.");
                }
                else
                {
                    battle.doneWithStrikes();
                }
            }
        };

        concedeBattleAction = new AbstractAction(concedeBattle)
        {
            public void actionPerformed(ActionEvent e)
            {
                String [] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(battleFrame,
                    "Are you sure you wish to concede the battle?",
                    "Confirm Concession?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    String playerName = client.getPlayerName();
                    Log.event(playerName + " concedes the battle");
                    client.concede(battle.getLegionByPlayerName(playerName).
                        getMarkerId());
                };
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

        battleFrame.setTitle(battle.getActivePlayerName() +
            " Turn " + battle.getTurnNumber() + " : Summon");
        phaseMenu.removeAll();

        requestFocus();
    }


    public void setupRecruitMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(battle.getActivePlayerName() +
            " Turn " + battle.getTurnNumber() + " : Recruit");
        if (phaseMenu != null)
        {
            phaseMenu.removeAll();
        }

        requestFocus();
    }


    public void setupMoveMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(battle.getActivePlayerName() +
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

        highlightMobileCritters();
        requestFocus();
    }


    public void setupFightMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(battle.getActivePlayerName() +
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

        highlightCrittersWithTargets();
        requestFocus();
    }


    public JFrame getFrame()
    {
        return battleFrame;
    }


    private void setupIcon()
    {
        try
        {
            battleFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(
                getClass().getResource(Chit.getImagePath(
                Creature.colossus.getImageName()))));
        }
        catch (NullPointerException e)
        {
            Log.error(e.toString() + " Couldn't find " +
                Creature.colossus.getImageName());
            dispose();
        }
    }


    public void placeNewChit(Critter critter, boolean inverted)
    {
        BattleHex entrance = getEntrance(terrain, masterHexLabel,
            critter.getLegion());
        String entranceLabel = entrance.getLabel();

        // Add chit to client.
        client.addBattleChit(
            critter.getImageName(inverted), critter);
        critter.addBattleInfo(entranceLabel, entranceLabel, battle);
        alignChits(entranceLabel);
    }


    private void placeLegion(Legion legion, boolean inverted)
    {
        BattleHex entrance = getEntrance(terrain, masterHexLabel, legion);
        String entranceLabel = entrance.getLabel();
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            // Add chit to client
            client.addBattleChit(critter.getImageName(inverted), critter);

            String currentHexLabel = critter.getCurrentHexLabel();
            if (currentHexLabel == null)
            {
                currentHexLabel = entranceLabel;
            }
            String startingHexLabel = critter.getStartingHexLabel();
            if (startingHexLabel == null)
            {
                startingHexLabel = entranceLabel;
            }

            critter.addBattleInfo(currentHexLabel, startingHexLabel,
                battle);
            alignChits(currentHexLabel);
        }
    }


    public static BattleHex getEntrance(char terrain, String masterHexLabel,
        Legion legion)
    {
        int side = legion.getEntrySide(masterHexLabel);
        return HexMap.getHexByLabel(terrain, "X" + side);
    }


    /** Return the Critter whose chit contains the given point,
     *  or null if none does. */
    private BattleChit getBattleChitAtPoint(Point point)
    {
        Iterator it = client.getBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.contains(point))
            {
                return chit;
            }
        }
        return null;
    }


    public void alignChits(String hexLabel)
    {
        GUIBattleHex hex = getGUIHexByLabel(hexLabel);
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
            BattleChit chit = client.getBattleChit(critter.getTag());
            chit.setLocation(point);
            point.x += chitScale4;
            point.y += chitScale4;
        }
        hex.repaint();
    }

    public void alignChits(Set hexLabels)
    {
        Iterator it = hexLabels.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            alignChits(hexLabel);
        }
    }


    /** Select all hexes containing critters eligible to move.
     *  Return the number of hexes selected (not the number
     *  of critters). */
    public int highlightMobileCritters()
    {
        Set set = battle.findMobileCritters();
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }

    public void highlightMoves(Critter critter)
    {
        Set set = battle.showMoves(critter, false);
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    /** Select hexes containing critters that have valid strike targets.
     *  Return the number of selected hexes. */
    public int highlightCrittersWithTargets()
    {
        Set set = battle.findCrittersWithTargets();
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }

    /** Highlight all hexes with targets that the critter can strike.
     *  Return the number of hexes highlighted. */
    public int highlightStrikes(Critter critter)
    {
        Set set = battle.findStrikes(critter, true);
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }


    public int highlightCarries()
    {
        Set set = battle.findCarryTargets();
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }


    private boolean confirmLeavingCreaturesOffboard()
    {
        String [] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(battleFrame,
            "Are you sure you want to leave creatures offboard?",
            "Confirm Leaving Creatures Offboard?",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[1]);
        return (answer == JOptionPane.YES_OPTION);
    }


    private void actOnCritter(Critter critter)
    {
        // Only the active player can move or strike.
        if (critter != null && critter.getPlayer() == battle.getActivePlayer())
        {
            critterSelected = true;

            // Put selected chit at the top of the z-order.
            if (battle.getActiveLegion().moveToTop(critter))
            {
                getGUIHexByLabel(critter.getCurrentHexLabel()).repaint();
            }

            switch (battle.getPhase())
            {
                case Battle.MOVE:
                    // Highlight all legal destinations for this critter.
                    highlightMoves(critter);
                    break;

                case Battle.FIGHT:
                case Battle.STRIKEBACK:
                    // Leave carry mode.
                    battle.clearAllCarries();

                    // Highlight all legal strikes for this critter.
                    highlightStrikes(critter);
                    break;

                default:
                    break;
            }
        }
    }


    public void actOnHex(BattleHex hex)
    {
        switch (battle.getPhase())
        {
            case Battle.MOVE:
                if (critterSelected)
                {
                    battle.doMove(battle.getActiveLegion().getCritter(0), hex);
                    critterSelected = false;
                    highlightMobileCritters();
                }
                break;

            case Battle.FIGHT:
            case Battle.STRIKEBACK:
                if (battle.getCarryDamage() > 0)
                {
                    battle.applyCarries(battle.getCritter(hex));
                }
                else if (critterSelected)
                {
                    battle.getActiveLegion().getCritter(0).strike(
                        battle.getCritter(hex), false);
                    critterSelected = false;
                }

                if (battle.getCarryDamage() == 0)
                {
                    Player player = battle.getActivePlayer();
                    if (client.getOption(Options.autoForcedStrike))
                    {
                        battle.makeForcedStrikes(false);
                    }
                    highlightCrittersWithTargets();
                }
                break;

            default:
                break;
        }
    }

    private void actOnMisclick()
    {
        switch (battle.getPhase())
        {
            case Battle.MOVE:
                critterSelected = false;
                highlightMobileCritters();
                break;

            case Battle.FIGHT:
            case Battle.STRIKEBACK:
                critterSelected = false;
                highlightCrittersWithTargets();
                break;

            default:
                break;
        }
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        BattleChit chit = getBattleChitAtPoint(point);
        BattleHex hex = getHexContainingPoint(point);

        Critter critter = battle.getCritter(chit.getTag());

        // Only the active player can move or strike.
        if (critter != null && critter.getPlayerName() ==
            battle.getActivePlayerName())
        {
            actOnCritter(critter);
        }

        // No hits on chits, so check map.
        else if (hex != null && hex.isSelected())
        {
            actOnHex(hex);
        }

        // No hits on selected hexes, so clean up.
        else
        {
            actOnMisclick();
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
            BattleChit chit = client.getBattleChit(critter.getTag());
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


    public void rescale()
    {
        setupHexes();
        setupEntrances();
        placeLegion(battle.getDefender(), true);
        placeLegion(battle.getAttacker(), false);
    }
}
