import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class BattleDice displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 */


public final class BattleDice extends JFrame implements WindowListener
{
    private Game game;
    private static final int scale = 60;
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
    private JLabel label1 = new JLabel();
    private JLabel label2 = new JLabel();
    private JLabel label3 = new JLabel();
    private Chit [] dice;


    public BattleDice(Game game)
    {
        super("Battle Dice Rolls");

        this.game = game;
        setVisible(false);
        addWindowListener(this);
        setupIcon();
        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);
        pack();

        setBackground(Color.lightGray);
        setResizable(false);

        // Move dialog to saved location, or middle right of screen.
        if (location == null)
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            location = new Point(d.width - getSize().width,
                (d.height - getSize().height) / 2);
        }
        setLocation(location);
    }


    public void setValues(Critter attacker, Critter defender,
        int targetNumber, int [] rolls, int hits, int carries)
    {
        this.attacker = attacker;
        this.defender = defender;
        this.targetNumber = targetNumber;
        this.rolls = rolls;
        numDice = rolls.length;
        this.hits = hits;
        this.carries = carries;
    }


    public void setCarries(int carries)
    {
        this.carries = carries;
    }


    private String getDieImageName(int roll)
    {
        StringBuffer basename = new StringBuffer();
        if (roll >= targetNumber)
        {
            basename.append("Hit");
        }
        else
        {
            basename.append("Miss");
        }
        basename.append(roll);

        return basename.toString();
    }


    // XXX Cache die images?
    /** Initialize and layout the components, in response to new data. */
    public void showRoll()
    {
        // Top row: label like "Serpent in Plains attacks Archangel in brush"
        // Second row: label like "Rolling 18 dice with target number 6"
        // Rows 3-N: Dice, maximum 12 per row
        // Last row: label like "4 hits, 0 possible carries"

        setVisible(false);
        Container contentPane = getContentPane();
        contentPane.removeAll();

        label1.setText(attacker.getName() + " in " +
            attacker.getCurrentHex().getDescription() + " attacks " +
            defender.getName() + " in " +
            defender.getCurrentHex().getDescription());
        label1.setAlignmentX(Label.LEFT_ALIGNMENT);
        constraints.gridy = 0;
        constraints.gridwidth = 6;
        constraints.ipadx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insets;
        gridbag.setConstraints(label1, constraints);
        contentPane.add(label1);

        label2.setText("Rolling " + numDice + " dice with target number " +
            targetNumber);
        label3.setAlignmentX(Label.LEFT_ALIGNMENT);
        constraints.gridy = 1;
        constraints.gridwidth = 6;
        constraints.ipadx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insets;
        gridbag.setConstraints(label2, constraints);
        contentPane.add(label2);

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
            contentPane.add(dice[i]);
        }

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
        label3.setAlignmentX(Label.LEFT_ALIGNMENT);
        constraints.gridy = 3 + numDice / 6;
        constraints.gridwidth = 6;
        constraints.ipadx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insets;
        gridbag.setConstraints(label3, constraints);
        contentPane.add(label3);

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

        repaint();
    }


    private void setupIcon()
    {
        if (game != null && !game.isApplet())
        {
            try
            {
                setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource(Chit.getImagePath(
                    Creature.colossus.getImageName()))));
            }
            catch (NullPointerException e)
            {
                Game.logError(e.toString() + " Couldn't find " +
                    Creature.colossus.getImageName());
                dispose();
            }
        }
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        if (game != null)
        {
            game.setOption(Options.showDice, false);
        }
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


    /** Never grab the keyboard focus. */
    public boolean isFocusTraversable()
    {
        return false;
    }


    public static void main(String [] args)
    {
        BattleDice battleDice = new BattleDice(null);

        Player player1 = new Player("Attacker", null);
        player1.setScore(1400);
        Legion attacker = new Legion("Rd01", null, null, null,
            Creature.titan, Creature.colossus, Creature.serpent,
            Creature.archangel, Creature.hydra, Creature.giant,
            Creature.dragon, null, player1);

        Player player2 = new Player("Defender", null);
        player2.setScore(1000);
        Legion defender = new Legion("Bk01", null, null, null,
            Creature.titan, Creature.colossus, Creature.serpent,
            Creature.angel, Creature.hydra, Creature.giant,
            Creature.dragon, null, player2);

        Critter serpent = attacker.getCritter(Creature.serpent);
        BattleHex hex1 = new BattleHex(1, 1, 60, null, 1, 1);
        serpent.addBattleInfo(hex1, hex1, null, null, null);

        Critter angel = defender.getCritter(Creature.angel);
        BattleHex hex2 = new BattleHex(2, 2, 60, null, 2, 2);
        angel.addBattleInfo(hex2, hex2, null, null, null);

        int targetNumber = 6;
        int [] rolls = {1,2,3,4,5,6,1,2,3,4,5,6,1,2,3,4,5,6};
        int hits = 3;
        int carries = 0;

        battleDice.setValues(serpent, angel, targetNumber, rolls, hits,
            carries);
        battleDice.showRoll();
    }
}
