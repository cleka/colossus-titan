import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public class BattleMap extends Frame implements MouseListener,
    MouseMotionListener, WindowListener, AdjustmentListener,
    ActionListener
{
    public static final double SQRT3 = Math.sqrt(3.0);
    private Hex[][] h = new Hex[6][6];
    private Chit[] chits = new Chit[24];
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
    private Rectangle rectClip = new Rectangle();
    private Image offImage;
    private Graphics gBack;
    private Dimension offDimension;
    private boolean needToClear;
    private MediaTracker tracker;
    private boolean imagesLoaded;

    private int scale;
    private int cx;
    private int cy;
    private int chitScale;
    Dimension preferredSize;
    Scrollbar vBar;
    Scrollbar hBar; 
    int tx;
    int ty;
    MenuBar mb;
    Menu m1;
    MenuItem mi1_1, mi1_2;


    public BattleMap()
    {
        super("BattleMap");

        scale = 17;
        cx = 3 * scale;
        cy = 3 * scale;
        chitScale = (int) Math.round(2.4 * scale);

        preferredSize = new Dimension(28 * scale, 28 * scale);
        
        setSize(preferredSize);
        setBackground(java.awt.Color.white);
        setVisible(true);
        addWindowListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        
        tracking = -1;
        needToClear = false;
        imagesLoaded = false;

        // XXX Scale scrollbars and don't display if not needed
        tx =  0;
        ty =  0;
        Dimension d = getSize();
        vBar = new Scrollbar(Scrollbar.VERTICAL, 0, scale, 0, 
            20 * scale - d.height);
        hBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, scale, 0,
            20 * scale - d.width);
        add("East", vBar);
        add("South", hBar);
        vBar.addAdjustmentListener(this);
        hBar.addAdjustmentListener(this);
        
        mb = new MenuBar();
        m1 = new Menu("Size", false);
        mb.add(m1);
        mi1_1 = new MenuItem("Shrink");
        m1.add(mi1_1);
        mi1_2 = new MenuItem("Grow");
        m1.add(mi1_2);
        setMenuBar(mb);
        mi1_1.addActionListener(this);
        mi1_2.addActionListener(this);

        pack();
        validate();

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new Hex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + i % 2) *
                        SQRT3 * scale), scale);
                }
            }
        }

        tracker = new MediaTracker(this);

        chits[0] = new Chit(1 * scale, 1 * scale, chitScale, 
            "images/Angel.gif", this);
        chits[1] = new Chit(2 * scale, 2 * scale, chitScale,
            "images/Archangel.gif", this);
        chits[2] = new Chit(3 * scale, 3 * scale, chitScale,
            "images/Behemoth.gif", this);
        chits[3] = new Chit(4 * scale, 4 * scale, chitScale,
            "images/Centaur.gif", this);
        chits[4] = new Chit(5 * scale, 5 * scale, chitScale,
            "images/Colossus.gif", this);
        chits[5] = new Chit(6 * scale, 6 * scale, chitScale,
            "images/Cyclops.gif", this);
        chits[6] = new Chit(7 * scale, 7 * scale, chitScale,
            "images/Gargoyle.gif", this);
        chits[7] = new Chit(8 * scale, 8 * scale, chitScale,
            "images/Giant.gif", this);
        chits[8] = new Chit(9 * scale, 9 * scale, chitScale,
            "images/Gorgon.gif", this);
        chits[9] = new Chit(10 * scale, 10 * scale, chitScale,
            "images/Griffon.gif", this);
        chits[10] = new Chit(11 * scale, 11 * scale, chitScale,
            "images/Guardian.gif", this);
        chits[11] = new Chit(12 * scale, 12 * scale, chitScale,
            "images/Hydra.gif", this);
        chits[12] = new Chit(13 * scale, 13 * scale, chitScale,
            "images/Lion.gif", this);
        chits[13] = new Chit(14 * scale, 14 * scale, chitScale,
            "images/Minotaur.gif", this);
        chits[14] = new Chit(15 * scale, 15 * scale, chitScale,
            "images/Ogre.gif", this);
        chits[15] = new Chit(16 * scale, 16 * scale, chitScale,
            "images/Ranger.gif", this);
        chits[16] = new Chit(17 * scale, 17 * scale, chitScale,
            "images/Serpent.gif", this);
        chits[17] = new Chit(18 * scale, 18 * scale, chitScale,
            "images/Titan.gif", this);
        chits[18] = new Chit(19 * scale, 19 * scale, chitScale,
            "images/Troll.gif", this);
        chits[19] = new Chit(20 * scale, 20 * scale, chitScale,
            "images/Unicorn.gif", this);
        chits[20] = new Chit(21 * scale, 21 * scale, chitScale,
            "images/Warbear.gif", this);
        chits[21] = new Chit(22 * scale, 22 * scale, chitScale,
            "images/Warlock.gif", this);
        chits[22] = new Chit(23 * scale, 23 * scale, chitScale,
            "images/Dragon.gif", this);
        chits[23] = new Chit(24 * scale, 24 * scale, chitScale,
            "images/Wyvern.gif", this);

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


    void rescale(int scale)
    {
        int oldscale = this.scale;
        this.scale = scale;
        chitScale = (int) Math.round(2.4 * scale);
        cx = 3 * scale;
        cy = 3 * scale; 
        
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j].rescale
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + i % 2) *
                        SQRT3 * scale), scale);
                }
            }
        }

        for (int i = 0; i < chits.length; i++)
        {
            chits[i].rescale(chitScale);
            Rectangle rect = chits[i].getBounds();
            Point point = new Point((int) Math.round((double) rect.x * 
                scale / oldscale), (int) Math.round((double) rect.y * 
                scale / oldscale));
            chits[i].setLocation(point);
        }

        // XXX Rescale scrollbars

        Dimension d = getSize();
        vBar.setValues(ty, scale, 0, 20 * scale - d.height);
        vBar.setBlockIncrement(scale);
        hBar.setValues(tx, scale, 0, 20 * scale - d.width);
        hBar.setBlockIncrement(scale);

        validate();
        needToClear = true;
        repaint();
    }


    public void mouseDragged(MouseEvent e)
    {
        if (tracking != -1)
        {
            Point point = e.getPoint();
            point.x += tx;
            point.y += ty;

            Rectangle clip = new Rectangle(chits[tracking].getBounds());
            chits[tracking].setLocation(point);
            clip.add(chits[tracking].getBounds());
            needToClear = true;
            repaint(clip.x - tx, clip.y - ty, clip.width, clip.height);
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        tracking = -1;
    }
    
    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        point.x += tx;
        point.y += ty;

        for (int i=0; i < chits.length; i++)
        {
            if (chits[i].select(point))
            {
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
                    repaint(clip.x - tx, clip.y - ty, clip.width, clip.height);
                }
                return;
            }
        }

        // No hits on chits, so check map.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].select(point))
                {
                    Rectangle clip = new Rectangle(h[i][j].getBounds());
                    repaint(clip.x - tx, clip.y - ty, clip.width, clip.height);
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


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        System.exit(0);
    }

    public void windowDeactivated(WindowEvent e)
    {
    }
    
    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }


    public void adjustmentValueChanged(AdjustmentEvent e)
    {
        // Horizontal or vertical?
        if (e.getAdjustable() == hBar)
        {
            int oldtx = tx;
            tx = e.getValue();
            if (tx > oldtx)
            {
                // Need to invalidate newly-visible region at right
                Dimension d = getSize();
                repaint(tx, 0, d.width - tx, d.height);
            }
        }
        else
        {
            int oldty = ty;
            ty = e.getValue();
            if (ty > oldty)
            {
                // Need to invalidate newly-visible region at bottom
                Dimension d = getSize();
                repaint(0, ty, d.width, d.height - ty);
            }
        }

        repaint();
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "Shrink")
        {
            rescale(scale - 1);
        }
        else  // "Grow"
        {
            rescale(scale + 1);            
        }
    }


    public void paint(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        g.translate(-tx, -ty);
        rectClip = g.getClipBounds();

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(g);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = chits.length - 1; i >= 0; i--)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                chits[i].paint(g);
            }
        }
    }

    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();
        
        // Create the back buffer only if we don't have a good one.
        if (gBack == null || d.width != offDimension.width || 
            d.height != offDimension.height)
        {
            offDimension = d;
            // XXX Account for scrolling
            offImage = createImage(2 * d.width, 2 * d.height);
            gBack = offImage.getGraphics();
        }

        g.translate(-tx, -ty);
        rectClip = g.getClipBounds();

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
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(gBack);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = chits.length - 1; i >= 0; i--)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                chits[i].paint(gBack);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }

    
    public Dimension getMinimumSize()
    {
        return new Dimension(480, 480);
    }

    public Dimension getPreferredSize()
    {
        return preferredSize;
    }


    public static void main(String args[])
    {
        BattleMap battlemap = new BattleMap();
    }

}



