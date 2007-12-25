package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.server.Legion;
import net.sf.colossus.util.KDialog;


/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 * @version $Id$
 * @author David Ripton
 */

final class Concede extends KDialog implements ActionListener, WindowListener
{
    private boolean flee;
    private Point location;
    private Client client;
    private String allyMarkerId;
    private SaveWindow saveWindow;

    private Concede(Client client, JFrame parentFrame, String allyMarkerId,
        String enemyMarkerId, boolean flee)
    {
        super(parentFrame, (flee ? "Flee" : "Concede") + " with Legion " +
            Legion.getLongMarkerName(allyMarkerId) + " in " +
            MasterBoard.getHexByLabel(
            client.getHexForLegion(allyMarkerId)).getDescription() + "?",
            false);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        addWindowListener(this);

        this.flee = flee;
        this.client = client;
        this.allyMarkerId = allyMarkerId;

        setBackground(Color.lightGray);

        showLegion(allyMarkerId);
        showLegion(enemyMarkerId);

        JPanel buttonPane = new JPanel();
        contentPane.add(buttonPane);

        JButton button1 = new JButton(flee ? "Flee" : "Concede");
        button1.setMnemonic(flee ? KeyEvent.VK_F : KeyEvent.VK_C);
        buttonPane.add(button1);
        button1.addActionListener(this);

        JButton button2 = new JButton(flee ? "Don't Flee" : "Don't Concede");
        button2.setMnemonic(KeyEvent.VK_D);
        buttonPane.add(button2);
        button2.addActionListener(this);

        pack();

        saveWindow = new SaveWindow(client, "Concede");

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

    private void showLegion(String markerId)
    {
        Box pane = new Box(BoxLayout.X_AXIS);
        pane.setAlignmentX(0);
        getContentPane().add(pane);

        int scale = 4 * Scale.get();

        Marker marker = new Marker(scale, markerId, client);
        pane.add(marker);
        pane.add(Box.createRigidArea(new Dimension(scale / 4, 0)));

        int points = client.getLegionInfo(markerId).getPointValue();
        Box pointsPanel = new Box(BoxLayout.Y_AXIS);
        pointsPanel.setSize(marker.getSize());
        pointsPanel.add(new JLabel(""+points));
        pointsPanel.add(new JLabel("points"));
        pane.add(pointsPanel);
        pane.add(Box.createRigidArea(new Dimension(scale / 4, 0)));

        List imageNames = client.getLegionImageNames(markerId);
        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName);
            pane.add(chit);
        }
    }

    static void concede(Client client, JFrame parentFrame,
        String allyMarkerId, String enemyMarkerId)
    {
        new Concede(client, parentFrame, allyMarkerId, enemyMarkerId, false);
    }

    static void flee(Client client, JFrame parentFrame, String allyMarkerId,
        String enemyMarkerId)
    {
        new Concede(client, parentFrame, allyMarkerId, enemyMarkerId, true);
    }

    private void cleanup(boolean answer)
    {
        location = getLocation();
        saveWindow.saveLocation(location);
        dispose();
        if (flee)
        {
            client.answerFlee(allyMarkerId, answer);
        }
        else
        {
            client.answerConcede(allyMarkerId, answer);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Flee") ||
            e.getActionCommand().equals("Concede"))
        {
            cleanup(true);
        }
        else
        {
            cleanup(false);
        }
    }

    public void windowClosing(WindowEvent e)
    {
        cleanup(false);
    }
}
