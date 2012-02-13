package net.sf.colossus.appmain;


import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.server.GameServerSideTestAccess;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.MasterHex;


public class LoadGameWithBattleTest extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(LoadGameWithBattleTest.class.getName());

    public static final String SAVED_GAMES_TEST_DIR = "./core/src/functest/resource/colossus-home/saves/";

    public LoadGameWithBattleTest(String testName)
    {
        super(testName);
    }

    public void testLoadReadyToAttackGame()
    {
        // setupTheGame waits in server's mutex until game is up
        GameServerSideTestAccess game = setupTheGame("00-attacked.xml");
        if (game == null)
        {
            assertTrue("Game Test Access returned as null!", false);
            return;
        }

        // // TODO: do better
        // LOGGER.info("Sleeping 5 secs to give game time to come up...");
        // WhatNextManager.sleepFor(5000);

        game.showLocalClients();
        Client attacker = game.getClientForName("attacker");
        Client defender = game.getClientForName("defender");

        assertNotNull(attacker);
        assertNotNull(defender);

        // in this case attacker is given chance to concede
        attacker.testCaseAutoDontFlee = true;
        // don't negotiate either, do same as click "fight"
        attacker.testCaseAutoDenyNegotiate = true;

        MasterHex hex = game.getVariant().getMasterBoard()
            .getHexByLabel("112");
        attacker.engage(hex);

        WhatNextManager.sleepFor(5000);

        boolean DO_QUIT = false;
        if (DO_QUIT)
        {
            triggerQuitAfterOneSec(attacker);
        }

        LOGGER.fine("Wait that  game is completed");
        game.waitThatGameIsCompleted();
        LOGGER.fine("All right, game is completed");

        assertFalse("Error message dialog would have been shown!",
            ErrorUtils.checkErrorDuringFunctionalTest());

    }

    /**
     * We trigger menuQuitGame action after a delay, otherwise
     * game.server might be already null by the time when we want
     * to wait until game is completed.
     * @param c The Client in which to trigger menuQuit
     */
    private void triggerQuitAfterOneSec(final Client c)
    {
        WhatNextManager.sleepFor(1000);
        Runnable r = new Runnable()
        {
            public void run()
            {
                LOGGER.info("Triggering menuQuitGame now!");
                c.getGUI().menuQuitGame();
            }
        };
        new Thread(r).start();
    }

    private GameServerSideTestAccess setupTheGame(final String filename)
    {
        WhatNextManager.sleepFor(2000);
        GameServerSideTestAccess.clearLastGame();

        Options.setFunctionalTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        Runnable runTheGame = new Runnable()
        {
            public void run()
            {

                String fileName = makeFullPath(filename);
                LOGGER.info("Filename: " + fileName);
                String[] args = { "--load", fileName };
                Start.main(args);
            }
        };
        Thread doRunTheGame = new Thread(runTheGame);
        doRunTheGame.start();

        GameServerSideTestAccess game = GameServerSideTestAccess
            .staticWaitThatGameComesUp();

        return game;
    }

    private String makeFullPath(String fileName)
    {
        return SAVED_GAMES_TEST_DIR + fileName;
    }
}
