package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;
import net.sf.colossus.server.Legion;

/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 * @version $Id$
 * @author David Ripton
 */

final class Concede extends KDialog implements ActionListener, WindowListener
{
    private JFrame parentFrame;
    private boolean flee;
    private Chit allyMarker;
    private Chit enemyMarker;
    private Point location;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
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
        contentPane.setLayout(gridbag);
        addWindowListener(this);

        this.parentFrame = parentFrame;
        this.flee = flee;
        this.client = client;
        this.allyMarkerId = allyMarkerId;

        pack();

        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        allyMarker = new Marker(scale, allyMarkerId, this, client);
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(allyMarker, constraints);
        contentPane.add(allyMarker);

        java.util.List allyImageNames = client.getLegionImageNames(
            allyMarkerId);
        Iterator it = allyImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
        }

        enemyMarker = new Marker(scale, enemyMarkerId, this, client);
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(enemyMarker, constraints);
        contentPane.add(enemyMarker);

        java.util.List enemyImageNames = client.getLegionImageNames(
            enemyMarkerId);
        it = enemyImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
        }

        JButton button1 = new JButton(flee ? "Flee" : "Concede");
        button1.setMnemonic(flee ? KeyEvent.VK_F : KeyEvent.VK_C);
        JButton button2 = new JButton(flee ? "Don't Flee" : "Don't Concede");
        button2.setMnemonic(KeyEvent.VK_D);

        // Attempt to center the buttons.
        int chitWidth = 1 + Math.max(allyImageNames.size(), 
            enemyImageNames.size());
        if (chitWidth < 4)
        {
            constraints.gridwidth = 1;
        }
        else
        {
            constraints.gridwidth = 2;
        }
        int leadSpace = (chitWidth - 2 * constraints.gridwidth) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        constraints.gridy = 2;
        constraints.gridx = leadSpace;
        gridbag.setConstraints(button1, constraints);
        contentPane.add(button1);
        button1.addActionListener(this);
        constraints.gridx = leadSpace + constraints.gridwidth;
        gridbag.setConstraints(button2, constraints);
        contentPane.add(button2);
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
