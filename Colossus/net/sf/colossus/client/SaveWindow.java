package net.sf.colossus.client;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.sf.colossus.util.Options;


/** 
 *  Saves window position and size.
 *  @version $Id$
 *  @author David Ripton 
 */
public final class SaveWindow
{
    private Client client;
    private String name;
    private final Dimension screen = 
        Toolkit.getDefaultToolkit().getScreenSize();


    public SaveWindow(Client client, String name)
    {
        this.client = client;
        this.name = name;
    }

    public Dimension loadSize()
    {
        int x = client.getIntOption(name + Options.sizeX);
        int y = client.getIntOption(name + Options.sizeY);
        Dimension size = null;
        if (x > 0 && y > 0)
        {
            size = new Dimension(x, y);
        }
        return size;
    }

    public void saveSize(final Dimension size)
    {
        client.setOption(name + Options.sizeX, (int)size.getWidth());
        client.setOption(name + Options.sizeY, (int)size.getHeight());
    }

    public Point loadLocation()
    {
        int x = client.getIntOption(name + Options.locX);
        int y = client.getIntOption(name + Options.locY);
        Point location = null;
        if (x >= 0 && y >= 0 && x < screen.width && y < screen.height)
        {
            location = new Point(x, y);
        }
        return location;
    }

    public void saveLocation(final Point location)
    {
        client.setOption(name + Options.locX, location.x);
        client.setOption(name + Options.locY, location.y);
    }
}

