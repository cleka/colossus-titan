
package net.sf.colossus.masterboard;

import java.util.*;
import java.io.*;

/**
 * Class MasterBoardFixed has description of fixed, non GUI elements
 * of a masterboard, like hex labels and connectivity, but not 
 * dynamic contents like legion or selection
 * @version $Id$
 * @author David Ripton
 */

public final class MasterBoardFixed
{
    /** A static set of non-GUI MasterHexes */
    private static MasterHexFixed[][] plain = new MasterHexFixed[15][8];

    /** For ease of iterating through all hexes, they'll also be
     *  stored in an ArrayList. */
    private static ArrayList plainHexes = new ArrayList(96);

    /** The hexes in the 15x8 array that actually exist are
     *  represented by true. */
    private static final boolean[][] show =
    {
        {false, false, false, true, true, false, false, false},
        {false, false, true, true, true, true, false, false},
        {false, true, true, true, true, true, true, false},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {false, true, true, true, true, true, true, false},
        {false, false, true, true, true, true, false, false},
        {false, false, false, true, true, false, false, false}
    };

    static
    {
        plainHexes.clear();

        // Initialize plain hexes.
        for (int i = 0; i < plain.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
            {
                if (show[i][j])
                {
                    MasterHexFixed hex = new MasterHexFixed();
                    plain[i][j] = hex;
                    plainHexes.add(hex);
                }
            }
        }

        setupHexesGameState(plain);
    }

    public MasterBoardFixed()
    {
    }

