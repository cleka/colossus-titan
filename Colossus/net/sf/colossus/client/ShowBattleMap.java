package net.sf.colossus.client;


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


    public ShowBattleMap(JFrame parentFrame, String masterHexLabel)
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
}
