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

    ShowBattleMap(JFrame parentFrame, String masterHexLabel)
    {
        super(masterHexLabel);

        MasterHex hex = MasterBoard.getHexByLabel(masterHexLabel);
        dialog = new JDialog(parentFrame, "Battle Map for " +
                hex.getTerrainName(), true);

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());

        addMouseListener(this);
        dialog.addWindowListener(this);

        contentPane.add(this, BorderLayout.CENTER);
        dialog.pack();
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
}
