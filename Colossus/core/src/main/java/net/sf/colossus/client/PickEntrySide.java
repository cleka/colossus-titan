package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

import net.sf.colossus.server.Constants;


/**
 * Class PickEntrySide allows picking which side of a MasterBoard hex
 * to enter.
 * @version $Id$
 * @author David Ripton
 */

final class PickEntrySide extends HexMap implements ActionListener,
    WindowListener
{
    private static JButton leftButton;
    private static JButton bottomButton;
    private static JButton rightButton;
    private static boolean laidOut;
    private JDialog dialog;
    private static String entrySide = "";

    private PickEntrySide(JFrame parentFrame, String masterHexLabel, Set sides)
    {
        super(masterHexLabel);
        dialog = new JDialog(parentFrame, "Pick entry side", true);
        laidOut = false;
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(null);

        if (sides.contains(Constants.left))
        {
            leftButton = new JButton(Constants.left);
            leftButton.setMnemonic(KeyEvent.VK_L);
            contentPane.add(leftButton);
            leftButton.addActionListener(this);
        }

        if (sides.contains(Constants.bottom))
        {
            bottomButton = new JButton(Constants.bottom);
            bottomButton.setMnemonic(KeyEvent.VK_B);
            contentPane.add(bottomButton);
            bottomButton.addActionListener(this);
        }

        if (sides.contains(Constants.right))
        {
            rightButton = new JButton(Constants.right);
            rightButton.setMnemonic(KeyEvent.VK_R);
            contentPane.add(rightButton);
            rightButton.addActionListener(this);
        }

        dialog.addWindowListener(this);

        setSize(getPreferredSize());
        contentPane.add(this);
        dialog.pack();

        dialog.setSize(getPreferredSize());
        dialog.setBackground(Color.white);
        dialog.setVisible(true);
    }

    static String pickEntrySide(JFrame parentFrame, String masterHexLabel,
        Set sides)
    {
        if (sides.size() >= 2)
        {
            new PickEntrySide(parentFrame, masterHexLabel, sides);
        }
        else
        {
            Iterator it = sides.iterator();
            if (it.hasNext())
            {
                entrySide = (String)it.next();
            }
        }
        return entrySide;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Abort if called too early.
        Rectangle rectClip = g.getClipBounds();
        if (rectClip == null)
        {
            return;
        }

        int scale = 2 * Scale.get();
        Dimension d = getSize();

        if (!laidOut)
        {
            if (leftButton != null)
            {
                leftButton.setBounds(cx + 1 * scale, cy + 1 * scale,
                    d.width / 7, d.height / 16);
            }
            if (bottomButton != null)
            {
                bottomButton.setBounds(cx + 1 * scale, cy + 21 * scale,
                    d.width / 7, d.height / 16);
            }
            if (rightButton != null)
            {
                rightButton.setBounds(cx + 19 * scale, cy + 11 * scale,
                    d.width / 7, d.height / 16);
            }

            laidOut = true;
        }

        if (rightButton != null)
        {
            rightButton.repaint();
        }
        if (bottomButton != null)
        {
            bottomButton.repaint();
        }
        if (leftButton != null)
        {
            leftButton.repaint();
        }
    }

    // Set hex's entry side to side, and then exit the dialog.  If side
    // is -1, then do not set an entry side, which will abort the move.
    private void cleanup(String side)
    {
        entrySide = side;
        dialog.dispose();
    }

    public void actionPerformed(ActionEvent e)
    {
        cleanup(e.getActionCommand());
    }

    public void windowClosing(WindowEvent e)
    {
        // Abort the move.
        cleanup("");
    }
}
