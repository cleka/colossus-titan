import java.awt.*;
import java.awt.event.*;

/**
 * Class ShowDice displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 */


public class ShowDice extends Dialog implements WindowListener
{
    private Frame parentFrame;
    private MediaTracker tracker;
    private static final int scale = 60;
    private boolean imagesLoaded = false;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private Insets insets = new Insets(5, 5, 5, 5);
    private static Point location;
    private Critter attacker;
    private Critter defender;
    private int numDice;
    private int targetNumber;
    private int [] rolls;
    private int hits;
    private int carries;
    private Label label1 = new Label();
    private Label label2 = new Label();
    private Label label3 = new Label();
    private Chit [] dice;


    public ShowDice(Frame parentFrame)
    {
        super(parentFrame, "Show Dice Rolls", false);

        this.parentFrame = parentFrame;

        setVisible(false);
        setEnabled(false);

        addWindowListener(this);

        setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);
        setResizable(false);

        // Move dialog to saved location, or upper right corner of screen.
        if (location == null)
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            location = new Point(d.width - getSize().width, 0);
        }
        setLocation(location);

        repaint();
    }


    public void setAttacker(Critter attacker) 
    {
        this.attacker = attacker;
    }
    
    
    public void setDefender(Critter defender) 
    {
        this.defender = defender;
    }


    public void setTargetNumber(int targetNumber)
    {
        this.targetNumber = targetNumber;
    }
    
    
    public void setRolls(int [] rolls)
    {
        this.rolls = rolls;
        numDice = rolls.length;
    }
    
    
    public void setHits(int hits)
    {
        this.hits = hits;
    }
    
    
    public void setCarries(int carries)
    {
        this.carries = carries;
    }


    private String getDieImageName(int roll)
    {
        String basename;
        if (roll >= targetNumber)
        {
            basename = "Hit";
        }
        else
        {
            basename = "Miss";
        }

        return "images/" + basename + roll + ".gif";
    }


    // Initialize and layout the components, in response to new data.
    public void setup()
    {
        // Top row: label like "Serpent in Plains attacks Archangel in brush"
        // Second row: label like "Rolling 18 dice with target number 6"
        // Rows 3-N: Dice, maximum 12 per row
        // Last row: label like "4 hits, 0 possible carries"

        setVisible(false);
        setEnabled(false);
        removeAll();

        label1.setText(attacker.getName() + " in " + 
            attacker.getCurrentHex().getTerrainName() + " hex " +
            attacker.getCurrentHex().getLabel() + " attacks " + 
            defender.getName() + " in " + 
            defender.getCurrentHex().getTerrainName() + " hex " +
            defender.getCurrentHex().getLabel());
        label1.setAlignment(Label.LEFT);
        constraints.gridy = 0;
        constraints.gridwidth = 6;
        constraints.ipadx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insets;
        gridbag.setConstraints(label1, constraints);
        add(label1);

        label2.setText("Rolling " + numDice + " dice with target number " +
            targetNumber);
        label3.setAlignment(Label.LEFT);
        constraints.gridy = 1;
        constraints.gridwidth = 6;
        constraints.ipadx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insets;
        gridbag.setConstraints(label2, constraints);
        add(label2);

        tracker = new MediaTracker(this);
        dice = new Chit[numDice];
        for (int i = 0; i < numDice; i++)
        {
            dice[i] = new Chit(scale, getDieImageName(rolls[i]), this);
            constraints.gridy = 2 + (i / 6); 
            constraints.gridwidth = 1;
            constraints.ipadx = 5;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = insets;
            gridbag.setConstraints(dice[i], constraints);
            add(dice[i]);
            tracker.addImage(dice[i].getImage(), 0);
        }
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

        String hitString;
        if (hits == 1)
        {
            hitString = " hit, ";
        }
        else
        {
            hitString = " hits, ";
        }
        String carryString;
        if (carries == 1)
        {
            carryString = " possible carry";
        }
        else
        {
            carryString = " possible carries";
        }
        label3.setText(hits + hitString + carries + carryString);
        label3.setAlignment(Label.LEFT);
        constraints.gridy = 3 + numDice / 6;
        constraints.gridwidth = 6;
        constraints.ipadx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insets;
        gridbag.setConstraints(label3, constraints);
        add(label3);

        pack();

        // If the dialog is moving off the right edge of the screen,
        // move it left until it fits.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        location = getLocation();
        int adj = location.x + getSize().width - d.width;
        if (adj > 0)
        {
            location.x -= adj;
            setLocation(location);
        }
        setVisible(true);
        setEnabled(true);

        repaint();
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


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        // XXX Don't allow disposing this until there's a
        // menu that allows recalling it.
        // dispose();
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
}
