package net.sf.colossus.client;


import java.awt.Container;


/**
 * Class MovementDie displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 */


final class MovementDie extends Chit
{
    private int lastRoll = 0;

    MovementDie(int scale, String id, Container container)
    {
        super(scale, id, container);
    }

    static String getDieImageName(int roll)
    {
        StringBuffer basename = new StringBuffer("Hit");
        basename.append(roll);

        return basename.toString();
    }

    int getLastRoll()
    {
        return lastRoll;
    }
}
