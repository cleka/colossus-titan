package net.sf.colossus.client;


/**
 * Class StrikeDie displays a die representing the threshold for 
 * a successful hit during the Strike/strike-back phase.
 * --Cloned from David Ripton's MovementDie.--
 * @version $Id: StrikeDie.java 2575 2007-11-05 21:50:28Z Dranathi $
 * @author Dranathi
 */

final class StrikeDie extends Chit
{
    private int lastRoll = 0;

    StrikeDie(int scale, int roll, String type)
    {
        this(scale, roll, type, null);
    }

    StrikeDie(int scale, int roll, String type, String[] overlays)
    {
        super(scale, getDieImageName(type, roll), overlays);
        lastRoll = roll;
        setOpaque(false);
    }

    static String getDieImageName(String type, int roll)
    {
        StringBuffer basename = new StringBuffer(type);
        basename.append(roll);
        return basename.toString();
    }

    int getLastRoll()
    {
        return lastRoll;
    }
}
