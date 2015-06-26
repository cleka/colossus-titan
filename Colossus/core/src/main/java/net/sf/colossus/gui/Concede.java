package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.variant.MasterHex;


/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 *
 * @author David Ripton
 */
final class Concede extends KDialog
{
    private static final Logger LOGGER = Logger.getLogger(Concede.class
        .getName());

    private final boolean flee;
    private Point location;
    private final ClientGUI gui;
    private final Legion ally;
    private final Legion attacker;
    private final Legion defender;

    private final SaveWindow saveWindow;

    private final JButton showMapButton;

    private Concede(ClientGUI clientGui, JFrame parentFrame, Legion ally,
        Legion enemy, boolean flee)
    {
        super(parentFrame, (flee ? "Flee" : "Concede") + " with Legion "
            + LegionServerSide.getLongMarkerName(ally.getMarkerId()) + " in "
            + ally.getCurrentHex().getDescription() + "?", false);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        this.flee = flee;
        this.gui = clientGui;
        this.ally = ally;

        if (ally.hasMoved())
        {
            this.attacker = ally;
            this.defender = enemy;
        }
        else
        {
            this.attacker = enemy;
            this.defender = ally;
        }

        setBackground(Color.lightGray);
        // Don't allow closing without explicit decision:
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        MasterHex hex = defender.getCurrentHex();
        EntrySide entrySide = attacker.getEntrySide();
        int direction = hex.findDirectionForEntrySide(entrySide);

        StringBuffer infoText = new StringBuffer();
        try
        {
            MasterHex neighborHex = hex.getNeighbor(direction);
            String neighborHexInfo;
            if (neighborHex != null)
            {
                neighborHexInfo = neighborHex.getDescription();
            }
            else
            {
                neighborHexInfo = "[No hex there - perhaps teleport?]";
            }

            infoText.append(attacker.getMarkerId() + " attacks ");
            infoText.append(defender.getMarkerId() + " in ");
            infoText.append(hex.getDescription() + ", entering from ");
            infoText.append(attacker.getEntrySide().getLabel() + " (");
            infoText.append(neighborHexInfo + ")");
        }
        catch (Exception e)
        {
            LOGGER
                .log(Level.WARNING, "Exception during infoTextCreation: ", e);
        }

        JLabel label1 = new JLabel(infoText.toString());
        contentPane.add(label1);
        label1.setAlignmentX(CENTER_ALIGNMENT);

        Box allyPane = showLegion(ally, true);
        Box enemyPane = showLegion(enemy, false);

        Box legionsPane = new Box(BoxLayout.PAGE_AXIS);
        legionsPane.setAlignmentX(CENTER_ALIGNMENT);
        legionsPane.add(allyPane);
        legionsPane.add(enemyPane);
        getContentPane().add(legionsPane);

        int resultingPoints = ((LegionClientSide)ally).getPointValue();
        if (flee)
        {
            resultingPoints /= 2;
        }
        int score = enemy.getPlayer().getScore();
        int newScore = score + resultingPoints;

        StringBuffer infoText2 = new StringBuffer("");
        infoText2.append("This will give player "
            + enemy.getPlayer().getName() + "  " + resultingPoints
            + " points, " + "i.e. increase the score from " + score + " to "
            + newScore + ".");

        JLabel label2 = new JLabel(infoText2.toString());
        contentPane.add(label2);
        contentPane.add(label2);
        label2.setAlignmentX(CENTER_ALIGNMENT);

        JPanel buttonPane = new JPanel();
        buttonPane.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(buttonPane);

        showMapButton = new JButton("Show Battle Map");
        showMapButton.setMnemonic(KeyEvent.VK_M);
        getRootPane().setDefaultButton(showMapButton);

        buttonPane.add(showMapButton);
        showMapButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                GUIMasterHex hex = gui.getBoard().getGUIHexByMasterHex(
                    attacker.getCurrentHex());
                gui.getBoard().showBattleMap(hex);
            }
        });

        JButton doItButton = new JButton(flee ? "Flee" : "Concede");
        doItButton.setMnemonic(flee ? KeyEvent.VK_F : KeyEvent.VK_C);
        buttonPane.add(doItButton);
        doItButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                processAnswer(true);
            }
        });

        JButton dontDoitButton = new JButton(flee ? "Don't Flee"
            : "Don't Concede");
        dontDoitButton.setMnemonic(KeyEvent.VK_D);
        buttonPane.add(dontDoitButton);
        dontDoitButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                processAnswer(false);
            }
        });

        Action doInstead = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                Concede.this.notifyConcede();
            }
        };

        // Prevent that space triggers the buttons. This frequently happened
        // when players type in the chat while the concede/flee dialog becomes
        // displayed, causing them accidentally to flee or concede.
        preventSpaceAction(doItButton, doInstead);
        preventSpaceAction(dontDoitButton, doInstead);
        preventSpaceAction(showMapButton, doInstead);

        pack();

        saveWindow = new SaveWindow(gui.getOptions(), "Concede");

        if (location == null)
        {
            location = saveWindow.loadLocation();
        }
        if (location == null)
        {
            centerOnScreen();
            location = getLocation();
        }
        else
        {
            setLocation(location);
        }
        setVisible(true);
        repaint();
    }

    /**
     * Prevent Space from triggering the firing of the action normally
     * associated to the given button; trigger the given action instead.
     * The "normal" action of the button can still be activated with mouse
     * click or with the mnemonic key (on windows with Alt + the underlined
     * character)
     * @param button
     * @param newAction
     */
    private void preventSpaceAction(JButton button, Action newAction)
    {
        button.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "doInstead");
        button.getActionMap().put("doInstead", newAction);
    }

    /**
     * Make the user aware that there is a concede or flee dialog waiting
     * for response. This is called by the doInsteadAction (which is fired
     * when SPACE or ENTER is pressed).
     * Make a beep, requests focus and puts it to front, and sets as default
     * action the show Battle Map button.
     */
    public void notifyConcede()
    {
        this.toFront();
        this.requestFocus();
        this.showMapButton.requestFocusInWindow();
        gui.getBoard().getToolkit().beep();
    }

    private Box showLegion(Legion legion, boolean dead)
    {
        Box pane = new Box(BoxLayout.X_AXIS);
        pane.setAlignmentX(LEFT_ALIGNMENT);

        int scale = 4 * Scale.get();

        Marker marker = new Marker(legion, scale, legion.getLongMarkerId(),
            gui.getClient(), true);
        pane.add(marker);
        pane.add(Box.createRigidArea(new Dimension(scale / 4, 0)));

        int points = ((LegionClientSide)legion).getPointValue();
        Box pointsPanel = new Box(BoxLayout.PAGE_AXIS);
        pointsPanel.setSize(marker.getSize());
        pointsPanel.add(new JLabel("" + points));
        pointsPanel.add(new JLabel("points"));
        pane.add(pointsPanel);
        pane.add(Box.createRigidArea(new Dimension(scale / 4, 0)));

        List<String> imageNames = gui.getGameClientSide().getLegionImageNames(
            legion);
        Iterator<String> it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            Chit chit = Chit.newCreatureChit(scale, imageName);
            chit.setDead(dead);
            pane.add(chit);
        }
        return pane;
    }

    private static Concede currentDialog = null;

    static void concede(ClientGUI gui, JFrame parentFrame, Legion ally,
        Legion enemy)
    {
        currentDialog = new Concede(gui, parentFrame, ally, enemy, false);
    }

    static void flee(ClientGUI gui, JFrame parentFrame, Legion ally,
        Legion enemy)
    {
        currentDialog = new Concede(gui, parentFrame, ally, enemy, true);
    }

    /*
     * the argument is the yes or no; which type it is (flee or concede)
     * is based on which type the last opened dialog was
     */
    static void inactivityAutoFleeOrConcede(boolean reply)
    {
        if (currentDialog == null)
        {
            LOGGER
                .severe("inactivityAutoFleeOrConcede called, but currentDialog is null???");
        }
        Concede tmp = currentDialog;
        currentDialog = null;
        tmp.finishUp(reply);
    }

    public Legion getAttacker()
    {
        return this.attacker;
    }

    private void processAnswer(boolean answer)
    {
        // Feature Request #223, confirm before allowing the player to concede
        // if their Titan is in the legion
        if (!flee
            && ally.hasTitan()
            && answer == true
            && gui.getOptions().getOption(Options.confirmConcedeWithTitan,
                true))
        {
            String message = "Are you sure you want to concede? This legion "
                + "contains your Titan, and conceding will cause you to lose "
                + "the game!";

            String[] options = new String[] { "Yes", "No", "Don't ask again" };

            int confirmAnswer = JOptionPane.showOptionDialog(this, message,
                "Confirm Concession With Titan", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

            if (confirmAnswer == 1 || confirmAnswer == -1)
            {
                // answered "No", abort concede
                return;
            }
            if (confirmAnswer == 2)
            {
                // don't ask again
                gui.getClient().setPreferencesCheckBoxValue(
                    Options.confirmConcedeWithTitan, false);

            }
        }

        finishUp(answer);
    }

    private void finishUp(boolean answer)
    {
        location = getLocation();
        saveWindow.saveLocation(location);
        dispose();
        if (flee)
        {
            gui.getCallbackHandler().answerFlee(ally, answer);
        }
        else
        {
            gui.getCallbackHandler().answerConcede(ally, answer);
        }
    }
}
