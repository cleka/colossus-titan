import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class BattleDice displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 */


public final class BattleDice extends JPanel
{
    private Insets insets = new Insets(5, 5, 5, 5);
    private static Point location;
    private String attackerName;
    private String defenderName;
    private String attackerHexId;
    private String defenderHexId;
    private int numDice;
    private int targetNumber;
    private int [] rolls;
    private int hits;
    private int carries;
    private char terrain;
    private JLabel label1 = new JLabel();
    private JLabel label2 = new JLabel();
    private JLabel label3 = new JLabel();
    private Chit [] dice;


    public BattleDice()
    {
        setVisible(false);
        setLayout(new FlowLayout());

        setBackground(Color.lightGray);

        // Move dialog to saved location, or middle right of screen.
        if (location == null)
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            location = new Point(d.width - getSize().width,
                (d.height - getSize().height) / 2);
        }
        setLocation(location);
    }


    public void setValues(String attackerName, String defenderName,
        String attackerHexId, String defenderHexId, char terrain,
        int targetNumber, int hits, int carries, int [] rolls)
    {
        this.attackerName = attackerName;
        this.defenderName = defenderName;
        this.attackerHexId = attackerHexId;
        this.defenderHexId = defenderHexId;
        this.terrain = terrain;
        this.targetNumber = targetNumber;
        this.hits = hits;
        this.carries = carries;
        this.rolls = rolls;
        numDice = rolls.length;
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


    /** Initialize and layout the components, in response to new data. */
    public void showRoll()
    {
        // Top row: label like "Serpent in Plains attacks Archangel in brush"
        // Second row: label like "Rolling 18 dice with target number 6"
        // Rows 3-N: Dice, maximum 12 per row
        // Last row: label like "4 hits, 0 possible carries"

        setVisible(false);
        removeAll();

        label1.setText(attackerName + " in " +
            HexMap.getHexByLabel(terrain, attackerHexId).getDescription() +
            " attacks " + defenderName + " in " +
            HexMap.getHexByLabel(terrain, defenderHexId).getDescription());
        label1.setAlignmentX(Label.LEFT_ALIGNMENT);
        add(label1);

        label2.setText("Rolling " + numDice + " dice with target number " +
            targetNumber);
        label3.setAlignmentX(Label.LEFT_ALIGNMENT);
        add(label2);

        dice = new Chit[numDice];
        for (int i = 0; i < numDice; i++)
        {
            dice[i] = new Chit(2 * Scale.get(), getDieImageName(rolls[i]),
                this);
            add(dice[i]);
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
        add(label3);

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

    public void rescale()
    {
        showRoll();
    }
}
