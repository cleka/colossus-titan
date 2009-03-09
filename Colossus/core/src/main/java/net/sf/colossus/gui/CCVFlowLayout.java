package net.sf.colossus.gui;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;


public class CCVFlowLayout extends FlowLayout implements ComponentListener
{
    private final JScrollPane parentScrollPane;
    private final JComponent parentComponent;

    CCVFlowLayout(JScrollPane sp, JComponent me, int al, int sx, int sy)
    {
        super(al, sx, sy);
        parentScrollPane = sp;
        parentComponent = me;
        parentScrollPane.addComponentListener(this);
    }

    protected Dimension getOurSize()
    {
        javax.swing.JViewport viewport = parentScrollPane.getViewport();
        Dimension extentSize = viewport.getExtentSize();
        java.awt.Insets insets = parentComponent.getInsets();
        int maxLegalWidth = extentSize.width - (insets.left + insets.right);
        int x = 0, y = insets.top + getVgap();
        int rowHeight = 0, maxWidth = 0;
        Component[] allComponents = parentComponent.getComponents();
        for (Component component : allComponents)
        {
            if (component.isVisible())
            {
                Dimension d = component.getPreferredSize();
                if ((x == 0) || ((x + getHgap() + d.width) <= maxLegalWidth))
                {
                    if (x > 0)
                    {
                        x += getHgap();
                    }
                    x += d.width;
                    rowHeight = (rowHeight < d.height ? d.height : rowHeight);
                }
                else
                {
                    if (x > maxWidth)
                    {
                        maxWidth = x;
                    }
                    x = d.width;
                    y += getVgap() + rowHeight;
                    rowHeight = d.height;
                }
            }
        }
        if (x > (maxWidth - getHgap()))
        {
            maxWidth = x + getHgap();
        }
        y += getVgap() + rowHeight + insets.bottom;
        return new Dimension(maxWidth, y);
    }

    public void componentResized(ComponentEvent e)
    {
        javax.swing.JViewport viewport = parentScrollPane.getViewport();
        Dimension viewSize = viewport.getViewSize();
        Dimension extentSize = viewport.getExtentSize();
        Dimension ourSize = getOurSize();
        if ((viewSize.width != extentSize.width)
            || (viewSize.height != ourSize.height))
        {
            int x = (ourSize.width > extentSize.width ? ourSize.width
                : extentSize.width);
            int y = (ourSize.height > extentSize.height ? ourSize.height
                : extentSize.height);
            parentComponent.setPreferredSize(new Dimension(x, y));
            viewport.setViewSize(new Dimension(x, y));
            parentComponent.doLayout();
            parentScrollPane.doLayout();
        }
    }

    public void componentMoved(ComponentEvent e)
    {
        // necessary to implement interface
    }

    public void componentShown(ComponentEvent e)
    {
        // necessary to implement interface
    }

    public void componentHidden(ComponentEvent e)
    {
        // necessary to implement interface
    }
}
