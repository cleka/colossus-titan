import java.awt.*;
import java.awt.event.*;

/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 * @version $Id$
 * @author David Ripton
 */

public class Concede extends Dialog implements ActionListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private static final int scale = 60;
    private Frame parentFrame;
    private boolean flee;
    private Legion ally;
    private Legion enemy;
    private Chit [] allyChits;
    private Chit [] enemyChits;
    private Chit allyMarker;
    private Chit enemyMarker;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private static Point location;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();


    public Concede(Frame parentFrame, Legion ally, Legion enemy, boolean flee)
    {
        super(parentFrame, ally.getPlayer().getName() + ": " + (flee ?
            "Flee" : "Concede") + " with Legion "  + ally.getMarkerId() +
            " in " + ally.getCurrentHex().getDescription() + "?", true);

        setLayout(gridbag);

        this.parentFrame = parentFrame;
        this.ally = ally;
        this.enemy = enemy;
        this.flee = flee;

        pack();

        setBackground(Color.lightGray);
        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        allyMarker = new Marker(scale, ally.getImageName(), this, ally);
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(allyMarker, constraints);
        add(allyMarker);

        // Leave space for angels.
        allyChits = new Chit[7];
        for (int i = 0; i < ally.getHeight(); i++)
        {
            allyChits[i] = new Chit(scale, ally.getCritter(i).getImageName(),
                this);
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(allyChits[i], constraints);
            add(allyChits[i]);
        }

        enemyMarker = new Marker(scale, enemy.getImageName(), this, enemy);
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(enemyMarker, constraints);
        add(enemyMarker);

        // Leave space for angels.
        enemyChits = new Chit[7];
        for (int i = 0; i < enemy.getHeight(); i++)
        {
            enemyChits[i] = new Chit(scale, enemy.getCritter(i).getImageName(),
                this);
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(enemyChits[i], constraints);
            add(enemyChits[i]);
        }

        tracker = new MediaTracker(this);

        for (int i = 0; i < ally.getHeight(); i++)
        {
            tracker.addImage(allyChits[i].getImage(), 0);
        }
        for (int i = 0; i < enemy.getHeight(); i++)
        {
            tracker.addImage(enemyChits[i].getImage(), 0);
        }
        tracker.addImage(allyMarker.getImage(), 0);
        tracker.addImage(enemyMarker.getImage(), 0);

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(parentFrame, e.toString() +
                " waitForAll was interrupted");
        }
        imagesLoaded = true;

        Button button1 = new Button(flee ? "Flee" : "Concede");
        Button button2 = new Button(flee ? "Don't Flee" : "Don't Concede");

        // Attempt to center the buttons.
        int chitWidth = Math.max(ally.getHeight(), enemy.getHeight()) + 1;
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

        constraints.gridy = 2;
        constraints.gridx = leadSpace;
        gridbag.setConstraints(button1, constraints);
        add(button1);
        button1.addActionListener(this);
        constraints.gridx = leadSpace + constraints.gridwidth; 
        gridbag.setConstraints(button2, constraints);
        add(button2);
        button2.addActionListener(this);

        pack();
        
        // Initially, center the dialog on the screen.  Save the
        // location for future invocations.
        if (location == null)
        {
            location = new Point(d.width / 2 - getSize().width / 2, 
                d.height / 2 - getSize().height / 2);
        }
        setLocation(location);

        setVisible(true);
        repaint();
    }


    public static void saveLocation(Point point)
    {
        location = point;
    }


    public static Point returnLocation()
    {
        return location;
    }


    public void cleanup()
    {
        location = getLocation();
        dispose();
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
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Flee") || 
            e.getActionCommand().equals("Concede"))
        {
            // Figure how many points the victor receives.
            int points = ally.getPointValue();
            if (flee)
            {
                points /= 2;
                Game.logEvent("Legion " + ally.getMarkerId() + 
                    " flees from legion " + enemy.getMarkerId());
            }
            else
            {
                Game.logEvent("Legion " + ally.getMarkerId() + 
                    " concedes to legion " + enemy.getMarkerId());
            }

            // Remove the dead legion.
            ally.removeLegion();

            // Add points, and angels if necessary.
            enemy.addPoints(points);
            // Remove any fractional points.
            enemy.getPlayer().truncScore();

            // If this was the titan stack, its owner dies and gives half
            // points to the victor.
            if (ally.numCreature(Creature.titan) == 1)
            {
                ally.getPlayer().die(enemy.getPlayer(), true);
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
}
