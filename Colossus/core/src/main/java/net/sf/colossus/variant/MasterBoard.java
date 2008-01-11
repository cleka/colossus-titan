package net.sf.colossus.variant;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.HexMap;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.ArrayHelper;
import net.sf.colossus.util.NullCheckPredicate;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.xmlparser.StrategicMapLoader;


/**
 * The master board as part of a variant.
 * 
 * Instances of this class are immutable.
 */
public class MasterBoard
{
    private static final Logger LOGGER = Logger.getLogger(MasterBoard.class
        .getName());

    /**
     * The number of hexes in the widest section.
     */
    private final int horizSize;

    /**
     * The number of hexes in the tallest section.
     */
    private final int vertSize;

    /** 
     * "parity" of the board, so that hexes are displayed the proper way
     */
    private final int boardParity;

    /**
     * TODO do something more OO, don't use arrays, fold {@link #show} into
     * it somehow (even using null seems better than the split).
     */
    private final MasterHex[][] plainHexArray;

    /** 
     * The hexes in the horizSize*vertSize array that actually exist are
     * represented by true.
     */
    private final boolean[][] show;

    /**
     * A Set of label (String) of all Tower hexes.
     * 
     * TODO should probably be objects instead.
     */
    private final Set<String> towerSet;

    /** 
     * The cache used inside 'hexByLabel'.
     * 
     * TODO do we really need this? It doesn't seem to be used much.
     */
    private final Vector<MasterHex> hexByLabelCache = new Vector<MasterHex>();

    /**
     * TODO move loading code out of here, make constructor taking all the values and put
     * method on the loader returning an instance of this class.
     */
    public MasterBoard() throws FileNotFoundException
    {
        List<String> directories = VariantSupport.getVarDirectoriesList();
        InputStream mapIS = ResourceLoader.getInputStream(VariantSupport
            .getMapName(), directories);
        if (mapIS == null)
        {
            throw new FileNotFoundException(VariantSupport.getMapName());
        }
        StrategicMapLoader sml = new StrategicMapLoader(mapIS);
        this.horizSize = sml.getHorizSize();
        this.vertSize = sml.getVertSize();
        this.show = sml.getShow();
        this.plainHexArray = sml.getHexes();
        this.boardParity = computeBoardParity();
        this.towerSet = new HashSet<String>();

        setupExits(plainHexArray);
        setupEntrances(plainHexArray);
        setupHexLabelSides(plainHexArray);
        setupNeighbors(plainHexArray);

        setupTowerSet();
    }

    public int getBoardParity()
    {
        return boardParity;
    }

    public MasterHex[][] getPlainHexArray()
    {
        return plainHexArray;
    }

    public boolean[][] getShow()
    {
        return show;
    }

    public int getHorizSize()
    {
        return horizSize;
    }

    public int getVertSize()
    {
        return vertSize;
    }

    private int computeBoardParity()
    {
        int parity = 0;
        outer: for (int x = 0; x < horizSize; x++)
        {
            for (int y = 0; y < vertSize - 1; y++)
            {
                if (show[x][y] && show[x][y + 1])
                {
                    parity = 1 - ((x + y) & 1);
                    break outer;
                }
            }
        }
        return parity;
    }

