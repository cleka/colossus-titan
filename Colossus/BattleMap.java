/** Attempted port of hexmap from MFC to Java dripton  12/10/97 */

import java.awt.*;


class Hex extends Canvas implements Shape
{
    protected boolean selected;
    private int[] x_vertex = new int[6];
    private int[] y_vertex = new int[6];
    private Polygon p;
    private Rectangle rectBound;

    Hex(int cx, int cy, int scale)
    {
        selected = false;

        x_vertex[0] = cx;
        y_vertex[0] = cy;
        x_vertex[1] = cx + 2 * scale;
        y_vertex[1] = cy;
        x_vertex[2] = cx + 3 * scale;
        y_vertex[2] = cy + (int)java.lang.Math.round(java.lang.Math.sqrt(3.0)
                       * scale);
        x_vertex[3] = cx + 2 * scale;
        y_vertex[3] = cy + (int)java.lang.Math.round(2 * java.lang.Math.sqrt(3.0)
                       * scale);
        x_vertex[4] = cx;
        y_vertex[4] = cy + (int)java.lang.Math.round(2 * java.lang.Math.sqrt(3.0)
                       * scale);
        x_vertex[5] = cx - 1 * scale;
        y_vertex[5] = cy + (int)java.lang.Math.round(java.lang.Math.sqrt(3.0)
                       * scale);

        p = new Polygon(x_vertex, y_vertex, 6);
        rectBound = new Rectangle(x_vertex[5], y_vertex[0], x_vertex[2] - 
            x_vertex[5], y_vertex[3] - y_vertex[0]);
    }


    public void paint(Graphics g)
    {
        update(g);
    }
    

    public void update(Graphics g)
    {
        System.out.println("painting a Hex");
        if (selected)
        {
            g.setColor(java.awt.Color.red);
            g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
        }
        else
        {
            g.setColor(java.awt.Color.white);
	    g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
        }
    }


    boolean select(Point point)
    {
        if (p.contains(point))
        {
            selected = !selected;
            repaint();
	    return true;
        }
	return false;
    }


    public Rectangle getBounds()
    {
        return rectBound;
    }

    public boolean contains(Point point)
    {
        return (p.contains(point));
    }
    
    public Dimension minimumsize()
    {
        return new Dimension(x_vertex[2] - x_vertex[5], y_vertex[3] 
	    - y_vertex[0]);
    }

    public Dimension preferredSize()
    {
        return new Dimension(x_vertex[2] - x_vertex[5], y_vertex[3] 
	    - y_vertex[0]);
    }
}



class Chit extends Canvas implements Shape, java.awt.image.ImageObserver
{
    protected boolean selected;
    private Rectangle rect;
    private Image image;

    Chit(int cx, int cy, int scale, String image_filename)
    {
        selected = false;
        rect = new Rectangle(cx, cy, scale, scale);
        image = Toolkit.getDefaultToolkit().getImage(image_filename);
    }


    public void paint(Graphics g)
    {
        update(g);
    }

    public void update(Graphics g)
    {
        if (g.drawImage(image, rect.x, rect.y, this) == false)
        {
            System.out.println("image started drawing but isn't done");
        }
    }


    boolean select(Point point)
    {
        if (rect.contains(point))
        {
            selected = true;
            repaint();
        }
        else
        {
            selected = false;
        }
        return selected;
    }


    void move(Point point)
    {
        // Repainting needs to happen at a higher level.
        rect.setLocation(point);
    }

    // Required by interface Shape
    public Rectangle getBounds()
    {
        return (Rectangle) rect;
    }

    // Required by interface ImageObserver
    public boolean imageUpdate(Image img, int infoflags, int x,
        int y, int width, int height)
    {
        // If image is done loading, forget about it.
        if (infoflags >= 32)
        {
            return false;
        }
        else 
        {
            return true;
        }
    }
    
    public Dimension minimumsize()
    {
        return new Dimension(rect.width, rect.height);
    }

    public Dimension preferredSize()
    {
        return new Dimension(rect.width, rect.height);
    }

}



