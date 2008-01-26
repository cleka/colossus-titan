package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.variant.MasterHex;


/** 
 *  JUnit test for balanced towers. 
 *  @version $Id$
 *  @author David Ripton
 */
public class BalancedTowersTest extends TestCase
{
    private static final MasterHex T100 = VariantSupport.getCurrentVariant()
        .getMasterBoard().getHexByLabel("100");
    private static final MasterHex T200 = VariantSupport.getCurrentVariant()
        .getMasterBoard().getHexByLabel("200");
    private static final MasterHex T300 = VariantSupport.getCurrentVariant()
        .getMasterBoard().getHexByLabel("300");
    private static final MasterHex T400 = VariantSupport.getCurrentVariant()
        .getMasterBoard().getHexByLabel("400");
    private static final MasterHex T500 = VariantSupport.getCurrentVariant()
        .getMasterBoard().getHexByLabel("500");
    private static final MasterHex T600 = VariantSupport.getCurrentVariant()
        .getMasterBoard().getHexByLabel("600");

    public BalancedTowersTest(String name)
    {
        super(name);
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

        assertTrue(results.size() == 4);
        assertTrue((results.contains(T100) && results.contains(T200)
            && results.contains(T400) && results.contains(T500))
            || (results.contains(T100) && results.contains(T300)
                && results.contains(T400) && results.contains(T600))
            || (results.contains(T200) && results.contains(T300)
                && results.contains(T500) && results.contains(T600)));
    }
}
