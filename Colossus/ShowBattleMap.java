import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Class ShowBattleMap displays a battle map. 
 * @version $Id$
 * @author David Ripton
 */

public class ShowBattleMap extends Dialog implements WindowListener,
    MouseListener
{
    // Singleton class, so make everything static.

    private BattleHex [][] h = new BattleHex[6][6];
    private ArrayList hexes = new ArrayList(27);

    private Image offImage;
    private Graphics offGraphics;
    private Dimension offDimension;

    private static int scale;

    private MasterHex masterHex;


    public ShowBattleMap(Frame parentFrame, MasterHex masterHex)
    {
        super(parentFrame, "Battle Map for " + 
            masterHex.getTerrainName(), true);

        this.masterHex = masterHex;

        setLayout(null);

        scale = BattleMap.getScale();

        addMouseListener(this);
        addWindowListener(this);

        pack();

        setSize(getPreferredSize());
        setResizable(false);
        setBackground(Color.white);

        SetupBattleHexes.setupHexes(h, masterHex.getTerrain(), null, hexes);

        setVisible(true);
        repaint();
    }

    
    public void update(Graphics g)
    {
        Dimension d = getSize();

        // Abort if called too early.
        Rectangle rectClip = g.getClipBounds();
        if (rectClip == null)
        {
            return;
        }
        
        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (rectClip.intersects(hex.getBounds()))
            {
                hex.paint(offGraphics);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }


    public Dimension getPreferredSize()
    {
        return new Dimension(30 * scale, 30 * scale);
    }


    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mousePressed(MouseEvent e)
    {
        dispose();
    }


    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowOpened(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        dispose();
    }


    public void windowDeactivated(WindowEvent e)
    {
    }


    public void windowIconified(WindowEvent e)
    {
    }


    public void windowDeiconified(WindowEvent e)
    {
    }


    public static void main(String [] args)
    {
        Frame frame = new Frame("testing ShowBattleMap");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        new ShowBattleMap(frame, hex);
    }
}
