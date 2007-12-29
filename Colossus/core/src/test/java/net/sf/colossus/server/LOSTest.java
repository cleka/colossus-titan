package net.sf.colossus.server;


import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;


/** 
 *  JUnit test for line of sight. 
 *  @version $Id$
 *  @author David Ripton
 */
public class LOSTest extends TestCase
{
    // TODO the unit test might as well use stdout
    private static final Logger LOGGER = Logger.getLogger(LOSTest.class
        .getName());

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
    Creature dragon;
    Creature minotaur;
    Creature guardian;

    public LOSTest(String name)
    {
        super(name);
    }

    protected void setUp()
    {
        game = new Game();
        VariantSupport.loadVariant("Default", true);

        game.addPlayer("Black", "SimpleAI");
        game.addPlayer("Green", "SimpleAI");
        game.addPlayer("Red", "SimpleAI");
        game.addPlayer("Blue", "SimpleAI");

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
        dragon = Creature.getCreatureByName("Dragon");
        minotaur = Creature.getCreatureByName("Minotaur");
        guardian = Creature.getCreatureByName("Guardian");
    }

    // Example 6 from Bruno Wolff's clarifications.
    // TODO Should allow two different strike numbers against centaur.
    public void testLOS1()
    {
        LOGGER.log(Level.FINEST, "testLOS1()");
        String hexLabel = "3"; // Brush

        defender = new Legion("Rd03", "Rd01", hexLabel, null, centaur,
            gargoyle, null, null, null, null, null, null, "Red", game);
        attacker = new Legion("Bl03", "Bl01", hexLabel, null, hydra, null,
            null, null, null, null, null, null, "Black", game);

        game.getPlayer("Red").addLegion(defender);
        game.getPlayer("Blue").addLegion(attacker);

        attacker.setEntrySide(1);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        Critter centaur1 = defender.getCritter(0);
        Critter gargoyle1 = defender.getCritter(1);

        Critter hydra1 = attacker.getCritter(0);

        centaur1.setCurrentHexLabel("B3");
        gargoyle1.setCurrentHexLabel("B4");

        hydra1.setCurrentHexLabel("D4");

        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), gargoyle1
            .getCurrentHex()));
    }

    public void testLOS2()
    {
        LOGGER.log(Level.FINEST, "testLOS2()");
        String hexLabel = "40"; // Jungle

        defender = new Legion("Gr03", "Gr01", hexLabel, null, centaur,
            centaur, lion, lion, ranger, ranger, ranger, null, "Green", game);
        attacker = new Legion("Bk03", "Bk01", hexLabel, null, gargoyle,
            cyclops, cyclops, cyclops, gorgon, gorgon, ranger, null, "Black",
            game);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

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

        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), gargoyle1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), cyclops1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), cyclops2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), cyclops3
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), gorgon1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), gorgon2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), ranger4
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), gargoyle1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), cyclops1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), cyclops2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), cyclops3
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), ranger4
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), gargoyle1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), cyclops1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), cyclops2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), cyclops3
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), gorgon1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), gorgon2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), ranger4
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), centaur2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(), lion2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), ranger2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), ranger3
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), centaur2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), lion2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon2.getCurrentHex(), ranger2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon2.getCurrentHex(), ranger3
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), centaur2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), lion2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger4.getCurrentHex(), ranger2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger4.getCurrentHex(), ranger3
            .getCurrentHex()));
    }

    public void testLOS3()
    {
        LOGGER.log(Level.FINEST, "testLOS3()");
        String hexLabel = "100"; // Tower

        defender = new Legion("Gr03", "Gr01", hexLabel, null, centaur, lion,
            ranger, ranger, null, null, null, null, "Green", game);
        attacker = new Legion("Bk03", "Bk01", hexLabel, null, cyclops, gorgon,
            gorgon, gorgon, gorgon, ranger, ranger, null, "Black", game);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(3);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        Critter centaur1 = defender.getCritter(0);
        Critter lion1 = defender.getCritter(1);
        Critter ranger1 = defender.getCritter(2);
        Critter ranger2 = defender.getCritter(3);

        Critter cyclops1 = attacker.getCritter(0);
        Critter gorgon1 = attacker.getCritter(1);
        Critter gorgon2 = attacker.getCritter(2);
        Critter gorgon3 = attacker.getCritter(3);
        Critter gorgon4 = attacker.getCritter(4);
        Critter ranger3 = attacker.getCritter(5);
        Critter ranger4 = attacker.getCritter(6);

        centaur1.setCurrentHexLabel("D3");
        lion1.setCurrentHexLabel("E3");
        ranger1.setCurrentHexLabel("C3");
        ranger2.setCurrentHexLabel("D4");

        cyclops1.setCurrentHexLabel("D2");
        gorgon1.setCurrentHexLabel("A3");
        gorgon2.setCurrentHexLabel("A2");
        gorgon3.setCurrentHexLabel("A1");
        gorgon4.setCurrentHexLabel("C1");
        ranger3.setCurrentHexLabel("F1");
        ranger4.setCurrentHexLabel("B4");

        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), cyclops1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), gorgon1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), gorgon2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), gorgon3
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), gorgon4
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), ranger3
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), ranger4
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), cyclops1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon3
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon4
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), ranger3
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), ranger4
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon2.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(!battle.isLOSBlocked(gorgon3.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon3.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon3.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon3.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(!battle.isLOSBlocked(gorgon4.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon4.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon4.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon4.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger4.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), ranger2
            .getCurrentHex()));
    }

    public void testLOS4()
    {
        LOGGER.log(Level.FINEST, "testLOS4()");
        String hexLabel = "4"; // Hills

        defender = new Legion("Gr03", "Gr01", hexLabel, null, centaur,
            centaur, lion, lion, ranger, ranger, null, null, "Green", game);
        attacker = new Legion("Bk03", "Bk01", hexLabel, null, gorgon, gorgon,
            ranger, ranger, null, null, null, null, "Black", game);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(3);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        Critter centaur1 = defender.getCritter(0);
        Critter centaur2 = defender.getCritter(1);
        Critter lion1 = defender.getCritter(2);
        Critter lion2 = defender.getCritter(3);
        Critter ranger1 = defender.getCritter(4);
        Critter ranger2 = defender.getCritter(5);

        Critter gorgon1 = attacker.getCritter(0);
        Critter gorgon2 = attacker.getCritter(1);
        Critter ranger3 = attacker.getCritter(2);
        Critter ranger4 = attacker.getCritter(3);

        centaur1.setCurrentHexLabel("D4");
        centaur2.setCurrentHexLabel("B1");
        lion1.setCurrentHexLabel("C5");
        lion2.setCurrentHexLabel("B2");
        ranger1.setCurrentHexLabel("D6");
        ranger2.setCurrentHexLabel("E3");

        gorgon1.setCurrentHexLabel("D2");
        gorgon2.setCurrentHexLabel("D3");
        ranger3.setCurrentHexLabel("B3");
        ranger4.setCurrentHexLabel("F1");

        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), gorgon1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), gorgon2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), ranger3
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), ranger4
            .getCurrentHex()));

        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), ranger3
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), ranger4
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(), centaur2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), lion2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon1.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(!battle.isLOSBlocked(gorgon2.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), centaur2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon2.getCurrentHex(), lion2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(gorgon2.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(gorgon2.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), centaur2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), lion2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), centaur2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), lion1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), lion2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger4.getCurrentHex(), ranger1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger4.getCurrentHex(), ranger2
            .getCurrentHex()));
    }

    public void testLOS5()
    {
        LOGGER.log(Level.FINEST, "testLOS5()");
        VariantSupport.loadVariant("TG-ConceptIII", true);
        String hexLabel = "1"; // Plains - Delta

        defender = new Legion("Gr03", "Gr01", hexLabel, null, troll, troll,
            troll, troll, wyvern, null, null, null, "Green", game);
        attacker = new Legion("Bk03", "Bk01", hexLabel, null, ranger, ranger,
            ranger, null, null, null, null, null, "Black", game);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 2,
            Constants.BattlePhase.FIGHT);

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

        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), troll1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), troll2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), troll3
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), troll4
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), wyvern1
            .getCurrentHex()));
        assertTrue(!ranger1.canStrike(troll1));
        assertTrue(!ranger1.canStrike(troll2));
        assertTrue(ranger1.canStrike(troll3));
        assertTrue(!ranger1.canStrike(troll4));
        assertTrue(!ranger1.canStrike(wyvern1));

        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), troll1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), troll2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), troll3
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), troll4
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), wyvern1
            .getCurrentHex()));
        assertTrue(!ranger2.canStrike(troll1));
        assertTrue(!ranger2.canStrike(troll2));
        assertTrue(!ranger2.canStrike(troll3));
        assertTrue(!ranger2.canStrike(troll4));
        assertTrue(ranger2.canStrike(wyvern1));

        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), troll1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), troll2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), troll3
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), troll4
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), wyvern1
            .getCurrentHex()));
        assertTrue(!ranger3.canStrike(troll1));
        assertTrue(!ranger3.canStrike(troll2));
        assertTrue(!ranger3.canStrike(troll3));
        assertTrue(!ranger3.canStrike(troll4));
        assertTrue(ranger3.canStrike(wyvern1));
    }

    public void testLOS6()
    {
        LOGGER.log(Level.FINEST, "testLOS6()");
        VariantSupport.loadVariant("Badlands-JDG", true);
        String hexLabel = "5000"; // MountainsAlt

        defender = new Legion("Gr03", "Gr01", hexLabel, null, dragon, dragon,
            minotaur, minotaur, minotaur, null, null, null, "Green", game);
        attacker = new Legion("Bk03", "Bk01", hexLabel, null, ranger, ranger,
            ranger, null, null, null, null, null, "Black", game);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 2,
            Constants.BattlePhase.FIGHT);

        Critter dragon1 = defender.getCritter(0);
        Critter dragon2 = defender.getCritter(1);
        Critter minotaur1 = defender.getCritter(2);
        Critter minotaur2 = defender.getCritter(3);
        Critter minotaur3 = defender.getCritter(4);

        Critter ranger1 = attacker.getCritter(0);
        Critter ranger2 = attacker.getCritter(1);
        Critter ranger3 = attacker.getCritter(2);

        dragon1.setCurrentHexLabel("D3");
        dragon2.setCurrentHexLabel("C3");
        minotaur1.setCurrentHexLabel("E4");
        minotaur2.setCurrentHexLabel("B2");
        minotaur3.setCurrentHexLabel("A1");

        ranger1.setCurrentHexLabel("E2");
        ranger2.setCurrentHexLabel("C2");
        ranger3.setCurrentHexLabel("E5");

        assertTrue(!battle.isLOSBlocked(ranger1.getCurrentHex(), dragon1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), dragon2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), minotaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), minotaur2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger1.getCurrentHex(), minotaur3
            .getCurrentHex()));
        assertTrue(ranger1.canStrike(dragon1));
        assertTrue(!ranger1.canStrike(dragon2));
        assertTrue(!ranger1.canStrike(minotaur1));
        assertTrue(!ranger1.canStrike(minotaur2));
        assertTrue(!ranger1.canStrike(minotaur3));

        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), dragon1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), dragon2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), minotaur1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), minotaur2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger2.getCurrentHex(), minotaur3
            .getCurrentHex()));
        assertTrue(ranger2.canStrike(dragon1));
        assertTrue(!ranger2.canStrike(dragon2));
        assertTrue(!ranger2.canStrike(minotaur1));
        assertTrue(ranger2.canStrike(minotaur2));
        assertTrue(!ranger2.canStrike(minotaur3));

        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), dragon1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), dragon2
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(ranger3.getCurrentHex(), minotaur1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), minotaur2
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger3.getCurrentHex(), minotaur3
            .getCurrentHex()));
        assertTrue(!ranger3.canStrike(dragon1));
        assertTrue(!ranger3.canStrike(dragon2));
        assertTrue(ranger3.canStrike(minotaur1));
        assertTrue(!ranger3.canStrike(minotaur2));
        assertTrue(!ranger3.canStrike(minotaur3));
    }

    public void testLOS7()
    {
        LOGGER.log(Level.FINEST, "testLOS7()");
        String hexLabel = "40"; // Jungle

        defender = new Legion("Gr03", "Gr01", hexLabel, null, hydra, null,
            null, null, null, null, null, null, "Green", game);
        attacker = new Legion("Bk03", "Bk01", hexLabel, null, hydra, guardian,
            null, null, null, null, null, null, "Black", game);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        Critter hydra1 = defender.getCritter(0);
        Critter hydra2 = attacker.getCritter(0);
        Critter guardian1 = attacker.getCritter(1);

        hydra1.setCurrentHexLabel("D5");
        hydra2.setCurrentHexLabel("E3");
        guardian1.setCurrentHexLabel("E4");

        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), hydra2
            .getCurrentHex()));
        assertEquals(5, hydra2.getStrikeNumber(hydra1));
    }
}
