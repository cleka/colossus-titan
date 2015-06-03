package net.sf.colossus.gui;


import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;


/**
 * This listener can be used to make a Component draggable.
 *
 * If subscribed as MouseListener and MouseMotionListener, the component this
 * listener subscribes to will become draggable by mouse. This subscription is
 * done by the static method #makeDraggable(Component) to make sure it is done
 * the right way.
 */
public class DragListener extends MouseAdapter implements MouseMotionListener
{
    private final Component component;

    private Point lastMousePos;

    private DragListener(Component component)
    {
        this.component = component;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        Point loc = component.getLocation();
        // find mouse pos on parent instead of on panel, since the latter moves
        Point newMousePos = e.getPoint();
        newMousePos.translate(loc.x, loc.y);

        if (lastMousePos != null)
        {
            int diffX = newMousePos.x - lastMousePos.x;
            int diffY = newMousePos.y - lastMousePos.y;
            loc.x += diffX;
            loc.y += diffY;
            component.setLocation(loc);
        }
        lastMousePos = newMousePos;
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        // nothing to do
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // reset stored mouse position in case a second drag occurs
        this.lastMousePos = null;
    }

    public static void makeDraggable(Component component)
    {
        DragListener listener = new DragListener(component);
        component.addMouseListener(listener);
        component.addMouseMotionListener(listener);
    }
}
