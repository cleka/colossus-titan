import java.awt.*;
import java.awt.event.*;

/**
 * Class PickEntrySide allows picking which side of a MasterBoard hex
 * to enter.
 * @version $Id$
 * @author David Ripton
 */

public class PickEntrySide extends Dialog implements ActionListener,
    WindowListener
{
    private static BattleHex [][] h = new BattleHex[6][6];

    private static Image offImage;
    private static Graphics offGraphics;
    private static Dimension offDimension;

    private static int scale;

    private static MasterHex masterHex;

    private static Button button5;  // left
    private static Button button3;  // bottom
    private static Button button1;  // right

    private static boolean laidOut;
    private static boolean hexesReady;


    public PickEntrySide(Frame parentFrame, MasterHex masterHex)
    {
        super(parentFrame, "Pick entry side", true);

        // Reinitialize these every time, since they're static.
        laidOut = false;
        hexesReady = false;

        this.masterHex = masterHex;

        setLayout(null);

        scale = BattleMap.getScale();


        if (masterHex.canEnterViaSide(5))
        {
            button5 = new Button("Left");
            add(button5);
            button5.addActionListener(this);
        }
        
        if (masterHex.canEnterViaSide(3))
        {
            button3 = new Button("Bottom");
            add(button3);
            button3.addActionListener(this);
        }
        
        if (masterHex.canEnterViaSide(1))
        {
            button1 = new Button("Right");
            add(button1);
            button1.addActionListener(this);
        }

        addWindowListener(this);

        pack();

        setSize(getPreferredSize());
        setResizable(false);
        setBackground(Color.white);

        SetupBattleHexes.setupHexes(h, masterHex.getTerrain(), null);
        hexesReady = true;

        setVisible(true);
        repaint();
    }
    

    public void update(Graphics g)
    {
        Dimension d = getSize();

        // Abort if called too early.
        Rectangle rectClip = g.getClipBounds();
        if (rectClip == null || !hexesReady)
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

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (SetupBattleHexes.show[i][j] && h[i][j] != null &&
                    rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(offGraphics);
                }
            }
        }

        if (!laidOut)
        {
            int cx = 6 * scale;
            int cy = 3 * scale;

            if (button5 != null)
            {
                button5.setBounds(cx + 1 * scale,
                    (int) Math.round(cy + 1 * scale),
                    d.width / 7, d.height / 16);
            }
            if (button3 != null)
            {
                button3.setBounds(cx + 1 * scale,
                    (int) Math.round(cy + 21 * scale),
                    d.width / 7, d.height / 16);
            }
            if (button1 != null)
            {
                button1.setBounds(cx + 19 * scale,
                    (int) Math.round(cy + 11 * scale),
                    d.width / 7, d.height / 16);
            }

            laidOut = true;
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


    // Set hex's entry side to side, and then exit the dialog.  If side
    // is -1, then do not set an entry side, which will abort the move.
    private void cleanup(int side)
    {
        masterHex.clearAllEntrySides();

        if (side == 1 || side == 3 || side == 5)
        {
            masterHex.setEntrySide(side);
        }

        dispose();
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Left"))
        {
            cleanup(5);
        }

        else if (e.getActionCommand().equals("Right"))
        {
            cleanup(1);
        }

        else if (e.getActionCommand().equals("Bottom"))
        {
            cleanup(3);
        }
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
        // Abort the move.
        cleanup(-1);
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
        Frame frame = new Frame("testing PickEntrySide");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setEntrySide(1);
        hex.setEntrySide(3);
        hex.setEntrySide(5);
        new PickEntrySide(frame, hex);
    }
}
