package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.server.Constants;

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
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setBackground(Color.lightGray);
        int scale = 4 * Scale.get();

        JPanel legionPane = new JPanel();
        contentPane.add(legionPane);

        legionMarker = new Marker(scale, markerId, this, null);
        legionPane.add(legionMarker);

        java.util.List imageNames = client.getLegionImageNames(markerId);
        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            legionPane.add(chit);
        }

        JPanel recruiterPane = new JPanel();
        contentPane.add(recruiterPane);

        int i = 0;
        it = recruiters.iterator();
        while (it.hasNext())
        {
            String recruiterName = (String)it.next();
            if (recruiterName.equals(Constants.titan))
            {
                recruiterName = client.getTitanBasename(markerId);
            }
            Chit chit = new Chit(scale, recruiterName, this);
            recruiterChits.add(chit);
            recruiterPane.add(chit);
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
            if (recruiterName.startsWith(Constants.titan))
            {
                recruiterName = Constants.titan;
            }

            // Then exit.
            dispose();
        }
    }


    public void windowClosing(WindowEvent e)
    {
        dispose();
    }
}
