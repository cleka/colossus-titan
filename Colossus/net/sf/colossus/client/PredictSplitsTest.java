package net.sf.colossus.server;

import java.util.*;
import junit.framework.*;



/** 
 *  JUnit test for split prediction. 
 *  @version $Id$
 *  @author David Ripton
 */
public class PredictSplitsTest extends TestCase
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
    Creature wyvern;
    Creature dragon;
    Creature minotaur;


    public PredictSplitsTest(String name)
    {
        super(name);
    }

    protected void setUp()
    {
        game = new Game();
        VariantSupport.loadVariant("Default");

        game.addPlayer("Black", "SimpleAI");
    }

    public void testPredictSplits()
    {
        root = new Legion("Bl01", null, hexLabel, null,
            titan, angel, centaur, centaur, ogre, ogre, gargoyle, gargoyle,
            "Black", game);

        game.getPlayer("Black").addLegion(root);
        // TODO finish
    }
}

