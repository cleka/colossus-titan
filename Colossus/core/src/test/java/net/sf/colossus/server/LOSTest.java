package net.sf.colossus.server;


import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.game.Player;


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

    GameServerSide game;
    BattleServerSide battle;
    LegionServerSide attacker;
    LegionServerSide defender;
    CreatureTypeServerSide cyclops;
    CreatureTypeServerSide troll;
    CreatureTypeServerSide ogre;
    CreatureTypeServerSide ranger;
    CreatureTypeServerSide gorgon;
    CreatureTypeServerSide lion;
    CreatureTypeServerSide griffon;
    CreatureTypeServerSide hydra;
    CreatureTypeServerSide centaur;
    CreatureTypeServerSide colossus;
    CreatureTypeServerSide gargoyle;
    CreatureTypeServerSide wyvern;
    CreatureTypeServerSide dragon;
    CreatureTypeServerSide minotaur;
    CreatureTypeServerSide guardian;
    Player black;
    Player green;
    Player red;
    Player blue;

    public LOSTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        game = new GameServerSide();
        VariantSupport.loadVariant("Default", true);

        black = game.addPlayer("Black", "SimpleAI");
        green = game.addPlayer("Green", "SimpleAI");
        red = game.addPlayer("Red", "SimpleAI");
        blue = game.addPlayer("Blue", "SimpleAI");

        cyclops = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Cyclops");
        troll = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Troll");
        ogre = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Ogre");
        ranger = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Ranger");
        gorgon = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Gorgon");
        lion = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Lion");
        griffon = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Griffon");
        hydra = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Hydra");
        centaur = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Centaur");
        colossus = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Colossus");
        gargoyle = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Gargoyle");
        wyvern = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Wyvern");
        dragon = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Dragon");
        minotaur = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Minotaur");
        guardian = (CreatureTypeServerSide)game.getVariant().getCreatureByName("Guardian");
    }

    // Example 6 from Bruno Wolff's clarifications.
    // TODO Should allow two different strike numbers against centaur.
    public void testLOS1()
    {
        LOGGER.log(Level.FINEST, "testLOS1()");
        String hexLabel = "3"; // Brush

        defender = new LegionServerSide("Rd03", "Rd01", hexLabel, null, red, game,
            centaur, gargoyle);
        attacker = new LegionServerSide("Bl03", "Bl01", hexLabel, null, black, game,
            hydra);

        game.getPlayer("Red").addLegion(defender);
        game.getPlayer("Blue").addLegion(attacker);

        attacker.setEntrySide(1);

        battle = new BattleServerSide(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        CreatureServerSide centaur1 = defender.getCritter(0);
        CreatureServerSide gargoyle1 = defender.getCritter(1);

        CreatureServerSide hydra1 = attacker.getCritter(0);

        placeCreature(centaur1, "B3");
        placeCreature(gargoyle1, "B4");

        placeCreature(hydra1, "D4");

        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), gargoyle1
            .getCurrentHex()));
    }

    private void placeCreature(CreatureServerSide creature, String battleHexLabel)
    {
        String terrain = battle.getTerrain();
        BattleHex battleHex = HexMap.getHexByLabel(terrain, battleHexLabel);
        creature.setCurrentHex(battleHex);
    }

    public void testLOS2()
    {
        LOGGER.log(Level.FINEST, "testLOS2()");
        String hexLabel = "40"; // Jungle

        defender = new LegionServerSide("Gr03", "Gr01", hexLabel, null, green, game,
            centaur, centaur, lion, lion, ranger, ranger, ranger);
        attacker = new LegionServerSide("Bk03", "Bk01", hexLabel, null, black, game,
            gargoyle, cyclops, cyclops, cyclops, gorgon, gorgon, ranger);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new BattleServerSide(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        CreatureServerSide centaur1 = defender.getCritter(0);
        CreatureServerSide centaur2 = defender.getCritter(1);
        CreatureServerSide lion1 = defender.getCritter(2);
        CreatureServerSide lion2 = defender.getCritter(3);
        CreatureServerSide ranger1 = defender.getCritter(4);
        CreatureServerSide ranger2 = defender.getCritter(5);
        CreatureServerSide ranger3 = defender.getCritter(6);

        CreatureServerSide gargoyle1 = attacker.getCritter(0);
        CreatureServerSide cyclops1 = attacker.getCritter(1);
        CreatureServerSide cyclops2 = attacker.getCritter(2);
        CreatureServerSide cyclops3 = attacker.getCritter(3);
        CreatureServerSide gorgon1 = attacker.getCritter(4);
        CreatureServerSide gorgon2 = attacker.getCritter(5);
        CreatureServerSide ranger4 = attacker.getCritter(6);

        placeCreature(centaur1, "D1");
        placeCreature(centaur2, "E1");
        placeCreature(lion1, "F1");
        placeCreature(lion2, "C1");
        placeCreature(ranger1, "D2");
        placeCreature(ranger2, "E2");
        placeCreature(ranger3, "F2");

        placeCreature(gargoyle1, "A1");
        placeCreature(cyclops1, "A2");
        placeCreature(cyclops2, "C4");
        placeCreature(cyclops3, "E5");
        placeCreature(gorgon1, "C3");
        placeCreature(gorgon2, "D4");
        placeCreature(ranger4, "E4");

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

        defender = new LegionServerSide("Gr03", "Gr01", hexLabel, null, green, game,
            centaur, lion, ranger, ranger);
        attacker = new LegionServerSide("Bk03", "Bk01", hexLabel, null, black, game,
            cyclops, gorgon, gorgon, gorgon, gorgon, ranger, ranger);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(3);

        battle = new BattleServerSide(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        CreatureServerSide centaur1 = defender.getCritter(0);
        CreatureServerSide lion1 = defender.getCritter(1);
        CreatureServerSide ranger1 = defender.getCritter(2);
        CreatureServerSide ranger2 = defender.getCritter(3);

        CreatureServerSide cyclops1 = attacker.getCritter(0);
        CreatureServerSide gorgon1 = attacker.getCritter(1);
        CreatureServerSide gorgon2 = attacker.getCritter(2);
        CreatureServerSide gorgon3 = attacker.getCritter(3);
        CreatureServerSide gorgon4 = attacker.getCritter(4);
        CreatureServerSide ranger3 = attacker.getCritter(5);
        CreatureServerSide ranger4 = attacker.getCritter(6);

        placeCreature(centaur1, "D3");
        placeCreature(lion1, "E3");
        placeCreature(ranger1, "C3");
        placeCreature(ranger2, "D4");

        placeCreature(cyclops1, "D2");
        placeCreature(gorgon1, "A3");
        placeCreature(gorgon2, "A2");
        placeCreature(gorgon3, "A1");
        placeCreature(gorgon4, "C1");
        placeCreature(ranger3, "F1");
        placeCreature(ranger4, "B4");

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

        defender = new LegionServerSide("Gr03", "Gr01", hexLabel, null, green, game,
            centaur, centaur, lion, lion, ranger, ranger);
        attacker = new LegionServerSide("Bk03", "Bk01", hexLabel, null, black, game,
            gorgon, gorgon, ranger, ranger);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(3);

        battle = new BattleServerSide(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        CreatureServerSide centaur1 = defender.getCritter(0);
        CreatureServerSide centaur2 = defender.getCritter(1);
        CreatureServerSide lion1 = defender.getCritter(2);
        CreatureServerSide lion2 = defender.getCritter(3);
        CreatureServerSide ranger1 = defender.getCritter(4);
        CreatureServerSide ranger2 = defender.getCritter(5);

        CreatureServerSide gorgon1 = attacker.getCritter(0);
        CreatureServerSide gorgon2 = attacker.getCritter(1);
        CreatureServerSide ranger3 = attacker.getCritter(2);
        CreatureServerSide ranger4 = attacker.getCritter(3);

        placeCreature(centaur1, "D4");
        placeCreature(centaur2, "B1");
        placeCreature(lion1, "C5");
        placeCreature(lion2, "B2");
        placeCreature(ranger1, "D6");
        placeCreature(ranger2, "E3");

        placeCreature(gorgon1, "D2");
        placeCreature(gorgon2, "D3");
        placeCreature(ranger3, "B3");
        placeCreature(ranger4, "F1");

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

        defender = new LegionServerSide("Gr03", "Gr01", hexLabel, null, green, game,
            troll, troll, troll, troll, wyvern);
        attacker = new LegionServerSide("Bk03", "Bk01", hexLabel, null, black, game,
            ranger, ranger, ranger);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new BattleServerSide(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 2,
            Constants.BattlePhase.FIGHT);

        CreatureServerSide troll1 = defender.getCritter(0);
        CreatureServerSide troll2 = defender.getCritter(1);
        CreatureServerSide troll3 = defender.getCritter(2);
        CreatureServerSide troll4 = defender.getCritter(3);
        CreatureServerSide wyvern1 = defender.getCritter(4);

        CreatureServerSide ranger1 = attacker.getCritter(0);
        CreatureServerSide ranger2 = attacker.getCritter(1);
        CreatureServerSide ranger3 = attacker.getCritter(2);

        placeCreature(troll1, "D6");
        placeCreature(troll2, "B3");
        placeCreature(troll3, "C3");
        placeCreature(troll4, "E4");
        placeCreature(wyvern1, "E3");

        placeCreature(ranger1, "E1");
        placeCreature(ranger2, "E2");
        placeCreature(ranger3, "F2");

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

        defender = new LegionServerSide("Gr03", "Gr01", hexLabel, null, green, game,
            dragon, dragon, minotaur, minotaur, minotaur);
        attacker = new LegionServerSide("Bk03", "Bk01", hexLabel, null, black, game,
            ranger, ranger, ranger);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new BattleServerSide(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 2,
            Constants.BattlePhase.FIGHT);

        CreatureServerSide dragon1 = defender.getCritter(0);
        CreatureServerSide dragon2 = defender.getCritter(1);
        CreatureServerSide minotaur1 = defender.getCritter(2);
        CreatureServerSide minotaur2 = defender.getCritter(3);
        CreatureServerSide minotaur3 = defender.getCritter(4);

        CreatureServerSide ranger1 = attacker.getCritter(0);
        CreatureServerSide ranger2 = attacker.getCritter(1);
        CreatureServerSide ranger3 = attacker.getCritter(2);

        placeCreature(dragon1, "D3");
        placeCreature(dragon2, "C3");
        placeCreature(minotaur1, "E4");
        placeCreature(minotaur2, "B2");
        placeCreature(minotaur3, "A1");

        placeCreature(ranger1, "E2");
        placeCreature(ranger2, "C2");
        placeCreature(ranger3, "E5");

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

        defender = new LegionServerSide("Gr03", "Gr01", hexLabel, null, green, game,
            hydra);
        attacker = new LegionServerSide("Bk03", "Bk01", hexLabel, null, black, game,
            hydra, guardian);

        game.getPlayer("Green").addLegion(defender);
        game.getPlayer("Black").addLegion(attacker);

        attacker.setEntrySide(5);

        battle = new BattleServerSide(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 1,
            Constants.BattlePhase.FIGHT);

        CreatureServerSide hydra1 = defender.getCritter(0);
        CreatureServerSide hydra2 = attacker.getCritter(0);
        CreatureServerSide guardian1 = attacker.getCritter(1);

        placeCreature(hydra1, "D5");
        placeCreature(hydra2, "E3");
        placeCreature(guardian1, "E4");

        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), hydra2
            .getCurrentHex()));
        assertEquals(5, hydra2.getStrikeNumber(hydra1));
    }
}
