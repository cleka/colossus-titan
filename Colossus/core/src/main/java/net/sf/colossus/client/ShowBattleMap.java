package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;

import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KFrame;


/**
 * Class ShowBattleMap displays a battle map.
 * @version $Id$
 * @author David Ripton
 */

final class ShowBattleMap extends HexMap implements WindowListener,
    MouseListener
{
    private static final String NoLandText = "--";

    private final JDialog dialog;

    private static JButton leftButton;
    private static JButton bottomButton;
    private static JButton rightButton;
    private static boolean laidOut;

    private int oldScale = -1;

    ShowBattleMap(KFrame parentFrame, String masterHexLabel,
        GUIMasterHex guiHex)
    {
        super(masterHexLabel);
        MasterHex hex = MasterBoard.getHexByLabel(masterHexLabel);

        Map<String, String> neighbors = findOutNeighbors(guiHex);
        String neighborsText = neighbors.get("text");

        dialog = new JDialog(parentFrame, "Battle Map for "
            + hex.getTerrainName() + " " + masterHexLabel + " "
            + neighborsText, true);
        laidOut = false;

        Container contentPane = dialog.getContentPane();
        // contentPane.setLayout(new BorderLayout());
        contentPane.setLayout(null);

        String text = neighbors.get(Constants.left);
        if (!text.equals(NoLandText))
        {
            leftButton = new JButton("<HTML>" + Constants.left + ":<BR>"
                + text + "</HTML>");
            leftButton.setEnabled(false);
            contentPane.add(leftButton);
        }

        text = neighbors.get(Constants.bottom);
        if (!text.equals(NoLandText))
        {
            bottomButton = new JButton("<HTML>" + Constants.bottom + ":<BR>"
                + text + "</HTML>");
            bottomButton.setEnabled(false);
            contentPane.add(bottomButton);
        }

        text = neighbors.get(Constants.right);
        if (!text.equals(NoLandText))
        {
            rightButton = new JButton("<HTML>" + Constants.right + ":<BR>"
                + text + "</HTML>");
            rightButton.setEnabled(false);
            contentPane.add(rightButton);
        }

        addMouseListener(this);
        dialog.addWindowListener(this);

        setSize(getPreferredSize());
        contentPane.add(this, BorderLayout.CENTER);
        dialog.pack();

        dialog.setSize(getPreferredSize());
        dialog.setBackground(Color.white);
        dialog.setVisible(true);
    }

    private Map<String, String> findOutNeighbors(GUIMasterHex guiHex)
    {
        String neighborsText = "";

        boolean inverted = guiHex.isInverted();
        MasterHex model = guiHex.getMasterHexModel();

        String right = NoLandText;
        String left = NoLandText;
        String bottom = NoLandText;

        for (int i = 0; i < 6; i++)
        {
            int sideConsideringInverting = inverted ? ((i + 3) % 6) : i;
            MasterHex neighbor = model.getNeighbor(sideConsideringInverting);
            if (neighbor != null)
            {
                String nName = neighbor.getTerrainDisplayName();
                String nLabel = neighbor.getLabel();
                int entrySide = (6 + sideConsideringInverting - model
                    .getLabelSide()) % 6;

                if (entrySide == 1)
                {
                    right = nName + " " + nLabel;
                }
                if (entrySide == 3)
                {
                    bottom = nName + " " + nLabel;
                }
                if (entrySide == 5)
                {
                    left = nName + " " + nLabel;
                }
            }
        }

        neighborsText = Constants.right + ": " + right + ", "
            + Constants.bottom + ": " + bottom + ", " + Constants.left + ": "
            + left + ")";

        Map<String, String> neighbors = new HashMap<String, String>(4);
        neighbors.put("text", neighborsText);
        neighbors.put(Constants.right, right);
        neighbors.put(Constants.bottom, bottom);
        neighbors.put(Constants.left, left);

        return neighbors;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Abort if called too early.
        Rectangle rectClip = g.getClipBounds();
        if (rectClip == null)
        {
            return;
        }

        int scale = 2 * Scale.get();
        if (oldScale != scale)
        {
            laidOut = false;
            this.oldScale = scale;
        }

        Dimension d = getSize();

        if (!laidOut)
        {
            int height = d.height / 16;
            if (leftButton != null)
            {
                leftButton.setBounds(cx - 20 + 0 * scale, cy + 0 * scale,
                    d.width / 5, height);
            }
            if (bottomButton != null)
            {
                bottomButton.setBounds(cx - 20 + 0 * scale, cy + 21 * scale,
                    d.width / 5, height);
            }
            if (rightButton != null)
            {
                rightButton.setBounds((int)(cx + 18.5 * scale), cy + 11
                    * scale, d.width / 6, height);
            }

            laidOut = true;
        }

        if (rightButton != null)
        {
            rightButton.repaint();
        }
        if (bottomButton != null)
        {
            bottomButton.repaint();
        }
        if (leftButton != null)
        {
            leftButton.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        dialog.dispose();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        dialog.dispose();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        dialog.dispose();
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        dialog.dispose();
    }
}
