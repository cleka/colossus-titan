package net.sf.colossus.server;


import java.util.Iterator;

import junit.framework.TestCase;


/** 
 *  JUnit test for line of sight. 
 *  @version $Id$
 *  @author David Ripton
 */

public class CarryTest extends TestCase
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
    Creature warlock;

    Player red;
    Player blue;

    public CarryTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        game = new Game();
        VariantSupport.loadVariant("Default", true);

        red = game.addPlayer("Red", "SimpleAI");
        blue = game.addPlayer("Blue", "SimpleAI");

        cyclops = (Creature)game.getVariant().getCreatureByName("Cyclops");
        troll = (Creature)game.getVariant().getCreatureByName("Troll");
        ogre = (Creature)game.getVariant().getCreatureByName("Ogre");
        ranger = (Creature)game.getVariant().getCreatureByName("Ranger");
        gorgon = (Creature)game.getVariant().getCreatureByName("Gorgon");
        lion = (Creature)game.getVariant().getCreatureByName("Lion");
        griffon = (Creature)game.getVariant().getCreatureByName("Griffon");
        hydra = (Creature)game.getVariant().getCreatureByName("Hydra");
        centaur = (Creature)game.getVariant().getCreatureByName("Centaur");
        colossus = (Creature)game.getVariant().getCreatureByName("Colossus");
        gargoyle = (Creature)game.getVariant().getCreatureByName("Gargoyle");
        warlock = (Creature)game.getVariant().getCreatureByName("Warlock");
    }

    public void testCarries()
    {
        String hexLabel = "35"; // Desert

        attacker = new Legion("Rd03", "Rd01", hexLabel, null, red, game,
            centaur, centaur, lion, colossus);
        defender = new Legion("Bu03", "Bu01", hexLabel, null, blue, game,
            hydra);

        game.getPlayer("Red").addLegion(attacker);
        game.getPlayer("Blue").addLegion(defender);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.DEFENDER, hexLabel, 2,
            Constants.BattlePhase.FIGHT);

        Critter centaur1 = attacker.getCritter(0);
        Critter centaur2 = attacker.getCritter(1);
        Critter lion1 = attacker.getCritter(2);
        Critter colossus1 = attacker.getCritter(3);

        Critter hydra1 = defender.getCritter(0);

        centaur1.setCurrentHexLabel("C5");
        centaur2.setCurrentHexLabel("D6");
        lion1.setCurrentHexLabel("E4");
        colossus1.setCurrentHexLabel("C4");

        hydra1.setCurrentHexLabel("D5");

        assertTrue(hydra1.canStrike(centaur1));
        assertTrue(hydra1.canStrike(centaur2));
        assertTrue(hydra1.canStrike(lion1));
        assertTrue(hydra1.canStrike(colossus1));

        assertTrue(hydra1.getDice(centaur1) == 10);
        assertTrue(hydra1.getDice(centaur2) == 10);
        assertTrue(hydra1.getDice(lion1) == 10);
        assertTrue(hydra1.getDice(colossus1) == 12);

        assertTrue(hydra1.getStrikeNumber(centaur1) == 5);
        assertTrue(hydra1.getStrikeNumber(centaur2) == 5);
        assertTrue(hydra1.getStrikeNumber(lion1) == 4);
        assertTrue(hydra1.getStrikeNumber(colossus1) == 5);

        hydra1.findCarries(centaur1);
        assertTrue(battle.getCarryTargets().size() == 3);
        assertTrue(hydra1.getPenaltyOptions().size() == 0);

        hydra1.findCarries(centaur2);
        assertTrue(battle.getCarryTargets().size() == 3);
        assertTrue(hydra1.getPenaltyOptions().size() == 0);

        hydra1.findCarries(lion1);
        assertTrue(battle.getCarryTargets().size() == 0);
        assertTrue(hydra1.getPenaltyOptions().size() == 2);

        hydra1.findCarries(colossus1);
        assertTrue(battle.getCarryTargets().size() == 0);
        assertTrue(hydra1.getPenaltyOptions().size() == 0);
    }

    public void testCarries2()
    {
        String hexLabel = "1"; // Plains

        attacker = new Legion("Rd03", "Rd01", hexLabel, null, red, game,
            warlock, warlock, colossus);
        defender = new Legion("Bu03", "Bu01", hexLabel, null, blue, game,
            gargoyle, ogre, ogre);

        game.getPlayer("Red").addLegion(attacker);
        game.getPlayer("Blue").addLegion(defender);

        attacker.setEntrySide(5);

        battle = new Battle(game, attacker.getMarkerId(), defender
            .getMarkerId(), Constants.ATTACKER, hexLabel, 3,
            Constants.BattlePhase.FIGHT);

        Critter warlock1 = attacker.getCritter(0);
        Critter warlock2 = attacker.getCritter(1);
        Critter colossus1 = attacker.getCritter(2);

        Critter gargoyle1 = defender.getCritter(0);
        Critter ogre1 = defender.getCritter(1);
        Critter ogre2 = defender.getCritter(2);

        gargoyle1.setHits(3);
        ogre1.setHits(5);
        ogre2.setHits(5);

        warlock1.setCurrentHexLabel("A3");
        warlock2.setCurrentHexLabel("B4");
        colossus1.setCurrentHexLabel("E3");

        gargoyle1.setCurrentHexLabel("D3");
        ogre1.setCurrentHexLabel("E2");
        ogre2.setCurrentHexLabel("F2");

        assertTrue(colossus1.canStrike(gargoyle1));
        assertTrue(colossus1.canStrike(ogre1));
        assertTrue(colossus1.canStrike(ogre2));

        assertTrue(colossus1.getDice(gargoyle1) == 10);
        assertTrue(colossus1.getDice(ogre1) == 10);
        assertTrue(colossus1.getDice(ogre2) == 10);

        assertTrue(colossus1.getStrikeNumber(gargoyle1) == 3);
        assertTrue(colossus1.getStrikeNumber(ogre1) == 2);
        assertTrue(colossus1.getStrikeNumber(ogre2) == 2);

        colossus1.findCarries(gargoyle1);
        assertTrue(battle.getCarryTargets().size() == 2);
        assertTrue(colossus1.getPenaltyOptions().size() == 0);

        colossus1.findCarries(ogre1);
        assertTrue(battle.getCarryTargets().size() == 1);
        assertTrue(colossus1.getPenaltyOptions().size() == 2);
        Iterator<PenaltyOption> it = colossus1.getPenaltyOptions().iterator();
        PenaltyOption po = it.next();
        assertTrue(po.getCarryTargets().size() == 2);

        colossus1.findCarries(ogre2);
        assertTrue(battle.getCarryTargets().size() == 1);
        assertTrue(colossus1.getPenaltyOptions().size() == 2);
        it = colossus1.getPenaltyOptions().iterator();
        po = it.next();
        assertTrue(po.getCarryTargets().size() == 2);
    }
}
