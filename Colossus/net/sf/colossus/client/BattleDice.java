package net.sf.colossus.client;


import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import net.sf.colossus.util.HTMLColor;

/**
 * Class BattleDice displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */


final class BattleDice extends JPanel
{
    private String strikerDesc = "";
    private String targetDesc = "";
    private int numDice = 0;
    private int targetNumber = 0;
    private java.util.List rolls = new ArrayList();
    private int hits = 0;
    private JLabel label1 = new JLabel();
    private Chit [] dice;
    private int averageMiss = -1;

    private JPanel diceBox, missBox, hitBox;
    private TitledBorder diceBoxTitledBorder;

    BattleDice()
    {
        setVisible(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.lightGray);

        diceBox = new JPanel();
        diceBox.setLayout(new FlowLayout());
        diceBoxTitledBorder = new TitledBorder("");
        diceBoxTitledBorder.setTitleJustification(TitledBorder.CENTER);
        diceBox.setBorder(diceBoxTitledBorder);
        add(diceBox);
        
        missBox = new JPanel();
        hitBox = new JPanel();
        missBox.setLayout(new FlowLayout());
        missBox.setBorder(new LineBorder(HTMLColor.blue));
        hitBox.setBorder(new LineBorder(HTMLColor.red));
        hitBox.setLayout(new FlowLayout());
        diceBox.add(missBox);
        diceBox.add(hitBox);

        showRoll();
    }


    void setValues(String strikerDesc, String targetDesc, int targetNumber, 
        int hits, java.util.List rolls)
    {
        this.strikerDesc = strikerDesc;
        this.targetDesc = targetDesc;
        this.targetNumber = targetNumber;
        this.hits = hits;

        this.rolls.clear();
        if (rolls != null)
        {
            this.rolls.addAll(rolls);
            Collections.sort(this.rolls);
        }
        numDice = this.rolls.size();

        // for average miss number, let's assume 6-sided roll :-)
        float floatAM = (numDice * (targetNumber - 1)) / 6;

        averageMiss = Math.round(floatAM);
    }

    private String getDieImageName(String rollString)
    {
        int roll;
        try
        {
            roll = Integer.parseInt(rollString);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
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
        hitBox.removeAll();
        hitBox.setVisible(false);
        missBox.removeAll();
        missBox.setVisible(false);

        if (strikerDesc.equals(""))
        {
            diceBoxTitledBorder.setTitle("Attack results");
        }
        else
        {
            diceBoxTitledBorder.setTitle(strikerDesc +
                                         " attacks " +
                                         targetDesc +
                                         " (target number is " +
                                         targetNumber + ")");
        }

        dice = new Chit[numDice];

        for (int i = 0; i < numDice; i++)
        {
            String imageName = getDieImageName((String)rolls.get(i));
            if (imageName != null)
            {
                dice[i] = new Chit(2 * Scale.get(), imageName, this);
                if (averageMiss > i)
                    missBox.add(dice[i]);
                else
                    hitBox.add(dice[i]);
            }
        }

        if (numDice > 0)
        {
            hitBox.setVisible(true);
            missBox.setVisible(true);
        }

        invalidate();
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
