package net.sf.colossus.variant;


import java.awt.Color;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.HexMap;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.xmlparser.BattlelandLoader;


/**
 * A master board terrain.
 *
 * This class describes a terrain on the master board, including its name, color and the
 * layout of a generic battle land. It can occur multiple times on a master board layout
 * attached to the {@link MasterHex} class.
 *
 * Battle land information could probably split out into another class, which could then
 * be immutable.
 */
public class MasterBoardTerrain implements Comparable<MasterBoardTerrain>
{
    private static final Logger LOGGER = Logger
        .getLogger(MasterBoardTerrain.class.getName());

    /** The (unique) identifier of this terrain.
     * Should also be used for all Battlelands purpose.
     */
    private final String id;

    /** The name displayed on the Masterboard.
     * Should also be used for all recruiting purpose.
     * WARNING: this is not done that way yet. It shoud be, so that a single
     * name on the Masterboard will represent a single recruiting branch,
     * even if it' backed by several different Battlelands. This would also
     * remove a lot of duplicated entries in the Full Recruit Tree.
     * WIP.
     * ADDITIONAL WARNING: What about variant such as Balrog? The recruitment
     * is Hex-specific, not Terrain-specific...
     */
    private final String displayName;
    /** Subtitle, for the Battlelands. Cosmetic only, but nice */
    private String subtitle;
    private final Color color;
    /** TODO this should be a List<BattleHex> ... or a List<GUIBattleHex> ???
     * If non-null, this is the list of hexes a defending legion will start
     * in, in a similar way to the Tower in the Default variant.
     */
    private List<String> startList;
    /** Whether this is a Tower-like building, with regards to starting the
     * game, not recruiting or defender entering in a non-default location on
     * the Battlemap.
     */
    private boolean isTower;
    private Map<HazardTerrain, Integer> hazardNumberMap;
    // TODO this should be a Map<HazardHexside, Integer>
    private Map<Character, Integer> hazardSideNumberMap;

    // TODO right now we set up both, until all queries can use the new form
    private Map<HazardHexside, Integer> hexsideHazardNumberMap;

    /** The other MasterBoardTerrain using the same recruit tree */
    private final Set<MasterBoardTerrain> aliases = new TreeSet<MasterBoardTerrain>();
    /** Whether this terrain uses another Terrain recruit tree. */
    private final boolean isAlias;

    // TODO it might be worthwhile moving the battle land into a separate class
    private final BattleHex[][] battleHexes = new BattleHex[6][6];
    private final BattleHex[] entrances = new BattleHex[6];

    /** The recruiting tree of this terrain */
    IRecruiting recruitingSubTree;

    public MasterBoardTerrain(String id, String displayName, Color color,
        boolean isAlias)
    {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.subtitle = null;
        this.isAlias = isAlias;
        setupHexArrays();
    }

    private void setupHexArrays()
    {
        // Initialize game state hex array.
        for (int i = 0; i < battleHexes.length; i++)
        {
            for (int j = 0; j < battleHexes[0].length; j++)
            {
                if (HexMap.VISIBLE_HEXES[i][j])
                {
                    BattleHex hex = new BattleHex(i, j);

                    battleHexes[i][j] = hex;
                }
            }
        }
        setupHexesGameState();
        setupNeighbors();
        setupEntrances();
    }

    private void setupEntrances()
    {
        for (int k = 0; k < 6; k++)
        {
            entrances[k] = new BattleHex(-1, k);
        }
        entrances[0].setNeighbor(3, battleHexes[3][0]);
        entrances[0].setNeighbor(4, battleHexes[4][1]);
        entrances[0].setNeighbor(5, battleHexes[5][1]);

        entrances[1].setNeighbor(3, battleHexes[5][1]);
        entrances[1].setNeighbor(4, battleHexes[5][2]);
        entrances[1].setNeighbor(5, battleHexes[5][3]);
        entrances[1].setNeighbor(0, battleHexes[5][4]);

        entrances[2].setNeighbor(4, battleHexes[5][4]);
        entrances[2].setNeighbor(5, battleHexes[4][5]);
        entrances[2].setNeighbor(0, battleHexes[3][5]);

        entrances[3].setNeighbor(5, battleHexes[3][5]);
        entrances[3].setNeighbor(0, battleHexes[2][5]);
        entrances[3].setNeighbor(1, battleHexes[1][4]);
        entrances[3].setNeighbor(2, battleHexes[0][4]);

        entrances[4].setNeighbor(0, battleHexes[0][4]);
        entrances[4].setNeighbor(1, battleHexes[0][3]);
        entrances[4].setNeighbor(2, battleHexes[0][2]);

        entrances[5].setNeighbor(1, battleHexes[0][2]);
        entrances[5].setNeighbor(2, battleHexes[1][1]);
        entrances[5].setNeighbor(3, battleHexes[2][1]);
        entrances[5].setNeighbor(4, battleHexes[3][0]);
    }

