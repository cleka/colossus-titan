/**
 * Class PenaltyOption holds the information needed to decide whether to take 
 * a strike penalty, for one chit.
 * @version $Id$
 * @author David Ripton
 */

public class PenaltyOption
{
    private Critter critter;
    private int dice;
    private int strikeNumber;


    public PenaltyOption(Critter critter, int dice, int strikeNumber)
    {
        this.critter = critter;
        this.dice = dice;
        this.strikeNumber = strikeNumber;
    }


    public Critter getCritter()
    {
        return critter;
    }
    
    
    public int getDice()
    {
        return dice;
    }
    
    
    public int getStrikeNumber()
    {
        return strikeNumber;
    }


    // Sort first by ascending dice, then by descending strike number.
    public int compareTo(PenaltyOption other)
    {
        if (dice < other.getDice())
        {
            return -1;
        }
        else if (dice > other.getDice())
        {
            return 1;
        }
        else if (strikeNumber > other.getStrikeNumber())
        {
            return -1;
        }
        else if (strikeNumber < other.getStrikeNumber())
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }


    // Sort an array of penalty options by number of dice (ascending), then by
    //    strike number (descending).
    public static void sort(PenaltyOption [] penaltyOptions, int 
        numPenaltyOptions)
    {
        for (int i = 0; i < numPenaltyOptions - 1; i++)
        {
            for (int j = i + 1; j < numPenaltyOptions; j++)
            {
                if (penaltyOptions[i].compareTo(penaltyOptions[j]) > 0)
                {
                    PenaltyOption temp = penaltyOptions[i];
                    penaltyOptions[i] = penaltyOptions[j];
                    penaltyOptions[j] = temp;
                }
            }
        }
    }
}
