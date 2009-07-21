package net.sf.colossus.guiutil;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;


/**
 * Saves window position and size.
 *
 * @author David Ripton
 */
public final class SaveWindow
{
    private final IOptions options;
    private final String name;

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
     * @return saved location, or null if none
     */
    public Point loadLocation()
    {
        int x = options.getIntOption(name + Options.locX);
        if (x == -1)
        {
            return null;
        }
        int y = options.getIntOption(name + Options.locY);
        if (y == -1)
        {
            return null;
        }
        return new Point(x, y);
    }

    public void saveLocation(final Point location)
    {
        if (location != null)
        {
            int x = location.x;
            if (x == -1)
            {
                x = 0; // tweak to disambiguate from unset
            }
            int y = location.y;
            if (y == -1)
            {
                y = 0; // tweak to disambiguate from unset
            }
            options.setOption(name + Options.locX, x);
            options.setOption(name + Options.locY, y);
        }
    }

    public void save(Window window)
    {
        saveLocation(window.getLocation());
        saveSize(window.getSize());
    }

    public void restore(Window window, Point defaultLocation)
    {
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

    /*
     * Restore size and location, center on screen if no saved location.
     */
    public void restoreOrCenter(KDialog window)
    {
        Dimension size = loadSize();
        if (size == null)
        {
            size = window.getPreferredSize();
        }
        window.setSize(size);

        Point location = loadLocation();
        if (location != null)
        {
            window.setLocation(location);
        }
        else
        {
            window.centerOnScreen();
        }
    }

    public void restoreOrCenter(KFrame window)
    {
        Dimension size = loadSize();
        if (size == null)
        {
            size = window.getPreferredSize();
        }
        window.setSize(size);

        Point location = loadLocation();
        if (location != null)
        {
            window.setLocation(location);
        }
        else
        {
            window.centerOnScreen();
        }
    }

}
