/**
 *
 */
package net.sf.colossus.game;


import junit.framework.TestCase;
import net.sf.colossus.client.BattleClientSide;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.server.BattleServerSide;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 *
 */
public class BattleTest extends TestCase
{

    Game game;
    CreatureType cyclops;
    CreatureType troll;
    Player red;
    Player blue;
    Legion attacker;
    Legion defender;
    MasterHex hex;
    Battle battle;
    BattleClientSide battleCS;
    BattleServerSide battleSS;

    /**
     * @param name
     */
    public BattleTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        String[] names = new String[0];

        game = new Game(variant, names);
        assertEquals("Default", game.getVariant().getName());

        hex = game.getVariant().getMasterBoard().getHexByLabel("35");
        assertEquals("Desert", hex.getTerrain().getId());

        red = new Player(game, "Red", 0);
        blue = new Player(game, "Blue", 1);
        game.addPlayer(red);
        game.addPlayer(blue);

        attacker = new LegionClientSide(red, "Rd03", hex);
        defender = new LegionClientSide(blue, "Bu09", hex);

        red.addLegion(attacker);
        blue.addLegion(defender);

        cyclops = game.getVariant().getCreatureByName("Cyclops");
        troll = game.getVariant().getCreatureByName("Troll");

        battleCS = new BattleClientSide(game, attacker, defender, hex);
        battle = battleCS;

        assertEquals(battleCS.getBattleTurnNumber(), 1);
        assertEquals(battleCS.getAttackingLegion(), attacker);
        assertEquals(battleCS.getDefendingLegion(), defender);

        // Can't do below, because LegionClientSide does not work with generic Player
        // assertEquals(battle.getAttackingLegion().getPlayer().getName(), "Red");
        // assertEquals(battle.getDefendingLegion().getPlayer().getName(), "Blue");

    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testInit()
    {
        battleCS.init(2, red, BattlePhase.MOVE);
        assertEquals(battle.getBattleTurnNumber(), 2);
        assertEquals(battle.getAttackingLegion(), attacker);
    }

    public void testBattleTurnNumber()
    {
        battleCS.setBattleTurnNumber(4);
        assertEquals(battleCS.getBattleTurnNumber(), 4);
        assertEquals(battle.getBattleTurnNumber(), 4);
    }

}
