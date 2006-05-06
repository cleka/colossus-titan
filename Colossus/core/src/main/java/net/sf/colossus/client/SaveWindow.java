package net.sf.colossus.client;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;

import net.sf.colossus.util.Options;


/** 
 *  Saves window position and size.
 *  @version $Id$
 *  @author David Ripton 
 */
public final class SaveWindow
{
    private IOptions options;
    private String name;

    public static final Point nullPoint = new Point(-1, -1);
    
    public SaveWindow(IOptions options, String name)
    {
        this.options = options;
        this.name = name;
    }

    public Dimension loadSize()
    {
        int x = options.getIntOption(name + Options.sizeX);
        int y = options.getIntOption(name + Options.sizeY);
        Dimension size = null;
        if (x > 0 && y > 0)
        {
            size = new Dimension(x, y);
        }
        return size;
    }

    public void saveSize(final Dimension size)
    {
        options.setOption(name + Options.sizeX, (int)size.getWidth());
        options.setOption(name + Options.sizeY, (int)size.getHeight());
    }

    /**
     * BUG: A Lot of code checks for loadLocation() == null, when in fact this 
     * can never occur.
     * As <code>getIntOptions</code> returns -1 for unknown property values.
     * 
     * @return
     */
    public Point loadLocation()
    {
        int x = options.getIntOption(name + Options.locX);
        int y = options.getIntOption(name + Options.locY);
        return new Point(x, y);
    }

    public void saveLocation(final Point location)
    {
        options.setOption(name + Options.locX, location.x);
        options.setOption(name + Options.locY, location.y);
    }
    
    public void save(Window window ) {
        saveLocation(window.getLocation());
        saveSize(window.getSize());
    }
    
    public void restore(Window window, Point defaultLocation) {
        Point location = loadLocation();
        if (location == null)
        {
            location = defaultLocation;
        }
        window.setLocation(location);

        Dimension size = loadSize();
        if (size == null)
        {
            size = window.getPreferredSize();
        }
        window.setSize(size);
    }
}

