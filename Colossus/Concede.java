import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class Concede allows a player to flee or concede before starting a Battle.
 * @version $Id$
 * @author David Ripton
 */

public class Concede extends JDialog implements ActionListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private static final int scale = 60;
    private JFrame parentFrame;
    private boolean flee;
    private Legion ally;
    private Legion enemy;
    private Chit [] allyChits;
    private Chit [] enemyChits;
    private Chit allyMarker;
    private Chit enemyMarker;
    private static Point location;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();


    public Concede(JFrame parentFrame, Legion ally, Legion enemy, boolean flee)
    {
        super(parentFrame, ally.getPlayer().getName() + ": " + (flee ?
            "Flee" : "Concede") + " with Legion "  + ally.getMarkerId() +
            " in " + ally.getCurrentHex().getDescription() + "?", true);

        Container contentPane = getContentPane();

        contentPane.setLayout(gridbag);

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
        contentPane.add(allyMarker);

        // Leave space for angels.
        allyChits = new Chit[7];
        for (int i = 0; i < ally.getHeight(); i++)
        {
            allyChits[i] = new Chit(scale, ally.getCritter(i).getImageName(),
                this);
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(allyChits[i], constraints);
            contentPane.add(allyChits[i]);
        }

        enemyMarker = new Marker(scale, enemy.getImageName(), this, enemy);
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(enemyMarker, constraints);
        contentPane.add(enemyMarker);

        // Leave space for angels.
        enemyChits = new Chit[7];
        for (int i = 0; i < enemy.getHeight(); i++)
        {
            enemyChits[i] = new Chit(scale, enemy.getCritter(i).getImageName(),
                this);
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(enemyChits[i], constraints);
            contentPane.add(enemyChits[i]);
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
            JOptionPane.showMessageDialog(parentFrame, e.toString() +
                " waitForAll was interrupted");
        }
        imagesLoaded = true;

        JButton button1 = new JButton(flee ? "Flee" : "Concede");
        JButton button2 = new JButton(flee ? "Don't Flee" : "Don't Concede");

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
        contentPane.add(button1);
        button1.addActionListener(this);
        constraints.gridx = leadSpace + constraints.gridwidth; 
        gridbag.setConstraints(button2, constraints);
        contentPane.add(button2);
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


    private void cleanup()
    {
        location = getLocation();
        dispose();
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
            MasterBoard.unselectHexByLabel(hex.getLabel());
        }

        else
        {
            cleanup();
        }
    }
}
