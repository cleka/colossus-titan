package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.parser.TerrainRecruitLoader;
import net.sf.colossus.server.Constants;

/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 * @version $Id$
 * @author David Ripton
 */

final class SplitLegion extends KDialog implements MouseListener,
    ActionListener, WindowListener
{
    private java.util.List oldChits = new ArrayList(8);
    private java.util.List newChits = new ArrayList(8);

    private Marker oldMarker;
    private Marker newMarker;

    private Client client;
    private static boolean active;
    private String selectedMarkerId;

    /** new marker id,creature1,creature2... */
    private static String results;

    // XXX Using Boxes here caused invisible chits in 1.3.1 but worked
    // in 1.4.0.  JPanels with BoxLayouts work.
    private JPanel oldBox;
    private JPanel newBox;
    private JPanel buttonBox;

    private int totalChits;
    private int scale;


    private SplitLegion(Client client, String parentId,
        String selectedMarkerId)
    {
        super(client.getBoard().getFrame(), client.getPlayerName() + 
            ": Split Legion " + parentId, true);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        this.client = client;

        if (selectedMarkerId == null)
        {
            dispose();
            return;
        }

        setBackground(Color.lightGray);

        addMouseListener(this);
        addWindowListener(this);

        scale = 4 * Scale.get();


        oldBox = new JPanel();
        oldBox.setLayout(new BoxLayout(oldBox, BoxLayout.X_AXIS));
        contentPane.add(oldBox);

        newBox = new JPanel();
        newBox.setLayout(new BoxLayout(newBox, BoxLayout.X_AXIS));
        contentPane.add(newBox);

        buttonBox = new JPanel();
        buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.X_AXIS));
        contentPane.add(buttonBox);


        oldMarker = new Marker(scale, parentId, this, null);
        oldBox.add(oldMarker);
        oldBox.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
        oldBox.add(Box.createHorizontalGlue());

        java.util.List imageNames = client.getLegionImageNames(parentId);
        totalChits = imageNames.size();

        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            oldChits.add(chit);
            oldBox.add(chit);
            chit.addMouseListener(this);
        }


        newMarker = new Marker(scale, selectedMarkerId, this, null);
        newBox.add(newMarker);
        newBox.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
        newBox.add(Box.createHorizontalGlue());

        // Add chit-sized invisible spacers.
        for (int i = 0; i < totalChits; i++)
        {
            newBox.add(Box.createRigidArea(new Dimension(scale, scale)));
        }

        JButton button1 = new JButton("Done");
        button1.setMnemonic(KeyEvent.VK_D);
        button1.addActionListener(this);
        buttonBox.add(button1);

        JButton button2 = new JButton("Cancel");
        button2.setMnemonic(KeyEvent.VK_C);
        button2.addActionListener(this);
        buttonBox.add(button2);

        pack();
        centerOnScreen();
        setVisible(true);
    }


    /** Return childMarkerId,splitCreature1,splitCreature2,etc. */
    static String splitLegion(Client client, String parentId,
        String selectedMarkerId)
    {
        if (!active)
        {
            active = true;
            new SplitLegion(client, parentId, selectedMarkerId);
            active = false;
            return results;
        }
        return null;
    }


    /** Move a chit to the end of the other line. */ 
    private void moveChitToOtherLine(java.util.List fromChits, java.util.List
        toChits, Container fromBox, Container toBox, int oldPosition)
    {
        Chit chit = (Chit)fromChits.remove(oldPosition);
        fromBox.remove(chit);
        fromBox.add(Box.createRigidArea(new Dimension(scale, 0)));

        toBox.remove(toBox.getComponentCount() - 1);
        toChits.add(chit);
        // Account for the marker and the spacer.
        toBox.add(chit, toChits.size() + 1);

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
                if (id.startsWith(Constants.titan) ||
                    id.equals(TerrainRecruitLoader.getPrimaryAcquirable()))
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
            if (creatureName.startsWith(Constants.titan))
            {
                creatureName = Constants.titan;
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
            moveChitToOtherLine(oldChits, newChits, oldBox, newBox, i);
            return;
        }
        else
        {
            i = newChits.indexOf(source);
            if (i != -1)
            {
                moveChitToOtherLine(newChits, oldChits, newBox, oldBox, i);
                return;
            }
        }
    }

    public void windowClosing(WindowEvent e)
    {
        cancel();
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
