package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.sf.colossus.client.LegionClientSide;
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

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                cleanup(false);
            }
        });

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
                cleanup(true);
            }
        });

        JButton dontDoitButton = new JButton(flee ? "Don't Flee" : "Don't Concede");
        dontDoitButton.setMnemonic(KeyEvent.VK_D);
        buttonPane.add(dontDoitButton);
        dontDoitButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                cleanup(false);
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

        Marker marker = new Marker(scale, legion.getLongMarkerId(),
          gui.getClient());
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
            Chit chit = new Chit(scale, imageName);
            chit.setDead(dead);
            pane.add(chit);
        }
        return pane;
    }

    static void concede(ClientGUI gui, JFrame parentFrame, Legion ally,
        Legion enemy)
    {
        new Concede(gui, parentFrame, ally, enemy, false);
    }

    static void flee(ClientGUI gui, JFrame parentFrame, Legion ally,
        Legion enemy)
    {
        new Concede(gui, parentFrame, ally, enemy, true);
    }

    public Legion getAttacker()
    {
        return this.attacker;
    }

    private void cleanup(boolean answer)
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
