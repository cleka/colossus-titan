package net.sf.colossus.client;


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
    private Chit [] dice;


    public BattleDice()
    {
        setVisible(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setBackground(Color.lightGray);
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
        // Abort if called too early.
        if (attackerName == null)
        {
            return;
        }

        setVisible(false);
        removeAll();

        label1.setText(attackerName + " in " +
            HexMap.getHexByLabel(terrain, attackerHexId).getDescription() +
            " attacks " + defenderName + " in " +
            HexMap.getHexByLabel(terrain, defenderHexId).getDescription());
        label1.setAlignmentX(Label.CENTER_ALIGNMENT);
        add(label1);

        JPanel diceBox = new JPanel();
        diceBox.setLayout(new FlowLayout());
        add(diceBox);
        dice = new Chit[numDice];
        Dimension d = new Dimension(3, 0);
        for (int i = 0; i < numDice; i++)
        {
            dice[i] = new Chit(2 * Scale.get(), getDieImageName(rolls[i]),
                this);
            diceBox.add(dice[i]);
        }

        String carryString;
        if (carries == 1)
        {
            carryString = " carry";
        }
        else
        {
            carryString = " carries";
        }
        label2.setText(carries + carryString);
        label2.setAlignmentX(Label.CENTER_ALIGNMENT);
        add(label2);

        setVisible(true);

        repaint();
    }

    public void rescale()
    {
        showRoll();
    }
}
