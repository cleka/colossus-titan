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
    CreatureNameList cnl;
    PredictSplits ps;


    public PredictSplitsTest(String name)
    {
        super(name);
    }

    protected void setUp()
    {
        // Needed for Creatures
        Game game = new Game();
        VariantSupport.loadVariant("Default");

        cnl = new CreatureNameList();
        cnl.add("Titan");
        cnl.add("Angel");
        cnl.add("Centaur");
        cnl.add("Centaur");
        cnl.add("Gargoyle");
        cnl.add("Gargoyle");
        cnl.add("Ogre");
        cnl.add("Ogre");

        ps = new PredictSplits("Red", "Rd01", cnl);
    }


    public void testPredictSplits()
    {
        int turn = 1;
        Log.debug("Turn " + turn);
        Node root = ps.getLeaf("Rd01");
        assertTrue(root != null);
        assertTrue(ps.getLeaves(root) != null);
        ps.getLeaf("Rd01").split(4, "Rd02", turn);
        cnl.clear(); cnl.add("Ogre"); cnl.add("Ogre");
        ps.getLeaf("Rd01").revealSomeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Troll");
        cnl.clear(); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd02").revealSomeCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Lion");
        ps.printLeaves();

        turn = 2;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Gargoyle");
        ps.getLeaf("Rd01").revealSomeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Gargoyle");
        cnl.clear(); cnl.add("Lion");
        ps.getLeaf("Rd02").revealSomeCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Lion");
        ps.printLeaves();

        turn = 3;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Titan");
        ps.getLeaf("Rd01").revealSomeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Warlock");
        ps.getLeaf("Rd02").addCreature("Gargoyle");
        ps.printLeaves();

        turn = 4;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd01").split(2, "Rd03", turn);
        ps.getLeaf("Rd02").split(2, "Rd04", turn);
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Rd01").revealSomeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Cyclops");
        cnl.clear(); cnl.add("Gargoyle"); cnl.add("Gargoyle");
        ps.getLeaf("Rd02").revealSomeCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Cyclops");
        ps.printLeaves();

        turn = 5;
        Log.debug("Turn " + turn);
        cnl.clear(); cnl.add("Warlock");
        ps.getLeaf("Rd01").revealSomeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Warlock");
        ps.getLeaf("Rd02").addCreature("Ogre");
        cnl.clear(); cnl.add("Ogre"); cnl.add("Ogre");
        ps.getLeaf("Rd03").revealSomeCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Troll");
        cnl.clear(); cnl.add("Centaur"); cnl.add("Centaur");
        ps.getLeaf("Rd04").revealSomeCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Lion");
        ps.printLeaves();

        turn = 6;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd02").split(2, "Rd05", turn);
        cnl.clear();
        cnl.add("Titan");
        cnl.add("Warlock");
        cnl.add("Warlock");
        cnl.add("Cyclops");
        cnl.add("Troll");
        cnl.add("Gargoyle");
        cnl.add("Gargoyle");
        ps.getLeaf("Rd01").revealAllCreatures(cnl);
        ps.getLeaf("Rd01").removeCreature("Gargoyle");
        ps.getLeaf("Rd01").removeCreature("Gargoyle");
        ps.getLeaf("Rd02").removeCreature("Angel");
        ps.getLeaf("Rd01").addCreature("Angel");
        cnl.clear();
        cnl.add("Lion");
        cnl.add("Lion");
        ps.getLeaf("Rd02").revealSomeCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Minotaur");
        cnl.clear();
        cnl.add("Lion");
        ps.getLeaf("Rd04").revealSomeCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Lion");
        cnl.clear();
        cnl.add("Cyclops");
        cnl.add("Minotaur");
        cnl.add("Lion");
        cnl.add("Lion");
        cnl.add("Ogre");
        ps.getLeaf("Rd02").revealAllCreatures(cnl);
        ps.getLeaf("Rd02").addCreature("Minotaur");
        ps.getLeaf("Rd02").removeAllCreatures();
        ps.printLeaves();

        turn = 7;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd01").addCreature("Angel");
        cnl.clear();
        cnl.add("Troll");
        ps.getLeaf("Rd03").revealSomeCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Troll");
        cnl.clear();
        cnl.add("Lion");
        ps.getLeaf("Rd04").revealSomeCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Lion");
        ps.printLeaves();

        turn = 8;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd01").split(2, "Rd02", turn);
        cnl.clear();
        cnl.add("Cyclops");
        ps.getLeaf("Rd01").revealSomeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Cyclops");
        cnl.clear();
        cnl.add("Gargoyle");
        cnl.add("Gargoyle");
        ps.getLeaf("Rd05").revealSomeCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Cyclops");
        ps.printLeaves();

        turn = 9;
        Log.debug("Turn " + turn);
        cnl.clear();
        cnl.add("Troll");
        ps.getLeaf("Rd01").revealSomeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Troll");
        cnl.clear();
        cnl.add("Troll");
        ps.getLeaf("Rd03").revealSomeCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Troll");
        cnl.clear();
        cnl.add("Lion");
        cnl.add("Lion");
        cnl.add("Lion");
        ps.getLeaf("Rd04").revealSomeCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Griffon");
        cnl.clear();
        cnl.add("Cyclops");
        ps.getLeaf("Rd05").revealSomeCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Cyclops");
        ps.printLeaves();

        turn = 10;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd01").split(2, "Rd06", turn);
        ps.printLeaves();

        turn = 11;
        Log.debug("Turn " + turn);
        cnl.clear();
        cnl.add("Griffon");
        cnl.add("Lion");
        cnl.add("Lion");
        cnl.add("Lion");
        cnl.add("Centaur");
        cnl.add("Centaur");
        ps.getLeaf("Rd04").revealAllCreatures(cnl);
        cnl.clear();
        cnl.add("Cyclops");
        ps.getLeaf("Rd01").revealSomeCreatures(cnl);
        ps.getLeaf("Rd01").addCreature("Cyclops");
        cnl.clear();
        cnl.add("Troll");
        cnl.add("Troll");
        ps.getLeaf("Rd03").revealSomeCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Ranger");
        ps.printLeaves();

        turn = 12;
        Log.debug("Turn " + turn);
        ps.getLeaf("Rd02").addCreature("Centaur");
        cnl.clear();
        cnl.add("Troll");
        cnl.add("Troll");
        ps.getLeaf("Rd03").revealSomeCreatures(cnl);
        ps.getLeaf("Rd03").addCreature("Warbear");
        cnl.clear();
        cnl.add("Cyclops");
        ps.getLeaf("Rd05").revealSomeCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Cyclops");
        ps.printLeaves();

        turn = 13;
        Log.debug("Turn " + turn);
        cnl.clear();
        cnl.add("Titan");
        cnl.add("Warlock");
        cnl.add("Warlock");
        cnl.add("Cyclops");
        cnl.add("Cyclops");
        cnl.add("Cyclops");
        ps.getLeaf("Rd01").revealAllCreatures(cnl);
        cnl.clear();
        cnl.add("Cyclops");
        cnl.add("Cyclops");
        cnl.add("Cyclops");
        ps.getLeaf("Rd05").revealSomeCreatures(cnl);
        ps.getLeaf("Rd05").addCreature("Behemoth");
        ps.printLeaves();

        turn = 14;
        Log.debug("Turn " + turn);
        cnl.clear();
        cnl.add("Griffon");
        cnl.add("Lion");
        cnl.add("Lion");
        cnl.add("Lion");
        cnl.add("Centaur");
        cnl.add("Centaur");
        ps.getLeaf("Rd04").revealAllCreatures(cnl);
        ps.getLeaf("Rd02").removeCreature("Angel");
        ps.getLeaf("Rd04").addCreature("Angel");
        cnl.clear();
        cnl.add("Angel");
        cnl.add("Lion");
        cnl.add("Lion");
        cnl.add("Lion");
        cnl.add("Centaur");
        cnl.add("Centaur");
        ps.getLeaf("Rd04").removeCreatures(cnl);
        ps.getLeaf("Rd04").addCreature("Angel");
        ps.printLeaves();
    }
}

