import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class ShowBattleMap displays a battle map. 
 * @version $Id$
 * @author David Ripton
 */

public class ShowBattleMap extends JPanel implements WindowListener,
    MouseListener
{
    private BattleHex [][] h = new BattleHex[6][6];
    private ArrayList hexes = new ArrayList(27);

    private static int scale;

    private MasterHex masterHex;

    private JDialog dialog;


    public ShowBattleMap(JFrame parentFrame, MasterHex masterHex)
    {
        dialog = new JDialog(parentFrame, "Battle Map for " +
            masterHex.getTerrainName(), true);

        this.masterHex = masterHex;

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());

        scale = BattleMap.getScale();

        addMouseListener(this);
        dialog.addWindowListener(this);

        contentPane.add(this, BorderLayout.CENTER);
        dialog.pack();

        dialog.setResizable(false);
        setOpaque(true);
        setBackground(Color.white);

        SetupBattleHexes.setupHexes(h, masterHex.getTerrain(), null, hexes);

        dialog.setVisible(true);
    }

    
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Abort if called too early.
        Rectangle rectClip = g.getClipBounds();
        if (rectClip == null)
        {
            return;
        }
        
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (rectClip.intersects(hex.getBounds()))
            {
                hex.paint(g);
            }
        }
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
        dialog.setVisible(false);
        dialog.dispose();
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mousePressed(MouseEvent e)
    {
        dialog.setVisible(false);
        dialog.dispose();
    }


    public void mouseReleased(MouseEvent e)
    {
        dialog.setVisible(false);
        dialog.dispose();
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
        dialog.setVisible(false);
        dialog.dispose();
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
        JFrame frame = new JFrame("testing ShowBattleMap");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        new ShowBattleMap(frame, hex);
    }
}
