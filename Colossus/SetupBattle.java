import java.awt.*;
import java.awt.event.*;

/** 
 *  Class SetupBattle is a dialog to set up a standalone battle.
 *  @version $Id$
 *  @author David Ripton
 */

// Terrain type
// Attacking and defending creatures
// Both players' scores (used for Titan power)
// Whether angel and/or archangel is available to summon
// Whether each possible recruit for this terrain is available
// Entry side (call PickEntrySide)



public class SetupBattle extends Frame 
{
    private int chitScale = 60;



    public SetupBattle()
    {
        super("Set Up Battle");

        setLayout(null);
    }


    public static void main(String [] args)
    {
        new SetupBattle();
    }
}