    /** Add references to neighbor hexes. */
    private void setupNeighbors()
    {
        for (int i = 0; i < battleHexes.length; i++)
        {
            for (int j = 0; j < battleHexes[0].length; j++)
            {
                if (HexMap.VISIBLE_HEXES[i][j])
                {
                    if (j > 0 && HexMap.VISIBLE_HEXES[i][j - 1])
                    {
                        battleHexes[i][j]
                            .setNeighbor(0, battleHexes[i][j - 1]);
                    }

                    if (i < 5
                        && HexMap.VISIBLE_HEXES[i + 1][j - ((i + 1) & 1)])
                    {
                        battleHexes[i][j].setNeighbor(1, battleHexes[i + 1][j
                            - ((i + 1) & 1)]);
                    }

                    if (i < 5 && j + (i & 1) < 6
                        && HexMap.VISIBLE_HEXES[i + 1][j + (i & 1)])
                    {
                        battleHexes[i][j].setNeighbor(2, battleHexes[i + 1][j
                            + (i & 1)]);
                    }

                    if (j < 5 && HexMap.VISIBLE_HEXES[i][j + 1])
                    {
                        battleHexes[i][j]
                            .setNeighbor(3, battleHexes[i][j + 1]);
                    }

                    if (i > 0 && j + (i & 1) < 6
                        && HexMap.VISIBLE_HEXES[i - 1][j + (i & 1)])
                    {
                        battleHexes[i][j].setNeighbor(4, battleHexes[i - 1][j
                            + (i & 1)]);
                    }

                    if (i > 0
                        && HexMap.VISIBLE_HEXES[i - 1][j - ((i + 1) & 1)])
                    {
                        battleHexes[i][j].setNeighbor(5, battleHexes[i - 1][j
                            - ((i + 1) & 1)]);
                    }
                }
            }
        }
    }

    public BattleHex getEntrance(EntrySide entrySide)
    {
        return getHexByLabel("X" + entrySide.ordinal());
    }