    private void setupExits(MasterHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[i].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 3; k++)
                    {
                        if (h[i][j].getBaseExitType(k) != Constants.NONE)
                        {
                            setupOneExit(h, i, j, k);
                        }
                    }
                }
            }
        }
    }

    private void setupOneExit(MasterHex[][] h, int i, int j, int k)
    {
        MasterHex dh = hexByLabel(h[i][j].getBaseExitLabel(k));
        assert dh != null : "null pointer ; i=" + i + ", j=" + j + ", k=" + k;
        if (dh.getXCoord() == i)
        {
            if (dh.getYCoord() == (j - 1))
            {
                h[i][j].setExitType(0, h[i][j].getBaseExitType(k));
            }
            else
            {
                assert dh.getYCoord() == (j + 1) : "bad exit ; i=" + i
                    + ", j=" + j + ", k=" + k;
                h[i][j].setExitType(3, h[i][j].getBaseExitType(k));
            }
        }
        else if (dh.getXCoord() == (i + 1))
        {
            assert dh.getYCoord() == j : "bad exit ; i=" + i + ", j=" + j
                + ", k=" + k;
            h[i][j].setExitType(2 - ((i + j + boardParity) & 1), h[i][j]
                .getBaseExitType(k));
        }
        else
        {
            assert dh.getXCoord() == (i - 1) && dh.getYCoord() == j : "bad exit ; i="
                + i + ", j=" + j + ", k=" + k;
            h[i][j].setExitType(4 + ((i + j + boardParity) & 1), h[i][j]
                .getBaseExitType(k));
        }
    }

    private void setupEntrances(MasterHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 6; k++)
                    {
                        int gateType = h[i][j].getExitType(k);
                        if (gateType != Constants.NONE)
                        {
                            switch (k)
                            {
                                case 0:
                                    h[i][j - 1].setEntranceType(3, gateType);
                                    break;

                                case 1:
                                    h[i + 1][j].setEntranceType(4, gateType);
                                    break;

                                case 2:
                                    h[i + 1][j].setEntranceType(5, gateType);
                                    break;

                                case 3:
                                    h[i][j + 1].setEntranceType(0, gateType);
                                    break;

                                case 4:
                                    h[i - 1][j].setEntranceType(1, gateType);
                                    break;

                                case 5:
                                    h[i - 1][j].setEntranceType(2, gateType);
                                    break;

                                default:
                                    LOGGER.log(Level.SEVERE, "Bogus hexside");
                            }
                        }
                    }
                }
            }
        }
    }

    /** If the shortest hexside closest to the center of the board
     *  is a short hexside, set the label side to it.
     *  Else set the label side to the opposite hexside. */
    private void setupHexLabelSides(MasterHex[][] h)
    {
        // First find the center of the board.
        int width = h.length;
        int height = h[0].length;

        // Subtract 1 to account for 1-based length of 0-based array.
        double midX = (width - 1) / 2.0;
        double midY = (height - 1) / 2.0;

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    double deltaX = i - midX;
                    // Adjust for aspect ratio of h array, which has roughly
                    // twice as many horizontal as vertical elements even
                    // though the board is roughly square.
                    double deltaY = (j - midY) * width / height;

                    double ratio;

                    // Watch for division by zero.
                    if (deltaY == 0)
                    {
                        ratio = deltaX * 99999999;
                    }
                    else
                    {
                        ratio = deltaX / deltaY;
                    }

                    // Derive the exact number if needed.
                    if (Math.abs(ratio) < 0.6)
                    {
                        // Vertically dominated, so top or bottom hexside.
                        // top, unless inverted
                        if (isHexInverted(i, j))
                        {
                            h[i][j].setLabelSide(3);
                        }
                        else
                        {
                            h[i][j].setLabelSide(0);
                        }
                    }
                    else
                    {
                        // One of the left or right side hexsides.
                        if (deltaX >= 0)
                        {
                            if (deltaY >= 0)
                            {
                                // 2 unless inverted
                                if (isHexInverted(i, j))
                                {
                                    h[i][j].setLabelSide(5);
                                }
                                else
                                {
                                    h[i][j].setLabelSide(2);
                                }
                            }
                            else
                            {
                                // 4 unless inverted
                                if (isHexInverted(i, j))
                                {
                                    h[i][j].setLabelSide(1);
                                }
                                else
                                {
                                    h[i][j].setLabelSide(4);
                                }
                            }
                        }
                        else
                        {
                            if (deltaY >= 0)
                            {
                                // 4 unless inverted
                                if (isHexInverted(i, j))
                                {
                                    h[i][j].setLabelSide(1);
                                }
                                else
                                {
                                    h[i][j].setLabelSide(4);
                                }
                            }
                            else
                            {
                                // 2 unless inverted
                                if (isHexInverted(i, j))
                                {
                                    h[i][j].setLabelSide(5);
                                }
                                else
                                {
                                    h[i][j].setLabelSide(2);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isHexInverted(int i, int j)
    {
        return ((i + j) & 1) == boardParity;
    }

    private void setupNeighbors(MasterHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    MasterHex hex = h[i][j];

                    if (hex.getExitType(0) != Constants.NONE
                        || hex.getEntranceType(0) != Constants.NONE)
                    {
                        hex.setNeighbor(0, h[i][j - 1]);
                    }
                    if (hex.getExitType(1) != Constants.NONE
                        || hex.getEntranceType(1) != Constants.NONE)
                    {
                        hex.setNeighbor(1, h[i + 1][j]);
                    }
                    if (hex.getExitType(2) != Constants.NONE
                        || hex.getEntranceType(2) != Constants.NONE)
                    {
                        hex.setNeighbor(2, h[i + 1][j]);
                    }
                    if (hex.getExitType(3) != Constants.NONE
                        || hex.getEntranceType(3) != Constants.NONE)
                    {
                        hex.setNeighbor(3, h[i][j + 1]);
                    }
                    if (hex.getExitType(4) != Constants.NONE
                        || hex.getEntranceType(4) != Constants.NONE)
                    {
                        hex.setNeighbor(4, h[i - 1][j]);
                    }
                    if (hex.getExitType(5) != Constants.NONE
                        || hex.getEntranceType(5) != Constants.NONE)
                    {
                        hex.setNeighbor(5, h[i - 1][j]);
                    }
                }
            }
        }
    }

    /**
     * towi changes: here is now a cache implemented so that the nested
     *   loop is not executed at every call. the cache is implemented with
     *   a vector. it will work as long as the hex-labels-strings can be
     *   converted to int. this must be the case anyway since the
     *   param 'label' is an int here.
     */
    private MasterHex hexByLabel(int label)
    {
        if (hexByLabelCache.isEmpty())
        {
            initHexByLabelCache();
        }
        // the cache is built and looks like this:
        //   _hexByLabel_cache[0...] =
        //      [ h00,h01,h02, ..., null, null, ..., h30,h31,... ]
        final MasterHex found = hexByLabelCache.get(label);
        if (found == null)
        {
            LOGGER.log(Level.WARNING, "Couldn't find Masterhex labeled "
                + label);
        }
        return found;
    }

    private void initHexByLabelCache()
    {
        for (int i = 0; i < plainHexArray.length; i++)
        {
            for (int j = 0; j < plainHexArray[i].length; j++)
            {
                if (show[i][j])
                {
                    final int iLabel = Integer.parseInt(plainHexArray[i][j]
                        .getLabel());
                    if (hexByLabelCache.size() <= iLabel)
                    {
                        hexByLabelCache.setSize(iLabel + 1);
                    }
                    hexByLabelCache.set(iLabel, plainHexArray[i][j]);
                }
            }
        }
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null if none is found. */
    public MasterHex getHexByLabel(final String label)
    {
        return ArrayHelper.findFirstMatch(this.plainHexArray,
            new NullCheckPredicate<MasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(MasterHex hex)
                {
                    if (hex.getLabel().equals(label))
                    {
                        return true;
                    }
                    return false;
                }
            });
    }

    public Set<String> getTowerSet()
    {
        return Collections.unmodifiableSet(towerSet);
    }

    private void setupTowerSet()
    {
        ArrayHelper.findFirstMatch(this.plainHexArray,
            new NullCheckPredicate<MasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(MasterHex hex)
                {
                    if (HexMap.terrainIsTower(hex.getTerrain()))
                    {
                        towerSet.add(hex.getLabel());
                    }
                    return false;
                }
            });
    }

    /** Return a set of all hex labels. */
    public Set<String> getAllHexLabels()
    {
        final Set<String> set = new HashSet<String>();
        ArrayHelper.findFirstMatch(this.plainHexArray,
            new NullCheckPredicate<MasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(MasterHex hex)
                {
                    set.add(hex.getLabel());
                    return false;
                }
            });
        return set;
    }

}
