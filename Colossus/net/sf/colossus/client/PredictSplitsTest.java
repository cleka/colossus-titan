package net.sf.colossus.client;

import java.util.*;
import junit.framework.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Game;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.Log;


/**
 *  JUnit test for split prediction.
 *  @version $Id$
 *  @author David Ripton
 */
public class PredictSplitsTest extends TestCase
{
    List cnl;
    AllPredictSplits aps;
    PredictSplits ps;
    int turn = 0;


    public PredictSplitsTest(String name)
    {
        super(name);
    }

    protected void setUp()
    {
        VariantSupport.loadVariant("Default", true);
        aps = new AllPredictSplits();

        cnl = new ArrayList();
        cnl.add("Titan");
        cnl.add("Angel");
        cnl.add("Centaur");
        cnl.add("Centaur");
        cnl.add("Gargoyle");
        cnl.add("Gargoyle");
        cnl.add("Ogre");
        cnl.add("Ogre");
    }


    public void testPredictSplits1()
    {
        Log.debug("testPredictSplits1()");
        ps = new PredictSplits("Red", "Rd01", cnl);

        turn = 1;
        Log.debug("Turn " + turn);
        Node root = ps.getLeaf("Rd01");
        assertTrue(root != null);
        assertTrue(ps.getLeaves(root) != null);
        ps.getLeaf("Rd01").split(4, "Rd02", turn);
        ps.getLeaf("Rd01").merge(ps.getLeaf("Rd02"), turn);
        ps.getLeaf("Rd01").split(4, "Rd02", turn);
        cnl.clear(); cnl.add("Ogre"); cnl.add("Ogre");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Troll");
        cnl.clear(); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd02").revealCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Lion");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        ps.printLeaves();

