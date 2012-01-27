package net.sf.colossus.appmain;


import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Options;
import net.sf.colossus.util.ErrorUtils;


public class LoadGameTest extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(LoadGameTest.class
        .getName());

    public static final String SAVED_GAMES_TEST_DIR = "./core/src/functest/resource/colossus-home/saves/";

    public LoadGameTest(String testName)
    {
        super(testName);
    }

    public void testLoadSimple3PlayerGame()
    {
        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();
        String fileName = makeFullPath("3-players-no-recruit.xml");
        LOGGER.info("Filename: " + fileName);
        String[] args = { "--load", fileName };

        Start.main(args);

        assertFalse("Error message dialog would have been shown!", ErrorUtils
            .checkErrorDuringFunctionalTest());
    }

    public void testLoadGameWithRecruit()
    {
        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();
        String fileName = makeFullPath("Simple-6-players.xml");
        LOGGER.info("Filename: " + fileName);

        String[] args = { "--load", fileName };

        Start.main(args);

        assertFalse("Error message dialog would have been shown!", ErrorUtils
            .checkErrorDuringFunctionalTest());
    }

    private String makeFullPath(String fileName)
    {
        return SAVED_GAMES_TEST_DIR + fileName;
    }
}
