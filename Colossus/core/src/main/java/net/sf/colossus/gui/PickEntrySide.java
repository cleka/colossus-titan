package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

import net.sf.colossus.client.HexMap;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.variant.MasterHex;


/**
 * Class PickEntrySide allows picking which side of a MasterBoard hex
 * to enter.
 *
 * @author David Ripton
 */
@SuppressWarnings("serial")
final class PickEntrySide extends HexMap
{
    private static JButton leftButton;
    private static JButton bottomButton;
    private static JButton rightButton;
    private static boolean laidOut;
    private final JDialog dialog;
    private static EntrySide entrySide;

    private PickEntrySide(JFrame parentFrame, MasterHex masterHex,
        Set<EntrySide> sides)
    {
        super(masterHex);
        dialog = new JDialog(parentFrame, "Pick entry side", true);
        laidOut = false;
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(null);

        // TODO refactor with a loop over sides and a switch over the enum
        if (sides.contains(EntrySide.LEFT))
        {
            leftButton = new JButton(EntrySide.LEFT.getLabel());
            leftButton.setMnemonic(KeyEvent.VK_L);
            contentPane.add(leftButton);
            leftButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    cleanup(EntrySide.LEFT);
                }
            });
        }

        if (sides.contains(EntrySide.BOTTOM))
        {
            bottomButton = new JButton(EntrySide.BOTTOM.getLabel());
            bottomButton.setMnemonic(KeyEvent.VK_B);
            contentPane.add(bottomButton);
            bottomButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    cleanup(EntrySide.BOTTOM);
                }
            });
        }

        if (sides.contains(EntrySide.RIGHT))
        {
            rightButton = new JButton(EntrySide.RIGHT.getLabel());
            rightButton.setMnemonic(KeyEvent.VK_R);
            contentPane.add(rightButton);
            rightButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    cleanup(EntrySide.RIGHT);
                }
            });
        }

        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                // Abort the move.
                cleanup(null);
            }
        });

        setSize(getPreferredSize());
        contentPane.add(this);
        dialog.pack();

        dialog.setSize(getPreferredSize());
        dialog.setBackground(Color.white);
        dialog.setVisible(true);
    }

    static EntrySide pickEntrySide(JFrame parentFrame, MasterHex masterHex,
        Set<EntrySide> sides)
    {
        if (sides.size() >= 2)
        {
            new PickEntrySide(parentFrame, masterHex, sides);
        }
        else
        {
            Iterator<EntrySide> it = sides.iterator();
            if (it.hasNext())
            {
                entrySide = it.next();
            }
        }
        return entrySide;
    }

    @Override
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
    private void cleanup(EntrySide side)
    {
        entrySide = side;
        dialog.dispose();
    }
}
