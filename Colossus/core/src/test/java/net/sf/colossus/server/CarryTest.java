package net.sf.colossus.server;


import java.util.Iterator;

import junit.framework.TestCase;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;


/**
 *  JUnit test for line of sight.
 *  @version $Id$
 *  @author David Ripton
 */

public class CarryTest extends TestCase
{
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
    CreatureType warlock;

    Player red;
    Player blue;

    public CarryTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        String[] dummyArgs = new String[0];
        Start startObject = new Start(dummyArgs);
        game = new GameServerSide(startObject);
        VariantSupport.loadVariantByName("Default", true);

        red = game.addPlayer("Red", "SimpleAI");
        blue = game.addPlayer("Blue", "SimpleAI");

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
        warlock = game.getVariant().getCreatureByName("Warlock");
    }

    public void testCarries()
    {
        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("35"); // Desert

        attacker = new LegionServerSide("Rd03", "Rd01", hex, hex, red, game,
            centaur, centaur, lion, colossus);
        defender = new LegionServerSide("Bu03", "Bu01", hex, hex, blue, game,
            hydra);

        game.getPlayer("Red").addLegion(attacker);
        game.getPlayer("Blue").addLegion(defender);

        attacker.setEntrySide(EntrySide.values()[5]);

        battle = new BattleServerSide(game, attacker, defender,
            BattleServerSide.LegionTags.DEFENDER, hex, 2,
            BattlePhase.FIGHT);

        CreatureServerSide centaur1 = attacker.getCritter(0);
        CreatureServerSide centaur2 = attacker.getCritter(1);
        CreatureServerSide lion1 = attacker.getCritter(2);
        CreatureServerSide colossus1 = attacker.getCritter(3);

        CreatureServerSide hydra1 = defender.getCritter(0);

        placeCreature(centaur1, "C5");
        placeCreature(centaur2, "D6");
        placeCreature(lion1, "E4");
        placeCreature(colossus1, "C4");

        placeCreature(hydra1, "D5");

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

    private void placeCreature(CreatureServerSide creature,
        String battleHexLabel)
    {
        MasterBoardTerrain terrain = battle.getMasterHex().getTerrain();
        BattleHex battleHex = HexMap.getHexByLabel(terrain, battleHexLabel);
        creature.setCurrentHex(battleHex);
    }

    public void testCarries2()
    {
        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("1"); // Plains

        attacker = new LegionServerSide("Rd03", "Rd01", hex, hex, red, game,
            warlock, warlock, colossus);
        defender = new LegionServerSide("Bu03", "Bu01", hex, hex, blue, game,
            gargoyle, ogre, ogre);

        game.getPlayer("Red").addLegion(attacker);
        game.getPlayer("Blue").addLegion(defender);

        attacker.setEntrySide(EntrySide.values()[5]);

        battle = new BattleServerSide(game, attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, 3,
            BattlePhase.FIGHT);

        CreatureServerSide warlock1 = attacker.getCritter(0);
        CreatureServerSide warlock2 = attacker.getCritter(1);
        CreatureServerSide colossus1 = attacker.getCritter(2);

        CreatureServerSide gargoyle1 = defender.getCritter(0);
        CreatureServerSide ogre1 = defender.getCritter(1);
        CreatureServerSide ogre2 = defender.getCritter(2);

        gargoyle1.setHits(3);
        ogre1.setHits(5);
        ogre2.setHits(5);

        placeCreature(warlock1, "A3");
        placeCreature(warlock2, "B4");
        placeCreature(colossus1, "E3");

        placeCreature(gargoyle1, "D3");
        placeCreature(ogre1, "E2");
        placeCreature(ogre2, "F2");

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
