/**
 * Class SetupBattleHexes initializes hex contents for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public class SetupBattleHexes
{
    public static final boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };


    public static void setupHexes(BattleHex [][] h, char terrain,
        BattleMap map)
    {
        int scale = 30;

        int cx = 6 * scale;
        int cy = 3 * scale;
        
        // Initialize hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new BattleHex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + (i & 1)) *
                        BattleHex.SQRT3 * scale), scale, map, i, j);
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


        // XXX Since the hex array is static, this should only be
        // done once.
        // Add references to neighbor hexes.
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
}
