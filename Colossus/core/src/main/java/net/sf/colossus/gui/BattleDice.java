package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import net.sf.colossus.util.HTMLColor;


/**
 * Class BattleDice displays dice rolls during a battle.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */
final class BattleDice extends Box
{
    private static class DiceEntry
    {
        String battlePhaseDesc = "";
        String attackerDesc = "";
        String strikerDesc = "";
        String targetDesc = "";
        int numDice = 0;
        int targetNumber = 0;
        final List<String> rolls = new ArrayList<String>();
        int averageMiss = -1;
    }

    private Chit[] dice;

    private final JPanel diceBox, missBox, hitBox;
    private final JLabel attackerText;
    private final TitledBorder diceBoxTitledBorder;

    private final List<DiceEntry> diceResults = new ArrayList<DiceEntry>();
    private int currentEntry = 0;

    BattleDice()
    {
        super(BoxLayout.Y_AXIS);
        setVisible(false);
        setBackground(Color.lightGray);

        attackerText = new JLabel();
        attackerText.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(attackerText);

        diceBox = new JPanel();
        diceBox.setLayout(new FlowLayout());
        diceBoxTitledBorder = new TitledBorder("");
        diceBoxTitledBorder.setTitleJustification(TitledBorder.CENTER);
        diceBox.setBorder(diceBoxTitledBorder);

        missBox = new JPanel();
        hitBox = new JPanel();
        missBox.setLayout(new FlowLayout());
        missBox.setBorder(new LineBorder(HTMLColor.blue));
        hitBox.setBorder(new LineBorder(HTMLColor.red));
        hitBox.setLayout(new FlowLayout());
        diceBox.add(missBox);
        diceBox.add(hitBox);

        add(diceBox);

        diceResults.add(new DiceEntry());

        setVisible(true);
        showLastRoll();
    }

    void addValues(String battlePhaseDesc, String attackerDesc,
        String strikerDesc, String targetDesc, int targetNumber,
        List<String> rolls)
    {
        DiceEntry entry = new DiceEntry();
        entry.battlePhaseDesc = battlePhaseDesc;
        entry.attackerDesc = attackerDesc;
        entry.strikerDesc = strikerDesc;
        entry.targetDesc = targetDesc;
        entry.targetNumber = targetNumber;

        entry.rolls.addAll(rolls);
        Collections.sort(entry.rolls);

        entry.numDice = entry.rolls.size();

        // for average miss number, let's assume 6-sided roll :-)
        entry.averageMiss = Math
            .round((entry.numDice * (targetNumber - 1) / 6.0f));
        diceResults.add(entry);
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
        StringBuilder basename = new StringBuilder();
        if (roll >= targetNumber())
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
    void showLastRoll()
    {
        currentEntry = diceResults.size() - 1;
        showCurrentRoll();
    }

    void showCurrentRoll()
    {
        attackerText.setText(attackerDesc() + " " + battlePhaseDesc());

        diceBox.setVisible(false);

        hitBox.removeAll();
        hitBox.setVisible(false);
        missBox.removeAll();
        missBox.setVisible(false);

        if (strikerDesc().equals(""))
        {
            diceBoxTitledBorder.setTitle("Attack results");
        }
        else
        {
            diceBoxTitledBorder.setTitle("(" + currentEntry + " of "
                + (diceResults.size() - 1) + ") " + strikerDesc()
                + " attacks " + targetDesc() + " (target number is "
                + targetNumber() + ")");
        }

        if (numDice() > 0)
        {
            dice = new Chit[numDice()];

            for (int i = 0; i < numDice(); i++)
            {
                String imageName = getDieImageName(rolls().get(i));
                if (imageName != null)
                {
                    dice[i] = Chit.newDiceChit(2 * Scale.get(), imageName);
                    if (averageMiss() > i)
                    {
                        missBox.add(dice[i]);
                    }
                    else
                    {
                        hitBox.add(dice[i]);
                    }
                }
            }

            if (averageMiss() < numDice())
            {
                hitBox.setVisible(true);
            }
            if (averageMiss() > 0)
            {
                missBox.setVisible(true);
            }
        }
        else
        {
            dice = null;
        }

        diceBox.invalidate();
        diceBox.setVisible(true);
    }

    private int averageMiss()
    {
        return getCurrentResults().averageMiss;
    }

    private List<String> rolls()
    {
        return getCurrentResults().rolls;
    }

    private int numDice()
    {
        return getCurrentResults().numDice;
    }

    private int targetNumber()
    {
        return getCurrentResults().targetNumber;
    }

    private DiceEntry getCurrentResults()
    {
        assert diceResults.size() > 0;
        return diceResults.get(currentEntry);
    }

    private String targetDesc()
    {
        return getCurrentResults().targetDesc;
    }

    private String attackerDesc()
    {
        return getCurrentResults().attackerDesc;
    }

    private String battlePhaseDesc()
    {
        return getCurrentResults().battlePhaseDesc;
    }

    private String strikerDesc()
    {
        return getCurrentResults().strikerDesc;
    }

    void rescale()
    {
        showLastRoll();
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

    /**
     * Use to set the current roll in the history.  Updates the control.
     *
     * @param pValue
     */
    public void setCurrentRoll(int pValue)
    {
        if (pValue != currentEntry && pValue < diceResults.size())
        {
            currentEntry = pValue;
            showCurrentRoll();
        }
    }

    /**
     * @return number of dice rolls stored in history
     */
    public int getHistoryLength()
    {
        return diceResults.size();
    }
}
