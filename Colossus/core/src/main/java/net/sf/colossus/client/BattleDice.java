package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import net.sf.colossus.util.HTMLColor;


/**
 * Class BattleDice displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

final class BattleDice extends Box
{
    private String strikerDesc = "";
    private String targetDesc = "";
    private int numDice = 0;
    private int targetNumber = 0;
    private final List<String> rolls = new ArrayList<String>();
    private Chit[] dice;
    private int averageMiss = -1;

    private final JPanel diceBox, missBox, hitBox;
    private final TitledBorder diceBoxTitledBorder;

    BattleDice()
    {
        super(BoxLayout.Y_AXIS);
        setVisible(false);
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
        List<String> rolls)
    {
        this.strikerDesc = strikerDesc;
        this.targetDesc = targetDesc;
        this.targetNumber = targetNumber;

        this.rolls.clear();
        if (rolls != null)
        {
            this.rolls.addAll(rolls);
            Collections.sort(this.rolls);
        }
        numDice = this.rolls.size();

        // for average miss number, let's assume 6-sided roll :-)
        averageMiss = Math.round((numDice * (targetNumber - 1) / 6.0f));
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
            diceBoxTitledBorder.setTitle(strikerDesc + " attacks "
                + targetDesc + " (target number is " + targetNumber + ")");
        }

        if (numDice > 0)
        {
            dice = new Chit[numDice];

            for (int i = 0; i < numDice; i++)
            {
                String imageName = getDieImageName(rolls.get(i));
                if (imageName != null)
                {
                    dice[i] = new Chit(2 * Scale.get(), imageName);
                    if (averageMiss > i)
                    {
                        missBox.add(dice[i]);
                    }
                    else
                    {
                        hitBox.add(dice[i]);
                    }
                }
            }

            if (averageMiss < numDice)
            {
                hitBox.setVisible(true);
            }
            if (averageMiss > 0)
            {
                missBox.setVisible(true);
            }
        }
        else
        {
            dice = null;
        }

        invalidate();
        setVisible(true);
        repaint();
    }

    void rescale()
    {
        showRoll();
    }

    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize()
    {
        int scale = Scale.get();
        return new Dimension(60 * scale, 6 * scale);
    }
}
