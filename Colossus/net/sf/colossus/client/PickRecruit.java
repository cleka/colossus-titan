package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.server.Creature;


/**
 * Class PickRecruit allows a player to pick a creature to recruit.
 * @version $Id$
 * @author David Ripton
 */


public final class PickRecruit extends JDialog implements MouseListener,
    WindowListener
{
    private java.util.List recruits;   // of Creatures
    private ArrayList recruitChits = new ArrayList();
    private Marker legionMarker;
    private ArrayList legionChits = new ArrayList();
    private JFrame parentFrame;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private static String recruit;
    private static boolean active;


    private PickRecruit(JFrame parentFrame, java.util.List recruits, 
        java.util.List imageNames, String hexDescription, String markerId,
        Client client)
    {
        super(parentFrame, client.getPlayerName() +
            ": Pick Recruit in " + hexDescription, true);

        this.parentFrame = parentFrame;
        this.recruits = recruits;
        int numEligible = recruits.size();

        addMouseListener(this);
        addWindowListener(this);
        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);
        pack();
        setBackground(Color.lightGray);
        setResizable(false);
        int scale = 4 * Scale.get();

        legionMarker = new Marker(scale, markerId, this, null);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        gridbag.setConstraints(legionMarker, constraints);
        contentPane.add(legionMarker);

        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            legionChits.add(chit);
            constraints.gridy = 0;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
        }

        int height = imageNames.size();
        // There are height + 1 chits in the top row.  There
        // are numEligible chits / labels to place beneath.
        // So we have (height + 1) - numEligible empty
        // columns, half of which we'll put in front.
        int leadSpace = ((height + 1) - numEligible) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        it = recruits.iterator();
        int i = 0;
        while (it.hasNext())
        {
            Creature recruit = (Creature)it.next();
            String recruitName = recruit.getName();
            Chit chit = new Chit(scale, recruitName, this);
            recruitChits.add(chit);

            constraints.gridx = leadSpace + i;
            constraints.gridy = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);

            int count = client.getCreatureCount(recruitName);
            JLabel countLabel = new JLabel(Integer.toString(count),
                JLabel.CENTER);
            constraints.gridy = 2;
            gridbag.setConstraints(countLabel, constraints);
            contentPane.add(countLabel);
            i++;
        }

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }


    /** Return the creature recruited, or null if none. */
    static String pickRecruit(JFrame parentFrame, java.util.List recruits,
        java.util.List imageNames, String hexDescription, String markerId,
        Client client)
    {
        recruit = null;
        if (!active)
        {
            active = true;
            new PickRecruit(parentFrame, recruits, imageNames,
                hexDescription, markerId, client);
            active = false;
        }
        return recruit;
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = recruitChits.indexOf(source);
        if (i != -1)
        {
            // Recruit the chosen creature.
            recruit = ((Creature)recruits.get(i)).getName();
            dispose();
        }
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mouseClicked(MouseEvent e)
    {
    }


    public void mouseReleased(MouseEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        dispose();
    }


    public void windowDeactivated(WindowEvent e)
    {
    }


    public void windowDeiconified(WindowEvent e)
    {
    }


    public void windowIconified(WindowEvent e)
    {
    }


    public void windowOpened(WindowEvent e)
    {
    }
}
