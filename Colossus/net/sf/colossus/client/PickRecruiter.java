package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;

/**
 * Class PickRecruiter allows a player to choose which creature(s) recruit.
 * @version $Id$
 * @author David Ripton
 */


final class PickRecruiter extends KDialog implements MouseListener,
    WindowListener
{
    private java.util.List recruiters;
    private java.util.List recruiterChits = new ArrayList();
    private Marker legionMarker;
    private int height;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private static String recruiterName;


    /** recruiters is a list of creature name strings */
    private PickRecruiter(JFrame parentFrame, java.util.List recruiters, 
        String hexDescription, String markerId, Client client)
    {
        super(parentFrame, client.getPlayerName() + ": Pick Recruiter in " +
            hexDescription, true);

        recruiterName = null;
        this.recruiters = recruiters;

        addMouseListener(this);
        addWindowListener(this);
        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);
        pack();
        setBackground(Color.lightGray);
        int scale = 4 * Scale.get();

        legionMarker = new Marker(scale, markerId, this, null);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        gridbag.setConstraints(legionMarker, constraints);
        contentPane.add(legionMarker);

        java.util.List imageNames = client.getLegionImageNames(markerId);
        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
        }

        height = imageNames.size();

        // There are height + 1 chits in the top row.  There
        // are numEligible chits to place beneath.
        // So we have (height + 1) - numEligible empty
        // columns, half of which we'll put in front.
        int numEligible = recruiters.size();
        int leadSpace = ((height + 1) - numEligible) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        int i = 0;
        it = recruiters.iterator();
        while (it.hasNext())
        {
            String recruiterName = (String)it.next();
            Chit chit = new Chit(scale, recruiterName, this);
            recruiterChits.add(chit);
            constraints.gridx = leadSpace + i;
            constraints.gridy = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
            i++;
        }

        pack();
        centerOnScreen();
        setVisible(true);
        repaint();
    }


    static String pickRecruiter(JFrame parentFrame, java.util.List recruiters, 
        String hexDescription, String markerId, Client client)
    {
        new PickRecruiter(parentFrame, recruiters, hexDescription, markerId, 
            client);
        return recruiterName;
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = recruiterChits.indexOf(source);
        if (i != -1)
        {
            recruiterName = (String)recruiters.get(i);

            // Then exit.
            dispose();
        }
    }


    public void windowClosing(WindowEvent e)
    {
        dispose();
    }
}
