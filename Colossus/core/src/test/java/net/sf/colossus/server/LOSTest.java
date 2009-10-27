package net.sf.colossus.server;


import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * JUnit test for line of sight.
 *
 * @author David Ripton
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
    CreatureType cyclops;
    CreatureType troll;
    CreatureType ogre;
    CreatureType ranger;
    CreatureType gorgon;
    CreatureType lion;
    CreatureType griffon;
    CreatureType hydra;
    CreatureType centaur;
    CreatureType colossus;
    CreatureType gargoyle;
    CreatureType wyvern;
    CreatureType dragon;
    CreatureType minotaur;
    CreatureType guardian;
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
        // Nothing to do here, moved stuff to setupForVariant because
        // we need now the variant name as argument.
    }

    private void setupForVariant(String variantName)
    {
        Variant v = VariantSupport.loadVariantByName(variantName, true);
        game = GameServerSide.makeNewGameServerSide(v);
        createPlayersAndCreatures();
    }

    private void createPlayersAndCreatures()
    {
        black = game.createAndAddPlayer("Black", "SimpleAI");
        green = game.createAndAddPlayer("Green", "SimpleAI");
        red = game.createAndAddPlayer("Red", "SimpleAI");
        blue = game.createAndAddPlayer("Blue", "SimpleAI");

        cyclops = game.getVariant().getCreatureByName("Cyclops");
        troll = game.getVariant().getCreatureByName("Troll");
        ogre = game.getVariant().getCreatureByName("Ogre");
        ranger = game.getVariant().getCreatureByName("Ranger");
        gorgon = game.getVariant().getCreatureByName("Gorgon");
        lion = game.getVariant().getCreatureByName("Lion");
        griffon = game.getVariant().getCreatureByName("Griffon");
        hydra = game.getVariant().getCreatureByName("Hydra");
        centaur = game.getVariant().getCreatureByName("Centaur");
        colossus = game.getVariant().getCreatureByName("Colossus");
        gargoyle = game.getVariant().getCreatureByName("Gargoyle");
        wyvern = game.getVariant().getCreatureByName("Wyvern");
        dragon = game.getVariant().getCreatureByName("Dragon");
        minotaur = game.getVariant().getCreatureByName("Minotaur");
        guardian = game.getVariant().getCreatureByName("Guardian");
    }

    // Example 6 from Bruno Wolff's clarifications.
    // TODO Should allow two different strike numbers against centaur.
    public void testLOS1()
    {
        LOGGER.log(Level.FINEST, "testLOS1()");
        setupForVariant("Default");

        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("3"); // Brush

        defender = new LegionServerSide("Rd03", null, hex, hex, red, game,
            centaur, gargoyle);
        attacker = new LegionServerSide("Bl03", null, hex, hex, black, game,
            hydra);

        game.getPlayerByName("Red").addLegion(defender);
        game.getPlayerByName("Blue").addLegion(attacker);

        attacker.setEntrySide(EntrySide.RIGHT);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature centaur1 = defender.getCritter(0);
        Creature gargoyle1 = defender.getCritter(1);

        Creature hydra1 = attacker.getCritter(0);

        placeCreature(centaur1, "B3");
        placeCreature(gargoyle1, "B4");

        placeCreature(hydra1, "D4");

        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), centaur1
            .getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), gargoyle1
            .getCurrentHex()));
    }

    private void placeCreature(Creature creature, String battleHexLabel)
    {
        MasterBoardTerrain terrain = battle.getLocation().getTerrain();
        BattleHex battleHex = terrain.getHexByLabel(battleHexLabel);
        creature.setCurrentHex(battleHex);
    }

    public void testLOS2()
    {
        LOGGER.log(Level.FINEST, "testLOS2()");
        setupForVariant("Default");

        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("40"); // Jungle

        defender = new LegionServerSide("Gr03", null, hex, hex, green, game,
            centaur, centaur, lion, lion, ranger, ranger, ranger);
        attacker = new LegionServerSide("Bk03", null, hex, hex, black, game,
            gargoyle, cyclops, cyclops, cyclops, gorgon, gorgon, ranger);

        game.getPlayerByName("Green").addLegion(defender);
        game.getPlayerByName("Black").addLegion(attacker);

        attacker.setEntrySide(EntrySide.LEFT);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature centaur1 = defender.getCritter(0);
        Creature centaur2 = defender.getCritter(1);
        Creature lion1 = defender.getCritter(2);
        Creature lion2 = defender.getCritter(3);
        Creature ranger1 = defender.getCritter(4);
        Creature ranger2 = defender.getCritter(5);
        Creature ranger3 = defender.getCritter(6);

        Creature gargoyle1 = attacker.getCritter(0);
        Creature cyclops1 = attacker.getCritter(1);
        Creature cyclops2 = attacker.getCritter(2);
        Creature cyclops3 = attacker.getCritter(3);
        Creature gorgon1 = attacker.getCritter(4);
        Creature gorgon2 = attacker.getCritter(5);
        Creature ranger4 = attacker.getCritter(6);

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
        setupForVariant("Default");

        MasterHex hex = game.getVariant().getMasterBoard()
            .getHexByLabel("100"); // Tower

        defender = new LegionServerSide("Gr03", null, hex, hex, green, game,
            centaur, lion, ranger, ranger);
        attacker = new LegionServerSide("Bk03", null, hex, hex, black, game,
            cyclops, gorgon, gorgon, gorgon, gorgon, ranger, ranger);

        game.getPlayerByName("Green").addLegion(defender);
        game.getPlayerByName("Black").addLegion(attacker);

        attacker.setEntrySide(EntrySide.BOTTOM);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature centaur1 = defender.getCritter(0);
        Creature lion1 = defender.getCritter(1);
        Creature ranger1 = defender.getCritter(2);
        Creature ranger2 = defender.getCritter(3);

        Creature cyclops1 = attacker.getCritter(0);
        Creature gorgon1 = attacker.getCritter(1);
        Creature gorgon2 = attacker.getCritter(2);
        Creature gorgon3 = attacker.getCritter(3);
        Creature gorgon4 = attacker.getCritter(4);
        Creature ranger3 = attacker.getCritter(5);
        Creature ranger4 = attacker.getCritter(6);

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
        setupForVariant("Default");

        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("4"); // Hills

        defender = new LegionServerSide("Gr03", null, hex, hex, green, game,
            centaur, centaur, lion, lion, ranger, ranger);
        attacker = new LegionServerSide("Bk03", null, hex, hex, black, game,
            gorgon, gorgon, ranger, ranger);

        game.getPlayerByName("Green").addLegion(defender);
        game.getPlayerByName("Black").addLegion(attacker);

        attacker.setEntrySide(EntrySide.BOTTOM);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature centaur1 = defender.getCritter(0);
        Creature centaur2 = defender.getCritter(1);
        Creature lion1 = defender.getCritter(2);
        Creature lion2 = defender.getCritter(3);
        Creature ranger1 = defender.getCritter(4);
        Creature ranger2 = defender.getCritter(5);

        Creature gorgon1 = attacker.getCritter(0);
        Creature gorgon2 = attacker.getCritter(1);
        Creature ranger3 = attacker.getCritter(2);
        Creature ranger4 = attacker.getCritter(3);

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
        setupForVariant("TG-ConceptIII");

        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("1"); // Plains - Delta

        defender = new LegionServerSide("Gr03", null, hex, hex, green, game,
            troll, troll, troll, troll, wyvern);
        attacker = new LegionServerSide("Bk03", null, hex, hex, black, game,
            ranger, ranger, ranger);

        game.getPlayerByName("Green").addLegion(defender);
        game.getPlayerByName("Black").addLegion(attacker);

        attacker.setEntrySide(EntrySide.LEFT);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature troll1 = defender.getCritter(0);
        Creature troll2 = defender.getCritter(1);
        Creature troll3 = defender.getCritter(2);
        Creature troll4 = defender.getCritter(3);
        Creature wyvern1 = defender.getCritter(4);

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
        setupForVariant("Badlands-JDG");

        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel(
            "5000"); // MountainsAlt

        defender = new LegionServerSide("Gr03", null, hex, hex, green, game,
            dragon, dragon, minotaur, minotaur, minotaur);
        attacker = new LegionServerSide("Bk03", null, hex, hex, black, game,
            ranger, ranger, ranger);

        game.getPlayerByName("Green").addLegion(defender);
        game.getPlayerByName("Black").addLegion(attacker);

        attacker.setEntrySide(EntrySide.LEFT);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature dragon1 = defender.getCritter(0);
        Creature dragon2 = defender.getCritter(1);
        Creature minotaur1 = defender.getCritter(2);
        Creature minotaur2 = defender.getCritter(3);
        Creature minotaur3 = defender.getCritter(4);

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
        setupForVariant("Default");

        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("40"); // Jungle

        defender = new LegionServerSide("Gr03", null, hex, hex, green, game,
            hydra);
        attacker = new LegionServerSide("Bk03", null, hex, hex, black, game,
            hydra, guardian);

        game.getPlayerByName("Green").addLegion(defender);
        game.getPlayerByName("Black").addLegion(attacker);

        attacker.setEntrySide(EntrySide.LEFT);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature hydra1 = defender.getCritter(0);
        CreatureServerSide hydra2 = attacker.getCritter(0);
        Creature guardian1 = attacker.getCritter(1);

        placeCreature(hydra1, "D5");
        placeCreature(hydra2, "E3");
        placeCreature(guardian1, "E4");

        assertTrue(!battle.isLOSBlocked(hydra1.getCurrentHex(), hydra2
            .getCurrentHex()));
        assertEquals(5, game.getBattleStrikeSS().getStrikeNumber(hydra2,
            hydra1));
    }

    public void testLOS8()
    {
        LOGGER.log(Level.FINEST, "testLOS8()");
        setupForVariant("Default");

        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("7"); // Desert

        defender = new LegionServerSide("Gr03", null, hex, hex, green, game,
            ranger);
        attacker = new LegionServerSide("Bk03", null, hex, hex, black, game,
            hydra);

        game.getPlayerByName("Green").addLegion(defender);
        game.getPlayerByName("Black").addLegion(attacker);

        attacker.setEntrySide(EntrySide.RIGHT);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature hydra = defender.getCritter(0);
        Creature ranger = attacker.getCritter(0);

        placeCreature(ranger, "D4");
        placeCreature(hydra, "A1");

        // should be blocked: even from cliff may not RS over a dune
        // Current implementation is not working correctly
        //   - see 2820231 Illegal rangestrike
        assertTrue(battle.isLOSBlocked(ranger.getCurrentHex(), hydra
            .getCurrentHex()));

    }

    public void testLOS9()
    {
        LOGGER.log(Level.FINEST, "testLOS9()");
        setupForVariant("Default");

        MasterHex hex = game.getVariant().getMasterBoard()
            .getHexByLabel("100"); // Tower

        defender = new LegionServerSide("Gr03", null, hex, hex, green, game,
            centaur, lion, ranger, ranger);
        attacker = new LegionServerSide("Bk03", null, hex, hex, black, game,
            cyclops, gorgon, gorgon, gorgon, gorgon, ranger, ranger);

        game.getPlayerByName("Green").addLegion(defender);
        game.getPlayerByName("Black").addLegion(attacker);

        attacker.setEntrySide(EntrySide.BOTTOM);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

        Creature ranger2 = defender.getCritter(3);

        Creature cyclops1 = attacker.getCritter(0);
        Creature gorgon1 = attacker.getCritter(1);

        placeCreature(ranger2, "D4");

        placeCreature(cyclops1, "D2");
        placeCreature(gorgon1, "C2");

        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), cyclops1
            .getCurrentHex()));
        assertTrue(battle.isLOSBlocked(ranger2.getCurrentHex(), gorgon1
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(gorgon1.getCurrentHex(), ranger2
            .getCurrentHex()));

        assertTrue(battle.isLOSBlocked(cyclops1.getCurrentHex(), ranger2
            .getCurrentHex()));
    }
}
