/* 
 * MasterBoard, for Titan
 * version $Id$
 * dripton
 */

// TODO: Add the dragon and hydra.
// TODO: Restrict chit dragging to within window

import java.awt.*;
import java.awt.event.*;

/**
 * Class MasterBoard implements the GUI for a Titan
 * masterboard.
 * @version $Id$
 * @author David Ripton
 */


public class MasterBoard extends Frame implements MouseListener,
    MouseMotionListener
{
    // There are a total of 96 hexes 
    // Their Titan labels are:
    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

    // For easy of mapping to the GUI, they'll be stored
    // in a 15x8 array, with some empty elements.

    private MasterHex[][] h = new MasterHex[15][8];
    private Chit[] chits = new Chit[22];
    private int tracking;
    final private boolean[][] show =
    {
        {false, false, false, true, true, false, false, false},
        {false, false, true, true, true, true, false, false},
        {false, true, true, true, true, true, true, false},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {false, true, true, true, true, true, true, false},
        {false, false, true, true, true, true, false, false},
        {false, false, false, true, true, false, false, false},
    };
    private Rectangle rectClip = new Rectangle();
    private Image offImage;
    private Graphics gBack;
    private Dimension offDimension;
    private boolean needToClear;
    private MediaTracker tracker;
    private boolean imagesLoaded;

    public MasterBoard()
    {
        super("MasterBoard");

        int cx=80;
        int cy=80;
        int scale=15;
        
        //System.out.println("Creating a MasterBoard.");

        pack();
        setSize(700, 700);
        setBackground(java.awt.Color.white);
        setVisible(true);
        addWindowListener(new InnerWindowAdapter());
        addMouseListener(this);
        addMouseMotionListener(this);
        
        tracking = -1;
        needToClear = false;
        imagesLoaded = false;

        // Initialize hexes
        for (int i = 0; i < 15; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new MasterHex
                        ((int) java.lang.Math.round(cx + 4 * i * scale),
                        (int) java.lang.Math.round(cy + (3 * j + (i % 2) * 
                        (1 + 2 * (j / 2)) + ((i + 1) % 2) * 2 * ((j + 1) / 2))
                        * java.lang.Math.sqrt(3.0) * scale), scale,
                        (i + j) % 2 == 0);
                }
            }
        }

        tracker = new MediaTracker(this);

        chits[0] = new Chit(100, 100, 60, "images/Angel.gif", this);
        chits[1] = new Chit(120, 120, 60, "images/Archangel.gif", this);
        chits[2] = new Chit(140, 140, 60, "images/Behemoth.gif", this);
        chits[3] = new Chit(160, 160, 60, "images/Centaur.gif", this);
        chits[4] = new Chit(180, 180, 60, "images/Colossus.gif", this);
        chits[5] = new Chit(200, 200, 60, "images/Cyclops.gif", this);
        chits[6] = new Chit(220, 220, 60, "images/Gargoyle.gif", this);
        chits[7] = new Chit(240, 240, 60, "images/Giant.gif", this);
        chits[8] = new Chit(260, 260, 60, "images/Gorgon.gif", this);
        chits[9] = new Chit(280, 280, 60, "images/Griffon.gif", this);
        chits[10] = new Chit(300, 300, 60, "images/Guardian.gif", this);
        chits[11] = new Chit(320, 320, 60, "images/Hydra.gif", this);
        chits[12] = new Chit(340, 340, 60, "images/Lion.gif", this);
        chits[13] = new Chit(360, 360, 60, "images/Minotaur.gif", this);
        chits[14] = new Chit(380, 380, 60, "images/Ogre.gif", this);
        chits[15] = new Chit(400, 400, 60, "images/Ranger.gif", this);
        chits[16] = new Chit(420, 420, 60, "images/Serpent.gif", this);
        chits[17] = new Chit(460, 460, 60, "images/Titan.gif", this);
        chits[18] = new Chit(480, 480, 60, "images/Troll.gif", this);
        chits[19] = new Chit(500, 500, 60, "images/Unicorn.gif", this);
        chits[20] = new Chit(520, 520, 60, "images/Warbear.gif", this);
        chits[21] = new Chit(540, 540, 60, "images/Warlock.gif", this);

        for (int i = 0; i < chits.length; i++)
        {
            tracker.addImage(chits[i].image, 0);
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            System.out.println("waitForAll was interrupted");
        }
        imagesLoaded = true;

        repaint();
    }


    public void mouseDragged(MouseEvent e)
    {
        Point point = e.getPoint();
        if (tracking != -1)
        {
            Rectangle clip = new Rectangle(chits[tracking].getBounds());
            chits[tracking].setLocation(point);
            clip.add(chits[tracking].getBounds());
            needToClear = true;
            repaint(clip.x, clip.y, clip.width, clip.height);
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        tracking = -1;
    }
    
    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i=0; i < chits.length; i++)
        {
            if (chits[i].select(point))
            {
                //System.out.println("Selected chits[" + i +"]");
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
                    Rectangle clip = new Rectangle(chits[0].getBounds());
                    repaint(clip.x, clip.y, clip.width, clip.height);
                }
                return;
            }
        }

        // No hits on chits, so check map.
        for (int i = 0; i < h.length; i++)
        {
            //XXX
            for (int j = 0; j < 8; j++)
            {
                if (show[i][j] && h[i][j].contains(point))
                {
                    //System.out.println("Calling select for h[" + i + 
                    //    "][" + j +"]");
                    h[i][j].select(point);
                    Rectangle clip = new Rectangle(h[i][j].getBounds());
                    repaint(clip.x, clip.y, clip.width, clip.height);
                    return;
                }
            }
        }
    }

    public void mouseMoved(MouseEvent e)
    {
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }


    public void paint(Graphics g)
    {
        //System.out.println("Called MasterBoard.paint()");
        if (!imagesLoaded)
        {
            //System.out.println("Images are not loaded yet");
            return;
        }

        rectClip = g.getClipBounds();
        //System.out.println("rectClip: " + rectClip.x + " " + rectClip.y 
        //    + " " + rectClip.width + " " + rectClip.height);

        for (int i = 0; i < h.length; i++)
        {
            //XXX
            for (int j = 0; j < 8; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    //System.out.println("drawing h[" + i + "][" + j + "]");
                    h[i][j].paint(g);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = chits.length - 1; i >= 0; i--)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                //System.out.println("Drawing chits[" + i + "]");
                chits[i].paint(g);
            }
        }
    }

    public void update(Graphics g)
    {
        //System.out.println("Called MasterBoard.update()");

        Dimension d = getSize();
        rectClip = g.getClipBounds();
        
        // Create the back buffer only if we don't have a good one.
        if (gBack == null || d.width != offDimension.width || 
            d.height != offDimension.height)
        {
            //System.out.println("Creating a new back buffer");
            offDimension = d;
            offImage = createImage(d.width, d.height);
            gBack = offImage.getGraphics();
        }

        // XXX: Find out which is faster, this needToClear business
        //      or just clearing the whole back buffer every time.

        // Clear the background only when chits are dragged.
        if (needToClear)
        {
            gBack.setColor(getBackground());
            gBack.fillRect(rectClip.x, rectClip.y, rectClip.width, 
                rectClip.height);
            gBack.setColor(getForeground());
            needToClear = false;
        }

        for (int i = 0; i < h.length; i++)
        {
            //XXX
            for (int j = 0; j < 8; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    //System.out.println("drawing h[" + i + "][" + j + "]");
                    h[i][j].paint(gBack);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = chits.length - 1; i >= 0; i--)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                //System.out.println("Drawing chits[" + i + "]");
                chits[i].paint(gBack);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }

    
    public Dimension getMinimumSize()
    {
        return new Dimension(400, 400);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(700, 700);
    }


    public static void main(String args[])
    {
        MasterBoard masterboard = new MasterBoard();
    }


    class InnerWindowAdapter extends WindowAdapter 
    {
        public void windowClosing(WindowEvent event)
        {
            System.exit(0);
        }
    }
}


