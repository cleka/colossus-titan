import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public class BattleMap extends Frame implements MouseListener,
    MouseMotionListener, WindowListener
{
    private Hex[][] h = new Hex[6][6];
    private int numChits;
    private Chit[] chits = new Chit[14];
    private int tracking;
    private static final boolean[][] show =
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

    static private int scale = 30;
    static private int chitScale = 12 * scale / 5;
    private Dimension preferredSize;

    private Legion attacker;
    private Legion defender;
    private char terrain;
    private char side;

    // B,D,H,J,m,M,P,S,T,t,W
    // Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
    // Swamp, Tower, tundra, Woods

    // l, r, b for left, right, bottom

    public BattleMap(Legion attacker, Legion defender, char terrain, char side)
    {
        super("BattleMap");

        this.attacker = attacker;
        this.defender = defender;
        this.terrain = terrain;
        this.side = side;

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

        pack();
        validate();

        int cx = 3 * scale;
        int cy = 3 * scale;

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new Hex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + i % 2) *
                        Hex.SQRT3 * scale), scale);
                }
            }
        }

        tracker = new MediaTracker(this);

        int ah = attacker.getHeight();
        numChits = ah + defender.getHeight();

        for (int i = 0; i < ah; i++)
        {
            chits[i] = new Chit(0, 0, chitScale, 
                attacker.getCreature(i).getImageName(), this);
            tracker.addImage(chits[i].getImage(), 0);
        }
        for (int i = ah; i < numChits; i++)
        {
            chits[i] = new Chit(0, 0, chitScale, 
                defender.getCreature(i - ah).getImageName(), this);
            tracker.addImage(chits[i].getImage(), 0);
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

        for (int i = 0; i < numChits; i++)
        {
            if (chits[i].select(point))
            {
                tracking = 0;

                // Don't swap if it is already on top.
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
                if (show[i][j] && h[i][j].select(point))
                {
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
        for (int i = numChits - 1; i >= 0; i--)
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
            offImage = createImage(2 * d.width, 2 * d.height);
            gBack = offImage.getGraphics();
        }

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
        for (int i = numChits - 1; i >= 0; i--)
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

    public Dimension getMapSize()
    {
        Rectangle xRect = h[5][3].getBounds();
        Rectangle yRect = h[3][5].getBounds(); 
        return new Dimension(xRect.x + xRect.width, yRect.y + yRect.height);
    }


    public static void main(String args[])
    {
        Player player1 = new Player("Player 1", null);
        Player player2 = new Player("Player 2", null);
        Legion attacker = new Legion(0, 0, chitScale, null, null, null, 7,
            null, Creature.ogre, Creature.troll, Creature.ranger, 
            Creature.hydra, Creature.griffon, Creature.angel, 
            Creature.warlock, null, player1);
        Legion defender = new Legion(0, 0, chitScale, null, null, null, 6, 
            null, Creature.centaur, Creature.lion, Creature.gargoyle, 
            Creature.cyclops, Creature.gorgon, Creature.guardian, null, null,
            player2);
        new BattleMap(attacker, defender, 'p', 'b');
    }
}
