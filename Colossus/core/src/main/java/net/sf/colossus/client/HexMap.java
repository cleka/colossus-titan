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

import net.sf.colossus.gui.GUIBattleHex;
import net.sf.colossus.gui.GUIHex;
import net.sf.colossus.gui.Scale;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.BattlelandLoader;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Class HexMap displays a basic battle map.
 * 
 * TODO it also contains model information, particularly in the static members
 * 
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */
public class HexMap extends JPanel
{
    private static final Logger LOGGER = Logger.getLogger(HexMap.class
        .getName());

    private final MasterHex masterHex;

    // GUI hexes need to be recreated for each object, since scale varies.
    protected final GUIBattleHex[][] h = new GUIBattleHex[6][6];
    protected final List<GUIBattleHex> hexes = new ArrayList<GUIBattleHex>(33);

    // The game state hexes can be set up once for each terrain type.
    private static Map<MasterBoardTerrain, GUIBattleHex[][]> terrainH = new HashMap<MasterBoardTerrain, GUIBattleHex[][]>();
    private static Map<MasterBoardTerrain, GUIBattleHex[]> entranceHexes = new HashMap<MasterBoardTerrain, GUIBattleHex[]>();

    /** ne, e, se, sw, w, nw */
    protected final GUIBattleHex[] entrances = new GUIBattleHex[6];

    protected static final boolean[][] show = {
        { false, false, true, true, true, false },
        { false, true, true, true, true, false },
        { false, true, true, true, true, true },
        { true, true, true, true, true, true },
        { false, true, true, true, true, true },
        { false, true, true, true, true, false } };

    int scale = 2 * Scale.get();
    protected int cx = 6 * scale;
    protected int cy = 2 * scale;

    /* not just a cache of the MasterHex info,
     * but also a way for MasterHex-less subclass
     * to set those informations.
     */
    private String displayName = "undefined";
    private String basicName = "undefined";
    private String subtitle = null;

    public HexMap(MasterHex masterHex)
    {
        this(masterHex, true);
    }

    public HexMap(MasterHex masterHex, boolean doSetup)
    {
        this.masterHex = masterHex;

        setOpaque(true);
        setLayout(null); // we want to manage things ourselves
        setBackground(Color.white);
        if (doSetup)
        {
            setupHexes();
            MasterBoardTerrain terrain = masterHex.getTerrain();
            displayName = terrain.getDisplayName();
            basicName = terrain.getId();
            subtitle = terrain.getSubtitle();
        }
    }

    /** Set up a static non-GUI hex map for each terrain type. */
    public static void staticBattlelandsInit(boolean serverSideFirstLoad)
    {
        terrainH.clear();
        entranceHexes.clear();

        for (MasterBoardTerrain terrain : TerrainRecruitLoader.getTerrains())
        {
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
        }
    }

    protected MasterHex getMasterHex()
    {
        return masterHex;
    }

    protected void setupHexes()
    {
        setupHexesGUI();
        setupHexesGameState(masterHex.getTerrain(), h, false);
        setupNeighbors(h);
        setupEntrances();
    }

