package net.sf.colossus.util;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/** KDialog adds some generally useful functions to JDialog.
 *  @version $Id$
 *  @author David Ripton */

public class KDialog extends JDialog implements MouseListener, WindowListener
{
    /** Only support one of JDialog's many constructor forms. */    
    public KDialog (Frame owner, String title, boolean modal)
    {
        super(owner, title, modal);

        Container cont = getContentPane();

        if (cont instanceof JComponent)
        {
            ((JComponent)cont).setBorder(
                 new javax.swing.border.TitledBorder(title));
        }
    }

    /** Place dialog relative to parentFrame's origin, offset by 
     *  point, and fully on-screen. */
    public void placeRelative(JFrame parentFrame, Point point)
    {
        Point parentOrigin = parentFrame.getLocation();
        // XXX x had - scale, check
        Point origin = new Point(point.x + parentOrigin.x, point.y +
            parentOrigin.y);
        if (origin.x < 0)
        {
            origin.x = 0;
        }
        if (origin.y < 0)
        {
            origin.y = 0;
        }
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int adj = origin.x + getSize().width - d.width;
        if (adj > 0)
        {
            origin.x -= adj;
        }
        adj = origin.y + getSize().height - d.height;
        if (adj > 0)
        {
            origin.y -= adj;
        }
        setLocation(origin);
    }

    /** Center this dialog on the screen.  Must be called after the dialog
     *  size has been set. */
    public void centerOnScreen()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));
    }

    public void upperRightCorner()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Point location = new Point(d.width - getSize().width, 0);
        setLocation(location);
    }

    public void lowerRightCorner()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width,
            d.height - getSize().height));
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
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }
}
