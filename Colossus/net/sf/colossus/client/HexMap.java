package net.sf.colossus.client;


import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.parser.BattlelandLoader;
import net.sf.colossus.parser.BattlelandRandomizerLoader;
import net.sf.colossus.parser.TerrainRecruitLoader;
import net.sf.colossus.server.VariantSupport;

/**
 * Class HexMap displays a basic battle map.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class HexMap extends JPanel implements MouseListener, WindowListener
{
    private String masterHexLabel;
    private String terrain;

    // GUI hexes need to be recreated for each object, since scale varies.
    private GUIBattleHex [][] h = new GUIBattleHex[6][6];
    private java.util.List hexes = new ArrayList(33);

    // The game state hexes can be set up once for each terrain type.
    private static Map terrainH = new HashMap();
    private static Map terrainHexes = new HashMap();
    private static Map entranceHexes = new HashMap();
    private static Map startlistMap = new HashMap();
    private static Map subtitleMap = new HashMap();
    private static Map towerStatusMap = new HashMap();
    private static Map hazardNumberMap = new HashMap();
    private static Map hazardSideNumberMap = new HashMap();

    /* map from the terrain name to the String represneting the terrain after it has been randomized. String is in BattlelandLoader format. */
    private static Map randomizedTerrainMap = new HashMap();

    /** ne, e, se, sw, w, nw */
    private GUIBattleHex [] entrances = new GUIBattleHex[6];

    private static final boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };


    int scale = 2 * Scale.get();
    int cx = 6 * scale;
    int cy = 2 * scale;


    HexMap(String masterHexLabel)
    {
        this.masterHexLabel = masterHexLabel;
        this.terrain = getMasterHex().getTerrain();

        setOpaque(true);
        setBackground(Color.white);
        setupHexes();
    }


    /** Set up a static non-GUI hex map for each terrain type. */
    public static void staticBattlelandsInit()
    {
        terrainH.clear();
        terrainHexes.clear();
        entranceHexes.clear();
        startlistMap.clear();
        subtitleMap.clear();
        towerStatusMap.clear();
        hazardNumberMap.clear();
        hazardSideNumberMap.clear();
        randomizedTerrainMap.clear();

        String[] terrains = TerrainRecruitLoader.getTerrains();
        for (int t = 0; t < terrains.length; t++)
        {
            String terrain = terrains[t];

            BattleHex [][] gameH = new BattleHex[6][6];
            java.util.List gameHexes = new ArrayList();

            // Initialize game state hex array.
            for (int i = 0; i < gameH.length; i++)
            {
                for (int j = 0; j < gameH[0].length; j++)
                {
                    if (show[i][j])
                    {
                        BattleHex hex = new BattleHex(i, j);

                        gameH[i][j] = hex;
                        gameHexes.add(hex);
                    }
                }
            }
            setupNeighbors(gameH);
            setupHexesGameState(terrain, gameH);

            // Initialize non-GUI entrances
            BattleHex[] gameEntrances = new BattleHex[6];
            for (int k = 0; k < 6; k++)
            {
                gameEntrances[k] = new BattleHex(-1, k);
                gameHexes.add(gameEntrances[k]);
            }
            setupEntrancesGameState(gameEntrances, gameH);
            entranceHexes.put(terrain, gameEntrances);
            // Add hexes to both the [][] and ArrayList maps.
            terrainH.put(terrain, gameH);
            terrainHexes.put(terrain, gameHexes);
        }
    }


    MasterHex getMasterHex()
    {
        return MasterBoard.getHexByLabel(masterHexLabel);
    }


    void setupHexes()
    {
        setupHexesGUI();
        setupNeighbors(h);
        setupHexesGameState(terrain, h);
        setupEntrances();
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
                    GUIBattleHex hex = new GUIBattleHex
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
    private static synchronized void setupHexesGameState(String terrain, 
        BattleHex [][] h)
    {
        java.util.List directories = 
            VariantSupport.getBattlelandsDirectoriesList();
        String rndSourceName =
            TerrainRecruitLoader.getTerrainRandomName(terrain);
        try
        {
            if (rndSourceName == null)
            { // static Battlelands
                InputStream batIS =
                    ResourceLoader.getInputStream(terrain,
                                                  directories);
                BattlelandLoader bl = new BattlelandLoader(batIS);
                while (bl.oneBattlelandCase(h) >= 0) {}
                java.util.List tempTowerStartList = bl.getStartList();
                if (tempTowerStartList != null)
                {
                    startlistMap.put(terrain,
                                     tempTowerStartList);
                }
                towerStatusMap.put(terrain,
                                   new Boolean(bl.isTower()));
                subtitleMap.put(terrain, bl.getSubtitle());
            }
            else
            { // random Battlelands
                String mapData = (String)randomizedTerrainMap.get(terrain);
                if (mapData == null)
                {
                    Log.debug("Randomizing " + terrain +
                              " with " + rndSourceName);
                    InputStream brlIS =
                        ResourceLoader.getInputStream(rndSourceName,
                                                      directories);
                    BattlelandRandomizerLoader brl =
                        new BattlelandRandomizerLoader(brlIS);
                    while (brl.oneArea(h) >= 0) {}
                    brl.resolveAllHexsides(h);
                    towerStatusMap.put(terrain,
                                       new Boolean(false));
                    subtitleMap.put(terrain, null);
                    StringBuffer buf = new StringBuffer();
                    for (int i = 0; i < 6; i++)
                    {
                        for (int j = 0 ; j < 6 ; j++)
                        {
                            if (show[i][j])
                            {
                                boolean doDumpSides = false;
                                String haz = h[i][j].getTerrain();
                                int e = h[i][j].getElevation();
                                for (int k = 0; k < 6; k++)
                                {
                                    char s = h[i][j].getHexside(k);
                                    if (s != ' ')
                                        doDumpSides = true;
                                }
                                if (doDumpSides ||
                                    (!haz.equals("Plains")) ||
                                    (e != 0))
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
                    randomizedTerrainMap.put(terrain, new String(buf));
                }
                else
                {
                    InputStream batIS = new ByteArrayInputStream(mapData.getBytes());
                    BattlelandLoader bl = new BattlelandLoader(batIS);
                    while (bl.oneBattlelandCase(h) >= 0) {}
                    java.util.List tempTowerStartList = bl.getStartList();
                    if (tempTowerStartList != null)
                    {
                        startlistMap.put(terrain,
                                         tempTowerStartList);
                    }
                    towerStatusMap.put(terrain,
                                       new Boolean(false));
                    subtitleMap.put(terrain, null);
                }
            }
            
            /* count all hazards & hazard sides */
            /* slow & inefficient... */
            String[] hazards = BattleHex.getTerrains();
            HashMap t2n = new HashMap();
            for (int i = 0; i < hazards.length; i++)
            {
                int count = 0;
                for (int x = 0 ; x < 6; x++)
                {
                    for (int y = 0; y < 6; y++)
                    {
                        if (show[x][y])
                        {
                            if (h[x][y].getTerrain().equals(hazards[i]))
                                count++;
                        }
                    }
                }
                if (count >0)
                    t2n.put(hazards[i], new Integer(count));
            }
            hazardNumberMap.put(terrain, t2n);
            char[] hazardSides = BattleHex.getHexsides();
            HashMap s2n = new HashMap();
            for (int i = 0; i < hazardSides.length; i++)
            {
                int count = 0;
                for (int x = 0 ; x < 6; x++)
                {
                    for (int y = 0; y < 6; y++)
                    {
                        if (show[x][y])
                        {
                            for (int k = 0; k < 6; k++)
                            {
                                if (h[x][y].getHexside(k) ==
                                    hazardSides[i])
                                    count++;
                            }
                        }
                    }
                }
                if (count >0)
                    s2n.put(new Character(hazardSides[i]), new Integer(count));
            }
            hazardSideNumberMap.put(terrain, s2n);
        }
        catch (Exception e) 
        {
            Log.error("Battlelands loading failed : " + e);
        }
    }


    /** Add references to neighbor hexes. */
    private static void setupNeighbors(BattleHex [][] h)
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

    private static void setupEntrancesGameState(BattleHex [] entrances,
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
            GUIBattleHex hex = (GUIBattleHex)it.next();
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
            GUIBattleHex hex = (GUIBattleHex)it.next();
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
            GUIBattleHex hex = (GUIBattleHex)it.next();
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
            GUIBattleHex hex = (GUIBattleHex)it.next();
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
            GUIBattleHex hex = (GUIBattleHex)it.next();
            if (!hex.isSelected() && labels.contains(hex.getLabel()))
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
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = (GUIBattleHex)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }

        Log.error("Could not find GUIBattleHex " + label);
        return null;
    }

    /** Look for the Hex matching the Label in the terrain static map */
    public static BattleHex getHexByLabel(String terrain, String label)
    {
        int x = 0, y = Integer.parseInt(new String(label.substring(1)));
        switch (label.charAt(0))
        {
        case 'A':
        case 'a':
            x = 0;
            break;
        case 'B':
        case 'b':
            x = 1;
            break;
        case 'C':
        case 'c':
            x = 2;
            break;
        case 'D':
        case 'd':
            x = 3;
            break;
        case 'E':
        case 'e':
            x = 4;
            break;
        case 'F':
        case 'f':
            x = 5;
            break;
        case 'X':
        case 'x':
            /* entrances */
            BattleHex[] gameEntrances =
                (BattleHex[])entranceHexes.get(terrain);
            return gameEntrances[y];
        default:
            Log.error("Label " + label + " is invalid");
        }
        y = 6 - y - (int)Math.abs(((x - 3) / 2));
        BattleHex[][] correctHexes = (BattleHex[][])terrainH.get(terrain);
        return correctHexes[x][y];
    }

    /** Return the GUIBattleHex that contains the given point, or
     *  null if none does. */
    GUIBattleHex getHexContainingPoint(Point point)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = (GUIBattleHex)it.next();
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

    public static java.util.List getTowerStartList(String terrain)
    {
        return (java.util.List)startlistMap.get(terrain);
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
        try
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
    
            // Abort if called too early.
            Rectangle rectClip = g.getClipBounds();
            if (rectClip == null)
            {
                return;
            }
    
            Iterator it = hexes.iterator();
            while (it.hasNext())
            {
                GUIBattleHex hex = (GUIBattleHex)it.next();
                if (!hex.isEntrance() && rectClip.intersects(hex.getBounds()))
                {
                    hex.paint(g);
                }
            }
    
            /* always antialias this, the font is huge */
            Object oldantialias = g2.getRenderingHint(
                RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
    
            Font oldFont = g.getFont();
            FontMetrics fm;
            String dn = getMasterHex().getTerrainDisplayName();
            String bn = getMasterHex().getTerrainName();
            String sub = (String)subtitleMap.get(terrain);
    
            if (sub == null)
                sub = (dn.equals(bn) ? null : bn);
            
            g.setFont(ResourceLoader.defaultFont.deriveFont((float)48));
            fm = g.getFontMetrics();
            int tma = fm.getMaxAscent();
            g.drawString(dn, 80, 4 + tma);
            
            if (sub != null)
            {
                g.setFont(ResourceLoader.defaultFont.deriveFont((float)24));
                fm = g.getFontMetrics();
                int tma2 = fm.getMaxAscent();
                g.drawString(sub, 80, 4 + tma + 8 + tma2);
            }
            /* reset antialiasing */
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                oldantialias);
            g.setFont(oldFont);
        }
        catch (NullPointerException ex)
        {
            // If we try to paint before something is loaded, just retry later.
        }
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(60 * Scale.get(), 55 * Scale.get());
    }

    public boolean terrainIsTower()
    {
        return terrainIsTower(terrain);
    }

    public static boolean terrainIsTower(String t)
    {
        boolean temp =
            ((Boolean)towerStatusMap.get(t)).booleanValue();
        return temp;
    }

    public boolean terrainHasStartlist()
    {
        return terrainHasStartlist(terrain);
    }

    public static boolean terrainHasStartlist(String t)
    {
        java.util.List temp =
            (java.util.List)startlistMap.get(t);
        return (!(temp == null));
    }

    public static int getHazardCountInTerrain(String hazard, String terrain)
    {
        HashMap t2n = (HashMap)hazardNumberMap.get(terrain);
        Object o = null;

        if (t2n == null)
        {
            Log.debug("Terrain " + terrain +
                      " doesn't exist in this variant.");
        }
        else
        {
            o = t2n.get(hazard);
        }

        if (o == null)
            return 0;
        
        Integer number = (Integer)o;

        return (number.intValue());
    }
    
    public static int getHazardSideCountInTerrain(char hazard,
                                                  String terrain)
    {
        HashMap s2n = (HashMap)hazardSideNumberMap.get(terrain);
        Object o = null;
        
        if (s2n == null)
        {
            Log.debug("Terrain " + terrain +
                      " doesn't exist in this variant.");
        }
        else
        {
            o = s2n.get(new Character(hazard));
        }
        
        if (o == null)
            return 0;
        
        Integer number = (Integer)o;

        return (number.intValue());
    }
}
