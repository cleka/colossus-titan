package net.sf.colossus.util;


import junit.framework.TestCase;


/**
 * JUnit test for MultiSet.
 *
 * @author David Ripton
 */
public class MultiSetTest extends TestCase
{
    public MultiSetTest(String name)
    {
        super(name);
    }

    public void testMultiSet()
    {
        MultiSet<String> ms = new MultiSet<String>();
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
