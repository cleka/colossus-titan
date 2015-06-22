package net.sf.colossus.variant;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Constants.HexsideGates;
import net.sf.colossus.util.ArrayHelper;
import net.sf.colossus.util.NullCheckPredicate;


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
     * A Set of all Tower hexes.
     */
    private final Set<MasterHex> towerSet;

    /**
     * A cache for faster lookup of hexes using their labels.
     */
    private final Map<String, MasterHex> hexByLabelCache = new HashMap<String, MasterHex>();

    public MasterBoard(int horizSize, int vertSize, boolean show[][],
        MasterHex[][] plainHexArray)
    {
        this.horizSize = horizSize;
        this.vertSize = vertSize;
        this.show = show;
        this.plainHexArray = plainHexArray;

        initHexByLabelCache();
        this.boardParity = computeBoardParity();
        this.towerSet = new HashSet<MasterHex>();
        setupTowerSet();

        setupExits(plainHexArray);
        setupEntrances(plainHexArray);
        setupHexLabelSides(plainHexArray);
        setupNeighbors(plainHexArray);
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
                        if (h[i][j].getBaseExitType(k) != Constants.HexsideGates.NONE)
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
        MasterHex dh = getHexByLabel(h[i][j].getBaseExitLabel(k));
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
            h[i][j].setExitType(2 - ((i + j + boardParity) & 1),
                h[i][j].getBaseExitType(k));
        }
        else
        {
            assert dh.getXCoord() == (i - 1) && dh.getYCoord() == j : "bad exit ; i="
                + i + ", j=" + j + ", k=" + k;
            h[i][j].setExitType(4 + ((i + j + boardParity) & 1),
                h[i][j].getBaseExitType(k));
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
                        HexsideGates gateType = h[i][j].getExitType(k);
                        if (gateType != Constants.HexsideGates.NONE)
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

                    if (hex.getExitType(0) != Constants.HexsideGates.NONE
                        || hex.getEntranceType(0) != Constants.HexsideGates.NONE)
                    {
                        hex.setNeighbor(0, h[i][j - 1]);
                    }
                    if (hex.getExitType(1) != Constants.HexsideGates.NONE
                        || hex.getEntranceType(1) != Constants.HexsideGates.NONE)
                    {
                        hex.setNeighbor(1, h[i + 1][j]);
                    }
                    if (hex.getExitType(2) != Constants.HexsideGates.NONE
                        || hex.getEntranceType(2) != Constants.HexsideGates.NONE)
                    {
                        hex.setNeighbor(2, h[i + 1][j]);
                    }
                    if (hex.getExitType(3) != Constants.HexsideGates.NONE
                        || hex.getEntranceType(3) != Constants.HexsideGates.NONE)
                    {
                        hex.setNeighbor(3, h[i][j + 1]);
                    }
                    if (hex.getExitType(4) != Constants.HexsideGates.NONE
                        || hex.getEntranceType(4) != Constants.HexsideGates.NONE)
                    {
                        hex.setNeighbor(4, h[i - 1][j]);
                    }
                    if (hex.getExitType(5) != Constants.HexsideGates.NONE
                        || hex.getEntranceType(5) != Constants.HexsideGates.NONE)
                    {
                        hex.setNeighbor(5, h[i - 1][j]);
                    }
                }
            }
        }
    }

    private void initHexByLabelCache()
    {
        for (MasterHex[] row : plainHexArray)
        {
            for (MasterHex masterHex : row)
            {
                if (masterHex != null)
                {
                    hexByLabelCache.put(masterHex.getLabel(), masterHex);
                }
            }
        }
    }

    /**
     * Retrieve a hex by its label.
     *
     * @param label The label to find the hex for. Valid label, not null.
     * @return The label found.
     */
    public MasterHex getHexByLabel(final String label)
    {
        MasterHex hex = hexByLabelCache.get(label);
        // TODO such an assertion would be nice, but seems to fail when loading
        // a game:
        // assert hex != null : "No hex with label '" + label + "'";
        return hex;
    }

    public Set<MasterHex> getTowerSet()
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
                    if (hex.getTerrain().isTower())
                    {
                        towerSet.add(hex);
                    }
                    return false;
                }
            });
    }

    /**
     * Return a set of all hex labels.
     */
    public Set<String> getAllHexLabels()
    {
        return hexByLabelCache.keySet();
    }

    /**
     * Return a set of all hex labels.
     */
    public Collection<MasterHex> getAllHexes()
    {
        return hexByLabelCache.values();
    }
}
