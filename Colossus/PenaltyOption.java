/**
 * Class PenaltyOption holds the information needed to decide whether to take 
 * a strike penalty, for one chit.
 * @version $Id$
 * @author David Ripton
 */

class PenaltyOption
{
    private Critter critter;
    private int dice;
    private int strikeNumber;


    PenaltyOption(Critter critter, int dice, int strikeNumber)
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
}
