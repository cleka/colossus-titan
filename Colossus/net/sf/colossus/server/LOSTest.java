package net.sf.colossus.server;

import java.util.*;
import junit.framework.*;

import net.sf.colossus.client.VariantSupport;


/** JUnit test for line of sight. */

public class LOSTest extends TestCase
{
    public LOSTest(String name)
    {
        super(name);
    }

    protected void setUp()
    {
        Game game = new Game();
        VariantSupport.loadVariant("Default");
        game.initAndLoadData();  // Will load creatures

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

        Legion attacker = new Legion("Bk03", "Bk01", hexLabel, null,
            gargoyle, cyclops, cyclops, cyclops, gorgon, gorgon, ranger, null,
            "Black", game);
        Legion defender = new Legion("Gr03", "Gr01", hexLabel, null,
            centaur, centaur, lion, lion, ranger, ranger, ranger, null,
            "Green", game);

        Battle battle = new Battle(game, attacker.getMarkerId(), 
            defender.getMarkerId(), Constants.DEFENDER, hexLabel,
            1, Constants.MOVE);

        // TODO Move the critters into position and advance to a fight phase.
    }

    public void testLOS()
    {
        fail();
    }
}
