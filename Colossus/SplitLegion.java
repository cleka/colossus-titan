import java.awt.*;
import java.awt.event.*;

/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 * @version $Id$
 * author David Ripton
 */

class SplitLegion extends Dialog implements MouseListener, ActionListener
{
    MediaTracker tracker;
    boolean imagesLoaded;
    Legion oldLegion;
    Legion newLegion;
    Chit [] oldChits;
    Chit [] newChits;
    Chit oldMarker;
    Player player;
    final int scale = 60;

    SplitLegion(Frame parentFrame, Legion oldLegion, Player player)
    {
        super(parentFrame, "Split Legion " + oldLegion.markerId, true);

        setLocation(new Point(scale, 4 * scale));
        setLayout(null);

        this.oldLegion = oldLegion;
        this.player = player;

        imagesLoaded = false;

        PickMarker pickmarker = new PickMarker(parentFrame, player);
        setSize((21 * scale / 20) * (oldLegion.height + 1), 7 * scale / 2);

        // If there were no markers left to pick, exit.
        if (player.markerSelected == null)
        {
            dispose();
        }
        else
        {
            addMouseListener(this);

            oldChits = new Chit[oldLegion.height];
            for (int i = 0; i < oldLegion.height; i++)
            {
                oldChits[i] = new Chit((i + 1) * scale + (scale / 5), scale / 2,
                    scale, "images/" + oldLegion.creatures[i].name + ".gif",
                    this);
            }
            newChits = new Chit[oldLegion.height];
            
            oldMarker = new Chit(scale / 5, scale / 2, scale, 
                "images/" + oldLegion.markerId + ".gif", this);

            newLegion = new Legion(scale / 5, 2 * scale, scale, 
                player.markerSelected, oldLegion.markerId, this, 0, null,
                null, null, null, null, null, null, null);

            tracker = new MediaTracker(this);

            for (int i = 0; i < oldLegion.height; i++)
            {
                tracker.addImage(oldChits[i].image, 0);
            }
            tracker.addImage(oldMarker.image, 0);
            tracker.addImage(newLegion.chit.image, 0);

            try
            {
                tracker.waitForAll();
            }
            catch (InterruptedException e)
            {
                System.out.println("waitForAll was interrupted");
            }

            Button button1 = new Button("Done");
            Button button2 = new Button("Cancel");
            add(button1);
            add(button2);
            button1.addActionListener(this);
            button2.addActionListener(this);

            pack();

            imagesLoaded = true;
            setVisible(true);
            repaint();
        }
    }


    public void paint(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        //Rectangle rectClip = g.getClipBounds();

        //if (rectClip.intersects(oldMarker.getBounds()))
        {
            oldMarker.paint(g);
        }
        //if (rectClip.intersects(newLegion.chit.getBounds()))
        {
            newLegion.chit.paint(g);
        }

        for (int i = oldLegion.height - 1; i >= 0; i--)
        {
            //if (rectClip.intersects(oldChits[i].getBounds()))
            {
                oldChits[i].paint(g);
            }
        }
        for (int i = newLegion.height - 1; i >= 0; i--)
        {
            //if (rectClip.intersects(newChits[i].getBounds()))
            {
                newChits[i].paint(g);
            }
        }
    }


    public void mouseClicked(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i = 0; i < oldLegion.height; i++)
        {
            if (oldChits[i].select(point))
            {
System.out.println("hit on old chit " + i); 
                // Got a hit.
                //Rectangle clip = new Rectangle(oldChits[i].getBounds());

                // Move this Creature over to the other Legion and adjust 
                // appropriate chit screen coordinates.
                newLegion.height++;
System.out.println("new legion height is now " + newLegion.height);
                newLegion.creatures[newLegion.height - 1] = 
                    oldLegion.creatures[i];
System.out.println("new legion creature " + (newLegion.height - 1) +
" set to old legion creature " + i);
                newChits[newLegion.height - 1] = oldChits[i];
                newChits[newLegion.height - 1].setLocation(new 
                    Point(newLegion.height * scale + scale / 5, 2 * scale));
System.out.println("chit now at (" + newChits[newLegion.height - 1].topLeft().x 
+ ", " + newChits[newLegion.height - 1].topLeft().y + ")");

                for (int j = i; j < oldLegion.height - 1; j++)
                {
                    oldLegion.creatures[j] = oldLegion.creatures[j + 1];
System.out.println("old legion creature " + j + " set to old legion creature "
+ (j + 1));
                    oldChits[j] = oldChits[j + 1];
                    oldChits[j].setLocation(new Point((j + 1) * scale + scale / 5,
                        scale));
                }
                oldLegion.creatures[oldLegion.height - 1] = null;
                oldChits[oldLegion.height - 1] = null;
                oldLegion.height--;

                //clip.add(newChits[].getBounds());
                repaint(/*clip.x, clip.y, clip.width, clip.height*/);
                return;
            }
        }
        for (int i = 0; i < newLegion.height; i++)
        {
            if (newChits[i].select(point))
            {
System.out.println("hit on new chit " + i); 
                // Got a hit.
                //Rectangle clip = new Rectangle(newChits[i].getBounds());

                // Move this Creature over to the other Legion.

                // And adjust its screen coordinates.

                //clip.add(oldChits[i].getBounds());
                repaint(/*clip.x, clip.y, clip.width, clip.height*/);
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

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
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
            if (oldLegion.height < 2 || newLegion.height < 2)
            {
                System.out.println("Legion too short.");
                return;
            }
            if (oldLegion.height + newLegion.height == 8)
            {
                if (oldLegion.height != newLegion.height)
                {
                    System.out.println("Initial split not 4-4.");
                    return;
                }
                else
                {
                    int lordCounter = 0;
                    for (int i = 0; i < 4; i++)
                    {
                        if (oldLegion.creatures[i].lord)
                        {
                            lordCounter++;
                        }
                    }
                    if (lordCounter != 1)
                    {
                        System.out.println("Each stack must have one lord.");
                        return;
                    }
                }
            }

            // The split is legal.
            // Set the new chit next to the old chit on the masterboard.
            newLegion.chit.setLocation(oldLegion.chit.center());

            // Add the new legion to the player.
            player.numLegions++;
            player.legions[player.numLegions - 1] = newLegion;

            // Exit.
            dispose();
        }

        else if (e.getActionCommand() == "Cancel")
        {
            // Original legion must have height < 8 for this
            // to be allowed.
            if (oldLegion.height >= 8)
            {
                System.out.println("Must split.");
            }

            // Put the stack marker back, reset the old legion, then exit.
            else
            {
                player.numMarkersAvailable++;
                player.markersAvailable[player.numMarkersAvailable - 1] =
                    new String(player.markerSelected);
                player.markerSelected = null;

                for (int i = 0; i < newLegion.height; i++)
                {
                    oldLegion.height++;
                    oldLegion.creatures[oldLegion.height - 1] = 
                        newLegion.creatures[i];
                }

                dispose();
            }
        }
    }
}
