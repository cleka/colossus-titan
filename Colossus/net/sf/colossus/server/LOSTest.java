package net.sf.colossus.server;

import java.util.*;
import junit.framework.*;

import net.sf.colossus.client.VariantSupport;
import net.sf.colossus.client.MasterBoard;


/** 
 *  JUnit test for line of sight. 
 *  @version $Id$
 *  @author David Ripton
 */
public class LOSTest extends TestCase
{
    Game game;
    Battle battle;
    Legion attacker;
    Legion defender;
    Creature cyclops;
    Creature troll;
    Creature ogre;
    Creature ranger;
    Creature gorgon;
    Creature lion;
    Creature griffon;
    Creature hydra;
    Creature centaur;
    Creature colossus;
    Creature gargoyle;
    Creature wyvern;


    public LOSTest(String name)
    {
        super(name);
    }

    protected void setUp()
    {
        game = new Game();
        VariantSupport.loadVariant("Default");
        game.initAndLoadData();  // Will load creatures

        game.addPlayer("Black", "SimpleAI");
        game.addPlayer("Green", "SimpleAI");

        // Need a non-GUI board so we can look up hexes.
        MasterBoard board = new MasterBoard();

        cyclops = Creature.getCreatureByName("Cyclops");
        troll = Creature.getCreatureByName("Troll");
        ogre = Creature.getCreatureByName("Ogre");
        ranger = Creature.getCreatureByName("Ranger");
        gorgon = Creature.getCreatureByName("Gorgon");
        lion = Creature.getCreatureByName("Lion");
        griffon = Creature.getCreatureByName("Griffon");
        hydra = Creature.getCreatureByName("Hydra");
        centaur = Creature.getCreatureByName("Centaur");
        colossus = Creature.getCreatureByName("Colossus");
        gargoyle = Creature.getCreatureByName("Gargoyle");
        wyvern = Creature.getCreatureByName("Wyvern");
    }

