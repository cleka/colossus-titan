package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;


/**
 * JUnit test for combinations.
 *
 * @author David Ripton
 */
public class CombosTest extends TestCase
{
    public CombosTest(String name)
    {
        super(name);
    }

    public void testCombos()
    {
        List<String> startlist = new ArrayList<String>();
        startlist.add("a");
        startlist.add("b");
        startlist.add("c");
        startlist.add("d");
        int n = 3;

        List<List<String>> results = new ArrayList<List<String>>();

        Combos<String> combos = new Combos<String>(startlist, n);
        Iterator<List<String>> it = combos.iterator();
        while (it.hasNext())
        {
            List<String> nextCombo = it.next();
            results.add(nextCombo);
        }

        assertEquals(results.size(), 4);
    }
}
