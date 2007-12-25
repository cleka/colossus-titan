package net.sf.colossus.util;


import javax.swing.JFrame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.awt.*;


/** KFrame adds some generally useful functions to JFrame.
 *  @version $Id$
 *  @author Clemens Katzer */

public class KFrame extends JFrame implements MouseListener, WindowListener
{

    /** Only support the simple constructor forms of JFrame. */

    public KFrame()
    {
        super();
        net.sf.colossus.webcommon.FinalizeManager.register(this, "<no title>");
    }

    public KFrame(String title)
    {
        super(title);
        net.sf.colossus.webcommon.FinalizeManager.register(this, title);
    }

    /** Center this dialog on the screen.  Must be called after the dialog
     *  size has been set. */
    public void centerOnScreen()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));
    }

    // Add the do-nothing mouse and window listener methods here, rather 
    // than using Adapters, to reduce the number of useless little inner
    // class files we generate.

    // Note the potential for error if a subclass tries to override
    // one of these methods, but fails due to a typo, and the compiler
    // no longer flags the error because the interface is legally implemented.
    // (Adapters have the same problem.)

    public void mouseClicked(MouseEvent e)
    {
        // nothing to do
    }

    public void mouseEntered(MouseEvent e)
    {
        // nothing to do
    }

    public void mouseExited(MouseEvent e)
    {
        // nothing to do
    }

    public void mousePressed(MouseEvent e)
    {
        // nothing to do
    }

    public void mouseReleased(MouseEvent e)
    {
        // nothing to do
    }

    public void windowClosed(WindowEvent e)
    {
        // nothing to do
    }

    public void windowActivated(WindowEvent e)
    {
        // nothing to do
    }

    public void windowClosing(WindowEvent e)
    {
        // nothing to do
    }

    public void windowDeactivated(WindowEvent e)
    {
        // nothing to do
    }

    public void windowDeiconified(WindowEvent e)
    {
        // nothing to do
    }

    public void windowIconified(WindowEvent e)
    {
        // nothing to do
    }

    public void windowOpened(WindowEvent e)
    {
        // nothing to do
    }
}
