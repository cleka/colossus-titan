package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.server.Options;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;


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
    private Cursor defaultCursor;

    /** tag of the selected critter, or -1 if no critter is selected. */
    private int selectedCritterTag = -1;

    public static final String undoLast = "Undo Last";
    public static final String undoAll = "Undo All";
    public static final String doneWithPhase = "Done";
    public static final String concedeBattle = "Concede Battle";

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;
    private AbstractAction concedeBattleAction;



    public BattleMap(Client client, String masterHexLabel)
    {
        super(masterHexLabel);

        this.client = client;

        battleFrame = new JFrame();
        battleFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Container contentPane = battleFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        battleFrame.setSize(getPreferredSize());

        setupIcon();

        battleFrame.addWindowListener(this);
        addMouseListener(this);

        setupActions();
        setupTopMenu();

        setupEntrances();

        if (location == null)
        {
            location = new Point(0, 4 * Scale.get());
        }
        battleFrame.setLocation(location);

        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);

        setupPlayerLabel();
        contentPane.add(playerLabel, BorderLayout.NORTH);

        defaultCursor = battleFrame.getCursor();

        battleFrame.pack();
        battleFrame.setVisible(true);
    }


    // Simple constructor for testing and AICopy()
    public BattleMap(String masterHexLabel)
    {
        super(masterHexLabel);
        setupEntrances();
    }


    public BattleMap AICopy(String masterHexLabel)
    {
        BattleMap newMap = new BattleMap(masterHexLabel);
        return newMap;
    }


    private void setupActions()
    {
        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getPlayerName().equals(
                    client.getBattleActivePlayerName()))
                {
                    return;
                }
                if (client.getBattlePhase() == Constants.MOVE)
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
                if (!client.getPlayerName().equals(
                    client.getBattleActivePlayerName()))
                {
                    return;
                }
                if (client.getBattlePhase() == Constants.MOVE)
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
                if (!client.getPlayerName().equals(
                    client.getBattleActivePlayerName()))
                {
                    return;
                }

                int phase = client.getBattlePhase();
                switch (phase)
                {
                    case Constants.MOVE:
                        if (!client.getOption(Options.autoBattleMove) &&
                            client.anyOffboardCreatures() && 
                            !confirmLeavingCreaturesOffboard())
                        {
                                return;
                        }
                        client.doneWithBattleMoves();
                        break;

                    case Constants.FIGHT:
                    case Constants.STRIKEBACK:
                        if (!client.doneWithStrikes())
                        {
                            highlightCrittersWithTargets();
                            client.showMessageDialog(
                                "Engaged creatures must strike.");
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
                    client.concede();
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

        battleFrame.setTitle(client.getBattleActivePlayerName() +
            " Turn " + client.getBattleTurnNumber() + " : Summon");
        phaseMenu.removeAll();

        requestFocus();
    }


    public void setupRecruitMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(client.getBattleActivePlayerName() +
            " Turn " + client.getBattleTurnNumber() + " : Recruit");
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

        battleFrame.setTitle(client.getBattleActivePlayerName() +
            " Turn " + client.getBattleTurnNumber() + " : Move");

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

        if (client.getPlayerName().equals(client.getBattleActivePlayerName()))
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

        battleFrame.setTitle(client.getBattleActivePlayerName() +
            " Turn " + client.getBattleTurnNumber() +
            ((client.getBattlePhase() == Constants.FIGHT) ?
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

        if (client.getPlayerName().equals(client.getBattleActivePlayerName()))
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
            battleFrame.setIconImage(Chit.getImageIcon(
                Chit.getImagePath("Colossus")).getImage());
        }
        catch (NullPointerException e)
        {
            Log.error(e.toString() + " Couldn't find " +
                Creature.getCreatureByName("Colossus").getImageName());
            dispose();
        }
    }


    /** Show which player owns this board. */
    public void setupPlayerLabel()
    {
        String playerName = client.getPlayerName();
        playerLabel = new JLabel(playerName);

        String colorName = client.getColor();
        // If we call this before player colors are chosen, just use
        // the defaults.
        if (colorName != null)
        {
            Color color = PickColor.getBackgroundColor(colorName);
            playerLabel.setForeground(color);
        }
        playerLabel.repaint();
    }


    public void placeNewChit(String imageName, int tag, String hexLabel)
    {
        client.addBattleChit(imageName, tag);
        alignChits(hexLabel);
    }


    public static BattleHex getEntrance(char terrain, String masterHexLabel,
        int entrySide)
    {
        return HexMap.getHexByLabel(terrain, "X" + entrySide);
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


    public void alignChits(String hexLabel)
    {
        GUIBattleHex hex = getGUIHexByLabel(hexLabel);
        int [] tags = client.getCritterTags(hexLabel);
        int numCritters = tags.length;
        if (numCritters < 1)
        {
            hex.repaint();
            return;
        }
        BattleChit chit = client.getBattleChit(tags[0]);
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
            chit = client.getBattleChit(tags[i]);
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
        Set set = client.findMobileCritters();
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    private void highlightMoves(int tag)
    {
        Set set = client.showBattleMoves(tag);
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    /** Select hexes containing critters that have valid strike targets. */
    private void highlightCrittersWithTargets()
    {
        Set set = client.findCrittersWithTargets();
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    /** Highlight all hexes with targets that the critter can strike. */
    private void highlightStrikes(int tag)
    {
        Set set = client.findStrikes(tag);
        unselectAllHexes();
        selectHexesByLabels(set);
    }


    public void highlightCarries()
    {
        Set set = client.getCarryTargets();
        unselectAllHexes();
        selectHexesByLabels(set);
        setupCarryCursor();
    }

    public void clearCarries()
    {
        unselectAllHexes();
        setupCarryCursor();
    }


    private void setupCarryCursor()
    {
        int numCarries = client.getCarryDamage();
        Cursor cursor = null;

        if (numCarries == 0)
        {
            setDefaultCursor();
        }
        else
        {
            try
            {
                Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(
                    2 * Scale.get(), 2 * Scale.get());
                int numPixels = d.width * d.height;
                int [] pixels = new int[numPixels];

                Point point = new Point(0, 0);

                // Use a special image for unlikely huge carries.
                String basename;
                if (numCarries > 15)
                {
                    basename = "carry";
                }
                else
                {
                    basename = "carry" + numCarries;
                }
                String imageFilename = Chit.getImagePath(basename);

                // This syntax works with either images in a jar file or images
                // in the local filesystem.
                java.net.URL url = getClass().getResource(imageFilename);
                Image image = Toolkit.getDefaultToolkit().getImage(url);

                cursor = Toolkit.getDefaultToolkit().createCustomCursor(
                    image, point, basename);

                battleFrame.setCursor(cursor);
            }
            catch (Exception e)
            {
                // If it fails, just use the default cursor.
                Log.error("Problem creating custom cursor: " + e);
                return;
            }
            battleFrame.setCursor(cursor);
        }
    }

    public void setDefaultCursor()
    {
        battleFrame.setCursor(defaultCursor);
    }

    public void setWaitCursor()
    {
        battleFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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


    private void actOnCritter(int tag, String hexLabel)
    {
        selectedCritterTag = tag;

        // XXX Put selected chit at the top of the z-order.
        // Then getGUIHexByLabel(hexLabel).repaint();

        switch (client.getBattlePhase())
        {
            case Constants.MOVE:
                // Highlight all legal destinations for this critter.
                highlightMoves(tag);
                break;

            case Constants.FIGHT:
            case Constants.STRIKEBACK:
                // Leave carry mode.
                client.leaveCarryMode();

                // Highlight all legal strikes for this critter.
                highlightStrikes(tag);
                break;

            default:
                break;
        }
    }

    private void actOnHex(String hexLabel)
    {
        switch (client.getBattlePhase())
        {
            case Constants.MOVE:
                if (selectedCritterTag != -1)
                {
                    client.doBattleMove(selectedCritterTag, hexLabel);
                    selectedCritterTag = -1;
                    highlightMobileCritters();
                }
                break;

            case Constants.FIGHT:
            case Constants.STRIKEBACK:
                if (client.getCarryDamage() > 0)
                {
                    client.applyCarries(hexLabel);
                }
                else if (selectedCritterTag != -1)
                {
                    client.strike(selectedCritterTag, hexLabel);
                    selectedCritterTag = -1;
                }

                if (client.getCarryDamage() == 0)
                {
                    if (client.getOption(Options.autoForcedStrike))
                    {
                        client.makeForcedStrikes(false);
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
        switch (client.getBattlePhase())
        {
            case Constants.MOVE:
                selectedCritterTag = -1;
                highlightMobileCritters();
                break;

            case Constants.FIGHT:
            case Constants.STRIKEBACK:
                selectedCritterTag = -1;
                client.leaveCarryMode();
                highlightCrittersWithTargets();
                break;

            default:
                break;
        }
    }


    public void mousePressed(MouseEvent e)
    {
        // Only the active player can click on stuff.
        if (!client.getPlayerName().equals(client.getBattleActivePlayerName()))
        {
            return;
        }

        Point point = e.getPoint();

        BattleChit chit = getBattleChitAtPoint(point);
        BattleHex hex = getHexContainingPoint(point);
        String hexLabel = ""; 
        if (hex != null)
        {
            hexLabel = hex.getLabel();
        }

        // XXX Only the active player can move or strike.
        if (chit != null && client.getPlayerNameByTag(chit.getTag()).equals(
            client.getBattleActivePlayerName()))
        {
            actOnCritter(chit.getTag(), hexLabel);
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


    public void windowClosing(WindowEvent e)
    {
        String [] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(battleFrame,
           "Are you sure you wish to quit?",
           "Quit Game?",
           JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
           null, options, options[1]);

        if (answer == JOptionPane.YES_OPTION)
        {
            System.exit(0);
        }
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
        int chitScale = 4 * Scale.get();
        Iterator it = client.getBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            chit.rescale(chitScale);
        }
        alignChits(getAllHexLabels());
    }
}
