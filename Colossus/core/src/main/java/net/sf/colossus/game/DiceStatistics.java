package net.sf.colossus.game;


import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.util.Glob;


@SuppressWarnings("boxing")
public class DiceStatistics
{
    private static final Logger LOGGER = Logger.getLogger(DiceStatistics.class
        .getName());

    private final List<DiceRollSet> rolls = new ArrayList<DiceRollSet>();

    private PrintStream statisticsOutputStream;

    public DiceStatistics(String statisticsFileName)
    {
        openStatisticsFile(statisticsFileName);
    }

    private void openStatisticsFile(String statisticsFileName)
    {
        this.statisticsOutputStream = null;
        if (statisticsFileName != null)
        {
            try
            {
                statisticsOutputStream = new PrintStream(statisticsFileName);
            }
            catch (IOException e)
            {
                LOGGER.log(Level.SEVERE,
                    "Couldn't open statics file for writing?"
                        + statisticsFileName, e);
            }
        }
    }

    private void writeToStatFile(String string)
    {
        if (statisticsOutputStream != null)
        {
            statisticsOutputStream.println(string);
        }
    }

    public void addOneSet(int turn, int battleTurn, Creature striker,
        Creature target, int strikeNumber, List<String> rollsString)
    {
        LOGGER.finer("Adding rollset for turn " + turn + " battleturn "
            + battleTurn + " Striker " + striker.getDescription());
        DiceRollSet newSet = new DiceRollSet(striker.getPlayer(), turn,
            battleTurn, striker, target, strikeNumber, rollsString);
        newSet.printRollSet();
        rolls.add(newSet);
    }

    public void printStatistics(Player p)
    {
        int dicePerNumber[] = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        int totalCount = 0;
        int totalSum = 0;

        if (rolls.size() == 0)
        {
            writeToStatFile("\nPlayer " + p.getName()
                + " has not done any battle rolls in this game.");
            return;
        }
        for (DiceRollSet set : rolls)
        {
            if (set.player.equals(p))
            {
                for (String oneRoll : set.rolls)
                {
                    int roll = Integer.parseInt(oneRoll);
                    dicePerNumber[roll - 1]++;
                    totalCount++;
                    totalSum += roll;
                }
            }
        }

        String average = String.format("%3.2f", (float)totalSum / totalCount);
        writeToStatFile("\nBattle roll statistics for player " + p.getName()
            + ", in total " + totalCount
            + " battle dice rolled; average of all rolls: " + average);
        for (int i = 0; i < 6; i++)
        {
            int count = dicePerNumber[i];
            String percent = String.format("%3.1f", 100 * (float)count
                / totalCount);
            int j = i + 1;
            writeToStatFile("Dice roll " + j + ": " + count + " times (= "
                + percent + "%)");
        }

    }

    private class DiceRollSet
    {
        private final Player player;

        private final int turn;

        private final int battleTurn;

        private final Creature striker;

        private final Creature target;

        private final int strikeNumber;

        private final List<String> rolls;

        DiceRollSet(Player player, int turn, int battleTurn, Creature striker,
            Creature target, int strikeNumber, List<String> rollsString)
        {
            this.player = player;
            this.turn = turn;
            this.battleTurn = battleTurn;
            this.striker = striker;
            this.target = target;
            this.strikeNumber = strikeNumber;
            this.rolls = new ArrayList<String>(rollsString);
        }

        public void printRollSet()
        {
            String rollsGlob = Glob.glob(", ", this.rolls);

            String result = String.format(
                "Turn %d, Player %s, BT %d - %s strikes %s with SN %d: %s",
                turn, this.player.getName(), battleTurn, striker.getName(),
                target.getName(), strikeNumber, rollsGlob);

            writeToStatFile(result);
        }

    }

}
