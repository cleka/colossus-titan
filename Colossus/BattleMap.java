/** Attempted port of hexmap from MFC to Java
    dripton  12/10/97 */

import java.awt.*;


class Hex implements Shape
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
        System.out.println("painting a Hex");
        if (selected == true)
        {
            g.setColor(java.awt.Color.red);
            g.fillPolygon(p);
            g.drawPolygon(p);
        }
        else
        {
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



class Chit implements Shape
{
    protected boolean selected;
    private Rectangle rect;
    private Image image;

    Chit(int cx, int cy, int scale, String image_filename)
    {
        selected = false;
        rect = new Rectangle(cx, cy, scale, scale);
        Toolkit.getDefaultToolkit().getImage(image_filename);
    }


    void paint(Graphics g)
    {    
          //g.drawImage(image, 0, 0, this);
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
        
        return selected;
    }


    void move(Point point)
    {
        Rectangle tmprect = rect;
        rect.setLocation(point);
        tmprect = tmprect.union(rect);
        // set invalid rectangle
    }
    

    public Rectangle getBounds()
    {
        return (Rectangle) rect;
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
    
        chits[0] = new Chit(100, 100, 60, "ogre.gif");
        chits[1] = new Chit(150, 150, 60, "lion.gif");
        chits[2] = new Chit(200, 200, 60, "angel.gif");
        chits[3] = new Chit(250, 250, 60, "gargoyle.gif");
        chits[4] = new Chit(300, 300, 60, "behemoth.gif");
        chits[5] = new Chit(350, 350, 60, "serpent.gif");
        chits[6] = new Chit(400, 400, 60, "troll.gif");
        chits[7] = new Chit(450, 450, 60, "giant.gif");

        tracking = -1;
    }
    
    
    public void paint(Graphics g)
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
    
        for (int i=0; i < chits.length; i++)
        {
            Rectangle rectChit = new Rectangle(chits[i].getBounds());
            
            System.out.println("Drawing chits[" + i + "]");
            chits[i].paint(g);
                
            // Now remove the area just painted by the chit from
            // what's left of the invalid region.
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
 
    public boolean handleEvent(Event event)
    {
        //System.out.println("HexMap got event " + event.toString());

        if (event.id == event.MOUSE_DOWN)
        {
            mouseDown(event.x,event.y);
        }
        else if (event.id == event.MOUSE_UP)
        {
            mouseUp();
        }
        else if (event.id == event.MOUSE_MOVE)
        {
            mouseMove(event.x,event.y);
        }

        return super.handleEvent(event);
    }

  
    void mouseDown(int x, int y)
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
                }
    
                return;
            }
        }
    
        // No hits on chits, so check map.
        select(point);
    }
    
    
    void mouseUp()
    {
        tracking = -1;
    }
    
    
    void mouseMove(int x, int y)
    {
        Point point = new Point(x,y);
        if (tracking != -1) 
        {
            chits[tracking].move(point);
        }
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
        map.paint(g);
    }        

    public static void main(String args[])
    {
        BattleMap window = new BattleMap();
        window.setTitle("BattleMap");
        window.pack();
	window.setVisible(true);
    }

}
