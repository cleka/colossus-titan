package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.xmlparser.BattlelandLoader;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Class HexMap displays a basic battle map.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class HexMap extends JPanel implements MouseListener, WindowListener
{
    private static final Logger LOGGER = Logger.getLogger(HexMap.class
        .getName());

    private final String masterHexLabel;
    private final String terrain;

    // GUI hexes need to be recreated for each object, since scale varies.
    private final GUIBattleHex[][] h = new GUIBattleHex[6][6];
    private final List<GUIBattleHex> hexes = new ArrayList<GUIBattleHex>(33);

    // The game state hexes can be set up once for each terrain type.
    private static Map<String, GUIBattleHex[][]> terrainH = new HashMap<String, GUIBattleHex[][]>();
    private static Map<String, List<GUIBattleHex>> terrainHexes = new HashMap<String, List<GUIBattleHex>>();
    private static Map<String, GUIBattleHex[]> entranceHexes = new HashMap<String, GUIBattleHex[]>();
    private static Map<String, List<String>> startlistMap = new HashMap<String, List<String>>();
    private static Map<String, String> subtitleMap = new HashMap<String, String>();
    private static Map<String, Boolean> towerStatusMap = new HashMap<String, Boolean>();
    private static Map<String, HashMap<HazardTerrain, Integer>> hazardNumberMap = new HashMap<String, HashMap<HazardTerrain, Integer>>();
    private static Map<String, HashMap<Character, Integer>> hazardSideNumberMap = new HashMap<String, HashMap<Character, Integer>>();

    /** ne, e, se, sw, w, nw */
    private final GUIBattleHex[] entrances = new GUIBattleHex[6];

    private static final boolean[][] show = {
        { false, false, true, true, true, false },
        { false, true, true, true, true, false },
        { false, true, true, true, true, true },
        { true, true, true, true, true, true },
        { false, true, true, true, true, true },
        { false, true, true, true, true, false } };

    int scale = 2 * Scale.get();
    int cx = 6 * scale;
    int cy = 2 * scale;

    HexMap(String masterHexLabel)
    {
        this.masterHexLabel = masterHexLabel;
        this.terrain = getMasterHex().getTerrain();

        setOpaque(true);
        setLayout(null); // we want to manage things ourselves
        setBackground(Color.white);
        setupHexes();
    }

    /** Set up a static non-GUI hex map for each terrain type. */
    public static void staticBattlelandsInit(boolean serverSideFirstLoad)
    {
        terrainH.clear();
        terrainHexes.clear();
        entranceHexes.clear();
        startlistMap.clear();
        subtitleMap.clear();
        towerStatusMap.clear();
        hazardNumberMap.clear();
        hazardSideNumberMap.clear();

        String[] terrains = TerrainRecruitLoader.getTerrains();
        for (int t = 0; t < terrains.length; t++)
        {
            String terrain = terrains[t];

            GUIBattleHex[][] gameH = new GUIBattleHex[6][6];
            List<GUIBattleHex> gameHexes = new ArrayList<GUIBattleHex>();

            // Initialize game state hex array.
            for (int i = 0; i < gameH.length; i++)
            {
                for (int j = 0; j < gameH[0].length; j++)
                {
                    if (show[i][j])
                    {
                        GUIBattleHex hex = new GUIBattleHex(i, j);

                        gameH[i][j] = hex;
                        gameHexes.add(hex);
                    }
                }
            }
            setupHexesGameState(terrain, gameH, serverSideFirstLoad);
            setupNeighbors(gameH);

            // Initialize non-GUI entrances
            GUIBattleHex[] gameEntrances = new GUIBattleHex[6];
            for (int k = 0; k < 6; k++)
            {
                gameEntrances[k] = new GUIBattleHex(-1, k);
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
        setupHexesGameState(terrain, h, false);
        setupNeighbors(h);
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
                    GUIBattleHex hex = new GUIBattleHex(Math.round(cx + 3 * i
                        * scale), (int)Math.round(cy + (2 * j + (i & 1))
                        * GUIHex.SQRT3 * scale), scale, this, i, j);

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
        GUIBattleHex[][] h, boolean serverSideFirstLoad)
    {
        List directories = VariantSupport.getBattlelandsDirectoriesList();
        String rndSourceName = TerrainRecruitLoader
            .getTerrainRandomName(terrain);
        BattleHex[][] hexModel = new BattleHex[h.length][h[0].length];
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    hexModel[i][j] = new BattleHex(i, j);
                }
            }
        }
        try
        {
            if ((rndSourceName == null) || (!serverSideFirstLoad))
            { // static Battlelands
                InputStream batIS = ResourceLoader.getInputStream(terrain
                    + ".xml", directories);

                BattlelandLoader bl = new BattlelandLoader(batIS, hexModel);
                List<String> tempTowerStartList = bl.getStartList();
                if (tempTowerStartList != null)
                {
                    startlistMap.put(terrain, tempTowerStartList);
                }
                towerStatusMap.put(terrain, new Boolean(bl.isTower()));
                subtitleMap.put(terrain, bl.getSubtitle());
            }
            else
            {// random Battlelands

                /*  Commented out until ported to XML
                 Log.debug("Randomizing " + terrain + " with " + rndSourceName);
                 InputStream brlIS =
                 ResourceLoader.getInputStream(rndSourceName,
                 directories);
                 BattlelandRandomizerLoader brl =
                 new BattlelandRandomizerLoader(brlIS);
                 while (brl.oneArea(h) >= 0)
                 {
                 }
                 brl.resolveAllHexsides(h);
                 List tempTowerStartList = brl.getStartList();
                 if (tempTowerStartList != null)
                 {
                 startlistMap.put(terrain, tempTowerStartList);
                 }
                 towerStatusMap.put(terrain, new Boolean(brl.isTower()));
                 subtitleMap.put(terrain, null);
                 StringBuffer buf = new StringBuffer();
                 for (int i = 0; i < 6; i++)
                 {
                 for (int j = 0; j < 6; j++)
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
                 {
                 doDumpSides = true;
                 }
                 }
                 if (doDumpSides ||
                 (!haz.equals("Plains")) ||
                 (e != 0))
                 {
                 buf.append(i + " " + j + " ");
                 buf.append(h[i][j].getTerrain());
                 buf.append(" ");
                 buf.append(h[i][j].getElevation());
                 if (doDumpSides)
                 {
                 for (int k = 0; k < 6; k++)
                 {
                 if (h[i][j].getHexside(k) != ' ')
                 {
                 buf.append(" " + k + " ");
                 buf.append(h[i][j].getHexside(k));
                 }
                 }
                 }
                 buf.append("\n");
                 }
                 }
                 }
                 }
                 if (brl.isTower())
                 {
                 buf.append("TOWER\n");
                 }
                 if (tempTowerStartList != null)
                 {
                 buf.append("STARTLIST");
                 Iterator it = tempTowerStartList.iterator();
                 while (it.hasNext())
                 {
                 String label = (String)it.next();
                 buf.append(" " + label);
                 }
                 buf.append("\n");
                 }
                 ResourceLoader.putIntoFileCache(terrain, directories,
                 (new String(buf)).getBytes());
                 */
            }

            /* count all hazards & hazard sides */

            /* slow & inefficient... */
            HashMap<HazardTerrain, Integer> t2n = new HashMap<HazardTerrain, Integer>();
            for (Iterator<HazardTerrain> iterator = HazardTerrain
                .getAllHazardTerrains().iterator(); iterator.hasNext();)
            {
                HazardTerrain hTerrain = iterator.next();
                int count = 0;
                for (int x = 0; x < 6; x++)
                {
                    for (int y = 0; y < 6; y++)
                    {
                        if (show[x][y])
                        {
                            if (hexModel[x][y].getTerrain().equals(hTerrain))
                            {
                                count++;
                            }
                        }
                    }
                }
                if (count > 0)
                {
                    t2n.put(hTerrain, new Integer(count));
                }
            }
            hazardNumberMap.put(terrain, t2n);
            char[] hazardSides = BattleHex.getHexsides();
            HashMap<Character, Integer> s2n = new HashMap<Character, Integer>();
            for (int i = 0; i < hazardSides.length; i++)
            {
                int count = 0;
                for (int x = 0; x < 6; x++)
                {
                    for (int y = 0; y < 6; y++)
                    {
                        if (show[x][y])
                        {
                            for (int k = 0; k < 6; k++)
                            {
                                if (hexModel[x][y].getHexside(k) == hazardSides[i])
                                {
                                    count++;
                                }
                            }
                        }
                    }
                }
                if (count > 0)
                {
                    s2n.put(new Character(hazardSides[i]), new Integer(count));
                }
            }
            hazardSideNumberMap.put(terrain, s2n);
            // map model into GUI
            for (int i = 0; i < hexModel.length; i++)
            {
                BattleHex[] row = hexModel[i];
                for (int j = 0; j < row.length; j++)
                {
                    BattleHex hex = row[j];
                    if (show[i][j])
                    {
                        h[i][j].setHexModel(hex);
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Battleland " + terrain
                + " loading failed.", e);
            e.printStackTrace();
        }
    }

    /** Add references to neighbor hexes. */
    private static void setupNeighbors(GUIBattleHex[][] h)
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

    private void setupEntrances()
    {
        setupEntrancesGUI();
        setupEntrancesGameState(entrances, h);
    }

    private void setupEntrancesGUI()
    {
        // Initialize entrances.
        entrances[0] = new GUIBattleHex(cx + 15 * scale, Math.round(cy + 1
            * scale), scale, this, -1, 0);
        entrances[1] = new GUIBattleHex(cx + 21 * scale, Math.round(cy + 10
            * scale), scale, this, -1, 1);
        entrances[2] = new GUIBattleHex(cx + 17 * scale, Math.round(cy + 22
            * scale), scale, this, -1, 2);
        entrances[3] = new GUIBattleHex(cx + 2 * scale, Math.round(cy + 21
            * scale), scale, this, -1, 3);
        entrances[4] = new GUIBattleHex(cx - 3 * scale, Math.round(cy + 10
            * scale), scale, this, -1, 4);
        entrances[5] = new GUIBattleHex(cx + 1 * scale, Math.round(cy + 1
            * scale), scale, this, -1, 5);

        hexes.add(entrances[0]);
        hexes.add(entrances[1]);
        hexes.add(entrances[2]);
        hexes.add(entrances[3]);
        hexes.add(entrances[4]);
        hexes.add(entrances[5]);
    }

    private static void setupEntrancesGameState(GUIBattleHex[] entrances,
        GUIBattleHex[][] h)
    {
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
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
            if (hex.isSelected())
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    void unselectHexByLabel(String label)
    {
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
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
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
            if (hex.isSelected()
                && labels.contains(hex.getHexModel().getLabel()))
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    void selectHexByLabel(String label)
    {
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
            if (!hex.isSelected()
                && label.equals(hex.getHexModel().getLabel()))
            {
                hex.select();
                hex.repaint();
                return;
            }
        }
    }

    void selectHexesByLabels(Set labels)
    {
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
            if (!hex.isSelected()
                && labels.contains(hex.getHexModel().getLabel()))
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
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
            if (hex.getHexModel().getLabel().equals(label))
            {
                return hex;
            }
        }

        LOGGER.log(Level.SEVERE, "Could not find GUIBattleHex " + label);
        return null;
    }

    /** Look for the Hex matching the Label in the terrain static map */
    public static BattleHex getHexByLabel(String terrain, String label)
    {
        int x = 0;
        int y = Integer.parseInt(new String(label.substring(1)));
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
                GUIBattleHex[] gameEntrances = entranceHexes.get(terrain);
                return gameEntrances[y].getBattleHexModel();

            default:
                LOGGER.log(Level.SEVERE, "Label " + label + " is invalid");
        }
        y = 6 - y - Math.abs((x - 3) / 2);
        GUIBattleHex[][] correctHexes = terrainH.get(terrain);
        return correctHexes[x][y].getBattleHexModel();
    }

    /** Return the GUIBattleHex that contains the given point, or
     *  null if none does. */
    GUIBattleHex getHexContainingPoint(Point point)
    {
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
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
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
            set.add(hex.getBattleHexModel().getLabel());
        }
        return set;
    }

    public static List getTowerStartList(String terrain)
    {
        return startlistMap.get(terrain);
    }

    public void mousePressed(MouseEvent e)
    {
        // necessary to implement interface
    }

    public void mouseReleased(MouseEvent e)
    {
        // necessary to implement interface
    }

    public void mouseClicked(MouseEvent e)
    {
        // necessary to implement interface
    }

    public void mouseEntered(MouseEvent e)
    {
        // necessary to implement interface
    }

    public void mouseExited(MouseEvent e)
    {
        // necessary to implement interface
    }

    public void windowActivated(WindowEvent e)
    {
        // necessary to implement interface
    }

    public void windowClosed(WindowEvent e)
    {
        // necessary to implement interface
    }

    public void windowClosing(WindowEvent e)
    {
        // necessary to implement interface
    }

    public void windowDeactivated(WindowEvent e)
    {
        // necessary to implement interface
    }

    public void windowDeiconified(WindowEvent e)
    {
        // necessary to implement interface
    }

    public void windowIconified(WindowEvent e)
    {
        // necessary to implement interface
    }

    public void windowOpened(WindowEvent e)
    {
        // necessary to implement interface
    }

    @Override
    public void paintComponent(Graphics g)
    {
        // TODO the hexes should be on a separate background component
        // that is below the other components, then we wouldn't need
        // complicated drawing code doing it all ourselves
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

            Iterator<GUIBattleHex> it = hexes.iterator();
            while (it.hasNext())
            {
                GUIBattleHex hex = it.next();
                if (!hex.getBattleHexModel().isEntrance()
                    && rectClip.intersects(hex.getBounds()))
                {
                    hex.paint(g);
                }
            }

            /* always antialias this, the font is huge */
            Object oldantialias = g2
                .getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            // TODO this should probably be a standard JLabel placed properly
            Font oldFont = g.getFont();
            FontMetrics fm;
            String dn = getMasterHex().getTerrainDisplayName();
            String bn = getMasterHex().getTerrainName();
            String sub = subtitleMap.get(terrain);

            if (sub == null)
            {
                sub = (dn.equals(bn) ? null : bn);
            }

            g.setFont(ResourceLoader.defaultFont.deriveFont((float)48));
            fm = g.getFontMetrics();
            int tma = fm.getMaxAscent();

            // calculate needed space, set xPos so that it's drawn 
            // right-aligned 80 away from right window border.
            Rectangle2D bounds = fm.getStringBounds(dn, g);
            int width = (int)bounds.getWidth();
            int windowWidth = super.getWidth();
            int xPos = windowWidth - 80 - width;
            g.drawString(dn, xPos, 4 + tma);

            if (sub != null)
            {
                g.setFont(ResourceLoader.defaultFont.deriveFont((float)24));
                fm = g.getFontMetrics();
                int tma2 = fm.getMaxAscent();
                bounds = fm.getStringBounds(sub, g);
                width = (int)bounds.getWidth();
                windowWidth = super.getWidth();
                xPos = windowWidth - 80 - width;
                g.drawString(sub, xPos, 4 + tma + 8 + tma2);
            }

            /* reset antialiasing */
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldantialias);
            g.setFont(oldFont);
        }
        catch (NullPointerException ex)
        {
            // If we try to paint before something is loaded, just retry later.
        }
    }

    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    @Override
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
        try
        {
            return towerStatusMap.get(t).booleanValue();
        }
        catch (NullPointerException ex)
        // XXX Called too early, before towerStatusMap is setup?
        {
            LOGGER.log(Level.SEVERE, "Null pointer exception caught", ex);
            return false;
        }
    }

    public boolean terrainHasStartlist()
    {
        return terrainHasStartlist(terrain);
    }

    public static boolean terrainHasStartlist(String t)
    {
        java.util.List temp = startlistMap.get(t);
        return (!(temp == null));
    }

    public static int getHazardCountInTerrain(HazardTerrain hazard,
        String terrain)
    {
        HashMap t2n = hazardNumberMap.get(terrain);
        Object o = null;

        if (t2n == null)
        {

            /*
             Log.debug("Terrain " + terrain +
             " doesn't exist in this variant.");
             */
        }
        else
        {
            o = t2n.get(hazard);
        }

        if (o == null)
        {
            return 0;
        }

        Integer number = (Integer)o;

        return (number.intValue());
    }

    public static int getHazardSideCountInTerrain(char hazard, String terrain)
    {
        HashMap s2n = hazardSideNumberMap.get(terrain);
        Object o = null;

        if (s2n == null)
        {

            /*
             Log.debug("Terrain " + terrain +
             " doesn't exist in this variant.");
             */
        }
        else
        {
            o = s2n.get(new Character(hazard));
        }

        if (o == null)
        {
            return 0;
        }

        Integer number = (Integer)o;

        return (number.intValue());
    }
}