    public void testLOS()
    {
        String hexLabel = "40";  // Jungle

        defender = new Legion("Gr03", "Gr01", hexLabel, null,
            centaur, centaur, lion, lion, ranger, ranger, ranger, null,
            "Green", game);
        attacker = new Legion("Bk03", "Bk01", hexLabel, null,
            gargoyle, cyclops, cyclops, cyclops, gorgon, gorgon, ranger, null,
            "Black", game);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), 
            defender.getMarkerId(), Constants.ATTACKER, hexLabel,
            1, Constants.FIGHT);

        Critter centaur1 = defender.getCritter(0);
        Critter centaur2 = defender.getCritter(1);
        Critter lion1 = defender.getCritter(2);
        Critter lion2 = defender.getCritter(3);
        Critter ranger1 = defender.getCritter(4);
        Critter ranger2 = defender.getCritter(5);
        Critter ranger3 = defender.getCritter(6);

        Critter gargoyle1 = attacker.getCritter(0);
        Critter cyclops1 = attacker.getCritter(1);
        Critter cyclops2 = attacker.getCritter(2);
        Critter cyclops3 = attacker.getCritter(3);
        Critter gorgon1 = attacker.getCritter(4);
        Critter gorgon2 = attacker.getCritter(5);
        Critter ranger4 = attacker.getCritter(6);

        centaur1.setCurrentHexLabel("D1");
        centaur2.setCurrentHexLabel("E1");
        lion1.setCurrentHexLabel("F1");
        lion2.setCurrentHexLabel("C1");
        ranger1.setCurrentHexLabel("D2");
        ranger2.setCurrentHexLabel("E2");
        ranger3.setCurrentHexLabel("F2");

        gargoyle1.setCurrentHexLabel("A1");
        cyclops1.setCurrentHexLabel("A2");
        cyclops2.setCurrentHexLabel("C4");
        cyclops3.setCurrentHexLabel("E5");
        gorgon1.setCurrentHexLabel("C3");
        gorgon2.setCurrentHexLabel("D4");
        ranger4.setCurrentHexLabel("E4");

        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(),
            gargoyle1.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(),
            cyclops1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(),
            cyclops2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(),
            cyclops3.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(),
            gorgon1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(),
            gorgon2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(),
            ranger4.getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(),
            gargoyle1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(),
            cyclops1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(),
            cyclops2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(),
            cyclops3.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(),
            gorgon1.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(),
            gorgon2.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(),
            ranger4.getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(),
            gargoyle1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(),
            cyclops1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(),
            cyclops2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(),
            cyclops3.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(),
            gorgon1.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(),
            gorgon2.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(),
            ranger4.getCurrentHex()));


        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(),
            centaur1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(),
            centaur2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(),
            lion1.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(),
            lion2.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(),
            ranger1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(),
            ranger2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(),
            ranger3.getCurrentHex()));

        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(),
            centaur1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(),
            centaur2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(),
            lion1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(),
            lion2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(),
            ranger1.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon2.getCurrentHex(),
            ranger2.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon2.getCurrentHex(),
            ranger3.getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(),
            centaur1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(),
            centaur2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(),
            lion1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(),
            lion2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(),
            ranger1.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger4.getCurrentHex(),
            ranger2.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger4.getCurrentHex(),
            ranger3.getCurrentHex()));
    }

    public void testLOS2()
    {
        VariantSupport.loadVariant("TG-ConceptIII");
        String hexLabel = "1";  // Plains - Delta

        defender = new Legion("Gr03", "Gr01", hexLabel, null,
            troll, troll, troll, troll, wyvern, null, null, null,
            "Green", game);
        attacker = new Legion("Bk03", "Bk01", hexLabel, null,
            ranger, ranger, ranger, null, null, null, null, null,
            "Black", game);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), 
            defender.getMarkerId(), Constants.ATTACKER, hexLabel,
            2, Constants.FIGHT);

        Critter troll1 = defender.getCritter(0);
        Critter troll2 = defender.getCritter(1);
        Critter troll3 = defender.getCritter(2);
        Critter troll4 = defender.getCritter(3);
        Critter wyvern1 = defender.getCritter(4);

        Critter ranger1 = attacker.getCritter(0);
        Critter ranger2 = attacker.getCritter(1);
        Critter ranger3 = attacker.getCritter(2);

        troll1.setCurrentHexLabel("D6");
        troll2.setCurrentHexLabel("B3");
        troll3.setCurrentHexLabel("C3");
        troll4.setCurrentHexLabel("E4");
        wyvern1.setCurrentHexLabel("E3");

        ranger1.setCurrentHexLabel("E1");
        ranger2.setCurrentHexLabel("E2");
        ranger3.setCurrentHexLabel("F2");


        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(),
            troll1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(),
            troll2.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(),
            troll3.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(),
            troll4.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(),
            wyvern1.getCurrentHex()));
        assertTrue(!ranger1.canStrike(troll1));
        assertTrue(!ranger1.canStrike(troll2));
        assertTrue(ranger1.canStrike(troll3));
        assertTrue(!ranger1.canStrike(troll4));
        assertTrue(!ranger1.canStrike(wyvern1));

        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(),
            troll1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(),
            troll2.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(),
            troll3.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(),
            troll4.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(),
            wyvern1.getCurrentHex()));
        assertTrue(!ranger2.canStrike(troll1));
        assertTrue(!ranger2.canStrike(troll2));
        assertTrue(!ranger2.canStrike(troll3));
        assertTrue(!ranger2.canStrike(troll4));
        assertTrue(ranger2.canStrike(wyvern1));

        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(),
            troll1.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(),
            troll2.getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(),
            troll3.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(),
            troll4.getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(),
            wyvern1.getCurrentHex()));
        assertTrue(!ranger3.canStrike(troll1));
        assertTrue(!ranger3.canStrike(troll2));
        assertTrue(!ranger3.canStrike(troll3));
        assertTrue(!ranger3.canStrike(troll4));
        assertTrue(ranger3.canStrike(wyvern1));
    }
}
