
package net.sf.colossus.datatools;

import java.awt.*;
import java.util.*;
import java.awt.geom.*;

import net.sf.colossus.util.HTMLColor;

/**
 * Class BuilderHex.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */
public class BuilderHex extends Hex
{
    /** Valid elevations are 0, 1, and 2. */
    private int elevation;

    // Hex terrain types are:
    // p, r, s, t, o, v, d
    // plain, bramble, sand, tree, bog, volcano, drift
    // also
    // l
    // lake

    // Hexside terrain types are:
    // d, c, s, w, space
    // dune, cliff, slope, wall, no obstacle
    // The hexside is marked only in the higher hex.
    private char [] hexsides = new char[6];

    private BuilderHex [] neighbors = new BuilderHex[6];

    private int xCoord;
    private int yCoord;

    // Hex labels are:
    // A1-A3, B1-B4, C1-C5, D1-D6, E1-E5, F1-F4.
    // Letters increase left to right; numbers increase bottom to top.

    /** Movement costs */
    public static final int IMPASSIBLE_COST = 99;
    private static final int SLOW_COST = 2;
    private static final int NORMAL_COST = 1;



    BuilderHex(int xCoord, int yCoord)
    {
        this.xCoord = xCoord;
        this.yCoord = yCoord;

        for (int i = 0; i < 6; i++)
        {
            hexsides[i] = ' ';
        }

        setTerrain('p');
        assignLabel();
    }


    public String getTerrainName()
    {
        String terrainName;

        switch (getTerrain())
        {
        case 'p':
            terrainName = "Plains";
            break;
        case 'w':
            terrainName = "Tower";
            break;
        case 'r':
            terrainName = "Bramble";
            break;
        case 's':
            terrainName = "Sand";
            break;
        case 't':
            /* tree height is irrelevant, so get out now */
            return "Tree";
        case 'o':
            terrainName = "Bog";
            break;
        case 'v':
            terrainName = "Volcano";
            break;
        case 'd':
            terrainName = "Drift";
            break;
        case 'l':
            terrainName = "Lake";
            break;
        default:
            terrainName = "?????";
            break;
        }
        if (elevation == 0)
            return(terrainName);
        else
            return(terrainName + " (" + elevation + ")");
    }
    

    Color getTerrainColor()
    {
        switch (getTerrain())
        {
        case 'p':  // plain
            switch (elevation)
            {
            case 0:
                return HTMLColor.lightOlive;
            case 1:
                return HTMLColor.darkYellow;
            default:
            case 2:
                return Color.yellow;
            }
        case 'w':  // tower
            switch (elevation)
            {
            case 0:
                return HTMLColor.lightGray;
            case 1:
                return Color.gray;
            default:
            case 2:
                return HTMLColor.darkGray;
            }
        case 'r':  // bramble
            return Color.green;
        case 's':  // sand
            return Color.orange;
        case 't':  // tree
            return HTMLColor.brown;
        case 'o':  // bog
            return Color.gray;
        case 'v':  // volcano
            return Color.red;
        case 'd':  // drift
            return Color.blue;
        case 'l':  // lake
            return HTMLColor.skyBlue;
        default:
            return Color.black;
        }
    }


    public boolean isNativeBonusTerrain()
    {
        char t = getTerrain();
        if (t == 'r' || t == 'v')
        {
            return true;
        }
        for (int i = 0; i < 6; i++)
        {
            char h = getHexside(i);
            if (h == 'w' || h == 's' || h == 'd')
            {
                return true;
            }
        }
        return false;
    }


    public boolean isNonNativePenaltyTerrain()
    {
        char t = getTerrain();
        if (t == 'r' || t == 'd')
        {
            return true;
        }
        for (int i = 0; i < 6; i++)
        {
            char h = getOppositeHexside(i);
            if (h == 'w' || h == 's' || h == 'd')
            {
                return true;
            }
        }
        return false;
    }


    private void assignLabel()
    {
        if (xCoord == -1)
        {
            label = "X" + yCoord;
            return;
        }

        char xLabel;
        switch (xCoord)
        {
            case 0:
                xLabel = 'A';
                break;
            case 1:
                xLabel = 'B';
                break;
            case 2:
                xLabel = 'C';
                break;
            case 3:
                xLabel = 'D';
                break;
            case 4:
                xLabel = 'E';
                break;
            case 5:
                xLabel = 'F';
                break;
            default:
                xLabel = '?';
        }

        int yLabel = 6 - yCoord - (int)Math.abs(((xCoord - 3) / 2));
        label = xLabel + Integer.toString(yLabel);
    }


    public void setHexside(int i, char hexside)
    {
        this.hexsides[i] = hexside;
    }


    public char getHexside(int i)
    {
        return hexsides[i];
    }


    /** Return the flip side of hexside i. */
    public char getOppositeHexside(int i)
    {
        char hexside = ' ';

        BuilderHex neighbor = getNeighbor(i);
        if (neighbor != null)
        {
            hexside = neighbor.getHexside((i + 3) % 6);
        }

        return hexside;
    }


    public int getElevation()
    {
        return elevation;
    }

    public void setElevation (int elevation)
    {
        this.elevation = elevation;
    }


    public BuilderHex getNeighbor(int i)
    {
        if (i < 0 || i > 6)
        {
            return null;
        }
        else
        {
            return neighbors[i];
        }
    }

    void setNeighbor(int i, BuilderHex hex)
    {
        if (i >= 0 && i < 6)
        {
            neighbors[i] = hex;
        }
    }


    public int getXCoord()
    {
        return xCoord;
    }

    public int getYCoord()
    {
        return yCoord;
    }


    public boolean isEntrance()
    {
        return (xCoord == -1);
    }


    public boolean hasWall()
    {
        for (int i = 0; i < 6; i++)
        {
            if (hexsides[i] == 'w')
            {
                return true;
            }
        }
        return false;
    }
}
