import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 * @version $Id$
 * author David Ripton
 */

class Concede extends JDialog implements ActionListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private static final int scale = 60;
    private JFrame parentFrame;
    private JButton button1;
    private JButton button2;
    private boolean laidOut = false;
    private boolean flee;
    private Legion friend;
    private Legion enemy;
    private Chit [] friendChits;
    private Chit [] enemyChits;
    private Chit friendMarker;
    private Chit enemyMarker;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    Concede(JFrame parentFrame, Legion friend, Legion enemy, boolean flee)
    {
        super(parentFrame, friend.getPlayer().getName() + ": " + (flee ?
            "Flee" : "Concede") + " with Legion "  + friend.getMarkerId() +
            " in " + friend.getCurrentHex().getTerrainName().toLowerCase() +
            "?", true);

        Container contentPane = getContentPane();
        contentPane.setLayout(null);

        this.parentFrame = parentFrame;
        this.friend = friend;
        this.enemy = enemy;
        this.flee = flee;

        pack();

        setBackground(java.awt.Color.lightGray);
        setSize(getPreferredSize());
        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
            - getSize().height / 2));

        // Leave space for angels.
        friendChits = new Chit[7];
        for (int i = 0; i < friend.getHeight(); i++)
        {
            friendChits[i] = new Chit((i + 1) * scale + (scale / 5), scale / 2,
                scale, friend.getCreature(i).getImageName(), this);
        }

        // Leave space for angels.
        enemyChits = new Chit[7];
        for (int i = 0; i < enemy.getHeight(); i++)
        {
            enemyChits[i] = new Chit((i + 1) * scale + (scale / 5),
                2 * scale, scale, enemy.getCreature(i).getImageName(), this);
        }

        friendMarker = new Marker(scale / 5, scale / 2, scale,
            friend.getImageName(), this, friend);

        enemyMarker = new Marker(scale / 5, 2 * scale, scale,
            enemy.getImageName(), this, enemy);

        tracker = new MediaTracker(this);

        for (int i = 0; i < friend.getHeight(); i++)
        {
            tracker.addImage(friendChits[i].getImage(), 0);
        }
        for (int i = 0; i < enemy.getHeight(); i++)
        {
            tracker.addImage(enemyChits[i].getImage(), 0);
        }
        tracker.addImage(friendMarker.getImage(), 0);
        tracker.addImage(enemyMarker.getImage(), 0);

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            JOptionPane.showMessageDialog(parentFrame,
                "waitForAll was interrupted");
        }
        imagesLoaded = true;

        button1 = new JButton(flee ? "Flee" : "Concede");
        button2 = new JButton(flee ? "Don't Flee" : "Don't Concede");
        contentPane.add(button1);
        contentPane.add(button2);
        button1.addActionListener(this);
        button2.addActionListener(this);

        setVisible(true);
        repaint();
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();
        Rectangle rectClip = g.getClipBounds();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        friendMarker.paint(offGraphics);

        enemyMarker.paint(offGraphics);

        for (int i = friend.getHeight() - 1; i >= 0; i--)
        {
            // If the stack just acquired an angel, it won't be painted.
            if (friendChits[i] != null)
            {
                friendChits[i].paint(offGraphics);
            }
        }
        for (int i = enemy.getHeight() - 1; i >= 0; i--)
        {
            // If the stack just acquired an angel, it won't be painted.
            if (enemyChits[i] != null)
            {
                enemyChits[i].paint(offGraphics);
            }
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

        // These are necessary because JButtons are lightweight.
        button1.repaint();
        button2.repaint();
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    // Attempt to free resources to work around Java memory leaks.
    private void cleanup()
    {
        setVisible(false);

        if (offImage != null)
        {
            offImage.flush();
            offGraphics.dispose();
        }

        if (imagesLoaded)
        {
            for (int i = 0; i < friend.getHeight(); i++)
            {
                tracker.removeImage(friendChits[i].getImage());
                friendChits[i].getImage().flush();
            }
            for (int i = 0; i < enemy.getHeight(); i++)
            {
                // Enemy may just have acquired an angel which does not have
                // a chit here, so check for null.
                if (enemyChits[i] != null)
                {
                    tracker.removeImage(enemyChits[i].getImage());
                    enemyChits[i].getImage().flush();
                }
            }
            tracker.removeImage(friendMarker.getImage());
            friendMarker.getImage().flush();
            tracker.removeImage(enemyMarker.getImage());
            enemyMarker.getImage().flush();
        }

        dispose();
        System.gc();
        try
        {
            finalize();
        }
        catch (Throwable e)
        {
            System.out.println("caught " + e.toString());
        }
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "Flee" || e.getActionCommand() ==
            "Concede")
        {
            // Figure how many points the victor receives.
            int points = friend.getPointValue();
            if (flee)
            {
                points /= 2;
            }

            // Remove the dead legion.
            friend.removeLegion();

            // Add points, and angels if necessary.
            enemy.addPoints(points);

            // If this was the titan stack, its owner dies and gives half
            // points to the victor.
            if (friend.numCreature(Creature.titan) == 1)
            {
                friend.getPlayer().die(enemy.getPlayer(), true);
            }

            // Exit this dialog.
            cleanup();

            // Unselect and repaint the hex.
            MasterHex hex = enemy.getCurrentHex();
            hex.unselect();
            hex.repaint();
        }

        else
        {
            cleanup();
        }
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(scale * (Math.max(friend.getHeight(),
            enemy.getHeight()) + 2), 4 * scale);
    }
}
