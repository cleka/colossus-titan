package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;


/**
 * Class PickRecruiter allows a player to choose which creature(s) recruit.
 * @version $Id$
 * @author David Ripton
 */


final class PickRecruiter extends KDialog implements MouseListener,
            WindowListener
{
    private List recruiters;
    private List recruiterChits = new ArrayList();
    private Marker legionMarker;
    private static String recruiterName;

    /** recruiters is a list of creature name strings */
    private PickRecruiter(JFrame parentFrame, List recruiters,
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

        legionMarker = new Marker(scale, markerId, null);
        legionPane.add(legionMarker);

        List imageNames = client.getLegionImageNames(markerId);
        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName);
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
            Chit chit = new Chit(scale, recruiterName);
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

    static String pickRecruiter(JFrame parentFrame, List recruiters,
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
