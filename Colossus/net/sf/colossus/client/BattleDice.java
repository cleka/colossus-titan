package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Class BattleDice displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 */


final class BattleDice extends JPanel
{
    private String attackerName = "";
    private String defenderName = "";
    private String attackerHexId = "";
    private String defenderHexId = "";
    private int numDice = 0;
    private int targetNumber = 0;
    private int [] rolls = new int[0];
    private int hits = 0;
    private int carries = 0;
    private char terrain = '?';
    private JLabel label1 = new JLabel();
    private JLabel label2 = new JLabel();
    private Chit [] dice;


    BattleDice()
    {
        setVisible(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.lightGray);
        showRoll();
    }


    void setValues(String attackerName, String defenderName,
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

    void setCarries(int carries)
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
    void showRoll()
    {
        setVisible(false);
        removeAll();

        if (attackerName.equals(""))
        {
            label1.setText("");
        }
        else
        {
            label1.setText(attackerName + " in " +
                HexMap.getHexByLabel(terrain, attackerHexId).getDescription() +
                " attacks " + defenderName + " in " +
                HexMap.getHexByLabel(terrain, defenderHexId).getDescription());
        }
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
        if (attackerName.equals(""))
        {
            label2.setText("");
        }
        else
        {
            label2.setText(carries + carryString);
        }
        label2.setAlignmentX(Label.CENTER_ALIGNMENT);
        add(label2);

        setVisible(true);
        repaint();
    }

    void rescale()
    {
        showRoll();
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        int scale = Scale.get();
        return new Dimension(60 * scale, 6 * scale);
    }
}