class MasterHex
{
    private boolean selected;
    private int[] x_vertex = new int[6];
    private int[] y_vertex = new int[6];
    private Polygon p;
    private Rectangle rectBound;


    MasterHex(int cx, int cy, int scale, boolean inverted)
    {
        selected = false;
        if (!inverted)
        {
            x_vertex[0] = cx;
            y_vertex[0] = cy;
            x_vertex[1] = cx + 2 * scale;
            y_vertex[1] = cy;
            x_vertex[2] = cx + 4 * scale;
            y_vertex[2] = cy + (int)java.lang.Math.round(2 * java.lang.Math.sqrt(3.0)
                            * scale);
            x_vertex[3] = cx + 3 * scale;
            y_vertex[3] = cy + (int)java.lang.Math.round(3 * 
                            java.lang.Math.sqrt(3.0) * scale);
            x_vertex[4] = cx - scale;
            y_vertex[4] = cy + (int)java.lang.Math.round(3 * 
                            java.lang.Math.sqrt(3.0) * scale);
            x_vertex[5] = cx - 2 * scale;
            y_vertex[5] = cy + (int)java.lang.Math.round(2 * java.lang.Math.sqrt(3.0)
                            * scale);
        }
        else
        {
            x_vertex[0] = cx - scale;
            y_vertex[0] = cy;
            x_vertex[1] = cx + 3 * scale;
            y_vertex[1] = cy;
            x_vertex[2] = cx + 4 * scale;
            y_vertex[2] = cy + (int)java.lang.Math.round(java.lang.Math.sqrt(3.0)
                            * scale);
            x_vertex[3] = cx + 2 * scale;
            y_vertex[3] = cy + (int)java.lang.Math.round(3 * 
                            java.lang.Math.sqrt(3.0) * scale);
            x_vertex[4] = cx;
            y_vertex[4] = cy + (int)java.lang.Math.round(3 * 
                            java.lang.Math.sqrt(3.0) * scale);
            x_vertex[5] = cx - 2 * scale;
            y_vertex[5] = cy + (int)java.lang.Math.round(java.lang.Math.sqrt(3.0)
                            * scale);
        }

        p = new Polygon(x_vertex, y_vertex, 6);
        rectBound = new Rectangle(x_vertex[5], y_vertex[0], x_vertex[2] - 
                        x_vertex[5], y_vertex[3] - y_vertex[0]);
    }


    public void paint(Graphics g)
    {
        if (selected)
        {
            g.setColor(java.awt.Color.red);
            g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
        }
        else
        {
            g.setColor(java.awt.Color.green);
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
    
}


class Chit
{
    // The container's MediaTracker needs to access the image.
    Image image;

    private boolean selected;
    private Rectangle rect;
    private Container container;

    // offset of the mouse cursor within the chit.
    private int dx;
    private int dy;


    Chit(int cx, int cy, int scale, String image_filename, 
        Container my_container)
    {
        selected = false;
        rect = new Rectangle(cx, cy, scale, scale);
        image = Toolkit.getDefaultToolkit().getImage(image_filename);
        container = my_container;
        dx = 0;
        dy = 0;
    }
    
    public void paint(Graphics g)
    {
        g.drawImage(image, rect.x, rect.y, container);
    }

    boolean select(Point point)
    {
        if (rect.contains(point))
        {
            selected = true;
            dx = point.x - rect.x;
            dy = point.y - rect.y;
        }
        else
        {
            selected = false;
        }
        return selected;
    }

    void setLocation(Point point)
    {
        point.x -= dx;
        point.y -= dy;
        rect.setLocation(point);
    }

    public Rectangle getBounds()
    {
        return rect;
    }

}

