import java.awt.*;
import java.awt.event.*;

/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 * @version $Id$
 * author David Ripton
 */

class Concede extends Dialog implements ActionListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private static final int scale = 60;
    private Frame parentFrame;
    private Button button1;
    private Button button2;
    private boolean laidOut = false;
    private boolean flee;
    private Legion friend;
    private Legion enemy;
    private Chit [] friendChits;
    private Chit [] enemyChits;
    private Chit friendMarker;
    private Chit enemyMarker;


    Concede(Frame parentFrame, Legion friend, Legion enemy, boolean flee)
    {
        super(parentFrame, friend.getPlayer().getName() + ": " + (flee ? 
            "Flee" : "Concede") + " with Legion "  + friend.getMarkerId() 
            + "?", true);

        setResizable(false);
        setLayout(null);

        this.parentFrame = parentFrame;
        this.friend = friend;
        this.enemy = enemy;
        this.flee = flee;

        setSize(getPreferredSize());

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
            - getSize().height / 2));

        // Leave space for angels.
        friendChits = new Chit[7];
        for (int i = 0; i < friend.getHeight(); i++)
        {
            friendChits[i] = new Chit((i + 1) * scale + (scale / 5), scale / 2,
                scale, friend.getCreature(i).getImageName(), this, false);
        }

        // Leave space for angels.
        enemyChits = new Chit[7];
        for (int i = 0; i < enemy.getHeight(); i++)
        {
            enemyChits[i] = new Chit((i + 1) * scale + (scale / 5), 
                2 * scale, scale, enemy.getCreature(i).getImageName(), this,
                false);
        }

        friendMarker = new Chit(scale / 5, scale / 2, scale, 
            friend.getImageName(), this, false);
        
        enemyMarker = new Chit(scale / 5, 2 * scale, scale, 
            enemy.getImageName(), this, false);

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
            new MessageBox(parentFrame, "waitForAll was interrupted");
        }

        button1 = new Button(flee ? "Flee" : "Concede");
        button2 = new Button(flee ? "Don't Flee" : "Don't Concede");
        add(button1);
        add(button2);
        button1.addActionListener(this);
        button2.addActionListener(this);

        pack();

        imagesLoaded = true;
        setVisible(true);
        repaint();
    }


    public void paint(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        friendMarker.paint(g);

        enemyMarker.paint(g);

        // ArrayIndexOutOfBoundsException when an angel is acquired?
        for (int i = friend.getHeight() - 1; i >= 0; i--)
        {
            // If the stack just acquired an angel, make a chit for it.
            if (friendChits[i] == null)
            {
                friendChits[i] = new Chit((i + 1) * scale + (scale / 5), 
                    scale / 2, scale, friend.getCreature(i).getImageName(),
                    this, false);
            }
            friendChits[i].paint(g);
        }
        for (int i = enemy.getHeight() - 1; i >= 0; i--)
        {
            if (enemyChits[i] == null)
            {
                enemyChits[i] = new Chit((i + 1) * scale + (scale / 5), 
                    2 * scale, scale, enemy.getCreature(i).getImageName(),
                    this, false);
            }

            enemyChits[i].paint(g);
        }

        if (!laidOut)
        {
            Insets insets = getInsets();
            Dimension d = getSize();
            button1.setBounds(insets.left + d.width / 9, 7 * d.height / 8 - 
                insets.bottom, d.width / 3, d.height / 8);
            button2.setBounds(5 * d.width / 9 - insets.right, 
                7 * d.height / 8 - insets.bottom, d.width / 3, d.height / 8);
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
                friend.getPlayer().die(enemy.getPlayer());
            }

            // Exit this dialog.
            dispose();

            // Unselect and repaint the hex.
            MasterHex hex = enemy.getCurrentHex();
            hex.unselect();
            hex.repaint();
        }

        else
        {
            dispose();
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
