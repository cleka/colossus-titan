/* 
 * MasterBoard, for Titan
 * version $Id$
 * dripton
 */

// TODO: Restrict chit dragging to within window
// TODO: Add stack markers
// TODO: Fix colors

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
        
        pack();
        setSize(1000, 1000);
        setBackground(java.awt.Color.white);
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
                        ((int) Math.round(cx + 4 * i * scale),
                        (int) Math.round(cy + (3 * j + (i % 2) * 
                        (1 + 2 * (j / 2)) + ((i + 1) % 2) * 2 * ((j + 1) / 2))
                        * Math.sqrt(3.0) * scale), scale, (i + j) % 2 == 0);
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
        h[3][3].exitType[3]=4;

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
        h[6][0].exitType[1]=4;

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
        h[7][4].exitType[3]=3;
        h[7][4].exitType[5]=1;

        h[7][5].terrain='P';
        h[7][5].label=1;
        h[7][5].exitType[0]=2;
        h[7][5].exitType[2]=4;

        h[7][6].terrain='T';
        h[7][6].label=100;
        h[7][6].exitType[1]=3;
        h[7][6].exitType[3]=3;
        h[7][6].exitType[5]=1;

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
        h[9][1].exitType[4]=2;

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
        h[10][3].exitType[2]=2;
        h[10][3].exitType[4]=4;

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
        h[11][7].exitType[4]=3;

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
        return new Dimension(400, 400);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(1000, 1000);
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
    private int[] xVertex = new int[6];
    private int[] yVertex = new int[6];
    private Polygon p;
    private Rectangle rectBound;

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

    MasterHex(int cx, int cy, int scale, boolean inverted)
    {
        selected = false;
        if (!inverted)
        {
            xVertex[0] = cx;
            yVertex[0] = cy;
            xVertex[1] = cx + 2 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int)Math.round(2 * Math.sqrt(3.0) * scale);
            xVertex[3] = cx + 3 * scale;
            yVertex[3] = cy + (int)Math.round(3 * Math.sqrt(3.0) * scale);
            xVertex[4] = cx - scale;
            yVertex[4] = cy + (int)Math.round(3 * Math.sqrt(3.0) * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int)Math.round(2 * Math.sqrt(3.0) * scale);
        }
        else
        {
            xVertex[0] = cx - scale;
            yVertex[0] = cy;
            xVertex[1] = cx + 3 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int)Math.round(Math.sqrt(3.0) * scale);
            xVertex[3] = cx + 2 * scale;
            yVertex[3] = cy + (int)Math.round(3 * Math.sqrt(3.0) * scale);
            xVertex[4] = cx;
            yVertex[4] = cy + (int)Math.round(3 * Math.sqrt(3.0) * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int)Math.round(Math.sqrt(3.0) * scale);
        }

        p = new Polygon(xVertex, yVertex, 6);
        // Add 1 to width and height because Java rectangles come up
        // one pixel short.
        rectBound = new Rectangle(xVertex[5], yVertex[0], xVertex[2] - 
                        xVertex[5] + 1, yVertex[3] - yVertex[0] + 1);
    }


    // XXX: Pick better colors.
    public void paint(Graphics g)
    {
        // XXX: Change this to something that keeps the basic hex color
        if (selected)
        {
            g.setColor(java.awt.Color.black);
            g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
        }
        else
        {
            switch(terrain)
            {
                case 'B':
                    g.setColor(java.awt.Color.green);
                    break; 
                case 'D':
                    g.setColor(java.awt.Color.orange);
                    break;
                case 'H':
                    g.setColor(java.awt.Color.cyan);
                    break;
                case 'J':
                    g.setColor(java.awt.Color.darkGray);
                    break;
                case 'm':
                    g.setColor(java.awt.Color.red);
                    break;
                case 'M':
                    g.setColor(java.awt.Color.pink);
                    break;
                case 'P':
                    g.setColor(java.awt.Color.yellow);
                    break;
                case 'S':
                    g.setColor(java.awt.Color.blue);
                    break;
                case 'T':
                    g.setColor(java.awt.Color.gray);
                    break;
                case 't':
                    g.setColor(java.awt.Color.lightGray);
                    break;
                case 'W':
                    g.setColor(java.awt.Color.magenta);
                    break;
                default:
                    g.setColor(java.awt.Color.white);
                    break;
            }
            g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
            // XXX scale this
            FontMetrics fontMetrics = g.getFontMetrics();
            g.drawString("" + label, rectBound.x + rectBound.width / 2 -
                fontMetrics.stringWidth("" + label) / 2,
                rectBound.y + rectBound.height / 3);
            g.drawString(getTerrainName(), rectBound.x + rectBound.width / 2 -
                fontMetrics.stringWidth(getTerrainName()) / 2,
                rectBound.y + rectBound.height * 2 / 3);

            // draw exits
            // There are up to 3 gates to draw.  Each is 1/6 of a hexside
            // square.  The first is positioned from 1/6 to 1/3 of the way
            // along the hexside, the second from 5/12 to 7/12, and the 
            // third from 2/3 to 5/6.  The inner edge of each is 1/12 of a
            // hexside inside the hexside, and the outer edge is 1/12 of a
            // hexside outside the hexside.

            for (int i = 0; i < 6; i++)
            {
                switch(exitType[i])
                {
                    case 1:
/*
                        x1 = xVertex[i] + (xVertex[(i + 1) % 6] - xVertex[i]) / 6;
                        y1 = yVertex[i] + (yVertex[(i + 1) % 6] - yVertex[i]) / 6;
                        x2 = 

                        x4 = xVertex[i] + (xVertex[(i + 1) % 6] - xVertex[i]) / 3;
                        y4 = yVertex[i] + (yVertex[(i + 1) % 6] - yVertex[i]) / 3;
*/
                        
                        break;

                    case 2:   // arch
                        break;

                    case 3:   // arrow
                        break;

                    case 4:   // arrows
                        break;

                    case 0:   // none
                    default:
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
                return "brush";
            case 'D':
                return "desert";
            case 'H':
                return "hills";
            case 'J':
                return "jungle";
            case 'm':
                return "mountains";
            case 'M':
                return "marsh";
            case 'P':
                return "plains";
            case 'S':
                return "swamp";
            case 'T':
                return "tower";
            case 't':
                return "tundra";
            case 'W':
                return "woods";
            default:
                return "?????";
        }
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


    Chit(int cx, int cy, int scale, String imageFilename, 
        Container myContainer)
    {
        selected = false;
        rect = new Rectangle(cx, cy, scale, scale);
        image = Toolkit.getDefaultToolkit().getImage(imageFilename);
        container = myContainer;
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

