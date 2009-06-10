package net.sf.colossus.guiutil;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JFrame;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.util.InstanceTracker;


/**
 * KFrame adds some generally useful functions to JFrame.
 *
 * TODO SaveWindow handling should be on this level
 *
 * @author Clemens Katzer
 */
public class KFrame extends JFrame
{
    private SaveWindow kSaveWindow;

    /** Only support the simple constructor forms of JFrame. */

    public KFrame()
    {
        super();
        InstanceTracker.register(this, "<no title>");
    }

    public KFrame(String title)
    {
        super(title);
        InstanceTracker.register(this, title);
    }

    /**
     * If, and only if, the extending class calls this useSaveWindow,
     * then the KFrame will handle the SaveWindow work:
     * creating it when useSaveWindow is called, and saving back
     * always when setVisible(false) is called (and useSaveWindow was
     * called before, of course).
     *
     * TODO maybe we should enforce this by calling it through the
     *      constructor
     *
     * @param options IOptions reference to the client for saving window
     *        size+pos in the Options data
     * @param windowName name/title of the window,
     *        window size+pos are stored for that name
     * @param defaultLocation to be used if no location was earlier stored:
     *        place there; give null to center on screen.
     */
    public void useSaveWindow(IOptions options, String windowName,
        Point defaultLocation)
    {
        kSaveWindow = new SaveWindow(options, windowName);
        if (defaultLocation == null)
        {
            kSaveWindow.restoreOrCenter(this);
        }
        else
        {
            kSaveWindow.restore(this, defaultLocation);
        }
    }

    @Override
    public void setVisible(boolean val)
    {
        if (!val && kSaveWindow != null)
        {
            kSaveWindow.save(this);
        }
        super.setVisible(val);
    }

    @Override
    public void dispose()
    {
        if (kSaveWindow != null)
        {
            kSaveWindow.save(this);
        }
        super.dispose();
        kSaveWindow = null;
    }

    /** Center this dialog on the screen.  Must be called after the dialog
     *  size has been set. */
    public void centerOnScreen()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
            - getSize().height / 2));
    }
}
