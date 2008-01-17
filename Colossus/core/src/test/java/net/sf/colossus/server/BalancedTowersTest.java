package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;


/** 
 *  JUnit test for balanced towers. 
 *  @version $Id$
 *  @author David Ripton
 */
public class BalancedTowersTest extends TestCase
{
    public BalancedTowersTest(String name)
    {
        super(name);
    }

    public void testGetBalancedTowers()
    {
        int numPlayers = 4;
        int numTowers = 6;

        List<String> towerList = new ArrayList<String>();
        for (int i = 0; i < numTowers; i++)
        {
            towerList.add("" + 100 * (i + 1));
        }

        List<String> results = GameServerSide.getBalancedTowers(numPlayers, towerList);

        assertTrue(results.size() == 4);
        assertTrue((results.contains("100") && results.contains("200")
            && results.contains("400") && results.contains("500"))
            || (results.contains("100") && results.contains("300")
                && results.contains("400") && results.contains("600"))
            || (results.contains("200") && results.contains("300")
                && results.contains("500") && results.contains("600")));
    }
}
