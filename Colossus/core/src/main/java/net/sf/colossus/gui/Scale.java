package net.sf.colossus.gui;


import java.awt.Dimension;
import java.awt.Toolkit;


/**
 * Class Scale holds static information used to scale all GUI elements.
 *
 * @author David Ripton
 */
public final class Scale
{
    static int scale = 15;

    static
    {
        fitScreenRes();
    }

    // TODO neede to make public during GUI carveout
    public static int get()
    {
        return scale;
    }

    static void set(int scale)
    {
        Scale.scale = scale;
    }

    /** Set the scale so that the MasterBoard fits on the screen.
     *  Default scale should be 15 for screen resolutions with
     *  height 1000 or more.  For less, scale it down linearly. */
    static void fitScreenRes()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000)
        {
            scale = scale * d.height / 1000;
        }
    }
}
