package net.sf.colossus.gui;


/**
 * Class MovementDie displays dice rolls during a battle.
 *
 * @author David Ripton
 */
final class MovementDie extends Chit
{
    private final int lastRoll = 0;

    MovementDie(int scale, String id)
    {
        super(scale, id);
    }

    static String getDieImageName(int roll)
    {
        StringBuilder basename = new StringBuilder("Hit");
        basename.append(roll);

        return basename.toString();
    }

    int getLastRoll()
    {
        return lastRoll;
    }
}
