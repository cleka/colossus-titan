package net.sf.colossus.server;

import java.util.*;
import junit.framework.*;

import net.sf.colossus.client.VariantSupport;
import net.sf.colossus.client.MasterBoard;
import net.sf.colossus.client.HexMap;


/** JUnit test for line of sight. */

public class CarryTest extends TestCase
{
    Battle battle;
    Legion attacker;
    Legion defender;


    public CarryTest(String name)
    {
        super(name);
    }

    protected void setUp()
    {
        Game game = new Game();
        VariantSupport.loadVariant("Default");
        game.initAndLoadData();  // Will load creatures

        game.addPlayer("Red", "SimpleAI");
        game.addPlayer("Blue", "SimpleAI");

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

        String hexLabel = "35";  // Desert

        attacker = new Legion("Rd03", "Rd01", hexLabel, null,
            centaur, centaur, lion, colossus, null, null, null, null,
            "Red", game);
        defender = new Legion("Bu03", "Bu01", hexLabel, null,
            hydra, null, null, null, null, null, null, null,
            "Blue", game);

        game.getPlayer("Red").addLegion(attacker);
        game.getPlayer("Blue").addLegion(defender);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), 
            defender.getMarkerId(), Constants.DEFENDER, hexLabel,
            2, Constants.FIGHT);

        defender.getCritter(0).setCurrentHexLabel("D5");

        attacker.getCritter(0).setCurrentHexLabel("C5");
        attacker.getCritter(1).setCurrentHexLabel("D6");
        attacker.getCritter(2).setCurrentHexLabel("E4");
        attacker.getCritter(3).setCurrentHexLabel("C4");
    }

    public void testCarries()
    {
        Critter hydra = defender.getCritter(0);

        Critter centaur1 = attacker.getCritter(0);
        Critter centaur2 = attacker.getCritter(1);
        Critter lion = attacker.getCritter(2);
        Critter colossus = attacker.getCritter(3);

        assertTrue(hydra.canStrike(centaur1));
        assertTrue(hydra.canStrike(centaur2));
        assertTrue(hydra.canStrike(lion));
        assertTrue(hydra.canStrike(colossus));

        assertTrue(hydra.getDice(centaur1) == 10);
        assertTrue(hydra.getDice(centaur2) == 10);
        assertTrue(hydra.getDice(lion) == 10);
        assertTrue(hydra.getDice(colossus) == 12);

        assertTrue(hydra.getStrikeNumber(centaur1) == 5);
        assertTrue(hydra.getStrikeNumber(centaur2) == 5);
        assertTrue(hydra.getStrikeNumber(lion) == 4);
        assertTrue(hydra.getStrikeNumber(colossus) == 5);

        hydra.findCarries(centaur1);
        assertTrue(battle.getCarryTargets().size() == 3);
        assertTrue(hydra.getPenaltyOptions().size() == 0);

        hydra.findCarries(centaur2);
        assertTrue(battle.getCarryTargets().size() == 3);
        assertTrue(hydra.getPenaltyOptions().size() == 0);

        hydra.findCarries(lion);
        assertTrue(battle.getCarryTargets().size() == 0);
        assertTrue(hydra.getPenaltyOptions().size() == 1);

        hydra.findCarries(colossus);
        assertTrue(battle.getCarryTargets().size() == 0);
        assertTrue(hydra.getPenaltyOptions().size() == 0);
    }
}
