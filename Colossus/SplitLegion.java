import java.awt.*;
import java.awt.event.*;

/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 * @version $Id$
 * @author David Ripton
 */

class SplitLegion extends Dialog implements MouseListener, ActionListener,
    WindowListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private Legion oldLegion;
    private Legion newLegion;
    private Chit [] oldChits;
    private Chit [] newChits;
    private Marker oldMarker;
    private Marker newMarker;
    private Player player;
    private static final int scale = 60;
    private Frame parentFrame;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();


    SplitLegion(Frame parentFrame, Legion oldLegion, Player player)
    {
        super(parentFrame, player.getName() + ": Split Legion " +
            oldLegion.getMarkerId(), true);

        setLayout(gridbag);

        this.oldLegion = oldLegion;
        this.player = player;
        this.parentFrame = parentFrame;

        new PickMarker(parentFrame, player);

        if (player.getSelectedMarker() == null)
        {
            dispose();
            return;
        }
       
        pack();

        newLegion = new Legion(scale, player.getSelectedMarker(), oldLegion,
            this, 0, oldLegion.getCurrentHex(), null, null, null, null, null,
            null, null, null, player);

        setBackground(Color.lightGray);
        setResizable(false);

        // If there were no markers left to pick, exit.
        if (player.getSelectedMarker() == null)
        {
            dispose();
        }
        else
        {
            addMouseListener(this);
            addWindowListener(this);

            oldMarker = new Marker(scale, oldLegion.getImageName(), this,
                oldLegion);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(oldMarker, constraints);
            add(oldMarker);

            oldChits = new Chit[oldLegion.getHeight()];
            for (int i = 0; i < oldLegion.getHeight(); i++)
            {
                oldChits[i] = new Chit(scale,
                    oldLegion.getCritter(i).getImageName(), this);
                gridbag.setConstraints(oldChits[i], constraints);
                add(oldChits[i]);
                oldChits[i].addMouseListener(this);
            }
       
            newMarker = newLegion.getMarker();
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(newMarker, constraints);
            add(newMarker);

            newChits = new Chit[oldLegion.getHeight()];

            tracker = new MediaTracker(this);

            for (int i = 0; i < oldLegion.getHeight(); i++)
            {
                tracker.addImage(oldChits[i].getImage(), 0);
            }
            tracker.addImage(oldMarker.getImage(), 0);
            tracker.addImage(newMarker.getImage(), 0);

            try
            {
                tracker.waitForAll();
            }
            catch (InterruptedException e)
            {
                new MessageBox(parentFrame, e.toString() +
                    "waitForAll was interrupted");
            }
            imagesLoaded = true;

            Button button1 = new Button("Done");
            Button button2 = new Button("Cancel");

             // Attempt to center the buttons.
            int chitWidth = Math.max(oldLegion.getHeight(),
                newLegion.getHeight()) + 1;
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
            add(button1);
            button1.addActionListener(this);
            constraints.gridx = leadSpace + constraints.gridwidth;
            gridbag.setConstraints(button2, constraints);
            add(button2);
            button2.addActionListener(this);

            pack();

            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2));

            setVisible(true);
        }
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(d.width, d.height);
            offGraphics = offImage.getGraphics();
        }

        g.drawImage(offImage, 0, 0, this);
    }


    // Double-buffer everything.
    public void paint(Graphics g)
    {
        update(g);
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


    // Move a Creature over to the other Legion and move its chit to the
    // end of the other line.
    private void moveCreatureToOtherLegion(Legion fromLegion, Legion toLegion,
        Chit [] fromChits, Chit [] toChits, int oldPosition, int gridy)
    {
        toLegion.setHeight(toLegion.getHeight() + 1);
        toLegion.setCreature(toLegion.getHeight() - 1,
            fromLegion.getCreature(oldPosition));
        toChits[toLegion.getHeight() - 1] = fromChits[oldPosition];
        remove(fromChits[oldPosition]);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = gridy;
        constraints.gridwidth = 1;
        gridbag.setConstraints(toChits[toLegion.getHeight() - 1],
            constraints);
        add(toChits[toLegion.getHeight() - 1]);

        for (int j = oldPosition; j < fromLegion.getHeight() - 1; j++)
        {
            fromLegion.setCreature(j, fromLegion.getCreature(j + 1));
            fromChits[j] = fromChits[j + 1];
        }
        fromLegion.setCreature(fromLegion.getHeight() - 1, null);
        fromChits[fromLegion.getHeight() - 1] = null;
        fromLegion.setHeight(fromLegion.getHeight() - 1);

        // Update the stack heights on both markers.
        oldMarker.repaint();
        newMarker.repaint();

        pack();
       
        repaint();
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        for (int i = 0; i < oldLegion.getHeight(); i++)
        {
            if (oldChits[i] == source)
            {
                moveCreatureToOtherLegion(oldLegion, newLegion,
                    oldChits, newChits, i, 1);
                return;
            }
        }

        for (int i = 0; i < newLegion.getHeight(); i++)
        {
            if (newChits[i] == source)
            {
                moveCreatureToOtherLegion(newLegion, oldLegion,
                    newChits, oldChits, i, 0);
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
            // Check to make sure that each Legion is legal.
            // Each legion must have 2 <= height <= 7.
            // Also, if this was an initial split, each Legion
            // must have height 4 and one lord.
            if (oldLegion.getHeight() < 2 || newLegion.getHeight() < 2)
            {
                new MessageBox(parentFrame, "Legion too short.");
                return;
            }
            if (oldLegion.getHeight() + newLegion.getHeight() == 8)
            {
                if (oldLegion.getHeight() != newLegion.getHeight())
                {
                    new MessageBox(parentFrame, "Initial split must be 4-4.");
                    return;
                }
                else
                {
                    if (oldLegion.numLords() != 1)
                    {
                        new MessageBox(parentFrame,
                            "Each stack must have one lord.");
                        return;
                    }
                }
            }

            // The split is legal.

            // Resize the new legion to MasterBoard scale.
            newMarker.rescale(oldLegion.getMarker().getBounds().width);

            // Add the new legion to the player.
            if (player != null)
            {
                player.addLegion(newLegion);
            }

            // Set the new chit next to the old chit on the masterboard.
            MasterHex hex = newLegion.getCurrentHex();
            if (hex != null)
            {
                newLegion.getCurrentHex().addLegion(newLegion);
            }

            // Hide the contents of both legions.
            oldLegion.hideAllCreatures();
            newLegion.hideAllCreatures();

            // Mark the last legion split.
            if (player != null)
            {
                player.markLastLegionSplit(newLegion);
            }

            // Exit.
            dispose();

            Game.logEvent(newLegion.getHeight() + 
                " creatures were split off from legion " + 
                oldLegion.getMarkerId() +
                " into new legion " + newLegion.getMarkerId());
        }

        else if (e.getActionCommand().equals("Cancel"))
        {
            cancel();
        }
    }


    public static void main(String [] args)
    {
        Frame frame = new Frame("testing SplitLegion");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        Player player = new Player("Test", null);
        player.setTower(1);
        player.setColor("Red");
        player.initMarkersAvailable();
        player.selectMarker(0);

        Legion legion = new Legion(scale,
            player.getSelectedMarker(), null, frame, 8,
            null, Creature.titan, Creature.angel, Creature.ogre,
            Creature.ogre, Creature.centaur, Creature.centaur,
            Creature.gargoyle, Creature.gargoyle, player);

        new SplitLegion(frame, legion, player);
    }
}
