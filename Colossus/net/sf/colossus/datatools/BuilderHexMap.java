
package net.sf.colossus.datatools;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import net.sf.colossus.parser.BattlelandLoader;
import net.sf.colossus.client.Hex;
import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.BasicGUIBattleHex;

/**
 * Class BuilderHexMap displays a basic battle map.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class BuilderHexMap extends JPanel implements MouseListener, WindowListener
{
    private String masterHexLabel;
    private char terrain;
    String filename = null;
    
    // GUI hexes need to be recreated for each object, since scale varies.
    private GUIBuilderHex [][] h = new GUIBuilderHex[6][6];
    private java.util.List hexes = new ArrayList(33);

    /** ne, e, se, sw, w, nw */
    private GUIBuilderHex [] entrances = new GUIBuilderHex[6];

    private static final boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };

    int scale = 2 * 15;
    int cx = 6 * scale;
    int cy = 2 * scale;

    BuilderHexMap(String masterHexLabel, char terrain, String f)
    {
        this.masterHexLabel = masterHexLabel;
        this.terrain = terrain;
        filename = f;

        setOpaque(true);
        setBackground(Color.white);
        setupHexes();
    }

    void setupHexes()
    {
        setupHexesGUI();
        setupHexesGameState(terrain, h);
        setupNeighbors(h);
    }

    private void setupHexesGUI()
    {
        hexes.clear();

        // Initialize hex array.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    GUIBuilderHex hex = new GUIBuilderHex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + (i & 1)) *
                        Hex.SQRT3 * scale), scale, this, i, j);

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
    private synchronized void setupHexesGameState(char terrain, 
        BattleHex [][] h)
    {
        if (filename != null)
        {
            InputStream terIS = null;
        
            try
            {
                terIS = new FileInputStream(filename);
            }
            catch (Exception e) 
            {
                System.out.println("Battlelands loading failed : " + e);
            }
            try
            {
                BattlelandLoader bl = new BattlelandLoader(terIS);
                while (bl.oneBattlelandCase(h) >= 0) {}
            }
            catch (Exception e) 
            {
                System.out.println("Battlelands loading failed : " + e);
            }
        }
    }


    /** Add references to neighbor hexes. */
    private void setupNeighbors(BattleHex [][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    if (j > 0 && show[i][j - 1])
                    {
                        h[i][j].setNeighbor(0, h[i][j - 1]);
                    }

                    if (i < 5 && show[i + 1][j - ((i + 1) & 1)])
                    {
                        h[i][j].setNeighbor(1, h[i + 1][j - ((i + 1) & 1)]);
                    }

                    if (i < 5 && j + (i & 1) < 6 && show[i + 1][j + (i & 1)])
                    {
                        h[i][j].setNeighbor(2, h[i + 1][j + (i & 1)]);
                    }

                    if (j < 5 && show[i][j + 1])
                    {
                        h[i][j].setNeighbor(3, h[i][j + 1]);
                    }

                    if (i > 0 && j + (i & 1) < 6 && show[i - 1][j + (i & 1)])
                    {
                        h[i][j].setNeighbor(4, h[i - 1][j + (i & 1)]);
                    }

                    if (i > 0 && show[i - 1][j - ((i + 1) & 1)])
                    {
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
        entrances[0] = new GUIBuilderHex(cx + 15 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 0);
        entrances[1] = new GUIBuilderHex(cx + 21 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 1);
        entrances[2] = new GUIBuilderHex(cx + 17 * scale,
            (int) Math.round(cy + 22 * scale), scale, this, -1, 2);
        entrances[3] = new GUIBuilderHex(cx + 2 * scale,
            (int) Math.round(cy + 21 * scale), scale, this, -1, 3);
        entrances[4] = new GUIBuilderHex(cx - 3 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 4);
        entrances[5] = new GUIBuilderHex(cx + 1 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 5);

        hexes.add(entrances[0]);
        hexes.add(entrances[1]);
        hexes.add(entrances[2]);
        hexes.add(entrances[3]);
        hexes.add(entrances[4]);
        hexes.add(entrances[5]);
    }

    private void setupEntrancesGameState(BattleHex [] entrances,
        BattleHex [][] h)
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
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBuilderHex hex = (GUIBuilderHex)it.next();
            if (hex.isSelected())
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    void unselectHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBuilderHex hex = (GUIBuilderHex)it.next();
            if (hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
                return;
            }
        }
    }

    void unselectHexesByLabels(Set labels)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBuilderHex hex = (GUIBuilderHex)it.next();
            if (hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    void selectHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBuilderHex hex = (GUIBuilderHex)it.next();
            if (!hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
                return;
            }
        }
    }

    void selectHexesByLabels(Set labels)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBuilderHex hex = (GUIBuilderHex)it.next();
            if (!hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
            }
        }
    }


    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null. */
    GUIBuilderHex getGUIHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBuilderHex hex = (GUIBuilderHex)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }

        System.err.println("Could not find hex " + label);
        return null;
    }

    /** Return the GUIBuilderHex that contains the given point, or
     *  null if none does. */
    GUIBuilderHex getHexContainingPoint(Point point)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBuilderHex hex = (GUIBuilderHex)it.next();
            if (hex.contains(point))
            {
                return hex;
            }
        }

        return null;
    }

    Set getAllHexLabels()
    {
        Set set = new HashSet();
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            set.add(hex.getLabel());
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

        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBuilderHex hex = (GUIBuilderHex)it.next();
            if (!hex.isEntrance() && rectClip.intersects(hex.getBounds()))
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
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 6; i++)
        {
            for (int j = 0 ; j < 6 ; j++)
            {
                if (show[i][j])
                {
                    boolean doDumpSides = false;
                    for (int k = 0; k < 6; k++)
                    {
                        if (h[i][j].getHexside(k) != ' ')
                            doDumpSides = true;
                    }
                    if (doDumpSides ||
                        (h[i][j].getTerrain() != 'p') ||
                        (h[i][j].getElevation() != 0))
                    {
                        buf.append(i);
                        buf.append(" ");
                        buf.append(j);
                        buf.append(" ");
                        buf.append(h[i][j].getTerrain());
                        buf.append(" ");
                        buf.append(h[i][j].getElevation());
                        if (doDumpSides)
                        {
                            for (int k = 0; k < 6; k++)
                            {
                                if (h[i][j].getHexside(k) != ' ')
                                {
                                    buf.append(" ");
                                    buf.append(k);
                                    buf.append(" ");
                                    buf.append(h[i][j].getHexside(k));
                                }
                            }
                        }
                        buf.append("\n");
                    }
                }
            }
        }
        return(buf.toString());
    }
}
