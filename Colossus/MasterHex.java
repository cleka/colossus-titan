import java.awt.*;

/**
 * Class MasterHex describes one Masterboard hex
 * @version $Id$
 * @author David Ripton
 */

public class MasterHex extends Hex
{
    private boolean inverted;
    private int numLegions = 0;
    private Legion [] legions = new Legion[3];
    private MasterBoard board;

    private MasterHex [] neighbors = new MasterHex[6];
    
    // Terrain types are:
    // B,D,H,J,m,M,P,S,T,t,W
    // Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
    // Swamp, Tower, tundra, Woods

    // Hex labels are:
    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

    // n, ne, se, s, sw, nw
    private int labelSide;
    private int[] exitType = new int[6];
    private int[] entranceType = new int[6];

    // hexsides 0 - 5, though only 1, 3, and 5 are actually used.
    private boolean[] entrySide = new boolean[6];
    private boolean teleported = false;

    public static final int NONE = 0;
    public static final int BLOCK = 1;
    public static final int ARCH = 2;
    public static final int ARROW = 3;
    public static final int ARROWS = 4;


    public MasterHex(int cx, int cy, int scale, boolean inverted, 
        MasterBoard board)
    {
        this.inverted = inverted;
        this.board = board;
        this.scale = scale;
        len = scale / 3.0;
        if (inverted)
        {
            xVertex[0] = cx - scale;
            yVertex[0] = cy;
            xVertex[1] = cx + 3 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
            xVertex[3] = cx + 2 * scale;
            yVertex[3] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[4] = cx;
            yVertex[4] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int) Math.round(SQRT3 * scale);
        }
        else
        {
            xVertex[0] = cx;
            yVertex[0] = cy;
            xVertex[1] = cx + 2 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int) Math.round(2 * SQRT3 * scale);
            xVertex[3] = cx + 3 * scale;
            yVertex[3] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[4] = cx - scale;
            yVertex[4] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int) Math.round(2 * SQRT3 * scale);
        }

        hexagon = new Polygon(xVertex, yVertex, 6);
        // Add 1 to width and height because Java rectangles come up
        // one pixel short of the area actually painted.
        rectBound = new Rectangle(xVertex[5], yVertex[0], xVertex[2] -
                        xVertex[5] + 1, yVertex[3] - yVertex[0] + 1);
    }


    public void rescale(int cx, int cy, int scale)
    {
        this.scale = scale;
        len = scale / 3.0;
        if (inverted)
        {
            xVertex[0] = cx - scale;
            yVertex[0] = cy;
            xVertex[1] = cx + 3 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
            xVertex[3] = cx + 2 * scale;
            yVertex[3] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[4] = cx;
            yVertex[4] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int) Math.round(SQRT3 * scale);
        }
        else
        {
            xVertex[0] = cx;
            yVertex[0] = cy;
            xVertex[1] = cx + 2 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int) Math.round(2 * SQRT3 * scale);
            xVertex[3] = cx + 3 * scale;
            yVertex[3] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[4] = cx - scale;
            yVertex[4] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int) Math.round(2 * SQRT3 * scale);
        }

        hexagon.xpoints = xVertex;
        hexagon.ypoints = yVertex;

        // Add 1 to width and height because Java rectangles come up
        // one pixel short of the area actually painted.
        rectBound.x =  xVertex[5];
        rectBound.y =  yVertex[0];
        rectBound.width = xVertex[2] - xVertex[5] + 1;
        rectBound.height = yVertex[3] - yVertex[0] + 1;
    }


    public void paint(Graphics g)
    {
        if (isSelected())
        {
            g.setColor(Color.white);
        }
        else
        {
            g.setColor(getTerrainColor());
        }

        g.fillPolygon(hexagon);
        g.setColor(Color.black);
        g.drawPolygon(hexagon);

        // Draw label and terrain name
        FontMetrics fontMetrics = g.getFontMetrics();
        String name = getTerrainName().toUpperCase();
        int fontHeight = fontMetrics.getMaxAscent() + fontMetrics.getLeading();

        switch (getLabelSide())
        {
            case 0:
                g.drawString(label, rectBound.x + 
                    (rectBound.width - fontMetrics.stringWidth(label)) / 2,
                    rectBound.y + fontHeight / 2 + rectBound.height / 10);
                break;

            case 1:
                g.drawString(label, rectBound.x + (rectBound.width - 
                    fontMetrics.stringWidth(label)) * 4 / 5,
                    rectBound.y + fontHeight / 2 + rectBound.height / 5);
                break;

            case 2:
                g.drawString(label, rectBound.x + (rectBound.width - 
                    fontMetrics.stringWidth(label)) * 4 / 5,
                    rectBound.y + fontHeight / 2 + rectBound.height * 4 / 5);
                break;

            case 3:
                g.drawString(label, rectBound.x + (rectBound.width - 
                    fontMetrics.stringWidth(label)) / 2,
                    rectBound.y + fontHeight / 2 + rectBound.height * 9 / 10);
                break;

            case 4:
                g.drawString(label, rectBound.x + (rectBound.width - 
                    fontMetrics.stringWidth(label)) / 5,
                    rectBound.y + fontHeight / 2 + rectBound.height * 4 / 5);
                break;

            case 5:
                g.drawString(label, rectBound.x + (rectBound.width - 
                    fontMetrics.stringWidth(label)) / 5,
                    rectBound.y + fontHeight / 2 + rectBound.height / 5);
                break;
        }

        // The word "MOUNTAINS" needs to be printed in the wide part of the hex.
        if (name.equals("MOUNTAINS"))
        {
            g.drawString(name, rectBound.x + (rectBound.width -
                fontMetrics.stringWidth(name)) / 2,
                rectBound.y + fontHeight / 2 + rectBound.height * 2 / 3);
        }
        else
        {
            g.drawString(name, rectBound.x + (rectBound.width -
                fontMetrics.stringWidth(name)) / 2,
                rectBound.y + fontHeight / 2 + rectBound.height / 2);
        }
        

        // Draw exits and entrances
        for (int i = inverted ? 0 : 1; i < 6; i += 2)
        {
            int n = (i + 1) % 6;

            // Draw exits
            // There are up to 3 gates to draw.  Each is 1/6 of a hexside
            // square.  The first is positioned from 1/6 to 1/3 of the way
            // along the hexside, the second from 5/12 to 7/12, and the
            // third from 2/3 to 5/6.  The inner edge of each is 1/12 of a
            // hexside inside the hexside, and the outer edge is 1/12 of a
            // hexside outside the hexside.

            if (exitType[i] != NONE)
            {
                drawGate(g, xVertex[i], yVertex[i], xVertex[n], yVertex[n],
                                exitType[i]);
            }

            // Draw entrances
            // Unfortunately, since exits extend out into adjacent hexes,
            // they sometimes get overdrawn.  So we need to draw them
            // again from the other hex, as entrances.

            if (entranceType[i] != NONE)
            {
                drawGate(g, xVertex[n], yVertex[n], xVertex[i], yVertex[i],
                                entranceType[i]);
            }
        }
    }


    public void repaint()
    {
        board.repaint(rectBound.x, rectBound.y, rectBound.width, 
            rectBound.height);
    }


    private void drawGate(Graphics g, int vx1, int vy1, int vx2, int vy2, 
        int gateType)
    {
        int x0;                 // first focus point
        int y0;
        int x1;                 // second focus point
        int y1;
        int x2;                 // center point
        int y2;
        double theta;           // gate angle
        int [] x = new int[4];  // gate points
        int [] y = new int[4];

        x0 = vx1 + (vx2 - vx1) / 6;
        y0 = vy1 + (vy2 - vy1) / 6;
        x1 = vx1 + (vx2 - vx1) / 3;
        y1 = vy1 + (vy2 - vy1) / 3;

        theta = Math.atan2(vy2 - vy1, vx2 - vx1);

        switch (gateType)
        {
            case BLOCK:
                x[0] = (int) Math.round(x0 - len * Math.sin(theta));
                y[0] = (int) Math.round(y0 + len * Math.cos(theta));
                x[1] = (int) Math.round(x0 + len * Math.sin(theta));
                y[1] = (int) Math.round(y0 - len * Math.cos(theta));
                x[2] = (int) Math.round(x1 + len * Math.sin(theta));
                y[2] = (int) Math.round(y1 - len * Math.cos(theta));
                x[3] = (int) Math.round(x1 - len * Math.sin(theta));
                y[3] = (int) Math.round(y1 + len * Math.cos(theta));

                g.setColor(Color.white);
                g.fillPolygon(x, y, 4);
                g.setColor(Color.black);
                g.drawPolyline(x, y, 4);
                break;

            case ARCH:
                x[0] = (int) Math.round(x0 - len * Math.sin(theta));
                y[0] = (int) Math.round(y0 + len * Math.cos(theta));
                x[1] = (int) Math.round(x0 + len * Math.sin(theta));
                y[1] = (int) Math.round(y0 - len * Math.cos(theta));
                x[2] = (int) Math.round(x1 + len * Math.sin(theta));
                y[2] = (int) Math.round(y1 - len * Math.cos(theta));
                x[3] = (int) Math.round(x1 - len * Math.sin(theta));
                y[3] = (int) Math.round(y1 + len * Math.cos(theta));

                x2 = (int) Math.round((x0 + x1) / 2);
                y2 = (int) Math.round((y0 + y1) / 2);
                Rectangle rect = new Rectangle();
                rect.x = x2 - (int) Math.round(len);
                rect.y = y2 - (int) Math.round(len);
                rect.width = (int) (2 * Math.round(len));
                rect.height = (int) (2 * Math.round(len));
                
                g.setColor(Color.white);
                // Draw a bit more than a semicircle, to clean edge.
                g.fillArc(rect.x, rect.y, rect.width, rect.height,
                    (int) Math.round((2 * Math.PI - theta) * 
                    RAD_TO_DEG - 10), 200);
                g.setColor(Color.black);
                g.drawArc(rect.x, rect.y, rect.width, rect.height,
                    (int) Math.round((2 * Math.PI - theta) * RAD_TO_DEG), 
                    180);
                        
                x[2] = x[0];
                y[2] = y[0];
                x[0] = x1;
                y[0] = y1;
                x[1] = x[3];
                y[1] = y[3];
                x[3] = x0;
                y[3] = y0;
                g.setColor(Color.white);
                g.fillPolygon(x, y, 4);
                g.setColor(Color.black);
                g.drawLine(x1, y1, x[1], y[1]);
                g.drawLine(x[2], y[2], x0, y0);
                break;

            case ARROW:
                x[0] = (int) Math.round(x0 - len * Math.sin(theta));
                y[0] = (int) Math.round(y0 + len * Math.cos(theta));
                x[1] = (int) Math.round((x0 + x1) / 2 + len * 
                    Math.sin(theta));
                y[1] = (int) Math.round((y0 + y1) / 2 - len * 
                    Math.cos(theta));
                x[2] = (int) Math.round(x1 - len * Math.sin(theta));
                y[2] = (int) Math.round(y1 + len * Math.cos(theta));

                g.setColor(Color.white);
                g.fillPolygon(x, y, 3);
                g.setColor(Color.black);
                g.drawPolyline(x, y, 3);
                break;

            case ARROWS:
                for (int j = 0; j < 3; j++)
                {
                    x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                    y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;

                    x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                    y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                    x[0] = (int) Math.round(x0 - len * Math.sin(theta));
                    y[0] = (int) Math.round(y0 + len * Math.cos(theta));
                    x[1] = (int) Math.round((x0 + x1) / 2 + len * 
                           Math.sin(theta));
                    y[1] = (int) Math.round((y0 + y1) / 2 - len * 
                           Math.cos(theta));
                    x[2] = (int) Math.round(x1 - len * Math.sin(theta));
                    y[2] = (int) Math.round(y1 + len * Math.cos(theta));
    
                    g.setColor(Color.white);
                    g.fillPolygon(x, y, 3);
                    g.setColor(Color.black);
                    g.drawPolyline(x, y, 3);
                }
                break;
        }
    }


    public String getTerrainName()
    {
        switch (getTerrain())
        {
            case 'B':
                return "Brush";
            case 'D':
                return "Desert";
            case 'H':
                return "Hills";
            case 'J':
                return "Jungle";
            case 'm':
                return "Mountains";
            case 'M':
                return "Marsh";
            case 'P':
                return "Plains";
            case 'S':
                return "Swamp";
            case 'T':
                return "Tower";
            case 't':
                return "Tundra";
            case 'W':
                return "Woods";
            default:
                return "?????";
        }
    }


    public Color getTerrainColor()
    {
        switch (getTerrain())
        {
            case 'B':
                return Color.green;
            case 'D':
                return Color.orange;
            case 'H':
                return new Color(128, 64, 0);
            case 'J':
                return new Color(0, 128, 0);
            case 'm':
                return Color.red;
            case 'M':
                return new Color(180, 90, 0);
            case 'P':
                return Color.yellow;
            case 'S':
                return Color.blue;
            case 'T':
                return Color.gray;
            case 't':
                return new Color(128, 170, 255);
            case 'W':
                return new Color(128, 128, 0);
            default:
                return Color.black;
        }
    }


    // Return the number of types of recruits for this terrain type.
    public int getNumRecruitTypes()
    {
        switch (getTerrain())
        {
            case 'B':
            case 'D':
            case 'H':
            case 'M':
            case 'P':
            case 'S':
            case 'W':
                return 3;

            case 'J':
            case 'm':
            case 't':
                return 4;

            case 'T':
                return 5;

            default:
                return -1;
        }
    }


    // Return the ith recruit possible in this terrain type.
    public Creature getRecruit(int i)
    {
        switch (getTerrain())
        {
            case 'B':
                switch (i)
                {
                    case 0:
                        return Creature.gargoyle;
                    case 1:
                        return Creature.cyclops;
                    case 2:
                        return Creature.gorgon;
                    default:
                        return null;
                }

            case 'D':
                switch (i)
                {
                    case 0:
                        return Creature.lion;
                    case 1:
                        return Creature.griffon;
                    case 2:
                        return Creature.hydra;
                    default:
                        return null;
                }

            case 'H':
                switch (i)
                {
                    case 0:
                        return Creature.ogre;
                    case 1:
                        return Creature.minotaur;
                    case 2:
                        return Creature.unicorn;
                    default:
                        return null;
                }

            case 'J':
                switch (i)
                {
                    case 0:
                        return Creature.gargoyle;
                    case 1:
                        return Creature.cyclops;
                    case 2:
                        return Creature.behemoth;
                    case 3:
                        return Creature.serpent;
                    default:
                        return null;
                }

            case 'm':
                switch (i)
                {
                    case 0:
                        return Creature.lion;
                    case 1:
                        return Creature.minotaur;
                    case 2:
                        return Creature.dragon;
                    case 3:
                        return Creature.colossus;
                    default:
                        return null;
                }

            case 'M':
                switch (i)
                {
                    case 0:
                        return Creature.ogre;
                    case 1:
                        return Creature.troll;
                    case 2:
                        return Creature.ranger;
                    default:
                        return null;
                }

            case 'P':
                switch (i)
                {
                    case 0:
                        return Creature.centaur;
                    case 1:
                        return Creature.lion;
                    case 2:
                        return Creature.ranger;
                    default:
                        return null;
                }

            case 'S':
                switch (i)
                {
                    case 0:
                        return Creature.troll;
                    case 1:
                        return Creature.wyvern;
                    case 2:
                        return Creature.hydra;
                    default:
                        return null;
                }

            case 'T':
                switch (i)
                {
                    case 0:
                        return Creature.centaur;
                    case 1:
                        return Creature.gargoyle;
                    case 2:
                        return Creature.ogre;
                    case 3:
                        return Creature.guardian;
                    case 4:
                        return Creature.warlock;
                    default:
                        return null;
                }

            case 't':
                switch (i)
                {
                    case 0:
                        return Creature.troll;
                    case 1:
                        return Creature.warbear;
                    case 2:
                        return Creature.giant;
                    case 3:
                        return Creature.colossus;
                    default:
                        return null;
                }

            case 'W':
                switch (i)
                {
                    case 0:
                        return Creature.centaur;
                    case 1:
                        return Creature.warbear;
                    case 2:
                        return Creature.unicorn;
                    default:
                        return null;
                }

            default:
                return null;
        }
    }

    
    // Return the number of the next lower creature needed to muster the ith 
    // recruit possible in this terrain type. If not applicable, return 0.
    public int getNumToRecruit(int i)
    {
        switch (getTerrain())
        {
            case 'B':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 2;
                    case 2:
                        return 2;
                    default:
                        return 0;
                }

            case 'D':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 3;
                    case 2:
                        return 2;
                    default:
                        return 0;
                }

            case 'H':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 3;
                    case 2:
                        return 2;
                    default:
                        return 0;
                }

            case 'J':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 2;
                    case 2:
                        return 3;
                    case 3:
                        return 2;
                    default:
                        return 0;
                }

            case 'm':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 2;
                    case 2:
                        return 2;
                    case 3:
                        return 2;
                    default:
                        return 0;
                }

            case 'M':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 2;
                    case 2:
                        return 2;
                    default:
                        return 0;
                }

            case 'P':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 2;
                    case 2:
                        return 2;
                    default:
                        return 0;
                }

            case 'S':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 3;
                    case 2:
                        return 2;
                    default:
                        return 0;
                }

            case 'T':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 0;
                    case 2:
                        return 0;
                    case 3:
                        return 0;
                    case 4:
                        return 0;
                    default:
                        return 0;
                }

            case 't':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 2;
                    case 2:
                        return 2;
                    case 3:
                        return 2;
                    default:
                        return 0;
                }

            case 'W':
                switch (i)
                {
                    case 0:
                        return 0;
                    case 1:
                        return 3;
                    case 2:
                        return 2;
                    default:
                        return 0;
                }

            default:
                return 0;
        }
    }


    // Return a point near the center of the hex, vertically offset
    // a bit toward the fat side.
    public Point getOffCenter()
    {
        return new Point((xVertex[0] + xVertex[1]) / 2, (yVertex[0] + 
            yVertex[3]) / 2 + (inverted ? -(scale / 6) : (scale / 6))); 
    }


    public int getNumLegions()
    {
        return numLegions;
    }


    public boolean isOccupied()
    {
        return (numLegions > 0);
    }


    public Legion getLegion(int i)
    {
        if (i < 0 || i > numLegions - 1)
        {
            return null;
        }
        else
        {
            return legions[i];
        }
    }


    public int getNumFriendlyLegions(Player player)
    {
        int count = 0;
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getPlayer() == player)
            {
                count++;
            }
        }
        return count;
    }

    
    public int getNumEnemyLegions(Player player)
    {
        int count = 0;
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getPlayer() != player)
            {
                count++;
            }
        }
        return count;
    }


    public boolean isEngagement()
    {
        if (numLegions > 0)
        {
            Player player = legions[0].getPlayer();
            for (int i = 1; i < numLegions; i++)
            {
                if (legions[i].getPlayer() != player)
                {
                    return true;
                }
            }
        }

        return false;
    }


    public Legion getFriendlyLegion(Player player)
    {
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getPlayer() == player)
            {
                return legions[i];
            }
        }
        return null;
    }


    public Legion getEnemyLegion(Player player)
    {
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getPlayer() != player)
            {
                return legions[i];
            }
        }
        return null;
    }


    public void alignLegions()
    {
        if (numLegions == 0)
        {
            return;
        }

        int chitScale = legions[0].getMarker().getBounds().width;
        Point point = getOffCenter();

        if (numLegions == 1)
        {
            // Place legion in the center of the hex.
            point.x -= chitScale / 2;
            point.y -= chitScale / 2;
            legions[0].getMarker().setLocationAbs(point);     
        }
        else if (numLegions == 2)
        {
            // Place legions in NW and SE corners.
            point.x -= 3 * chitScale / 4;
            point.y -= 3 * chitScale / 4;
            legions[0].getMarker().setLocationAbs(point);

            point = getOffCenter();
            point.x -= chitScale / 4;
            point.y -= chitScale / 4;
            legions[1].getMarker().setLocationAbs(point);
        }
        else if (numLegions == 3)
        {
            // Place legions in NW, SE, NE corners.
            point.x -= 3 * chitScale / 4;
            point.y -= 3 * chitScale / 4;
            legions[0].getMarker().setLocationAbs(point);

            point = getOffCenter();
            point.x -= chitScale / 4;
            point.y -= chitScale / 4;
            legions[1].getMarker().setLocationAbs(point);

            point = getOffCenter();
            point.x -= chitScale / 4;
            point.y -= chitScale;
            legions[2].getMarker().setLocationAbs(point);
        }
    }
    
    
    public void addLegion(Legion legion)
    {
        numLegions++;
        legions[numLegions - 1] = legion;

        // Reposition all legions within the hex.
        alignLegions();
    }


    public void removeLegion(Legion legion)
    {
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i] == legion)
            {
                for (int j = i; j < numLegions - 1; j++)
                {
                    legions[j] = legions[j + 1];
                }
                legions[numLegions - 1] = null;
                numLegions--;

                // Write over the bounding area of the hex with
                // the background color, to prevent artifacts from
                // chits that used to hang outside the hex boundary.
                if (numLegions >= 1)
                {
                    board.setEraseFlag();
                }

                // Reposition all legions within the hex.
                alignLegions();
                return;
            }
        }
    }


    public void setNeighbor(int i, MasterHex hex)
    {
        neighbors[i] = hex;
    }


    public MasterHex getNeighbor(int i)
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


    public String getLabel()
    {
        return label;
    }


    public int getLabelSide()
    {
        return labelSide;
    }


    public void setLabel(int label)
    {
        this.label = Integer.toString(label);
    }


    public void setLabelSide(int labelSide)
    {
        this.labelSide = labelSide;
    }


    public void setExitType(int i, int exitType)
    {
        this.exitType[i] = exitType;
    }


    public int getExitType(int i)
    {
        return exitType[i];
    }
    

    public void setEntranceType(int i, int entranceType)
    {
        this.entranceType[i] = entranceType;
    }


    public int getEntranceType(int i)
    {
        return entranceType[i];
    }


    public void setEntrySide(int side)
    {
        entrySide[side] = true;
    }


    // Return a possible entry side.  If there are more than one, only one
    // will be returned.
    public int getEntrySide()
    {
        for (int i = 0; i < 6; i++)
        {
            if (entrySide[i])
            {
                return i;
            }
        }

        return -1;
    }


    // Return the number of possible entry sides.
    public int getNumEntrySides()
    {
        int count = 0;
        for (int i = 0; i < 6; i++)
        {
            if (entrySide[i])
            {
                count++;
            }
        }
        
        return count;
    }


    public boolean canEnterViaSide(int side)
    {
        if (0 <= side && side < 6)
        {
            return entrySide[side];
        }
        else
        {
            return false;
        }
    }


    public boolean canEnterViaLand()
    {
        for (int i = 0; i < 6; i++)
        {
            if (entrySide[i])
            {
                return true;
            }
        }

        return false;
    }


    public boolean teleported()
    {
        return teleported;
    }


    public void setTeleported()
    {
        teleported = true;
    }


    public void clearTeleported()
    {
        teleported = false;
    }


    public void clearAllEntrySides()
    {
        for (int i = 0; i < 6; i++)
        {
            entrySide[i] = false;
        }
    }


    // Present a dialog allowing the player to enter via land or teleport.
    public void chooseWhetherToTeleport()
    {
        new OptionDialog(board, "Teleport?", "Teleport?", "Teleport", 
            "Move Normally");

        // If Teleport, then leave teleported set.
        if (OptionDialog.getLastAnswer() == OptionDialog.NO_OPTION)
        {
            clearTeleported();
        }
    }


    public boolean inverted()
    {
        return inverted;
    }
}
