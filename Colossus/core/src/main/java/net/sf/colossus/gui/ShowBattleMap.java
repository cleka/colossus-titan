package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.sf.colossus.client.HexMap;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.variant.MasterHex;


/**
 * Class ShowBattleMap displays a battle map.
 *
 * TODO this is inside out: the dialog this class is displayed in is owned
 *      and managed by this class
 *
 * TODO the only reason this class needs GUIMasterHex and not just MasterHex
 *      is that the MasterHex class doesn't offer isInverted(), which it
 *      probably could (and maybe should)
 *
 * @author David Ripton
 */
@SuppressWarnings("serial")
final class ShowBattleMap extends HexMap
{
    private static final String NoLandText = "--";

    private static JButton leftButton;
    private static JButton bottomButton;
    private static JButton rightButton;
    private static boolean laidOut;

    private int oldScale = -1;

    ShowBattleMap(final JFrame parentFrame, final ClientGUI gui,
        final GUIMasterHex hex)
    {
        super(hex.getHexModel());

        assert SwingUtilities.isEventDispatchThread() : "Constructor should be called only on the EDT";

        Map<EntrySide, String> neighbors = findOutNeighbors(hex);
        String neighborsText = "(" + EntrySide.RIGHT.getLabel() + ": "
            + neighbors.get(EntrySide.RIGHT) + ", "
            + EntrySide.BOTTOM.getLabel() + ": "
            + neighbors.get(EntrySide.BOTTOM) + ", "
            + EntrySide.LEFT.getLabel() + ": " + neighbors.get(EntrySide.LEFT)
            + ")";

        final KDialog dialog = new KDialog(parentFrame, "Battle Map for "
            + hex.getHexModel().getTerrainName() + " " + hex.getHexModel()
            + " " + neighborsText, false);
        laidOut = false;

        final Container contentPane = dialog.getContentPane();
        contentPane.setLayout(null);

        //TODO: three or more, use a for (maybe integrating findOutNeighbours code)
        String text = neighbors.get(EntrySide.LEFT);
        if (!text.equals(NoLandText))
        {
            leftButton = new JButton("<HTML>" + EntrySide.LEFT.getLabel()
                + ":<BR>" + text + "</HTML>");
            leftButton.setEnabled(false);
            contentPane.add(leftButton);
        }

        text = neighbors.get(EntrySide.BOTTOM);
        if (!text.equals(NoLandText))
        {
            bottomButton = new JButton("<HTML>" + EntrySide.BOTTOM.getLabel()
                + ":<BR>" + text + "</HTML>");
            bottomButton.setEnabled(false);
            contentPane.add(bottomButton);
        }

        text = neighbors.get(EntrySide.RIGHT);
        if (!text.equals(NoLandText))
        {
            rightButton = new JButton("<HTML>" + EntrySide.RIGHT + ":<BR>"
                + text + "</HTML>");
            rightButton.setEnabled(false);
            contentPane.add(rightButton);
        }

        dialog.useSaveWindow(gui.getOptions(), "ShowBattleMap", null);

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getButton() == MouseEvent.BUTTON1)
                {
                    dialog.dispose();
                }
                else
                {
                    new BattleTerrainHazardWindow(parentFrame, gui,
                        hex.getHexModel());
                }
            }
        });
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setSize(getPreferredSize());
        contentPane.add(ShowBattleMap.this);
        dialog.pack();

        dialog.setSize(getPreferredSize());
        dialog.setBackground(Color.white);
        dialog.setVisible(true);
    }

    private Map<EntrySide, String> findOutNeighbors(GUIMasterHex guiHex)
    {
        boolean inverted = guiHex.isInverted();
        MasterHex model = guiHex.getHexModel();

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

        Map<EntrySide, String> neighbors = new HashMap<EntrySide, String>(4);
        neighbors.put(EntrySide.RIGHT, right);
        neighbors.put(EntrySide.BOTTOM, bottom);
        neighbors.put(EntrySide.LEFT, left);

        return neighbors;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        assert SwingUtilities.isEventDispatchThread() : "Painting code should be called only on the EDT";

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
}