    final protected void setupHexesGUI()
    {
        hexes.clear();

        // Initialize hex array.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    GUIBattleHex hex = new GUIBattleHex(cx + 3 * i * scale,
                        (int)Math.round(cy + (2 * j + (i & 1)) * GUIHex.SQRT3
                            * scale), scale, this, i, j);

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
    @SuppressWarnings("unused")
    private static synchronized void setupHexesGameState(
        MasterBoardTerrain masterBoardTerrain, GUIBattleHex[][] h,
        boolean serverSideFirstLoad)
    {
        List<String> directories = VariantSupport
            .getBattlelandsDirectoriesList();
        //String rndSourceName = TerrainRecruitLoader
        //    .getTerrainRandomName(masterBoardTerrain);
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
            // if ((rndSourceName == null) || (!serverSideFirstLoad))
            { // static Battlelands
                InputStream batIS = ResourceLoader.getInputStream(
                    masterBoardTerrain.getId() + ".xml", directories);

                BattlelandLoader bl = new BattlelandLoader(batIS, hexModel);
                List<String> tempTowerStartList = bl.getStartList();
                masterBoardTerrain.setStartList(tempTowerStartList);
                masterBoardTerrain.setTower(bl.isTower());
                masterBoardTerrain.setSubtitle(bl.getSubtitle());
            }
            /*  Commented out until ported to XML
            else
            {// random Battlelands
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
                 StringBuilder buf = new StringBuilder();
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
                 
            }
            */
            /* count all hazards & hazard sides */

            /* slow & inefficient... */
            Map<HazardTerrain, Integer> t2n = new HashMap<HazardTerrain, Integer>();
            for (HazardTerrain hTerrain : HazardTerrain.getAllHazardTerrains())
            {
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
                t2n.put(hTerrain, Integer.valueOf(count));
            }
            masterBoardTerrain.setHazardNumberMap(t2n);
            char[] hazardSides = BattleHex.getHexsides();
            Map<Character, Integer> s2n = new HashMap<Character, Integer>();
            for (char side : hazardSides)
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
                                if (hexModel[x][y].getHexside(k) == side)
                                {
                                    count++;
                                }
                            }
                        }
                    }
                }
                s2n.put(Character.valueOf(side), Integer.valueOf(count));
            }
            masterBoardTerrain.setHazardSideNumberMap(s2n);
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
            LOGGER.log(Level.SEVERE, "Battleland " + masterBoardTerrain
                + " loading failed.", e);
            e.printStackTrace();
        }
    }

    /** Add references to neighbor hexes. */
    final protected static void setupNeighbors(GUIBattleHex[][] h)
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
        setupEntrancesGUI();
        setupEntrancesGameState(entrances, h);
    }

    protected void setupEntrancesGUI()
    {
        // Initialize entrances.
        entrances[0] = new GUIBattleHex(cx + 15 * scale, cy + 1 * scale,
            scale, this, -1, 0);
        entrances[1] = new GUIBattleHex(cx + 21 * scale, cy + 10 * scale,
            scale, this, -1, 1);
        entrances[2] = new GUIBattleHex(cx + 17 * scale, cy + 22 * scale,
            scale, this, -1, 2);
        entrances[3] = new GUIBattleHex(cx + 2 * scale, cy + 21 * scale,
            scale, this, -1, 3);
        entrances[4] = new GUIBattleHex(cx - 3 * scale, cy + 10 * scale,
            scale, this, -1, 4);
        entrances[5] = new GUIBattleHex(cx + 1 * scale, cy + 1 * scale, scale,
            this, -1, 5);

        hexes.add(entrances[0]);
        hexes.add(entrances[1]);
        hexes.add(entrances[2]);
        hexes.add(entrances[3]);
        hexes.add(entrances[4]);
        hexes.add(entrances[5]);
    }

    protected static void setupEntrancesGameState(GUIBattleHex[] entrances,
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

    protected void unselectAllHexes()
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

    protected void unselectHex(BattleHex battleHex)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (hex.isSelected() && battleHex.equals(hex.getHexModel()))
            {
                hex.unselect();
                hex.repaint();
                return;
            }
        }
    }

    protected void unselectHexes(Set<BattleHex> battleHexes)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (hex.isSelected()
                && battleHexes.contains(hex.getHexModel()))
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    protected void selectHex(BattleHex battleHex)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (!hex.isSelected()
                && battleHex.equals(hex.getHexModel()))
            {
                hex.select();
                hex.repaint();
                return;
            }
        }
    }

    protected void selectHexes(Set<BattleHex> battleHexes)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (!hex.isSelected()
                && battleHexes.contains(hex.getHexModel()))
            {
                hex.select();
                hex.repaint();
            }
        }
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null. */
    protected GUIBattleHex getGUIHexByModelHex(BattleHex battleHex)
    {
        for(GUIBattleHex hex: hexes){
            if (hex.getHexModel().getLabel().equals(battleHex.getLabel()))
            {
                return hex;
            }
        }

        assert false: "Could not find GUIBattleHex for " + battleHex.getLabel();
        LOGGER.log(Level.SEVERE, "Could not find GUIBattleHex " + battleHex.getLabel());
        return null;
    }

    /** Look for the Hex matching the Label in the terrain static map */
    public static BattleHex getHexByLabel(MasterBoardTerrain terrain,
        String label)
    {
        assert terrain != null: "We must have a terrain";
        assert label != null: "We must have a label";
        int x = 0;
        int y = Integer.parseInt(label.substring(1));
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
                return gameEntrances[y].getHexModel();

            default:
                String message = "Label " + label + " is invalid";
                LOGGER.log(Level.SEVERE, message);
                assert false: message;
        }
        y = 6 - y - Math.abs((x - 3) / 2);
        GUIBattleHex[][] correctHexes = terrainH.get(terrain);
        return correctHexes[x][y].getHexModel();
    }
    
    public BattleHex getHexByLabel(String hexLabel) 
    {
        return getHexByLabel(masterHex.getTerrain(), hexLabel);
    }

    /** Return the GUIBattleHex that contains the given point, or
     *  null if none does. */
    protected GUIBattleHex getHexContainingPoint(Point point)
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

    protected Set<BattleHex> getAllHexes()
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        Iterator<GUIBattleHex> it = hexes.iterator();
        while (it.hasNext())
        {
            GUIBattleHex hex = it.next();
            set.add(hex.getHexModel());
        }
        return set;
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
                if (!hex.getHexModel().isEntrance()
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

            g.setFont(ResourceLoader.defaultFont.deriveFont((float)48));
            fm = g.getFontMetrics();
            int tma = fm.getMaxAscent();

            // calculate needed space, set xPos so that it's drawn 
            // right-aligned 80 away from right window border.
            Rectangle2D bounds = fm.getStringBounds(getDisplayName(), g);
            int width = (int)bounds.getWidth();
            int windowWidth = super.getWidth();
            int xPos = windowWidth - 80 - width;
            g.drawString(getDisplayName(), xPos, 4 + tma);

            if (getSubtitle() != null)
            {
                g.setFont(ResourceLoader.defaultFont.deriveFont((float)24));
                fm = g.getFontMetrics();
                int tma2 = fm.getMaxAscent();
                bounds = fm.getStringBounds(getSubtitle(), g);
                width = (int)bounds.getWidth();
                windowWidth = super.getWidth();
                xPos = windowWidth - 80 - width;
                g.drawString(getSubtitle(), xPos, 4 + tma + 8 + tma2);
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

    /**
     * @return the basicName
     */
    public String getBasicName()
    {
        return basicName;
    }

    /**
     * @param basicName the basicName to set
     */
    public void setBasicName(String basicName)
    {
        this.basicName = basicName;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * @return the subtitle
     */
    public String getSubtitle()
    {
        return subtitle;
    }

    /**
     * @param subtitle the subtitle to set
     */
    public void setSubtitle(String subtitle)
    {
        this.subtitle = subtitle;
    }
}
