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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.server.LegionServerSide;


/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 * @version $Id$
 * @author David Ripton
 */

final class Concede extends KDialog
{
    private final boolean flee;
    private Point location;
    private final Client client;
    private final Legion ally;
    private final SaveWindow saveWindow;

    private Concede(Client client, JFrame parentFrame, Legion ally,
        Legion enemy, boolean flee)
    {
        super(parentFrame, (flee ? "Flee" : "Concede") + " with Legion "
            + LegionServerSide.getLongMarkerName(ally.getMarkerId()) + " in "
            + ally.getCurrentHex().getDescription() + "?", false);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                cleanup(false);
            }
        });

        this.flee = flee;
        this.client = client;
        this.ally = ally;

        setBackground(Color.lightGray);

        showLegion(ally);
        showLegion(enemy);

        JPanel buttonPane = new JPanel();
        contentPane.add(buttonPane);

        JButton button1 = new JButton(flee ? "Flee" : "Concede");
        button1.setMnemonic(flee ? KeyEvent.VK_F : KeyEvent.VK_C);
        buttonPane.add(button1);
        button1.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                cleanup(true);
            }
        });

        JButton button2 = new JButton(flee ? "Don't Flee" : "Don't Concede");
        button2.setMnemonic(KeyEvent.VK_D);
        buttonPane.add(button2);
        button2.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                cleanup(false);

            }
        });

        pack();

        saveWindow = new SaveWindow(client.getOptions(), "Concede");

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

    private void showLegion(Legion legion)
    {
        Box pane = new Box(BoxLayout.X_AXIS);
        pane.setAlignmentX(0);
        getContentPane().add(pane);

        int scale = 4 * Scale.get();

        Marker marker = new Marker(scale, legion.getMarkerId(), client);
        pane.add(marker);
        pane.add(Box.createRigidArea(new Dimension(scale / 4, 0)));

        int points = ((LegionClientSide)legion).getPointValue();
        Box pointsPanel = new Box(BoxLayout.Y_AXIS);
        pointsPanel.setSize(marker.getSize());
        pointsPanel.add(new JLabel("" + points));
        pointsPanel.add(new JLabel("points"));
        pane.add(pointsPanel);
        pane.add(Box.createRigidArea(new Dimension(scale / 4, 0)));

        List<String> imageNames = client.getLegionImageNames(legion);
        Iterator<String> it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            Chit chit = new Chit(scale, imageName);
            pane.add(chit);
        }
    }

    static void concede(Client client, JFrame parentFrame, Legion ally,
        Legion enemy)
    {
        new Concede(client, parentFrame, ally, enemy, false);
    }

    static void flee(Client client, JFrame parentFrame, Legion ally,
        Legion enemy)
    {
        new Concede(client, parentFrame, ally, enemy, true);
    }

    private void cleanup(boolean answer)
    {
        location = getLocation();
        saveWindow.saveLocation(location);
        dispose();
        if (flee)
        {
            client.answerFlee(ally, answer);
        }
        else
        {
            client.answerConcede(ally, answer);
        }
    }
}