    /** Add terrain, hexsides, elevation, and exits to hexes.
     *  Cliffs are bidirectional; other hexside obstacles are noted
     *  only on the high side, since they only interfere with
     *  uphill movement. */
    private void setupHexesGameState()
    {
        List<String> directories = VariantSupport
            .getBattlelandsDirectoriesList();
        BattleHex[][] hexModel = new BattleHex[battleHexes.length][battleHexes[0].length];
        for (int i = 0; i < battleHexes.length; i++)
        {
            for (int j = 0; j < battleHexes[0].length; j++)
            {
                if (HexMap.VISIBLE_HEXES[i][j])
                {
                    hexModel[i][j] = new BattleHex(i, j);
                }
            }
        }
        try
        {
            // TODO variant loading code does not belong here
            { // static Battlelands
                InputStream batIS = StaticResourceLoader.getInputStream(
                    getId() + ".xml", directories);

                BattlelandLoader bl = new BattlelandLoader(batIS, hexModel);
                List<String> tempTowerStartList = bl.getStartList();
                setStartList(tempTowerStartList);
                setTower(bl.isTower());
                setSubtitle(bl.getSubtitle());
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
                        if (HexMap.VISIBLE_HEXES[x][y])
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
            setHazardNumberMap(t2n);
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
                        if (HexMap.VISIBLE_HEXES[x][y])
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
            setHazardSideNumberMap(s2n);
            setHexsideHazardNumberMap(h2n);
            // map model into GUI
            for (int i = 0; i < hexModel.length; i++)
            {
                BattleHex[] row = hexModel[i];
                for (int j = 0; j < row.length; j++)
                {
                    BattleHex hex = row[j];
                    if (HexMap.VISIBLE_HEXES[i][j])
                    {
                        battleHexes[i][j] = hex;
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Battleland " + this.displayName
                + " loading failed.", e);
            e.printStackTrace();
        }
    }

    /**
     * Look for the Hex matching the Label in this terrain.
     */
    public BattleHex getHexByLabel(String label)
    {
        assert label != null : "We must have a label";
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
                return entrances[y];

            default:
                String message = "Label " + label + " is invalid";
                LOGGER.log(Level.SEVERE, message);
                assert false : message;
        }
        y = 6 - y - Math.abs((x - 3) / 2);
        return battleHexes[x][y];
    }

    public void setRecruitingSubTree(IRecruiting rst)
    {
        this.recruitingSubTree = rst;
    }

    public IRecruiting getRecruitingSubTree()
    {
        return recruitingSubTree;
    }

    public MasterBoardTerrain(String id, String displayName, Color color)
    {
        this(id, displayName, color, false);
    }

    public int compareTo(MasterBoardTerrain m)
    {
        return this.id.compareTo(m.id);
    }

    public void addAlias(MasterBoardTerrain t)
    {
        aliases.add(t);
    }

    public boolean isAlias()
    {
        return isAlias;
    }

    public Set<MasterBoardTerrain> getAliases()
    {
        return Collections.unmodifiableSet(aliases);
    }

    public String getId()
    {
        return id;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public void setSubtitle(String s)
    {
        subtitle = s;
    }

    public Color getColor()
    {
        return color;
    }

    // TODO get rid of dependencies into client package
    public boolean hasNativeCombatBonus(CreatureType creature)
    {
        int bonusHazardCount = 0;
        int bonusHazardSideCount = 0;

        for (HazardTerrain hTerrain : HazardTerrain.getAllHazardTerrains())
        {
            int count = this.getHazardCount(hTerrain);
            if (hTerrain.isNativeBonusTerrain()
                && creature.isNativeIn(hTerrain))
            {
                bonusHazardCount += count;
            }
            else
            {
                if (hTerrain.isNonNativePenaltyTerrain()
                    && !creature.isNativeIn(hTerrain))
                {
                    bonusHazardCount -= count;
                }
            }
        }

        final Collection<HazardHexside> hazardTypes = HazardHexside
            .getAllHazardHexsides();

        for (HazardHexside hazard : hazardTypes)
        {
            int count = this.getHazardHexsideCount(hazard);

            if (hazard.isNativeBonusHexside() && (creature).isNativeAt(hazard))
            {
                bonusHazardSideCount += count;
            }
            else
            {
                if (hazard.isNonNativePenaltyHexside()
                    && !(creature).isNativeAt(hazard))
                {
                    bonusHazardSideCount -= count;
                }
            }
        }
        if (((bonusHazardCount + bonusHazardSideCount) > 0)
            && ((bonusHazardCount >= 3) || (bonusHazardSideCount >= 5)))
        {
            return true;
        }
        return false;
    }

    public void setStartList(List<String> startList)
    {
        this.startList = startList;
    }

    public List<String> getStartList()
    {
        if (startList == null)
        {
            return null;
        }
        return Collections.unmodifiableList(startList);
    }

    public void setTower(boolean isTower)
    {
        this.isTower = isTower;
    }

    public boolean isTower()
    {
        return isTower;
    }

    public boolean hasStartList()
    {
        return startList != null;
    }

    public void setHazardNumberMap(Map<HazardTerrain, Integer> hazardNumberMap)
    {
        this.hazardNumberMap = hazardNumberMap;
    }

    public int getHazardCount(HazardTerrain terrain)
    {
        return hazardNumberMap.get(terrain).intValue();
    }

    public void setHazardSideNumberMap(
        Map<Character, Integer> hazardSideNumberMap)
    {
        this.hazardSideNumberMap = hazardSideNumberMap;
    }

    public int getHazardSideCount(char hazardSide)
    {
        return hazardSideNumberMap.get(Character.valueOf(hazardSide))
            .intValue();
    }

    public void setHexsideHazardNumberMap(
        Map<HazardHexside, Integer> hexsideHazardNumberMap)
    {
        this.hexsideHazardNumberMap = hexsideHazardNumberMap;
    }

    // TODO Keep the old style as paranoid counterCheck and compare results.
    //      If this does now show up problems, the old way can be eliminated
    //      (refactored 2009-04-06 by Clemens)
    public int getHazardHexsideCount(HazardHexside hazard)
    {
        int oldCount = getHazardSideCount(hazard.getCode());
        int newCount = hexsideHazardNumberMap.get(hazard).intValue();
        if (oldCount != newCount)
        {
            LOGGER.warning("Refactored getCount for hexside hazard types "
                + "returns different value (" + newCount + ") than old "
                + "one does (" + oldCount + ")");
        }
        return newCount;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MasterBoardTerrain other = (MasterBoardTerrain)obj;
        if (id == null)
        {
            if (other.id != null)
                return false;
        }
        return id.equals(other.id);
    }
}