class HexMap extends Canvas
{
    private Hex[][] h = new Hex[6][6];
    private Chit[] chits = new Chit[22];
    private int tracking;
    Rectangle rectClip = new Rectangle();
    final private boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };

    HexMap(int cx, int cy, int scale)
    {
        System.out.println("Creating a HexMap.");
	tracking = -1;

        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new Hex
                        ((int)java.lang.Math.round(cx + 3 * i * scale),
                        (int)java.lang.Math.round(cy + (2 * j + i % 2) *
                        java.lang.Math.sqrt(3.0) * scale), scale);
                }
            }
        }

        chits[0] = new Chit(100, 100, 60, "Angel.gif");
        chits[1] = new Chit(120, 120, 60, "Archange.gif");
        chits[2] = new Chit(140, 140, 60, "Behemoth.gif");
        chits[3] = new Chit(160, 160, 60, "Centaur.gif");
        chits[4] = new Chit(180, 180, 60, "Colossus.gif");
        chits[5] = new Chit(200, 200, 60, "Cyclops.gif");
        chits[6] = new Chit(220, 220, 60, "Gargoyle.gif");
        chits[7] = new Chit(240, 240, 60, "Giant.gif");
        chits[8] = new Chit(260, 260, 60, "Gorgon.gif");
        chits[9] = new Chit(280, 280, 60, "Griffon.gif");
        chits[10] = new Chit(300, 300, 60, "Guardian.gif");
        chits[11] = new Chit(320, 320, 60, "Hydra.gif");
        chits[12] = new Chit(340, 340, 60, "Lion.gif");
        chits[13] = new Chit(360, 360, 60, "Minotaur.gif");
        chits[14] = new Chit(380, 380, 60, "Ogre.gif");
        chits[15] = new Chit(400, 400, 60, "Ranger.gif");
        chits[16] = new Chit(420, 420, 60, "Serpent.gif");
        chits[17] = new Chit(460, 460, 60, "Titan.gif");
        chits[18] = new Chit(480, 480, 60, "Troll.gif");
        chits[19] = new Chit(500, 500, 60, "Unicorn.gif");
        chits[20] = new Chit(520, 520, 60, "Warbear.gif");
        chits[21] = new Chit(540, 540, 60, "Warlock.gif");
    }


    public void paint(Graphics g)
    {
        update(g);
    }

    public void update(Graphics g)
    {
        rectClip = g.getClipBounds();
        System.out.println("rectClip: " + rectClip.x + " " + rectClip.y 
	    + " " + rectClip.width + " " + rectClip.height);

        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    System.out.println("drawing h[" + i + "][" + j + "]");
                    h[i][j].update(g);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = chits.length - 1; i >= 0; i--)
        {
	    if (rectClip.intersects(chits[i].getBounds()))
	    {
                System.out.println("Drawing chits[" + i + "]");
                chits[i].update(g);
            }
        }
    }




    public Dimension minimumsize()
    {
        return new Dimension(200, 200);
    }

    public Dimension preferredSize()
    {
        return new Dimension(700,700);
    }


    public boolean mouseDown(Event event, int x, int y)
    {
        Point point = new Point(x,y);
        for (int i=0; i < chits.length; i++)
        {
            if (chits[i].select(point))
            {
                System.out.println("Selected chits[" + i +"]");
                // Move selected chit to top of Z-order
                tracking = 0;

                // Don't swap if it's already on top.
                if (i != 0)
                {
                    Chit tmpchit = chits[i];
                    for (int j = i; j > 0; j--)
                    {
		        chits[j] = chits[j - 1];
                    }
                    chits[0] = tmpchit;
                    chits[0].repaint();
		    // TODO: Make this work without a global repaint.
		    repaint();
                }
                return false;
            }
        }

        // No hits on chits, so check map.
        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                if (show[i][j] && h[i][j].contains(point))
                {
                    System.out.println("Calling select for h[" + i + "]["
                        + j +"]");
                    h[i][j].select(point);
                    // TODO: Make this work without a global repaint.
                    repaint();
		    return false;
                }
            }
        }

        return false;
    }


    public boolean mouseUp(Event event, int x, int y)
    {
        tracking = -1;
        return false;
    }


    public boolean mouseDrag(Event event, int x, int y)
    {
        Point point = new Point(x,y);
        if (tracking != -1)
        {
            // TODO: Set clip so that the chit's old 
            // location gets erased, so we don't need
            // a global repaint.
            chits[tracking].move(point);
            repaint();
        }
        return false;
    }

}


public class BattleMap extends Frame
{
    private HexMap map;

    public BattleMap()
    {
        super("BattleMap");

        map = new HexMap(80, 20, 25);
        add("Center", map);
        validate();
    }

    public boolean handleEvent(Event event)
    {
        //System.out.println("BattleMap got event " + event.toString());
        if (event.id == Event.WINDOW_DESTROY)
	{
	    System.exit(0);
        }
        return super.handleEvent(event);
    }

    public void update(Graphics g)
    {
        System.out.println("Called BattleMap.update()");
    }
    
    public void paint(Graphics g)
    {
        update(g);
    }

    public static void main(String args[])
    {
        BattleMap window = new BattleMap();
        window.setTitle("BattleMap");
        window.pack();
	window.setVisible(true);
    }

}
