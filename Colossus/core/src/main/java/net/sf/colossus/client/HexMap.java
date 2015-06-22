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
import java.util.Collection;
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
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.BattlelandLoader;


/**
 * Class HexMap displays a basic battle map.
 *
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

    /** ne, e, se, sw, w, nw */
    private final GUIBattleHex[] entrances = new GUIBattleHex[6];

    public static final boolean[][] VISIBLE_HEXES = {
        { false, false, true, true, true, false },
        { false, true, true, true, true, false },
        { false, true, true, true, true, true },
        { true, true, true, true, true, true },
        { false, true, true, true, true, true },
        { false, true, true, true, true, false } };

    private final int scale = 2 * Scale.get();
    protected final int cx = 6 * scale;
    protected final int cy = 2 * scale;

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
                if (VISIBLE_HEXES[i][j])
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
                if (VISIBLE_HEXES[i][j])
                {
                    hexModel[i][j] = new BattleHex(i, j);
                }
            }
        }
        try
        {
            // if ((rndSourceName == null) || (!serverSideFirstLoad))
            { // static Battlelands
                InputStream batIS = StaticResourceLoader.getInputStream(
                    masterBoardTerrain.getId() + ".xml", directories);

                BattlelandLoader bl = new BattlelandLoader(batIS, hexModel);
                List<String> tempTowerStartList = bl.getStartList();
                masterBoardTerrain.setStartList(tempTowerStartList);
                masterBoardTerrain.setTower(bl.isTower());
                masterBoardTerrain.setSubtitle(bl.getSubtitle());
            }

            /* slow & inefficient... */
            Map<HazardTerrain, Integer> t2n = new HashMap<HazardTerrain, Integer>();
            for (HazardTerrain hTerrain : HazardTerrain.getAllHazardTerrains())
            {
                int count = 0;
                for (int x = 0; x < 6; x++)
                {
                    for (int y = 0; y < 6; y++)
                    {
                        if (VISIBLE_HEXES[x][y])
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
            Collection<HazardHexside> hazardTypes = HazardHexside
                .getAllHazardHexsides();

            // old way
            Map<Character, Integer> s2n = new HashMap<Character, Integer>();
            // new way
            Map<HazardHexside, Integer> h2n = new HashMap<HazardHexside, Integer>();

            for (HazardHexside hazard : hazardTypes)
            {
                int count = 0;
                for (int x = 0; x < 6; x++)
                {
                    for (int y = 0; y < 6; y++)
                    {
                        if (VISIBLE_HEXES[x][y])
                        {
                            for (int k = 0; k < 6; k++)
                            {
                                if (hexModel[x][y].getHexsideHazard(k) == hazard)
                                {
                                    count++;
                                }
                            }
                        }
                    }
                }
                char side = hazard.getCode();
                // old way
                s2n.put(Character.valueOf(side), Integer.valueOf(count));
                // new way
                h2n.put(hazard, Integer.valueOf(count));
            }
            masterBoardTerrain.setHazardSideNumberMap(s2n);
            masterBoardTerrain.setHexsideHazardNumberMap(h2n);
            // map model into GUI
            for (int i = 0; i < hexModel.length; i++)
            {
                BattleHex[] row = hexModel[i];
                for (int j = 0; j < row.length; j++)
                {
                    BattleHex hex = row[j];
                    if (VISIBLE_HEXES[i][j])
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
                if (VISIBLE_HEXES[i][j])
                {
                    if (j > 0 && VISIBLE_HEXES[i][j - 1])
                    {
                        h[i][j].setNeighbor(0, h[i][j - 1]);
                    }

                    if (i < 5 && VISIBLE_HEXES[i + 1][j - ((i + 1) & 1)])
                    {
                        h[i][j].setNeighbor(1, h[i + 1][j - ((i + 1) & 1)]);
                    }

                    if (i < 5 && j + (i & 1) < 6
                        && VISIBLE_HEXES[i + 1][j + (i & 1)])
                    {
                        h[i][j].setNeighbor(2, h[i + 1][j + (i & 1)]);
                    }

                    if (j < 5 && VISIBLE_HEXES[i][j + 1])
                    {
                        h[i][j].setNeighbor(3, h[i][j + 1]);
                    }

                    if (i > 0 && j + (i & 1) < 6
                        && VISIBLE_HEXES[i - 1][j + (i & 1)])
                    {
                        h[i][j].setNeighbor(4, h[i - 1][j + (i & 1)]);
                    }

                    if (i > 0 && VISIBLE_HEXES[i - 1][j - ((i + 1) & 1)])
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
    }

    private void setupEntrancesGUI()
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

    protected void selectHex(BattleHex battleHex)
    {
        for (GUIBattleHex hex : hexes)
        {
            if (!hex.isSelected() && battleHex.equals(hex.getHexModel()))
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
            if (!hex.isSelected() && battleHexes.contains(hex.getHexModel()))
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
        for (GUIBattleHex hex : hexes)
        {
            if (hex.getHexModel().getLabel().equals(battleHex.getLabel()))
            {
                return hex;
            }
        }

        assert false : "Could not find GUIBattleHex for "
            + battleHex.getLabel();
        LOGGER.log(Level.SEVERE,
            "Could not find GUIBattleHex " + battleHex.getLabel());
        return null;
    }

    public BattleHex getHexByLabel(String hexLabel)
    {
        return masterHex.getTerrain().getHexByLabel(hexLabel);
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

            g.setFont(StaticResourceLoader.DEFAULT_FONT.deriveFont((float)48));
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
                g.setFont(StaticResourceLoader.DEFAULT_FONT
                    .deriveFont((float)24));
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
