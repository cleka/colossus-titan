package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import net.sf.colossus.game.Legion;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;


/**
 * A GUI representation of a battle in the game.
 * 
 * TODO this is split of the former BattleMap which did everything by itself. The
 * split is not really completed, there is still code which potentially belongs into
 * the other class.
 */

public final class BattleBoard extends KFrame
{
    private static final Logger LOGGER = Logger.getLogger(BattleMap.class
        .getName());

    private static int count = 1;

    private Point location;
    private JMenuBar menuBar;
    private JMenu phaseMenu;
    private JMenu helpMenu;
    private final InfoPanel infoPanel;
    private final Client client;
    private final Cursor defaultCursor;

    /** tag of the selected critter, or -1 if no critter is selected. */
    private int selectedCritterTag = -1;

    private static final String undoLast = "Undo Last";
    private static final String undoAll = "Undo All";
    private static final String doneWithPhase = "Done";
    private static final String concedeBattle = "Concede Battle";
    private static final String showTerrainHazard = "Show Terrain";

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;
    private AbstractAction concedeBattleAction;
    private AbstractAction showTerrainHazardAction;

    private final SaveWindow saveWindow;

    private final BattleMap battleMap;
    private final BattleDice battleDice;

    // TODO pass Legions instead of the markerIds
    public BattleBoard(final Client client, MasterHex masterHex,
        Legion attacker, Legion defender)
    {
        super(); // title will be set later

        this.client = client;

        String attackerMarkerId = attacker.getMarkerId();
        String defenderMarkerId = defender.getMarkerId();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        setupIcon();

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                client.askNewCloseQuitCancel(BattleBoard.this, true);
            }
        });

        setupActions();
        setupTopMenu();
        setupHelpMenu();

        saveWindow = new SaveWindow(client.getOptions(), "BattleMap");

        if (location == null)
        {
            location = saveWindow.loadLocation();
        }
        if (location == null)
        {
            location = new Point(0, 4 * Scale.get());
        }
        setLocation(location);

        battleMap = new BattleMap(client, masterHex, attackerMarkerId,
            defenderMarkerId);
        contentPane.add(new JScrollPane(battleMap), BorderLayout.CENTER);
        battleMap.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                // Only the active player can click on stuff.
                if (!client.getOwningPlayer().equals(
                    client.getBattleActivePlayer()))
                {
                    return;
                }

                Point point = e.getPoint();

                BattleChit chit = getBattleChitAtPoint(point);
                GUIBattleHex hex = battleMap.getHexContainingPoint(point);
                
                handleMousePressed(chit, hex);
            }
        });

        infoPanel = new InfoPanel();
        contentPane.add(infoPanel, BorderLayout.NORTH);

        String colorName = client.getColor();
        if (colorName != null)
        {
            Color color = PickColor.getBackgroundColor(colorName);
            contentPane.setBorder(BorderFactory.createLineBorder(color));
        }
        defaultCursor = getCursor();

        battleDice = new BattleDice();
        getContentPane().add(battleDice, BorderLayout.SOUTH);

        setTitle(client.getOwningPlayer().getName() + ": "
            + LegionServerSide.getMarkerName(attackerMarkerId) + " ("
            + attackerMarkerId + ") attacks "
            + LegionServerSide.getMarkerName(defenderMarkerId) + " ("
            + defenderMarkerId + ") in " + masterHex.getLabel());

        String instanceId = client.getOwningPlayer().getName() + ": "
            + attackerMarkerId + "/" + defenderMarkerId + " (" + count + ")";
        count++;
        net.sf.colossus.webcommon.InstanceTracker.setId(this, instanceId);

        pack();
        setVisible(true);

        // @TODO: perhaps those could be done earlier, but in previous code
        // (still in Client) they were done after BattleBoard instantiation,
        // so I keep them like that, for now.
        setPhase(client.getBattlePhase());
        setTurn(client.getBattleTurnNumber());
        setBattleMarkerLocation(false, "X" + attacker.getEntrySide());
        setBattleMarkerLocation(true, "X" + defender.getEntrySide());
        reqFocus();
    }

    private void handleMousePressed(BattleChit chit, GUIBattleHex hex)
    {
        String hexLabel = "";
        client.resetStrikeNumbers();
        if (hex != null)
        {
            hexLabel = hex.getHexModel().getLabel();
        }

        String choiceDesc;
        PickCarry pickCarryDialog = client.getPickCarryDialog();
        boolean ownChit = ( chit != null
            && client.getPlayerByTag(chit.getTag()).equals(
                client.getBattleActivePlayer())); 
        
        if (pickCarryDialog != null)
        {
            if (chit != null && !ownChit)
            {
                choiceDesc = pickCarryDialog.findCarryChoiceForHex(hexLabel);
                // clicked on possible carry target
                if (choiceDesc != null)
                {
                    pickCarryDialog.handleCarryToDescription(choiceDesc);
                }
                else
                {
                    // enemy but not carryable to there
                }
            }
            else
            {
                // not a chit, or at least not own chit
            }
        }
        else if (chit != null && ownChit)
        {
            actOnCritter(chit.getTag());
        }

        // No hits on friendly chits, so check map.
        else if (hex != null && hex.isSelected())
        {
            actOnHex(hexLabel);
        }

        // No hits on selected hexes, so clean up.
        else
        {
            actOnMisclick();
        }
    }

    private void setBattleMarkerLocation(boolean isDefender, String hexLabel)
    {
        battleMap.setBattleMarkerLocation(isDefender, hexLabel);
    }

    private void setupActions()
    {
        showTerrainHazardAction = new AbstractAction(showTerrainHazard)
        {
            public void actionPerformed(ActionEvent e)
            {
                new BattleTerrainHazardWindow(BattleBoard.this, client,
                    battleMap.getMasterHex());
            }
        };
        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getOwningPlayer().equals(
                    client.getBattleActivePlayer()))
                {
                    return;
                }
                if (client.getBattlePhase() == Constants.BattlePhase.MOVE)
                {
                    selectedCritterTag = -1;
                    client.undoLastBattleMove();
                    highlightMobileCritters();
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getOwningPlayer().equals(
                    client.getBattleActivePlayer()))
                {
                    return;
                }
                if (client.getBattlePhase() == Constants.BattlePhase.MOVE)
                {
                    selectedCritterTag = -1;
                    client.undoAllBattleMoves();
                    highlightMobileCritters();
                }
            }
        };

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getOwningPlayer().equals(
                    client.getBattleActivePlayer()))
                {
                    return;
                }

                Constants.BattlePhase phase = client.getBattlePhase();
                if (phase == Constants.BattlePhase.MOVE)
                {
                    if (!client.getOptions().getOption(Options.autoPlay)
                        && client.anyOffboardCreatures()
                        && !confirmLeavingCreaturesOffboard())
                    {
                        return;
                    }
                    unselectAllHexes();
                    battleMap.unselectEntranceHexes();
                    client.doneWithBattleMoves();
                }
                else if (phase.isFightPhase())
                {
                    unselectAllHexes();
                    battleMap.unselectEntranceHexes();
                    client.doneWithStrikes();
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Bogus phase");
                }
            }
        };

        concedeBattleAction = new AbstractAction(concedeBattle)
        {
            public void actionPerformed(ActionEvent e)
            {
                String[] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(BattleBoard.this,
                    "Are you sure you wish to concede the battle?",
                    "Confirm Concession?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    String playerName = client.getOwningPlayer().getName();
                    LOGGER
                        .log(Level.INFO, playerName + " concedes the battle");
                    client.concede();
                }
            }
        };
    }

    private void setupTopMenu()
    {
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // Phase menu items change by phase and will be set up later.
        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(phaseMenu);
        menuBar.add(helpMenu);
    }

    public void setupHelpMenu()
    {
        JMenuItem mi;

        mi = helpMenu.add(showTerrainHazardAction);
        mi.setMnemonic(KeyEvent.VK_T);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));

        reqFocus();
    }

    public void setupSummonMenu()
    {
        phaseMenu.removeAll();

        reqFocus();
    }

    public void setupRecruitMenu()
    {
        if (phaseMenu != null)
        {
            phaseMenu.removeAll();
        }

        reqFocus();
    }

    public void setupMoveMenu()
    {
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

        if (client.getOwningPlayer().equals(client.getBattleActivePlayer()))
        {
            highlightMobileCritters();
            reqFocus();
        }
    }

    void setupFightMenu()
    {
        phaseMenu.removeAll();

        if (client.getMyEngagedLegion() == null)
        {
            // We are not involved - we can't do concede or done
            return;
        }
        JMenuItem mi;

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        if (client.getOwningPlayer().equals(client.getBattleActivePlayer()))
        {
            highlightCrittersWithTargets();
            reqFocus();
        }
    }

    public void setPhase(Constants.BattlePhase newBattlePhase)
    {
        if (client.getOwningPlayer().equals(client.getBattleActivePlayer()))
        {
            enableDoneButton();
            infoPanel.setOwnPhase(newBattlePhase.toString());
        }
        else
        {
            disableDoneButton();
            infoPanel.setForeignPhase(newBattlePhase.toString());
        }
    }

    public void setTurn(int newturn)
    {
        infoPanel.turnPanel.advTurn(newturn);
    }

    private void setupIcon()
    {
        List<String> directories = new ArrayList<String>();
        directories.add(Constants.defaultDirName
            + ResourceLoader.getPathSeparator() + Constants.imagesDirName);

        String[] iconNames = {
            Constants.battlemapIconImage,
            Constants.battlemapIconText + "-Name-"
                + Constants.battlemapIconTextColor,
            Constants.battlemapIconSubscript + "-Subscript-"
                + Constants.battlemapIconTextColor };

        Image image = ResourceLoader.getCompositeImage(iconNames, directories,
            60, 60);

        if (image == null)
        {
            LOGGER.log(Level.SEVERE, "ERROR: Couldn't find Colossus icon");
            dispose();
        }
        else
        {
            setIconImage(image);
        }
    }

    public static BattleHex getEntrance(MasterBoardTerrain terrain,
        int entrySide)
    {
        return HexMap.getHexByLabel(terrain, "X" + entrySide);
    }

    /** Return the BattleChit containing the given point,
     *  or null if none does. */
    private BattleChit getBattleChitAtPoint(Point point)
    {
        List<BattleChit> battleChits = client.getBattleChits();
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (chit.contains(point))
            {
                return chit;
            }
        }
        return null;
    }

    public void alignChits(String hexLabel)
    {
        GUIBattleHex hex = battleMap.getGUIHexByLabel(hexLabel);
        List<BattleChit> chits = client.getBattleChits(hexLabel);
        int numChits = chits.size();
        if (numChits < 1)
        {
            hex.repaint();
            return;
        }
        BattleChit chit = chits.get(0);
        int chitscale = chit.getBounds().width;
        int chitscale4 = chitscale / 4;

        Point point = new Point(hex.findCenter());

        // Cascade chits diagonally.
        int offset = ((chitscale * (1 + numChits))) / 4;
        point.x -= offset;
        point.y -= offset;

        battleMap.add(chit);
        chit.setLocation(point);

        for (int i = 1; i < numChits; i++)
        {
            point.x += chitscale4;
            point.y += chitscale4;
            chit = chits.get(i);
            chit.setLocation(point);
        }
        hex.repaint();
    }

    public void alignChits(Set<String> hexLabels)
    {
        Iterator<String> it = hexLabels.iterator();
        while (it.hasNext())
        {
            String hexLabel = it.next();
            alignChits(hexLabel);
        }
    }

    /** Select all hexes containing critters eligible to move. */
    public void highlightMobileCritters()
    {
        Set<String> set = client.findMobileCritterHexes();
        unselectAllHexes();
        battleMap.unselectEntranceHexes();
        battleMap.selectHexesByLabels(set);
        battleMap.selectEntranceHexes(set);
    }

    private void highlightMoves(int tag)
    {
        Set<String> set = client.showBattleMoves(tag);
        battleMap.unselectAllHexes();
        battleMap.unselectEntranceHexes();
        battleMap.selectHexesByLabels(set);
    }

    /** Select hexes containing critters that have valid strike targets. */
    public void highlightCrittersWithTargets()
    {
        Set<String> set = client.findCrittersWithTargets();
        unselectAllHexes();
        battleMap.selectHexesByLabels(set);
        // XXX Needed?
        repaint();
    }

    /** Highlight all hexes with targets that the critter can strike. */
    private void highlightStrikes(int tag)
    {
        Set<String> set = client.findStrikes(tag);
        unselectAllHexes();
        client.resetStrikeNumbers();
        battleMap.selectHexesByLabels(set);
        client.setStrikeNumbers(tag, set);
        // XXX Needed?
        repaint();
    }

    /** Highlight all hexes to which carries could be applied */
    public void highlightPossibleCarries(Set<String> set)
    {
        unselectAllHexes();
        client.resetStrikeNumbers();
        battleMap.selectHexesByLabels(set);
        // client.setStrikeNumbers(tag, set);
        // XXX Needed?
        repaint();
    }

    public void setDefaultCursor()
    {
        setCursor(defaultCursor);
    }

    public void setWaitCursor()
    {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private boolean confirmLeavingCreaturesOffboard()
    {
        String[] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(this,
            "Are you sure you want to leave creatures offboard?",
            "Confirm Leaving Creatures Offboard?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return (answer == JOptionPane.YES_OPTION);
    }

    private void actOnCritter(int tag)
    {
        selectedCritterTag = tag;

        // XXX Put selected chit at the top of the z-order.
        // Then getGUIHexByLabel(hexLabel).repaint();
        Constants.BattlePhase phase = client.getBattlePhase();
        if (phase == Constants.BattlePhase.MOVE)
        {
            highlightMoves(tag);
        }
        else if (phase.isFightPhase())
        {
            client.leaveCarryMode();
            highlightStrikes(tag);
        }
    }

    private void actOnHex(String hexLabel)
    {
        Constants.BattlePhase phase = client.getBattlePhase();
        if (phase == Constants.BattlePhase.MOVE)
        {
            if (selectedCritterTag != -1)
            {
                client.doBattleMove(selectedCritterTag, hexLabel);
                selectedCritterTag = -1;
                highlightMobileCritters();
            }
        }
        else if (phase.isFightPhase())
        {
            if (selectedCritterTag != -1)
            {
                client.strike(selectedCritterTag, hexLabel);
                selectedCritterTag = -1;
            }
        }
    }

    private void actOnMisclick()
    {
        Constants.BattlePhase phase = client.getBattlePhase();
        if (phase == Constants.BattlePhase.MOVE)
        {
            selectedCritterTag = -1;
            highlightMobileCritters();
        }
        else if (phase.isFightPhase())
        {
            selectedCritterTag = -1;
            client.leaveCarryMode();
            highlightCrittersWithTargets();
        }
    }

    public void rescale()
    {
        battleMap.setupHexes();

        int chitScale = 4 * Scale.get();
        List<BattleChit> battleChits = client.getBattleChits();
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            chit.rescale(chitScale);
        }
        alignChits(battleMap.getAllHexLabels());

        battleDice.rescale();

        setSize(getPreferredSize());
        pack();
        repaint();
    }

    public static String entrySideName(int side)
    {
        switch (side)
        {
            case 1:
                return Constants.right;

            case 3:
                return Constants.bottom;

            case 5:
                return Constants.left;

            default:
                return "";
        }
    }

    public static int entrySideNum(String side)
    {
        if (side == null)
        {
            return -1;
        }
        if (side.equals(Constants.right))
        {
            return 1;
        }
        else if (side.equals(Constants.bottom))
        {
            return 3;
        }
        else if (side.equals(Constants.left))
        {
            return 5;
        }
        else
        {
            return -1;
        }
    }

    public void reqFocus()
    {
        if (client.getOptions().getOption(Options.stealFocus))
        {
            requestFocus();
            toFront();
        }
    }

    private class TurnPanel extends JPanel
    {

        private final JLabel[] turn;
        private int turnNumber;

        private TurnPanel()
        {
            this(client.getMaxBattleTurns());
        }

        private TurnPanel(int MAXBATTLETURNS)
        {
            super(new GridLayout((MAXBATTLETURNS + 1) % 8 + 1, 0));
            turn = new JLabel[MAXBATTLETURNS + 1];
            // Create Special labels for Recruitment turns
            int[] REINFORCEMENTTURNS = client.getReinforcementTurns();
            for (int j : REINFORCEMENTTURNS)
            {
                turn[j - 1] = new JLabel((j) + "+", SwingConstants.CENTER);
                resetTurn(j); // Set thin Border
            }
            // make the final "extra" turn label to show "time loss"
            turn[turn.length - 1] = new JLabel("Loss", SwingConstants.CENTER);
            resetTurn(turn.length);
            // Create remaining labels
            for (int i = 0; i < turn.length; i++)
            {
                if (turn[i] == null)
                {
                    turn[i] = new JLabel(Integer.toString(i + 1),
                        SwingConstants.CENTER);
                    resetTurn(i + 1); // Set thin Borders
                }
            }
            turnNumber = 0;

            for (JLabel label : turn)
            {
                this.add(label);
            }
        }

        private void advTurn(int newturn)
        {
            if (turnNumber > 0)
            {
                resetTurn(turnNumber);
            }
            setTurn(newturn);
        }

        private void resetTurn(int newTurn)
        {
            setBorder(turn[newTurn - 1], 1);
        }

        private void setTurn(int newTurn)
        {
            if (client.isMyBattlePhase())
            {
                setBorder(turn[newTurn - 1], 5);
            }
            else
            {
                setBorder(turn[newTurn - 1], 3);
            }
            turnNumber = newTurn;
        }

        private void setBorder(JLabel turn, int thick)
        {
            if (thick == 3)
            {
                turn.setBorder(BorderFactory.createLineBorder(Color.GRAY, 3));
            }
            else
            {
                if (thick == 5)
                {
                    turn.setBorder(BorderFactory
                        .createLineBorder(Color.RED, 5));
                }
                else
                {
                    turn
                        .setBorder(BorderFactory.createLineBorder(Color.BLACK));
                }
            }
        }
    } // class TurnPanel

    private class InfoPanel extends JPanel
    {
        private final TurnPanel turnPanel;
        private final JButton doneButton;
        private final JLabel phaseLabel;

        // since inner class most methods can be private.
        private InfoPanel()
        {
            super();
            setLayout(new java.awt.BorderLayout());

            doneButton = new JButton(doneWithPhaseAction);
            add(doneButton, BorderLayout.WEST);

            phaseLabel = new JLabel("- phase -");
            add(phaseLabel, BorderLayout.EAST);

            turnPanel = new TurnPanel();
            add(turnPanel, BorderLayout.CENTER);
        }

        private void setOwnPhase(String s)
        {
            phaseLabel.setText(s);
            doneButton.setEnabled(true);
        }

        private void setForeignPhase(String s)
        {
            String name = client.getBattleActivePlayer().getName();
            phaseLabel.setText("(" + name + ") " + s);
            doneButton.setEnabled(false);
        }

        private void disableDoneButton()
        {
            doneButton.setEnabled(false);
        }

        private void enableDoneButton()
        {
            doneButton.setEnabled(true);
        }

    }

    public void enableDoneButton()
    {
        infoPanel.enableDoneButton();
    }

    public void disableDoneButton()
    {
        infoPanel.disableDoneButton();
    }

    public void unselectAllHexes()
    {
        battleMap.unselectAllHexes();
    }

    public void unselectHexByLabel(String hexLabel)
    {
        battleMap.unselectHexByLabel(hexLabel);
    }

    public void setDiceValues(String strikerDesc, String targetDesc,
        int targetNumber, List<String> rolls)
    {
        battleDice.setValues(strikerDesc, targetDesc, targetNumber, rolls);
        battleDice.showRoll();
    }
}
