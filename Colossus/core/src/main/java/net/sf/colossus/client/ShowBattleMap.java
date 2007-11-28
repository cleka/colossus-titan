package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.JFrame;


/**
 * Class ShowBattleMap displays a battle map.
 * @version $Id$
 * @author David Ripton
 */

final class ShowBattleMap extends HexMap implements WindowListener,
            MouseListener
{
    private JDialog dialog;

    ShowBattleMap(JFrame parentFrame, String masterHexLabel, GUIMasterHex guiHex)
    {
        super(masterHexLabel);
        MasterHex hex = MasterBoard.getHexByLabel(masterHexLabel);
  
        String neighbors = findOutNeighbors(guiHex);
        
        dialog = new JDialog(parentFrame, "Battle Map for " +
                hex.getTerrainName() + " " + masterHexLabel + " " +
                neighbors, true);

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());

        addMouseListener(this);
        dialog.addWindowListener(this);

        contentPane.add(this, BorderLayout.CENTER);
        dialog.pack();
        dialog.setVisible(true);
    }

    private String findOutNeighbors(GUIMasterHex guiHex)
    {
        String neighbors = "";

        boolean inverted = guiHex.isInverted();
        MasterHex model = guiHex.getMasterHexModel();
//        String name = model.getTerrainDisplayName();
//        String label = model.getLabel();
//        int labelSide = model.getLabelSide();
        
        String east = "--";
        String nw   = "--";
        String sw   = "--";
        
        for (int i=0 ; i < 6 ; i++)
        {
            int sideConsideringInverting = inverted ? ((i+3) % 6) : i;
            MasterHex neighbor = model.getNeighbor(sideConsideringInverting);
            if (neighbor != null)
            {
                String nName = neighbor.getTerrainDisplayName();
                String nLabel = neighbor.getLabel();
                int entrySide = (6 + sideConsideringInverting - model.getLabelSide()) % 6;
//                String direction = BattleMap.entrySideName(i);
//                String entrySideText = BattleMap.entrySideName(entrySide);
                
//                System.out.println("Neighbor at " + direction + 
//                        "("+ i + ")" + " is " + nName + ", label " + 
//                        nLabel + ", entrySide: " + entrySide + " txt " + entrySideText );
                
                if (entrySide == 1)
                {
                    east = nName + " " + nLabel; 
                }
                if (entrySide == 3)
                {
                    sw = nName + " " + nLabel; 
                }
                if (entrySide == 5)
                {
                    nw = nName + " " + nLabel; 
                }
            }
        }

        neighbors = "(East: " + east + ", SouthWest: " + sw + ", " +
                "NorthWest: " + nw + ")";
        
        return neighbors;
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
}
