package net.sf.colossus.util;


import java.util.*;
import junit.framework.*;


/** 
 *  JUnit test for MultiSet. 
 *  @version $Id$
 *  @author David Ripton
 */
public class MultiSetTest extends TestCase
{
    public MultiSetTest(String name)
    {
        super(name);
    }

    public void testMultiSet()
    {
        MultiSet ms = new MultiSet();
        assertEquals(ms.size(), 0);
        assertTrue(ms.isEmpty());
        assertEquals(ms.remove("a"), false);
        assertEquals(ms.count("a"), 0);
        assertFalse(ms.contains("a"));
        ms.add("a");
        assertEquals(ms.count("a"), 1);
        assertEquals(ms.max(), 1);
        assertFalse(ms.isEmpty());
        assertTrue(ms.contains("a"));
        ms.add("a");
        assertEquals(ms.count("a"), 2);
        assertEquals(ms.max(), 2);
        assertTrue(ms.contains("a"));
        assertEquals(ms.remove("a"), true);
        assertEquals(ms.count("a"), 1);
        assertEquals(ms.max(), 1);
        assertTrue(ms.contains("a"));
        assertEquals(ms.remove("a"), true);
        assertEquals(ms.count("a"), 0);
        assertEquals(ms.max(), 0);
        assertEquals(ms.remove("a"), false);
        assertEquals(ms.count("a"), 0);
        assertFalse(ms.contains("a"));
        assertTrue(ms.isEmpty());
    }
}