        turn = 2;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Gargoyle");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Gargoyle");
        cnl.clear(); cnl.add("Lion");
        ps.getLeaf("Rd02").revealCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Lion");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        ps.printLeaves();

        turn = 3;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Titan");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Warlock");
        ps.getLeaf("Rd02").addCreature("Gargoyle");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd02").allCertain());
        ps.printLeaves();

        turn = 4;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd01").split(2, "Rd03", turn);
        ps.getLeaf("Rd02").split(2, "Rd04", turn);
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Cyclops");
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Rd02").revealCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Cyclops");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertFalse(ps.getLeaf("Rd03").allCertain());
        assertFalse(ps.getLeaf("Rd04").allCertain());
        ps.printLeaves();

        turn = 5;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Warlock");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Warlock");
        ps.getLeaf("Rd02").addCreature("Ogre");
        cnl.clear(); cnl.add("Ogre"); cnl.add("Ogre");
        ps.getLeaf("Rd03").revealCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Troll");
        cnl.clear(); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Lion");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd02").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        ps.printLeaves();

        turn = 6;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd02").split(2, "Rd05", turn);
        cnl.clear(); 
        cnl.add("Titan"); cnl.add("Warlock"); cnl.add("Warlock");
        cnl.add("Cyclops"); cnl.add("Troll"); cnl.add("Gargoyle"); 
        cnl.add("Gargoyle");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").removeCreature("Gargoyle");
        ps.getLeaf("Rd01").removeCreature("Gargoyle");
        ps.getLeaf("Rd02").removeCreature("Angel");
        ps.getLeaf("Rd01").addCreature("Angel");
        cnl.clear(); cnl.add("Lion"); cnl.add("Lion");
        ps.getLeaf("Rd02").revealCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Minotaur");
        cnl.clear(); cnl.add("Lion");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Lion");
        cnl.clear();
        cnl.add("Cyclops"); cnl.add("Minotaur"); cnl.add("Lion");
        cnl.add("Lion"); cnl.add("Ogre");
        ps.getLeaf("Rd02").revealCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Minotaur");
        cnl.clear(); 
        cnl.add("Cyclops"); cnl.add("Minotaur"); cnl.add("Minotaur");
        cnl.add("Lion"); cnl.add("Lion"); cnl.add("Ogre");
        ps.getLeaf("Rd02").removeCreatures(cnl);
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        ps.printLeaves();

        turn = 7;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd01").addCreature("Angel");
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd03").revealCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Troll");
        cnl.clear(); cnl.add("Lion");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Lion");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        ps.printLeaves();

        turn = 8;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd01").split(2, "Rd02", turn);
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Cyclops");
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Cyclops");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        ps.printLeaves();

        turn = 9;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Troll");
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd03").revealCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Troll");
        cnl.clear(); cnl.add("Lion"); cnl.add("Lion"); cnl.add("Lion");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Griffon");
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Cyclops");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        ps.printLeaves();

        turn = 10;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd01").split(2, "Rd06", turn);
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertFalse(ps.getLeaf("Rd06").allCertain());
        ps.printLeaves();

        turn = 11;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Griffon"); cnl.add("Lion"); cnl.add("Lion"); 
        cnl.add("Lion"); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Cyclops");
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd03").revealCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Ranger");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertFalse(ps.getLeaf("Rd06").allCertain());
        ps.printLeaves();

        turn = 12;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd02").addCreature("Centaur");
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd03").revealCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Warbear");
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Cyclops");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertFalse(ps.getLeaf("Rd06").allCertain());
        ps.printLeaves();

        turn = 13;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Titan"); cnl.add("Warlock"); cnl.add("Warlock");
        cnl.add("Cyclops"); cnl.add("Cyclops"); cnl.add("Cyclops");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        cnl.clear(); cnl.add("Cyclops"); cnl.add("Cyclops"); cnl.add("Cyclops");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Behemoth");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd02").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        ps.printLeaves();

        turn = 14;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Griffon"); cnl.add("Lion"); cnl.add("Lion");
        cnl.add("Lion"); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        ps.getLeaf("Rd02").removeCreature("Angel");
        ps.getLeaf("Rd04").addCreature("Angel");
        cnl.clear(); cnl.add("Angel"); cnl.add("Lion"); cnl.add("Lion");
        cnl.add("Lion"); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd04").removeCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Angel");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd02").allCertain());
        assertTrue(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        ps.printLeaves();

        assertEquals(ps.getLeaf("Rd01").getHeight(), 6);
        assertEquals(ps.getLeaf("Rd02").getHeight(), 2);
        assertEquals(ps.getLeaf("Rd03").getHeight(), 7);
        assertEquals(ps.getLeaf("Rd04").getHeight(), 2);
        assertEquals(ps.getLeaf("Rd05").getHeight(), 6);
        assertEquals(ps.getLeaf("Rd06").getHeight(), 2);
    }

    public void testPredictSplits2()
    {
        Log.debug("testPredictSplits2()");
        ps = new PredictSplits("Red", "Rd11", cnl);

        turn = 1;
        Log.debug("Turn " + turn);
        Node root = ps.getLeaf("Rd11");
        assertTrue(root != null);
        assertTrue(ps.getLeaves(root) != null);
        ps.getLeaf("Rd11").split(4, "Rd10", turn);
        cnl.clear(); cnl.add("Ogre"); cnl.add("Ogre");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Troll");
        cnl.clear(); cnl.add("Gargoyle");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Gargoyle");
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 2;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Troll");
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 3;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Ranger");
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Rd11").addCreature("Cyclops");
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 4;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Titan"); cnl.add("Ranger"); cnl.add("Troll");
            cnl.add("Troll"); cnl.add("Gargoyle"); cnl.add("Ogre"); 
            cnl.add("Ogre");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        cnl.clear(); cnl.add("Gargoyle");
        ps.getLeaf("Rd10").removeCreatures(cnl);
        cnl.clear(); cnl.add("Angel");
        ps.getLeaf("Rd11").removeCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Angel");
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertTrue(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 5;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd10").split(2, "Rd01", turn);
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Troll");
        cnl.clear(); cnl.add("Ogre"); cnl.add("Ogre");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Troll");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertTrue(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 6;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Troll"); cnl.add("Ogre"); cnl.add("Ogre");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Troll");
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Wyvern");
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Cyclops");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertTrue(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 7;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd10").split(2, "Rd06", turn);
        cnl.clear(); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Lion");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd06").allCertain());
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertTrue(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 8;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd11").split(2, "Rd07", turn);
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll"); cnl.add("Ogre");
            cnl.add("Ogre");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        cnl.clear(); cnl.add("Angel");
        ps.getLeaf("Rd10").removeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Angel");
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll"); cnl.add("Ogre");
            cnl.add("Ogre");
        ps.getLeaf("Rd01").removeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Angel");
        cnl.clear(); cnl.add("Wyvern");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Wyvern");
        cnl.clear(); cnl.add("Lion");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Lion");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd06").allCertain());
        assertFalse(ps.getLeaf("Rd07").allCertain());
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 9;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd07").addCreature("Centaur");
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Cyclops");
        assertTrue(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd06").allCertain());
        assertFalse(ps.getLeaf("Rd07").allCertain());
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 10;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd11").split(2, "Rd08", turn);
        cnl.clear(); cnl.add("Angel"); cnl.add("Angel");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd06").revealCreatures(cnl);
        ps.getLeaf("Rd06").addCreature("Warbear");
        cnl.clear(); cnl.add("Centaur");
        ps.getLeaf("Rd07").revealCreatures(cnl);
        ps.getLeaf("Rd07").addCreature("Centaur");
        cnl.clear(); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd08").revealCreatures(cnl);
        ps.getLeaf("Rd08").addCreature("Lion");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Ranger");
        cnl.clear(); cnl.add("Cyclops"); cnl.add("Cyclops"); 
            cnl.add("Cyclops");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Behemoth");
        cnl.clear(); cnl.add("Angel"); cnl.add("Angel");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        cnl.clear(); cnl.add("Angel"); cnl.add("Angel");
        ps.getLeaf("Rd01").removeCreatures(cnl);
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertTrue(ps.getLeaf("Rd07").allCertain());
        assertTrue(ps.getLeaf("Rd08").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertTrue(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 11;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd06").revealCreatures(cnl);
        ps.getLeaf("Rd06").addCreature("Ranger");
        cnl.clear(); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd07").revealCreatures(cnl);
        ps.getLeaf("Rd07").addCreature("Lion");
        cnl.clear(); cnl.add("Lion");
        ps.getLeaf("Rd08").revealCreatures(cnl);
        ps.getLeaf("Rd08").addCreature("Lion");
        cnl.clear(); cnl.add("Titan");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Warlock");
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertTrue(ps.getLeaf("Rd07").allCertain());
        assertTrue(ps.getLeaf("Rd08").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertTrue(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 12;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd10").split(2, "Rd05", turn);
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Troll");
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd06").revealCreatures(cnl);
        ps.getLeaf("Rd06").addCreature("Warbear");
        cnl.clear(); cnl.add("Lion");
        ps.getLeaf("Rd07").revealCreatures(cnl);
        ps.getLeaf("Rd07").addCreature("Lion");
        cnl.clear(); cnl.add("Lion"); cnl.add("Lion");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Ranger");
        assertFalse(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertTrue(ps.getLeaf("Rd07").allCertain());
        assertTrue(ps.getLeaf("Rd08").allCertain());
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertTrue(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 13;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd11").split(2, "Rd04", turn);
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Warbear");
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Rd07").revealCreatures(cnl);
        ps.getLeaf("Rd07").addCreature("Cyclops");
        cnl.clear(); cnl.add("Lion"); cnl.add("Lion"); cnl.add("Centaur");
            cnl.add("Centaur");
        ps.getLeaf("Rd08").revealCreatures(cnl);
        ps.getLeaf("Rd08").removeCreatures(cnl);
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Ranger");
        assertFalse(ps.getLeaf("Rd04").allCertain());
        assertFalse(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertTrue(ps.getLeaf("Rd07").allCertain());
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 14;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Warbear"); cnl.add("Warbear"); 
            cnl.add("Ranger"); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd06").revealCreatures(cnl);
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Cyclops");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd06").revealCreatures(cnl);
        ps.getLeaf("Rd06").addCreature("Ranger");
        cnl.clear(); cnl.add("Wyvern"); cnl.add("Wyvern");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Hydra");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Ranger");
        assertFalse(ps.getLeaf("Rd04").allCertain());
        assertFalse(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertTrue(ps.getLeaf("Rd07").allCertain());
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 15;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd07").split(2, "Rd02", turn);
        ps.getLeaf("Rd11").split(2, "Rd01", turn);
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Troll");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd06").revealCreatures(cnl);
        ps.getLeaf("Rd06").addCreature("Ranger");
        cnl.clear(); cnl.add("Cyclops"); cnl.add("Cyclops");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Gorgon");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertFalse(ps.getLeaf("Rd04").allCertain());
        assertFalse(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertFalse(ps.getLeaf("Rd07").allCertain());
        assertFalse(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 16;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Warbear"); cnl.add("Warbear"); 
            cnl.add("Ranger"); cnl.add("Ranger"); cnl.add("Ranger"); 
            cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd06").revealCreatures(cnl);
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Ranger");
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Cyclops");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Ranger");
        cnl.clear(); cnl.add("Lion"); cnl.add("Lion");
        ps.getLeaf("Rd07").revealCreatures(cnl);
        ps.getLeaf("Rd07").addCreature("Ranger");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Ranger");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Ranger");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertFalse(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertFalse(ps.getLeaf("Rd07").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 17;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd06").split(2, "Rd08", turn);
        ps.getLeaf("Rd11").split(2, "Rd03", turn);
        cnl.clear(); cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd08").revealCreatures(cnl);
        ps.getLeaf("Rd08").removeCreatures(cnl);
        cnl.clear(); cnl.add("Warbear");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Warbear");
        cnl.clear(); cnl.add("Behemoth");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Behemoth");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertFalse(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertFalse(ps.getLeaf("Rd07").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 18;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd10").split(2, "Rd12", turn);
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Ranger");
        cnl.clear(); cnl.add("Gorgon");
        ps.getLeaf("Rd11").revealCreatures(cnl);
        ps.getLeaf("Rd11").addCreature("Gorgon");
        cnl.clear(); cnl.add("Ranger"); cnl.add("Ranger");
        ps.getLeaf("Rd12").revealCreatures(cnl);
        ps.getLeaf("Rd12").removeCreatures(cnl);
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertFalse(ps.getLeaf("Rd02").allCertain());
        assertFalse(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertFalse(ps.getLeaf("Rd07").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 19;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd11").split(2, "Rd08", turn);
        cnl.clear(); cnl.add("Cyclops"); cnl.add("Ranger"); cnl.add("Lion"); 
            cnl.add("Lion"); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd07").revealCreatures(cnl);
        cnl.clear(); cnl.add("Lion"); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd07").removeCreatures(cnl);
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd01").revealCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Ranger");
        cnl.clear(); cnl.add("Cyclops");
        ps.getLeaf("Rd03").revealCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Cyclops");
        cnl.clear(); cnl.add("Cyclops"); cnl.add("Cyclops");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Gorgon");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd07").revealCreatures(cnl);
        ps.getLeaf("Rd07").addCreature("Ranger");
        cnl.clear(); cnl.add("Ranger");
        ps.getLeaf("Rd08").revealCreatures(cnl);
        ps.getLeaf("Rd08").addCreature("Ranger");
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd02").allCertain());
        assertFalse(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertTrue(ps.getLeaf("Rd07").allCertain());
        assertFalse(ps.getLeaf("Rd08").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 20;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Gorgon"); cnl.add("Cyclops"); cnl.add("Cyclops"); 
            cnl.add("Cyclops"); cnl.add("Lion");
        ps.getLeaf("Rd04").revealCreatures(cnl);
        cnl.clear(); cnl.add("Titan"); cnl.add("Hydra"); cnl.add("Wyvern"); 
            cnl.add("Wyvern"); cnl.add("Warlock");
        ps.getLeaf("Rd10").revealCreatures(cnl);
        ps.getLeaf("Rd10").addCreature("Angel");
        cnl.clear(); cnl.add("Warbear"); cnl.add("Warbear"); 
            cnl.add("Ranger"); cnl.add("Ranger"); cnl.add("Troll"); 
            cnl.add("Troll"); cnl.add("Troll");
        ps.getLeaf("Rd05").revealCreatures(cnl);
        cnl.clear(); cnl.add("Troll");
        ps.getLeaf("Rd05").removeCreatures(cnl);
        cnl.clear(); cnl.add("Angel");
        ps.getLeaf("Rd10").removeCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Angel");
        cnl.clear(); cnl.add("Angel"); cnl.add("Warbear"); cnl.add("Warbear"); 
            cnl.add("Ranger"); cnl.add("Ranger"); cnl.add("Troll"); 
        ps.getLeaf("Rd05").removeCreatures(cnl);
        assertFalse(ps.getLeaf("Rd01").allCertain());
        assertTrue(ps.getLeaf("Rd02").allCertain());
        assertFalse(ps.getLeaf("Rd03").allCertain());
        assertTrue(ps.getLeaf("Rd04").allCertain());
        assertTrue(ps.getLeaf("Rd05").allCertain());
        assertTrue(ps.getLeaf("Rd06").allCertain());
        assertTrue(ps.getLeaf("Rd07").allCertain());
        assertFalse(ps.getLeaf("Rd08").allCertain());
        assertTrue(ps.getLeaf("Rd10").allCertain());
        assertFalse(ps.getLeaf("Rd11").allCertain());
        ps.printNodes();
        ps.printLeaves();
    }

    public void testPredictSplits3()
    {
        Log.debug("testPredictSplits3()");
        ps = new PredictSplits("Green", "Gr07", cnl);

        turn = 1;
        Log.debug("Turn " + turn);
        Node root = ps.getLeaf("Gr07");
        assertTrue(root != null);
        assertTrue(ps.getLeaves(root) != null);
        ps.getLeaf("Gr07").split(4, "Gr11", turn);
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Gr07").revealCreatures(cnl);
        ps.getLeaf("Gr07").addCreature("Cyclops");
        cnl.clear(); cnl.add("Centaur");
        ps.getLeaf("Gr11").revealCreatures(cnl);
        ps.getLeaf("Gr11").addCreature("Centaur");
        assertFalse(ps.getLeaf("Gr07").allCertain());
        assertFalse(ps.getLeaf("Gr11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 2;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Gr07").revealCreatures(cnl);
        ps.getLeaf("Gr07").addCreature("Cyclops");
        cnl.clear(); cnl.add("Ogre");
        ps.getLeaf("Gr11").revealCreatures(cnl);
        ps.getLeaf("Gr11").addCreature("Ogre");
        assertFalse(ps.getLeaf("Gr07").allCertain());
        assertFalse(ps.getLeaf("Gr11").allCertain());
        ps.printNodes();
        ps.printLeaves();

        turn = 3;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Centaur"); cnl.add("Centaur"); 
            cnl.add("Centaur");
        ps.getLeaf("Gr11").revealCreatures(cnl);
        ps.getLeaf("Gr11").addCreature("Warbear");
        assertFalse(ps.getLeaf("Gr07").allCertain());
        assertFalse(ps.getLeaf("Gr11").allCertain());
        ps.printNodes();
        ps.printLeaves();
    }

    public void testPredictSplits4()
    {
        Log.debug("testPredictSplits4()");
        CreatureInfoList cil = new CreatureInfoList();
        cil.add(new CreatureInfo("Angel", true, true));
        cil.add(new CreatureInfo("Gargoyle", true, true));
        cil.add(new CreatureInfo("Centaur", true, true));
        cil.add(new CreatureInfo("Centaur", false, true));
        cil.add(new CreatureInfo("Centaur", true, false));
        Node n = new Node("Gd10", 1, cil, null);
        cnl.clear();
        cnl.add("Gargoyle");
        cnl.add("Gargoyle");
        n.revealCreatures(cnl);
        assertTrue(n.allCertain());
    }


    public void testPredictSplits5()
    {
        cnl.clear();
        cnl.add("Titan");
        cnl.add("Angel");
        cnl.add("Gargoyle");
        cnl.add("Gargoyle");
        cnl.add("Centaur");
        cnl.add("Centaur");
        cnl.add("Ogre");
        cnl.add("Ogre");
        ps = new PredictSplits("Bk", "Bk06", cnl);
        ps.printLeaves();

        turn = 1;
        Log.debug("Turn " + turn);
        ps.getLeaf("Bk06").split(4, "Bk09", turn);
        cnl.clear();
        cnl.add("Gargoyle"); 
        cnl.add("Gargoyle"); 
        ps.getLeaf("Bk09").revealCreatures(cnl);
        ps.getLeaf("Bk09").addCreature("Cyclops");
        assertFalse(ps.getLeaf("Bk06").allCertain());
        assertFalse(ps.getLeaf("Bk09").allCertain());
        ps.printLeaves();

        turn = 2;
        Log.debug("Turn " + turn);
        cnl.clear();
        cnl.add("Ogre"); 
        ps.getLeaf("Bk09").revealCreatures(cnl);
        ps.getLeaf("Bk09").addCreature("Ogre");
        assertFalse(ps.getLeaf("Bk06").allCertain());
        assertFalse(ps.getLeaf("Bk09").allCertain());
        ps.printLeaves();

        turn = 3;
        Log.debug("Turn " + turn);
        cnl.clear();
        cnl.add("Centaur"); 
        cnl.add("Centaur"); 
        ps.getLeaf("Bk06").revealCreatures(cnl);
        ps.getLeaf("Bk06").addCreature("Lion");
        cnl.clear();
        cnl.add("Gargoyle"); 
        cnl.add("Gargoyle"); 
        ps.getLeaf("Bk09").revealCreatures(cnl);
        ps.getLeaf("Bk09").addCreature("Cyclops");
        assertFalse(ps.getLeaf("Bk06").allCertain());
        assertFalse(ps.getLeaf("Bk09").allCertain());
        ps.printLeaves();

        turn = 4;
        Log.debug("Turn " + turn);
        ps.getLeaf("Bk09").split(2, "Bk10", turn);
        cnl.clear();
        cnl.add("Titan"); 
        ps.getLeaf("Bk06").revealCreatures(cnl);
        ps.getLeaf("Bk06").addCreature("Warlock");
        ps.getLeaf("Bk09").addCreature("Ogre");
        assertTrue(ps.getLeaf("Bk06").allCertain());
        assertFalse(ps.getLeaf("Bk09").allCertain());
        assertFalse(ps.getLeaf("Bk10").allCertain());
        ps.printLeaves();

        turn = 5;
        Log.debug("Turn " + turn);
        cnl.clear();
        cnl.add("Centaur"); 
        cnl.add("Centaur"); 
        ps.getLeaf("Bk06").revealCreatures(cnl);
        ps.getLeaf("Bk06").addCreature("Lion");
        cnl.clear();
        cnl.add("Cyclops"); 
        ps.getLeaf("Bk09").revealCreatures(cnl);
        ps.getLeaf("Bk09").addCreature("Cyclops");
        assertTrue(ps.getLeaf("Bk06").allCertain());
        assertFalse(ps.getLeaf("Bk09").allCertain());
        assertFalse(ps.getLeaf("Bk10").allCertain());
        ps.printLeaves();
        ps.getLeaf("Bk10").removeCreature("Gargoyle");
        ps.getLeaf("Bk10").removeCreature("Gargoyle");
        assertTrue(ps.getLeaf("Bk06").allCertain());
        assertTrue(ps.getLeaf("Bk09").allCertain());
        ps.printLeaves();
    }
}


class AllPredictSplits
{
    private List pslist = new ArrayList();

    void add_ps(PredictSplits ps)
    {
        pslist.add(ps);
    }

    Node getLeaf(String markerId)
    {
        Iterator it = pslist.iterator();
        while (it.hasNext())
        {
            PredictSplits ps = (PredictSplits)it.next();
            Node leaf = ps.getLeaf(markerId);
            if (leaf != null)
            {
                return leaf;
            }
        }
        return null;
    }

    void printLeaves()
    {
        Iterator it = pslist.iterator();
        while (it.hasNext())
        {
            PredictSplits ps = (PredictSplits)it.next();
            ps.printLeaves();
        }
    }

    void printNodes()
    {
        Iterator it = pslist.iterator();
        while (it.hasNext())
        {
            PredictSplits ps = (PredictSplits)it.next();
            ps.printNodes();
        }
    }
}
