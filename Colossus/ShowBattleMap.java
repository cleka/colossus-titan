import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class ShowBattleMap displays a battle map.
 * @version $Id$
 * @author David Ripton
 */

public final class ShowBattleMap extends HexMap implements WindowListener,
    MouseListener
{
    private JDialog dialog;


    public ShowBattleMap(JFrame parentFrame, MasterHex masterHex)
    {
        super(masterHex);

        dialog = new JDialog(parentFrame, "Battle Map for " +
            masterHex.getTerrainName(), true);

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());

        addMouseListener(this);
        dialog.addWindowListener(this);

        contentPane.add(this, BorderLayout.CENTER);
        dialog.pack();

        dialog.setResizable(false);
        dialog.setVisible(true);
    }


    public void mouseClicked(MouseEvent e)
    {
        dialog.dispose();
    }


    public void mousePressed(MouseEvent e)
    {
        dialog.dispose();
    }


    public void mouseReleased(MouseEvent e)
    {
        dialog.dispose();
    }


    public void windowClosing(WindowEvent e)
    {
        dialog.dispose();
    }


    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing ShowBattleMap");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('D');
        new ShowBattleMap(frame, hex);
    }
}
