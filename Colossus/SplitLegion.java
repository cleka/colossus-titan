import java.awt.*;
import java.awt.event.*;
import com.sun.java.swing.*;

/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 * @version $Id$
 * author David Ripton
 */

class SplitLegion extends JDialog implements MouseListener, ActionListener,
    WindowListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private Legion oldLegion;
    private Legion newLegion;
    private Chit [] oldChits;
    private Chit [] newChits;
    private Chit oldMarker;
    private Player player;
    private static final int scale = 60;
    private JFrame parentFrame;
    private JButton button1;
    private JButton button2;
    private boolean laidOut = false;
    private boolean eraseFlag = false;
    private Graphics gBack;
    private Dimension offDimension;
    private Image offImage;


    SplitLegion(JFrame parentFrame, Legion oldLegion, Player player)
    {
        super(parentFrame, player.getName() + ": Split Legion " + 
            oldLegion.getMarkerId(), true);

        setResizable(false);
        getContentPane().setLayout(null);
        setBackground(java.awt.Color.lightGray);

        this.oldLegion = oldLegion;
        this.player = player;
        this.parentFrame = parentFrame;

        imagesLoaded = false;

        PickMarker pickmarker = new PickMarker(parentFrame, player);

        if (player.getSelectedMarker() == null)
        {
            dispose();
            return;
        }

        newLegion = new Legion(scale / 5, 2 * scale, scale,
            player.getSelectedMarker(), oldLegion, this, 0, 
            oldLegion.getCurrentHex(), null, null, null, null, null, null, 
            null, null, player);

        pack();
        setSize(getPreferredSize());

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
            - getSize().height / 2));

        // If there were no markers left to pick, exit.
        if (player.getSelectedMarker() == null)
        {
            dispose();
        }
        else
        {
            addMouseListener(this);
            addWindowListener(this);

            oldChits = new Chit[oldLegion.getHeight()];
            for (int i = 0; i < oldLegion.getHeight(); i++)
            {
                oldChits[i] = new Chit((i + 1) * scale + (scale / 5), 
                    scale / 2, scale, oldLegion.getCreature(i).getImageName(),
                    this, false);
            }
            newChits = new Chit[oldLegion.getHeight()];
            
            oldMarker = new Chit(scale / 5, scale / 2, scale, 
                oldLegion.getImageName(), this, false);

            tracker = new MediaTracker(this);

            for (int i = 0; i < oldLegion.getHeight(); i++)
            {
                tracker.addImage(oldChits[i].getImage(), 0);
            }
            tracker.addImage(oldMarker.getImage(), 0);
            tracker.addImage(newLegion.getMarker().getImage(), 0);

            try
            {
                tracker.waitForAll();
            }
            catch (InterruptedException e)
            {
                JOptionPane.showMessageDialog(parentFrame, 
                    "waitForAll was interrupted");
            }

            button1 = new JButton("Done");
            button2 = new JButton("Cancel");
            getContentPane().add(button1);
            getContentPane().add(button2);
            button1.addActionListener(this);
            button2.addActionListener(this);

            imagesLoaded = true;
            setVisible(true);
        }
    }


    // Double-buffer everything.
    public void paint(Graphics g)
    {
        update(g);
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();

        // Create the back buffer only if we don't have a good one.
        if (gBack == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(d.width, d.height);
            gBack = offImage.getGraphics();
            eraseFlag = true;
        }

        // Clear the chit area to clean up behind chits that have been moved.
        if (eraseFlag)
        {
            gBack.setColor(getBackground());
            gBack.fillRect(0, 0, d.width, d.height);
            gBack.setColor(getForeground());
            eraseFlag = false;
        }

        oldMarker.paint(gBack);

        newLegion.getMarker().paint(gBack);

        for (int i = oldLegion.getHeight() - 1; i >= 0; i--)
        {
            oldChits[i].paint(gBack);
        }
        for (int i = newLegion.getHeight() - 1; i >= 0; i--)
        {
            newChits[i].paint(gBack);
        }

        if (!laidOut)
        {
            Insets insets = getInsets(); 
            button1.setBounds(insets.left + d.width / 9, 3 * d.height / 4 - 
                insets.bottom, d.width / 3, d.height / 8);
            button2.setBounds(5 * d.width / 9 - insets.right, 
                3 * d.height / 4 - insets.bottom, d.width / 3, d.height / 8);
            laidOut = true;
        }

        g.drawImage(offImage, 0, 0, this);

        button1.repaint();
        button2.repaint();
    }


    void cancel()
    {
        player.addSelectedMarker();

        for (int i = 0; i < newLegion.getHeight(); i++)
        {
            oldLegion.setHeight(oldLegion.getHeight() + 1);
            oldLegion.setCreature(oldLegion.getHeight() - 1,
                newLegion.getCreature(i));
        }

        dispose();
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i = 0; i < oldLegion.getHeight(); i++)
        {
            if (oldChits[i].select(point))
            {
                // Got a hit.
                // Move this Creature over to the other Legion and adjust 
                // appropriate chit screen coordinates.
                newLegion.setHeight(newLegion.getHeight() + 1);
                newLegion.setCreature(newLegion.getHeight() - 1,
                    oldLegion.getCreature(i));
                newChits[newLegion.getHeight() - 1] = oldChits[i];
                newChits[newLegion.getHeight() - 1].setLocationAbs(new 
                    Point(newLegion.getHeight() * scale + scale / 5, 
                    2 * scale));

                for (int j = i; j < oldLegion.getHeight() - 1; j++)
                {
                    oldLegion.setCreature(j, oldLegion.getCreature(j + 1));
                    oldChits[j] = oldChits[j + 1];
                    oldChits[j].setLocationAbs(new Point((j + 1) * scale + 
                        scale / 5, scale / 2));
                }
                oldLegion.setCreature(oldLegion.getHeight() - 1, null);
                oldChits[oldLegion.getHeight() - 1] = null;
                oldLegion.setHeight(oldLegion.getHeight() - 1);

                eraseFlag = true;
                repaint();
                return;
            }
        }
        for (int i = 0; i < newLegion.getHeight(); i++)
        {
            if (newChits[i].select(point))
            {
                // Got a hit.
                // Move this Creature over to the other Legion and adjust 
                // appropriate chit screen coordinates.
                oldLegion.setHeight(oldLegion.getHeight() + 1);
                oldLegion.setCreature(oldLegion.getHeight() - 1, 
                    newLegion.getCreature(i));
                oldChits[oldLegion.getHeight() - 1] = newChits[i];
                oldChits[oldLegion.getHeight() - 1].setLocationAbs(new 
                    Point(oldLegion.getHeight() * scale + scale / 5, 
                    scale / 2));

                for (int j = i; j < newLegion.getHeight() - 1; j++)
                {
                    newLegion.setCreature(j, newLegion.getCreature(j + 1));
                    newChits[j] = newChits[j + 1];
                    newChits[j].setLocationAbs(new Point((j + 1) * scale + 
                        scale / 5, 2 * scale));
                }
                newLegion.setCreature(newLegion.getHeight() - 1, null);
                newChits[newLegion.getHeight() - 1] = null;
                newLegion.setHeight(newLegion.getHeight() - 1);

                eraseFlag = true;
                repaint();
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



    public void windowActivated(WindowEvent event)
    {
    }

    public void windowClosed(WindowEvent event)
    {
    }

    public void windowClosing(WindowEvent event)
    {
        cancel();
    }

    public void windowDeactivated(WindowEvent event)
    {
    }
                                                                                                                     
    public void windowDeiconified(WindowEvent event)
    {
    }

    public void windowIconified(WindowEvent event)
    {
    }

    public void windowOpened(WindowEvent event)
    {
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "Done")
        {
            // Check to make sure that each Legion is legal.
            // Each legion must have 2 <= height <= 7.
            // Also, if this was an initial split, each Legion
            // must have height 4 and one lord.
            if (oldLegion.getHeight() < 2 || newLegion.getHeight() < 2)
            {
                JOptionPane.showMessageDialog(parentFrame, 
                    "Legion too short.");
                return;
            }
            if (oldLegion.getHeight() + newLegion.getHeight() == 8)
            {
                if (oldLegion.getHeight() != newLegion.getHeight())
                {
                    JOptionPane.showMessageDialog(parentFrame, 
                        "Initial split not 4-4.");
                    return;
                }
                else
                {
                    if (oldLegion.numLords() != 1)
                    {
                        JOptionPane.showMessageDialog(parentFrame, 
                            "Each stack must have one lord.");
                        return;
                    }
                }
            }

            // The split is legal.

            // Resize the new legion to MasterBoard scale.
            newLegion.getMarker().rescale(
                oldLegion.getMarker().getBounds().width);

            // Add the new legion to the player.
            player.addLegion(newLegion);
            
            // Set the new chit next to the old chit on the masterboard.
            newLegion.getCurrentHex().addLegion(newLegion);

            // Exit.
            dispose();
        }

        else if (e.getActionCommand() == "Cancel")
        {
            cancel();
        }
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(scale * (oldLegion.getHeight() + 
            newLegion.getHeight() + 2), 4 * scale);
    }
}
