package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;


/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 * @version $Id$
 * @author David Ripton
 */

final class SplitLegion extends JDialog implements MouseListener,
    ActionListener, WindowListener
{
    private java.util.List oldChits = new ArrayList(8);
    private java.util.List newChits = new ArrayList(8);

    private Marker oldMarker;
    private Marker newMarker;

    private Client client;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private static boolean active;
    private String selectedMarkerId;

    /** new marker id,creature1,creature2... */
    private static String results;


    private SplitLegion(Client client, String oldMarkerId, String 
        longMarkerName, String selectedMarkerId, java.util.List imageNames)
    {
        super(client.getBoard().getFrame(), client.getPlayerName() + 
            ": Split Legion " + longMarkerName, true);

        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);

        this.client = client;

        if (selectedMarkerId == null)
        {
            dispose();
            return;
        }

        setBackground(Color.lightGray);

        addMouseListener(this);
        addWindowListener(this);

        int scale = 4 * Scale.get();

        oldMarker = new Marker(scale, oldMarkerId, this, null);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(oldMarker, constraints);
        contentPane.add(oldMarker);

        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            oldChits.add(chit);
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        newMarker = new Marker(scale, selectedMarkerId, this, null);

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(newMarker, constraints);
        contentPane.add(newMarker);

        JButton button1 = new JButton("Done");
        button1.setMnemonic(KeyEvent.VK_D);
        JButton button2 = new JButton("Cancel");
        button2.setMnemonic(KeyEvent.VK_C);

         // Attempt to center the buttons.
        int chitWidth = Math.max(oldChits.size(), newChits.size()) + 1;
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

        constraints.gridx = leadSpace;
        constraints.gridy = 2;
        gridbag.setConstraints(button1, constraints);
        contentPane.add(button1);
        button1.addActionListener(this);

        constraints.gridx = leadSpace + constraints.gridwidth;
        gridbag.setConstraints(button2, constraints);
        contentPane.add(button2);
        button2.addActionListener(this);

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
    }


    /** Return childMarkerId,splitCreature1,splitCreature2,etc. */
    static String splitLegion(Client client, String oldMarkerId,
        String longMarkerName, String selectedMarkerId, 
        java.util.List imageNames)
    {
        if (!active)
        {
            active = true;
            new SplitLegion(client, oldMarkerId, longMarkerName,
                selectedMarkerId, imageNames);
            active = false;
            return results;
        }
        return null;
    }


    /** Move a chit to the end of the other line. */
    private void moveChitToOtherLine(java.util.List fromChits, java.util.List
        toChits, int oldPosition, int gridy)
    {
        Container contentPane = getContentPane();

        Chit chit = (Chit)fromChits.remove(oldPosition);
        contentPane.remove(chit);

        toChits.add(chit);

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = gridy;
        constraints.gridwidth = 1;
        gridbag.setConstraints(chit, constraints);
        contentPane.add(chit);

        pack();
        repaint();
    }


    /** Return true if the split is legal.  Each legion must have
     *  height >= 2.  If this was an initial split, each legion
     *  must have height == 4 and one lord. */
    private boolean isSplitLegal()
    {
        if (oldChits.size() < 2 || newChits.size() < 2)
        {
            client.showMessageDialog("Legion too short.");
            return false;
        }
        if (oldChits.size() + newChits.size() == 8)
        {
            if (oldChits.size() != newChits.size())
            {
                client.showMessageDialog("Initial split must be 4-4.");
                return false;
            }

            int numLords = 0;
            Iterator it = oldChits.iterator();
            while (it.hasNext())
            {
                Chit chit = (Chit)it.next();
                String id = chit.getId();
                if (id.startsWith("Titan") || id.equals("Angel"))
                {
                    numLords++;
                }
            }
            if (numLords != 1)
            {
                client.showMessageDialog("Each stack must have one lord.");
                return false;
            }
        }
        return true;
    }


    private void cancel()
    {
        results = null;
        dispose();
    }


    private void returnSplitResults()
    {
        StringBuffer buf = new StringBuffer(newMarker.getId());
        Iterator it = newChits.iterator();
        while (it.hasNext())
        {
            buf.append(",");
            Chit chit = (Chit)it.next();
            String creatureName = chit.getId();
            if (creatureName.startsWith("Titan"))
            {
                creatureName = "Titan";
            }
            buf.append(creatureName);
        }
        results = buf.toString();
        dispose();
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = oldChits.indexOf(source);
        if (i != -1)
        {
            moveChitToOtherLine(oldChits, newChits, i, 1);
            return;
        }
        else
        {
            i = newChits.indexOf(source);
            if (i != -1)
            {
                moveChitToOtherLine(newChits, oldChits, i, 0);
                return;
            }
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
        cancel();
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

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Done"))
        {
            if (!isSplitLegal())
            {
                return;
            }
            returnSplitResults();
        }
        else if (e.getActionCommand().equals("Cancel"))
        {
            cancel();
        }
    }
}
