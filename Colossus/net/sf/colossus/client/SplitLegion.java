package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import net.sf.colossus.server.Game;
import net.sf.colossus.util.KDialog;

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

    private Box oldBox;
    private Box newBox;
    private Box buttonBox;

    private Component oldGlue;
    private Component newGlue;

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

        oldBox = Box.createHorizontalBox();
        newBox = Box.createHorizontalBox();
        buttonBox = Box.createHorizontalBox();

        contentPane.add(oldBox);
        contentPane.add(newBox);
        contentPane.add(buttonBox);

        oldMarker = new Marker(scale, parentId, this, null);
        oldBox.add(oldMarker);
        oldBox.add(Box.createHorizontalStrut(scale / 4));

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
        newBox.add(Box.createHorizontalStrut(scale / 4));

        oldGlue = Box.createHorizontalGlue();
        newGlue = Box.createHorizontalGlue();
        oldBox.add(oldGlue);
        newBox.add(newGlue);

        JButton button1 = new JButton("Done");
        button1.setMnemonic(KeyEvent.VK_D);
        JButton button2 = new JButton("Cancel");
        button2.setMnemonic(KeyEvent.VK_C);

        buttonBox.add(button1);
        button1.addActionListener(this);

        buttonBox.add(button2);
        button2.addActionListener(this);

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
        toChits, Box fromBox, Box toBox, int oldPosition)
    {
        oldBox.remove(oldGlue);
        newBox.remove(newGlue);

        Chit chit = (Chit)fromChits.remove(oldPosition);
        fromBox.remove(chit);
        toChits.add(chit);
        toBox.add(chit);

        oldBox.add(oldGlue);
        newBox.add(newGlue);

        //pack();
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
                if (id.startsWith("Titan") ||
                    id.equals(Game.getPrimaryAcquirable()))
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
