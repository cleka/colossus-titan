import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class HexMap displays a basic battle map.
 * @version $Id$
 * @author David Ripton
 */

public class HexMap extends JPanel implements MouseListener, WindowListener
{
    private BattleHex [][] h = new BattleHex[6][6];
    private ArrayList hexes = new ArrayList(33);
    protected String masterHexLabel;
    protected MasterBoard board;

    // ne, e, se, sw, w, nw
    protected BattleHex [] entrances = new BattleHex[6];

    private static final boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };


    public HexMap(MasterBoard board, String masterHexLabel)
    {
        this.masterHexLabel = masterHexLabel;
        this.board = board;
        setOpaque(true);
        setBackground(Color.white);
        setupHexes();
        setupNeighbors();
    }


    public MasterHex getMasterHex()
    {
        return board.getHexByLabel(masterHexLabel);
    }


    protected void setupHexes()
    {
        char terrain = getMasterHex().getTerrain();
        hexes.clear();

        int scale = 2 * Scale.get();
        int cx = 6 * scale;
        int cy = 3 * scale;

        // Initialize hex array.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    BattleHex hex = new BattleHex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + (i & 1)) *
                        Hex.SQRT3 * scale), scale, this, i, j);
                    hex.setXCoord(i);
                    hex.setYCoord(j);

                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }


        // Add terrain, hexsides, elevation, and exits to hexes.
        // Cliffs are bidirectional; other hexside obstacles are noted
        // only on the high side, since they only interfere with
        // uphill movement.
        switch (terrain)
        {
            case 'P':
                break;

            case 'W':
                h[0][2].setTerrain('t');
                h[2][3].setTerrain('t');
                h[3][5].setTerrain('t');
                h[4][1].setTerrain('t');
                h[4][3].setTerrain('t');

                h[0][2].setElevation(1);
                h[2][3].setElevation(1);
                h[3][5].setElevation(1);
                h[4][1].setElevation(1);
                h[4][3].setElevation(1);
                break;

            case 'D':
                h[0][3].setTerrain('s');
                h[0][4].setTerrain('s');
                h[1][3].setTerrain('s');
                h[3][0].setTerrain('s');
                h[3][1].setTerrain('s');
                h[3][2].setTerrain('s');
                h[3][5].setTerrain('s');
                h[4][1].setTerrain('s');
                h[4][2].setTerrain('s');
                h[4][5].setTerrain('s');
                h[5][1].setTerrain('s');

                h[0][3].setHexside(0, 'd');
                h[0][3].setHexside(1, 'd');
                h[1][3].setHexside(0, 'd');
                h[1][3].setHexside(1, 'd');
                h[1][3].setHexside(2, 'd');
                h[1][3].setHexside(3, 'c');
                h[3][1].setHexside(4, 'd');
                h[3][2].setHexside(2, 'd');
                h[3][2].setHexside(3, 'c');
                h[3][2].setHexside(4, 'c');
                h[3][2].setHexside(5, 'd');
                h[3][5].setHexside(0, 'd');
                h[3][5].setHexside(5, 'd');
                h[4][2].setHexside(2, 'd');
                h[4][2].setHexside(3, 'd');
                h[4][5].setHexside(0, 'c');
                h[4][5].setHexside(1, 'd');
                h[4][5].setHexside(5, 'd');
                break;

            case 'B':
                h[0][2].setTerrain('r');
                h[1][3].setTerrain('r');
                h[2][2].setTerrain('r');
                h[3][1].setTerrain('r');
                h[3][4].setTerrain('r');
                h[3][5].setTerrain('r');
                h[4][3].setTerrain('r');
                h[5][1].setTerrain('r');
                break;

            case 'J':
                h[0][3].setTerrain('r');
                h[1][1].setTerrain('t');
                h[2][1].setTerrain('r');
                h[2][3].setTerrain('r');
                h[2][5].setTerrain('r');
                h[3][2].setTerrain('r');
                h[3][3].setTerrain('t');
                h[4][4].setTerrain('r');
                h[5][1].setTerrain('r');
                h[5][2].setTerrain('t');

                h[1][1].setElevation(1);
                h[3][3].setElevation(1);
                h[5][2].setElevation(1);
                break;

            case 'M':
                h[0][2].setTerrain('o');
                h[2][3].setTerrain('o');
                h[2][4].setTerrain('o');
                h[3][1].setTerrain('o');
                h[4][3].setTerrain('o');
                h[4][5].setTerrain('o');
                break;

            case 'S':
                h[1][3].setTerrain('o');
                h[2][1].setTerrain('o');
                h[2][2].setTerrain('t');
                h[2][4].setTerrain('t');
                h[3][3].setTerrain('o');
                h[3][5].setTerrain('o');
                h[4][2].setTerrain('t');
                h[5][3].setTerrain('o');

                h[2][2].setElevation(1);
                h[2][4].setElevation(1);
                h[4][2].setElevation(1);
                break;

            case 'H':
                h[2][2].setTerrain('t');
                h[2][4].setTerrain('t');
                h[5][3].setTerrain('t');

                h[1][2].setElevation(1);
                h[1][4].setElevation(1);
                h[2][2].setElevation(1);
                h[2][4].setElevation(1);
                h[3][0].setElevation(1);
                h[3][4].setElevation(1);
                h[4][3].setElevation(1);
                h[5][3].setElevation(1);

                h[1][2].setHexside(0, 's');
                h[1][2].setHexside(1, 's');
                h[1][2].setHexside(2, 's');
                h[1][2].setHexside(3, 's');
                h[1][2].setHexside(4, 's');
                h[1][2].setHexside(5, 's');
                h[1][4].setHexside(0, 's');
                h[1][4].setHexside(1, 's');
                h[1][4].setHexside(2, 's');
                h[1][4].setHexside(5, 's');
                h[3][0].setHexside(2, 's');
                h[3][0].setHexside(3, 's');
                h[3][0].setHexside(4, 's');
                h[3][4].setHexside(0, 's');
                h[3][4].setHexside(1, 's');
                h[3][4].setHexside(2, 's');
                h[3][4].setHexside(3, 's');
                h[3][4].setHexside(4, 's');
                h[3][4].setHexside(5, 's');
                h[4][3].setHexside(0, 's');
                h[4][3].setHexside(1, 's');
                h[4][3].setHexside(2, 's');
                h[4][3].setHexside(3, 's');
                h[4][3].setHexside(4, 's');
                h[4][3].setHexside(5, 's');
                break;

            case 'm':
                h[3][2].setTerrain('v');

                h[0][4].setElevation(1);
                h[1][1].setElevation(1);
                h[1][3].setElevation(1);
                h[1][4].setElevation(2);
                h[2][1].setElevation(2);
                h[2][2].setElevation(1);
                h[2][5].setElevation(1);
                h[3][0].setElevation(2);
                h[3][1].setElevation(1);
                h[3][2].setElevation(2);
                h[3][3].setElevation(1);
                h[4][1].setElevation(1);
                h[4][2].setElevation(1);
                h[4][3].setElevation(1);
                h[5][1].setElevation(2);
                h[5][2].setElevation(1);
                h[5][3].setElevation(2);
                h[5][4].setElevation(1);

                h[0][4].setHexside(0, 's');
                h[1][1].setHexside(3, 's');
                h[1][1].setHexside(4, 's');
                h[1][3].setHexside(0, 's');
                h[1][3].setHexside(1, 's');
                h[1][3].setHexside(2, 's');
                h[1][3].setHexside(5, 's');
                h[1][4].setHexside(0, 's');
                h[1][4].setHexside(1, 'c');
                h[1][4].setHexside(2, 's');
                h[1][4].setHexside(5, 's');
                h[2][1].setHexside(2, 's');
                h[2][1].setHexside(3, 's');
                h[2][1].setHexside(4, 's');
                h[2][2].setHexside(3, 's');
                h[2][2].setHexside(4, 's');
                h[2][5].setHexside(0, 's');
                h[2][5].setHexside(1, 's');
                h[2][5].setHexside(2, 's');
                h[3][0].setHexside(2, 's');
                h[3][0].setHexside(3, 's');
                h[3][2].setHexside(0, 's');
                h[3][2].setHexside(1, 's');
                h[3][2].setHexside(2, 's');
                h[3][2].setHexside(3, 's');
                h[3][2].setHexside(4, 'c');
                h[3][2].setHexside(5, 's');
                h[3][3].setHexside(2, 's');
                h[3][3].setHexside(3, 's');
                h[3][3].setHexside(4, 's');
                h[3][3].setHexside(5, 's');
                h[4][3].setHexside(3, 's');
                h[5][1].setHexside(3, 's');
                h[5][1].setHexside(4, 's');
                h[5][1].setHexside(5, 's');
                h[5][3].setHexside(0, 's');
                h[5][3].setHexside(3, 's');
                h[5][3].setHexside(4, 'c');
                h[5][3].setHexside(5, 's');
                h[5][4].setHexside(4, 's');
                h[5][4].setHexside(5, 's');
                break;

            case 't':
                h[0][4].setTerrain('d');
                h[1][3].setTerrain('d');
                h[2][1].setTerrain('d');
                h[2][2].setTerrain('d');
                h[2][4].setTerrain('d');
                h[3][3].setTerrain('d');
                h[4][2].setTerrain('d');
                h[4][5].setTerrain('d');
                h[5][3].setTerrain('d');
                break;

            case 'T':
                h[2][2].setElevation(1);
                h[2][3].setElevation(1);
                h[3][1].setElevation(1);
                h[3][2].setElevation(2);
                h[3][3].setElevation(1);
                h[4][2].setElevation(1);
                h[4][3].setElevation(1);

                h[2][2].setHexside(0, 'w');
                h[2][2].setHexside(4, 'w');
                h[2][2].setHexside(5, 'w');
                h[2][3].setHexside(3, 'w');
                h[2][3].setHexside(4, 'w');
                h[2][3].setHexside(5, 'w');
                h[3][1].setHexside(0, 'w');
                h[3][1].setHexside(1, 'w');
                h[3][1].setHexside(5, 'w');
                h[3][2].setHexside(0, 'w');
                h[3][2].setHexside(1, 'w');
                h[3][2].setHexside(2, 'w');
                h[3][2].setHexside(3, 'w');
                h[3][2].setHexside(4, 'w');
                h[3][2].setHexside(5, 'w');
                h[3][3].setHexside(2, 'w');
                h[3][3].setHexside(3, 'w');
                h[3][3].setHexside(4, 'w');
                h[4][2].setHexside(0, 'w');
                h[4][2].setHexside(1, 'w');
                h[4][2].setHexside(2, 'w');
                h[4][3].setHexside(1, 'w');
                h[4][3].setHexside(2, 'w');
                h[4][3].setHexside(3, 'w');
                break;
        }
    }


    /** Add references to neighbor hexes. */
    protected void setupNeighbors()
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


    protected void setupEntrances()
    {
        int scale = 2 * Scale.get();

        int cx = 6 * scale;
        int cy = 3 * scale;

        // Initialize entrances.
        entrances[0] = new BattleHex(cx + 15 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 0);
        entrances[1] = new BattleHex(cx + 21 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 1);
        entrances[2] = new BattleHex(cx + 17 * scale,
            (int) Math.round(cy + 22 * scale), scale, this, -1, 2);
        entrances[3] = new BattleHex(cx + 2 * scale,
            (int) Math.round(cy + 21 * scale), scale, this, -1, 3);
        entrances[4] = new BattleHex(cx - 3 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 4);
        entrances[5] = new BattleHex(cx + 1 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 5);

        hexes.add(entrances[0]);
        hexes.add(entrances[1]);
        hexes.add(entrances[2]);
        hexes.add(entrances[3]);
        hexes.add(entrances[4]);
        hexes.add(entrances[5]);

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


    public void unselectAllHexes()
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.isSelected())
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    public void unselectHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
                return;
            }
        }
    }

    public void unselectHexesByLabels(Set labels)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    public void selectHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (!hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
                return;
            }
        }
    }

    public void selectHexesByLabels(Set labels)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (!hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
            }
        }
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null. */
    public BattleHex getHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }

        Log.error("Could not find hex " + label);
        return null;
    }

    /** Return the BattleHex that contains the given point, or
     *  null if none does. */
    protected BattleHex getHexContainingPoint(Point point)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            BattleHex hex = (BattleHex)it.next();
            if (hex.contains(point))
            {
                return hex;
            }
        }

        return null;
    }


    /** Return the hex that is defined as the center of the map,
     *  for defender tower entry purposes. */
    public BattleHex getCenterHex()
    {
        return h[3][2];
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
            BattleHex hex = (BattleHex)it.next();
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
        int scale = Scale.get();
        return new Dimension(60 * scale, 60 * scale);
    }


    public static void main(String [] args)
    {
        char terrain;
        if (args.length == 0)
        {
            terrain = 'D';
        }
        else
        {
            terrain = args[0].charAt(0);
        }
        Game game = new Game();
        game.initBoard();
        MasterBoard board = game.getBoard();
        MasterHex hex = board.getAnyHexWithTerrain(terrain);

        JFrame frame = new JFrame("Hex Map for " + hex.getTerrainName());
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        HexMap hexMap = new HexMap(board, hex.getLabel());

        contentPane.add(hexMap, BorderLayout.CENTER);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
