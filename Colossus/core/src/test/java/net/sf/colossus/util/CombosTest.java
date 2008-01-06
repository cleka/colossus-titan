package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;


/** 
 *  JUnit test for combinations. 
 *  @version $Id$
 *  @author David Ripton
 */
public class CombosTest extends TestCase
{
    public CombosTest(String name)
    {
        super(name);
    }

    public void testCombos()
    {
        ArrayList startlist = new ArrayList();
        startlist.add("a");
        startlist.add("b");
        startlist.add("c");
        startlist.add("d");
        int n = 3;

        ArrayList<ArrayList> results = new ArrayList<ArrayList>();

        Combos combos = new Combos(startlist, n);
        Iterator<List> it = combos.iterator();
        while (it.hasNext())
        {
            ArrayList nextCombo = (ArrayList)it.next();
            results.add(nextCombo);
        }

        assertEquals(results.size(), 4);
    }
}
