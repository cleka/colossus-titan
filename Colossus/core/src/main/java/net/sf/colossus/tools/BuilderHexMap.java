package net.sf.colossus.tools;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import net.sf.colossus.xmlparser.BattlelandLoader;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.client.GUIBattleHex;
import net.sf.colossus.client.GUIHex;
import net.sf.colossus.variant.HazardTerrain;

/**
 * Class BuilderHexMap displays a basic battle map.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */
public class BuilderHexMap extends JPanel implements MouseListener,
        WindowListener
{

    String filename = null;

    // GUI hexes need to be recreated for each object, since scale varies.
    protected GUIBattleHex[][] h = new GUIBattleHex[6][6];
    private List<GUIBattleHex> hexes = new ArrayList<GUIBattleHex>(33);
    /** ne, e, se, sw, w, nw */
    private GUIBattleHex[] entrances = new GUIBattleHex[6];
    private static final boolean[][] show =
    {
        {
            false, false, true, true, true,
            false
        },
        {
            false, true, true, true, true, false
        },
        {
            false, true, true,
            true, true, true
        },
        {
            true, true, true, true, true, true
        },
        {
            false,
            true, true, true, true, true
        },
        {
            false, true, true, true, true,
            false
        }
    };
    protected boolean isTower = false;
    protected int scale = 2 * 15;
    protected int cx = 6 * scale;
    protected int cy = 2 * scale;

    BuilderHexMap(String f)
    {
        filename = f;

        setOpaque(true);
        setBackground(Color.white);
        setupHexes();
    }

    void setupHexes()
    {
        setupHexesGUI(h);
        setupNeighbors(h);
        setupHexesGameState(getBattleHexArray());
    }

    BattleHex[][] getBattleHexArray()
    {
        BattleHex[][] h2 = new BattleHex[6][6];
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (h[i][j] != null)
                {
                    h2[i][j] = h[i][j].getHexModel();
                }
            }

        }
        return h2;
    }

    private void setupHexesGUI(GUIBattleHex h[][])
    {
        hexes.clear();

        // Initialize hex array.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    GUIBattleHex hex = new GUIBattleHex(
                            (int) Math.
                            round(cx + 3 * i * scale),
                            (int) Math.round(cy + (2 * j + (i & 1)) *
                            GUIHex.SQRT3 * scale), scale, this, i, j);

                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
    }

    /** Add terrain, hexsides, elevation, and exits to hexes.
     *  Cliffs are bidirectional; other hexside obstacles are noted
     *  only on the high side, since they only interfere with
     *  uphill movement. */
    private synchronized void setupHexesGameState(BattleHex[][] lh)
    {
        if (filename != null)
        {
            InputStream terIS = null;

            try
            {
                terIS = new FileInputStream(filename);
            } catch (Exception e)
            {
                System.out.println("Battlelands file loading failed : " + e);
            }
            try
            {
                BattlelandLoader bl = new BattlelandLoader(terIS, lh);
            } catch (Exception e)
            {
                System.out.println("Battlelands file parsing failed : " + e);
            }
        }
    }

    /** Add references to neighbor hexes. */
    private void setupNeighbors(GUIBattleHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    assert h[i][j] != null : "Oups, hex model is null...";
                    if (j > 0 && show[i][j - 1])
                    {
                        assert h[i][j - 1] != null : "Oups, hex model is null...";
                        h[i][j].setNeighbor(0, h[i][j - 1]);
                    }

                    if (i < 5 && show[i + 1][j - ((i + 1) & 1)])
                    {
                        assert h[i + 1][j - ((i + 1) & 1)] != null : "Oups, hex model is null...";
                        h[i][j].setNeighbor(1, h[i + 1][j - ((i + 1) & 1)]);
                    }

                    if (i < 5 && j + (i & 1) < 6 && show[i + 1][j + (i & 1)])
                    {
                        assert h[i + 1][j + (i & 1)] != null : "Oups, hex model is null...";
                        h[i][j].setNeighbor(2, h[i + 1][j + (i & 1)]);
                    }

                    if (j < 5 && show[i][j + 1])
                    {
                        assert h[i][j + 1] != null : "Oups, hex model is null...";
                        h[i][j].setNeighbor(3, h[i][j + 1]);
                    }

                    if (i > 0 && j + (i & 1) < 6 && show[i - 1][j + (i & 1)])
                    {
                        assert h[i - 1][j + (i & 1)] != null : "Oups, hex model is null...";
                        h[i][j].setNeighbor(4, h[i - 1][j + (i & 1)]);
                    }

                    if (i > 0 && show[i - 1][j - ((i + 1) & 1)])
                    {
                        assert h[i - 1][j - ((i + 1) & 1)] != null : "Oups, hex model is null...";
                        h[i][j].setNeighbor(5, h[i - 1][j - ((i + 1) & 1)]);
                    }
                }
            }
        }
    }

    void setupEntrances()
    {
        setupEntrancesGUI();
        setupEntrancesGameState(entrances, h);
    }

    private void setupEntrancesGUI()
    {
        // Initialize entrances.
        entrances[0] = new GUIBattleHex(cx + 15 * scale,
                (int) Math.round(cy + 1 * scale), scale, this, -1, 0);
        entrances[1] = new GUIBattleHex(cx + 21 * scale,
                (int) Math.round(cy + 10 * scale), scale, this, -1, 1);
        entrances[2] = new GUIBattleHex(cx + 17 * scale,
                (int) Math.round(cy + 22 * scale), scale, this, -1, 2);
        entrances[3] = new GUIBattleHex(cx + 2 * scale,
                (int) Math.round(cy + 21 * scale), scale, this, -1, 3);
        entrances[4] = new GUIBattleHex(cx - 3 * scale,
                (int) Math.round(cy + 10 * scale), scale, this, -1, 4);
        entrances[5] = new GUIBattleHex(cx + 1 * scale,
                (int) Math.round(cy + 1 * scale), scale, this, -1, 5);

        hexes.add(entrances[0]);
        hexes.add(entrances[1]);
        hexes.add(entrances[2]);
        hexes.add(entrances[3]);
        hexes.add(entrances[4]);
        hexes.add(entrances[5]);
    }

    private void setupEntrancesGameState(GUIBattleHex[] entrances,
            GUIBattleHex[][] h)
    {
        // Add neighbors to entrances.
        entrances[0].setNeighbor(3, h[3][0]);
        entrances[0].setNeighbor(4, h[4][1]);
        entrances[0].setNeighbor(5, h[5][1]);

        entrances[1].setNeighbor(3, h[5][1]);
        entrances[1].setNeighbor(4, h[5][2]);
        entrances[1].setNeighbor(5, h[5][3]);
        entrances[1].setNeighbor(0, h[5][4]);

        entrances[2].setNeighbor(4, h[5][4]);
        entrances[2].setNeighbor(5, h[4][5]);
        entrances[2].setNeighbor(0, h[3][5]);

        entrances[3].setNeighbor(5, h[3][5]);
        entrances[3].setNeighbor(0, h[2][5]);
        entrances[3].setNeighbor(1, h[1][4]);
        entrances[3].setNeighbor(2, h[0][4]);

        entrances[4].setNeighbor(0, h[0][4]);
        entrances[4].setNeighbor(1, h[0][3]);
        entrances[4].setNeighbor(2, h[0][2]);

        entrances[5].setNeighbor(1, h[0][2]);
        entrances[5].setNeighbor(2, h[1][1]);
        entrances[5].setNeighbor(3, h[2][1]);
        entrances[5].setNeighbor(4, h[3][0]);
    }

    void unselectAllHexes()
    {
        for (GUIBattleHex hex : hexes)
        {
            if (hex.isSelected())
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    void unselectHexByLabel(String label)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (hex.isSelected() && label.equals(hex.getHexModel().getLabel()))
            {
                hex.unselect();
                hex.repaint();
                return;
            }
        }
    }

    void unselectHexesByLabels(Set labels)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (hex.isSelected() &&
                    labels.contains(hex.getHexModel().getLabel()))
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    void selectHexByLabel(String label)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (!hex.isSelected() && label.equals(hex.getHexModel().getLabel()))
            {
                hex.select();
                hex.repaint();
                return;
            }
        }
    }

    void selectHexesByLabels(Set<String> labels)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (!hex.isSelected() && labels.contains(
                    hex.getHexModel().getLabel()))
            {
                hex.select();
                hex.repaint();
            }
        }
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null. */
    GUIBattleHex getGUIHexByLabel(String label)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (hex.getHexModel().getLabel().equals(label))
            {
                return hex;
            }
        }

        System.err.println("Could not find hex " + label);
        return null;
    }

    /** Return the GUIBattleHex that contains the given point, or
     *  null if none does. */
    GUIBattleHex getHexContainingPoint(Point point)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (hex.contains(point))
            {
                return hex;
            }
        }

        return null;
    }

    Set<String> getAllHexLabels()
    {
        Set<String> set = new HashSet<String>();
        for (GUIBattleHex hex : hexes)
        {
            set.add(hex.getHexModel().getLabel());
        }
        return set;
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
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

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Abort if called too early.
        Rectangle rectClip = g.getClipBounds();
        if (rectClip == null)
        {
            return;
        }

        for (GUIBattleHex hex : hexes)
        {
            if (!hex.getHexModel().isEntrance() && rectClip.intersects(hex.
                    getBounds()))
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
        return new Dimension(60 * 15, 55 * 15);
    }

    public String dumpAsString()
    {
        StringBuilder buf = new StringBuilder();
        HazardTerrain terrain;
        char s;
        int e;
        List<GUIBattleHex> localStartList = new ArrayList<GUIBattleHex>();

        buf.append("# Battlelands generated by BattlelandsBuilder\n");
        buf.append(
                "# You can uncomment the line below to give this Battlelands a subtitle\n");
        buf.append("#SUBTITLE <put_your_subtitle_here>\n");

        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                if (show[i][j])
                {
                    boolean doDumpSides = false;
                    boolean hasSlope = false;
                    boolean hasWall = false;
                    terrain = h[i][j].getHexModel().getTerrain();
                    e = h[i][j].getHexModel().getElevation();

                    if (h[i][j].isSelected())
                    {
                        localStartList.add(h[i][j]);
                    }

                    for (int k = 0; k < 6; k++)
                    {
                        s = h[i][j].getHexModel().getHexside(k);
                        if (s != ' ')
                        {
                            doDumpSides = true;
                        }
                        if (s == 's')
                        {
                            hasSlope = true;
                        }
                        if (s == 'w')
                        {
                            hasWall = true;
                        }
                    }
                    if (doDumpSides ||
                            (!terrain.equals(HazardTerrain.getTerrainByName(
                            "Plains"))) ||
                            (e != 0))
                    {
                        if ((e < 1) && hasSlope)
                        {
                            buf.append(
                                    "# WARNING: slope on less-than-1 elevation Hex\n");
                        }
                        if ((!terrain.equals(HazardTerrain.getTerrainByName(
                                "Tower"))) && hasWall)
                        {
                            buf.append("# WARNING: wall on non-Tower Hex\n");
                        }
                        if ((e < 1) && hasWall)
                        {
                            buf.append(
                                    "# WARNING: wall on less-than-1 elevation Hex\n");
                        }
                        if ((terrain.equals(HazardTerrain.getTerrainByName(
                                "Lake"))) && doDumpSides)
                        {
                            buf.append("# WARNING: non-default sides on Lake\n");
                        }
                        if ((terrain.equals(HazardTerrain.getTerrainByName(
                                "Tree"))) && doDumpSides)
                        {
                            buf.append("# WARNING: non-default sides on Tree\n");
                        }
                        buf.append(i);
                        buf.append(" ");
                        buf.append(j);
                        buf.append(" ");
                        buf.append(h[i][j].getHexModel().getTerrain());
                        buf.append(" ");
                        buf.append(h[i][j].getHexModel().getElevation());
                        if (doDumpSides)
                        {
                            for (int k = 0; k < 6; k++)
                            {
                                if (h[i][j].getHexModel().getHexside(k) != ' ')
                                {
                                    buf.append(" ");
                                    buf.append(k);
                                    buf.append(" ");
                                    buf.append(h[i][j].getHexModel().getHexside(
                                            k));
                                }
                            }
                        }
                        buf.append("\n");
                    }
                }
            }
        }

        if (isTower)
        {
            buf.append("# This is a Tower\nTOWER\n");
        }
        if (!localStartList.isEmpty())
        {
            buf.append("# This terrain has a startlist\nSTARTLIST");
            for (GUIBattleHex lh : localStartList) {
                buf.append(" " + lh.getHexModel().getLabel());
            }
            buf.append("\n");
        }

        return (buf.toString());
    }

    void eraseMap()
    {
        for (GUIBattleHex hex : hexes)
        {
            hex.getHexModel().setTerrain(HazardTerrain.getTerrainByName("Plains"));
            hex.getHexModel().setElevation(0);
            for (int i = 0; i < 6; i++)
            {
                hex.getHexModel().setHexside(i, ' ');
            }
        }
    }
}
