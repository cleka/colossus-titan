/** Attempted port of hexmap from MFC to Java
    dripton  12/10/97 */

import java.awt.*;


class Hex extends Canvas implements Shape
{
    protected boolean selected;
    private int[] x_vertex = new int[6];
    private int[] y_vertex = new int[6];
    private Polygon p;

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
    }


    public void paint(Graphics g)
    {
        update(g);
    }
    

    public void update(Graphics g)
    {
        System.out.println("painting a Hex");
        if (selected == true)
        {
            g.setColor(java.awt.Color.red);
            g.fillPolygon(p);
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


    void select(Point point)
    {
        // This only checks the bounding rectangle; need to fix.
        if (getBounds().contains(point))
        {
            selected = !selected;
            repaint();
        }
    }


    public Rectangle getBounds()
    {
        Rectangle rect = new Rectangle();
        rect.x = x_vertex[5];
        rect.y = y_vertex[0];
        rect.width  = x_vertex[2] - rect.x;
        rect.height = y_vertex[3] - rect.y;
        return rect;
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
//          g.drawImage(image, 0, 0, this);

          if (selected)
          {
              g.setColor(java.awt.Color.yellow);
          }
          else
          {
              g.setColor(java.awt.Color.blue);
          }
          g.fillRect(rect.x,rect.y,rect.width,rect.height);

    }


    boolean select(Point point)
    {
        if (rect.contains(point))
        {
            selected = true;
        }
        else
        {
            selected = false;
        }

        repaint();

        return selected;
    }


    void move(Point point)
    {
        rect.setLocation(point);
        repaint();
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
        return false;
    }

}



class HexMap extends Canvas
{
    private Hex[][] h = new Hex[6][6];
    private Chit[] chits = new Chit[8];
    private int tracking;
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

        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                if (show[i][j] == true)
                {
                    h[i][j] = new Hex
                        ((int)java.lang.Math.round(cx + 3 * i * scale),
                        (int)java.lang.Math.round(cy + (2 * j + i % 2) *
                        java.lang.Math.sqrt(3.0) * scale), scale);
                }
            }
        }

        chits[0] = new Chit(100, 100, 60, "Ogre.gif");
        chits[1] = new Chit(150, 150, 60, "Lion.gif");
        chits[2] = new Chit(200, 200, 60, "Angel.gif");
        chits[3] = new Chit(250, 250, 60, "Gargoyle.gif");
        chits[4] = new Chit(300, 300, 60, "Behemoth.gif");
        chits[5] = new Chit(350, 350, 60, "Serpent.gif");
        chits[6] = new Chit(400, 400, 60, "Troll.gif");
        chits[7] = new Chit(450, 450, 60, "Giant.gif");

        tracking = -1;
    }


    public void paint(Graphics g)
    {
        update(g);
    }

    public void update(Graphics g)
    {
        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                if (show[i][j])   // need to do only if in invalid rectangle
                {
                    System.out.println("drawing h[" + i + "][" + j + "]");
                    h[i][j].paint(g);
                }
            }
        }

        // Draw chits from low end of array (top of Z-order), de-
        // invalidating as we draw each chit so that element 0 is
        // on top even though it's drawn first.  This should
        // eliminate flicker caused by back objects being briefly
        // drawn then overlaid by front objects.

        for (int i = 0; i < chits.length; i++)
        {
            System.out.println("Drawing chits[" + i + "]");
            chits[i].paint(g);
            // Rectangle rectClip = new Rectangle(g.getClipBounds());
            // rectClip.add(chits[i].getBounds());
            // g.setClip(rectClip);
        }
    }



    void select(Point point)
    {
        Rectangle rect;
        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                if (show[i][j])
                {
                    rect = h[i][j].getBounds();
                    if (rect.contains(point))
                    {
                        System.out.println("Calling select for h[" + i + "][" + j +"]");
                        h[i][j].select(point);
                    }
                }
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
                    repaint();
                }

                return false;
            }
        }

        // No hits on chits, so check map.
        select(point);
        repaint();
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

    public void paint(Graphics g)
    {
        update(g);
    }

    public void update(Graphics g)
    {
        //map.update(g);
    }

    public static void main(String args[])
    {
        BattleMap window = new BattleMap();
        window.setTitle("BattleMap");
        window.pack();
	window.setVisible(true);
    }

}
