package net.sf.colossus.server;

import java.util.*;
import junit.framework.*;

import net.sf.colossus.client.VariantSupport;
import net.sf.colossus.client.MasterBoard;
import net.sf.colossus.client.HexMap;


/** JUnit test for line of sight. */

public class LOSTest extends TestCase
{
    Battle battle;
    Legion attacker;
    Legion defender;


    public LOSTest(String name)
    {
        super(name);
    }

    protected void setUp()
    {
        Game game = new Game();
        VariantSupport.loadVariant("Default");
        game.initAndLoadData();  // Will load creatures

        game.addPlayer("Black", "SimpleAI");
        game.addPlayer("Green", "SimpleAI");

        // Need a non-GUI board so we can look up hexes.
        MasterBoard board = new MasterBoard();

        Creature cyclops = Creature.getCreatureByName("Cyclops");
        Creature troll = Creature.getCreatureByName("Troll");
        Creature ogre = Creature.getCreatureByName("Ogre");
        Creature ranger = Creature.getCreatureByName("Ranger");
        Creature gorgon = Creature.getCreatureByName("Gorgon");
        Creature lion = Creature.getCreatureByName("Lion");
        Creature griffon = Creature.getCreatureByName("Griffon");
        Creature hydra = Creature.getCreatureByName("Hydra");
        Creature centaur = Creature.getCreatureByName("Centaur");
        Creature colossus = Creature.getCreatureByName("Colossus");
        Creature gargoyle = Creature.getCreatureByName("Gargoyle");

        String hexLabel = "40";  // Jungle

        attacker = new Legion("Bk03", "Bk01", hexLabel, null,
            gargoyle, cyclops, cyclops, cyclops, gorgon, gorgon, ranger, null,
            "Black", game);
        defender = new Legion("Gr03", "Gr01", hexLabel, null,
            centaur, centaur, lion, lion, ranger, ranger, ranger, null,
            "Green", game);

        game.getPlayer("Black").addLegion(attacker);
        game.getPlayer("Green").addLegion(defender);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), 
            defender.getMarkerId(), Constants.ATTACKER, hexLabel,
            1, Constants.FIGHT);

        defender.getCritter(0).setCurrentHexLabel("D1");
        defender.getCritter(1).setCurrentHexLabel("E1");
        defender.getCritter(2).setCurrentHexLabel("F1");
        defender.getCritter(3).setCurrentHexLabel("C1");
        defender.getCritter(4).setCurrentHexLabel("D2");
        defender.getCritter(5).setCurrentHexLabel("E2");
        defender.getCritter(6).setCurrentHexLabel("F2");

        attacker.getCritter(0).setCurrentHexLabel("A1");
        attacker.getCritter(1).setCurrentHexLabel("A2");
        attacker.getCritter(2).setCurrentHexLabel("C4");
        attacker.getCritter(3).setCurrentHexLabel("E5");
        attacker.getCritter(4).setCurrentHexLabel("C3");
        attacker.getCritter(5).setCurrentHexLabel("D4");
        attacker.getCritter(6).setCurrentHexLabel("E4");
    }

    public void testLOS()
    {
        assertTrue(!battle.isLOSBlocked(defender.getCritter(4).getCurrentHex(),
            attacker.getCritter(0).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(defender.getCritter(4).getCurrentHex(),
            attacker.getCritter(1).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(4).getCurrentHex(),
            attacker.getCritter(2).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(4).getCurrentHex(),
            attacker.getCritter(3).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(defender.getCritter(4).getCurrentHex(),
            attacker.getCritter(4).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(4).getCurrentHex(),
            attacker.getCritter(5).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(4).getCurrentHex(),
            attacker.getCritter(6).getCurrentHex()));

        assertTrue(battle.isLOSBlocked(defender.getCritter(5).getCurrentHex(),
            attacker.getCritter(0).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(5).getCurrentHex(),
            attacker.getCritter(1).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(5).getCurrentHex(),
            attacker.getCritter(2).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(5).getCurrentHex(),
            attacker.getCritter(3).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(5).getCurrentHex(),
            attacker.getCritter(4).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(defender.getCritter(5).getCurrentHex(),
            attacker.getCritter(5).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(defender.getCritter(5).getCurrentHex(),
            attacker.getCritter(6).getCurrentHex()));

        assertTrue(battle.isLOSBlocked(defender.getCritter(6).getCurrentHex(),
            attacker.getCritter(0).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(6).getCurrentHex(),
            attacker.getCritter(1).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(6).getCurrentHex(),
            attacker.getCritter(2).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(6).getCurrentHex(),
            attacker.getCritter(3).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(defender.getCritter(6).getCurrentHex(),
            attacker.getCritter(4).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(defender.getCritter(6).getCurrentHex(),
            attacker.getCritter(5).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(defender.getCritter(6).getCurrentHex(),
            attacker.getCritter(6).getCurrentHex()));


        assertTrue(battle.isLOSBlocked(attacker.getCritter(4).getCurrentHex(),
            defender.getCritter(0).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(4).getCurrentHex(),
            defender.getCritter(1).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(4).getCurrentHex(),
            defender.getCritter(2).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(attacker.getCritter(4).getCurrentHex(),
            defender.getCritter(3).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(attacker.getCritter(4).getCurrentHex(),
            defender.getCritter(4).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(4).getCurrentHex(),
            defender.getCritter(5).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(4).getCurrentHex(),
            defender.getCritter(6).getCurrentHex()));

        assertTrue(battle.isLOSBlocked(attacker.getCritter(5).getCurrentHex(),
            defender.getCritter(0).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(5).getCurrentHex(),
            defender.getCritter(1).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(5).getCurrentHex(),
            defender.getCritter(2).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(5).getCurrentHex(),
            defender.getCritter(3).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(5).getCurrentHex(),
            defender.getCritter(4).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(attacker.getCritter(5).getCurrentHex(),
            defender.getCritter(5).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(attacker.getCritter(5).getCurrentHex(),
            defender.getCritter(6).getCurrentHex()));

        assertTrue(battle.isLOSBlocked(attacker.getCritter(6).getCurrentHex(),
            defender.getCritter(0).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(6).getCurrentHex(),
            defender.getCritter(1).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(6).getCurrentHex(),
            defender.getCritter(2).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(6).getCurrentHex(),
            defender.getCritter(3).getCurrentHex()));
        assertTrue(battle.isLOSBlocked(attacker.getCritter(6).getCurrentHex(),
            defender.getCritter(4).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(attacker.getCritter(6).getCurrentHex(),
            defender.getCritter(5).getCurrentHex()));
        assertTrue(!battle.isLOSBlocked(attacker.getCritter(6).getCurrentHex(),
            defender.getCritter(6).getCurrentHex()));
    }
}
