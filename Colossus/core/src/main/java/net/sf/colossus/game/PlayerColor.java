package net.sf.colossus.game;


import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.util.HTMLColor;


/**
 * Models the notion of a color a player can pick.
 *
 * This is not just the actual color of the markers, but also the names used
 * for it (long and short version) as well as a suitable foreground color. The
 * marker color itself is available through the {{@link #getBackgroundColor()}
 * method.
 */
public enum PlayerColor
{
    BLACK("Black", "Bk", KeyEvent.VK_B), BLUE("Blue", "Bu", KeyEvent.VK_L), BROWN(
        "Brown", "Br", KeyEvent.VK_O), GOLD("Gold", "Gd", KeyEvent.VK_G), GREEN(
        "Green", "Gr", KeyEvent.VK_E), RED("Red", "Rd", KeyEvent.VK_R), ORANGE(
        "Orange", "Or", KeyEvent.VK_A), PURPLE("Purple", "Pu", KeyEvent.VK_P), SILVER(
        "Silver", "Si", KeyEvent.VK_S), SKY("Sky", "Sk", KeyEvent.VK_K), PINE(
        "Pine", "Pi", KeyEvent.VK_N), INDIGO("Indigo", "In", KeyEvent.VK_I);

    private final String name;
    private final String shortName;
    private final int mnemonic;
    private final Color backgroundColor;
    private final Color foregroundColor;

    private PlayerColor(String name, String shortName, int mnemonic)
    {
        this.name = name;
        this.shortName = shortName;
        this.mnemonic = mnemonic;
        this.backgroundColor = HTMLColor.stringToColor(name + "Colossus");
        int sum = backgroundColor.getRed() + backgroundColor.getGreen()
            + backgroundColor.getBlue();
        this.foregroundColor = (sum > 200 ? Color.black : Color.white);
    }

    public int getMnemonic()
    {
        return mnemonic;
    }

    public String getName()
    {
        return name;
    }

    public String getShortName()
    {
        return shortName;
    }

    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    public Color getForegroundColor()
    {
        return foregroundColor;
    }

    public static PlayerColor getByName(String name)
    {
        for (PlayerColor color : values())
        {
            if (color.getName().equals(name))
            {
                return color;
            }
        }
        return null; // seems to happen when game starts
    }

    public static PlayerColor getByShortName(String shortName)
    {
        for (PlayerColor color : values())
        {
            if (color.getShortName().equals(shortName))
            {
                return color;
            }
        }
        return null;
    }

    public static List<PlayerColor> getByName(List<String> names)
    {
        List<PlayerColor> retVal = new ArrayList<PlayerColor>();
        for (String name : names)
        {
            retVal.add(getByName(name));
        }
        return retVal;
    }

    @Override
    public String toString()
    {
        return getName(); // important as long as some code might still expect the old strings
    }
}