    /** This method only needs to be run once, since the attributes it
     *  sets up are constant for the game. */
    private static void setupHexesGameState(MasterHexFixed [][] h)
    {
        // Add terrain types, id labels, label sides, and exits to hexes.
        h[0][3].setTerrain('S');
        h[0][3].setLabel(132);
        h[0][3].setLabelSide(2);
        h[0][3].setExitType(1, MasterHexFixed.ARROWS);

        h[0][4].setTerrain('P');
        h[0][4].setLabel(133);
        h[0][4].setLabelSide(1);
        h[0][4].setExitType(0, MasterHexFixed.ARROWS);

        h[1][2].setTerrain('B');
        h[1][2].setLabel(130);
        h[1][2].setLabelSide(2);
        h[1][2].setExitType(1, MasterHexFixed.ARROWS);

        h[1][3].setTerrain('M');
        h[1][3].setLabel(131);
        h[1][3].setLabelSide(5);
        h[1][3].setExitType(0, MasterHexFixed.ARROWS);
        h[1][3].setExitType(2, MasterHexFixed.ARCH);

        h[1][4].setTerrain('B');
        h[1][4].setLabel(134);
        h[1][4].setLabelSide(4);
        h[1][4].setExitType(1, MasterHexFixed.ARCH);
        h[1][4].setExitType(5, MasterHexFixed.ARROWS);

        h[1][5].setTerrain('J');
        h[1][5].setLabel(135);
        h[1][5].setLabelSide(1);
        h[1][5].setExitType(0, MasterHexFixed.ARROWS);

        h[2][1].setTerrain('D');
        h[2][1].setLabel(128);
        h[2][1].setLabelSide(2);
        h[2][1].setExitType(1, MasterHexFixed.ARROWS);

        h[2][2].setTerrain('P');
        h[2][2].setLabel(129);
        h[2][2].setLabelSide(5);
        h[2][2].setExitType(0, MasterHexFixed.ARROWS);
        h[2][2].setExitType(2, MasterHexFixed.ARCH);

        h[2][3].setTerrain('H');
        h[2][3].setLabel(32);
        h[2][3].setLabelSide(2);
        h[2][3].setExitType(3, MasterHexFixed.ARROWS);
        h[2][3].setExitType(5, MasterHexFixed.BLOCK);

        h[2][4].setTerrain('J');
        h[2][4].setLabel(33);
        h[2][4].setLabelSide(1);
        h[2][4].setExitType(2, MasterHexFixed.ARROWS);
        h[2][4].setExitType(4, MasterHexFixed.BLOCK);

        h[2][5].setTerrain('M');
        h[2][5].setLabel(136);
        h[2][5].setLabelSide(4);
        h[2][5].setExitType(1, MasterHexFixed.ARCH);
        h[2][5].setExitType(5, MasterHexFixed.ARROWS);

        h[2][6].setTerrain('B');
        h[2][6].setLabel(137);
        h[2][6].setLabelSide(1);
        h[2][6].setExitType(0, MasterHexFixed.ARROWS);

        h[3][0].setTerrain('M');
        h[3][0].setLabel(126);
        h[3][0].setLabelSide(2);
        h[3][0].setExitType(1, MasterHexFixed.ARROWS);

        h[3][1].setTerrain('B');
        h[3][1].setLabel(127);
        h[3][1].setLabelSide(5);
        h[3][1].setExitType(0, MasterHexFixed.ARROWS);
        h[3][1].setExitType(2, MasterHexFixed.ARCH);

        h[3][2].setTerrain('T');
        h[3][2].setLabel(500);
        h[3][2].setLabelSide(2);
        h[3][2].setExitType(1, MasterHexFixed.ARROW);
        h[3][2].setExitType(3, MasterHexFixed.ARROW);
        h[3][2].setExitType(5, MasterHexFixed.ARROW);

        h[3][3].setTerrain('B');
        h[3][3].setLabel(31);
        h[3][3].setLabelSide(5);
        h[3][3].setExitType(0, MasterHexFixed.ARCH);
        h[3][3].setExitType(4, MasterHexFixed.ARROWS);

        h[3][4].setTerrain('P');
        h[3][4].setLabel(34);
        h[3][4].setLabelSide(4);
        h[3][4].setExitType(1, MasterHexFixed.ARROWS);
        h[3][4].setExitType(3, MasterHexFixed.ARCH);

        h[3][5].setTerrain('T');
        h[3][5].setLabel(600);
        h[3][5].setLabelSide(1);
        h[3][5].setExitType(0, MasterHexFixed.ARROW);
        h[3][5].setExitType(2, MasterHexFixed.ARROW);
        h[3][5].setExitType(4, MasterHexFixed.ARROW);

        h[3][6].setTerrain('P');
        h[3][6].setLabel(138);
        h[3][6].setLabelSide(4);
        h[3][6].setExitType(1, MasterHexFixed.ARCH);
        h[3][6].setExitType(5, MasterHexFixed.ARROWS);

        h[3][7].setTerrain('D');
        h[3][7].setLabel(139);
        h[3][7].setLabelSide(1);
        h[3][7].setExitType(0, MasterHexFixed.ARROWS);

        h[4][0].setTerrain('J');
        h[4][0].setLabel(125);
        h[4][0].setLabelSide(3);
        h[4][0].setExitType(2, MasterHexFixed.ARROWS);

        h[4][1].setTerrain('J');
        h[4][1].setLabel(26);
        h[4][1].setLabelSide(2);
        h[4][1].setExitType(3, MasterHexFixed.ARROWS);
        h[4][1].setExitType(5, MasterHexFixed.BLOCK);

        h[4][2].setTerrain('M');
        h[4][2].setLabel(27);
        h[4][2].setLabelSide(5);
        h[4][2].setExitType(2, MasterHexFixed.ARROWS);
        h[4][2].setExitType(4, MasterHexFixed.ARCH);

        h[4][3].setTerrain('W');
        h[4][3].setLabel(30);
        h[4][3].setLabelSide(2);
        h[4][3].setExitType(3, MasterHexFixed.ARCH);
        h[4][3].setExitType(5, MasterHexFixed.ARROWS);

        h[4][4].setTerrain('D');
        h[4][4].setLabel(35);
        h[4][4].setLabelSide(1);
        h[4][4].setExitType(0, MasterHexFixed.ARCH);
        h[4][4].setExitType(2, MasterHexFixed.ARROWS);

        h[4][5].setTerrain('B');
        h[4][5].setLabel(38);
        h[4][5].setLabelSide(4);
        h[4][5].setExitType(3, MasterHexFixed.ARROWS);
        h[4][5].setExitType(5, MasterHexFixed.ARCH);

        h[4][6].setTerrain('W');
        h[4][6].setLabel(39);
        h[4][6].setLabelSide(1);
        h[4][6].setExitType(2, MasterHexFixed.ARROWS);
        h[4][6].setExitType(4, MasterHexFixed.BLOCK);

        h[4][7].setTerrain('M');
        h[4][7].setLabel(140);
        h[4][7].setLabelSide(0);
        h[4][7].setExitType(5, MasterHexFixed.ARROWS);

        h[5][0].setTerrain('P');
        h[5][0].setLabel(124);
        h[5][0].setLabelSide(0);
        h[5][0].setExitType(1, MasterHexFixed.ARROWS);
        h[5][0].setExitType(3, MasterHexFixed.ARCH);

        h[5][1].setTerrain('W');
        h[5][1].setLabel(25);
        h[5][1].setLabelSide(3);
        h[5][1].setExitType(0, MasterHexFixed.BLOCK);
        h[5][1].setExitType(4, MasterHexFixed.ARROWS);

        h[5][2].setTerrain('S');
        h[5][2].setLabel(28);
        h[5][2].setLabelSide(2);
        h[5][2].setExitType(1, MasterHexFixed.ARCH);
        h[5][2].setExitType(3, MasterHexFixed.ARROWS);

        h[5][3].setTerrain('P');
        h[5][3].setLabel(29);
        h[5][3].setLabelSide(5);
        h[5][3].setExitType(2, MasterHexFixed.ARCH);
        h[5][3].setExitType(4, MasterHexFixed.ARROWS);

        h[5][4].setTerrain('M');
        h[5][4].setLabel(36);
        h[5][4].setLabelSide(4);
        h[5][4].setExitType(1, MasterHexFixed.ARCH);
        h[5][4].setExitType(3, MasterHexFixed.ARROWS);

        h[5][5].setTerrain('H');
        h[5][5].setLabel(37);
        h[5][5].setLabelSide(1);
        h[5][5].setExitType(2, MasterHexFixed.ARCH);
        h[5][5].setExitType(4, MasterHexFixed.ARROWS);

        h[5][6].setTerrain('J');
        h[5][6].setLabel(40);
        h[5][6].setLabelSide(0);
        h[5][6].setExitType(1, MasterHexFixed.ARROWS);
        h[5][6].setExitType(3, MasterHexFixed.BLOCK);

        h[5][7].setTerrain('B');
        h[5][7].setLabel(141);
        h[5][7].setLabelSide(3);
        h[5][7].setExitType(0, MasterHexFixed.ARCH);
        h[5][7].setExitType(4, MasterHexFixed.ARROWS);

        h[6][0].setTerrain('B');
        h[6][0].setLabel(123);
        h[6][0].setLabelSide(3);
        h[6][0].setExitType(2, MasterHexFixed.ARROWS);

        h[6][1].setTerrain('B');
        h[6][1].setLabel(24);
        h[6][1].setLabelSide(0);
        h[6][1].setExitType(1, MasterHexFixed.ARCH);
        h[6][1].setExitType(5, MasterHexFixed.ARROWS);

        h[6][2].setTerrain('H');
        h[6][2].setLabel(23);
        h[6][2].setLabelSide(3);
        h[6][2].setExitType(0, MasterHexFixed.ARROWS);
        h[6][2].setExitType(4, MasterHexFixed.ARCH);

        h[6][3].setTerrain('m');
        h[6][3].setLabel(5000);
        h[6][3].setLabelSide(2);
        h[6][3].setExitType(1, MasterHexFixed.ARROW);
        h[6][3].setExitType(3, MasterHexFixed.ARROW);
        h[6][3].setExitType(5, MasterHexFixed.BLOCK);

        h[6][4].setTerrain('t');
        h[6][4].setLabel(6000);
        h[6][4].setLabelSide(1);
        h[6][4].setExitType(0, MasterHexFixed.ARROW);
        h[6][4].setExitType(2, MasterHexFixed.ARROW);
        h[6][4].setExitType(4, MasterHexFixed.BLOCK);

        h[6][5].setTerrain('S');
        h[6][5].setLabel(42);
        h[6][5].setLabelSide(0);
        h[6][5].setExitType(1, MasterHexFixed.ARROWS);
        h[6][5].setExitType(5, MasterHexFixed.ARCH);

        h[6][6].setTerrain('M');
        h[6][6].setLabel(41);
        h[6][6].setLabelSide(3);
        h[6][6].setExitType(0, MasterHexFixed.ARROWS);
        h[6][6].setExitType(2, MasterHexFixed.ARCH);

        h[6][7].setTerrain('S');
        h[6][7].setLabel(142);
        h[6][7].setLabelSide(0);
        h[6][7].setExitType(5, MasterHexFixed.ARROWS);

        h[7][0].setTerrain('M');
        h[7][0].setLabel(122);
        h[7][0].setLabelSide(0);
        h[7][0].setExitType(1, MasterHexFixed.ARROWS);
        h[7][0].setExitType(3, MasterHexFixed.ARCH);

        h[7][1].setTerrain('T');
        h[7][1].setLabel(400);
        h[7][1].setLabelSide(3);
        h[7][1].setExitType(0, MasterHexFixed.ARROW);
        h[7][1].setExitType(2, MasterHexFixed.ARROW);
        h[7][1].setExitType(4, MasterHexFixed.ARROW);

        h[7][2].setTerrain('M');
        h[7][2].setLabel(22);
        h[7][2].setLabelSide(0);
        h[7][2].setExitType(3, MasterHexFixed.ARCH);
        h[7][2].setExitType(5, MasterHexFixed.ARROWS);

        h[7][3].setTerrain('t');
        h[7][3].setLabel(4000);
        h[7][3].setLabelSide(3);
        h[7][3].setExitType(0, MasterHexFixed.BLOCK);
        h[7][3].setExitType(2, MasterHexFixed.ARROW);
        h[7][3].setExitType(4, MasterHexFixed.ARROW);

        h[7][4].setTerrain('m');
        h[7][4].setLabel(1000);
        h[7][4].setLabelSide(0);
        h[7][4].setExitType(1, MasterHexFixed.ARROW);
        h[7][4].setExitType(3, MasterHexFixed.BLOCK);
        h[7][4].setExitType(5, MasterHexFixed.ARROW);

        h[7][5].setTerrain('P');
        h[7][5].setLabel(1);
        h[7][5].setLabelSide(3);
        h[7][5].setExitType(0, MasterHexFixed.ARCH);
        h[7][5].setExitType(2, MasterHexFixed.ARROWS);

        h[7][6].setTerrain('T');
        h[7][6].setLabel(100);
        h[7][6].setLabelSide(0);
        h[7][6].setExitType(1, MasterHexFixed.ARROW);
        h[7][6].setExitType(3, MasterHexFixed.ARROW);
        h[7][6].setExitType(5, MasterHexFixed.ARROW);

        h[7][7].setTerrain('P');
        h[7][7].setLabel(101);
        h[7][7].setLabelSide(3);
        h[7][7].setExitType(0, MasterHexFixed.ARCH);
        h[7][7].setExitType(4, MasterHexFixed.ARROWS);

        h[8][0].setTerrain('S');
        h[8][0].setLabel(121);
        h[8][0].setLabelSide(3);
        h[8][0].setExitType(2, MasterHexFixed.ARROWS);

        h[8][1].setTerrain('P');
        h[8][1].setLabel(20);
        h[8][1].setLabelSide(0);
        h[8][1].setExitType(3, MasterHexFixed.ARROWS);
        h[8][1].setExitType(5, MasterHexFixed.ARCH);

        h[8][2].setTerrain('D');
        h[8][2].setLabel(21);
        h[8][2].setLabelSide(3);
        h[8][2].setExitType(2, MasterHexFixed.ARCH);
        h[8][2].setExitType(4, MasterHexFixed.ARROWS);

        h[8][3].setTerrain('m');
        h[8][3].setLabel(3000);
        h[8][3].setLabelSide(4);
        h[8][3].setExitType(1, MasterHexFixed.BLOCK);
        h[8][3].setExitType(3, MasterHexFixed.ARROW);
        h[8][3].setExitType(5, MasterHexFixed.ARROW);

        h[8][4].setTerrain('t');
        h[8][4].setLabel(2000);
        h[8][4].setLabelSide(5);
        h[8][4].setExitType(0, MasterHexFixed.ARROW);
        h[8][4].setExitType(2, MasterHexFixed.BLOCK);
        h[8][4].setExitType(4, MasterHexFixed.ARROW);

        h[8][5].setTerrain('W');
        h[8][5].setLabel(2);
        h[8][5].setLabelSide(0);
        h[8][5].setExitType(1, MasterHexFixed.ARCH);
        h[8][5].setExitType(3, MasterHexFixed.ARROWS);

        h[8][6].setTerrain('B');
        h[8][6].setLabel(3);
        h[8][6].setLabelSide(3);
        h[8][6].setExitType(2, MasterHexFixed.ARROWS);
        h[8][6].setExitType(4, MasterHexFixed.ARCH);

        h[8][7].setTerrain('B');
        h[8][7].setLabel(102);
        h[8][7].setLabelSide(0);
        h[8][7].setExitType(5, MasterHexFixed.ARROWS);

        h[9][0].setTerrain('B');
        h[9][0].setLabel(120);
        h[9][0].setLabelSide(0);
        h[9][0].setExitType(1, MasterHexFixed.ARROWS);
        h[9][0].setExitType(3, MasterHexFixed.ARCH);

        h[9][1].setTerrain('J');
        h[9][1].setLabel(19);
        h[9][1].setLabelSide(3);
        h[9][1].setExitType(0, MasterHexFixed.BLOCK);
        h[9][1].setExitType(4, MasterHexFixed.ARROWS);

        h[9][2].setTerrain('W');
        h[9][2].setLabel(16);
        h[9][2].setLabelSide(4);
        h[9][2].setExitType(1, MasterHexFixed.ARROWS);
        h[9][2].setExitType(5, MasterHexFixed.ARCH);

        h[9][3].setTerrain('P');
        h[9][3].setLabel(15);
        h[9][3].setLabelSide(1);
        h[9][3].setExitType(0, MasterHexFixed.ARROWS);
        h[9][3].setExitType(4, MasterHexFixed.ARCH);

        h[9][4].setTerrain('M');
        h[9][4].setLabel(8);
        h[9][4].setLabelSide(2);
        h[9][4].setExitType(1, MasterHexFixed.ARROWS);
        h[9][4].setExitType(5, MasterHexFixed.ARCH);

        h[9][5].setTerrain('D');
        h[9][5].setLabel(7);
        h[9][5].setLabelSide(5);
        h[9][5].setExitType(0, MasterHexFixed.ARROWS);
        h[9][5].setExitType(4, MasterHexFixed.ARCH);

        h[9][6].setTerrain('H');
        h[9][6].setLabel(4);
        h[9][6].setLabelSide(0);
        h[9][6].setExitType(1, MasterHexFixed.ARROWS);
        h[9][6].setExitType(3, MasterHexFixed.BLOCK);

        h[9][7].setTerrain('M');
        h[9][7].setLabel(103);
        h[9][7].setLabelSide(3);
        h[9][7].setExitType(0, MasterHexFixed.ARCH);
        h[9][7].setExitType(4, MasterHexFixed.ARROWS);

        h[10][0].setTerrain('P');
        h[10][0].setLabel(119);
        h[10][0].setLabelSide(3);
        h[10][0].setExitType(2, MasterHexFixed.ARROWS);

        h[10][1].setTerrain('H');
        h[10][1].setLabel(18);
        h[10][1].setLabelSide(4);
        h[10][1].setExitType(1, MasterHexFixed.BLOCK);
        h[10][1].setExitType(5, MasterHexFixed.ARROWS);

        h[10][2].setTerrain('B');
        h[10][2].setLabel(17);
        h[10][2].setLabelSide(1);
        h[10][2].setExitType(0, MasterHexFixed.ARROWS);
        h[10][2].setExitType(2, MasterHexFixed.ARCH);

        h[10][3].setTerrain('S');
        h[10][3].setLabel(14);
        h[10][3].setLabelSide(4);
        h[10][3].setExitType(3, MasterHexFixed.ARCH);
        h[10][3].setExitType(5, MasterHexFixed.ARROWS);

        h[10][4].setTerrain('H');
        h[10][4].setLabel(9);
        h[10][4].setLabelSide(5);
        h[10][4].setExitType(0, MasterHexFixed.ARCH);
        h[10][4].setExitType(2, MasterHexFixed.ARROWS);

        h[10][5].setTerrain('P');
        h[10][5].setLabel(6);
        h[10][5].setLabelSide(2);
        h[10][5].setExitType(1, MasterHexFixed.ARCH);
        h[10][5].setExitType(5, MasterHexFixed.ARROWS);

        h[10][6].setTerrain('J');
        h[10][6].setLabel(5);
        h[10][6].setLabelSide(5);
        h[10][6].setExitType(0, MasterHexFixed.ARROWS);
        h[10][6].setExitType(2, MasterHexFixed.BLOCK);

        h[10][7].setTerrain('J');
        h[10][7].setLabel(104);
        h[10][7].setLabelSide(0);
        h[10][7].setExitType(5, MasterHexFixed.ARROWS);

        h[11][0].setTerrain('D');
        h[11][0].setLabel(118);
        h[11][0].setLabelSide(4);
        h[11][0].setExitType(3, MasterHexFixed.ARROWS);

        h[11][1].setTerrain('M');
        h[11][1].setLabel(117);
        h[11][1].setLabelSide(1);
        h[11][1].setExitType(2, MasterHexFixed.ARROWS);
        h[11][1].setExitType(4, MasterHexFixed.ARCH);

        h[11][2].setTerrain('T');
        h[11][2].setLabel(300);
        h[11][2].setLabelSide(4);
        h[11][2].setExitType(1, MasterHexFixed.ARROW);
        h[11][2].setExitType(3, MasterHexFixed.ARROW);
        h[11][2].setExitType(5, MasterHexFixed.ARROW);

        h[11][3].setTerrain('M');
        h[11][3].setLabel(13);
        h[11][3].setLabelSide(1);
        h[11][3].setExitType(0, MasterHexFixed.ARCH);
        h[11][3].setExitType(4, MasterHexFixed.ARROWS);

        h[11][4].setTerrain('B');
        h[11][4].setLabel(10);
        h[11][4].setLabelSide(2);
        h[11][4].setExitType(1, MasterHexFixed.ARROWS);
        h[11][4].setExitType(3, MasterHexFixed.ARCH);

        h[11][5].setTerrain('T');
        h[11][5].setLabel(200);
        h[11][5].setLabelSide(5);
        h[11][5].setExitType(0, MasterHexFixed.ARROW);
        h[11][5].setExitType(2, MasterHexFixed.ARROW);
        h[11][5].setExitType(4, MasterHexFixed.ARROW);

        h[11][6].setTerrain('B');
        h[11][6].setLabel(106);
        h[11][6].setLabelSide(2);
        h[11][6].setExitType(3, MasterHexFixed.ARROWS);
        h[11][6].setExitType(5, MasterHexFixed.ARCH);

        h[11][7].setTerrain('P');
        h[11][7].setLabel(105);
        h[11][7].setLabelSide(5);
        h[11][7].setExitType(4, MasterHexFixed.ARROWS);

        h[12][1].setTerrain('B');
        h[12][1].setLabel(116);
        h[12][1].setLabelSide(4);
        h[12][1].setExitType(3, MasterHexFixed.ARROWS);

        h[12][2].setTerrain('P');
        h[12][2].setLabel(115);
        h[12][2].setLabelSide(1);
        h[12][2].setExitType(2, MasterHexFixed.ARROWS);
        h[12][2].setExitType(4, MasterHexFixed.ARCH);

        h[12][3].setTerrain('J');
        h[12][3].setLabel(12);
        h[12][3].setLabelSide(4);
        h[12][3].setExitType(1, MasterHexFixed.BLOCK);
        h[12][3].setExitType(5, MasterHexFixed.ARROWS);

        h[12][4].setTerrain('W');
        h[12][4].setLabel(11);
        h[12][4].setLabelSide(5);
        h[12][4].setExitType(0, MasterHexFixed.ARROWS);
        h[12][4].setExitType(2, MasterHexFixed.BLOCK);

        h[12][5].setTerrain('M');
        h[12][5].setLabel(108);
        h[12][5].setLabelSide(2);
        h[12][5].setExitType(3, MasterHexFixed.ARROWS);
        h[12][5].setExitType(5, MasterHexFixed.ARCH);

        h[12][6].setTerrain('D');
        h[12][6].setLabel(107);
        h[12][6].setLabelSide(5);
        h[12][6].setExitType(4, MasterHexFixed.ARROWS);

        h[13][2].setTerrain('J');
        h[13][2].setLabel(114);
        h[13][2].setLabelSide(4);
        h[13][2].setExitType(3, MasterHexFixed.ARROWS);

        h[13][3].setTerrain('B');
        h[13][3].setLabel(113);
        h[13][3].setLabelSide(1);
        h[13][3].setExitType(2, MasterHexFixed.ARROWS);
        h[13][3].setExitType(4, MasterHexFixed.ARCH);

        h[13][4].setTerrain('P');
        h[13][4].setLabel(110);
        h[13][4].setLabelSide(2);
        h[13][4].setExitType(3, MasterHexFixed.ARROWS);
        h[13][4].setExitType(5, MasterHexFixed.ARCH);

        h[13][5].setTerrain('B');
        h[13][5].setLabel(109);
        h[13][5].setLabelSide(5);
        h[13][5].setExitType(4, MasterHexFixed.ARROWS);

        h[14][3].setTerrain('M');
        h[14][3].setLabel(112);
        h[14][3].setLabelSide(4);
        h[14][3].setExitType(3, MasterHexFixed.ARROWS);

        h[14][4].setTerrain('S');
        h[14][4].setLabel(111);
        h[14][4].setLabelSide(5);
        h[14][4].setExitType(4, MasterHexFixed.ARROWS);

        // Derive entrances from exits.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 6; k++)
                    {
                        int gateType = h[i][j].getExitType(k);
                        if (gateType != MasterHexFixed.NONE)
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
                            }
                        }
                    }
                }
            }
        }

        // Add references to neighbor hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
            {
                if (show[i][j])
                {
                    MasterHexFixed hex = h[i][j];

                    if (hex.getExitType(0) != MasterHexFixed.NONE ||
                        hex.getEntranceType(0) != MasterHexFixed.NONE)
                    {
                        hex.setNeighbor(0, h[i][j - 1]);
                    }
                    if (hex.getExitType(1) != MasterHexFixed.NONE ||
                        hex.getEntranceType(1) != MasterHexFixed.NONE)
                    {
                        hex.setNeighbor(1, h[i + 1][j]);
                    }
                    if (hex.getExitType(2) != MasterHexFixed.NONE ||
                        hex.getEntranceType(2) != MasterHexFixed.NONE)
                    {
                        hex.setNeighbor(2, h[i + 1][j]);
                    }
                    if (hex.getExitType(3) != MasterHexFixed.NONE ||
                        hex.getEntranceType(3) != MasterHexFixed.NONE)
                    {
                        hex.setNeighbor(3, h[i][j + 1]);
                    }
                    if (hex.getExitType(4) != MasterHexFixed.NONE ||
                        hex.getEntranceType(4) != MasterHexFixed.NONE)
                    {
                        hex.setNeighbor(4, h[i - 1][j]);
                    }
                    if (hex.getExitType(5) != MasterHexFixed.NONE ||
                        hex.getEntranceType(5) != MasterHexFixed.NONE)
                    {
                        hex.setNeighbor(5, h[i - 1][j]);
                    }
                }
            }
        }
    }


    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null if none is found. */
    public static MasterHexFixed getHexByLabel(String label)
    {
        Iterator it = plainHexes.iterator();
        while (it.hasNext())
        {
            MasterHexFixed hex = (MasterHexFixed)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }
        //Log.error("Could not find hex " + label);
        return null;
    }

    /** Do a brute-force search through the hex array, looking for
     *  a hex with the proper terrain type.  Return the hex, or null
     *  if none is found. */
    public static MasterHexFixed getAnyHexWithTerrain(char terrain)
    {
        Iterator it = plainHexes.iterator();
        while (it.hasNext())
        {
            MasterHexFixed hex = (MasterHexFixed)it.next();
            if (hex.getTerrain() == terrain)
            {
                return hex;
            }
        }

        //Log.error("Could not find hex with terrain " + terrain);
        return null;
    }



}
