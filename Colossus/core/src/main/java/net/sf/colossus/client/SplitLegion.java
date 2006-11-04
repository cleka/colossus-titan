package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;

import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;


/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 * @version $Id$
 * @author David Ripton
 */

final class SplitLegion extends KDialog implements MouseListener,
            ActionListener, WindowListener
{
    private List oldChits = new ArrayList(8);
    private List newChits = new ArrayList(8);

    private Marker oldMarker;
    private Marker newMarker;

    private Client client;
    private static boolean active;

    /** new marker id,creature1,creature2... */
    private static String results;

    private Box oldBox;
    private Box newBox;
    private Box buttonBox;

    private JButton button1;
    private JButton button2;

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

        oldBox = new Box(BoxLayout.X_AXIS);
        contentPane.add(oldBox);

        newBox = new Box(BoxLayout.X_AXIS);
        contentPane.add(newBox);

        buttonBox = new Box(BoxLayout.X_AXIS);
        contentPane.add(buttonBox);

        oldMarker = new Marker(scale, parentId);
        oldBox.add(oldMarker);
        oldBox.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
        oldBox.add(Box.createHorizontalGlue());

        List imageNames = client.getLegionImageNames(parentId);
        totalChits = imageNames.size();

        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName);
            oldChits.add(chit);
            oldBox.add(chit);
            chit.addMouseListener(this);
        }

        newMarker = new Marker(scale, selectedMarkerId);
        newBox.add(newMarker);
        newBox.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
        newBox.add(Box.createHorizontalGlue());

        // Add chit-sized invisible spacers.
        for (int i = 0; i < totalChits; i++)
        {
            newBox.add(Box.createRigidArea(new Dimension(scale, scale)));
        }

        button1 = new JButton("Done");
        button1.setEnabled(false);
        button1.setMnemonic(KeyEvent.VK_D);
        button1.addActionListener(this);
        buttonBox.add(button1);

        button2 = new JButton("Cancel");
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
    private void moveChitToOtherLine(List fromChits, List
            toChits, Container fromBox, Container toBox, int oldPosition)
    {
        Chit chit = (Chit)fromChits.remove(oldPosition);
        fromBox.remove(chit);
        fromBox.add(Box.createRigidArea(new Dimension(scale, 0)));

        toBox.remove(toBox.getComponentCount() - 1);
        toChits.add(chit);
        // Account for the marker and the spacer.
        toBox.add(chit, toChits.size() + 1);

        button1.setEnabled(isSplitLegal(false));

        pack();
        repaint();
    }

    /** Return true if the split is legal.  Each legion must have
     *  height >= 2.  If this was an initial split, each legion
     *  must have height == 4 and one lord. */
    private boolean isSplitLegal(boolean showMessage)
    {
        if (oldChits.size() < 2 || newChits.size() < 2)
        {
            if (showMessage)
            {
                client.showMessageDialog("Legion too short.");
            }
            return false;
        }
        if (oldChits.size() + newChits.size() == 8)
        {
            if (oldChits.size() != newChits.size())
            {
                if (showMessage)
                {
                    client.showMessageDialog("Initial split must be 4-4.");
                }
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
                if (showMessage)
                {
                    client.showMessageDialog("Each stack must have one lord.");
                }
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
        StringBuffer buf = new StringBuffer();
        Iterator it = newChits.iterator();
        while (it.hasNext())
        {
            Chit chit = (Chit)it.next();
            String creatureName = chit.getId();
            if (creatureName.startsWith(Constants.titan))
            {
                creatureName = Constants.titan;
            }
            buf.append(creatureName);
            if (it.hasNext())
            {
                buf.append(",");
            }
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
            if (!isSplitLegal(true))
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
