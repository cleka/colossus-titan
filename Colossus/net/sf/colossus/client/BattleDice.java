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
    private String strikerDesc = "";
    private String targetDesc = "";
    private int numDice = 0;
    private int targetNumber = 0;
    private int [] rolls = new int[0];
    private int hits = 0;
    private JLabel label1 = new JLabel();
    private Chit [] dice;


    BattleDice()
    {
        setVisible(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.lightGray);
        showRoll();
    }


    void setValues(String strikerDesc, String targetDesc, int targetNumber, 
        int hits, int [] rolls)
    {
        this.strikerDesc = strikerDesc;
        this.targetDesc = targetDesc;
        this.targetNumber = targetNumber;
        this.hits = hits;
        this.rolls = rolls;
        if (rolls != null)
            numDice = rolls.length;
        else
            numDice = 0;
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

        if (strikerDesc.equals(""))
        {
            label1.setText("");
        }
        else
        {
            label1.setText(strikerDesc + " attacks " + targetDesc); 
        }
        label1.setAlignmentX(Label.CENTER_ALIGNMENT);
        add(label1);

        JPanel diceBox = new JPanel();
        diceBox.setLayout(new FlowLayout());
        add(diceBox);
        dice = new Chit[numDice];
        for (int i = 0; i < numDice; i++)
        {
            dice[i] = new Chit(2 * Scale.get(), getDieImageName(rolls[i]),
                this);
            diceBox.add(dice[i]);
        }

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
