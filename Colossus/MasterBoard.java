import java.awt.*;
import java.awt.event.*;

/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
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

    public final double SQRT3 = 1.73205080757;

    private MasterHex[][] h = new MasterHex[15][8];
    private Chit[] chits = new Chit[4];
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
    private int scale;

    public MasterBoard()
    {
        super("MasterBoard");

        scale = 15;
        int cx = 6 * scale;
        int cy = 6 * scale;

        pack();
        setSize(69 * scale, 69 * scale);
        setBackground(java.awt.Color.black);
        setVisible(true);
        addWindowListener(new InnerWindowAdapter());
        addMouseListener(this);
        addMouseMotionListener(this);

        tracking = -1;
        needToClear = false;
        imagesLoaded = false;

        // Initialize hexes
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new MasterHex
                        (cx + 4 * i * scale,
                        (int) Math.round(cy + (3 * j + (i % 2) *
                        (1 + 2 * (j / 2)) + ((i + 1) % 2) * 2 * ((j + 1) / 2))
                        * SQRT3 * scale), scale, (i + j) % 2 == 0);
                }
            }
        }

        // Add terrain types, id labels, and exits to hexes
        h[0][3].terrain='S';
        h[0][3].label=132;
        h[0][3].exitType[1]=4;

        h[0][4].terrain='P';
        h[0][4].label=133;
        h[0][4].exitType[0]=4;

        h[1][2].terrain='B';
        h[1][2].label=130;
        h[1][2].exitType[1]=4;

        h[1][3].terrain='M';
        h[1][3].label=131;
        h[1][3].exitType[0]=4;
        h[1][3].exitType[2]=2;

        h[1][4].terrain='B';
        h[1][4].label=134;
        h[1][4].exitType[1]=2;
        h[1][4].exitType[5]=4;

        h[1][5].terrain='J';
        h[1][5].label=135;
        h[1][5].exitType[0]=4;

        h[2][1].terrain='D';
        h[2][1].label=128;
        h[2][1].exitType[1]=4;

        h[2][2].terrain='P';
        h[2][2].label=129;
        h[2][2].exitType[0]=4;
        h[2][2].exitType[2]=2;

        h[2][3].terrain='H';
        h[2][3].label=32;
        h[2][3].exitType[3]=4;
        h[2][3].exitType[5]=1;

        h[2][4].terrain='J';
        h[2][4].label=33;
        h[2][4].exitType[2]=4;
        h[2][4].exitType[4]=1;

        h[2][5].terrain='M';
        h[2][5].label=136;
        h[2][5].exitType[1]=2;
        h[2][5].exitType[5]=4;

        h[2][6].terrain='B';
        h[2][6].label=137;
        h[2][6].exitType[0]=4;

        h[3][0].terrain='M';
        h[3][0].label=126;
        h[3][0].exitType[1]=4;

        h[3][1].terrain='B';
        h[3][1].label=127;
        h[3][1].exitType[0]=4;
        h[3][1].exitType[2]=2;

        h[3][2].terrain='T';
        h[3][2].label=500;
        h[3][2].exitType[1]=3;
        h[3][2].exitType[3]=3;
        h[3][2].exitType[5]=3;

        h[3][3].terrain='B';
        h[3][3].label=31;
        h[3][3].exitType[0]=2;
        h[3][3].exitType[4]=4;

        h[3][4].terrain='P';
        h[3][4].label=34;
        h[3][4].exitType[1]=4;
        h[3][4].exitType[3]=2;

        h[3][5].terrain='T';
        h[3][5].label=600;
        h[3][5].exitType[0]=3;
        h[3][5].exitType[2]=3;
        h[3][5].exitType[4]=3;

        h[3][6].terrain='P';
        h[3][6].label=138;
        h[3][6].exitType[1]=2;
        h[3][6].exitType[5]=4;

        h[3][7].terrain='D';
        h[3][7].label=139;
        h[3][7].exitType[0]=4;

        h[4][0].terrain='J';
        h[4][0].label=125;
        h[4][0].exitType[2]=4;

        h[4][1].terrain='J';
        h[4][1].label=26;
        h[4][1].exitType[3]=4;
        h[4][1].exitType[5]=1;

        h[4][2].terrain='M';
        h[4][2].label=27;
        h[4][2].exitType[2]=4;
        h[4][2].exitType[4]=2;

        h[4][3].terrain='W';
        h[4][3].label=30;
        h[4][3].exitType[3]=2;
        h[4][3].exitType[5]=4;

        h[4][4].terrain='D';
        h[4][4].label=35;
        h[4][4].exitType[0]=2;
        h[4][4].exitType[2]=4;

        h[4][5].terrain='B';
        h[4][5].label=38;
        h[4][5].exitType[3]=4;
        h[4][5].exitType[5]=2;

        h[4][6].terrain='W';
        h[4][6].label=39;
        h[4][6].exitType[2]=4;
        h[4][6].exitType[4]=1;

        h[4][7].terrain='M';
        h[4][7].label=140;
        h[4][7].exitType[5]=4;

        h[5][0].terrain='P';
        h[5][0].label=124;
        h[5][0].exitType[1]=4;
        h[5][0].exitType[3]=2;

        h[5][1].terrain='W';
        h[5][1].label=25;
        h[5][1].exitType[0]=1;
        h[5][1].exitType[4]=4;

        h[5][2].terrain='S';
        h[5][2].label=28;
        h[5][2].exitType[1]=2;
        h[5][2].exitType[3]=4;

        h[5][3].terrain='P';
        h[5][3].label=29;
        h[5][3].exitType[2]=2;
        h[5][3].exitType[4]=4;

        h[5][4].terrain='M';
        h[5][4].label=36;
        h[5][4].exitType[1]=2;
        h[5][4].exitType[3]=4;

        h[5][5].terrain='H';
        h[5][5].label=37;
        h[5][5].exitType[2]=2;
        h[5][5].exitType[4]=4;

        h[5][6].terrain='J';
        h[5][6].label=40;
        h[5][6].exitType[1]=4;
        h[5][6].exitType[3]=1;

        h[5][7].terrain='B';
        h[5][7].label=141;
        h[5][7].exitType[0]=2;
        h[5][7].exitType[4]=4;

        h[6][0].terrain='B';
        h[6][0].label=123;
        h[6][0].exitType[2]=4;

        h[6][1].terrain='B';
        h[6][1].label=24;
        h[6][1].exitType[1]=2;
        h[6][1].exitType[5]=4;

        h[6][2].terrain='H';
        h[6][2].label=23;
        h[6][2].exitType[0]=4;
        h[6][2].exitType[4]=2;

        h[6][3].terrain='m';
        h[6][3].label=5000;
        h[6][3].exitType[1]=3;
        h[6][3].exitType[3]=3;
        h[6][3].exitType[5]=1;

        h[6][4].terrain='t';
        h[6][4].label=6000;
        h[6][4].exitType[0]=3;
        h[6][4].exitType[2]=3;
        h[6][4].exitType[4]=1;

        h[6][5].terrain='S';
        h[6][5].label=42;
        h[6][5].exitType[1]=4;
        h[6][5].exitType[5]=2;

        h[6][6].terrain='M';
        h[6][6].label=41;
        h[6][6].exitType[0]=4;
        h[6][6].exitType[2]=2;

        h[6][7].terrain='S';
        h[6][7].label=142;
        h[6][7].exitType[5]=4;

        h[7][0].terrain='M';
        h[7][0].label=122;
        h[7][0].exitType[1]=4;
        h[7][0].exitType[3]=2;

        h[7][1].terrain='T';
        h[7][1].label=400;
        h[7][1].exitType[0]=3;
        h[7][1].exitType[2]=3;
        h[7][1].exitType[4]=3;

        h[7][2].terrain='M';
        h[7][2].label=22;
        h[7][2].exitType[3]=2;
        h[7][2].exitType[5]=4;

        h[7][3].terrain='t';
        h[7][3].label=4000;
        h[7][3].exitType[0]=1;
        h[7][3].exitType[2]=3;
        h[7][3].exitType[4]=3;

        h[7][4].terrain='m';
        h[7][4].label=1000;
        h[7][4].exitType[1]=3;
        h[7][4].exitType[3]=1;
        h[7][4].exitType[5]=3;

        h[7][5].terrain='P';
        h[7][5].label=1;
        h[7][5].exitType[0]=2;
        h[7][5].exitType[2]=4;

        h[7][6].terrain='T';
        h[7][6].label=100;
        h[7][6].exitType[1]=3;
        h[7][6].exitType[3]=3;
        h[7][6].exitType[5]=3;

        h[7][7].terrain='P';
        h[7][7].label=101;
        h[7][7].exitType[0]=2;
        h[7][7].exitType[4]=4;

        h[8][0].terrain='S';
        h[8][0].label=121;
        h[8][0].exitType[2]=4;

        h[8][1].terrain='P';
        h[8][1].label=20;
        h[8][1].exitType[3]=4;
        h[8][1].exitType[5]=2;

        h[8][2].terrain='D';
        h[8][2].label=21;
        h[8][2].exitType[2]=2;
        h[8][2].exitType[4]=4;

        h[8][3].terrain='m';
        h[8][3].label=3000;
        h[8][3].exitType[1]=1;
        h[8][3].exitType[3]=3;
        h[8][3].exitType[5]=3;

        h[8][4].terrain='t';
        h[8][4].label=2000;
        h[8][4].exitType[0]=3;
        h[8][4].exitType[2]=1;
        h[8][4].exitType[4]=3;

        h[8][5].terrain='W';
        h[8][5].label=2;
        h[8][5].exitType[1]=2;
        h[8][5].exitType[3]=4;

        h[8][6].terrain='B';
        h[8][6].label=3;
        h[8][6].exitType[2]=4;
        h[8][6].exitType[4]=2;

        h[8][7].terrain='B';
        h[8][7].label=102;
        h[8][7].exitType[5]=4;

        h[9][0].terrain='B';
        h[9][0].label=120;
        h[9][0].exitType[1]=4;
        h[9][0].exitType[3]=2;

        h[9][1].terrain='J';
        h[9][1].label=19;
        h[9][1].exitType[0]=1;
        h[9][1].exitType[4]=4;

        h[9][2].terrain='W';
        h[9][2].label=16;
        h[9][2].exitType[1]=4;
        h[9][2].exitType[5]=2;

        h[9][3].terrain='P';
        h[9][3].label=15;
        h[9][3].exitType[0]=4;
        h[9][3].exitType[4]=2;

        h[9][4].terrain='M';
        h[9][4].label=8;
        h[9][4].exitType[1]=4;
        h[9][4].exitType[5]=2;

        h[9][5].terrain='D';
        h[9][5].label=7;
        h[9][5].exitType[0]=4;
        h[9][5].exitType[4]=2;

        h[9][6].terrain='H';
        h[9][6].label=4;
        h[9][6].exitType[1]=4;
        h[9][6].exitType[3]=1;

        h[9][7].terrain='M';
        h[9][7].label=103;
        h[9][7].exitType[0]=2;
        h[9][7].exitType[4]=4;

        h[10][0].terrain='P';
        h[10][0].label=119;
        h[10][0].exitType[2]=4;

        h[10][1].terrain='H';
        h[10][1].label=18;
        h[10][1].exitType[1]=1;
        h[10][1].exitType[5]=4;

        h[10][2].terrain='B';
        h[10][2].label=17;
        h[10][2].exitType[0]=4;
        h[10][2].exitType[2]=2;

        h[10][3].terrain='S';
        h[10][3].label=14;
        h[10][3].exitType[3]=2;
        h[10][3].exitType[5]=4;

        h[10][4].terrain='H';
        h[10][4].label=9;
        h[10][4].exitType[0]=2;
        h[10][4].exitType[2]=4;

        h[10][5].terrain='P';
        h[10][5].label=6;
        h[10][5].exitType[1]=2;
        h[10][5].exitType[5]=4;

        h[10][6].terrain='J';
        h[10][6].label=5;
        h[10][6].exitType[0]=4;
        h[10][6].exitType[2]=1;

        h[10][7].terrain='J';
        h[10][7].label=104;
        h[10][7].exitType[5]=4;

        h[11][0].terrain='D';
        h[11][0].label=118;
        h[11][0].exitType[3]=4;

        h[11][1].terrain='M';
        h[11][1].label=117;
        h[11][1].exitType[2]=4;
        h[11][1].exitType[4]=2;

        h[11][2].terrain='T';
        h[11][2].label=300;
        h[11][2].exitType[1]=3;
        h[11][2].exitType[3]=3;
        h[11][2].exitType[5]=3;

        h[11][3].terrain='M';
        h[11][3].label=13;
        h[11][3].exitType[0]=2;
        h[11][3].exitType[4]=4;

        h[11][4].terrain='B';
        h[11][4].label=10;
        h[11][4].exitType[1]=4;
        h[11][4].exitType[3]=2;

        h[11][5].terrain='T';
        h[11][5].label=200;
        h[11][5].exitType[0]=3;
        h[11][5].exitType[2]=3;
        h[11][5].exitType[4]=3;

        h[11][6].terrain='B';
        h[11][6].label=106;
        h[11][6].exitType[3]=4;
        h[11][6].exitType[5]=2;

        h[11][7].terrain='P';
        h[11][7].label=105;
        h[11][7].exitType[4]=4;

        h[12][1].terrain='B';
        h[12][1].label=116;
        h[12][1].exitType[3]=4;

        h[12][2].terrain='P';
        h[12][2].label=115;
        h[12][2].exitType[2]=4;
        h[12][2].exitType[4]=2;

        h[12][3].terrain='J';
        h[12][3].label=12;
        h[12][3].exitType[1]=1;
        h[12][3].exitType[5]=4;

        h[12][4].terrain='W';
        h[12][4].label=11;
        h[12][4].exitType[0]=4;
        h[12][4].exitType[2]=1;

        h[12][5].terrain='M';
        h[12][5].label=108;
        h[12][5].exitType[3]=4;
        h[12][5].exitType[5]=2;

        h[12][6].terrain='D';
        h[12][6].label=107;
        h[12][6].exitType[4]=4;

        h[13][2].terrain='J';
        h[13][2].label=114;
        h[13][2].exitType[3]=4;

        h[13][3].terrain='B';
        h[13][3].label=113;
        h[13][3].exitType[2]=4;
        h[13][3].exitType[4]=2;

        h[13][4].terrain='P';
        h[13][4].label=110;
        h[13][4].exitType[3]=4;
        h[13][4].exitType[5]=2;

        h[13][5].terrain='B';
        h[13][5].label=109;
        h[13][5].exitType[4]=4;

        h[14][3].terrain='M';
        h[14][3].label=112;
        h[14][3].exitType[3]=4;

        h[14][4].terrain='S';
        h[14][4].label=111;
        h[14][4].exitType[4]=4;

        // Derive entrances from exits
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 6; k++)
                    {
                        int gateType = h[i][j].exitType[k];
                        if (gateType != 0)
                        {
                            switch(k)
                            {
                                case 0:
                                    h[i][j - 1].entranceType[3] = gateType;
                                    break;
                                case 1:
                                    h[i + 1][j].entranceType[4] = gateType;
                                    break;
                                case 2:
                                    h[i + 1][j].entranceType[5] = gateType;
                                    break;
                                case 3:
                                    h[i][j + 1].entranceType[0] = gateType;
                                    break;
                                case 4:
                                    h[i - 1][j].entranceType[1] = gateType;
                                    break;
                                case 5:
                                    h[i - 1][j].entranceType[2] = gateType;
                                break;
                            }
                        }
                    }
                }
            }
        }


        tracker = new MediaTracker(this);

        chits[0] = new Chit(100, 100, 60, "images/Bk01.gif", this);
        chits[1] = new Chit(120, 120, 60, "images/Bk04.gif", this);
        chits[2] = new Chit(140, 140, 60, "images/Rd08.gif", this);
        chits[3] = new Chit(160, 160, 60, "images/Rd12.gif", this);

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
        if (tracking != -1)
        {
            Point point = e.getPoint();
            point.x = Math.max(point.x, 30);
            point.y = Math.max(point.y, 60);
            point.x = Math.min(point.x, getSize().width - 30);
            point.y = Math.min(point.y, getSize().height - 30);
        
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
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].contains(point))
                {
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
        if (!imagesLoaded)
        {
            return;
        }

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
        Dimension d = getSize();
        rectClip = g.getClipBounds();

        // Create the back buffer only if we don't have a good one.
        if (gBack == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(d.width, d.height);
            gBack = offImage.getGraphics();
        }

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
        return new Dimension(65 * scale, 65 * scale);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(65 * scale, 65 * scale);
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


/**
 * Class MasterHex describes one Masterboard hex
 * @version $Id$
 * @author David Ripton
 */

class MasterHex
{
    public static final double SQRT3 = 1.73205080757;
    private boolean selected;
    private int[] xVertex = new int[6];
    private int[] yVertex = new int[6];
    private Polygon p;
    private Rectangle rectBound;
    private boolean inverted;
    private int scale;
    private double l;              // hexside length

    // B,D,H,J,m,M,P,S,T,t,W
    // Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
    // Swamp, Tower, tundra, Woods
    char terrain;

    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000
    int label;

    // n, ne, se, s, sw, nw
    // 0=none, 1=block, 2=arch, 3=arrow 4=arrows
    int[] exitType = new int[6];
    int[] entranceType = new int[6];

    private int x0;                // first focus point
    private int y0;
    private int x1;                // second focus point
    private int y1;
    private double theta;          // gate angle
    private int x[] = new int[4];  // gate points
    private int y[] = new int[4];


    MasterHex(int cx, int cy, int inScale, boolean inInverted)
    {
        selected = false;
        inverted = inInverted;
        scale = inScale;
        l = scale / 3.0;
        if (!inverted)
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
        else
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

        p = new Polygon(xVertex, yVertex, 6);
        // Add 1 to width and height because Java rectangles come up
        // one pixel short of the area actually painted.
        rectBound = new Rectangle(xVertex[5], yVertex[0], xVertex[2] -
                        xVertex[5] + 1, yVertex[3] - yVertex[0] + 1);
    }


    public void paint(Graphics g)
    {
        if (selected)
        {
            g.setColor(java.awt.Color.white);
        }
        else
        {
            g.setColor(getTerrainColor());
        }

        g.fillPolygon(p);
        g.setColor(java.awt.Color.black);
        g.drawPolygon(p);

        FontMetrics fontMetrics = g.getFontMetrics();

        if (inverted)
        {
            g.drawString(Integer.toString(label), 
                rectBound.x + rectBound.width / 2 - 
                fontMetrics.stringWidth(Integer.toString(label)) / 2,
                rectBound.y + rectBound.height / 3);
            g.drawString(getTerrainName(), rectBound.x + rectBound.width / 2 -
                fontMetrics.stringWidth(getTerrainName()) / 2,
                rectBound.y + rectBound.height * 2 / 3);
        }
        else
        {
            g.drawString(getTerrainName(), rectBound.x + rectBound.width / 2 -
                fontMetrics.stringWidth(getTerrainName()) / 2,
                rectBound.y + rectBound.height * 2 / 3);
            g.drawString(Integer.toString(label), 
                rectBound.x + rectBound.width / 2 -
                fontMetrics.stringWidth(Integer.toString(label)) / 2,
                rectBound.y + rectBound.height / 3);
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

            if (exitType[i] != 0)
            {
                x0 = xVertex[i] + ((xVertex[n] - xVertex[i]) / 6);
                y0 = yVertex[i] + ((yVertex[n] - yVertex[i]) / 6);
                x1 = xVertex[i] + ((xVertex[n] - xVertex[i]) / 3);
                y1 = yVertex[i] + ((yVertex[n] - yVertex[i]) / 3);

                theta = Math.atan2(yVertex[n] - yVertex[i],
                              xVertex[n] - xVertex[i]);

                switch(exitType[i])
                {
                    case 1:   // block
                        x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                        y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                        x[1] = (int) Math.round(x0 + l * Math.sin(theta));
                        y[1] = (int) Math.round(y0 - l * Math.cos(theta));
                        x[2] = (int) Math.round(x1 + l * Math.sin(theta));
                        y[2] = (int) Math.round(y1 - l * Math.cos(theta));
                        x[3] = (int) Math.round(x1 - l * Math.sin(theta));
                        y[3] = (int) Math.round(y1 + l * Math.cos(theta));

                        g.setColor(java.awt.Color.white);
                        g.fillPolygon(x, y, 4);
                        g.setColor(java.awt.Color.black);
                        g.drawPolyline(x, y, 4);
                        break;

                    case 2:   // arch
                        x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                        y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                        x[1] = (int) Math.round(x0 + l * Math.sin(theta));
                        y[1] = (int) Math.round(y0 - l * Math.cos(theta));
                        x[2] = (int) Math.round(x1 + l * Math.sin(theta));
                        y[2] = (int) Math.round(y1 - l * Math.cos(theta));
                        x[3] = (int) Math.round(x1 - l * Math.sin(theta));
                        y[3] = (int) Math.round(y1 + l * Math.cos(theta));
                        Polygon p = new Polygon(x, y, 4);
                        Rectangle rect = p.getBounds();
    
                        g.setColor(java.awt.Color.white);
                        g.fillArc(rect.x, rect.y, rect.width, rect.height,
                             (int) Math.round((2 * Math.PI - theta) * 180 / Math.PI), 
                             180);
                        g.setColor(java.awt.Color.black);
                        g.drawArc(rect.x, rect.y, rect.width, rect.height,
                             (int) Math.round((2 * Math.PI - theta) * 180 / Math.PI), 
                             180);
                        break;

                    case 3:   // 1 arrow
                        x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                        y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                        x[1] = (int) Math.round((x0 + x1) / 2 + l * Math.sin(theta));
                        y[1] = (int) Math.round((y0 + y1) / 2 - l * Math.cos(theta));
                        x[2] = (int) Math.round(x1 - l * Math.sin(theta));
                        y[2] = (int) Math.round(y1 + l * Math.cos(theta));

                        g.setColor(java.awt.Color.white);
                        g.fillPolygon(x, y, 3);
                        g.setColor(java.awt.Color.black);
                        g.drawPolyline(x, y, 3);
                        break;

                    case 4:   // 3 arrows
                        x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                        y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                        x[1] = (int) Math.round((x0 + x1) / 2 + l * 
                               Math.sin(theta));
                        y[1] = (int) Math.round((y0 + y1) / 2 - l * 
                               Math.cos(theta));
                        x[2] = (int) Math.round(x1 - l * Math.sin(theta));
                        y[2] = (int) Math.round(y1 + l * Math.cos(theta));

                        g.setColor(java.awt.Color.white);
                        g.fillPolygon(x, y, 3);
                        g.setColor(java.awt.Color.black);
                        g.drawPolyline(x, y, 3);

                        for (int j = 1; j < 3; j++)
                        {
                            x0 = xVertex[i] + ((xVertex[n] - xVertex[i]) *
                                 (2 + 3 * j) / 12);
                            y0 = yVertex[i] + ((yVertex[n] - yVertex[i]) *
                                 (2 + 3 * j) / 12);

                            x1 = xVertex[i] + ((xVertex[n] - xVertex[i]) *
                                 (4 + 3 * j) / 12);
                            y1 = yVertex[i] + ((yVertex[n] - yVertex[i]) *
                                 (4 + 3 * j) / 12);

                            x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                            y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                            x[1] = (int) Math.round((x0 + x1) / 2 + l * 
                                   Math.sin(theta));
                            y[1] = (int) Math.round((y0 + y1) / 2 - l * 
                                   Math.cos(theta));
                            x[2] = (int) Math.round(x1 - l * Math.sin(theta));
                            y[2] = (int) Math.round(y1 + l * Math.cos(theta));
    
                            g.setColor(java.awt.Color.white);
                            g.fillPolygon(x, y, 3);
                            g.setColor(java.awt.Color.black);
                            g.drawPolyline(x, y, 3);
                        }
                        break;
                }
            }

            // Draw entrances
            // Unfortunately, since exits extend out into adjacent hexes,
            // they sometimes get overdrawn.  So we need to draw them
            // again from the other hex, as entrances.

            if (entranceType[i] != 0)
            {
                x0 = xVertex[n] + ((xVertex[i] - xVertex[n]) / 6);
                y0 = yVertex[n] + ((yVertex[i] - yVertex[n]) / 6);
                x1 = xVertex[n] + ((xVertex[i] - xVertex[n]) / 3);
                y1 = yVertex[n] + ((yVertex[i] - yVertex[n]) / 3);

                theta = Math.atan2(yVertex[i] - yVertex[n], xVertex[i] - 
                        xVertex[n]);

                switch(entranceType[i])
                {
                    case 1:   // block
                        x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                        y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                        x[1] = (int) Math.round(x0 + l * Math.sin(theta));
                        y[1] = (int) Math.round(y0 - l * Math.cos(theta));
                        x[2] = (int) Math.round(x1 + l * Math.sin(theta));
                        y[2] = (int) Math.round(y1 - l * Math.cos(theta));
                        x[3] = (int) Math.round(x1 - l * Math.sin(theta));
                        y[3] = (int) Math.round(y1 + l * Math.cos(theta));

                        g.setColor(java.awt.Color.white);
                        g.fillPolygon(x, y, 4);
                        g.setColor(java.awt.Color.black);
                        g.drawPolyline(x, y, 4);
                        break;

                    case 2:   // arch
                        x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                        y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                        x[1] = (int) Math.round(x0 + l * Math.sin(theta));
                        y[1] = (int) Math.round(y0 - l * Math.cos(theta));
                        x[2] = (int) Math.round(x1 + l * Math.sin(theta));
                        y[2] = (int) Math.round(y1 - l * Math.cos(theta));
                        x[3] = (int) Math.round(x1 - l * Math.sin(theta));
                        y[3] = (int) Math.round(y1 + l * Math.cos(theta));

                        Polygon p = new Polygon(x, y, 4);
                        Rectangle rect = p.getBounds();

                        g.setColor(java.awt.Color.white);
                        g.fillArc(rect.x, rect.y, rect.width, rect.height,
                             (int) ((2 * Math.PI - theta) * 180 / Math.PI), 180);
                        g.setColor(java.awt.Color.black);
                        g.drawArc(rect.x, rect.y, rect.width, rect.height,
                             (int) ((2 * Math.PI - theta) * 180 / Math.PI), 180);
                        break;

                    case 3:   // 1 arrow
                        x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                        y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                        x[1] = (int) Math.round((x0 + x1) / 2 + l * Math.sin(theta));
                        y[1] = (int) Math.round((y0 + y1) / 2 - l * Math.cos(theta));
                        x[2] = (int) Math.round(x1 - l * Math.sin(theta));
                        y[2] = (int) Math.round(y1 + l * Math.cos(theta));

                        g.setColor(java.awt.Color.white);
                        g.fillPolygon(x, y, 3);
                        g.setColor(java.awt.Color.black);
                        g.drawPolyline(x, y, 3);
                        break;

                    case 4:   // 3 arrows
                        x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                        y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                        x[1] = (int) Math.round((x0 + x1) / 2 + l * 
                               Math.sin(theta));
                        y[1] = (int) Math.round((y0 + y1) / 2 - l *
                               Math.cos(theta));
                        x[2] = (int) Math.round(x1 - l * Math.sin(theta));
                        y[2] = (int) Math.round(y1 + l * Math.cos(theta));

                        g.setColor(java.awt.Color.white);
                        g.fillPolygon(x, y, 3);
                        g.setColor(java.awt.Color.black);
                        g.drawPolyline(x, y, 3);
                    
                        for (int j = 1; j < 3; j++)
                        {
                            x0 = xVertex[n] + ((xVertex[i] -
                                 xVertex[n]) * (2 + 3 * j) / 12);
                            y0 = yVertex[n] + ((yVertex[i] -
                                 yVertex[n]) * (2 + 3 * j) / 12);

                            x1 = xVertex[n] + ((xVertex[i] -
                                 xVertex[n]) * (4 + 3 * j) / 12);
                            y1 = yVertex[n] + ((yVertex[i] -
                                 yVertex[n]) * (4 + 3 * j) / 12);

                            x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                            y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                            x[1] = (int) Math.round((x0 + x1) / 2 + l * 
                                   Math.sin(theta));
                            y[1] = (int) Math.round((y0 + y1) / 2 - l *
                                   Math.cos(theta));
                            x[2] = (int) Math.round(x1 - l * Math.sin(theta));
                            y[2] = (int) Math.round(y1 + l * Math.cos(theta));

                            g.setColor(java.awt.Color.white);
                            g.fillPolygon(x, y, 3);
                            g.setColor(java.awt.Color.black);
                            g.drawPolyline(x, y, 3);
                        }
                        break;
                }
            }
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

    String getTerrainName()
    {
        switch(terrain)
        {
            case 'B':
                return "BRUSH";
            case 'D':
                return "DESERT";
            case 'H':
                return "HILLS";
            case 'J':
                return "JUNGLE";
            case 'm':
                return "MOUNTAINS";
            case 'M':
                return "MARSH";
            case 'P':
                return "PLAINS";
            case 'S':
                return "SWAMP";
            case 'T':
                return "TOWER";
            case 't':
                return "TUNDRA";
            case 'W':
                return "WOODS";
            default:
                return "?????";
        }
    }

    Color getTerrainColor()
    {
        switch(terrain)
        {
            case 'B':
                return java.awt.Color.green;
            case 'D':
                return java.awt.Color.orange;
            case 'H':
                return new Color(128, 64, 0);
            case 'J':
                return new Color(0, 128, 0);
            case 'm':
                return java.awt.Color.red;
            case 'M':
                return new Color(180, 90, 0);
            case 'P':
                return java.awt.Color.yellow;
            case 'S':
                return java.awt.Color.blue;
            case 'T':
                return java.awt.Color.gray;
            case 't':
                return new Color(128, 170, 255);
            case 'W':
                return new Color(128, 128, 0);
            default:
                return java.awt.Color.black;
        }
    }
}
