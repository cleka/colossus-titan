import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class MovementDie displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 */


public final class MovementDie extends Chit
{
    private int lastRoll = 0;


    public MovementDie(int scale, String id, Container container)
    {
        super(scale, id, container);
    }


    public static String getDieImageName(int roll)
    {
        StringBuffer basename = new StringBuffer("Hit");
        basename.append(roll);

        return basename.toString();
    }


    public int getLastRoll()
    {
        return lastRoll;
    }
}
