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
    private JLabel playerLabel;

    // XXX Remove battle reference
    private Battle battle;
    private boolean critterSelected;

    public static final String undoLast = "Undo Last";
    public static final String undoAll = "Undo All";
    public static final String doneWithPhase = "Done";
    public static final String concedeBattle = "Concede Battle";

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;
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

        placeLegionChits(attacker, false);
        placeLegionChits(defender, true);

        if (location == null)
        {
            location = new Point(0, 4 * Scale.get());
        }
        battleFrame.setLocation(location);

        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);

        setupPlayerLabel();
        contentPane.add(playerLabel, BorderLayout.NORTH);

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
        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getPlayerName().equals(
                    battle.getActivePlayerName()))
                {
                    return;
                }
                if (battle.getPhase() == battle.MOVE)
                {
                    critterSelected = false;
                    battle.undoLastMove();
                    highlightMobileCritters();
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getPlayerName().equals(
                    battle.getActivePlayerName()))
                {
                    return;
                }
                if (battle.getPhase() == Battle.MOVE)
                {
                    critterSelected = false;
                    battle.undoAllMoves();
                    highlightMobileCritters();
                }
            }
        };

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getPlayerName().equals(
                    battle.getActivePlayerName()))
                {
                    return;
                }

                int phase = battle.getPhase();
                switch (phase)
                {
                    case Battle.MOVE:
                        if (battle.anyOffboardCreatures())
                        {
                            if (!client.getOption(Options.autoBattleMove) &&
                                !confirmLeavingCreaturesOffboard())
                            {
                                return;
                            }
                        }
                        battle.doneWithMoves();
                        break;

                    case Battle.FIGHT:
                    case Battle.STRIKEBACK:
                        if (battle.isForcedStrikeRemaining())
                        {
                            highlightCrittersWithTargets();
                            client.showMessageDialog(
                                "Engaged creatures must strike.");
                        }
                        else
                        {
                            battle.doneWithStrikes();
                        }
                        break;
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

        mi = phaseMenu.add(undoLastAction);
        mi.setMnemonic(KeyEvent.VK_U);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));

        mi = phaseMenu.add(undoAllAction);
        mi.setMnemonic(KeyEvent.VK_A);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        if (client.getPlayerName().equals(battle.getActivePlayerName()))
        {
            highlightMobileCritters();
            requestFocus();
        }
    }


    public void setupFightMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(battle.getActivePlayerName() +
            " Turn " + battle.getTurnNumber() +
            ((battle.getPhase() == Battle.FIGHT) ?
            " : Strike" : " : Strikeback"));
        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        if (client.getPlayerName().equals(battle.getActivePlayerName()))
        {
            highlightCrittersWithTargets();
            requestFocus();
        }
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


    /** Show which player owns this board. */
    public void setupPlayerLabel()
    {
        String playerName = client.getPlayerName();
        playerLabel = new JLabel(playerName);

        Player player = battle.getGame().getPlayer(playerName);
        String colorName = player.getColor();
        // If we call this before player colors are chosen, just use
        // the defaults.
        if (colorName != null)
        {
            Color color = PickColor.getBackgroundColor(colorName);
            playerLabel.setForeground(color);
        }
        playerLabel.repaint();
    }



    public void placeNewChit(Critter critter, boolean inverted)
    {
        // Add chit to client.
        client.addBattleChit(
            critter.getImageName(inverted), critter);
        alignChits(critter.getCurrentHexLabel());
    }


    private void placeLegionChits(Legion legion, boolean inverted)
    {
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            client.addBattleChit(critter.getImageName(inverted), critter);
            alignChits(critter.getCurrentHexLabel());
        }
    }


    public static BattleHex getEntrance(char terrain, String masterHexLabel,
        Legion legion)
    {
        int side = legion.getEntrySide(masterHexLabel);
        return HexMap.getHexByLabel(terrain, "X" + side);
    }


    /** Return the BattleChit containing the given point,
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


    // XXX Just do chits, without going through critters.
    public void alignChits(String hexLabel)
    {
        GUIBattleHex hex = getGUIHexByLabel(hexLabel);
        ArrayList critters = battle.getCritters(hexLabel);
        int numCritters = critters.size();
        if (numCritters < 1)
        {
            hex.repaint();
            return;
        }
        Critter critter = (Critter)critters.get(0);
        BattleChit chit = client.getBattleChit(critter.getTag());
        if (chit == null)
        {
            hex.repaint();
            return;
        }
        int chitscale = chit.getBounds().width;
        int chitscale4 = chitscale / 4;

        Point point = new Point(hex.findCenter());

        // Cascade chits diagonally.
        int offset = ((chitscale * (1 + numCritters))) / 4;
        point.x -= offset;
        point.y -= offset;

        chit.setLocation(point);

        for (int i = 1; i < numCritters; i++)
        {
            point.x += chitscale4;
            point.y += chitscale4;
            critter = (Critter)critters.get(i);
            chit = client.getBattleChit(critter.getTag());
            if (chit != null)
            {
                chit.setLocation(point);
            }
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


    /** Select all hexes containing critters eligible to move. */
    private void highlightMobileCritters()
    {
        Set set = battle.findMobileCritters();
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    private void highlightMoves(Critter critter)
    {
        Set set = battle.showMoves(critter, false);
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    /** Select hexes containing critters that have valid strike targets. */
    private void highlightCrittersWithTargets()
    {
        Set set = battle.findCrittersWithTargets();
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    /** Highlight all hexes with targets that the critter can strike. */
    private void highlightStrikes(Critter critter)
    {
        Set set = battle.findStrikes(critter, true);
        unselectAllHexes();
        selectHexesByLabels(set);
    }


    public void highlightCarries()
    {
        Set set = battle.getCarryTargets();
        unselectAllHexes();
        selectHexesByLabels(set);
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

    private void actOnHex(BattleHex hex)
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
        // Only the active player can click on stuff.
        if (!client.getPlayerName().equals(battle.getActivePlayerName()))
        {
            return;
        }

        Point point = e.getPoint();

        BattleChit chit = getBattleChitAtPoint(point);
        Critter critter = null;
        if (chit != null)
        {
            critter = battle.getCritter(chit.getTag());
        }
        BattleHex hex = getHexContainingPoint(point);

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

        ArrayList battleChits = client.getBattleChits();
        ListIterator lit = battleChits.listIterator(battleChits.size());
        while (lit.hasPrevious())
        {
            BattleChit chit = (BattleChit)lit.previous();
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

        if (battleFrame != null)
        {
            battleFrame.dispose();
        }
    }


    public void rescale()
    {
        setupHexes();
        setupEntrances();
        placeLegionChits(battle.getDefender(), true);
        placeLegionChits(battle.getAttacker(), false);
    }
}
