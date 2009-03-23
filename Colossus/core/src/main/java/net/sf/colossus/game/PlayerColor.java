package net.sf.colossus.game;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO the colors themselves should be defined in here
 */
public enum PlayerColor
{
    BLACK("Black", "Bk", KeyEvent.VK_B), BLUE("Blue", "Bu", KeyEvent.VK_L), BROWN(
        "Brown", "Br", KeyEvent.VK_O), GOLD("Gold", "Gd", KeyEvent.VK_G), GREEN(
        "Green", "Gr", KeyEvent.VK_E), RED("Red", "Rd", KeyEvent.VK_R), ORANGE(
        "Orange", "Or", KeyEvent.VK_A), PURPLE("Purple", "Pu",
        KeyEvent.VK_P), SILVER("Silver", "Si", KeyEvent.VK_S), SKY("Sky",
        "Sk", KeyEvent.VK_K), PINE("Pine", "Pi", KeyEvent.VK_N), INDIGO(
        "Indigo", "In", KeyEvent.VK_I);

    private final String name;
    private final String shortName;
    private final int mnemonic;

    private PlayerColor(String name, String shortName, int mnemonic)
    {
        this.name = name;
        this.shortName = shortName;
        this.mnemonic = mnemonic;
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