package net.sf.colossus.server;


import java.util.Iterator;

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
        Variant variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);

        assertEquals("Default", game.getVariant().getName());

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
        warlock = game.getVariant().getCreatureByName("Warlock");
    }

    public void testCarries()
    {
        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("35");
        assertEquals("Desert", hex.getTerrain().getId());

        attacker = new LegionServerSide("Rd03", null, hex, hex, red, game,
            centaur, centaur, lion, colossus);
        defender = new LegionServerSide("Bu03", null, hex, hex, blue, game,
            hydra);

        game.getPlayerByName("Red").addLegion(attacker);
        game.getPlayerByName("Blue").addLegion(defender);

        attacker.setEntrySide(EntrySide.LEFT);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.DEFENDER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

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

        assertEquals(10, game.getBattleStrikeSS().getDice(hydra1, centaur1));
        assertEquals(10, game.getBattleStrikeSS().getDice(hydra1, centaur2));
        assertEquals(10, game.getBattleStrikeSS().getDice(hydra1, lion1));
        assertEquals(12, game.getBattleStrikeSS().getDice(hydra1, colossus1));

        assertEquals(5, game.getBattleStrikeSS().getStrikeNumber(hydra1,
            centaur1));
        assertEquals(5, game.getBattleStrikeSS().getStrikeNumber(hydra1,
            centaur2));
        assertEquals(4, game.getBattleStrikeSS()
            .getStrikeNumber(hydra1, lion1));
        assertEquals(5, game.getBattleStrikeSS().getStrikeNumber(hydra1,
            colossus1));

        hydra1.findCarries(centaur1);
        assertEquals(3, battle.getCarryTargets().size());
        assertEquals(0, hydra1.getPenaltyOptions().size());

        hydra1.findCarries(centaur2);
        assertEquals(3, battle.getCarryTargets().size());
        assertEquals(0, hydra1.getPenaltyOptions().size());

        hydra1.findCarries(lion1);
        assertEquals(0, battle.getCarryTargets().size());
        assertEquals(2, hydra1.getPenaltyOptions().size());

        hydra1.findCarries(colossus1);
        assertEquals(0, battle.getCarryTargets().size());
        assertEquals(0, hydra1.getPenaltyOptions().size());
    }

    private void placeCreature(Creature creature, String battleHexLabel)
    {
        MasterBoardTerrain terrain = battle.getLocation().getTerrain();
        BattleHex battleHex = terrain.getHexByLabel(battleHexLabel);
        creature.setCurrentHex(battleHex);
    }

    public void testCarries2()
    {
        MasterHex hex = game.getVariant().getMasterBoard().getHexByLabel("1"); // Plains

        attacker = new LegionServerSide("Rd03", null, hex, hex, red, game,
            warlock, warlock, colossus);
        defender = new LegionServerSide("Bu03", null, hex, hex, blue, game,
            gargoyle, ogre, ogre);

        game.getPlayerByName("Red").addLegion(attacker);
        game.getPlayerByName("Blue").addLegion(defender);

        attacker.setEntrySide(EntrySide.LEFT);

        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        battle = game.getBattleSS();

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

        assertEquals(10, game.getBattleStrikeSS()
            .getDice(colossus1, gargoyle1));
        assertEquals(10, game.getBattleStrikeSS().getDice(colossus1, ogre1));
        assertEquals(10, game.getBattleStrikeSS().getDice(colossus1, ogre2));

        assertEquals(3, game.getBattleStrikeSS().getStrikeNumber(colossus1,
            gargoyle1));
        assertEquals(2, game.getBattleStrikeSS().getStrikeNumber(colossus1,
            ogre1));
        assertEquals(2, game.getBattleStrikeSS().getStrikeNumber(colossus1,
            ogre2));

        colossus1.findCarries(gargoyle1);
        assertEquals(2, battle.getCarryTargets().size());
        assertEquals(0, colossus1.getPenaltyOptions().size());

        colossus1.findCarries(ogre1);
        assertEquals(1, battle.getCarryTargets().size());
        assertEquals(2, colossus1.getPenaltyOptions().size());
        Iterator<PenaltyOption> it = colossus1.getPenaltyOptions().iterator();
        PenaltyOption po = it.next();
        assertEquals(2, po.getCarryTargets().size());

        colossus1.findCarries(ogre2);
        assertEquals(1, battle.getCarryTargets().size());
        assertEquals(2, colossus1.getPenaltyOptions().size());
        it = colossus1.getPenaltyOptions().iterator();
        po = it.next();
        assertEquals(2, po.getCarryTargets().size());
    }
}