/**
 * Class Hex describes one Battlemap hex.
 * @version $Id$
 * @author David Ripton
 */

class Hex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    private boolean selected;
    private int[] xVertex = new int[6];
    private int[] yVertex = new int[6];
    private Polygon p;
    private Rectangle rectBound;


    Hex(int cx, int cy, int scale)
    {
        selected = false;

        xVertex[0] = cx;
        yVertex[0] = cy;
        xVertex[1] = cx + 2 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + 3 * scale;
        yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
        xVertex[3] = cx + 2 * scale;
        yVertex[3] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[4] = cx;
        yVertex[4] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[5] = cx - 1 * scale;
        yVertex[5] = cy + (int) Math.round(SQRT3 * scale);

        p = new Polygon(xVertex, yVertex, 6);
        // Add 1 to width and height because Java rectangles come up
        // one pixel short.
        rectBound = new Rectangle(xVertex[5], yVertex[0], xVertex[2] - 
                        xVertex[5] + 1, yVertex[3] - yVertex[0] + 1);
    }

    
    void rescale(int cx, int cy, int scale)
    {
        xVertex[0] = cx;
        yVertex[0] = cy;
        xVertex[1] = cx + 2 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + 3 * scale;
        yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
        xVertex[3] = cx + 2 * scale;
        yVertex[3] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[4] = cx;
        yVertex[4] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[5] = cx - scale;
        yVertex[5] = cy + (int) Math.round(SQRT3 * scale);

        // The hit testing breaks if we just reassign the vertices
        // of the old polygon.
        p = new Polygon(xVertex, yVertex, 6);

        // Add 1 to width and height because Java rectangles come up
        // one pixel short.
        rectBound.x =  xVertex[5];
        rectBound.y =  yVertex[0];
        rectBound.width = xVertex[2] - xVertex[5] + 1;
        rectBound.height = yVertex[3] - yVertex[0] + 1;
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
