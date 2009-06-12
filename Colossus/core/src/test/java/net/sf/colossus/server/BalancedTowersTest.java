package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.variant.MasterHex;


/**
 * JUnit test for balanced towers.
 *
 * @author David Ripton
 */
public class BalancedTowersTest extends TestCase
{
    public BalancedTowersTest(String name)
    {
        super(name);
        VariantSupport.loadVariantByName("Default", true);
    }

    public void testGetBalancedTowers()
    {
        int numPlayers = 4;
        int numTowers = 6;

        List<MasterHex> towerList = new ArrayList<MasterHex>();
        for (int i = 0; i < numTowers; i++)
        {
            towerList.add(VariantSupport.getCurrentVariant().getMasterBoard()
                .getHexByLabel(String.valueOf(100 * (i + 1))));
        }

        List<MasterHex> results = GameServerSide.getBalancedTowers(numPlayers,
            towerList);

        MasterHex T100 = towerList.get(0);
        MasterHex T200 = towerList.get(1);
        MasterHex T300 = towerList.get(2);
        MasterHex T400 = towerList.get(3);
        MasterHex T500 = towerList.get(4);
        MasterHex T600 = towerList.get(5);

        assertTrue(results.size() == 4);
        assertTrue((results.contains(T100) && results.contains(T200)
            && results.contains(T400) && results.contains(T500))
            || (results.contains(T100) && results.contains(T300)
                && results.contains(T400) && results.contains(T600))
            || (results.contains(T200) && results.contains(T300)
                && results.contains(T500) && results.contains(T600)));
    }
}
